package 数据结构.栈队列;

import java.util.ArrayDeque;
import java.util.Deque;

public class ImplementQueueUsingStacks {
    public static void main(String[] args) {
        MyQueue myQueue = new MyQueue();
        myQueue.push(1); // queue is: [1]
        myQueue.push(2); // queue is: [1, 2] (leftmost is front of the queue)
        System.out.println(myQueue.peek()); // return 1
        myQueue.pop(); // return 1, queue is [2]
        myQueue.empty(); // return false


    }
}

/**
 * 用栈实现队列
 * https://leetcode-cn.com/problems/implement-queue-using-stacks
 */
class MyQueue {
    Deque<Integer> input = new ArrayDeque<>();
    Deque<Integer> output = new ArrayDeque<>();

    /**
     * Initialize your data structure here.
     */
    public MyQueue() {

    }

    /**
     * Push element x to the back of queue.
     */
    public void push(int x) {
        input.push(x);
    }

    /**
     * Removes the element from in front of queue and returns that element.
     */
    public int pop() {
        this.statckToStack(input, output);
        int temp = this.output.poll();
        this.statckToStack(output, input);
        return temp;
    }

    /**
     * Get the front element.
     */
    public int peek() {
        this.statckToStack(input, output);
        int temp = this.output.peekFirst();
        this.statckToStack(output, input);
        return temp;
    }

    public void statckToStack(Deque d1, Deque d2) {
        int len = d1.size();
        for (int a = 0; a < len; a++) {
            d2.push(d1.pop());
        }
    }

    /**
     * Returns whether the queue is empty.
     */
    public boolean empty() {
        return input.isEmpty();
    }
}