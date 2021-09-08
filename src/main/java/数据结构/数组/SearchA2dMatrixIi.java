package 数据结构.数组;

import java.util.Arrays;

public class SearchA2dMatrixIi {
    /**
     * 有序二维数组中找到特定值
     * https://leetcode-cn.com/problems/search-a-2d-matrix-ii/solution/sou-suo-er-wei-ju-zhen-ii-by-leetcode-2/
     *
     * @param matrix
     * @param target
     * @return
     */
    public boolean searchMatrix(int[][] matrix, int target) {
        int loc = 0;
        int yLen = matrix[0].length;
        while (true) {
            if (loc >= matrix.length) return false;
            if (target >= matrix[loc][0] && target <= matrix[loc][yLen - 1]) {
                if (Arrays.binarySearch(matrix[loc], target) > -1) return true;
            }
            loc += 1;
        }
    }
}
