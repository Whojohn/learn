package 数据结构.数组;

public class MaxChunksToMakeSorted {
    /**
     * 最多能完成排序的块
     * https://leetcode-cn.com/problems/max-chunks-to-make-sorted/
     * <p>
     * 解题思路：
     * 1. 利用可排序的数组内最大的数值必须是k，k是当前位置进行判定。
     *
     * @param arr
     * @return
     */
    public int maxChunksToSorted(int[] arr) {
        int total = 0, preMax = 0;
        for (int i = 0; i < arr.length; ++i) {
            preMax = Math.max(preMax, arr[i]);
            if (preMax == i) total++;
        }
        return total;
    }
}
