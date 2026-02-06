# Spring-Core 项目结构介绍

Spring-Core 是整个 Spring Framework 的**基石模块**，位于 `spring-core/` 目录下。它提供了框架运行所需的核心工具和基础组件，设计上**仅依赖日志模块**（spring-jcl），不依赖其他任何 Spring 模块。

## 目录结构总览

```
spring-core/
├── src/main/java/org/springframework/
│   ├── core/           # 核心框架基础功能（核心）
│   ├── util/           # 通用工具类
│   ├── lang/           # 语言级注解（@Nullable, @NonNull等）
│   ├── aot/            # AOT 预先编译支持（GraalVM）
│   ├── asm/            # 嵌入式 ASM 字节码操作
│   ├── cglib/          # 嵌入式 CGLIB 代理生成
│   ├── objenesis/      # 对象实例化工具
│   └── javapoet/       # Java 代码生成
├── src/test/java/      # 测试代码
└── src/testFixtures/   # 测试夹具
```

## 核心包详解（org.springframework.core）

| 包名 | 核心功能 | 关键类 |
|------|---------|--------|
| `core.annotation` | 注解处理框架 | `AnnotationUtils`, `MergedAnnotations`, `@Order` |
| `core.io` | 资源 I/O 抽象 | `Resource`, `ResourceLoader`, `PathMatchingResourcePatternResolver` |
| `core.convert` | 类型转换系统 | `ConversionService`, `Converter`, `TypeDescriptor` |
| `core.env` | 环境配置管理 | `Environment`, `PropertySource`, `Profiles` |
| `core.task` | 任务执行抽象 | `TaskExecutor`, `AsyncTaskExecutor` |
| `core.type` | 类型内省 | `AnnotationMetadata`, `ClassMetadata` |
| `core.codec` | 编解码器 | `Encoder`, `Decoder` |
| `core.serializer` | 序列化 | `Serializer`, `Deserializer` |

## 常用工具包（org.springframework.util）

这个包包含大量高频使用的工具类：

- **`StringUtils`** - 字符串处理（最常用）
- **`ClassUtils`** - 类加载和检查
- **`ReflectionUtils`** - 反射操作
- **`CollectionUtils`** - 集合工具
- **`Assert`** - 参数校验
- **`AntPathMatcher`** - Ant 风格路径匹配

## 嵌入式第三方库

Spring-Core 内嵌了以下库的源码（避免外部依赖）：

1. **ASM** - 字节码操作（类扫描、注读取）
2. **CGLIB** - 代理类生成
3. **Objenesis** - 绕过构造函数实例化对象

## 关键抽象接口

Spring-Core 的核心设计是**面向接口编程**，提供多个策略抽象：

| 接口 | 作用 |
|------|------|
| `Resource` | 统一访问类路径、文件系统、URL 等资源 |
| `ResourceLoader` | 资源加载策略 |
| `ConversionService` | 运行时类型转换 |
| `Environment` | 管理 profiles 和 properties |
| `TaskExecutor` | 任务执行（同步/异步） |

## 统计数据

- **主代码**: 729 个 Java 文件
- **核心包**: 338 个 Java 文件
- **测试代码**: 300 个 Java 文件

## 模块依赖关系

```
spring-core (基石)
    ↑
spring-beans → spring-context → 其他模块
```

Spring-Core 作为最底层模块，**被所有其他 Spring 模块依赖**，是整个 Spring 生态系统的技术基础。

---

## 详细功能说明

### 1. annotation - 注解处理框架

提供 Spring 的注解处理基础设施，支持元注解、合并注解和属性覆盖。

**核心类**:
- `AnnotationUtils` - 注解工具类，提供注解检索和处理功能
- `AnnotatedElementUtils` - 用于处理注解元素的实用工具
- `MergedAnnotations` - 支持属性覆盖的合并注解
- `AnnotationAttributes` - 注解属性映射
- `Order` / `OrderUtils` - 排序注解支持
- `AliasFor` - 注解属性别名

### 2. io - 资源输入/输出抽象

统一抽象各种底层资源（文件、类路径、URL 等），提供一致的资源访问 API。

**核心类**:
- `Resource` - 资源描述符接口（核心抽象）
- `ResourceLoader` - 资源加载策略接口
- `DefaultResourceLoader` - 默认资源加载实现
- `ClassPathResource` - 类路径资源实现
- `FileSystemResource` - 文件系统资源实现
- `UrlResource` - URL 资源实现
- `ByteArrayResource` - 字节数组资源
- `InputStreamResource` - 输入流资源
- `WritableResource` - 可写资源接口
- `PathMatchingResourcePatternResolver` - Ant 风格路径匹配资源解析器

### 3. convert - 类型转换系统

提供强大的类型转换 SPI，支持运行时类型转换和格式化。

**核心类**:
- `ConversionService` - 类型转换服务接口
- `DefaultConversionService` - 默认转换服务实现
- `GenericConversionService` - 通用转换服务
- `TypeDescriptor` - 类型描述符

### 4. env - 环境抽象和属性管理

管理应用运行环境，包括 profiles（开发、测试、生产）和 properties（配置属性）。

**核心类**:
- `Environment` - 环境接口（profiles + properties）
- `ConfigurableEnvironment` - 可配置环境
- `StandardEnvironment` - 标准环境实现
- `PropertyResolver` - 属性解析器
- `PropertySource` - 属性源抽象
- `MutablePropertySources` - 可变属性源集合

### 5. task - 任务执行抽象

为同步和异步任务执行提供抽象，支持虚拟线程。

**核心类**:
- `TaskExecutor` - 任务执行器接口（Spring 的 Executor 抽象）
- `AsyncTaskExecutor` - 异步任务执行器
- `SyncTaskExecutor` - 同步任务执行器
- `SimpleAsyncTaskExecutor` - 简单异步实现
- `VirtualThreadTaskExecutor` - 虚拟线程执行器（Java 21+）

### 6. type - 类型内省

提供类元数据的抽象，用于类路径扫描和组件索引。

**核心类**:
- `ClassMetadata` - 类元数据接口
- `AnnotationMetadata` - 注解元数据
- `MethodMetadata` - 方法元数据
- `MetadataReader` - 元数据读取器

### 7. lang - 语言级注解

提供语言级语义注解，用于空值检查和工具验证。

**核心注解**:
- `@Nullable` - 可空标记
- `@NonNull` - 非空标记
- `@NonNullApi` - 包级非空 API
- `@Contract` - 契约注解

### 8. aot - 预先编译支持

支持 Spring AOT（Ahead-Of-Time）处理和 GraalVM 原生镜像。

**核心包**:
- `generate` - 代码生成（AOT 时代码生成上下文）
- `hint` - 运行时提示（反射、资源、代理等）
- `nativex` - GraalVM 原生镜像配置生成
