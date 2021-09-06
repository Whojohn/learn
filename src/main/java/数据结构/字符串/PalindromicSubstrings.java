package 数据结构.字符串;

public class PalindromicSubstrings {
    int total = 0;

    /**
     * 回文子串总数
     * https://leetcode-cn.com/problems/palindromic-substrings/description/
     * <p>
     * 暴力穷尽解法
     * <p>
     * 可以参照(最长回文子串) 用动态规划来做
     *
     * @param s
     * @return
     */
    public int countSubstrings(String s) {
        for (int loc = 0; loc < s.length(); loc++) {
            this.checkPal(s, loc, loc);
            this.checkPal(s, loc, loc + 1);
        }
        return this.total;
    }

    public void checkPal(String s, int start, int end) {
        while (start > -1 && end < s.length() && s.charAt(start) == s.charAt(end)) {
            start -= 1;
            end += 1;
            this.total += 1;
        }
    }
}
