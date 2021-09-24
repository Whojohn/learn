package 数据结构.数组;

public class ToeplitzMatrix {

    /**
     * 数组对角是否相等
     * https://leetcode-cn.com/problems/toeplitz-matrix/submissions/
     *
     * @param matrix
     * @return
     */
    public boolean isToeplitzMatrix(int[][] matrix) {
        // 右上角
        for (int i = 0; i < matrix[0].length; i++) {
            if (this.checkLine(matrix, 0, i) != true) return false;
        }
        // 左下角
        for (int i = 0; i < matrix.length; i++) {
            if (this.checkLine(matrix, i, 0) != true) return false;
        }
        System.out.println();
        return true;
    }

    public boolean checkLine(int[][] matrix, int x, int y) {
        int xEnd = matrix.length;
        int yEnd = matrix[0].length;
        int pre = matrix[x][y];
        while (x < xEnd && y < yEnd) {
            if (matrix[x][y] != pre) {
                return false;
            }
            x += 1;
            y += 1;
        }
        return true;
    }

    public static void main(String[] args) {
        new ToeplitzMatrix().isToeplitzMatrix(new int[][]{{1, 2, 3, 4}, {5, 1, 2, 3}, {9, 5, 1, 2}});

    }
}
