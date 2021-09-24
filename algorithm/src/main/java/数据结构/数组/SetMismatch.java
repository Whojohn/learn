package 数据结构.数组;

import java.util.HashMap;
import java.util.Map;

public class SetMismatch {
    /**
     * 寻找重复数字
     * https://leetcode-cn.com/problems/set-mismatch/submissions/
     *
     * @param nums
     * @return
     */
    public int[] findErrorNums(int[] nums) {
        Map<Integer, Integer> temp = new HashMap<>(256);
        for (int each : nums) {
            temp.put(each, temp.getOrDefault(each, 0) + 1);
        }
        int dup = 0;
        int mis = 0;
        for (Integer a = 1; a <= nums.length; a++) {
            if (temp.containsKey(a)) {
                if (temp.get(a) > 1) dup = a;
            } else {
                mis = a;
            }
            if (dup != 0 & mis != 0) return new int[]{dup, mis};
        }
        return new int[]{dup, mis};
    }

    public static void main(String[] args) {
        new SetMismatch().findErrorNums(new int[]{1, 2, 2, 4});
    }
}
