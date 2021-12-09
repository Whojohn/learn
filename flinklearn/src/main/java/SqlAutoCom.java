import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;

import java.util.*;
import java.util.stream.Collectors;

public class SqlAutoCom {
    public static void blinkImpl(List<String> testCase) {
        TableEnvironment tableEnvironment = TableEnvironment.create(EnvironmentSettings.newInstance().build());
        // for sql context test
        tableEnvironment.executeSql("CREATE TABLE source_table (\n" +
                "    user_id INT,\n" +
                "    cost DOUBLE,\n" +
                "    ts AS localtimestamp,\n" +
                "    WATERMARK FOR ts AS ts\n" +
                ") WITH (\n" +
                "    'connector' = 'datagen',\n" +
                "    'rows-per-second'='5',\n" +
                "\n" +
                "    'fields.user_id.kind'='random',\n" +
                "    'fields.user_id.min'='1',\n" +
                "    'fields.user_id.max'='10',\n" +
                "\n" +
                "    'fields.cost.kind'='random',\n" +
                "    'fields.cost.min'='1',\n" +
                "    'fields.cost.max'='100'\n" +
                ")");
        testCase.forEach(e -> System.out.println(Arrays.toString(tableEnvironment.getCompletionHints(e, e.length()))));
    }

    public static void main(String[] args) {

        // flink sql client implement
//        blinkImpl(new ArrayList<String>() {{
//            add("sel");
//            add("select * f");
//            add("select * frem");
//            add("select * from source_tab");
//            add("select * from source_tab where user_i");
//        }});

        // return
        // []              #we want select
        // [FETCH, FROM]   #perfect
        // []              #maybe from is better choice
        // [default_catalog.default_database.source_table] # perfect
        // []  # we want notice user_id


    }
}

class FstTree {
    TreeNode readFromHead = new TreeNode(' ');
    TreeNode readFromTail = new TreeNode(' ');

    class Single {
        public Integer loc = new Integer(0);
    }

    public void initSearchTree(String word) {
        this.buildTree(word, this.readFromHead);
        this.buildTree(new StringBuffer(word).reverse().toString(), this.readFromTail);
    }

    public void buildTree(String word, TreeNode buildWay) {
        int loc = 0;

        Map<Character, TreeNode> nowStep = buildWay.getNext();

        while (loc < word.length()) {
            Character nowChar = word.charAt(loc);

            if (!nowStep.containsKey(nowChar)) {
                TreeNode temp = new TreeNode(nowChar);
                nowStep.put(nowChar, temp);
            }
            nowStep = nowStep.get(nowChar).getNext();
            loc += 1;
        }
    }

    public List<TreeNode> getMaybeNodeList(String word, TreeNode searchWay, Single breakLoc) {
        int loc = 0;
        Map<Character, TreeNode> nowStep = searchWay.getNext();
        while (loc < word.length()) {
            Character nowChar = word.charAt(loc);

            if (!nowStep.containsKey(nowChar)) {
                // maybe wrong typing
                breakLoc.loc = loc;
                break;
            }
            nowStep = nowStep.get(nowChar).getNext();
            loc += 1;
        }
        breakLoc.loc = loc;
        // 最起码有1位以上的匹配，不然全部输出没有意义
        return loc > 0 ? new ArrayList<TreeNode>(nowStep.values()) : new ArrayList<>();
    }

    public void getDFSWord(List<String> returnSource, String buffer, TreeNode now) {


        if (now.getNext().size() == 0) {
            returnSource.add(buffer + now.getStep());
            return;
        } else {
            now.getNext().values().forEach(each -> this.getDFSWord(returnSource, buffer + now.getStep(), each));
        }
    }

    public Set<String> tryComplicate(String word, TreeNode tree) {
        List<String> temp = new ArrayList<>();
        Single breLoc = new Single();
        this.getMaybeNodeList(word, tree, breLoc).forEach(each -> this.getDFSWord(temp, "", each));

        return temp.stream().map(e -> word.substring(0, breLoc.loc) + e).collect(Collectors.toSet());
    }

    public List<String> getComplicate(String word) {
        List<String> returnSource = new ArrayList<>();

        Set<String> head = tryComplicate(word, this.readFromHead);
        Set<String> tail = tryComplicate(new StringBuffer(word).reverse().toString(), this.readFromTail)
                .stream()
                .map(e -> new StringBuffer(e).reverse().toString())
                .collect(Collectors.toSet());
        Set<String> temp = new HashSet<>();
        temp.addAll(head);
        temp.retainAll(tail);
        head.removeAll(tail);

        temp.forEach(e -> returnSource.add(e));
        head.forEach(e -> returnSource.add(e));
        return returnSource;
    }

    public static void main(String[] args) {
        FstTree fstTree = new FstTree();

        fstTree.initSearchTree("select");
        fstTree.initSearchTree("from");
        fstTree.getComplicate("f");
    }

}

class TreeNode {
    private Character step;
    private Map<Character, TreeNode> next;

    public TreeNode(Character step) {
        this.step = step;
        this.next = new HashMap<>();
    }

    public Character getStep() {
        return step;
    }

    public Map<Character, TreeNode> getNext() {
        return next;
    }

    public void setNext(Map<Character, TreeNode> next) {
        this.next = next;
    }

    public void setStep(Character step) {
        this.step = step;
    }
}

