# 数据结构

2021.08.02

reference:

1. https://dunwu.github.io/algorithm-tutorial/#/README?id=%f0%9f%93%9d-%e7%9f%a5%e8%af%86%e7%82%b9
2. http://data.biancheng.net/view/154.html
3. 算法导论（部分经典算法）
4. Java or Python 源码 (比如链表，树)
5. https://python-data-structures-and-algorithms.readthedocs.io/zh/latest/14_%E6%A0%91%E4%B8%8E%E4%BA%8C%E5%8F%89%E6%A0%91/tree/#_5

![img](https://raw.githubusercontent.com/dunwu/images/dev/snap/20200605164316.png)



## 1. 线性表

> 同类元素组成，按顺序排列的线程结构。

### 1.1 数组(顺序表)

- python

  ```
  temp = list()
  temp = []
  ```

- Java

  ```
  import java.util.List;
  
  // 定长数组
  String[] temp = new String[]{"1", "2"};
  
  // 变长数组
  List<String> temp = new ArrayList();
  ```

  

### 1.2 链表

- python（python 原生中只有 deque， deque 底层是双向链表）

```
from collections import deque
deque()
```

- java (双向链表 )

```
import java.util.LinkedList;
import java.util.List;

List<String> temp = new LinkedList<String>();
```

- python 实现

```
# -*- encoding: utf-8 -*-
"""
@File    :   linkedlist.py    

@Modify Time      @Author    @Version    @Desciption
------------      -------    --------    -----------
2021/8/2 15:31   lzy      1.0         None
"""


class Node(object):
    def __init__(self, value):
        self.pre = None
        self.next = None
        self.value = value

    def set_next(self, next_node):
        self.next = next_node

    def set_pre(self, pre_node):
        self.pre = pre_node


class LinkList(object):
    def __init__(self, value):
        temp = Node(value)
        self.head = temp
        self.tail = temp

    def insert(self, source):
        temp = Node(source)
        temp.set_pre(self.tail)
        self.tail.set_next(temp)
        self.tail = temp

    def read_from_head(self):
        source_list = []
        step = self.head
        while step is not None:
            source_list.append(step.value)
            step = step.next
        return source_list

    def read_from_tail(self):
        source_list = []
        step = self.tail
        while step is not None:
            source_list.append(step.value)
            step = step.pre
        return source_list


if __name__ == "__main__":
    source = LinkList(3)
    source.insert(2)
    source.insert(1)
    print(source.read_from_head())
    print(source.read_from_tail())

```

- java 实现

```
import java.util.ArrayList;
import java.util.List;

class Node<T> {
    Node pre;
    Node next;
    T value;

    Node(T source) {
        this.value = source;
    }
}

public class LinkList<T> {
    Node head;
    Node tail;

    public LinkList(T source) {
        Node<T> temp = new Node<T>(source);
        this.head = temp;
        this.tail = temp;
    }

    public void insert(T source) {
        Node<T> temp = new Node<T>(source);
        this.tail.next = temp;
        temp.pre = this.tail;
        this.tail = temp;
    }

    public List<T> getFromHead() {
        List<T> sourceList = new ArrayList<T>();
        Node temp;

        temp = this.head;

        while (temp != null) {
            sourceList.add((T) temp.value);
            temp = temp.next;
        }
        return sourceList;
    }


    public List<T> getFromTail() {
        List<T> sourceList = new ArrayList<T>();
        Node temp;

        temp = this.tail;

        while (temp != null) {
            sourceList.add((T) temp.value);
            temp = temp.pre;
        }
        return sourceList;
    }


    public static void main(String[] args) {
        LinkList<Integer> temp = new LinkList<Integer>(1);
        temp.insert(2);
        temp.insert(3);
        temp.getFromHead().forEach(System.out::println);
        temp.getFromTail().forEach(System.out::println);
    }
}

```

### 1.3 栈

> 栈是先进后出的一种单向操作数组，可以用链表和数组实现。

### 1.4 队列

> 队列是一种先进先出的顺序表，可以用链表和数组实现。

## 2. 散列表(Hash表，线性表的应用)

### 2.1 Java 散列表

- Map 容器 中的散列表实现如下

1. HashMap

2. HashTable

3. SortedMap

4. LinkedHashMap

   ！！！注意 TreeMap 是基于树的实现 ！！！

#### 2.2 Python 散列表

1. dict

2. collections.OrderedDict


### 3. 树
- [ 数据结构-树 ](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%91/SymmetricTree.java)





