package 数据结构.链表;

import java.util.ArrayDeque;
import java.util.Deque;

public class AddTwoNumbersIi {
    /**
     * 两数以链表的方式进行存储，将两数相加
     * https://leetcode-cn.com/problems/add-two-numbers-ii
     * 用堆栈进行存储每一位数字
     *
     * @param l1 数1链表
     * @param l2 数2链表
     * @return 两者和的链表表示
     */
    public ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        Deque<Integer> templ1 = new ArrayDeque<>();
        while (l1 != null) {
            templ1.push(l1.val);
            l1 = l1.next;
        }

        Deque<Integer> templ2 = new ArrayDeque<>();
        while (l2 != null) {
            templ2.push(l2.val);
            l2 = l2.next;
        }

        int pre = 0;
        Deque<Integer> finalNum = new ArrayDeque<>();
        while (true) {
            Integer a = templ1.poll();
            Integer b = templ2.poll();
            if (a == null && b == null && pre == 0) {
                break;
            }
            int temp = (a == null ? 0 : a) + (b == null ? 0 : b) + pre;
            finalNum.push(temp % 10);
            pre = temp / 10;
        }

        ListNode returnSource = new ListNode(finalNum.poll());
        ListNode tempList = returnSource;
        while (true) {
            Integer temp = finalNum.poll();
            if (temp == null) break;
            tempList.next = new ListNode(temp);
            tempList = tempList.next;
        }
        return returnSource;

    }
}
