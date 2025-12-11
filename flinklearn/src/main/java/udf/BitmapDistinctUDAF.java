package udf;

import org.apache.flink.table.functions.AggregateFunction;
import org.apache.flink.table.functions.FunctionContext;

/**
 * Distinct Count UDAF using RoaringBitmap.
 */
public class BitmapDistinctUDAF extends AggregateFunction<Long, BitmapWrapper> {

    @Override
    public void open(FunctionContext context) throws Exception {
        super.open(context);
    }

    @Override
    public void close() throws Exception {
        super.close();
    }

    @Override
    public BitmapWrapper createAccumulator() {
        BitmapWrapper acc = new BitmapWrapper();
        return acc;
    }

    @Override
    public Long getValue(BitmapWrapper accumulator) {
        return accumulator.getLongCardinality();
    }

    public void accumulate(BitmapWrapper accumulator, Integer value) {
        if (value != null) {
            accumulator.add(value);
        }
    }

    public void merge(BitmapWrapper accumulator, Iterable<BitmapWrapper> it) {
        accumulator.merge(it);
    }
}
