# Yarn-基础架构

       Yarn 是基于 `事件驱动模型` 与 `状态机` 的**容器服务**。它将资源以容器(轻量容器，配合cgroup)的方式提供服务.核心包括两大部分：资源管理、作业控制。`ResourceManger` 和`NodeManger`组件共同组成了`yarn`，内部通信是通过 `RPC Server Client` 完成。`ResourceManager` 有两个主要组件：`Scheduler` 和 `ApplicationsManager`  。前者只负责调度，后者负责作业的监控，运行，注意 ``ApplicationsManager` 运行在`NodeManger`的`container(资源的抽象实体)`中。

> **RPC :  RPC 分为：Client Server 端。 Client 是调用方， Server 是被调用方。**

Hadoop 中 RPC 流向如下图（箭头指向server 端）

![Hadoop 中 RPC 流向如下图（箭头指向server 端）](https://github.com/Whojohn/learn/blob/master/hdfslearn/docs/pic/yarnRpc.png?raw=true)



![Yarn架构](https://github.com/Whojohn/learn/blob/master/hdfslearn/docs/pic/yarnArchitecture.gif?raw=true)

## 1. Yarn 工作流程

> reference:
>
> https://hadoop.apache.org/docs/r3.1.4/hadoop-yarn/hadoop-yarn-site/WritingYarnApplications.html

## 1.1 编写Yarn 程序

### 1.1.1 `Client` 与 `ResourceManager` 交互

1. `CLient` 新建 `YarnClient`对象，并且初始化任务。
2. `ResourceManager`返回任务`id`，队列状态，以及可用资源等资源信息。
3. `CLine` 新建`ApplicationSubmissionContext` 对象，并且定义 `AM` 启动的任务名字，环境变量，任务依赖，启动 shell 脚本，执行的启动命令，`AM` 所需的`内存`和`vcore`，运行队列，优先级，并且绑定对象与刚刚接收到的 `ResourceManager` 返回的任务`id` 作为任务的唯一标识。
4. 此时，RM 将接受申请，按照队列分配规则分配所需资源，在分配的容器上设置和启动 AM。AM后续流程见 ：`AM` 与 `ResourceManger 交互`部分。
5. `YarnCLient` 可以通过与`ResourceManager`交互知道任务的进度，`AM`的信息等总体信息。详细信息可以通过与`AM`交互进行获取(需要 AM 启动的任务具有交互能力)。

### 1.1.2 `AM` 与 `ResourceManger 交互`

> AM 负责作业控制，定期与 RM 保持心跳，告知任务状态。`ResourceManger`只负责与`Client`,`Am`交互。 AM 启动成功后，他负责上报任务相关的总体信息给 `ResourceManger`，自身任务的协调：如 MR 、Spark、Flink 启动相应框架，完成任务。

1. 初始化 `AM` 启动完成后，`AM` 可以向 `ResourceManager` 申请任务具体所需的资源。`AM` 类比 `Client` 与 `ResourceManger`的交互，都需要声明环境，配置，脚本，执行依赖等。
2. `AM` 初始化作业 `Container` 后，负责控制这些运行在`NodeManger`的`Container`，处理任务的异常，重试等任务控制流程。

