package 数据结构.字符串;

public class RepeatedStringMatch {
    /**
     * a 串多次复制自身是否能组成 b
     * https://leetcode-cn.com/problems/repeated-string-match
     *
     * @param a
     * @param b
     * @return
     */
    public int repeatedStringMatch(String a, String b) {
        if (a.contains(b)) return 1;

        int min = b.length() / a.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= min + 2; i++) {
            sb.append(a);
            // 通过判定长度，减少翻转和判定次数，提高性能
            if (i >= min && sb.toString().contains(b)) return i;
        }
        return -1;
    }


}
