package 数据结构.数组;

public class MaxConsecutiveOnes {
    /**
     * 数组中1最大连续出现长度
     * https://leetcode-cn.com/problems/max-consecutive-ones/
     *
     * @param nums
     * @return
     */
    public int findMaxConsecutiveOnes(int[] nums) {
        int maxSeqCou = 0;
        int preCou = 0;
        for (int each : nums) {
            if (each == 0) {
                maxSeqCou = Math.max(preCou, maxSeqCou);
                preCou = 0;
            } else {
                preCou += 1;
            }
        }
        maxSeqCou = Math.max(preCou, maxSeqCou);
        return maxSeqCou;

    }
}
