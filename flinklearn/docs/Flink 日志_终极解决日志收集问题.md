# Flink(spark等计算集群日志采集) 日志第二弹(终极解决日志收集问题)

问题1 ：

1. 无论是`Flink on yarn` 还是`on k8s`，都会存在一个问题，`crash`后的日志不方便查看。

   > yarn 聚合是不包含重启`container`日志，`k8s` pod 重启也是类似(刨除共享存储)。
   >
   > 可以通过 Push 到 es 解决，只要 k8s 外挂log 目录， yarn 采集 tmp 下目录也可以实现类似的功能(crash 掉的文件也能重启)。

2. 把日志采集到`ES` ，如何区分`tm`，`jm`等角色日志，如何进行集群的唯一标识(作业标识做不了，对于 session 集群来说)。

   > 方案1：
   >
   > Filebeat -> es
   >
   > 1. Filebeat 采集的时候进行文件名处理，在推送的日志前做好逻辑。(无需任何改动，唯一需要修改的是log4j 引入 appName 进行标识)
   >
   > 方案2：
   >
   > 修改log4j 配置 -> Filebeat -> Es
   >
   > Log4j 自定义`LogEventPatternConverter`方式，添加：集群标识，节点类型标识，节点唯一标识。(好处是解耦了filebeat 依赖，数据源解决问题，推送到任何源都含有标识)

   **本文采用方案二**

   ```
   整体业务流程
   
   ## LogEventPatternConverter 添加信息
   -> k8s 挂载日志目录
   --> filebeat 扫描固定目录 
   ---> push es
   ----> 用户按照使用的集群标识，对集群的日志搜索
   ```

   ## LogEventPatternConverter 

   ### 自定义 LogEventPatternConverter 规则

   ```
   
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
   
   ```

### log4j 配置修改

```
第一个填入 集群标识，第二引用自定义 LogEventPatternConverter  后面的信息保持原状
appender.main.layout.pattern = test_cluster %fl %d{yyyy-MM-dd HH:mm:ss,SSS} %-5p %-60c %x - %m%n

```

### 日志打印效果

```
test_cluster JobManager   standalonesession-1-cdh01 2022-03-10 14:24:45,337 INFO  org.apache.flink.runtime.rpc.akka.AkkaRpcServiceUtils        [] - Trying to staxxxx
```

   