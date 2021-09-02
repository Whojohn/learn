package 数据结构.链表;

public class MergeTwoSortedLists {
    /**
     * 按照大小顺序合并两个链表
     * 非递归方法
     * https://leetcode-cn.com/problems/merge-two-sorted-lists
     *
     * @param l1
     * @param l2
     * @return
     */
//    public ListNode mergeTwoLists(ListNode l1, ListNode l2) {
//        if (l1 == null) return l2;
//        if (l2 == null) return l1;
//        ListNode source;
//
//        if (l1.val < l2.val) {
//            source = new ListNode(l1.val);
//            l1 = l1.next;
//        }else {
//            source = new ListNode(l2.val);
//            l2 = l2.next;
//        }
//
//        ListNode head = source;
//
//        while (l1 != null){
//            if (l1.val < l2.val) {
//                source.next = new ListNode(l1.val);
//                source = source.next;
//                l1 = l1.next;
//            }else {
//                source.next = new ListNode(l2.val);
//                source = source.next;
//                l2 = l2.next;
//            }
//        }
//        source.next = l2;
//        return head;
//    }

    /**
     * 递归解法
     *
     * @param l1
     * @param l2
     * @return
     */
    public ListNode mergeTwoLists(ListNode l1, ListNode l2) {
        if (l1 == null) return l2;
        if (l2 == null) return l1;
        if (l1.val < l2.val) {
            l1.next = mergeTwoLists(l1.next, l2);
            return l1;
        } else {
            l2.next = mergeTwoLists(l1, l2.next);
            return l2;
        }
    }
}
