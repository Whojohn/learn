# 数据结构-栈队列
## 1. 栈&队列的基本概念

**栈**: 栈是先进后出的一种单向操作数组，可以用链表和数组实现。
**队列**: 队列是一种先进先出的顺序表，可以用链表和数组实现。

## 2. 栈&队列算法& 总结

### 2.1 栈&队列的实现
#### 2.1.1 java 栈实现
```
Deque<Integer> temp = new ArrayDeque<>();
// deque 作为栈时候，放入元素必须使用 push，add 放入元素会导致先进后出失败
temp.push(1);
temp.push(2);
// 栈顶
System.out.println(temp.peek());
System.out.println(temp.poll());
System.out.println(temp.poll());
// poll 栈中没有元素会返回 null ,pop 会引发异常
System.out.println(temp.poll());
// 栈为空, pop 引发异常
System.out.println(temp.pop());
```
#### 2.1.2 java 队列实现
```
Deque<Integer> temp = new ArrayDeque<>();
temp.add(1);
temp.addFirst(2);
temp.addLast(3);
// 等同 addLast
temp.add(4);
// 2 1 3 4 队列中元素排列顺序

// 打印 队列头,等于 peekfirst
System.out.println(temp.peek()); // 2
// 打印 队列尾巴
System.out.println(temp.peekLast());
// 队列出列
System.out.println(temp.pollFirst());
```

### 2.2 算法
#### 2.2.1 栈模拟队列
- [java](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%88%E9%98%9F%E5%88%97/ImplementQueueUsingStacks.java)

#### 2.2.2 用队列实现栈
- [java](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%88%E9%98%9F%E5%88%97/ImplementStackUsingQueues.java)

#### 2.2.3 最小栈
> 通过维护源数据栈和当前数据位的最小栈，o(1) 获得当前栈中最小的元素

- [java](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%88%E9%98%9F%E5%88%97/MinStack.java)

#### 2.2.4  字符串合法性判定

- [java](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%88%E9%98%9F%E5%88%97/ValidParentheses.java)

#### 2.2.5 最近温度上升的日期间隔
> 通过维护数组下标栈，遍历判定历史温度。

- [java](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%88%E9%98%9F%E5%88%97/DailyTemperatures.java)

#### 2.2.6 每一个元素的下一个最大元素
> 类似 DailyTemperatures 最近温度上升的日期间隔;通过维护数组下标栈，遍历判定历史温度。

- [java](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%88%E9%98%9F%E5%88%97/NextGreaterElementIi.java)
