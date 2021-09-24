# 数据结构-链表

reference:

1. https://zh.wikipedia.org/wiki/%E9%93%BE%E8%A1%A8

## 1. 链表的基本概念

**链表**：属于顺序表的一种，与数组相比单纯链表不能支持随机访问，必须顺序访问，但是链表删除，插入比数组更有效率。

**链表如何插入节点**: 

> 1. 找到插入位置的前向节点loc。
> 2. temp = loc.next;
> 3. loc.next = new Node();
> 4. loc.next.next = temp;

**链表如何删除节点:**

> 注意移除当前节点必须将指向移除，不然 `jvm` 无法回收删除节点内存。
>
> 1. 找到需要删除的节点的前向节点 loc。
> 2. temp = loc.next;
> 3. loc.next = loc.next.next;
> 4. temp = null；

**如何翻转链表**:

>// 增加一个 head 节点更加方便操作，不然需要额外处理首个节点和后续节点的逻辑
>
>tempHead = new node(-1);
>
>while(head != Null){
>
>loc = head
>
>head  = head.next
>
>loc.next = tempHead.next
>
>tempHead.next = loc
>
>}

**链表回环判定**:

> 一个走两步，一个走一步，当相遇时候，则存在回环。快的走多少步都能判定是否回环，走两步是为了更快的收敛而已。快-慢=k，以k 部进行循环判定回环。回环 m ， m*k = 判定回环需要的步数。

**多链表的交叉点判定**:

> 求 a,b 长度，长度相减，长的先走减下的长度，然后一起遍历，判定。

### 1.1 链表的实现

- python 中原生链表

> python 原生中只有 deque， deque 底层是双向链表。

- Java 中原生链表

> LinkedList

- Java 自定义链表(Leetcode 链表)

```
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
```

## 2. 链表的基本操作

### 2.1 链表的遍历

#### 2.1.1 数字以链表表达，将数字相加

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E9%93%BE%E8%A1%A8/AddTwoNumbersIi.java)

#### 2.1.2 两数以链表且逆序表达，相加两数

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E9%93%BE%E8%A1%A8/AddTwoNumbers.java)

#### 2.1.3 链表是否存在回文

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E9%93%BE%E8%A1%A8/PalindromeLinkedList.java)

### 2.2 链表的回环判定

#### 2.2.1 两链表是否存在交点

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E9%93%BE%E8%A1%A8/IntersectionOfTwoLinkedLists.java)

#### 2.2.2 链表是否存在回环

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E9%93%BE%E8%A1%A8/LinkedListCycle.java)

### 2.3 链表的插入&删除

####  2.3.1 链表按照奇偶顺序重排列

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E9%93%BE%E8%A1%A8/OddEvenLinkedList.java)

#### 2.3.2 两链表合并

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E9%93%BE%E8%A1%A8/MergeTwoSortedLists.java)

#### 2.3.4 多链表合并

> 二分法

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E9%93%BE%E8%A1%A8/MergeKSortedLists.java)

#### 2.3.5 删除链表重复节点

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E9%93%BE%E8%A1%A8/RemoveDuplicatesFromSortedList.java)

#### 2.3.6 删除链表后n个元素

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E9%93%BE%E8%A1%A8/RemoveNthNodeFromEndOfList.java)

#### 2.3.7 链表翻转

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E9%93%BE%E8%A1%A8/ReverseLinkedList.java)

#### 2.3.8 翻转链表中特定位置的节点

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E9%93%BE%E8%A1%A8/ReverseLinkedListIi.java)

#### 2.3.9 按照k拆分链表

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E9%93%BE%E8%A1%A8/SplitLinkedListInParts.java)

