# Flink sql - Join存储&底层&常见问题分析

> reference :
>
> https://developer.aliyun.com/article/679659?spm=a2c6h.13262185.profile.8.3a447105crnSsJ

> 本文只讨论 stream 下的 join

- 数据丢失/推进问题

1. 只有 windows join 才会丢，其他在 outer full join 下，只要条件正确就不会丢数据；(outer 能保证晚到的数据一样被输出)
2. 除`lookup join`以为，任何`join`在`event time` 下`watermark`推进都会受上游两条`join`流影响；因此，任何一条流的`watermark`推进异常都会对数据产生影响。

- Join 导致数据乱序问题

1. 导致数据乱序问题必须条件：join 的字段不是唯一主键字段就会乱序；

```
create table a (a int primary key,b int , c int) with ('connector' = 'datagen');
以 b , c 进行join 就会引发乱序问题; 因为数据按照b,c 重新 keyed hash 混洗了（进一步加剧乱序）
```

- Stream join 类型

1. StreamExecJoin:  Regular join 
2. StreamExecIntervalJoin:  interval join
3. StreamExecLookupJoin:  lookup join（1.19 支持非append 流）
4. StreamExecWindowJoin:  windows join(1.14引入)

- Join 是否支持 changelog 或者 产生 changelog 流

> FlinkChangelogModeInferenceProgram#SatisfyModifyKindSetTraitVisitor 记录了所有 operator 的规则

- flink 里面 join 有几种类型

> FlinkJoinType 

```
INNER,
LEFT,
RIGHT,
FULL,
SEMI,
ANTI
```

- join 性能问题

1. Regular join 撤回流问题；（1.19 引入 `mini-batch join` 支持）
2. `Event time`  temporal join , `windows join` 性能一般； 特别是`window join` 在同一`join key`有大量重复数据下；（`windows join`实现类似笛卡尔积）

# StreamExecJoin-Rugular Join

- 总结

1. Regular join 支持 full join ，但是必须带有 on 条件 。
2.  **存储对主键join有优化 ,left join right 只要left 声明 primary key，即可享受优化；**
3. Regular join 会产生回撤流，产生的原因：
   1. Retract 流，有delete ，导致左右流数据变动；
   2. Left/Right join 先是 join  null  ，数据到来了，不再是null 撤回；
4. join 在 append 流中不会产生撤回流。
5. Regular join 还支持 `SEMI`，`ANTI`  join ；与`left/right join ` 的区别 ，存储只存`a`表的明细，还有`r` 表的`d`字段； 

```
-- SEMI JON
SELECT * FROM l WHERE a IN (SELECT d FROM r)

-- ANTI JOIN
SELECT * FROM l WHERE a NOT IN (SELECT d FROM r)
```

## Regular Join 底层存储解析

- 核心存储代码

```
    private transient JoinRecordStateView leftRecordStateView;
    private transient JoinRecordStateView rightRecordStateView;
    
    # JoinRecordStateView 非 outer 实现：
    #JoinRecordStateViews#JoinKeyContainsUniqueKey
    # primary key 下实现 
    ValueState<RowData> recordState;
    
    ... 中间还有个未知场景的实现
    
    #JoinRecordStateViews#InputSideHasNoUniqueKey
    #非primary key 实现
    MapState<RowData, Integer> recordState; // integer 用于记录重复记录数 
    
```

| State 根据 Sql 初始化存储实现（非outer 实现）                | State Structure                     | Update Row | Query by JK | Note                 |
| ------------------------------------------------------------ | ----------------------------------- | ---------- | ----------- | -------------------- |
| JoinKeyContainsUniqueKey (left join rigth ，只要left join 的key 为 primary key ) | <JK,ValueState<Record>>             | O(1)       | O(1)        |                      |
| InputSideHasUniqueKey （无法模拟该场景）                     |                                     |            |             |                      |
| InputSideHasNoUniqueKey                                      | <JK,MapState<Record, appear-times>> | O(2)       | O(N)        | N = size of MapState |

- 总结

> 1. JoinRecordStateView 就是 join state 的父类，有 `outer` / 非`outer`实现: `OuterJoinRecordStateViews` ,`JoinRecordStateViews`; 
> 2. 两者区别就是`outer` 会**额外记录**另一头关联的条数；作用就是防止二次检测，即只要读一次当前 value 即可，不需要遍历另一头state 的情况；如：最终结果为0 时候要发送 null (`OuterRecord`有描述该逻辑注释 );

## Regular join 底层实现逻辑

- StreamingJoinOperator

```
 -- append 流逻辑
 -- 基本逻辑
 if input record is accumulate
  |  if input side is outer
  |  |  if there is no matched rows on the other side, send +I[record+null], state.add(record, 0)
  |  |  if there are matched rows on the other side
  |  |  | if other side is outer
  |  |  | |  if the matched num in the matched rows == 0, send -D[null+other]
  |  |  | |  if the matched num in the matched rows > 0, skip
  |  |  | |  otherState.update(other, old + 1)
  |  |  | endif
  |  |  | send +I[record+other]s, state.add(record, other.size)
  |  |  endif
  |  endif
  
  
  |  if input side not outer
  |  |  state.add(record)
  |  |  if there is no matched rows on the other side, skip
  |  |  if there are matched rows on the other side
  |  |  |  if other side is outer
  |  |  |  |  if the matched num in the matched rows == 0, send -D[null+other]
  |  |  |  |  if the matched num in the matched rows > 0, skip
  |  |  |  |  otherState.update(other, old + 1)
  |  |  |  |  send +I[record+other]s
  |  |  |  else
  |  |  |  |  send +I/+U[record+other]s (using input RowKind)
  |  |  |  endif
  |  |  endif
  |  endif
  endif
```





- 完整流程

```
  StreamingJoinOperator#processElement 整体
  if input record is accumulate
  |  if input side is outer
  |  |  if there is no matched rows on the other side, send +I[record+null], state.add(record, 0)
  |  |  if there are matched rows on the other side
  |  |  | if other side is outer
  |  |  | |  if the matched num in the matched rows == 0, send -D[null+other]
  |  |  | |  if the matched num in the matched rows > 0, skip
  |  |  | |  otherState.update(other, old + 1)
  |  |  | endif
  |  |  | send +I[record+other]s, state.add(record, other.size)
  |  |  endif
  |  endif
  |  if input side not outer
  |  |  state.add(record)
  |  |  if there is no matched rows on the other side, skip
  |  |  if there are matched rows on the other side
  |  |  |  if other side is outer
  |  |  |  |  if the matched num in the matched rows == 0, send -D[null+other]
  |  |  |  |  if the matched num in the matched rows > 0, skip
  |  |  |  |  otherState.update(other, old + 1)
  |  |  |  |  send +I[record+other]s
  |  |  |  else
  |  |  |  |  send +I/+U[record+other]s (using input RowKind)
  |  |  |  endif
  |  |  endif
  |  endif
  endif
 
  if input record is retract
  |  state.retract(record)
  |  if there is no matched rows on the other side
  |  | if input side is outer, send -D[record+null]
  |  endif
  |  if there are matched rows on the other side, send -D[record+other]s if outer, send -D/-U[record+other]s if inner.
  |  |  if other side is outer
  |  |  |  if the matched num in the matched rows == 0, this should never happen!
  |  |  |  if the matched num in the matched rows == 1, send +I[null+other]
  |  |  |  if the matched num in the matched rows > 1, skip
  |  |  |  otherState.update(other, old - 1)
  |  |  endif
  |  endif
  endif
```

# StreamExecIntervalJoin-Inteval join
- 总结

1. Interval join 不支持`SEMI`, `ANTI` join;

2. **Interval join** 虽然不需要主键进行`join`，但是建议join 的 key 不能是一个多次重复 key， 否则性能会下降；（底层会在 left /rigth  流维护）
3. **`Watermark` 乱序不会引起数据的丢失，只会导致迟到的数据无法`join`到有效数据而已；**
4. **不能出现负数时间范围的表达式，否则`join`场景下数据将会丢弃，`full join` 将会左右两条数据+另一条`null`字段进行输出，`left/right` 将会只有左右流+非`outer` 流`null`字段输出。**

``` 
负数表达式
t1.row_time BETWEEN t2.row_time + INTERVAL '2' minutes AND t2.row_time - INTERVAL '1'
正数表达式
t1.row_time BETWEEN t2.row_time - INTERVAL '2' minutes AND t2.row_time + INTERVAL '1'
```

## Inteval join 存储底层

- 核心代码

```
# TimeIntervalJoin

// long 为插入记录的时间戳，假如有重复，追加进去list
//  List<Tuple2<RowData, Boolean> row 为数据，Boolean 标识结果是否已经发送，用于时间到了后 outer 判断是否发送一个空值
private transient MapState<Long, List<Tuple2<RowData, Boolean>>> leftCache; 
private transient MapState<Long, List<Tuple2<RowData, Boolean>>> rightCache;

// 申请一个额外的state 记录，是因为 coKeyProcessFunction 层面的 timer 触发，不区分左右流，需要额外字段用以区分，然后触发对于输出逻辑
private transient ValueState<Long> leftTimerState;
private transient ValueState<Long> rightTimerState;
```

## Interval join 底层实现逻辑

```
TimeIntervalJoin#processElement1
TimeIntervalJoin#processElement2
注意：
1. 两者的逻辑类似，只是操作的变量不一致而已；
2. 与正常思维不一样的是，左流来了，迭代操作右流的数据；反之亦然；
3. watermark 在 function 中是一致的，受限于最迟的左右流的 watermark 。

以左流为例：
1. 提取数据中的时间属性，计算数据时间对应右流的时间范围；(！！！上下限使用反向计算逻辑！！！)
2. rightExpirationTime（右流的下限=watermark-下限）< 右流上界执行逻辑： （1-3 的操作只是针对右流，最后是针对左流）
2.1 更新 rightExpirationTime 时间：watermark-时间区右手上限；
2.2 迭代右流 cache 对象，时间在右流的时间范围内的对象，触发join 逻辑；假如join 到数据，那么把 MapState<Long, List<Tuple2<RowData, Boolean>>> 对应数据的状态变为 true 标记为已经触发过；
2.3 假如有数据已经小于触发范围内，清除该数据；假如该数据 mapstate 中 boolean 为 false 且当前还是 outer join ，发送一条outer 数据；
2.4 假如 watermark 时间比流范围的上界小，缓存该数据到！！！左流！！！；
3. 右流的上界大于 `watermark` 情况下，缓存该数据到左流 cache 中。

右手流也是类似的，只是操作对象是反向而已
```

# Lookup join

- 总结

1. 原生的 Look join 没有存储；高版本（1.19 有根据 key hash 功能），可以修改源码，支持 hash + rocksdb 功能；`paimon` 是直接本地起一个`rocksdb` 实例实现类似原生功能；(高版本 key hash 在非 append 流下才会触发，引入是为了解决 ndu 问题)
2. lookup join 只支持 left inner join。
3. Lookup join 要求，右表必须有主键。

# Temporal join

- 总结

1. Temporal join 截止到 1.19 原生还是不支持 Process time Temporal join。（物理算子已经实现，要解除该限制，解开该判断条件即可）；
2. 只支持`left/inner join`; 

2. `Temporal join`  `process time` 和 `event time` 底层差别很大。无论是实现机制还是存储。
3. `Temporal join` 中的 `process time`里面  `ttl`实现与状态存储无关，是代码逻辑控制，因此每一次改变 ttl ，重启后对旧数据依旧有效（变为新的ttl）；并且需要额外多一个` ValueState ` 记录触发时间，避免`timer` 触发的清理和数据更新逻辑存在冲突；

## Temporal join 存储底层

- process time 存储底层

```
# TemporalProcessTimeJoinOperator
# 只有右流的状态，并且右流有数据就更新，因此性能很好；
ValueState<RowData> rightState;

# BaseTwoInputStreamOperatorWithStateRetention
# ttl 额外的消耗
ValueState<Long> latestRegisteredCleanupTimer；
```

- event time 存储底层

```
 /** Incremental index generator for {@link #leftState}'s keys. */
 # 从0 开始自增+1 
 # 只有配置了 ttl 才会删除该值，并且把该值重制从0开始算
private transient ValueState<Long> nextLeftIndex;

/** Mapping from artificial row index (generated by `nextLeftIndex`) into the left side `Row`. We can not use List to accumulate Rows, because we need efficient deletes of the oldest rows.*/
# left key 存放的是 index， value 存放的是数据
private transient MapState<Long, RowData> leftState;
# rigth key 存放的是 数据的时间，value 存放的是数据
private transient MapState<Long, RowData> rightState;


private transient ValueState<Long> registeredTimer;
```



## Temporal join 底层实现逻辑

- process time 略
- event time

```
    @Override
    public void processElement1(StreamRecord<RowData> element) throws Exception {
        RowData row = element.getValue();
        leftState.put(getNextLeftIndex(), row);
        registerSmallestTimer(getLeftTime(row)); // Timer to emit and clean up the state

        registerProcessingCleanupTimer();
    }

    @Override
    public void processElement2(StreamRecord<RowData> element) throws Exception {
        RowData row = element.getValue();

        long rowTime = getRightTime(row);
        rightState.put(rowTime, row);
        registerSmallestTimer(rowTime); // Timer to clean up the state

        registerProcessingCleanupTimer();
    }
    
    
   核心是 onEventTime#emitResultAndCleanUpState
   1. 里面涉及到迭代，排序，总体而言，性能更加差；
   2. join key 下重复的数据越多，timer 相关操作也越多；
   
    
```



# Window join

- 总结

1. Windows join 会丢数据。（数据乱序的情况下）
2. 支持所有`join` 类型；

## window join 存储底层

```
// 实际底层就是 listsate+namespace 的 state； namespace 用于 windows 区分；
private transient WindowListState<Long> leftWindowState;
private transient WindowListState<Long> rightWindowState;
```

## windows join 底层实现

```
1. 数据中会有 endindex 记录该数据属于哪个windows；
2. 对比 watermark 和窗口时间，假如超过 watermark 数据丢弃；（metric 会记录丢弃的数据）
3. 数据窗口时间没有超过 watermark ，累加；
4. 注册一个 timer ，以窗口结束时间；
5. onevent time 时候触发 join 逻辑；join 比较低效，以inner join 为例，是两个 for 循环进行;
```















