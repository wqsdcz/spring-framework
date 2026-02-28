# Spring Beans Factory Annotation 包详解

## 概述

`org.springframework.beans.factory.annotation` 包是 Spring 框架中用于注解驱动 Bean 配置的核心包。它提供了依赖注入（DI）的基础注解、Bean 后处理器、元数据封装以及相关的工具类。

---

## 一、核心注解

### 1.1 @Autowired - 自动装配注解

**文件路径**: `spring-beans/src/main/java/org/springframework/beans/factory/annotation/Autowired.java`

#### 用途

`@Autowired` 标记构造函数、字段、setter 方法或配置方法，表示应由 Spring 的依赖注入工具进行自动装配。它是 JSR-330 `@Inject` 注解的替代方案，增加了必需与可选的语义。

#### 属性

```java
public @interface Autowired {
    /**
     * 声明注解的依赖项是否必需。
     * 默认为 true。
     */
    boolean required() default true;
}
```

#### 使用位置

- `ElementType.CONSTRUCTOR` - 构造函数
- `ElementType.METHOD` - 方法
- `ElementType.PARAMETER` - 参数
- `ElementType.FIELD` - 字段
- `ElementType.ANNOTATION_TYPE` - 注解类型（作为元注解）

#### 使用示例

```java
@Component
public class UserService {

    // 字段注入（字段不必是 public）
    @Autowired
    private UserRepository userRepository;

    // 构造函数注入（推荐方式）
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 可选依赖
    @Autowired(required = false)
    private EmailService emailService;

    // Setter 方法注入
    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 配置方法注入（可以有任意名称和任意数量的参数）
    @Autowired
    public void configure(UserRepository repo, EmailService email) {
        this.userRepository = repo;
        this.emailService = email;
    }
}
```

#### 自动装配规则

1. **构造函数自动装配**:
   - 只有一个构造函数可以声明 `required = true`
   - 多个非必需构造函数时，选择能满足最多依赖项的构造函数
   - 只有一个构造函数时，即使没有注解也会被使用

2. **数组、集合和映射自动装配**:
   - 数组、`Collection` 或 `Map` 类型会注入所有匹配的 Bean
   - Map 的键必须是 `String` 类型，对应 Bean 名称
   - 集合会按 `@Order` 或注册顺序排序

---

### 1.2 @Qualifier - 限定符注解

**文件路径**: `spring-beans/src/main/java/org/springframework/beans/factory/annotation/Qualifier.java`

#### 用途

`@Qualifier` 用于在自动装配时区分相同类型的多个 Bean。它可以用在字段或参数上作为限定符，也可以用作元注解来创建自定义限定符注解。

#### 属性

```java
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
         ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Qualifier {
    String value() default "";
}
```

#### 使用示例

```java
// 定义自定义限定符注解
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface MySql {
}

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface MongoDB {
}

// 使用限定符标记 Bean
@Component
@MySql
public class MySqlUserRepository implements UserRepository {
}

@Component
@MongoDB
public class MongoUserRepository implements UserRepository {
}

// 使用限定符注入
@Component
public class UserService {
    @Autowired
    @MySql
    private UserRepository userRepository;

    // 或在构造函数参数上使用
    @Autowired
    public UserService(@MongoDB UserRepository mongoRepo) {
        this.mongoRepo = mongoRepo;
    }
}
```

---

### 1.3 @Value - 值注入注解

**文件路径**: `spring-beans/src/main/java/org/springframework/beans/factory/annotation/Value.java`

#### 用途

`@Value` 用于注入配置值，支持 SpEL 表达式和属性占位符。通常用于表达式驱动或属性驱动的依赖注入。

#### 属性

```java
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {
    /**
     * 实际值表达式，如 #{systemProperties.myProp} 或 ${my.app.myProp}
     */
    String value();
}
```

#### 使用示例

```java
@Component
public class AppConfig {

    // 注入属性占位符
    @Value("${app.name}")
    private String appName;

    // 注入带默认值的属性
    @Value("${app.timeout:3000}")
    private int timeout;

    // 使用 SpEL 表达式
    @Value("#{systemProperties['user.home']}")
    private String userHome;

    // 注入系统环境变量
    @Value("#{systemEnvironment['PATH']}")
    private String path;

    // 注入其他 Bean 的属性
    @Value("#{anotherBean.property}")
    private String otherProperty;

    // 注入资源
    @Value("classpath:config.json")
    private Resource configResource;
}
```

---

### 1.4 @Lookup - 查找方法注解

**文件路径**: `spring-beans/src/main/java/org/springframework/beans/factory/annotation/Lookup.java`

#### 用途

`@Lookup` 标记"查找"方法，容器会重写这些方法，将它们重定向回 `BeanFactory` 进行 `getBean` 调用。这是基于注解的 XML `lookup-method` 属性的版本。

#### 实现原理

1. 容器通过 CGLIB 生成包含查找方法的类的运行时子类
2. 查找方法可以有默认（存根）实现或声明为抽象方法
3. 目标 Bean 的解析可以基于返回类型或建议的 Bean 名称

#### 属性

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lookup {
    /**
     * 建议要查找的目标 bean 名称。
     * 如果未指定，则根据返回类型解析目标 bean。
     */
    String value() default "";
}
```

#### 使用示例

```java
@Component
public abstract class CommandManager {

    // 每次调用都获取新的 Command 实例（原型作用域）
    @Lookup
    protected abstract Command createCommand();

    // 或使用具体实现
    @Lookup("myCommand")
    protected abstract Command getCommand();

    public Object process(Object commandState) {
        Command command = createCommand();
        command.setState(commandState);
        return command.execute();
    }
}

// 原型作用域的 Command
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MyCommand implements Command {
    // ...
}
```

#### 重要限制

- 查找方法无法在从工厂方法返回的 Bean 上被替换
- 配置类中的 `@Bean` 方法返回的 Bean 不支持 `@Lookup`
- 必须使用 CGLIB 生成子类，所以不能是 final 类

---

### 1.5 @Configurable - 可配置注解

**文件路径**: `spring-beans/src/main/java/org/springframework/beans/factory/annotation/Configurable.java`

#### 用途

`@Configurable` 标记类有资格进行 Spring 驱动的配置。通常与 AspectJ 的 `AnnotationBeanConfigurerAspect` 一起使用，用于在 Spring 容器外创建的对象进行依赖注入。

#### 属性

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Configurable {
    /**
     * 作为配置模板的 bean 定义名称
     */
    String value() default "";

    /**
     * 是否通过自动装配注入依赖
     */
    Autowire autowire() default Autowire.NO;

    /**
     * 是否为配置的对象执行依赖检查
     */
    boolean dependencyCheck() default false;

    /**
     * 是否在对象构造之前注入依赖
     */
    boolean preConstruction() default false;
}
```

#### 使用示例

```java
// 使用 AspectJ 进行领域对象的依赖注入
@Configurable(autowire = Autowire.BY_TYPE, preConstruction = true)
public class DomainEntity {
    @Autowired
    private transient UserRepository userRepository;

    public void doSomething() {
        // 可以使用注入的 repository
        userRepository.save(...);
    }
}
```

---

### 1.6 Autowire - 自动装配枚举

**文件路径**: `spring-beans/src/main/java/org/springframework/beans/factory/annotation/Autowire.java`

#### 用途

枚举确定自动装配状态：即 Bean 是否应该通过 setter 注入自动注入其依赖项。

#### 枚举值

```java
public enum Autowire {
    NO(AutowireCapableBeanFactory.AUTOWIRE_NO),      // 不自动装配
    BY_NAME(AutowireCapableBeanFactory.AUTOWIRE_BY_NAME),  // 按名称自动装配
    BY_TYPE(AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE);  // 按类型自动装配
}
```

---

## 二、BeanPostProcessor 实现

### 2.1 AutowiredAnnotationBeanPostProcessor - 自动装配注解处理器

**文件路径**: `spring-beans/src/main/java/org/springframework/beans/factory/annotation/AutowiredAnnotationBeanPostProcessor.java`

#### 概述

这是 Spring 依赖注入的核心处理器，负责处理 `@Autowired`、`@Value` 和 `@Lookup` 注解。它实现了多个接口：

- `MergedBeanDefinitionPostProcessor` - 合并 Bean 定义后处理
- `InstantiationAwareBeanPostProcessor` - 实例化感知后处理
- `BeanRegistrationAotProcessor` - AOT 编译支持
- `PriorityOrdered` - 优先级排序

#### 核心属性

```java
public class AutowiredAnnotationBeanPostProcessor implements
        MergedBeanDefinitionPostProcessor, InstantiationAwareBeanPostProcessor,
        BeanRegistrationAotProcessor, PriorityOrdered, BeanFactoryAware {

    // 要处理的注解类型集合（默认可配置）
    private final Set<Class<? extends Annotation>> autowiredAnnotationTypes = new LinkedHashSet<>();

    // 缓存：类的构造函数候选者
    private final Map<Class<?>, Constructor<?>[]> candidateConstructorsCache = new ConcurrentHashMap<>(256);

    // 缓存：注入元数据
    private final Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);

    // 已检查的 lookup 方法
    private final Set<String> lookupMethodsChecked = Collections.newSetFromMap(new ConcurrentHashMap<>(256));
}
```

#### 构造函数与初始化

```java
public AutowiredAnnotationBeanPostProcessor() {
    // 默认处理 @Autowired 和 @Value 注解
    this.autowiredAnnotationTypes.add(Autowired.class);
    this.autowiredAnnotationTypes.add(Value.class);

    // 尝试添加 JSR-330 的 @Inject 注解
    try {
        this.autowiredAnnotationTypes.add((Class<? extends Annotation>)
                ClassUtils.forName("jakarta.inject.Inject", AutowiredAnnotationBeanPostProcessor.class.getClassLoader()));
    }
    catch (ClassNotFoundException ex) {
        // JSR-330 API 不可用 - 跳过
    }
}
```

#### 核心方法详解

##### 1. postProcessMergedBeanDefinition - 合并 Bean 定义后处理

```java
@Override
public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition,
        Class<?> beanType, String beanName) {
    // 查找并缓存注入元数据
    InjectionMetadata metadata = findAutowiringMetadata(beanName, beanType, null);
    // 注册外部管理的配置成员
    metadata.checkConfigMembers(beanDefinition);
}
```

##### 2. determineCandidateConstructors - 确定候选构造函数

```java
@Override
@Nullable
public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, final String beanName) {
    // 检查 lookup 方法
    checkLookupMethods(beanClass, beanName);

    // 从缓存获取
    Constructor<?>[] candidateConstructors = this.candidateConstructorsCache.get(beanClass);
    if (candidateConstructors == null) {
        synchronized (this.candidateConstructorsCache) {
            // 双重检查
            candidateConstructors = this.candidateConstructorsCache.get(beanClass);
            if (candidateConstructors == null) {
                Constructor<?>[] rawCandidates = beanClass.getDeclaredConstructors();
                List<Constructor<?>> candidates = new ArrayList<>();
                Constructor<?> requiredConstructor = null;
                Constructor<?> defaultConstructor = null;

                for (Constructor<?> candidate : rawCandidates) {
                    // 查找 @Autowired 注解
                    MergedAnnotation<?> ann = findAutowiredAnnotation(candidate);
                    if (ann != null) {
                        boolean required = determineRequiredStatus(ann);
                        if (required) {
                            if (requiredConstructor != null) {
                                throw new BeanCreationException(beanName,
                                    "Invalid autowire-marked constructor...");
                            }
                            requiredConstructor = candidate;
                        }
                        candidates.add(candidate);
                    }
                    else if (candidate.getParameterCount() == 0) {
                        defaultConstructor = candidate;
                    }
                }

                // 确定最终候选构造函数
                if (!candidates.isEmpty()) {
                    if (requiredConstructor == null && defaultConstructor != null) {
                        candidates.add(defaultConstructor);
                    }
                    candidateConstructors = candidates.toArray(EMPTY_CONSTRUCTOR_ARRAY);
                }
                // 缓存结果
                this.candidateConstructorsCache.put(beanClass, candidateConstructors);
            }
        }
    }
    return (candidateConstructors.length > 0 ? candidateConstructors : null);
}
```

##### 3. checkLookupMethods - 检查 Lookup 方法

```java
private void checkLookupMethods(Class<?> beanClass, final String beanName) {
    if (!this.lookupMethodsChecked.contains(beanName)) {
        if (AnnotationUtils.isCandidateClass(beanClass, Lookup.class)) {
            Class<?> targetClass = beanClass;
            do {
                ReflectionUtils.doWithLocalMethods(targetClass, method -> {
                    Lookup lookup = method.getAnnotation(Lookup.class);
                    if (lookup != null) {
                        // 创建 LookupOverride 并添加到 Bean 定义
                        LookupOverride override = new LookupOverride(method, lookup.value());
                        RootBeanDefinition mbd = (RootBeanDefinition)
                            this.beanFactory.getMergedBeanDefinition(beanName);
                        mbd.getMethodOverrides().addOverride(override);
                    }
                });
                targetClass = targetClass.getSuperclass();
            }
            while (targetClass != null && targetClass != Object.class);
        }
        this.lookupMethodsChecked.add(beanName);
    }
}
```

##### 4. postProcessProperties - 属性后处理（核心注入逻辑）

```java
@Override
public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
    // 查找注入元数据
    InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), pvs);
    try {
        // 执行注入
        metadata.inject(bean, beanName, pvs);
    }
    catch (BeanCreationException ex) {
        throw ex;
    }
    catch (Throwable ex) {
        throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", ex);
    }
    return pvs;
}
```

##### 5. buildAutowiringMetadata - 构建自动装配元数据

```java
private InjectionMetadata buildAutowiringMetadata(Class<?> clazz) {
    // 检查是否是候选类（是否有相关注解）
    if (!AnnotationUtils.isCandidateClass(clazz, this.autowiredAnnotationTypes)) {
        return InjectionMetadata.EMPTY;
    }

    List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
    Class<?> targetClass = ClassUtils.getUserClass(clazz);

    do {
        final List<InjectionMetadata.InjectedElement> fieldElements = new ArrayList<>();

        // 处理字段
        ReflectionUtils.doWithLocalFields(targetClass, field -> {
            MergedAnnotation<?> ann = findAutowiredAnnotation(field);
            if (ann != null) {
                // 不支持静态字段
                if (Modifier.isStatic(field.getModifiers())) {
                    logger.info("Autowired annotation is not supported on static fields: " + field);
                    return;
                }
                boolean required = determineRequiredStatus(ann);
                fieldElements.add(new AutowiredFieldElement(field, required));
            }
        });

        // 处理方法
        final List<InjectionMetadata.InjectedElement> methodElements = new ArrayList<>();
        ReflectionUtils.doWithLocalMethods(targetClass, method -> {
            if (method.isBridge()) {
                return;
            }
            MergedAnnotation<?> ann = findAutowiredAnnotation(method);
            if (ann != null) {
                // 不支持静态方法
                if (Modifier.isStatic(method.getModifiers())) {
                    logger.info("Autowired annotation is not supported on static methods: " + method);
                    return;
                }
                // 方法必须有参数
                if (method.getParameterCount() == 0) {
                    logger.info("Autowired annotation should only be used on methods with parameters: " + method);
                }
                boolean required = determineRequiredStatus(ann);
                PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method, clazz);
                methodElements.add(new AutowiredMethodElement(method, required, pd));
            }
        });

        // 添加到元素列表（字段在前，方法在后）
        elements.addAll(0, sortMethodElements(methodElements, targetClass));
        elements.addAll(0, fieldElements);
        targetClass = targetClass.getSuperclass();
    }
    while (targetClass != null && targetClass != Object.class);

    return InjectionMetadata.forElements(elements, clazz);
}
```

#### 内部类详解

##### AutowiredFieldElement - 自动装配字段元素

```java
private class AutowiredFieldElement extends AutowiredElement {
    private volatile boolean cached;
    @Nullable
    private volatile Object cachedFieldValue;

    @Override
    protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs)
            throws Throwable {
        Field field = (Field) this.member;
        Object value;

        // 使用缓存的值
        if (this.cached) {
            try {
                value = resolveCachedArgument(beanName, this.cachedFieldValue);
            }
            catch (BeansException ex) {
                // 缓存不匹配，重新解析
                value = resolveFieldValue(field, bean, beanName);
            }
        }
        else {
            value = resolveFieldValue(field, bean, beanName);
        }

        if (value != null) {
            ReflectionUtils.makeAccessible(field);
            field.set(bean, value);
        }
    }

    @Nullable
    private Object resolveFieldValue(Field field, Object bean, @Nullable String beanName) {
        // 创建依赖描述符
        DependencyDescriptor desc = new DependencyDescriptor(field, this.required);
        desc.setContainingClass(bean.getClass());
        Set<String> autowiredBeanNames = new LinkedHashSet<>(2);

        // 解析依赖
        TypeConverter typeConverter = beanFactory.getTypeConverter();
        Object value = beanFactory.resolveDependency(desc, beanName, autowiredBeanNames, typeConverter);

        // 缓存结果
        synchronized (this) {
            if (!this.cached) {
                if (value != null || this.required) {
                    Object cachedFieldValue = desc;
                    registerDependentBeans(beanName, autowiredBeanNames);

                    // 如果只有一个匹配的 bean，使用 ShortcutDependencyDescriptor 优化
                    if (value != null && autowiredBeanNames.size() == 1) {
                        String autowiredBeanName = autowiredBeanNames.iterator().next();
                        if (beanFactory.containsBean(autowiredBeanName) &&
                                beanFactory.isTypeMatch(autowiredBeanName, field.getType())) {
                            cachedFieldValue = new ShortcutDependencyDescriptor(desc, autowiredBeanName);
                        }
                    }
                    this.cachedFieldValue = cachedFieldValue;
                    this.cached = true;
                }
            }
        }
        return value;
    }
}
```

##### AutowiredMethodElement - 自动装配方法元素

```java
private class AutowiredMethodElement extends AutowiredElement {
    private volatile boolean cached;
    @Nullable
    private volatile Object[] cachedMethodArguments;

    @Override
    protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs)
            throws Throwable {
        if (!shouldInject(pvs)) {
            return;
        }

        Method method = (Method) this.member;
        Object[] arguments;

        if (this.cached) {
            arguments = resolveCachedArguments(beanName, this.cachedMethodArguments);
        }
        else {
            arguments = resolveMethodArguments(method, bean, beanName);
        }

        if (arguments != null) {
            ReflectionUtils.makeAccessible(method);
            method.invoke(bean, arguments);
        }
    }

    @Nullable
    private Object[] resolveMethodArguments(Method method, Object bean, @Nullable String beanName) {
        int argumentCount = method.getParameterCount();
        Object[] arguments = new Object[argumentCount];
        DependencyDescriptor[] descriptors = new DependencyDescriptor[argumentCount];
        Set<String> autowiredBeanNames = CollectionUtils.newLinkedHashSet(argumentCount);

        TypeConverter typeConverter = beanFactory.getTypeConverter();

        // 解析每个参数
        for (int i = 0; i < arguments.length; i++) {
            MethodParameter methodParam = new MethodParameter(method, i);
            DependencyDescriptor currDesc = new DependencyDescriptor(methodParam, this.required);
            currDesc.setContainingClass(bean.getClass());
            descriptors[i] = currDesc;

            try {
                Object arg = beanFactory.resolveDependency(currDesc, beanName, autowiredBeanNames, typeConverter);
                if (arg == null && !this.required && !methodParam.isOptional()) {
                    arguments = null;
                    break;
                }
                arguments[i] = arg;
            }
            catch (BeansException ex) {
                throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(methodParam), ex);
            }
        }

        // 缓存结果
        synchronized (this) {
            if (!this.cached) {
                if (arguments != null) {
                    DependencyDescriptor[] cachedMethodArguments = Arrays.copyOf(descriptors, argumentCount);
                    registerDependentBeans(beanName, autowiredBeanNames);

                    // 优化：如果参数数量和自动装配 bean 数量匹配，使用 ShortcutDependencyDescriptor
                    if (autowiredBeanNames.size() == argumentCount) {
                        Iterator<String> it = autowiredBeanNames.iterator();
                        Class<?>[] paramTypes = method.getParameterTypes();
                        for (int i = 0; i < paramTypes.length; i++) {
                            String autowiredBeanName = it.next();
                            if (arguments[i] != null && beanFactory.containsBean(autowiredBeanName) &&
                                    beanFactory.isTypeMatch(autowiredBeanName, paramTypes[i])) {
                                cachedMethodArguments[i] = new ShortcutDependencyDescriptor(
                                    descriptors[i], autowiredBeanName);
                            }
                        }
                    }
                    this.cachedMethodArguments = cachedMethodArguments;
                    this.cached = true;
                }
            }
        }
        return arguments;
    }
}
```

---

### 2.2 InitDestroyAnnotationBeanPostProcessor - 生命周期注解处理器

**文件路径**: `spring-beans/src/main/java/org/springframework/beans/factory/annotation/InitDestroyAnnotationBeanPostProcessor.java`

#### 概述

处理带有初始化注解和销毁注解的方法，作为 Spring 的 `InitializingBean` 和 `DisposableBean` 回调接口的替代方案。

#### 核心功能

```java
public class InitDestroyAnnotationBeanPostProcessor implements
        DestructionAwareBeanPostProcessor,
        MergedBeanDefinitionPostProcessor,
        BeanRegistrationAotProcessor,
        PriorityOrdered,
        Serializable {

    // 初始化注解类型集合
    private final Set<Class<? extends Annotation>> initAnnotationTypes = new LinkedHashSet<>(2);

    // 销毁注解类型集合
    private final Set<Class<? extends Annotation>> destroyAnnotationTypes = new LinkedHashSet<>(2);

    // 生命周期元数据缓存
    private final Map<Class<?>, LifecycleMetadata> lifecycleMetadataCache = new ConcurrentHashMap<>(256);
}
```

#### 配置方法

```java
// 设置初始化注解类型（如 @PostConstruct）
public void setInitAnnotationType(Class<? extends Annotation> initAnnotationType) {
    this.initAnnotationTypes.clear();
    this.initAnnotationTypes.add(initAnnotationType);
}

// 添加额外的初始化注解类型
public void addInitAnnotationType(@Nullable Class<? extends Annotation> initAnnotationType) {
    if (initAnnotationType != null) {
        this.initAnnotationTypes.add(initAnnotationType);
    }
}

// 设置销毁注解类型（如 @PreDestroy）
public void setDestroyAnnotationType(Class<? extends Annotation> destroyAnnotationType) {
    this.destroyAnnotationTypes.clear();
    this.destroyAnnotationTypes.add(destroyAnnotationType);
}

// 添加额外的销毁注解类型
public void addDestroyAnnotationType(@Nullable Class<? extends Annotation> destroyAnnotationType) {
    if (destroyAnnotationType != null) {
        this.destroyAnnotationTypes.add(destroyAnnotationType);
    }
}
```

#### 生命周期方法调用

```java
@Override
public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
    try {
        // 调用初始化方法
        metadata.invokeInitMethods(bean, beanName);
    }
    catch (InvocationTargetException ex) {
        throw new BeanCreationException(beanName, "Invocation of init method failed", ex.getTargetException());
    }
    catch (Throwable ex) {
        throw new BeanCreationException(beanName, "Failed to invoke init method", ex);
    }
    return bean;
}

@Override
public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
    LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
    try {
        // 调用销毁方法
        metadata.invokeDestroyMethods(bean, beanName);
    }
    catch (InvocationTargetException ex) {
        // 销毁方法异常处理
        if (logger.isDebugEnabled()) {
            logger.warn("Destroy method on bean with name '" + beanName + "' threw an exception", ex.getTargetException());
        }
    }
}
```

#### 使用示例

```java
@Component
public class MyService {

    @PostConstruct
    public void init() {
        // 初始化逻辑
        System.out.println("MyService initialized");
    }

    @PreDestroy
    public void cleanup() {
        // 清理逻辑
        System.out.println("MyService destroyed");
    }
}
```

---

## 三、元数据类

### 3.1 InjectionMetadata - 注入元数据

**文件路径**: `spring-beans/src/main/java/org/springframework/beans/factory/annotation/InjectionMetadata.java`

#### 概述

`InjectionMetadata` 封装了类的注入元数据，管理一组 `InjectedElement` 对象。这是内部类，不供应用程序直接使用。

#### 核心结构

```java
public class InjectionMetadata {
    // 空的元数据实例（无操作回调）
    public static final InjectionMetadata EMPTY = new InjectionMetadata(Object.class, Collections.emptyList()) {
        @Override
        protected boolean needsRefresh(Class<?> clazz) { return false; }
        @Override
        public void checkConfigMembers(RootBeanDefinition beanDefinition) { }
        @Override
        public void inject(Object target, @Nullable String beanName, @Nullable PropertyValues pvs) { }
        @Override
        public void clear(@Nullable PropertyValues pvs) { }
    };

    private final Class<?> targetClass;
    private final Collection<InjectedElement> injectedElements;
    @Nullable
    private volatile Set<InjectedElement> checkedElements;
}
```

#### 核心方法

```java
// 注册外部管理的配置成员
public void checkConfigMembers(RootBeanDefinition beanDefinition) {
    if (this.injectedElements.isEmpty()) {
        this.checkedElements = Collections.emptySet();
    }
    else {
        Set<InjectedElement> checkedElements = CollectionUtils.newLinkedHashSet(this.injectedElements.size());
        for (InjectedElement element : this.injectedElements) {
            Member member = element.getMember();
            // 检查是否已注册
            if (!beanDefinition.isExternallyManagedConfigMember(member)) {
                beanDefinition.registerExternallyManagedConfigMember(member);
                checkedElements.add(element);
            }
        }
        this.checkedElements = checkedElements;
    }
}

// 执行注入
public void inject(Object target, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
    Collection<InjectedElement> checkedElements = this.checkedElements;
    Collection<InjectedElement> elementsToIterate =
            (checkedElements != null ? checkedElements : this.injectedElements);
    if (!elementsToIterate.isEmpty()) {
        for (InjectedElement element : elementsToIterate) {
            element.inject(target, beanName, pvs);
        }
    }
}

// 检查是否需要刷新
public static boolean needsRefresh(@Nullable InjectionMetadata metadata, Class<?> clazz) {
    return (metadata == null || metadata.needsRefresh(clazz));
}
```

### 3.2 InjectedElement - 注入元素基类

```java
public abstract static class InjectedElement {
    protected final Member member;           // 字段或方法
    protected final boolean isField;         // 是否为字段
    @Nullable
    protected final PropertyDescriptor pd;   // 属性描述符
    @Nullable
    protected volatile Boolean skip;         // 是否跳过

    protected InjectedElement(Member member, @Nullable PropertyDescriptor pd) {
        this.member = member;
        this.isField = (member instanceof Field);
        this.pd = pd;
    }

    // 获取资源类型
    protected final Class<?> getResourceType() {
        if (this.isField) {
            return ((Field) this.member).getType();
        }
        else if (this.pd != null) {
            return this.pd.getPropertyType();
        }
        else {
            return ((Method) this.member).getParameterTypes()[0];
        }
    }

    // 执行注入（模板方法）
    protected void inject(Object target, @Nullable String requestingBeanName, @Nullable PropertyValues pvs)
            throws Throwable {
        if (!shouldInject(pvs)) {
            return;
        }
        if (this.isField) {
            Field field = (Field) this.member;
            ReflectionUtils.makeAccessible(field);
            field.set(target, getResourceToInject(target, requestingBeanName));
        }
        else {
            Method method = (Method) this.member;
            ReflectionUtils.makeAccessible(method);
            method.invoke(target, getResourceToInject(target, requestingBeanName));
        }
    }

    // 子类需要覆盖的方法
    @Nullable
    protected abstract Object getResourceToInject(Object target, @Nullable String requestingBeanName);
}
```

---

### 3.3 AnnotatedBeanDefinition - 带注解的 Bean 定义接口

**文件路径**: `spring-beans/src/main/java/org/springframework/beans/factory/annotation/AnnotatedBeanDefinition.java`

#### 概述

扩展 `BeanDefinition` 接口，暴露 bean 类的 `AnnotationMetadata`，无需预先加载类。

```java
public interface AnnotatedBeanDefinition extends BeanDefinition {
    /**
     * 获取此 bean 定义的 bean 类的注解元数据
     */
    AnnotationMetadata getMetadata();

    /**
     * 获取此 bean 定义的工厂方法的元数据（如果有）
     */
    @Nullable
    MethodMetadata getFactoryMethodMetadata();
}
```

---

### 3.4 AnnotatedGenericBeanDefinition - 带注解的通用 Bean 定义

**文件路径**: `spring-beans/src/main/java/org/springframework/beans/factory/annotation/AnnotatedGenericBeanDefinition.java`

#### 概述

`GenericBeanDefinition` 的扩展，通过 `AnnotatedBeanDefinition` 接口添加对注解元数据的支持。

```java
public class AnnotatedGenericBeanDefinition extends GenericBeanDefinition implements AnnotatedBeanDefinition {
    private final AnnotationMetadata metadata;
    @Nullable
    private MethodMetadata factoryMethodMetadata;

    // 从类创建
    public AnnotatedGenericBeanDefinition(Class<?> beanClass) {
        setBeanClass(beanClass);
        this.metadata = AnnotationMetadata.introspect(beanClass);
    }

    // 从注解元数据创建（支持 ASM 处理，避免提前加载类）
    public AnnotatedGenericBeanDefinition(AnnotationMetadata metadata) {
        Assert.notNull(metadata, "AnnotationMetadata must not be null");
        if (metadata instanceof StandardAnnotationMetadata sam) {
            setBeanClass(sam.getIntrospectedClass());
        }
        else {
            setBeanClassName(metadata.getClassName());
        }
        this.metadata = metadata;
    }

    // 从注解元数据和工厂方法元数据创建
    public AnnotatedGenericBeanDefinition(AnnotationMetadata metadata, MethodMetadata factoryMethodMetadata) {
        this(metadata);
        Assert.notNull(factoryMethodMetadata, "MethodMetadata must not be null");
        setFactoryMethodName(factoryMethodMetadata.getMethodName());
        this.factoryMethodMetadata = factoryMethodMetadata;
    }
}
```

---

## 四、工具类

### 4.1 QualifierAnnotationAutowireCandidateResolver - 限定符注解自动装配候选解析器

**文件路径**: `spring-beans/src/main/java/org/springframework/beans/factory/annotation/QualifierAnnotationAutowireCandidateResolver.java`

#### 概述

匹配 bean 定义限定符与字段或参数上的 `@Qualifier` 注解。同时支持 `@Value` 注解的建议表达式值。

#### 核心功能

```java
public class QualifierAnnotationAutowireCandidateResolver extends GenericTypeAwareAutowireCandidateResolver {
    private final Set<Class<? extends Annotation>> qualifierTypes = CollectionUtils.newLinkedHashSet(2);
    private Class<? extends Annotation> valueAnnotationType = Value.class;

    public QualifierAnnotationAutowireCandidateResolver() {
        // 默认支持 Spring 的 @Qualifier
        this.qualifierTypes.add(Qualifier.class);

        // 尝试添加 JSR-330 的 @Qualifier
        try {
            this.qualifierTypes.add((Class<? extends Annotation>)
                ClassUtils.forName("jakarta.inject.Qualifier", ...));
        }
        catch (ClassNotFoundException ex) {
            // JSR-330 API 不可用
        }
    }
}
```

#### 限定符匹配逻辑

```java
@Override
public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
    // 首先检查父类的类型匹配
    if (!super.isAutowireCandidate(bdHolder, descriptor)) {
        return false;
    }

    // 检查限定符注解
    Boolean checked = checkQualifiers(bdHolder, descriptor.getAnnotations());
    if (checked != Boolean.FALSE) {
        MethodParameter methodParam = descriptor.getMethodParameter();
        if (methodParam != null) {
            Method method = methodParam.getMethod();
            if (method == null || void.class == method.getReturnType()) {
                // 检查方法上的限定符注解
                Boolean methodChecked = checkQualifiers(bdHolder, methodParam.getMethodAnnotations());
                if (methodChecked != null && checked == null) {
                    checked = methodChecked;
                }
            }
        }
    }
    return (checked == Boolean.TRUE ||
            (checked == null && ((RootBeanDefinition) bdHolder.getBeanDefinition()).isDefaultCandidate()));
}

@Nullable
protected Boolean checkQualifiers(BeanDefinitionHolder bdHolder, Annotation[] annotationsToSearch) {
    boolean qualifierFound = false;
    if (!ObjectUtils.isEmpty(annotationsToSearch)) {
        SimpleTypeConverter typeConverter = new SimpleTypeConverter();
        for (Annotation annotation : annotationsToSearch) {
            Class<? extends Annotation> type = annotation.annotationType();
            if (isPlainJavaAnnotation(type)) {
                continue;
            }

            boolean checkMeta = true;
            boolean fallbackToMeta = false;

            // 检查是否是限定符类型
            if (isQualifier(type)) {
                qualifierFound = true;
                if (!checkQualifier(bdHolder, annotation, typeConverter)) {
                    fallbackToMeta = true;
                }
                else {
                    checkMeta = false;
                }
            }

            // 检查元注解
            if (checkMeta) {
                for (Annotation metaAnn : type.getAnnotations()) {
                    Class<? extends Annotation> metaType = metaAnn.annotationType();
                    if (isQualifier(metaType)) {
                        qualifierFound = true;
                        // 检查元限定符
                        if (!checkQualifier(bdHolder, metaAnn, typeConverter)) {
                            return false;
                        }
                    }
                }
            }
        }
    }
    return (qualifierFound ? true : null);
}
```

#### @Value 处理

```java
@Override
@Nullable
public Object getSuggestedValue(DependencyDescriptor descriptor) {
    Object value = findValue(descriptor.getAnnotations());
    if (value == null) {
        MethodParameter methodParam = descriptor.getMethodParameter();
        if (methodParam != null) {
            value = findValue(methodParam.getMethodAnnotations());
        }
    }
    return value;
}

@Nullable
protected Object findValue(Annotation[] annotationsToSearch) {
    if (annotationsToSearch.length > 0) {
        AnnotationAttributes attr = AnnotatedElementUtils.getMergedAnnotationAttributes(
                AnnotatedElementUtils.forAnnotations(annotationsToSearch), this.valueAnnotationType);
        if (attr != null) {
            return extractValue(attr);
        }
    }
    return null;
}
```

---

### 4.2 BeanFactoryAnnotationUtils - BeanFactory 注解工具类

**文件路径**: `spring-beans/src/main/java/org/springframework/beans/factory/annotation/BeanFactoryAnnotationUtils.java`

#### 概述

提供与 Spring 特定注解（如 `@Qualifier`）相关的 Bean 查找便利方法。

#### 核心方法

```java
/**
 * 获取所有声明了匹配限定符的指定类型的 bean
 */
public static <T> Map<String, T> qualifiedBeansOfType(
        ListableBeanFactory beanFactory, Class<T> beanType, String qualifier) {

    String[] candidateBeans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, beanType);
    Map<String, T> result = new LinkedHashMap<>(4);
    for (String beanName : candidateBeans) {
        if (isQualifierMatch(qualifier::equals, beanName, beanFactory)) {
            result.put(beanName, beanFactory.getBean(beanName, beanType));
        }
    }
    return result;
}

/**
 * 获取单个声明了匹配限定符的指定类型的 bean
 */
public static <T> T qualifiedBeanOfType(BeanFactory beanFactory, Class<T> beanType, String qualifier) {
    Assert.notNull(beanFactory, "BeanFactory must not be null");

    if (beanFactory instanceof ListableBeanFactory lbf) {
        return qualifiedBeanOfType(lbf, beanType, qualifier);
    }
    else if (beanFactory.containsBean(qualifier) && beanFactory.isTypeMatch(qualifier, beanType)) {
        // 回退：通过 bean 名称查找
        return beanFactory.getBean(qualifier, beanType);
    }
    else {
        throw new NoSuchBeanDefinitionException(qualifier, ...);
    }
}

/**
 * 检查指定 bean 是否声明了匹配的限定符
 */
public static boolean isQualifierMatch(
        Predicate<String> qualifier, String beanName, @Nullable BeanFactory beanFactory) {

    // 首先尝试 bean 名称或别名匹配
    if (qualifier.test(beanName)) {
        return true;
    }
    if (beanFactory != null) {
        for (String alias : beanFactory.getAliases(beanName)) {
            if (qualifier.test(alias)) {
                return true;
            }
        }

        try {
            // 检查 bean 定义上的限定符元数据（XML 定义）
            if (beanFactory instanceof ConfigurableBeanFactory cbf) {
                BeanDefinition bd = cbf.getMergedBeanDefinition(beanName);
                if (bd instanceof AbstractBeanDefinition abd) {
                    AutowireCandidateQualifier candidate = abd.getQualifier(Qualifier.class.getName());
                    if (candidate != null) {
                        Object value = candidate.getAttribute(AutowireCandidateQualifier.VALUE_KEY);
                        if (value != null && qualifier.test(value.toString())) {
                            return true;
                        }
                    }
                }

                // 检查工厂方法上的限定符（配置类）
                if (bd instanceof RootBeanDefinition rbd) {
                    Method factoryMethod = rbd.getResolvedFactoryMethod();
                    if (factoryMethod != null) {
                        Qualifier targetAnnotation = AnnotationUtils.getAnnotation(factoryMethod, Qualifier.class);
                        if (targetAnnotation != null) {
                            return qualifier.test(targetAnnotation.value());
                        }
                    }
                }
            }

            // 检查 bean 实现类上的限定符
            Class<?> beanType = beanFactory.getType(beanName);
            if (beanType != null) {
                Qualifier targetAnnotation = AnnotationUtils.getAnnotation(beanType, Qualifier.class);
                if (targetAnnotation != null) {
                    return qualifier.test(targetAnnotation.value());
                }
            }
        }
        catch (NoSuchBeanDefinitionException ex) {
            // 忽略
        }
    }
    return false;
}
```

---

### 4.3 CustomAutowireConfigurer - 自定义自动装配配置器

**文件路径**: `spring-beans/src/main/java/org/springframework/beans/factory/annotation/CustomAutowireConfigurer.java`

#### 概述

`BeanFactoryPostProcessor` 实现，允许方便地注册自定义自动装配限定符类型。

#### 使用示例

```java
// XML 配置
<bean id="customAutowireConfigurer" class="org.springframework.beans.factory.annotation.CustomAutowireConfigurer">
    <property name="customQualifierTypes">
        <set>
            <value>com.example.MyQualifier</value>
        </set>
    </property>
</bean>

// Java 配置
@Configuration
public class AppConfig {
    @Bean
    public CustomAutowireConfigurer customAutowireConfigurer() {
        CustomAutowireConfigurer configurer = new CustomAutowireConfigurer();
        Set<Class<?>> qualifierTypes = new HashSet<>();
        qualifierTypes.add(MyQualifier.class);
        configurer.setCustomQualifierTypes(qualifierTypes);
        return configurer;
    }
}
```

#### 实现逻辑

```java
@Override
@SuppressWarnings("unchecked")
public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    if (this.customQualifierTypes != null) {
        if (!(beanFactory instanceof DefaultListableBeanFactory dlbf)) {
            throw new IllegalStateException(
                "CustomAutowireConfigurer needs to operate on a DefaultListableBeanFactory");
        }

        // 确保使用 QualifierAnnotationAutowireCandidateResolver
        if (!(dlbf.getAutowireCandidateResolver() instanceof QualifierAnnotationAutowireCandidateResolver)) {
            dlbf.setAutowireCandidateResolver(new QualifierAnnotationAutowireCandidateResolver());
        }

        QualifierAnnotationAutowireCandidateResolver resolver =
                (QualifierAnnotationAutowireCandidateResolver) dlbf.getAutowireCandidateResolver();

        // 注册自定义限定符类型
        for (Object value : this.customQualifierTypes) {
            Class<? extends Annotation> customType = null;
            if (value instanceof Class) {
                customType = (Class<? extends Annotation>) value;
            }
            else if (value instanceof String className) {
                customType = (Class<? extends Annotation>)
                    ClassUtils.resolveClassName(className, this.beanClassLoader);
            }
            resolver.addQualifierType(customType);
        }
    }
}
```

---

### 4.4 ParameterResolutionDelegate - 参数解析委托

**文件路径**: `spring-beans/src/main/java/org/springframework/beans/factory/annotation/ParameterResolutionDelegate.java`

#### 概述

用于解析外部管理的构造函数和方法上的可自动装配参数的公共委托。

#### 核心方法

```java
/**
 * 确定参数是否可以自动装配
 */
public static boolean isAutowirable(Parameter parameter, int parameterIndex) {
    Assert.notNull(parameter, "Parameter must not be null");
    AnnotatedElement annotatedParameter = getEffectiveAnnotatedParameter(parameter, parameterIndex);
    return (AnnotatedElementUtils.hasAnnotation(annotatedParameter, Autowired.class) ||
            AnnotatedElementUtils.hasAnnotation(annotatedParameter, Qualifier.class) ||
            AnnotatedElementUtils.hasAnnotation(annotatedParameter, Value.class));
}

/**
 * 解析参数的依赖
 */
@Nullable
public static Object resolveDependency(
        Parameter parameter, int parameterIndex, Class<?> containingClass,
        AutowireCapableBeanFactory beanFactory) throws BeansException {

    Assert.notNull(parameter, "Parameter must not be null");
    Assert.notNull(containingClass, "Containing class must not be null");
    Assert.notNull(beanFactory, "AutowireCapableBeanFactory must not be null");

    AnnotatedElement annotatedParameter = getEffectiveAnnotatedParameter(parameter, parameterIndex);
    Autowired autowired = AnnotatedElementUtils.findMergedAnnotation(annotatedParameter, Autowired.class);
    boolean required = (autowired == null || autowired.required());

    MethodParameter methodParameter = SynthesizingMethodParameter.forExecutable(
            parameter.getDeclaringExecutable(), parameterIndex);
    DependencyDescriptor descriptor = new DependencyDescriptor(methodParameter, required);
    descriptor.setContainingClass(containingClass);
    return beanFactory.resolveDependency(descriptor, null);
}
```

#### JDK < 9 的兼容性处理

```java
/**
 * 处理 JDK < 9 中 javac 的 bug：内部类构造函数的参数注解数组
 * 排除了隐式的封闭实例参数
 */
private static AnnotatedElement getEffectiveAnnotatedParameter(Parameter parameter, int index) {
    Executable executable = parameter.getDeclaringExecutable();
    if (executable instanceof Constructor && ClassUtils.isInnerClass(executable.getDeclaringClass()) &&
            executable.getParameterAnnotations().length == executable.getParameterCount() - 1) {
        // JDK < 9 的 bug：注解数组排除了封闭实例参数
        return (index == 0 ? EMPTY_ANNOTATED_ELEMENT : executable.getParameters()[index - 1]);
    }
    return parameter;
}
```

---

### 4.5 JakartaAnnotationsRuntimeHints - Jakarta 注解运行时提示

**文件路径**: `spring-beans/src/main/java/org/springframework/beans/factory/annotation/JakartaAnnotationsRuntimeHints.java`

#### 概述

为 Jakarta 注解及其 pre-Jakarta 等效项注册运行时提示，用于 GraalVM 原生镜像支持。

```java
class JakartaAnnotationsRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        Stream.of(
            "jakarta.inject.Inject",
            "jakarta.inject.Provider",
            "jakarta.inject.Qualifier",
            "javax.inject.Inject",
            "javax.inject.Qualifier"
        ).forEach(typeName -> hints.reflection().registerType(TypeReference.of(typeName)));
    }
}
```

---

### 4.6 AnnotationBeanWiringInfoResolver - 注解 Bean 装配信息解析器

**文件路径**: `spring-beans/src/main/java/org/springframework/beans/factory/annotation/AnnotationBeanWiringInfoResolver.java`

#### 概述

使用 `@Configurable` 注解识别哪些类需要自动装配的 `BeanWiringInfoResolver` 实现。

```java
public class AnnotationBeanWiringInfoResolver implements BeanWiringInfoResolver {
    @Override
    @Nullable
    public BeanWiringInfo resolveWiringInfo(Object beanInstance) {
        Assert.notNull(beanInstance, "Bean instance must not be null");
        Configurable annotation = beanInstance.getClass().getAnnotation(Configurable.class);
        return (annotation != null ? buildWiringInfo(beanInstance, annotation) : null);
    }

    protected BeanWiringInfo buildWiringInfo(Object beanInstance, Configurable annotation) {
        if (!Autowire.NO.equals(annotation.autowire())) {
            // 按名称或类型自动装配
            return new BeanWiringInfo(annotation.autowire().value(), annotation.dependencyCheck());
        }
        else if (!annotation.value().isEmpty()) {
            // 显式指定的 bean 名称
            return new BeanWiringInfo(annotation.value(), false);
        }
        else {
            // 默认 bean 名称（类的全限定名）
            return new BeanWiringInfo(getDefaultBeanName(beanInstance), true);
        }
    }

    protected String getDefaultBeanName(Object beanInstance) {
        return ClassUtils.getUserClass(beanInstance).getName();
    }
}
```

---

## 五、依赖注入流程总结

### 5.1 整体流程

```
1. Bean 定义加载
   ↓
2. MergedBeanDefinitionPostProcessor.postProcessMergedBeanDefinition()
   - AutowiredAnnotationBeanPostProcessor 查找注入元数据并缓存
   - InitDestroyAnnotationBeanPostProcessor 查找生命周期元数据
   ↓
3. InstantiationAwareBeanPostProcessor.determineCandidateConstructors()
   - 确定候选构造函数（检查 @Autowired 构造函数）
   ↓
4. 实例化 Bean（使用选定的构造函数）
   ↓
5. InstantiationAwareBeanPostProcessor.postProcessProperties()
   - AutowiredAnnotationBeanPostProcessor 执行字段和方法注入
   ↓
6. BeanPostProcessor.postProcessBeforeInitialization()
   - InitDestroyAnnotationBeanPostProcessor 调用 @PostConstruct 方法
   ↓
7. 初始化完成
   ↓
8. Bean 销毁
   ↓
9. DestructionAwareBeanPostProcessor.postProcessBeforeDestruction()
   - InitDestroyAnnotationBeanPostProcessor 调用 @PreDestroy 方法
```

### 5.2 @Autowired 处理流程

```
1. 构建注入元数据 (buildAutowiringMetadata)
   - 遍历类的所有字段和方法
   - 查找 @Autowired、@Value、@Inject 注解
   - 创建 AutowiredFieldElement 或 AutowiredMethodElement
   ↓
2. 执行注入 (InjectionMetadata.inject)
   - 遍历所有 InjectedElement
   - 调用 element.inject()
   ↓
3. 字段注入 (AutowiredFieldElement.inject)
   - 创建 DependencyDescriptor
   - 调用 beanFactory.resolveDependency() 解析依赖
   - 使用反射设置字段值
   ↓
4. 方法注入 (AutowiredMethodElement.inject)
   - 解析每个方法参数
   - 创建每个参数的 DependencyDescriptor
   - 解析所有参数值
   - 使用反射调用方法
```

### 5.3 @Lookup 处理流程

```
1. 检查 @Lookup 方法 (checkLookupMethods)
   - 在类及其父类中查找 @Lookup 注解的方法
   ↓
2. 创建 LookupOverride
   - 将方法信息和可选的 bean 名称封装到 LookupOverride
   ↓
3. 添加到 Bean 定义
   - 将 LookupOverride 添加到 RootBeanDefinition 的 methodOverrides
   ↓
4. 实例化时处理
   - AbstractAutowireCapableBeanFactory 使用 CGLIB 生成子类
   - 重写 lookup 方法，重定向到 BeanFactory.getBean()
```

---

## 六、关键设计模式

### 6.1 模板方法模式

`InjectionMetadata.InjectedElement` 使用模板方法模式定义注入流程：

```java
public abstract static class InjectedElement {
    // 模板方法
    protected void inject(Object target, @Nullable String requestingBeanName,
                         @Nullable PropertyValues pvs) throws Throwable {
        if (this.isField) {
            // 字段注入逻辑
            field.set(target, getResourceToInject(target, requestingBeanName));
        }
        else {
            // 方法注入逻辑
            method.invoke(target, getResourceToInject(target, requestingBeanName));
        }
    }

    // 子类实现
    protected abstract Object getResourceToInject(Object target, @Nullable String requestingBeanName);
}
```

### 6.2 策略模式

`QualifierAnnotationAutowireCandidateResolver` 使用策略模式支持不同的限定符注解：

```java
protected boolean isQualifier(Class<? extends Annotation> annotationType) {
    for (Class<? extends Annotation> qualifierType : this.qualifierTypes) {
        if (annotationType.equals(qualifierType) ||
            annotationType.isAnnotationPresent(qualifierType)) {
            return true;
        }
    }
    return false;
}
```

### 6.3 缓存模式

大量使用缓存提高性能：

```java
// 构造函数缓存
private final Map<Class<?>, Constructor<?>[]> candidateConstructorsCache = new ConcurrentHashMap<>(256);

// 注入元数据缓存
private final Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);

// 生命周期元数据缓存
private final Map<Class<?>, LifecycleMetadata> lifecycleMetadataCache = new ConcurrentHashMap<>(256);
```

---

## 七、注意事项

1. **不支持静态字段/方法**: `@Autowired` 不能用于静态字段或静态方法
2. **BeanPostProcessor 限制**: 不能在 `BeanPostProcessor` 或 `BeanFactoryPostProcessor` 中使用 `@Autowired`
3. **Lookup 方法限制**: `@Lookup` 方法不能用于从 `@Bean` 方法返回的 Bean
4. **线程安全**: 所有缓存使用 `ConcurrentHashMap`，确保线程安全
5. **类加载**: `AnnotatedGenericBeanDefinition` 支持 ASM 处理，避免提前加载类
