# Flink HDFS 相关问题

- 问题

  1. `FileSystem` 连接器能否支持多集群写入，比如 `HDFS a`，`HDFS b` 两个不同的集群。

  > 不可以， 因为`FileSystem`连接器底层走的是`FileSystem`，`FileSystem`的实例只能有一个。并且`HDFS`的实现是通过`HadoopUtils.getHadoopConfiguration`函数获取配置实例化，该函数写死，集群配置从环境变量中取。配置多个集群配置下，会覆盖掉另一个集群配置。

  2. `checkpoint` 与 `savepoint` 是如何写入到状态后端(`FileSystem`)？

     > FileSystem 的 HDFS 的实现缺陷是必须配置环境变量。不能在代码中配置状态后端地址(**除非`hdfs`不是 `ha`或者没有验证等能够直接通过物理地址连接**)。

  3. `FileSystem`连接器与`Hive`连接器区别？

  > `FileSystem`连接器底层走的是`FileSystem`(因此必须与集群信息绑定，能写入的目标集群必须与集群配置的一致)。`Hive`连接器走的是直接返回`HdfsFileSystem`对象，因此可以适配多个集群。

  4. 为什么本地配置`debug`涉及状态后端(排除本地文件)，都必须配置环境变量如：`HADOOP_HOME`， `HADOOP_CONF_DIR` ？

  > 因为状态后端持久化需要`FileSystem`支持，`hdfs`是通过环境变量去读取实现的，所以必须配置环境变量。

- 结论

1. `FileSystem` 中默认文件系统只能有一个实例对象(`FS_FACTORIES`是`MAP`对象)，注意这个`FileSystem`与`Java`原生的`FileSystem`不是一个东西。
2. `FileSystem`中`HDFS`实现类为:`HadoopFileSystem`，配置的读取由`HadoopUtils.getHadoopConfiguration`控制，**控制`HDFS`配置都必须通过环境变量传入。**
3. `Hive`连接器能写入多集群是因为不走`FileSystem`，直接实例化`HDFSFileSystem`对象。
4. 如何解决`FileSystem`连接器不支持多集群问题？

> 1. 自行实现连接器：如 `flinkx`（`Flinkx`）的实现是旧版的声明方式，1.15 会被移除。
> 2. `FlileSystem`连接器改写：`FileSystemTableFactory`允许传入配置文件地址，`FileSystemTableSink` 对应特定的连接器，不走`FileSystem`获取对象，直接获取对应的实现对象。如`Hive`中一样。
> 3. 走 hive 写入，配一个没有用的 `MeatStore`即可。

## Flink Hadoop 依赖读取方式详解

- 总结

1. 必须通过环境变量获取配置。
2. 假如有多个配置，优先级高配置会覆盖优先级低的配置，配置优先级：HADOOP_HOME <-  flink 配置写死(改方法移除)  <-  HADOOP_CONF_DIR。

- 核心代码  （HadoopUtils.getHadoopConfiguration）

```
    public static Configuration getHadoopConfiguration(
            org.apache.flink.configuration.Configuration flinkConfiguration) {

        // Instantiate an HdfsConfiguration to load the hdfs-site.xml and hdfs-default.xml
        // from the classpath

        Configuration result = new HdfsConfiguration();
        boolean foundHadoopConfiguration = false;

        // We need to load both core-site.xml and hdfs-site.xml to determine the default fs path and
        // the hdfs configuration.
        // The properties of a newly added resource will override the ones in previous resources, so
        // a configuration
        // file with higher priority should be added later.

        // Approach 1: HADOOP_HOME environment variables
        String[] possibleHadoopConfPaths = new String[2];

        final String hadoopHome = System.getenv("HADOOP_HOME");
        if (hadoopHome != null) {
            LOG.debug("Searching Hadoop configuration files in HADOOP_HOME: {}", hadoopHome);
            possibleHadoopConfPaths[0] = hadoopHome + "/conf";
            possibleHadoopConfPaths[1] = hadoopHome + "/etc/hadoop"; // hadoop 2.2
        }

        for (String possibleHadoopConfPath : possibleHadoopConfPaths) {
            if (possibleHadoopConfPath != null) {
                foundHadoopConfiguration = addHadoopConfIfFound(result, possibleHadoopConfPath);
            }
        }

        // Approach 2: Flink configuration (deprecated)
        final String hdfsDefaultPath =
                flinkConfiguration.getString(ConfigConstants.HDFS_DEFAULT_CONFIG, null);
        ....


        // Approach 3: HADOOP_CONF_DIR environment variable
        String hadoopConfDir = System.getenv("HADOOP_CONF_DIR");
		....

        return result;
    }
```



## FileSystem 

- 获取 FileSystem 实例方法(获取实例后用于读写)

> FileSystem.get(URI uri)



## FlieSystem Sql 连接器相关方法

FileSystemTableFactory -> FileSystemTableSink -> StreamingSink

- 总结

1. `FileSystem`连接器是特殊的，内部包括了几个操作流程如：分区，提交等。**实际 StreamGraph 会有多个操作**。
2. 需要修改`FileSystem`方法到支持多个同类源，不同集群实例，如：hdfs 支持多集群。需要改写：FileSystemTableSink.createStreamingSink 中 **`fsFactory`实例为非`FileSystem`实现。**

- FileSystemTableSink.createStreamingSink 方法解析

```
    private DataStreamSink<?> createStreamingSink(
            DataStream<RowData> dataStream, Context sinkContext) {
            
         ！！！！！！！ 多实例支持修改这里，不要走`FileSystem`，或者返回非默认的`FileSystem`实现
        FileSystemFactory fsFactory = FileSystem::get;
        
        
        // 文件滚动配置
        TableRollingPolicy rollingPolicy 
		// 文件名
        String randomPrefix = "part-" + UUID.randomUUID().toString();
        
        // 编码处理
        if (isEncoder) 
        // 是否开启合并
        if (autoCompaction) 
        ...
            writerStream =
                    StreamingSink.compactionWriter(
                            dataStream,
                            bucketCheckInterval,
                            bucketsBuilder,
                            fsFactory,
                            path,
                            reader,
                            compactionSize);
        
        return StreamingSink.sink(
                writerStream,
                path,
                tableIdentifier,
                partitionKeys,
                new EmptyMetaStoreFactory(path),
                fsFactory,
                tableOptions);
    }
```



