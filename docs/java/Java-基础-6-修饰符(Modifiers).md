# Java-基础-6-修饰符(Modifiers)

- 访问修饰符

1. 访问修饰符

访问修饰符使用对象

| 访问修饰符号 | 类                      | 接口                      | 变量 | 函数 |
| ------------ | ----------------------- | :------------------------ | ---- | ---- |
| private      | N（只能修饰**内部**类） | N（只能修饰**内部**接口） | Y    | Y    |
| default      | Y                        | Y                         | Y    | Y    |
| protect      | N（只能修饰内部类）     | N（只能修饰内部接口）     | Y    | Y    |
| public       | Y                       | Y                         | Y    | Y    |



| 访问修饰符 | 自身类 | 包(同一包的定义是包的路径完全相同) | 子类                | 其他包 |
| ---------- | ------ | ---------------------------------- | ------------------- | ------ |
| private    | Y      | N                                  | N                   | N      |
| default    | Y      | Y                                  | Y(同包下才可以)     | N      |
| protect    | Y      | Y                                  | Y(只要是子类都可以) | N      |
| public     | Y      | Y                                  | Y                   | Y      |

- 其他修饰符

| 修饰符       | 类   | 接口 | 构造函数 | 方法 | 数据 |
| ------------ | ---- | ---- | -------- | ---- | ---- |
| abstract     | Y    | Y    | N        | Y    | N    |
| final        | Y    | N    | N        | Y    | Y    |
| native       | N    | N    | N        | Y    | N    |
| static       | N    | N    | N        | Y    | Y    |
| synchronized | N    | N    | N        | Y    | N    |
| transient    | N    | N    | N        | N    | Y    |
| volatile     | N    | N    | N        | N    | Y    |



- abstract 

  1. 抽象类

     ```
     public abstract class Bird {
         String birdName = "";
         int birdSpeed = 0;
         static String color = "white";
     
         public Bird() {
             this.birdName = "default";
         }
     
         public void setBirdName(String birdName) {
             this.birdName = birdName;
         }
     
         public static String printBird() {
             return color;
         }
     
         abstract public void setBirdSpeed(int speed);
     }
     
     
     public class BlueBird extends Bird{
         @Override
         public void setBirdSpeed(int speed) {
             this.birdSpeed = 2*speed;
         }
     }
     
     ```

     

     **作用：**

           抽象类不能用来实例化对象，抽象类中抽象方法(非必须),子类必须实现抽象类方法，约束了子类的行为。

     **注意**：

     > 类
     >
     >     抽象类可以不包含抽象方法，包含抽象方法的必定是抽象类。
     >
     > 变量
     >
     >     变量不能以 abstract 修饰。
     >
     > 方法
     >
     >          1. 构造函数，静态(static)方法无法声明为 abstract。
     >          2. 构造函数也不能声明为 abstract

     2. 抽象接口(抽象接口=接口；默认接口就是 abstract 修饰，并且有且仅有abstract 修饰。)

        ```
        public abstract interface BirdInterface {
            
            public static final String name="";
            String getBirdName();
        
            void setBirdName(String birdName);
        
            static void printSong() {
                System.out.println("Hello .");
            }
        
            default void printType() {
                System.out.println("I am a bird!");
            }
        
        }
        
        class BlueBirdBridImp implements BirdInterface {
            private String birdName = "";
        
            @Override
            public String getBirdName() {
                return this.birdName;
            }
        
            @Override
            public void setBirdName(String birdName) {
                this.birdName = birdName;
            }
        }
        ```

        **作用**:

             接口不可以实例化，必须通过类实现才可。与抽象类对比，接口强调的是实现约束，而不是实现细节。并且一个类可以实现多个接口。

        **注意**：

          >类
          >
          >    一个类可以实现多个接口，比如 有：跑接口，走接口，人类可以实现跑接口和走接口。
          >
          >变量
          >
          >        1. 接口(抽象接口)一般没有变量。
          >        2. 当接口声明了变量，都是默认带有 public static final 修饰的。
          >
          >方法
          >
          >        1. 接口方法一般带有 **public abstract** 修饰(接口方法默认修饰符)。
          >        2. java 1.8后接口引入静态代码、静态方法和default 修饰方法的支持(使得接口的改变，不再需要重写所有实现类）。
          >        3. java 1.9 又引入私有方法和私有静态方法支持。

- final

  修饰不可基础的类，不可变的变量。

- native

  非java 实现方法(外部实现方法)

- static

  > - 静态方法
  > -     静态方法中无法直接(this.)访问类非静态变量，外部使用时候，可以通过类实例，类指向该方法
  > - 静态变量
  > -    static 关键字用来声明独立于对象的静态变量，无论一个类实例化多少对象，它的静态变量只有一份拷贝。 静态变量也被称为类变量。局部变量不能被声明为 static 变量。

- synchronized/volatile

  > 控制并发使用的修饰符

- transient

> 声明transient的变量，实现Serilizable接口的时候不进行序列化。