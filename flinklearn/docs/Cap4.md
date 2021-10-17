# Flink-4-数据类型和序列化

`Flink` 针对不同的数据类型采取不同序列化策略，以节省内存、网络的开销。`Flink`序列化应用于网络数据交换和内存管理中，其中**内存管理是应用在`Batch`模式**下的`MemoryManager`中用于处理`sort`，`join`，`shuffle`等操作。数据类型的声明必须在`execute()`, `print()`, `count()`, or `collect()` 方法调用前确定。


- Flink 序列化调用图
![stream序列化调用图](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/cap4-howSerialWorkInFlinkMethod.png?raw=true)

- Flink batch模式下数据类型与内存管理关系图
![Flink batch模式下数据类型与内存管理关系图](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/cap4-typeWorkWithSerialInMemory.png?raw=true)


## 1.1 Flink 数据类型

### 1.1.1 Flink 支持数据类型：

1. 基础数据类型：`Java` 基础数据类型及包装类，`String`，`Date`，`BigDecimal`，`BigInteger`。
2. 数组：`Java`基础数据类型的数组和Object数组
3. 复杂类型：

> Flink java tuple 类：最大长度为25，不支持为空对象
>
> Scala case calss: 不支持空对象
>
> Flink Row: 支持为空，不限长度，可以下标+名字的方式访问变量
>
> POJOs: 符合 pojo 的类

4. 辅助类型：`option`，`Either`，`Lists`，`Maps`，`Value`， `Hadoop Writables`对象...
5. Generic type:  非`Flink`框架序列化，只能由 `Kyro`序列化。
6. **不支持数据类型**: 文件对象，`i/o stream` ，native 对象，`guava`对象等，这些 `kyro`序列化也会引发异常。

### 1.1.2 Pojo

**Pojo 定义：**

1. 该类必须用`public`修饰，且必须有一个`public`的无参数的构造函数。
2. 类属性**不能含有**`static`修饰的变量。属性要么`public`修饰 ，要么带有`getxxx`，`setxxx` 变量读写方法。
3. 所有子字段也必须是Flink支持的数据类型。

**如何检测是否为 Pojo **

```
// 通过序列方法得知是否为pojo
System.out.println(TypeInformation.of(xxx.class).createSerializer(new ExecutionConfig()));
// 详细信息
System.out.println(TypeInformation.of(new TypeHint<A>() {});)
```

### 1.2 数据类型声明

>        `Flink`中数据类型的以`TypeInformation`及其子类表达，由于 Java 泛型擦除的存在，会导致 `Flink` 无法正确捕获`operator` 输入/输出的数据类型，虽然默认情况下`Flink`会分析出数据类型(利用`TypeExtractror`）。但是更多时候是无法获取，这时候则必须通过`.return`在`stream`声明所用的数据类型，以帮助 `Flink` 序列化。

- 如何确保数据类型不存在`Generic type`

```
// 禁止 gereric type 出现
env.getConfig().disableGenericTypes();
// 各个stream 环境查看数据类型，用于定位 gereric type 出现的算子
getRuntimeContext().getExecutionConfig()
stream.getExecutionConfig()
stream.getType()
```

- .return 声明数据类型场景

```
泛型数据类型使用：map， list 
row使用
```

#### 1.2.1 Flink 数据类型获取方法

> 注意，数据类型的获取也不一定能够解决泛型擦除：如: **`java map`，`java list`，`Row`**等，无法解决的类型的的数据类型声明必须走手动声明，不然会当作`Generic type`处理。

Flink 数据类型的获取有以下方法(`TypeInformation`类获取)：

1. **TypeExtractror** : 默认 `Stream` 间`operator` 提取输出数据类型的类。

```
TypeExtractror.getForClass(ObjectNode.class)
//stream 中 map 算子调用的是
TypeExtractror.getMapReturnTypes...
```

2. 借助`TypeHint` **！！！注意！！！，type hint 获取的类型也不一定对，如：map，list，必须手动声明，不然会被识别为`Generic type`**

```
TypeInformation.of(new TypeHint<Tuple2<String, Double>>(){});
```

#### 1.2.2. 手动声明数据类型

1. 借助 `Types` 类实现

```
Types.MAP(Types.STRING,Types.STRING)
Types.ROW(Types.STRING,Types.LIST(Types.STRING), Types.MAP(Types.STRING,Types.INT))
```

2. 实现 `typeinfo.TypeInfoFactory` 接口
    - 使用方法

```
@TypeInfo(MyTupleTypeInfoFactory.class)
public class MyTuple<T0, T1> {
  public T0 myfield0;
  public T1 myfield1;
}

public class MyPojo {
  public int id;

  @TypeInfo(MyTupleTypeInfoFactory.class)
  public MyTuple<Integer, String> tuple;
}
```

- 实现 `typeinfo.TypeInfoFactory` 接口

```
public class MyTupleTypeInfoFactory extends TypeInfoFactory<MyTuple> {

  @Override
  public TypeInformation<MyTuple> createTypeInfo(Type t, Map<String, TypeInformation<?>> genericParameters) {
    return new MyTupleTypeInfo(genericParameters.get("T0"), genericParameters.get("T1"));
  }
}
```



### 2. 数据类型序列化

### 2.1 TypeInformation 声明自定义序列化方式

```
TypeInformation.createSerializer()
```

### 2.2 第三方序列化方法引入

> 用于 guava 等外部包，无法序列化的场合，一般第三方包自行实现了序列化的实现，可以通过引入解决序列化。

```
env.getConfig().registerTypeWithKryoSerializer(MyCustomType.class, MyCustomSerializer.class)
```











