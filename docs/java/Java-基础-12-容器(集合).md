# Java-基础-12-容器(集合)

## 1. 容器

Java 中容器有：

1. 数组：[]。

2. Collection 和 Map 接口实现的容器。

   > Collection 接口下又分为三大类:
   >
   > 1. List：顺序数组
   > 2. Set：不含有重复数据的集合
   > 3. Queue：有特定顺序的队列，如：先进先出、双端队列(头尾可插入队列)、优先级队列。
   >
   > Map 接口具体实现类有：
   >
   > 1. HashMap: 
   >
   > 2. ConcurrentMap (线程安全的 Map)
   >
   > 3. LinkedHashMap （保持插入顺序，链表作为底层，适用于LRU）
   >
   > 4. HashTable (线程安全k,v 容器，现在基本弃用)
   >
   > 5. TreeMap (按一定的排序规则保存数据)
   >
   >    ...

两种容器区别：

1. 数组定长，数组可以是基础数据类型和引用数据类型。
2. 集合变长，集合只能存放引用数据类型。要存放基础数据类型，必须是存放包装类。

## 1.2 容器所用技术

Java 容器部分依赖了以下这些技术：

1. 泛型

2. Lambda 表达式

3. fail-fast( `java.util.ConcurrentModificationException`异常  )

- 当迭代容器；多线程操作容器时候，只要容器结构发生变化，引发该异常。

```
# 迭代修改变量，引发错误
        List<Integer> temp = new ArrayList<Integer>() {{
            add(1);
            add(2);
            add(3);
        }};
        temp.forEach(a -> temp.add(a));
        
Exception in thread "main" java.util.ConcurrentModificationException
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1543)
	at LambdaTest.main(LambdaTest.java:15)
```
- 假如需要迭代时候修改变量如何安全实现呢？
```
# 使用 iter 对象进行删除
List<String> list = new ArrayList<>();
list.add("1");
list.add("2");
list.add("3");
Iterator temp = list.listIterator();
while (temp.hasNext()) {
    if (temp.next() == "2") temp.remove();
}
```

- fail-fast 底层原理
> 容器中有个`modCount`变量，每一个修改或者是增加后假如`modCount`与预期逻辑不对(如：并发插入，正常来说`modcount`!=预期长度；遍历时候`modcount`大小改变了)


4. 常用接口：

- Cloneable  

      所有类父类都是Object，虽然Object 实现了Cloneable  方法 ， **但是Object类将Cloneable  的 Clone 方法声明为 protect, 导致非子类无法直接调用父类方法，要实现Clone 方法，必须重新实现Cloneable  接口**。容器中Clone 方法为浅拷贝。需要深拷贝需要重写该方法。

```
        ArrayList<List<Integer>> temp = new ArrayList<List<Integer>>() {{
            add(new ArrayList<Integer>() {{
                add(1);
            }});
            add(new ArrayList<Integer>() {{
                add(2);
            }});
            add(new ArrayList<Integer>() {{
                add(3);
            }});
        }};
        List<List<Integer>> temp2 = (List<List<Integer>>) temp.clone();
        System.out.println("Copy source must be as follow when copy is deepcopy");
        temp.forEach(System.out::println);
        temp.get(1).add(3);
        System.out.println("Copy source:");
        temp2.forEach(System.out::println);
        
Copy source must be as follow when copy is deepcopy
[1]
[2]
[3]
Copy source:
[1]
[2, 3]
[3]

```

- Comparable 

> 容器对比方法。

- Iterable

> 容器迭代方法

## 2. 集合-Collection & Map

- 集合多线程安全：

  默认Collection，Map接口实现都不是线程安全。推荐使用JUC实现类保证线程安全，如：

  1. 写时复制集合：CopyOnWriteArrayList、CopyOnWriteArraySet  ；
  2. 比对交换集合(CAS): ConcurrentLinkedQueue、ConcurrentSkipListMap
  3. 对象锁(ReentrantLock)：LinkedBlockingQueue，ConcurrentHashMap ，BlockingQueue。

不推荐使用Collections.synchronizedXXX 修饰对应的集合保证数据安全，因为性能低下。

- 集合顺序

### 2.1 List

- List 实现类：

  1. ArrayList : 存储底层是 Object []
  2. LinkedList: 存储底层是双向链表。
  3. Vector: 存底层是Oject[]，基本弃用该实现类。

- `Arrays.asList()` **慎用**

  `Arrays.asList()`  方法把数组转换为 List。创建的 List 对象是Array`**特有的List接口实现**`会导致无法使用集合的方法如：**`add()`、`remove()`、`clear()`**；要创建真正的List ，需要使用Stream配合Collectors.toList()方法。

```
        Integer[] arr = { 1, 2, 3 };
        List list = Arrays.asList(arr);
        list.add(3);
        list.remove(2);

Exception in thread "main" java.lang.UnsupportedOperationException
	at java.base/java.util.AbstractList.add(AbstractList.jav:153)
	at java.base/java.util.AbstractList.add(AbstractList.java:111)
	at LambdaTest.main(LambdaTest.java:14)
```



#### 2.1.1 ArrayList

reference:

1. https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/collection/ArrayList%E6%BA%90%E7%A0%81+%E6%89%A9%E5%AE%B9%E6%9C%BA%E5%88%B6%E5%88%86%E6%9E%90.md

       底层基于 Ojbect[] 实现，内部通过下标、扩容(Array.copy)的方式实现了数组动态增长。`ArrayList`继承于 **`AbstractList`** ，实现了 **`List`**, **`RandomAccess`**, **`Cloneable`**, **`java.io.Serializable`** 这些接口。实现了`java.io.Serializable`接口是为了底层利用 Object[] 存放，不能把整个 Object[] 序列化，只能序列化有数据的部分。

-  核心知识点：

1. 初始化：
   - 默认大小： 10 、用户定义、传入的Collection 对象的大小、无参实例化时大小为0；

> 注意无参实例化 ArrayList时候，new  ArrayList() ，内部初始化为空的 []；

2. 扩容

   - 插入扩容判定流程：

     1. 取默认容量10，最小需要空间(`List 已用 size+1`) 最大值作为当前需要的空间。
     2. 判定当前数组`空间总大小`是否满足所需空间。假如不满足，进入扩容：第三步，否则插入数据。
     3. 当前总空间大小*1.5倍作为基本扩容大小。假如 1.5 倍不满足所需空间，选所需空间大小。最终对比扩容大小和 Integer.MAX_VALUE， 保证数组大小不超过Integer.MAX_VALUE。
     4. 调用 Array.copy 方法扩容。

     ```
       
       /**
          * 扩容代码
          * 要分配的最大数组大小
          */
         private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
     
         /**
          * ArrayList扩容的核心方法。
          */
         private void grow(int minCapacity) {
             // oldCapacity为旧容量，newCapacity为新容量
             int oldCapacity = elementData.length;
             //将oldCapacity 右移一位，其效果相当于oldCapacity /2，
             //我们知道位运算的速度远远快于整除运算，整句运算式的结果就是将新容量更新为旧容量的1.5倍，
             int newCapacity = oldCapacity + (oldCapacity >> 1);
             //然后检查新容量是否大于最小需要容量，若还是小于最小需要容量，那么就把最小需要容量当作数组的新容量，
             if (newCapacity - minCapacity < 0)
                 newCapacity = minCapacity;
            // 如果新容量大于 MAX_ARRAY_SIZE,进入(执行) `hugeCapacity()` 方法来比较 minCapacity 和 MAX_ARRAY_SIZE，
            //如果minCapacity大于最大容量，则新容量则为`Integer.MAX_VALUE`，否则，新容量大小则为 MAX_ARRAY_SIZE 即为 `Integer.MAX_VALUE - 8`。
             if (newCapacity - MAX_ARRAY_SIZE > 0)
                 newCapacity = hugeCapacity(minCapacity);
             // minCapacity is usually close to size, so this is a win:
             elementData = Arrays.copyOf(elementData, newCapacity);
         }
     ```

   - System.arraycopy() 和 Arrays.copyOf()方法

         Arrays.copyOf() 最终调用的是System.arraycopy() 方法。

   - **优化点**

     1. 大容量的List，使用时候可以在调用构建时候时候指定，减少扩容消耗。
     2. 对于已经存在的List，需要执行多个迭代插入可以用**`ensureCapacity`** 方法提前扩容。

#### 2.1.2 LinkedList（Queue中Deque 具体实现）

reference:

1. https://github.com/dunwu/javacore/blob/master/docs/container/java-container-list.md#3-linkedlist

2. https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/collection/LinkedList%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%90.md

   LinkedList 基于双链表实现，可以作为`队列`，`双端队列`，`栈`等数据结构；LinkedList 继承于 `AbstractSequentialList`,  实现 `List` ， `Deque` ， `Cloneable` ， `Serializable` 接口。 

- 核心知识点：

  1. 双链表

  ```
  // 链表长度
  transient int size = 0;
  // 链表头节点
  transient Node<E> first;
  // 链表尾节点
  transient Node<E> last;
  
  // 节点结构
  
  private static class Node<E> {
      E item;
      Node<E> next;
      Node<E> prev;
      ...
  }
  ```

  2. 元素添加方法
  3. 元素删除方法



### 2.2 Map

Map 主要类和接口：

- `Map` 是 Map 容器的父类，Map 是一个用于保存键值对(key-value)的接口。**Map 中不能包含重复的键；每个键最多只能映射到一个值。**
- `AbstractMap` 继承了 `Map` 的抽象类，它实现了 `Map` 中的核心 API。其它 `Map` 的实现类可以通过继承 `AbstractMap` 来减少重复编码。
- `SortedMap` 继承了 `Map` 的接口。`SortedMap` 中的内容是排序的键值对，排序的方法是通过实现比较器(`Comparator`)完成的。
- `NavigableMap` 继承了 `SortedMap` 的接口。相比于 `SortedMap`，`NavigableMap` 有一系列的“导航”方法；如"获取大于/等于某对象的键值对"、“获取小于/等于某对象的键值对”等等。
- `HashMap` 继承了 `AbstractMap`，但没实现 `NavigableMap` 接口。`HashMap` 的主要作用是储存无序的键值对，而 `Hash` 也体现了它的查找效率很高。`HashMap` 是使用最广泛的 `Map`。
- `Hashtable` 虽然没有继承 `AbstractMap`，但它继承了 `Dictionary`（`Dictionary` 也是键值对的接口），而且也实现 `Map` 接口。因此，`Hashtable` 的主要作用是储存无序的键值对。和 HashMap 相比，`Hashtable` 在它的主要方法中使用 `synchronized` 关键字修饰，来保证线程安全。但是，由于它的锁粒度太大，非常影响读写速度，所以，现代 Java 程序几乎不会使用 `Hashtable` ，如果需要保证线程安全，一般会用 `ConcurrentHashMap` 来替代。
- `TreeMap` 继承了 `AbstractMap`，且实现了 `NavigableMap` 接口。`TreeMap` 的主要作用是储存有序的键值对，排序依据根据元素类型的 `Comparator` 而定。
- `WeakHashMap` 继承了 `AbstractMap`。`WeakHashMap` 的键是**弱引用** （即 `WeakReference`），它的主要作用是当 GC 内存不足时，会自动将 `WeakHashMap` 中的 key 回收，这避免了 `WeakHashMap` 的内存空间无限膨胀。很明显，`WeakHashMap` 适用于作为缓存。

#### 2.2.1 HashMap

- 核心知识点

  1. 数据结构：Hash 表

     > 								HashMap 底层利用了 Node<K,V>[] 作为 Hash表，Hash表长度为 `2的幂次方`，Hash表下标算法决定的。Hash表下标的方式是：(n - 1) & hash，n为Hash表长度。正常的Hash表数组下标应该是`%取余数`。

  - **为什么下标计算是(n - 1) & hash？**

    正常的Hash表数组下标应该是`%取余数`。但是，取余(%)操作中如果**除数是 2 的幂次**则等价于与其除数减一的与(&)操作（也就是说 hash%length==hash&(length-1)的前提是 length 是 2 的 n 次方；）。”** 并且 **采用二进制位操作 &，相对于%能够提高运算效率，这就解释了 HashMap 的长度为什么是 2 的幂次方。**

  - 计算Key的Hash方法（hash后扰动获取Hash值）

    因为游标的计算方式是(n - 1) & hash = hash%n，当 n 较小时候，容易发生hash 表碰撞，为了减少碰撞将算出的 Hash code 再次处理。

    ```
    static final int hash(Object key) {    int h;    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);}
    ```

  2. 初始容量 & 负载因子 & 构造方法

  初始容量： 16 （这个是Hash 表的容量，Node<K,V>[]  数组长度）

  负载因子： 0.75

  构造方法：

  ```
  public HashMap(); // 默认加载因子0.75
  public HashMap(int initialCapacity); // 默认加载因子0.75；以 initialCapacity 为初始化容量，假如输入的容量不是2的幂次方，内部会转换为2的幂次方作为容量大小(严格保证Hash表是2的幂次方)。
  public HashMap(int initialCapacity, float loadFactor); // 以 initialCapacity 初始化容量；以 loadFactor 初始化加载因子
  public HashMap(Map<? extends K, ? extends V> m) // 默认加载因子0.75
  ```

  3. 插入机制

  > 1. 对 key 的 `hashCode()` 做 hash 计算，然后根据 hash 值再计算 Node 的存储位置;
  >
  > 2. 如果没有哈希碰撞，直接放到桶里；如果有哈希碰撞，以链表的形式存在桶后。
  >
  > 3. 如果哈希碰撞导致链表过长(大于等于 `TREEIFY_THRESHOLD`，数值为 8)，并且Hash 数组`长度超过64`就把链表转换成红黑树，否则链表扩容2倍，直至 Hash数组`长度超过64`才会触发红黑树转换；
  >
  > 4. 如果节点已经存在就替换旧值
  >
  > 5. 桶数量（HashMap 现有key的总数）超过容量(HashMap table)*负载因子（即 load factor * current capacity），HashMap 调用 `resize` 自动扩容一倍

  4. 获取数据机制

  > 1. 对 key 的 hashCode() 做 hash 计算，然后根据 hash 值再计算桶的 index
  >
  > 2. 如果桶中的第一个节点命中，直接返回；
  >
  > 3. 如果有冲突，则通过 `key.equals(k)` 去查找对应的 entry
  >
  > - 若为树，则在红黑树中通过 key.equals(k) 查找，O(logn)；
  > - 若为链表，则在链表中通过 key.equals(k) 查找，O(n)。

  5. 扩容机制

  > 1. 扩容后容量为 Hash 表原长度2倍。
  >
  > 2. 保持扩容2倍，使得 Hash 表中位置下标计算更加简单。下标计算方式如下：
  >
  >    如 16 容量扩容到32（下标计算方式：(n - 1) & hash， n为容量）：
  >
  >                                       n=16                         n=32
  >
  >    n-1                             0000  1111         0001  1111
  >
  >    hash1                        0000  0101         0000  0101
  >
  >    hash1 & (n-1)           0000  0101         0000  0101
  >
  > 
  >
  >    n-1                             0000  1111         0001  1111
  >
  >    hash2                        0001  0101         0001  0101
  >
  >    hash2 &(n-1)            0000   0101        0001  0101
  >
  > 总结：
  >
  > 元素的位置要么是在原位置，要么是在原位置再移动 2 次幂的位置就是新的 Hash 数组下标。这样减少了预算复杂度，还能将之前的链表冲突打散。

  #### 2.2.2 LinkedHashMap

       LinkedHashMap 保持了Map 的插入顺序，内部是通过继承 HashMap 把数据放入到链表中而不是Map中的节点，Map中的节点value 指向链表对象。实现了Map 的插入顺序访问方法。

  #### 2.2.3 TreeMap

  基于红黑树实现，用户可以通过传入 Comparator 的实现类，自定义排序规则，默认字典序排序。

  #### 2.2.4 WeakHashMap

  Java 中对象从内存回收的角度有多种引用类型，其中弱引用将会在GC时候回收弱应用类型的内存。WeahHashMap 中

 ### 2.3 Set

Set 家族成员简介：

- `Set` 继承了 `Collection` 的接口。实际上 `Set` 就是 `Collection`，只是行为略有不同：`Set` 集合不允许有重复元素。
- `SortedSet` 继承了 `Set` 的接口。`SortedSet` 中的内容是排序的唯一值，排序的方法是通过比较器(Comparator)。
- `NavigableSet` 继承了 `SortedSet` 的接口。它提供了丰富的查找方法：如"获取大于/等于某值的元素"、“获取小于/等于某值的元素”等等。
- `AbstractSet` 是一个抽象类，它继承于 `AbstractCollection`，`AbstractCollection` 实现了 Set 中的绝大部分方法，为实现 `Set` 的实例类提供了便利。
- `HashSet` 类依赖于 `HashMap`，它实际上是通过 `HashMap` 实现的。`HashSet` 中的元素是无序的、散列的。
- `TreeSet` 类依赖于 `TreeMap`，它实际上是通过 `TreeMap` 实现的。`TreeSet` 中的元素是有序的，它是按自然排序或者用户指定比较器排序的 Set。
- `LinkedHashSet` 是按插入顺序排序的 Set。
- `EnumSet` 是只能存放 Emum 枚举类型的 Set。

！！！**注意**！！！:
**Hashset的底层是 HashMap， Hastset 的value 并不为空，因为HashSet 移除数据的时候使用了 HashMap remove， remove 方法会返回移除对象。假如是Null无法判定是否成功移除值。**


### 2.4Queue

reference:

1. https://github.com/dunwu/javacore/blob/master/docs/container/java-container-queue.md

除了 BlockingQueue 其他队列都非多线程安全。

- Deque

> 双端队列接口，具体实现有LinkedList等

- ArrayDeque

> `ArrayDeque` 是 `Deque` 的顺序表实现。

- PriorityQueue

> 基于堆的优先级队列;
> 能够用于 topk 问题的解决 如：
> https://leetcode-cn.com/problems/kth-smallest-element-in-a-sorted-matrix/

```
Queue<int[]> temp = new PriorityQueue<int[]>(Comparator.comparingInt(o -> 
o[0]));

temp.add(new int[]{1,2,3});
temp.add(new int[]{2,2,3});

System.out.println(temp.peek());
```
