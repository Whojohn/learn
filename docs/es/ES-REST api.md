# ES-REST api

> reference:
>
> https://www.elastic.co/guide/en/elasticsearch/reference/current/rest-apis.html

    `REST api` 中包含集群所有的接口，常用的接口是`集群管理相关（cluster,index,snapshot）`和`文档接口(CURD)`。这里主要介绍`CURD`还有基础的`cluster`和`index`常用接口。

## 1. Document api(CURD&Multi操作)

> https://www.elastic.co/guide/en/elasticsearch/reference/7.15/docs.html

- 前置知识

  1. 任何`rest`操作都有返回值，其中`delete`,`update`,`index`中有 `result `，包含以下状态：`create`，`delelte`，`noop` ，`update`。

     ```
     {
       "_index" : "test_index",
       "_type" : "_doc",
       "_id" : "1",
       "_version" : 15, // 版本号
       "result" : "noop",// 执行操作
       "_shards" : {
         "total" : 0,
         "successful" : 0,
         "failed" : 0
       },
       "_seq_no" : 14,
       "_primary_term" : 1
     }
     ```

  2. 更新有2种：`update`：覆盖更新，`noop`：部分更新。`update`会把文档更新为输入`field`，`noop`会对比假如存在差异则覆盖更新，不存在则不执行任何操作。

### 1.1 Create(创建index&文档)

> reference:
> https://www.elastic.co/guide/en/elasticsearch/reference/7.6/docs-index_.html

#### 1.1.1 创建index

- 基本语法：

```
PUT /my_index
{
    "settings": { ... any settings ... },
    "mappings": {
    "type_one": { ... any mappings ... },
    "type_two": { ... any mappings ... },
    ...
    
}
```

#### 1.1.2 创建文档

> 默认情况下，假如`index`不存在，`es`会自动创建`index`，并且配置`mapping`

- 基本语法

  **必须要注意部分更新和覆盖更新，覆盖更新会把旧数据覆盖**

```
#相当于PUT/<index>/_doc/<_id>?op_type=create已经存在_id则失败，报版本错误
PUT /<index>/_create/<_id>
# 已经存在id则覆盖更新
PUT /<index>/_doc/<_id>
# 部分更新

#已经存在_id则失败，报版本错误
POST /<index>/_create/<_id>
# 已经存在id则覆盖更新
POST /<index>/_doc/<_id>
```

- 例子：

```
POST test_2/_doc
{
  "stringTest": "Shard allocation is the process of allocating shards to nodes. This can happen during initial recovery, replica allocation, rebalancing, or when nodes are added or removed.",
  "shorWord": "tom",
  "longTest": 123456
}

返回
{
  "_index" : "test_2",
  "_type" : "_doc",
  "_id" : "TDoooXIBNUaOL4fc2RMK",
  "_version" : 1,
  "result" : "created",
  "_shards" : {
    "total" : 2,
    "successful" : 1,
    "failed" : 0
  },
  "_seq_no" : 9,
  "_primary_term" : 1
}
```

### 1.2 Update

#### 1.2.1 update 文档

reference:
https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update.html

https://www.elastic.co/guide/cn/elasticsearch/guide/current/partial-updates.html

>           `es`对于并发更新是对比`if_seq_no`且`if_primary_term`是否一致控制的。因此对于用户来说，需要控制并发，必须更新时要控制并发，必须声明该遍历。对于update by query 内部是通过`_version`控制的。**注意7版本已经不允许外部通过_version控制并发，只能指定`if_seq_no`，`if_primary_term`控制。**

- 基本语法

```
#指定文档更新
POST /<index>/_update/<_id>
POST /<index>/_update/<_id>?if_seq_no=xxx&if_primary_term=xxx
```

- 核心配置

1. **retry_on_conflict**： `update`失败的时候重试。

>     为了避免数据丢失， `update API` 在 *检索* 步骤时检索得到文档当前的 `_version` 号，并传递版本号到 *重建索引* 步骤的 `index` 请求。 如果另一个进程修改了处于检索和重新索引步骤之间的文档，那么 `_version` 号将不匹配，更新请求将会失败。

2. `if_seq_no`和`if_primary_term`：并发版本控制
3. `_source_excludes`：排除索引字段
4. `wait_for_active_shards`: 需要多少个副本`shard`成功后才返回

- 例子

```
PUT test_1/_doc/1
{"content":"asdfasdfasdf"}

POST test_1/_update/1?if_seq_no=0&if_primary_term=1
{
  "doc": {
    "content": "test"
  }
}
```



#### 1.2.2 update by query

> reference:
>
> https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update-by-query.html

- 注意

1. **！！！注意！！！update by query 假如没有query 会更新所有文档；不带有 query 一般用于`field` 添加新的类型，通过`update`写入了新字段。**
2. update by query 内部只会执行执行那一刻生成的查询快照进行更新。

- 基本语法

```
#按照查询条件更新
POST /<index>/_update_by_query
#POST twitter/_update_by_query?conflicts=proceed
```

- 核心配置

1. wait_for_completion=false ，后台执行，返回执行任务号。对于大修改来说更加友好。
2. requests_per_second ，防止批量更新过多，设置为`-1`表示不限制。
3. slices ，后台并发数，可以提高`update`速度，但是需要消耗更多资源。
4. scroll_size， 单次操作条数。

### 1.3 Retrieve

> reference:
>
> https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html

- 基本语法

```
#获取单条记录
GET <index>/_doc/<_id>
GET <index>/_source/<_id>
#判定该_id 文档是否存在
HEAD <index>/_doc/<_id>
HEAD <index>/_source/<_id>
```

### 1.4 Delete

#### 1.4.1 delete index

- 基本语法

```
DELETE /<index>
```

#### 1.4.2 delete document

- 基本语法

```
DELETE /<index>/_doc/<_id>
```

#### 1.4.2 delete by query 

- 基本语法

```
POST /<index>/_delete_by_query
```

- 核心配置

1. requests_per_second , 控制每一秒删除条数。
2. slices ，删除并发度。
3. scroll_size，删除单次大小。

- 样例

````
POST /my-index-000001/_delete_by_query
{
  "query": {
    "match": {
      "user.id": "elkbee"
    }
  }
}
````









