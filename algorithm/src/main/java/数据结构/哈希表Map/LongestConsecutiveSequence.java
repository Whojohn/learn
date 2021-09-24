package 数据结构.哈希表Map;

import java.util.HashSet;
import java.util.Set;

public class LongestConsecutiveSequence {
    public int longestConsecutive(int[] nums) {
        Set<Integer> numsSet = new HashSet<>();
        for (int each : nums) numsSet.add(each);
        Set<Integer> banStep = new HashSet<>();
        int total = 0;
        for (int each : numsSet) {
            if (banStep.contains(each)) continue;
            int step = 1;
            int numTemp = each;
            while (numsSet.contains(numTemp - 1)) {
                numTemp -= 1;
                banStep.add(numTemp);
                step += 1;
            }
            numTemp = each;
            while (numsSet.contains(numTemp + 1)) {
                numTemp += 1;
                banStep.add(numTemp);
                step += 1;
            }
            total = Math.max(step, total);
        }
        return total;

    }

    public static void main(String[] args) {
        new LongestConsecutiveSequence().longestConsecutive(new int[]{100, 4, 200, 1, 3, 2});
    }
}
