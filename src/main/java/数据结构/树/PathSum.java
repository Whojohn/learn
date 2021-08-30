package 数据结构.树;

public class PathSum {
    boolean label = false;

    /**
     * 是否存在一个从根节点出发到叶子节点的路径，该路径的节点值的和等于目标。
     * https://leetcode-cn.com/problems/path-sum/
     *
     * @param root      根节点
     * @param targetSum 目标总和
     * @return boolean 是否存在该节点
     */
    public boolean hasPathSum(TreeNode root, int targetSum) {
        this.dfs(root, 0, targetSum);
        return this.label;
    }

    public void dfs(TreeNode root, int total, int targetSum) {
        if (this.label) return;
        if (root == null) return;
        if (root.left == null && root.right == null && total + root.val == targetSum) {
            this.label = true;
        }
        this.dfs(root.left, total + root.val, targetSum);
        this.dfs(root.right, total + root.val, targetSum);
    }

    public static void main(String[] args) {
        System.out.println(new PathSum().hasPathSum(TreeNode.buildTree(new Integer[]{5, 4, 8, 11, null, 13, 4, 7, 2, null, null, null, 1}),
                22));
    }
}
