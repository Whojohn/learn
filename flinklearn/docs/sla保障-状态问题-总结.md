# sla保障-状态问题-总结

>  reference :
>
>  https://nightlies.apache.org/flink/flink-docs-master/docs/ops/state/large_state_tuning/
>
>  https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/deployment/config/

- 什么是大状态？一个没有开启sp 有100g 算大吗？大状态的定义是啥？（应该把大状态问题拆分为state使用问题，然后分析，拆解出方案）

```
一个任务sp有100g，用了用了32个slot ，也就是3g 一个，每一个slot 有2g manager memory ，根本不大；
假如把slot 变为6，单个slot 状态就15g 左右，这样状态就大？
假如把manager memory 变为 500m 单个slot state 有 15g 的情况下呢？
大状态其实是一个无法量化的标准，但是state 我们可以量化很多东西，我们需要把使用到state 的场景拆解，才能把大状态，状态问题解决好。
```

- 状态相关问题如何解决

```
思考的维度：
事前，事中，事后；
io,cpu 两者平衡；
写入，读取平衡；
热点代码分析（特定场景的优化），metric , log 定位排查问题；
```



## 监控&排查问题手段

- 主机层面

> cpu io 

- Rocksdb 层面 metric 监控

>写入监控
>
>state.backend.rocksdb.metrics.is-write-stopped
>
>state.backend.rocksdb.metrics.actual-delayed-write-rate
>
>结合stop  delay 分析
>
>state.backend.rocksdb.metrics.compaction-pending
>
>state.backend.rocksdb.metrics.mem-table-flush-pending
>
>高版本- state.backend.rocksdb.metrics.bytes-written
>
>读监控（读相关指标都比较模糊, 高版本比较好衡量，低版本很难衡量）
>
>高版本-state.backend.rocksdb.metrics.iter-bytes-read
>
>高版本-state.backend.rocksdb.metrics.block-cache-hit
>
>高版本-state.backend.rocksdb.metrics.block-cache-miss
>
>state.backend.rocksdb.metrics.block-cache-usage
>
>state.backend.rocksdb.metrics.estimate-table-readers-mem

- Rocksdb log 日志

> 注意1.13 需要登陆到物理机器才能查看该日志，1.15 `webui`能直接可见。
>
> state.backend.rocksdb.log.level

- flink 层面 （tm层面）

> 热点图
>
> rest.flamegraph.enabled
>
> Thread dump
>
> Busy  



## 状态涉及的问题

### 故障恢复慢

- 故障恢复是否需要重新分配`tm`容器；（`oom`,`内存溢出`，`heartbeat.timeout`会导致`tm`重新分配。）

> 解决办法：
>
> 预先分配资源，减少容器申请时间            slotmanager.redundant-taskmanager-num 

- 故障恢复需要重新拉取`state`文件

> state.backend.local-recovery                                                               尝试本地恢复（本地恢复会与部分特性使用存在冲突）
>
> state.backend.rocksdb.checkpoint.transfer.thread.num               rocksdb 下载文件并发
>
> state.backend.rocksdb.compaction.level.target-file-size-base     大状态可以增大这个（充分利用顺序写）



### 状态启动慢

> referenece:
>
> https://github.com/facebook/rocksdb/wiki/RocksDB-Tuning-Guide

- 修改并行度后恢复慢 

**分析：**

> RocksDBFullRestoreOperation
>
> 

情况1: 从`sp`启动。流程如下： `sp`文件按照`keygroup`下载对应的数据，将`kv`数据以`byte[]` 通过`writebatch`写入到本地`rocksdb`；

情况2: 从`ck`启动。流程如下： `ck`文件按照`keygroup`下载对应的`rocksdb sst`文件，启动一个临时`rocksdb`实例，导入下载的`sst`文件，迭代这个实例的所有数据，调用`rocksdb`的`writebatch`写入到一个最终`state rocksdb`（状态裁剪），销毁临时实例；

*！！！注意！！！*

> reference : 高版本官方提高性能方案（delete range）
>
> https://issues.apache.org/jira/browse/FLINK-21321
>
> https://issues.apache.org/jira/browse/FLINK-31238
>
> flink 1.16后从优化启动速度，比如利用`deleterange`跳过无关数据。更高版本，进一步优化`31238`。但是，仍然需要调用`writeBatch`重新写入数据到新的`rocksdb` 实例。

总结：

1. 都依赖了`writebatch` api ,因此优化写入能够加速恢复。(写入提高方法略)
2. 并行度修改，可能会很慢。都有一个迭代数据，写入数据的过程。可以简单看作是一个生产，消费者模型。两者都需要提高才能提高恢复速度。其中，对于`sp`恢复来说，**生产者有一个严重的性能缺陷，本地文件读取无`buffer` 
   io， hdfs inputstream 有 buffer（hdfs client 自身能力） 但是由于加了额外的统计信息会导致性能很差**。对于低于`1.
   18` flink 来说，官方没有关闭`wal`，会导致无效`io`。

> 无`buffer io` 提高 （生产者cpu缺陷问题）
>
> https://issues.apache.org/jira/browse/FLINK-19911
>
> 关闭`wal`优化
>
> https://github.com/apache/flink/pull/21015



### state 有了ttl 也很大怎么办

- 事前

> State 大小：
>
> - 业务优化序列化的角度：
>
>   1.自己声明序列化改为 probuff （字节内部有不少业务线自自定义序列化方式）；把数据编码为 probuff byte[] 赛进去 state； （flink原生序列化不够稠密）
>
>   2.优化字段组织：枚举类型的不应该用string, 考虑用 int 等编码代表；int 表达多个字段（把多个4 bit 等字段编码为一个int 来表达，信息更加稠密）；
>  
> - 底层&业务理解底层
>
> 1. 类型kryo 回退问题；使用了map, list 等容器类型，导致序列化退化为kryo，而不是flink 内部最优类型（TypeSerializer）。（jar 用户最常见，读写影响都大）
> 2. Rocksdb 内存不足，读写线程不足，block size ，write buffer太小等，还有一个最重要的参数就是state.backend.rocksdb.memory.partitioned-index-filters !!!（cache 划分独立的 sst bloom filter 内存，防止写入，读取污染bloom filter buffer；部分场景能有倍数提升） ；（平台根据用户的内存，提供一个合理的默认配置）
>3. Rocksdb 压缩问题； 默认关闭 rocksdb snappy 压缩；我们可以调整为rocksdb分层压缩；首层不压缩， 1～5层用lz4，后面的层用zstd；（20%的提升；io 换cpu 前提是io 不是瓶颈）

- 事中

1. 依靠mertic , 写入核心是：`state.backend.rocksdb.metrics.is-write-stopped` , 不要stw ，stw 就是世界末日；读取是`cache-miss` （注意 fix-per-slot 开了，可能有些指标就不准确了。）
2. 合理的`checkpoint` 间隔，过小的`ck`间隔会导致吞吐下降。

- 事后

1. 总结问题案例，把一些问题通过规则扫描，转变为事前避免。

### 写入优化

- flink 原生可配置部分

```
懒人配置：
state.backend.rocksdb.predefined-options : SPINNING_DISK_OPTIMIZED_HIGH_MEM

state.backend.rocksdb.memory.write-buffer-ratio
state.backend.rocksdb.block.blocksize
state.backend.rocksdb.compaction.level.max-size-level-base
state.backend.rocksdb.compaction.level.target-file-size-base
state.backend.rocksdb.compaction.level.use-dynamic-size
state.backend.rocksdb.thread.num
```

- 自定义RocksDBOptionsFactory 解锁 rocksdb 更多细节

```
- flush 线程，重中之重！！！
> 默认flush 线程为1,虽然每个slot 是根据column family 隔离，但是1.13 来说 5.x rocksdb ，这个flush 是总可用线程数，并且不自定义 factory ,或者修改源码，用户没有任何参数可以控制。

# 1.13 用的rocksdb 版本 flush 线程是反着配置的 （5.x rocksdb java bug）
DBOptions.getEnv().setBackgroundThreads(2,Priority.LOW);
高版本
DBOptions.getEnv().setBackgroundThreads(2,Priority.HIGH);

# 1.15 以及以上版本rocksdb 配置方式
DBOptions.setIncreaseParallelism(6);
DBOptions.setMaxBackgroundFlushes(3);


# 调整rocksdb 压缩层级
List<CompressionType> compressionTypeList =
                    new ArrayList<>();
            try (final Options options = new Options()) {
                for (int i = 0; i < options.numLevels(); i++) {
                    if (i < 2) {
                        compressionTypeList.add(CompressionType.NO_COMPRESSION);
                    } else (i<5) {
                        compressionTypeList.add(CompressionType.LZ4_COMPRESSION);
                    }else{
                       compressionTypeList.add(CompressionType.ZSTD_COMPRESSION);
                    }
                }

            }
currentOptions.setMinWriteBufferNumberToMerge(3).setMaxWriteBufferNumber(4);
currentOptions = currentOptions.setCompressionPerLevel(compressionTypeList);


# 并发写muntable
currentOptions.setAllowConcurrentMemtableWrite(true);
currentOptions.setEnableWriteThreadAdaptiveYield(true);
```



### 读优化

- flink 原生可配置部分

```
懒人配置：
state.backend.rocksdb.predefined-options : SPINNING_DISK_OPTIMIZED_HIGH_MEM

state.backend.rocksdb.memory.partitioned-index-filters
state.backend.rocksdb.block.cache-size


```









