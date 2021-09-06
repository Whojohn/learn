package 数据结构.字符串;

import java.util.ArrayList;
import java.util.List;

public class PalindromeNumber {
    /**
     * 数字是否是回文串
     * https://leetcode-cn.com/problems/palindrome-number
     *
     * @param x
     * @return
     */
    public boolean isPalindrome(int x) {
        if (x < 0) return false;
        List<Integer> temp = new ArrayList<>();
        while (x > 0) {
            temp.add(x % 10);
            x = x / 10;
        }
        int start = temp.size() % 2 == 0 ? temp.size() / 2 - 1 : temp.size() / 2 - 1;
        int end = temp.size() % 2 == 0 ? temp.size() / 2 : temp.size() / 2 + 1;
        while (start > -1 && end < temp.size() && temp.get(start).equals(temp.get(end))) {
            start -= 1;
            end += 1;
        }
        return start == -1;
    }

    public static void main(String[] args) {
        new PalindromeNumber().isPalindrome(121);
    }

}
