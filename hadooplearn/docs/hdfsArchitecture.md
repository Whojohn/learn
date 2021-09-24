# HDFS - 基础架构

HDFS 是**文件系统**，它有两大核心**命名空间**，**底层存储(块)**。命名空间只能是`Namenode`  管理和实现。底层存储可以是 `HDFS DistributedFileSystem`(原生实现)，更可以是 `s3`，`oos`等。

> reference:
>
> 1. https://hadoop.apache.org/docs/r3.1.4
> 2. https://tech.meituan.com/
>    hdfs 源码解析：
> 3. Hadoop 2.X HDFS源码剖析 by 徐鹏 (与hdfs 3.x 部分细节不一样，总体思路不变)
> 4. https://zhangboyi.blog.csdn.net/article/details/111830496 （3.2 源码)

- 核心存储结构如下

  ```
  ├── dfs
  │   ├── data
  │   │   ├── current
  │   │   │   ├── BP-1166290150-127.0.0.1-1631504107159
  │   │   │   │   ├── current
  │   │   │   │   │   ├── dfsUsed
  │   │   │   │   │   ├── finalized
  │   │   │   │   │   │   └── subdir0
  │   │   │   │   │   │       ├── subdir0
  │   │   │   │   │   │       ├── subdir1
  │   │   │   │   │   │       ├── subdir2
  │   │   │   │   │   │       ├── subdir3
  │   │   │   │   │   │       └── subdir4
  │   │   │   │   │   │           ├── blk_1073743014
  │   │   │   │   │   │           ├── blk_1073743014_2194.meta
  │   │   │   │   │   │           ├── blk_1073743015
  │   │   │   │   │   │           └── blk_1073743015_2195.meta
  │   │   │   │   │   ├── rbw
  │   │   │   │   │   └── VERSION // 所属集群
  │   │   │   │   ├── scanner.cursor
  │   │   │   │   └── tmp
  │   │   │   └── VERSION
  │   │   └── in_use.lock
  │   ├── name
  │   │   ├── current
  │   │   │   ├── edits_0000000000000000001-0000000000000000002
  │   │   │   ├── edits_0000000000000015905-0000000000000015906
  │   │   │   ├── edits_inprogress_0000000000000015907
  │   │   │   ├── fsimage_0000000000000015906
  │   │   │   ├── fsimage_0000000000000015906.md5
  │   │   │   ├── seen_txid
  │   │   │   └── VERSION // 集群唯一标识
  │   │   └── in_use.lock
  ├── hadoop-hadoop-datanode.pid
  ├── hadoop-hadoop-namenode.pid
  ```



## 1 Namenode

### 1.1 Namenode 启动流程

> reference:
>
> https://zhangboyi.blog.csdn.net/article/details/108416995
>
> https://hadoop.apache.org/docs/r3.1.4/hadoop-project-dist/hadoop-hdfs/HDFSCommands.html#namenode

启动步骤：

1. 解析配置文件

2. 解析启动命令行配置

3. 根据启动命令行执行不同的操作：

   > format ：初始化 `NameNode`
   >
   > rollback: 回滚操作
   >
   > recorer: 恢复元数据错误
   >
   > upgrade: 升级使用
   >
   > namenode: 启动namenode
   >
   > bootstrapstandby: 初始化 standbyNamenode（复制active 节点所有 命名空间信息）
   >
   > initializeSharedEdits：standbyNamenode 重新拉取 active 节点所有命名空间信息
   >
   > checkpointnode: 类似 Secondary node 作用，不同的是，`checkpointnode` 能够跨机器上传
   >
   > 默认项目(不填任何启动参数): 相当于namenode

4. 假设启动 `NameNode` 执行以下操作

   > 1. 是否ha， 非ha 直接进入 active 状态。ha 进入到对应角色中。
   > 2. 启动 http 服务、启动 FSNamesystem(命名空间服务)、rpc、监控数值等服务。

### 1.2 NameSpace (命名空间的管理)

reference:

https://hadoop.apache.org/docs/current/api/org/apache/hadoop/fs/FileSystem.html

    HDFS 命名空间管理的文件组织结构的信息如：文件与文件夹的路径。文件夹，文件的权限、创建、修改时间副本，还控制着文件的打开、关闭、写入(并发控制由租约实现)、重命名等操作。

### 1.2.1 Fsimage & Edit log

> reference:
>
> Fsimage&Edit log 源码解析：https://blog.csdn.net/zhanglong_4444/article/details/108469062
>
> 文件作用解析：https://docs.cloudera.com/runtime/7.2.10/data-protection/topics/hdfs-namenodes.html

      `Fsimage` 是周期性的命名空间内存快照，`edit log` 是每一个操作的日志。通过内存快照+操作日志回放，可以保证 `NameNode`每一次重启后都能正确恢复状态。并且通过 StandbyNameNode ，周期性整理快照合并操作日志，以提高重启恢复速度。假如`edit log`存在逻辑异常如：File is not under construction 等，可以通过`namenode -recover`跳过异常行恢复。注意！！！ `edit log `的周期整理前提是不存在逻辑异常，假如逻辑异常，会导致快照整理失败，无法启动。以下是一个文件非读产生事务操作的整体流程。

![事务触发 Editlog 修改流程](https://github.com/Whojohn/learn/blob/master/hadooplearn/docs/pic/editlogWriteProcess.png?raw=true)

- transactionId

  任何一个命名空间相关的操作都会有一个 `transactionId ` 进行唯一标识(自增顺序，不是自增也会引发回放逻辑异常)。

- Edit log 命名方式是：

  > 注意正常退出的namenode 只会存在历史editlog 格式，非正常退出或者Namenode 正在运行，会以正在运行的格式进行保存。

  1. 历史editlog 格式： **edits_start transaction id-end transaction id**。 如：edits_0000000000000000001-0000000000000000002 记录了 1到2的事务。
  2. 正在进行的editlog格式：**edits_inprogress_start transaction** id。

- seen_txid

      存放最后一个checkpoint（fsimage & edit log 合并结果）或者是当前 `edits_inprogress_start `的`transactionId `。**注意seen_txid并不是最新的事务id。** seen_txid 用于 Namenode 启动检测，内存中加载的事务是否超过seen_txid，超过才能正常启动。**不使用最新事务id是为了减少同步写压力。**

### 1.3 元数据管理(块管理)

    `HDFS`原生通过 Datanode 提供块存储的能力，NameNode 启动时候，`DataNode` 扫描自身块信息上班到`Namenode`中，Namenode 建立整个系统的元数据管理。**注意元数据管理在NameNode只有内存对象，所有元数据信息都保存在datanode中**，块管理包括：

      1. `DataNode`注册，接收`DataNode`块信息上报，维护全局块位置信息。
      2. 处理块相关的操作：创建、删除、修改、复制、删除以及获取块位置列表。

### 1.4 租约管理(写入并发管理)

> reference:
>
> https://blog.cloudera.com/understanding-hdfs-recovery-processes-part-1/
>
> https://zhangboyi.blog.csdn.net/article/details/109409016



    `HDFS`通过引入分布式租约(lease)来实现对**并发写入的互斥**，**租约在HDFS中也只有内存对象**。

- 租约

  写入必须先向`Namenode`申请租约，只有获取租约成功才能写入文件。`HDFS`中租约分为软租约和硬租约。软租约：在软租约持有者不超时(60s)的情况下，互斥所有其他请求写入。当软租约持有者大于60s，没有续约(正常写入端会不断发送心跳以续约)，其他租约申请者可以强制获取租约。硬租约：当60min过去，没有任何收到任何租约结束信号，`NameNode`会移除该租约。

- 并发操作限制

| 并发类型                       | 写                                                           | 并发读\修改文件名\修改文件                                   |
| ------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| **写**                         | 禁止并发写。资源的释放要么主动，要么软租约超时未续约。       | 对于正在写入的文件，改名不会影响写入。可以更改文件路径和文件名。 |
| **并发读\修改文件名\修改文件** | 对于正在写入的文件，改名不会影响写入。可以更改文件路径和文件名。 | 不允许                                                       |

### 1.5 NetworkTopology & CacheManger 等其他管理

      **这些管理都是只有内存对象。**`NetworkTopology`数据物理分布的拓扑图，机架感知实现。`CacheManger ` hdfs cache 管理，用于缓存小数据，只能缓存读不能缓存写(hdfs 缓存写是通过 disk = ram 类型实现，效果差一般不启用)。

### 1.6 version 文件

所属集群唯一标识



## 2 Datanode

### 2.1 DataNode 启动过程

![DataNode 启动过程](https://github.com/Whojohn/learn/blob/master/hadooplearn/docs/pic/datanodeStartProcess.png?raw=true)

Datanode将块管理功能切分为两个部分：

1. `DataStorage`:  管理与组织磁盘存储目录（由dfs.data.dir指定，具体物理存储格式如`2.2.1`所示） ， 如current、previous、 detach、 tmp等；

2. 管理与组织数据块及其元数据文件， 这个功能主要由FsDatasetlmpl相关类实现。



### 2.2 块管理

####  2.2.1 (物理文件组织格式)

```
> │   ├── BP-1166290150-127.0.0.1-1631504107159
> │   │   ├── current
> │   │   │   ├── dfsUsed
> │   │   │   ├── finalized
> │   │   │   │   └── subdir0
> │   │   │   │       ├── subdir0
> │   │   │   │       ├── subdir1
> │   │   │   │       ├── subdir2
> │   │   │   │       ├── subdir3
> │   │   │   │       └── subdir4
> │   │   │   │           ├── blk_1073743014
> │   │   │   │           ├── blk_1073743014_2194.meta
> │   │   │   │           ├── blk_1073743015
> │   │   │   │           └── blk_1073743015_2195.meta
> │   │   │   ├── rbw
> │   │   │   └── VERSION // 所属集群
> │   │   ├── scanner.cursor
> │   │   └── tmp
> │   └── VERSION
> └── in_use.lock
```

- BP-random integer-NameNode-IP address-creation time

BP-随机数-Namonodeip-创建时间；块池目录，联邦模式会有多个块池。

- VERSION 文件
```
> `DataNode`  Version 文件样式：
>
> [hadoop@test current]$ cat VERSION
> #Wed Sep 15 21:16:18 EDT 2021
> namespaceID=1335010847
> cTime=1631504107159
> blockpoolID=BP-1166290150-127.0.0.1-1631504107159  // 跟NameNode 标识一致
> layoutVersion=-57
```
- finalized 目录

存放已经写入完成的文件

- rbw

正在写入的文件

- scanner.cursor

datanode 定期扫描每个块文件并进行校验和验证以检测数据是否正常

-  in_use.lock

datanode 实例互斥文件夹锁

### 2.3 零拷贝

**`DataNode` 支持数据的读取通过零拷贝实现，所以数据的校验是在客户端进行校验的。**



## 2.4 JournalNode & DFSZKFailoverController （HA 角色）

### 3. 读写流程

## 3.1 读流程

1. `Client` 通过调用 `FileSystem.get` 初始化连接器配置并且返回 `FileSystem` 实例化对象 `DistributedFileSystem` ，`DistributedFileSystem.open` 触发返回`FSInputStream` 实例，`FSInputStream`实例了标准 `inputstream` 接口，屏蔽了 `hdfs`底层块存储读取等细节。`FSInputStream`  实例`HdfsDataInputStream`  初始化大致为(底层存储用s3 等则走不一样实现)：

   > 1.  checkopen(检测 与 namenode 连接是否正常)
   >
   > 2.  调用 `getBlockLocations` 返回第一个数据块的`datanode`网络拓扑图。

2. NameNode 判定`Client`是否具有对应的操作权限，通过则返回数据所在`DataNode`列表以及`block`信息(文件的网络拓扑图)。

3. Clinet 调用`read` 读取文件，根据网络拓扑图计算出数据分布不一样，读取方式有以下。

> - 目标datanode 与 client 在同一主机
>   1. 尝试是否可以进行零拷贝。
>   2. 零拷贝失败，短路读取(直接读取本地文件)。
> - `datanode` 与 client 不再同一主机上
>
> 通过网络rpc 进行传输。
>
> **注意 datanode 不进行数据校验，只有 client 才进行数据的校验。数据校验失败时，通过 `reportBadBlocks` 告知 `NameNode` 数据损坏。**

4. 用户调用close 回收资源。

### 3.1.1 读流程异常处理

- 权限，文件不存在异常
- datanode 传输时连接失败

> 本地会维护一个不可达datanode 列表，避免网络重试，并且从数据分布列表中挑选新的读取 `datanode`节点。

- 下载数据校验失败

      **注意 datanode 不进行数据校验，只有 client 才进行数据的校验。数据校验失败时，通过 `reportBadBlocks` 告知 `NameNode` 数据损坏。**

## 3.2 写过程

![HDFS 写入流程](https://github.com/Whojohn/learn/blob/master/hadooplearn/docs/pic/hdfsWriteProcess.png?raw=true)

1. `Client` 通过调用 `FileSystem.get` 初始化连接器配置并且返回 `FileSystem` 实例化对象 `DistributedFileSystem` 。`DistributedFileSystem.create` 调用 `DFSClient.create`方法检测是否具有操作权限，创建`HdfsDataOutputStream(DFSOutputStream,FSOutputStream 的子类)`实例用于文件写入，并且尝试获取`lease` 返回`HdfsDataOutputStream`实例。

2. `Namenode` 鉴权通过后，在命名空间中新建文件，且记录到`edit log`中，`client`在没有调用`write`方法前，并不会申请任何存储块。

3. `Client`调用`FSOutputStream.write`方法写入数据，会先通过`addBlock`方法返回对应的数据块(`DFSClient`同时在后台保持`lease`的续约心跳)。数据写入具体步骤如下：



   > 1. 调用write 方法的数据会被写入`dataqueue`中，并且按照大小切分数据，填上数据校验信息。`DataStreamer`会负责单个数据包的写入，如数据块申请，写入，写入数据放入`ackQueue`中。`PackerProcessor`负责接收`datanode`写入完成的`ack信号`，并且从`ackQueue`中删除数据(没有收到ack，重新放入`dataQueue`)。
   > 2. 对于`datanode`来说，建立rpc 调用链，首个`datanode`按照client指定的调用链依次调用写入，完成后，从链反向传输ack给client(ack 次数不一定要满足副本，只要满足最小写入副本也视为成功)。

4. 关闭流，释放资源，租约。

### 3.2.1 写故障处理

> 注意ack 信号只要满足最小写入副本，即可返回，没有返回意味着datanode掉线。当ack 满足最小写入副本时，**调用链中某个`datanode` 掉线，只会剔除该节点，更新块时间戳，不会进行重试。**

- 没有收到ack(datanode 掉线)

  输出流中缓存的没有确认的数据包会重新加入发送队列，这种机制确保了数据节点出现故障时不会丢失任何数据，并且会为数据块申请个新时间戳，防止`datanode`恢复后，集群出现数据不一致。(掉线的datanode上报到namenode，namenode告知最新的时间戳，datanode会删除旧时间戳的数据块)

  故障数据节点会从输入流管道中删除，通知 Namenode 分配新的数据节点到数据流管道中。接下来输出流会将新分配的 Datanode 添加到数据流管道中，并使用**新的时间戳**重新建立数据流管道。由于新添加的数据节点上并没有存储这个新的数据块，这时 HDFS 客户端会通过 DataTransferProtocol 通知数据流管道中的 Datanode 复制这个数据块到新的 Datanode 上。

- ack 次数大于等于最小写入数但链中某个datanode 失败

  调用链中某个`datanode` 掉线，只会剔除该节点，**更新块时间戳**，不会进行重试，总副本数由`Namenode`进行保障。

