import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.time.Duration;


public class TestSource extends RichParallelSourceFunction<Tuple2<Long, Long>> {

    @Override
    public void run(SourceContext<Tuple2<Long, Long>> ctx) throws Exception {
        int cou = 1;
        Tuple2<Long, Long> tempTuple = new Tuple2<>();
        Long[] timeSource = new Long[]{
                1632981239000L,
                1632981240000L,
                1632981243000L,
                1632981247000L,
                1632981249000L,
                1632981259000L,
                1632981249000L};
        for (Long each : timeSource) {
            tempTuple.setFields((long) cou, each);
            ctx.collect(tempTuple);
            System.out.println("source output:" + each);
            Thread.sleep(3000);
            cou += 1;
        }
        Thread.sleep(10000);
        // ctx.collect 方法也有空闲检测，但是没有开放给用户，因此 source 中只能markAsTemporarilyIdle 显式声明告知watermark 此流空闲，生成watermark 不需要等待该流，防止并发流下堵塞
        // 或者通过 WatermarkStrategy.withIdleness 配置，生成全局watermark 时忽略一段时间没有改变的watermark。
        ctx.markAsTemporarilyIdle();

        while (true) {
            Thread.sleep(500);
        }
    }


    @Override
    public void cancel() {

    }

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = TestUtil.iniEnv(2);
        env.getConfig().setAutoWatermarkInterval(10000L);
        env.addSource(new TestSource()).assignTimestampsAndWatermarks(
                WatermarkStrategy
                        .<Tuple2<Long, Long>>forGenerator(WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(0)))
                        .withTimestampAssigner((event, pre) -> event.f1)
                        .withIdleness(Duration.ofSeconds(5))
        )
                .keyBy(e -> e.f0)
                .window(TumblingEventTimeWindows.of(Time.seconds(10)))
                .allowedLateness(Time.seconds(0))
                // 这里用null 是因为分区把每一个元素都分为独立的一个区，所以reduce 相当于无效，没有任何作用，可以 把流转化为 SingleOutputStreamOperator 以查看 windows 中的数据
                .reduce((value1, value2) -> null)
                .print();
        env.execute();
    }
}
