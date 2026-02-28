# org.springframework.beans.factory 包详解

## 概述

`org.springframework.beans.factory` 包是 Spring Beans 模块的核心包，定义了 Bean 工厂接口体系。这是 Spring IoC 容器的基础，提供了：

1. **BeanFactory** - 访问 Spring bean 容器的根本接口
2. **层次化工厂支持** - HierarchicalBeanFactory 支持父子工厂关系
3. **可列举工厂** - ListableBeanFactory 支持枚举所有 bean 实例
4. **生命周期管理** - InitializingBean 和 DisposableBean 接口
5. **FactoryBean** - 用于创建复杂对象的工厂模式支持
6. **注入点支持** - ObjectProvider 用于延迟依赖注入

---

## 1. BeanFactory 接口体系

### 1.1 接口继承层次

```
BeanFactory (核心接口)
├── HierarchicalBeanFactory (分层支持)
│   └── ConfigurableBeanFactory (配置支持) [在 config 包]
│       └── ConfigurableListableBeanFactory (完整功能) [在 config 包]
└── ListableBeanFactory (列举支持)
    └── ConfigurableListableBeanFactory (完整功能)

AutowireCapableBeanFactory (自动装配支持) [在 config 包]
```

### 1.2 BeanFactory 接口

**位置**: `BeanFactory.java`

**核心概念**:

`BeanFactory` 是访问 Spring bean 容器的根本接口，它是 bean 容器的基本客户端视图。实现此接口的对象持有多个 bean 定义，每个 bean 定义由字符串名称唯一标识。

**FactoryBean 前缀**:

```java
// 用于引用 FactoryBean 本身，而不是它创建的对象
String FACTORY_BEAN_PREFIX = "&";
char FACTORY_BEAN_PREFIX_CHAR = '&';

// 示例：
// myJndiObject - 获取 FactoryBean 创建的对象
// &myJndiObject - 获取 FactoryBean 实例本身
```

**核心方法**:

| 方法签名 | 说明 |
|---------|------|
| `Object getBean(String name)` | 根据名称获取 bean 实例 |
| `<T> T getBean(String name, Class<T> requiredType)` | 根据名称和类型获取 bean |
| `Object getBean(String name, Object... args)` | 根据名称和构造参数获取 bean |
| `<T> T getBean(Class<T> requiredType)` | 根据类型获取唯一 bean |
| `<T> T getBean(Class<T> requiredType, Object... args)` | 根据类型和构造参数获取 bean |
| `<T> ObjectProvider<T> getBeanProvider(Class<T> requiredType)` | 获取 ObjectProvider（延迟访问）|
| `<T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType)` | 支持泛型的 ObjectProvider |
| `boolean containsBean(String name)` | 判断是否包含指定名称的 bean |
| `boolean isSingleton(String name)` | 判断 bean 是否为单例 |
| `boolean isPrototype(String name)` | 判断 bean 是否为原型 |
| `boolean isTypeMatch(String name, ResolvableType typeToMatch)` | 判断类型是否匹配 |
| `Class<?> getType(String name)` | 获取 bean 的类型 |
| `String[] getAliases(String name)` | 获取 bean 的所有别名 |

**Bean 生命周期（初始化顺序）**:

```
1. BeanNameAware.setBeanName
2. BeanClassLoaderAware.setBeanClassLoader
3. BeanFactoryAware.setBeanFactory
4. EnvironmentAware.setEnvironment
5. EmbeddedValueResolverAware.setEmbeddedValueResolver
6. ResourceLoaderAware.setResourceLoader (仅在应用上下文中)
7. ApplicationEventPublisherAware.setApplicationEventPublisher (仅在应用上下文中)
8. MessageSourceAware.setMessageSource (仅在应用上下文中)
9. ApplicationContextAware.setApplicationContext (仅在应用上下文中)
10. ServletContextAware.setServletContext (仅在 Web 应用上下文中)
11. BeanPostProcessor.postProcessBeforeInitialization
12. InitializingBean.afterPropertiesSet
13. 自定义 init-method
14. BeanPostProcessor.postProcessAfterInitialization
```

**Bean 生命周期（销毁顺序）**:

```
1. DestructionAwareBeanPostProcessor.postProcessBeforeDestruction
2. DisposableBean.destroy
3. 自定义 destroy-method
```

---

## 2. HierarchicalBeanFactory 接口

**位置**: `HierarchicalBeanFactory.java`

**作用**: 支持 bean 工厂的层次结构，允许设置父工厂。

**核心方法**:

```java
// 获取父级 bean 工厂
@Nullable
BeanFactory getParentBeanFactory();

// 判断本地工厂是否包含给定名称的 bean（忽略祖先工厂）
boolean containsLocalBean(String name);
```

**层次化查找规则**:
- 如果在当前工厂中找不到 bean，会询问父工厂
- 当前工厂中的 bean 定义会覆盖父工厂中同名的 bean
- `containsBean()` 会检查层次结构
- `containsLocalBean()` 只检查当前工厂

---

## 3. ListableBeanFactory 接口

**位置**: `ListableBeanFactory.java`

**作用**: 扩展 `BeanFactory`，支持枚举所有 bean 实例，而不是逐个按名称查找。

**核心方法**:

### 3.1 Bean 定义信息

```java
// 是否包含 bean 定义（不考虑层次结构）
boolean containsBeanDefinition(String beanName);

// 获取 bean 定义数量（不考虑层次结构）
int getBeanDefinitionCount();

// 获取所有 bean 定义名称
String[] getBeanDefinitionNames();
```

### 3.2 按类型获取

```java
// 获取匹配类型的 bean 名称数组（支持泛型）
String[] getBeanNamesForType(ResolvableType type);
String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit);

// 获取匹配类型的 bean 名称数组（Class 版本）
String[] getBeanNamesForType(@Nullable Class<?> type);
String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit);

// 获取匹配类型的 bean 实例 Map
<T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException;
<T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
        throws BeansException;
```

**参数说明**:
- `includeNonSingletons`: 是否包括原型或作用域 bean
- `allowEagerInit`: 是否允许为类型检查而初始化延迟加载的单例和 FactoryBean

### 3.3 ObjectProvider 获取

```java
// 获取 ObjectProvider（支持 allowEagerInit 参数）
<T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit);
<T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit);
```

### 3.4 注解支持

```java
// 查找带指定注解的 bean 名称
String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType);

// 查找带指定注解的 bean 实例
Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException;

// 在指定 bean 上查找注解
@Nullable
<A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
        throws NoSuchBeanDefinitionException;

@Nullable
<A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
        throws NoSuchBeanDefinitionException;

// 查找所有指定类型的注解
<A extends Annotation> Set<A> findAllAnnotationsOnBean(String beanName, Class<A> annotationType,
        boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException;
```

---

## 4. ObjectProvider 接口

**位置**: `ObjectProvider.java`

**作用**: 专门为注入点设计的 `ObjectFactory` 变体，允许程序化可选性和宽松的不唯一处理。

**特点**:
- 从 Spring 5.1 开始扩展 `Iterable`，支持 `for` 循环和 `Stream` 操作
- 从 Spring 6.2 开始，所有方法都有默认实现，便于自定义实现
- 支持延迟按需检索实例
- 区分 "不可用" 和 "不唯一" 的情况

**核心方法**:

### 4.1 基本获取

```java
// 获取对象（必须有且只有一个，否则抛异常）
@Override
default T getObject() throws BeansException {
    Iterator<T> it = iterator();
    if (!it.hasNext()) {
        throw new NoSuchBeanDefinitionException(Object.class);
    }
    T result = it.next();
    if (it.hasNext()) {
        throw new NoUniqueBeanDefinitionException(Object.class, 2, "more than 1 matching bean");
    }
    return result;
}

// 带参数的获取
default T getObject(Object... args) throws BeansException
```

### 4.2 可选获取

```java
// 如果可用则返回，否则返回 null（会抛出不唯一异常）
@Nullable
default T getIfAvailable() throws BeansException {
    try {
        return getObject();
    } catch (NoUniqueBeanDefinitionException ex) {
        throw ex;
    } catch (NoSuchBeanDefinitionException ex) {
        return null;
    }
}

// 如果可用则返回，否则使用默认供应商
default T getIfAvailable(Supplier<T> defaultSupplier) throws BeansException

// 如果可用则消费
default void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException
```

### 4.3 唯一获取

```java
// 如果唯一则返回，否则返回 null（找不到或多个都不返回异常）
@Nullable
default T getIfUnique() throws BeansException {
    try {
        return getObject();
    } catch (NoSuchBeanDefinitionException ex) {
        return null;
    }
}

// 如果唯一则返回，否则使用默认供应商
default T getIfUnique(Supplier<T> defaultSupplier) throws BeansException

// 如果唯一则消费
default void ifUnique(Consumer<T> dependencyConsumer) throws BeansException
```

### 4.4 迭代和流式访问

```java
// Iterable 接口实现
@Override
default Iterator<T> iterator() {
    return stream().iterator();
}

// 返回所有匹配对象的 Stream（通常是注册顺序）
default Stream<T> stream() {
    throw new UnsupportedOperationException("Element access not supported");
}

// 返回排序后的 Stream（根据 OrderComparator）
default Stream<T> orderedStream() {
    return stream().sorted(OrderComparator.INSTANCE);
}

// 自定义过滤的 Stream
default Stream<T> stream(Predicate<Class<?>> customFilter)
default Stream<T> stream(Predicate<Class<?>> customFilter, boolean includeNonSingletons)

// 自定义过滤的排序 Stream
default Stream<T> orderedStream(Predicate<Class<?>> customFilter)
default Stream<T> orderedStream(Predicate<Class<?>> customFilter, boolean includeNonSingletons)
```

**唯一性判定**:
- 遵守 "primary" 标志（只有一个标记为 primary 的候选者获胜）
- 遵守 "fallback" 标志（只有一个未标记为 fallback 的候选者获胜）
- 考虑 "default" 候选标志

---

## 5. FactoryBean 接口

**位置**: `FactoryBean.java`

**作用**: 由在 `BeanFactory` 中使用的对象实现，这些对象本身是单个对象的工厂。

**核心概念**:

如果一个 bean 实现了 `FactoryBean` 接口，它将被用作暴露对象的工厂，而不是直接作为 bean 实例本身。

**重要注意事项**:
1. 实现此接口的 bean **不能用作普通 bean**
2. FactoryBean 是程序化契约，不依赖注解驱动注入
3. 容器只管理 FactoryBean 实例的生命周期，而不是它创建的对象
4. FactoryBean 对象参与包含 BeanFactory 的 bean 创建同步

**核心方法**:

```java
// 对象类型属性名（用于在 BeanDefinition 上设置，指示对象类型）
String OBJECT_TYPE_ATTRIBUTE = "factoryBeanObjectType";

// 返回此工厂管理的对象实例（可能是共享的或独立的）
@Nullable
T getObject() throws Exception;

// 返回此 FactoryBean 创建的对象类型，如果事先不知道则返回 null
@Nullable
Class<?> getObjectType();

// 此工厂管理的对象是否是单例？
default boolean isSingleton() {
    return true;
}
```

**典型实现**:
- `ProxyFactoryBean` - AOP 代理创建
- `JndiObjectFactoryBean` - JNDI 对象查找
- `LocalSessionFactoryBean` - Hibernate SessionFactory 创建

---

## 6. 生命周期接口

### 6.1 InitializingBean 接口

**位置**: `InitializingBean.java`

**作用**: 由需要在 BeanFactory 设置完所有属性后做出反应的 bean 实现。

```java
public interface InitializingBean {
    // 在所有 bean 属性设置后调用
    void afterPropertiesSet() throws Exception;
}
```

**替代方案**: 指定自定义初始化方法（如在 XML bean 定义中的 `init-method`）

### 6.2 DisposableBean 接口

**位置**: `DisposableBean.java`

**作用**: 由希望在销毁时释放资源的 bean 实现。

```java
public interface DisposableBean {
    // 在销毁 bean 时调用
    void destroy() throws Exception;
}
```

**替代方案**:
- 实现 Java 的 `AutoCloseable` 接口
- 指定自定义销毁方法（如在 XML bean 定义中的 `destroy-method`）

### 6.3 SmartInitializingSingleton 接口

**位置**: `SmartInitializingSingleton.java`

**作用**: 在单例预实例化阶段结束时调用的回调接口。

```java
public interface SmartInitializingSingleton {
    void afterSingletonsInstantiated();
}
```

**触发时机**: 所有常规单例 bean 都实例化后，在 `DefaultListableBeanFactory.preInstantiateSingletons()` 中调用。

---

## 7. Aware 接口体系

### 7.1 Aware 标记接口

**位置**: `Aware.java`

```java
public interface Aware {
    // 标记接口，用于指示实现类可以通过回调风格方法被 Spring 容器通知
}
```

### 7.2 具体 Aware 接口

```java
// BeanNameAware - 通知 bean 它的名称
public interface BeanNameAware extends Aware {
    void setBeanName(String name);
}

// BeanClassLoaderAware - 通知 bean 类加载器
public interface BeanClassLoaderAware extends Aware {
    void setBeanClassLoader(ClassLoader classLoader);
}

// BeanFactoryAware - 通知 bean BeanFactory
public interface BeanFactoryAware extends Aware {
    void setBeanFactory(BeanFactory beanFactory) throws BeansException;
}
```

---

## 8. BeanFactoryUtils 工具类

**位置**: `BeanFactoryUtils.java`

**作用**: 在 bean 工厂上操作的便利方法，特别是在 `ListableBeanFactory` 接口上，考虑工厂的层次结构。

### 8.1 名称处理

```java
// 生成的 bean 名称分隔符（用于处理重复名称）
public static final String GENERATED_BEAN_NAME_SEPARATOR = "#";

// 判断是否是 FactoryBean 解引用（以 & 开头）
public static boolean isFactoryDereference(@Nullable String name)

// 返回实际 bean 名称（去掉 & 前缀）
public static String transformedBeanName(String name)

// 判断是否是生成的 bean 名称
public static boolean isGeneratedBeanName(@Nullable String name)

// 从生成的名称中提取原始 bean 名称
public static String originalBeanName(String name)
```

### 8.2 层次化查找（包含祖先工厂）

```java
// 统计所有 bean（包括祖先工厂）
public static int countBeansIncludingAncestors(ListableBeanFactory lbf)

// 获取所有 bean 名称（包括祖先工厂）
public static String[] beanNamesIncludingAncestors(ListableBeanFactory lbf)

// 获取匹配类型的 bean 名称（包括祖先工厂）
public static String[] beanNamesForTypeIncludingAncestors(ListableBeanFactory lbf, ResolvableType type)
public static String[] beanNamesForTypeIncludingAncestors(ListableBeanFactory lbf, Class<?> type)
public static String[] beanNamesForTypeIncludingAncestors(ListableBeanFactory lbf, Class<?> type,
        boolean includeNonSingletons, boolean allowEagerInit)

// 获取带注解的 bean 名称（包括祖先工厂）
public static String[] beanNamesForAnnotationIncludingAncestors(
        ListableBeanFactory lbf, Class<? extends Annotation> annotationType)

// 获取匹配类型的 bean 实例（包括祖先工厂）
public static <T> Map<String, T> beansOfTypeIncludingAncestors(ListableBeanFactory lbf, Class<T> type)
public static <T> Map<String, T> beansOfTypeIncludingAncestors(ListableBeanFactory lbf, Class<T> type,
        boolean includeNonSingletons, boolean allowEagerInit)

// 获取唯一匹配类型的 bean（包括祖先工厂）
public static <T> T beanOfTypeIncludingAncestors(ListableBeanFactory lbf, Class<T> type)
public static <T> T beanOfTypeIncludingAncestors(ListableBeanFactory lbf, Class<T> type,
        boolean includeNonSingletons, boolean allowEagerInit)
```

**层次化查找规则**:
- 同名的 bean 在子工厂中优先
- 子工厂中的 bean 会隐藏祖先工厂中的同名 bean
- 这允许通过在子工厂中显式选择相同 bean 名称来"替换"bean

---

## 9. 其他核心接口

### 9.1 ObjectFactory 接口

**位置**: `ObjectFactory.java`

```java
@FunctionalInterface
public interface ObjectFactory<T> {
    T getObject() throws BeansException;
}
```

简单的对象工厂接口，用于延迟创建对象。

### 9.2 NamedBean 接口

**位置**: `NamedBean.java`

```java
public interface NamedBean {
    String getBeanName();
}
```

用于获取 bean 名称的接口。

### 9.3 InjectionPoint 类

**位置**: `InjectionPoint.java`

封装注入点的元数据，可以是字段或方法/构造函数参数。

```java
public class InjectionPoint {
    // 字段或方法参数
    @Nullable
    protected final Field field;
    @Nullable
    protected final MethodParameter methodParameter;

    // 是否必需
    protected boolean required = true;

    // 获取注解
    public <A extends Annotation> A getAnnotation(Class<A> annotationType)

    // 获取字段/参数类型
    public Class<?> getDeclaredType()

    // 获取声明的类
    public Class<?> getDeclaredClass()
}
```

---

## 10. 异常类体系

```
BeansException
├── BeanCreationException              // Bean 创建失败
│   ├── BeanCreationNotAllowedException    // 不允许创建
│   └── FactoryBeanNotInitializedException // FactoryBean 未初始化
├── BeanDefinitionStoreException       // Bean 定义存储失败
├── BeanIsAbstractException            // Bean 是抽象的
├── BeanIsNotAFactoryException         // Bean 不是 FactoryBean
├── BeanNotOfRequiredTypeException     // Bean 类型不匹配
├── CannotLoadBeanClassException       // 无法加载 Bean 类
├── NoSuchBeanDefinitionException      // 找不到 Bean 定义
├── NoUniqueBeanDefinitionException    // 找到多个匹配的 Bean
└── UnsatisfiedDependencyException     // 依赖未满足
```

---

## 11. 使用示例

### 11.1 基本 BeanFactory 使用

```java
// 获取 bean
MyService service = (MyService) beanFactory.getBean("myService");

// 带类型安全的方式获取
MyService service = beanFactory.getBean("myService", MyService.class);

// 根据类型获取
MyService service = beanFactory.getBean(MyService.class);

// 使用 ObjectProvider（推荐用于可选依赖）
ObjectProvider<MyService> provider = beanFactory.getBeanProvider(MyService.class);
MyService service = provider.getIfAvailable(() -> new DefaultMyService());
```

### 11.2 FactoryBean 使用

```java
// 获取 FactoryBean 创建的对象
Object bean = beanFactory.getBean("myFactoryBean");

// 获取 FactoryBean 本身
FactoryBean<?> factoryBean = (FactoryBean<?>) beanFactory.getBean("&myFactoryBean");
```

### 11.3 实现 InitializingBean 和 DisposableBean

```java
@Component
public class MyBean implements InitializingBean, DisposableBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化逻辑
        System.out.println("Bean 初始化完成");
    }

    @Override
    public void destroy() throws Exception {
        // 销毁逻辑
        System.out.println("Bean 正在销毁");
    }
}
```

### 11.4 使用 BeanFactoryUtils

```java
// 获取包括祖先工厂的所有匹配 bean
Map<String, MyService> services = BeanFactoryUtils.beansOfTypeIncludingAncestors(
        beanFactory, MyService.class);

// 获取唯一的 bean
MyService service = BeanFactoryUtils.beanOfTypeIncludingAncestors(
        beanFactory, MyService.class);
```

### 11.5 ObjectProvider 注入

```java
@Component
public class MyComponent {

    // 延迟注入，支持可选依赖
    @Autowired
    private ObjectProvider<MyService> serviceProvider;

    public void doSomething() {
        // 获取服务（如果不存在返回 null）
        MyService service = serviceProvider.getIfAvailable();

        // 获取服务（如果不存在使用默认值）
        MyService service = serviceProvider.getIfAvailable(() -> new DefaultMyService());

        // 遍历所有匹配的服务
        serviceProvider.forEach(service -> service.process());

        // 获取排序后的流
        List<MyService> services = serviceProvider.orderedStream()
                .collect(Collectors.toList());
    }
}
```

---

## 12. 总结

`org.springframework.beans.factory` 包定义了 Spring IoC 容器的核心接口：

1. **BeanFactory** - 访问 bean 的根本接口，定义了获取 bean 的基本操作
2. **HierarchicalBeanFactory** - 支持父子工厂层次结构
3. **ListableBeanFactory** - 支持枚举所有 bean，按类型查找
4. **FactoryBean** - 用于创建复杂对象的工厂模式
5. **ObjectProvider** - 支持延迟注入和可选依赖
6. **生命周期接口** - InitializingBean 和 DisposableBean 用于初始化和销毁回调
7. **BeanFactoryUtils** - 考虑层次结构的便利工具方法

这些接口和类构成了 Spring IoC 容器的基础，具体的实现（如 `DefaultListableBeanFactory`）位于 `support` 子包中。
