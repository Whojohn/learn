# Java-基础-3-运算符(操作符)

## 1. 算术运算符

- + -*/

- % 

- ++ --

  > 对于++，-- 自增运算符， i++先赋值在运算，++i先运算在赋值
  >
  > 如：
  >
  > ```
  > int i = 0;
  > i = ++i;
  > System.out.println("i=" + i);//i=1
  > i = 0;
  > i = i++;
  > System.out.println("i=" + i);//i=0
  > ```
  >
  > 

## 2.关系运算符

- ==

- !=
- \>
- <
- \>=
- <=

## 3. 位操作符


&、|、 ^、~、<<、\>>、>>>

## 4. 逻辑操作符号

- && || ！

  > && vs & (|| vs |)
  >
  > &&：第一个表达式为 false，则不再计算第二个表达式
  >
  > &：执行所有表达式
  >
  > ```
  > Test test = new Test();
  > if (test.a != null && test.a > -1) {
  >  System.out.println();
  > }
  > System.out.println("Pass rule:test != null &&");
  > if (test.a != null & test.a > -1) {
  >  System.out.println();
  > }
  > 
  > 
  > 输出：
  > Pass rule:test != null &&
  > Exception in thread "main" java.lang.NullPointerException
  > 	at Test.main(Test.java:10)
  > 
  > Process finished with exit code 1
  > ```
  >
  > 

## 5. 赋值操作符

= 、+= 、-=  、*=、/=、%= 、<<= 、 >>= 、 & = 、|=



