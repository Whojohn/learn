package 数据结构.字符串;

public class RotateString {
    /**
     * 旋转字符串
     * https://leetcode-cn.com/problems/rotate-string/
     *
     * @param s
     * @param goal
     * @return
     */
    public boolean rotateString(String s, String goal) {
        if (s.length() > goal.length()) return false;
        String temp = s.concat(s);
        return temp.indexOf(goal) > -1;
    }
}
