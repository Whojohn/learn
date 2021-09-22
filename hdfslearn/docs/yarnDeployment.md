# Yarn 部署(非HA)

特性： lxc+cgroup

Yarn 完整部署方式

# 依赖

## 1. 独立的操作用户(添加非root用户)

- 执行

```
> useradd yarn
> usermod -a -G yarn yarn
```

## 2. 修改所有涉及 LXC 配置

- 执行

1. hadoop 目录配置，用户组一定要配置为root，同时要注意修改完后hadoop的数据目录能否正常读写，如果不能可能造成DataNone无法正常启动

```
> echo $HADOOP_HOME
> /data/hadoop/hadoop-3.1.3
> chown root:yarn /data
> chown -R root:yarn /data/hadoop/
```

1. LXC 配置 (yarn LinuxContainerExecutor 支持)

**修改 $HADOOP_HOME/etc/hadoop/container-executor.cfg 文件**

```
> yarn.nodemanager.linux-container-executor.group=yarn#configured value of yarn.nodemanager.linux-container-executor.group
> banned.users=none#comma separated list of users who can not run applications
> min.user.id=1#Prevent other super-users
> allowed.system.users=root,yarn##comma separated list of system users who CAN run applications
> feature.tc.enabled=false
```

**修改 $HADOOP_HOME/etc/hadoop/container-executor.cfg $HADOOP_HOME/bin/container-executor 权限**

```
> chown root:yarn   $HADOOP_HOME/etc/hadoop/container-executor.cfg
> chmod 0400 $HADOOP_HOME/etc/hadoop/container-executor.cfg
> chown root:yarn $HADOOP_HOME/bin/container-executor
> chmod 6050 $HADOOP_HOME/bin/container-executor
```

1. 修改resourcemanger nodemanager 写入目录(不能放入 $HADOOP_HOME 中，$HADOOP_HOME 不是根目录权限验证无法通过)

**yarn-site.xml 修改 nodemanager 工作目录(目录不能与 $HADOOP_HOME 前缀一致 )**

```
<property>
 <!--nodemanger 工作目录(容器依赖等，混洗工作目录)-->
 <name>yarn.nodemanager.local-dirs</name>
 <value>/data/tmp</value>
</property>
```

**hadoop-env.sh 修改 pid 日志目录(目录不能与 $HADOOP_HOME 前缀一致 )**

```
export HADOOP_PID_DIR=/data/pid
export HADOOP_LOG_DIR=/data/tmp
```

## 4. root 用户创建 cgroup 组

```
/bin/sudo /bin/bash -c '/usr/bin/mkdir -p /sys/fs/cgroup/cpu/yarn'
/bin/sudo chown -R yarn:yarn /sys/fs/cgroup/cpu/yarn/
```

ps: 删除组方式

```
rmdir xxx
```

## 5. 修改yarn-site.xml

1. yarn-site.xml 添加

```
 <property>
        <!--单容器最高内存-->
        <name>yarn.scheduler.maximum-allocation-mb</name>
        <value>4843</value>
    </property>
    <property>
        <!--单容器最高申请虚拟vcore-->
        <name>yarn.scheduler.maximum-allocation-vcores</name>
        <value>6</value>
    </property>
    <property>
        <!--nodemanger 工作目录(容器依赖等，混洗工作目录)-->
        <name>yarn.nodemanager.local-dirs</name>
        <value>/data/tmp</value>
    </property>
    <property>
        <name>yarn.nodemanager.container-executor.class</name>
        <value>org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor</value>
    </property>
    <property>
        <name>yarn.nodemanager.linux-container-executor.resources-handler.class</name>
        <value>org.apache.hadoop.yarn.server.nodemanager.util.CgroupsLCEResourcesHandler</value>
    </property>
    <!-- NM 的 Unix 用户组，需要跟 container-executor.cfg 里面的配置一致，主要用来验证是否有安全访问 container-executor 二进制的权限 -->
    <property>
        <name>yarn.nodemanager.linux-container-executor.group</name>
        <value>yarn</value>
    </property>
    <!-- 集群在 nonsecure 模式时，是否限制 container 的启动用户，true：container 使用统一的用户启动 false: container 使用任务用户启动 -->
    <property>
        <name>yarn.nodemanager.linux-container-executor.nonsecure-mode.limit-users</name>
        <value>true</value>
    </property>
    <!-- 集群在 nonsecure 模式时，且开启 container 启动用户限制时，统一使用的用户，如果不设置，默认为 nobody -->
    <property>
        <name>yarn.nodemanager.linux-container-executor.nonsecure-mode.local-user</name>
        <value>yarn</value>
    </property>
    <property>
        <!-- cgroup 挂载名字 -->
        <name>yarn.nodemanager.linux-container-executor.cgroups.hierarchy</name>
        <value>/yarn</value>
    </property>
    <property>
        <!-- 禁止自动挂载 -->
        <name>yarn.nodemanager.linux-container-executor.cgroups.mount</name>
        <value>false</value>
    </property>
    <property>
        <!-- 自动挂载目录-->
        <name>yarn.nodemanager.linux-container-executor.cgroups.mount-path</name>
        <value>/sys/fs/cgroup</value>
    </property>
    <property>
        <!-- cpu 上限强制禁止-->
        <name>yarn.nodemanager.resource.percentage-physical-cpu-limit</name>
        <value>50</value>
    </property>
    <property>
        <!-- 按照平分原则划分vcore-->
        <name>yarn.nodemanager.linux-container-executor.cgroups.strict-resource-usage</name>
        <value>true</value>
    </property>
    <property>
        <!-- 将虚拟vcore 换算为真实cpu 运行时执行-->
        <name>yarn.nodemanager.resource.count-logical-processors-as-cores</name>
        <value>true</value>
    </property>
```

## 6. 切换为yarn 用户，启动yarn

1. 环境检查(正常不会有任何报错,自行通过错误信息修改依赖)

```
> $HADOOP_HOME/bin/hadoop checknative
# 不能全为 false；部分压缩格式需要自行打包，才能支持。
2020-11-04 04:52:18,485 INFO bzip2.Bzip2Factory: Successfully loaded & initialized native-bzip2 library system-native
2020-11-04 04:52:18,488 INFO zlib.ZlibFactory: Successfully loaded & initialized native-zlib library
2020-11-04 04:52:18,490 WARN zstd.ZStandardCompressor: Error loading zstandard native libraries: java.lang.InternalError: Cannot load libzstd.so.1 (libzstd.so.1: cannot open shared object file: No such file or directory)!
2020-11-04 04:52:18,840 WARN erasurecode.ErasureCodeNative: ISA-L support is not available in your platform... using builtin-java codec where applicable
Native library checking:
hadoop:  true /data/hadoop/hadoop-3.1.3/lib/native/libhadoop.so.1.0.0
zlib:    true /lib64/libz.so.1
zstd  :  false
snappy:  true /lib64/libsnappy.so.1
lz4:     true revision:10301
bzip2:   true /lib64/libbz2.so.1
openssl: true /lib64/libcrypto.so
ISA-L:   false libhadoop was built without ISA-L support
> $HADOOP_HOME/bin/container-executor --checksetup
```

1. 启动

```
> $HADOOP_HOME/bin/yarn --debug  --daemon start resourcemanager
> $HADOOP_HOME/bin/yarn --debug  --daemon start nodemanager
```
