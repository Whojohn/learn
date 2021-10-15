package queryable_state_demo;

import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.queryablestate.client.QueryableStateClient;

import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class StateQueryByClient {
    public static void main(String[] args) throws UnknownHostException, InterruptedException, ExecutionException {
        String tmHostname = "test";
        int proxyPort = 9069;
        JobID jobID = JobID.fromHexString("c5f1d83590b0d7f092af5f88d27f294a");

        QueryableStateClient client = new QueryableStateClient(tmHostname, proxyPort);

        // the state descriptor of the state to be fetched.
        ValueStateDescriptor<Tuple2<Long, Long>> descriptor =
                new ValueStateDescriptor<>(
                        "query-name",
                        TypeInformation.of(new TypeHint<Tuple2<Long, Long>>() {
                        })
                );

        CompletableFuture<ValueState<Tuple2<Long, Long>>> resultFuture =
                client.getKvState(jobID, "query-name", 6L, BasicTypeInfo.LONG_TYPE_INFO, descriptor);

        // now handle the returned value
        resultFuture.thenAccept(response -> {
            try {
                Tuple2<Long, Long> res = response.value();
                System.out.println("aaa");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        resultFuture.get();


    }
}
