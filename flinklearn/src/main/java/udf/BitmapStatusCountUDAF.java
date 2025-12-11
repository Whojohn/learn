package udf;

import org.apache.flink.table.functions.AggregateFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * 单 Bitmap 统计不同订单状态下的订单数量
 *
 * 函数: count_status_by_sequence(order_id STRING, order_status INT, all_order_status ARRAY<INT>)
 * 返回: MAP<INT, BIGINT> 状态 -> 订单数
 *
 * key = hash(status + "@" + orderId)
 */
public class BitmapStatusCountUDAF extends AggregateFunction<Map<Integer, Long>, BitmapStatusCountAccumulator> {

    @Override
    public BitmapStatusCountAccumulator createAccumulator() {
        return new BitmapStatusCountAccumulator();
    }

    @Override
    public Map<Integer, Long> getValue(BitmapStatusCountAccumulator acc) {
        Map<Integer, Long> result = new HashMap<>();
        if (acc.allOrderStatus == null) return result;

        for (int i = 0; i < acc.allOrderStatus.length; i++) {
            if (acc.statusCounts[i] > 0) {
                result.put(acc.allOrderStatus[i], acc.statusCounts[i]);
            }
        }
        return result;
    }

    // 64 bitmap 必须修改为long 实现
    private int hash(String s) {
        return Math.abs(s.hashCode());
    }

    public void accumulate(BitmapStatusCountAccumulator acc, String orderId, Integer orderStatus, int[] allOrderStatus) {
        if (orderId == null || orderStatus == null || allOrderStatus == null) return;

        // 假如订单状态要变自己每一次更新这里逻辑
        if (acc.allOrderStatus == null) {
            acc.allOrderStatus = allOrderStatus;
            acc.statusCounts = new long[allOrderStatus.length];
        }
        // 找新状态索引
        int newIdx = -1;
        for (int i = 0; i < allOrderStatus.length; i++) {
            if (allOrderStatus[i] == orderStatus) {
                newIdx = i;
                break;
            }
        }
        if (newIdx == -1) return;


        // 从尾部遍历找当前状态，只需要按照 allOrderStatus 从尾巴到头开始找
        // 可能的问题：存在hash 冲突，可以通过更大的并行度活着 64 bitmap 解决
        for (int i = allOrderStatus.length - 1; i >= 0; i--) {
            //  改用类雪花 10000000000001 + orderId 这种更好，因为 orderId 天然有序且局部分布稠密，适合RoaringBitmap
            int key = hash(allOrderStatus[i] + "@" + orderId);
            if (acc.bitmap.contains(key)) {
                // 迟到的数据
                if (newIdx <= i) {
                    return;
                }
                // 数据是最新的，删除老的
                acc.bitmap.remove(key);
                acc.statusCounts[i]--;
                break;
            }
        }

        // 把最新状态写回去，包括第一次插入/之前插入过一个老版本的
        acc.bitmap.add(hash(orderStatus + "@" + orderId));
        acc.statusCounts[newIdx]++;
    }

    public void merge(BitmapStatusCountAccumulator acc, Iterable<BitmapStatusCountAccumulator> it) {
        for (BitmapStatusCountAccumulator other : it) {
            if (other == null || other.allOrderStatus == null) continue;

            if (acc.allOrderStatus == null) {
                acc.allOrderStatus = other.allOrderStatus;
                acc.statusCounts = new long[other.allOrderStatus.length];
            }

            // 合并计数和bitmap，处理冲突时保留更新的状态
            // 简化：直接合并bitmap，重新计算计数
            for (int i = 0; i < other.allOrderStatus.length; i++) {
                // 遍历other bitmap中该状态的所有key并尝试添加
                // 由于无法反向遍历，这里采用简化策略：直接or合并
            }
            acc.bitmap.or(other.bitmap);
            for (int i = 0; i < acc.statusCounts.length; i++) {
                acc.statusCounts[i] += other.statusCounts[i];
            }
        }
    }

    public void resetAccumulator(BitmapStatusCountAccumulator acc) {
        acc.bitmap.clear();
        acc.allOrderStatus = null;
        acc.statusCounts = null;
    }
}
