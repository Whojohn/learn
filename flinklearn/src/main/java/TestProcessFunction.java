import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestProcessFunction extends ProcessFunction<Tuple2<Long, Long>, Tuple2<Long, Long>> {
    // 用于内部标记 fire 次数，防止日志打印乱序，影响判定
    private Integer cou;
    // 注意本地 state 必须以transient修饰
    private transient ValueState<Tuple2<Long, Long>> sum;


    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        cou = 0;
        ValueStateDescriptor<Tuple2<Long, Long>> descriptor =
                new ValueStateDescriptor<>(
                        "average", // state 逻辑名字
                        TypeInformation.of(new TypeHint<Tuple2<Long, Long>>() {
                        }) // 数据类型信息
                );
        // 绑定本地 state
        sum = getRuntimeContext().getState(descriptor);
    }

    /**
     * timer 触发时候处理函数
     *
     * @param timestamp 触发 timer fire 的时间戳
     * @param ctx       OnTimerContext 对象，能够获取触发器相关信息如timestamp，fire 时间等信息。
     * @param out       fire 时间段函数发送的数据(因为fire是通过processElement触发的，因此能够捕获当时的数据)
     * @throws Exception 异常
     */
    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<Tuple2<Long, Long>> out) throws Exception {
        super.onTimer(timestamp, ctx, out);
        cou += 1;
        System.out.println("fire timer: " + timestamp + "  " + cou);
    }

    /**
     * 以 F1 进行分组，每一条数据进行累计
     *
     * @param value 输入值
     * @param ctx   context
     * @param out   输出值
     * @throws Exception 异常
     */
    @Override
    public void processElement(Tuple2<Long, Long> value, Context ctx, Collector<Tuple2<Long, Long>> out) throws Exception {
        // 触发timer 执行。注意没有timestamp的时候会返回空
        if (ctx.timestamp() != null) ctx.timerService().registerProcessingTimeTimer(ctx.timestamp() + 300);
        // 处理逻辑
        System.out.println("source in state is:" + sum.value());
        Tuple2<Long, Long> temp = sum.value();
        temp = temp == null ? new Tuple2<>() : temp;
        // 以f1 进行分组，进行 count 累加
        temp.f0 = (temp.f0 == null ? value.f1 : temp.f0);
        temp.f1 = (temp.f1 == null ? 0 : temp.f1) + 1;

        sum.update(temp);
        out.collect(temp);
    }

    public static List<Row> buildSource() {
        List<Long> timestamp = Arrays.asList(1632981239000L,
                1632981242000L,
                1632981244000L,
                1632981231000L,
                1632981249000L,
                1632981259000L,
                1632981249000L);
        List<Long> message = Arrays.asList(6L,
                6L,
                2L,
                2L,
                3L,
                4L,
                6L);
        List<Row> source = new ArrayList<>();
        for (int a = 0; a < timestamp.size(); a++) {
            Row each = new Row(2);
            each.setField(0, timestamp.get(a));
            each.setField(1, message.get(a));
            source.add(each);
        }
        return source;
    }

    /**
     * keyed state 累计演示
     *
     * @param args 启动参数
     * @throws Exception 异常
     */
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = TestUtil.iniEnv(1);
        env.addSource(new TestSource("TestProcessFunction", buildSource()))
                .map(e -> {
                    Tuple2<Long, Long> temp = new Tuple2<Long, Long>();
                    temp.f0 = (Long) e.getField(0);
                    temp.f1 = (Long) e.getField(1);
                    return temp;
                }).returns(new TypeHint<Tuple2<Long, Long>>() {
        })
                .keyBy(value -> value.getField(1))
                .process(new TestProcessFunction())
                .print();
        env.execute();
    }
}