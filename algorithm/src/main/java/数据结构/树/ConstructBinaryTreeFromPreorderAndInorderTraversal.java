package 数据结构.树;

import java.util.Arrays;
import java.util.stream.IntStream;

public class ConstructBinaryTreeFromPreorderAndInorderTraversal {
    /**
     * 前序遍历+中序遍历恢复树
     * https://leetcode-cn.com/problems/construct-binary-tree-from-preorder-and-inorder-traversal/submissions/
     *
     * @param preorder 前序遍历
     * @param inorder  中序遍历
     * @return 树
     */
    public TreeNode buildTree(int[] preorder, int[] inorder) {
        if (preorder.length == 0) return null;
        int step = preorder[0];
        int inoLoc = IntStream.range(0, inorder.length).filter(i -> inorder[i] == step).findFirst().orElse(-1);
        TreeNode temp = new TreeNode(step);
        if (preorder.length == 1) return temp;

        temp.left = this.buildTree(
                Arrays.copyOfRange(preorder, 1, inoLoc + 1),
                Arrays.copyOfRange(inorder, 0, inoLoc));

        temp.right = this.buildTree(
                Arrays.copyOfRange(preorder, inoLoc + 1, preorder.length),
                Arrays.copyOfRange(inorder, inoLoc + 1, inorder.length)
        );
        return temp;
    }

//    /**
//     * 双指针法
//     * @param preorder
//     * @param inorder
//     * @return
//     */
//    public TreeNode buildTree(int[] preorder, int[] inorder) {
//        return this.buildTreeByLoc(preorder, inorder, 0, preorder.length - 1, 0, inorder.length - 1);
//    }
//
//    public TreeNode buildTreeByLoc(int[] preorder, int[] inorder, int preStart, int preEnd, int inStart, int inEnd) {
//        if (preEnd - preStart < 0 || inEnd - inStart < 0) return null;
//
//        int step = preorder[preStart];
//        TreeNode temp = new TreeNode(step);
//
//        int inorderLoc = inStart;
//        for (; inorderLoc < inEnd + 1; inorderLoc++) {
//            if (inorder[inorderLoc] == step) break;
//        }
//        System.out.println(preStart + "  " + preEnd);
//
//        temp.left = this.buildTreeByLoc(preorder, inorder, preStart + 1, preStart + (inorderLoc - inStart), inStart, inorderLoc - 1);
//        temp.right = this.buildTreeByLoc(preorder, inorder, (inorderLoc - inStart) + preStart + 1, preEnd, inorderLoc + 1, inEnd);
//        return temp;
//    }


    public static void main(String[] args) {
        TreeNode temp = new ConstructBinaryTreeFromPreorderAndInorderTraversal().buildTree(new int[]{1, 2, 3},
                new int[]{3, 2, 1});
        System.out.println();


    }
}
