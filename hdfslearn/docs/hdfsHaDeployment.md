# [HDFS-HA高可用集群（生产）]

## [服务规划]

|        | NN-1 | NN-2 | DN   | ZKFC | JNN  |
| :----- | :--- | :--- | :--- | :--- | :--- |
| hdfs01 | √    |      | √    | √    | √    |
| hdfs02 |      | √    | √    | √    | √    |
| hdfs03 |      |      | √    |      | √    |

## [前置准备]

### [ZK节点创建]

- （1）登录zk机器
- （2）docker ps
- （3）docker exec -it zk-10 /bin/bash
- （4）bin/zkCli.sh -server 127.0.0.1:12181
- （5）create /hdfs_ha01 hdfs
- （6）get /hdfs_ha01

相关安装均需放到/data目录下 原因：/data属于挂载的磁盘，空间大

### [设置主机名（3台）] 

- hostnamectl （hdfs 读取的是临时hostname）

> hostname 有静态 hostname 和 临时 hostname
>
> - /etc/hostname 修改的是静态hostname
> - hostnamectl set-hostname <newhostname> 修改的是临时hostname

### [添加主机IP映射（3台）]

### [防火墙开端口（3台）]

NameNode:

- rpc:8020
- http:9870
- https:9871

DataNode:

- address:9866
- rpc:9867
- http:9864
- https:9865

JournalNode:

- rpc：8485
- http: 8480
- https:8481

### [机器配置优化]

#### [修改limits文件限制大小]

echo "* soft nofile 1048576" >> /etc/security/limits.conf

echo "* hard nofile 1048576" >> /etc/security/limits.conf

#### [开启时间同步服务]

systemctl status chronyd

systemctl enable chronyd

### [配置SSH免密登录（3台）] （可选，推荐不配置)

```
ssh-keygen -t rsa 

ssh-copy-id hdfs01
ssh-copy-id hdfs02
ssh-copy-id hdfs03
```

同时在hdfs02和hdfs03上也要执行该命令，配置到其他两台机器的免密登录

### [卸载自带JDK（3台）]

```
rpm -qa|grep java
rpm -qa|grep jdk

rpm -e --nodeps
```

## [安装JDK（3台）]

```
# 解压
tar -zxvf /opt/software/jdk-8u261-linux-x64.tar.gz -C /data

# 配置环境变量
vi ~/.bash_profile

# 环境变量内容
export JAVA_HOME=/data/jdk1.8.0_261
export JRE_HOME=$JAVA_HOME/jre
export CLASSPATH=.:$JAVA_HOME/jre/lib/rt.jar:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
export PATH=$PATH:$JAVA_HOME/bin:$JRE_HOME/bin

# source立即生效
source ~/.bash_profile

# 校验
java
java -version

# 分发

# 其他机器source立即生效
source ~/.bash_profile

# 其它机器校验
java
java -version
```

## [安装Hadoop HDFS（3台）]

### [创建目录]

```
[root@k8s01 ~]# mkdir -p /data/hadoop
```

### [解压]

```
[root@k8s01 ~]# tar -zxvf /opt/software/hadoop-3.3.0.tar.gz -C /data/hadoop/
```

### [配置环境变量]

```
[root@k8s01 ~]# vi ~/.bash_profile 
export JAVA_HOME=/data/jdk1.8.0_261
export JRE_HOME=$JAVA_HOME/jre
export CLASSPATH=.:$JAVA_HOME/jre/lib/rt.jar:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
export HADOOP_HOME=/data/hadoop/hadoop-3.3.0
export PATH=$PATH:$JAVA_HOME/bin:$JRE_HOME/bin:$HADOOP_HOME/bin:$HADOOP_HOME/sbin
[root@k8s01 ~]# source ~/.bash_profile 
```

### [修改配置文件（总8个）]

- core-site.xml
- hdfs-site.xml
- hadoop-env.sh
- workers
- mapred-site.xml（此次不使用，未配置）
- mapred-env.sh（此次不使用，未配置）
- yarn-site.xml（此次不使用，未配置）
- yarn-env.sh（此次不使用，未配置）

#### [hadoop-env.sh]

```sh
export JAVA_HOME=/data/jdk1.8.0_261
export HDFS_NAMENODE_USER=hadoop
export HDFS_DATANODE_USER=hadoop
export HDFS_SECONDARYNAMENODE_USER=hadoop
export HDFS_ZKFC_USER=hadoop
export HDFS_JOURNALNODE_USER=hadoop

#这种方式需要所有机器内网IP前缀一致
bind_ip=$(/bin/hostname)
#这种方式需要在/etc/hosts里将hostname指向内网IP
export BIND_OPTS="-Dlocal.bind.address=${bind_ip}"
# Command specific options appended to HADOOP_OPTS when specified (hadoop3 命令变成hdfs，hadoop命令被废弃)
export HDFS_NAMENODE_OPTS="-Dcom.sun.management.jmxremote $HDFS_NAMENODE_OPTS $BIND_OPTS"
export HDFS_SECONDARYNAMENODE_OPTS="-Dcom.sun.management.jmxremote $HDFS_SECONDARYNAMENODE_OPTS $BIND_OPTS"
export HDFS_DATANODE_OPTS="-Dcom.sun.management.jmxremote $HDFS_DATANODE_OPTS $BIND_OPTS"
export HDFS_JOURNALNODE_OPTS="-Dcom.sun.management.jmxremote $HDFS_JOURNALNODE_OPTS $BIND_OPTS"
export HDFS_BALANCER_OPTS="-Dcom.sun.management.jmxremote $HDFS_BALANCER_OPTS $BIND_OPTS"
```

#### [core-site.xml]

```xml
<configuration>
<!-- 指定hdfs的nameservice为ns1 -->
<property>
<name>fs.defaultFS</name>
<value>hdfs://ns1</value>
</property>

<!-- 指定hadoop临时目录 -->
<property>
<name>hadoop.tmp.dir</name>
<value>/data/hadoop/hdata/tmp</value>
</property>

<!-- 指定zookeeper地址 -->
<property>
<name>ha.zookeeper.quorum</name>
<value>xxx.10:12181,xxx:12181,xxx:12181/hdfs_ha01</value>
</property>

</configuration>
```

#### [hdfs-site.xml]

```xml
<configuration>
<!--指定hdfs的nameservice为ns1，需要和core-site.xml中的保持一致 -->
<property>
<name>dfs.nameservices</name>
<value>ns1</value>
</property>

<!-- ns1下面有两个NameNode，分别是nn1，nn2 -->
<property>
<name>dfs.ha.namenodes.ns1</name>
<value>nn1,nn2</value>
</property>

<!-- nn1的RPC通信地址 -->
<property>
<name>dfs.namenode.rpc-address.ns1.nn1</name>
<value>hdfs01:8020</value>
</property>
<!-- nn1的http通信地址 -->
<property>
<name>dfs.namenode.http-address.ns1.nn1</name>
<value>hdfs01:9870</value>
</property>
<!-- nn1的https通信地址 -->
<property>
<name>dfs.namenode.https-address.ns1.nn1</name>
<value>hdfs01:9871</value>
</property>

<!-- nn1的rpc通信主机 -->
<property>
<name>dfs.namenode.rpc-bind-host.ns1.nn1</name>
<value>hdfs01</value>
</property>
<!-- nn1的http通信主机 -->
<property>
<name>dfs.namenode.http-bind-host.ns1.nn1</name>
<value>hdfs01</value>
</property>
<!-- nn1的https通信主机 -->
<property>
<name>dfs.namenode.https-bind-host.ns1.nn1</name>
<value>hdfs01</value>
</property>
<!-- nn1的servicerpc通信主机 -->
<property>
<name>dfs.namenode.servicerpc-bind-host.ns1.nn1</name>
<value>hdfs01</value>
</property>

<!-- nn2的RPC通信地址 -->
<property>
<name>dfs.namenode.rpc-address.ns1.nn2</name>
<value>hdfs02:8020</value>
</property>
<!-- nn2的http通信地址 -->
<property>
<name>dfs.namenode.http-address.ns1.nn2</name>
<value>hdfs02:9870</value>
</property>
<!-- nn2的https通信地址 -->
<property>
<name>dfs.namenode.https-address.ns1.nn2</name>
<value>hdfs02:9871</value>
</property>
<!-- nn2的rpc通信主机 -->
<property>
<name>dfs.namenode.rpc-bind-host.ns1.nn2</name>
<value>hdfs02</value>
</property>
<!-- nn2的http通信主机 -->
<property>
<name>dfs.namenode.http-bind-host.ns1.nn2</name>
<value>hdfs02</value>
</property>
<!-- nn2的https通信主机 -->
<property>
<name>dfs.namenode.https-bind-host.ns1.nn2</name>
<value>hdfs02</value>
</property>
<!-- nn2的servicerpc通信主机 -->
<property>
<name>dfs.namenode.servicerpc-bind-host.ns1.nn2</name>
<value>hdfs02</value>
</property>

<!-- 指定NameNode的edits元数据在JournalNode上的存放位置 -->
<property>
<name>dfs.namenode.shared.edits.dir</name>
<value>qjournal://hdfs01:8485;hdfs02:8485;hdfs03:8485/ns1</value>
</property>

<!-- 开启NameNode失败自动切换 -->
<property>
<name>dfs.ha.automatic-failover.enabled</name>
<value>true</value>
</property>

<!-- 配置失败自动切换实现方式 -->
<property>
<name>dfs.client.failover.proxy.provider.ns1</name>
<value>org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider</value>
</property>

<!-- 配置隔离机制方法，多个机制用换行分割，即每个机制暂用一行-->
<property>
<name>dfs.ha.fencing.methods</name>
<value>sshfence</value>
</property>

<!-- 使用sshfence隔离机制时需要ssh免登陆 -->
<property>
<name>dfs.ha.fencing.ssh.private-key-files</name>
<value>/root/.ssh/id_rsa</value>
</property>

<!-- 配置sshfence隔离机制超时时间 -->
<property>
<name>dfs.ha.fencing.ssh.connect-timeout</name>
<value>30000</value>
</property>

<!-- 指定JournalNode在本地磁盘存放数据的位置 -->
<property>
<name>dfs.journalnode.edits.dir</name>
<value>/data/hadoop/hdata/jdata</value>
</property>

<!-- 配置journalnode rpc监听地址 -->
<property>
<name>dfs.journalnode.rpc-address</name>
<value>${local.bind.address}:8485</value>
</property>
<!-- 配置journalnode http监听地址 -->
<property>
<name>dfs.journalnode.http-address</name>
<value>${local.bind.address}:8480</value>
</property>
<!-- 配置journalnode https监听地址 -->
<property>
<name>dfs.journalnode.https-address</name>
<value>${local.bind.address}:8481</value>
</property>

<!-- 配置datanode HTTP服务端口 -->
<property>
<name>dfs.datanode.http.address</name>
<value>${local.bind.address}:9864</value>
</property>
<!-- 配置datanode HTTPS服务端口 -->
<property>
<name>dfs.datanode.https.address</name>
<value>${local.bind.address}:9865</value>
</property>
<!-- 配置datanode 控制端口 -->
<property>
<name>dfs.datanode.address</name>
<value>${local.bind.address}:9866</value>
</property>
<!-- 配置datanode RPC服务端口 -->
<property>
<name>dfs.datanode.ipc.address</name>
<value>${local.bind.address}:9867</value>
</property>

</configuration>
```

#### [workers]

```
# 编辑workers文件
vi workers
hdfs01
hdfs02
hdfs03
```

### [修改启动文件]

start-dfs.sh，stop-dfs.sh两个文件顶部添加以下参数

```
#!/usr/bin/env bash

HDFS_NAMENODE_USER=root
HDFS_DATANODE_USER=root
HDFS_SECONDARYNAMENODE_USER=root
HDFS_JOURNALNODE_USER=root
HDFS_ZKFC_USER=root
HADOOP_SECURE_DN_USER=hdfs
```

### [分发到其他机器]

### [启动过程]

#### [安装journalnode的机器均启动]

```
hdfs --daemon start journalnode
```

#### [第一台上（hdfs01）格式化namenode]

```
hdfs namenode -format
```

检查日志信息：successfully formatted. 这条日志出现在中间某处，不是最底部

#### [当前机器（hdfs01）启动namenode]

```
hdfs --daemon start namenode
```

查看日志，仅通过jps查看是不够的 日志路径：$HADOOP_HOME/logs/

#### [hdfs02 同步 hdfs01]

```
hdfs namenode -bootstrapStandby
```

#### [验证同步]

/data/hadoop/hdata/tmp 为配置文件中配置的dfs.journalnode.edits.dir路径

```
[root@hdfs01 hadoop]# cat /data/hadoop/hdata/tmp/dfs/name/current/VERSION

[root@hdfs02 hadoop]# cat /data/hadoop/hdata/tmp/dfs/name/current/VERSION
```

#### [zk格式化（在hdfs01节点做一次即可）]

```
hdfs zkfc -formatZK
```

此时可以使用zkCli检查节点情况

#### [启动Hadooop集群]

```
sbin/start-dfs.sh
```

只用在第一台执行即可，其余机器的程序会远程拉起来

### [验证]

#### [获取节点状态]
```
[root@hdfs01 ~]# hdfs haadmin -getServiceState nn1
active
[root@hdfs02 ~]# hdfs haadmin -getServiceState nn2
standby
```

#### [通过网页验证]

```
http://hdfs01:9870/

http://hdfs02:9870/
```