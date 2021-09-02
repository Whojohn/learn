package 数据结构.链表;

/**
 * leetcode 链表实现
 */
public class ListNode {
    int val;
    ListNode next;

    ListNode(int x) {
        val = x;
        next = null;
    }

    public static ListNode buildListNode(int[] source) {
        ListNode node = new ListNode(source[0]);
        ListNode temp = node;
        for (int a = 1; a < source.length; a++) {
            temp.next = new ListNode(source[a]);
            temp = temp.next;
        }
        return node;
    }
}