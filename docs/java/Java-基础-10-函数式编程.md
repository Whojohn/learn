# Java-基础-10-函数式编程

reference: 

1. https://github.com/CarpenterLee/JavaLambdaInternals
2. https://www.runoob.com/java/java8-new-features.html

## 1. 函数式编程

- 核心思想

  >  将流程和行为分开。其中，行为由用户定义(lambda 函数)，而流程则是使用 Java 内置的一系列 接口配合泛型、lambda 等实现的一种编程方式。



## 2. lambda 

### 2.1 Lambda 定义

 lambda 表达式跟匿名类类似，不同的是 lambda 只能`声明实现仅有一个抽象方法的接口`。匿名类则可以是接口、抽象类，也可以重写、实现多个接口。从编译角度来看，匿名类实际`编译时自动生成了类`，而 lambda 并没有生成类，而是通过编译时候生成`内部函数`实现。

   lambda 表达式无需声明函数传参类型，一切交由`编译器`（类型推断）实现。



语法:

> (parameters) -> expression 或 (parameters) ->{ statements; }
>
> ```
> // Lambda表达式的书写形式
> Runnable run = () -> System.out.println("Hello World");// 无参函数写法
> ActionListener listener = event -> System.out.println("button clicked");// 有参函数写法
> listener = (event) -> System.out.println("button clicked");// 有参函数另一种写法
> Runnable multiLine = () -> {
> System.out.print("Hello");
> System.out.println(" Hoolee");
> };// 函数代码块写法
> BinaryOperator<Long> add = (Long x, Long y) -> x + y;// 声明传入类型
> BinaryOperator<Long> addImplicit = (x, y) -> x + y;// 利用自动类型推断
> ```



示例：

```
// @FunctionalInterface 注解，相当于 @Overwrite 一样，用于检测接口是否符合 lambda 表达式规范
@FunctionalInterface
interface lambdaInterface {
    String getString(String input);
}

interface addLambdaInterface {
    Integer add(Integer add);
}


class Demo {
    public String getString(String input, lambdaInterface aL) {
        return aL.getString(input);
    }

    public Integer add(Integer input, addLambdaInterface aL) {
        return aL.add(input);
    }
}


public class LambdaTest {
    public static void main(String[] args) {
        // 注意，函数能够被传入 lambda ，传入 lambda 表达式的形参必须为接口，且接口只能包含一个抽象方法。
        System.out.println(new Demo().getString("Head", (head) -> head + "tail"));
        //  更简单的写法
        lambdaInterface li = (head) -> head + "tail";
        System.out.println(li.getString("head"));

        // lambda 使用局部变量，且变量在 lambda 后有改变者，将会报错
        // 如下，移除 num=5 即可正常
        //http://cr.openjdk.java.net/~briangoetz/lambda/lambda-state-final.html jvm 特性
//        int num = 1;
//        System.out.println(new Demo().getString("Head", (head) -> head + num));
//        num = 5;

    }
}
```

### 2.2 常用 Lambda 接口

reference:

1.  https://cloud.tencent.com/developer/article/1488128

#### 2.2.1 Consumer

> 一个没有返回的操作。如：打印元素。

```
        List<Integer> temp = new ArrayList<Integer>() {{
            add(1);
            add(2);
        }};
        System.out.println("Consumer 接口");
        Consumer consumer = System.out::println;
        temp.forEach(consumer);
        
Consumer 接口
1
2       
```

#### 2.2.2 Supplier

> 返回一个值的方法，一般用于返回常量，随机值等。

```
        Supplier<Double> supplier = Math::random;
        System.out.println(supplier.get());
```

#### 2.2.3 Predicate

> 相当于一个if 判定，返回 boolean 值。

```
        Predicate<Integer> predicate = (t) -> t > 5;
        System.out.println(predicate.test(1));
```

#### 2.2.4 Function

> 将输入值转换为另一个的输出值，输出输出类型可以不一致。

 ```
        Function<Integer, String> function = (input) -> {
            return input.toString();
        };
        System.out.println(function.apply(new Integer(2)));
 ```

## 3. 方法引用 (Method References)

> 方法引用只是更加便利的使 lambda 表达式，直接传入调用方法而已。

- 语法:

> :: 表示引用
>
> 引用允许使用范围：
>
> 1. 构造器 
> 2. 静态方法
> 3. 特定类的任意对象的方法引用
> 4. 特定对象的方法引用

- 样例

```
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

class Car {
    //Supplier是jdk1.8的接口，这里使用只是为了演示 lambda 利用方法引用传入构造器的例子
    public static Car create(Supplier<Car> supplier) {
        return supplier.get();
    }

    public static void collide(Car car) {
        System.out.println("Collided " + car.toString());
    }

    public void follow(Car another) {
        System.out.println("Following the " + another.toString());
    }

    public void repair() {
        System.out.println("Repaired " + this.toString());
    }
}


public class LambdaTest {
    public static void main(String[] args) {
        // 两种写法相同，方法引用 写法只是语法糖而已，会看即可
        Car car = Car.create(() -> new Car());
        List<Car> cars = Arrays.asList(car);
        Car car2 = Car.create(Car::new);
        List<Car> cars2 = Arrays.asList(car2);
        // 静态方法引用
        cars.forEach(Car::collide);
        // 特定类的任意对象的方法引用
        cars.forEach(Car::repair);
        // 特定对象的方法引用
        Car police = Car.create(Car::new);
        cars.forEach(police::follow);
    }
}
```



## 4. Lambda & Collections & Map

reference:

1. https://github.com/CarpenterLee/JavaLambdaInternals/blob/master/3-Lambda%20and%20Collections.md



> Java 中 Collection 接口有三个实现：Set List Queue;  Map 有两个实现： SorteddMap 和 ConcurrentMap实现;



### 4.1 Collections & Map 接口方法

| 接口名     | Java8新加入的方法                                            |
| ---------- | ------------------------------------------------------------ |
| Collection | removeIf()  spliterator() stream() parallelStream() forEach() |
| List       | replaceAll() sort()                                          |
| Map        | getOrDefault() forEach()  replaceAll() putIfAbsent() remove() replace() computeIfAbsent() computeIfPresent() compute() merge() |

#### 4.1.2 Collection 中的方法

- foreach  

> 实现 Lambda Consumer 接口，执行一个没有返回的操作。如：打印对象。

- removeIf

> 实现了Lambda Predicate 接口，移除满足条件的元素。

- replaceAll()

> 执行一个操作，替换每一个元素。如：*假设有一个字符串列表，将其中所有长度大于3的元素转换成大写，其余元素不变。*

- sort()

> 实现Lambda Comparator接口，用于排序。

- spliterator()

> 一般不会直接使用，parallelStream  的底层实现方式。

- stream() & parallelStream()

> 返回一个 stream 流， parallelStream 是一个不保证安全多线程操作的流。

- 代码样例

```
List<Integer> temp = new ArrayList<Integer>() {{
	add(1);
	add(2);
	add(3);
}};
temp.forEach(System.out::println);
temp.parallelStream().forEach(System.out::println);
temp.removeIf(a -> a > 2);
temp.forEach(System.out::println);
```



#### 4.1.2 Map 中的方法

- forEach

> 遍历 kv 值

- getOrDefault

> 通过 key 获取 value 。假如Key 不存在返回一个默认值(普通get 不存在返回为null)。

- putIfAbsent

> key不存在`或` **value**为空时放入将k,v 值放入 Map 中。

- remove

> remove(Object key, Object value)，只有 kv 等于输入值才移除元素。

- replace

> 与 put 不同的是， replace 必须包含 key 值才执行操作。
>
> `replace(K key, V value)`，只有在当前`Map`中**`key`的映射存在时**才用`value`去替换原来的值，否则什么也不做．
>
> replace(K key, V oldValue, V newValue)`，只有在当前`Map`中**`key`的映射存在且等于`oldValue`时**才用`newValue`去替换原来的值，否则什么也不做．

- replaceAll

> 遍历所有的值，用户改变对应的value

- merge

> 找到对应key，假如value 不存在或者为null 用用户定义的value 替换。假如value 不为空，传入用户方法，用用户方法返回的值进行替换。用户方法方法的值null 则删除key、value，否则更新value。

- compute

> 找到key对应的value 执行用户更新value操作，假如用户函数返回 null 删除 value。

- computeIfAbsent

> 只有在当前`Map`中**不存在`key`值的映射或映射值为`null`时**，才调用`mappingFunction`，并在`mappingFunction`执行结果非`null`时，将结果跟`key`关联．

- computeIfPresent

> 与computeIfAbsent 相反的方法，`Map`中**存在`key`值的映射且非`null`时**才调用方法。

- 代码

```
        Map<String, Integer> temp = new HashMap<String, Integer>() {{
            put("1", 2);
            put("2", 2);
        }};
        System.out.println("foreach method");
        temp.forEach((k, v) -> System.out.println("key=" + k + " values=" + v));
        System.out.println("get method");
        System.out.println(temp.get(3));
        System.out.println(temp.getOrDefault("3", 0));
        System.out.println("remove method");
        temp.remove("1", 3);
        temp.forEach((k, v) -> System.out.println("key=" + k + " values=" + v));
        System.out.println("merge method: key not exists add k,v");
        temp.merge("3", 3, (v1, v2) -> v1 + v2);
        temp.forEach((k, v) -> System.out.println("key=" + k + " values=" + v));
        System.out.println("merge method: key exists add v by k");
        temp.merge("3", 3, (oldValue, newValue) -> oldValue + newValue);
        temp.forEach((k, v) -> System.out.println("key=" + k + " values=" + v));
        System.out.println("merge method: key exists user method return null delete value");
        temp.merge("3", 3, (oldValue, newValue) -> null);
        temp.forEach((k, v) -> System.out.println("key=" + k + " values=" + v));
        System.out.println("compute method");
        temp.compute("2", (k, v) -> v);
        temp.forEach((k, v) -> System.out.println("key=" + k + " values=" + v));
        System.out.println("computeIfAbsent");
        temp.computeIfAbsent("3", v->3);
        temp.forEach((k, v) -> System.out.println("key=" + k + " values=" + v));
        
        
        
foreach method
key=1 values=2
key=2 values=2
get method
null
0
remove method
key=1 values=2
key=2 values=2
merge method: key not exists add k,v
key=1 values=2
key=2 values=2
key=3 values=3
merge method: key exists add v by k
key=1 values=2
key=2 values=2
key=3 values=6
merge method: key exists user method return null delete value
key=1 values=2
key=2 values=2
compute method
key=1 values=2
key=2 values=2
computeIfAbsent
key=1 values=2
key=2 values=2
key=3 values=3

Process finished with exit code 0

```

## 4.2 Stream

Stream有两大类。他们都继承于 BaseStream

1. 包装类：IntStream, LongStream, DoubleStream。
2. 容器类：Stream (包括实现 Collection 和 Map 接口的 stream实现， 他们通过Collection.stream() , Map.stream() 获得 Stream)。

*stream*和*collections*有以下不同：

- **无存储**。*stream*不是一种数据结构，它只是某种数据源的一个视图，数据源可以是一个数组，Java容器或I/O channel等。
- **为函数式编程而生，一般无法更改数据源**。对*stream*的任何修改都不会修改背后的数据源，比如对*stream*执行过滤操作并不会删除被过滤的元素，而是会产生一个不包含被过滤元素的新*stream*。
- **惰式执行**。*stream*上的操作并不会立即执行，只有等到用户真正需要结果的时候才会执行。
- **可消费性**。*stream*只能被“消费”一次，一旦遍历过就会失效，就像容器的迭代器那样，想要再次遍历必须重新生成。  

对*stream*的操作分为为两类，**中间操作(\*intermediate operations\*)和结束操作(\*terminal operations\*)**，二者特点是：

1. **中间操作总是会惰式执行**。调用中间操作只会生成一个标记了该操作的新*stream*，仅此而已。
2. **结束操作会触发实际计算**。计算发生时会把所有中间操作积攒的操作以*pipeline*的方式执行，这样可以减少迭代次数。计算完成之后*stream*就会失效。

| 操作类型 | 接口方法                                                     |
| -------- | ------------------------------------------------------------ |
| 中间操作 | concat() distinct() filter() flatMap() limit() map() peek() skip() sorted() parallel() sequential() unordered() |
| 结束操作 | allMatch() anyMatch() collect() count() findAny() findFirst() forEach() forEachOrdered() max() min() noneMatch() reduce() toArray() |

  

  ### 4.2.1 Stream 方法

Stream 方法通过 lambda 实现了函数式编程。

- concat (流合并)

```
        Stream<String> streamWord = Stream.of("I", "love", "love", "you", "too");
        Stream<String> stringToken = Stream.of("I", "hate", "you");
        // 
        Stream<String> concatStream = Stream.concat(streamWord, stringToken);
        concatStream.forEach(System.out::println);

I
love
love
you
too
I
hate
you
```

- flatMap （每个元素输出为多个元素）

> 如：
>
> 1. ((a,b),(c,d,e)) 变为 (a,b,c,d,e)
> 2. ("i like you.", "do you like me") 变为 ("i", "like", "you", "do", "you", "like", "me")

```
        Stream<String> streamWord = Stream.of("I maybe", "love you dog and", "love", "you", "too");
        streamWord.flatMap(a-> Arrays.stream(a.split(" "))).forEach(System.out::println);
        String[] temp = new String[] {"foo", "bar"};
        Stream<List<String>> arrayStream = Stream.of(Arrays.asList("1","2"), Arrays.asList("3","4"));
        arrayStream.flatMap(a-> a.stream()).forEach(System.out::println);
        
I
maybe
love
you
dog
and
love
you
too
1
2
3
4
```

- filter（过滤）

```
        Stream<String> streamWord = Stream.of("I maybe", "love you dog and", "love", "you", "too");
        streamWord.flatMap(a -> Arrays.stream(a.split(" "))).forEach(System.out::println);
        streamWord.filter(a -> a.equals("love")).forEach(System.out::println);
```



- peek（类似 foreach 作用，一般用于debug）

> 类似 foreach 作用，一般用于debug， 与foreach 相比并不会终止运算

```
        Stream<String> streamWord = Stream.of("I maybe", "love you dog and", "love", "you", "too");
        streamWord.flatMap(a -> Arrays.stream(a.split(" ")))
                .filter(a -> !a.equals("love"))
                .peek(System.out::println)
                .filter(a -> !a.equals("you"))
                .forEach(System.out::println);
                
I
I
maybe
maybe
you
dog
dog
and
and
you
too
too
```



- distinct （去重）

```
        Stream<String> streamWord = Stream.of("I maybe", "love you dog and", "love", "you", "too");
        streamWord.flatMap(a -> Arrays.stream(a.split(" ")))
                .distinct().forEach(System.out::println);
                
I
maybe
love
you
dog
and
too                
```

- sort （排序）
- anyMath(任意一个满足)
- allMatch(所有都满足)

- reduce（合并操作）

> reduce 有三种写法：
>
> // 单纯计算方式，输入输出类型必须一致
>
> - `Optional<T> reduce(BinaryOperator<T> accumulator)`
>
> //  传入一个别的类型的默认值，输入，输出类型可以不一致
>
> - `T reduce(T identity, BinaryOperator<T> accumulator)`
>
> // 并发执行，必须传入第三个方法，以支持 stream 流结果合并
>
> - `<U> U reduce(U identity, BiFunction<U,? super T,U> accumulator, BinaryOperator<U> combiner)`

```
        Stream<String> streamWord = Stream.of("I maybe", "love you dog and", "love", "you", "too");
        System.out.println(streamWord.flatMap(a -> Arrays.stream(a.split(" ")))
                .reduce((a, b) -> a + b).get());
                
Imaybeloveyoudogandloveyoutoo
```

- collect 

> collect 作用：
>
> 1. collect  能够将 stream 转化为特定数据类型如： set，list， map。
>
>        2. group by  ， partitioningBy 分流。

collect 常用 Collectors 方法：

1. Collectors.toMap()  Collectors.toList() Collectors.toSet()
2. Collectors.partitioningBy()
3. Collectors.groupingBy()

```
        Stream<String> streamWord = Stream.of("I I", "love you dog and", "and", "and", "and");
        streamWord.flatMap(a -> Arrays.stream(a.split(" ")))
                .collect(Collectors.partitioningBy(a -> a.length() > 2))
                .forEach((a, b) -> System.out.println(a.toString() + b));
```



## 4.3 Stream 原理

详细原理见：

https://github.com/CarpenterLee/JavaLambdaInternals/blob/master/6-Stream%20Pipelines.md

| Stream操作分类                    |                                                         |                                                              |
| --------------------------------- | ------------------------------------------------------- | ------------------------------------------------------------ |
| 中间操作(Intermediate operations) | 无状态(Stateless)                                       | unordered() filter() map() mapToInt() mapToLong() mapToDouble() flatMap() flatMapToInt() flatMapToLong() flatMapToDouble() peek() |
| 有状态(Stateful)                  | distinct() sorted() sorted() limit() skip()             |                                                              |
| 结束操作(Terminal operations)     | 非短路操作                                              | forEach() forEachOrdered() toArray() reduce() collect() max() min() count() |
| 短路操作(short-circuiting)        | anyMatch() allMatch() noneMatch() findFirst() findAny() |                                                              |

如上图所示，Stream 通过把操作类型进行分类，利用了流水线技术将每一个中间步骤串联起来，结束操作相当于函数的return 操作。

