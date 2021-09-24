package 数据结构.链表;

public class ReverseLinkedListIi {
    /**
     * 翻转链表中特定位置的节点
     * https://leetcode-cn.com/problems/reverse-linked-list-ii/
     *
     * @param head
     * @param left
     * @param right
     * @return
     */
    public ListNode reverseBetween(ListNode head, int left, int right) {
        ListNode header = new ListNode(-1);
        header.next = head;
        ListNode pre = header;
        for (int i = 0; i < left - 1; i++) {
            pre = pre.next;
        }
        ListNode cur = pre.next;
        ListNode next;
        for (int i = 0; i < right - left; i++) {
            next = cur.next;
            cur.next = next.next;
            next.next = pre.next;
            pre.next = next;
        }
        return header.next;
    }

    public static void main(String[] args) {
        new ReverseLinkedListIi().reverseBetween(
                ListNode.buildListNode(new int[]{3, 5, 1, 2, 3}),
                1, 3);
    }

}
