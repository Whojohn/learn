package 数据结构.树;


public class SecondMinimumNodeInABinaryTree {

    int label;

    /**
     * 二叉树中第二小的节点
     * https://leetcode-cn.com/problems/second-minimum-node-in-a-binary-tree/
     *
     * @param root 根节点
     * @return 假如第二小的值存在返回-1，否则返回第二小值
     */
    public int findSecondMinimumValue(TreeNode root) {
        if (root == null || root.left == null) return -1;
        this.label = root.val;
        int temp = this.dfs(root);
        if (temp == this.label) return -1;
        return temp;
    }

    public int dfs(TreeNode root) {
        if (root.val == this.label) {
            if (root.left != null) {
                int le = this.dfs(root.left);
                int ri = this.dfs(root.right);
                if (le == this.label || ri == this.label) {
                    return Math.max(le, ri);
                } else {
                    return Math.min(le, ri);
                }
            }
        } else {
            return root.val;
        }

        return root.val;
    }

    public static void main(String[] args) {
        System.out.println(new SecondMinimumNodeInABinaryTree().findSecondMinimumValue(TreeNode.buildTree(new Integer[]{1, 1, 3, 1, 1, 3, 4, 3, 1, 1, 8})));

    }


}
