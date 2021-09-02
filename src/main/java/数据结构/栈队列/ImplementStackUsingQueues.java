package 数据结构.栈队列;

import java.util.ArrayList;
import java.util.List;

public class ImplementStackUsingQueues {
}

/**
 * 用队列实现栈
 * https://leetcode-cn.com/problems/implement-stack-using-queues/
 *
 */
class MyStack {

    List<Integer> source;

    /**
     * Initialize your data structure here.
     */
    public MyStack() {
        source = new ArrayList<>();
    }

    /**
     * Push element x onto stack.
     */
    public void push(int x) {
        source.add(x);
    }

    /**
     * Removes the element on top of the stack and returns that element.
     */
    public int pop() {
        int len = source.size();
        return source.remove(len - 1);
    }

    /**
     * Get the top element.
     */
    public int top() {
        return source.get(source.size() - 1);
    }

    /**
     * Returns whether the stack is empty.
     */
    public boolean empty() {
        return source.size() == 0;
    }
}
