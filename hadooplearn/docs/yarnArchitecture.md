# Yarn-基础架构

       Yarn 是基于 `事件驱动模型` 与 `状态机` 的**容器服务**。它将资源以容器(轻量容器，配合cgroup)的方式提供服务.核心包括两大部分：资源管理、作业控制。`ResourceManger` 和`NodeManger`组件共同组成了`yarn`，内部通信是通过 `RPC Server Client` 完成。`ResourceManager` 有两个主要组件：`Scheduler` 和 `ApplicationsManager`  。前者只负责调度，后者负责作业的监控，运行，注意 ``ApplicationsManager` 运行在`NodeManger`的`container(资源的抽象实体)`中。

> **RPC :  RPC 分为：Client Server 端。 Client 是调用方， Server 是被调用方。**

Hadoop 中 RPC 流向如下图（箭头指向server 端）

![Hadoop 中 RPC 流向如下图（箭头指向server 端）](https://github.com/Whojohn/learn/blob/master/hadooplearn/docs/pic/yarnRpc.png?raw=true)



![Yarn 架构](https://github.com/Whojohn/learn/blob/master/hadooplearn/docs/pic/yarnArchitecture.gif?raw=true)


![ResourceManger 架构](https://github.com/Whojohn/learn/blob/master/hadooplearn/docs/pic/ResourceManager.svg?raw=true)

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



## 2 Yarn 资源分配

> reference:
>
> Hadoop技术内幕：深入解析YARN架构设计与实现原理

### 2.1 调度器

### 2.1.1 Fifo

### **官网没有文档，但是代码还有，并且官网Scheduler Load Simulator 还是有该调度器选择**

先来先服务，并且队列中的任务带有优先级，高优先级的先执行，**该调度器不支持子队列**。

### 2.1.2 Capacity

    支持多队列，每个队列自身有一定总体资源保障。单任务资源的分配按照队列的最大，最小值直接分配，队列内的任务支持优先级进行执行(FIFO)。

     **Capacity 只支持 Fair(最大最小公平算法实现) 和 FiFO 两种调度策略算法，具体算法实现见 2.1.3.1 调度策略算法。**

### 2.1.3 Fair （主流使用的队列）

   与`Capacity ` 相比，队列中按照用户平分资源，如2个用户，各自占用 50% 队列资源，3个用户, 33% 的资源平分。Fair 的子队列还能配置：“fifo”，“fair”，“drf” 三种调度策略，以进一步实现队列的细粒度控制。

#### 2.1.3.1 调度策略算法

-  Fair 实现算法(最大最小公平算法 max-min fairness)

> reference :
>
> https://en.wikipedia.org/wiki/Max-min_fairness
>
> https://yoelee.github.io/2018/02/24/Yarn%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%904-%E8%B5%84%E6%BA%90%E8%B0%83%E5%BA%A6%E7%AE%97%E6%B3%95/

优点：

1. 较`Fifo`提高吞吐

缺点:

1. **最大最小公平算法的缺点是只能针对单一资源进行划分**

#### 1. max-min fairness

      有一个资源，其容量为C(capacity) 为 `100`  ，欲分配给n 为 `5`个人，其中他们对于资源的需求：(10,20,40,50,80)。

- 分配流程如下：

  将所有用户对于资源的需求按照升序排列，将剩余容量除以待分配人数，容量不能满足最小资源所需则平分所有的剩下容量，容量有空余，则累加后循环进行分配。即：

**样例1**

```
  有一个资源，其容量为C(capacity) 为 100  ，欲分配给n 为 5个人，其中他们对于资源的需求：(10,20,40,50,80)。

 第一步：

 100 / 5 =20； 1，2 满足，并且1剩余10。

 第二步:

 70/(5-2) = 23.3 ，bu连最小需要的容量都不满足，则平分所有容量。

 最终分配结果：

 (10, 20, 20.3, 20.3,20.3)
```

**样例2**

```
  有一个资源，其容量为C(capacity) 为 10  ，欲分配给n 为 4个人，其中他们对于资源的需求：(2,2.6,4,5)。

 第一步：
 10/4 = 2.5， 满足第一位，剩余0.5。
 第二步:
 8 / 3 = 2.6, 满足第二位，剩余0。
 第三步：
 5.4 / 2 = 2. 7 bu连最小需要的容量都不满足，则平分所有容量。
 最终分配结果：
 (2, 2.6 , 2.7, 2.7)

```

#### 2.  加权 max-min fairness

     按照权重正规化计算可获得资源值，然后按照原算法计算。正规化为： 权重总和=1， 归一化权重后可获得资源表示为：总资源值*(权重/总权重);

**样例1**

```
  有一个资源，其容量为C(capacity) 为 100  ，欲分配给n 为 4个人，其中他们对于资源的需求：(20, 50, 10, 60), 权重为 (0.4, 0.3, 0.2, 0.1)。

 第一步：
 权重归一化： 0.4+0.3+0.2+0.1 = 1； 可获得资源为：(100*(0.4/1) , 100*(0.3/1), 100*(0.2/1), 100*(0.1/1)) = (40， 30， 20， 10) ; 1，3满足，空余70总容量。
 第二步：
 权重归一化：  0.3+ 0.1 = 0.4 ；可获得资源 (70*(0.3/0.4), 70*(0.1/0.4)) = (52.5,17.5 ); 2 满足， 4剩余20资源。
 最终分配结果为：
 (20, 50, 10, 20)
```

**样例2**

```
  有一个资源，其容量为C(capacity) 为 10  ，欲分配给n 为 4个人，其中他们对于资源的需求：(4,2,10,4),权重为 (2.5,4,0.5,1)。

  第一步：
  权重归一化: 2.5+4+0.5+1 = 8 ；可获得资源为： (10*(2.5/8), 10*(4/8), 10*(0.5/8), 10*(1/8)) = (3.125, 5, 0.625, 1.25); 2 满足，空余总容量8。
  第二步:
  权重归一化： 2.5+0.5+1 = 7； 可获得资源为： (8*(2.5/7),0, 8*(0.5/7), 8*(1/7)) = （2.85， 0， 0.5， 1.14）; 1 满足，空余容量为4；
  第三步：
  权重归一化:  0.5+1 ;可获得资源： (4*(0.5/1)，4*(1/1.5)) = （0，0，2.64，1.36 ）；
  最终分配结果为：
  (4,2,2.64,1.36)


```

- DRF 算法实现

> reference:
>
> drf论文：  https://github.com/Whojohn/learn/blob/master/hadooplearn/docs/nsdi_drf.pdf

- 优点：

1. DRF 算法与 最大最小公平算法相比支持多维资源的限制。


    有多个资源总量为：(c1, c2, c3,c4)，有n个用户，他们的资源需求为(r11,r12,…,r1m),(r21,r22,…,r2m),…,(rn1,rn2,…,rnm)。

  - 主资源  : 各个所需资源占该类总资源的比例的最大值。单个用户请求按照主资源进行分配。

  - share 值： 所有用户分得的主资源累积值占其维度资源总量的百分比。

    分配流程：每一个从share 中取出最小值，然后选举出用户最小的主资源值，累加所有 share 值，一直重复分配操作。

  **样例1**

> ！！！注意！！！起始时，share 都为零，可以任选一项作为起始。论文中选 主share 最大占用作为起始。

  ```
总资源: (9c, 18G), 用户a需要 (1c, 4G), b需要 (3c, 1G)

流程：
a 主资源 memory : 4/18 , b 主资源 3/9；
分配流程如下：
  ```

|      | resource.use | mem.share.now | cpu.share.now |
| ---- | ------------ | ------------- | ------------- |
| a    | （1，4）     | 4/18          | 1/9           |
| b    | （4，5）     | 5/18          | 4/9           |
| a    | （5，9）     | 9/18          | 5/9           |
| a    | （6，13）    | 13/18         | 6/9           |
| b    | (9,14)       | 14/18         | 9/9           |