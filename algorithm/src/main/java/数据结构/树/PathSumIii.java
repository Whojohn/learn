package 数据结构.树;

public class PathSumIii {
    int total = 0;

    /**
     * 子树中是否包含路径等于某数值的路径，返回存在这些路径有多少条
     * https://leetcode-cn.com/problems/path-sum-iii/
     *
     * @param root      根节点
     * @param targetSum 目标值
     * @return 符合目标值的总数
     */
    public int pathSum(TreeNode root, int targetSum) {
        if (root == null) return total;
        this.dfs(root, targetSum, 0);
        this.pathSum(root.left, targetSum);
        this.pathSum(root.right, targetSum);
        return this.total;
    }

    public void dfs(TreeNode root, int targetSum, int total) {
        if (root == null) return;
        if (total + root.val == targetSum) {
            this.total += 1;
        }
        this.dfs(root.left, targetSum, total + root.val);
        this.dfs(root.right, targetSum, total + root.val);
    }

    public static void main(String[] args) {
        System.out.println(new PathSumIii().pathSum(TreeNode.buildTree(new Integer[]{5, 4, 8, 11, null, 13, 4, 7, 2, null, null, 5, 1}),
                22));
    }

}
