# Flink-9-State

## 1. State 概念

reference: https://nightlies.apache.org/flink/flink-docs-release-1.14/docs/ops/state/state_backends/

### 1.1 基础概念

- 什么是 `state` && 为什么需要`state` ?

  `operator`中某些操作依赖上一次的的结果，如：windows，agg 等。这些`operator`依赖的结果保存的数据结构统称为`state`，这些`operator`具有`stateful`的特性。以下是使用`state`的场景(stateful算子使用场景):

> 1.  使用窗口函数，数据去重场景，需要保留一定时间的数据：当天活跃用户，pv，uv，用户首次下单等。
>
> 2.  机器学习，需要不断更新模型的参数。
> 3.  Flink 框架自生的容错，checkpoint 依赖 `state`。

### 1.2 state 持久化

- state 在flink 中如何保存(checkpoint ,state如何持久化)

  Flink 会在`checkpoint`触发的时候，将内存中的state保存到特定的`state`后端。Flink 的容错机制底层是基于`stream replay`(流回放)和`checkpoint`实现的。`checkpoint`就是特殊的`operator state`+`keyed state`保存特定的状态信息。

- state Backend （state 持久化后端）

>    Flink 1.13 重新设计了 state Backend ;旧版 state Backend 有：
>
>    memory： MemoryStateBackend；
>
>    hdfs: FsStateBackend；
>
>    rockdb：RocksDBStateBackend；
>
>    新版hdfs 依旧作为 rockdb 的持久化保存，但是以上的接口都被移除。新版通过配置setCheckpointStorage 控制使用不同的状态后端：memory、hdfs、rocksdb。新版 backend 如下：

1. HashMapStateBackend (默认)
2. EmbeddedRocksDBStateBackend

### 1.3 state 类型

- state 类型

> **注意：新版 Flink 官网已经移除 raw state 这个概念，也没有明确定义 raw state 是啥，flink 包中根本没有相关类。**

|              | Managed State                                                | Raw State      |
| ------------ | ------------------------------------------------------------ | -------------- |
| 状态管理方式 | Flink Runtime托管，自动存储、自动恢复、自动伸缩              | 用户自己管理   |
| 状态数据结构 | ValueState、ListState、ReducingState、AggregatingState、MapState | 字节: byte     |
| 使用场景     | 所有算子                                                     | 用户自定义算子 |

- managed state 类型

  > **`operator ` 与 `keyed ` 最大区别是，keyed state 中的keyedby的`key` 作为key, operation 中的以 partition 作为key(每个sub task 作为一个key)。！！！注意！！！所有状态的key 都是由框架控制，用户无需关系，函数内调用即可。**

  1. operator state

  > 使用场景：
  >
  > 1. 一般都是source、sink 调用 CheckpointedFunction#snapshotState 把连接数据保存在state中。

  2. keyed state

  > 使用场景：
  >
  > 1. **只能使用在 keyedStream 中**，如：windows 中用户自定义数据，如process funciton。


### 1.4 State 数据结构

​		Flink state 提供不同的数据结构，其中 xxxDescriptor 是对应数据结构绑定`state`内部名字和数据类型信息。常用数据结构信息如下：

- State  数据结构

1. ValueState<T>:  键值对存入，key 为keyedby stream 中的key。

2. MapState`<UK, UV>`: 假如keyed stream 中的key 不满足，内部细分需要用到map，使用该方法。

3. ListState<T>:数组存入 。

4. ReducingState<T>:用户定义add 方法，框架累积状态量。

5. AggregatingState<IN, OUT>: 输入，输出不同类型，以累积的方式记录状态量。

### 1.5 operator State

> ​        operator state 一般用于source/sink。source/sink 中必须实现`CheckpointedFunction`，**source**中使用还必须利用`checkpoint lock`保证数据的安全(**SourceFunction方法文档写明必须带有该操作**)。其他`Function`中使用，无需`checkpoint lock`。

- operator state 使用方法

  1. operator state 都通过调用`initializeState` 方法中调用`getOperatorStateStore().getxxx` 的方法初始化`operatorstate`。**任务重启、初始化都会调用`initializeState` 方法，因此通过`context.isRestored()` 可以判定是否为重启，重启的时候读取状态中的值即可。**

  2. operator state 通过 add、clear 方法对state 进行修改，**注意必须先clear然后再add数据，不然历史数据会保留。**

  3. **source 中使用`operator state`必须实现`CheckpointedFunction`且操作时必须用`checkpoint lock`保证操作的原子和数据正确性。**

     

- operator state 重新分发(并行度修改后对state的影响)

​        operator state 与 keyed state 不同，需要考虑**并发度**修改后的使用不同的重新分发模式。常见的`state 重新分发`模式有：**Even-split redistribution，  **Union redistribution**。

 **Even-split redistribution：** getOperatorStateStore().getListState 中重新分发的方法；框架会平均为sub-task 重新分配算子中state，假如state小于并行度，某些`subtask`的state为空。

**Union redistribution**：在恢复、重新分配的时候，所有operator 都会收到全量的state 状态，而不是某一个部分的state。 

- operator state 调用方法

```
org.apache.flink.api.common.state.OperatorStateStore.
getBroadcastState(MapStateDescriptor<K, V> stateDescriptor) // 广播流中应用
getListState(ListStateDescriptor<S> stateDescriptor) //  Even-split redistribution		getUnionListState(ListStateDescriptor<S> stateDescriptor) // Union redistribution
```

- operator state 与 checkpoint 

> operator state 一样用于 source/sink，1.12 flink 引入新的source定义，1.14开始连接器新版改造，新旧source 最大区别就是旧版需要`checkpoint lock`保证数据安全，以确保框架的一致性。

- source demo (operator state 使用方法)

```
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.types.Row;

import java.util.ArrayList;
import java.util.List;

public class TestSource extends RichParallelSourceFunction<Row> implements CheckpointedFunction {
    private boolean label = true;
    private final String sourceName;
    private final List<Row> source;
    private transient ListState<Integer> sourceLoc;
    private final List<Integer> sourceLocBuffer = new ArrayList<Integer>();


    /**
     * 必须传入一个 list 包裹的row 才能模拟数据
     *
     * @param sourceName
     * @param source
     */
    public TestSource(String sourceName, List<Row> source) {
        this.sourceName = sourceName;
        this.source = source;
    }


    @Override
    public void run(SourceContext<Row> ctx) throws Exception {

        int signal = 0;
        while (signal < this.sourceLocBuffer.size()) {
            signal = 0;
            for (int a = 0; a < this.sourceLocBuffer.size(); a++) {
                int nowStep = this.sourceLocBuffer.get(a);
                if (nowStep < this.source.size()) {
                    System.out.println(sourceName + " output:" + this.source.get(nowStep).toString());
                    // checkpoint lock 保证操作原子和数据正确
                    synchronized (ctx.getCheckpointLock()) {
                        ctx.collect(this.source.get(nowStep));
                        Thread.sleep(10000);
                        nowStep += 1;
                        this.sourceLocBuffer.set(a, nowStep);
                    }
                } else {
                    signal += 1;
                }
            }
        }
        Thread.sleep(10000);
        // ctx.collect 方法也有空闲检测，但是没有开放给用户，因此 source 中只能markAsTemporarilyIdle 显式声明告知watermark 此流空闲，生成watermark 不需要等待该流，防止并发流下堵塞
        // 或者通过 WatermarkStrategy.withIdleness 配置，生成全局watermark 时忽略一段时间没有改变的watermark。
        ctx.markAsTemporarilyIdle();

        while (this.label) {
            Thread.sleep(3000);
        }
    }

    @Override
    public void cancel() {
        this.label = false;
    }

    @Override
    public void snapshotState(FunctionSnapshotContext functionSnapshotContext) throws Exception {
        this.sourceLoc.clear();
        for (Integer each : this.sourceLocBuffer) {
            this.sourceLoc.add(each);
        }
    }

    /***
     * checkpoint 相关初始化
     * @param functionInitializationContext
     * @throws Exception
     */
    @Override
    public void initializeState(FunctionInitializationContext functionInitializationContext) throws Exception {
        ListStateDescriptor<Integer> source = new ListStateDescriptor<>("sourceState", Integer.class);
        this.sourceLoc = functionInitializationContext.getOperatorStateStore().getListState(source);
        // 判定是否恢复，假如是从状态量恢复，从状态量中读取数据
        if (functionInitializationContext.isRestored()) {
            for (Integer each : this.sourceLoc.get()) {
                sourceLocBuffer.add(each);
            }
        } else {
            this.sourceLocBuffer.add(0);
        }
    }
}
```



```
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestProcessFunction extends ProcessFunction<Tuple2<Long, Long>, Tuple2<Long, Long>> {
    // 用于内部标记 fire 次数，防止日志打印乱序，影响判定
    private Integer cou;
    // 注意本地 state 必须以transient修饰
    private transient ValueState<Tuple2<Long, Long>> sum;


    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        cou = 0;
        ValueStateDescriptor<Tuple2<Long, Long>> descriptor =
                new ValueStateDescriptor<>(
                        "average", // state 逻辑名字
                        TypeInformation.of(new TypeHint<Tuple2<Long, Long>>() {
                        }) // 数据类型信息
                );
        // 绑定本地 state
        sum = getRuntimeContext().getState(descriptor);
    }

    /**
     * timer 触发时候处理函数
     *
     * @param timestamp 触发 timer fire 的时间戳
     * @param ctx       OnTimerContext 对象，能够获取触发器相关信息如timestamp，fire 时间等信息。
     * @param out       fire 时间段函数发送的数据(因为fire是通过processElement触发的，因此能够捕获当时的数据)
     * @throws Exception 异常
     */
    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<Tuple2<Long, Long>> out) throws Exception {
        super.onTimer(timestamp, ctx, out);
        cou += 1;
        System.out.println("fire timer: " + timestamp + "  " + cou);
    }

    /**
     * 以 F1 进行分组，每一条数据进行累计
     *
     * @param value 输入值
     * @param ctx   context
     * @param out   输出值
     * @throws Exception 异常
     */
    @Override
    public void processElement(Tuple2<Long, Long> value, Context ctx, Collector<Tuple2<Long, Long>> out) throws Exception {
        // 触发timer 执行。注意没有timestamp的时候会返回空
        if (ctx.timestamp() != null) ctx.timerService().registerProcessingTimeTimer(ctx.timestamp() + 300);
        // 处理逻辑
        System.out.println("source in state is:" + sum.value());
        Tuple2<Long, Long> temp = sum.value();
        temp = temp == null ? new Tuple2<>() : temp;
        // 以f1 进行分组，进行 count 累加
        temp.f0 = (temp.f0 == null ? value.f1 : temp.f0);
        temp.f1 = (temp.f1 == null ? 0 : temp.f1) + 1;
        sum.clear();
        sum.update(temp);
        out.collect(temp);
    }

    public static List<Row> buildSource() {
        List<Long> timestamp = Arrays.asList(1632981239000L,
                1632981242000L,
                1632981244000L,
                1632981231000L,
                1632981249000L,
                1632981259000L,
                1632981249000L);
        List<Long> message = Arrays.asList(6L,
                6L,
                2L,
                2L,
                3L,
                4L,
                6L);
        List<Row> source = new ArrayList<>();
        for (int a = 0; a < timestamp.size(); a++) {
            Row each = new Row(2);
            each.setField(0, timestamp.get(a));
            each.setField(1, message.get(a));
            source.add(each);
        }
        return source;
    }

    /**
     * keyed state 累计演示
     *
     * @param args 启动参数
     * @throws Exception 异常
     */
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = TestUtil.iniEnv(1);
        env.addSource(new TestSource("TestProcessFunction", buildSource()))
                .map(e -> {
                    Tuple2<Long, Long> temp = new Tuple2<Long, Long>();
                    temp.f0 = (Long) e.getField(0);
                    temp.f1 = (Long) e.getField(1);
                    return temp;
                }).returns(new TypeHint<Tuple2<Long, Long>>() {
        })
                .keyBy(value -> value.getField(1))
                .process(new TestProcessFunction())
                .print();
        env.execute();
    }
}
```



### 1.6 Keyed State

> ​        keyed state 一样可以通过 implements CheckpointedFunction 的形式控制批量刷新state，以下例子略去该优化。

```
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestProcessFunction extends ProcessFunction<Tuple2<Long, Long>, Tuple2<Long, Long>> {
    // 用于内部标记 fire 次数，防止日志打印乱序，影响判定
    private Integer cou;
    // 注意本地 state 必须以transient修饰
    private transient ValueState<Tuple2<Long, Long>> sum;


    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        cou = 0;
        ValueStateDescriptor<Tuple2<Long, Long>> descriptor =
                new ValueStateDescriptor<>(
                        "average", // state 逻辑名字
                        TypeInformation.of(new TypeHint<Tuple2<Long, Long>>() {
                        }) // 数据类型信息
                );
        // 绑定本地 state
        sum = getRuntimeContext().getState(descriptor);
    }

    /**
     * timer 触发时候处理函数
     *
     * @param timestamp 触发 timer fire 的时间戳
     * @param ctx       OnTimerContext 对象，能够获取触发器相关信息如timestamp，fire 时间等信息。
     * @param out       fire 时间段函数发送的数据(因为fire是通过processElement触发的，因此能够捕获当时的数据)
     * @throws Exception 异常
     */
    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<Tuple2<Long, Long>> out) throws Exception {
        super.onTimer(timestamp, ctx, out);
        cou += 1;
        System.out.println("fire timer: " + timestamp + "  " + cou);
    }

    /**
     * 以 F1 进行分组，每一条数据进行累计
     *
     * @param value 输入值
     * @param ctx   context
     * @param out   输出值
     * @throws Exception 异常
     */
    @Override
    public void processElement(Tuple2<Long, Long> value, Context ctx, Collector<Tuple2<Long, Long>> out) throws Exception {
        // 触发timer 执行。注意没有timestamp的时候会返回空
        if (ctx.timestamp() != null) ctx.timerService().registerProcessingTimeTimer(ctx.timestamp() + 300);
        // 处理逻辑
        System.out.println("source in state is:" + sum.value());
        Tuple2<Long, Long> temp = sum.value();
        temp = temp == null ? new Tuple2<>() : temp;
        // 以f1 进行分组，进行 count 累加
        temp.f0 = (temp.f0 == null ? value.f1 : temp.f0);
        temp.f1 = (temp.f1 == null ? 0 : temp.f1) + 1;
        sum.clear();
        sum.update(temp);
        out.collect(temp);
    }

    public static List<Row> buildSource() {
        List<Long> timestamp = Arrays.asList(1632981239000L,
                1632981242000L,
                1632981244000L,
                1632981231000L,
                1632981249000L,
                1632981259000L,
                1632981249000L);
        List<Long> message = Arrays.asList(6L,
                6L,
                2L,
                2L,
                3L,
                4L,
                6L);
        List<Row> source = new ArrayList<>();
        for (int a = 0; a < timestamp.size(); a++) {
            Row each = new Row(2);
            each.setField(0, timestamp.get(a));
            each.setField(1, message.get(a));
            source.add(each);
        }
        return source;
    }

    /**
     * keyed state 累计演示
     *
     * @param args 启动参数
     * @throws Exception 异常
     */
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = TestUtil.iniEnv(1);
        env.addSource(new TestSource("TestProcessFunction", buildSource()))
                .map(e -> {
                    Tuple2<Long, Long> temp = new Tuple2<Long, Long>();
                    temp.f0 = (Long) e.getField(0);
                    temp.f1 = (Long) e.getField(1);
                    return temp;
                }).returns(new TypeHint<Tuple2<Long, Long>>() {
        })
                .keyBy(value -> value.getField(1))
                .process(new TestProcessFunction())
                .print();
        env.execute();
    }
}
```

## 2. State TTL & State 清除

> ​       1.14 版本至今也只支持`process time` 的`State TTL`。`State TTL`是懒删除，只有读取、修改的时候才会检测是否删除，然后标记删除，对应后端执行内部`TTL`删除逻辑(**无法删除不再访问的`key`**)。
>
> ​      假如需要主动删除(**删除不再访问的key**)，需要自定义 `timer` 定期删除状态(timer大量调用时，会引发性能问题)。`Sql` 中可以使用`setIdleStateRetentionTime`实现。

### 2.1 State TTL

- TTL 作用

​    长期使用`state`会导致state过大，一般业务上能清除一些历史`state`如：昨天的`pv`,`uv`等。通过配置TTL 就能实现`state`清除。

- **TTL 使用须知**

1. 状态后端把 `TTL` 的时间戳和`state`保存在一起(`TTL`会消耗额外的空间)。
2. **TTL只支持`Process time`**;
3. 之前没有使用`TTL`的状态不能使用`TTL`，使用后会导致`StateMigrationException`异常。(重写 TTL 逻辑加一个 null 处理逻辑应该可以解决)
4. `TTL`与`checkpoint` \ `savepoint`没有任何联系。
5. `Map value`使用`TTL`必须确保序列化方法支持`value`存在`null`否则抛出`NullableSerializer`异常。

- TTL 清除策略（根据不同后端配置不同策略）

1. savepoint 生成时删除： cleanupFullSnapshot （RocksDB 不可用）

2. 增量删除：

   >  cleanupIncrementally (只对 memory 后端有效)
   >
   >  假如 memory 后端配置为同步写入时候，由于同步写入禁止并发，会导致内存使用增长(会保留所有数据，删除无效)。

3. RocksDB 专用删除：cleanupInRocksdbCompactFilter

- TTL使用方法

```
 StateTtlConfig ttlConfig = StateTtlConfig
                // ttl 时间长度;假如上次时间戳+ttl > 当前时间戳，该状态被标记清除
                .newBuilder(Time.seconds(1))
                // ttl 更新策略：Disable，OnCreateAndWrite，OnReadAndWrite;注意使用 disable 会使 ttl 失效
                .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
                // 对已过期但未清理的状态处理策略：ReturnExpiredIfNotCleanedUp，NeverReturnExpired；
                // ReturnExpiredIfNotCleanedUp 会禁用 state 读缓存
                // NeverReturnExpired 会禁用 state 读写缓存
                .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
                .build();
```

#### 2.1.1 State TTL 内部实现机制

reference: https://cloud.tencent.com/developer/article/1452844

​       对应的 `state` 会被 `AbstractTtlDecorator` 类包装在一起，`AbstractTtlDecorator` 提供了 `TTL` 逻辑操作，控制每一次读，调用`TTL`逻辑。TTL 清除核心方法是`getWrappedWithTtlCheckAndUpdate `。

- 以 TtlMapState 为例说明源码工作过程

TtlMapState.get -> TtlMapState.getWrapped  -> getWrappedWithTtlCheckAndUpdate 判定是否清除

```
	@Override
    public UV get(UK key) throws Exception {
        TtlValue<UV> ttlValue = getWrapped(key);
        return ttlValue == null ? null : ttlValue.getUserValue();
    }


    private TtlValue<UV> getWrapped(UK key) throws Exception {
        accessCallback.run(); //是一个回调对象，增删改查操作之前都需要执行accessCallback.run()方法。如果启用了增量清理策略，该Runnable会通过在状态数据上维护一个全局迭代器向前清理过期数据。如果未启用增量清理策略，accessCallback为空。TtlStateFactory.registerTtlIncrementalCleanupCallback 工厂初始化该变量。
        return getWrappedWithTtlCheckAndUpdate(
                () -> original.get(key), v -> original.put(key, v), () -> original.remove(key)); // 判定当前数据是否应该被清除逻辑
    }
```

```
# getWrappedWithTtlCheckAndUpdate  核心处理逻辑
        TtlValue<V> ttlValue = getter.get();
        if (ttlValue == null) {
            return null;
        } else if (expired(ttlValue)) {
            stateClear.run(); // 清除状态
            if (!returnExpired) {
                return null;
            }
        } else if (updateTsOnRead) {
            updater.accept(rewrapWithNewTs(ttlValue));
        }
        return ttlValue;
```


### 2.2 手动主动清除 state 例子



```
 		@Override
    	public void processElement(byte[] bytes, Context context, Collector<String> collector) throws Exception 	{

        try {
            // 30 秒触发一次清除，可以定义更复杂样例，这里为了说明逻辑
            context.timerService().registerProcessingTimeTimer(System.currentTimeMillis() + 30000);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

    }

    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {
        // 定时会触发该操作
        state.clear();
    }
```

## 3. State 查询

### 3.1 Queryable State 接口

> 通过queryable state 可以实时的监控state ，**但是这不是一个稳定的接口，1.13.2 官网demo 一直无法跑起来，无论是使用stream 声明方式，还是 state 声明方式。**并且该方法只能查询，无法修改状态量。


- 集群配置打开可查询状态

> queryable-state.enable ： true

- 声明状态可查询方式：

  ·1. Stream 中声明(**声明该流可以查询后不能继续进行任何算子处理，该方法也不支持ListState**)

  ```
  stream.keyBy(value -> value.f0).asQueryableState("query-name")
  ```

         2. StateDescriptor 中声明（没有任何限制）

```
ValueStateDescriptor<Tuple2<Long, Long>> descriptor =
        new ValueStateDescriptor<>(
                "average", // the state name
                TypeInformation.of(new TypeHint<Tuple2<Long, Long>>() {})); // type information
descriptor.setQueryable("query-name"); // queryable state name
```

- 查询代码

```
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
```



### 3.2 State Processor API

> State Processor API 无法实时读取 `state`，但是可以以`batch`的方式读取、修改 `checkpoint`,`savapoint`。







