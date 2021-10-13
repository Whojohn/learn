package 算法.排序;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SortAnArray {
    public int[] sortArray(int[] nums) {
        this.sortByQuick(nums, 0, nums.length - 1);
        return nums;
    }

    /**
     * 冒泡排序：
     * 相邻两个数：i i+1 对比，假如 i>i+1 交换两数，从 i+1, i+2 继续执行以上判断。最终 len-1 位置为最大的数值，由此类推。
     * 如： 2,3,1,5 降序
     * 第一轮：1,3,2,5
     * 2,3,1,5; 2,1,3,5; 2,1,3,5;
     * 第二轮 2,1,3
     * 1,2,3 1,2,3
     * 第三轮： 1,2
     * 1,2
     *
     * @param nums 数组
     * @return 降序数组
     */
    public int[] sortByBubble(int[] nums) {
        for (int step = 0; step < nums.length; step++) {
            int label = 0;
            for (int each = 0; each < nums.length - step - 1; each++) {
                if (nums[each] > nums[each + 1]) {
                    label += 1;
                    int temp = nums[each + 1];
                    nums[each + 1] = nums[each];
                    nums[each] = temp;
                }
            }
            if (label == 0) return nums;
        }
        return nums;
    }

    /**
     * 选择排序
     * 每一轮次将最小的值放到最右手，依次完成
     * 如： 2,3,1,5 降序
     * 第一轮：2,3,1,5
     * 1,3,2,5
     * 第二轮：3,2,5
     * 2,3,5
     * 第三轮：3,5
     * 3,5
     *
     * @param nums nums 数组
     * @return 降序数组
     */
    public int[] sortBySelect(int[] nums) {
        for (int step = 0; step < nums.length; step++) {
            for (int each = step + 1; each < nums.length; each++) {
                if (nums[step] > nums[each]) {
                    int temp = nums[step];
                    nums[step] = nums[each];
                    nums[each] = temp;
                }
            }
        }
        return nums;
    }

    /**
     * 二分查找寻找插入点，稳定算法
     *
     * @param source 目标列表
     * @param target 待插入值
     * @param start  二分查找开始位置
     * @param end    二分查找结束位置
     * @return int 插入位置
     */
    private int binarySearch(List<Integer> source, Integer target, int start, int end) {
        if (start > end) {
            return start;
        } else {
            int mid = (end - start) / 2 + start;
            if (source.get(mid) >= target) {
                return this.binarySearch(source, target, start, mid - 1);
            } else {
                return this.binarySearch(source, target, mid + 1, end);
            }
        }
    }

    /**
     * 插入排序算法
     * 每一轮次把数插入到已排序的数组中
     * 如： 2,3,1,5 降序
     * 第一轮：
     * 已排序数组为空，直接插入2
     * 第二轮： 把 3 插入已排序的数组 2中
     * 2,3
     * 第三轮：把1插入到已排序的数组中
     * 1,2,3
     *
     * @param nums nums 数组
     * @return 降序数组
     */
    public int[] sortByInsert(int[] nums) {
        List<Integer> temp = new ArrayList<Integer>() {{
            add(nums[0]);
        }};

        for (int a = 1; a < nums.length; a++) {
            temp.add(this.binarySearch(temp, nums[a], 0, temp.size() - 1), nums[a]);
        }
        for (int a = 0; a < nums.length; a++) {
            nums[a] = temp.get(a);
        }
        return nums;
    }

    /**
     * 快排排序
     * 每一轮选取第一数，将数组按照小于等于该数排列在左，大于等于排列在右手，利用双指针进行原地交换数
     * 如： 2,3,1,5 降序
     * 第一轮：
     * 取2：从3，1，5进行判定。 2,5,1,3 ;2,1,5,3;1,1,5,3; 插入2到左手的指针-1位置：1，2，5，3；递归 (1) (2，5，3) 排序
     *
     * @param nums  原始数组
     * @param start 快排起始位置
     * @param end   快排结束位置
     */
    public void sortByQuick(int[] nums, int start, int end) {
        if (start > end)
            return;
//        快排优化方式，避免有序出现，引发树偏移，导致复杂度上升
//        int change = nums[(start+end)/2];
//        nums[(start+end)/2] = nums[start];
//        nums[start] = change;


        int node = nums[start];
        int i = start + 1;
        int j = end;
        while (i <= j) {
            if (nums[i] <= node) {
                nums[i - 1] = nums[i];
                i += 1;
            } else {
                int temp = nums[j];
                nums[j] = nums[i];
                nums[i] = temp;
                j -= 1;
            }
        }
        nums[i - 1] = node;
        this.sortByQuick(nums, start, i - 2);
        this.sortByQuick(nums, i, end);
    }

    /**
     * 插入排序，两两合并数组
     *
     * @param nums 数组
     * @return 升序数组
     */
    public int[] sortByMerge(int[] nums) {
        if (nums.length <= 1) {
            return nums;
        } else if (nums.length == 2) {
            if (nums[0] < nums[1]) {
                return nums;
            } else {
                int temp = nums[0];
                nums[0] = nums[1];
                nums[1] = temp;
                return nums;
            }
        } else {
            int mid = nums.length / 2;
            return this.mergeTwo(this.sortByMerge(Arrays.copyOfRange(nums, 0, mid)), this.sortByMerge(Arrays.copyOfRange(nums, mid, nums.length)));
        }

    }

    public int[] mergeTwo(int[] left, int[] right) {
        int[] source = new int[left.length + right.length];
        int l = 0;
        int r = 0;
        int loc = 0;
        while (l < left.length && r < right.length) {
            if (left[l] < right[r]) {
                source[loc] = left[l];
                l += 1;
            } else {
                source[loc] = right[r];
                r += 1;
            }
            loc += 1;
        }
        if (l < left.length) {
            for (; l < left.length; l++) {
                source[loc] = left[l];
                loc += 1;
            }
        }

        if (r < right.length) {
            for (; r < right.length; r++) {
                source[loc] = right[r];
                loc += 1;
            }
        }

        return source;
    }


    public static void main(String[] args) {

        Random random = new Random();

        int[] source = new int[100];
        for (int a = 0; a < source.length; a++) {
            source[a] = random.nextInt();
        }
//        int[] source = new int[]{-2128065468, -890071049, -69097316, 231241380, 696505860, -560977694};
        int[] judge = new SortAnArray().sortByMerge(source);
        int[] judge2 = Arrays.copyOf(source, source.length);
        Arrays.sort(judge2);
        for (int a = 0; a < judge2.length; a++) {
            if (judge[a] != judge2[a]) {
                System.out.println("逻辑错误");
            }
        }
    }

}
