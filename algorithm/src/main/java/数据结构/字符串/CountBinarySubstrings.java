package 数据结构.字符串;

public class CountBinarySubstrings {
    /**
     * 二进制串中连续字符子串最大长度
     * https://leetcode-cn.com/problems/count-binary-substrings/
     * <p>
     * 例如：
     * 00110011
     * “0011”，“01”，“1100”，“10”，“0011” 和 “01” 都是连续字符子串。而 00110011 不是连续字符子串。
     * <p>
     * 思路
     * <p>
     * 当前连续字串的字串长度，由上一个非连续字符的长度决定。Math.min(now, preLen).
     * 如 001
     * 前向最大长度为2，现在1为1，所以当前子串长度为 1*2
     *
     * @param s
     * @return
     */
    public int countBinarySubstrings(String s) {
        int preLen = 0, curLen = 1, count = 0;
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) == s.charAt(i - 1)) {
                curLen++;
            } else {
                preLen = curLen;
                curLen = 1;
            }

            if (preLen >= curLen) {
                count++;
            }
        }
        return count;
    }
}
