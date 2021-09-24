package 数据结构.树;

public class MergeTwoBinaryTrees {
    /**
     * 合并左右两棵树
     * https://leetcode-cn.com/problems/merge-two-binary-trees/submissions/
     *
     * @param root1 树 a
     * @param root2 树 b
     * @return 合并后的树
     */
    public TreeNode mergeTrees(TreeNode root1, TreeNode root2) {
        if (root1 == null) return root2;
        if (root2 == null) return root1;
        if (root1 != null && root2 != null) {
            root1.val = root1.val + root2.val;
        }
        root1.left = this.mergeTrees(root1.left, root2.left);
        root1.right = this.mergeTrees(root1.right, root2.right);
        return root1;
    }

    public static void main(String[] args) {
        System.out.println(new MergeTwoBinaryTrees().mergeTrees(TreeNode.buildTree(new Integer[]{1, 3, 2, 5}),
                TreeNode.buildTree(new Integer[]{2, 1, 3, null, 4, null, 7})));
    }
}
