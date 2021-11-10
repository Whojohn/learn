# Hive-3-DDL & DML

>reference:
>
>https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL
>
>https://cwiki.apache.org/confluence/display/Hive/ListBucketing
>
>https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DML#LanguageManualDML-Delete

![hive-架构](https://github.com/Whojohn/learn/blob/master/docs/hive/pic/DDL&DML.svg?raw=true)


- 注意

1. `hive`中`schema`等同于`database`。
2. `hive3`中支持物化视图：`MATERIALIZED VIEW `，但是很多功能尚未完善。

## 1. DDL Database 级别操作

### 1.1 Create (新建库)

```
CREATE  (DATABASE|SCHEMA) [IF NOT EXISTS] database_name
  [COMMENT database_comment]
  // 存储路径
  [LOCATION hdfs_path]
  // DBPROPERTIES 一般没有作用，只是用于特殊标识
  [WITH DBPROPERTIES (property_name=property_value, ...)];
  
CREATE DATABASE IF NOT EXISTS hive_test
  COMMENT 'hive test for john'
  WITH DBPROPERTIES ('author'='john');
```

### 1.2 Show 查看库及 Hive 级别操作

```
SHOW (DATABASES|SCHEMAS) [LIKE 'identifier_with_wildcards'];

show databases;
| default        |
| iceberg        |
| ods            |
| t              |
| testhive2      |
show databases like 't*';
| t              |
| testhive2      |

// 查看函数
SHOW FUNCTIONS [LIKE "<pattern>"];
// 查看hive配置
SHOW CONF <configuration_name>;
// 查看锁
SHOW LOCKS <table_name>;
```

### 1.3 Describe (查看库信息)

```
DESCRIBE DATABASE [EXTENDED] db_name;
describe database ods;

+----------+----------+-----------------------------------------+-------------+-------------+-------------+
| db_name  | comment  |                location                 | owner_name  | owner_type  | parameters  |
+----------+----------+-----------------------------------------+-------------+-------------+-------------+
| ods      |          | hdfs://nss1/user/hive/warehouse/ods.db  | hadoop      | USER        |             |
+----------+----------+-----------------------------------------+-------------+-------------+-------------+

```

### 1.4 Alter(修改)

```
ALTER (DATABASE|SCHEMA) database_name SET DBPROPERTIES (property_name=property_value, ...);  
ALTER (DATABASE|SCHEMA) database_name SET OWNER [USER|ROLE] user_or_role; 
ALTER (DATABASE|SCHEMA) database_name SET LOCATION hdfs_path;
```

### 1.5 Drop(删除)

```
DROP (DATABASE|SCHEMA) [IF EXISTS] database_name [RESTRICT|CASCADE];
DROP TEMPORARY FUNCTION [IF EXISTS] function_name;

```

## 2 DDL Table/View 级别操作

### 2.1 Create 新建

>       虽然`hive 3`支持物化视图，但是很多特性还是非`ga`状态，见：https://cwiki.apache.org/confluence/display/Hive/Materialized+views；因此不讨论物化视图的操作。
>
>         创建的表结构可以通过`desc xxx`，` desc formatted xxx`，`show create table`查看信息。
>
>         可以通过`CTAS`方式建表，如`create table xxx as selec xxx`;
>
>         可以通过表复制形式创建表，如`CREATE TEMPORARY EXTERNAL TABLE  IF NOT EXISTS  emp_co  LIKE emp`；

- 语法

```
CREATE [TEMPORARY] [EXTERNAL] TABLE [IF NOT EXISTS] [db_name.]table_name   
  [(col_name data_type [column_constraint_specification] [COMMENT col_comment])]
  [COMMENT table_comment] // 注释
  [PARTITIONED BY (col_name data_type [COMMENT col_comment], ...)] // 表分区方式
 
  [  
  [CLUSTERED BY (col_name, col_name, ...)  // 表分桶
  [SORTED BY (col_name [ASC|DESC], ...)] // 可选，分桶内数据是否有序
  INTO num_buckets BUCKETS // 必填，桶个数
  ]
  
  [SKEWED BY (col_name, col_name, ...) ] // 倾斜表
     ON ((col_value, col_value, ...), (col_value, col_value, ...), ...) 
  [STORED AS DIRECTORIES]  // 倾斜表 list bucket
   [ROW FORMAT row_format]  // 行序列化方式,包括序列类的方式，编码(gbk or utf-8)和数据切分方式(文件，csv等文件类型特有)
   [STORED AS file_format] // 存储格式，一般行序列化方式和存储方式是绑定的
| STORED BY 'storage.handler.class.name' [WITH SERDEPROPERTIES (...)]  ]
  [LOCATION hdfs_path]
  [TBLPROPERTIES (property_name=property_value, ...)] // 表属性，不属于hive管理的属性，如parquet属性
  [AS select_statement]; 
  
  
CREATE VIEW [IF NOT EXISTS] [db_name.]view_name [(column_name [COMMENT column_comment], ...) ]
  [COMMENT view_comment]
  [TBLPROPERTIES (property_name = property_value, ...)]
  AS SELECT ...;
```

- TEMPORARY vs EXTERNAL  vs MANAGED

      默认没有修饰符为内部表（`MANAGED_TABLE`)。`TEMPORARY `为临时表，只有在当前访问会话可见，结束会话后丢失，不支持分区等特性。`EXTERNAL  `外部表，表的管理不是由`hive`可见，只要目录存在数据，数据即为可见(新写入内容无需提交到`metastore`)，删除表时，`metastore`记录删除，物理存储仍在。

- PARTITIONED vs CLUSTERED vs CLUSTERED SORTED BY vs SKEWED 

     1.PARTITIONED  分区表

  > 总结：
  >
  > 1. 分区表分区字段不能同时是数据列。
  >
  >    > create table test_partition (create_tim date) partitioned by (create_tim date);
  >    >
  >    > Column repeated in partitioning columns (state=42000,code=10035)
  >
  > 2. 分区字段可以是`date`，`string`，`int`类型。
  >
  > 3. 分区可以是多个字段，如： partitioned by (create_time date, nginx_ip string)
  >
  > 4. 静态分区写入需要指定分区名，动态分区无需指定，动态分区需要配置`set hive.exec.dynamic.partition=true;`。

  ```
  create table table_name (
    id                int,
    name              string
  )
  partitioned by (date string, country string)
  ```

  2. clustered && clustered sorted by (分桶表)

  > 总结：
  >
  > 1. 分桶和分区表可以一起使用。
  > 2. 分桶表可以控制字段有序(sorted by)。
  > 3. 分桶表可以`hash%`划分或通过
  > 4. 分桶表不能直接`load`从文件导入，比如通过`insert overwrite select xxx`，且开启`hive.enforce.bucketing = true`配置。

  ```
  create table table_name (
    id                int,
    name              string
  )
  partitioned by (date string, country string)
  clustered by(name) into 3 buckets
  ```

  3. skew 

  > 总结：
  >
  > 1. 提醒`hive`表中哪些值倾斜，用于执行优化，指定值都会放入独立的文件中，并且`hive`知道哪些文件是倾斜。
  > 2. STORED AS DIRECTORIES 参数会把倾斜值独立存储，非倾斜值放入到一个目录中。
  > 3. 倾斜值可以通过`alter`修改，修改后对于之前的表不会有影响(保持旧值)，新表按照新参数执行。

```
CREATE TABLE list_bucket_multiple (col1 STRING, col2 int, col3 STRING)
  SKEWED BY (col1, col2) ON (('s1',1), ('s3',3), ('s13',13), ('s78',78)) [STORED AS DIRECTORIES];
```

### 2.2 Show 查询信息

- 语法

```
SHOW TABLES/VIEWS [IN database_name] ['identifier_with_wildcards'];
SHOW PARTITIONS [db_name.]table_name [PARTITION(partition_spec)];  
SHOW COLUMNS (FROM|IN) table_name [(FROM|IN) db_name];
```

### 2.3 Truncate 

用于清空表数据，如某个分区数据。

- 语法

```
TRUNCATE [TABLE] table_name [PARTITION partition_spec];
 
partition_spec:
  : (partition_column = partition_col_value, partition_column = partition_col_value, ...)
```

### 2.4 Drop

- 语法

```
DROP TABLE [IF EXISTS] table_name [PURGE];   // PURGE 假如配置了 trash 不会马上删除，需要purge 才能马上删除
```

### 2.5 Describe

- 语法

```
DESCRIBE [EXTENDED|FORMATTED] 
  [table_name/view][.col_name ( [.field_name] | [.'$elem$'] | [.'$key$'] | [.'$value$'] )* ];
DESCRIBE [EXTENDED|FORMATTED] table_name[.column_name] PARTITION partition_spec;
```

### 2.6 msck repair

表修复，一般用于元信息异常，或者外部写入没有添加`meta`信息

- 语法

```
MSCK [REPAIR] TABLE table_name [ADD/DROP/SYNC PARTITIONS];

```

### 2.7 alter

修改信息

- 语法

```
ALTER TABLE table_name RENAME TO new_table_name;
ALTER TABLE table_name ADD [IF NOT EXISTS] PARTITION partition_spec [LOCATION 'location'][, PARTITION partition_spec [LOCATION 'location'], ...];
 
partition_spec:
  : (partition_column = partition_col_value, partition_column = partition_col_value, ...)
ALTER TABLE table_name SET TBLPROPERTIES table_properties;
table_properties:
  : (property_name = property_value, property_name = property_value, ... )
ALTER TABLE table_name SET TBLPROPERTIES ('comment' = new_comment);
ALTER TABLE table_name SKEWED BY (col_name1, col_name2, ...)
  ON ([(col_name1_value, col_name2_value, ...) [, (col_name1_value, col_name2_value), ...]
  [STORED AS DIRECTORIES];  
  
```

## 3. DMl 操作

### 3.1 从文件导入数据到表（load操作）

- 总结

> 1. `hive 3` 之前的`load`操作只是单纯的把文件放入到目录中。
> 2. `hive 3`可以通过`insert as select`执行过滤，格式转换，分区等特定操作。

- 语法 

```
LOAD DATA [LOCAL] INPATH 'filepath' [OVERWRITE] INTO TABLE tablename [PARTITION (partcol1=val1, partcol2=val2 ...)] [INPUTFORMAT 'inputformat' SERDE 'serde'] 
```

### 3.2 从查询插入数据

- 语法

```
INSERT OVERWRITE TABLE tablename1 [PARTITION (partcol1=val1, partcol2=val2 ...) [IF NOT EXISTS]]   
INSERT INTO TABLE tablename PARTITION (partcol1[=val1], partcol2[=val2] ...) 
```

### 3.3 查询写入到文件中

- 语法

```
INSERT OVERWRITE [LOCAL] DIRECTORY directory1
  [ROW FORMAT row_format] [STORED AS file_format] 
  SELECT ... FROM ...
```

### 3.4 ACID 表操作(Update Deleta Merge)

> 了解有3个操作即可

## 4. 并发

> reference :
>
> https://cwiki.apache.org/confluence/display/Hive/Locking

	`hive`对于并发写需要`Shared lock`对于`修改，插入，删除`是`Exclusive lock`。`Shard lock`可以重复获取(可以并发读)，`Exclusive lock`具有互斥性，只能有一个对象持有`Exclusive lock`。

| **Hive Command**                                            | **Locks Acquired**                           |
| ----------------------------------------------------------- | -------------------------------------------- |
| **select .. T1 partition P1**                               | **S on T1, T1.P1**                           |
| **insert into T2(partition P2) select .. T1 partition P1**  | **S on T2, T1, T1.P1 and X on T2.P2**        |
| **insert into T2(partition P.Q) select .. T1 partition P1** | **S on T2, T2.P, T1, T1.P1 and X on T2.P.Q** |
| **alter table T1 rename T2**                                | **X on T1**                                  |
| **alter table T1 add cols**                                 | **X on T1**                                  |
| **alter table T1 replace cols**                             | **X on T1**                                  |
| **alter table T1 change cols**                              | **X on T1**                                  |
| **alter table T1 \**concatenate\****                        | **X on T1**                                  |
| **alter table T1 add partition P1**                         | **S on T1, X on T1.P1**                      |
| **alter table T1 drop partition P1**                        | **S on T1, X on T1.P1**                      |
| **alter table T1 touch partition P1**                       | **S on T1, X on T1.P1**                      |
| **alter table T1 set serdeproperties**                      | **S on T1**                                  |
| **alter table T1 set serializer**                           | **S on T1**                                  |
| **alter table T1 set file format**                          | **S on T1**                                  |
| **alter table T1 set tblproperties**                        | **X on T1**                                  |
| **alter table T1 partition P1 concatenate**                 | **X on T1.P1**                               |
| **drop table T1**                                           | **X on T1**                                  |

