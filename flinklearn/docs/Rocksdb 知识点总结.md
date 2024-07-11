# Rocksdb 知识点总结

核心概念

- Compaction VS Compression
- 内存相关
- 写入
- 读取
- 监测指标
- 写入放大 vs 空间放大 vs 读放大

## Rocksdb 核心概念

> reference :
>
> https://github.com/facebook/rocksdb/wiki/Terminology  （官方文档里有更详细的说明）

**LSM-tree**:  参考 wiki 定义 https://en.wikipedia.org/wiki/Log-structured_merge-tree RocksDB 是基于 LSM-tree-based 的存储引擎.

**LSM level**: 逻辑上 lsm 物理文件的层级；l0 -> l1 -> .... 

**Base Level**: 一般为 l1 层，排序后的层级；

**SST file**:SST是Sorted Sequence Table（排序队列表）。他们是排好序的数据文件。在这些文件里，所有键都按照排序好的顺序组织，一个键或者一个迭代位置可以通过二分查找进行定位。

**Table Format**:  sst 物理文件的组织方式

**block-based table**：默认的SST文件格式

**Memtable** / **write buffer**:  l0 层（未排序的数据，rocksdb 默认实现是 skiplist）;

**Block cache**：内存控制（可以配合 writebuffermanager 同时控制写入的内存）

**Block**：SST文件的数据块。一般都checksum且压缩。

**block based bloom filter/bloom filter**： 两种不同的SST文件里的bloom filter存储方式。

**checkpoint**:  快照

**column family**：列族是一个DB里的独立键值空间。(类似表的概念，可以不同 column family 直接可以看作是独立的)

**Compaction**：将一些SST文件合并成另外一些SST文件的后台任务。LevelDB的压缩还包括落盘。在RocksDB，我们进一步区分两个操作。

**compaction filter**：一种用户插件，可以用于在压缩过程中，修改，或者丢弃一些键。

**comparator**：一种插件类，用于定义key的顺序。

**2PC/两阶段提交**：悲观事务可以通过两个阶段进行提交：先准备，然后正式提交。

**Pessimistic Transactions**：用锁来保证多个并行事务的独立性。默认的写策略是WriteCommited。

**SeqNum/SeqNo**：数据库的每个写入请求都会分配到一个自增长的ID数字。这个数字会跟键值对一起追加到WAL文件，memtable，以及SST文件。序列号用于实现snapshot读，压缩过程的垃圾回收，MVCC事务和其他一些目的。

**WriteCommited提交写**：悲观事务的默认写策略，会把写入请求缓存在内存，然后在事务提交的时候才写入DB。

**WritePrepared预写**：一种悲观事务的写策略，会把写请求缓存在内存，如果是二阶段提交，就在准备阶段写入DB，否则，在提交的时候写入DB。

**WriteUnprepared**：一种悲观事务的写策略，由于这个是事务发送过来的请求，所以直接写入DB，以此避免写数据的时候需要使用过大的内存。

**memtable/写缓冲(write buffer)**：在内存中存储最新更新的数据的数据结构。通常它会按顺序组织，并且会包含一个二分查找索引。

**immutable memtable**：一个已经关闭的，正在等待被落盘的memtable。

**memtable switch切换**：在这个过程，**当前活动的memtable**（现在正在写入的那个）被关闭，然后转换成 **immtable memtable**。同时，我们会关闭当前的WAL文件，然后打开一个新的。

**Flush**：将memtable的数据写入SST文件的后台任务。

**Index**： SST文件里的数据块索引。他会被保存成SST文件里的一个索引块。默认的索引个是是二分搜索索引。

**Iterator**： 迭代器被用户用于按顺序查询一个区间内的键值。

**Partitioned Index**：被分割成许多更小的块的二分搜索索引。

**prefix bloom filter**：一种特殊的bloom filter，只能在迭代器里被使用。通过前缀提取器，如果某个SST文件或者memtable里面没有指定的前缀，那么可以避免这部分文件的读取。

**prefix extractor**：一个用于提取一个键的前缀部分的回调类。通常被用于prefix bloom filter。

**partitioned Filters**：分片过滤器是将一个full bloom filter分片进更小的块里面。

**Version**：这个是RocksDB内部概念。一个版本包含某个时间点的所有存活SST文件。一旦一个落盘或者压缩完成，由于存活SST文件发生了变化，一个新的“版本”会被创建。一个旧的“版本”还会被仍在进行的读请求或者压缩工作使用。旧的版本最终会被回收。

**WAL/LOG**：预写日志，用于恢复重启后没有变成sst 文件的数据。

**Write stall **：如果有大量落盘以及压缩工作被积压，RocksDB可能会主动减慢写速度，确保落盘和压缩可以按时完成。

**Perf context**：用于衡量本地线程情况的内存数据结构。通常被用于衡量单请求性能。

**Rate limiter**：用于限制落盘和压缩的时候写文件系统的速度。

**snapshot**：一个快照是在一个运行中的数据库上的，在一个特定时间点上，逻辑一致的，视图。

**statistics**：一个在内存的，用于存储运行中数据库的累积统计信息的数据结构。

**table properites**：每个SST文件的元数据。包括一些RocksDB生成的系统属性，以及一些通过用户定义回调函数生成的，用户定义的表属性。

## Rocksdb 架构

>reference :
>
>https://github.com/facebook/rocksdb/wiki/RocksDB-Overview

![rocksdb_架构](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/rocksdb_架构.png?raw=true)

### sst file 格式-block base format (默认格式)

> reference :
>
> https://github.com/facebook/rocksdb/wiki/Rocksdb-BlockBasedTable-Format
>
> https://github.com/facebook/rocksdb/wiki/RocksDB-Bloom-Filter

```
<beginning_of_file>
[data block 1]
[data block 2]
...
[data block N]
[meta block 1: filter block]                  (see section: "filter" Meta Block)
[meta block 2: index block]
[meta block 3: compression dictionary block]  (see section: "compression dictionary" Meta Block)
[meta block 4: range deletion block]          (see section: "range deletion" Meta Block)
[meta block 5: stats block]                   (see section: "properties" Meta Block)
...
[meta block K: future extended block]  (we may add more meta blocks in the future)
[metaindex block]
[Footer]                               (fixed size; starts at file_size - sizeof(Footer))
<end_of_file>
```



> 整体文件大小：target_file_size_base
>
> data block 大小： block_size 
>
> filter block : 一般为 bloom filter ；6.15 后引入 Ribbon filter ，占用内存更少

**注意：**

1. **filter / index block 都有 partitioned 功能；使用 partitioned 后，将会把 sst 文件级别的单一filter / index block 文件，拆分成由多个 block 组成的 partitioned index ；**好处就是使用的内存可以更少，并且配合 cache ，只需要读取 metadata_block_size 大小block 即可；而不是整个 filter / index block 文件；
2. 开启 partitioned 需要配置以下；

```
 flink 开启 partition 后配置
# filter partitioned 开启方式
partition_filters = true
# index partitioned 开启方式
index_type = IndexType::kTwoLevelIndexSearch

flink 默认配置
metadata_block_size = 4096
cache_index_and_filter_blocks = true and pin_top_level_index_and_filter = true 
pin_l0_filter_and_index_blocks_in_cache = true
```

3. **filter / index block 大概占用内存 0.5/5MB  之间，256 mb sst ,4-32KB block size 来说；**（增大 block size 能够减少内存使用）
4. **假如 index 内存占用 block cache 过大，甚至超过 cache 上限，可以关闭 `cache_index_and_filter_blocks` ；**(读取时候仍然会占用内存，但不再常驻 cache 中)
5. **filter 一般为 sst 级别的 Bloom filter ，6.15 后引入 Ribbon filter ，占用内存更少；**
6. **Bloom filter 分为：full key （默认）和 prefix ，prefix_extractor 控制 prefix 开启，prefix 能减少内存使用，缺点是降低命中率；prefix 能用于 seek ，get 加速。**
7. Bloom filter 可以开启 BlockBasedTableOptions::optimize_filters_for_memory 更充分利用内存。（原理，内存按页大小使用；900k -> 完全使用 1024 kb）
8. **data block 以及 Rocksdb 内部中，针对用户输入的 key ，底层上实际存储了internal_key=  user_key (用户输入的 key)+ sequence + type；！！！sequence 全局自增, 是为了支持快照读，因此flink 在并行度修改后恢复，必须要执行数据遍历+写入到最终 rocksdb 实例这个过程 ！！！**



### 数据写入

> reference 
>
> Rocksdb 写入逻辑  http://mysql.taobao.org/monthly/2018/07/04/
>
> Wal  开启下，打开流水线写提高性能 https://github.com/facebook/rocksdb/wiki/Pipelined-Write
>
> memtable 并发写（默认打开） https://github.com/facebook/rocksdb/wiki/MemTable#concurrent-insert
>
> rocksdb 写入分析 http://mysql.taobao.org/monthly/2018/07/04/

- 写入流程

Put -> wal -> memtable ->immutable  -> flush sst (l0) -> compaction -> stt file (l1)  ... 

- 性能相关参数

```
put :
用 writebatch 替代单个put 操作

wal ：  
1. 批量导入时候可以 disable ；
2. 开启 pipeline write；
 
memtable:
1. 默认实现为 `SkipList`，支持并发插入,allow_concurrent_memtable_write 默认打开；
2. memtable 单个大小由 `ColumnFamilyOptions::write_buffer_size` 控制；`max_write_buffer_number` 控制`memtable` 最内存；
3. 内存控制： db_write_buffer_size db 级别控制总`memtable` 大小， write_buffer_manager 可以在不指定 `write_buffer_size` 下控制总内存大小；
4. enable_write_thread_adaptive_yield 配合 allow_concurrent_memtable_write 使用提高 cpu 利用率；
5. unordered_write , 开启后会导致可见性问题，建议在批量导入时候引入，不建议开启。并且，在5.7 版本下，开启这个会导致性能抖动，时差，时好。

flush：
1. max_write_buffer_number, min_write_buffer_number_to_merge 控制flush 的 memtable 个数；（读放大一定增加，写放大减少）
2. max_background_flushes 控制 flush 线程数；

compaction:
1. max_subcompactions 当使用 level  compaction 策略方式时候， l0 -> l1 并行化可以调整这个参数。**注意，，开启后并发数不受线程池控制**；
2. max_background_jobs 后台线程数， compaction 需要使用该线程；
3. max_background_compactions ，最大后台 compaction 数，默认无限制；
4. writable_file_max_buffer_size , 控制写入 sst/wal 文件的buffer; （1m -> 4m ,ssd 能有 10% 提高）
5. max_auto_readahead_size , 提高能增加 compaction 性能；（1m -> 8m ，官方说机械硬盘有提高，ssd 无改善）
6. 配置 use_direct_io_for_flush_and_compaction ;（实测没啥影响，可能和ssd 有关）
7. target_file_size_base 单个 sst 文件大小，防止小文件产生；
8. 默认是 level compaction , 改成 UNIVERSAL compaction 能够减少写入放大，但是增加了读取放大；
... 限流相关不列举


sst:
1. 默认 sst format 为 blockbase，提高block_size 获得更好的顺序 io ，**总体index 内存会更少**；但是会导致更大的读放大；
2. 分层压缩策略，如：l0:lz4,l1:lz4....l6:zstd （起码有30%以上的提高）
```

### 数据读取

> reference :
>
> https://github.com/facebook/rocksdb/wiki/Setup-Options-and-Basic-Tuning
>
> https://github.com/facebook/rocksdb/wiki/Block-Cache
>
> https://github.com/facebook/rocksdb/wiki/Rocksdb-BlockBasedTable-Format

- 读取流程

Get -> memtable -> immutable -> sst per level

- 性能相关参数

```
1. 分层压缩策略，如：l0:lz4,l1:lz4....l6:zstd
2. 使用 block-cache ，提高查询性能；(具体参考内存控制一章)
3. 根据不同的 sst table format 采用不同的优化方案，默认使用的是`BlockBasedTable`，下面讨论的也是基于 `BlockBasedTable` 配置；
4. block_size （4k -> 64kb or more，增大会增大读放大，减少写放大，还有空间，hdd 应该增大该值）
```

## 内存相关

> reference :
>
> https://github.com/facebook/rocksdb/wiki/Memory-usage-in-RocksDB

- 核心知识点

1. `Rocksdb` 内存使用大头： `block cache` + `Indexes and bloom filters`+`Memtables`+`Blocks pinned by iterators`; 

```
注意：
1.`block cache` 开启 cache 后才有
2. 当不使用  `write buffer manager` 下，  Memtables = total column family * (active +immutable) = total column family * max_write_buffer_number*db_write_buffer_size; 
3. 使用write buffer manager 后 = write buffer manager capactity
4. Blocks pinned by iterators： 迭代对象使用的内存，包括读取，批量写入使用；
5. 特别的， compaction ，文件 write buffer ，预读等功能也会占用一些内存；compaction 相关使用占用，尚不明确如何监控。
```

2. 内存使用除了原生 `matric` 外，MemoryUtil#getApproximateMemoryUsageByType 也可以知道内存相关信息；

3. `Rocksdb`默认不开启`cache`+ `write buffer manager` 情况下；内存使用与 max_write_buffer_number 和 db_write_buffer_size 以及`cf`个数有关；开启后，所有 `cf` 内存都受到 ``write buffer manager` 管理；

4. **原生 `Rocksdb` 可以通过 `block cache(LRU/HyperClockCache)` + `write buffer manager` 控制读写内存；（不开启 `cache`，数据的读取会直接产生 io ，即只有 page cache **）

### Block Cache

>reference :
>
> Flink 1.13，state backend 优化及生产实践分享    https://flink-learning.org.cn/article/detail/3d7ccd2f4d8800f748859ef6ba1e6b55?name=author&tab=9be7942c4f817a7f78fe58ad074021d9&page=xiangguanwenzhang
>

- Rocksdb cache 

**cache管理内存示意图**

![rocksdb_cache管理内存示意图](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/rocksdb_Write%20buffer%20manager%20%E5%86%85%E5%AD%98%E7%AE%A1%E7%90%86%E7%A4%BA%E6%84%8F%E5%9B%BE.jpg?raw=true)

**Write buffer manager 内存管理示意图**

![Write buffer manager 内存管理示意图](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/rocksdb_Write%20buffer%20manager%20%E5%86%85%E5%AD%98%E7%AE%A1%E7%90%86%E7%A4%BA%E6%84%8F%E5%9B%BE.jpg?raw=true)

**write buffer manager arena block 底层（arena block -> 进一步拆分为 shard /dummy entry，物理分配最小单位，因此 arena block 不能小于 dummy entry 默认大小）**

![rocksdb_writebuffer_manager arena block 底层](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/rocksdb_writebuffer_manager%20arena%20block%20%E5%BA%95%E5%B1%82.jpg?raw=true)

**总结**

1. **cache_index_and_filter_blocks 开启后会可能会导致 cache 内存使用超量；（不可驱逐内存部分）**
2. **Cache 中针对读，每一个 block 大小由 sst data block block_size 控制；**
3. **当利用 cache +  write buffer manager 控制写入内存时候，内存分配最小单位为 block ，由 arena_block_size 控制；默认为write_buffer_memory / 8**；

```
cache 内存分配层级

- 读取视角
get api
...
cache
data block (sst block_size )
rocksdb memory allocator 
glibc/jemalloc

- 写入视角
put api
...
writebuffer manager
cache
arena block size（底层再拆分为多个 dummy entry，因此arena block size 不能小于 dummy entry 大小）
rocksdb memory allocator 
glibc/jemalloc
```



#### LRU Cache

****

#### HyperClockCache

7.16 引入，性能会更好

### write buffer manager

> 1. cache 内存分配层级已经很明确说明了和read 的分配区别，在于最小分配单位为 arena block size；
> 2. 默认flink 没有开启严格内存限制，因为会导致 api 调用失败；
> 3. 6.26 java api writebuffer manger 有 wait stall 功能，应该能减少内存超用情况；（flink 需要到8.x 系列 rocksdb 才可以）

- flush 触发机制
> reference :
> https://github.com/facebook/rocksdb/blob/22fe23edc89e9842ed72b613de172cd80d3b00da/include/rocksdb/write_buffer_manager.h#L101

**！！！注意下面说的内存控制逻辑并不是真正的总内存控制，而是 mutable inmutable 内存，需要严格控制总内存，只有 waitstall = true才可以！！！**
1. 当 mutable 达到 wbm size 的 7/8 时，**触发 flush ；**（rocksdb 文档写的是 90% ，实际代码是 7/8）
2. 总 cache 内存超用，且 memtable 内存大小超过 50% write buffer 限制，**触发 flush ；**

- arena block

1. **arena block size 过大，会导致 memtable 提前 flush ，因为每一次分配内存都是记账方式（按照 block 大小记账，哪怕没用全），分配时候会触发检测。**
2. **flink 中可以适当减少 arena block size，但是不要低于1mb，6.1 高版本后不要低于 256 kb；**

- 注意

1. **当同时使用setMinWriteBufferNumberToMerge 和 write buffer manager 时候，可能会导致write buffer manager 无法控制内存上限。(6.26 java api 允许 WriteBufferManager allowStall, 可以通过 allowStall 控制) **

>  reference : https://github.com/facebook/rocksdb/pull/9076

> 如：
>
> ```
> 超过内存限制配置1
> Cache cache = new LRUCache(MemorySize.parse("512mb").getBytes(), -1, false,0.1);
> WriteBufferManager writeBufferManager = new WriteBufferManager(MemorySize.parse("256mb").getBytes(), cache);
> setMinWriteBufferNumberToMerge(1).setMaxWriteBufferNumber(1)
> !!! buffer size 过大导致的超用
> setWriteBufferSize(MemorySize.parse("512mb")
> 
> 超过内存限制配置2
> Cache cache = new LRUCache(MemorySize.parse("512mb").getBytes(), -1, false,0.1);
> WriteBufferManager writeBufferManager = new WriteBufferManager(MemorySize.parse("256mb").getBytes(), cache);
> !!! 
> setMinWriteBufferNumberToMerge(6).setMaxWriteBufferNumber(8)
> setWriteBufferSize(MemorySize.parse("64mb")
> ```

### Rocksdb 内存控制方法

- 不使用 Writebuffer Manager

> 总内存 =    Read Memory (默认带有8m cache) +  Memtables 
>
> =  total column family * (active +immutable) 
>
> flink 中= state number * tm-slot-number * max_write_buffer_number*db_write_buffer_size; 

**注意：**

1. **默认`sst`格式`blockformat`会自动配置一个8m的 `cache`。（setNoBlockCache 可以彻底关闭，不使用 cache 管理内存）  **
2. **writebuffer manager 无法控制内存潜在风险。！！！（详细见write buffer manager 章节描述）**

- Writebuffer manager + Block cache

>  总内存 =  `block cache` + `Writebuffer Manager`

  

### Rocksdb 内存监控

> reference :
>
> 读写相关计算方式： https://github.com/apache/spark/pull/35480
>
> db 级别支持的监控数据： https://github.com/facebook/rocksdb/blob/d6f265f9d6bd4ee3c5356b6e7b7f7e2f5e2ea716/include/rocksdb/db.h
>
> https://github.com/facebook/rocksdb/blob/073ac547391870f464fae324a19a6bc6a70188dc/db/internal_stats.cc
>
> 指标含义： https://gukaifeng.cn/posts/rocksdb-db-properties-zhong-de-shu-xing-xiang-jie/index.html
>
> https://github.com/facebook/rocksdb/wiki/Compaction-Stats-and-DB-Status

```
api 调用方式
db.getProperty("rocksdb.cur-size-all-mem-tables")

写入内存监控：
总写入内存：rocksdb.cur-size-all-mem-tables （是否开启 writebuffer manager 写入内存都可以这样去监控）

读相关内存监控：
block cache配置大小：block-cache-capacity 
block cache读取内存：rocksdb.block-cache-usage （当没有显式生命关闭 cache 时候，包括迭代对象所占用的内存）
迭代内存：rocksdb.block-cache-pinned-usage （rocksdb.block-cache-usage 包含这一部分内存）
读取sst 文件使用的内存： rocksdb.estimate-table-readers-mem（不包括 block cache 使用的内存）

compaciton 监控：
rocksdb.estimate-pending-compaction-bytes （对 level-based 以外的其他压缩无效？？？不确定）


```





### Flink 中的 Rocksdb 内存管理

- 1.10 or 以后版本内存分配情况

1. 默认： manager memory
2. 使用 state.backend.rocksdb.memory.fixed-per-slot/state.backend.rocksdb.memory.fixed-per-tm ；
3. disable manager && not use per-slot/per-tm

> 情况 12 都会使用 cache 管理内存，特别的 Flink 把 writebuffer manager 和读使用的 cache 分开。
>
> 情况 3 不会管理内存
>

- flink 针对 Rocksdb 内存计算方式

```
Manager memory = total_memory_size = tm memory * fraction

# ！！！不知道这个系数怎么来的，没有任何说明，应该和 memtable 达到50% 就会触发 flush 有关
write_buffer_manager_memory = 1.5 * write_buffer_manager_capacity
write_buffer_manager_memory = total_memory_size * write_buffer_ratio
write_buffer_manager_memory + other_part = total_memory_size
write_buffer_manager_capacity + other_part = cache_capacity
 
# cache_capacity 为 cache 申请的大小
cache_capacity = (3 - write_buffer_ratio) * total_memory_size / 3  
# 按照 1g memory 计算 ，50% write_buffer_ratio
# cache_capacity = 0.83 g
# 按照 1g memory 计算 ，90% write_buffer_ratio
# cache_capacity = 0.7 g
# ！！！ 得出结论，实际 manager 控制下，大概flink 只使用了 70～85% 左右的内存，剩下作为余量

WriteBufferManagerCapacity = (2 * totalMemorySize * writeBufferRatio / 3) = totalMemorySize * 1.5 * writeBufferRatio
# 可以看出来，flink 这里计算出来的内存比例故意调大，比如说 0.4 的写内存比例，实际是 0.6 ，这样做的目的，应该也是减缓 flush 50% 就会触发的影响

# 整体内存视图
mamager memory 
rocksdb = cache + iter
        = 70%~85%*manager memroy + iter
        = write buffer manager(write ratio*1.5) + read block cache + iter
        = cache + filter/index + iter
```


#### Flink 中使用 rocksdb 管理方式

```
用户代码如下：
    transient ValueState<Tuple2<Long, Long>> sum;
    transient MapStateDescriptor<Long, Long> calcute;
    
该算子并行度是4 用了 2个 tm ,每个 tm 2 slot 
实际这个算子：4个 cf 在单个tm 中，假如使用了 timer 还会多2个；     
```
**总结：**
1. rocksdb cf 个数=slot*state 个数
2. **rocksdb 多个cf 中写入使用 write buffer manager ，使用的是同一个 `cache` 进行管理。读取也是通过 cache 管理，但是每一个 cf 都有单独的 cache。**
```
cf 读控制代码
blockBasedTableConfig.setBlockCacheSize(
                internalGetOption(RocksDBConfigurableOptions.BLOCK_CACHE_SIZE).getBytes());
```
3. **实际上，rocksdb 层面来说读也可以通过和写使用同一个cache 实例控制。**
```text
blockBasedTableConfig.setBlockCache()
```

