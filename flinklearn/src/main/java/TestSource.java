import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.types.Row;

import java.util.List;

public class TestSource extends RichParallelSourceFunction<Row> {
    private boolean label = true;
    private final String sourceName;
    private final List<Row> source;

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
        for (Row each : this.source) {
            System.out.println(sourceName + " output:" + each.toString());
            ctx.collect(each);
            Thread.sleep(10000);
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

}