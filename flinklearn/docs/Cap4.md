# Flink-4-Operators 

## 1. DataStream 中的 Operators

### 1.1 DataStream 类与子类包含的Operators

      Flink source就是 `DataStream`的源头，   `operator` 操作会把输入的`DataStreams` 转化为一个或多个其他`stream`。其中`DataStream` 子类有： `SingleOutputStreamOperator`，`IterativeStream`， `KeyedStream`，`DataStreamSource` 等。`operator`  通过 `stream` 的函数进行调用。

**源码 Stream路径 ：org.apache.flink.streaming.api.datastream，以下列举常见 stream **
![stream流源码路径](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/cap4-stream.png?raw=true)

**stream类**
![stream分类](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/cap4-stream(%E6%8A%BD%E8%B1%A1%E6%A6%82%E5%BF%B5%E7%9A%84stream).svg?raw=true)

**operator总览**
![operator总览](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/cap4-operator.svg?raw=true)

- DataStream

    一个`DataStream` 实例内的每一个元素的数据类型必须一致。核心方法：

> **流合并**
>
> union ：多个同类型 stream 合并。
>
> connect：**只能合并两个流，流的格式可以不一样。一般其中一条流相当于信号，用于控制，算法等场景。**
>
> **算子数据交换策略：**
>
> forwark：算子间数据没有交换，算子链的工作模式。
>
> rebalance：Round-ribon算法，轮询发送到下游。(**默认不同并行度算子的数据交换交换方式。StreamGraph.addEdgeInternal 方法控制**)
>
> rescale：将数据均匀的轮询到下游，与`reblance` 区别是只在同slot 下轮询发送。
>
> shuffle：随机发送到下游。
>
> broadcast：广播，将数据广播到下游所有分区，所有分区接收一样的数据。
>
> partitionCustom：用户自定义数据交换方式
>
> global： 所有数据发送到下游的某一个实例上
>
> **其他常用operator：**
>
> map flatMap process filter

- SingleOutputStreamOperator

> 预定义输出类型的流，子类有：`DataStreamSource`， `IterativeStream`。

- IterativeStream

> 通过将一个运算符的输出重定向到某个先前的运算符。

- KeyedStream

> 将流以 hash 的方式进行分区，调用windows 函数必须是已分区的数据。
>
> **常用operator:**
>
> agg，sum，min，max ，TimeWindow，CountWindow，SessionWindow， reduce。

- WindowedStream

>**常用operator:**
>windowAll, window，reduce

- ConnectedStreams

> connect 连接的两个数据类型不一样的流产生的流

### 1.1.2 自定义操作函数使用(ProcessFunction及其子类应用)

```

```

> 对于没有预定义操作的函数，必须传入lambda 表达式式或者实体类。其中自定义操作函数有 xxxFunction 接口，RichxxxFunction 接口两种。其中 Rich 与 普通实现相比增加以下方法：
>
> - `open()`方法：Flink在算子调用前会执行一次，用于初始化工作。
> - `close()`方法：Flink在算子最后一次调用结束后执行这个方法，用于释放资源。
> - `getRuntimeContext()`方法：获取运行时上下文。每个并行的算子子任务都有一个运行时上下文，上下文记录了这个算子运行过程中的一些信息，包括算子当前的并行度、算子子任务序号、广播数据、累加器、监控数据。最重要的是，我们可以从上下文里获取**状态数据**。

使用样例(**假如使用lambda 表达式必须使用return 函数声明返回数据类型，因为java 泛型存在类型擦除**)

 ```
.reduce(new ReduceFunction<Tuple2<StringValue, Integer>>() {
                    @Override
                    public Tuple2<StringValue, Integer> reduce(Tuple2<StringValue, Integer> value1, Tuple2<StringValue, Integer> value2) throws Exception {
                        return new Tuple2(value1.f0, value1.f1 + value2.f1);
                    }
                })
 ```

### 1.2 ProcessFunction 

> reference:
>
> https://ci.apache.org/projects/flink/flink-docs-master/docs/dev/datastream/operators/process_function/#the-keyedprocessfunction

- ProcessFunction 用于一般 `operator`无法满足的场合，是`1.1`中的operator 的底层，它能够处理：

1. event （流中的数据）
2. 状态（在keyed stream中的容错、一致性），状态的使用放在状态中。
3. timers（**在keyed stream**中的process \ envent time 触发特殊处理; timer 必须在 keyed stream 中使用）

- ProcessFunction 子类以及变种

> 单流子类：
>
> 1. ProcessFunction ；不使用timer 的情况下，可以是非 keyedStream
> 2. KeyedProcessFunction ；用于KeyedStream，keyBy之后的流处理
>
> 窗口相关子类：
>
> 1. ProcessWindowFunction 
>
> 多流子类：
>
> 1. CoProcessFunction 用于connect连接的流
> 2. ProcessJoinFunction 用于join流操作
> 3. BroadcastProcessFunction 用于广播
> 4. KeyedBroadcastProcessFunction keyBy之后的广播
>
> 

#### 1.2.1 Timer & ProcessFunction 使用方法

> **注意必须要区分 Timer 只能使用 Timestamp ，与 watermark 没有直接联系。也不会影响 Timestamp 的生成，processFunction 中关于时间的操作都是控制 Timer。ProcessFunction 可以读取 Timestamp 和 WaterMark 信息**

- Timer 使用场景场景(ProcessFunction 的作用)

1. 定期发送统计信息，告警触发。
2. 机器学习。
3. 自定义状态量；用timer 进行清楚过期状态量；双流join 同时更新状态量。

- Timer 用于定时触发特定行为，必须满足以下两点：

1.  流中必须包含 `Timestamp`，并且必须是 `keyedStream`。
2.  `processElement` 中利用 Context.TimerService 控制 Timer，如：registerProcessingTimeTimer 控制 timer fire，deleteProcessingTimeTimer 取消 fire。

```
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

public class CountProcessFunction extends ProcessFunction<Tuple2<Long, Long>, Tuple2<Long, Long>> {
    private Integer cou;


    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        cou = new Integer(0);
    }


    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<Tuple2<Long, Long>> out) throws Exception {
        super.onTimer(timestamp, ctx, out);
        cou += 1;
        System.out.println("fire timer: "+timestamp+"  "+cou);
    }

    @Override
    public void processElement(Tuple2<Long, Long> value, Context ctx, Collector<Tuple2<Long, Long>> out) throws Exception {
        ctx.timerService().registerEventTimeTimer(ctx.timestamp() + 300);
    }
}

```



## 2. Flink Sql Api

> Flink 原生的 Operators  功能少，假如需要更高级的功能可以借助 flink sql api 的operator 实现。



### 3. Stream 流源码追踪

>       源码追踪是为了更好的了解 stream 的转变和 operator 的关系。 在 flink 中框架负责 stream 的转变(对用户是无感知)，`operator`的操作逻辑(类似lambda表达式的逻辑，只有流转化逻辑，没有实际处理逻辑)，用户必须实现对应`operator`的操作逻辑。stream 的转变最重要是处理 stream 的数据类型(底层基于泛型擦除实现，必须引入类型以消除影响)。内部`operator`逻辑主要处理算子间连接方式：ChainingStrategy 的定义，还有调用用户定义`operator`处理逻辑的连接。

- 样例代码

```
StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment();
// 重点追踪以下代码，以说明 operator 以及 stream 的转变在flink 中的使用
env.fromElements("1").map(e->e).print();
env.execute();
```

.map(e->e)

```
    // 第一步
	// java 泛型存在泛型擦除，对于没有数据传入数据类型，尝试通过工具类进行类型推断
    public <R> SingleOutputStreamOperator<R> map(MapFunction<T, R> mapper) {

        TypeInformation<R> outType =
                TypeExtractor.getMapReturnTypes(
                        clean(mapper), getType(), Utils.getCallLocationName(), true);
        return map(mapper, outType);
    }

	// 第二步 
	// 通过 transform 作用：1. 将 stream 转变为输出的数据类型。 2. 传入 `operator` 具体实现类，
    public <R> SingleOutputStreamOperator<R> map(
            MapFunction<T, R> mapper, TypeInformation<R> outputType) {
        return transform("Map", outputType, new StreamMap<>(clean(mapper)));
    }
    
    // transform 作用：1. 将 stream 转变为输出的数据类型。
    @PublicEvolving
    public <R> SingleOutputStreamOperator<R> transform(
            String operatorName,
            TypeInformation<R> outTypeInfo,
            OneInputStreamOperatorFactory<T, R> operatorFactory) {

        return doTransform(operatorName, outTypeInfo, operatorFactory);
    }

    protected <R> SingleOutputStreamOperator<R> doTransform(
            String operatorName,
            TypeInformation<R> outTypeInfo,
            StreamOperatorFactory<R> operatorFactory) {
        // 把流的输出类型信息绑定到流中
        return returnStream;
    }
    
    // 通过 transform  2. 传入 `operator` 具体实现类;注意这个是内部类逻辑，属于operator 的内部处理逻辑。不负责业务逻辑，只负责框架处理抽象逻辑。
    @Internal
	public class StreamMap<IN, OUT> extends AbstractUdfStreamOperator<OUT, MapFunction<IN, OUT>>
        implements OneInputStreamOperator<IN, OUT> {

    private static final long serialVersionUID = 1L;

    public StreamMap(MapFunction<IN, OUT> mapper) {
        super(mapper);
        chainingStrategy = ChainingStrategy.ALWAYS;
    }

    @Override
    public void processElement(StreamRecord<IN> element) throws Exception {
        output.collect(element.replace(userFunction.map(element.getValue())));
    }
}
    
```











