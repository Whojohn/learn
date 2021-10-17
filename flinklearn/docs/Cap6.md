# Flink-5-时间

reference:

https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/concepts/time/

[Flink 源码之时间处理 - 简书 (jianshu.com)](https://www.jianshu.com/p/18f680247ef1)
https://mjz-cn.github.io/2020/03/02/Flink%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%90-Watermark%E5%8E%9F%E7%90%86/
http://www.liaojiayi.com/
https://lulaoshi.info/flink/chapter-time-window/process-function.html
https://mjz-cn.github.io/2020/03/02/Flink%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%90-Watermark%E5%8E%9F%E7%90%86/

# 1. Time(时间)
> **注意时间戳在flink 内部以 ms 为单位，unix timestamp 作为基础单位。因此，使用时候要注意时区的影响。**

## 1.1 Flink 时间类型

​        Flink 中有三种时间类型：process time, ingest time, event time，性能从好到差，**只有 event time 才有watermark 这个概念。**

- process time

Flink 算子所在机器的时间。

- ingest time

**Flink 1.12 还有这个概念，Flink 1.13 setStreamTimeCharacteristic() 方法被移除，该概念也消失在官网文档中。**

​    Flink 源所在的机器的时间，与 process time 相比，它一般是由 source 机器产生的，一般用于处理过程比较耗时的场景。

- event time

​     event 中的事件时间，时间抽取规则必须由用户指定，并且用户也需要额外指定`watermark`机制。

### 1.2 Event time & WaterMark

### 1.2.1 WaterMark 基础

**！！！Event time可以是乱序，WaterMark 必须单调递增！！！**

- 什么是 WaterMark 

​    WaterMark 是一个窗口开始&结束的时间标记 ，它是一个特殊时间，意味着逻辑上应该没有比这个时间更晚到来的数据，假如有，则不放入计算中(数据延迟引发的晚来)。

- 为什么要引入 WaterMark

​    event 到达是乱序的，窗口的关闭需要一个时间作为标记，这个时间的标记就是 WaterMark ， WaterMark 定义了乱序数据中，窗口的开始&结束时间。

- 生成 WaterMark 注意事项

1. Watermark从evnet 中抽取,必须单调递增。
2. 假如Flink算子收到一个evnet中time小于WaterMark。Flink提供了一些其他机制来处理迟到数据。(sql中没有晚到数据处理方法)
3. Watermark机制允许用户来控制准确度和延迟。Watermark设置得与事件时间戳相距紧凑，会产生不少迟到数据，影响计算结果的准确度，整个应用的延迟很低；Watermark设置得非常宽松，准确度能够得到提升，但应用的延迟较高，因为Flink必须等待更长的时间才进行计算。

### 1.2.2 WaterMark 传播原理(分布式WaterMark)

reference:

Apache flink 时间属性深度解析

- WaterMark 三个阶段：
  1. 产生
  2. 传播
  3. 处理
- WaterMark 的传播策略基本上遵循以下三个规则
  1. watermark 会以广播的形式在算子之间进行传播。比如说上游的算子 连接了三个下游的任务，它会把自己当前的收到的 watermark 以广播的形式 传到下游。 
  2.  如果在程序里面收到了一个 Long.MAX_VALUE 这个数值的 watermark，就表示对应的那一条流的一个部分不会再有数据发过来了，它相当于就 是一个终止的标志。
  3. 遵循单输入取其大，多输入取小。



### 1.2.3 Timestamp & WaterMark 生成方式

> https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/sources/



- **Timestamp & WaterMark 生成方式，主要有两种方式：**

1. Source 中定义
2. Stream 中定义

- **WaterMark 生成的方式又有：**

**注意，每一条数据都必须有 Timestamp 但是不一定触发 WaterMark 计算。任何`WaterMark`生成方式，都是通过emitWatermark 函数的调用进行控制。**

1. 定期生成(Punctuated/Periodic)。

2. 特殊事件生成(用户自定义特殊事件触发 watermark )。



- **如何在source 和 stream 中声明 Timestamp 和 WaterMark**

**注意不同source 定义方式不能混用 Timestamp 生成方法，使用了旧版声明方式会导致新版声明方式失败，因为新版source 调用方法 fromSource 会导致assignTimestampsAndWatermarks失效。**

**对于1.10 以下版本，旧版source 声明方式：org.apache.flink.streaming.api.functions.source.SourceFunction / RichSourceFunction 实现连接器**

1. source 实现 collectWithTimestamp 方法以提供 Timestamp 的生成, 调用 emitWatermark  配置waterMarker。
2. stream.assignTimestampsAndWatermarks(WatermarkStrategy) ，stream 配置 timestamp 和 watermark。

**新版source声明方式：org.apache.flink.api.connector.source.Source 实现的连接器 **

1. env.fromSource 方法声明 source 中定义 timestamp 提前与 watermark 实现策略。

```
environment.fromSource(
    Source<OUT, ?, ?> source,
    WatermarkStrategy<OUT> timestampsAndWatermarks,
    String sourceName)
```

2. **新版fromSource 方法无法通过 assignTimestampsAndWatermarks 配置 stream 中 Timestamp和WaterMark ，因为 fromsource 会使得assignTimestampsAndWatermarks 失效。**

### 1.2.4 WatermarkStrategy

WatermarkStrategy 中规定了 Timestamp、WaterMark 生成与管理方式。

- WatermarkStrategy  源码

```
public interface WatermarkStrategy<T>
        extends TimestampAssignerSupplier<T>, WatermarkGeneratorSupplier<T> {

    // ------------------------------------------------------------------------
    //  必须实现：createWatermarkGenerator，createTimestampAssigner 方法。
    // ------------------------------------------------------------------------

    /** 
    * 用户自定义WatermarkStrategy 必须重写实现event 中提取timestamp的方法
    */
    @Override
    WatermarkGenerator<T> createWatermarkGenerator(WatermarkGeneratorSupplier.Context context);

    /**
     * 用户自定义WatermarkStrategy 必须重写实现event 中提取timestamp的方法
     */
    @Override
    default TimestampAssigner<T> createTimestampAssigner(
            TimestampAssignerSupplier.Context context) {
        // 默认情况下会调用 source 中实现的提前方式，如kafka 中默认的提取方式
        return new RecordTimestampAssigner<>();
    }

    // ------------------------------------------------------------------------
    //  用于实例化接口(调用以下方法传入时间戳抽取方式，配合预定义watermark生成方法，可以实例化WatermarkStrategy接口)
    // ------------------------------------------------------------------------

    /**
     * 
     */
    default WatermarkStrategy<T> withTimestampAssigner(
            TimestampAssignerSupplier<T> timestampAssigner) {
        checkNotNull(timestampAssigner, "timestampAssigner");
        return new WatermarkStrategyWithTimestampAssigner<>(this, timestampAssigner);
    }

    /**
     * 传入，event 中抽取时间逻辑实现
     */
    default WatermarkStrategy<T> withTimestampAssigner(
            SerializableTimestampAssigner<T> timestampAssigner) {
        checkNotNull(timestampAssigner, "timestampAssigner");
        return new WatermarkStrategyWithTimestampAssigner<>(
                this, TimestampAssignerSupplier.of(timestampAssigner));
    }

    /**
     * 固定时间触发事件戳，用于数据流停止，窗口没法停止的场景
     */
    default WatermarkStrategy<T> withIdleness(Duration idleTimeout) {
        checkNotNull(idleTimeout, "idleTimeout");
        checkArgument(
                !(idleTimeout.isZero() || idleTimeout.isNegative()),
                "idleTimeout must be greater than zero");
        return new WatermarkStrategyWithIdleness<>(this, idleTimeout);
    }

    // ------------------------------------------------------------------------
    //  Convenience methods for common watermark strategies
    // ------------------------------------------------------------------------

    /**
     * 延迟为0的时间戳
     * 底层实现为BoundedOutOfOrdernessWatermarks 传入最大等待时间为0
     *
     */
    static <T> WatermarkStrategy<T> forMonotonousTimestamps() {
        return (ctx) -> new AscendingTimestampsWatermarks<>();
    }

    /**
     * 固定延迟时间
     * watermark 策略使用 BoundedOutOfOrdernessWatermarks 。BoundedOutOfOrdernessWatermarks 策略：     
     * 用户传入一个最大等待时间，每一个传入timestamp 更新内部 maxTimestamp = max(用户最大等待时间, 事件时间)，
     * 周期发送 watermark = maxTimestamp-用户最大等待时间-1。
     */
    static <T> WatermarkStrategy<T> forBoundedOutOfOrderness(Duration maxOutOfOrderness) {
        return (ctx) -> new BoundedOutOfOrdernessWatermarks<>(maxOutOfOrderness);
    }

    /** 从 WatermarkGeneratorSupplier 中提取 watermark 生成策略 */
    static <T> WatermarkStrategy<T> forGenerator(WatermarkGeneratorSupplier<T> generatorSupplier) {
        return generatorSupplier::createWatermarkGenerator;
    }

    /**
     * 不产生watermark，用于 processtime 场景
     */
    static <T> WatermarkStrategy<T> noWatermarks() {
        return (ctx) -> new NoWatermarksGenerator<>();
    }
}

```

#### 1.2.4.1 TimestampAssigner(WatermarkStrateg.createTimestampAssigner 底层实现)

- TimestampAssigner 源码核心

```
public interface TimestampAssigner<T> {

    // 当extractTimestamp 传入 前一个时间recordTimestamp 为空时候，使用的是该值
    long NO_TIMESTAMP = Long.MIN_VALUE;

    /**
     * 抽取当前时间戳
     * 传入当前对象和前一个时间戳用于处理；假如前一个时间戳为空，则传入NO_TIMESTAMP；
     */
    long extractTimestamp(T element, long recordTimestamp);
}

```

#### 1.2.4.2 WatermarkGenerator（WatermarkStrate.reateWatermarkGenerator 底层实现）

- WatermarkGenerator 源码

```
public interface WatermarkGenerator<T> {

   /**
   * 每一条数据都执行该方法
   * 假如每一条数据都触发，或者根据特定事件触发 watermark， 触发时候调用 emitWatermark 即可
   */
    void onEvent(T event, long eventTimestamp, WatermarkOutput output);

    /**
     *  定期调用该方法
     * 触发时候调用 emitWatermark 即可
     */
    void onPeriodicEmit(WatermarkOutput output);
}
```

#### 1.2.4.3  WatermarkStrategy 调用例子

- 周期性聚合

```
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.time.Duration;


public class TestSource extends RichParallelSourceFunction<Tuple2<Long, Long>> {

    @Override
    public void run(SourceContext<Tuple2<Long, Long>> ctx) throws Exception {
        int cou = 1;
        Tuple2 tempTuple = new Tuple2();
        Long[] timeSource = new Long[]{
                1632981239000l,
                1632981240000l,
                1632981243000l,
                1632981247000l,
                1632981249000l,
                1632981259000l,
                1632981249000l};
        for (Long each : timeSource) {
            tempTuple.setFields(new Long(cou), each);
            ctx.collect(tempTuple);
            System.out.println("source output:" + each);
            Thread.sleep(3000);
            cou += 1;
        }
        Thread.sleep(10000);
        // ctx.collect 方法也有空闲检测，但是没有开放给用户，因此 source 中只能markAsTemporarilyIdle 显式声明告知watermark 此流空闲，生成watermark 不需要等待该流，防止并发流下堵塞
        // 或者通过 WatermarkStrategy.withIdleness 配置，生成全局watermark 时忽略一段时间没有改变的watermark。
        ctx.markAsTemporarilyIdle();

        while (true) {
            Thread.sleep(500);
        }
    }

    @Override
    public void cancel() {

    }

    public static void main(String[] args) throws Exception {
        org.apache.flink.configuration.Configuration conf = new Configuration();
        conf.setBoolean("rest.flamegraph.enabled", true);
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment(1, conf);
        env.getConfig().setAutoWatermarkInterval(10000L);
        env.addSource(new TestSource()).assignTimestampsAndWatermarks(
                WatermarkStrategy
                        .<Tuple2<Long, Long>>forGenerator(WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(0)))
                        .withTimestampAssigner((event, pre) -> event.f1)
                        .withIdleness(Duration.ofSeconds(5))
        )
                .keyBy(e -> e.f0)
                .window(TumblingEventTimeWindows.of(Time.seconds(10)))
                .allowedLateness(Time.seconds(0))
                // 这里用null 是因为分区把每一个元素都分为独立的一个区，所以reduce 相当于无效，没有任何作用，可以 把流转化为 SingleOutputStreamOperator 以查看 windows 中的数据
                .reduce((value1, value2) -> null)
                .print();
        env.execute();
    }
}


source output:1632981239000
source output:1632981240000
source output:1632981243000
source output:1632981247000
source output:1632981249000
(1,1632981239000)
source output:1632981259000
source output:1632981249000
(4,1632981247000)
(7,1632981249000)
(2,1632981240000)
(5,1632981249000)
(3,1632981243000)
```



### 1.2.5 WaterMark 停滞

- 造成原因

  1. 并发流中，有一sub-task没有数据，或者大面积延迟。

     > 因为并发流是需要取所有流的 WaterMark 然后 min(all(WaterMark)) 获取当前算子的watermark 然后往后传递，因为会因为某个流产生滞后的情况。

       2.  多流join，整体的 watermark 是所有流的最小值，因为较慢的流会导致停滞
       3.  没有新的数据到来，导致生成的 watermark 停止，最后一个窗口无法关闭。

- 解决办法

对于并发流中有一个sub-task 没有数据

1. 算子间添加 shuffle ，reblance 等方法，使得数据重新分部(必须放入到 watermark 声明前，因此对于 source 作用不大)。
2. 对于 source 来说调用 `markAsTemporarilyIdle` 告知下游(框架collect 也会有定时idle 检测机制），watermark 计算临时忽略该流。

对于没有新的数据到来，引发watermark 停止，导致窗口无法关闭

1. 修改watermark 中的 onPeriodicEmit , 一定时长后推进 `watermark`。**！！！注意！！！，可能会引发窗口过早关闭**



#### 1.2.6 总结

Watermark 对于event 可复现是因为watermark 只是让窗口累积数据，直至watermark 到达，然后触发窗口生成。**逻辑上 watermark 才有晚于该点的时间数据丢弃，实际上实现是所有数据都下流到windows ，具体数据流的处理的逻辑交由windows处理。**

