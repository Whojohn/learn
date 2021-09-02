package 数据结构.链表;

public class IntersectionOfTwoLinkedLists {
    /**
     * 判定两链表是否存在交点
     * https://leetcode-cn.com/problems/intersection-of-two-linked-lists
     *
     * @param headA 链表a
     * @param headB 链表b
     * @return 相加的节点
     */
    public ListNode getIntersectionNode(ListNode headA, ListNode headB) {
        int lenA = getLength(headA);
        int lenB = getLength(headB);
        int lag = Math.abs(lenA - lenB);
        if (lenA > lenB) {
            for (int a = 0; a < lag; a++) headA = headA.next;
        } else {
            for (int a = 0; a < lag; a++) headB = headB.next;
        }
        while (headA != null) {
            if (headA == headB) {
                return headA;
            } else {
                headA = headA.next;
                headB = headB.next;
            }
        }
        return null;

    }

    private int getLength(ListNode headA) {
        ListNode tmp = headA;
        int total = 0;
        while (tmp != null) {
            total += 1;
            tmp = tmp.next;
        }
        return total;
    }

}
