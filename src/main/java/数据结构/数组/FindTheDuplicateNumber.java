package 数据结构.数组;

public class FindTheDuplicateNumber {
    /**
     * 寻找数组中重复的数字(不能使用额外空间)
     * https://leetcode-cn.com/problems/find-the-duplicate-number/submissions/
     * <p>
     * 特殊解法：
     * 1. n 必须 在 1到n 中，利用从二分查找,找到目标k.
     * 2. 每一个 n+1/2 = mid 必须符合以下条件， <= mid 的值要么 = mid 要么 = mid +1
     * 3. 说明1到mid 中存在重复，否则则是 mid+1 到n 存在重复
     * <p>
     * https://leetcode-cn.com/problems/kth-smallest-element-in-a-sorted-matrix/solution/378java-er-fen-fa-tu-jie-you-xian-dui-lie-liang-ch/
     *
     * @param nums
     * @return
     */
    public int findDuplicate(int[] nums) {
        int len = nums.length;
        int left = 1;
        int right = nums.length;
        while (left < right) {
            int mid = left + (right - left) / 2;
            int cnt = this.getMin(nums, mid);
            if (cnt > mid) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }
        return left;

    }

    private int getMin(int[] nums, int target) {
        int total = 0;
        for (int each : nums) {
            if (each <= target) total += 1;
            if (total >= target + 1) return total;
        }
        return total;
    }
}
