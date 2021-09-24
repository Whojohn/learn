package 数据结构.树;

public class KthSmallestElementInABst {
    int source = 0;

    /**
     * 二叉搜索树(BST) 第k小
     * https://leetcode-cn.com/problems/kth-smallest-element-in-a-bst/
     *
     * @param root
     * @param k
     * @return
     */
    public int kthSmallest(TreeNode root, int k) {
        this.dfs(root, k);
        return this.source;
    }

    public int dfs(TreeNode root, int k) {
        if (k < 0) return k;

        if (root.left != null) k = this.dfs(root.left, k);
        k = k - 1;
        if (k == 0) {
            this.source = root.val;
            return -1;
        }
        if (root.right != null) k = this.dfs(root.right, k);
        return k;
    }

    public static void main(String[] args) {
        int temp = new KthSmallestElementInABst().kthSmallest(TreeNode.buildTree(new Integer[]{3, 1, 4, null, 2}), 1);
        System.out.println(temp);
    }
}
