# bimap
## sql udaf 支持 bitmap
> reference (官方有讨论，没有实现): https://issues.apache.org/jira/browse/FLINK-24959  

- 使用场景
```
------------- 需求1  -------------
需求: 从网关日志统计每个页面用户/设备号（统称为id）的uv
周期: 当天
原表实时队列： id (int,long,string) , page_id
数据量: 网关日增量超百亿


------------- 需求2  -------------
需求: 统计在最新状态下，统计每个状态的订单数量
周期: 历史全部
原表信息: order_tab(百亿条)
原表实时队列: order_tab(最近3天的binlog增量，日增数亿，数据乱序，必须保证乱序下的正确性)
字段: order_id, status, create_time, update_time
结果呈现:
    status | order_count
    ----------------------
    'pending' | 1000
    'completed' | 5000
    'canceled' | 2000

注意：
1. 订单状态是会更改，但是不会发生 status 1,2,3  ,2-> 1 , 只会出现 1 -> 2 > 3 的改变；
2. 数据会乱序，需要实现乱序；
3. 订单为long 字段；

```

方案：
1. bitmap/HyperLogLog 针对单一uv 统计（只有插入，没有删除）；
2. bitmap+递归枚举值去重 针对状态类统计；
- 坑
1. state 过大：大规模使用该udf 必须group by 条件后再 hash 取mod 一个数值进行数据打散（否则会导致单个state value 过大）

|                           | 去重数据（亿条） | 状态大小（mb） | 每1亿条数据的大小 |   
|---------------------------|----------|----------|-----------|
| roaringbitmap             | 2.04     | 439      | 215       |
| roaringbitmap64           | 1.00     | 431      | 431       |
| flink-HyperLogLogPlusPlus | 1.10     | 200      | 181       |

2. 性能问题：大规模使用该udf，必须考虑统计聚合高低基数的问题。低基数可以通过打散+mini_batch+heap state获得最大性能。高基数，可能使用外部状态，或者手写代码会更好（序列化开销的原因）。
> 参考数据：heap state 下：minibath  5w+/per/s 性能; 非minibath 2~7k/per/s; 
3. bitmap 默认是32bit ，一般都不支持**负数**；假如数据量大于这个范围需要使用 
4. bitmap 输入只支持int,long；
> 针对能够允许一定误差场景string其他类型可以通过 hash 取mod 获取是直接取hash 后转化为long 进行计算；
> 不允许误差的场景，可以通过发号器，外部redis/hbase 取发号器的id 作为唯一映射；


- 使用方式
```sql
--- 统计每个页面下的pv
CREATE TABLE sou (id INT, url_page int) WITH (
  'connector' = 'datagen',
  'rows-per-second' = '500000',
  'fields.category.min' = '1',
  'fields.category.max' = '1',
  'number-of-rows' = '1000000000'
);

CREATE TABLE sin (url_page INT, distinct_cnt BIGINT) WITH ('connector' = 'print');

insert into sin
SELECT
    url_page,
    bitmap_distinct(id) as distinct_cnt
FROM
    sou
GROUP BY
    category


--- 乱序统计在最新状态下，统计每个状态的订单数量
CREATE TABLE orders (
                        order_id STRING,
                        order_status INT,
                        proc_time AS PROCTIME()
) WITH (
      'connector' = 'datagen',
      'rows-per-second' = '10',
      'fields.order_id.kind' = 'random',
      'fields.order_id.length' = '5',
      'fields.order_status.kind' = 'random',
      'fields.order_status.min' = '1',
      'fields.order_status.max' = '5'
      );

--- all_order_status = [1,2,3,4,5] 表示状态顺序：1->2->3->4->5
--- 10分钟统计一次
SELECT
    TUMBLE_START(proc_time, INTERVAL '10' SECOND) AS window_start,
    count_status_by_sequence(order_id, order_status, ARRAY[1,2,3,4,5]) AS status_counts
FROM orders
GROUP BY TUMBLE(proc_time, INTERVAL '10' minute )

```

- HyperLogLogPlusPlus源码修改处
```text
org.apache.flink.table.planner.plan.utils.AggFunctionFactory#createApproxCountDistinctAggFunction
### 删除下面这一行即可
    if (!isBounded) {
      throw new TableException(
        s"APPROX_COUNT_DISTINCT aggregate function does not support yet for streaming.")
    }
```
- [去重bitmap代码](../../src/test/java/udf/BitmapStatusCountUDAFTest.java)
- [乱序取最新情况下去重bitmap代码](../../src/test/java/udf/BitmapStatusCountUDAFTest.java)
