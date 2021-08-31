package 数据结构.树;

public class ValidateBinarySearchTree {

    /**
     * 验证二叉搜索树(BST)是否合法
     * https://leetcode-cn.com/problems/validate-binary-search-tree
     *
     * @param root
     * @return
     */
    public boolean isValidBST(TreeNode root) {
        return this.dfs(root, (long) Integer.MAX_VALUE + 1, (long) Integer.MIN_VALUE - 1);
    }

    public boolean dfs(TreeNode root, Long max, Long min) {
        if (root == null) return true;
        if (!(root.val < max && root.val > min)) {
            return false;
        }
        return this.dfs(root.left, (long) root.val, min) && this.dfs(root.right, max, (long) root.val);
    }

}

