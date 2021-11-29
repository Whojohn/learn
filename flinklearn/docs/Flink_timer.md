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
4. `Timer`可以注册，也可以取消。注册大量`Timer`会消耗内存和性能，`Timer`的不应该频繁使用。

5. 对于`ProcessTime`下`Timer`是`regisXXXTimer`把`ontime`操作放入到队列中，并且发起定时线程(`ScheduledThreadPoolExecutor.schedule`)，定时投递事件到`mailbox`，触发`mailbox`事件处理队列中的数据。对于`EventTime`下`Timer`是`regisXXXTimer`把`ontime`操作放入到队列中，数据流中插入`WaterMark Event`，触发检测队列中的`Timer`执行。**他们都会在 regisXXX Time 阶段把事件放入到队列中，但是触发的机制不一样。**



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





