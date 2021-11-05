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

## 2. String

> `string`在`es`中有两类表达方式，一种是`keyword`，一种是`text`，区别是完整匹配还是分词搜索。

### 2.1 Keyword

- 核心配置

1. doc_value 用于聚合,建议聚合用于低基数，或者高基数先过滤后聚合(默认开启)
2. ignore_above 用于控制keyword 长度，防止过长(过长的唯一匹配没有意义，信息密度原理，一般长度越长，前缀一致的越少）
3. norms 默认关闭，评分不需要考虑长度，可以关闭

- 作用

1. 聚合
2. 精准命中

- 优化项

1. 假如不需要聚合建议关闭`doc_value`，`doc_value`在高基数下存储和性能消耗很高。
2. 7.15 引入 `wildcard`类型用于改进高基数下，长文本下的，`wildcard`，`regexp`查询。(该`wildcard`类型与`wildcard`查询没有关系，只是名字一样。)

#### 2.1.1 keyword

#### 2.1.2 constant_keyword（所有keyword类型一致）

#### 2.1.3 wildcard

> reference:
>
> https://www.elastic.co/cn/blog/find-strings-within-strings-faster-with-the-new-elasticsearch-wildcard-field

- 使用场景

​        用于代码，安全审计领域，等非正常组织文本。如高频出现高基数特殊优化的字段如：NoSuchFile  `*such` 查询 。

- 底层原理

​        底层利用所有字符串的每隔3个字符的`ngram`缩小正则匹配的代价，以`ngram`进行粗选。然后利用一个`bytes`代表的原始日志，进行精确定位。

### 2.2 Text

- 核心配置

1. analyzer(分词器)
2. fielddata (开启后能过统计热词，但是需要占用大量内存)
3. fieldata_frequency_filter 过滤掉特定词组 ，防止fielddata 内存占用过多
4. index 是否需要索引，默认 true
5. index_prefixes 提高 wildcard regex 前缀匹配效率
6. norms 是否记录长度，用于影响打分，默认开启，日志等只命中不需要打分场合可以关闭
7. index_option  默认是 positions 等级，假如日志，或者不需要高亮，可以是 docs(只命中性能最好) ；freqs 等级，减少存储提高性能。
8. term_vector  用于统计单个文档，和单个分片的 tfidf 信息

- 作用

1. 分词搜索，而不是准确匹配的场景

- 注意事项

​      `fieldata`消耗内存较多，必须配置`fielddata_frequency_filter`过滤频次过低的文本，`term_vector`消耗物理空间比较多，`term_vector`只能查询单个文档的词频，并且返回的结果只能是单个`shard`的结果。解决了：`词频率`，`词云`，`text字段查询词频率`等相关问题如：

   >- Q1：Elasticsearch可以根据检索词在doc中的词频进行检索排序嘛？
   >- Q2：求教 ES 可以查询某个索引中某个text类型字段的词频数量最大值和词所在文档数最大值么？例：索引中有两个文档 doc1：{"text":"*"}  分词结果有两个北京，一个南京 doc2：{"text":"*"} 分词结果有一个北京想要一下结果：北京：词频3，文档量2 南京：词频1，文档量1
   >- Q3：对某些文章的词频统计除了用fielddata之外还有没有效率比较高的解决办法呢？目前统计有时候会遇到十万级的文章数直接在通过聚合效率上不是特别理想。
   >
   >```
   ># 解决方案1：fielddata, term_vector
   ># 解决方案2：外部打标签，插入到keyword字段
   >```

#### 2.2.1 text

#### 2.2.2 match_only_text 

- 使用场景

不需要自定义分词器，不需要高亮，打分搜索，只需要搜索命中的场合

- 底层原理

match_only_text = norms=false and index_option = docs;

- 特殊限制

1. index__option(词距离查询) 会导致 span query 不可用，需要用interval query 替代。
2. 只能使用`default`分词器。

### 3. Date

- 核心配置

1. format 时间格式，`java`时间格式

- 注意事项

1. `date`日期在es内部都转化为时间戳进行存储。日期支持多种格式，但是多种格式的支持性能会变差。
2. 日期的转化是利用`java`来实现，`java`日期的转化可能会带来脏数据，如：`021-01-01` 变成一个1999年的时间，该字段正常时间是`2021`，转化过程没有报错，插入成功。
3. `date`的时间精度是`ms`，`data_naos`时间精度为`ns`，带有小数点的日期格式如：{"date": 1618249875.123456}，`data_naos`可能会精度丢失。

### 3.1 date

### 3.2 date_nanos

## 4. Agg type（预聚合类型）

## 5. 时空

### 5.1 geo-ip

### 5.2 ip

## 6. 特殊类型

### 6.1 completion（输入提示）

- 注意

1. 一般使用前缀搜索，新版支持中间命中搜索。

### 6.2 murmur3(一般用于文本聚合)

### 6.3 嵌套类型

#### 6.3.1 数组实现类型嵌套

```
PUT my-index-000001/_doc/1
{
  "group" : "fans",
  "user" : [ 
    {
      "first" : "John",
      "last" :  "Smith"
    },
    {
      "first" : "Alice",
      "last" :  "White"
    }
  ]
}
```

#### 6.3.2 nested

```
PUT my-index-000001
{
  "mappings": {
    "properties": {
      "user": {
        "type": "nested" 
      }
    }
  }
}
PUT my-index-000001/_doc/1
{
  "group" : "fans",
  "user" : [
    {
      "first" : "John",
      "last" :  "Smith"
    },
    {
      "first" : "Alice",
      "last" :  "White"
    }
  ]
}

GET my-index-000001/_search
{
  "query": {
    "nested": {
      "path": "user",
      "query": {
        "bool": {
          "must": [
            { "match": { "user.first": "Alice" }},
            { "match": { "user.last":  "Smith" }} 
          ]
        }
      }
    }
  }
}

GET my-index-000001/_search
{
  "query": {
    "nested": {
      "path": "user",
      "query": {
        "bool": {
          "must": [
            { "match": { "user.first": "Alice" }},
            { "match": { "user.last":  "White" }} 
          ]
        }
      },
      "inner_hits": { 
        "highlight": {
          "fields": {
            "user.first": {}
          }
        }
      }
    }
  }
}
```

#### 6.3.3 join 

```
PUT my-index-000001
{
  "mappings": {
    "properties": {
      "my_id": {
        "type": "keyword"
      },
      "my_join_field": { 
        "type": "join",
        "relations": {
          "question": "answer" 
        }
      }
    }
  }
}

PUT my-index-000001/_doc/1?refresh
{
  "my_id": "1",
  "text": "This is a question",
  "my_join_field": {
    "name": "question" 
  }
}

PUT my-index-000001/_doc/2?refresh
{
  "my_id": "2",
  "text": "This is another question",
  "my_join_field": {
    "name": "question"
  }
}

PUT my-index-000001/_doc/1?refresh
{
  "my_id": "1",
  "text": "This is a question",
  "my_join_field": "question" 
}

PUT my-index-000001/_doc/2?refresh
{
  "my_id": "2",
  "text": "This is another question",
  "my_join_field": "question"
}


```

