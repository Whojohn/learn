package 数据结构.树;


public class SubtreeOfAnotherTree {
    boolean step = false;

    /**
     * 给定两棵树a,b。 判定 a 中是否存在子节点，与 b 的树的整体结构一致。
     * https://leetcode-cn.com/problems/subtree-of-another-tree/description/
     *
     * @param root    树 a
     * @param subRoot 树 b
     * @return boolean true false
     */
    public boolean isSubtree(TreeNode root, TreeNode subRoot) {
        if (root == null) return this.step;

        if (this.dfs(root, subRoot)) {
            this.step = true;
        } else {
            this.isSubtree(root.right, subRoot);
            this.isSubtree(root.left, subRoot);
        }
        return this.step;
    }

    public boolean dfs(TreeNode root, TreeNode targex) {

        if (root == null && targex != null) {
            return false;
        }
        if (root == null) {
            return true;
        }
        if (root.val != targex.val) return false;
        return (this.dfs(root.left, targex.left) && this.dfs(root.right, targex.right));
    }

    public static void main(String[] args) {
        System.out.println(new SubtreeOfAnotherTree().isSubtree(TreeNode.buildTree(new Integer[]{3, 4, 5, 1, 2, null, null, null, null, 0}),
                TreeNode.buildTree(new Integer[]{4, 1, 2})));
    }
}
