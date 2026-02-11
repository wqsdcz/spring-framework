# Spring Core 源码学习指南

## 推荐学习顺序

### 阶段 1: 基础工具类（1-2 周）

**从简单开始，理解 Spring 的基础设计**

| 主题 | 关键类 | 说明 |
|------|--------|------|
| 排序机制 | `Ordered`, `OrderComparator` | Spring 的排序基础，很多地方用到 |
| 异常体系 | `NestedRuntimeException` | Spring 的异常包装机制 |
| 泛型解析 | `ResolvableType` | 理解 Spring 如何处理 Java 泛型 |

**入门测试**: `OrderComparatorTests.java`

---

### 阶段 1.5: 核心根包其他重要类（1 周）

**这些类是 Spring 框架内部广泛使用的底层机制**

| 主题 | 关键类 | 说明 |
|------|--------|------|
| 属性访问器 | `AttributeAccessor` | 通用属性设置/获取接口，BeanDefinition 的基础 |
| 方法参数 | `MethodParameter` | 封装方法参数信息，支持泛型解析 |
| 参数名发现 | `ParameterNameDiscoverer` | 获取方法参数名称（反射/ASM/Kotlin） |
| 类型引用 | `ParameterizedTypeReference` | 保留泛型信息的类型引用（如 `new ParameterizedTypeReference<List<String>>(){}`） |
| 响应式适配 | `ReactiveAdapterRegistry` | 响应式编程适配器（Reactor/RxJava） |
| 全局属性 | `SpringProperties` | Spring 框架全局配置属性管理 |
| 方法解析 | `MethodIntrospector` | 方法选择策略（用于找 @Bean 方法等） |

**重点**: `MethodParameter` 在 Spring MVC、Spring Data 中被大量使用

---

### 阶段 2: 资源加载（1-2 周）

**理解 Spring 如何统一访问文件、类路径、URL**:

- 资源、资源的加载器、资源定位符（模糊匹配）、资源来源

| 主题 | 关键类 | 说明 |
|------|--------|------|
| 资源抽象 | `Resource`, `ResourceLoader` | 统一资源访问接口 |
| 默认实现 | `DefaultResourceLoader` | 资源加载的基础实现 |
| 模式匹配 | `PathMatchingResourcePatternResolver` | 支持 `classpath*:/**/*.xml` 这类通配符 |

**重点测试**: `PathMatchingResourcePatternResolverTests.java`

---

### 阶段 3: 环境配置（1-2 周）

**理解配置文件、系统属性、环境变量的管理**：

- 属性的访问、多属性源的管理、多环境配置的支持

| 主题 | 关键类 | 说明 |
|------|--------|------|
| 环境接口 | `Environment`, `PropertyResolver` | 统一属性访问 |
| 属性源 | `PropertySource`, `MutablePropertySources` | 多来源属性管理 |
| Profile | `Profiles` | 多环境配置支持 |

---

### 阶段 4: 类型转换系统（2-3 周）

**Spring 非常强大的类型转换机制**：

- 类型描述符、类型转化器（一对一）、类型转换服务（转化功能的集成）

| 主题 | 关键类 | 说明 |
|------|--------|------|
| 转换服务 | `ConversionService` | 类型转换的入口 |
| 转换器 | `Converter`, `GenericConverter` | 自定义转换逻辑 |
| 类型描述 | `TypeDescriptor` | 携带泛型信息的类型描述 |

**关键测试**: `DefaultConversionServiceTests.java`

---

### 阶段 5: 注解处理（2-3 周）

**Spring 最复杂的部分之一，支持组合注解、属性别名**

| 主题 | 关键类 | 说明 |
|------|--------|------|
| 注解工具 | `AnnotationUtils` | 基础注解处理 |
| 合并注解 | `MergedAnnotations`, `MergedAnnotation` | Spring 5.2+ 新 API |
| 元数据 | `AnnotationMetadata` | ASM 读取类元数据（不加载类） |
| 属性别名 | `@AliasFor` | 注解属性别名机制 |

**难点**: 这部分涉及大量反射和 ASM 字节码操作

---

### 阶段 6: 高级主题（选学）

| 主题 | 位置 | 说明 |
|------|------|------|
| 任务执行 | `core/task/` | 异步任务抽象 |
| 编解码 | `core/codec/` | 响应式编程支持 |
| AOT 支持 | `org/springframework/aot/` | GraalVM 原生镜像支持（Spring 6+） |

---

## 学习方法建议

1. **从测试入手** - `spring-core/src/test/java/` 下有 261 个测试类，每个都是很好的示例

2. **Debug 跟踪** - 运行测试时单步调试，理解执行流程

3. **画图辅助** - 对复杂流程（如注解合并、类型转换）画时序图

4. **对比版本** - 对比 Spring 4/5/6 的差异，理解演进思路

5. **动手实践** - 尝试写自定义的 `Converter`、`ResourceLoader`

---

## 源码位置速查

```
spring-core/src/main/java/org/springframework/core/
├── annotation/     # 注解处理
├── convert/        # 类型转换
├── env/            # 环境配置
├── io/             # 资源加载
│   └── support/    # ResourcePatternResolver, SpringFactoriesLoader
├── task/           # 任务执行
├── type/           # 类元数据
├── codec/          # 编解码
├── serializer/     # 序列化
├── style/          # 格式化
├── log/            # 日志
├── metrics/        # 启动指标
└── 根包            # Ordered, ResolvableType, MethodParameter 等

spring-core/src/main/java/org/springframework/util/
├── StringUtils.java           # 字符串工具
├── ClassUtils.java            # 类操作工具
├── ReflectionUtils.java       # 反射工具
├── CollectionUtils.java       # 集合工具
├── Assert.java                # 断言工具
├── AntPathMatcher.java        # 路径匹配
├── ResourceUtils.java         # 资源工具
├── StopWatch.java             # 性能计时
├── LinkedMultiValueMap.java   # 多值 Map
└── ...                        # 更多工具类

spring-core/src/main/java/org/springframework/
├── lang/           # @Nullable, @NonNull 注解
├── aot/            # AOT 支持（GraalVM）
├── asm/            # 嵌入式 ASM
├── cglib/          # 嵌入式 CGLIB
└── objenesis/      # 嵌入式 Objenesis
```

**推荐开始阅读的第一个文件**: `spring-core/src/main/java/org/springframework/core/Ordered.java`

这个接口只有十几行代码，但它是理解 Spring 排序机制（Bean 加载顺序、AOP 拦截器顺序等）的基础。

---

## 补充：util 工具包（必读）

`org.springframework.util` 包含大量高频使用的工具类，**这是日常开发和使用 Spring 时最常接触的部分**。

### 核心工具类分类

| 类别 | 关键类 | 说明 |
|------|--------|------|
| **字符串处理** | `StringUtils` | 最常用工具：判空、分割、合并、trim |
| **类操作** | `ClassUtils` | 类加载、接口判断、CGLIB 代理类处理 |
| **反射** | `ReflectionUtils` | 字段/方法访问、回调操作 |
| **集合** | `CollectionUtils` | 集合判空、合并、转换 |
| **断言** | `Assert` | 参数校验（Spring 内部大量使用） |
| **资源** | `ResourceUtils` | 资源路径解析（classpath:, file: 等） |
| **路径匹配** | `AntPathMatcher` | Ant 风格路径匹配（`/**/ *.xml`） |
| **占位符** | `PlaceholderParser` | 解析 `${...}` 占位符 |
| **文件操作** | `FileCopyUtils`, `StreamUtils` | IO 流和文件工具 |
| **性能** | `StopWatch` | 代码执行时间测量 |
| **Map** | `LinkedMultiValueMap` | 一个 key 对应多个 value 的 Map |
| **Map** | `LinkedCaseInsensitiveMap` | 大小写不敏感的 Map |

### 学习建议

1. 先通读 `StringUtils` - 几乎所有 Spring 类都用到它
2. 了解 `Assert` 的使用模式 - 学习如何写出健壮的参数校验
3. `AntPathMatcher` 是 Spring MVC 路径匹配的核心

---

## 补充：io.support 中的重要类

| 关键类 | 说明 |
|--------|------|
| `SpringFactoriesLoader` | **Spring SPI 机制**，从 `META-INF/spring.factories` 加载配置（Spring Boot 自动配置的核心） |
| `PropertiesLoaderUtils` | 属性文件加载工具 |
| `EncodedResource` | 带编码（UTF-8）的资源包装 |

**重点**: `SpringFactoriesLoader` 是理解 Spring Boot 自动配置的必学内容

---

## 补充：其他重要包

### serializer - 序列化支持
- `Serializer` / `Deserializer` - 序列化接口
- `DefaultSerializer` / `DefaultDeserializer` - 基于 Java 序列化的默认实现

### style - 格式化输出
- `ToStringCreator` - 简化 toString() 方法编写

### log - 日志支持
- `LogMessage` - 延迟计算的日志消息（性能优化）

### metrics - 启动指标（Spring 5.3+）
- `ApplicationStartup` - 应用启动指标收集
- `StartupStep` - 启动步骤记录

---

## 补充：嵌入式第三方库

Spring-Core 内嵌了以下库的源码，避免外部依赖：

| 包名 | 说明 | 用途 |
|------|------|------|
| `org.springframework.asm` | ASM 字节码库 | 类扫描、注解读取（不加载类） |
| `org.springframework.cglib` | CGLIB 代理库 | 生成代理类、Bean 增强 |
| `org.springframework.objenesis` | 对象实例化库 | 绕过构造函数创建对象 |

**学习建议**: 了解它们的作用即可，除非你要深入研究代理或字节码操作。
