# Flink_窗口

> - 注意
>
>   **时间戳在flink 内部以 ms 为单位，unix timestamp 作为基础单位。因此，使用时候要注意时区的影响。比如使用 Tumbling 窗口，中国时间必须处理才能使数据开窗规则为中国自然日。**
>  窗口计算范围为: start<=time<end ，不包括上界

      窗口将流拆分为有限的桶。窗口内的数据会按照用户逻辑计算，stream 定期生成窗口这样就能达到：每5分钟统计一次pv uv 等维度统计。

## 1.1 窗口语法

```
- keyed stream
stream
       .keyBy(...)               <-  keyed versus non-keyed windows
       .window(...)              <-  required: "assigner"
      [.trigger(...)]            <-  optional: "trigger" (else default trigger)
      [.evictor(...)]            <-  optional: "evictor" (else no evictor)
      [.allowedLateness(...)]    <-  optional: "lateness" (else zero)
      [.sideOutputLateData(...)] <-  optional: "output tag" (else no side output for late data)
       .reduce/aggregate/apply()      <-  required: "function"
      [.getSideOutput(...)]      <-  optional: "output tag"
      
      
- non-keyed stream(注意，该窗口并发度只能为1，会引发性能问题)

stream
       .windowAll(...)           <-  required: "assigner"
      [.trigger(...)]            <-  optional: "trigger" (else default trigger)
      [.evictor(...)]            <-  optional: "evictor" (else no evictor)
      [.allowedLateness(...)]    <-  optional: "lateness" (else zero)
      [.sideOutputLateData(...)] <-  optional: "output tag" (else no side output for late data)
       .reduce/aggregate/apply()      <-  required: "function"
      [.getSideOutput(...)]      <-  optional: "output tag"

```

无论是keyed 或者是 non-keyed window，都按照以下步骤：

1. 定义窗口类型，并且传入窗口 `assigner`。默认`assigner`窗口有：`Tumbling`，`Sliding`，`Session`，`Global` ；自定义窗口需要自行实现`WindowAssigner`接口即可。
2. **可选定义**：   定义窗口触发条件`trigger`， 窗口数据清理条件`evictor`，窗口延迟 `allowedLateness`，迟到数据旁路输出。
3. 定义计算方法。

## 2.1 窗口类型 (Window assigner)

### 2.1.1 Tumbling (滚动)

        以用户定义间隙将数据无重叠的划分。如，窗口大小为5分钟，从 00:00 ~ 00:05 （包括00 不包括 05）、00:05 ~ 00:10、00:10 ~ 00:15 。。。范围对数据进行划分。

![翻滚的窗户](https://nightlies.apache.org/flink/flink-docs-release-1.14/fig/tumbling-windows.svg)

```
DataStream<T> input = ...;

// tumbling event-time windows
input
    .keyBy(<key selector>)
    // process time 的时候使用 window(TumblingEventTimeWindows.of(Time.days(1), Time.hours(-8)))

    .window(TumblingEventTimeWindows.of(Time.seconds(5)))
    .<windowed transformation>(<window function>);
```

### 2.1.2 Sliding 滑动窗口

![滑动窗口](https://nightlies.apache.org/flink/flink-docs-release-1.14/fig/sliding-windows.svg)

```
// sliding processing-time windows offset by -8 hours
input
    .keyBy(<key selector>)
    .window(SlidingProcessingTimeWindows.of(Time.hours(12), Time.hours(1), Time.hours(-8)))
    .<windowed transformation>(<window function>);
```

### 2.1.3 Session 会话

![会话窗口](https://nightlies.apache.org/flink/flink-docs-release-1.14/fig/session-windows.svg)

```
DataStream<T> input = ...;

// event-time session windows with static gap
input
    .keyBy(<key selector>)
    .window(EventTimeSessionWindows.withGap(Time.minutes(10)))
    .<windowed transformation>(<window function>);
    
// event-time session windows with dynamic gap
input
    .keyBy(<key selector>)
    .window(EventTimeSessionWindows.withDynamicGap((element) -> {
        // determine and return session gap
    }))
    .<windowed transformation>(<window function>);
```

### 2.1.4 全局窗口

![全局窗口](https://nightlies.apache.org/flink/flink-docs-release-1.14/fig/non-windowed.svg)

```
DataStream<T> input = ...;

input
    .keyBy(<key selector>)
    .window(GlobalWindows.create())
    .<windowed transformation>(<window function>);
```

### 3. 窗口函数(windows operator)

- reduce
- agg 

> 与 reduce 相比，输入，输出类型可以不一致

```
public interface AggregateFunction<IN, ACC, OUT> extends Function, Serializable {

   // 在一次新的aggregate发起时，创建一个新的Accumulator，Accumulator是我们所说的中间状态数据，简称ACC
   // 这个函数一般在初始化时调用
   ACC createAccumulator();

   // 当一个新元素流入时，将新元素与状态数据ACC合并，返回状态数据ACC
   ACC add(IN value, ACC accumulator);
  
   // 将两个ACC合并
   ACC merge(ACC a, ACC b);

   // 将中间数据转成结果数据
   OUT getResult(ACC accumulator);

}
```

**Agg demo**

```
import env.TestUtil;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.table.expressions.In;
import org.apache.flink.types.Row;
import scala.Int;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestAggWindow {
    public static List<Row> buildSource() {
        List<String> dat = Arrays.asList("2021-11-24 12:00:00.000",
                "2021-11-24 12:00:01.000",
                "2021-11-24 12:00:11.000",
                "2021-11-24 12:00:06.000",
                "2021-11-24 12:00:07.000",
                "2021-11-24 12:00:08.000",
                "2021-11-24 12:00:09.000",
                "2021-11-24 12:00:09.000",
                "2021-11-24 12:00:32.000");
        List<String> message = Arrays.asList("job1",
                "job1",
                "job2",
                "job1",
                "job3",
                "job1",
                "job3",
                "job3",
                "job1");
        List<Row> source = new ArrayList<>();
        for (int a = 0; a < dat.size(); a++) {
            Row each = new Row(2);
            each.setField(0, dat.get(a));
            each.setField(1, message.get(a));
            source.add(each);
        }
        return source;
    }

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = TestUtil.iniEnv(1);
        env.addSource(new TestSource("TestProcessFunction", buildSource()))
                .returns(Types.ROW_NAMED(new String[]{"event_time", "action"}, Types.STRING, Types.STRING))
                .assignTimestampsAndWatermarks(WatermarkStrategy
                        .<Row>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                        .withTimestampAssigner((event, pre) -> {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                            try {
                                return sdf.parse((String) event.getField(0)).getTime();
                            } catch (ParseException e) {
                                return 0;
                            }
                        })
                ).keyBy(e -> e.getField(1))
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                .aggregate(new AggregateFunction<Row, Tuple2<String,Integer>, Tuple2<String, Integer>>() {

                    @Override
                    public Tuple2<String,Integer> createAccumulator() {
                        return new Tuple2("",new Integer(0));
                    }

                    @Override
                    public Tuple2<String,Integer> add(Row value, Tuple2<String,Integer> accumulator) {
                        return Tuple2.of((String) value.getField(1), (Integer) accumulator.getField(1)+1);
                    }

                    @Override
                    public Tuple2 getResult(Tuple2<String,Integer> accumulator) {
                        return accumulator;
                    }

                    @Override
                    public Tuple2 merge(Tuple2<String,Integer> a, Tuple2<String,Integer> b) {
                        return Tuple2.of(a.f0, a.f1+b.f1);
                    }
                })
                .print();
        env.execute();

    }
}



######################################### 输出
TestProcessFunction output:2021-11-24 12:00:00.000,job1
TestProcessFunction output:2021-11-24 12:00:01.000,job1
TestProcessFunction output:2021-11-24 12:00:11.000,job2
TestProcessFunction output:2021-11-24 12:00:06.000,job1
(job1,2)
TestProcessFunction output:2021-11-24 12:00:07.000,job3
TestProcessFunction output:2021-11-24 12:00:08.000,job1
TestProcessFunction output:2021-11-24 12:00:09.000,job3
TestProcessFunction output:2021-11-24 12:00:09.000,job3
TestProcessFunction output:2021-11-24 12:00:32.000,job1
(job1,2)
(job3,3)
(job2,1)
```

- ProcessWindowFunction 

> 与 `reduce`，`agg`相比。`ProcessWindowFunction`能够保留整个窗口的数据，一次性进行处理。缺点是需要内存保存状态。

```
public abstract class ProcessWindowFunction<IN, OUT, KEY, W extends Window> implements Function {

    /**
     * 自定义窗口处理函数
     *
     * @param key 输入键
     * @param context context；具体变量见Context类实现
     * @param elements 迭代对象，用于访问窗口下特定key的所有值
     * @param out 用于输出
     *
     * @throws Exception The function may throw exceptions to fail the program and trigger recovery.
     */
    public abstract void process(
            KEY key,
            Context context,
            Iterable<IN> elements,
            Collector<OUT> out) throws Exception;


   	// 带有窗口信息的Context实现
   	public abstract class Context implements java.io.Serializable {
   	    // 返回当前窗口
   	    public abstract W window();

   	    /** 返回processtime */
   	    public abstract long currentProcessingTime();

   	    /** 返回当前watermark */
   	    public abstract long currentWatermark();

   	    // 窗口状态，使用该状态，必须实现ProcessWindowFunction#clear(Context)方法，以回收内存
   	    public abstract KeyedStateStore windowState();
   	  
   	    // 所有key都能访问的信息
   	    public abstract KeyedStateStore globalState();
   	}
}
```

**ProcessWindowFunction  demo**

```
import env.TestUtil;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestProcessWindowFunction {
    public static List<Row> buildSource() {
        List<String> dat = Arrays.asList("2021-11-24 12:00:00.000",
                "2021-11-24 12:00:01.000",
                "2021-11-24 12:00:11.000",
                "2021-11-24 12:00:06.000",
                "2021-11-24 12:00:07.000",
                "2021-11-24 12:00:08.000",
                "2021-11-24 12:00:09.000",
                "2021-11-24 12:00:09.000",
                "2021-11-24 12:00:32.000");
        List<String> message = Arrays.asList("job1",
                "job1",
                "job2",
                "job1",
                "job3",
                "job1",
                "job3",
                "job3",
                "job1");
        List<Row> source = new ArrayList<>();
        for (int a = 0; a < dat.size(); a++) {
            Row each = new Row(2);
            each.setField(0, dat.get(a));
            each.setField(1, message.get(a));
            source.add(each);
        }
        return source;
    }

    /**
     * 5秒的watermark 延迟， 5秒一个滚动的 tumble window，以 action 字段进行 group by , count(*) 操作； 实现是基于
     * ProcessWindowFunction
     */
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = TestUtil.iniEnv(1);
        env.addSource(new TestSource("TestProcessFunction", buildSource()))
                .returns(Types.ROW_NAMED(new String[]{"event_time", "action"}, Types.STRING, Types.STRING))
                .assignTimestampsAndWatermarks(WatermarkStrategy
                        .<Row>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                        .withTimestampAssigner((event, pre) -> {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                            try {
                                return sdf.parse((String) event.getField(0)).getTime();
                            } catch (ParseException e) {
                                return 0;
                            }
                        })
                ).keyBy(e -> (String) e.getField(1))
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                .process(new ProcessWindowFunction<Row, Row, String, TimeWindow>() {
                    @Override
                    public void process(String s, Context context, Iterable<Row> elements, Collector<Row> out) {
                        int cou = 0;
                        for (Row e : elements) cou += 1;
                        Row outSource = new Row(4);
                        outSource.setField(0, context.window().getStart());
                        outSource.setField(1, context.window().getEnd());
                        outSource.setField(2, s);
                        outSource.setField(3, cou);
                        out.collect(outSource);
                    }
                })
                .print();
        env.execute();
    }
}

#########################################
TestProcessFunction output:2021-11-24 12:00:00.000,job1
TestProcessFunction output:2021-11-24 12:00:01.000,job1
TestProcessFunction output:2021-11-24 12:00:11.000,job2
TestProcessFunction output:2021-11-24 12:00:06.000,job1
1637726400000,1637726405000,job1,2
TestProcessFunction output:2021-11-24 12:00:07.000,job3
TestProcessFunction output:2021-11-24 12:00:08.000,job1
TestProcessFunction output:2021-11-24 12:00:09.000,job3
TestProcessFunction output:2021-11-24 12:00:09.000,job3
TestProcessFunction output:2021-11-24 12:00:32.000,job1
1637726405000,1637726410000,job1,2
1637726405000,1637726410000,job3,3
1637726410000,1637726415000,job2,1

```



## 4. 触发器(Trigger) & 清除器(Evictor)

### 4.1 触发器（Trigger）

​       **Trigger本质是一个特殊的timer**。触发器（Trigger）控制了窗口的结束，任何一个 `WindowAssigner` 都会定义一个默认的 `Trigger`，如：使用`TumblingEventTimeWindows`，默认会使用`EventTimeTrigger`。`Trigger`可以按照一定规律触发行为，实现在watermark 没有到达前输出部分数据。 自定义 `Trigger` 由两部分构成：触发`Trigger`的事件&`Trigger`触发后的行为。

- 触发调用 `Trigger` 的事件（Trigger 接口中包含的函数）

> 注意前三种必须返回 `TriggerResult` 对象，通过`TriggerResult`控制窗口行为 。后两种没有任何返回。

1. onElement : 每一条数据，触发。
2. onEventTime：当定义使用`event time `的`Timer fire `时候，触发。

3. onProcessingTime：当定义使用`process time`的`Timer fire` 时候，触发。

4. onMerge： 两个窗口被合并时候触发。如： session window。
5. clear：窗口被清除时触发。

- 触发后的行为(`TriggerResult`对象，以下操作可以是event or process time触发)

1. CONTINUE：什么都不做。

2. FIRE：启动计算并将结果发送给下游，不清理窗口数据。

3. PURGE：清理窗口数据但不执行计算。

4. FIRE_AND_PURGE：启动计算，发送结果然后清理窗口数据。

- 预定义 Trigger

1. EventTimeTrigger：当 event time 的watermark 大于窗口边界触发。
2. ProcessingTimeTrigger：当 process time 的watermark 大于窗口边界触发。
3. CountTrigger ：在Windows 中的数据达到一定量时触发。
4. DeltaTrigger：计算上次触发的数据点与当前到达的数据点之间的增量。如果 delta 高于指定的阈值，它就会触发。
5. NeverTrigger：永远不会触发的触发器，作为 GlobalWindows 的默认触发器。
6. ContinuousEventTimeTrigger：一定时间间隔内多次触发窗口，**注意必须 watermark 流动才能触发窗口**。
7. ContinuousProcessingTimeTrigger：一定时间间隔内多次触发窗口。

### 4.1.1 EventTimeTrigger源码

```
// EventTimeTrigger.java

// 针对每个元素触发
@Override
public TriggerResult onElement(Object element, long timestamp, TimeWindow window, TriggerContext ctx) throws Exception {
    if (window.maxTimestamp() <= ctx.getCurrentWatermark()) {
        // window的最大时间戳比watermark小，该window需要立刻进行计算
        return TriggerResult.FIRE;
    } else {
        // 当 WaterMark小于窗口最大时间，注册一个Event Timer事件(用于后续处理窗口)；当watermark 触发该 Timer 时候，会调用onEventTime方法
        // 注意：
        // 1. 虽然每进入一个数都会注册一个 Event Timer，但是 Event Timer 会去重(因此重复的)。
        // 2. TimerHeapInternalTimer 通过重写 equal 规则，一个窗口内触发的 timer 是一致的
        ctx.registerEventTimeTimer(window.maxTimestamp());
        return TriggerResult.CONTINUE;
    }
}

@Override
public TriggerResult onEventTime(long time, TimeWindow window, TriggerContext ctx) {
    // 当前时间为window的最大时间戳，触发计算
    return time == window.maxTimestamp() ?
        TriggerResult.FIRE :
        TriggerResult.CONTINUE;
}

@Override
public TriggerResult onProcessingTime(long time, TimeWindow window, TriggerContext ctx) throws Exception {
    // 对于processing time，不做任何处理
    return TriggerResult.CONTINUE;
}
```



### 4.2 清除器(Evictor)

       清除器（Evictor）是在`WindowAssigner`和`Trigger`的基础上的一个可选选项(Trigger 中的 clear 方法一样能够清除数据)，用来清除一些数据。我们可以在Window Function执行前或执行后调用Evictor。

- 语法

```
/**
	* T为元素类型
	* W为窗口
  */
public interface Evictor<T, W extends Window> extends Serializable {

    // 在Window Function前调用
    void evictBefore(Iterable<TimestampedValue<T>> elements, int size, W window, EvictorContext evictorContext);

    // 在Window Function后调用
    void evictAfter(Iterable<TimestampedValue<T>> elements, int size, W window, EvictorContext evictorContext);

    // Evictor的上下文
    interface EvictorContext {
        long getCurrentProcessingTime();
        MetricGroup getMetricGroup();
        long getCurrentWatermark();
    }
}
```

        evictBefore和evictAfter分别在Window Function之前和之后被调用，窗口的所有元素被放在了Iterable<TimestampedValue<T>>；

- 内置 Evictor

1. `CountEvictor`保留一定数目的元素，多余的元素按照从前到后的顺序先后清理。
2. `TimeEvictor`保留一个时间段的元素，早于这个时间段的元素会被清理。

- 自定 Evictor（TimeEvictor源码）

```
if (!hasTimestamp(elements)) {
	return;
}

long currentTime = getMaxTimestamp(elements);
long evictCutoff = currentTime - windowSize;

for (Iterator<TimestampedValue<Object>> iterator = elements.iterator();
		iterator.hasNext(); ) {
	TimestampedValue<Object> record = iterator.next();
	if (record.getTimestamp() <= evictCutoff) {
		iterator.remove();
	}
}
```

## 5. Window 工作原理 & 源码分析

​    window 的工作逻辑`WindowOperator`有详细描述，主要涉及到以下几个类：`WindowAssigner`（数据分配到对应的窗口 pane 中），`Trigger`，`Evictor`。注意：窗口的触发底层还是基于`EventTime/ProcessTime Timer`实现，具体`Timer`作用省略。

### 5.1 WindowOperator 处理流程

```
windowoperator 
- 处理流程
1. 利用 WindowAssigner 把数据划分到到对应 pane 中, pane 就是逻辑上的窗口(pane 中的数据，window 和 key是一致的)。
2. 数据放入到对应的 state 中。
3. Trigger.onElement 调用窗口 triger 实现，确定是否触发窗口计算。
4. 触发完成后逻辑处理完，清空对应的窗口状态。迟到数据可以 sideout 输出。

调用过程
-- processElement()
	|   
	|----> windowAssigner.assignWindows
	|      //通过WindowAssigner为element分配一系列windows
	|----> windowState.add(element.getValue())
	|      //把当前的element加入buffer state 
	|----> TriggerResult triggerResult = triggerContext.onElement(element)
	|      //触发onElment，得到triggerResult
	|----> Trigger.OnMergeContext.onElement()
	|----> trigger.onElement(element.getValue(), element.getTimestamp(), window,...)
	|----> EventTimeTriggers.onElement()
	|      //如果当前window.maxTimestamp已经小于CurrentWatermark，直接触发  
	|      //否则将window.maxTimestamp注册到TimeService中，等待触发   
	|----> contents = windowState.get(); emitWindowContents(actualWindow, contents)
	|      //对triggerResult做各种处理,如果fire，真正去计算窗口中的elements
	|----> registerCleanupTimer 清除窗口

```

**注意：**

1. 一条数据可以分配到多个pane中，每个 pane 对应的 trigger 是唯一的。(一个window激活的timer是唯一的)
   trigger负责timer 的触发，并且调用  InternalWindowFunction 处理数据，生成窗口输出数据。
2. `WindowOperator `通过`windowState`存放窗口数据。

### 5.2 Window 类

```
public class TimeWindow extends Window {

    private final long start; //窗口上界
    private final long end;//窗口上下界

    public TimeWindow(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public long getStart() {
        return start;
    }


    public long getEnd() {
        return end;
    }

	//窗口中时间最大值(用于判定 WaterMark 的到来是否能够激活该窗口的操作)；注意，这个值是通过 end -1 ms 得到的；如下所示
    @Override
    public long maxTimestamp() {
        return end - 1;
    }
    
}

```



