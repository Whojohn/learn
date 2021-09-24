# 数据结构-字符串

2021.09.06

## 1. 字符串基本概念

**java 字符串**：

1. String  - 不可变字符串
2. StringBuilder - 可变字符串（**非线程安全**）
3. StringBuffer- 可变字符串 （**线程安全**）

**回文串**： "a", "aba", "eeree" 是回文串。

**字符串轮转**： "abc" 轮转： "bca" ,"cab", "abc"

## 2. 字符串基本操作

### 2.1 模式匹配

> 1. 字符串轮转相当于 2*s.substring(x,x+ s.length)  x 为 0 到 s.length 。
> 2. 字典的映射。
> 3. 堆栈的使用，如表达式是否合法。(编译原理)
> 4. stringbuild 合并&修改字符串操作

#### 2.1.1 组成字符串的字符及数量是否相同

- [java 实现 ](https://github.com/Whojohn/learn/tree/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E5%AD%97%E7%AC%A6%E4%B8%B2/ValidAnagram.java)

#### 2.1.2 二进制串中连续字符子串最大长度

- [java 实现 ](https://github.com/Whojohn/learn/tree/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E5%AD%97%E7%AC%A6%E4%B8%B2/CountBinarySubstrings.java)

#### 2.1.3 字符串匹配首次出现位置 （indexof 手动实现）

- [java 实现 ](https://github.com/Whojohn/learn/tree/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E5%AD%97%E7%AC%A6%E4%B8%B2/ImplementStrstr.java)

#### 2.1.4 字符串是否同构(两字符串间字符映射是否唯一)

- [java 实现 ](https://github.com/Whojohn/learn/tree/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E5%AD%97%E7%AC%A6%E4%B8%B2/IsomorphicStrings.java)

#### 2.1.5 a 串多次复制自身是否能组成 b

- [java 实现 ](https://github.com/Whojohn/learn/tree/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E5%AD%97%E7%AC%A6%E4%B8%B2/RepeatedStringMatch.java)

#### 2.1.6 旋转字符串

- [java 实现 ](https://github.com/Whojohn/learn/tree/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E5%AD%97%E7%AC%A6%E4%B8%B2/RotateString.java)

#### 2.1.7 字符串b 是否能够通过a 轮转获得

- [java 实现 ](https://github.com/Whojohn/learn/tree/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E5%AD%97%E7%AC%A6%E4%B8%B2/StringRotationLcci.java)

## 2.2 回文串

> 1. 回文串翻转等于自身
>
>    ```
>    String temp = "aba";
>    return (temp.equals(new StringBuilder(temp).reverse().toString()));
>        
>    ```
>
> 2. 字符串中字串是否是回文的判定方法：
>
>    ```
>    - 方法1
>    暴力穷举
>    - 方法2
>    动态规划
>    
>    动态规划如下：
>    1. i，j 是字串，那么 i-1, j-1 必须为子串。并且， i == j。
>    2. 假如 i+1 != j-1 那么必须不为子串。(通过记录下i+1,j-1 的状态，)，从短的字符到长字符过度。
>    3. i<=j ，且 j-i <= 2 时，直接对比i,j的值，不使用递推式计算。
>    ```



#### 2.2.1 字符串自由组合最长回文串

- [java 实现 ](https://github.com/Whojohn/learn/tree/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E5%AD%97%E7%AC%A6%E4%B8%B2/LongestPalindrome.java)

#### 2.2.2 最长回文子串（字符串中字串是否是回文的判定方法）

- [java 实现 ](https://github.com/Whojohn/learn/tree/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E5%AD%97%E7%AC%A6%E4%B8%B2/LongestPalindromicSubstring.java)

#### 2.2.3 数字是否是回文串

- [java 实现 ](https://github.com/Whojohn/learn/tree/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E5%AD%97%E7%AC%A6%E4%B8%B2/PalindromeNumber.java)

#### 2.2.4 回文子串总数（字符串中字串是否是回文的判定方法）

- [java 实现 ](https://github.com/Whojohn/learn/tree/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E5%AD%97%E7%AC%A6%E4%B8%B2/PalindromicSubstrings.java)

#### 2.2.5 字符串头添加最少的字符以实现回文 (回文串翻转等于自身)

- [java 实现 ](https://github.com/Whojohn/learn/tree/master/algorithm/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E5%AD%97%E7%AC%A6%E4%B8%B2/ShortestPalindrome.java)