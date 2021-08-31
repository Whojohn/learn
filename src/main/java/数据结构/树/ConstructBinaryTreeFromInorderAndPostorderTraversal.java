package 数据结构.树;

import java.util.Arrays;
import java.util.stream.IntStream;

public class ConstructBinaryTreeFromInorderAndPostorderTraversal {

    /**
     * 中序加后续遍历恢复树
     * https://leetcode-cn.com/problems/construct-binary-tree-from-inorder-and-postorder-traversal/
     *
     * @param inorder   中序遍历
     * @param postorder 后续遍历
     * @return 树根节点
     */
    public TreeNode buildTree(int[] inorder, int[] postorder) {
        if (inorder.length == 0) return null;
        int step = postorder[postorder.length - 1];
        TreeNode temp = new TreeNode(step);
        if (inorder.length == 1) return temp;
        int inoLoc = IntStream.range(0, inorder.length).filter(i -> inorder[i] == step).findFirst().orElse(-1);
        temp.left = this.buildTree(Arrays.copyOfRange(inorder, 0, inoLoc), Arrays.copyOfRange(postorder, 0, inoLoc));
        temp.right = this.buildTree(Arrays.copyOfRange(inorder, inoLoc + 1, inorder.length), Arrays.copyOfRange(postorder, inoLoc, postorder.length - 1));
        return temp;

    }
}
