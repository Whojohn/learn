# Java-基础-8-异常处理

## 1. Throwable 类

Throwable 有两大子类：Exception和Error；对应一般异常和运行错误(一般不捕获运行错误)。在 Java 中只有 `Throwable` 类型的实例才可以被抛出（`throw`）或者捕获（`catch`），它是异常处理机制的基本组成类型。

### 1.1 Throwable

`Throwable` 包含了其线程创建时线程执行堆栈的快照，它提供了 `printStackTrace()` 等接口用于获取堆栈跟踪数据等信息。

主要方法：

- `fillInStackTrace` - 用当前的调用栈层次填充 `Throwable` 对象栈层次，添加到栈层次任何先前信息中。
- `getMessage` - 返回关于发生的异常的详细信息。这个消息在 `Throwable` 类的构造函数中初始化了。
- `getCause` - 返回一个 `Throwable` 对象代表异常原因。
- `getStackTrace` - 返回一个包含堆栈层次的数组。下标为 0 的元素代表栈顶，最后一个元素代表方法调用堆栈的栈底。
- `printStackTrace` - 打印 `toString()` 结果和栈层次到 `System.err`，即错误输出流。
- `toString` - 使用 `getMessage` 的结果返回代表 `Throwable` 对象的字符串。

### 1.2 Exception

### 1.3 Error

### 1.4 RuntimeException

> RuntimeException 继承于 Exception ，与 Exception 不一样的是，编译器不会检查是否捕获了 RuntimeException 及其子类。

## 2 抛出&异常捕获

- 抛出异常

  ```
  throw new Exception("抛出一个异常");
  ```

- 异常捕获

  ```
  try{
  do something
  }catch (Exception1 e){
  do something
  }catch (Exception2 e)
  {
  do something
  }finally{
  do something
  }
  ```

  代码样例 

  ```
  class BasicException extends Exception {
  
      public BasicException() {
          super("BasicException throw");
      }
  
      public BasicException(String msg) {
          super(msg);
      }
  
  }
  
  class ChildException extends BasicException {
  
  
      @Override
      public String getMessage() {
          /**
           *  除非要覆盖、改写父类信息才需要重写 getMessage 方法;
           */
          System.out.println(super.getMessage());
          return "ChildException throw";
      }
  }
  
  public class ArrayTest {
  
  
      public static void main(String[] args) throws Exception {
  //        throw new BasicException();
  //        Exception in thread "main" BasicException: BasicException throw
  //                at ArrayTest.main(ArrayTest.java:29)
  
  
  //        throw new BasicException("basic");
  //        Exception in thread "main" BasicException: basic
  //        at ArrayTest.main(ArrayTest.java:33)
  
          // 父子类异常同时出现在异常中，优先按照顺序捕获子类
          try {
              throw new ChildException();
          } catch (ChildException e2) {
              // 注意必须是父类在后，不然编译报错
              System.out.println("ChildException catch");
          } catch (BasicException e) {
              System.out.println("BasicException catch");
          }
          //ChildException catch
      }
  }
  
  
  ```

- 反射对异常捕获的影响

> 反射中的异常继承 ReflectiveOperationException 类
>
> 反射中的异常，由于反射包裹了一层堆栈，因此要获取准确的异常信息应该利用getTargetException获取真实的异常信息。

- 多线程与异常

> 假如子线程不捕获异常，会导致该线程退出。

## 3 异常链(深层异常抛出，反射包裹)

## 

## 4. 小建议

- 对可恢复的情况使用检查性异常（Exception），对编程错误使用运行时异常（RuntimeException）。
- 优先使用 Java 标准的异常。
- 抛出与抽象相对应的异常。
- 在细节消息中包含能捕获失败的信息。
- 尽可能减少 try 代码块的大小。
- 尽量缩小异常范围。例如，如果明知尝试捕获的是一个 `ArithmeticException`，就应该 `catch` `ArithmeticException`，而不是 `catch` 范围较大的 `RuntimeException`，甚至是 `Exception`。
- 尽量不要在 `finally` 块抛出异常或者返回值。
- 不要忽略异常，一旦捕获异常，就应该处理，而非丢弃。
- 异常处理效率很低，所以不要用异常进行业务逻辑处理。
- 各类异常必须要有单独的日志记录，将异常分级，分类管理，因为有的时候仅仅想给第三方运维看到逻辑异常，而不是更细节的信息。
- 如何对异常进行分类：
  - 逻辑异常，这类异常用于描述业务无法按照预期的情况处理下去，属于用户制造的意外。
  - 代码错误，这类异常用于描述开发的代码错误，例如 NPE，ILLARG，都属于程序员制造的 BUG。
  - 专有异常，多用于特定业务场景，用于描述指定作业出现意外情况无法预先处理。