# ES-数据类型

> reference:
>
> https://www.elastic.co/guide/en/elasticsearch/reference/7.15/mapping-types.html

![数据类型](https://github.com/Whojohn/learn/blob/master/docs/es/pic/数据类型(Field+data+type).svg?raw=true)

**注意**

1.  es 中数组是特殊类型，任何字段，都可以是一个独立对象或者是同类型对象组成的数组。
2.  一个字段以多种数据类型存储，如：同时是`keyword`，`text`，`long`等多种类型。
3.  字段以别名进行搜索，需要`mapping`中预定义`alias`。

## 1. Number

- 注意

1. number 类型适用于范围查询，而不是精准查询，精准查找场合使用`keyword`性能更好。

2. 固定精度的存储，应该用`scaled_float`存储，底层是`long`存储，性能更好，注意`scaled_float`对于超出精度部分会截断。

```
PUT test_index
{
  "mappings": {
    "properties": {
      "price": {
        "type": "scaled_float",
        "scaling_factor": 100
      }
    }
  }
}

POST test_index/_doc
{
  "price":1.22
}
POST test_index/_doc
{
  "price":1.223
}

GET test_index/_search
{
  "size": 0,
  "aggs": {
    "NAME": {
      "sum": {
        "field": "price"
      }
    }
  }
}

# scaled_float 内部截断了1.223 为 1.22存储
  "aggregations" : {
    "NAME" : {
      "value" : 2.44
    }
  }

```

## 2. Keyword

1. 假如不需要聚合建议关闭`doc_value`，`doc_value`在高基数下存储和性能消耗很高。
2. 7.15 引入 `wildcard`类型用于改进高基数下，长文本下的，`wildcard`，`regexp`查询。(该`wildcard`类型与`wildcard`查询没有关系，只是名字一样。)

### 3. 日期

1. `date`日期在es内部都转化为时间戳进行存储。日期支持多种格式，但是多种格式的支持性能会变差。
2. 日期的转化是利用`java`来实现，`java`日期的转化可能会带来脏数据，如：`021-01-01` 变成一个1999年的时间，该字段正常时间是`2021`，转化过程没有报错，插入成功。

3. `date`的时间精度是`ms`，`data_naos`时间精度为`ns`，带有小数点的日期格式如：{"date": 1618249875.123456}，`data_naos`可能会精度丢失。

### 4. text

1. 日志场合可以关闭`narms`（评分不考虑文本长度），`index_options`调整为`docs`(只记录文档是否存在词，不记录词频，位置等信息)。

2. `fielddata`和`term_vector`,`fieldata`消耗内存较多，必须配置`fielddata_frequency_filter`过滤频次过低的文本，`term_vector`消耗物理空间比较多，`term_vector`只能查询单个文档的词频。解决了：`词频率`，`词云`，`text字段查询词频率`等相关问题如：

   >- Q1：Elasticsearch可以根据检索词在doc中的词频进行检索排序嘛？
   >- Q2：求教 ES 可以查询某个索引中某个text类型字段的词频数量最大值和词所在文档数最大值么？例：索引中有两个文档 doc1：{"text":"*"}  分词结果有两个北京，一个南京 doc2：{"text":"*"} 分词结果有一个北京想要一下结果：北京：词频3，文档量2 南京：词频1，文档量1
   >- Q3：对某些文章的词频统计除了用fielddata之外还有没有效率比较高的解决办法呢？目前统计有时候会遇到十万级的文章数直接在通过聚合效率上不是特别理想。
   >
   >```
   ># 解决方案1：fielddata, term_vector
   ># 解决方案2：外部打标签，插入到keyword字段
   >```

