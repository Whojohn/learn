package 数据结构.树;

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



