# ES&Lucene-基本概念

> reference:
>
> （Mastering Elasticsearch(中文版)） https://doc.yonyoucloud.com/doc/mastering-elasticsearch/chapter-1/121_README.html
>
> lucene文档：https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/codecs/lucene87/package-summary.html
>
> https://www.easyice.cn/archives/164

ES 的底层是`lucene`，因此很多概念都是从`lucene`过来的有必要了解`luene`基本概念。

## 1. 数据组织方式

### 1.1 ES 数据组织方式

- index（索引）：由`document`组成的集合，是一类数据的标识
- type（7后被废弃，只有`_doc`这一种类型）：一个`index`可以有多个`_type`，每个`_type`可以是不同的`mapping`结构
- document（文档）：完整的一条数据
- mapping（映射）：`index`对应唯一的`mapping`，相当于表结构
- field(单个字段)
- field  datatype（单个字段数据格式）
- field 保留字段（metadata)

> _id：文档`id`，多个`index`中`_id`可以相同，单个`index`中`_id`唯一
>
> _type（type）：7后不可选
>
> _source：原始完整文档
>
> _routing：路由方式，用于自定义路由，默认是根据`_id`进行`hash%分片数`定位
>
> _ignore: 忽略索引


## 1.1 Lucene 数据组织方式

- index(索引)
- segment（段）：一个索引可以由多个段组成
- document：`segment`里面存放的是`document`
- term:  由`token`构成的词组，术语检索粒度

## 2. ES集群架构

### 2.1 角色

- master

- data

- ingest

- coordinate

- ml

### 2.2 架构名词

- node：一个`es`实例就是一个`node`节点
- gateway：内部通信模块，用于保持心跳
- allocate: `shard`分配方式
- recovery：分片恢复

> 分片迁移，都是走`recovery`流程，`recovery`状态如下：
>
> 1. init：没有开始
> 2. index：读取`index`元数据，节点间复制索引
> 3. verify_index: 验证index
> 4. translong: 载入`index`，回放`translog`
> 5. finalize: 清理中间文件
> 6. done:完成

- shard：分片
- replica:副本