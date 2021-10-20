# Java-基础-4-类&接口

reference:

https://zh.wikipedia.org/wiki/%E9%9D%A2%E5%90%91%E5%AF%B9%E8%B1%A1%E7%A8%8B%E5%BA%8F%E8%AE%BE%E8%AE%A1

## 1. 类声明

```
# 只能继承一个类。
# 可以实现多个接口。
[访问修饰符] [abstract] class className [extend singleClass] [implement interfaceName] {
      # 变量
      访问修饰符 变量；
      # 构造函数
      public className([ParamsType paramsname]){
      
      }
      # 方法
      访问修饰符 [abstract|static]  [函数返回类型(引用类型+基础数据类型+void)] 函数名字([变量]){
      
      }
}
```

## 2. 概念

```
interface Bird {
    String getBirdName();

    Double getBirdSpeed();
}

// 继承
public class T extends BirdModel {
    Integer duplicate;
    // 初始化块
    {
        duplicate = 2;
    }

    public T(String birdName) {
        super(birdName, new Double("3.2"));
        System.out.println(super.duplicate);
        System.out.println(this.duplicate);
    }

    // 方法重写
    @Override
    public String getBirdName() {
        return super.getBirdName();
    }

    public static void main(String[] args) {
        // b1,b2为多态体现
        Bird b1 = new T("b1");
        System.out.println(b1.getBirdSpeed());
        Bird b2 = new BirdModel("b2");
        System.out.println(b2.getBirdSpeed());
    }
}

class BirdModel implements Bird {
    // 封装的体现
    private final String birdName;
    private final Double birdSpeed;
    public static Integer duplicate;
    // 静态初始化代码块，只能操作外部静态变量
    static {
        duplicate=1;
    }

    // Bird(String birdName) Bird(String birdName, Double birdSpeed) 体现重载
    public BirdModel(String birdName) {
        this.birdName = birdName;
        this.birdSpeed = new Double("2.2");
    }

    public BirdModel(String birdName, Double birdSpeed) {
        this.birdName = birdName;
        this.birdSpeed = birdSpeed;
    }


    // 封装的体现
    public String getBirdName() {
        return birdName;
    }

    // 封装的体现
    public Double getBirdSpeed() {
        return birdSpeed;
    }
}
```



- 类(Class)

  > 定义了一件事物的抽象特点。类的定义包含了数据的形式以及对数据的操作。BirdModel是类。

- 对象（Object）

  > 对象是类的一个实例， new BirdModel() 就是一个对象。

- 引用（type,注意java 中reference 跟 refercence type 不一样）

  > b2为引用，指向 new BirdModel() 这一个对象。

- 继承

  > 1. 在某种情况下，一个类会有子类。子类比原本的类（称为[父类](https://zh.wikipedia.org/w/index.php?title=父类&action=edit&redlink=1)）要更加具体化。
  > 2. **静态类不能继承**。静态方法，静态变量，子类不能重写，子类只是覆盖了而已。子类需要调用父类的静态方法，静态变量，需要用父类.xxx 进行访问(不能用super 进行访问)。

- 重载

- 重写

> 父类的实现被子类覆盖，子类覆盖的实现优先调用。

- 多态

  > 是指由继承而产生的相关的不同的类，其对象对同一消息会做出不同的响应。

- 封装

  > 隐藏了某一方法的具体运行步骤，取而代之的是通过消息传递机制发送消息给它。

- oop-行为  (函数)

- oop-状态  (属性)

- oop-标识（函数名）



## 3. 类访问修饰符(见Java基础-修饰符(Modifiers))



## 4. 类语法详解

- 变量（final & static ）

  总结：

  1.  预定于常量声明为 static fianl。
  2.  类属性声明为 static。
  3.  需要不可变对象使用 final， final 修饰的变量只能实例化一次。
  4.  final 修饰的常量还能避免子类修改该对象(理由同上)。

> ```
> public class JsT2 {
>  // static 声明的变量为类变量，类的实例都指向同一对象。配合声明final 且初始化为固定值则为预定义常量写法(所有类共享+无法更改变量)。
>  private static final Integer DEFAULT_NUB = 2;
>  // final 用于修饰常量，final 修饰的值一旦实例化后无法修改
>  private final Integer tempNonInstance;
>  private final Integer tempInstance = 2;
> 
> 
>  public JsT2() {
>      this.tempNonInstance = 4;
>      // 报错，因为已经实例化
>      // this.tempInstance = 3;
>  }
> }
> ```

- 构造函数

      总结：
      
             1.  类默认都带有一个无参的构造函数。
             2.  super 用于调用父类方法，this 用于调用当前类方法。当需要调用父类构造函数时候，super(xxx)方式进行调用。当前类与父类变量同名时候，也是通过super和this 声明使用父类还是子类的变量。

- 初始化块

  总结：

1. 初始化块一般放于所有变量声明的后面。
2. 初始化块一般配合 static 字段实现单例。

- 方法

  总结：

1. java是按值传递的。

```
public class JsT {
    private Integer in = 2;

    public JsT(Integer in) {
        this.in = in;
    }

    public static void changeJsT(JsT t1, JsT t2) {
        JsT temp = t1;
        t1 = t2;
        t2 = temp;
        System.out.println("change t1 and t2!t1,t2 value is :");
        System.out.println(t1.in);
        System.out.println(t2.in);
    }

    public static void main(String[] args) {
        JsT t1 = new JsT(1);
        JsT t2 = new JsT(2);
        changeJsT
装载(t1, t2);
        System.out.println("show after change t1 and t2! t1,t2 value is :");
        System.out.println(t1.in);
        System.out.println(t2.in);
    }
}
```


## 5. 类加载

reference :

1. https://shuyi.tech/archives/thejavaclassloadmechamism
   2.https://snailclimb.gitee.io/javaguide/#/docs/java/jvm/%E7%B1%BB%E5%8A%A0%E8%BD%BD%E8%BF%87%E7%A8%8B
   3.. https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-5.html#jvms-5.1

### 5.1 类加载流程

**类的加载流程如下**：
装载(load)-> 链接(link)  -> 初始化(initial)

> 其中 link截断又可分为：  验证(verification)->准备（perparation）->解析(resoluation)

- 加载

把代码数据加载到内存中。

>    加载阶段是类加载过程的第一个阶段。在这个阶段，JVM 的主要目的是将字节码从各个位置（网络、磁盘等）转化为二进制字节流加载到内存中，接着会为这个类在 JVM 的方法区创建一个对应的 Class 对象，这个 Class 对象就是这个类各种数据的访问入口。（对应知识点ClassLoader）

**知识点： 加载器类型，双亲委派，自定义加载器，SPI等；**

>  - 默认加载器类型：
>
>  1. Bootstrap classLoader:主要负责加载核心的类库(java.lang.*等)，构造ExtClassLoader和APPClassLoader。
>  2. ExtClassLoader：主要负责加载jre/lib/ext目录下的一些扩展的jar。
>  3. AppClassLoader：主要负责加载应用程序的主函数类

> 加载器加载流程(默认位双亲委派)
>
> - 以默认加载器为例说明
>
> 1. AppClassLoader 判定类是否已加载，已加载则不进行加载。未加载向上调用ExtClassLoader进行加载。
> 2. ExtClassLoader 判定类是否加载已加载，已加载则不进行加载。未加载向上调用Bootstrap 进行加载。
> 3. ExtClassLoader 判定类是否加载已加载，已加载则不进行加载。未加载直接进行加载。
>
> - 双亲委派的目的是？
>   1. 利用系统加载器优先加载，防止系统类被替换(保证系统类安全)。


- 验证
  进行jvm 规范和代码逻辑验证。

> 当 JVM 加载完 Class 字节码文件并在方法区创建对应的 Class 对象之后，JVM 便会启动对该字节码流的校验，只有符合 JVM 字节码规范的文件才能被 JVM 正确执行。这个校验过程大致可以分为下面几个类型：

> - JVM规范校验。 
>   JVM 会对字节流进行文件格式校验，判断其是否符合 JVM 规范，是否能被当前版本的虚拟机处理。例如：文件是否是以 0x cafe bene开头，主次版本号是否在当前虚拟机处理范围之内等。
> - 代码逻辑校验。 
>   JVM 会对代码组成的数据流和控制流进行校验，确保 JVM 运行该字节码文件后不会出现致命错误。例如一个方法要求传入 int 类型的参数，但是使用它的时候却传入了一个 String 类型的参数。一个方法要求返回 String 类型的结果，但是最后却没有返回结果。代码中引用了一个名为 Apple 的类，但是你实际上却没有定义 Apple 类。

- 准备（**注意，准备阶段不等于类的初始化**）

> 准备阶段只负责处理**static修饰的变量(类变量)的内存分配**。假如是Final 声明的将会直接绑定值，否则绑定默认值。**不会进行任何代码的处理，只处理静态变量。**

```
// temp 准备阶段，分配内存，将会被**初始化为0**；
public static int temp = 3;
// default 准备阶段,分配内存，将会被**初始化为3**；
final  public static int default = 3;
// refertypeInteger 准备阶段，分配内存，初始化为null;
final  public static Integer refertypeInteger = 3;
// 准备阶段，什么也不做。
public String website = "aaai";
```

- 解析

> 解析是将符号引用替换为直接引用，解析动作针对类或接口，字段，类或接口的方法进行解析。（jvm 底层对指针的一些操作)

- 初始化
  真正开始执行用户代码，按照执行顺序对类进行初始化。

**触发初始化的几种情况**：

1. 遇到 new、getstatic、putstatic、invokestatic 这四条字节码指令时，如果类没有进行过初始化，则需要先触发其初始化。生成这4条指令的最常见的Java代码场景是：使用new关键字实例化对象的时候、读取或设置一个类的静态字段（被final修饰、已在编译器把结果放入常量池的静态字段除外）的时候，以及调用一个类的静态方法的时候。
2. 使用 java.lang.reflect 包的方法对类进行反射调用的时候，如果类没有进行过初始化，则需要先触发其初始化。
3. 当初始化一个类的时候，如果发现其父类还没有进行过初始化，则需要先触发其父类的初始化。
4. 当虚拟机启动时，用户需要指定一个要执行的主类（包含main()方法的那个类），虚拟机会先初始化这个主类。
5. 当使用 JDK1.7 动态语言支持时，如果一个 java.lang.invoke.MethodHandle实例最后的解析结果 REF_getstatic,REF_putstatic,REF_invokeStatic 的方法句柄，并且这个方法句柄所对应的类没有进行初始化，则需要先出触发其初始化。

> 注意！！！优先执行静态变量，静态代码块(假如不存在父类)，并且按照代码出现顺序初始化(即先类初始化后执行对象初始化)。假如存在父类**未初始化**，则先执行父类的初始化。被调用类不一定会被初始化，可能变量是 final static 修饰(读取该变量，无需初始化，因为准备阶段已经完成final static 修饰的变量初始化)，或者是父类的变量(从基类出发，只初始化到出现变量的父类)。
> 注意！！！ **数组声明不会初始化包裹的类，如 Test[] temp= new Test[10]; 是不会触发 Test的初始化。**


- 使用

> 当 JVM 完成初始化阶段之后，JVM 便开始从入口方法开始执行用户的程序代码。

- 卸载

> 当用户程序代码执行完毕后，JVM 便开始销毁创建的 Class 对象，最后负责运行的 JVM 也退出内存。

# 6. Abstract 类

1. 抽象类不能被实例化(初学者很容易犯的错)。

2.  抽象类中不一定包含抽象方法，但是有抽象方法的类必定是抽象类。

3. 抽象类中的抽象方法只是声明，不包含方法体。

4. 构造方法，类方法（用 static 修饰的方法）不能声明为抽象方法。

5. 抽象类的子类必须给出抽象类中的抽象方法的具体实现，除非该子类也是抽象类。
6. 抽象类不能是 final 修饰，继承失去了意义。
7. 抽象类方法也不能是 private 修饰，继承失去了意义。
8. 抽象类可以存在 private 修饰的变量，以及具体实现方法。

# 7. Interface

1. 默认 interface 方法只能是public(缺省就是public)。
2. 接口没有构造方法。
3. 接口中所有的方法必须是抽象方法，Java 8 之后 接口中可以使用 default 关键字修饰的非抽象方法。
4. 接口不能包含成员变量，除了 static 和 final 变量。
5. 接口中每一个方法也是隐式抽象的,接口中的方法会被隐式的指定为 **public abstract**（只能是 public abstract，其他修饰符都会报错）。
6. 接口中可以含有变量，但是接口中的变量会被隐式的指定为 **public static final** 变量（并且只能是 public，用 private 修饰会报编译错误）。

