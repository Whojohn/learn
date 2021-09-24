package 数据结构.链表;

public class RemoveDuplicatesFromSortedList {
    /**
     * 删除链表重复节点
     * https://leetcode-cn.com/problems/remove-duplicates-from-sorted-list/
     *
     * @param head
     * @return
     */
    public ListNode deleteDuplicates(ListNode head) {
        if (head == null) return head;
        ListNode source = head;
        while (source.next != null) {
            if (source.next.val == source.val) {
                source.next = source.next.next;
            } else {
                source = source.next;
            }
        }
        return head;
    }
}
