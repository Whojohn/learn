# Flink监控
![flink窗口](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/Flink监控.svg?raw=true)


- 监控的目的

1. 任务是否正常

2. 任务的延时：总体延时，处理各个环节的延时，全链路延时等。

3. 分析定位任务的瓶颈

- 监控的手段

1. Rest api 主动查询
2. Flink metric 上报
3. 物理机层面

> 为什么需要物理机层面？
>
> 1. 磁盘 io 瓶颈，其他手段无法监控
> 2. 网络的 io 是否异常，也难以通过 Flink metric 区分
>
> 3. 线程数异常，句柄异常这些也是无法通过 Flink metric 区分

## 监控手段 Restapi 

> reference:
>
> https://nightlies.apache.org/flink/flink-docs-release-1.12/ops/rest_api.html

> Rest api 一般来说监控的是任务的状态等比较粗粒度的数据，与`Flink metric`不同的是，它是可以主动查询的。(`Flink metric`更多是推的架构，而不是主动拉取的架构)



### 集群层面

- 配置及集群信息

| Rest Api           | 请求Demo                           | 作用                                                         |
| ------------------ | ---------------------------------- | ------------------------------------------------------------ |
| /overview          | http://test:8081/overview          | 集群版本，作业数，作业运行中、完成、失败、取消次数；`slot`总数，可用数 |
| /jobmanager/config | http://test:8081/jobmanager/config | tm, jm 配置信息(flink.yml + 集群默认配置项)                  |
| /taskmanagers      | http://test:8081/taskmanagers      | 获取集群下所有**taskmanager**唯一标识，地址信息，资源(cpu 内存)等简略信息 |

- 日志

| Rest Api                                                     | 请求Demo                                                     | 作用                           |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------ |
| /jobmanager/logs                                             | http://test:8081/jobmanager/log                              | **查看`jm`日志目录下日志列表** |
| /jobmanager/log                                              | http://test:8081/jobmanager/log                              | 查看当前 `jm` 日志             |
| /jobmanager/log/{logfile}                                    | http://test:8081/jobmanager/logs/flink-flink-taskexecutor-8-test.out | 查看`jm`日志目录下特定日志     |
| **/taskmanagers/:taskmanagerid/logs**                        | http://test:8081/taskmanagers/192.168.174.135:32934-b09e28/logs | 查看特定`tm`目录下日志列表     |
| /taskmanagers/:taskmanagerid/log  （先通过 /taskmangers 获取 taskmangerid，再获取日志信息） | http://test:8081/taskmanagers/192.168.174.135:32934-b09e28/log | 查看当前 `tm` 日志             |
| /taskmanagers/:taskmanagerid/logs/{logfile}                  | http://test:8081/taskmanagers/192.168.174.135:32934-b09e28/logs/flink-flink-taskexecutor-8-test.out | 查看特定`jm`目录下日志列表     |



### 作业

| Rest Api                                                     | 请求Demo                                                     | 作用                                                         |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| /jobs                                                        | http://test:8081/jobs                                        | 查看集群作业数                                               |
| /jobs/overview   (检测作业的状态)                            | http://test:8081/jobs/overview                               | 查看集群下作业的缩略信息：作业名字，作业下的task ，以及 task 状态 |
| /jobs/{jobid} (作业明细信息，与web ui 中可见的信息一致)      | http://test:8081/jobs/138288f62cba9641d69399ebdfb2eb11       | 显示逻辑执行图中每个task 的输入，输出失，task 状态明细信息   |
| **/jobs/:jobid/checkpoints** (监控checkpoint 信息)           | http://test:8081/jobs/138288f62cba9641d69399ebdfb2eb11/checkpoints | `checkpoint` 缩略信息，成功，失败次数，最大最小状态量等信息  |
| **/jobs/:jobid/checkpoints/config**                          | http://test:8081/jobs/138288f62cba9641d69399ebdfb2eb11/checkpoints/config | `checkpoint`配置信息                                         |
| /jobs/:jobid (可以用来获取 vertices 进而进一步实现监控)      | http://test:8081/jobs/138288f62cba9641d69399ebdfb2eb11       | 查看特定`job`信息                                            |
| **/jobs/:jobid/vertices/:vertexid/metrics**                  | http://test:8081/jobs/138288f62cba9641d69399ebdfb2eb11/vertices/cbc357ccb763df2852fee8c4fc7d55f2/metrics | 查看特定`job`特定`vertric(task)`下的**可查询**监控指标（**Flink metrics**) |
| **/jobs/:jobid/vertices/:vertexid/metrics**( url 中的操作方法有：get ,agg 对应 metric 数值的不同记录方式) | http://test:8081/jobs/138288f62cba9641d69399ebdfb2eb11/vertices/cbc357ccb763df2852fee8c4fc7d55f2/metrics?get=0.buffers.inPoolUsage | 查看特定`job`特定`vertric(task)`下的**特定**监控指标(**Flink metrics**) |
| **/jobs/:jobid/vertices/:vertexid/subtasks/metrics** (**这里substack 有迷惑性，数据是task下的所有subtask的总和**) | http://test:8081/jobs/138288f62cba9641d69399ebdfb2eb11/vertices/f6dc7f4d2283f4605b127b9364e21148/subtasks/metrics | 查看`subtask`聚合信息指标 (包括1.12前的 isBackPressured)     |
| **/jobs/:jobid/vertices/:vertexid/subtasks/metrics** (**这里substack 有迷惑性，数据是task下的所有subtask的总和,操作方法有：get**) | http://test:8081/jobs/138288f62cba9641d69399ebdfb2eb11/vertices/f6dc7f4d2283f4605b127b9364e21148/subtasks/metrics?get=backPressuredTimeMsPerSecond,busyTimeMsPerSecond | **查看特定指标**                                             |

## Flink metric 

> reference:
>
> https://flink-learning.org.cn/article/detail/1fd44c5dcac8380a90e2bccef9790cc6
>
> https://flink-learning.org.cn/article/detail/1fd44c5dcac8380a90e2bccef9790cc6

### Flink metric 度量

- counter （累加）
- Gauge (瞬间值)
- Meter  (事件次数除以使用的时间)
- Histogram （平均值，中位数等复合统计指标）

### Flink metric 上报结构

```
•TaskManagerMetricGroup
    •TaskManagerJobMetricGroup
        •TaskMetricGroup
            •TaskIOMetricGroup
            •OperatorMetricGroup
                •${User-defined Group} / ${User-defined Metrics}
                •OperatorIOMetricGroup
•JobManagerMetricGroup
    •JobManagerJobMetricGroup
```

### Flink metric 常用指标

- 系统 & jvm 相关

```
Status.JVM.CPU.Load : 为1的时候说明所有分配的cpu被占满(yarn,k8s这里的load都是所分配的cpu计算出来的，而不是物理机占用情况)
Status.JVM.Memory.xxx: 内存相关
Status.JVM.Threads： 线程数(一般用于定位是否存在线程异常如：线程没有正确回收，连接池异常等)

# GC 相关(根据GC算法不同会有不同的指标，以 tm 默认G1算法为例，jm具体算法走jvm 默认配置)
Status.JVM.GarbageCollector	
# tm 监控
flink_taskmanager_Status_JVM_GarbageCollector_G1_Young_Generation_Count
flink_taskmanager_Status_JVM_GarbageCollector_G1_Old_Generation_Count
# 内存分布使用情况(注意内存在 Flink 中分为：heap , offheap（meta,direct）,native;前两者可以被监控，后者无法监控；native :JVM 本身使用、JNI 使用或通过 Java 非安全内部类 sun.misc.Unsafe 申请的内存
,如：glibc Thread Arena(yarn 提示虚拟内存不足))
taskmanager_Status_JVM_Memory_Direct_Count
taskmanager_Status_JVM_Memory_Direct_MemoryUsed
taskmanager_Status_JVM_Memory_Direct_TotalCapacity
taskmanager_Status_JVM_Memory_Heap_Committed
taskmanager_Status_JVM_Memory_Heap_Max
taskmanager_Status_JVM_Memory_Heap_Used
taskmanager_Status_JVM_Memory_Mapped_Count
taskmanager_Status_JVM_Memory_Mapped_MemoryUsed
taskmanager_Status_JVM_Memory_Mapped_TotalCapacity
taskmanager_Status_JVM_Memory_NonHeap_Committed
taskmanager_Status_JVM_Memory_NonHeap_Max
taskmanager_Status_JVM_Memory_NonHeap_Used
```

- **buffer & io 相关** （重点）

```
Status.Shuffle.Netty.inputQueueLength (注意这个指标，这个指标没有太大作用，这里的queue长度是从network buffer 反序列化到 jvm 内存后存放的队列长度，因为会超过 network buffer 长度)
Status.Shuffle.Netty.inPoolUsage (local时候，这里为空)
Status.Shuffle.Netty.outPoolUsage (local remote 都会不为空，比 inPoolUsage 实用)
numRecordsInPerSecond (每秒进入的记录数)
isBackPressured (是否背压，这个参数并不是基于采样，区别于 web ui 上面的 backpressured)
########################
webui 1.13 前是通过以下请求触发采样，不是以上的指标：http://test:8081/jobs/f858d0ed57661659570c4f8e57bff529/vertices/cbc357ccb763df2852fee8c4fc7d55f2/backpressure
#########################
```

- checkpoint

```
lastCheckpointSize 
numberOfCompletedCheckpoints
numberOfFailedCheckpoints
checkpointStartDelayNanos （过大表明有慢处理，背压问题，GC 问题）
```



- 延时(LatencyMarker)

```
# 需要配置 latencyTrackingInterval  才能启用
# 原理是定时从 source 源数据中放入一个特殊标识到数据中，供下游计算延时
.latency
```



