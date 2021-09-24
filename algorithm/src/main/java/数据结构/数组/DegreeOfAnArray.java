package 数据结构.数组;

import java.util.HashMap;
import java.util.Map;

public class DegreeOfAnArray {
    /**
     * 数组中出现频率最高的数字的最短子数组长度
     * https://leetcode-cn.com/problems/degree-of-an-array/
     *
     * @param nums
     * @return
     */
    public int findShortestSubArray(int[] nums) {
        Map<Integer, Integer> temp = new HashMap<>(256);
        Map<Integer, Map<String, Integer>> loc = new HashMap<>(256);
        Integer maxKey = new Integer(1);

        for (int i = 0; i < nums.length; i++) {
            int each = nums[i];
            temp.put(each, temp.getOrDefault(each, 0) + 1);

            if (loc.containsKey(each)) {
                loc.get(each).put("end", i);
            } else {
                Map<String, Integer> locStartEndMap = new HashMap<String, Integer>();
                locStartEndMap.put("start", i);
                locStartEndMap.put("end", i);
                loc.put(each, locStartEndMap);
            }
            if (temp.getOrDefault(maxKey, 0) < temp.get(each)) maxKey = each;
        }

        int minGap = Integer.MAX_VALUE;
        for (Integer each : temp.keySet()) {
            if (temp.get(each).equals(temp.get(maxKey))) {
                minGap = Math.min(minGap, loc.get(each).get("end") - loc.get(each).get("start") + 1);
            }
        }

        return minGap;
    }

    public static void main(String[] args) {
        new DegreeOfAnArray().findShortestSubArray(new int[]{2, 1, 1, 2, 1, 3, 3, 3, 1, 3, 1, 3, 2});
    }
}
