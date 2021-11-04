# ES-CURD

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

## 1. 新建文档

> reference:
> https://www.elastic.co/guide/en/elasticsearch/reference/7.6/docs-index_.html

### 1.1 创建idnex 

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

### 1.2 创建文档

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

## update

reference:
https://www.elastic.co/guide/en/elasticsearch/reference/7.6/docs-update.html
https://www.elastic.co/guide/en/elasticsearch/reference/7.6/docs-update-by-query.html#docs-update-by-query

- 基本语法

> \#指定文档更新
> POST /<index>/_update/<_id>
> \#按照查询条件更新
>