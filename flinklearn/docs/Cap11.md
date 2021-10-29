# Flink-11-内存配置相关

> 不同版本的flink 配置有所不一样，`1.10 `是内存模型版本的分界线。

- 注意点

1. `Off-headp`内存中`Managed Memory`:

   > 该部分部分用于 ：`Batch`模式使用；`Rocksdb`使用；`Python`使用；(见官网 https://ci.apache.org/projects/flink/flink-docs-master/zh/docs/deployment/config managed memory 部分)；`因此，假如是存ETL，不涉及状态使用，可以把这部分比例减少`。

2. `Total process memory` vs `Total flink memory`

> `Total flink memory`不包含:`JVM Metaspace`和`JVM Overhead`部分。因此，**`On Yarn`，`K8s`  申请容器大小必须以 `Total process memory` 进行申请，预留足够的空间给`JVM Metaspace`和`JVM Overhead`**。

- 总体结构

  主要由 `Heap`，`OffHeap` 内存构成。源码在`org.apache.flink.runtime.clusterframework.TaskExecutorProcessSpec`中

```
              ┌ ─ ─ Total Process Memory  ─ ─ ┐
               ┌ ─ ─ Total Flink Memory  ─ ─ ┐
              │ ┌───────────────────────────┐ │
               ││   Framework Heap Memory   ││  ─┐
              │ └───────────────────────────┘ │  │
              │ ┌───────────────────────────┐ │  │
           ┌─  ││ Framework Off-Heap Memory ││   ├─ On-Heap
           │  │ └───────────────────────────┘ │  │
           │   │┌───────────────────────────┐│   │
           │  │ │     Task Heap Memory      │ │ ─┘
           │   │└───────────────────────────┘│
           │  │ ┌───────────────────────────┐ │
           ├─  ││   Task Off-Heap Memory    ││
           │  │ └───────────────────────────┘ │
           │   │┌───────────────────────────┐│
           ├─ │ │      Network Memory       │ │
           │   │└───────────────────────────┘│
           │  │ ┌───────────────────────────┐ │
 Off-Heap ─┼─   │      Managed Memory       │
           │  ││└───────────────────────────┘││
           │   └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
           │  │┌─────────────────────────────┐│
           ├─  │        JVM Metaspace        │
           │  │└─────────────────────────────┘│
           │   ┌─────────────────────────────┐
           └─ ││        JVM Overhead         ││
               └─────────────────────────────┘
              └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
```



  ![数据流中的检查点障碍](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/cap11-memory-arch.png?raw=true)


## 1. Heap 内存

  该部分使用JVM 中堆内存，内部没有严格划分，可以混合使用该内存部分。因此监控把框架使用和用户使用部分都放入到一个部分，进行监控。



### 1.1 Framework Heap

  框架使用的内存

- 控制方法

taskmanager.memory.task.heap.size+taskmanager.memory.framework.heap.size

> 默认计算方式是：taskmanager.memory.task.heap.size+taskmanager.memory.framework.heap.size= Total Flink Memory – Framework Heap –  Managed Memory – Network Memory

### 1.2 Task Heap

  任务内存使用的内存：包括：MemoryStateBackend/FsStateBackend (1.13 以后重构了状态量相关`api`，只有`rocksdb`和`非rocksdb`两种，`非Rocksdb`使用的是heap内存)；用户代码运行所需内存等。

## 2. Off Heap 内存

  该部分使用的是直接内存，除了`Managed Memory`，`Network`，`Jvm Metaspace`其他部分没法监控使用情况。

### 2.1 Managed Memory

- 使用者：

1. Rocksdb
2. Python
3. Batch 模式下用于存放数据，执行`sort`，`join`，`shuffle`等操作。

- 控制方法

taskmanager.memory.managed.fraction： 0.4

> Managed Memory 总内存比率

taskmanager.memory.managed.consumer-weights ： OPERATOR:70,STATE_BACKEND:70,PYTHON:30 

> 内存类型只用到一种时，会全部分配出去

### 2.2 Direct Memory

#### 2.2.1 Framwork Off-heap 

   框架保留 off-heap 内存，默认为128mb

#### 2.2.2 Task Off-heap

   任务保留 off-heap 内存，用于调用 native 方法时使用

#### 2.2.3 Network

   网络缓冲区，用于`task` 直接数据交换

- 控制方法

taskmanager.memory.network.fraction：

> 算出来的值不能小于最小值，不能大于最大值

### 2.3 JVM 相关

#### 2.3.1 JVM Metaspace

jvm 元空间

#### 2.3.2 JVM OverHead

保留给JVM其他的内存开销。例如：Thread Stack、code cache、GC回收空间等等