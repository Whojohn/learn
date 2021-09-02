package 数据结构.链表;

public class LinkedListCycle {
    /**
     * 链表回环判定
     * https://leetcode-cn.com/problems/linked-list-cycle
     *
     * @param head 链表头
     * @return 是否存在回环
     */
    public boolean hasCycle(ListNode head) {
        ListNode l1 = head;
        ListNode l2 = head;
        while (l2 != null && l2.next != null) {
            l1 = l1.next;
            l2 = l2.next.next;
            if (l1 == l2) return true;
        }
        return false;
    }
}
