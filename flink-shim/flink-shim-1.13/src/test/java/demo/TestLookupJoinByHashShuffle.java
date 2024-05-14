package demo;


import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.junit.Test;

public class TestLookupJoinByHashShuffle {

    @Test
    public void testByPlan() throws Exception {
        Configuration conf = new Configuration();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(conf);
        env.disableOperatorChaining();
        StreamTableEnvironment tenv = StreamTableEnvironment.create(env);

        tenv.executeSql("create table a (a int , b int ,proctime as proctime(), primary key(a,b)  NOT ENFORCED )" +
                " with ('connector' ='datagen')");
        tenv.executeSql("create table b (a int ,b int, primary key(a,b)  NOT ENFORCED)" +
                " with ('connector' = 'demo')");
        tenv.executeSql("create table c (a int) with ('connector' = 'blackhole')");

//        StatementSet statementSet = tenv.createStatementSet();
        tenv.executeSql(" explain insert into c select a.`a` from a join b for SYSTEM_TIME as of a.proctime" +
                " " +
                "on (a.a=b.a and  a.b=b.b) ").print();

    }

}
