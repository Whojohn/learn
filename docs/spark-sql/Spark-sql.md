# Spark-sql 

## sql 问题定位方法

```

  - 先查看执行计划(SparkUI)，看看哪个stage慢
  - 慢的stage中，继续检查
    + 是不是并行度不够(task数量很少，每个task都处理很多数据)
      - 增大任务并行度(可能还要关闭spark.sql.adaptive.enabled，自适应有可能会降低并行度)
    + 是不是有性能很差的udf，可以查看该task所在executor进程的jstack情况(看看runnable的线程都在干什么)
      - 比如有explode函数，一行数据可以爆炸出非常多数据(可能会引起executor不停gc甚至oom)
  - 检查慢task所在节点的物理机监控情况
    + 磁盘IO是否有问题
	      - 节点上运行的进程过多？
	        + (同一个sparkApp的多个executor在同一个节点上)是否有标签调度的方法？在yarn上我们内部主要是因为分配策略导致的(单次单台节点尽量为每次请求分配15个以上容器)；
	          - 换成kubernetes的话，可以通过标签调度进行改善
	          - 减少每个executor的cpu核心数量，降低单个节点的并行任务数量
	      - 是否有办法减少磁盘IO
	        + 如果是排序(sortMerge)导致的
	          - 小表的数据量是否可以通过适当增大内存进行广播，降低shuffle的代价(在一定数据量下，shuffle过程会默认先排序)
	          - 尝试增大executor内存(降低溢写到磁盘的次数)
	          - 尝试增大写磁盘buffer的大小(降低溢写到磁盘的次数，较少寻址压力，但是写入量不一定会减少)
	    + 网卡流量是不是异常
	      - shuffle的数据量跟并发太高的话，可能会导致流量异常(降低方式主要是通过减少并发，减少shuffle)
	      - 是不是其他任务进程打满了网卡
	  - 任务本身数据量就非常多，导致shuffle、排序等压力很大
	    + 尝试对任务进行拆分，多个任务并行处理，分别处理一部分数据，降低单个任务全局排序的压力
	  - sortMergeJoin能否转化为broadcastJoin
	
	2. spark任务变慢，排查思路？
	  - 先查看执行计划(SparkUI)，看看哪个stage慢
	  - 慢的stage中，继续检查
	    + 该stage处理的数据量是不是明显增加？
	    + 该stage的tasks数量是不是有明显降低？
	    + 该stage中是不是大部分task都执行完，只剩下部分一直执行？
	      - 数据倾斜
	    + udf是否逻辑异常
	  - executor是否异常
	    + 比如gc时间占比很长(UI可以查看到)
	    + 网络、磁盘等
	  - 文件系统是否有问题
	    + hdfs是不是有慢节点？
```

## Spark sql vs Mr sql 

优势：

计算模型：

1. mr 一几个 jvm 一个 task; spark 一个 jvm 多个 task；（并且 spark task 个数会自动通过内存计算，进一步防止oom ）

内存

1. Rdd + spark sql row 数据序列化，减少gc；
2. 内存+spill 设计（spill 不是必须的）

其他优化

1. Shuffle 有 hash shuffle
2. 内存序列化，减少gc 消耗； 

## spark 数据分发类型（Distribution）

> reference :
>
> https://cloud.tencent.com/developer/article/2019628

- UnknownPartitioning：不进行分区
- SinglePartition：单分区
- RoundRobinPartitioning：在1-numPartitions范围内轮询式分区
- BroadcastPartitioning：广播分区
- HashPartitioning：基于哈希的分区方式
- RangePartitioning：基于范围的分区方式
- PartitioningCollection：分区方式的集合，描述物理算子的输出
- DataSourcePartitioning：V2 DataSource的分区方式

##  join 类型

> reference :
>
> - 小萝卜算子
>
> https://cloud.tencent.com/developer/article/2176232

- 抽象层面

1. map join
2. sort merge join
3. hash join

- 类实现层面

1. SortMergeJoinExec

> 对应分发类型
>
> UnsafeShuffleWriter (Tungsten-sort; 不支持 map 端 combine ,即map端执行agg 逻辑)
>
> SortShuffleWriter

1. ShuffledHashJoinExec

> 对应分发类型
>
>  BypassMergeSortShuffleWriter

1. BroadcastHashJoinExec

> Join 语法：
>
> map join
>
> 对应分发类型
>
> Broadcast

1. BroadcastNestedLoopJoinExec

   > Join 语法：
   >
   > 笛卡尔乘积
   >
   > 对应分发类型
   >
   > Broadcast

##  shuffle 类型

> reference :
>
> https://dataelement.top/2021/02/08/spark-shuffle-internal-part-iii/
>
> https://dataelement.top/2021/02/05/spark-shuffle-internal-part-ii/
>
> https://dataelement.top/2021/02/03/spark-shuffle-internal-part-i/

- Sort
  - SortShuffleWriter(Default)
  - UnsafeShuffleWriter
    - 也叫Tungsten-sort
- BypassMergeSortShuffleWrite

##  内存

> reference :
>
> https://dcoliversun.github.io/spark3-memory-monitoring/
>
> https://spark.apache.org/docs/latest/configuration.html#memory-management



- spark.memory.fraction

- **Execution 内存**：主要用于存放 Shuffle、Join、Sort、Aggregation 等计算过程中的临时数据
- **Storage 内存**：主要用于存储 spark 的 cache 数据，例如RDD的缓存、unroll数据；
- **用户内存（User Memory）**：主要用于存储 RDD 转换操作所需要的数据，例如 RDD 依赖等信息。
- **预留内存（Reserved Memory）**：系统预留内存，会用来存储Spark内部对象。

##  优化

### 倾斜处理

- 倾斜出现的原因：

1. 数据问题： null ，默认值，局部热点导致部分key 过多；
2. Udf 问题

#### join 倾斜

#### windows 倾斜

```
spark.sql.windowExec.buffer.in.memory.threshold  
```

#### agg 倾斜

1. 加盐，先聚合一层，然后再一次聚合；
2. 抽离有倾斜的值

#### Udf 倾斜

### 资源使用优化

- spill 过多

加内存，或者减少 core 数量

- spill 少，内存gc 无压力

增加 container vcore ，变相增加 task 个数



### 小文件

#### 读小文件

```
spark.sql.files.openCostInBytes
```

### 写小文件

```
hints 加混洗
aqe
```

