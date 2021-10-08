import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.runtime.state.storage.JobManagerCheckpointStorage;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Properties;

public class TestUtil {

    public static StreamExecutionEnvironment iniEnv(int parall) {
        // 解决 hadoop simple auth
        Properties properties = System.getProperties();
        properties.setProperty("HADOOP_USER_NAME", "hadoop");

        Configuration conf = new Configuration();
        conf.setBoolean("rest.flamegraph.enabled", true);
        conf.setInteger(RestOptions.PORT, 8082);

        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment(parall, conf);

        env.enableCheckpointing(10000);
        // 新版api 标识了 memory 状态量会被移除的信息，改用以下方式进行调用
        env.setStateBackend(new HashMapStateBackend());
        env.getCheckpointConfig().setCheckpointStorage(new JobManagerCheckpointStorage());
        return env;
    }
}
