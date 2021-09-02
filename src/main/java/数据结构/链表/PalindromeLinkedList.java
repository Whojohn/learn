package 数据结构.链表;

import java.util.ArrayList;
import java.util.List;

public class PalindromeLinkedList {
    /**
     * 判定链表是否存在回文
     * https://leetcode-cn.com/problems/palindrome-linked-list/
     *
     * @param head
     * @return
     */
    public boolean isPalindrome(ListNode head) {
        List<ListNode> source = new ArrayList<>();
        for (ListNode cur = head; cur != null; cur = cur.next) source.add(cur);

        for (int a = 0; a < source.size() / 2; a++) {
            if (source.get(a).val != source.get(source.size() - 1 - a).val) return false;
        }
        return true;
    }
}
