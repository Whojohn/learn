# Flink_Sql_Join

**Sql Join 总结**：

1. `Join`大概分为：`Inner join`(包括`Regular join`)，`Outer join `（`Left join`，`Right join` 和`Full join`），`Interval join` 和`Temporal join`。

> 如何确认(知道当前`join`类型)：explain join 语句

2. **`Flink Sql `中`event-time`数据类型(`Sql`数据类型)为`Rowtime`,`desc table`时候可见该信息**。

3. `Interval join`必须使用`Process-time`或者是`Event-time`；`Temporal join` 只支持`Event-time`;**其他join类型**不能带有`Event-time`（必须移除掉`Event-time`属性才能`join`），可以带有`Process-time`进行`join`。

4. **`Interval join`，`Tmeporal` 底层基于`state`和`timer`，因此有两种提高性能的方式**

   >方法1 ：(`timer`状态放入 `heap`，默认开启 `rocksdb` 后放入 `rocksdb`，性能会比 `heap` 降低很多;！！！但是使用 `heap` 作为作为状态后端，会有`exceeds the maximum akka framesize` 的风险！！！)：
   >
   >state.backend.rocksdb.timer-service.factory: HEAP
   >
   >> `interval join` 大概有 3% 到 5% 提高，但是由于`timer`在`interval join`中用于状态清除, 性能还是比较差。
   >
   >方法2：
   >
   >调整`Rocksdb`状态量策略，如`state.backend.rocksdb.block.blocksize`， 太大会影响读性能，太小会影响写性能和磁盘空间利用率。(根据数据和磁盘特性选择大小)
   >
   >！！！方法1对于内存占用很敏感，容易出现 oom，现实中一般不采用，并且有akka 异常的风险！！！
   >
   >！！！尝试对时间戳钝化的方式(秒级别时间戳变为分钟级，对于修改有一定时间间隔的场景，能保证数据准确性)，实现 Timer 批次触发(总次数不变)， 对Heap 占用不变，没有任何性能提高(因为 Timer 队列中仍需要保留每一条数据)！！！

5. **非时间窗口`Temporal join`的状态量清除需要依靠：`table.exec.state.ttl` 配置保障**

6. **批量模式不能跑 interval join 和 temporal join ，比如 datagen 不能使用`sequence` 产生数据进行`interval join`和`temporal join`**

7. `Flink 1.13.2` 下`standalone`模式下，**`interval join`仍然存在内存控制异常。**如：`cancel`掉`interval join`，作业占用的内存不释放，再次启动新的`interval join`作业，会导致申请新内存导致 `taskmanger` 内存控制失效，大于配置内存(启停多少次作业就占用多个`tm`内存)。(这个是一个bug，好像 1.14 也存在该问题)。

8. `Join`性能在开启`Rocksdb state`情况下，单字段越大，总条数越多，列越多，性能越差(性能衰减，大致是如排序中依次递减)。

9. `Temporal join`性能是所有`join`中最差的。尝试利用`upsert-kafka`替代`kafka`源+去重视图,减少构建状态表消耗，发现无论是`changlog stream`（`upsert-kafka`流）消耗跟去重视图一样，都需要额外的`state`生成版本表。

## Inner join & Outer join

- 注意

1. **Inner join **不能带有`Event time`属性(`rowtime`数据类型)。
2. 默认不带窗口的`Inner join`不会清空状态量

### Inner join 

- source

```
CREATE TABLE lef (
 id BIGINT,
 rate INT,
 f_random_str STRING,
 ts AS PROCTIME(), 
 primary key(id) not enforced
) WITH (
 'connector' = 'datagen',
 'fields.id.min'='1',
 'fields.id.max'='10',
  'rows-per-second'='10',
 'fields.f_random_str.length'='2'
);

CREATE TABLE rig (
 id BIGINT,
 rate INT,
 f_random_str STRING,
 ts AS PROCTIME(), 
 primary key(id) not enforced
) WITH (
 'connector' = 'datagen',
 'fields.id.min'='1',
 'fields.id.max'='10',
  'rows-per-second'='10',
 'fields.f_random_str.length'='2'
);
```

- inner join demo

```
select * from lef join rig on lef.id=rig.id;
select * from lef join rig on lef.id<rig.id;
```

### Outer join 

> 略

## Interval join

- 注意

1. 必须带有`Event-time`或者`Process-time`属性

### demo

- source 

```
CREATE TABLE post (
  `post` STRING,
  `id` bigint,
  `ts` bigint,
  ts_w AS TO_TIMESTAMP_LTZ(ts, 3),
  watermark for ts_w as ts_w
) WITH (
  'connector' = 'kafka',
  'topic' = 'post',
  'properties.bootstrap.servers' = 'test:9092',
  'properties.group.id' = 'post',
  'scan.startup.mode' = 'earliest-offset',
  'format' = 'json'
);

CREATE TABLE comm (
  `post` STRING,
  `id` bigint,
  `ts` bigint,
  ts_w AS TO_TIMESTAMP_LTZ(ts, 3),
  watermark for ts_w as ts_w
) WITH (
  'connector' = 'kafka',
  'topic' = 'comment',
  'properties.bootstrap.servers' = 'test:9092',
  'properties.group.id' = 'post',
  'scan.startup.mode' = 'earliest-offset',
  'format' = 'json'
);
```

- join 方式

```
create table si(
`post` STRING,
  `id` bigint,
  `ts` bigint,
  ts_w timestamp,
  co string
)WITH (
  'connector' = 'blackhole'
);

insert into si 
SELECT post.*,comm.post as co
FROM post, comm 
WHERE post.id = comm.id
AND post.ts_w BETWEEN comm.ts_w - INTERVAL '4' HOUR AND comm.ts_w;
```

## Temporal join

- 注意

1. `Temporal join ` 左右表必须为`Event-time`版本表，带有主键和`Event-time`。

2. 注意，默认`Kafka`作为源，是不能作为主键(只有`upsert-kafka`才支持主键定义)，因此需要借助`row numer partition = 1` 去重的功能实现主键，生成版本表。

3. **`Kafka upsert`  需要`key`中写入主键，普通的`kakfa`只读取 `value`中的内容。

- demo

```
CREATE TABLE post (
  `post` STRING,
  `id` bigint,
  `ts` bigint,
  ts_w AS TO_TIMESTAMP_LTZ(ts, 3),
  watermark for ts_w as ts_w
) WITH (
  'connector' = 'kafka',
  'topic' = 'post',
  'properties.bootstrap.servers' = 'test:9092',
  'properties.group.id' = 'post',
  'scan.startup.mode' = 'earliest-offset',
  'format' = 'json'
);

CREATE TABLE comm (
  `post` STRING,
  `id` bigint,
  `ts` bigint,
  ts_w AS TO_TIMESTAMP_LTZ(ts, 3),
  watermark for ts_w as ts_w
) WITH (
  'connector' = 'kafka',
  'topic' = 'comment',
  'properties.bootstrap.servers' = 'test:9092',
  'properties.group.id' = 'post',
  'scan.startup.mode' = 'earliest-offset',
  'format' = 'json'
);
```

- join 方式

```
CREATE VIEW t_comm AS              
SELECT post,id,ts,ts_w          
  FROM (
      SELECT *,
      ROW_NUMBER() OVER (PARTITION BY id  
         ORDER BY ts DESC) AS rowNum 
      FROM comm )
WHERE rowNum = 1; 

CREATE VIEW t_post AS              
SELECT post,id,ts,ts_w          
  FROM (
      SELECT *,
      ROW_NUMBER() OVER (PARTITION BY id  
         ORDER BY ts DESC) AS rowNum 
      FROM post )
WHERE rowNum = 1; 

create table si(
`post` STRING,
  `id` bigint,
  `ts` bigint,
  ts_w timestamp,
  co string
)WITH (
  'connector' = 'blackhole'
);

insert into si 
SELECT t_comm.*, t_post.post as co
FROM t_comm
LEFT JOIN t_post FOR SYSTEM_TIME AS OF t_comm.ts_w
ON t_comm.id = t_post.id;
```

## Rocksdb 优化

```
state.backend: rocksdb
state.backend.incremental: true
execution.checkpointing.interval: 120000
state.backend.rocksdb.thread.num: 2
state.backend.rocksdb.memory.partitioned-index-filters: true
state.backend.rocksdb.memory.managed: true
state.backend.rocksdb.writebuffer.number-to-merge: 2
taskmanager.memory.managed.fraction: 0.65
state.backend.rocksdb.memory.write-buffer-ratio: 0.4
# 调大影响读取，太小影响写入性能和写入放大
state.backend.rocksdb.block.blocksize: 32KB
```

- state.backend.rocksdb.block.blocksize 对读取的影响

> 注意，当内存不足时(性能会更加差)，所以测试设计 post commet 处理量都超过内存所能容纳的量  (运行20分钟后，都超出内存所能容纳数据的大小)

|                      | post.post: 8000 随机长度，comment.post: 400 随机长度， block size :64 k | post.post: 8000 随机长度，comment.post: 400 随机长度，  block size :256 k | post.post: 2000 随机长度，comment.post: 200 随机长度，  block size : 256K | post.post: 2000 随机长度，comment.post: 200 随机长度，  block size : 2048K |
| -------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 平均吞吐             | 722.2w/1155s =6252r/s                                        | 643.1w/1208s =5359r/s                                        | 992.7w/1201s = 8,265.6 r/s                                   | 471.3w/1201s= 3895r/s                                        |
| 运行 20min时的吞吐量 | 1600 r/s                                                     | 1300 r/s                                                     | 2800 r/s                                                     | 800 r/s                                                      |

