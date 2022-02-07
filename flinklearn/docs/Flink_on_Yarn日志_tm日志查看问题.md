# Flink_on_Yarn日志_tm日志查看问题

问题：

1. Yarn 历史记录从界面上只有`jobmanager`的历史记录，没有`taskmanger`日志，不方便进行排查问题。

**任务结束时**

![任务结束时](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/Flink_yarn_job_not_running_tm.png?raw=true)

![任务结束时_attempt](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/Flink_yarn_job_not_running_tm_attempt.png?raw=true)


**任务运行中，能够查看 tm jm 日志**

![任务运行时_attempt](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/Flink_yarn_job_running_tm_attempt.png?raw=true)

## 问题分析

>  reference :
>
> （yarn rest api）https://hadoop.apache.org/docs/stable/hadoop-yarn/hadoop-yarn-site/ResourceManagerRest.html
>
> （yarn logs cli 样例) https://docs.cloudera.com/HDPDocuments/HDP3/HDP-3.1.0/data-operating-system/content/use_the_yarn_cli_to_view_logs_for_running_applications.html
>
> 



1. `yarn` 中任何容器都会有日志，除非完成后立刻清理日志。
2. `yarn logs -applicationId application_1644158107991_0001   -log_files all` 命令进行查看容器日志，发现虽然界面上没有日志，但是 `log api`仍然能找到日志。
3. 翻阅`yarn rest api`没有找到可用的`api`（`rest api`）信息只有`am`的信息。
4. 找到日志聚合的`hdfs`处，发现`tm`，`jm`日志是写入到一起，符合第一点(只要`yarn`启动`container`就会有日志)。

### 需要解决的问题：

1. 一共启动了多少个 `container`。
2. 这些`container`的日志如何读取。(日志聚合会挤在一起，如何正常查看日志)

## 解决办法

### 问题1 解决办法

- 方案1 `rest api`尝试遍历

  >  container 是有规律的，按照+1 的样式进行自增，可以通过+1 遍历的方式尝试遍历所有 container。
  >
  > 尝试的方式可以通过 RestApi 进行测试。

  ```
  curl http://test:8088/ws/v1/cluster/apps/application_1644158107991_0001/appattempts
  
  ...
  <containerId>container_1644158107991_0001_01_000001</containerId>
  <nodeHttpAddress>test:8042</nodeHttpAddress>
  <nodeId>test:41285</nodeId>
  <logsLink>http://test:8042/node/containerlogs/container_1644158107991_0001_01_000001/hadoop</logsLink>
  ...
  
  提取 container 和 logslink 尝试去获得所有的container
  curl http://test:8042/node/containerlogs/container_1644158107991_0001_01_000001/hadoop
  curl http://test:8042/node/containerlogs/container_1644158107991_0001_01_000002/hadoop
  # 3 返回 No logs available for container 结束遍历
  curl http://test:8042/node/containerlogs/container_1644158107991_0001_01_000003/hadoop
  
  ```

- 方案二 `java api` 的方式获取所有`container`

  ```
  /bin/yarn logs -applicationId application_1644158107991_0001  -show_application_log_info
  ```

  ```
  代码的方式
  
   System.setProperty("HADOOP_USER_NAME", "hadoop");
          String appId = "application_1644158107991_0001";
          Configuration conf = new YarnConfiguration();
          String[] source = {"-applicationId", appId, "-show_application_log_info"};
          conf.addResource(new Path("C:\\Users\\john\\Desktop\\yarn-site.xml"));
          
          YarnClient yarnClient = createYarnClient();
          yarnClient.init(conf);
          yarnClient.start();
          ApplicationReport ap = yarnClient.getApplicationReport(ApplicationId.fromString(appId));
  
          LogAggregationFileControllerFactory factory = new LogAggregationFileControllerFactory(conf);
          ContainerLogsRequest request = new ContainerLogsRequest(ApplicationId.fromString(appId),
                  isApplicationFinished(ap.getYarnApplicationState()), ap.getUser(), null, null,
                  null, null, new HashSet<String>() {{
              add("ALL");
          }}, Integer.MAX_VALUE, null);
  
  
          if (ap.getYarnApplicationState() == YarnApplicationState.RUNNING) {
              // 运行中需要从 attempts 取，直接做一个跳转即可，写代码比较繁琐  具体繁琐程度如下
  //            List<ContainerReport> reports = new ArrayList<ContainerReport>();
  //
  //            List<ApplicationAttemptReport> attempts =
  //                    yarnClient.getApplicationAttempts(ApplicationId.fromString(appId));
  //            Map<ContainerId, ContainerReport> containerMap = new TreeMap<
  //                    ContainerId, ContainerReport>();
  //            for (ApplicationAttemptReport attempt : attempts) {
  //                List<ContainerReport> containers = yarnClient.getContainers(
  //                        attempt.getApplicationAttemptId());
  //                for (ContainerReport container : containers) {
  //                    if (!containerMap.containsKey(container.getContainerId())) {
  //                        containerMap.put(container.getContainerId(), container);
  //                    }
  //                }
  //            }
  //            reports.addAll(containerMap.values());
          } else {
          // 完成的情况下获取
              LogAggregationFileController t = factory.getFileControllerForRead(ApplicationId.fromString(appId), ap.getUser());
              List<ContainerLogMeta> containersLogMeta = t.readAggregatedLogsMeta(request)     ;
              for(ContainerLogMeta logMeta : containersLogMeta) {
                  System.out.println(logMeta.getContainerId());
              }
          }
          yarnClient.close();
  ```

### 问题2 解决办法

- 方案1 `rest api`

```
http://test:8042/node/containerlogs/container_1644158107991_0001_01_000001/hadoop
```

- 方案二 `java api`

```
  for (ContainerLogMeta logMeta : containersLogMeta) {
                System.out.println(logMeta.getContainerId());
                ContainerLogsRequest newOptions = new ContainerLogsRequest(request);
                // 打印日志
                newOptions.setContainerId(logMeta.getContainerId());
                newOptions.setLogTypes(logMeta.getContainerLogMeta().stream().map(e -> e.getFileName()).collect(Collectors.toSet()));
                OutputStream os = new ByteArrayOutputStream();
                t.readAggregatedLogs(newOptions, os);
                System.out.println(os);
            }
```



## Demo

```
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.cli.LogsCLI;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.logaggregation.ContainerLogMeta;
import org.apache.hadoop.yarn.logaggregation.ContainerLogsRequest;
import org.apache.hadoop.yarn.logaggregation.filecontroller.LogAggregationFileController;
import org.apache.hadoop.yarn.logaggregation.filecontroller.LogAggregationFileControllerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.hadoop.yarn.client.api.YarnClient.createYarnClient;


public class YarnDemo {
    public static Configuration YARN_CONF = new YarnConfiguration();

    static {
        YARN_CONF.addResource(new Path("C:\\Users\\john\\Desktop\\yarn-site.xml"));

    }

    private static boolean isApplicationFinished(YarnApplicationState appState) {
        return appState == YarnApplicationState.FINISHED
                || appState == YarnApplicationState.FAILED
                || appState == YarnApplicationState.KILLED;
    }

    /**
     * yarn logs cli 调用
     *
     * @param params 传入 cli 控制参数
     * @throws Exception 抛出异常
     */
    public static void yarnCli(String[] params) throws Exception {
        LogsCLI logDumper = new LogsCLI();
        logDumper.setConf(YARN_CONF);
        int exitCode = logDumper.run(params);
    }

    /**
     * 打印 appid 下创建的 container, 打印所有 container 日志；注意只打印非运行中的日志
     *
     * @param appId appid
     * @throws IOException   io异常
     * @throws YarnException yarn异常
     */
    public static void logReadWithRunning(String appId) throws IOException, YarnException {

        // 通过 yarn client 获取作业信息
        YarnClient yarnClient = createYarnClient();
        yarnClient.init(YARN_CONF);
        yarnClient.start();

        ApplicationId applicationId = ApplicationId.fromString(appId);
        ApplicationReport ap = yarnClient.getApplicationReport(applicationId);

        // 日志聚合方法工厂，用于读取聚合日志
        LogAggregationFileControllerFactory factory = new LogAggregationFileControllerFactory(YARN_CONF);
        ContainerLogsRequest request = new ContainerLogsRequest(applicationId,
                isApplicationFinished(ap.getYarnApplicationState()), ap.getUser(), null, null,
                null, null, new HashSet<String>() {{
            add("ALL");
        }}, Integer.MAX_VALUE, null);


        if (ap.getYarnApplicationState() == YarnApplicationState.RUNNING) {
            // 运行中需要从 attempts 取
            // 写代码比较繁琐  具体繁琐程度如下 ... 更详细的代码不追踪了
//            List<ContainerReport> reports = new ArrayList<ContainerReport>();
//
//            List<ApplicationAttemptReport> attempts =
//                    yarnClient.getApplicationAttempts(ApplicationId.fromString(appId));
//            Map<ContainerId, ContainerReport> containerMap = new TreeMap<
//                    ContainerId, ContainerReport>();
//            for (ApplicationAttemptReport attempt : attempts) {
//                List<ContainerReport> containers = yarnClient.getContainers(
//                        attempt.getApplicationAttemptId());
//                for (ContainerReport container : containers) {
//                    if (!containerMap.containsKey(container.getContainerId())) {
//                        containerMap.put(container.getContainerId(), container);
//                    }
//                }
//            }
//            reports.addAll(containerMap.values());
        } else {
            // 运行中的容器这样取会引发 io 异常
            LogAggregationFileController t = factory.getFileControllerForRead(applicationId, ap.getUser());
            List<ContainerLogMeta> containersLogMeta = t.readAggregatedLogsMeta(request);
            for (ContainerLogMeta logMeta : containersLogMeta) {
                System.out.println(logMeta.getContainerId());
                ContainerLogsRequest newOptions = new ContainerLogsRequest(request);
                newOptions.setContainerId(logMeta.getContainerId());
                newOptions.setLogTypes(logMeta.getContainerLogMeta().stream().map(e -> e.getFileName()).collect(Collectors.toSet()));
                OutputStream os = new ByteArrayOutputStream();
                t.readAggregatedLogs(newOptions, os);
                System.out.println(os);
            }
        }
        yarnClient.close();
    }


    public static void main(String[] args) throws Exception {
        System.setProperty("HADOOP_USER_NAME", "hadoop");
        String appId = "application_1644158107991_0002";

        // yarn log cli 实现方式，用于源码分析 ，打印 appid 下的所有 container 
        String[] listContainers = {"-applicationId", appId, "-show_application_log_info"};
        yarnCli(listContainers);
//         打印 appid 下特定 container 的日志
//        String[] fetchContainerLog = {"-applicationId", appId, "-containerId", "container_1644158107991_0001_01_000002"};
//        yarnCli(fetchContainerLog);
        logReadWithRunning(appId);

    }
}

```

