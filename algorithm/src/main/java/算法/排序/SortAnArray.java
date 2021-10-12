package 算法.排序;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SortAnArray {
    public int[] sortArray(int[] nums) {
        this.quickSort(nums, 0, nums.length - 1);
        return nums;
    }

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

    public int[] insertSort(int[] nums) {
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


    public void quickSort(int[] nums, int start, int end) {
        if (start > end)
            return;
        // 快排优化方式，避免有序出现，引发树偏移，导致复杂度上升
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
        this.quickSort(nums, start, i - 2);
        this.quickSort(nums, i, end);
    }

    public static void main(String[] args) {

        Random random = new Random();

        int[] source = new int[100];
        for (int a = 0; a < source.length; a++) {
            source[a] = random.nextInt();
        }
//        int[] source = new int[]{-2128065468, -890071049, -69097316, 231241380, 696505860, -560977694};
        int[] judge = new SortAnArray().sortArray(source);
        int[] judge2 = Arrays.copyOf(source, source.length);
        Arrays.sort(judge2);
        for (int a = 0; a < judge2.length; a++) {
            if (judge[a] != judge2[a]) {
                System.out.println("aaa");
            }
        }
    }

}
