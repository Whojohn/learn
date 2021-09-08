package 数据结构.数组;

public class BeautifulArrangementIi {

    /**
     * 数组相邻差值的个数
     * https://leetcode-cn.com/problems/beautiful-arrangement-ii/submissions/
     * 规则就是 n, 1, n-2, 2 构建不同的相差数值
     *
     * @param n
     * @param k
     * @return
     */
    public int[] constructArray(int n, int k) {
        int[] ret = new int[n];
        ret[0] = 1;
        for (int i = 1, interval = k; i <= k; i++, interval--) {
            ret[i] = i % 2 == 1 ? ret[i - 1] + interval : ret[i - 1] - interval;
        }
        for (int i = k + 1; i < n; i++) {
            ret[i] = i + 1;
        }
        return ret;
    }

    //    /**
//     * 啰嗦写法
//     *
//     * @param n
//     * @param k
//     * @return
//     */
//    public int[] constructArray(int n, int k) {
//        int[] temp = new int[n];
//        int left = 1;
//        int right = n;
//        int cou = 1;
//        int loc = 0;
//        if (k > 1) {
//            temp[0] = left;
//            temp[1] = right;
//            loc = 2;
//            left += 1;
//            right -= 1;
//        }
//        while (cou + 1 <= k - 1) {
//            if (cou % 2 == 0) {
//                temp[loc] = right;
//                right -= 1;
//            } else {
//                temp[loc] = left;
//                left += 1;
//            }
//            cou += 1;
//            loc += 1;
//        }
//
//        if (cou % 2 == 0) {
//            for (int a = 0; a < (k == 1 ? n : n - k); a++) {
//                temp[loc] = left + a;
//                loc += 1;
//            }
//        } else {
//            for (int a = 0; a < (k == 1 ? n : n - k); a++) {
//                temp[loc] = right - a;
//                loc += 1;
//            }
//
//        }
//        return temp;
//    }
    public static void main(String[] args) {
        new BeautifulArrangementIi().constructArray(6, 1);
    }
}
