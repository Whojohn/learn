package 数据结构.数组;

public class MoveZeroes {
    /**
     * 数组中0移动到末尾
     * https://leetcode-cn.com/problems/move-zeroes/solution/yi-dong-ling-by-leetcode-solution/
     *
     * @param nums
     */
    public void moveZeroes(int[] nums) {
        int left = 0;
        int right = 0;
        while (right < nums.length) {
            if (nums[right] == 0) {
                right += 1;
            } else {
                nums[left] = nums[right];
                right += 1;
                left += 1;
            }
        }
        while (left < nums.length) {
            nums[left] = 0;
            left += 1;
        }
    }
}
