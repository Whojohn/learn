package 数据结构.树;

public class ConvertBstToGreaterTree {
    /**
     * 将 bst 树转化为累加树
     * https://leetcode-cn.com/problems/convert-bst-to-greater-tree
     *
     * @param root 树根节点
     * @return 树的根节点
     */
    public TreeNode convertBST(TreeNode root) {
        this.dfs(root, 0);
        return root;
    }

    public int dfs(TreeNode root, int total) {
        if (root == null) return total;
        total = this.dfs(root.right, total);
        root.val += total;
        total = root.val;
        total = this.dfs(root.left, total);
        return total;
    }
}
