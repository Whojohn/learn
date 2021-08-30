package 数据结构.树;

public class MaximunDepthOfBinaryTree {
    /**
     * 树的最大深度
     * https://leetcode-cn.com/problems/maximum-depth-of-binary-tree/description/
     *
     * @param root
     * @return
     */
    public int maxDepth(TreeNode root) {
        if (root == null) {
            return 0;
        } else {
            return Math.max(this.maxDepth(root.right) + 1, this.maxDepth(root.left) + 1);
        }
    }
}
