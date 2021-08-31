package 数据结构.树;

import java.util.ArrayList;
import java.util.List;

public class DeleteNodeInABst {

    /**
     * 二叉搜索树删除节点
     * https://leetcode-cn.com/problems/delete-node-in-a-bst/
     *
     * @param root
     * @param key
     * @return
     */
    public TreeNode deleteNode(TreeNode root, int key) {
        if (root == null) {
            return null;
        }
        if (root.val > key) {
            root.left = this.deleteNode(root.left, key);
        } else if (root.val < key) {
            root.right = this.deleteNode(root.right, key);
        } else {
            if (root.right == null || root.left == null) {
                if (root.right != null) {
                    root = root.right;
                } else {
                    root = root.left;
                }
            } else {
                List<TreeNode> middleList = readFromMiddle(new ArrayList<>(), root);
                int ind = 0;
                for (; ind < middleList.size(); ind++) {

                    if (middleList.get(ind).val == key) {
                        ind += 1;
                        break;
                    }
                }
                TreeNode targetNode = middleList.get(ind);
                root.val = targetNode.val;
                root.right = this.deleteNode(root.right, targetNode.val);

            }
        }

        return root;
    }

    public List<TreeNode> readFromMiddle(List<TreeNode> nodes, TreeNode tree) {
        if (tree != null) {
            this.readFromMiddle(nodes, tree.left);
            nodes.add(tree);
            this.readFromMiddle(nodes, tree.right);
        } else {
            return nodes;
        }
        return nodes;
    }
}
