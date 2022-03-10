package log;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;


@Plugin(name = "FlinkLog", category = PatternConverter.CATEGORY)
@ConverterKeys({"fl", "flinkLog"})
public class SpecParrten extends LogEventPatternConverter {
    private final static Map<String, String> dic = new HashMap<String, String>() {{
        put("taskexecutor", "TaskManager");
        put("zookeeper", "FlinkZooKeeperQuorumPeer");
        put("historyserver", "HistoryServer");
        put("standalonesession", "JobManager");
        put("standalonejob", "JobManager");
        put("kubernetes-session", "JobManager");
        put("kubernetes-application", "JobManager");
        put("kubernetes-taskmanager", "TaskManager");
    }};

    private String type = "NeedIni";
    private String nodeId = "NeedIni";

    private SpecParrten(String name, String style) {
        super(name, style);

    }

    public static SpecParrten newInstance(final String[] options) {
        return new SpecParrten("FlinkLog", "flinkLog");
    }

    @Override
    public void format(LogEvent logEvent, StringBuilder stringBuilder) {
        if (type.equals("NeedIni") || nodeId.equals("NeedIni")) {
            synchronized (this) {
                String filename = Paths.get(System.getProperty("log.file")).getFileName().toString();
                for (Map.Entry<String, String> each : dic.entrySet()) {
                    if (filename.contains(each.getKey())) {
                        this.type = each.getValue();
                        filename.indexOf(each.getKey());
                        this.nodeId = filename.substring(filename.indexOf(each.getKey()), filename.length() - 4);
                    }
                }
                if (this.type.equals("NeedIni")) {
                    this.type = "unknow";
                    this.nodeId = "unknow";
                }
            }
        }
        stringBuilder.append(this.type);
        stringBuilder.append(" ");
        stringBuilder.append(this.nodeId);
    }
}
