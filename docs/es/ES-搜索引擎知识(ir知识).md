# 搜索引擎知识(ir知识)

reference:

https://www.elastic.co/guide/cn/elasticsearch/guide/current/inverted-index.html

## 1. 什么是搜索引擎

- 什么是搜索引擎

(ir)信息检索系统的一种。

- 搜索引擎的发展历史

1. 文本检索(Es)
2. 链接分析(访问量，协同过滤)
3. 用户中心(nlp等)

- 搜索引擎的三个目标

1. 更快
2. 更准
3. 更全

- 搜索引擎的三个核心问题

1. 用户真正的需求是什么
2. 哪些信息是和用户需求真正相关的
3. 哪些信息是用户可以信赖的

## 2. 倒排索引 vs 正排索引 && 字典树联系

## 2.1 倒排索引

- 什么是倒排索引

     将文本通过分词，拆分为`token`，通过记录`token`和对应文档，所在的位置等信息。查询时候就能通过`token`反向找到所在的文档。如：

```
# 文档
1. The quick brown fox jumped over the lazy dog
2. Quick brown foxes leap over lazy dogs in summer
# 分词后
Term      Doc_1  Doc_2
-------------------------
Quick   |       |  X
The     |   X   |
brown   |   X   |  X
dog     |   X   |
dogs    |       |  X
fox     |   X   |
foxes   |       |  X
in      |       |  X
jumped  |   X   |
lazy    |   X   |  X
leap    |       |  X
over    |   X   |  X
quick   |   X   |
summer  |       |  X
the     |   X   |
------------------------
# 搜索 `quick brown` 命中情况如下
Term      Doc_1  Doc_2
-------------------------
brown   |   X   |  X
quick   |   X   |
------------------------
Total   |   2   |  1

```

- es (lucene)中倒排索引是如何组织的？

1. 字典树存放`token`，对应的`lucene`
2. 索引信息单独存放，如`token`所在的文档，`token`所在文档的`offsest`。

- lucene 底层文件

| Segment Info                                                 | .si                                                          | segment的元数据文件                                          |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| Compound File                                                | .cfs, .cfe                                                   | 一个segment包含了如下表的各个文件，为减少打开文件的数量，在segment小的时候，segment的所有文件内容都保存在cfs文件中，cfe文件保存了lucene各文件在cfs文件的位置信息 |
| Fields                                                       | .fnm                                                         | 保存了fields的相关信息                                       |
| Field Index                                                  | .fdx                                                         | 正排存储文件的元数据信息                                     |
| Field Data                                                   | .fdt                                                         | 存储了正排存储数据，写入的原文存储在这                       |
| Term Dictionary                                              | .tim                                                         | 倒排索引的元数据信息                                         |
| Term Index                                                   | .tip                                                         | 倒排索引文件，存储了所有的倒排索引数据                       |
| Frequencies                                                  | .doc                                                         | 保存了每个term的doc id列表和term在doc中的词频                |
| Positions                                                    | .pos                                                         | Stores position information about where a term occurs in the index |
| 全文索引的字段，会有该文件，保存了term在doc中的位置          | 全文索引的字段，会有该文件，保存了term在doc中的位置          | 全文索引的字段，会有该文件，保存了term在doc中的位置          |
| Payloads                                                     | .pay                                                         | Stores additional per-position metadata information such as character offsets and user payloads |
| 全文索引的字段，使用了一些像payloads的高级特性会有该文件，保存了term在doc中的一些高级特性 | 全文索引的字段，使用了一些像payloads的高级特性会有该文件，保存了term在doc中的一些高级特性 | 全文索引的字段，使用了一些像payloads的高级特性会有该文件，保存了term在doc中的一些高级特性 |
| Norms                                                        | .nvd, .nvm                                                   | 文件保存索引字段加权数据                                     |
| Per-Document Values                                          | .dvd, .dvm                                                   | lucene的docvalues文件，即数据的列式存储，用作聚合和排序      |
| Term Vector Data                                             | .tvx, .tvd, .tvf                                             | Stores offset into the document data file                    |
| 保存索引字段的矢量信息，用在对term进行高亮，计算文本相关性中使用 | 保存索引字段的矢量信息，用在对term进行高亮，计算文本相关性中使用 | 保存索引字段的矢量信息，用在对term进行高亮，计算文本相关性中使用 |
| Live Documents                                               | .liv                                                         | 记录了segment中删除的doc                                     |



## 2.2 正排索引

- 什么是正排索引

   关系型数据库中如`id`等具体字段，完整匹配查询建立的就是正排索引，`es`中`doc_value`属于正排索引。

### 2.3 字典树

- `lucene` 使用的是`FST`树，`lucene`内部通过把字符串编码成`Byte`，通过**共享前缀**和**共享后缀**的方式组成`FST`树。如：输入：

  mop/0
  moth/1
  pop/2
  star/3
  stop/4
  top/5

![数据类型](https://github.com/Whojohn/learn/blob/master/docs/es/pic/FST_tree.png?raw=true)


## 3. 分词器

## 4. 搜索质量判定

## 4.1 召回率

本次搜索结果中包含的相关文档占整个集合中所有相关文档的比率。

### 4.2 准确率

本次搜索结果中相关文档所占的比例

|                  | 相关(返回结果) | 不相关(返回结果) |
| ---------------- | -------------- | ---------------- |
| 相关(真实结果)   | 真阳性         | 假阴性           |
| 不相关(真实结果) | 假阳性         | 真阴性           |