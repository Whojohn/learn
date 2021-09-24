package 数据结构.树;

public class BalancedBinaryTree {
    /**
     * 判定一棵树是否为高度平衡树。高度平衡树左右节点高度差小于1
     * https://leetcode-cn.com/problems/balanced-binary-tree/description/
     *
     * @param root
     * @return
     */
    public boolean isBalanced(TreeNode root) {
        if (root == null) return true;
        if (Math.abs(this.getTreeLength(root.left) - this.getTreeLength(root.right)) > 1) return false;
        boolean label = true;
        if (this.isBalanced(root.left) == false || this.isBalanced(root.right) == false) label = false;
        return label;
    }

    public int getTreeLength(TreeNode tree) {
        if (tree == null) return 0;
        return Math.max(this.getTreeLength(tree.left), this.getTreeLength(tree.right)) + 1;
    }

    public static void main(String[] args) {
        System.out.println(new BalancedBinaryTree().isBalanced(TreeNode.buildTree(new Integer[]{1, 2, 3, 4, 5, null, 6, 7, null, null, null, null, 8})));
    }
}
