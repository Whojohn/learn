package 数据结构.树;

import java.util.Arrays;

public class MaximumBinaryTree {
    /**
     * 递归组建最大二叉树
     * https://leetcode-cn.com/problems/maximum-binary-tree/
     *
     * @param nums 数组
     * @return
     */
    public TreeNode constructMaximumBinaryTree(int[] nums) {

        if (nums.length == 0) return null;

        int max = 0;
        int label = 0;
        for (int ind = 0; ind < nums.length; ind++) {
            if (nums[ind] > max) {
                max = nums[ind];
                label = ind;
            }
        }
        TreeNode temp = new TreeNode(max);
        temp.left = this.constructMaximumBinaryTree(Arrays.copyOfRange(nums, 0, label));
        temp.right = this.constructMaximumBinaryTree(Arrays.copyOfRange(nums, label + 1, nums.length));
        return temp;
    }

    public static void main(String[] args) {
        System.out.println(new MaximumBinaryTree().constructMaximumBinaryTree(
                new int[]{3, 2, 1, 6, 0, 5}));
    }
}
