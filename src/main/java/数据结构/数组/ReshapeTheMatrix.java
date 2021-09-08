package 数据结构.数组;

public class ReshapeTheMatrix {
    /**
     * 数组重排列
     * https://leetcode-cn.com/problems/reshape-the-matrix/
     *
     * @param mat
     * @param r
     * @param c
     * @return
     */
    public int[][] matrixReshape(int[][] mat, int r, int c) {
        int x = mat.length;
        int y = mat[0].length;
        if (x * y != r * c) return mat;
        int[][] returnSource = new int[r][c];
        int preSize = x * y;
        int loc = 0;
        while (loc < preSize) {
            int preX = loc / y;
            int preY = loc % y;
            int nowX = loc / c;
            int nowY = loc % c;
            returnSource[nowX][nowY] = mat[preX][preY];
            loc += 1;
        }
        return returnSource;
    }
}
