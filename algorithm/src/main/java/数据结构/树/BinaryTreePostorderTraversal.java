package 数据结构.树;

import java.util.ArrayList;
import java.util.List;

public class BinaryTreePostorderTraversal {
    List<Integer> nodes = new ArrayList<>();

    /***
     * 二叉树后续遍历
     * https://leetcode-cn.com/problems/binary-tree-postorder-traversal/submissions/
     *
     * @param root 根节点
     * @return 后续遍历节点值
     */
    public List<Integer> postorderTraversal(TreeNode root) {
        if (root == null) return this.nodes;
        this.postorderTraversal(root.left);
        this.postorderTraversal(root.right);
        this.nodes.add(root.val);
        return this.nodes;
    }
}
