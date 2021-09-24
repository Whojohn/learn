package 数据结构.栈队列;

import java.util.ArrayList;
import java.util.List;

/**
 * 最小栈
 * https://leetcode-cn.com/problems/min-stack/
 * 通过维护源数据栈和当前数据位的最小栈，o(1) 获得当前栈中最小的元素
 */
public class MinStack {

    List<Integer> source;
    List<Integer> min;

    /**
     * initialize your data structure here.
     */
    public MinStack() {
        source = new ArrayList();
        min = new ArrayList();
    }

    public void push(int val) {
        this.source.add(val);
        this.min.add(Math.min(min.size() > 0 ? min.get(min.size() - 1) : val, val));
    }

    public void pop() {
        int len = source.size();
        source.remove(len - 1);
        min.remove(len - 1);
    }

    public int top() {
        return source.get(source.size() - 1);
    }

    public int getMin() {
        return min.get(min.size() - 1);
    }

    public static void main(String[] args) {

        MinStack minStack = new MinStack();
        minStack.push(2);
        minStack.push(0);
        minStack.push(3);
        minStack.push(0);

        minStack.getMin();

        minStack.pop();
        minStack.getMin();
        minStack.pop();
        minStack.getMin();
        minStack.pop();
        minStack.getMin();


    }
}

