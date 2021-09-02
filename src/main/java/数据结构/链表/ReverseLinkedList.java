package 数据结构.链表;

public class ReverseLinkedList {
    /**
     * 链表翻转
     * https://leetcode-cn.com/problems/reverse-linked-list/
     *
     * @param head
     * @return
     */
    public ListNode reverseList(ListNode head) {
        if (head == null) return null;
        ListNode tail = head.next;
        head.next = null;
        while (tail != null) {
            ListNode temp = tail;
            tail = tail.next;
            temp.next = head;
            head = temp;
        }
        return head;

    }

}
