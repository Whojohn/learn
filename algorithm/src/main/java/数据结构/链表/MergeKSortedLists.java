package 数据结构.链表;

public class MergeKSortedLists {
    /**
     * 多链表合并
     * https://leetcode-cn.com/problems/merge-k-sorted-lists
     *
     * @param lists
     * @return
     */
    public ListNode mergeKLists(ListNode[] lists) {
        if (lists.length == 1) return lists[0];
        ListNode first = lists[0];
        for (int a = 1; a < lists.length; a++) {
            first = this.mergeTwo(first, lists[a]);
        }
        return first;

    }

    private ListNode mergeTwo(ListNode first, ListNode two) {
        ListNode head = new ListNode(-1);
        ListNode temp = head;
        while (first != null && two != null) {
            if (first.val > two.val) {
                temp.next = two;
                two = two.next;
            } else {
                temp.next = first;
                first = first.next;
            }
            temp = temp.next;
        }
        if (first == null) {
            temp.next = two;
        } else {
            temp.next = first;
        }
        return head.next;

    }

    public static void main(String[] args) {
        new MergeKSortedLists().mergeKLists(new ListNode[]{
                ListNode.buildListNode(new int[]{1, 4, 5}),
                ListNode.buildListNode(new int[]{1, 3, 4}),

        });
    }
}
