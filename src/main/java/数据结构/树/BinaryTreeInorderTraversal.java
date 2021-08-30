package 数据结构.树;

import java.util.ArrayList;
import java.util.List;

public class BinaryTreeInorderTraversal {
    List<Integer> nodes = new ArrayList<>();

    /**
     * 树的中序遍历
     * https://leetcode-cn.com/problems/binary-tree-preorder-traversal/submissions/
     *
     * @param root 根节点
     * @return 中序遍历节点值
     */
    public List<Integer> inorderTraversal(TreeNode root) {
        if (root == null) return this.nodes;
        this.inorderTraversal(root.left);
        this.nodes.add(root.val);
        this.inorderTraversal(root.right);
        return this.nodes;
    }
}
