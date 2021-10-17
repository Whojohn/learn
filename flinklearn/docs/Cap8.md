# Flink-8-双流join

   **至今 `Flink` 只支持 innner join 和 left join, 无论是 sql api 还是 stream api，无论是哪种join 底层都是基于`CoGroupFunction `实现。** Flink从数据划分又分为：Window Join（窗口连接）和Interval Join（时间间隔连接）;

- stream inner join 语法

```
stream.join(otherStream)
    .where(<KeySelector>)      //stream 使用哪个字段作为Key
    .equalTo(<KeySelector>)    //otherStream 使用哪个字段作为Key
    .window(<WindowAssigner>)
    .apply(<JoinFunction>)
```

- stream left join 语法(用**CoGroup** 实现，**CoGroup** 能够将双流同key合并成一个分组，假如有一侧流不存在key，也会保留下key，实现了left join的功能)

```
dataStream.coGroup(otherStream)
    .where(0)
    .equalTo(1)
    .window(TumblingEventTimeWindows.of(Time.seconds(3)))
    .apply (new CoGroupFunction () {...});
    
```

## 1.1 Window join

    代码样例用 `inner join` 进行说明。其中window 可以用预定义 window: tumbling , sliding，session；**也可以自定义WindowAssigner，并且自定义对应的 窗口相关的：trigger，evictor 等。**

   以下代码以 tumbling  窗口为例，每间隔 `5s` 开窗口

```
//  生成数据代码
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.types.Row;

import java.util.List;

public class TestSource extends RichParallelSourceFunction<Row> {
    private boolean label = true;
    private final String sourceName;
    private final List<Row> source;

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
        for (Row each : this.source) {
            System.out.println(sourceName + " output:" + each.toString());
            ctx.collect(each);
            Thread.sleep(5000);
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
}
```

- join 代码

```
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.JoinFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.types.Row;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestJoin {

    public static List<Row> buildOrder() {
        List<Long> timestamp = Arrays.asList(1632981239000L,
                1632981240000L,
                1632981243000L,
                1632981247000L,
                1632981249000L,
                1632981259000L,
                1632981249000L);
        List<String> order = Arrays.asList("1001",
                "1002",
                "1004",
                "1003",
                "1005",
                "1006",
                "1007");
        List<Row> source = new ArrayList<>();
        for (int a = 0; a < timestamp.size(); a++) {
            Row each = new Row(2);
            each.setField(0, timestamp.get(a));
            each.setField(1, order.get(a));
            source.add(each);
        }
        return source;
    }

    public static List<Row> buildOrderInfo() {
        List<Long> timestamp = Arrays.asList(1632981239000L,
                1632981242000L,
                1632981244000L,
                1632981231000L,
                1632981249000L,
                1632981259000L,
                1632981249000L);
        List<String> message = Arrays.asList("this",
                "is",
                "a",
                "join",
                "test",
                "which",
                "ama");
        List<String> order = Arrays.asList("1001",
                "1002",
                "1007",
                "1003",
                "1005",
                "1006",
                "1002");
        List<Row> source = new ArrayList<>();
        for (int a = 0; a < timestamp.size(); a++) {
            Row each = new Row(3);
            each.setField(0, timestamp.get(a));
            each.setField(1, order.get(a));
            each.setField(2, message.get(a));
            source.add(each);
        }
        return source;
    }


    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = TestUtil.iniEnv(1);
        env.getConfig().setAutoWatermarkInterval(200L);


        // order
        String[] names = new String[]{"eve_time", "order"};
        TypeInformation[] types = new TypeInformation[]{Types.LONG, Types.STRING};
        DataStream<Row> order = env.addSource(new TestSource("oder", buildOrder()))
                .returns(Types.ROW_NAMED(names, types))
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy
                                .<Row>forGenerator(WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(0)))
                                .withTimestampAssigner((event, pre) -> (long) event.getField(0))
                                .withIdleness(Duration.ofSeconds(5))
                );

        names = new String[]{"eve_time", "order", "info"};
        types = new TypeInformation[]{Types.LONG, Types.STRING, Types.STRING};
        DataStream<Row> orderInfo = env.addSource(new TestSource("order info", buildOrderInfo()))
                .returns(Types.ROW_NAMED(names, types))
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy
                                .<Row>forGenerator(WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(0)))
                                .withTimestampAssigner((event, pre) -> (long) event.getField(0))
                                .withIdleness(Duration.ofSeconds(5))
                );

        order.join(orderInfo)
                .where(
                        e -> e.getField(1)
                )
                .equalTo(
                        e -> e.getField(1)
                )
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                .apply(new JoinFunction<Row, Row, String>() {
                    @Override
                    public String join(Row first, Row second) throws Exception {
                        return (first.getField(0).toString()) + "," + first.getField(1) + "," + second.getField(2);
                    }
                })
                .print();
        env.execute();

    }
}

```





- 输出结果

```
order info output:+I[1632981239000, 1001, this]
oder output:+I[1632981239000, 1001]
// watermark: 1632981239000

order info output:+I[1632981242000, 1002, is]
oder output:+I[1632981240000, 1002]
// watermark: 1632981240000 触发 >=35 < 40 窗口

1632981239000,1001,this



order info output:+I[1632981244000, 1007, a]
oder output:+I[1632981243000, 1004]
// watermark：1632981243000

order info output:+I[1632981231000, 1003, join]
oder output:+I[1632981247000, 1003]
// watermark：1632981247000 触发 >=40 < 45 窗口

1632981240000,1002,is

order info output:+I[1632981249000, 1005, test]
oder output:+I[1632981249000, 1005]
// watermark：1632981247000

oder output:+I[1632981259000, 1006]
order info output:+I[1632981259000, 1006, which]
// watermark：1632981259000 触发 >=45 < 50 窗口

1632981249000,1005,test


order info output:+I[1632981249000, 1002, ama]
oder output:+I[1632981249000, 1007]

```



### 1.2. Interval Join

     与Window Join不同，Interval Join不依赖Flink的`WindowAssigner`，而是根据一个时间间隔（Interval）界定时间。Interval需要一个时间下界（Lower Bound）和上界（Upper Bound），如果我们将input1和input2进行Interval Join，input1中的某个元素为input1.element1，时间戳为input1.element1.ts，那么一个Interval就是[input1.element1.ts + lowerBound, input1.element1.ts + upperBound]，input2中落在这个时间段内的元素将会和input1.element1组成一个数据对。用数学公式表达为，凡是符合下面公式的元素，会两两组合在一起。 input1.element1.ts + lowerBound \le input2.elementX.ts \le input1.element1.ts + upperBound*i**n**p**u**t*1.*e**l**e**m**e**n**t*1.*t**s*+*l**o**w**e**r**B**o**u**n**d*≤*i**n**p**u**t*2.*e**l**e**m**e**n**t**X*.*t**s*≤*i**n**p**u**t*1.*e**l**e**m**e**n**t*1.*t**s*+*u**p**p**e**r**B**o**u**n**d* 上下界可以是正数也可以是负数。



## 2.  迟到数据处理

Flink windows 默认会对迟到数据进行丢弃。

### 2.1 sideout 旁路输出过期数据

```
final OutputTag<T> lateOutputTag = new OutputTag<T>("late-data"){};

DataStream<T> input = ...

SingleOutputStreamOperator<T> result = input
    .keyBy(<key selector>)
    .window(<window assigner>)
    .allowedLateness(<time>)
    .sideOutputLateData(lateOutputTag)
    .<windowed transformation>(<window function>);

DataStream<T> lateStream = result.getSideOutput(lateOutputTag);
```

### 2.2 allowedLateness 

**注意 window 中 `allowedLateness`只能在`evnet time`中使用，`allowedLateness ` 会多次触发窗口：第一次是5s 到达，正常触发一次。后续当 窗口关闭时间(watermark)+allowedLateness > timestamp > 窗口关闭时间（watermark）的数据到来会再次触发窗口的运算。假如自定义 state ，必须要自行处理window 更新的逻辑，不能光一次性插入状态。**

- 样例代码：

```
DataStream<Tuple4<String, String, Integer, String>> allowedLatenessStream = input
      .assignTimestampsAndWatermarks(
                WatermarkStrategy
                        .<Row>forGenerator(WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(3)))
                        .withTimestampAssigner(...)
                        .withIdleness(Duration.ofSeconds(5))
        ) 
      .keyBy(item -> item.f0)
      .timeWindow(Time.seconds(5))
      .allowedLateness(Time.seconds(5))
      .process(统计每一个窗口的个数);
```

- 测试用例

> 以上面的代码为例，说明 allowedLateness  配合 watermark 的使用。

```
// watermark= max(timestamp-3)， 窗口延迟为 5， 窗口范围为： 00~05时间段。则允许的时间范围为00~13(watermark+3+5)
1633920480000
1633920485000
// 注意，1633920485000不会触发窗口计算，因为 watermark = max(timestamp-3), forBoundedOutOfOrderness 策略造成
1633920488000
// 1633920488000-1633920480000 = 8s 触发运算
(1633920480000~1633920485000,1)
1633920481000 
// 触发运算
(1633920480000~1633920485000,2)
1633920490000
1633920493000
// 触发运算,关闭窗口1633920480000~1633920485000
(1633920485000~1633920490000,2)
1633920481000
// 丢弃，过期
```



