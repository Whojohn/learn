package 数据结构.数组;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

public class KthSmallestElementInASortedMatrix {
    /**
     * 二维有序数组中找出第k小值
     * https://leetcode-cn.com/problems/kth-smallest-element-in-a-sorted-matrix/
     * <p>
     * 利用 PriorityQueue (堆排序)维护 top k
     *
     * @param matrix
     * @param k
     * @return
     */
    public int kthSmallest(int[][] matrix, int k) {
        Queue<int[]> temp = new PriorityQueue<int[]>(Comparator.comparingInt(o -> o[0]));
        int[] label = new int[matrix.length];
        // 需要把当前数值和所在列信息记录以实现
        for (int loc = 0; loc < matrix.length; loc++) temp.add(new int[]{matrix[loc][0], loc, 1});

        int targetK = 0;
        for (int i = 0; i < k; i++) {
            int[] t = temp.poll();
            targetK = t[0];
            if (t[2] < matrix[0].length) {
                temp.add(new int[]{matrix[t[1]][t[2]], t[1], t[2] + 1});
            }

        }
        return targetK;
    }

    public static void main(String[] args) {
        new KthSmallestElementInASortedMatrix().kthSmallest(new int[][]{{1, 5, 9}, {10, 11, 13}, {12, 13, 15}}, 8);
    }
}
