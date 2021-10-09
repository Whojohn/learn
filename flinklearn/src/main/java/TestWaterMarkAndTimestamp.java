import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.types.Row;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TestWaterMarkAndTimestamp {

    public static List<Row> buildRow() {
        List<Long> timestamp = Arrays.asList(1632981239000L,
                1632981240000L,
                1632981243000L,
                1632981247000L,
                1632981249000L,
                1632981259000L,
                1632981249000L);
        List<String> message = Arrays.asList("1",
                "2",
                "3",
                "4",
                "5",
                "6",
                "7");
        List<Row> source = new ArrayList<>();
        for (int a = 0; a < timestamp.size(); a++) {
            Row each = new Row(2);
            each.setField(0, timestamp.get(a));
            each.setField(1, message.get(a));
            source.add(each);
        }
        return source;
    }


    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = TestUtil.iniEnv(1);
        env.getConfig().setAutoWatermarkInterval(10000L);

        String[] names = new String[]{"eve_time", "message"};
        TypeInformation[] types = new TypeInformation[]{Types.LONG, Types.STRING};


        env.addSource(new TestSource("TestWaterMarkAndTimestamp", buildRow())).returns(Types.ROW_NAMED(names, types)).assignTimestampsAndWatermarks(
                WatermarkStrategy
                        .<Row>forGenerator(WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(0)))
                        .withTimestampAssigner((event, pre) -> (long) event.getField(0))
                        .withIdleness(Duration.ofSeconds(5))
        )
                .keyBy(e -> e.getField(1))
                .window(TumblingEventTimeWindows.of(Time.seconds(10)))
                .allowedLateness(Time.seconds(0))
                // 这里用null 是因为分区把每一个元素都分为独立的一个区，所以reduce 相当于无效，没有任何作用，可以 把流转化为 SingleOutputStreamOperator 以查看 windows 中的数据
                .reduce((value1, value2) -> null)
                .print();
        env.execute();
    }
}
