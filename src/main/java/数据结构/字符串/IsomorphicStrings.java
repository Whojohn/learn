package 数据结构.字符串;

import java.util.HashMap;
import java.util.Map;

public class IsomorphicStrings {
    /**
     * 字符串是否同构(两字符串间字符映射是否唯一)
     * https://leetcode-cn.com/problems/isomorphic-strings/
     *
     *
     * @param s
     * @param t
     * @return
     */
    public boolean isIsomorphic(String s, String t) {
        if (s.length() != t.length()) return false;
        Map<Character, Character> sTot = new HashMap<>();
        Map<Character, Character> tTos = new HashMap<>();
        char[] chars = s.toCharArray();
        char[] chart = t.toCharArray();
        for (int loc = 0; loc < s.length(); loc++) {
            Character locCharS = s.charAt(loc);
            Character locCharT = t.charAt(loc);
            if ((sTot.containsKey(locCharS) && !tTos.containsKey(locCharT)) || (!sTot.containsKey(locCharS) && tTos.containsKey(locCharT))) {
                return false;
            } else if (sTot.containsKey(locCharS) && tTos.containsKey(locCharT)) {
                if (!sTot.get(locCharS).equals(locCharT) || !tTos.get(locCharT).equals(locCharS)) {
                    return false;
                }
            } else {
                sTot.put(locCharS, locCharT);
                tTos.put(locCharT, locCharS);
            }

        }
        return true;
    }

    public static void main(String[] args) {
        new IsomorphicStrings().isIsomorphic("egg", "add");
    }
}
