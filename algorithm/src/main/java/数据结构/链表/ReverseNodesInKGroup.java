package 数据结构.链表;

public class ReverseNodesInKGroup {
    /**
     * 按照 k 的大小间隙翻转链表
     * https://leetcode-cn.com/problems/reverse-nodes-in-k-group/
     *
     * @param head
     * @param k
     * @return
     */
    public ListNode reverseKGroup(ListNode head, int k) {
        if (head == null) return head;
        int locCount = 0;
        ListNode returnSource = new ListNode(-1);
        ListNode nowLoc = head;
        ListNode pre = returnSource;
        while (nowLoc != null && locCount < k) {
            ListNode nextTemp = nowLoc.next;
            nowLoc.next = returnSource.next;
            returnSource.next = nowLoc;
            nowLoc = nextTemp;
            locCount += 1;
        }
        if (nowLoc != null) {
            head.next = this.reverseKGroup(nowLoc, k);
        }

        if (locCount < k) {
            nowLoc = returnSource.next;
            returnSource = new ListNode(-1);
            while (nowLoc != null) {
                ListNode nextTemp = nowLoc.next;
                nowLoc.next = returnSource.next;
                returnSource.next = nowLoc;
                nowLoc = nextTemp;
            }
        }

        return returnSource.next;
    }

    public static void main(String[] args) {
        new ReverseNodesInKGroup().reverseKGroup(ListNode.buildListNode(new int[]{1, 2, 3, 4, 5, 6}), 3);
    }
}
