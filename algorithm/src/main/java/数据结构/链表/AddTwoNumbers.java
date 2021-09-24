package 数据结构.链表;

public class AddTwoNumbers {
    /**
     * 两数以链表且逆序表达，相加两数
     * https://leetcode-cn.com/problems/add-two-numbers/
     *
     * @param l1
     * @param l2
     * @return
     */
    public ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        ListNode head = new ListNode(-1);
        ListNode temp = head;
        int pre = 0;
        while (l1 != null || l2 != null) {
            if (l1 == null && l2 == null) break;
            int now = ((l1 == null ? 0 : l1.val) + (l2 == null ? 0 : l2.val) + pre);
            pre = now / 10;
            temp.next = new ListNode(now % 10);
            temp = temp.next;
            l1 = l1 == null ? null : l1.next;
            l2 = l2 == null ? null : l2.next;
        }
        if (pre != 0) temp.next = new ListNode(pre);
        return head.next;
    }

    public static void main(String[] args) {
        new AddTwoNumbers().addTwoNumbers(
                ListNode.buildListNode(new int[]{1, 2, 4, 3}),
                ListNode.buildListNode(new int[]{5, 6, 4})
        );
    }
}
