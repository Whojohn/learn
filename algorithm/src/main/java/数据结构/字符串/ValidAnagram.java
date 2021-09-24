package 数据结构.字符串;

import java.util.HashMap;
import java.util.Map;

public class ValidAnagram {
    /**
     * 组成字符串的字符及数量是否相同
     * https://leetcode-cn.com/problems/valid-anagram/
     *
     * @param s
     * @param t
     * @return
     */
    public boolean isAnagram(String s, String t) {
        Map<Character, Integer> countS = new HashMap<>();
        Map<Character, Integer> countT = new HashMap<>();
        for (Character each : s.toCharArray()) countS.put(each, countS.getOrDefault(each, 0) + 1);
        for (Character each : t.toCharArray()) countT.put(each, countT.getOrDefault(each, 0) + 1);
        if (countS.keySet().size() != countT.keySet().size()) return false;
        for (Character each : countS.keySet()) {
            if (!countS.get(each).equals(countT.get(each))) {
                return false;
            }
        }
        return true;

    }

    public static void main(String[] args) {
        new ValidAnagram().isAnagram("rat"
                , "car");
    }
}
