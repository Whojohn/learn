package utils;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;


public class EnvInital {
    private StreamExecutionEnvironment env;
    private StreamTableEnvironment tenv;

    public void initial() {
        Configuration conf = new Configuration();
        conf.setString("state.backend", "rocksdb");
        conf.setString("state.checkpoints.dir", "file:///Users/whojohn/workspace/learn/flinklearn/tmp");
        conf.setString("state.backend.incremental", "true");
        conf.setString("io.tmp.dirs", "file:///Users/whojohn/workspace/learn/flinklearn/tmp");
        this.env = StreamExecutionEnvironment.getExecutionEnvironment(conf);
        this.tenv = StreamTableEnvironment.create(env);
    }

    public StreamTableEnvironment getTenv() {
        return tenv;
    }

    public StreamExecutionEnvironment getEnv() {
        return env;
    }
}
