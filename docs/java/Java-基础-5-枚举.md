 # Java-基础-5-枚举

## 1. 枚举声明

```
// [] 表示 [] 里内容可选
[public(枚举只允许默认、public两种修饰符)] enum EnumClassName [implements interface] {
@overwrite
String record(){

}
{
枚举常量[],枚举常量,枚举常量;

// 类变量
[[修饰符] 数据类型 变量名;]
// 构造函数
[public] EnumClassName(){}
// 函数方法

abstract String record()
}
```

- 代码示例

```
package 枚举;

import java.util.Arrays;

enum Alarm {
    RED(500) {
        public String alarm(String head) {
            return this.getCode().toString() + "   " + "Cluster has shard all replace down.";
        }
    },
    YELLOW(200, "Cluster has shard not all replace ready.") {
        public String alarm(String head) {
            return this.getCode() + "   " + this.getMsg();
        }
    },
    GREEN(200, "Cluster is health.") {
        @Override
        public String alarm(String head) {
            return this.getCode().toString();
        }
    };
    private final Integer code;
    private String msg;

    // 构造函数
    Alarm(int number) {
        this.code = number;
    }

    // 构造函数
    Alarm(int number, String msg) {
        this.code = number;
        this.msg = msg;
    }

    // 方法
    public Integer getCode() {
        return code;
    }

    // 方法
    public String getMsg() {
        return msg;
    }

    abstract public String alarm(String head);

    public static void main(String[] args) {
        Arrays.stream(Alarm.values()).forEach(e -> System.out.println(e.alarm("")));
    }
}

```

- 使用场景

  1. 如上错误码定义，异常信息。

  2. swith 中使用(较少)。

     > switch 中支持int`、`char`、`String`、`enum 几种数据类型

  3. 单例、策略。

     > 单例是利用了static 和 jvm 对于 enum 的特殊处理实现了饿汉加载。(利用的是staic 修饰的变量只会jvm初始化一次的特点。)
     >
     > 策略模式则是利用了单例的构造函数特性，实现了特定的方法。

  

## 2. 枚举底层

reference:

1. https://github.com/dunwu/javacore/blob/master/docs/basics/java-enum.md   

  

枚举其实是一个继承 Enum 类的**静态类**;

> 因此枚举不能被继承。**特殊的也不能继承其他类**，但可以实现接口。

```
public abstract class Enum<E extends Enum<E>>
        implements Comparable<E>, Serializable { ... }
```

新建一个 ColorEn.java 文件，内容如下：

```
package io.github.dunwu.javacore.enumeration;

public enum ColorEn {
    RED,YELLOW,BLUE
}
```

执行 `javac ColorEn.java` 命令，生成 ColorEn.class 文件。

然后执行 `javap ColorEn.class` 命令，输出如下内容：

```
Compiled from "ColorEn.java"
public final class io.github.dunwu.javacore.enumeration.ColorEn extends java.lang.Enum<io.github.dunwu.javacore.enumeration.ColorEn> {
  public static final io.github.dunwu.javacore.enumeration.ColorEn RED;
  public static final io.github.dunwu.javacore.enumeration.ColorEn YELLOW;
  public static final io.github.dunwu.javacore.enumeration.ColorEn BLUE;
  public static io.github.dunwu.javacore.enumeration.ColorEn[] values();
  public static io.github.dunwu.javacore.enumeration.ColorEn valueOf(java.lang.String);
  static {};
}
```



