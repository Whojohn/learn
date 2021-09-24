package 数据结构.数组;

import java.util.HashSet;
import java.util.Set;

public class ArrayNesting {
    int maxLoopLen = 0;
    Set<Integer> loopSet = new HashSet<>(4096);

    /**
     * 数组最大循环数
     * https://leetcode-cn.com/problems/array-nesting/
     *
     * @param nums
     * @return
     */
    public int arrayNesting(int[] nums) {
        Set<Integer> loopSet = new HashSet<>();

        for (int loc = 0; loc < nums.length; loc++) {
            this.checkLoop(nums, loc);
        }
        return this.maxLoopLen;
    }

    private void checkLoop(int[] nums, int loc) {
        int loopLen = 0;
        while (true) {
            if (this.loopSet.contains(nums[loc])) {
                this.maxLoopLen = Math.max(this.maxLoopLen, loopLen);
                return;
            } else {
                loopLen += 1;
                this.loopSet.add(nums[loc]);
                loc = nums[loc];
            }
        }

    }
}
