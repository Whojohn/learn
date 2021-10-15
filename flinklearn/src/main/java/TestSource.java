import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.types.Row;

import java.util.ArrayList;
import java.util.List;

public class TestSource extends RichParallelSourceFunction<Row> implements CheckpointedFunction {
    private boolean label = true;
    private final String sourceName;
    private final List<Row> source;
    private transient ListState<Integer> sourceLoc;
    private final List<Integer> sourceLocBuffer = new ArrayList<Integer>();


    /**
     * 必须传入一个 list 包裹的row 才能模拟数据
     *
     * @param sourceName
     * @param source
     */
    public TestSource(String sourceName, List<Row> source) {
        this.sourceName = sourceName;
        this.source = source;
    }


    @Override
    public void run(SourceContext<Row> ctx) throws Exception {

        int signal = 0;
        while (signal < this.sourceLocBuffer.size()) {
            signal = 0;
            for (int a = 0; a < this.sourceLocBuffer.size(); a++) {
                int nowStep = this.sourceLocBuffer.get(a);
                if (nowStep < this.source.size()) {
                    System.out.println(sourceName + " output:" + this.source.get(nowStep).toString());
                    // checkpoint lock 保证操作原子和数据正确
                    synchronized (ctx.getCheckpointLock()) {
                        ctx.collect(this.source.get(nowStep));
                        Thread.sleep(4000);
                        nowStep += 1;
                        this.sourceLocBuffer.set(a, nowStep);
                    }
                } else {
                    signal += 1;
                }
            }
        }
        Thread.sleep(10000);
        // ctx.collect 方法也有空闲检测，但是没有开放给用户，因此 source 中只能markAsTemporarilyIdle 显式声明告知watermark 此流空闲，生成watermark 不需要等待该流，防止并发流下堵塞
        // 或者通过 WatermarkStrategy.withIdleness 配置，生成全局watermark 时忽略一段时间没有改变的watermark。
        ctx.markAsTemporarilyIdle();

        while (this.label) {
            Thread.sleep(3000);
        }
    }

    @Override
    public void cancel() {
        this.label = false;
    }

    @Override
    public void snapshotState(FunctionSnapshotContext functionSnapshotContext) throws Exception {
        this.sourceLoc.clear();
        for (Integer each : this.sourceLocBuffer) {
            this.sourceLoc.add(each);
        }
    }

    /***
     * checkpoint 相关初始化
     * @param functionInitializationContext
     * @throws Exception
     */
    @Override
    public void initializeState(FunctionInitializationContext functionInitializationContext) throws Exception {
        ListStateDescriptor<Integer> source = new ListStateDescriptor<>("sourceState", Integer.class);
        this.sourceLoc = functionInitializationContext.getOperatorStateStore().getListState(source);
        // 判定是否恢复，假如是从状态量恢复，从状态量中读取数据
        if (functionInitializationContext.isRestored()) {
            for (Integer each : this.sourceLoc.get()) {
                sourceLocBuffer.add(each);
            }
        } else {
            this.sourceLocBuffer.add(0);
        }
    }
}