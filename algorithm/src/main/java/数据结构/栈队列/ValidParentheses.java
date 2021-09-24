package 数据结构.栈队列;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class ValidParentheses {
    /**
     * 字符串合法性判定
     * https://leetcode-cn.com/problems/valid-parentheses/
     *
     * @param s
     * @return
     */
    public boolean isValid(String s) {
        Deque<Character> temp = new ArrayDeque<>();
        if (s.length() % 2 != 0) return false;
        Map<Character, Character> label = new HashMap<Character, Character>() {{
            put(')', '(');
            put(']', '[');
            put('}', '{');
        }};
        for (char each : s.toCharArray()) {
            if (!label.containsKey(each)) {
                temp.push(each);
            } else {
                if (!(label.get(each) == temp.poll())) {
                    return false;
                }
            }
        }
        return temp.isEmpty();
    }
}
