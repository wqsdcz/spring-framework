# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 提供在此仓库中工作的指导。

## 构建命令

本项目使用 Gradle 构建。所有构建操作使用 `./gradlew`（Windows 上使用 `gradlew.bat`）。

### 常用构建任务

```bash
# 构建整个项目（编译、测试、检查）
./gradlew build

# 运行所有测试
./gradlew test

# 运行所有检查，包括测试和 RuntimeHints 测试
./gradlew check

# 清理构建产物
./gradlew clean

# 发布到本地 Maven 仓库
./gradlew publishToMavenLocal
```

### 模块特定构建

```bash
# 构建特定模块（例如 spring-core）
./gradlew :spring-core:build

# 运行特定模块的测试
./gradlew :spring-core:test

# 仅编译特定模块
./gradlew :spring-core:compileJava
```

### 运行单个测试

```bash
# 运行特定测试类
./gradlew :spring-core:test --tests "org.springframework.core.io.support.PathMatchingResourcePatternResolverTests"

# 运行特定测试方法
./gradlew :spring-core:test --tests "org.springframework.core.io.support.PathMatchingResourcePatternResolverTests.testMethodName"
```

### 特殊测试类型

```bash
# 运行 RuntimeHints 测试（用于 GraalVM 原生镜像兼容性）
./gradlew runtimeHintsTest

# 运行集成测试
./gradlew :integration-tests:test

# 运行 JMH 微基准测试
./gradlew jmh
```

### 文档

```bash
# 生成参考文档（Antora）
./gradlew antora

# 查看生成的文档
open framework-docs/build/site/index.html
```

### IDE 设置

```bash
# 在导入 IDEA 前预编译 spring-oxm（由于依赖重新打包，必须执行）
./gradlew :spring-oxm:compileTestJava

# 生成 IDE 元数据
./gradlew idea
./gradlew eclipse
```

## 项目结构

Spring Framework 分为 24 个子项目：

**核心容器：**
- `spring-core` - 核心工具类、资源处理、类型转换
- `spring-beans` - Bean 工厂和依赖注入
- `spring-context` - 应用上下文、事件、组件扫描
- `spring-context-support` - 额外的上下文支持（缓存、调度）
- `spring-expression` - Spring 表达式语言（SpEL）
- `spring-aop` - 面向切面编程支持

**数据访问：**
- `spring-jdbc` - JDBC 抽象
- `spring-tx` - 事务管理
- `spring-orm` - JPA/Hibernate 集成
- `spring-r2dbc` - 响应式数据库连接

**Web：**
- `spring-web` - 核心 Web 支持（HTTP、客户端/服务端）
- `spring-webmvc` - 基于 Servlet 的 MVC 框架
- `spring-webflux` - 响应式 Web 框架
- `spring-websocket` - WebSocket 支持
- `spring-messaging` - 消息处理抽象

**测试：**
- `spring-test` - TestContext 框架和测试支持
- `spring-core-test` - 核心测试工具，包括 RuntimeHints 代理

**基础设施：**
- `framework-docs` - 参考文档（AsciiDoc/Antora）
- `integration-tests` - 跨模块集成测试

## 高层架构

### 依赖注入容器

核心 IoC 容器跨 `spring-core`、`spring-beans` 和 `spring-context` 实现：

- **BeanFactory** (`spring-beans`) - Bean 容器的根接口
- **ApplicationContext** (`spring-context`) - 添加事件传播、资源加载、国际化
- **BeanDefinition** - Bean 配置的元数据（类、作用域、属性等）
- **BeanPostProcessor** - 自定义 Bean 初始化逻辑的扩展点

关键源码位置：
- Bean 工厂：`spring-beans/src/main/java/org/springframework/beans/factory/`
- 应用上下文：`spring-context/src/main/java/org/springframework/context/support/`
- 注解配置：`spring-context/src/main/java/org/springframework/context/annotation/`

### 资源抽象

Spring 提供统一的 `Resource` 接口用于加载文件、类路径资源、URL：

- **Resource** - 文件/URL/类路径资源的抽象
- **ResourceLoader** - 加载资源的策略
- **PathMatchingResourcePatternResolver** - 支持 Ant 风格模式（例如 `classpath*:com/example/**/*.xml`）

关键源码：`spring-core/src/main/java/org/springframework/core/io/`

### 类型转换系统

- **Converter** SPI - 简单类型转换接口
- **ConversionService** - 转换器的注册和执行
- **PropertyEditor** - JavaBeans 属性编辑器支持（传统）
- **ResolvableType** - 泛型类型解析工具

关键源码：`spring-core/src/main/java/org/springframework/core/convert/`

### 注解处理

Spring 具有复杂的注解内省功能，支持组合注解和属性覆盖：

- **AnnotatedElementUtils** - 带合并的高级注解检索
- **AnnotationUtils** - 低级注解工具
- **@AliasFor** - 注解属性别名机制
- **MergedAnnotations** - 合并注解属性的不可变表示

关键源码：`spring-core/src/main/java/org/springframework/core/annotation/`

### AOP 基础设施

- **ProxyFactory** - 创建 AOP 代理（JDK 动态或 CGLIB）
- **Advisor** - 组合切入点和通知
- **@AspectJ 支持** - 注解驱动的切面
- **Load-time weaving** - AspectJ LTW 集成

关键源码：
- 核心 AOP：`spring-aop/src/main/java/org/springframework/aop/`
- AspectJ 集成：`spring-aspects/src/main/java/org/springframework/aspects/`

### Web 框架

**Spring MVC**（基于 Servlet）：
- **DispatcherServlet** - 前端控制器
- **@Controller/@RequestMapping** - 注解驱动的控制器
- **HandlerMapping/HandlerAdapter** - 请求路由和执行

**Spring WebFlux**（响应式）：
- **DispatcherHandler** - 响应式前端控制器
- **RouterFunction** - 函数式路由（注解的替代方案）
- **ReactiveAdapterRegistry** - 适配响应式类型（Reactor、RxJava）

关键源码：
- Spring MVC：`spring-webmvc/src/main/java/org/springframework/web/servlet/`
- WebFlux：`spring-webflux/src/main/java/org/springframework/web/reactive/`

### 提前编译（AOT）

用于 GraalVM 原生镜像支持：

- **RuntimeHints** - 声明反射、资源、代理、序列化需求
- **BeanFactoryInitializationAotProcessor** - 为 Bean 工厂贡献 AOT 处理
- **RuntimeHintsAgent** - Java 代理，用于在测试中检测运行时提示

关键源码：
- AOT 基础设施：`spring-core/src/main/java/org/springframework/aot/`
- 上下文 AOT：`spring-context/src/main/java/org/springframework/context/aot/`
- 测试代理：`spring-core-test/src/main/java/org/springframework/core/test/tools/`

## 测试方法

- **JUnit 5** (Jupiter) 是主要测试框架
- **Mockito** 用于模拟
- **AssertJ** 用于流式断言
- **@EnabledIfRuntimeHintsAgent** - 需要 RuntimeHints 代理时条件运行测试

测试通常与源代码一起组织在 `src/test/java` 中。

## 关键约定

### 代码风格

- 遵循你编辑的文件中现有的代码格式
- 使用制表符缩进（非空格）
- 最大行长度：120 个字符
- Checkstyle 配置位于 `buildSrc/config/`

### 提交消息

- 主题行最多 55 个字符
- 描述每行最多 72 个字符
- 引用 GitHub 问题：`Closes gh-12345`
- 所有提交必须包含 `Signed-off-by` 尾部（需要 DCO）

示例：
```
修复 jar:file: URL 的资源加载

此提交修复了 Windows 上 jar:file: URL 的资源
无法正确解析的问题。

Closes gh-12345

Signed-off-by: 你的名字 <email@example.com>
```

### 版本信息

- 当前版本：`6.2.16-SNAPSHOT`（定义在 `gradle.properties`）
- 基线 Java 版本：17
- 多版本 JAR 支持 Java 21 特性

## 重要构建注意事项

1. **导入 IDEA 前预编译 spring-oxm** - 由于依赖重新打包（XSD 模式），导入 IntelliJ IDEA 前必须运行 `./gradlew :spring-oxm:compileTestJava`。

2. **在 IDEA 中排除 spring-aspects** - `spring-aspects` 模块包含 IntelliJ 无法编译的 AspectJ 切面。在项目结构中排除此模块。

3. **Gradle 工具链** - 构建使用 Gradle 工具链自动下载和使用适当的 JDK 版本。使用 `./gradlew -q javaToolchains` 列出可用工具链。

4. **可选依赖** - 构建使用自定义的 `optional` 配置，用于应在编译类路径上但不传播给依赖者的依赖。

## 参考文档

- 参考文档源码：`framework-docs/modules/ROOT/pages/`
- Wiki 页面：https://github.com/spring-projects/spring-framework/wiki
- 从源码构建 wiki：https://github.com/spring-projects/spring-framework/wiki/Build-from-Source
- 代码风格 wiki：https://github.com/spring-projects/spring-framework/wiki/Code-Style
