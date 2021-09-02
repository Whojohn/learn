package 数据结构.链表;

public class OddEvenLinkedList {
    /**
     * 下标为奇数、偶数字段都放一起，重新排列链表
     * https://leetcode-cn.com/problems/odd-even-linked-list
     *
     * @param head
     * @return
     */
    public ListNode oddEvenList(ListNode head) {
        ListNode insertStep = head;
        int count = 0;
        ListNode loc = head;
        while (loc != null) {
            count += 1;
            if (count % 2 == 0) {
                ListNode temp = loc.next;
                if (loc.next == null) break;
                loc.next = loc.next.next;
                temp.next = insertStep.next;
                insertStep.next = temp;
                insertStep = insertStep.next;
            } else {
                loc = loc.next;
            }
        }
        return head;

    }
}
