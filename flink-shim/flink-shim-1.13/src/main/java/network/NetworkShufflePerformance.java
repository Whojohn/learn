package network;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.DiscardingSink;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.transformations.PartitionTransformation;
import org.apache.flink.streaming.runtime.partitioner.OptimizeShufflePartitioner;
import org.apache.flink.util.StringUtils;

import java.time.Duration;
import java.util.Random;

public class NetworkShufflePerformance {
    private final static int LOOP_SIZE = 20000;

    private final static int TEST_STRING_LENGTH = 1000;

    private final static boolean OPTIMIZE_SHUFFLE = true;

    private static <T> DataStream<T> setOptimize(DataStream<T> stream) {
        if (OPTIMIZE_SHUFFLE) {
            return new DataStream<>(
                    stream.getExecutionEnvironment(),
                    new PartitionTransformation<>(stream.getTransformation(), new OptimizeShufflePartitioner<>()));
        } else {
            return stream.rebalance();
        }
    }

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        setOptimize(env.addSource(new RichParallelSourceFunction<String>() {
            long start = 0;
            boolean label = true;
            final long startTime = System.currentTimeMillis();

            @Override
            public void run(SourceContext<String> ctx) throws Exception {
                String testStr = StringUtils.getRandomString(new Random(), TEST_STRING_LENGTH, TEST_STRING_LENGTH);
                while (label) {
                    start += 1;
                    if (start < LOOP_SIZE) {
                        ctx.collect(testStr);
                    } else if (start == LOOP_SIZE) {
                        System.out.println("Finish !!! cost time:   " + (System.currentTimeMillis() - startTime));
                        System.exit(9);
                    } else {
                        Thread.sleep(Duration.ofSeconds(1).toMillis());
                    }
                }
            }

            @Override
            public void cancel() {
                label = false;
            }
        }).setParallelism(1)).
                map(new RichMapFunction<String, String>() {
                    // mock as sink stuck
                    @Override
                    public String map(String s) throws Exception {
                        if (getRuntimeContext().getIndexOfThisSubtask() == 0) {
                            Thread.sleep(Duration.ofMillis(10).toMillis());
                        }
                        return null;
                    }
                }).setParallelism(2).addSink(new DiscardingSink<>()).setParallelism(1);
        env.execute();
    }

}
