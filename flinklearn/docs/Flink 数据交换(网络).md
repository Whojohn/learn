# Flink 数据交换(网络)
> reference:
> https://blog.jrwang.me/2019/flink-source-code-data-exchange/#%E5%87%A0%E4%B8%AA%E5%9F%BA%E6%9C%AC%E6%A6%82%E5%BF%B5
> https://flink.apache.org/2019/06/05/a-deep-dive-into-flinks-network-stack/

- 核心问题：

1. Flink 如何进行数据交换？

> 流：
>
> 网络：逻辑处理线程 -> RecordWrite ->ChannelSelector (并且序列化)->  ResultPartition-> netty handle -> socket -> netty handle -> channel -> InputGate -> 逻辑处理线程；
>
> 本地： `buffer`(没有`operator chain`， 通过**ResultSubpartition**视图读取数据，消费者(下游)，不需要像网络一样额外维护一个`buffer`)；
>
>  线程内函数传递(`operator chian`)；
>
> 批：忽略。

2. Flink 批流数据交换的区别？

> 批走的的memory segment + split 的形式。

3. Flink 网络中如何进行数据 `hash`？网络中的`partition`  vs `channel` vs `subtask并行度关系(下游 Task 数)` 关系？

> partition = channel  = subtask并行度关系(下游 Task 数)
>
> hash partition 计算公式：(MathUtils.murmurHash(keyHash) % maxParallelism) * parallelism / maxParallelism
>
> hash ： partition 个数 = channel = subtask并行度关系(下游 Task 数) ; 

5. 网络常见的配置。

> taskmanager.memory.segment-size
>
> execution.buffer-timeout

6. 数据交换`streamRecord`只有用户数据吗？

> 否，还有 `watermark` ，`checkpoint barriar` 等特殊标记，都是通过数据交换机制传递。

7. 数据交换网络连接如何共享

> **同一个`job`下的`subtask`共享`tm`上的连接。** 不同 `job` 的连接不共享。连接根据生成者，消费者，各自组成`Netty server`， `Netty client` 作为 `channel` 的线程写入到对应的数据`buffer`中。

8. 网络和`checkpoint`的关系？

> 网络的消费者`netty handle`具体是 `CheckpointBarrierHandler` ，`EXACTLY_ONCE` 会触发对齐操作， `AT_LEAST_ONCE` 不对其，只在最后一个`barrier`出现的时候进行`checkpoint`操作。 (`handle`控制了数据的流动，进而控制对齐)

9. `network buffer pool` 和 `local buffer pool` 关系

> `local buffer pool`的内存是从`network buffer pool` 申请的

10. network buffer vs netty buffer vs socket buffer

> 如 9 所说，`network buffer ` 只是程序使用的buffer。netty 自生也有 buffer ，socket 自生也有buffer.

11. 在有网络数据的交换的情况下，如何确定`buffer`总内存大小。(注意这里的`buffer`都是数据根据`Flink 自行序列化后存放的数据`)

>- local buffer hash 的情况(没有hash，forward 的情况下是 channel = 1)
>
>发送，接收各自独立内存总buffer（发送，接收的buffer 一样存在buffer）：channel数(slot数) * `taskmanager.network.memory.buffers-per-channel` + 
> `taskmanager.
> network.
> memory.
> floating-buffers-per-gate`   (对于网络来说发送端和接收段的区别是，发送端会分配所有buffer，接收端会有浮动和独占部分。)
>
>| 参数                                                 | 含义                          | 默认值 |
>| ---------------------------------------------------- | ----------------------------- | ------ |
>| taskmanager.network.memory.buffers-per-channel       | 每个 channel 独享的 buffer 数 | 2      |
>| taskmanager.network.memory.floating-buffers-per-gate | Floating Buffer 数量          | 8      |
>
>- nework buffer （对应 `tm`上的 `network` 内存）
>
>> tm中所有任务共享这个buffer
>
>taskmanager.memory.network.fraction: （默认 64m）
>
>taskmanager.memory.network.max: （默认1G）
>
>taskmanager.memory.network.min：(默认 64m)

12. 网络相关的监控

> **Task.XXX 的参数已经是过时了，应该使用 Task.Shuffle.Netty 监控**
>
> 常用网络监测
>
> inPoolUsage(输入`buffer`使用比)
>
> >  **operator chian、本地数据交换下**，不计算。(因为没有 `LocalInputChannels` 直接使用的是上游的`buffer`)
>
> outPoolUsage(输出`buffer`使用比)
>
> > **operator chain下没有数据**。其他都会显示
>
> inputQueueLength
>
> > 根据拓扑图有不同的表现，一般不用这个参数。因为`inputQueueLength`是`network buffer`反序列化后存入`java arraydeque`的对象。(会比**发送\接收内存总buffer要大**)

> reference :
>
> 1. https://flink.apache.org/2019/06/05/flink-network-stack.html （官方简述）
> 2. https://cwiki.apache.org/confluence/display/FLINK/Data+exchange+between+tasks (官方wiki 数据交换方式)
> 3. https://blog.jrwang.me/2019/flink-source-code-data-exchange/#inputgate-%E5%92%8C-inputchannel (源码走读)
> 4. https://flink-learning.org.cn/article/detail/138316d1556f8f9d34e517d04d670626 (网络剖析)
> 5. https://blog.csdn.net/zhanglong_4444/article/details/117093830

13. 网络流量控制协议
> 
> - `Credit-based Flow Control` (走网络时候才有)
> 1. 接收端向发送端声明可用的 Credit（一个buffer = 1个credit）；
> 2. 当发送端获得了 X 点 Credit，表明它可以向网络中发送 X 个 buffer；当接收端分配了 X 点 Credit 给发送端，表明它有 X 个空闲的 buffer 可以接收数据
> 3. 只有在 Credit > 0 的情况下发送端才发送 buffer；发送端每发送一个 buffer，Credit 也相应地减少一点
> 4. 当发送端发送 buffer 的时候，它同样把当前堆积的 buffer 数量（backlog size）告知接收端；接收端根据发送端堆积的数量来申请 floating buffer

## FLink 传输底层

### FLink 网络传输

数据流动整体如下所示

```
-> 逻辑处理线程
-> RecordWriter 数据输出
-> channelSelector 选择写入channel (channel 总数与下游有关，channel 连接的方式根据数据交换方式不同而不同，由`JobGraph` 进行描述；channel 的选择与最大并行度和当前并行度有关。)
-> 计算 partition (以`hash`为例)：(MathUtils.murmurHash(keyHash) % maxParallelism) * parallelism / maxParallelism
-> RecordSerializer 序列化数据
-> 向 ResultPartition 请求 BufferBuilder，用于写入序列化结果

... NettyConnectionManager.server 交互，epoll server 发送(handle 负责发送)
    线程名字Flink Netty Server
-------------> netty socket 传输 `job` 共享连接负责数据传输
    线程名字Flink Netty Client
... NettyConnectionManager.client 交互 epoll client 接收(handle 处理逻辑)

-> 反序列化
-> InputGate 
-> 逻辑处理线程

```

- 写入模型(带有定时刷新)

![Record-writer-to-network-with-flusher-Flink 的网络堆栈](https://flink.apache.org/img/blog/2019-06-05-network-stack/flink-network-stack8.png)



![null](https://img.alicdn.com/imgextra/i2/O1CN01jFTUA81RWXwK5PreI_!!6000000002119-2-tps-1024-576.png)

![null](https://img.alicdn.com/imgextra/i4/O1CN01H0aEgY1mAewhXetKB_!!6000000004914-2-tps-1024-576.png)



### FLink 本地传输

> LocalInputChannel 实现数据的输入，与网络相比，下游直接从上游`buffer`取数据，只有上游才有`buffer`。