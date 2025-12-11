package udf;

import org.apache.flink.table.annotation.DataTypeHint;
import org.roaringbitmap.RoaringBitmap;

/**
 * 单 Bitmap 累加器
 * key = hash(status + "@" + orderId)
 */
public class BitmapStatusCountAccumulator {

    @DataTypeHint("RAW")
    public RoaringBitmap bitmap;

    // 订单状态表
    @DataTypeHint("RAW")
    public int[] allOrderStatus;

    // 每个状态的订单计数，索引对应 allOrderStatus
    public long[] statusCounts;
}
