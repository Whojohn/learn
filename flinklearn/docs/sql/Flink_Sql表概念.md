# Flink_Sql表概念

> reference :
>
> https://nightlies.apache.org/flink/flink-docs-release-1.12/zh/docs/dev/table/concepts/dynamic_tables/
>

**流表总结：**

1. `Flink Sql` 底层都是 `RowData`负责单条数据的表达，`RowKind` 控制单条数据的操作类型：`INSERT` , `UPDATE_BEFORE`，`UPDATE_AFTER`，`DELETE`操作。（`UPDATE_BEFORE`，`UPDATE_AFTER`适用于`Retract`模式，`Retract`模式将删除和插入行为放入到一个操作中。）
2. `Flink Sql`根据**`source`，`sink`还有`transform`**操作选择特定的执行模式。执行模式有（底层对应的`Stream`方式）：`Append`，`Upsert`，`Retract`模式。

> 如 transform 对：
>
> 1. create table ka ; create table sink_ka;  insert into sink_ka select * from ka ;  输出是 Append 流。
> 2. create table ka ; create table sink_ka;  insert into sink_ka select id,count(*) from ka group by id;  输出是`Upsert`流。

3. `Flink sql`触发计算的方式：数据的改变，时间窗口的结束（假如使用了窗口）。

4. 动态表类似于虚拟视图随时间而改变可以像静态查询一样查询动态表，持续查询表类似于物化视图。

5. 时态表属于动态表，它强调了版本的概念。可以分为：版本表，普通表两种。

6. 注意版本表时至`1.14`中 `Temporal join`只支持`EventTime`，不支持`ProcessTime`.



## 1. 动态表

- 什么是动态表

类似虚拟视图，追踪数据的物理变化，生成表的某一刻的快照。(**动态表首先是一个逻辑概念。在查询执行期间不一定(完全)物化动态表。**因此真正的物化视图对应的应该是持续查询结果表)

- 什么是连续查询

所谓的连续查询是，在流的环境下，不断的更新结果表进行输出。每一次输出的结果表与这一刻的跑批结果是一致的。如下

![连续查询](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/sql/pic/flink_sql_continues_query.png?raw=true)

- 动态表与连续查询的关系 

如下图所示，一个`Flink Sql`中处理过程，动态表和连续查询的关系是：

1. `Stream`(`Changelog Stream`)转化为动态表
2. 在动态表中执行计算连续查询，持续的生成动态表
3. 生成的动态表转化为`Stream`流输出

![FLink sql动态表与连续查询关系](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/sql/pic/flink_dynamic_table_relation_with_continus_query.png?raw=true)

### 1.1 动态表底层

- **Append-only 流：** 只有`INSERT`操作的流。
- **Retract 流：** 包含`add message`和`retract message` 两种操作。`add message`是`insert`操作；`retract message`为`delete`操作；`UPDATE`翻译为：`retract message` + `add message`实现。

![Retract 流工作原理](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/sql/pic/flink_sql_retract_stream_working_process.png?raw=true)

- **Upsert 流(必须声明唯一键):** 包含`upsert message`和`delete message`操作。`upsert message`

由`INSERT`和`UPDATE`组成，单个`Message`执行操作。`delete message`是`DELETE`操作。与`Retract 流相比`，`UPDATE`是单次操作，`Retract 流`需要执行`retract message` + `add message`，需要二次操作。

![Upsert 流工作原理](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/sql/pic/flink_sql_upsert_stream_working_process.png?raw=true)


### 1.2 为什么`Upsert`，`Retract`需要先`Delete`后`Update`

> reference:
>
> https://developer.aliyun.com/article/667700?spm=a2c6h.13262185.0.0.7a547e18wFqnb1

- 为什么需要 Delete

旧的状态需要清除后再运算，如：

假如没有`Delete`随着时间偏移，动态表会变成以下的样子，假如用下表进行计算每个城市的点击数会引发异常。(我们想要的是)

**原始数据流**

| 城市 | 点击数 |
| ---- | ------ |
| GZ   | 1      |
| SZ   | 1      |
| GZ   | 2      |
| GZ   | 3      |

**直接计算**

| 城市 | 点击数                           |
| ---- | -------------------------------- |
| GZ   | 6 (错误的，我们需要的是3而不是6) |



## 2 时态表

> reference 
>
> https://nightlies.apache.org/flink/flink-docs-release-1.14/zh/docs/dev/table/concepts/versioned_tables/

- 时态表

时态表是一张随着时间改变的表(它也是动态表)。时态表分为：版本表和普通表。

- 版本

时态表中的版本记录数据有效区间(开始结束时间)。

- 版本表

时态表中的数据可以追踪数据的历史版本。

- 普通表

时态表中数据只有最新版本。

### 2.2 版本表声明与使用

> 版本表的必须拥有时间属性，和主键声明。

#### 2.2.1 源表声明为版本表

- 语法

```
-- 定义一张版本表
CREATE TABLE product_changelog (
 product_id STRING,
  product_name STRING,
  product_price DECIMAL(10, 4),
  update_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, // 抽取数据库中数据改变的时间，保证与数据库中逻辑保证一致
  PRIMARY KEY(product_id) NOT ENFORCED,      -- (1) 定义主键约束
  WATERMARK FOR update_time AS update_time   -- (2) 通过 watermark 定义事件时间             
) WITH (
);
```

#### 2.2.2 视图声明为版本表

> 必须源表存在时间属性

- 语法

```
-- 定义一张 append-only 表
CREATE TABLE RatesHistory (
    currency STRING,
    rate DECIMAL(38, 10),
    currency_time TIMESTAMP(3),
    WATERMARK FOR currency_time AS currency_time   -- 定义事件时间
) WITH (
)

-- 利用去重生成主键

CREATE VIEW versioned_rates AS              
SELECT currency, rate, currency_time            -- (1) `currency_time` 保留了事件时间
  FROM (
      SELECT *,
      ROW_NUMBER() OVER (PARTITION BY currency  -- (2) `currency` 是去重 query 的 unique key，可以作为主键
         ORDER BY currency_time DESC) AS rowNum 
      FROM RatesHistory )
WHERE rowNum = 1; 
```

### 2.3 普通表声明

> 不同时包含主键和事件事件的表即为普通表 
