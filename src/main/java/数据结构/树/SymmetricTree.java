package 数据结构.树;

public class SymmetricTree {

    /**
     * 是否为对称树
     * https://leetcode-cn.com/problems/symmetric-tree/
     *
     * @param root 根节点
     * @return boolean true false
     */
    public boolean isSymmetric(TreeNode root) {
        if (root == null) return true;
        return this.dfs(root.left, root.right);
    }

    public boolean dfs(TreeNode left, TreeNode right) {

        if (left == null && right == null) return true;
        if (left == null || right == null || left.val != right.val) return false;

        return this.dfs(left.left, right.right) && this.dfs(left.right, right.left);
    }
}
