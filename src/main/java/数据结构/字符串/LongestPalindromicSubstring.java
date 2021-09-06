package 数据结构.字符串;

public class LongestPalindromicSubstring {
    /**
     * 最长回文子串
     * https://leetcode-cn.com/problems/longest-palindromic-substring
     *
     * 暴力解题如下。
     *
     * 动态规划如下：
     * 1. i，j 是字串，那么 i-1, j-1 必须为子串。并且， i == j。
     * 2. 假如 i+1 != j-1 那么必须不为子串。(通过记录下i+1,j-1 的状态，)，从短的字符到长字符过度。
     *
     * @param s
     * @return
     */
    public String longestPalindrome(String s) {
        if (s.length() == 1) return s;
        int step = s.length();
        while (step > 0) {
            int loc = 0;
            while (step + loc <= s.length()) {
                if (s.charAt(loc) == s.charAt(step + loc - 1)) {
                    int start = loc;
                    int end = step + loc - 1;
                    while (start < end) {
                        start += 1;
                        end -= 1;
                        if (s.charAt(start) != s.charAt(end)) break;
                    }
                    if (start >= end) return s.substring(loc, step + loc);

//                    String temp = s.substring(loc, step + loc);
//                    if (temp.equals(new StringBuilder(temp).reverse().toString())) return s.substring(loc, step + loc);
                }
                loc += 1;
            }
            step -= 1;
        }
        return s;
    }

    public static void main(String[] args) {
        new LongestPalindromicSubstring().longestPalindrome("babad");
    }
}
