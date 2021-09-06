package 数据结构.字符串;

public class ShortestPalindrome {
    /**
     * 字符串头添加最少的字符以实现回文
     * https://leetcode-cn.com/problems/shortest-palindrome/
     *
     * @param s
     * @return
     */
    public String shortestPalindrome(String s) {
        String vers = new StringBuilder(s).reverse().toString();
        int loc = 0;
        while (loc < s.length()) {
            if (vers.charAt(loc) == s.charAt(0)) {
                int temp = 0;

//                while (loc + temp < s.length()) {
//                    if (vers.charAt(loc + temp) != s.charAt(temp)) break;
//                    temp += 1;
//                }
//                if (loc + temp == s.length()) {
//                    return vers.substring(0,loc)+s;
//                }

                if (s.substring(0, s.length() - loc).equals(vers.substring(loc, s.length()))) {
                    return vers.substring(0, loc) + s;
                }
            }
            loc += 1;
        }
        return s;
    }

    public static void main(String[] args) {
        System.out.println(new ShortestPalindrome().shortestPalindrome("abcd"));
    }

}
