# 概念&常用术语

> reference:
>
> https://nightlies.apache.org/flink/flink-docs-master/docs/concepts/glossary/
>
> https://izualzhy.cn/flink-source-job-graph
>
> https://niyanchun.com/flink-quick-learning-graph.html
>
> https://matt33.com/2019/12/20/flink-execution-graph-4/
>
> https://ci.apache.org/projects/flink/flink-docs-master/api/java/org/apache/flink/streaming/api/graph/package-summary.html
>
> https://ci.apache.org/projects/flink/flink-docs-master/api/java/org/apache/flink/runtime/jobgraph/JobGraph.html
>
> https://ci.apache.org/projects/flink/flink-docs-master/api/java/org/apache/flink/streaming/api/graph/StreamingJobGraphGenerator.html
>
> https://ci.apache.org/projects/flink/flink-docs-master/api/java/org/apache/flink/streaming/api/graph/package-summary.html
>
> 



## 1. 处理模型相关

> flink 把一个任务的处理过程分为以下几个阶段，这些阶段都是通过  `operator(算子) `  进行串联。

- source

输入源。输入源也是 `operator` 的一种

- transformation 

处理过程：包括 map , flatMap ,  keyBy , join ,  timeWindows ，reduce, agg  , 还有更底层的 `ProcessFunction ` 等算子。

- sink

输出目标。

## 2. Api 层面

Flink Api 从高处往低，分为以下几层：

- sql
- table api
- stream /  dataset api （flink core，process function 子类实现(operator 实现)，以及子类实现）
- stateful steam processing (flink 底层，process funciton 的更底层实现)

### 3. Flink 集群概念

>注意官网说 jobgraph 的节点是 operator，实际上 JobGraph 这个类生成的执行图已经是算子链优化后的节点。StreamGraph 节点才是 operator。

- record

    Record 是数据集或数据流的组成元素。Operator和Function接收 record 作为输入，并将 record 作为输出发出。

- event

    Event 是对应用程序建模的域的状态更改的声明。它可以同时为流或批处理应用程序的 input 和 output，也可以单独是 input 或者 output 中的一种。Event 是特殊类型的 Record。

- function

用户实现操作的方法，封装了用户具体处理逻辑 ，`operator` 的具体实现 。

- Flink Application Cluster
- Flink Job Cluster

- state

       状态， checkpoint ，sum ，count 累加，uv , pv 计算都是基于 state 来做的，甚至用户自定义 bloom filter 都可以通过 state 来做。1.9 后 flink 默认只有 `managed state`，假如需要使用 `raw state` 需要通过 `byte[]` 来进行实现，并且无法利用`Flink`类型优化。`managed state ` 分为：`Keyed State` 和`Operator State`。  `Keyed State` 只能用于 keyed 后的 stream。

- state backend

       状态后端

- taskmanager
- jobmanager


### 4. 执行图(任务启动)
reference: https://blog.csdn.net/baichoufei90/article/details/108274922

- StreamGraph(这个类是 flink 内部类，对用户来说是没有这个概念的)

     用户代码生成的最初执行图, 包含任务所有启动信息。在`StreamGraph`中，节点`StreamNode`就是算子。

- JobGraph (Logical Graph；不考虑并行化的工作流，只负责算子链优化，官网把JobGraph和ExecutionGraph作为等同看待)

    `StreamGraph` 调用 `StreamingJobGraphGenerator` 生成 `JobGraph`。`JobGraph` 中 `JobVertex` 作为 `task` 的一个节点，通过算子链的形式，放入多个 `operator`， 这样可以减少数据交换所需要的传输开销(算子链的实现)。

- ExecutionGraph (**官网把ExecutionGraph等同 PhysicalGraph** 分配资源化后的并行工作流，主要处理slot 的分配)

    JobManager将`JobGraph`转化为`ExecutionGraph`。`ExecutionGraph`是`JobGraph`的并行化版本：假如某个`JobVertex`的并行度是2，那么它将被划分为2个`ExecutionVertex`，`ExecutionVertex`表示一个算子子任务，它监控着单个子任务的执行情况。每个`ExecutionVertex`会输出一个`IntermediateResultPartition`，这是单个子任务的输出，再经过`ExecutionEdge`输出到下游节点。`ExecutionJobVertex`是这些并行子任务的合集，它监控着整个算子的运行情况。

- Physical Graph

     这个是虚拟的概念，只是表面具体实例化的对象。JobManager根据`ExecutionGraph`对作业进行调度后，在各个TaskManager上部署具体的任务，物理执行图并不是一个具体的数据结构。


#### 5. 资源相关
- operator

   Logical Graph的节点。算子执行某种操作，该操作通常由Function实现。Source 和 Sink 是数据输入和数据输出的特殊算子。

- operator chain

算子链，flink 把 多个算子串联，放入一个 task 中，每一个 task 都是一个线程。

- task 

**线程组，多个相同并行度算子、算子链接运行所在的线程组，由同类 subtask 组成。__**。

- subtask

**单线程，单个算子，算子链，运行所在的线程。**

- partition
**task 中的数据集合。**


- Flink job

单个`flink` 任务,由多个`task`构成.

- Flink appliation

Flink 应用，可以是单或多个Flink job


