# Hive-2-数据类型&常用函数

> reference:
>
> https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Types
>
> https://cwiki.apache.org/confluence/display/Hive/LanguageManual+UDF

## 1 常见类型&常用函数

> reference:
>
> （精度相关问题）https://docs.informatica.com/data-engineering/data-engineering-quality/10-2-1/big-data-management-user-guide/mappings-in-the-hadoop-environment/high-precision-decimal-data-type-on-the-hive-engine/precision-loss-due-to-intermediate-calculations.html

### 1.1 数字类型

> 使用小数时候，注意精度丢失，hive中`decimal`基于`java bigdecimal`实现。

- tinyint/smallint/int(integer)/bingint/float/double
- decimal(NUMERIC)

      `hive`中`decimal`基于`java bigdecimal`实现。分为整数精度和小数精度2部分，小数部分+整数部分必须不能超于精度，否则为**`null`**，传入数据大于定义精度小数部分会四舍五入。

```
select cast(1111111.7777777777777777777777777 as Decimal(10,3));
1111111.778
select cast(11111111.7777777777777777777777777 as Decimal(10,3)); 
NULL
```

### 1.2 字符类型

- char/varchar

    varchar 最大长度为 655356，超出部分会被截断；使用 `varchar`时，`udf`只能同通过`String`转换传参，假如需要直接使用`varchar`类型只能使用`GenericUDF`。

- String
常用函数
```
# json 处理
0: jdbc:hive2://127.0.0.1:10000> select get_json_object("{\"sites\":\"asdfasf\"}","$.sites");
...
INFO  : Returning Hive schema: Schema(fieldSchemas:[FieldSchema(name:_c0, type:string, comment:null)], properties:null)
asdfasf  
...

```

不限长度

### 1.3 时间类型

>  reference:
>
> https://cwiki.apache.org/confluence/display/Hive/Different+TIMESTAMP+types

- timestamp/date

  `timestamp` 最大精度为`nanos`，默认为`micros`；`date`代表日期。**默认timestamp是标准`utc`时间不会有任何时区处理，任何函数处理默认也当作是`utc`时区进行处理。**

- timestamp 时区问题
  1. 表定义`TIMESTAMP WITH LOCAL TIME ZONE`，假如没有声明时区，会把处理时区变为机器所在时区。
- 常用函数

```
# 字符串转换为时间戳，注意转换认为输入的都是`utc`时区时间
select unix_timestamp('2021-11-09', 'yyyy-MM-dd') ;
# 将中国时区时间转换为`utc`时区表达
select to_utc_timestamp(unix_timestamp('2021-11-09', 'yyyy-MM-dd')*1000,"GMT+8" );
# 加减运算
select to_utc_timestamp(unix_timestamp('2021-11-09', 'yyyy-MM-dd')*1000,"GMT+8" ) + INTERVAL '1' DAY;

```

### 1.4 binary/boolean/void/null

- void

  void 是一个特殊类型，假如特定类型需要`null`，需要利用`cast`，如：

```
# 默认 void null
0: jdbc:hive2://127.0.0.1:10000> select null;
...
INFO  : Returning Hive schema: Schema(fieldSchemas:[FieldSchema(name:_c0, type:void, comment:null)], properties:null)
...

# int 类型 Null
0: jdbc:hive2://127.0.0.1:10000> select cast(null as int);
...
INFO  : Returning Hive schema: Schema(fieldSchemas:[FieldSchema(name:_c0, type:int, comment:null)], properties:null)
...
```

## 2. 复杂类型(嵌套类型)

#### 2.1 Struct

类似于`json`，但是类型只能少，不能有多于声明的类型，插入可以为空

- 语法

```
 create table struct_test( temp struct<outside:struct<inside:string>>);
```

- 样例

```
select named_struct("number",named_struct("inside","hellow"));
{"number":{"inside":"hellow"}} 
select named_struct("number",named_struct("inside","hellow")).number.inside;
hellow
select  named_struct("outside",named_struct("inside",cast(null as string)));
{"outside":{"inside":null}} 
```

### 2.2 Map

key/value 键值对存储，key/value只能是单一的数据类型

- 样例

```
0: jdbc:hive2://127.0.0.1:10000> select map(1,2.2,3,"3.3",4,cast(null as string));
...
INFO  : Returning Hive schema: Schema(fieldSchemas:[FieldSchema(name:_c0, type:map<int,string>, comment:null)], properties:null)
...
{1:"2.2",3:"3.3",4:null} 
```

### 2.3 Array

数组只能存放一种类型的数据，值可以为空

```
0: jdbc:hive2://127.0.0.1:10000> select array(1,null,2.2);
...
INFO  : Returning Hive schema: Schema(fieldSchemas:[FieldSchema(name:_c0, type:array<decimal(11,1)>, comment:null)], properties:null)
...
[1,null,2.2]  
```

## 3. 类型转换

- 隐式转换

> 低精度会在运算中转换为高精度，比如`short`相加插入`int`表中

- 主动转换

cast 函数