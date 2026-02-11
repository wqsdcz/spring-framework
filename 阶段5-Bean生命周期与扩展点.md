# 阶段5：Bean生命周期与扩展点 - 详细学习指导

## 学习目标

1. 掌握Bean的完整生命周期流程
2. 理解InitializingBean、DisposableBean接口
3. 掌握BeanPostProcessor扩展机制
4. 理解Aware接口的作用

预计学习时间：4-5天

---

## Day 1: 生命周期概述与Aware接口

### Bean生命周期流程图

```
实例化前
    ↓ postProcessBeforeInstantiation
实例化（构造器）
    ↓
实例化后
    ↓ postProcessAfterInstantiation
属性处理
    ↓ postProcessProperties
属性填充（依赖注入）
    ↓
初始化前
    ↓ postProcessBeforeInitialization
    ↓ Aware接口注入
初始化
    ↓ InitializingBean.afterPropertiesSet()
    ↓ 自定义init-method
初始化后
    ↓ postProcessAfterInitialization
Bean就绪
    ↓
销毁前
    ↓ postProcessBeforeDestruction
销毁
    ↓ DisposableBean.destroy()
    ↓ 自定义destroy-method
```

### Aware接口体系

| 接口 | 注入内容 | 说明 |
|------|---------|------|
| BeanNameAware | bean名称 | 注入bean的名称 |
| BeanClassLoaderAware | 类加载器 | 注入加载bean的ClassLoader |
| BeanFactoryAware | BeanFactory | 注入创建bean的BeanFactory |
| EnvironmentAware | Environment | 注入环境配置（Spring 3.1+） |
| EmbeddedValueResolverAware | StringValueResolver | 注入值解析器 |
| ResourceLoaderAware | ResourceLoader | 注入资源加载器 |
| ApplicationEventPublisherAware | ApplicationEventPublisher | 注入事件发布器 |
| MessageSourceAware | MessageSource | 注入国际化消息源 |
| ApplicationContextAware | ApplicationContext | 注入应用上下文 |

### BeanNameAware示例

```java
@Component
public class NamedService implements BeanNameAware {
    private String beanName;

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
        System.out.println("My bean name is: " + name);
    }
}
```

### ApplicationContextAware示例

```java
@Component
public class ContextAwareService implements ApplicationContextAware {
    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
        // 可以通过context获取其他bean
    }
}
```

**注意**：Aware接口的使用破坏了IoC原则，应尽量避免，优先使用自动装配。

### 今日实践任务

1. 实现各种Aware接口，观察注入顺序
2. 思考：Aware接口和@Autowired获取ApplicationContext的区别

---

## Day 2: InitializingBean和DisposableBean

### InitializingBean接口

```java
public interface InitializingBean {
    void afterPropertiesSet() throws Exception;
}
```

**触发时机**：所有属性设置完成后，init-method之前。

### DisposableBean接口

```java
public interface DisposableBean {
    void destroy() throws Exception;
}
```

**触发时机**：容器关闭时，destroy-method之前。

### 完整示例

```java
@Component
public class LifecycleService implements InitializingBean, DisposableBean {

    private DatabaseConnection connection;

    // 构造器
    public LifecycleService() {
        System.out.println("1. 构造器执行");
    }

    // 依赖注入
    @Autowired
    public void setConnection(DatabaseConnection connection) {
        System.out.println("2. 属性注入");
        this.connection = connection;
    }

    // Aware接口
    @Override
    public void setBeanName(String name) {
        System.out.println("3. BeanNameAware.setBeanName");
    }

    // 初始化回调
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("4. InitializingBean.afterPropertiesSet");
        // 验证配置、建立连接等
        if (connection == null) {
            throw new IllegalStateException("Connection is required");
        }
    }

    // 自定义初始化方法
    @PostConstruct
    public void init() {
        System.out.println("5. @PostConstruct / init-method");
    }

    // 业务方法
    public void doSomething() {
        System.out.println("6. 业务方法执行");
    }

    // 销毁前回调
    @PreDestroy
    public void preDestroy() {
        System.out.println("7. @PreDestroy");
    }

    // 销毁回调
    @Override
    public void destroy() throws Exception {
        System.out.println("8. DisposableBean.destroy");
        // 关闭连接、释放资源
        if (connection != null) {
            connection.close();
        }
    }

    // 自定义销毁方法
    public void customDestroy() {
        System.out.println("9. destroy-method");
    }
}
```

### 三种初始化方式对比

| 方式 | 执行顺序 | 推荐使用 |
|------|---------|----------|
| @PostConstruct | 第1 | ⭐⭐⭐ 推荐 |
| InitializingBean | 第2 | ⭐⭐ 避免（侵入性） |
| init-method | 第3 | ⭐⭐ XML配置时使用 |

### 三种销毁方式对比

| 方式 | 执行顺序 | 推荐使用 |
|------|---------|----------|
| @PreDestroy | 第1 | ⭐⭐⭐ 推荐 |
| DisposableBean | 第2 | ⭐⭐ 避免（侵入性） |
| destroy-method | 第3 | ⭐⭐ XML配置时使用 |

### 今日实践任务

1. 实现InitializingBean和DisposableBean
2. 对比三种初始化/销毁方式
3. 思考：为什么优先使用注解方式？

---

## Day 3: BeanPostProcessor扩展机制

### BeanPostProcessor接口

```java
public interface BeanPostProcessor {
    @Nullable
    default Object postProcessBeforeInitialization(Object bean, String beanName)
            throws BeansException {
        return bean;
    }

    @Nullable
    default Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        return bean;
    }
}
```

### 使用场景

| 场景 | 使用时机 |
|------|---------|
| AOP代理创建 | postProcessAfterInitialization |
| 标记接口处理 | postProcessBeforeInitialization |
| 属性校验 | postProcessAfterInitialization |
| Bean包装 | postProcessAfterInitialization |

### 自定义BeanPostProcessor示例

```java
@Component
public class ValidationBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 检查是否有@Validated注解
        if (bean.getClass().isAnnotationPresent(Validated.class)) {
            // 创建代理进行验证
            return createValidationProxy(bean);
        }
        return bean;
    }

    private Object createValidationProxy(Object target) {
        // 使用JDK动态代理或CGLIB创建代理
        return Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),
            (proxy, method, args) -> {
                // 方法调用前验证参数
                validateParameters(method, args);
                return method.invoke(target, args);
            }
        );
    }
}
```

### InstantiationAwareBeanPostProcessor

```java
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {
    // 实例化前：可以返回代理替代正常实例化
    default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
        return null;
    }

    // 实例化后：返回false会跳过属性填充
    default boolean postProcessAfterInstantiation(Object bean, String beanName) {
        return true;
    }

    // 属性处理：修改PropertyValues
    default PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
        return null;
    }
}
```

### DestructionAwareBeanPostProcessor

```java
public interface DestructionAwareBeanPostProcessor extends BeanPostProcessor {
    void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException;

    default boolean requiresDestruction(Object bean) {
        return true;
    }
}
```

### BeanPostProcessor执行顺序

1. 实现`PriorityOrdered`的Processor
2. 实现`Ordered`的Processor
3. 无排序的Processor
4. 按注册顺序执行

```java
@Component
@Order(1)  // 控制执行顺序
public class FirstBeanPostProcessor implements BeanPostProcessor, Ordered {
    @Override
    public int getOrder() {
        return 1;
    }
}
```

### 今日实践任务

1. 实现自定义BeanPostProcessor
2. 观察多个Processor的执行顺序
3. 使用InstantiationAwareBeanPostProcessor控制实例化

---

## Day 4: 常用生命周期处理器

### AutowiredAnnotationBeanPostProcessor

- 处理`@Autowired`、`@Value`、`@Inject`
- 实现`InstantiationAwareBeanPostProcessor`
- 优先级：`PriorityOrdered`（最高优先级）

### CommonAnnotationBeanPostProcessor

- 处理`@PostConstruct`、`@PreDestroy`
- 处理`@Resource`（JSR-250）
- 优先级：`Ordered.LOWEST_PRECEDENCE - 3`

### ApplicationContextAwareProcessor

- 处理各种Aware接口
- ApplicationContext自动注册
- 在BeanPostProcessor之前执行

### InitDestroyAnnotationBeanPostProcessor

```java
// 处理@PostConstruct和@PreDestroy
public class InitDestroyAnnotationBeanPostProcessor implements
        DestructionAwareBeanPostProcessor, MergedBeanDefinitionPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        // 查找并调用@PostConstruct方法
        LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
        metadata.invokeInitMethods(bean, beanName);
        return bean;
    }

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) {
        // 查找并调用@PreDestroy方法
        LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
        metadata.invokeDestroyMethods(bean, beanName);
    }
}
```

### 生命周期元数据缓存

```java
// 在postProcessMergedBeanDefinition阶段缓存元数据
@Override
public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition,
        Class<?> beanType, String beanName) {
    LifecycleMetadata metadata = findLifecycleMetadata(beanType);
    metadata.checkConfigMembers(beanDefinition);
}
```

### 今日实践任务

1. 了解Spring内置的生命周期处理器
2. 跟踪@PostConstruct的处理过程
3. 理解生命周期元数据的缓存机制

---

## Day 5: 实践与调试

### 完整生命周期调试

```java
@Component
public class LifecycleDebugBean implements
        BeanNameAware,
        BeanFactoryAware,
        InitializingBean,
        DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(LifecycleDebugBean.class);

    public LifecycleDebugBean() {
        log.info("1. Constructor");
    }

    @Override
    public void setBeanName(String name) {
        log.info("2. BeanNameAware.setBeanName: {}", name);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        log.info("3. BeanFactoryAware.setBeanFactory");
    }

    @Autowired
    public void setDependency(SomeDependency dep) {
        log.info("4. Autowired setter");
    }

    @PostConstruct
    public void postConstruct() {
        log.info("5. @PostConstruct");
    }

    @Override
    public void afterPropertiesSet() {
        log.info("6. InitializingBean.afterPropertiesSet");
    }

    public void initMethod() {
        log.info("7. init-method");
    }

    @PreDestroy
    public void preDestroy() {
        log.info("8. @PreDestroy");
    }

    @Override
    public void destroy() {
        log.info("9. DisposableBean.destroy");
    }

    public void destroyMethod() {
        log.info("10. destroy-method");
    }
}
```

### 配置

```java
@Configuration
public class LifecycleConfig {

    @Bean(initMethod = "initMethod", destroyMethod = "destroyMethod")
    public LifecycleDebugBean lifecycleDebugBean() {
        return new LifecycleDebugBean();
    }
}
```

### 调试检查点

在以下方法打断点：

1. `AbstractAutowireCapableBeanFactory.createBean`
2. `AbstractAutowireCapableBeanFactory.populateBean`
3. `AbstractAutowireCapableBeanFactory.initializeBean`
4. `AbstractAutowireCapableBeanFactory.invokeAwareMethods`
5. `AbstractAutowireCapableBeanFactory.invokeInitMethods`

### 验证检查清单

- [ ] 能画出完整的生命周期流程图
- [ ] 理解Aware接口的作用和注入顺序
- [ ] 掌握三种初始化/销毁方式
- [ ] 理解BeanPostProcessor的扩展机制
- [ ] 通过调试观察过完整生命周期

---

## 常见问题

### Q1: @PostConstruct和afterPropertiesSet哪个先执行？

**A**: @PostConstruct先执行。顺序是：
1. @PostConstruct（CommonAnnotationBeanPostProcessor）
2. InitializingBean.afterPropertiesSet
3. init-method

### Q2: BeanPostProcessor可以返回null吗？

**A**: 可以，但返回null会中断后续BeanPostProcessor的调用。通常应该返回bean参数或其包装。

### Q3: 为什么ApplicationContextAwareProcessor在BeanPostProcessor之前执行？

**A**: ApplicationContextAwareProcessor实际上是一个特殊的处理器，它在BeanPostProcessor链之前执行，确保Aware接口的注入在其他处理之前完成。

### Q4: 如何在BeanPostProcessor中区分原始Bean和代理Bean？

**A**: 可以使用`AopUtils.isAopProxy(bean)`或`bean instanceof Advised`来判断是否是代理对象。

### Q5: Prototype Bean的销毁回调会执行吗？

**A**: 会执行，但需要手动触发。容器关闭时只会销毁Singleton Bean。Prototype Bean的销毁需要：
- 使用@PreDestroy（通过DestructionAwareBeanPostProcessor）
- 手动调用ConfigurableBeanFactory.destroyBean(beanName, beanInstance)

---

## 扩展阅读

### 下一阶段预告

阶段6将学习配置方式：
- XML配置解析
- Groovy DSL配置
- 不同配置方式的对比

### 推荐阅读源码

1. `CommonAnnotationBeanPostProcessor` - JSR-250注解处理
2. `ApplicationContextAwareProcessor` - Aware接口处理
3. `RequiredAnnotationBeanPostProcessor` - @Required处理

---

## 总结

完成阶段5后，你应该能够：

1. ✅ 掌握Bean的完整生命周期流程
2. ✅ 理解InitializingBean和DisposableBean的使用
3. ✅ 掌握BeanPostProcessor扩展机制
4. ✅ 理解Aware接口的作用
5. ✅ 能够自定义生命周期处理器

**核心流程记忆**：
```
构造器 → 属性注入 → Aware → @PostConstruct → afterPropertiesSet → init-method
                                                                    ↓
                                                              Bean就绪
                                                                    ↓
@PreDestroy → destroy → destroy-method
```
