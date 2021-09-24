package 数据结构.树;

public class InvertBinaryTree {
    /**
     * 树翻转
     * https://leetcode-cn.com/problems/invert-binary-tree/description/
     *
     * @param root
     * @return
     */
    public TreeNode invertTree(TreeNode root) {
        if (root == null) return null;
        TreeNode temp = root.left;
        root.left = this.invertTree(root.right);
        root.right = this.invertTree(temp);
        return root;
    }


}
