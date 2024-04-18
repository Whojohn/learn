import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.StateBackendOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.util.Collector;

import java.util.Random;

public class TimerBenchMark {
    public final static int TEST_KEY_NUMBER = 10000000;

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        conf.set(StateBackendOptions.STATE_BACKEND, "hashmap");
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(conf);
        env.setParallelism(1).setMaxParallelism(1);


        env.addSource(new RichParallelSourceFunction<String>() {
            int i = 0;

            @Override
            public void run(SourceContext<String> ctx) throws Exception {
                System.out.println("Start   !!!!!!!!!!          !!!!!!!!!!!!!!!!!!" + (System.currentTimeMillis()));
                while (true) {
                    if (i < TEST_KEY_NUMBER) {
                        ctx.collect("2024041708100000000001" + i);
                        i += 1;
                    } else {
                        Thread.sleep(1000);
                    }
                }
            }

            @Override
            public void cancel() {
            }
        }).keyBy(e -> e).process(new KeyedProcessFunction<String, String, Integer>() {

            private final long startTime = System.currentTimeMillis();
            final Random random = new Random();

            @Override
            public void processElement(String value, KeyedProcessFunction<String, String, Integer>.Context ctx,
                                       Collector<Integer> out) throws Exception {

                ctx.timerService().registerProcessingTimeTimer(startTime + (random.nextInt(5) * 1000));

            }

            @Override
            public void onTimer(long timestamp, KeyedProcessFunction<String, String, Integer>.OnTimerContext ctx,
                                Collector<Integer> out) throws Exception {
                super.onTimer(timestamp, ctx, out);
                ctx.getCurrentKey();
                out.collect(1);
            }
        }).addSink(new RichSinkFunction<Integer>() {
            int l;

            @Override
            public void open(Configuration parameters) throws Exception {
                super.open(parameters);
                l = 0;
            }

            @Override
            public void invoke(Integer value, Context context) throws Exception {
                l += value;
                if (l >= TEST_KEY_NUMBER) {
                    System.out.println("Finish  !!!!!!!!!!          !!!!!!!!!!!!!!!!!!" + (System.currentTimeMillis()));
                    System.exit(9);
                }
            }
        });
        env.execute();
    }
}
