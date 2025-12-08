# Calcite 功能

## 隐式转化(开关)
> reference(flip-154) : https://cwiki.apache.org/confluence/display/FLINK/FLIP-154%3A+SQL+Implicit+Type+Coercion

- 使用场景
>hive/spark 中含有大量的隐式转化，udf/join/转化中能够支持 int 类型 join string 类型，而flink  中需要大量的cast。

- 使用方式
```text
- before :
Flink SQL> select 1='1';
[ERROR] Could not execute SQL statement. Reason:
org.apache.flink.table.api.ValidationException: implicit type conversion between INTEGER and CHAR is not supported now

- after:
Flink SQL> select 1='1';
true
```

- 坑
>select 1 union all select 1.1 -------> 编译器会把第二条数据变为 1,因此需要第一条数据按照最大精度(cast,*1.0等)进行说明

- 源码修改处
```text
org.apache.flink.table.planner.calcite.FlinkPlannerImpl#
SqlValidator.Config.DEFAULT
        .withIdentifierExpansion(true)
        ...
        .withTypeCoercionEnabled(false) -------------> .withTypeCoercionEnabled(true) 
```



## 语法控制
> reference: https://calcite.apache.org/javadocAggregate/org/apache/calcite/sql/validate/SqlConformance.html

- 使用场景
> mysql hive != 是支持，但是在标准ansi-sql 里面必须使用 <>


- 使用方式
```text
Flink SQL> select 1!=1;
[ERROR] Could not execute SQL statement. Reason:
org.apache.calcite.runtime.CalciteException: Bang equal '!=' is not allowed under the current SQL conformance level

Flink SQL> select 1!=1;
false
```

- 源码修改处
```text
org.apache.flink.sql.parser.validate.FlinkSqlConformance#
public boolean isBangEqualAllowed() {
        return false;   --------> true
    }
```