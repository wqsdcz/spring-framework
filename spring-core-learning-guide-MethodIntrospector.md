# MethodIntrospector 深度解析

## 一、什么是 MethodIntrospector？

**MethodIntrospector** 是 Spring Framework 提供的一个**方法内省工具类**，用于在类及其继承体系中**全面搜索方法**，同时处理代理和桥接方法等复杂场景。

### 1.1 核心定义

```java
public final class MethodIntrospector {

    // 选择带有元数据的方法
    public static <T> Map<Method, T> selectMethods(Class<?> targetType,
                                                   MetadataLookup<T> metadataLookup)

    // 基于过滤器选择方法
    public static Set<Method> selectMethods(Class<?> targetType,
                                            ReflectionUtils.MethodFilter methodFilter)

    // 选择可调用的方法
    public static Method selectInvocableMethod(Method method, Class<?> targetType)

    // 元数据查找回调接口
    @FunctionalInterface
    public interface MetadataLookup<T> {
        @Nullable T inspect(Method method);
    }
}
```

**一句话理解**：它是 Spring 的**"方法搜索引擎"**，能够在复杂的类层次结构（包括接口、父类、代理类）中准确找到目标方法。

---

## 二、为什么需要它？

### 2.1 Java 方法查找的复杂性

在实际开发中，方法可能分布在多个地方：

```
目标类
├── 自身声明的方法
├── 父类继承的方法
├── 接口实现的方法
├── 默认方法（Java 8+）
└── 代理类的方法（JDK/CGLIB）
    ├── 接口方法
    └── 桥接方法（Bridge Method）
```

### 2.2 典型问题场景

#### 场景 1：泛型桥接方法

```java
public interface Service<T> {
    void process(T item);
}

public class UserService implements Service<User> {
    @Override
    public void process(User item) { }  // 实际方法

    // 编译器生成的桥接方法：
    // public void process(Object item) { process((User)item); }
}
```

**问题**：反射会拿到两个 `process` 方法，如何区分哪个是真正的实现？

#### 场景 2：JDK 动态代理

```java
public interface UserService {
    @Transactional
    User getUser(Long id);
}

// Spring 创建的代理
UserService proxy = (UserService) Proxy.newProxyInstance(...);
```

**问题**：代理类上的注解丢失了，如何找到接口中的原始注解？

#### 场景 3：CGLIB 代理

```java
public class OrderService {
    @Cacheable
    public Order getOrder(Long id) { }
}

// CGLIB 生成的代理子类
class OrderService$$EnhancerBySpringCGLIB extends OrderService {
    @Override
    public Order getOrder(Long id) {
        // 增强逻辑
        return super.getOrder(id);
    }
}
```

**问题**：代理子类可能重写方法，如何找到原始类中的方法？

### 2.3 MethodIntrospector 的解决方案

```
┌─────────────────────────────────────────────────────────────┐
│                   MethodIntrospector                        │
├─────────────────────────────────────────────────────────────┤
│ 1. 搜索范围：类 + 父类 + 所有接口                             │
│ 2. 处理桥接方法：通过 BridgeMethodResolver 找到原始方法       │
│ 3. 处理代理：识别 JDK/CGLIB 代理，找到目标类                  │
│ 4. 元数据提取：回调接口允许提取注解或其他元数据                │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、核心方法详解

### 3.1 selectMethods - 元数据检索

这是 MethodIntrospector 的核心方法：

```java
public static <T> Map<Method, T> selectMethods(
        Class<?> targetType,
        final MetadataLookup<T> metadataLookup) {

    final Map<Method, T> methodMap = new LinkedHashMap<>();
    Set<Class<?>> handlerTypes = new LinkedHashSet<>();
    Class<?> specificHandlerType = null;

    // 1. 处理非代理类
    if (!Proxy.isProxyClass(targetType)) {
        specificHandlerType = ClassUtils.getUserClass(targetType);
        handlerTypes.add(specificHandlerType);
    }

    // 2. 添加所有接口
    handlerTypes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetType));

    // 3. 遍历所有类型查找方法
    for (Class<?> currentHandlerType : handlerTypes) {
        final Class<?> targetClass = (specificHandlerType != null ?
            specificHandlerType : currentHandlerType);

        ReflectionUtils.doWithMethods(currentHandlerType, method -> {
            // 获取最具体的方法
            Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);

            // 调用回调提取元数据
            T result = metadataLookup.inspect(specificMethod);

            if (result != null) {
                // 处理桥接方法
                Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);

                // 避免重复注册桥接方法
                if (bridgedMethod == specificMethod ||
                    bridgedMethod == method ||
                    metadataLookup.inspect(bridgedMethod) == null) {
                    methodMap.put(specificMethod, result);
                }
            }
        }, ReflectionUtils.USER_DECLARED_METHODS);
    }

    return methodMap;
}
```

**执行流程**：

```
输入：目标类 UserServiceProxy
       ↓
1. 识别类型
   - 是代理类？→ 获取目标类 UserService
   - 收集所有接口：UserService, Service, ...
       ↓
2. 遍历每个类型（UserService, Service, ...）
   - 遍历类型中的每个方法
       ↓
3. 处理每个方法
   - 获取最具体的方法版本
   - 调用 MetadataLookup 提取元数据
   - 如果返回非 null，保留该方法
   - 处理桥接方法去重
       ↓
输出：Map<Method, T>（方法 → 元数据）
```

### 3.2 selectMethods - 过滤器版本

简化版本，只返回方法集合：

```java
public static Set<Method> selectMethods(
        Class<?> targetType,
        final ReflectionUtils.MethodFilter methodFilter) {

    return selectMethods(targetType,
        (MetadataLookup<Boolean>) method ->
            (methodFilter.matches(method) ? Boolean.TRUE : null)
    ).keySet();
}
```

### 3.3 selectInvocableMethod - 选择可调用方法

用于在代理上找到可以实际调用的方法：

```java
public static Method selectInvocableMethod(Method method, Class<?> targetType) {
    // 1. 方法已在目标类型上，直接返回
    if (method.getDeclaringClass().isAssignableFrom(targetType)) {
        return method;
    }

    try {
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();

        // 2. 在接口中查找
        for (Class<?> ifc : targetType.getInterfaces()) {
            try {
                return ifc.getMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException ex) {
                // 继续尝试下一个接口
            }
        }

        // 3. 在代理类本身查找
        return targetType.getMethod(methodName, parameterTypes);
    }
    catch (NoSuchMethodException ex) {
        throw new IllegalStateException(
            "Need to invoke method '" + method.getName() +
            "' declared on target class '" + method.getDeclaringClass().getSimpleName() +
            "', but not found in any interface(s) of the exposed proxy type. " +
            "Either pull the method up to an interface or switch to CGLIB " +
            "proxies by enforcing proxy-target-class mode in your configuration."
        );
    }
}
```

---

## 四、桥接方法（Bridge Method）详解

### 4.1 什么是桥接方法？

Java 泛型在编译时会擦除类型信息。当子类指定了父类泛型参数的具体类型时，编译器需要生成**桥接方法**来保持多态性。

### 4.2 示例分析

```java
// 泛型接口
public interface Converter<S, T> {
    T convert(S source);
}

// 具体实现
public class StringToIntegerConverter implements Converter<String, Integer> {
    @Override
    public Integer convert(String source) {
        return Integer.parseInt(source);
    }
}
```

编译后的字节码（等价）：

```java
public class StringToIntegerConverter implements Converter {
    // 实际方法
    public Integer convert(String source) {
        return Integer.parseInt(source);
    }

    // 桥接方法（编译器生成）
    public Object convert(Object source) {
        return convert((String) source);
    }
}
```

### 4.3 BridgeMethodResolver 的作用

```java
public final class BridgeMethodResolver {

    public static Method findBridgedMethod(Method bridgeMethod) {
        // 如果不是桥接方法，直接返回
        if (!bridgeMethod.isBridge()) {
            return bridgeMethod;
        }

        // 查找被桥接的原始方法
        // 通过比较方法签名和泛型信息匹配
        ...
    }
}
```

**在 MethodIntrospector 中的应用**：

```java
// 如果找到桥接方法，避免重复注册
Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);

// 条件判断：只有当桥接方法没有自己的元数据时才注册
if (bridgedMethod == specificMethod ||
    metadataLookup.inspect(bridgedMethod) == null) {
    methodMap.put(specificMethod, result);
}
```

---

## 五、实际应用场景

### 5.1 Spring MVC - 查找 @RequestMapping 方法

```java
@RequestMapping
public class UserController {

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) { }

    @PostMapping("/users")
    public User createUser(@RequestBody User user) { }
}

// Spring 内部使用 MethodIntrospector 查找
Map<Method, RequestMappingInfo> methods = MethodIntrospector.selectMethods(
    UserController.class,
    method -> {
        // 提取 @RequestMapping 注解信息
        RequestMapping annotation = method.getAnnotation(RequestMapping.class);
        return annotation != null ?
            RequestMappingInfo.fromAnnotation(annotation) : null;
    }
);

// 结果：{getUser=GET /users/{id}, createUser=POST /users}
```

### 5.2 Spring - 查找 @Bean 方法

```java
@Configuration
public class AppConfig {

    @Bean
    public DataSource dataSource() { }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource ds) { }
}

// 查找所有 @Bean 方法
Set<Method> beanMethods = MethodIntrospector.selectMethods(
    AppConfig.class,
    method -> method.isAnnotationPresent(Bean.class)
);
```

### 5.3 事件监听 - 查找 @EventListener

```java
@Component
public class UserEventHandler {

    @EventListener
    public void handleUserCreated(UserCreatedEvent event) { }

    @EventListener
    public void handleUserDeleted(UserDeletedEvent event) { }
}

// 查找事件监听方法
Map<Method, EventListener> listeners = MethodIntrospector.selectMethods(
    UserEventHandler.class,
    method -> method.getAnnotation(EventListener.class)
);
```

### 5.4 处理代理类

```java
@Service
public class OrderService {
    @Transactional
    public Order createOrder(OrderRequest request) { }
}

// Spring 创建的 CGLIB 代理
OrderService proxy = (OrderService) applicationContext.getBean("orderService");

// 使用 MethodIntrospector 找到原始方法
Method proxyMethod = proxy.getClass().getMethod("createOrder", OrderRequest.class);
Method targetMethod = MethodIntrospector.selectInvocableMethod(
    proxyMethod, OrderService.class
);

// targetMethod 指向 OrderService 类中的原始方法
// 可以从 targetMethod 上读取 @Transactional 注解
```

---

## 六、实战示例

### 6.1 自定义注解处理器

创建自定义注解：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcMethod {
    String value();
    int timeout() default 3000;
}
```

查找带注解的方法：

```java
@Component
public class RpcMethodScanner {

    public Map<Method, RpcMethodInfo> scan(Class<?> targetClass) {
        return MethodIntrospector.selectMethods(targetClass, method -> {
            RpcMethod annotation = method.getAnnotation(RpcMethod.class);
            if (annotation != null) {
                return new RpcMethodInfo(
                    annotation.value(),
                    annotation.timeout(),
                    method.getParameterTypes()
                );
            }
            return null;
        });
    }
}

// 使用
@Service
public class UserService {
    @RpcMethod("user.get", timeout = 5000)
    public User getUser(Long id) { }

    @RpcMethod("user.save")
    public void saveUser(User user) { }
}

// 扫描
Map<Method, RpcMethodInfo> rpcMethods = scanner.scan(UserService.class);
```

### 6.2 AOP 切面辅助工具

```java
@Component
public class AnnotationPointcutAdvisor {

    /**
     * 查找类中所有带指定注解的方法（包括继承的）
     */
    public List<Method> findAnnotatedMethods(Class<?> targetClass,
                                             Class<? extends Annotation> annotationType) {
        Map<Method, ?> methods = MethodIntrospector.selectMethods(
            targetClass,
            method -> method.isAnnotationPresent(annotationType) ? Boolean.TRUE : null
        );
        return new ArrayList<>(methods.keySet());
    }

    /**
     * 检查方法是否在目标类上可调用
     */
    public boolean isInvocable(Method method, Class<?> targetClass) {
        try {
            MethodIntrospector.selectInvocableMethod(method, targetClass);
            return true;
        } catch (IllegalStateException ex) {
            return false;
        }
    }
}
```

### 6.3 方法缓存键生成器

```java
@Component
public class CacheKeyGenerator {

    private final MethodIntrospector methodIntrospector;

    /**
     * 为类中所有带 @Cacheable 的方法生成缓存键前缀
     */
    public Map<Method, String> generateCacheKeys(Class<?> targetClass) {
        return MethodIntrospector.selectMethods(targetClass, method -> {
            Cacheable cacheable = method.getAnnotation(Cacheable.class);
            if (cacheable != null) {
                String className = targetClass.getSimpleName();
                String methodName = method.getName();
                return className + "." + methodName;
            }
            return null;
        });
    }
}
```

### 6.4 接口方法验证器

```java
@Component
public class InterfaceMethodValidator {

    /**
     * 验证实现类是否包含接口中定义的所有方法
     */
    public List<String> validateImplementation(Class<?> interfaceClass,
                                                Class<?> implementationClass) {
        List<String> errors = new ArrayList<>();

        // 获取接口中所有方法
        Map<Method, ?> interfaceMethods = MethodIntrospector.selectMethods(
            interfaceClass,
            method -> Boolean.TRUE  // 选择所有方法
        );

        for (Method interfaceMethod : interfaceMethods.keySet()) {
            try {
                // 检查实现类是否有可调用的对应方法
                MethodIntrospector.selectInvocableMethod(interfaceMethod, implementationClass);
            } catch (IllegalStateException ex) {
                errors.add("Missing implementation for: " + interfaceMethod.getName());
            }
        }

        return errors;
    }
}
```

---

## 七、最佳实践

### 7.1 缓存扫描结果

```java
@Component
public class CachedMethodScanner {

    private final Map<Class<?>, Map<Method, Object>> cache = new ConcurrentHashMap<>();

    public <T> Map<Method, T> scan(Class<?> targetClass,
                                   MetadataLookup<T> metadataLookup) {
        return (Map<Method, T>) cache.computeIfAbsent(targetClass,
            clazz -> MethodIntrospector.selectMethods(clazz, metadataLookup)
        );
    }
}
```

### 7.2 组合多个条件

```java
public class CompositeMetadataLookup<T> implements MethodIntrospector.MetadataLookup<T> {

    private final List<MethodIntrospector.MetadataLookup<T>> lookups;

    @SafeVarargs
    public CompositeMetadataLookup(MethodIntrospector.MetadataLookup<T>... lookups) {
        this.lookups = Arrays.asList(lookups);
    }

    @Override
    public T inspect(Method method) {
        for (MethodIntrospector.MetadataLookup<T> lookup : lookups) {
            T result = lookup.inspect(method);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}

// 使用
Map<Method, Object> methods = MethodIntrospector.selectMethods(
    targetClass,
    new CompositeMetadataLookup<>(
        method -> method.getAnnotation(GetMapping.class),
        method -> method.getAnnotation(PostMapping.class),
        method -> method.getAnnotation(PutMapping.class)
    )
);
```

### 7.3 处理重复注解

```java
public class RepeatableAnnotationLookup<T extends Annotation>
        implements MethodIntrospector.MetadataLookup<List<T>> {

    private final Class<T> annotationType;
    private final Class<? extends Annotation> containerType;

    public RepeatableAnnotationLookup(Class<T> annotationType) {
        this.annotationType = annotationType;
        this.containerType = annotationType.getAnnotation(Repeatable.class).value();
    }

    @Override
    public List<T> inspect(Method method) {
        List<T> annotations = new ArrayList<>();

        // 单个注解
        T single = method.getAnnotation(annotationType);
        if (single != null) {
            annotations.add(single);
        }

        // 可重复注解
        Annotation container = method.getAnnotation(containerType);
        if (container != null) {
            try {
                T[] values = (T[]) container.getClass().getMethod("value").invoke(container);
                annotations.addAll(Arrays.asList(values));
            } catch (Exception ex) {
                // ignore
            }
        }

        return annotations.isEmpty() ? null : annotations;
    }
}
```

---

## 八、常见问题

### Q1: selectMethods 和 ReflectionUtils.doWithMethods 有什么区别？

```java
// ReflectionUtils - 简单遍历当前类的方法
ReflectionUtils.doWithMethods(UserService.class, method -> {
    // 只处理 UserService 中声明的方法
});

// MethodIntrospector - 全面搜索包括继承体系
MethodIntrospector.selectMethods(UserService.class, method -> {
    // 处理 UserService + 父类 + 所有接口的方法
    // 自动处理桥接方法和代理
});
```

### Q2: 如何处理 Kotlin 类的默认方法？

```kotlin
interface Service {
    fun process() { // 默认实现
    }
}
```

```java
// MethodIntrospector 会自动处理接口的默认方法
Map<Method, ?> methods = MethodIntrospector.selectMethods(
    Service.class,
    method -> Boolean.TRUE
);
// 结果包含默认方法
```

### Q3: 为什么我的注解在代理类上找不到？

```java
// 错误：直接在代理类上查找
UserService proxy = ...;
Method method = proxy.getClass().getMethod("getUser");
Transactional tx = method.getAnnotation(Transactional.class); // null!

// 正确：使用 MethodIntrospector
Method targetMethod = MethodIntrospector.selectInvocableMethod(method, UserService.class);
Transactional tx = targetMethod.getAnnotation(Transactional.class); // 找到！
```

### Q4: 如何排除桥接方法？

```java
Map<Method, ?> methods = MethodIntrospector.selectMethods(targetClass, method -> {
    // 跳过桥接方法
    if (method.isBridge()) {
        return null;
    }
    // 处理逻辑
    return method.getAnnotation(MyAnnotation.class);
});
```

---

## 九、性能考虑

### 9.1 扫描成本

`selectMethods` 需要遍历类的整个继承体系，包括：
- 当前类
- 所有父类
- 所有接口

**建议**：
- 在启动时扫描并缓存结果
- 不要在请求热路径中频繁调用
- 使用 `ConcurrentHashMap` 缓存扫描结果

### 9.2 优化策略

```java
@Component
public class OptimizedMethodScanner {

    private final Map<Class<?>, Map<Method, Object>> cache =
        new ConcurrentReferenceHashMap<>();

    @PostConstruct
    public void preScan() {
        // 启动时预扫描关键类
        scan(UserController.class);
        scan(OrderService.class);
    }

    public <T> Map<Method, T> scan(Class<?> targetClass) {
        return (Map<Method, T>) cache.computeIfAbsent(targetClass, clazz -> {
            long start = System.currentTimeMillis();
            Map<Method, T> result = doScan(clazz);
            long duration = System.currentTimeMillis() - start;
            System.out.println("Scanned " + clazz + " in " + duration + "ms");
            return result;
        });
    }
}
```

---

## 十、本课小结

### 核心要点

1. **MethodIntrospector 是 Spring 的方法搜索引擎**，能够在复杂的类层次结构中准确找到目标方法

2. **三大核心功能**：
   - `selectMethods`：基于元数据查找方法
   - `selectMethods`（过滤器版）：基于条件查找方法
   - `selectInvocableMethod`：找到代理类上可调用的方法

3. **处理的复杂性**：
   - 类继承体系（父类、接口）
   - JDK 动态代理
   - CGLIB 代理
   - 泛型桥接方法

4. **典型应用场景**：
   - Spring MVC 的 @RequestMapping 处理
   - Spring 的 @Bean 方法发现
   - 事件监听器的注册
   - AOP 切点匹配

### 代码模板

```java
// 查找带注解的方法
Map<Method, RequestMapping> methods = MethodIntrospector.selectMethods(
    controllerClass,
    method -> method.getAnnotation(RequestMapping.class)
);

// 使用过滤器
Set<Method> beanMethods = MethodIntrospector.selectMethods(
    configClass,
    method -> method.isAnnotationPresent(Bean.class)
);

// 代理类找到原始方法
Method targetMethod = MethodIntrospector.selectInvocableMethod(
    proxyMethod, TargetClass.class
);
```

---

## 参考资料

- [Spring Framework Reference - Web MVC](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc)
- [Java Bridge Methods](https://docs.oracle.com/javase/tutorial/java/generics/bridgeMethods.html)
- [Spring AOP Proxies](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop-proxying)
- [Java Reflection API](https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/package-summary.html)
