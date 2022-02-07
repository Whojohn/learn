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
