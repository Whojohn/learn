# 数据结构-数组

## 1. 数组的基本概念

**java中数组不是基本数据类型**：int[] 是类。

**java中数组([])的工具类**: Arrays。



## 2. 基本操作 & 算法

### 2.1 数组的移动

#### 2.1.1 数组中0移动到末尾 (双指针)

> 利用双指针把剔除

- [java](https://github.com/Whojohn/learn/tree/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%95%B0%E7%BB%84/MoveZeroes.java)

#### 2.1.2 数组重排列

- [java](https://github.com/Whojohn/learn/tree/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%95%B0%E7%BB%84/ReshapeTheMatrix.java)

### 2.2 有序数组的判定

#### 2.2.1 有序二维数组中找出第k小值

- [java](https://github.com/Whojohn/learn/tree/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%95%B0%E7%BB%84/KthSmallestElementInASortedMatrix.java)

#### 2.2.2 有序二维数组中找到特定值

- [java](https://github.com/Whojohn/learn/tree/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%95%B0%E7%BB%84/SearchA2dMatrixIi.java)



### 2.3 模式匹配

> 模式匹配的难点在于找到规律。或者是利用数组特定减少内存和性能消耗，如利用数组而不是 map 进行标记。

#### 2.3.1 数组最大循环数

- [java](https://github.com/Whojohn/learn/tree/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%95%B0%E7%BB%84/ArrayNesting.java)

#### 2.3.2 数组相邻差值的个数

> ```
> 规则就是 n, 1, n-2, 2 构建不同的相差数值
> ```

- [java](https://github.com/Whojohn/learn/tree/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%95%B0%E7%BB%84/BeautifulArrangementIi.java)

#### 2.3.3 数组中出现频率最高的数字的最短子数组长度

- [java](https://github.com/Whojohn/learn/tree/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%95%B0%E7%BB%84/DegreeOfAnArray.java)

#### 2.3.4 寻找数组中重复的数字(不能使用额外空间)

> 二分查找法的特殊应用

- [java](https://github.com/Whojohn/learn/tree/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%95%B0%E7%BB%84/FindTheDuplicateNumber.java)

#### 2.3.5 最多能完成排序的块

> ```
> 利用可排序的数组内最大的数值必须是k，k是当前位置进行判定。
> ```

- [java](https://github.com/Whojohn/learn/tree/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%95%B0%E7%BB%84/MaxChunksToMakeSorted.java)

#### 2.3.6 数组中1最大连续出现长度

- [java](https://github.com/Whojohn/learn/tree/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%95%B0%E7%BB%84/MaxConsecutiveOnes.java)

#### 2.3.7 寻找重复数字

- [java](https://github.com/Whojohn/learn/tree/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%95%B0%E7%BB%84/SetMismatch.java)

#### 2.3.8 数组对角是否相等

- [java](https://github.com/Whojohn/learn/tree/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%95%B0%E7%BB%84/ToeplitzMatrix.java)

