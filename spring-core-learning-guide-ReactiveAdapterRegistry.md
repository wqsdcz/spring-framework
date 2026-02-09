# ReactiveAdapterRegistry 深度解析

## 一、什么是 ReactiveAdapterRegistry？

**ReactiveAdapterRegistry** 是 Spring 5.0 引入的核心组件，用于**适配不同的响应式编程库**（Reactor、RxJava、Kotlin Coroutines 等）到 **Reactive Streams** 标准。

### 1.1 核心定义

```java
public class ReactiveAdapterRegistry {
    private final List<ReactiveAdapter> adapters = new ArrayList<>();

    // 注册适配器
    public void registerReactiveType(ReactiveTypeDescriptor descriptor,
            Function<Object, Publisher<?>> toAdapter,
            Function<Publisher<?>, Object> fromAdapter);

    // 获取适配器
    public ReactiveAdapter getAdapter(Class<?> reactiveType);
}
```

**一句话理解**：它是一个**响应式类型的"翻译官"**，让 Spring 能够统一处理各种响应式类型（Mono、Flux、Flowable、CompletableFuture 等）。

---

## 二、为什么需要它？

### 2.1 响应式编程的碎片化问题

Java 生态中有多种响应式编程库：

| 库 | 核心类型 | 特点 |
|----|---------|------|
| **Project Reactor** | `Mono<T>`, `Flux<T>` | Spring 官方支持，功能最丰富 |
| **RxJava 3** | `Flowable`, `Single`, `Maybe`, `Completable` | Netflix 开发，生态成熟 |
| **Kotlin Coroutines** | `Deferred<T>`, `Flow<T>` | Kotlin 原生支持，简洁优雅 |
| **JDK 9+** | `Flow.Publisher<T>` | Java 标准，但功能基础 |
| **SmallRye Mutiny** | `Uni<T>`, `Multi<T>` | Quarkus 框架使用 |

**问题**：这些类型之间**不兼容**，无法直接互相调用。

### 2.2 Reactive Streams 标准

为了解决这个问题，业界制定了 **Reactive Streams** 规范（JEP 266）：

```java
// 核心接口
public interface Publisher<T> {
    void subscribe(Subscriber<? super T> s);
}

public interface Subscriber<T> {
    void onSubscribe(Subscription s);
    void onNext(T t);
    void onError(Throwable t);
    void onComplete();
}
```

**目标**：所有响应式库都实现这个标准，就可以互相通信。

### 2.3 Spring 的解决方案

```
Mono<T> ────┐
Flux<T> ────┼──► ReactiveAdapterRegistry ──► Publisher<T> ──► 统一处理
Flowable ───┤         (适配器注册表)              (标准接口)
Single ─────┤
Maybe ──────┤
Deferred ───┘
```

**ReactiveAdapterRegistry 的作用**：
- 将各种响应式类型**转换为** Reactive Streams `Publisher`
- 从 `Publisher`**转换回**具体的响应式类型
- **自动检测**类路径上的响应式库并注册适配器

---

## 三、核心架构

### 3.1 三大核心组件

```
ReactiveAdapterRegistry (注册表)
    ├── ReactiveAdapter (适配器)
    │       ├── ReactiveTypeDescriptor (类型描述)
    │       ├── toPublisherFunction (转换函数)
    │       └── fromPublisherFunction (反向转换函数)
    └── 各种 Registrar (自动注册器)
            ├── ReactorRegistrar (Project Reactor)
            ├── RxJava3Registrar (RxJava 3)
            ├── CoroutinesRegistrar (Kotlin)
            └── MutinyRegistrar (Mutiny)
```

### 3.2 ReactiveAdapter（适配器）

```java
public class ReactiveAdapter {
    private final ReactiveTypeDescriptor descriptor;
    private final Function<Object, Publisher<?>> toPublisherFunction;
    private final Function<Publisher<?>, Object> fromPublisherFunction;

    // 转换为 Publisher
    public <T> Publisher<T> toPublisher(@Nullable Object source) {
        return (Publisher<T>) this.toPublisherFunction.apply(source);
    }

    // 从 Publisher 转换回来
    public Object fromPublisher(Publisher<?> publisher) {
        return this.fromPublisherFunction.apply(publisher);
    }
}
```

**职责**：封装一种响应式类型的**双向转换逻辑**。

### 3.3 ReactiveTypeDescriptor（类型描述符）

```java
public final class ReactiveTypeDescriptor {
    private final Class<?> reactiveType;      // 响应式类型类
    private final boolean multiValue;         // 是否多值（Flux/Flowable）
    private final boolean noValue;            // 是否无值（Completable）
    private final Supplier<?> emptySupplier;  // 空值供应器
    private final boolean deferred;           // 是否延迟执行

    // 工厂方法
    public static ReactiveTypeDescriptor multiValue(Class<?> type, Supplier<?> emptySupplier);
    public static ReactiveTypeDescriptor singleOptionalValue(Class<?> type, Supplier<?> emptySupplier);
    public static ReactiveTypeDescriptor singleRequiredValue(Class<?> type);
    public static ReactiveTypeDescriptor noValue(Class<?> type, Supplier<?> emptySupplier);
}
```

**职责**：描述响应式类型的**语义特征**。

**类型分类**：

| 类型 | 示例 | 说明 |
|-----|------|------|
| **multiValue** | `Flux<T>`, `Flowable<T>` | 0..N 个值 |
| **singleOptionalValue** | `Mono<T>`, `Maybe<T>` | 0..1 个值 |
| **singleRequiredValue** | `Single<T>` | 必须 1 个值 |
| **noValue** | `Completable` | 无值，只有完成信号 |

---

## 四、源码深度解析

### 4.1 注册表初始化

```java
public class ReactiveAdapterRegistry {

    // 类路径检测标志
    private static final boolean reactorPresent;
    private static final boolean rxjava3Present;
    private static final boolean kotlinCoroutinesPresent;
    private static final boolean mutinyPresent;

    static {
        ClassLoader classLoader = ReactiveAdapterRegistry.class.getClassLoader();
        reactorPresent = ClassUtils.isPresent("reactor.core.publisher.Flux", classLoader);
        rxjava3Present = ClassUtils.isPresent("io.reactivex.rxjava3.core.Flowable", classLoader);
        kotlinCoroutinesPresent = ClassUtils.isPresent("kotlinx.coroutines.reactor.MonoKt", classLoader);
        mutinyPresent = ClassUtils.isPresent("io.smallrye.mutiny.Multi", classLoader);
    }

    public ReactiveAdapterRegistry() {
        // 自动注册可用的适配器
        if (reactorPresent) {
            new ReactorRegistrar().registerAdapters(this);
        }
        if (rxjava3Present) {
            new RxJava3Registrar().registerAdapters(this);
        }
        if (reactorPresent && kotlinCoroutinesPresent) {
            new CoroutinesRegistrar().registerAdapters(this);
        }
        if (mutinyPresent) {
            new MutinyRegistrar().registerAdapters(this);
        }
    }
}
```

**关键点**：
- 使用 `ClassUtils.isPresent()` **检测类路径**
- **延迟加载**：只在需要时创建适配器
- **条件注册**：没有依赖的库不会报错

### 4.2 Reactor 适配器注册

```java
private static class ReactorRegistrar {

    void registerAdapters(ReactiveAdapterRegistry registry) {
        // Mono: 0..1 个值
        registry.registerReactiveType(
            ReactiveTypeDescriptor.singleOptionalValue(Mono.class, Mono::empty),
            source -> (Mono<?>) source,           // 已经是 Publisher
            Mono::from                            // 从 Publisher 转回
        );

        // Flux: 0..N 个值
        registry.registerReactiveType(
            ReactiveTypeDescriptor.multiValue(Flux.class, Flux::empty),
            source -> (Flux<?>) source,
            Flux::from
        );

        // Publisher: 标准接口
        registry.registerReactiveType(
            ReactiveTypeDescriptor.multiValue(Publisher.class, Flux::empty),
            source -> (Publisher<?>) source,
            source -> source                      // 无需转换
        );

        // CompletableFuture: 异步类型
        registry.registerReactiveType(
            ReactiveTypeDescriptor.nonDeferredAsyncValue(CompletionStage.class, EmptyCompletableFuture::new),
            source -> Mono.fromCompletionStage((CompletionStage<?>) source),
            source -> Mono.from(source).toFuture()
        );

        // JDK 9 Flow.Publisher
        registry.registerReactiveType(
            ReactiveTypeDescriptor.multiValue(Flow.Publisher.class, () -> EMPTY_FLOW),
            source -> JdkFlowAdapter.flowPublisherToFlux((Flow.Publisher<?>) source),
            JdkFlowAdapter::publisherToFlowPublisher
        );
    }
}
```

### 4.3 适配器查找逻辑

```java
public ReactiveAdapter getAdapter(@Nullable Class<?> reactiveType, @Nullable Object source) {
    // 1. 确定类型（优先使用实例的实际类型）
    Object sourceToUse = (source instanceof Optional<?> optional ? optional.orElse(null) : source);
    Class<?> clazz = (sourceToUse != null ? sourceToUse.getClass() : reactiveType);

    if (clazz == null) {
        return null;
    }

    // 2. 第一遍：精确匹配
    for (ReactiveAdapter adapter : this.adapters) {
        if (adapter.getReactiveType() == clazz) {
            return adapter;
        }
    }

    // 3. 第二遍：继承匹配（isAssignableFrom）
    for (ReactiveAdapter adapter : this.adapters) {
        if (adapter.getReactiveType().isAssignableFrom(clazz)) {
            return adapter;
        }
    }

    return null;
}
```

**查找策略**：
1. **精确匹配**：类型完全一致（`==`）
2. **继承匹配**：类型是适配器类型的子类（`isAssignableFrom`）

### 4.4 共享实例（单例模式）

```java
private static volatile ReactiveAdapterRegistry sharedInstance;

public static ReactiveAdapterRegistry getSharedInstance() {
    ReactiveAdapterRegistry registry = sharedInstance;
    if (registry == null) {
        synchronized (ReactiveAdapterRegistry.class) {
            registry = sharedInstance;
            if (registry == null) {
                registry = new ReactiveAdapterRegistry();
                sharedInstance = registry;
            }
        }
    }
    return registry;
}
```

**双重检查锁定**：确保线程安全且只创建一次。

---

## 五、支持的所有响应式类型

### 5.1 类型对照表

| 库 | 类型 | 语义 | 适配方式 |
|----|------|------|---------|
| **Reactor** | `Mono<T>` | singleOptionalValue | 直接实现 Publisher |
| | `Flux<T>` | multiValue | 直接实现 Publisher |
| | `Publisher<T>` | multiValue | 原生支持 |
| **RxJava 3** | `Flowable<T>` | multiValue | 实现 Publisher |
| | `Observable<T>` | multiValue | 转为 Flowable |
| | `Single<T>` | singleRequiredValue | 转为 Flowable |
| | `Maybe<T>` | singleOptionalValue | 转为 Flowable |
| | `Completable` | noValue | 转为 Flowable |
| **Kotlin** | `Deferred<T>` | singleOptionalValue | 通过 Mono 桥接 |
| | `Flow<T>` | multiValue | 通过 Flux 桥接 |
| **JDK 9+** | `Flow.Publisher<T>` | multiValue | 通过 Flux 桥接 |
| **CompletableFuture** | `CompletionStage<T>` | nonDeferredAsyncValue | 通过 Mono 桥接 |
| **Mutiny** | `Uni<T>` | singleOptionalValue | 通过 Publisher 桥接 |
| | `Multi<T>` | multiValue | 通过 Publisher 桥接 |

### 5.2 适配器注册顺序

```java
// 注册顺序影响查找优先级
1. Mono                    // 最具体
2. Flux
3. Publisher               // 较通用
4. CompletableFuture
5. Flow.Publisher
6. RxJava Flowable         // 第三方库
7. RxJava Observable
...
```

**注意**：先注册的具体类型会优先匹配。

---

## 六、使用方法

### 6.1 基本用法

```java
// 获取共享注册表
ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();

// 获取 Mono 的适配器
ReactiveAdapter monoAdapter = registry.getAdapter(Mono.class);

// Mono 转 Publisher
Mono<String> mono = Mono.just("hello");
Publisher<String> publisher = monoAdapter.toPublisher(mono);

// Publisher 转 Mono
Mono<String> backToMono = (Mono<String>) monoAdapter.fromPublisher(publisher);
```

### 6.2 自动检测类型

```java
// 传入实例，自动检测类型
Object myFlux = Flux.just(1, 2, 3);
ReactiveAdapter adapter = registry.getAdapter(null, myFlux);

// 输出: reactor.core.publisher.Flux
System.out.println(adapter.getReactiveType());

// 转换为标准 Publisher
Publisher<Integer> publisher = adapter.toPublisher(myFlux);
```

### 6.3 检查类型语义

```java
ReactiveAdapter adapter = registry.getAdapter(Flux.class);

// 是否是多值类型？（Flux/Flowable）
boolean multi = adapter.isMultiValue();  // true

// 是否支持空值？
boolean empty = adapter.supportsEmpty();  // true

// 是否无值类型？（Completable）
boolean noValue = adapter.isNoValue();  // false
```

### 6.4 创建自定义注册表

```java
// 创建新的注册表（不影响共享实例）
ReactiveAdapterRegistry customRegistry = new ReactiveAdapterRegistry();

// 注册自定义适配器
customRegistry.registerReactiveType(
    ReactiveTypeDescriptor.multiValue(MyReactiveType.class, MyReactiveType::empty),
    source -> ((MyReactiveType<?>) source).toPublisher(),
    publisher -> MyReactiveType.fromPublisher(publisher)
);
```

---

## 七、Spring 中的实际应用

### 7.1 Spring WebFlux 返回值处理

```java
@RestController
public class UserController {

    // Spring 自动使用 ReactiveAdapterRegistry 处理
    @GetMapping("/users")
    public Flux<User> getAllUsers() {
        return userService.findAll();
    }

    @GetMapping("/users/{id}")
    public Mono<User> getUser(@PathVariable Long id) {
        return userService.findById(id);
    }

    // 也支持 RxJava
    @GetMapping("/users/rx")
    public Flowable<User> getAllUsersRx() {
        return rxUserService.findAll();
    }
}
```

**Spring 内部处理**：
```java
// 1. 获取控制器方法的返回类型
Class<?> returnType = method.getReturnType();

// 2. 查找适配器
ReactiveAdapter adapter = registry.getAdapter(returnType);

// 3. 转换为 Publisher 统一处理
Publisher<?> publisher = adapter.toPublisher(returnValue);

// 4. 写入 HTTP 响应
return writeToResponse(publisher, response);
```

### 7.2 响应式数据访问

```java
@Repository
public class ReactiveUserRepository {

    // 可以返回多种类型
    public Mono<User> findById(Long id) { ... }

    public Flux<User> findAll() { ... }

    // 也支持 CompletableFuture
    public CompletableFuture<User> findByIdAsync(Long id) { ... }
}

// Spring Data 自动适配
@Service
public class UserService {
    @Autowired
    private ReactiveUserRepository repository;

    public Flux<User> getAllUsers() {
        // 自动转换各种类型为 Flux
        return reactiveAdapterRegistry.fromPublisher(
            repository.findAll()
        );
    }
}
```

### 7.3 响应式 WebClient

```java
WebClient webClient = WebClient.create();

// 支持多种响应式类型
Mono<User> mono = webClient.get()
    .uri("/users/1")
    .retrieve()
    .bodyToMono(User.class);

// 也支持 RxJava
Flowable<User> flowable = webClient.get()
    .uri("/users")
    .retrieve()
    .bodyToFlux(User.class)
    .as(RxJava3Adapter::fluxToFlowable);
```

### 7.4 RSocket 响应式通信

```java
@Controller
public class RSocketController {

    @MessageMapping("users.stream")
    public Flux<User> streamUsers() { ... }

    @MessageMapping("users.request")
    public Mono<User> requestUser(Long id) { ... }
}
```

---

## 八、实战示例

### 8.1 自定义响应式类型适配

假设你有一个自定义的响应式类型：

```java
// 自定义响应式类型
public class MyPublisher<T> {
    private final List<T> values;

    public static <T> MyPublisher<T> of(T... values) {
        return new MyPublisher<>(Arrays.asList(values));
    }

    public static <T> MyPublisher<T> empty() {
        return new MyPublisher<>(Collections.emptyList());
    }

    public void subscribe(Consumer<T> consumer) {
        values.forEach(consumer);
    }

    // 转换为标准 Publisher
    public Publisher<T> toPublisher() {
        return Flux.fromIterable(values);
    }

    // 从 Publisher 创建
    public static <T> MyPublisher<T> fromPublisher(Publisher<T> publisher) {
        return new MyPublisher<>(Flux.from(publisher).collectList().block());
    }
}
```

注册适配器：

```java
@Component
public class CustomReactiveAdapterConfig {

    @PostConstruct
    public void register() {
        ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();

        registry.registerReactiveType(
            ReactiveTypeDescriptor.multiValue(MyPublisher.class, MyPublisher::empty),
            source -> ((MyPublisher<?>) source).toPublisher(),
            publisher -> MyPublisher.fromPublisher(publisher)
        );
    }
}
```

使用：

```java
@RestController
public class MyController {

    @GetMapping("/data")
    public MyPublisher<String> getData() {
        return MyPublisher.of("a", "b", "c");
    }
}
```

### 8.2 类型转换工具类

```java
@Component
public class ReactiveTypeConverter {

    private final ReactiveAdapterRegistry registry;

    public ReactiveTypeConverter(ReactiveAdapterRegistry registry) {
        this.registry = registry;
    }

    /**
     * 任意响应式类型转为 Flux
     */
    public <T> Flux<T> toFlux(Object source) {
        if (source == null) {
            return Flux.empty();
        }

        ReactiveAdapter adapter = registry.getAdapter(null, source);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported reactive type: " + source.getClass());
        }

        Publisher<T> publisher = adapter.toPublisher(source);
        return Flux.from(publisher);
    }

    /**
     * 任意响应式类型转为 Mono
     */
    public <T> Mono<T> toMono(Object source) {
        if (source == null) {
            return Mono.empty();
        }

        ReactiveAdapter adapter = registry.getAdapter(null, source);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported reactive type: " + source.getClass());
        }

        Publisher<T> publisher = adapter.toPublisher(source);
        return Mono.from(publisher);
    }

    /**
     * Publisher 转为目标类型
     */
    public <T> T fromPublisher(Publisher<?> publisher, Class<T> targetType) {
        ReactiveAdapter adapter = registry.getAdapter(targetType);
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter for type: " + targetType);
        }

        return (T) adapter.fromPublisher(publisher);
    }
}
```

使用：

```java
@Service
public class UserService {
    @Autowired
    private ReactiveTypeConverter converter;

    public Flux<User> mergeSources(
            Mono<User> mono,
            Flowable<User> flowable,
            CompletableFuture<User> future) {

        return Flux.merge(
            converter.toFlux(mono),
            converter.toFlux(flowable),
            converter.toFlux(future)
        );
    }
}
```

### 8.3 响应式类型检测

```java
@Component
public class ReactiveTypeInspector {

    @Autowired
    private ReactiveAdapterRegistry registry;

    public void inspect(Object obj) {
        ReactiveAdapter adapter = registry.getAdapter(null, obj);

        if (adapter == null) {
            System.out.println(obj.getClass() + " 不是响应式类型");
            return;
        }

        ReactiveTypeDescriptor descriptor = adapter.getDescriptor();

        System.out.println("类型: " + descriptor.getReactiveType());
        System.out.println("是否多值: " + descriptor.isMultiValue());
        System.out.println("是否无值: " + descriptor.isNoValue());
        System.out.println("是否延迟: " + descriptor.isDeferred());
        System.out.println("支持空值: " + descriptor.supportsEmpty());
    }
}

// 测试
inspector.inspect(Mono.just("test"));
// 类型: class reactor.core.publisher.Mono
// 是否多值: false
// 是否无值: false
// 是否延迟: true
// 支持空值: true

inspector.inspect(Flux.range(1, 10));
// 类型: class reactor.core.publisher.Flux
// 是否多值: true
// ...

inspector.inspect("not reactive");
// String 不是响应式类型
```

---

## 九、与 Reactor 的深度集成

### 9.1 ReactorAdapter 的特殊处理

当 Reactor 存在时，会使用 `ReactorAdapter` 子类：

```java
private static class ReactorAdapter extends ReactiveAdapter {

    @Override
    public <T> Publisher<T> toPublisher(@Nullable Object source) {
        Publisher<T> publisher = super.toPublisher(source);
        // 根据类型包装为 Flux 或 Mono
        return (isMultiValue() ? Flux.from(publisher) : Mono.from(publisher));
    }
}
```

**作用**：确保转换后的 Publisher 保持原有的 Reactor 类型特征。

### 9.2 BlockHound 集成

```java
static {
    try {
        BlockHoundIntegration integration = new ReactorBlockHoundIntegration();
        BlockHound.registerIntegration(integration);
    } catch (Throwable ex) {
        // BlockHound not available
    }
}
```

用于检测响应式代码中的阻塞调用。

---

## 十、最佳实践

### 10.1 优先使用共享实例

```java
// 推荐
ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();

// 不推荐（除非有特殊需求）
ReactiveAdapterRegistry registry = new ReactiveAdapterRegistry();
```

### 10.2 处理找不到适配器的情况

```java
ReactiveAdapter adapter = registry.getAdapter(type);
if (adapter == null) {
    // 1. 记录日志
    log.warn("No reactive adapter for type: {}", type);

    // 2. 回退处理
    return Flux.just(value);

    // 或 3. 抛出异常
    throw new IllegalArgumentException("Unsupported type: " + type);
}
```

### 10.3 自定义适配器优先级

```java
// 使用 registerReactiveTypeOverride 插入到列表头部
// 优先于默认适配器
registry.registerReactiveTypeOverride(
    ReactiveTypeDescriptor.multiValue(MyFlux.class, MyFlux::empty),
    source -> ((MyFlux<?>) source).toPublisher(),
    publisher -> MyFlux.fromPublisher(publisher)
);
```

### 10.4 延迟初始化

```java
@Configuration
public class ReactiveConfig {

    private ReactiveAdapterRegistry reactiveAdapterRegistry;

    @Bean
    public ReactiveAdapterRegistry reactiveAdapterRegistry() {
        if (this.reactiveAdapterRegistry == null) {
            this.reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
        }
        return this.reactiveAdapterRegistry;
    }
}
```

---

## 十一、常见问题

### Q1: 为什么我的自定义类型没有被适配？

```java
// 确保注册了适配器
registry.registerReactiveType(
    descriptor,
    toPublisherFunction,
    fromPublisherFunction
);

// 确保类型匹配（精确匹配或继承匹配）
ReactiveAdapter adapter = registry.getAdapter(MyType.class);
// 或
ReactiveAdapter adapter = registry.getAdapter(null, instance);
```

### Q2: Mono 和 Flux 有什么区别？

| 特性 | Mono<T> | Flux<T> |
|-----|---------|---------|
| 值数量 | 0..1 | 0..N |
| 适用场景 | 单个对象、可能为空 | 集合、流 |
| HTTP 类比 | `ResponseEntity<T>` | `List<T>` |

### Q3: 可以混合使用多种响应式类型吗？

```java
// 可以，通过 ReactiveAdapterRegistry 统一转换
public Flux<User> merge(Mono<User> mono, Flowable<User> flowable) {
    ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();

    Publisher<User> pub1 = registry.getAdapter(null, mono).toPublisher(mono);
    Publisher<User> pub2 = registry.getAdapter(null, flowable).toPublisher(flowable);

    return Flux.merge(pub1, pub2);
}
```

### Q4: CompletableFuture 是响应式类型吗？

```java
// CompletableFuture 不是 Reactive Streams 类型
// 但 Spring 提供了适配器，可以当作 Mono 使用

CompletableFuture<String> future = CompletableFuture.completedFuture("test");

// 转为 Mono
Mono<String> mono = Mono.fromCompletionStage(future);

// 或在 WebFlux 中自动适配
@GetMapping("/future")
public CompletableFuture<String> getFuture() {
    return asyncService.compute();
}
```

---

## 十二、本课小结

### 核心要点

1. **ReactiveAdapterRegistry 是响应式类型的"万能转换器"**，让 Spring 能够统一处理 Reactor、RxJava、Kotlin Coroutines 等多种响应式库

2. **三大核心组件**：
   - `ReactiveAdapterRegistry`：注册表，管理所有适配器
   - `ReactiveAdapter`：适配器，封装双向转换逻辑
   - `ReactiveTypeDescriptor`：描述符，定义类型语义

3. **自动检测机制**：通过 `ClassUtils.isPresent()` 自动检测类路径上的响应式库

4. **转换流程**：
   ```
   任意响应式类型 → toPublisher() → Publisher → fromPublisher() → 目标类型
   ```

5. **主要应用场景**：
   - Spring WebFlux 控制器返回值处理
   - 响应式数据访问
   - WebClient HTTP 调用
   - RSocket 通信

### 代码模板

```java
// 获取注册表
ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();

// 获取适配器
ReactiveAdapter adapter = registry.getAdapter(Mono.class);

// 双向转换
Publisher<T> publisher = adapter.toPublisher(source);
Object target = adapter.fromPublisher(publisher);

// 注册自定义适配器
registry.registerReactiveType(
    ReactiveTypeDescriptor.multiValue(MyType.class, MyType::empty),
    source -> ((MyType<?>) source).toPublisher(),
    publisher -> MyType.fromPublisher(publisher)
);
```

---

## 参考资料

- [Reactive Streams Specification](https://www.reactive-streams.org/)
- [Project Reactor Documentation](https://projectreactor.io/docs/core/release/reference/)
- [RxJava 3 Wiki](https://github.com/ReactiveX/RxJava/wiki)
- [Spring WebFlux Reference](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
