# 网络优化&shuffle 策略改善

> reference :
>
> https://flink.apache.org/2019/06/05/a-deep-dive-into-flinks-network-stack/
>
> https://github.com/Whojohn/learn/blob/master/flinklearn/docs/Flink%20%E6%95%B0%E6%8D%AE%E4%BA%A4%E6%8D%A2(%E7%BD%91%E7%BB%9C).md

网络相关的底层已经在`Flink 数据交换(网络)描述`的很清楚

## 网络可能存在的问题

### Buffer 相关问题

- Buffer /并行度过大，导致背压时候很难消除 （原因见flink 网络）, 考虑调小 segment 或者并发度

- 并行度过大， env.setBufferTimeout 应该调整，否则网络开销过大

- Buffer segment 过小，数据单条过大，会影响吞吐，考虑加大 segment


### LocalInputChannel 优化不能使用

-  超过一个并行度，一个slot 一个 tm ，存在网络消耗；默认情况下，对于同一个tm subtask 之间数据交互有  LocalInputChannel,  tm 上面 slot 过少，会导致数据都是走 RemoteInputChannel （网络交换），增加消化。建议增大单个tm 的 slot 个数

### shufflt 缺陷

> https://www.liaojiayi.com/flink-network-stack-opt/

- Flink  shuffle  策略的问题

    1. RESCALE , SHUFFLE, REBALANCE 都无法考虑到下游消费能力，会在部分场景下存在问题。

    - Shuffle 缺陷的影响场景（etl 来说，比较重要）

    1. Sink 来说：以写入到 `HDFS` 来说，当前`sink`某个 `block` 慢，导致整体任务反压。
    2. 中间处理operator  来说: 每条数据可能长度可能不均匀，导致下游可能触发反应。比如：新闻文章长度不唯一，shuffle 到下游分词， 1kb 比 16kb 处理快。



#### flink shuffle 增强（整体原理见 `Flink 数据交换(网络)描述`）


> 1. 核心思想利用定期采集 backlog 信息，知道下游哪一个处理速度更快，然后定期改变 shuffle 到下游的流量比例
>
> 2. 注意：这个代码修改方式基于 1.13 ，1.14后有通用异步线程。或者可以考虑，复用动态 network buffer 那一部分的metric 。(原生计算方式比较复杂)
> 3. 具体代码见 Flink-learn#flink-shim-1.13#org.apache.flink 部分

**ChannelSelectorRecordWriter**

  ```
 
 public void emit(T record) throws IOException {
        int targetChannel = channelSelector.selectChannel(record);
        # 假如使用的是优化后的逻辑，获取backlog 数据
        if (targetPartition instanceof PipelinedResultPartition && channelSelector instanceof OptimizeShufflePartitioner) {
            ((OptimizeShufflePartitioner) channelSelector).setBufferStatista(((PipelinedResultPartition) targetPartition).getBufferStatista());
        }
        
        emit(record, targetChannel);
    }
  ```

**PipelinedResultPartition**

  ```
# 1.13 没有公用的异步线程，1.14 streamtask 中引入了异步线程   
   private boolean iniAsyncBackground = true;
    private Thread asyncThread;
    private boolean flushStatista;
    private Map<Integer, Integer> bufferStatista;
    private final static int FLUSH_TIME = 2000;
    private final static int ACTIVE_FLUSH_TASK_BACK_PRESSURED_TIME = 300;


 public void close() {
        decrementNumberOfUsers(PIPELINED_RESULT_PARTITION_ITSELF);
        super.close();
        iniAsyncBackground = false;
        asyncThread.interrupt();
    }

## 定期采集各个channel backlog 数据
    public void iniEnv(int numberOfChannels) {
        bufferStatista = new ConcurrentHashMap<>(numberOfChannels);
        asyncThread = new Thread(() -> {
            try {
                Long checkStartTime = System.currentTimeMillis();
                while (iniAsyncBackground) {
                    if (System.currentTimeMillis() - checkStartTime > FLUSH_TIME) {
                        bufferStatista.clear();
                        checkStartTime = System.currentTimeMillis();
                    }
                    IntStream.range(0, numberOfChannels)
                            .forEach(e ->
                                    bufferStatista.put(e,
                                            bufferStatista.getOrDefault(e, 0)
                                                    + Math.max(subpartitions[e].getBuffersInBacklog(), 1)));
                    if (getBackPressuredTimeMsPerSecond().getValue() > ACTIVE_FLUSH_TASK_BACK_PRESSURED_TIME) {
                        flushStatista = true;
                    }
                    Thread.sleep(ACTIVE_FLUSH_TASK_BACK_PRESSURED_TIME);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        asyncThread.start();
    }

    public Map<Integer, Integer> getBufferStatista() {
        # 这里可以再优化下，提前把这个变量创建好，而不是每一次都新建一个副本
        if (flushStatista) {
            return new HashMap<>(bufferStatista);
        }
        return null;
    }
  ```

**OptimizeShufflePartitioner**

  ```
# 根据堵塞情况，改变发送给下游的数据   
   private class RandomCollection implements Serializable {
        private transient NavigableMap<Integer, Integer> map;
        private Random random = new Random();
        private int weightTotal = 0;
        private int total = 0;

        public RandomCollection() {
            map = new TreeMap<>();
            random = new Random();
        }

        public RandomCollection add(Map<Integer, Integer> source) {
            if (map == null) {
                map = new TreeMap<>();
            }
            total = source.values().stream().reduce(Integer::sum).orElse(1);
            source.forEach((k, v) -> addWeight(total - v, k));
            return this;
        }

        private void addWeight(double weight, Integer result) {
            if (weight <= 0) return;
            weightTotal += weight;
            map.put(weightTotal, result);
        }

        public Integer next() {
            if (total == 0) {
                return random.nextInt(numberOfChannels);
            }
            int value = (int) (random.nextDouble() * weightTotal);
            return map.higherEntry(value).getValue();
        }
    }

  ```

  

  

  

  

  

  