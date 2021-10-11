# Flink-3-开发基础

   Flink 开发基础涉及知识点： SPI 加载，flink 打包配置，依赖冲突解决，本地与集群调试不同，windows 调试， Flink 中 hadoop 依赖。



# 1.前置知识

- spi 类加载

  java 通过 spi 提供动态加载类， spi 需要接口的特定声明+策略工厂+配置文件(resource/META-INF.services 下文件) 三者。

- maven scope

1. compile

   > 打包包含依赖，默认配置。

2. test

> 依赖仅仅用于测试，一般打包不包含，可选。

3. runntime

> 打包不包含依赖，一般用于 SPI 等动态加载类本地调试。

4. provided

> 打包不包含依赖。

5. system

> 打包不包含依赖.

- maven 包默认同名包处理方式

1. 最短路径优先
> Maven 面对 D1 和 D2 时，会默认选择最短路径的那个 jar 包，即 D2。E->F->D2 比 A->B->C->D1 路径短 1。
2. 最先声明优先
> 假如两个路径长度一致，选择最先声明的路径。A->B->C1, E->F->C2 ，两个依赖路径长度都是 2，那么就选择最先声明。

- maven 冲突解决工具(maven helper)
- maven 版本冲突解决方法
1. exclude 依赖(较麻烦)
2. 版本锁定（如下）
````

<dependencyManagement>
    <dependency>
        <groupId>org.apache.curator</groupId>
        <artifactId>curator-client</artifactId>
        <version>4.0.1</version>
    </dependency></dependencyManagement>
```
3. maven shade 重新打包源码



## 2. SPI 相关 & 打 fat jar。

   目标：

1.  解决 SPI 文件 maven 不能追加问题。
2.  打包打出fat jar 。

​    **Flink connector 的加载是通过 SPI 实现的(jdbc 等加载也是通过 SPI 实现的)，默认maven 打包机制，会覆盖掉 SPI 声明文件 **。即： resource/META-INF.services 只能保留同一个工厂方法的声明文件。如： kafka , es ,hive 等同时使用，打包后只能使用一个连接器，其他连接器类找不到。**解决方法是利用 maven 的 org.apache.maven.plugins.shade.resource.AppendingTransformer**。

```
    <build>
        <plugins>
            <plugin>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.2.4</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <transformers>
                        <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                            <resource>src/main/resources/META-INF/services/org.apache.flink.table.factories.Factory
                            </resource>
                        </transformer>
                        <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                            <resource>META-INF/services/org.apache.flink.table.factories.Factory
                            </resource>
                        </transformer>
                        <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                            <resource>services/org.apache.flink.table.factories.Factory
                            </resource>
                        </transformer>
                    </transformers>
                    <shadedArtifactAttached>true</shadedArtifactAttached>
                    <shadedClassifierName>jar-with-dependencies</shadedClassifierName>
                </configuration>
            </plugin>

        </plugins>

    </build>
```



# 3. Flink 本地调试

目标：

1.  本地 web ui，可选 rest 地址。
2.  idea 输出日志，可选 debug 日志。

​       Flink 能够自动判定环境，启动对应的集群。本地使用的时候 minicluster 模式，本地启动需要以下这些依赖，才能显示 Flink web ui，debug日志。**注意idea启动时候，必须勾选 include dependence with "Provided" scope。**

```
        <!-- 不能去掉，去掉本地测试无法启动，旧版官网1.10以前是有这个依赖，新版没了，没了后果就是无法本地运行，通过provided控制不打包-->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-clients_${scala.binary.version}</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- 本地测试配置，将会显示flink 框架日志-->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>1.7.25</version>
            <scope>provided</scope>
        </dependency>
        <!-- 本地测试webui, 不指定启动环境为env时，输出日志Web frontend 会打印web ui 地址(非8081端口)；-->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-runtime-web_${scala.binary.version}</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>
```

- debug 需要在jvm 配置引入（或者配置日志配置）

> -Dorg.slf4j.simpleLogger.defaultLogLevel=debug

-  固定Web 接口(不固定需要从日志找到接口)

> conf.setInteger(RestOptions.PORT, 8082);



# 4 Windows 调试

​      windows 调试最大问题是需要解决 hadoop 依赖(简单使用，hadoop client 即可，涉及到 hive 多集群读写等需要配置windows hadoop依赖)，hadoop native 依赖。

- windows 配置hadoop 依赖(略)
- flink 集群不依赖 hadoop_home 加载 hadoop native 方法

> 涉及源码org.apache.hadoop.util NativeCodeLoader 方法

```
public final class NativeCodeLoader {

  private static final Logger LOG =
      LoggerFactory.getLogger(NativeCodeLoader.class);
  
  private static boolean nativeCodeLoaded = false;
  
  static {
    // Try to load native hadoop library and set fallback flag appropriately
    if(LOG.isDebugEnabled()) {
      LOG.debug("Trying to load the custom-built native-hadoop library...");
    }
    try {
      System.loadLibrary("hadoop");
      LOG.debug("Loaded the native-hadoop library");
      nativeCodeLoaded = true;
    } catch (Throwable t) {
      // Ignore failure to load
      if(LOG.isDebugEnabled()) {
        LOG.debug("Failed to load native-hadoop with error: " + t);
        LOG.debug("java.library.path=" +
            System.getProperty("java.library.path"));
      }
    }
    
    if (!nativeCodeLoaded) {
      LOG.warn("Unable to load native-hadoop library for your platform... " +
               "using builtin-java classes where applicable");
    }
  }
```

​     **System.loadLibrary("hadoop")，这里只有检查，该类没有任何初始化该值的方法。并且，该读取方法限制必须在jvm启动前把 native 相关路径记载到jvm 环境中。**

方法1：修改 taskmanger ，jobmanager 启动脚本-Djava.library.path=xxx(hadoop native 地址)

方法2：系统变量 export LD_LIBRARY_PATH=xxx(hadoop native 地址)



# 5. 总结

- maven 依赖

```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>learn</artifactId>
        <groupId>org.example</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>flinklearn</artifactId>

    <properties>
        <flink.version>1.12.2</flink.version>
        <java.version>1.8</java.version>
        <flink.version>1.12.2</flink.version>
        <scala.binary.version>2.12</scala.binary.version>
        <kafka.version>2.4.1</kafka.version>
        <hadoop.version>3.1.3</hadoop.version>
    </properties>
    <build>
        <plugins>
            <plugin>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.2.4</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <transformers>
                        <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                            <resource>src/main/resources/META-INF/services/org.apache.flink.table.factories.Factory
                            </resource>
                        </transformer>
                        <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                            <resource>META-INF/services/org.apache.flink.table.factories.Factory
                            </resource>
                        </transformer>
                        <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                            <resource>services/org.apache.flink.table.factories.Factory
                            </resource>
                        </transformer>
                    </transformers>
                    <shadedArtifactAttached>true</shadedArtifactAttached>
                    <shadedClassifierName>jar-with-dependencies</shadedClassifierName>
                </configuration>
            </plugin>

        </plugins>

    </build>


    <dependencies>
        <!-- flink table  -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-table-api-java-bridge_${scala.binary.version}</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-streaming-java_${scala.binary.version}</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- 不能去掉，去掉本地测试无法启动，旧版官网是有这个依赖，新版没了，没了后果就是无法本地运行，通过provided控制不打包-->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-clients_${scala.binary.version}</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>


        <!-- flink json format -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-json</artifactId>
            <version>${flink.version}</version>
        </dependency>


        <!-- 测试依赖包       -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.12</version>
        </dependency>

        <dependency>
            <groupId>org.powermock</groupId>
            <artifactId>powermock-module-junit4</artifactId>
            <version>2.0.7</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.powermock</groupId>
            <artifactId>powermock-api-mockito2</artifactId>
            <version>2.0.7</version>
            <scope>test</scope>
        </dependency>

        <!-- 开发配置       -->
        <!-- flink table for ide -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-table-planner_${scala.binary.version}</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>
        <!-- flink table for ide -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-table-planner-blink_${scala.binary.version}</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>
        <!-- 本地测试配置，将会显示flink 框架日志-->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>1.7.25</version>
            <scope>provided</scope>
        </dependency>
        <!-- 本地测试webui, 不指定启动环境为env时，输出日志Web frontend 会打印web ui 地址(非8081端口)；-->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-runtime-web_${scala.binary.version}</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>org.apache.hadoop</groupId>
            <artifactId>hadoop-client</artifactId>
            <version>${hadoop.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

