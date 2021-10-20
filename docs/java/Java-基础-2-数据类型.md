# Java-基础-2-数据类型

reference:

https://learning.oreilly.com/library/view/java-8-pocket/9781491901083/

https://snailclimb.gitee.io/javaguide/#/docs/java/basis/Java%E5%9F%BA%E7%A1%80%E7%9F%A5%E8%AF%86?id=%e5%ad%97%e7%ac%a6%e5%9e%8b%e5%b8%b8%e9%87%8f%e5%92%8c%e5%ad%97%e7%ac%a6%e4%b8%b2%e5%b8%b8%e9%87%8f%e7%9a%84%e5%8c%ba%e5%88%ab

https://github.com/Whojohn/AndroidAllGuide/blob/master/java/%E9%87%8D%E6%8B%BEJava%EF%BC%880%EF%BC%89-%E5%9F%BA%E7%A1%80%E7%9F%A5%E8%AF%86%E7%82%B9.md



```
# 核心知识点
1. Java 中只有基本数据类型，引用类型这两种数据类型。
2. temp = temp + 2 在java中2是数字常量，是 int 类型(假如是2.2则是 double 类型)，因此与更低精度的数值类型运算，必须强制转换数据类型。 
3. 在类变量中，变量的为类的默认值： null; 基础数据类型默认值:false、0、0.0, \u0000(ps：类属性指的是可以get set的变量，变量则可以没有get set方法。)；
4. 在类变量中，使用包装类更符合实际要求(基础数据类型拥有默认值，只有类才能表达Null)。


```

## 1.基础数据类型

| 数据类型 | 默认值  | 大小              | 包装类    |
| -------- | ------- | ----------------- | --------- |
| byte     | 0       | 8bit              | Byte      |
| short    | 0       | 16bit             | Short     |
| int      | 0       | 32bit             | Integer   |
| long     | 0L      | 64bit             | Long      |
| float    | 0.0f    | 32bit             | Float     |
| double   | 0.0d    | 64bit             | Double    |
| char     | 'u0000' | 16bit             | Character |
| boolean  | false   | (不同jvm实现不同) | Boolean   |

**重要概念**：

包装类：
   基本每一个基本数据类型都有包装类型(包装类型是引用类型，一个常用类而已)
内存表现：
   基础数据类型指向真实数据，并且避免重复建立相同对象(包装类型也有一定范围的常量池优化)。

常量：

> **默认数字类常量都是int。当常量数字用于运算时，必须强制向下转换数据类型。**
>
> ```
> // 20就是一个常量
> byte temp = 20;
> // 提示提供的是int类型，报错
> //byte total = temp + 2;
> //total = total +2;
> 
> byte total = (byte) (temp + 2);
> // += 意味着 total = (byte)(total + 1),因此不报错；total = total + 2 报类型为int错，因为不包含类型向下转换。
> total += 2;
> 
> short sho = 20;
> // 提示提供的是int类型，报错
> //short sho2 = sho+2;
> short sho2 = (short) (sho + 2);
> ```

包装类的常量池： 

    Java 基本类型的包装类的大部分都实现了常量池技术。`Byte`,`Short`,`Integer`,`Long` 这 4 种包装类默认创建了数值 **[-128，127]** 的相应类型的缓存数据，`Character` 创建了数值在[0,127]范围的缓存数据，`Boolean` 直接返回 `True` Or `False`。



 ### 1.1 基础数据类型特性

  1. 包装类，自动拆箱，装箱。

 ```
 // Integer 为int包装类
Integer i = 10;  //装箱
int n = i;   //拆箱

int i1 = 1;
Integer i2 = 1;
Integer i3 = new Integer(1);
// 返回true，因为自动拆箱，不推荐这么进行对比
System.out.println(i1 == i2);
// 返回True 自动拆箱
System.out.println(i1 == i3);
// 返回 False 对比的是对象地址
System.out.println(i2 == i3);
// 返回True ，推荐包装类和基础类型对比方法
System.out.println(i2.equals(i3));
 ```

  2. 平台无关性，任何平台存储所需的字节一致，精度也一致


 ### 1.2 基础数据类型转换

 **数字类型在java中以补码形式表达**

1. **注意数字常量在java中是int类型，因此运算包含常量时候，必须注意强制转换。**

   > ```
   > // 20就是一个常量
   > byte temp = 20;
   > // 提示提供的是int类型，报错
   > //byte total = temp + 2;
   > //total = total +2;
   > 
   > byte total = (byte) (temp + 2);
   > // += 意味着 total = (byte)(total + 1),因此不报错；total = total + 2 报类型为int错，因为不包含类型向下转换。
   > total += 2;
   > 
   > short sho = 20;
   > // 提示提供的是int类型，报错
   > //short sho2 = sho+2;
   > short sho2 = (short) (sho + 2);
   > ```
   >
   > 

2. 低精度向高精度转换时候，默认允许。

3. 高精度向低精度转换，存在**精度丢失**(丢掉小数)，**截断问题**(例：int 转byte 超过128会截断)； 假如需要确保转换后是正确的，请使用Math.toIXXXExact 等方法进行转换

4. 高精度向下转换必须强制声明转换。

5. 低精度向高精度转换不需要强制声明。

6. 转换的优先级为：

> ```
> # 从低到高
> byte,short,char—> int —> long—> float —> double 
> ```

 ```
 // 非包装类，转换
 long source = 100L;
int temp = (int) source ;
// 推荐转换方式，假如出现截断会报错
int temp2 = Math.toIntExact(source);
 ```

 ### 1.3 基础数据类型对比

  1. 非包装类对比(只允许非小数数字类型，非包装类型用==对比)

> int a = 1;
> int b = 1;
> System.out.println(a==b);

2. 基础小数数字类型，精度问题

>  **结论**：
>  (double float，都存在精度问题，要准确表达必须使用BigDecimal)

```
// 小数类对比存在精度问题，BigDecimal 表达才能保证精度
// float 精度存在问题
System.out.println(BigDecimal.valueOf(0.1f));
System.out.println(Float.compare(-1f, -1f) == 0);
System.out.println(Float.compare(-1.0f, -2.0f) == 0);
// 注意两个值长度不一致（精度不一样），但是对比结果竟然一样
System.out.println(Float.compare(-1.000000000000001f, -1.0000000000001f) == 0);

System.out.println(new Float(0.1f).equals(0.1f));
System.out.println(new Float(0.1f).equals(0.2f));
// 返回 true 好像精度没问题，但是输出显示就丢失精度
System.out.println(new Float(-1.000000000000001f).equals(-1.0000000000001f));
// 丢失精度
System.out.println(-1.000000000000001f);

// double 精度也存在问题，需要解决小数表达问题必须用 BigDecimal 表示
System.out.println(BigDecimal.valueOf(0.1d));
System.out.println(Double.compare(-0.1d, 0.1d) == 0);
System.out.println(-0.1d == 0.1d);
// 结果错误，显示精度丢失
Double d1 = new Double("1.00000001");
Double d2 = new Double("0.00000001");
System.out.println(d1 - d2);

> 0.10000000149011612
> true
> false
> true
> true
> false
> true
> -1.0
> 0.1
> false
> false
> 0.9999999999999999

```

 2.. ==(!=) 符号 vs equal (包装类对比使用equal) vs hashcode

> - ==(!=) 
>   == 默认对比的是对象内存地址
> - equal 对比对象的内容(约定俗成， 继承于Object 类的方法)
>   对于自己的类，要重写 equal 方法，必须同时重写 hashcode 方法。保证两
>   个相同内容hashcode返回值一致，且equal 返回True 时，equal 才能确保两者内容一致。**注意,Object 类中 hashcode（默认hashcode）方法是利用地址+某些信息构建的hashcode,改变对象的任何参数，除非改变引用，不然hashcode 不会改变。**

 ```
int i1 = 1;
Integer i2 = 1;
Integer i3 = new Integer(1);
// 返回true，因为自动拆箱，不推荐这么进行对比
System.out.println(i1 == i2);
// 返回True 自动拆箱
System.out.println(i1 == i3);
// 返回 False 对比的是对象地址
System.out.println(i2 == i3);
// 返回True ，推荐包装类和基础类型对比方法
System.out.println(i2.equals(i3));
 ```

## 1.4 默认值

1. 函数中变量必须初始化后才能使用，类中变量可以不初始化后使用。
2. 在类变量中，变量的为类的默认值为 null; 属性基础数据类型为:false、0、0.0, \u0000；
3. 类中使用**包装类**更加安全，避免 null 值被赋予默认值。如： 反序列化json 的时候把 null 值的对象，赋予了默认值(null != 默认值)。

```
// 默认值示例
public class T {
    public static void main(String[] args) {
        int a;
        // 报错，a没有被初始化。
        // System.out.println(a);
        So s = new So();
        System.out.println(s.getB());// 0
        System.out.println(s.getD());// 0.0
        System.out.println(s.getC());// ascii 0,就是ascii null
        System.out.println(s.getBo());// false
        System.out.println(s.getIn());// null
    }
}

class So {
    byte b;
    char c;
    double d;
    boolean bo;
    Integer In;

    public Integer getIn() {
        return In;
    }

    public boolean getBo() {
        return bo;
    }

    public double getD() {
        return d;
    }

    public char getC() {
        return c;
    }

    public byte getB() {
        return b;
    }
}
```





## 2.引用类型

**注意java中 reference type 不等于 reference **

- 数组，基础类型数组、类数组
- 类，如：String(String 只是一个常用类而已)等
- 注解
- 枚举
- 接口

jvm：
  jvm 堆中存放真实对象，栈中存放指向堆的真实地址。


### 2.1. 常用引用类型

- Stinrg

```
#声明方式
#stra 不会存放于常量池
String stra = new String("aaa");
#stra 放入常量池，必须保证放入常量池的值在一定范围内，以免引发反效果。
stra = stra.intern();
#strb 会存放于常量池中
String strb = "aaa";
```

- StringBuilder （非线程安全String 适合修改、拼接String）

```
StringBuilder sb = new StringBuilder(10);
sb.append("abc");
// 移除b
sb.delete(1,2);
// 数组下标1 的位置插入 bd
sb.insert(1,"bd");
System.out.println(sb.indexOf("bd"));
System.out.println(sb.toString());
```

- StringBuffer （线程安全String 适合修改、拼接String; synchronized 保证了线程安全）

- 数组(工具类为 Arrays )
reference:https://github.com/dunwu/javacore/blob/master/docs/basics/java-array.md

```
int[] temp = new int[20];
temp[3]=4;
System.out.println(temp[3]);
Arrays.sort(temp);
System.out.println(Arrays.binarySearch(temp, 4));
```

### 2.2 克隆(深拷贝，浅拷贝)

- 浅拷贝

```
复制被复制对象的引用，而不是基于值的复制。改变被复制对象内的值，会影响副本。
```

- 深拷贝

```
复制被复制对象的引用的真实值，而不是基于值的复制。被复制对象的改变，不影响副本。
```


- 克隆 (深拷贝clone()方法，实现 Cloneable 接口)

```
对于自己写的类，要实现 Cloneable 接口才能实现深拷贝。
```

##### 2.2.1 如何深拷贝对象：

1. 对于深拷贝对象的类，实现 Cloneable 接口。
2. 利用类序列化的方式进行。

> - json 
>   json 序列化容易丢失信息，比如 short 序列化后是数值，JSON 并没有 short 数据类型。
> - 实现 Serializable接口 配合 apache.commons 等第三方包

## 2.3 默认值

1. 函数中变量必须初始化后才能使用，类中变量可以不初始化后使用。
2. 在类变量中，变量的为类的默认值为 null; 属性基础数据类型为:false、0、0.0, \u0000；

```
public class T {
    public static void main(String[] args) {
        int a;
        // 报错，a没有被初始化。
        // System.out.println(a);
        So s = new So();
        System.out.println(s.getB());// 0
        System.out.println(s.getD());// 0.0
        System.out.println(s.getC());// ascii 0,就是ascii null
        System.out.println(s.getBo());// false
        System.out.println(s.getIn());// null
    }
}

class So {
    byte b;
    char c;
    double d;
    boolean bo;
    Integer In;

    public Integer getIn() {
        return In;
    }

    public boolean getBo() {
        return bo;
    }

    public double getD() {
        return d;
    }

    public char getC() {
        return c;
    }

    public byte getB() {
        return b;
    }
}
```





## 实例化，传参等问题，在类中总结，不在这总结