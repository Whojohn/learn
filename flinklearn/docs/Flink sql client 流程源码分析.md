# Flink sql client 流程源码分析

## 1. 前期准备

- 环境准备

1. 从官网 fork 指定的代码，并切换到对应的代码版本。
2. 需要使用 `maven 3.2.5 ` 具体见`docs\flinkDev\building.zh.md`中的说明。

3. 不能使用国内仓库加速，部分依赖必须走中央仓库下载。
4. profile 选定特定的 `scala`和`java`版本，勾选`skip-test`，`skip-webui`等操作。

- 存在问题

1. `sql client` 需要依赖`sql parse`和`flink python`的代码。

```
# package flink sqlparse
# 修改sql client 中的 maven 依赖，去除 flink python <scope>test</scope>
```

2. 依赖文件配置

```
# 项目启动参数 program arguments 填入：
embedded --defaults C:\workbench\flink\flink-table\flink-sql-client\src\main\resources\sql-client-defaults.yaml
# jvm 填入
FLINK_CONF_DIR=C:\workbench\flink\flink-table\flink-sql-client\src\main\resources\ 
```

3. 特定依赖导入(非必须，一般是使用了特定连接器才需要配置，如hive，rockesdb 状态端)

```
-l 把连接器相关依赖放入
--l C:\Users\john\Desktop\jar\
！！！注意不要放入 flink table flink blink 相关依赖，不然各种类异常！！！
```

## 2. 类分析

### 2.1 执行流程分析

- SqlClient(flink sql client入口)

```
# 目前只支持 embedded 模式
# 核心方法 start
private void start() {
        if (isEmbedded) {
        ...
       		 // 读取 jar 依赖配置
            // 生成 org.apache.flink.table.client.gateway.local.LocalExecutor
            // ！！！注意这个lcoalexecutor 根本不是 flink 默认的org.apache.flink.client.deployment.executors.LocalExecutor 实现 ！！！
            final Executor executor = new LocalExecutor(options.getDefaults(), jars, libDirs);
            // 初始化 Executor ，注意这里的 Executor 并不是 org.apache.flink.table.delegation.Executor ；还是 client 自定义的一个 Executor
            executor.start();

            // 新建一个session 会话配置, 配置集群环境变量
            // flink Environment 配置
            final Environment sessionEnv = readSessionEnvironment(options.getEnvironment());
            appendPythonConfig(sessionEnv, options.getPythonConfiguration());
            
            final SessionContext context;
            //...  sesssion 初始化

            // Open an new session
            // 查看 LocalExecutor 是否已经存在该会话
            String sessionId = executor.openSession(context);
            try {
                // add shutdown hook
                Runtime.getRuntime()
                        .addShutdownHook(new EmbeddedShutdownThread(sessionId, executor));

                // 至此，初始化完成，打开命令行，接收命令
                openCli(sessionId, executor);
            } finally {
                executor.closeSession(sessionId);
            }
        } else {
            throw new SqlClientException("Gateway mode is not supported yet.");
        }
    }
```

- CliClient

```
CliClient 中有三大最重要的方法：
1. openCli 类入口，用于初始化如：初始化日志等操作。
2. open 方法，在命令行中接收的每一个命令都由 open 方法调用`parse`，然后再执行操作逻辑。除非收到退出信号，不然，open 方法会一直死循环调用
3. callCommand 负责逻辑调用, 最终在该session下的tableenv内创建对应语句。
如：create table 堆栈：
executeSql:315, LocalExecutor (org.apache.flink.table.client.gateway.local)
callDdl:739, CliClient (org.apache.flink.table.client.cli)
callDdl:734, CliClient (org.apache.flink.table.client.cli)
callCommand:330, CliClient (org.apache.flink.table.client.cli)
accept:-1, 408543908 (org.apache.flink.table.client.cli.CliClient$$Lambda$496)
ifPresent:159, Optional (java.util)
open:214, CliClient (org.apache.flink.table.client.cli)
openCli:144, SqlClient (org.apache.flink.table.client)
start:115, SqlClient (org.apache.flink.table.client)
main:201, SqlClient (org.apache.flink.table.client)

如：select 堆栈

<init>:103, MaterializedCollectStreamResult (org.apache.flink.table.client.gateway.local.result)
<init>:129, MaterializedCollectStreamResult (org.apache.flink.table.client.gateway.local.result)
createResult:75, ResultStore (org.apache.flink.table.client.gateway.local)
executeQueryInternal:526, LocalExecutor (org.apache.flink.table.client.gateway.local)
executeQuery:374, LocalExecutor (org.apache.flink.table.client.gateway.local)
callSelect:648, CliClient (org.apache.flink.table.client.cli)
callCommand:323, CliClient (org.apache.flink.table.client.cli)
accept:-1, 408543908 (org.apache.flink.table.client.cli.CliClient$$Lambda$496)
ifPresent:159, Optional (java.util)
open:214, CliClient (org.apache.flink.table.client.cli)
openCli:144, SqlClient (org.apache.flink.table.client)
start:115, SqlClient (org.apache.flink.table.client)
main:201, SqlClient (org.apache.flink.table.client)
```

- callSelect 

> 1. 发起 select 远端 debug 调用，返回`ResultDescriptor`对象用于查询结果。
> 2. 根据不同显示模式和执行模式(注意执行模式是通过`Materialized`变量知道的)。

```
    private void callSelect(SqlCommandCall cmdCall) {
       // 返回一个ResultDescriptor 对象，用于查询数据
        final ResultDescriptor resultDesc;
        try {
        // executeQuery 会调用远程
            resultDesc = executor.executeQuery(sessionId, cmdCall.operands[0]);
        } catch (SqlExecutionException e) {
            printExecutionException(e);
            return;
        }
		
		// 根据不同的配置，配置不同的显示方式
		// table 模式
        if (resultDesc.isTableauMode()) {
            try (CliTableauResultView tableauResultView =
                    new CliTableauResultView(terminal, executor, sessionId, resultDesc)) {
                if (resultDesc.isMaterialized()) {
                    tableauResultView.displayBatchResults();
                } else {
                    tableauResultView.displayStreamResults();
                }
            } catch (SqlExecutionException e) {
                printExecutionException(e);
            }
        } else {
        // changelog 模式
            final CliResultView view;
            if (resultDesc.isMaterialized()) {
                view = new CliTableResultView(this, resultDesc);
            } else {
                view = new CliChangelogResultView(this, resultDesc);
            }

            // enter view
            try {
                view.open();

                // view left
                printInfo(CliStrings.MESSAGE_RESULT_QUIT);
            } catch (SqlExecutionException e) {
                printExecutionException(e);
            }
        }
    }
```

### 2.2 模拟执行流程

> 简化以上流程，核心实例代码如下

```
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.table.client.config.Environment;
import org.apache.flink.table.client.gateway.*;
import org.apache.flink.table.client.gateway.local.LocalExecutor;
import org.apache.flink.table.utils.PrintUtils;
import org.apache.flink.types.Row;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SqlTest {

    /**
     * 批获取数据方式
     *
     * @param sqlExecutor
     * @param sessionId
     * @param resultDescriptor
     * @return
     */
    private static List<Row> waitBatchResults(Executor sqlExecutor, String sessionId, ResultDescriptor resultDescriptor) {
        List<Row> resultRows;
        // take snapshot and make all results in one page
        do {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            TypedResult<Integer> result =
                    sqlExecutor.snapshotResult(
                            sessionId, resultDescriptor.getResultId(), Integer.MAX_VALUE);

            if (result.getType() == TypedResult.ResultType.EOS) {
                resultRows = Collections.emptyList();
                break;
            } else if (result.getType() == TypedResult.ResultType.PAYLOAD) {
                resultRows = sqlExecutor.retrieveResultPage(resultDescriptor.getResultId(), 1);
                break;
            } else {
                // result not retrieved yet
            }
        } while (true);

        return resultRows;
    }

    /**
     * 流获取数据方式
     *
     * @param sqlExecutor
     * @param sessionId
     * @param resultDescriptor
     */
    private static void printStreamResults(Executor sqlExecutor, String sessionId, ResultDescriptor resultDescriptor) {
        // 获取列信息
        // List<TableColumn> columns = resultDescriptor.getResultSchema().getTableColumns();
        AtomicInteger receivedRowCount = new AtomicInteger(0);

        while (true) {
            final TypedResult<List<Tuple2<Boolean, Row>>> result =
                    sqlExecutor.retrieveResultChanges(sessionId, resultDescriptor.getResultId());

            switch (result.getType()) {
                case EMPTY:
                    break;
                case EOS:
                    return;
                case PAYLOAD:
                    List<Tuple2<Boolean, Row>> changes = result.getPayload();
                    for (Tuple2<Boolean, Row> change : changes) {
                        final String[] cols = PrintUtils.rowToString(change.f1);
                        String[] row = new String[cols.length + 1];
                        row[0] = change.f0 ? "+" : "-";
                        System.arraycopy(cols, 0, row, 1, cols.length);
                        System.out.println(row);
                        receivedRowCount.incrementAndGet();
                    }
                    break;
                default:
                    throw new SqlExecutionException("Unknown result type: " + result.getType());
            }
        }
    }


    public static void main(String[] args) throws MalformedURLException {

        // 配置依赖所在路径
        List<URL> jars = new ArrayList<URL>() {{
            add(new URL("file:/C:/Users/john/Desktop/jar/"));
        }};

        // 新建 LocalExecutor
        Executor executor = new LocalExecutor(null, new ArrayList<URL>(), jars);
        // 初始化
        executor.start();

        // 新建 session 会话
        SessionContext context = new SessionContext("test-session", new Environment());
        // 切换到对应 session 会话
        String sessionId = executor.openSession(context);

        // 模拟执行查询
        final String ddlTemplate =
                "CREATE TABLE datagen (\n" +
                        " f_sequence INT,\n" +
                        " f_random INT,\n" +
                        " f_random_str STRING,\n" +
                        " ts AS localtimestamp,\n" +
                        " WATERMARK FOR ts AS ts\n" +
                        ") WITH (\n" +
                        " 'connector' = 'datagen',\n" +
                        " 'rows-per-second'='5',\n" +
                        " 'fields.f_sequence.kind'='sequence',\n" +
                        " 'fields.f_sequence.start'='1',\n" +
                        " 'fields.f_sequence.end'='1000',\n" +
                        " 'fields.f_random.min'='1',\n" +
                        " 'fields.f_random.max'='1000',\n" +
                        " 'fields.f_random_str.length'='10'\n" +
                        ")";

        // Test create table with simple name.
        executor.executeSql(sessionId, ddlTemplate);
        ResultDescriptor resultDescriptor = executor.executeQuery(sessionId, "select * from datagen");
        if (resultDescriptor.isMaterialized()) {
            while (true) {
                List<Row> temp = waitBatchResults(executor, sessionId, resultDescriptor);
                System.out.println(temp);
            }
        } else {
            printStreamResults(executor, sessionId, resultDescriptor);
        }
    }
}

```

## 3. sql client 显示底层

 `stream` 显示通过 ` MaterializedCollectStreamResult`实现，  `batch` 通过`MaterializedCollectBatchResult`实现，类基础关系如下所示。

> 他们的底层都是依赖`org.apache.flink.streaming.experimental.CollectSink`往目标者`org.apache.flink.streaming.experimental.SocketStreamIterator`发送数据实现。

  ![数据流中的检查点障碍](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/MaterializedCollectStreamResult.png?raw=true)

