# Hive-4-sql

>reference:
>
>https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Select
>
>https://github.com/wangzhiwubigdata/God-Of-BigData/blob/master/%E5%A4%A7%E6%95%B0%E6%8D%AE%E6%A1%86%E6%9E%B6%E5%AD%A6%E4%B9%A0/Hive%E6%95%B0%E6%8D%AE%E6%9F%A5%E8%AF%A2%E8%AF%A6%E8%A7%A3.md （尚硅谷的数据集）


![sql](https://github.com/Whojohn/learn/blob/master/docs/hive/pic/sql.svg?raw=true)


- 注意


1. `hive`中`database`,`table`,`view`，以及`表名`，`列名`,不区分大小写。（输入大写内部统一转换为小写）
2. `database`,`table`,`view`中名字不能包含`-`符号。

- 前期准备(数据集合)

> ```
> CREATE TABLE emp(
> empno INT comment "员工表编号",     
> ename STRING comment "员工姓名",  
> job STRING comment "职位类型",    
> mgr INT,   
> hiredate TIMESTAMP comment "雇佣日期", 
> sal DECIMAL(7,2) comment "工资",  
> comm DECIMAL(7,2))
> partitioned by (deptno int);  
> 
> 
> insert into  emp values (7369,"SMITH","CLERK",7902,"1980/12/17 0:00",800,Null,20),(7499,"ALLEN","SALESMAN",7698,"1981/2/20 0:00",1600,300,30),(7521,"WARD","SALESMAN",7698,"1981/2/22 0:00",1250,500,30),(7566,"JONES","MANAGER",7839,"1981/4/2 0:00",2975,Null,20),(7654,"MARTIN","SALESMAN",7698,"1981/9/28 0:00",1250,1400,30),(7698,"BLAKE","MANAGER",7839,"1981/5/1 0:00",2850,Null,30),(7782,"CLARK","MANAGER",7839,"1981/6/9 0:00",2450,Null,10),(7788,"SCOTT","ANALYST",7566,"1987/4/19 0:00",1500,Null,20),(7839,"KING","PRESIDENT",Null,"1981/11/17 0:00",5000,Null,10),(7844,"TURNER","SALESMAN",7698,"1981/9/8 0:00",1500,0,30),(7876,"ADAMS","CLERK",7788,"1987/5/23 0:00",1100,Null,20),(7900,"JAMES","CLERK",7698,"1981/12/3 0:00",950,Null,30),(7902,"FORD","ANALYST",7566,"1981/12/3 0:00",3000,Null,20),(7934,"MILLER","CLERK",7782,"1982/1/23 0:00",1300,Null,10);
> 
> CREATE TABLE dept(
> deptno INT comment "部门编号",   
> dname STRING comment "部门名称",  
> loc STRING comment "部门所在的城市"   
> );
> 
> insert into dept values (10,"ACCOUNTING","NEW YORK"),(20,"RESEARCH","DALLAS"),(30,"SALES","CHICAGO"),(40,"OPERATIONS","BOSTON");
> ```

- select 语法

```
SELECT [ALL | DISTINCT] select_expr, select_expr, ...
  FROM table_reference
  [WHERE where_condition]
  [GROUP BY col_list]
  [ORDER BY col_list]
  [CLUSTER BY col_list
    | [DISTRIBUTE BY col_list] [SORT BY col_list]
  ]
 [LIMIT [offset,] rows]
```



## 1. 简单 select

```
select * from emp;
select * from emp where emp.sal > 200 and emp.sal <= 1000;
select distinct emp.job from emp;
select distinct emp.job from emp where deptno=20;
select * from emp limit 5;
set hive.map.aggr=true;
select deptno,sum(sal) from emp group by deptno;
select deptno,sum(sal) from emp group by deptno having deptno>20;
select deptno,sum(sal) from emp group by deptno having sum(sal)>9000;
```

## 2. 复杂语法

### 2.1 order by vs sort by vs distribute by  vs cluster by  (排序)

- order by 

  `order by`会对所有数据进行全局排序。`order by` 会引发性能问题， 排序时，为了保证全局有序，都会放入到唯一的一个`reduce`中执行，因此一般只用`order by `都需要`limit `保证性能。

- sort by 

  `sort by`会对`reduce`内的数据进行排序。注意`sort by`字段可能分配到多个`reduce`中。

- distribute by

  `distribute by`保证特定字段都放入同一个`reduce`中，一般用于配合`sort by`保证某个字段内部有序的场景。比如：每个年份内部按照收入排序。

- cluster by 

  `cluster by`等价于`distribute by`和`sort by`使用同一个字段。但是排序只能是升序排序，不能指定排序规则为ASC或者DESC。

- 如何优化top n 排序 or 全局排序

  	可以利用 `sort by ` +`distribute by`或者`cluster by`保证内部排序，然后外层套`order by limit`获取`top n`结果。

### 2.2 join

> reference:
>
> https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Joins#LanguageManualJoins-JoinOptimization

### 2.2.1 join 类型

- inner join

```
-- 查询员工编号为 7369 的员工的详细信息
select * from emp join dept on emp.deptno=dept.deptno   where empno=7369;
```

- left join

```
select * from emp left join dept on emp.deptno=dept.deptno  
```

- right join

```
select * from emp right join dept on emp.deptno=dept.deptno  
```

- full join

```
# 保留左右两边无法关联的数据
SELECT *
FROM emp  FULL OUTER JOIN  dept 
ON emp.deptno = dept.deptno;
```

- join

```
SELECT *
FROM emp  FULL OUTER JOIN  dept 
```

### 2.2.2 join 实现类型 & 优化

> reference:
> https://en.wikipedia.org/wiki/Sort-merge_join
>
> https://www.cnblogs.com/163yun/p/9121530.html

```总结：
1. hive 内部一般会分析大表和小表，默认开启不同的`join`类型进行优化。
2. `join`的字段假如一致，如`a,b,c`三表都是`a`字段连接，那么将会转化为一个`map reduce`执行join。
3. `hive join` 类型一般分为：`map join`，`common join`，`sort merge join`。
```

#### 2.2.2.1 join 原理

> 不考虑分布式和框架的情况下，一般`join`实现主要有两种：`hash join`，`sort merge join`。以下`join`说明方式以`inner join`为例。

- hash join

将小表中`join key`进行`hash`，读取大表，依次利用`hash`查找小表中的记录。

- sort merge join

把`join`的左右表按照`join key`排序，从两表最小的`join key`开始，左右交替寻找最小`key`，当遍历左右表`key`相同时，`merge`，直至完成。

- key 不唯一如何处理

1. 把`key`相同的`value`放入到`list`中，`merge`时只需遍历即可。

2. 重复`key`的`value`按列排序。迭代进行`merge`：

   > <k1,t1>	<k1, v1>
   >
   > <k1,t2>	<k1, v2>
   >
   > step 1:
   >
   > <k1, t1, v1> <k1,t1,v2>
   >
   > step 2:(注意只滑动左手，右手需要判定`key`值是否存在重复，假如重复的话，需要确定左手的值与右手的值不一致才可以滑动)
   >
   > <k1, t2, v1> <k1,t2,v2>


#### 2.2.2.2 hive 实现方式

- Common join(hash join 实现)

  两张表走`map reduce`，在`reduce`阶段进行`join`
  
![common_join](https://github.com/Whojohn/learn/blob/master/docs/hive/pic/common_join.png?raw=true)

- map join & 优化

  指定表执行`map join`操作，`map join`会把小表加载到内存中并且在`map`阶段进行`join`。

![map_join](https://github.com/Whojohn/learn/blob/master/docs/hive/pic/map_join.png?raw=true)

- smb join（sort merge join）

  `map`阶段数据进行排序，分组。两表进入到`reduce`阶段执行`sort merge join`逻辑。

![sort_merge_join](https://github.com/Whojohn/learn/blob/master/docs/hive/pic/sort_merge_join.png?raw=true)

- 语法声明

```
# 说明大表位置
SELECT /*+ STREAMTABLE(d) */  e.*,d.* 
FROM emp e JOIN dept d
ON e.deptno = d.deptno
WHERE job='CLERK';
# 声明小表位置
SELECT /*+ MAPJOIN(b) */ a.key, a.value
FROM a JOIN b ON a.key = b.key
```

> 配置参数：
>
> set hive.auto.convert.join = true; （默认开启）
>
> set hive.mapjoin.smalltable.filesize=25000000； （小表内存）
>
> set hive.auto.convert.join.noconditionaltask=true;（开启通过输入文件判定把`comment join`转化为map join）
>
> set hive.auto.convert.join.noconditionaltask.size=10000000;（输入文件大小判定）

- sort merge bucket

  假如表是分桶并且桶内有序才可用的优化。

> 配置参数
>
> set hive.auto.convert.sortmerge.join=true;
> set hive.optimize.bucketmapjoin = true;
> set hive.optimize.bucketmapjoin.sortedmerge = true;

- 倾斜优化

1. set hive.map.aggr=true；(map 阶段聚合，减少数据)
2. set hive.optimize.skewjoin=true; &hive.optimize.skewjoin=100000；（分阶段`join`，对于超过`100000`的连接值，单独进行`join`，可以通过配置更大的`map join`内存，使倾斜值走`mapjoin`）。
3. set hive.optimize.skewjoin.compiletime=true;（只能用于`skew table`）

## 3. 其他常用语法

### 3.1 union 

>       用于将同名，同类型的查询结果合并。默认的`union`会删除结果的重复行，需要保留请使用`all`关键字，不需要保留重复项目，可以用`distinct`。多个`union`可以交替使用`distinct`和`union`关键字。

```
select_statement UNION [ALL | DISTINCT] select_statement UNION [ALL | DISTINCT] select_statement ...
```

### 3.2 sub query（子查询）

```
SELECT A
FROM T1
WHERE EXISTS (SELECT B FROM T2 WHERE T1.X = T2.Y);
SELECT *
FROM A
WHERE A.a IN (SELECT foo FROM B);
```

### 3.3 取样

```
# 桶取样
SELECT * FROM source TABLESAMPLE(BUCKET 3 OUT OF 32 ON rand()) s;
# 
select * from test  TABLESAMPLE(300 rows) where gt=xxx;
```



### 3.4 group vs grouping set

> reference :
>
> https://cwiki.apache.org/confluence/display/Hive/Enhanced+Aggregation%2C+Cube%2C+Grouping+and+Rollup

     hive 引入了 grouping set 代替多个group 查询union，对应关系如下：

| grouping set 语法                                            | 等同于                                                       |
| :----------------------------------------------------------- | :----------------------------------------------------------- |
| SELECT a, b, SUM(c) FROM tab1 GROUP BY a, b GROUPING SETS ( (a,b) ) | SELECT a, b, SUM(c) FROM tab1 GROUP BY a, b                  |
| SELECT a, b, SUM( c ) FROM tab1 GROUP BY a, b GROUPING SETS ( (a,b), a) | SELECT a, b, SUM( c ) FROM tab1 GROUP BY a, b<br/><br/>UNION<br/><br/>SELECT a, null, SUM( c ) FROM tab1 GROUP BY a |
| SELECT a,b, SUM( c ) FROM tab1 GROUP BY a, b GROUPING SETS (a,b) | SELECT a, null, SUM( c ) FROM tab1 GROUP BY a<br/><br/>UNION<br/><br/>SELECT null, b, SUM( c ) FROM tab1 GROUP BY b |
| SELECT a, b, SUM( c ) FROM tab1 GROUP BY a, b GROUPING SETS ( (a, b), a, b, ( ) ) | SELECT a, b, SUM( c ) FROM tab1 GROUP BY a, b<br/><br/>UNION<br/><br/>SELECT a, null, SUM( c ) FROM tab1 GROUP BY a, null<br/><br/>UNION<br/><br/>SELECT null, b, SUM( c ) FROM tab1 GROUP BY null, b<br/><br/>UNION<br/><br/>SELECT null, null, SUM( c ) FROM tab1 |

### 3.5 窗口

> reference：
>
> https://www.studytime.xin/article/hive-knowledge-window-function.html (使用示例)
>
> https://ericfu.me/sql-window-function/#comments (窗口函数执行原理)
>
> https://cwiki.apache.org/confluence/display/hive/languagemanual+windowingandanalytics （hive 官方）
>
> 

      窗口是为了解决如：用户什么时候累积消费达到10W，用户每一次消费与消费总额的占比，销售量环比等场合。

- 语法

```
window_function (expression) OVER (
   [ PARTITION BY part_list ]
   [ ORDER BY order_list ]
   [ { ROWS | RANGE } BETWEEN frame_start AND frame_end ] )
```

- window_function(窗口函数)

1. 聚合类：sum count avg max min;
2. 排序：row_number rank dense_rank 

3. 取值： lag lead first_value last_value

- over语法

> over 内控制了三部分，一个是分区的字段，分区内的排序，还有是窗口的范围。

```
OVER (
   [ PARTITION BY part_list ]
   [ ORDER BY order_list ]
   [ { ROWS | RANGE } BETWEEN frame_start AND frame_end ] )

# 窗口范围语法
(ROWS | RANGE) BETWEEN (UNBOUNDED | [num]) PRECEDING AND ([num] PRECEDING | CURRENT ROW | (UNBOUNDED | [num]) FOLLOWING)
(ROWS | RANGE) BETWEEN CURRENT ROW AND (CURRENT ROW | (UNBOUNDED | [num]) FOLLOWING)
(ROWS | RANGE) BETWEEN [num] FOLLOWING AND (UNBOUNDED | [num]) FOLLOWING

# row 是以列进行计算
# range 是以当前值为坐标，-+ 一定范围进行运算
```

- 窗口运行方式

> 窗口运行方式如下：分区(partition)->排序(order)->开窗范围控制(row/range控制)-> 执行窗口函数； 其中开窗范围控制对应下图的`frame`。

![Figure 1. 窗口函数的基本概念](https://github.com/Whojohn/learn/blob/master/docs/hive/pic/windows-function-concepts.png?raw=true)


- 样例数据准备

```
CREATE TABLE IF NOT EXISTS q1_sales (
    emp_name string,
    emp_mgr string,
    dealer_id int,
    sales int,
    stat_date string
);

insert into table q1_sales (emp_name,emp_mgr,dealer_id,sales,stat_date) 
values  
('Beverly Lang','Mike Palomino',2,16233,'2020-01-01'),
('Kameko French','Mike Palomino',2,16233,'2020-01-03'),
('Ursa George','Rich Hernandez',3,15427,'2020-01-04'),
('Ferris Brown','Dan Brodi',1,19745,'2020-01-02'),
('Noel Meyer','Kari Phelps',1,19745,'2020-01-05'),
('Abel Kim','Rich Hernandez',1,12369,'2020-01-03'),
('Raphael Hull','Kari Phelps',1,8227,'2020-01-02'),
('Jack Salazar','Kari Phelps',1,9710,'2020-01-01'),
('May Stout','Rich Hernandez',3,9308,'2020-01-05'),
('Haviva Montoya','Mike Palomino',2,9308,'2020-01-03');
```

### 3.5.1 窗口范围控制

![Figure 1. 窗口范围控制，range vs row](https://github.com/Whojohn/learn/blob/master/docs/hive/pic/windows_row_range_difference.png?raw=true)


- 注意

1. 窗口必须使用`window function`，不然无法使用。

2. 默认没有声明窗口范围，默认窗口范围为：[1,当前行]  (RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)

3. 没有声明`oder`和`窗口范围`，默认窗口范围为：[1,整个窗口大小] (ROW BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)

4. **`range`和`row`执行机制不一样。**假如分组内存在值相同，如: 1,1,3 order by排序，假如声明为`range` 聚合结果是 `2,2,5`；`row`聚合结果是`1,2,5`；因为`range`执行会把范围内的数据计算，不考虑是否为独立的一行。`row`是按照行来考虑。

5. 所有`排序函数`不能使用窗口范围控制。

   

- 语法

```
# 窗口范围语法
(ROWS | RANGE) BETWEEN (UNBOUNDED | [num]) PRECEDING AND ([num] PRECEDING | CURRENT ROW | (UNBOUNDED | [num]) FOLLOWING)
(ROWS | RANGE) BETWEEN CURRENT ROW AND (CURRENT ROW | (UNBOUNDED | [num]) FOLLOWING)
(ROWS | RANGE) BETWEEN [num] FOLLOWING AND (UNBOUNDED | [num]) FOLLOWING
```

- 样例

```
select *,
       sum(sales) over ()                                                                                    as sample1, -- 所有sales和
       sum(sales) over (partition by dealer_id)                                                              as sample2, -- 按dealer_id分组，组内数据累加
       sum(sales) over (partition by dealer_id ORDER BY stat_date)                                           as sample3, -- 按dealer_id分组，时间排序，组内**范围**数据逐个相加，默认不指定row 方式有坑，range 和 row 的机制不同
       sum(sales)
           OVER (PARTITION BY dealer_id ORDER BY stat_date ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) as sample4, -- 按dealer_id分组，时间排序，组内由起点到当前行的聚合
       sum(sales)
           OVER (PARTITION BY dealer_id ORDER BY stat_date ROWS BETWEEN 1 PRECEDING and CURRENT ROW)         as sample5, -- 按dealer_id分组，时间排序，组内当前行和前面一行做聚合
       sum(sales)
           over (PARTITION BY dealer_id ORDER BY stat_date ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING)         as sample6, -- 按dealer_id分组，时间排序，组内当前行和前一行和后一行聚合
       sum(sales)
           over (PARTITION BY dealer_id ORDER BY stat_date ROWS BETWEEN CURRENT ROW and UNBOUNDED FOLLOWING) as sample7 -- 按dealer_id分组，时间排序，组内当前行和后面所有行
from q1_sales;
```

```
+-----------------+-----------------+------------+--------+----------+----------+----------+----------+----------+----------+----------+
|    emp_name     |     emp_mgr     | dealer_id  | sales  | sample1  | sample2  | sample3  | sample4  | sample5  | sample6  | sample7  |
+-----------------+-----------------+------------+--------+----------+----------+----------+----------+----------+----------+----------+
| Jack Salazar    | Kari Phelps     | 1          | 9710   | 8227     | 8227     | 9710     | 9710     | 9710     | 9710     | 8227     |
| Ferris Brown    | Dan Brodi       | 1          | 19745  | 8227     | 8227     | 8227     | 9710     | 9710     | 8227     | 8227     |
| Raphael Hull    | Kari Phelps     | 1          | 8227   | 8227     | 8227     | 8227     | 8227     | 8227     | 8227     | 8227     |
| Abel Kim        | Rich Hernandez  | 1          | 12369  | 8227     | 8227     | 8227     | 8227     | 8227     | 8227     | 12369    |
| Noel Meyer      | Kari Phelps     | 1          | 19745  | 8227     | 8227     | 8227     | 8227     | 12369    | 12369    | 19745    |
| Beverly Lang    | Mike Palomino   | 2          | 16233  | 8227     | 9308     | 16233    | 16233    | 16233    | 16233    | 9308     |
| Kameko French   | Mike Palomino   | 2          | 16233  | 8227     | 9308     | 9308     | 16233    | 16233    | 9308     | 9308     |
| Haviva Montoya  | Mike Palomino   | 2          | 9308   | 8227     | 9308     | 9308     | 9308     | 9308     | 9308     | 9308     |
| Ursa George     | Rich Hernandez  | 3          | 15427  | 8227     | 9308     | 15427    | 15427    | 15427    | 9308     | 9308     |
| May Stout       | Rich Hernandez  | 3          | 9308   | 8227     | 9308     | 9308     | 9308     | 9308     | 9308     | 9308     |
+-----------------+-----------------+------------+--------+----------+----------+----------+----------+----------+----------+----------+

```



### 3.5.2 window function

**1. 聚合函数**

**2. 排序函数**

| 窗口函数       | 返回类型 | 函数功能说明                                                 |
| :------------- | :------- | :----------------------------------------------------------- |
| ROW_NUMBER()   | BIGINT   | 根据具体的分组和排序，生成从分组第一个开始序号为1的自增行数  |
| RANK()         | BIGINT   | 对组中的数据进行排名，如果名次相同，则排名也相同，但是下一个名次的排名序号会出现不连续，比如同时有两个排序字段一致，那么他们的排名也一致。如并列第一，后面接第三名。 |
| DENSE_RANK()   | BIGINT   | 类似RANK 函数，但是生成的序号连续，如并列第一，后面接第二名。 |
| PERCENT_RANK() | DOUBLE   | 计算给定行的百分比排名。可以用来计算超过了百分之多少的人;排名计算公式为：(当前行的rank值-1)/(分组内的总行数-1) |
| CUME_DIST()    | DOUBLE   | 计算某个窗口或分区中某个值的累积分布。假定升序排序，则使用以下公式确定累积分布：小于等于当前值x的行数 / 窗口或partition分区内的总行数。其中，x 等于 order by 子句中指定的列的当前行中的值 |
| NTILE()        | INT      | 已排序的行划分为大小尽可能相等的指定数量的排名的组，并返回给定行所在的组的排名。如果切片不均匀，默认增加第一个切片的分布，不支持ROWS BETWEEN |

```
select *,
ROW_NUMBER() over(partition by dealer_id order by sales desc) rk01,
RANK() over(partition by dealer_id order by sales desc) rk02,
DENSE_RANK() over(partition by dealer_id order by sales desc) rk03, 
PERCENT_RANK() over(partition by dealer_id order by sales desc) rk04,
CUME_DIST() over(partition by dealer_id order by sales ) rk05,
CUME_DIST() over(partition by dealer_id order by sales desc) rk06,
NTILE(2) over(partition by dealer_id order by sales ) rk07,
NTILE(3) over(partition by dealer_id order by sales ) rk08,
NTILE(4) over(partition by dealer_id order by sales ) rk09
from q1_sales;
```

```
+--------------------+-------------------+---------------------+-----------------+---------------------+-------+-------+-------+-------+---------------------+---------------------+-------+-------+-------+
| q1_sales.emp_name  | q1_sales.emp_mgr  | q1_sales.dealer_id  | q1_sales.sales  | q1_sales.stat_date  | rk01  | rk02  | rk03  | rk04  |        rk05         |        rk06         | rk07  | rk08  | rk09  |
+--------------------+-------------------+---------------------+-----------------+---------------------+-------+-------+-------+-------+---------------------+---------------------+-------+-------+-------+
| Raphael Hull       | Kari Phelps       | 1                   | 8227            | 2020-01-02          | 5     | 5     | 4     | 1.0   | 0.2                 | 1.0                 | 1     | 1     | 1     |
| Jack Salazar       | Kari Phelps       | 1                   | 9710            | 2020-01-01          | 4     | 4     | 3     | 0.75  | 0.4                 | 0.8                 | 1     | 1     | 1     |
| Abel Kim           | Rich Hernandez    | 1                   | 12369           | 2020-01-03          | 3     | 3     | 2     | 0.5   | 0.6                 | 0.6                 | 1     | 2     | 2     |
| Ferris Brown       | Dan Brodi         | 1                   | 19745           | 2020-01-02          | 1     | 1     | 1     | 0.0   | 1.0                 | 0.4                 | 2     | 2     | 3     |
| Noel Meyer         | Kari Phelps       | 1                   | 19745           | 2020-01-05          | 2     | 1     | 1     | 0.0   | 1.0                 | 0.4                 | 2     | 3     | 4     |
| Haviva Montoya     | Mike Palomino     | 2                   | 9308            | 2020-01-03          | 3     | 3     | 2     | 1.0   | 0.3333333333333333  | 1.0                 | 1     | 1     | 1     |
| Beverly Lang       | Mike Palomino     | 2                   | 16233           | 2020-01-01          | 1     | 1     | 1     | 0.0   | 1.0                 | 0.6666666666666666  | 1     | 2     | 2     |
| Kameko French      | Mike Palomino     | 2                   | 16233           | 2020-01-03          | 2     | 1     | 1     | 0.0   | 1.0                 | 0.6666666666666666  | 2     | 3     | 3     |
| May Stout          | Rich Hernandez    | 3                   | 9308            | 2020-01-05          | 2     | 2     | 2     | 1.0   | 0.5                 | 1.0                 | 1     | 1     | 1     |
| Ursa George        | Rich Hernandez    | 3                   | 15427           | 2020-01-04          | 1     | 1     | 1     | 0.0   | 1.0                 | 0.5                 | 2     | 2     | 2     |
+--------------------+-------------------+---------------------+-----------------+---------------------+-------+-------+-------+-------+---------------------+---------------------+-------+-------+-------+

```

**3.取值**

| 窗口函数    | 函数功能说明                                                 |
| :---------- | :----------------------------------------------------------- |
| LAG()       | 与lead相反，用于统计窗口内往上第n行值。第一个参数为列名，第二个参数为往上第n行（可选，默认为1），第三个参数为默认值（当往上第n行为NULL时候，取默认值，如不指定，则为NULL. |
| LEAD()      | 用于统计窗口内往下第n行值。第一个参数为列名，第二个参数为往下第n行（可选，默认为1），第三个参数为默认值（当往下第n行为NULL时候，取默认值，如不指定，则为NULL. |
| FIRST_VALUE | 取分组内排序后，截止到当前行，第一个值                       |
| LAST_VALUE  | 取分组内排序后，截止到当前行，最后一个值                     |

```
select *,lag(sales,1) over(partition by  dealer_id),lead(sales,1) over(partition by  dealer_id) from q1_sales;
```

```
+--------------------+-------------------+---------------------+-----------------+---------------------+---------------+----------------+
| q1_sales.emp_name  | q1_sales.emp_mgr  | q1_sales.dealer_id  | q1_sales.sales  | q1_sales.stat_date  | lag_window_0  | lead_window_1  |
+--------------------+-------------------+---------------------+-----------------+---------------------+---------------+----------------+
| Ferris Brown       | Dan Brodi         | 1                   | 19745           | 2020-01-02          | NULL          | 19745          |
| Noel Meyer         | Kari Phelps       | 1                   | 19745           | 2020-01-05          | 19745         | 12369          |
| Abel Kim           | Rich Hernandez    | 1                   | 12369           | 2020-01-03          | 19745         | 8227           |
| Raphael Hull       | Kari Phelps       | 1                   | 8227            | 2020-01-02          | 12369         | 9710           |
| Jack Salazar       | Kari Phelps       | 1                   | 9710            | 2020-01-01          | 8227          | NULL           |
| Beverly Lang       | Mike Palomino     | 2                   | 16233           | 2020-01-01          | NULL          | 16233          |
| Kameko French      | Mike Palomino     | 2                   | 16233           | 2020-01-03          | 16233         | 9308           |
| Haviva Montoya     | Mike Palomino     | 2                   | 9308            | 2020-01-03          | 16233         | NULL           |
| Ursa George        | Rich Hernandez    | 3                   | 15427           | 2020-01-04          | NULL          | 9308           |
| May Stout          | Rich Hernandez    | 3                   | 9308            | 2020-01-05          | 15427         | NULL           |
+--------------------+-------------------+---------------------+-----------------+---------------------+---------------+----------------+

```

**4.练习**

> reference
>
> https://www.studytime.xin/article/hive-knowledge-window-function.html

- 

```
查询店铺上个月的营业额，结果字段如下：
| 月份 | 商铺 | 本月营业额 | 上月营业额|

create table if not exists shop_sale(
month string comment "月份",
shop string comment "商店",
money int comment "金额");

insert into table shop_sale (month,shop,money) 
values 
('2019-01','a',1),
('2019-04','a',4),
('2019-02','a',2),
('2019-03','a',3),
('2019-06','a',6),
('2019-05','a',5),
('2019-01','b',2),
('2019-02','b',4),
('2019-03','b',6),
('2019-04','b',8),
('2019-05','b',10),
('2019-06','b',12);

查询店铺上个月的营业额，结果字段如下：
| 月份 | 商铺 | 本月营业额 | 上月营业额|

select month,shop,money,lag(money,1,1) over(partition by shop order by month) from shop_sale;
```

- 

```
create table login_date (use string, login date);
insert into table login_date values("A","2018-09-04"),
("B","2018-09-04"),
("C","2018-09-04"),
("A","2018-09-05"),
("A","2018-09-05"),
("C","2018-09-05"),
("A","2018-09-06"),
("B","2018-09-06"),
("C","2018-09-06"),
("A","2018-09-04"),
("B","2018-09-04"),
("C","2018-09-04"),
("A","2018-09-05"),
("A","2018-09-05"),
("C","2018-09-05"),
("A","2018-09-06"),
("B","2018-09-06"),
("C","2018-09-06");


#展现连续登陆两天的用户
select use from (select use,datediff(login,lag(login,2) over(partition by use order by login asc)) as la from (select use,login from login_date group by use,login) as t ) as lag where la=2;
```

## 4. 行列转换

> reference:
>
> https://cwiki.apache.org/confluence/display/Hive/LanguageManual+LateralView
>
> https://cwiki.apache.org/confluence/display/Hive/LanguageManual+UDF

| 函数名                                       | 作用                                         |
| -------------------------------------------- | -------------------------------------------- |
| collect_set                                  | 返回一个不重复的集合                         |
| concat                                       | 合并多个字符                                 |
| concat_ws(string SEP, string A, string B...) | 自定义分隔符，合并多个字符                   |
| explode（udtf 函数）                         | 输入一行，输出多行，需要配合 later view 实现 |

- 样例

```
# 列转行
# emp 表中，把同部门，相同的职位的人在一列中显示
select deptno,job,collect_set(ename) from emp group by deptno,job;
+---------+------------+-------------------------------------+
| deptno  |    job     |                 _c2                 |
+---------+------------+-------------------------------------+
| 20      | ANALYST    | ["SCOTT","FORD"]                    |
| 10      | CLERK      | ["MILLER"]                          |
| 20      | CLERK      | ["SMITH","ADAMS"]                   |
| 30      | CLERK      | ["JAMES"]                           |
| 10      | MANAGER    | ["CLARK"]                           |
| 20      | MANAGER    | ["JONES"]                           |
| 30      | MANAGER    | ["BLAKE"]                           |
| 10      | PRESIDENT  | ["KING"]                            |
| 30      | SALESMAN   | ["ALLEN","WARD","MARTIN","TURNER"]  |
+---------+------------+-------------------------------------+

# 行转列
# 把上面的结果拆开成单行

select deptno,job from (select deptno,job,collect_set(ename) as ename from emp group by deptno,job)  as t
LATERAL VIEW explode(ename) myTable1 AS ename;

+---------+------------+
| deptno  |    job     |
+---------+------------+
| 10      | CLERK      |
| 10      | MANAGER    |
| 10      | PRESIDENT  |
| 20      | ANALYST    |
| 20      | ANALYST    |
| 20      | CLERK      |
| 20      | CLERK      |
| 20      | MANAGER    |
| 30      | CLERK      |
| 30      | MANAGER    |
| 30      | SALESMAN   |
| 30      | SALESMAN   |
| 30      | SALESMAN   |
| 30      | SALESMAN   |
+---------+------------+

```