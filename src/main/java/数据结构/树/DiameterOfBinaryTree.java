package 数据结构.树;

public class DiameterOfBinaryTree {
    /**
     * 求左右子树最大深度和，开始节点可以不是树节点，可以是任意一个节点。
     * https://leetcode-cn.com/problems/diameter-of-binary-tree/submissions/
     *
     * @param root
     * @return
     */
    public int diameterOfBinaryTree(TreeNode root) {
        if (root == null) return 0;
        return Math.max(this.getMaxDeep(root.left) + this.getMaxDeep(root.right),
                Math.max(this.diameterOfBinaryTree(root.left),
                        this.diameterOfBinaryTree(root.right)));
    }

    private int getMaxDeep(TreeNode tree) {
        if (tree == null) return 0;
        return Math.max(this.getMaxDeep(tree.left), this.getMaxDeep(tree.right)) + 1;
    }

    public static void main(String[] args) {
        System.out.println(new DiameterOfBinaryTree().diameterOfBinaryTree(TreeNode.buildTree(new Integer[]{1, 2, 3, 4, 5})));
    }
}
