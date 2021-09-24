package 数据结构.栈队列;

import java.util.ArrayDeque;
import java.util.Deque;

public class DailyTemperatures {
    /**
     * 最近温度上升的日期间隔
     * https://leetcode-cn.com/problems/daily-temperatures/
     * 通过维护数组下标栈，遍历判定历史温度。
     *
     * @param temperatures
     * @return
     */
    public int[] dailyTemperatures(int[] temperatures) {
        Deque<Integer> preStack = new ArrayDeque<>();
        int[] returnSource = new int[temperatures.length];
        preStack.push(0);
        // 必须通过下标的方式进行对比，不然无法知道元素的位置
        for (int each = 1; each < temperatures.length; each++) {
            while (!preStack.isEmpty() && temperatures[preStack.peek()] < temperatures[each]) {
                int loc = preStack.poll();
                returnSource[loc] = each - loc;
            }
            preStack.push(each);
        }
        return returnSource;
    }

    public static void main(String[] args) {
        new DailyTemperatures().dailyTemperatures(new int[]{73, 74, 75, 71, 69, 72, 76, 73});
    }
}
