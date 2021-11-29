import env.TestUtil;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestProcessWindowFunction {
    public static List<Row> buildSource() {
        List<String> dat = Arrays.asList("2021-11-24 12:00:00.000",
                "2021-11-24 12:00:01.000",
                "2021-11-24 12:00:11.000",
                "2021-11-24 12:00:06.000",
                "2021-11-24 12:00:07.000",
                "2021-11-24 12:00:08.000",
                "2021-11-24 12:00:09.000",
                "2021-11-24 12:00:09.000",
                "2021-11-24 12:00:32.000");
        List<String> message = Arrays.asList("job1",
                "job1",
                "job2",
                "job1",
                "job3",
                "job1",
                "job3",
                "job3",
                "job1");
        List<Row> source = new ArrayList<>();
        for (int a = 0; a < dat.size(); a++) {
            Row each = new Row(2);
            each.setField(0, dat.get(a));
            each.setField(1, message.get(a));
            source.add(each);
        }
        return source;
    }

    /**
     * 5秒的watermark 延迟， 5秒一个滚动的 tumble window，以 action 字段进行 group by , count(*) 操作； 实现是基于
     * ProcessWindowFunction
     */
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = TestUtil.iniEnv(1);
        env.addSource(new TestSource("TestProcessFunction", buildSource()))
                .returns(Types.ROW_NAMED(new String[]{"event_time", "action"}, Types.STRING, Types.STRING))
                .assignTimestampsAndWatermarks(WatermarkStrategy
                        .<Row>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                        .withTimestampAssigner((event, pre) -> {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                            try {
                                return sdf.parse((String) event.getField(0)).getTime();
                            } catch (ParseException e) {
                                return 0;
                            }
                        })
                ).keyBy(e -> (String) e.getField(1))
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                .process(new ProcessWindowFunction<Row, Row, String, TimeWindow>() {
                    @Override
                    public void process(String s, Context context, Iterable<Row> elements, Collector<Row> out) {
                        int cou = 0;
                        for (Row e : elements) cou += 1;
                        Row outSource = new Row(4);
                        outSource.setField(0, context.window().getStart());
                        outSource.setField(1, context.window().getEnd());
                        outSource.setField(2, s);
                        outSource.setField(3, cou);
                        out.collect(outSource);
                    }
                })
                .print();
        env.execute();
    }
}
