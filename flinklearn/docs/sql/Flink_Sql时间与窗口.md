# Flink_Sql时间与窗口

> reference :
>
> https://nightlies.apache.org/flink/flink-docs-release-1.14/zh/docs/dev/table/concepts/time_attributes/



**总结：**

1. **`Flink Sql`聚合后时间列会变为计算列，时间属性消失。**

2. **`Flink Sql`中`WaterMark`表达式返回类型必须为`Timestamp(3)`**

3. **`EventTime`为空不发送`WaterMark`。**

4. `WaterMark`生成的频率`Sql`中由`pipeline-auto-watermark-interval`控制。

5. 注意部分时间格式不带时区参数，默认会转化为`UTC`时区进行计算。`TIMESTAMP_LTZ `可以解决时区的问题。

6. `over`窗口必须**排序，并且以时间升序排序。**

7. `over`窗口，假如存在多个聚合操作，开窗方式必须相同。

   ```
   SELECT
       agg1(col1) OVER (definition1) AS colName,
       ...
       aggN(colN) OVER (definition1) AS colNameN
   FROM Tab1;
   
   agg1到aggN所对应的OVER definition1必须相同。
   外层SQL可以通过AS的别名查询数据。
   ```

   

   

## 1. 时间的声明 & WaterMark 控制

> 注意只有 EventTime 才有 WaterMark 生成控制策略

### 1. 1Process Time

#### 1.1.1 DDL 声明方式

```
CREATE TABLE datagen (
 id INT,
 rate INT,
 f_random_str STRING,
 user_action_time AS PROCTIME()
) WITH (
 'connector' = 'datagen',
  'rows-per-second'='1000',
 'fields.f_random_str.length'='10'
);

SELECT TUMBLE_START(user_action_time, INTERVAL '5' second), COUNT(DISTINCT f_random_str)
FROM datagen
GROUP BY TUMBLE(user_action_time, INTERVAL '5' second);
```

####  1.1.2 DataStream 转 Table 声明

```
DataStream<Tuple2<String, String>> stream = ...;

// 声明一个额外的字段作为时间属性字段
Table table = tEnv.fromDataStream(stream, $("user_name"), $("user_action_time").proctime());
```

#### 1.1.3 TableSource 定义

```
// 定义一个由处理时间属性的 table source
public class UserActionSource implements StreamTableSource<Row>, DefinedProctimeAttribute {

	@Override
	public TypeInformation<Row> getReturnType() {
		String[] names = new String[] {"user_name" , "data"};
		TypeInformation[] types = new TypeInformation[] {Types.STRING(), Types.STRING()};
		return Types.ROW(names, types);
	}

	@Override
	public DataStream<Row> getDataStream(StreamExecutionEnvironment execEnv) {
		// create stream
		DataStream<Row> stream = ...;
		return stream;
	}

	@Override
	public String getProctimeAttribute() {
		// 这个名字的列会被追加到最后，作为第三列
		return "user_action_time";
	}
}

// 注册表
tEnv.registerTableSource("user_actions", new UserActionSource());
```

#### 1.1.4 View 中声明 Process Time

```
CREATE TABLE datagen (
 id INT,
 rate INT,
 f_random_str STRING
) WITH (
 'connector' = 'datagen',
  'rows-per-second'='1000',
 'fields.f_random_str.length'='10'
);

create view temp as select *,PROCTIME() AS user_action_time from datagen;

SELECT TUMBLE_START(user_action_time, INTERVAL '5' second), COUNT(DISTINCT f_random_str)
FROM temp
GROUP BY TUMBLE(user_action_time, INTERVAL '5' second);
```

### 1.2 Event Time

#### 1.2.1 WaterMark 定义

> reference:
>
> https://nightlies.apache.org/flink/flink-docs-master/zh/docs/dev/table/sql/create/#watermark

```
- 语法 
WATERMARK FOR rowtime_column_name AS watermark_strategy_expression
// 没有延时的递增，时间戳等于当前最大时间戳也认为是迟到
WATERMARK FOR rt AS rt
// 允许时间戳等于当前最大时间戳
WATERMARK FOR rt AS rt - INTERVAL '0.001' SECOND
// 允许延时1秒
WATERMARK FOR rt AS rt - INTERVAL '1' SECOND
```

#### 1.2.2 DDL 中定义

```
CREATE TABLE datagen (
 id INT,
 rate INT,
 f_random_str STRING,
 user_action_time AS localtimestamp,
 WATERMARK FOR user_action_time AS user_action_time - INTERVAL '5' SECOND
) WITH (
 'connector' = 'datagen',
  'rows-per-second'='1000',
 'fields.f_random_str.length'='10'
);

SELECT TUMBLE_START(user_action_time, INTERVAL '5' second), COUNT(DISTINCT f_random_str)
FROM datagen
GROUP BY TUMBLE(user_action_time, INTERVAL '5' second);

```

####  1.2.3 DataStream 转 Table 声明

```
// Option 1:
DataStream<Tuple2<String, String>> stream = ...;

// 声明一个额外的字段作为时间属性字段
Table table = tEnv.fromDataStream(stream, $("user_name"), 
$("user_action_time").rowtime());

// Option 2:
// 从第一个字段获取事件时间，并且产生 watermark
DataStream<Tuple3<Long, String, String>> stream = inputStream.assignTimestampsAndWatermarks(...);

// 第一个字段已经用作事件时间抽取了，不用再用一个新字段来表示事件时间了
Table table = tEnv.fromDataStream(stream, $("user_action_time").rowtime(), $("user_name"), $("data"));

```

#### 1.2.4 TableSource 定义

```
// 定义一个由处理时间属性的 table source
public class UserActionSource implements StreamTableSource<Row>, DefinedProctimeAttribute {

	@Override
	public TypeInformation<Row> getReturnType() {
		String[] names = new String[] {"user_name" , "data"};
		TypeInformation[] types = new TypeInformation[] {Types.STRING(), Types.STRING()};
		return Types.ROW(names, types);
	}

	@Override
	public DataStream<Row> getDataStream(StreamExecutionEnvironment execEnv) {
		// create stream
		DataStream<Row> stream = ...;
		return stream;
	}

	@Override
	public String getProctimeAttribute() {
		// 这个名字的列会被追加到最后，作为第三列
		return "user_action_time";
	}
}

// 注册表
tEnv.registerTableSource("user_actions", new UserActionSource());
```



## 2. 窗口

> reference:
>
> https://nightlies.apache.org/flink/flink-docs-release-1.14/docs/dev/table/sql/queries/window-tvf/
>
> 

- **Sql TVF**

注意

1. **`TVF` 1.13 才引入，只有 >= 1.13 才能使用。`TVF` 现在不能当作普通表计算，必须带有聚合，或者`Top N`等操作。**

2. 旧版的`Sql` 窗口函数只能支持聚合操作，TVF 支持 `TopN`，窗口连接等特性。**1.14中TVF还没实现 `Session` 窗口**。

3. `TVF`

- 聚合测试表

```
CREATE TABLE Bid (
 id INT,
 price DECIMAL(10, 2),
 name STRING,
 bidtime AS localtimestamp,
 watermark for bidtime as bidtime - INTERVAL '5' second
) WITH (
 'connector' = 'datagen',
  'rows-per-second'='100',
 'fields.id.min'='1',
 'fields.id.max'='10',
 'fields.name.length'='2'
);
```



#### 2.1 Tumble 窗口

- 旧版声明方式

```
SELECT
  id,
  TUMBLE_START(bidtime, INTERVAL '10' second) AS wStart,
  SUM(price) from Bid
GROUP BY
  TUMBLE(bidtime, INTERVAL '10' second),
  id;
```

- TVF 声明方式

```
SELECT id,window_start, window_end, SUM(price)
  FROM TABLE(
    TUMBLE(TABLE Bid, DESCRIPTOR(bidtime), INTERVAL '10' second))
  GROUP BY window_start, window_end,id;
```



#### 2.2 Hop 窗口 (Sliding 窗口)

- 旧版声明方式

时间列，滑动长度，窗口大小

```
SELECT
  id,
  HOP_START(bidtime, INTERVAL '10' second, INTERVAL '20' second) AS wStart,
  SUM(price) from Bid
GROUP BY
  hop(bidtime, INTERVAL '10' second, INTERVAL '20' second),
  id;
```

- TVF 声明方式

```
SELECT window_start, window_end, SUM(price)
  FROM TABLE(
    HOP(TABLE Bid, DESCRIPTOR(bidtime), INTERVAL '10' second, INTERVAL '20' second))
  GROUP BY window_start, window_end;
```

#### 2.3 CUMULATE （累积窗口，定期触发数据输出）



- TVF 声明方式

```
SELECT window_start, window_end, SUM(price)
  FROM TABLE(
    CUMULATE(TABLE Bid, DESCRIPTOR(bidtime), INTERVAL '10' second, INTERVAL '20' second))
  GROUP BY window_start, window_end;
```

#### 2.4 windws offset 

- offset 作用：

正常窗口都是 00 ~ 05 这样的开始进行计算，有时候我们想 03~08 这样推动窗口，就需要用到 offset。



#### 2.5 Over 窗口

> reference :
>
> https://help.aliyun.com/document_detail/62514.html （部分细节描述比官网清晰）

 OVER窗口（OVER Window）是传统数据库的标准开窗，不同于Group By Window，OVER窗口中每1个元素都对应1个窗口。OVER窗口可以按照实际元素的行或实际的元素值（时间戳值）确定窗口，因此流数据元素可能分布在多个窗口中。

 在应用OVER窗口的流式数据中，每1个元素都对应1个OVER窗口。每1个元素都触发1次数据计算，每个触发计算的元素所确定的行，都是该元素所在窗口的最后1行。在实时计算的底层实现中，**OVER窗口的数据进行全局统一管理（数据只存储1份）**，逻辑上为每1个元素维护1个OVER窗口，为每1个元素进行窗口计算，完成计算后会清除过期的数据。

- 语法

```
SELECT
  agg_func(agg_col) OVER (
    [PARTITION BY col1[, col2, ...]]
    ORDER BY time_col
    range_definition),
  ...
FROM ...
```

- agg_func 类型

| 函数名                   | 作用                                                        |
| ------------------------ | ----------------------------------------------------------- |
| lag                      | 与lead相反，用于统计窗口内往上第n行值                       |
| lead                     | 用于统计窗口内往下第n行值                                   |
| FIRST_VALUE              | 取分组内排序后，截止到当前行，第一个值                      |
| LAST_VALUE               | 取分组内排序后，截止到当前行，最后一个值                    |
| ROW_NUMBER               | 根据具体的分组和排序，生成从分组第一个开始序号为1的自增行数 |
| sum ,count ,avg, max,min |                                                             |
| DENSE_RANK               | **保留字段没有实现**                                        |
| RANK                     | **保留字段没有实现**                                        |
| PERCENT_RANK             | **保留字段没有实现**                                        |
| CUME_DIST                | **保留字段没有实现**                                        |

- range_definition (分区范围定义，可选, 不声明范围为无界流，声明范围为有界流计算)

分区范围定义分为：

1. range 间隔：

   ```
   # 当前行，前30分钟内的数据包含在内，进行统计
   RANGE BETWEEN INTERVAL '30' MINUTE PRECEDING AND CURRENT ROW
   ```

2. 行间隔

   ```
   # 当前行的前多少行数据，进行统计
   ROWS BETWEEN 10 PRECEDING AND CURRENT ROW
   WINDOW
   ```

- window 方法简写

```
SELECT order_id, order_time, amount,
  SUM(amount) OVER w AS sum_amount,
  AVG(amount) OVER w AS avg_amount
FROM Orders
WINDOW w AS (
  PARTITION BY product
  ORDER BY order_time
  RANGE BETWEEN INTERVAL '1' HOUR PRECEDING AND CURRENT ROW)
```

