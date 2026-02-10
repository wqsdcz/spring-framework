# Spring Core 完整学习总结

## 学习路线图

```
Spring Core 学习路径
│
├── 阶段 1: 基础工具类
│   ├── Ordered / OrderComparator / @Order - 排序机制
│   ├── NestedRuntimeException - 异常包装
│   └── ResolvableType - 泛型解析
│
├── 阶段 1.5: 核心根包其他重要类
│   ├── AttributeAccessor - 属性访问器
│   ├── MethodParameter - 方法参数封装
│   ├── ParameterNameDiscoverer - 参数名发现
│   ├── ParameterizedTypeReference - 泛型类型引用
│   ├── ReactiveAdapterRegistry - 响应式适配器
│   ├── SpringProperties - 全局属性管理
│   └── MethodIntrospector - 方法内省器
│
├── 阶段 2: 资源加载
│   ├── Resource - 资源抽象
│   └── ResourceLoader - 资源加载器
│
├── 阶段 3: 环境抽象
│   ├── Environment - 环境接口
│   └── PropertySource - 属性源
│
├── 阶段 4: 类型转换
│   ├── Converter - 转换器
│   └── ConversionService - 转换服务
│
└── 阶段 5: 注解处理
    ├── AnnotatedElementUtils - 注解工具类
    └── MergedAnnotations - 合并注解API
```

---

## 一、基础工具类（阶段 1）

### 1.1 Ordered - 排序机制

**核心接口**：
```java
public interface Ordered {
    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;  // 最高优先级
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;   // 最低优先级
    int getOrder();
}
```

**使用场景**：
- Bean 加载顺序控制
- AOP 拦截器链排序
- Spring Boot 自动配置排序
- 集合注入时的元素排序

**关键代码**：
```java
// 实现接口
@Component
public class MyProcessor implements BeanPostProcessor, Ordered {
    @Override
    public int getOrder() { return 1; }
}

// 使用注解
@Component
@Order(100)
public class AnotherProcessor implements BeanPostProcessor { }
```

### 1.2 NestedRuntimeException - 异常包装

**核心功能**：
- `getRootCause()` - 获取最底层异常
- `contains(Class<?> exType)` - 检查异常链中是否包含某类型

**使用场景**：
- Spring 各模块的异常基类
- 异常链追踪
- 特定异常类型检查

### 1.3 ResolvableType - 泛型解析

**核心方法**：
```java
// 创建
ResolvableType.forField(field)
ResolvableType.forMethodParameter(method, index)
ResolvableType.forClass(clazz)

// 解析
type.resolve()                    // 转为 Class
type.getGeneric(0)                // 获取泛型参数
type.resolveGeneric(1, 0)         // 快捷获取嵌套泛型
```

**使用场景**：
- 依赖注入时的泛型匹配
- 类型转换时的目标类型确定
- AOP 方法拦截

---

## 二、核心根包其他重要类（阶段 1.5）

### 2.1 AttributeAccessor - 属性访问器

**核心方法**：
```java
void setAttribute(String name, Object value)
Object getAttribute(String name)
boolean hasAttribute(String name)
<T> T computeAttribute(String name, Function<String, T> computeFunction)
```

**使用场景**：
- BeanDefinition 的元数据存储
- 运行时动态附加属性
- 延迟计算属性值

### 2.2 MethodParameter - 方法参数封装

**核心功能**：
- 封装方法/构造器的单个参数
- 支持泛型解析
- 支持参数名发现
- 处理嵌套类型

**使用代码**：
```java
Method method = MyClass.class.getMethod("process", String.class, int.class);
MethodParameter param = new MethodParameter(method, 0);  // 第一个参数
param.getParameterType();      // 获取参数类型
param.getParameterName();      // 获取参数名（需配置 -parameters）
param.nested();                // 进入嵌套层
```

### 2.3 ParameterNameDiscoverer - 参数名发现

**策略实现**：
- `StandardReflectionParameterNameDiscoverer` - JDK 8+ -parameters
- `LocalVariableTableParameterNameDiscoverer` - ASM 字节码
- `KotlinReflectionParameterNameDiscoverer` - Kotlin 支持

**使用代码**：
```java
ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
String[] paramNames = discoverer.getParameterNames(method);
```

### 2.4 ParameterizedTypeReference - 泛型类型引用

**核心原理**：通过创建匿名子类保留泛型信息

**使用代码**：
```java
// 保留 List<String> 的泛型信息
ParameterizedTypeReference<List<String>> typeRef =
    new ParameterizedTypeReference<List<String>>() {};

// 用于 RestTemplate
restTemplate.exchange(url, HttpMethod.GET, null,
    new ParameterizedTypeReference<List<User>>() {});
```

### 2.5 ReactiveAdapterRegistry - 响应式适配器

**核心功能**：
- 适配 Reactor、RxJava、Kotlin Coroutines 等到 Reactive Streams Publisher
- 自动检测类路径上的响应式库

**使用代码**：
```java
ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();
ReactiveAdapter adapter = registry.getAdapter(Mono.class);
Publisher<?> publisher = adapter.toPublisher(mono);
```

### 2.6 SpringProperties - 全局属性管理

**三级配置优先级**：
1. 编程式设置（最高）`SpringProperties.setProperty()`
2. `spring.properties` 文件
3. JVM 系统属性（最低）`-Dkey=value`

**使用代码**：
```java
// 读取
String value = SpringProperties.getProperty("spring.beaninfo.ignore");
boolean flag = SpringProperties.getFlag("feature.enabled");

// 设置
SpringProperties.setProperty("myapp.version", "1.0.0");
SpringProperties.setFlag("feature.enabled");
```

### 2.7 MethodIntrospector - 方法内省器

**核心方法**：
```java
// 基于元数据查找方法
Map<Method, T> selectMethods(Class<?> targetType, MetadataLookup<T> metadataLookup)

// 基于过滤器查找方法
Set<Method> selectMethods(Class<?> targetType, MethodFilter methodFilter)

// 找到代理类上可调用的方法
Method selectInvocableMethod(Method method, Class<?> targetType)
```

**处理复杂性**：
- 类 + 父类 + 所有接口
- JDK 动态代理
- CGLIB 代理
- 泛型桥接方法

---

## 三、资源加载（阶段 2）

### 3.1 Resource - 资源抽象

**实现类**：
| 实现类 | 用途 | 示例 |
|--------|------|------|
| `ClassPathResource` | 类路径资源 | `classpath:application.properties` |
| `FileSystemResource` | 文件系统资源 | `file:/path/file.txt` |
| `UrlResource` | URL资源 | `https://example.com/data` |
| `ByteArrayResource` | 字节数组 | 内存数据 |

**核心方法**：
```java
boolean exists()           // 资源是否存在
InputStream getInputStream() // 获取输入流
File getFile()             // 获取 File 对象（如果可用）
Resource createRelative(String relativePath) // 创建相对资源
```

### 3.2 ResourceLoader - 资源加载器

**路径前缀**：
- `classpath:` → ClassPathResource
- `file:` → FileSystemResource
- `http/https:` → UrlResource

**使用代码**：
```java
@Autowired
private ResourceLoader resourceLoader;

public void loadResource(String location) throws IOException {
    Resource resource = resourceLoader.getResource(location);
    if (resource.exists()) {
        try (InputStream is = resource.getInputStream()) {
            // 处理内容
        }
    }
}
```

---

## 四、环境抽象（阶段 3）

### 4.1 Environment - 环境接口

**核心功能**：
- Profile 管理（dev/test/prod）
- 属性解析
- 占位符解析

**使用代码**：
```java
@Autowired
private Environment env;

// 属性访问
String value = env.getProperty("key", "default");
Integer port = env.getProperty("server.port", Integer.class);

// Profile 检查
if (env.matchesProfiles("dev | test")) {
    // 开发或测试环境
}
```

### 4.2 PropertySource - 属性源

**默认优先级（从高到低）**：
1. ServletConfig 参数（Web环境）
2. ServletContext 参数（Web环境）
3. JNDI 属性
4. Java 系统属性（-D）
5. 系统环境变量
6. `application-{profile}.properties`
7. `application.properties`

**自定义属性源**：
```java
ConfigurableEnvironment env = new StandardEnvironment();
env.getPropertySources().addFirst(
    new MapPropertySource("custom", Collections.singletonMap("key", "value"))
);
```

---

## 五、类型转换（阶段 4）

### 5.1 Converter - 转换器

**函数式接口**：
```java
@FunctionalInterface
public interface Converter<S, T> {
    T convert(S source);

    // 5.3+ 支持组合
    default <U> Converter<S, U> andThen(Converter<? super T, ? extends U> after);
}
```

**自定义转换器**：
```java
public class StringToPhoneNumberConverter implements Converter<String, PhoneNumber> {
    @Override
    public PhoneNumber convert(String source) {
        // 转换逻辑
        return phoneNumber;
    }
}

// 注册
@Configuration
public class Config implements WebMvcConfigurer {
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToPhoneNumberConverter());
    }
}
```

### 5.2 ConversionService - 转换服务

**使用代码**：
```java
@Autowired
private ConversionService conversionService;

// 基本转换
Integer num = conversionService.convert("123", Integer.class);

// 集合转换
List<String> strings = Arrays.asList("1", "2", "3");
List<Integer> integers = (List<Integer>) conversionService.convert(
    strings,
    TypeDescriptor.forObject(strings),
    TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(Integer.class))
);
```

---

## 六、注解处理（阶段 5）

### 6.1 AnnotatedElementUtils - 注解工具类

**Get vs Find 语义**：
- **get***：当前元素 + 注解层次
- **find***：完整类型/方法层次（包括接口、父类）

**使用代码**：
```java
// 查找合并后的注解（支持 @AliasFor）
RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(
    method, RequestMapping.class);

// 获取合并后的属性
AnnotationAttributes attrs = AnnotatedElementUtils.getMergedAnnotationAttributes(
    method, RequestMapping.class);
```

### 6.2 MergedAnnotations - 合并注解API

**搜索策略**：
```java
public enum SearchStrategy {
    DIRECT,                   // 仅直接声明的注解
    INHERITED_ANNOTATIONS,    // 直接 + @Inherited
    SUPERCLASS,               // 父类（不包括接口）
    TYPE_HIERARCHY,           // 完整类型层次
    TYPE_HIERARCHY_AND_ENCLOSING_CLASSES
}
```

**流式操作**：
```java
MergedAnnotations annotations = MergedAnnotations.from(MyClass.class);

// 检查是否存在
boolean hasComponent = annotations.isPresent(Component.class);

// 流式过滤
annotations.stream()
    .filter(MergedAnnotation::isMetaPresent)
    .forEach(anno -> System.out.println(anno.getType().getName()));
```

### 6.3 @AliasFor - 属性别名

**使用方式**：
```java
// 1. 同一注解内的别名
public @interface RequestMapping {
    @AliasFor("path")
    String[] value() default {};

    @AliasFor("value")
    String[] path() default {};
}

// 2. 覆盖元注解的属性
@Target(ElementType.METHOD)
@RequestMapping(method = RequestMethod.GET)
public @interface GetMapping {
    @AliasFor(annotation = RequestMapping.class, attribute = "value")
    String[] value() default {};
}
```

---

## 七、实际应用场景汇总

### 7.1 框架开发场景

| 场景 | 使用的核心类 |
|------|------------|
| 实现自定义注解处理器 | `AnnotatedElementUtils`, `MergedAnnotations` |
| 创建组合注解 | `@AliasFor` |
| 处理方法参数 | `MethodParameter`, `ParameterNameDiscoverer` |
| 资源扫描加载 | `Resource`, `ResourceLoader` |
| 类型转换扩展 | `Converter`, `ConversionService` |

### 7.2 业务开发场景

| 场景 | 使用的核心类 |
|------|------------|
| 多环境配置管理 | `Environment`, `PropertySource` |
| 自定义配置属性 | `@ConfigurationProperties` + `ConversionService` |
| 响应式编程 | `ReactiveAdapterRegistry` |
| 异常处理 | `NestedRuntimeException` |
| 泛型处理 | `ResolvableType`, `ParameterizedTypeReference` |

### 7.3 测试场景

| 场景 | 使用的核心类 |
|------|------------|
| 模拟 Environment | `MockPropertySource` |
| 测试注解处理 | `MergedAnnotations.from()` |
| 测试资源加载 | `ClassPathResource` |
| 测试类型转换 | `DefaultConversionService` |

---

## 八、速查表

### 8.1 常用类速查

| 类 | 一句话描述 | 使用频率 |
|---|-----------|---------|
| `ResolvableType` | 运行时泛型解析 | ⭐⭐⭐⭐⭐ |
| `MethodParameter` | 方法参数封装 | ⭐⭐⭐⭐⭐ |
| `ResourceLoader` | 统一资源加载 | ⭐⭐⭐⭐⭐ |
| `Environment` | 配置管理入口 | ⭐⭐⭐⭐⭐ |
| `ConversionService` | 类型转换服务 | ⭐⭐⭐⭐⭐ |
| `AnnotatedElementUtils` | 注解处理工具 | ⭐⭐⭐⭐ |
| `MergedAnnotations` | 流式注解API | ⭐⭐⭐⭐ |
| `ReactiveAdapterRegistry` | 响应式适配 | ⭐⭐⭐ |
| `SpringProperties` | 全局属性管理 | ⭐⭐⭐ |
| `MethodIntrospector` | 方法内省 | ⭐⭐⭐ |

### 8.2 关键注解速查

| 注解 | 用途 | 使用场景 |
|------|------|---------|
| `@Order` | 指定顺序 | Bean 排序、切面排序 |
| `@AliasFor` | 属性别名 | 组合注解开发 |
| `@Profile` | 环境标识 | 多环境配置 |
| `@ConfigurationProperties` | 配置绑定 | 属性注入 |
| `@Value` | 值注入 | SpEL 表达式 |

### 8.3 常用路径前缀

| 前缀 | 说明 | 示例 |
|------|------|------|
| `classpath:` | 类路径资源 | `classpath:application.yml` |
| `file:` | 文件系统资源 | `file:/etc/config.properties` |
| `http/https:` | 网络资源 | `https://example.com/data` |
| `classpath*:` | 所有匹配的类路径资源 | `classpath*:mapper/**/*.xml` |

---

## 九、学习建议

### 9.1 学习路径建议

1. **入门阶段**：
   - 先掌握 `Ordered`, `Resource`, `Environment`
   - 理解 Spring 的基本设计思想

2. **进阶阶段**：
   - 深入学习 `ResolvableType`, `ConversionService`
   - 掌握注解处理机制

3. **高级阶段**：
   - 研究 `ReactiveAdapterRegistry`, `MethodIntrospector`
   - 尝试开发自定义注解处理器

### 9.2 实践建议

1. **多写代码**：理论结合实践，手写自定义转换器、注解处理器
2. **阅读源码**：Spring 自身的实现是最好的学习材料
3. **查看测试**：Spring 的测试用例展示了各种使用场景
4. **关注设计**：理解每个类的设计意图和解决的问题

### 9.3 相关资源

- [Spring Framework 官方文档](https://docs.spring.io/spring-framework/docs/current/reference/html/)
- [Spring Boot 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Framework 源码](https://github.com/spring-projects/spring-framework)

---

## 十、总结

Spring Core 提供了丰富而强大的基础设施，支撑起整个 Spring 生态：

1. **基础工具**：`Ordered`, `ResolvableType` 等解决通用编程问题
2. **资源管理**：`Resource`, `ResourceLoader` 提供统一的资源抽象
3. **配置管理**：`Environment`, `PropertySource` 实现灵活的配置机制
4. **类型系统**：`Converter`, `ConversionService` 支持强大的类型转换
5. **元编程**：`AnnotatedElementUtils`, `MergedAnnotations` 提供先进的注解处理

掌握这些核心类，将帮助你：
- 更好地理解 Spring 框架的工作原理
- 更高效地使用 Spring 进行开发
- 能够开发基于 Spring 的自定义框架和工具
- 提升 Java 编程的整体水平

---

**学习完成日期**：2026-02-09
**文档版本**：v1.0
**总计学习模块**：6 个阶段，11 个详细指南
