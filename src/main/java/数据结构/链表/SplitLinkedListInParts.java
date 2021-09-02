package 数据结构.链表;

public class SplitLinkedListInParts {
    /**
     * 按照k拆分链表
     * https://leetcode-cn.com/problems/split-linked-list-in-parts
     *
     * @param head
     * @param k
     * @return
     */
    public ListNode[] splitListToParts(ListNode head, int k) {
        int length = 0;
        ListNode temp = head;
        while (temp != null) {
            length += 1;
            temp = temp.next;
        }
        int perSize = (length / k) + 1;
        int step = length % k;
        ListNode[] returnSource = new ListNode[k];
        for (int a = 0; a < step; a++) {
            temp = head;
            for (int b = 0; b < perSize - 1; b++) head = head.next;
            ListNode pre = head;
            head = head.next;
            pre.next = null;
            returnSource[a] = temp;
        }
        for (int a = step; a < Math.min(k, length); a++) {
            temp = head;
            for (int b = 0; b < perSize - 2; b++) head = head.next;
            ListNode pre = head;
            head = head.next;
            pre.next = null;
            returnSource[a] = temp;
        }
        return returnSource;

    }
}
