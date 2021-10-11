package 数据结构.数组;

public class MinimunPathSum {
    private int xLen;
    private int yLen;
    private int[][] stepRecord;

    /**
     * 二维数组求最短路径，每一次只能往下或者往右走
     * https://leetcode-cn.com/problems/minimum-path-sum/
     *
     * @param grid
     * @return
     */
    public int minPathSum(int[][] grid) {
        this.xLen = grid.length;
        this.yLen = grid[0].length;
        // 初始化状态
        this.stepRecord = new int[xLen][yLen];
        for (int x = 0; x < xLen; x++) {
            for (int y = 0; y < yLen; y++) {
                this.stepRecord[x][y] = -1;
            }
        }


        this.dfs(grid, 0, 0, 0);

        return this.stepRecord[0][0] == -1 ? grid[0][0] : this.stepRecord[0][0];
    }

    public int dfs(int[][] grid, int x, int y, int valueCou) {


        if (x >= xLen || y >= yLen) {
            return 0;
        } else if (x == xLen - 1 && y == yLen - 1) {
            return grid[x][y];
        }

        if (this.stepRecord[x][y] != -1) {
            return this.stepRecord[x][y];
        } else {
            int temp = this.dfs(grid, x + 1, y, grid[x][y] + valueCou);
            int temp2 = this.dfs(grid, x, y + 1, grid[x][y] + valueCou);

            if (x + 1 < xLen && y + 1 < yLen) {
                this.stepRecord[x][y] = Math.min(temp, temp2) + grid[x][y];
            } else {
                this.stepRecord[x][y] = Math.max(temp, temp2) + grid[x][y];
            }
            return this.stepRecord[x][y];
        }
    }

    public static void main(String[] args) {
        new MinimunPathSum().minPathSum(new int[][]{{1, 3, 1}, {1, 5, 1}, {4, 2, 1}});
    }
}
