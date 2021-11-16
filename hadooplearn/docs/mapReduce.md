# MapReduce 处理流程&工作原理

> reference :
>
> https://cwiki.apache.org/confluence/display/HADOOP2/HadoopMapReduce
>
> https://hadoop.apache.org/docs/stable/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html
>
> https://hadoop.apache.org/docs/stable/api/org/apache/hadoop/mapreduce/Partitioner.html
>
> https://matt33.com/2016/03/02/hadoop-shuffle/
>
> <<Hadoop技术内幕>>
>
> 

- 核心知识点

    1. **官方文档明确的说明了`map`，`reduce`阶段都是有排序。**不同的是`reduce`一般只需要一次归并排序即可，`reduce`的处理必须经过这次处理才能开始。`map`阶段是先处理，然后执行多次归并排序，最终输出`map`处理文件。

2. 数据在`mapreduce`中都是以<key,value>的方式进行流动，目的是保证内部有序。
3. 如下图所示，`mapreduce`分为以下几个阶段：`map`，`sort`，`shuffle`，`merge`，`reduce`。

![mapreduce处理详细流程](https://github.com/Whojohn/learn/blob/master/hadooplearn/docs/pic/map_reduce_detail_process.png?raw=true)


![mapreduce处理大致流程](mapreduce_process.png)


## 1. Map 阶段

> map 阶段包括了:`map`，`store`操作。

### 1.1 map 执行流程

1. map逻辑执行：inputsplit（注意区分splitable）等处理读取的数据位置，按照`mapper`逻辑进行处理，输出到Partitioner。
2. partition 阶段：mapper 函数处理后，通过`Partitioner`根据`hash(map key)%reduce number`写入到对应的`buffer 环中`，`buffer 环`个数与`reduce`个数一致（buffer 大小由`mapreduce.task.io.sort.mb`控制）。
3. spill阶段：当`buffer 环`内存使用满足一定比率(mapreduce.map.sort.spill.percent控制),执行排序，排序后文件输出到`mapreduce.job.local.dir`中暂存。
4. combine开启：假如开启`combine`会在`spill阶段`排序后执行`combine`操作(相当于map agg)，`combine`可以通过`min.num.spill.for.combine`控制，即当`spill`文件达到一定数量时，提前触发运算。
5. Merge 阶段：前面步骤都执行完成后，最终会把`spill`文件进行归并排序处理，输出唯一map文件，通过`mapreduce.task.io.sort.factor`控制同时合并文件的并发数。
6. Compress: 开启压缩，减少io消耗。`mapreduce.map.output.compress`，`mapreduce.map.output.compress.codec`；参数控制。

- buffer 环

> 首尾指针标识的一个缓存环，当需要溢写的时候，通过标识溢写的范围，锁定该部分内存的写入，不干扰剩余内存的使用。

- inputsplit

> inputsplit 解决的是每一个`map`处理的数据量。具体实现有文件拆分读，合并文件读(小文件优化)等多种实现。**注意`splitable`这个参数的不同，`splitable`参数是跟压缩算法有关，假如压缩算法不支持`split`，意味着必须先解压，然后再执行数据的切分。假如支持每个`map`只需解压需要的数据部分即可。**

### 1.2 map 个数

> reference:
>
> https://github.com/apache/hadoop/blob/trunk/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapreduce/lib/input/FileInputFormat.java （输入处理中除了处理格式还处理了文件的切分，文件的切分数在`splitable`的情况下等同于`map`个数）
>
> https://zhuanlan.zhihu.com/p/76871203

**map 个数涉及参数：**

> mapreduce.input.fileinputformat.split.minsize //启动map最小的split size大小，默认0
> mapreduce.input.fileinputformat.split.maxsize //启动map最大的split size大小，默认256M
> dfs.block.size//block块大小，默认128M
> 计算公式：splitSize =  Math.max(minSize, Math.min(maxSize, blockSize)) 

    计算公式：splitSize =  Math.max(minSize, Math.min(maxSize, blockSize))
    
        下面是FileInputFormat class 的getSplits()的伪代码： 
          num_splits = 0
          for each input file f:
             remaining = f.length
             while remaining / split_size > split_slope:
                num_splits += 1
                remaining -= split_size
          where:
            split_slope = 1.1 分割斜率
            split_size =~ dfs.blocksize 分割大小约等于hdfs块大小
    
    会有一个比例进行运算来进行切片，为了减少资源的浪费
    例如一个文件大小为260M，在进行MapReduce运算时，会首先使用260M/128M，得出的结果和1.1进行比较
    大于则切分出一个128M作为一个分片，剩余132M，再次除以128，得到结果为1.03，小于1.1
    则将132作为一个切片，即最终260M被切分为两个切片进行处理，而非3个切片。  

### 1.3 map 参数

- map 个数，处理数量相关

```
mapreduce.input.fileinputformat.split.minsize
mapreduce.input.fileinputformat.split.maxsize
mapreduce.input.fileinputformat.split.minsize.per.node
```

- cpu 内存

```
# 总体资源
mapreduce.map.memory.mb 
mapreduce.map.cpu.vcores
```

- 其他配置

```
# buffer 环大小配置
mapreduce.task.io.sort.mb
# buffer 环spill 执行的时机，默认为80%的buffer环
mapreduce.map.sort.spill.percent
# spill 执行排序，同时可以合并的文件数量，默认为10
mapreduce.task.io.sort.factor
# combime 执行时间(默认为3)
min.num.spill.for.combine
# 最终map文件是否开启压缩
mapreduce.map.output.compress
```

## 2. Reduce 阶段

> Reduce 阶段第一步是通过 `shuffle（copy；两个名词的含义一致，官方说法）`执行合并文件，只有合并完成后。才执行`reduce`逻辑处理。

### 2.1 reduce 执行流程

1. Copy阶段：启动独立的`jvm`,从`map`中按照不同的分区路由复制到节点中，复制过程会类似的先放入内存，后写入到磁盘上，内存通过`mapreduce.reduce.shuffle.input.buffer.percent`(reduce总内存比率)控制。
2. Merge阶段: 将输入排序，并行执行边输入，边排序，最终形成一个唯一的文件。
3. reduce逻辑执行：唯一文件执行`reduce`处理逻辑。

### 2.2 reduce 个数

默认`reduce`个数为`1`，用户可以通过`mapred.reduce.tasks`配置，`reduce`。

### 2.3 reduce 参数

- cpu 内存配置

```
mapreduce.reduce.cpu.vcores
mapreduce.reduce.memory.mb	
```

- 其他配置

```
# copy(shuffle) 并发处理 map 个数
mapreduce.reduce.shuffle.parallelcopies	
```



## 3. mapreduce 总结

1. jvm 复用：mapreduce.job.jvm.numtasks：10

> jvm 复用缺点是会在任务结束前，锁定资源。比如`map`阶段需要10个容器，现在只省下`4个`容器正在运行，剩余`6个`容器的资源不会释放。

2. 多个`map reduce`嵌套，需要启动多轮次`map reduce`。如：hive 会把执行分成多个`stage`，每个`stage`是独立的`mapreduce`任务。

