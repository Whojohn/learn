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


## 2. 树的数据结构实现(leetcode 树实现，不用泛型是因为 leetcode 树官方定义与泛型在细节上不一致，比如泛型需要类型强转等。)
```
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Leetcode 算法树，
 */
public class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;


    TreeNode(int val) {
        this.val = val;
    }

    /**
     * Leetcode 树转换，将数组转换为树。
     *
     * @param arr 数组 new Integer[]{1, 2, 3, 4, 5}
     * @return TreeNode 节点组成的树
     */
    public static TreeNode buildTree(Integer[] arr) {
        TreeNode root;
        Queue<TreeNode> q = new LinkedList<>();
        int i = 0;
        root = arr[i] == null ? null : new TreeNode(arr[i]);
        q.add(root);
        i++;
        while (!q.isEmpty() && i < arr.length) {
            TreeNode t1 = q.poll();
            if (t1 != null) {
                t1.left = arr[i] == null ? null : new TreeNode(arr[i]);
                q.add(t1.left);
                i++;
                if (i >= arr.length) {
                    break;
                }
                t1.right = arr[i] == null ? null : new TreeNode(arr[i]);
                q.add(t1.right);
                i++;
            }
        }
        return root;
    }


    /**
     * 树在某个深度下的最大节点数
     *
     * @param step 树深度
     * @return 总节点数
     */
    public int getTwoQuota(int step) {
        int total = 0;
        for (int a = 0; a < step; a++) {
            total += Math.pow(2, a);
        }
        return total;
    }


    /**
     * 树前序遍历输出节点
     *
     * @param tree      树
     * @param nodeValue 节点累计数组
     * @return int 数组
     */
    public List<Integer> readFromFirst(TreeNode tree, List<Integer> nodeValue) {
        if (tree != null) {
            nodeValue.add(tree.val);
            this.readFromFirst(tree.left, nodeValue);
            this.readFromFirst(tree.right, nodeValue);
        } else {
            return nodeValue;
        }

        return nodeValue;
    }

    /**
     * 树中序遍历输出节点
     *
     * @param tree      根节点
     * @param nodeValue 节点累计数组
     * @return int 数组
     */
    public List<Integer> readFromMiddle(TreeNode tree, List<Integer> nodeValue) {
        if (tree != null) {
            this.readFromMiddle(tree.left, nodeValue);
            nodeValue.add(tree.val);
            this.readFromMiddle(tree.right, nodeValue);
        } else {
            return nodeValue;
        }
        return nodeValue;
    }

    public List<TreeNode> readFromMiddle(List<TreeNode> nodes, TreeNode tree) {
        if (tree != null) {
            this.readFromMiddle(nodes, tree.left);
            nodes.add(tree);
            this.readFromMiddle(nodes, tree.right);
        } else {
            return nodes;
        }
        return nodes;
    }


    /**
     * 后遍历输出节点
     *
     * @param tree      根节点
     * @param nodeValue 节点累计数组
     * @return int 数组
     */
    public List<Integer> readFromLast(TreeNode tree, List<Integer> nodeValue) {
        if (tree != null) {
            this.readFromLast(tree.left, nodeValue);
            this.readFromLast(tree.right, nodeValue);
            nodeValue.add(tree.val);
        } else {
            return nodeValue;
        }
        return nodeValue;
    }


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

    public static void main(String[] args) {
        TreeNode tree = TreeNode.buildTree(new Integer[]{5, 1, 6, 2, 0, 8, null, null, 7, 4});
        List<Integer> readFromFirst = tree.readFromFirst(tree, new ArrayList<>());
        System.out.println("前序遍历");
        readFromFirst.forEach(e -> System.out.print(e + " "));// 5 1 2 7 0 4 6 8
        System.out.println();

        List<Integer> readFromMiddle = tree.readFromMiddle(tree, new ArrayList<>());
        System.out.println("中序遍历");
        readFromMiddle.forEach(e -> System.out.print(e + " "));// 2 7 1 4 0 5 8 6
        System.out.println();

        List<Integer> readFromLast = tree.readFromLast(tree, new ArrayList<>());
        System.out.println("后序遍历");
        readFromLast.forEach(e -> System.out.print(e + " "));// 7 2 4 0 1 8 6 5
        System.out.println();

        tree = buildTree(new Integer[]{5, 3, 6, 2, 4, null, 7});
        tree.deleteNode(tree, 3);

    }
}
```

