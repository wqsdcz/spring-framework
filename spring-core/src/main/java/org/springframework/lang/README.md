# org.springframework.lang 包介绍

概述
org.springframework.lang 包是 Spring Framework 中用于提供语言级语义注解的包，主要关注空值处理和编程约束。这个包位于 Spring 框架最低层级的包依赖结构中，甚至比 org.springframework.util 更底层，不包含任何 Spring 特定的概念。
主要注解及其功能
1. @NonNull 注解
   功能: 声明被注解的元素不能为 null
   使用场景:
   方法参数
   返回值
   字段
   特点:
   利用 JSR-305 元注解来向支持 JSR-305 的工具指示空值可空性
   被 Kotlin 用来推断 Spring API 的空值安全性
   可与 @NonNullApi 或 @NonNullFields 结合使用
2. @Nullable 注解
   功能: 声明被注解的元素在某些情况下可以为 null
   使用场景:
   方法参数
   返回值
   字段
   特点:
   与 @NonNullApi 或 @NonNullFields 结合使用时，可以覆盖默认的非空语义
   与 @NonNull 形成对比，提供精确的空值语义声明
3. @NonNullApi 注解
   功能: 声明在一个包级别上，参数和返回值默认应被视为非空
   使用场景:
   包级别声明（使用 package-info.java）
   特点:
   适用于整个包的参数和返回值
   与 @Nullable 注解结合使用，可以在特定地方覆盖默认的非空语义
   减少在代码中到处添加 @NonNull 注解的需要
4. @NonNullFields 注解
   功能: 声明在一个包级别上，字段默认应被视为非空
   使用场景:
   包级别声明（使用 package-info.java）
   特点:
   仅适用于字段
   与 @Nullable 注解结合使用，可以在特定字段上覆盖默认的非空语义
5. @Contract 注解
   功能: 指定方法行为的某些方面，取决于参数。可用于工具进行高级数据分析
   使用场景:
   方法级别
   特点:
   受 JetBrains @Contract 注解启发，但避免了额外依赖
   描述方法参数和返回值之间的因果关系
   支持复杂的合同规范语法，例如：
   "_, null -> null" - 当第二个参数为 null 时方法返回 null
   "true -> fail" - 当传入 true 时方法抛出异常
   "_ -> this" - 方法总是返回其调用对象本身
6. @CheckReturnValue 注解
   功能: 指定方法的返回值必须被使用
   使用场景:
   方法级别
   特点:
   受 JetBrains @CheckReturnValue 注解启发
   适用于返回值是方法主要目的的情况（而不是附加信息）
   帮助防止忽略重要返回值的编程错误