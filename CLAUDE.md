# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 提供在本仓库中工作时的指导。

## 构建命令

本项目基于 Gradle 构建。使用 Gradle Wrapper（`./gradlew` 或 Windows 上的 `gradlew.bat`）进行所有构建。

**构建项目：**
- `./gradlew build` - 构建整个项目（包括测试）
- `./gradlew assemble` - 构建但不运行测试
- `./gradlew clean build` - 清理并重新构建
- `./gradlew :spring-core:build` - 构建指定模块

**运行测试：**
- `./gradlew test` - 运行所有测试
- `./gradlew :spring-core:test` - 运行指定模块的测试
- `./gradlew test --tests "org.springframework.core.io.ResourceTests"` - 运行单个测试类
- `./gradlew test --tests "*Resource*"` - 运行匹配模式的测试
- `./gradlew check` - 运行所有检查（测试、代码风格检查等）
- `./gradlew check antora` - 完整 CI 构建（CI 中使用）

**测试工具链（Java 版本）：**
- `./gradlew check -PtestToolchain=21` - 使用 Java 21 运行测试
- `./gradlew check -PtestToolchain=25` - 使用 Java 25（抢先体验版）运行测试
- `./gradlew java21Test` - 运行 Java 21 特定测试
- `./gradlew runtimeHintsTest` - 运行 GraalVM 原生镜像提示测试

**IDE 集成：**
- `./gradlew eclipse` - 生成 Eclipse 项目文件
- `./gradlew idea` - 生成 IntelliJ IDEA 项目文件
- 导入 IDE 前预编译：`./gradlew :spring-oxm:compileTestJava :spring-core:compileJava`

**生成文档：**
- `./gradlew javadoc` - 生成 Javadoc
- `./gradlew antora` - 生成 Antora 文档（输出位置：`framework-docs/build/site/index.html`）
- `./gradlew dokkaHtml` - 生成 Kotlin 文档

**发布：**
- `./gradlew -PdeploymentRepository=$(pwd)/deployment-repository build publishAllPublicationsToDeploymentRepository` - 发布到本地仓库

## 项目架构

**模块依赖关系（自下而上）：**
```
spring-core → spring-beans → spring-context → web/data 模块
```

**核心层：**
- `spring-core` - 基础工具类，`ResolvableType`，`CollectionFactory`，类型转换基础设施
- `spring-beans` - Bean 容器：`BeanFactory`，`ListableBeanFactory`，`FactoryBean`，属性编辑器
- `spring-context` - 应用运行时：`ApplicationContext`，事件传播，资源加载，`MessageSource`

**Web 层（双栈架构）：**
- `spring-web` - 公共基础设施：`WebApplicationInitializer`，HTTP 消息转换器
- `spring-webmvc` - 基于 Servlet 的 MVC：`DispatcherServlet`，`HandlerMapping`，`HandlerAdapter`，`ViewResolver`
- `spring-webflux` - 响应式 Web：`DispatcherHandler`，与 MVC 并行的架构，基于 Project Reactor 构建

**AOP 层：**
- `spring-aop` - 基于代理的 AOP：`Pointcut`（由 `ClassFilter` + `MethodMatcher` 组成），`Advisor`，`Advice`，`AopProxy`（JDK/CGLIB）
- `spring-aspects` - AspectJ 集成：加载时织入，支持 `@AspectJ` 注解

**数据访问层：**
- `spring-jdbc` - `JdbcTemplate`，`DataSourceUtils`，异常转换为 `DataAccessException`
- `spring-tx` - 事务抽象：`PlatformTransactionManager`，`TransactionDefinition`，`TransactionStatus`
- `spring-orm` - ORM 集成（Hibernate、JPA），使用相同的事务抽象

**关键设计模式：**
- 工厂模式：`BeanFactory`，`FactoryBean`，`ProxyFactoryBean`
- 策略模式：`HandlerMapping`，`TransactionManager`，`ViewResolver`
- 模板模式：`JdbcTemplate`，`AbstractPlatformTransactionManager`
- 代理模式：Spring AOP，事务代理
- 基于接口的编程：所有抽象都通过接口定义

**测试约定：**
- JUnit 5 (Jupiter) + Platform Suite
- 测试在 headless AWT 模式下运行，启用 Netty 严格泄漏检测
- JVM 参数包含：`--add-opens=java.base/java.lang=ALL-UNNAMED`
- CI 中启用测试重试（不稳定测试重试 3 次）

## 构建配置

**Java 版本：**
- 最低要求：Java 17（BellSoft Liberica 发行版）
- 测试工具链：Java 17、21、25
- Kotlin：1.9.25（目标 JVM 17）

**构建文件：**
- `build.gradle` - 根构建配置
- `settings.gradle` - 模块定义（21 个模块）
- `gradle/spring-module.gradle` - spring-* 模块的标准配置
- `gradle/toolchains.gradle` - JVM 工具链配置
- `buildSrc/src/main/java/org/springframework/build/` - 构建约定插件

**关键 Gradle 属性：**
- 版本定义在 `gradle.properties` 中
- 默认启用 Gradle Daemon、并行构建和构建缓存
- 内存设置：`-Xmx2048m`

## 贡献指南

来自 `CONTRIBUTING.md`：
- 向 `main` 分支提交 Pull Request（视情况考虑向后移植）
- 所有提交必须包含 Signed-off-by 签名（DCO）
- 提交信息格式：主题 55 个字符，描述每行 72 个字符
- 使用 `Closes gh-XXXX` 引用 Issue
- 代码风格定义在 wiki 中："Code Style" 和 "IntelliJ IDEA Editor Settings"

## 重要注意事项

- 某些模块需要在导入 IDE 前预编译，因为有生成的源代码（spring-oxm 使用 XJC）
- spring-aspects 使用 AspectJ，可能导致 IDE 问题；可能需要在项目结构中排除
- 为主源代码启用 NullAway 静态分析（空安全检查）
- 每个模块都包含 JMH 微基准测试（使用 `./gradlew jmh` 运行）
