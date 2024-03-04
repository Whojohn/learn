# Flink_sql_state_总结

>  reference :
>
> https://www.ververica.com/blog/flink-sql-secrets-mastering-the-art-of-changelog-event-out-of-orderness

- flink sql operator 是否产生状态以及operator 支持 changelog 操作方式


| SQL Operation        | Runtime Operator                                             | Use State | Support consuming update streams | Produce update streams | Comment                                                      |
| -------------------- | ------------------------------------------------------------ | --------- | -------------------------------- | ---------------------- | ------------------------------------------------------------ |
| SELECT & WHERE       | Calc                                                         | N         | Y                                | N                      | Stateless operator, pass-through input event type            |
| Lookup Join          | LookupJoin                                                   | *N        | Y                                | N                      | Stateless operator([except under try_resolve mode for NDU problem](https://nightlies.apache.org/flink/flink-docs-release-1.16/docs/dev/table/concepts/determinism/#33-how-to-eliminate-the-impact-of-non-deterministic-update-in-streaming)), pass-through input event type（16引入的特性；对于lookup join 来说支持了 changeling 流，并且delete update before 返回最后一次join的数据；16之前不支持） |
| Table Function       | Correlate                                                    | N         | Y                                | N                      | Stateless operator, pass-through input event type            |
| SELECT DISTINCT      | GroupAggregate                                               | Y         | Y                                | Y                      |                                                              |
| Group Aggregation    | GroupAggregate<br />LocalGroupAggregate<br />GlobalGroupAggregate<br />IncrementalGroupAggregate | *Y        | Y                                | Y                      | except LocalGroupAggregate                                   |
| Over Aggregation     | OverAggregate                                                | Y         | N                                | N                      |                                                              |
| Window Aggregation   | GroupWindowAggregate<br />WindowAggregate<br />LocalWindowAggregate<br />GlobalWindowAggregate | *Y [1]    | *Y[2]                            | *N [3]                 | [1] except LocalWindowAggregate[2] the legacy [GroupWindowAggregate](https://nightlies.apache.org/flink/flink-docs-release-1.16/docs/dev/table/sql/queries/window-agg/#group-window-aggregation) support consuming update, but the new [Window TVF Aggregation](https://nightlies.apache.org/flink/flink-docs-release-1.16/docs/dev/table/sql/queries/window-agg/#window-tvf-aggregation) doesn’t support[3] when enable early/late firing |
| Regular Join         | Join                                                         | Y         | Y                                | Y                      |                                                              |
| Interval Join        | IntervalJoin                                                 | Y         | N                                | N                      |                                                              |
| Temporal Join        | TemporalJoin                                                 | Y         | Y                                | N                      |                                                              |
| Window Join          | WindowJoin                                                   | Y         | N                                | N                      |                                                              |
| Top-N                | Rank                                                         | Y         | Y                                | Y                      |                                                              |
| Window Top-N         | WindowRank                                                   | Y         | N                                | N                      |                                                              |
| Deduplication        | Deduplicate                                                  | Y         | N                                | *Y                     | first row with proctime will not generate updates            |
| Window Deduplication | WindowDeduplicate                                            | Y         | N                                | N                      |                                                              |

## Flink sql state 恢复问题

可以把恢复失败整体情况划分为3种：

  1. 不涉及到state operator 修改语句后/配置后 IllegalStateException
  2. 涉及到 state operator 语句， 启动后报StateMigrationException 
  3. 涉及到 state operator 启动后直接报 IllegalStateException

解决办法：

针对1:  streamgraph 使用稳定的 uid 算法，并且针对 source/sink 配置特殊的uid，完美覆盖 `etl` 场景；(`CommonExecSink`上实现对应逻辑+特定`uid`算法)

针对2:  rowdata State serializers 实现 schema evolution。 （覆盖率很低）

针对3：不能纯靠技术，需要借助数据湖和`Hybrid Source` 解决

### 情况1:不涉及到state operator 修改语句后/配置后 IllegalStateException；

- 启动后报错样式

> IllegalStateException
>
> 报错如下：
>
> Failed to rollback to checkpoint/savepoint %s. Cannot map checkpoint/savepoint state for operator %s to the new program, because the operator is not available in the new program. If you want to allow to skip this, you can set the --allowNonRestoredState option on the CLI.

##### 场景1: 修改sql (整体sql 不涉及到 state operator；逻辑计划改变导致 streamgraph 也不一致)

```
- 修改前
create table sou(a int ,b int)...;
create table sin(a int )...;
insert into sin select a from sou;

- 修改后(会少了一个cacl 算子)
create table sou(a int ,b int)...;
create table sin(a int ,b int )...;
insert into sin select * from sou;
```

##### 场景2: 不涉及到state operator 使用参数如下

> table.exec.sink.upsert-materialize = true
>
> pipeline.operator-chaining.enabled = false
>
> ...

### 情况2:涉及到 state operator 语句， 启动后报StateMigrationException 

- 场景1(streamgaraph)

```
 - 修改前
create table sou(a int ,b int,c int)...;
create table sin(a int )...;
insert into sin select sum(a) from sou group by a;

- 修改后
create table sou(a int ,b int,c int)...;
create table sin(a int )...;
insert into sin select sum(a),sum(b) from sou group by a,b;
```

### 情况3:涉及到 state operator 启动后直接报 IllegalStateException

```
 - 修改前
create table sou(a int ,b int,c int)...;
create table sin(a int )...;
insert into sin select sum(a) from sou group by a;

- 修改后
create table sou(a int ,b int,c int)...;
create table sin(a int )...;
insert into sin select sum(a),sum(b) from sou group by a,b;
```

