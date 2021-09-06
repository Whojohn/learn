package 数据结构.字符串;

public class ImplementStrstr {
    /**
     * 字符串匹配首次出现位置
     * https://leetcode-cn.com/problems/implement-strstr/
     * <p>
     * ！！！注意！！！
     * indexOf 匹配的目标是从 "abc" 找 ""， 不是返回 -1 ，应该返回0;
     *
     * @param haystack
     * @param needle
     * @return
     */
    public int strStr(String haystack, String needle) {
        if (needle.length() == 0) return 0;
        char[] hayArray = haystack.toCharArray();
        char[] neeArray = needle.toCharArray();
        int loc = 0;
        while (loc < haystack.length()) {
            if (hayArray[loc] == neeArray[0]) {
                // 防止越界
                if (needle.length() - loc < 0) return -1;
                int label = 0;
                while (label < needle.length() && hayArray[loc + label] == neeArray[label]) {
                    label += 1;
                }
                if (label == needle.length()) return loc;
            }
            loc += 1;
        }
        return -1;

    }
}
