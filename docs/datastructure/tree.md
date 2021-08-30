# 数据结构-树

reference:

1. https://algs4.cs.princeton.edu/32bst/
2. https://zh.wikipedia.org/wiki/%E4%BA%8C%E5%8F%89%E6%A0%91
3. https://github.com/chachaxw/data-structure-and-algorithm
4. https://github.com/CyC2018/CS-Notes/blob/master/notes/Leetcode%20%E9%A2%98%E8%A7%A3%20-%20%E7%9B%AE%E5%BD%95.md

## 1. 树的基本概念

**根节点** ： 根节点是树层次结构中的最顶层节点。

**边**(edge)：所有结点都由边相连，用于标识结点间的关系。用它来确定节点之间的关系。

**叶节点**： 没有子节点的节点。

**父节点**：若一个节点含有子节点，则这个节点称为其子节点的父节点；

**子树**： 如果根节点不为空，则树`T1`，`T2`和`T3`称为根节点的子树。

**树高：**是由根结点出发，到子结点的最长路径长度。

**结点深度：**是指对应结点到根结点路径长度。

**度**： 节点的度数等于子节点数，节点数。 叶子节点的度数总是`0`。

**二叉树**(**二叉树是无序的，二叉搜索树是有序的**，)： 二叉树一颗度最大为2的树。

**二叉搜索树**：左子树任何节点值小于当前节点值，右子任何节点的值大于当前节点的值

```
    # 例子
    #      20
    #    /      \
    #   18        25
    #  /        /
    # 15       21
    #           \
    #            23
```

**先序遍历**：根节点->左子树->右子树

> [20, 18, 15, 25, 21, 23]

**中序遍历**：左子树->根节点->右子树

> [15, 18, 20, 21, 23, 25]

**后序遍历**：左子树->右子树->根节点

>[15, 18, 23, 21, 25, 20]

**前驱节点**：某个节点的前驱节点是中序遍历该节点的前一个元素；(前驱后继的子节点都只会有一层，且子节点要么是左节点，要么是右节点)

> 如：23节点前驱节点是18

**后继节点**：某个节点的前驱节点是中序遍历该节点的后一个元素；(前驱后继的子节点都只会有一层，且子节点要么是左节点，要么是右节点)

>如：23节点后继节点是21

**二叉搜索树如何删除节点：**

> 1. 删除节点为叶子节点，直接去除。
> 2. 删除节点只有一个子树时，将子树的根节点变为删除节点。
> 3. 删除节点有两个子树时候：
>
> > 1. 找到删除的节点
> > 2. 找到该节点的中序遍历的后继节点(右子树最小的节点)，将需要删除节点的值替换为后继节点的值。
> > 3. 后继节点假如有子节点，那么必须为右子节点，将后继节点的值替换为右子节点的值，移除右节点，将右节点的左右子树变为后继节点的左右子树。

## 2. 树的算法 && 总结

- 遍历方式的本质

  - 对于非二叉搜索树

    > 递归方式的不同。（前序是自顶向下，然后先左后右。类似的后续是自底向上遍历。）

  - 对于二叉搜索树(同时具备非二叉树的特性)

    > 1. 中序遍历可以获取树的有序列表。
    > 2. 只有左子树的遍历能够获取到最小值。
    > 3. 只有右子树的遍历可以获取到最大值。
    > 4. 第 n **大、小** 的值的获取，中序遍历，第n 次的数值。

- 树的本质是递归 & 图的一种特殊情形

- 递归的底层实现是堆栈

- 树相关算法的优化方式：

  > 1. 剪枝 (额外的条件终止不可能的遍历)
  > 2. 状态记录 (额外记录已经遍历过的路线，避免多次遍历同样路径的消耗)

### 2.1 leetcode 树实现

> leetcode 树实现，不用泛型是因为 leetcode 树官方定义与泛型在细节上不一致，比如泛型需要类型强转，包装类，上下界问题等。


- [java 实现 ](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%91/TreeNode.java)

### 2.2 树的基本操作

#### 2.2.1 前序遍历

- py 伪代码

```
dfs(root):
    if root == None: return
    print root.val
    self.dfs(root.left)
    self.dfs(root.right)
```

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%91/BinaryTreePreorderTraversal.java)

##### 2.2.1.1 先序遍历转化树为链表
- [java 实现 ](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%91/FlattenBinaryTreeToLinkedList.java)

#### 2.2.2 中序遍历

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%91/BinaryTreeInorderTraversal.java)

#### 2.2.3 后续遍历

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%91/BinaryTreePostorderTraversal.java)

### 2.3 Top n/ Min n问题

#### 2.3.1 第二小值

> 给定一棵特殊的树， roo.val = min(root.left.val, root.right.val) ，求第二小的节点值

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%91/SecondMinimumNodeInABinaryTree.java)

### 2.4 树的深度

#### 2.4.1 树的最大深度

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%91/MaximunDepthOfBinaryTree.java)

#### 2.4.2 树的最小深度

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%91/MinimumDepthOfBinaryTree.java)

#### 2.4.3 求左右子树最大深度和

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%91/DiameterOfBinaryTree.java)

### 2.5 二叉搜索树

**二叉搜索树如何删除节点：**

> 1. 删除节点为叶子节点，直接去除。
> 2. 删除节点只有一个子树时，将子树的根节点变为删除节点。
> 3. 删除节点有两个子树时候：
>
> > 1. 找到删除的节点
> > 2. 找到该节点的中序遍历的后继节点(右子树最小的节点)，将需要删除节点的值替换为后继节点的值。
> > 3. 后继节点假如有子节点，那么必须为右子节点，将后继节点的值替换为右子节点的值，移除右节点，将右节点的左右子树变为后继节点的左右子树。

#### 2.5.1 二叉搜索树删除

```
       public TreeNode deleteNode(TreeNode root, int key) {
        if (root == null) {
            return null;
        }
        if (root.val > key) {
            root.left = this.deleteNode(root.left, key);
        } else if (root.val < key) {
            root.right = this.deleteNode(root.right, key);
        } else {
            if (root.right == null || root.left == null) {
                if (root.right != null) {
                    root = root.right;
                } else {
                    root = root.left;
                }
            } else {
                List<TreeNode> middleList = readFromMiddle(new ArrayList<>(), root);
                int ind = 0;
                for (; ind < middleList.size(); ind++) {

                    if (middleList.get(ind).val == key) {
                        ind += 1;
                        break;
                    }
                }
                TreeNode targetNode = middleList.get(ind);
                root.val = targetNode.val;
                root.right = this.deleteNode(root.right, targetNode.val);

            }
        }

        return root;
    }
    public List<TreeNode> readFromMiddle(List<TreeNode> nodes,TreeNode tree) {
        if (tree != null ) {
            this.readFromMiddle(nodes,tree.left);
            nodes.add(tree);
            this.readFromMiddle(nodes,tree.right);
        } else {
            return nodes;
        }
        return nodes;
    }
```



### 2.6 树的其他问题

#### 2.6.1 二叉树是否为高度平衡树

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%91/BalancedBinaryTree.java)

#### 2.6.2 树翻转

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%91/InvertBinaryTree.java)

#### 2.6.3 等于某值的路径

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%91/PathSum.java)

#### 2.6.4 等于某值的路径条数

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%91/PathSumIii.java)

#### 2.6.5 是否存在特定子树

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%91/SubtreeOfAnotherTree.java)

#### 2.6.6 是否为对称树

- [java 实现 ](https://github.com/Whojohn/learn/blob/master/src/main/java/%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/%E6%A0%91/SymmetricTree.java)













