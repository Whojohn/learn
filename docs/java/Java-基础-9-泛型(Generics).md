# Java-基础-9-泛型(*Generics*)

reference:

1. https://segmentfault.com/a/1190000005179142
2. https://segmentfault.com/a/1190000005179147
3. https://segmentfault.com/a/1190000005337789



## 1. 泛型特点

### 1.1 泛型 Vs Object

> 1. Object 替代泛型，会放入多种数据类型(无法限定输入类型)。
> 2. 泛型能够在编译时进行强类型检查
> 3. Object 替代泛型，需要进行类型转换。而使用泛型则不必。
> 4. 通过泛型配合接口，抽象类，实现了不同数据类型使用同一函数处理的方法。



## 2. Java 泛型声明

reference:

1. https://www.cnblogs.com/kalton/p/13647258.html

## 2.1 泛型类

- 语法

  ```
  class name<T1, T2, ..., Tn> {
  [[修饰符] T1 temp ;]
  // 注意区分泛型类方法和泛型方法,两者是独立的
  [[修饰符] T2 doSomething(Tn source1, T2 source2){
  do something
  }]
  }
  ```

  

### 2.2 泛型接口 

- 语法

  ```
  interface name<T1,t2> {T dosomething();}
  ```

  

### 2.3泛型方法

- 语法

  ```
  // 注意区分泛型类方法和泛型方法,两者是独立的
  [[修饰符] T2 T2 doSomething(String source1, T2 source2){
  do something
  }]
  ```

### 2.4 通配符

 - T，E，K，V，？

1. ？ 表示不确定的 java 类型 （注意？ 最为特殊，一般只用于函数形参声明中）

> // 合法
>
> class <T> Temp{}
>
> // 非法
>
> class <?> Temp2{}

1. T (type) 表示具体的一个java类型
2. K V (key value) 分别代表java键值中的Key Value
3. E (element) 代表Element



## 3. 类型擦除

**Java 泛型是使用编译器实现的类型擦除来实现的，使用泛型时，任何具体的类型信息都被擦除了。**

那么，类型擦除做了什么呢？它在编译时做了以下工作：

1. 把泛型中的所有类型参数替换为 **Object** ，如果指定类型边界，则使用类型边界来替换。因此，生成的字节码仅包含普通的类，接口和方法。比如 `T get()` 方法声明就变成了 `Object get()` ；

2. 擦除出现的类型声明，即去掉 `<>` 的内容。`List<String>` 就变成了 `List`。如有必要，插入类型转换以保持类型安全。

3. 生成桥接方法以保留扩展泛型类型中的多态性。类型擦除确保不为参数化类型创建新类；因此，泛型不会产生运行时开销。

```
public class GenericsErasureTypeDemo {
    public static void main(String[] args) {
        List<Object> list1 = new ArrayList<Object>();
        List<String> list2 = new ArrayList<String>();
        System.out.println(list1.getClass());
        System.out.println(list2.getClass());
    }
}
// Output:
// class java.util.ArrayList
// class java.util.ArrayList


Pair<String> p = new Pair<>("Hello", "world");
String first = p.getFirst();
String last = p.getLast();

// 编译器替换为：
Pair p = new Pair("Hello", "world");
String first = (String) p.getFirst();
String last = (String) p.getLast();
```

>
>
>示例说明：
>
>上面的例子中，虽然指定了不同的类型参数，但是 list1 和 list2 的类信息却是一样的。
>
>这是因为：**使用泛型时，任何具体的类型信息都被擦除了**。这意味着：`ArrayList<Object>` 和 `ArrayList<String>` 在运行时，JVM 将它们视为同一类型(同为ArrayList)。只有在get set 对象时候才会进行类型转换(编译替换)。



       **泛型不能用于显式地引用运行时类型的操作之中，例如：类型转型、instanceof 操作和 new 表达式。因为所有关于参数的类型信息都丢失了**。

​		**泛型不能向上转型**(编译器**检测规则**)；如：ArrayList<Integer>`向上转型为`ArrayList<Number>`或`List<Number>，编译会报错，是因为`ArrayList<Integer>`转型为`ArrayList<Number>`类型后，这个`ArrayList<Number>`就可以接受`Float`类型，因为`Float`是`Number`的子类。但是，`ArrayList<Number>`实际上和`ArrayList<Integer>`是同一个对象，也就是`ArrayList<Integer>`类型，它不可能接受`Float`类型， 所以在获取`Integer`的时候将产生`ClassCastException`。编译器为了避免这种错误，根本就不允许把`ArrayList<Integer>`转型为`ArrayList<Number>`。

如：

```
// 泛型方法
    public <T3 extends Node> T3 genericMethod(T3 source) {
        // 泛型类型参数不能使用 instanceof，建议利用 Class 信息进行对比
        // if (source instanceof T3)
        // {}

        // 不能创建、catch 或 throw 参数化类型对象
        //class MathException<T3> extends Exception { /* ... */ }    // 编译错误

        // 编译报错
        //        ArrayList<Integer> integerList = new ArrayList<Integer>();
        //        // 添加一个Integer：
        //        integerList.add(new Integer(123));
        //        // “向上转型”为ArrayList<Number>：
        //        ArrayList<Number> numberList = integerList;
        //        // 添加一个Float，因为Float也是Number：
        //        numberList.add(new Float(12.34));
        //        

        Node temp = source;
        return source;
    }
```

**结论**：

**编译器**擦拭法决定了泛型`<T>`：

- 不能是基本类型，例如：`int`；
- 不能获取带泛型类型的`Class`，例如：`Pair<String>.class`；(编译后只有Pair.class 这个信息)
- 不能判断带泛型类型的类型，例如：`x instanceof Pair<String>`；
- **不能实例化`T`类型，例如：`new T()`。只能被传入，然后赋值。**





## 4. 类型边界(解决泛型擦除导致类型不明确的问题)

reference:

1. https://juejin.cn/post/6844903917835419661
2. https://my.oschina.net/u/2278977/blog/820771
3. https://www.liaoxuefeng.com/wiki/1252599548343744/1255945193293888
4. https://itimetraveler.github.io/2016/12/27/%E3%80%90Java%E3%80%91%E6%B3%9B%E5%9E%8B%E4%B8%AD%20extends%20%E5%92%8C%20super%20%E7%9A%84%E5%8C%BA%E5%88% AB%EF%BC%9F/   （最清晰的例子）


    ### 4.1 T extend     

   类型边界可以对泛型的类型参数设置限制条件。例如，对数字进行操作的方法可能只想接受 `Number` 或其子类的实例。并且通过 extend 声明，实行了实参调用泛型所声明形参类型的方法。

- 语法

```
<T extends XXX>
```

- 样例

```
interface Node {
    String buildSql();
}

//🔔 注意：extends 关键字后面的第一个类型参数可以是类或接口，其他类型参数只能是接口。
class Sql<T extends Node, T2 extends Check> {
    void doSomething(T source){
       // 实参调用泛型所声明形参类型的方法
       source.buildSql();
    }
}

```

### 4.2  ? exntend （上界）

- 语法

```
<? entend XXX>
```

- 样例

```
class Pair<T extends Number> {
    private final T first;
    private final T last;

    public Pair(T first, T last) {
        this.first = first;
        this.last = last;
    }

    public T getFirst() {
        return first;
    }

    public T getLast() {
        return last;
    }

    static int add(Pair<Number> p) {
        Number first = p.getFirst();
        Number last = p.getLast();
        return first.intValue() + last.intValue();
    }

    static int addUpper(Pair<? extends Number> p) {
        Number first = p.getFirst();
        Number last = p.getLast();
        return first.intValue() + last.intValue();
    }

}


public class GenericsTest {
    public static void main(String[] args) {
		// 默认泛型声明数据类型必须与初始化数据类型一致，(打破了多态)
        Pair<Integer> In = new Pair<Integer>(1, 2);
        // 泛型擦除导致多态声明失败
        // Pair<Number> Long = new Pair<Long>(1L, 2L);
        // 声明上界解决
        Pair<? extends Number> Long = new Pair<Long>(1L, 2L);

        //        System.out.println(Pair.add(In));//不兼容的类型: Pair<java.lang.Integer>无法转换为Pair<java.lang.Number>
        System.out.println(Pair.addUpper(In));
        System.out.println(Pair.addUpper(Long));
    }
}
```



- 作用

  > 1.  泛型擦除导致多态失败(如上代码)， 上界声明使得泛型变量声明可以保持多态（List 可以声明为List<Numbere> temp = new ArrayList<Integer>()）。
  >
  > 2.   泛型擦除对于传参也会导致函数必须传入与形参一样的数据类型(不能是形参子类，打破了多态)，为了维护多态，上界声明可以解决问题。 
  >
  > 3. 上界声明可以取出泛型变量，但是无法修改泛型变量。

  

  ### 4.3 ? super (下界)

- 语法

```
<? super XXX>
```

- 样例

```
class Plate<T> {
    private T item;

    public Plate(T t) {
        item = t;
    }

    public void set(T item) {
        this.item = item;
    }

    public T get() {
        return item;
    }
}

//Lev 1
class Food {
}

//Lev 2
class Fruit extends Food {
}

class Meat extends Food {
}

//Lev 3
class Apple extends Fruit {
}

class Banana extends Fruit {
}

class Pork extends Meat {
}

class Beef extends Meat {
}

//Lev 4
class RedApple extends Apple {
}

class GreenApple extends Apple {
}


public class Test {

    public static void main(String[] args) {

        Plate<? super Fruit> p = new Plate<Fruit>(new Fruit());

        //存入元素正常
        p.set(new Fruit());
        p.set(new Apple());

        //读取出来的东西只能存放在Object类里。
        Apple newFruit3 = p.get();    //Error， 编译器报错
        Fruit newFruit1 = p.get();    //Error, 编译器报错
        Object newFruit2 = p.get();

        // 需要同时能够 get set 只能通过 Plate<Apple> apple = new Plate<Apple>(new Apple());
    }
}


```

- 作用

> 1. 解决上界无法修改泛型类型变量的问题。(无法修改是因为 jvm **无法确定**放入的值的类型是否是同类)


**结论**：


1. XXX（非？通配符，? 特殊） extend ClassName ，能够使实参调用 ClassName中的方法。
2. 形参中<? extend ClassName> 使得泛型变量声明可以保持多态，声明 Number 可以用 Long Double 等类型。
3. 形参中<? super ClassName> 使得声明了Long 可以用 Number 和Object 类型。

   

```
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

interface Node {
    String buildSql();
}

abstract class Basic implements Node, Supplier {
    protected Integer id;
    protected String tableName;
    private final String executeSql = "";

    @Override
    public String buildSql() {
        return executeSql;
    }
}

class ESNode extends Basic {

    @Override
    public Object get() {
        return null;
    }
}

class SpecESNode extends ESNode {

}

// 泛型接口声明
interface Check<T> {
    Boolean isVaild(T check);
}

class ESVaild<T extends Node> implements Check<T> {

    @Override
    public Boolean isVaild(T check) {
        return Boolean.TRUE;
    }
}

// 泛型类型声明为 T T2
//🔔 注意：extends 关键字后面的第一个类型参数可以是类或接口，其他类型参数只能是接口。
class Sql<T extends Node, T2 extends Check> {
    // 注意 泛型不能实例化
    // T temp = new T(); // 非法
    // 但是反射可以突破这个限制,如下
    T temp;

    void buildTemp(Class<T> clazz) throws IllegalAccessException, InstantiationException {
        temp = clazz.newInstance();
    }

    // 注意 泛型变量不能以 static 修饰
    // static T temp2; // 非法
    // static List<T> executeNodeTest = new ArrayList<T>(); // 非法

    // 泛型无法声明为数组
    //List<Integer>[] arrayOfLists = new List<Integer>[2];  // 编译错误


    List<T> executeNode = new ArrayList<T>();
    List<String> executeSql = new ArrayList<String>();

    Sql(T singleExecute) {
        executeSql.add(singleExecute.buildSql());
        executeNode.add(singleExecute);
    }


    // 泛型类方法声明，注意与泛型方法区分(泛型方法无需泛型类)
    public List<T> getExecuteNode() {
        return executeNode;
    }

    // 泛型方法
    public <T3 extends Node> T3 genericMethod(T3 source) {
        // 泛型类型参数不能使用 instanceof
        // if (source instanceof T3)
        // {}

        // 不能创建、catch 或 throw 参数化类型对象
        //class MathException<T3> extends Exception { /* ... */ }    // 编译错误

        // 编译报错
        //        ArrayList<Integer> integerList = new ArrayList<Integer>();
        //        // 添加一个Integer：
        //        integerList.add(new Integer(123));
        //        // “向上转型”为ArrayList<Number>：
        //        ArrayList<Number> numberList = integerList;
        //        // 添加一个Float，因为Float也是Number：
        //        numberList.add(new Float(12.34));
        //
        Node node = source;
        return source;
    }

    /**
     * 上界通配符
     *
     * @param Nodes
     * @return
     */
    public static String changeListWithUpper(List<? extends Node> Nodes) {
        StringBuffer sb = new StringBuffer();
        for (Node each : Nodes) {
            sb.append(each.buildSql());
        }

        // Nodes.add(new SpecESNode());// 报错，上界不能改变列表
        Nodes.get(0);
        return sb.toString();
    }

    /**
     * 没有任何通配符
     *
     * @param Nodes
     * @return
     */
    public static String changeListRaw(List<Node> Nodes) {
        StringBuffer sb = new StringBuffer();
        for (Node each : Nodes) {
            sb.append(each.buildSql());
        }
        Nodes.add(new ESNode());
        Nodes.get(0);
        return sb.toString();
    }

    /**
     * 下界通配符
     *
     * @param Nodes
     * @return
     */
    public static String changeListWithLow(List<? super ESNode> Nodes) {
        StringBuffer sb = new StringBuffer();
        // 报错，提示 迭代类型应该为 Object
        //        for (Node each : Nodes) {
        //            sb.append(each.buildSql());
        //        }
        // 同样报错，原因同上
        // Nodes.get(0).buildSql()
        Nodes.add(new ESNode());
        Nodes.get(0);
        return sb.toString();
    }

}


public class Test {


    public static void main(String[] args) {

        new Sql<ESNode, ESVaild<ESNode>>(new ESNode());
        Basic temp = new ESNode();

        List<ESNode> nodes = new ArrayList<ESNode>();
        nodes.add(new SpecESNode());

        // 与正常的 Java 多态不一样的是，这里只有changeListRaw 类型变量使用 changeListRaw(List<ESNode> Nodes) 才能使用该函数
//        Sql.changeListRaw(nodes);

        Sql.changeListWithLow(nodes);
        Sql.changeListWithUpper(nodes);
    }
}

```





