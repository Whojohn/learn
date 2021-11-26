# Flink_窗口

> - 注意
>
>   **时间戳在flink 内部以 ms 为单位，unix timestamp 作为基础单位。因此，使用时候要注意时区的影响。比如使用 Tumbling 窗口，中国时间必须处理才能使数据开窗规则为中国自然日。**

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

## 4. 触发器(Trigger) & 清除器(Evictor)

### 4.1 触发器（Trigger）

    **Trigger本质是一个特殊的timer**。触发器（Trigger）控制了窗口的结束，任何一个 `WindowAssigner` 都会定义一个默认的 `Trigger`，如：使用`TumblingEventTimeWindows`，默认会使用`EventTimeTrigger`。`Trigger`可以按照一定规律触发行为，实现在watermark 没有到达前输出部分数据。 自定义 `Trigger` 由两部分构成：触发`Trigger`的事件&`Trigger`触发后的行为。

- 触发调用 `Trigger` 的事件（Trigger 接口中包含的函数）

> 注意前三种必须返回 `TriggerResult` 对象，通过`TriggerResult`控制窗口行为 。后两种没有任何返回。

1. onElement : 每一条数据，触发。
2. onEventTime：当定义使用event time 的timer fire 时候，触发。

3. onProcessingTime：当定义使用process time 的timer fire 时候，触发。

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

### 4.2 清除器(Evictor)

​       清除器（Evictor）是在`WindowAssigner`和`Trigger`的基础上的一个可选选项(Trigger 中的 clear 方法一样能够清除数据)，用来清除一些数据。我们可以在Window Function执行前或执行后调用Evictor。

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

​        evictBefore和evictAfter分别在Window Function之前和之后被调用，窗口的所有元素被放在了Iterable<TimestampedValue<T>>；

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



