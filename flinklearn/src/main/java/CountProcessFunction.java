import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

public class CountProcessFunction extends ProcessFunction<Tuple2<Long, Long>, Tuple2<Long, Long>> {
    private Integer cou;


    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        cou = new Integer(0);
    }


    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<Tuple2<Long, Long>> out) throws Exception {
        super.onTimer(timestamp, ctx, out);
        cou += 1;
        System.out.println("fire timer: " + timestamp + "  " + cou);
    }

    @Override
    public void processElement(Tuple2<Long, Long> value, Context ctx, Collector<Tuple2<Long, Long>> out) throws Exception {
        ctx.timerService().registerProcessingTimeTimer(ctx.timestamp() + 300);
    }
}