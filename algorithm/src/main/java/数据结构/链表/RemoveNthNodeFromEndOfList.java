package 数据结构.链表;

public class RemoveNthNodeFromEndOfList {
    int loop = 0;

    /**
     * 删除链表后n个元素
     * https://leetcode-cn.com/problems/remove-nth-node-from-end-of-list
     *
     * @param head
     * @param n
     * @return
     */
    public ListNode removeNthFromEnd(ListNode head, int n) {
        if (head == null) return null;
        head.next = this.removeNthFromEnd(head.next, n);
        loop += 1;
        if (loop == n) return head.next;
        return head;
    }
}
