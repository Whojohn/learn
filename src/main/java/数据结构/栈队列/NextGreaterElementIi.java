package 数据结构.栈队列;

import java.util.ArrayDeque;
import java.util.Deque;

public class NextGreaterElementIi {
    /**
     * 每一个元素的下一个最大元素(类似 DailyTemperatures )
     * https://leetcode-cn.com/problems/next-greater-element-ii/
     *
     * @param nums
     * @return
     */
    public int[] nextGreaterElements(int[] nums) {
        int[] returnSource = new int[nums.length];
        int[] temp = new int[nums.length * 2];
        for (int a = 0; a < nums.length; a++) returnSource[a] = -1;
        for (int a = 0; a < nums.length * 2; a++) temp[a] = nums[a % nums.length];
        Deque<Integer> locDeque = new ArrayDeque<>();
        locDeque.push(0);
        for (int each = 1; each < temp.length; each++) {
            while (!locDeque.isEmpty() && temp[locDeque.peek()] < temp[each]) {
                int loc = locDeque.poll();
                if (loc < nums.length) returnSource[loc] = temp[each];
            }
            locDeque.push(each);
        }

        return returnSource;
    }

    public static void main(String[] args) {
        new NextGreaterElementIi().nextGreaterElements(new int[]{1, 2, 1, 3, 4, 2, 1});
    }
}
