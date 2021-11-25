# Flink 工作线程 & 工作线程并发控制

> reference :
>
> https://docs.google.com/document/d/1eDpsUKv2FqwZiS1Pm6gYO5eFHScBHfULKmH1-ZEWB4g/edit#heading=h.me9nb71lvmah  （mailbox 原始设计文档）
>
> https://mp.weixin.qq.com/s/MrQZS7-dEuNr442lzw3xYg (Flink 官方微信)
>
> https://guosmilesmile.github.io/2020/06/03/Flink%E5%9F%BA%E4%BA%8Emailbox%E5%AE%9E%E7%8E%B0%E7%9A%84streamTask%E6%A8%A1%E5%9E%8B/
>
> https://matt33.com/2020/03/20/flink-task-mailbox/

- 先验知识(flink 层级结构)

1. job -> task(线程组) -> subtask（独立线程；工作线程）;
2. operator
3. operator chain
4. mailbox 机制

## 1. SubTask 是如何实现(Flink 工作线程的实现)

> `Flink`中的`subtask`是通过`org.apache.flink.runtime.taskmanager.Task` 和 `org.apache.flink.streaming.runtime.tasks.StreamTask` 实现的。
>
> 前者实现`runnable`接口，类加载，线程初始化等线程初始化操作。 后者是`mail box` 加具体逻辑实现抽象类，我们应该重点关注后者。

```
# 调用链条
poll:538, ArrayDeque (java.util)
poll:175, PrioritizedDeque (org.apache.flink.runtime.io.network.partition)
getChannel:932, SingleInputGate (org.apache.flink.runtime.io.network.partition.consumer)
waitAndGetNextData:644, SingleInputGate (org.apache.flink.runtime.io.network.partition.consumer)
getNextBufferOrEvent:626, SingleInputGate (org.apache.flink.runtime.io.network.partition.consumer)
pollNext:612, SingleInputGate (org.apache.flink.runtime.io.network.partition.consumer)
pollNext:109, InputGateWithMetrics (org.apache.flink.runtime.taskmanager)
pollNext:148, CheckpointedInputGate (org.apache.flink.streaming.runtime.io)
emitNext:179, StreamTaskNetworkInput (org.apache.flink.streaming.runtime.io)
processInput:65, StreamOneInputProcessor (org.apache.flink.streaming.runtime.io)
processInput:398, StreamTask (org.apache.flink.streaming.runtime.tasks)

# mail box 逻辑
runDefaultAction:-1, 1040369514 (org.apache.flink.streaming.runtime.tasks.StreamTask$$Lambda$777)
runMailboxLoop:191, MailboxProcessor (org.apache.flink.streaming.runtime.tasks.mailbox)
# streamtask 控制流程
runMailboxLoop:619, StreamTask (org.apache.flink.streaming.runtime.tasks)
invoke:583, StreamTask (org.apache.flink.streaming.runtime.tasks)
# task 类初始化线程
doRun:758, Task (org.apache.flink.runtime.taskmanager)
run:573, Task (org.apache.flink.runtime.taskmanager)
run:834, Thread (java.lang)
```



```
# StreamTask 核心操作过程：
 -- invoke()
       |
       +----> Create basic utils (config, etc) and load the chain of operators
       +----> operators.setup()
       +----> task specific init()
       +----> initialize-operator-states()
       +----> open-operators()
       +----> run()
			 --------------> mailboxProcessor.runMailboxLoop();
			 --------------> StreamTask.processInput()
			 --------------> StreamTask.inputProcessor.processInput()
 			 --------------> 间接调用 operator的processElement()和processWatermark()方法       
       +----> close-operators()
       +----> dispose-operators()
       +----> common cleanup
       +----> task specific cleanup()
```

![flink_StreamTask_work_flow](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/flink_StreamTask_work_flow.png?raw=true)



## 2. SubTask 线程并发如何控制?

> 通过 mailbox 机制控制

## 3. mailbox 为什么可以保证并发安全？

> mailbox 把所有操作都放入到一个线程安全的队列中，任何需要执行的操作都需要放入这个队列，执行这个操作只有一个线程，这个线程去取队列中的操作。相当于对某个对象只有单线程操作，不可能发生并发操作，保证该对象安全。

![flink_source_mailbox](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/flink_source_mailbox.png?raw=true)

## 4. flink 什么操作会有并发安全问题？

> - mailbox 解决的并发安全问题（下文主要讨论这个处理过程）
>
> 1. 事件处理：包括 events, watermarks, barriers, latency markers 的处理和发送
>
> 2. checkpoint 触发：通过 RPC 调用触发 checkpoint（在 Source 中）、通知 checkpoint 的完成情况，（注：对下游来说，checkpoint 触发和取消是通过 barrier 触发的，归为第一种情况）
>
> 3. Processing Time Timers: 处理时间定时器是通过 `ScheduledExecutor` 异步执行的（事件事件定时器触发是通过 watermark 触发的，归为第一种情况）
>
> - 非 mailbox 并发安全
>
> `state ttl`，使用`rockdb`可见性为`过期也可见`，同时配了`ttl`，可能读取和删除并发发生(ttl后台运行删除逻辑)。

## 5. Event / Process Timer 实现底层与 StreamTask 运行流程

## 5.1 ProcessTime Timer 底层原理

​      `process time`有 `mailbox`机制，时间的处理通过`mailbox`，投递请求，调用`StreamTask`实现。`mail`的投递是`SystemProcessingTimeService `开启定时线程实现的。

```
定时线程        										工作线程
														|1. 用户 jar       : ctx.timerService().registerProcessingTimeTimer 
														|2. streaming.api  : SimpleTimerService.registerProcessingTimeTimer
														|3. streaming.api  : InternalTimerServiceImpl.registerProcessingTimeTimer
				<------------------------------------- 				                |4. runtime.tasks  : ProcessingTimeService.registerTimer // 发起一个定时线程
				|										|
				|										|  StreamTask processinput 处理每一条数据
				|										|
定时线程发起 mailbox 投递----------------------------->	| 放入到 mailbox 队列中
														|
														| mailbox 每一轮次检测，检测到 mailbox 
														| 队列不为空，停止逻辑处理，处理 mail 事件
														| 调用该 Timer 对应的ontime 函数，执行操作；
														| 完成 mailbox 中所有 mail 事件处理
														|
														| StreamTask processinput 处理每一条数据
```



```
# processtime ontimer 处理堆栈
onTimer:54, TestProcessFunction
invokeUserFunction:96, LegacyKeyedProcessOperator (org.apache.flink.streaming.api.operators)
onProcessingTime:81, LegacyKeyedProcessOperator (org.apache.flink.streaming.api.operators)
onProcessingTime:284, InternalTimerServiceImpl (org.apache.flink.streaming.api.operators)
onProcessingTime:-1, 1809112443 (org.apache.flink.streaming.api.operators.InternalTimerServiceImpl$$Lambda$879)
invokeProcessingTimeCallback:1324, StreamTask (org.apache.flink.streaming.runtime.tasks)
lambda$null$17:1315, StreamTask (org.apache.flink.streaming.runtime.tasks)
run:-1, 396345110 (org.apache.flink.streaming.runtime.tasks.StreamTask$$Lambda$882)
runThrowing:50, StreamTaskActionExecutor$1 (org.apache.flink.streaming.runtime.tasks)
run:90, Mail (org.apache.flink.streaming.runtime.tasks.mailbox)
processMail:317, MailboxProcessor (org.apache.flink.streaming.runtime.tasks.mailbox)
runMailboxLoop:189, MailboxProcessor (org.apache.flink.streaming.runtime.tasks.mailbox)

# 处理 mailbox 时候处理 process time 逻辑 ，说明 process time 需要外部投递 mail 事件
runMailboxLoop:619, StreamTask (org.apache.flink.streaming.runtime.tasks)
invoke:583, StreamTask (org.apache.flink.streaming.runtime.tasks)
doRun:758, Task (org.apache.flink.runtime.taskmanager)
run:573, Task (org.apache.flink.runtime.taskmanager)
run:834, Thread (java.lang)
```

## 5.2 EventTime Timer 原理

`event time`有`mailbox`机制，**但是时间处理是通过`StreamTask逻辑`实现，不需要进行投递**。不需要使用额外线程触发和处理 `event time`。

```
定时触发生成watermark			|工作线程的上游线程(数据发送给下游)							    工作线程
|								|																|1. 用户 jar       : ctx.timerService().registerEventTimeTimer 
|								|																|2. streaming.api  : SimpleTimerService.registerEventTimeTimer
 ------------------->			|            													|3. streaming.api.operators  : InternalTimerServiceImpl.registerEventTimeTimer 
|								StreamTask processinput 处理每一条数据							   // 把事件插入到队列中
|								StreamTask processinput 处理每一条数据							|
|								收到mailbox	，生成 WaterMark 以放入到数据流，发送给下游			|
|								|																|  StreamTask processinput 处理每一条数据
|								|																|  StreamTask processinput 处理每一条数据
|								|																|  StreamTask processinput 处理每一条数据
|								|																	// 数据流中传入了watermark 事件，触发WaterMark处理
|								|																						|
|								|																						|
|								|																						-> | WaterMark 触发处理
|								|																						| 判定触发的 WaterMark 跟队列中需要触发的 Eventtime 时间
|								|																						| 取出时间小于 等于 WaterMark 的事件，处理事件（不需要mailbox 投递）
|								|																|						   
|								|																|	 StreamTask processinput 处理每一条数据					   
|								|																|    StreamTask processinput 处理每一条数据
|								|																|    StreamTask processinput 处理每一条数据
|								|																|
|								|																|
|								|																|
```

```
# eventtime ontimer 处理堆栈

onTimer:54, TestProcessFunction
invokeUserFunction:96, LegacyKeyedProcessOperator (org.apache.flink.streaming.api.operators)
onEventTime:75, LegacyKeyedProcessOperator (org.apache.flink.streaming.api.operators)
advanceWatermark:302, InternalTimerServiceImpl (org.apache.flink.streaming.api.operators)
advanceWatermark:194, InternalTimeServiceManagerImpl (org.apache.flink.streaming.api.operators)
processWatermark:626, AbstractStreamOperator (org.apache.flink.streaming.api.operators)
emitWatermark:197, OneInputStreamTask$StreamTaskNetworkOutput (org.apache.flink.streaming.runtime.tasks)
findAndOutputNewMinWatermarkAcrossAlignedChannels:196, StatusWatermarkValve (org.apache.flink.streaming.runtime.streamstatus)
inputWatermark:105, StatusWatermarkValve (org.apache.flink.streaming.runtime.streamstatus)
processElement:206, StreamTaskNetworkInput (org.apache.flink.streaming.runtime.io)
emitNext:174, StreamTaskNetworkInput (org.apache.flink.streaming.runtime.io)
processInput:65, StreamOneInputProcessor (org.apache.flink.streaming.runtime.io)
processInput:398, StreamTask (org.apache.flink.streaming.runtime.tasks)
runDefaultAction:-1, 203985065 (org.apache.flink.streaming.runtime.tasks.StreamTask$$Lambda$773)

# 执行逻辑阶段执行 event time 逻辑
runMailboxLoop:191, MailboxProcessor (org.apache.flink.streaming.runtime.tasks.mailbox)
runMailboxLoop:619, StreamTask (org.apache.flink.streaming.runtime.tasks)
invoke:583, StreamTask (org.apache.flink.streaming.runtime.tasks)
doRun:758, Task (org.apache.flink.runtime.taskmanager)
run:573, Task (org.apache.flink.runtime.taskmanager)
run:834, Thread (java.lang)
```







