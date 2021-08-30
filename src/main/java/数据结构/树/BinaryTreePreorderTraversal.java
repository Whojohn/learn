package 数据结构.树;

import java.util.ArrayList;
import java.util.List;

public class BinaryTreePreorderTraversal {
    List<Integer> temp = new ArrayList<>();

    /**
     * 树的前序遍历
     * https://leetcode-cn.com/problems/binary-tree-preorder-traversal/
     *
     *
     * @param root 根节点
     * @return 前序遍历节点值
     */
    public List<Integer> preorderTraversal(TreeNode root) {
        // 不能return null
        if (root == null) return temp;
        temp.add(root.val);
        this.preorderTraversal(root.left);
        this.preorderTraversal(root.right);
        return temp;
    }


    public static void main(String[] args) {
        TreeNode tree = TreeNode.buildTree(new Integer[]{});
        System.out.println(new BinaryTreePreorderTraversal().preorderTraversal(tree));

    }
}
