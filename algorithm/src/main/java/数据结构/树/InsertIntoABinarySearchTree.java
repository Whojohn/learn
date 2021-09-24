package 数据结构.树;

public class InsertIntoABinarySearchTree {

    /**
     * 二叉搜索树(BST)插入
     * https://leetcode-cn.com/problems/insert-into-a-binary-search-tree
     *
     * @param root
     * @param val
     * @return
     */
    public TreeNode insertIntoBST(TreeNode root, int val) {
        if (root == null) return new TreeNode(val);
        if (root.val > val) {
            root.left = this.insertIntoBST(root.left, val);
        } else {
            root.right = this.insertIntoBST(root.right, val);
        }
        return root;
    }
}
