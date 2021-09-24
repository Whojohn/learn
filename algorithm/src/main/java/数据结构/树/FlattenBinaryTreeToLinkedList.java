package 数据结构.树;

public class FlattenBinaryTreeToLinkedList {

    /**
     * 把树按照先序遍历的顺序重新组成一个链表
     * https://leetcode-cn.com/problems/flatten-binary-tree-to-linked-list/submissions/
     *
     * @param root 根节点
     */
    public void flatten(TreeNode root) {
        this.dfs(root);
    }

    /***
     *
     * @param root 根节点
     * @return 重新组装的节点
     */
    public TreeNode dfs(TreeNode root) {
        if (root == null) return null;
        TreeNode le = this.dfs(root.left);
        TreeNode ri = this.dfs(root.right);
        root.right = le;
        root.left = null;
        TreeNode judge = le;
        if (judge == null) {
            root.right = ri;
        } else {
            while (judge != null) {
                if (judge.right == null) {
                    judge.right = ri;
                    break;
                } else {
                    judge = judge.right;
                }
            }
        }

        return root;
    }

    public static void main(String[] args) {
        TreeNode temp = TreeNode.buildTree(new Integer[]{1, 2, 5, 3, 4, null, 6});

        new FlattenBinaryTreeToLinkedList().dfs(TreeNode.buildTree(new Integer[]{1, 2, 5, 3, 4, null, 6}));
        System.out.println(temp);
    }
}


