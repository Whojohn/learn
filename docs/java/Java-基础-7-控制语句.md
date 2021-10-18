# Java-基础-7-控制语句

## 1. 条件语句(选择)

### 1.1 if 

语法：

> if(布尔条件表达式A){
>
> // 表达式A为true 时执行
>
> }else if(布尔条件表达式B){
>
> // 表达式B为true 时执行
>
> }else{
>
> // 最后执行该表达式
>
> }

### 1.2 switch

语法：

>switch (表达式[表达式只支持 byte short int char string enum]) {
>
>// 按照顺序执行
>
>case value(必须为常量):
>
>// 必须带有 break 不然继续执行下一条case 语句，直至遇到break
>
>break;
>
>case value:
>
>break;
>
>// 默认，可选
>
>default: 
>
>break;
>
>}

示例

```
enum Color {
        Red, Blue
    }

    public static void main(String[] args) {
        Color judge = Color.Blue;
        // 没有break后果
        switch (judge) {
            case Blue:
                System.out.println("missing break");
            case Red:
                System.out.println("execute after missing break");
        }

        String label = "break";
        switch (label) {
            case "break":
                System.out.println("break");
            case "non break":
                System.out.println("non break")
                ;
        }


    }
```

## 2. 循环语句

### 2.1 while

> 语法：
>
> while(expression){
>
> do something;
>
> }

### 2.2 do while

语法：

>// 与While 的区别是，先执行一次代码块，再进行条件判定
>
>do{
>
>do something;
>
>} while(expression)

### 2.3 for 循环

语法:

>//先执行初始化代码，然后判定表达式，然后执行操作；循环以上条件
>
>for(初始化；表达式；操作){
>
>do something
>
>}

### 2.4 foreach 循环

语法：

>for(Class loopName：Iterator instance){
>
>do something
>
>}

## 3. 流程控制

### 3.1 break

break 字段会停止执行代码块内的代码。

### 3.2 continue

contine 会跳过(不执行)本次调用该代码块。

### 3.3 return

跳出整个函数体，函数体后面的部分不再执行。

> ```
>public class GenericsTest {
>    public static void main(String[] args) {
>        for (int x : new int[]{1, 2, 3, 4, 5}) {
>            if (x == 3) {
>                return;
>            }
>            System.out.println(x);
>        }
>        System.out.println("Exit loop");
>    }
>}
>
>1
>2
> ```



