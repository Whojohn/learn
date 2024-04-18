# Flink-Timer

>  reference:
>
> https://nightlies.apache.org/flink/flink-docs-master/zh/docs/dev/datastream/operators/process_function/
>
> 

      `Timer`在`Flink`中提供了用于特定`EventTime`，`ProcessTime`触发特定事件的机制，如: 用户定义事件(`onTimer`使用)，窗口的操作(触发，清除等)。不要混淆`Timer`就是定时线程(定时线程在 Flink 中有n种使用场景)，`Timer`官方定义就是一个事件触发机制而已(且必须是`keyedstream`下的事件触发机制)。

```
官方定义
The timers allow applications to react to changes in processing time and in event time. Every call to the function processElement(...) gets a Context object which gives access to the element’s event time timestamp, and to the TimerService. The TimerService can be used to register callbacks for future event-/processing-time instants. With event-time timers, the onTimer(...) method is called when the current watermark is advanced up to or beyond the timestamp of the timer, while with processing-time timers, onTimer(...) is called when wall clock time reaches the specified time. During that call, all states are again scoped to the key with which the timer was created, allowing timers to manipulate keyed state.
```

**注意**：

1. `Timer`会在`Checkpoint`时候进行保存(快照保存的是**EventTime /ProcessTime 的有序最小堆**)。
2. `Timer`内部会以有序最小堆保存。(同一`Key`的`Timer`会自动去重)
3. 使用`Timer`必须是`KeyedStream`, `Timer`实现依赖于`TimerService`服务。
4. `Timer`可以注册，也可以取消。**取消时候需要指定要取消的事件戳，用户必须自己保存这个时间戳。**
5. 对于`ProcessTime`下`Timer`是`regisXXXTimer`把`ontime`操作放入到队列中，并且发起定时线程(`ScheduledThreadPoolExecutor.schedule`)，定时投递事件到`mailbox`，触发`mailbox`事件处理队列中的数据。对于`EventTime`下`Timer`是`regisXXXTimer`把`ontime`操作放入到队列中，数据流中插入`WaterMark Event`，触发检测队列中的`Timer`执行。**他们都会在 regisXXX Time 阶段把事件放入到队列中，但是触发的机制不一样。**

**思考以下问题**
1. 什么地方依赖了 Timer?
> windows , interval join , temporal join
2. Timer 性能真的那么好吗？（就是好，对比rocksdb state 来说；具体见benchmark）
> 就是好，对比rocksdb keyed state 来说；具体见benchmark; 虽然 timer 插入要排序，但是还是十分高效；对比 rocksdb keystate 2w 的 qps，timer 千万级别， 
> rocksdb 都有 12w 的qps；
3. 怎么用 Timer 才能性能更好？

> 1. heap timer
> 2. 时间粒度减少，比如ms -> s， s -> 5s 一个粒度，会对性能倍数提高  ，但是粒度过大会导致单个timer 处理的事件变多；
> 3. 使用 rocksdb 时候，调整 #RocksDBPriorityQueueSetFactory#DEFAULT_CACHES_SIZE 大小。（内部没开放该参数，实际调整 cache 性能会变好）
4. 存储底层是怎么样的？
> - TimerHeapInternalTimer
>
> **TimerHeapInternalTimer 对象是表达 Timer 的抽象，一个 timer 必须包含： key, timestamp, namespace**;
>
> - 存放的底层 InternalPriorityQueue 具体实现类，保证 timestamp 有序排列；
>
> rocksdb : RocksDBCachingPriorityQueueSet ，有个 treemap cache (提高treemap 大小能提高性能)
>
> heap: HeapPriorityQueueSet


## 1. Timer 使用方式

- 注册一个 Timer

```
# 可以同时注册 EventTime 和 ProcessTime
Context.timerService().registerProcessingTimeTimer
Context.timerService().registerEventTimeTimer
```

- 注册一个 Timer 回调操作

```
    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<Tuple2<Long, Long>> out) throws Exception {
        super.onTimer(timestamp, ctx, out);
        // 
        // 同时使用 EvenTime ProcessTime 时候，必须要通过timeDomain 确定触发的时间类型
       	ctx.timeDomain()
}
```

**注意**

1. 由于触发`OnTimer`不考虑`EventTime`，还是`ProcessTime`。混合使用多种触发事件时，必须通过`timeDomain`确定触发的时间类型。
2. `EventTime`的 `Timer`触发需要`WaterMarker`。

## 2. Timer 底层原理(Mailbox 工作原理之 EventTime&ProcessTime)

### 整体流程
- ProcessTime Timer 工作原理

     `registerProcessingTimeTimer`把回调操作插入到队列中，并且发起一个后台的定期启动线程，当时间到底时候，投递`mail`事件到`mailbox`，触发处理队列中的回调处理。

```
定时线程        										                     工作线程
														|1. 用户 jar       : ctx.timerService().registerProcessingTimeTimer 
														|2. streaming.api  : SimpleTimerService.registerProcessingTimeTimer
														|3. streaming.api  : InternalTimerServiceImpl.registerProcessingTimeTimer
				<------------------------------------- 				                |4. runtime.tasks  : ProcessingTimeService.registerTimer // 操作插入到队列，缓存onTimer 操作，然后发起一个定时线程
				|										|
				|										|  StreamTask processinput 处理每一条数据
				|										|
定时线程发起 mailbox 投递----------------------------->	| 放入到 mailbox 队列中
														|
														| mailbox 每一轮次检测，检测到 mailbox 
														| 队列不为空，停止逻辑处理，处理 mail 事件。从ProcessTime 队列中取出操作
														| 调用该 Timer 对应的ontime 函数，执行操作；
														| 完成 mailbox 中所有 mail 事件处理
														|
														| StreamTask processinput 处理每一条数据
```

- EventTime Timer 工作原理

       `registerEventTimeTimer`把回调放入到队列中，当收到`WaterMark`事件(**注意WaterMark是通过数据流传入，与Mailbox没有任何关系**)，检查队列中的事件，事件符合的事件触发。

```
定时触发生成watermark       |工作线程的上游线程(数据发送给下游)                                                            工作线程
|                           |                                                                 |1. 用户 jar       : ctx.timerService().registerEventTimeTimer 
|                           |                                                                 |2. streaming.api  : SimpleTimerService.registerEventTimeTimer
| 生成watermark mail发送->  |                                                                 |3. streaming.api.operators  : InternalTimerServiceImpl.registerEventTimeTimer // 把事件插入到队列中
|                           |StreamTask processinput 处理每一条数据                           |   
|                           |StreamTask processinput 处理每一条数据                           |
|                           |收到mailbox，生成 WaterMark 以放入到数据流，发送给下游           |
|                           |                                                                 |  StreamTask processinput 处理每一条数据
|                           |                                                                 |  StreamTask processinput 处理每一条数据
|                           |                                                                 |  StreamTask processinput 处理每一条数据
|                           |                                                                         // 数据流中传入了watermark 事件，触发WaterMark处理
|                           |                                                                                                                 |
|                           |                                                                                                                 |
|                           |                                                                                                                 -> | WaterMark 触发处理
|                           |                                                                                                                 | 判定触发的 WaterMark 跟队列中需要触发的 Eventtime 时间
|                           |                                                                                                                 | 取出时间小于 等于 WaterMark 的事件，处理事件（不需要mailbox 投递）
|                           |                                                                 |                                                   
|                           |                                                                 |         StreamTask processinput 处理每一条数据                                           
|                           |                                                                 |    StreamTask processinput 处理每一条数据
|                           |                                                                 |    StreamTask processinput 处理每一条数据
|                           |                                                                 |
|                           |                                                                 |
|                           |                                                                 |
```
### event timer vs process timer 注册逻辑

1. Event time 直接放入有序队列中。（因为触发逻辑靠watermark）
2. 每一次插入 process timer 时候检测是否当前 timer 触发时间小于队列头的时间，假如是，重新注册定时器。

```

# InternalTimerServiceImpl.java  
# process time
# 每一次插入 process timer 时候检测是否当前 timer 触发时间小于队列头的时间，假如是，重新注册定时器
public void registerProcessingTimeTimer(N namespace, long time) {
    InternalTimer<K, N> oldHead = processingTimeTimersQueue.peek();
    if (processingTimeTimersQueue.add(
            new TimerHeapInternalTimer<>(time, (K) keyContext.getCurrentKey(), namespace))) {
        long nextTriggerTime = oldHead != null ? oldHead.getTimestamp() : Long.MAX_VALUE;
        // check if we need to re-schedule our timer to earlier
        if (time < nextTriggerTime) {
            if (nextTimer != null) {
                nextTimer.cancel(false);
            }
            // 注册定时器，定时器会定时插入到mailbox 中，
            nextTimer = processingTimeService.registerTimer(time, this::onProcessingTime);
        }
    }
}

# event time
    public void registerEventTimeTimer(N namespace, long time) {
        eventTimeTimersQueue.add(
                new TimerHeapInternalTimer<>(time, (K) keyContext.getCurrentKey(), namespace));
}
```



### Event time vs Process time 触发逻辑

1. 对于`ProcessTime`下`Timer`是`regisXXXTimer`把`ontime`操作放入到队列中，并且发起定时线程(`ScheduledThreadPoolExecutor.schedule`)，定时投递事件到`mailbox`，触发`mailbox`事件处理队列中的数据。

2. 对于`EventTime`下`Timer`是`regisXXXTimer`把`ontime`操作放入到队列中，数据流中插入`WaterMark Event`，触发检测队列中的`Timer`执行。**他们都会在 regisXXX Time 阶段把事件放入到队列中，但是触发的机制不一样。**

```
# InternalTimerServiceImpl.java    

# mailbox 事件，回调该函数
    private void onProcessingTime(long time) throws Exception {
        // null out the timer in case the Triggerable calls registerProcessingTimeTimer()
        // inside the callback.
        nextTimer = null;

        InternalTimer<K, N> timer;

        while ((timer = processingTimeTimersQueue.peek()) != null && timer.getTimestamp() <= time) {
            processingTimeTimersQueue.poll();
            keyContext.setCurrentKey(timer.getKey());
            triggerTarget.onProcessingTime(timer);
        }

        if (timer != null && nextTimer == null) {
            nextTimer =
                    processingTimeService.registerTimer(
                            timer.getTimestamp(), this::onProcessingTime);
        }
    }


# watermark 时间触发event time 检测
    public void advanceWatermark(long time) throws Exception {
        currentWatermark = time;

        InternalTimer<K, N> timer;

        while ((timer = eventTimeTimersQueue.peek()) != null && timer.getTimestamp() <= time) {
            eventTimeTimersQueue.poll();
            keyContext.setCurrentKey(timer.getKey());
            triggerTarget.onEventTime(timer);
        }
    }
```


## Timer BenchMark

> 测试结果如下：

```
|                                  | 内存(mb) | 耗时（ms） | qps               |
| -------------------------------- | -------- | ---------- | ----------------- |
| 10000000_12num__20num_ms_heap    | 794      | 26141      | 382540.8362       |
| 10000000_12num__20num_ms_rocksdb | 10       | 77164      | 129594.111243585  |
| 10000000_12num__20num_s_heap     | 794      | 9166       | 1090988.435522583 |
| 10000000_12num__20num_s_rocksdb  | 10       | 52086      | 191990.170103291  |
| 10000000_20num__30num_s_heap     | 1239     | 8542       | 1170686.022008897 |
|                                  |          |            |                   |
```

```
# 测试代码

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.StateBackendOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.util.Collector;

import java.util.Random;

public class TimerBenchMark {
    public final static int TEST_KEY_NUMBER = 10000000;

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        conf.set(StateBackendOptions.STATE_BACKEND, "hashmap");
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(conf);
        env.setParallelism(1).setMaxParallelism(1);


        env.addSource(new RichParallelSourceFunction<String>() {
            int i = 0;

            @Override
            public void run(SourceContext<String> ctx) throws Exception {
                System.out.println("Start   !!!!!!!!!!          !!!!!!!!!!!!!!!!!!" + (System.currentTimeMillis()));
                while (true) {
                    if (i < TEST_KEY_NUMBER) {
                        ctx.collect("2024041708100000000001" + i);
                        i += 1;
                    } else {
                        Thread.sleep(1000);
                    }
                }
            }

            @Override
            public void cancel() {
            }
        }).keyBy(e -> e).process(new KeyedProcessFunction<String, String, Integer>() {

            private final long startTime = System.currentTimeMillis();
            final Random random = new Random();

            @Override
            public void processElement(String value, KeyedProcessFunction<String, String, Integer>.Context ctx,
                                       Collector<Integer> out) throws Exception {

                ctx.timerService().registerProcessingTimeTimer(startTime + (random.nextInt(5)*1000));

            }

            @Override
            public void onTimer(long timestamp, KeyedProcessFunction<String, String, Integer>.OnTimerContext ctx,
                                Collector<Integer> out) throws Exception {
                super.onTimer(timestamp, ctx, out);
                ctx.getCurrentKey();
                out.collect(1);
            }
        }).addSink(new RichSinkFunction<Integer>() {
            int l;

            @Override
            public void open(Configuration parameters) throws Exception {
                super.open(parameters);
                l = 0;
            }

            @Override
            public void invoke(Integer value, Context context) throws Exception {
                l += value;
                if (l >= TEST_KEY_NUMBER) {
                    System.out.println("Finish  !!!!!!!!!!          !!!!!!!!!!!!!!!!!!" + (System.currentTimeMillis()));
                    System.exit(9);
                }
            }
        });
        env.execute();
    }
}

```



