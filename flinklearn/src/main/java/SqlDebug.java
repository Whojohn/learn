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

/**
 * flink sql debug 实现demo
 * 需要配置：
 * 1. 环境变量： FLINK_CONF_DIR=C:\workbench\flink\flink-table\flink-sql-client\src\main\resources
 * 2. 连接器依赖： dependenceJarsPath 指定 jar 存放路径
 */
public class SqlDebug {
    static String dependenceJarsPath = "file:/C:/Users/john/Desktop/jar/";

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
            add(new URL(dependenceJarsPath));
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
