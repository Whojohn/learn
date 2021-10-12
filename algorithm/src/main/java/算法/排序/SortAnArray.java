package 算法.排序;

public class SortAnArray {
    public int[] sortArray(int[] nums) {
        return this.sortBySelect(nums);
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
            for (int each = 0; each < nums.length-step-1; each++) {
                if (nums[each] > nums[each+1]) {
                    label +=1;
                    int temp = nums[each+1];
                    nums[each+1] = nums[each];
                    nums[each] = temp;
                }
            }
            if (label == 0) return nums;
        }
        return nums;
    }
}
