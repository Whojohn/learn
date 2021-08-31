package 数据结构.树;

public class SearchInABinarySearchTree {

    /**
     * 二叉搜索树(BST)搜索
     * https://leetcode-cn.com/problems/search-in-a-binary-search-tree
     *
     * @param root
     * @param val
     * @return
     */
    public TreeNode searchBST(TreeNode root, int val) {
        if (root == null) return null;
        if (root.val > val) return this.searchBST(root.left, val);
        if (root.val == val) return root;
        if (root.val < val) return this.searchBST(root.right, val);
        return root;
    }
}
