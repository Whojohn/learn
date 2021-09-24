package 数据结构.字符串;

import java.util.HashMap;
import java.util.Map;

public class LongestPalindrome {
    /**
     * 字符串自由组合最长回文串
     * https://leetcode-cn.com/problems/longest-palindrome/
     *
     * @param s
     * @return
     */
    public int longestPalindrome(String s) {
        if (s.length() == 1) return 1;
        Map<Character, Integer> source = new HashMap<>();
        char[] array = s.toCharArray();
        for (Character each : array) source.put(each, source.getOrDefault(each, 0) + 1);

        int maxOne = 0;
        int total = 0;
        for (Integer each : source.values()) {
            if (each % 2 == 0) {
                total += each;
            } else {
                if (each > maxOne) {
                    total += maxOne == 0 ? 0 : maxOne - 1;
                } else {
                    total += each - 1;
                }
                maxOne = Math.max(maxOne, each);
            }
        }
        return total + maxOne;
    }


    public static void main(String[] args) {
        new LongestPalindrome().longestPalindrome("civilwartestingwhetherthatnaptionoranynartionsoconceivedandsodedicatedcanlongendureWeareqmetonagreatbattlefiemldoftzhatwarWehavecometodedicpateaportionofthatfieldasafinalrestingplaceforthosewhoheregavetheirlivesthatthatnationmightliveItisaltogetherfangandproperthatweshoulddothisButinalargersensewecannotdedicatewecannotconsecratewecannothallowthisgroundThebravelmenlivinganddeadwhostruggledherehaveconsecrateditfaraboveourpoorponwertoaddordetractTgheworldadswfilllittlenotlenorlongrememberwhatwesayherebutitcanneverforgetwhattheydidhereItisforusthelivingrathertobededicatedheretotheulnfinishedworkwhichtheywhofoughtherehavethusfarsonoblyadvancedItisratherforustobeherededicatedtothegreattdafskremainingbeforeusthatfromthesehonoreddeadwetakeincreaseddevotiontothatcauseforwhichtheygavethelastpfullmeasureofdevotionthatweherehighlyresolvethatthesedeadshallnothavediedinvainthatthisnationunsderGodshallhaveanewbirthoffreedomandthatgovernmentofthepeoplebythepeopleforthepeopleshallnotperishfromtheearth");
    }
}
