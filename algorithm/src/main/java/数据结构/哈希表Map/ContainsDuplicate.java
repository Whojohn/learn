package 数据结构.哈希表Map;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ContainsDuplicate {
    public boolean containsDuplicate(int[] nums) {
        Map<Integer, Integer> temp = new HashMap<>();
        for (int each : nums) temp.put(each, temp.getOrDefault(each, 0) + 1);
        for (Integer each : temp.keySet()) if (temp.get(each) > 1) return true;
        Set<Integer> ddd = new HashSet<>();
        return false;


    }
}
