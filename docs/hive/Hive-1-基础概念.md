# Hive-1-基础概念

> reference:
>
> https://cwiki.apache.org/confluence/display/Hive/Design
> https://tech.meituan.com/2014/02/12/hive-sql-to-mapreduce.html

   `Hive `是数据仓库，将结构化的数据文件映射成表，并提供类 SQL 查询功能。核心组件是`driver`，`execution engine`，`metastore`，`Compiler(编译器)` 构成。其中`execution engine` 包括两大核心：`计算引擎`与`存储方式`。常见的存储：`hdfs`,`oss`等；常见的计算引擎：`spark`，`flink`，`mr`都支持。


![hive-架构](https://github.com/Whojohn/learn/blob/master/docs/hive/pic/hive_architecture.png?raw=true)


## 1. 组件架构

### 1.1 Driver（cli beeline）

提供`hive 命令、sql执行`与用户交互操作的组件，`beeline`通过`jdbc\thrift`协议进行连接。

### 1.2 Compiler （编译器）

解析查询组件，对不同的查询块和查询表达式进行语义分析，并最终在从元存储中查找的表和分区元数据的帮助下生成执行计划。

编译器包含以下部分：

- 解析器（Parser）——Antlr根据定义`Hive SQL`的词法，语法规则。完成`SQL`词法，语法解析，将SQL转化为`AST Tree`。

- 语义分析器（Semantic Analyser）——遍历`AST Tree`，节点转换为`QueryBlock`。**注意转换出来的并不是运算符树**。这一步将验证列名，表达式解析。在此执行类型检查和隐式类型转换。假如表是分区表，则收集该表所有表达式，以便后续修剪不需要的分区。如果查询已采样，收集该采样以供后续优化。

- 逻辑计划生成器(Logical Plan Generator) - 将内部查询表示转换为逻辑计划。逻辑计划就是生成运算符树(operator plan)，并且执行逻辑层面优化。包括以下处理：

  1. 关系运算符处理：一些运算符是关系代数运算符，如“过滤器”、“连接”等。但一些运算符是**计算引擎特有**操作。如：`reduceSink` 操作符，属于 `map-reduce计算引擎`。
  2. 操作优化：将多个`join`优化为`multi-way join,`，为 group-by 执行 `map 聚合`，执行 group-分 2 个阶段，以避免出现单个减速器可能成为分组键存在倾斜数据的瓶颈的情况。(下图为 multi-way join)

  ![multi-way join](https://pic4.zhimg.com/80/v2-2392c9bc96e057562c5fa05e7856e8c7_720w.jpg)

- 查询计划生成器(Query Plan Generator)——将逻辑计划转换为`计算引擎任务`。运算符树被递归遍历，形成`计算引擎任务`。并且执行物理层优化器进行优化，生成最终的执行计划。**注意，不同执行引擎的转换实现都是通过这层实现的如：`TezCompiler`, `SparkCompiler`。**

### 1.3 Execution Engine

	执行编译器创建的执行计划的组件。该计划是**无环**的DAG执行图。执行引擎管理计划的这些不同阶段之间的依赖关系，并在适当的系统组件上执行这些阶段。

### 1.4 Metastore 

     存储仓库中各种表和分区的所有结构信息的组件，包括列和列类型信息，读写数据所需的序列化器和反序列化器以及存储数据的路径。

## 2. Hive 数据模型

### 2.1 Tables

		类似于关系数据库中的表。可以过滤、投影(列操作)、连接和合并表。表的所有数据都存储在特定的`存储系统`，可以以内部表外部表的形式进行管理。表中的行被组织成类似于关系数据库的类型列。

### 2.2 Partitions

		每个表可以有一个或多个分区键来确定数据的存储方式，例如，具有日期分区列存储在 `特定日期格式` 的目录。分区允许系统根据查询谓词修剪要检查的数据，如：只扫描特定日期的分区。

### 2.3 Buckets

        每个分区中的数据可以根据表中列的哈希值依次划分为桶。每个bucket作为一个文件存储在分区目录中。分桶能够更有效的执行抽样。

## 3. 优势&劣势 

- 优势

`mr计算引擎`对内存要求不高，不容易发生`oom`

元数据管理，方便其他源读取

- 劣势 

速度慢

无法回环迭代运算

 