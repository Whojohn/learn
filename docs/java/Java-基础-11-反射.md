# Java-基础-11-反射

## 1.前置知识（Class类及类加载）

### 1.1 类加载

> 类加载详细在类和jvm 笔记中有记录。

- 类加载的流程

  装载(load；ClassLoader)-> 链接(link) -> 初始化(initial)

  						 |(连接又包含了以下三个流程)
  			
  							——》 验证(verification)->准备（perparation）->解析(resoluation) 

**注意**：

1. Load 阶段会把字节码转换为 Jvm 中的一个 **Class  对象**，每一个类都对应一个唯一的  **Class 对象**。
2. 反射或者用户调用ClassLoader找不到类会抛出 **ClassNotFoundException** (属于**Exception异常** )； JVM 自动类加载失败会抛出 **NoClassDefFoundError**(属于**Error异常**)；他们都是发生在 **load** 阶段的错误。

> - **ClassNotFoundException** (属于代码逻辑错误)
>
>   出现情况：
>
>   反射中调用
>
>   1. Class.forName()
>
>   手动调用类加载器
>
>   1. ClassLoader.loadClass()  
>   2. ClassLoader.findSystemClass()
>
> - NoClassDefFoundError
>
>   **编译是能够正常编译的**，属于JVM内部错误(一般是缺少 Jar 依赖，打包或者JVM类加载异常。)
>
>   出现情况：
>
>   1. 打包缺少 特定 Jar 类。
>
>   2. JVM 类加载 该类 Class 失败，运行到new 字段或者获取 静态成员或者方法时。
>
> 

### 1.2 Class 类

>         所有类(除了基础数据类型以外的数据类型：类，接口数组等；)在 JVM 中都以 Class 类的一个实例表示。Class 具有类的父子信息，所在包，方法，结构体，变量的等信息。

Class 具体方法见反射。

- Field 类(Class 类中描述字段的类)

  > `getName()`：返回字段名称，例如，`"name"`；
  >
  > `getType()`：返回字段类型，也是一个`Class`实例，例如，`String.class`；
  >
  > `getModifiers()`：返回字段的修饰符，它是一个`int`，不同的bit表示不同的含义。

- Method 类(Class 中描述方法的类)

  > - `Method getMethod(name, Class...)`：获取某个`public`的`Method`（包括父类）
  > - `Method getDeclaredMethod(name, Class...)`：获取当前类的某个`Method`（不包括父类）
  > - `Method[] getMethods()`：获取所有`public`的`Method`（包括父类）
  > - `Method[] getDeclaredMethods()`：获取当前类的所有`Method`（不包括父类）

- Constructor 类

### 1.3 java.lang.reflect.Field 包

> 除了 Class 类以外，还能通过  java.lang.reflect.Field 包实现反射一系列操作。



## 2. 反射

> 反射(Reflection)是 Java 程序开发语言的特征之一，它允许运行中的 Java 程序获取自身的信息，并且可以操作类或对象的内部属性。

### 2.1 获取Class对象方法

1. 类的静态变量 `.class `获取

```
Class cls = String.class;
System.out.println(cls.getCanonicalName());Class cls = String.class
```

2. 对象调用`getClass`方法(Object 基础而来的方法)

```
Class cls3 = "show".getClass();
System.out.println(cls3.getCanonicalName());
```

3. **`Class.forName` 静态方法**

```
Class cls2 = Class.forName("[Ljava.lang.String;");
System.out.println(cls2.getCanonicalName());
```

```
Class cls = String.class;
System.out.println(cls.getCanonicalName());
Class cls2 = Class.forName("[Ljava.lang.String;");
System.out.println(cls2.getCanonicalName());
Class cls3 = "show".getClass();
System.out.println(cls3.getCanonicalName());
```

### 2.2 类对比方法

> 注意类的**泛型无法进行对比**，只能对比类。因此泛型存在泛型擦除的问题。

#### 2.2.1 子类的判定

> 判定一个类是否是某个类的子类或自身

1. instanceof

2. Class 类中 isInstance 方法

   - 示例代码

   ```
           List<String> temp = new ArrayList<String>();
           // instanceof
           if (temp instanceof List) {
               System.out.println("ArrayList is subclass of List");
           }
           if (temp instanceof ArrayList) {
               System.out.println("ArrayList is subclass of ArrayList");
           }
           // Class 类中 isInstance 方法
           if (List.class.isInstance(temp)) {
               System.out.println("ArrayList is subclass of List");
           }
   ```

   

#### 2.2.2 同类的判定

> 一个类与另一个类是否同一个类。

1.  利用 Class 属性对比

```
        List<String> tempString = new ArrayList<String>();
        List<Integer> tempInteger = new ArrayList<Integer>();
        List<String> linkListString = new LinkedList<>();
        // 注意类的泛型无法进行对比，只能对比类。
        System.out.println(tempInteger.getClass());
        System.out.println(tempInteger.getClass().equals(tempString.getClass()));
        System.out.println(tempInteger.getClass().equals(linkListString.getClass()));
```

### 2.3 类字段操作

#### 2.3.1 类字段信息

- Field getField(name)：根据字段名获取某个`public`修饰的field（包括父类，只限定 `public` 修饰）
- Field getDeclaredField(name)：根据字段名获取当前类的某个field（不包括父类，任何修饰符）
- Field[] getFields()：获取所有`public`的field（包括父类，只限定`public` 修饰）
- Field[] getDeclaredFields()：获取当前类的所有field（不包括父类，所有修饰符）

```
public class Reflect {
    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        Class stdClass = Student.class;
        Field score = stdClass.getField("score");
        // 获取public字段"score":
        System.out.println(score.getName());
        System.out.println(score.getType());
        //返回字段的修饰符，它是一个int，不同的bit表示不同的含义。
        System.out.println(score.getModifiers());
        // 获取private字段"grade":
        System.out.println(stdClass.getDeclaredField("grade"));
    }
}

class Student extends Person {
    public int score;
    private int grade;
}

class Person {
    public String name;
}
```

#### 2.3.2 类实例对象字段值获取&修改

```
//对于一个Person实例，我们可以先拿到name字段对应的Field，再获取这个实例的name字段的值：
public class Reflect {
    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        Object p = new Person("Xiao Ming");
        Class c = p.getClass();
        Field f = c.getDeclaredField("name");
        // 通过  setAccessible(true) 使得非 public 对象能够被访问；
        // 注意：  SecurityManager 可能拒绝该操作。如 java 开头的包。
        f.setAccessible(true);
        Object value = f.get(p);
        System.out.println(value); // "Xiao Ming"
        f.set(p, "Xiao Hong");
        System.out.println(f.get(p));
    }
}

class Person {
    private String name;

    public Person(String name) {
        this.name = name;
    }
}
```

### 2.4 类方法操作

#### 2.4.1 类方法信息

```
public class Main {
    public static void main(String[] args) throws Exception {
        Class stdClass = Student.class;
        // 获取public方法getScore，参数为String:
        System.out.println(stdClass.getMethod("getScore", String.class));
        // 获取继承的public方法getName，无参数:
        System.out.println(stdClass.getMethod("getName"));
        // 获取private方法getGrade，参数为int:
        System.out.println(stdClass.getDeclaredMethod("getGrade", int.class));
    }
}

class Student extends Person {
    public int getScore(String type) {
        return 99;
    }
    private int getGrade(int year) {
        return 1;
    }
}

class Person {
    public String getName() {
        return "Person";
    }
}

```

#### 2.4.2 类方法的调用

```
 // 反射调用方法时，仍然遵循多态原则
        String s = "Hello world";
        // 获取String substring(int)方法，参数为int:
        Method m = String.class.getMethod("substring", int.class);
        // 在s对象上调用该方法并获取结果:
        String r = (String) m.invoke(s, 6);
        // 打印调用结果:
        System.out.println(r);

        m = String.class.getMethod("substring", int.class, int.class);
        // 在s对象上调用该方法并获取结果:
        // 传入的对象类型，个数必须与 method 中的声明一直
        r = (String) m.invoke(s, 6, 8);
        System.out.println(r);

        // 静态方法调用
        m = Integer.class.getMethod("parseInt", String.class);
        // 同理假如获取的方法不是 public 修饰，可以 Method.setAccessible(true) 解决
        // m.setAccessible(true)
        // 与非静态方法相比，第一位不需要传入实例对象,可以传入null对象
        Integer n = (Integer) m.invoke(null, "12345");
        // 打印调用结果:
        System.out.println(n);
```

### 2.5 类实例方法(反射如何实例化对象)

#### 2.5.1 获取构造函数

- `getConstructor` - 返回类的特定 public 构造方法。参数为方法参数对应 Class 的对象。
- `getDeclaredConstructor` - 返回类的特定构造方法。参数为方法参数对应 Class 的对象。
- `getConstructors` - 返回类的所有 public 构造方法。
- `getDeclaredConstructors` - 返回类的所有构造方法。

#### 2.5.2 实例化对象(调用构造函数)

```
        // 包括private 构造函数
        Constructor<?>[] constructors1 = String.class.getDeclaredConstructors();
        System.out.println("String getDeclaredConstructors 清单（数量 = " + constructors1.length + "）：");
        for (Constructor c : constructors1) {
            System.out.println(c);
        }

        // public 构造函数
        Constructor<?>[] constructors2 = String.class.getConstructors();
        System.out.println("String getConstructors 清单（数量 = " + constructors2.length + "）：");
        for (Constructor c : constructors2) {
            System.out.println(c);
        }

        System.out.println("====================");
        // 获取  String(String) 构造函数
        Constructor constructor = String.class.getConstructor(String.class);
        System.out.println(constructor);
        String str = (String) constructor.newInstance("String");
        System.out.println(str);


        System.out.println("====================");
        // 获取  String(char[].class) 构造函数
        constructor = String.class.getConstructor(char[].class);
        System.out.println(constructor);
        str = (String) constructor.newInstance("char array".toCharArray());
        System.out.println(str);

        System.out.println("====================");
        // 调用无参构造函数
        str = String.class.newInstance();
        System.out.println(str);
        System.out.println("====================");
```



### 3. 代理模式

- 代理模式解决了什么问题（应用场景是什么）:

  > 1. AOP, RPC 调用等。如：Springboot 的拦截器、日志模块、数据库JDBC连接器等。实现了类似 Python 装饰器的功能，在不改变具体类的情况下，扩充了类的行为。

#### 3.1 静态代理

> 通过提供代理类，而不是直接操纵对象的形式对对象进行操作。

例子: 

```
interface Base {
    void doSomething();
}

class Node implements Base {

    @Override
    public void doSomething() {
        System.out.println("do something!");
    }
}

class NodeProxy implements Base {

    @Override
    public void doSomething() {
        // 模拟初始化，如数据库连接，打印日志，统计耗时等行为
        System.out.println("Initial Node.");
        new Node().doSomething();
    }
}

public class Reflect {
    public static void main(String[] args) {
        // 静态代理模式
        // 缺点：
        // 每一个类都需要写一个代理类，不利用代码复用如： 初始化连接，打印日志，统计耗时等。
        Base temp = new NodeProxy();
        temp.doSomething();
    }
}
```

#### 3.2 动态代理

> 动态代理是为了解决静态代理的缺点，如：
>
> 1. 日志操作、连接初始化等流程都是固定的，假如每个实体都需要代理类(代理类的逻辑相同)，这样会有很多重复代码的代理类。

- InvocationHandler & Proxy 类配合实现动态代理

```
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

interface Base {
    void doSomething();
    String sayHellow();
}

class Node implements Base {
    private final String name;

    public Node(String name) {
        this.name = name;
    }

    @Override
    public void doSomething() {
        System.out.println(this.name + " do something!");
    }

    @Override
    public String sayHellow() {
        return "hellow !!!";
    }
}

class NodeProxy implements Base {
    /**
     * 静态代理对象
     */

    @Override
    public void doSomething() {
        // 模拟初始化，如数据库连接，打印日志，统计耗时等行为
        System.out.println("Initial Node by static proxy");
        new Node("NodeProxy").doSomething();
    }

    @Override
    public String sayHellow() {
        return "hellow  static proxy";
    }
}

class DynamicProxy implements InvocationHandler {
    // 代理对象
    private final Object source;

    // 利用构造函数传入代理对象
    public DynamicProxy(Object source) {
        this.source = source;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("Initial Node by dynamic proxy");
        Object result = method.invoke(this.source, args);
        return result;
    }
}

public class Reflect {
    public static void main(String[] args) {
        // 静态代理模式
        // 缺点：
        // 每一个类都需要写一个代理类，不利用代码复用如： 初始化连接，打印日志，统计耗时等。
        Base temp = new NodeProxy();
        temp.doSomething();

        // 动态代理
        // 传入代理对象
        InvocationHandler handler = new DynamicProxy(temp);
        /*
         * 通过Proxy的newProxyInstance方法来创建我们的代理对象，我们来看看其三个参数
         * 第一个参数 handler.getClass().getClassLoader() ，我们这里使用handler这个类的ClassLoader对象来加载我们的代理对象
         * 第二个参数 temp.getClass().getInterfaces()，我们这里为代理对象提供的接口是真实对象所实行的接口，表示我要代理的是该真实对象，这样我就能调用这组接口中的方法了
         * 第三个参数handler， 我们这里将这个代理对象关联到了上方的 InvocationHandler 这个对象上
         */
        Base result = (Base) Proxy.newProxyInstance(handler.getClass().getClassLoader(),
                temp.getClass().getInterfaces(),
                handler);
        // 利用代理对象，随意调用方法
        result.doSomething();
        result.sayHellow();
    }
}


```

### 4. 总结

反射的作用：

1. 框架如：springboot 底层原理。
2. 实现动态代理，如：日志统一打印，全链路跟追，连接初始化等。
3. 

反射的缺点：

1. **性能开销**

2. 封装破坏(引入非安全的代码访问，破坏 private 修饰符限制)

   