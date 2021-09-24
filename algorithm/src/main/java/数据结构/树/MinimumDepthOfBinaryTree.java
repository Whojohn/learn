package 数据结构.树;

public class MinimumDepthOfBinaryTree {

    /**
     * 树的最小深度
     *
     * @param root 根节点
     * @return 树的最小深度
     */
    public int minDepth(TreeNode root) {
        if (root == null) return 0;
        int left = 0;
        int right = 0;
        if (root.left == null && root.right == null) return 1;
        if (root.left != null) left = this.minDepth(root.left);
        if (root.right != null) right = this.minDepth(root.right);

        if (root.left == null) return right + 1;
        if (root.right == null) return left + 1;
        return Math.min(right, left) + 1;
    }
}
