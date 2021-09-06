package 数据结构.字符串;

public class StringRotationLcci {
    /**
     * 字符串b 是否能够通过a 轮转获得
     * https://leetcode-cn.com/problems/string-rotation-lcci/solution/
     *
     * @param s1
     * @param s2
     * @return
     */
    public boolean isFlipedString(String s1, String s2) {
        if (s2.equals("")) return false;
        return (s1 + s1).indexOf(s2) > -1;
    }

    public static void main(String[] args) {
        new StringRotationLcci().isFlipedString("waterbottle",
                "erbottlewat");
    }
}
