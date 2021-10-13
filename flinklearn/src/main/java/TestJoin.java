import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.JoinFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.types.Row;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestJoin {

    public static List<Row> buildOrder() {
        List<Long> timestamp = Arrays.asList(1632981239000L,
                1632981240000L,
                1632981243000L,
                1632981247000L,
                1632981249000L,
                1632981259000L,
                1632981249000L);
        List<String> order = Arrays.asList("1001",
                "1002",
                "1004",
                "1003",
                "1005",
                "1006",
                "1007");
        List<Row> source = new ArrayList<>();
        for (int a = 0; a < timestamp.size(); a++) {
            Row each = new Row(2);
            each.setField(0, timestamp.get(a));
            each.setField(1, order.get(a));
            source.add(each);
        }
        return source;
    }

    public static List<Row> buildOrderInfo() {
        List<Long> timestamp = Arrays.asList(1632981239000L,
                1632981242000L,
                1632981244000L,
                1632981231000L,
                1632981249000L,
                1632981259000L,
                1632981249000L);
        List<String> message = Arrays.asList("this",
                "is",
                "a",
                "join",
                "test",
                "which",
                "ama");
        List<String> order = Arrays.asList("1001",
                "1002",
                "1007",
                "1003",
                "1005",
                "1006",
                "1002");
        List<Row> source = new ArrayList<>();
        for (int a = 0; a < timestamp.size(); a++) {
            Row each = new Row(3);
            each.setField(0, timestamp.get(a));
            each.setField(1, order.get(a));
            each.setField(2, message.get(a));
            source.add(each);
        }
        return source;
    }


    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = TestUtil.iniEnv(1);
        env.getConfig().setAutoWatermarkInterval(200L);


        // order
        String[] names = new String[]{"eve_time", "order"};
        TypeInformation[] types = new TypeInformation[]{Types.LONG, Types.STRING};
        DataStream<Row> order = env.addSource(new TestSource("oder", buildOrder()))
                .returns(Types.ROW_NAMED(names, types))
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy
                                .<Row>forGenerator(WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(0)))
                                .withTimestampAssigner((event, pre) -> (long) event.getField(0))
                                .withIdleness(Duration.ofSeconds(5))
                );


        names = new String[]{"eve_time", "order", "info"};
        types = new TypeInformation[]{Types.LONG, Types.STRING, Types.STRING};
        DataStream<Row> orderInfo = env.addSource(new TestSource("order info", buildOrderInfo()))
                .returns(Types.ROW_NAMED(names, types))
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy
                                .<Row>forGenerator(WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(0)))
                                .withTimestampAssigner((event, pre) -> (long) event.getField(0))
                                .withIdleness(Duration.ofSeconds(5))
                );
        order.join(orderInfo)
                .where(
                        e -> e.getField(1)
                )
                .equalTo(
                        e -> e.getField(1)
                )
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                .apply(new JoinFunction<Row, Row, String>() {
                    @Override
                    public String join(Row first, Row second) throws Exception {
                        return (first.getField(0).toString()) + "," + first.getField(1) + "," + second.getField(2);
                    }
                })
                .print();
        env.execute();


    }
}
