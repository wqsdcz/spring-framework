# org.springframework.beans.factory.support 包详解

## 目录

1. [概述](#概述)
2. [核心 BeanFactory 实现](#核心-beanfactory-实现)
   - [DefaultListableBeanFactory](#defaultlistablebeanfactory)
   - [AbstractBeanFactory](#abstractbeanfactory)
   - [AbstractAutowireCapableBeanFactory](#abstractautowirecapablebeanfactory)
3. [BeanDefinition 实现](#beandefinition-实现)
   - [AbstractBeanDefinition](#abstractbeandefinition)
   - [RootBeanDefinition](#rootbeandefinition)
   - [ChildBeanDefinition](#childbeandefinition)
   - [GenericBeanDefinition](#genericbeandefinition)
4. [单例注册表与三级缓存](#单例注册表与三级缓存)
   - [DefaultSingletonBeanRegistry](#defaultsingletonbeanregistry)
   - [循环依赖解决方案](#循环依赖解决方案)
5. [实例化策略](#实例化策略)
   - [InstantiationStrategy](#instantiationstrategy)
   - [SimpleInstantiationStrategy](#simpleinstantiationstrategy)
   - [CglibSubclassingInstantiationStrategy](#cglibsubclassinginstantiationstrategy)
6. [构造器解析](#构造器解析)
   - [ConstructorResolver](#constructorresolver)
7. [方法注入](#方法注入)
   - [MethodOverride 体系](#methodoverride-体系)
8. [BeanDefinition 合并](#beandefinition-合并)
9. [其他重要组件](#其他重要组件)

---

## 概述

`org.springframework.beans.factory.support` 包是 Spring Beans 模块的核心实现包，提供了 BeanFactory 的完整实现机制。该包包含了从 Bean 定义注册、Bean 实例化、依赖注入到 Bean 销毁的全生命周期管理功能。

### 核心架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DefaultListableBeanFactory                          │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    BeanDefinitionRegistry                          │   │
│  │  - registerBeanDefinition()  - getBeanDefinition()                 │   │
│  │  - containsBeanDefinition()  - getBeanDefinitionNames()            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    ListableBeanFactory                             │   │
│  │  - getBeansOfType()  - getBeanNamesForType()                       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      AbstractAutowireCapableBeanFactory                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    AbstractBeanFactory                             │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │              FactoryBeanRegistrySupport                      │   │   │
│  │  │  ┌─────────────────────────────────────────────────────┐   │   │   │
│  │  │  │          DefaultSingletonBeanRegistry              │   │   │   │
│  │  │  │  - singletonObjects (一级缓存)                      │   │   │   │
│  │  │  │  - earlySingletonObjects (二级缓存)                 │   │   │   │
│  │  │  │  - singletonFactories (三级缓存)                    │   │   │   │
│  │  │  └─────────────────────────────────────────────────────┘   │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 核心 BeanFactory 实现

### DefaultListableBeanFactory

`DefaultListableBeanFactory` 是 Spring 中最核心的 BeanFactory 实现，它实现了 `ConfigurableListableBeanFactory` 和 `BeanDefinitionRegistry` 接口，提供了完整的 Bean 定义注册和 Bean 实例管理功能。

#### 核心属性

```java
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
        implements ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {

    /** 静态缓存，用于存储从依赖类型到自动装配值的映射 */
    private static final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<>(16);

    /** BeanDefinition 的映射表：bean 名称 -> BeanDefinition */
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

    /** 按注册顺序存储的 bean 定义名称列表 */
    private volatile List<String> beanDefinitionNames = new ArrayList<>(256);

    /** 按注册顺序存储的手动注册的单例 bean 名称列表 */
    private volatile Set<String> manualSingletonNames = new LinkedHashSet<>(16);

    /** 依赖比较器（用于按依赖顺序销毁 bean） */
    @Nullable
    private Comparator<Object> dependencyComparator;

    /** 自动装配候选解析器 */
    private AutowireCandidateResolver autowireCandidateResolver = new SimpleAutowireCandidateResolver();

    /** 是否允许同名 bean 定义覆盖 */
    private boolean allowBeanDefinitionOverriding = true;

    /** 是否允许循环引用 */
    private boolean allowCircularReferences = true;

    /** 是否允许在宽松创建模式下进行循环引用（6.2 新增） */
    private boolean allowCircularReferencesInLenientCreationMode = true;
}
```

#### Bean 注册流程

```java
@Override
public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
        throws BeanDefinitionStoreException {

    Assert.hasText(beanName, "'beanName' must not be empty");
    Assert.notNull(beanDefinition, "BeanDefinition must not be null");

    if (beanDefinition instanceof AbstractBeanDefinition) {
        try {
            // 验证 BeanDefinition
            ((AbstractBeanDefinition) beanDefinition).validate();
        }
        catch (BeanDefinitionValidationException ex) {
            throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
                    "Validation of bean definition failed", ex);
        }
    }

    BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
    if (existingDefinition != null) {
        // 处理 bean 定义覆盖
        if (!isAllowBeanDefinitionOverriding()) {
            throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);
        }
        // ... 日志记录和覆盖逻辑
        this.beanDefinitionMap.put(beanName, beanDefinition);
    }
    else {
        // 检查是否已经开始创建 bean
        if (hasBeanCreationStarted()) {
            // 需要同步，防止并发修改
            synchronized (this.beanDefinitionMap) {
                this.beanDefinitionMap.put(beanName, beanDefinition);
                List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
                updatedDefinitions.addAll(this.beanDefinitionNames);
                updatedDefinitions.add(beanName);
                this.beanDefinitionNames = updatedDefinitions;
            }
        }
        else {
            // 仍在启动注册阶段
            this.beanDefinitionMap.put(beanName, beanDefinition);
            this.beanDefinitionNames.add(beanName);
        }
    }
}
```

#### Bean 获取流程

```java
@Override
public <T> T getBean(Class<T> requiredType) throws BeansException {
    return resolveBean(ResolvableType.forClass(requiredType), null, false);
}

@Nullable
private <T> T resolveBean(ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull)
        throws BeansException {

    // 尝试按类型和名称解析 bean
    NamedBeanHolder<T> namedBean = resolveNamedBean(requiredType, args, nonUniqueAsNull);
    if (namedBean != null) {
        return namedBean.getBeanInstance();
    }

    // 如果当前工厂找不到，尝试从父工厂获取
    BeanFactory parent = getParentBeanFactory();
    if (parent instanceof DefaultListableBeanFactory) {
        return ((DefaultListableBeanFactory) parent).resolveBean(requiredType, args, nonUniqueAsNull);
    }
    else if (parent != null) {
        // 父工厂不是 DefaultListableBeanFactory，使用标准方法
        if (args == null) {
            return parent.getBean(requiredType);
        }
        else {
            return parent.getBean(requiredType, args);
        }
    }
    return null;
}
```

#### 按类型解析 Bean

```java
@Nullable
private <T> NamedBeanHolder<T> resolveNamedBean(
        ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) throws BeansException {

    Assert.notNull(requiredType, "Required type must not be null");

    // 获取匹配该类型的所有 bean 名称
    String[] candidateNames = getBeanNamesForType(requiredType);

    if (candidateNames.length > 1) {
        // 多个候选 bean，需要确定首选 bean
        List<String> autowireCandidates = new ArrayList<>(candidateNames.length);
        for (String beanName : candidateNames) {
            if (!containsBeanDefinition(beanName) || getBeanDefinition(beanName).isAutowireCandidate()) {
                autowireCandidates.add(beanName);
            }
        }

        if (autowireCandidates.size() > 1) {
            // 使用依赖比较器确定首选 bean
            if (this.dependencyComparator != null) {
                // ... 比较逻辑
            }
            // 如果有 @Primary 注解的 bean，选择它
            // ...
        }
    }

    // 只有一个候选，直接返回
    if (candidateNames.length == 1) {
        String beanName = candidateNames[0];
        return new NamedBeanHolder<>(beanName, getBean(beanName, requiredType.toClass(), args));
    }

    return null;
}
```

---

### AbstractBeanFactory

`AbstractBeanFactory` 是 `BeanFactory` 的抽象基类，实现了通用的 Bean 获取逻辑，同时保留了 `createBean` 方法供子类实现。

#### 核心功能

```java
public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport
        implements ConfigurableBeanFactory {

    /** 父 BeanFactory */
    @Nullable
    private BeanFactory parentBeanFactory;

    /** 类加载器 */
    @Nullable
    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

    /** 临时类加载器（用于类型匹配） */
    @Nullable
    private ClassLoader tempClassLoader;

    /** 是否缓存 bean 元数据 */
    private boolean cacheBeanMetadata = true;

    /** bean 表达式解析器 */
    @Nullable
    private BeanExpressionResolver beanExpressionResolver;

    /** Spring 转换服务 */
    @Nullable
    private ConversionService conversionService;

    /** 自定义属性编辑器注册器 */
    private final Set<PropertyEditorRegistrar> propertyEditorRegistrars = new LinkedHashSet<>(4);

    /** 自定义属性编辑器 */
    private final Map<Class<?>, Class<? extends PropertyEditor>> customEditors = new HashMap<>(4);

    /** 类型转换器 */
    @Nullable
    private TypeConverter typeConverter;

    /** 字符串值解析器（用于解析占位符） */
    private final List<StringValueResolver> embeddedValueResolvers = new CopyOnWriteArrayList<>();

    /** BeanPostProcessor 列表 */
    private final List<BeanPostProcessor> beanPostProcessors = new BeanPostProcessorCacheAwareList();

    /** 作用域注册表 */
    private final Map<String, Scope> scopes = new LinkedHashMap<>(8);

    /** 合并的 BeanDefinition 缓存 */
    private final Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256);
}
```

#### doGetBean 方法详解

`doGetBean` 是获取 Bean 的核心方法，处理了整个 Bean 获取流程：

```java
protected <T> T doGetBean(
        String name, @Nullable Class<T> requiredType, @Nullable Object[] args, boolean typeCheckOnly)
        throws BeansException {

    // 1. 转换 bean 名称（处理别名和 FactoryBean 前缀 &）
    String beanName = transformedBeanName(name);
    Object beanInstance;

    // 2. 尝试从缓存中获取单例 bean（处理循环依赖的关键）
    Object sharedInstance = getSingleton(beanName);
    if (sharedInstance != null && args == null) {
        // 处理 FactoryBean 的情况
        beanInstance = getObjectForBeanInstance(sharedInstance, name, beanName, null);
    }
    else {
        // 3. 检查父工厂
        BeanFactory parentBeanFactory = getParentBeanFactory();
        if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
            // 委托给父工厂获取
            return parentBeanFactory.getBean(name, requiredType);
        }

        if (!typeCheckOnly) {
            // 标记 bean 已创建
            markBeanAsCreated(beanName);
        }

        // 4. 获取合并的 BeanDefinition
        RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

        // 5. 检查 depends-on 依赖
        String[] dependsOn = mbd.getDependsOn();
        if (dependsOn != null) {
            for (String dep : dependsOn) {
                // 注册依赖关系
                registerDependentBean(dep, beanName);
                // 先创建依赖的 bean
                getBean(dep);
            }
        }

        // 6. 创建 bean 实例
        if (mbd.isSingleton()) {
            // 单例模式：通过 ObjectFactory 懒加载创建
            sharedInstance = getSingleton(beanName, () -> {
                try {
                    return createBean(beanName, mbd, args);
                }
                catch (BeansException ex) {
                    destroySingleton(beanName);
                    throw ex;
                }
            });
            beanInstance = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
        }
        else if (mbd.isPrototype()) {
            // 原型模式：每次创建新实例
            Object prototypeInstance = createBean(beanName, mbd, args);
            beanInstance = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
        }
        else {
            // 其他作用域
            String scopeName = mbd.getScope();
            Scope scope = this.scopes.get(scopeName);
            Object scopedInstance = scope.get(beanName, () -> createBean(beanName, mbd, args));
            beanInstance = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
        }
    }

    // 7. 类型转换
    return adaptBeanInstance(name, beanInstance, requiredType);
}
```

---

### AbstractAutowireCapableBeanFactory

`AbstractAutowireCapableBeanFactory` 提供了 Bean 的创建、自动装配、属性填充和初始化等核心功能。

#### Bean 创建流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Bean 创建完整流程                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. createBean(String beanName, RootBeanDefinition mbd, Object[] args)     │
│     │                                                                       │
│     ▼                                                                       │
│  2. resolveBeforeInstantiation()  ← 实例化前 BeanPostProcessor 干预        │
│     │  - InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation│
│     │                                                                       │
│     ▼                                                                       │
│  3. doCreateBean()  ← 实际创建 Bean                                         │
│     │                                                                       │
│     ├── 3.1 createBeanInstance()  ← 实例化                                  │
│     │   │  - 使用构造器/工厂方法创建实例                                     │
│     │   │  - 如果存在方法重写，使用 CGLIB 生成子类                          │
│     │   │                                                                   │
│     │   └── 3.1.1 addSingletonFactory()  ← 提前暴露 ObjectFactory          │
│     │       （解决循环依赖的关键步骤）                                       │
│     │                                                                       │
│     ├── 3.2 populateBean()  ← 属性填充                                      │
│     │   │  - InstantiationAwareBeanPostProcessor.postProcessProperties     │
│     │   │  - 应用属性值（包括自动装配）                                      │
│     │   │                                                                   │
│     │   └── 3.2.1 applyPropertyValues()  ← 应用属性值                       │
│     │       - 解析属性值中的 bean 引用                                       │
│     │       - 类型转换                                                      │
│     │                                                                       │
│     └── 3.3 initializeBean()  ← 初始化                                      │
│         │                                                                   │
│         ├── 3.3.1 invokeAwareMethods()  ← 调用 Aware 接口方法               │
│         │   - BeanNameAware.setBeanName()                                  │
│         │   - BeanClassLoaderAware.setBeanClassLoader()                    │
│         │   - BeanFactoryAware.setBeanFactory()                            │
│         │                                                                   │
│         ├── 3.3.2 applyBeanPostProcessorsBeforeInitialization()            │
│         │   ← 初始化前 BeanPostProcessor 处理                               │
│         │                                                                   │
│         ├── 3.3.3 invokeInitMethods()  ← 调用初始化方法                     │
│         │   - InitializingBean.afterPropertiesSet()                        │
│         │   - 自定义 init-method                                            │
│         │                                                                   │
│         └── 3.3.4 applyBeanPostProcessorsAfterInitialization()             │
│             ← 初始化后 BeanPostProcessor 处理（AOP 代理在此创建）           │
│                                                                             │
│  4. registerDisposableBeanIfNecessary()  ← 注册销毁回调                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### createBean 方法

```java
@Override
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
        throws BeanCreationException {

    RootBeanDefinition mbdToUse = mbd;

    // 1. 解析 bean 类
    Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
    if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
        mbdToUse = new RootBeanDefinition(mbd);
        mbdToUse.setBeanClass(resolvedClass);
    }

    // 2. 准备方法重写（lookup-method 和 replaced-method）
    try {
        mbdToUse.prepareMethodOverrides();
    }
    catch (BeanDefinitionValidationException ex) {
        throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
                beanName, "Validation of method overrides failed", ex);
    }

    // 3. 实例化前的后置处理（允许 BeanPostProcessor 返回代理）
    try {
        Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
        if (bean != null) {
            return bean;  // 如果返回了代理，直接返回
        }
    }
    catch (Throwable ex) {
        throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
                "BeanPostProcessor before instantiation of bean failed", ex);
    }

    // 4. 创建 bean 实例
    try {
        Object beanInstance = doCreateBean(beanName, mbdToUse, args);
        return beanInstance;
    }
    catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
        throw ex;
    }
    catch (Throwable ex) {
        throw new BeanCreationException(
                mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
    }
}
```

#### doCreateBean 方法

```java
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
        throws BeanCreationException {

    BeanWrapper instanceWrapper = null;

    // 1. 如果是单例，先尝试从工厂 bean 缓存中移除
    if (mbd.isSingleton()) {
        instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
    }

    // 2. 创建 bean 实例
    if (instanceWrapper == null) {
        instanceWrapper = createBeanInstance(beanName, mbd, args);
    }
    Object bean = instanceWrapper.getWrappedInstance();
    Class<?> beanType = instanceWrapper.getWrappedClass();

    // 3. 调用 MergedBeanDefinitionPostProcessor
    synchronized (mbd.postProcessingLock) {
        if (!mbd.postProcessed) {
            applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
            mbd.markAsPostProcessed();
        }
    }

    // 4. 【关键】提前暴露单例引用（解决循环依赖）
    boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
            isSingletonCurrentlyInCreation(beanName));
    if (earlySingletonExposure) {
        addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
    }

    // 5. 填充属性
    try {
        populateBean(beanName, mbd, instanceWrapper);
    }
    catch (Throwable ex) {
        throw new BeanCreationException(
                mbd.getResourceDescription(), beanName, "Property population failed", ex);
    }

    // 6. 初始化 bean
    Object exposedObject = bean;
    try {
        exposedObject = initializeBean(beanName, exposedObject, mbd);
    }
    catch (Throwable ex) {
        throw new BeanCreationException(
                mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
    }

    // 7. 处理循环依赖情况下的早期引用
    if (earlySingletonExposure) {
        Object earlySingletonReference = getSingleton(beanName, false);
        if (earlySingletonReference != null) {
            if (exposedObject == bean) {
                exposedObject = earlySingletonReference;
            }
            else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
                // 检查依赖该 bean 的其他 bean
                String[] dependentBeans = getDependentBeans(beanName);
                Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
                for (String dependentBean : dependentBeans) {
                    if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                        actualDependentBeans.add(dependentBean);
                    }
                }
                if (!actualDependentBeans.isEmpty()) {
                    throw new BeanCurrentlyInCreationException(beanName,
                            "Bean with name '" + beanName + "' has been injected into other beans " +
                            actualDependentBeans + " in its raw version as part of a circular reference");
                }
            }
        }
    }

    // 8. 注册销毁回调
    try {
        registerDisposableBeanIfNecessary(beanName, bean, mbd);
    }
    catch (BeanDefinitionValidationException ex) {
        throw new BeanCreationException(
                mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
    }

    return exposedObject;
}
```

#### 初始化流程

```java
protected Object initializeBean(String beanName, Object bean, @Nullable RootBeanDefinition mbd) {

    // 1. 调用 Aware 接口方法
    invokeAwareMethods(beanName, bean);

    Object wrappedBean = bean;
    if (mbd == null || !mbd.isSynthetic()) {
        // 2. 应用 BeanPostProcessor 的前置处理
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
    }

    try {
        // 3. 调用初始化方法
        invokeInitMethods(beanName, wrappedBean, mbd);
    }
    catch (Throwable ex) {
        throw new BeanCreationException(
                (mbd != null ? mbd.getResourceDescription() : null), beanName, ex.getMessage(), ex);
    }

    if (mbd == null || !mbd.isSynthetic()) {
        // 4. 应用 BeanPostProcessor 的后置处理（AOP 代理在此创建）
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    }

    return wrappedBean;
}
```

---

## BeanDefinition 实现

### AbstractBeanDefinition

`AbstractBeanDefinition` 是所有 BeanDefinition 实现的抽象基类，定义了 Bean 定义的所有通用属性和方法。

#### 核心属性

```java
public abstract class AbstractBeanDefinition extends BeanMetadataAttributeAccessor
        implements BeanDefinition, Cloneable {

    // ==================== 作用域相关 ====================
    public static final String SCOPE_DEFAULT = "";
    public static final int AUTOWIRE_NO = AutowireCapableBeanFactory.AUTOWIRE_NO;
    public static final int AUTOWIRE_BY_NAME = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;
    public static final int AUTOWIRE_BY_TYPE = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;
    public static final int AUTOWIRE_CONSTRUCTOR = AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;

    // ==================== 依赖检查常量 ====================
    public static final int DEPENDENCY_CHECK_NONE = 0;
    public static final int DEPENDENCY_CHECK_OBJECTS = 1;
    public static final int DEPENDENCY_CHECK_SIMPLE = 2;
    public static final int DEPENDENCY_CHECK_ALL = 3;

    // ==================== 推断方法常量 ====================
    public static final String INFER_METHOD = "(inferred)";

    // ==================== 核心字段 ====================
    @Nullable
    private volatile Object beanClass;  // bean 类（Class 或 String）

    @Nullable
    private String scope = SCOPE_DEFAULT;  // 作用域

    private boolean abstractFlag = false;  // 是否抽象

    @Nullable
    private Boolean lazyInit;  // 是否延迟初始化

    private int autowireMode = AUTOWIRE_NO;  // 自动装配模式

    private int dependencyCheck = DEPENDENCY_CHECK_NONE;  // 依赖检查模式

    @Nullable
    private String[] dependsOn;  // 依赖的 bean 名称

    private boolean autowireCandidate = true;  // 是否可作为自动装配候选

    private boolean primary = false;  // 是否首选

    private final Map<String, AutowireCandidateQualifier> qualifiers = new LinkedHashMap<>();

    @Nullable
    private Supplier<?> instanceSupplier;  // 实例供应商

    @Nullable
    private String factoryBeanName;  // 工厂 bean 名称

    @Nullable
    private String factoryMethodName;  // 工厂方法名称

    @Nullable
    private ConstructorArgumentValues constructorArgumentValues;  // 构造器参数

    @Nullable
    private MutablePropertyValues propertyValues;  // 属性值

    @Nullable
    private MethodOverrides methodOverrides;  // 方法重写

    @Nullable
    private String[] initMethodNames;  // 初始化方法名称

    @Nullable
    private String[] destroyMethodNames;  // 销毁方法名称

    private boolean enforceInitMethod = true;  // 是否强制存在初始化方法

    private boolean enforceDestroyMethod = true;  // 是否强制存在销毁方法

    private boolean synthetic = false;  // 是否合成 bean

    private int role = BeanDefinition.ROLE_APPLICATION;  // 角色

    @Nullable
    private String description;  // 描述

    @Nullable
    private Resource resource;  // 资源
}
```

#### 验证方法

```java
public void validate() throws BeanDefinitionValidationException {
    // 验证方法重写
    if (hasMethodOverrides() && getFactoryMethodName() != null) {
        throw new BeanDefinitionValidationException(
                "Cannot combine static factory method with method overrides: " +
                "the static factory method must create the instance");
    }

    // 验证构造器参数
    if (hasConstructorArgumentValues()) {
        validateConstructorArgumentValues();
    }
}
```

---

### RootBeanDefinition

`RootBeanDefinition` 代表运行时合并后的 Bean 定义，是 Spring 内部实际使用的 BeanDefinition 类型。

#### 核心特性

```java
public class RootBeanDefinition extends AbstractBeanDefinition {

    /** 装饰的定义（用于装饰器模式） */
    @Nullable
    private BeanDefinitionHolder decoratedDefinition;

    /** 限定元素（用于注解驱动的限定符） */
    @Nullable
    private AnnotatedElement qualifiedElement;

    /** 标记定义是否需要重新合并 */
    volatile boolean stale;

    boolean allowCaching = true;

    boolean isFactoryMethodUnique;

    @Nullable
    volatile ResolvableType targetType;

    /** 缓存的目标类型 */
    @Nullable
    volatile Class<?> resolvedTargetType;

    /** 缓存的是否为 FactoryBean */
    @Nullable
    volatile Boolean isFactoryBean;

    /** 缓存的工厂方法返回类型 */
    @Nullable
    volatile ResolvableType factoryMethodReturnType;

    /** 缓存的工厂方法 */
    @Nullable
    volatile Method factoryMethodToIntrospect;

    /** 构造器参数锁 */
    final Object constructorArgumentLock = new Object();

    /** 解析后的构造器或工厂方法 */
    @Nullable
    Executable resolvedConstructorOrFactoryMethod;

    /** 构造器参数是否已解析 */
    boolean constructorArgumentsResolved = false;

    /** 完全解析的构造器参数 */
    @Nullable
    Object[] resolvedConstructorArguments;

    /** 部分准备的构造器参数 */
    @Nullable
    Object[] preparedConstructorArguments;

    /** 后置处理锁 */
    final Object postProcessingLock = new Object();

    /** 是否已应用 MergedBeanDefinitionPostProcessor */
    boolean postProcessed = false;

    /** 实例化前是否已解析 */
    @Nullable
    volatile Boolean beforeInstantiationResolved;

    /** 外部管理的配置成员 */
    @Nullable
    private Set<Member> externallyManagedConfigMembers;

    /** 外部管理的初始化方法 */
    @Nullable
    private Set<String> externallyManagedInitMethods;

    /** 外部管理的销毁方法 */
    @Nullable
    private Set<String> externallyManagedDestroyMethods;
}
```

#### 关键方法

```java
@Override
@Nullable
public String getParentName() {
    return null;  // RootBeanDefinition 没有父定义
}

@Override
public void setParentName(@Nullable String parentName) {
    if (parentName != null) {
        throw new IllegalArgumentException("Root bean cannot be changed into a child bean with parent reference");
    }
}

/**
 * 标记为已后置处理
 */
public void markAsPostProcessed() {
    synchronized (this.postProcessingLock) {
        this.postProcessed = true;
    }
}

/**
 * 注册外部管理的配置成员（如 @Autowired 字段/方法）
 */
public void registerExternallyManagedConfigMember(Member configMember) {
    synchronized (this.postProcessingLock) {
        if (this.externallyManagedConfigMembers == null) {
            this.externallyManagedConfigMembers = new LinkedHashSet<>(1);
        }
        this.externallyManagedConfigMembers.add(configMember);
    }
}

/**
 * 注册外部管理的初始化方法（如 @PostConstruct）
 */
public void registerExternallyManagedInitMethod(String initMethod) {
    synchronized (this.postProcessingLock) {
        if (this.externallyManagedInitMethods == null) {
            this.externallyManagedInitMethods = new LinkedHashSet<>(1);
        }
        this.externallyManagedInitMethods.add(initMethod);
    }
}

/**
 * 注册外部管理的销毁方法（如 @PreDestroy）
 */
public void registerExternallyManagedDestroyMethod(String destroyMethod) {
    synchronized (this.postProcessingLock) {
        if (this.externallyManagedDestroyMethods == null) {
            this.externallyManagedDestroyMethods = new LinkedHashSet<>(1);
        }
        this.externallyManagedDestroyMethods.add(destroyMethod);
    }
}
```

---

### ChildBeanDefinition

`ChildBeanDefinition` 用于定义继承自父 Bean 定义的子 Bean 定义。从 Spring 2.5 开始，推荐使用 `GenericBeanDefinition`。

```java
public class ChildBeanDefinition extends AbstractBeanDefinition {

    @Nullable
    private String parentName;

    public ChildBeanDefinition(String parentName) {
        super();
        this.parentName = parentName;
    }

    @Override
    public void setParentName(@Nullable String parentName) {
        this.parentName = parentName;
    }

    @Override
    @Nullable
    public String getParentName() {
        return this.parentName;
    }

    @Override
    public void validate() throws BeanDefinitionValidationException {
        super.validate();
        if (this.parentName == null) {
            throw new BeanDefinitionValidationException("'parentName' must be set in ChildBeanDefinition");
        }
    }
}
```

---

### GenericBeanDefinition

`GenericBeanDefinition` 是一站式 Bean 定义类，支持动态设置父 Bean 定义，是声明式 Bean 定义的首选。

```java
public class GenericBeanDefinition extends AbstractBeanDefinition {

    @Nullable
    private String parentName;

    public GenericBeanDefinition() {
        super();
    }

    public GenericBeanDefinition(BeanDefinition original) {
        super(original);
    }

    @Override
    public void setParentName(@Nullable String parentName) {
        this.parentName = parentName;
    }

    @Override
    @Nullable
    public String getParentName() {
        return this.parentName;
    }
}
```

---

## 单例注册表与三级缓存

### DefaultSingletonBeanRegistry

`DefaultSingletonBeanRegistry` 是单例 Bean 注册表的基础实现，管理单例 Bean 的生命周期和循环依赖。

#### 三级缓存结构

```java
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

    /** 【一级缓存】单例对象的缓存: bean名称 -> bean实例（完全初始化好的 Bean） */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

    /** 【三级缓存】单例工厂的缓存: bean名称 -> ObjectFactory（用于生成早期引用的工厂） */
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>(16);

    /** 【二级缓存】提前暴露的单例对象缓存: bean名称 -> bean实例（尚未填充属性的 Bean） */
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

    /** 已注册的单例 bean 名称集合（按注册顺序） */
    private final Set<String> registeredSingletons = Collections.synchronizedSet(new LinkedHashSet<>(256));

    /** 当前正在创建中的单例 bean 名称集合 */
    private final Set<String> singletonsCurrentlyInCreation = ConcurrentHashMap.newKeySet(16);

    /** 创建检查排除的 bean 名称集合 */
    private final Set<String> inCreationCheckExclusions = ConcurrentHashMap.newKeySet(16);

    /** 单例锁 */
    final Lock singletonLock = new ReentrantLock();

    /** 宽松创建锁（6.2 新增，用于处理并发创建） */
    private final Lock lenientCreationLock = new ReentrantLock();

    /** 宽松创建条件 */
    private final Condition lenientCreationFinished = this.lenientCreationLock.newCondition();

    /** 当前处于宽松创建模式的 bean 名称 */
    private final Set<String> singletonsInLenientCreation = new HashSet<>();

    /** 可销毁的 bean 实例映射 */
    private final Map<String, DisposableBean> disposableBeans = new LinkedHashMap<>();

    /** 包含 bean 映射：外部 bean -> 内部 bean 集合 */
    private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<>(16);

    /** 依赖 bean 映射：被依赖 bean -> 依赖它的 bean 集合 */
    private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

    /** bean 的依赖映射：bean -> 它依赖的 bean 集合 */
    private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);
}
```

#### 三级缓存获取单例

```java
/**
 * 从缓存中获取单例 bean（解决循环依赖的核心方法）
 *
 * 查询顺序：singletonObjects -> earlySingletonObjects -> singletonFactories
 */
@Nullable
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    // 1. 首先尝试从一级缓存获取（完全初始化的 bean）
    Object singletonObject = this.singletonObjects.get(beanName);

    // 2. 如果一级缓存没有，且该 bean 正在创建中
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        // 3. 尝试从二级缓存获取（提前暴露的 bean）
        singletonObject = this.earlySingletonObjects.get(beanName);

        // 4. 如果二级缓存也没有，且允许提前引用
        if (singletonObject == null && allowEarlyReference) {
            // 获取锁以确保线程安全
            if (!this.singletonLock.tryLock()) {
                return null;  // 避免在原始创建线程外进行早期单例推断
            }
            try {
                // 双重检查
                singletonObject = this.singletonObjects.get(beanName);
                if (singletonObject == null) {
                    singletonObject = this.earlySingletonObjects.get(beanName);
                    if (singletonObject == null) {
                        // 5. 尝试从三级缓存获取 ObjectFactory
                        ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                        if (singletonFactory != null) {
                            // 通过工厂创建早期引用
                            singletonObject = singletonFactory.getObject();
                            // 从三级缓存移除，放入二级缓存
                            if (this.singletonFactories.remove(beanName) != null) {
                                this.earlySingletonObjects.put(beanName, singletonObject);
                            }
                            else {
                                // 并发情况下可能已被其他线程处理
                                singletonObject = this.singletonObjects.get(beanName);
                            }
                        }
                    }
                }
            }
            finally {
                this.singletonLock.unlock();
            }
        }
    }
    return singletonObject;
}
```

#### 添加单例工厂（解决循环依赖的关键）

```java
/**
 * 添加单例工厂到三级缓存
 * 在 bean 实例化后、属性填充前调用，用于解决循环依赖
 */
protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
    Assert.notNull(singletonFactory, "Singleton factory must not be null");
    this.singletonFactories.put(beanName, singletonFactory);
    this.earlySingletonObjects.remove(beanName);  // 清除二级缓存
    this.registeredSingletons.add(beanName);
}

/**
 * 添加单例到一级缓存
 * 在 bean 完全初始化后调用
 */
protected void addSingleton(String beanName, Object singletonObject) {
    Object oldObject = this.singletonObjects.putIfAbsent(beanName, singletonObject);
    if (oldObject != null) {
        throw new IllegalStateException("Could not register object [" + singletonObject +
                "] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
    }
    // 清除二级和三级缓存
    this.singletonFactories.remove(beanName);
    this.earlySingletonObjects.remove(beanName);
    this.registeredSingletons.add(beanName);

    // 执行单例创建回调
    Consumer<Object> callback = this.singletonCallbacks.get(beanName);
    if (callback != null) {
        callback.accept(singletonObject);
    }
}
```

---

### 循环依赖解决方案

#### 循环依赖场景

Spring 通过三级缓存解决单例 Bean 的循环依赖问题。典型的循环依赖场景：

```java
@Component
public class A {
    @Autowired
    private B b;  // A 依赖 B
}

@Component
public class B {
    @Autowired
    private A a;  // B 依赖 A
}
```

#### 解决流程图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         循环依赖解决流程                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  创建 A：                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ 1. getBean("a")                                                    │   │
│  │    │                                                               │   │
│  │    ▼                                                               │   │
│  │ 2. getSingleton("a") → null（缓存中没有）                          │   │
│  │    │                                                               │   │
│  │    ▼                                                               │   │
│  │ 3. beforeSingletonCreation("a") → 标记 a 正在创建                  │   │
│  │    │                                                               │   │
│  │    ▼                                                               │   │
│  │ 4. createBean("a")                                                 │   │
│  │    │                                                               │   │
│  │    ├── 4.1 createBeanInstance("a") → 实例化 A（调用构造器）        │   │
│  │    │   │                                                           │   │
│  │    │   └── 4.1.1 addSingletonFactory("a", ObjectFactory)          │   │
│  │    │       【关键】将 A 的早期引用工厂放入三级缓存                    │   │
│  │    │       singletonFactories.put("a", () -> getEarlyBeanReference(a))│ │
│  │    │                                                               │   │
│  │    ├── 4.2 populateBean("a") → 填充属性                            │   │
│  │    │   │                                                           │   │
│  │    │   └── 需要注入 B → getBean("b")                               │   │
│  │    │       │                                                       │   │
│  │    │       ▼                                                       │   │
│  │    │       ┌─────────────────────────────────────────────────────┐ │   │
│  │    │       │ 创建 B：                                              │ │   │
│  │    │       │ 1. getBean("b")                                      │ │   │
│  │    │       │ 2. getSingleton("b") → null                          │ │   │
│  │    │       │ 3. beforeSingletonCreation("b")                      │ │   │
│  │    │       │ 4. createBean("b")                                   │ │   │
│  │    │       │    ├── 4.1 createBeanInstance("b") → 实例化 B        │ │   │
│  │    │       │    │   └── addSingletonFactory("b", ...)            │ │   │
│  │    │       │    ├── 4.2 populateBean("b")                         │ │   │
│  │    │       │    │   │                                             │ │   │
│  │    │       │    │   └── 需要注入 A → getBean("a")                 │ │   │
│  │    │       │    │       │                                         │ │   │
│  │    │       │    │       ▼                                         │ │   │
│  │    │       │    │       ┌────────────────────────────────────────┐│ │   │
│  │    │       │    │       │ 获取 A：                                ││ │   │
│  │    │       │    │       │ 1. getSingleton("a", true)              ││ │   │
│  │    │       │    │       │    【关键】从三级缓存获取 A 的早期引用    ││ │   │
│  │    │       │    │       │    - singletonObjects: null             ││ │   │
│  │    │       │    │       │    - earlySingletonObjects: null        ││ │   │
│  │    │       │    │       │    - singletonFactories: 命中！         ││ │   │
│  │    │       │    │       │    - 调用 ObjectFactory.getObject()     ││ │   │
│  │    │       │    │       │    - 返回 A 的早期引用                   ││ │   │
│  │    │       │    │       │    - 升级到二级缓存                      ││ │   │
│  │    │       │    │       │ 2. 返回 A 的早期引用给 B                  ││ │   │
│  │    │       │    │       └────────────────────────────────────────┘│ │   │
│  │    │       │    │                                                 │ │   │
│  │    │       │    ├── 4.3 initializeBean("b") → 初始化 B            │ │   │
│  │    │       │    └── 返回完整的 B                                    │ │   │
│  │    │       │                                                      │ │   │
│  │    │       └── 返回 B 给 A                                         │ │   │
│  │    │                                                               │   │
│  │    ├── 4.3 initializeBean("a") → 初始化 A                          │   │
│  │    └── 返回完整的 A                                                │   │
│  │                                                                     │   │
│  │ 5. addSingleton("a", A) → 将 A 放入一级缓存                        │   │
│  │    - singletonObjects.put("a", A)                                  │   │
│  │    - singletonFactories.remove("a")                                │   │
│  │    - earlySingletonObjects.remove("a")                             │   │
│  │                                                                     │   │
│  │ 6. afterSingletonCreation("a") → 标记 a 不再创建中                 │   │
│  │                                                                     │   │
│  │ 7. 返回 A                                                          │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 关键代码：获取早期 Bean 引用

```java
/**
 * 获取早期 Bean 引用（用于解决循环依赖）
 * 在 bean 初始化前调用，可能返回代理对象
 */
protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
    Object exposedObject = bean;
    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                // 允许 SmartInstantiationAwareBeanPostProcessor 返回早期代理
                exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
            }
        }
    }
    return exposedObject;
}
```

---

## 实例化策略

### InstantiationStrategy

`InstantiationStrategy` 是实例化策略接口，定义了创建 Bean 实例的方法。

```java
public interface InstantiationStrategy {

    /**
     * 使用无参构造器实例化
     */
    Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner)
            throws BeansException;

    /**
     * 使用指定构造器实例化
     */
    Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
            Constructor<?> ctor, Object... args) throws BeansException;

    /**
     * 使用工厂方法实例化
     */
    Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
            @Nullable Object factoryBean, Method factoryMethod, Object... args)
            throws BeansException;

    /**
     * 获取实际的 bean 类（考虑 CGLIB 子类）
     */
    default Class<?> getActualBeanClass(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
        return bd.getBeanClass();
    }
}
```

---

### SimpleInstantiationStrategy

`SimpleInstantiationStrategy` 是简单的实例化策略，使用反射创建实例，不支持方法注入。

```java
public class SimpleInstantiationStrategy implements InstantiationStrategy {

    private static final ThreadLocal<Method> currentlyInvokedFactoryMethod = new ThreadLocal<>();

    @Override
    public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
        // 如果没有方法重写，使用反射直接实例化
        if (!bd.hasMethodOverrides()) {
            Constructor<?> constructorToUse;
            synchronized (bd.constructorArgumentLock) {
                constructorToUse = (Constructor<?>) bd.resolvedConstructorOrFactoryMethod;
                if (constructorToUse == null) {
                    Class<?> clazz = bd.getBeanClass();
                    if (clazz.isInterface()) {
                        throw new BeanInstantiationException(clazz, "Specified class is an interface");
                    }
                    try {
                        constructorToUse = clazz.getDeclaredConstructor();
                        bd.resolvedConstructorOrFactoryMethod = constructorToUse;
                    }
                    catch (Throwable ex) {
                        throw new BeanInstantiationException(clazz, "No default constructor found", ex);
                    }
                }
            }
            return BeanUtils.instantiateClass(constructorToUse);
        }
        else {
            // 有方法重写，需要生成 CGLIB 子类
            return instantiateWithMethodInjection(bd, beanName, owner);
        }
    }

    @Override
    public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
            Constructor<?> ctor, Object... args) {

        if (!bd.hasMethodOverrides()) {
            return BeanUtils.instantiateClass(ctor, args);
        }
        else {
            return instantiateWithMethodInjection(bd, beanName, owner, ctor, args);
        }
    }

    @Override
    public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
            @Nullable Object factoryBean, Method factoryMethod, Object... args) {

        return instantiateWithFactoryMethod(factoryMethod, () -> {
            try {
                ReflectionUtils.makeAccessible(factoryMethod);
                Object result = factoryMethod.invoke(factoryBean, args);
                if (result == null) {
                    result = new NullBean();
                }
                return result;
            }
            // ... 异常处理
        });
    }
}
```

---

### CglibSubclassingInstantiationStrategy

`CglibSubclassingInstantiationStrategy` 使用 CGLIB 生成子类来支持方法注入（Lookup 和 Replace）。

#### 核心实现

```java
public class CglibSubclassingInstantiationStrategy extends SimpleInstantiationStrategy {

    /** CGLIB 回调数组索引 */
    private static final int PASSTHROUGH = 0;      // 直接透传
    private static final int LOOKUP_OVERRIDE = 1;  // Lookup 方法重写
    private static final int METHOD_REPLACER = 2;  // 方法替换

    @Override
    protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName,
            BeanFactory owner) {
        return new CglibSubclassCreator(bd, owner).instantiate(null);
    }

    @Override
    protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName,
            BeanFactory owner, @Nullable Constructor<?> ctor, Object... args) {
        return new CglibSubclassCreator(bd, owner).instantiate(ctor, args);
    }

    /**
     * CGLIB 子类创建器
     */
    private static class CglibSubclassCreator {

        private static final Class<?>[] CALLBACK_TYPES = new Class<?>[] {
                NoOp.class,
                LookupOverrideMethodInterceptor.class,
                ReplaceOverrideMethodInterceptor.class
        };

        private final RootBeanDefinition beanDefinition;
        private final BeanFactory owner;

        CglibSubclassCreator(RootBeanDefinition beanDefinition, BeanFactory owner) {
            this.beanDefinition = beanDefinition;
            this.owner = owner;
        }

        /**
         * 创建增强子类并实例化
         */
        public Object instantiate(@Nullable Constructor<?> ctor, Object... args) {
            // 1. 创建 CGLIB 增强子类
            Class<?> subclass = createEnhancedSubclass(this.beanDefinition);

            Object instance;
            if (ctor == null) {
                // 使用无参构造器
                instance = BeanUtils.instantiateClass(subclass);
            }
            else {
                // 使用指定构造器
                try {
                    Constructor<?> enhancedSubclassConstructor = subclass.getConstructor(ctor.getParameterTypes());
                    instance = enhancedSubclassConstructor.newInstance(args);
                }
                catch (Exception ex) {
                    throw new BeanInstantiationException(this.beanDefinition.getBeanClass(),
                            "Failed to invoke constructor for CGLIB enhanced subclass [" + subclass.getName() + "]", ex);
                }
            }

            // 2. 设置 CGLIB 回调
            Factory factory = (Factory) instance;
            factory.setCallbacks(new Callback[] {
                    NoOp.INSTANCE,
                    new LookupOverrideMethodInterceptor(this.beanDefinition, this.owner),
                    new ReplaceOverrideMethodInterceptor(this.beanDefinition, this.owner)
            });
            return instance;
        }

        /**
         * 创建 CGLIB 增强子类
         */
        public Class<?> createEnhancedSubclass(RootBeanDefinition beanDefinition) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(beanDefinition.getBeanClass());
            enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
            enhancer.setAttemptLoad(AotDetector.useGeneratedArtifacts());

            if (this.owner instanceof ConfigurableBeanFactory cbf) {
                ClassLoader cl = cbf.getBeanClassLoader();
                enhancer.setStrategy(new ClassLoaderAwareGeneratorStrategy(cl));
            }

            // 设置回调过滤器，根据方法选择不同的回调
            enhancer.setCallbackFilter(new MethodOverrideCallbackFilter(beanDefinition));
            enhancer.setCallbackTypes(CALLBACK_TYPES);

            return enhancer.createClass();
        }
    }
}
```

#### Lookup 方法拦截器

```java
/**
 * CGLIB MethodInterceptor 用于实现 Lookup 方法注入
 * 将方法调用替换为从容器的 bean 查找
 */
private static class LookupOverrideMethodInterceptor extends CglibIdentitySupport implements MethodInterceptor {

    private final BeanFactory owner;

    public LookupOverrideMethodInterceptor(RootBeanDefinition beanDefinition, BeanFactory owner) {
        super(beanDefinition);
        this.owner = owner;
    }

    @Override
    @Nullable
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy mp) throws Throwable {
        // 获取 LookupOverride 配置
        LookupOverride lo = (LookupOverride) getBeanDefinition().getMethodOverrides().getOverride(method);
        Assert.state(lo != null, "LookupOverride not found");

        Object[] argsToUse = (args.length > 0 ? args : null);

        if (StringUtils.hasText(lo.getBeanName())) {
            // 按名称查找 bean
            Object bean = (argsToUse != null ?
                    this.owner.getBean(lo.getBeanName(), argsToUse) :
                    this.owner.getBean(lo.getBeanName()));
            return (bean.equals(null) ? null : bean);
        }
        else {
            // 按类型查找 bean
            ResolvableType genericReturnType = ResolvableType.forMethodReturnType(method);
            return (argsToUse != null ?
                    this.owner.getBeanProvider(genericReturnType).getObject(argsToUse) :
                    this.owner.getBeanProvider(genericReturnType).getObject());
        }
    }
}
```

#### Replace 方法拦截器

```java
/**
 * CGLIB MethodInterceptor 用于实现方法替换
 * 将方法调用委托给 MethodReplacer
 */
private static class ReplaceOverrideMethodInterceptor extends CglibIdentitySupport implements MethodInterceptor {

    private final BeanFactory owner;

    public ReplaceOverrideMethodInterceptor(RootBeanDefinition beanDefinition, BeanFactory owner) {
        super(beanDefinition);
        this.owner = owner;
    }

    @Override
    @Nullable
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy mp) throws Throwable {
        ReplaceOverride ro = (ReplaceOverride) getBeanDefinition().getMethodOverrides().getOverride(method);
        Assert.state(ro != null, "ReplaceOverride not found");

        // 获取 MethodReplacer bean
        MethodReplacer mr = this.owner.getBean(ro.getMethodReplacerBeanName(), MethodReplacer.class);

        // 调用替换逻辑
        return processReturnType(method, mr.reimplement(obj, method, args));
    }

    @Nullable
    private <T> T processReturnType(Method method, @Nullable T returnValue) {
        Class<?> returnType = method.getReturnType();
        if (returnValue == null && returnType != void.class && returnType.isPrimitive()) {
            throw new IllegalStateException(
                    "Null return value from MethodReplacer does not match primitive return type for: " + method);
        }
        return returnValue;
    }
}
```

---

## 构造器解析

### ConstructorResolver

`ConstructorResolver` 负责解析和选择用于实例化 Bean 的构造器或工厂方法。

#### 核心功能

```java
class ConstructorResolver {

    private static final Object[] EMPTY_ARGS = new Object[0];

    private final AbstractAutowireCapableBeanFactory beanFactory;

    /**
     * 使用自动装配的构造器解析创建 bean 实例
     */
    public BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mbd,
            @Nullable Constructor<?>[] chosenCtors, @Nullable Object[] explicitArgs) {

        BeanWrapperImpl bw = new BeanWrapperImpl();
        this.beanFactory.initBeanWrapper(bw);

        Constructor<?> constructorToUse = null;
        ArgumentsHolder argsHolderToUse = null;
        Object[] argsToUse = null;

        // 1. 确定构造器参数
        if (explicitArgs != null) {
            argsToUse = explicitArgs;
        }
        else {
            Object[] argsToResolve = null;
            synchronized (mbd.constructorArgumentLock) {
                constructorToUse = (Constructor<?>) mbd.resolvedConstructorOrFactoryMethod;
                if (constructorToUse != null && mbd.constructorArgumentsResolved) {
                    argsToUse = mbd.resolvedConstructorArguments;
                    if (argsToUse == null) {
                        argsToResolve = mbd.preparedConstructorArguments;
                    }
                }
            }
            if (argsToResolve != null) {
                argsToUse = resolvePreparedArguments(beanName, mbd, bw, constructorToUse, argsToResolve);
            }
        }

        // 2. 如果没有解析到构造器和参数，需要解析
        if (constructorToUse == null || argsToUse == null) {
            // 获取候选构造器
            Constructor<?>[] candidates = chosenCtors;
            if (candidates == null) {
                Class<?> beanClass = mbd.getBeanClass();
                candidates = (mbd.isNonPublicAccessAllowed() ?
                        beanClass.getDeclaredConstructors() : beanClass.getConstructors());
            }

            // 3. 选择最佳匹配构造器
            AutowireUtils.SortConstructors.sort(candidates);

            int minTypeDiffWeight = Integer.MAX_VALUE;
            Set<Constructor<?>> ambiguousConstructors = null;

            for (Constructor<?> candidate : candidates) {
                Class<?>[] paramTypes = candidate.getParameterTypes();

                // 解析构造器参数
                ArgumentsHolder argsHolder;
                if (explicitArgs != null) {
                    // 使用显式参数
                    if (paramTypes.length != explicitArgs.length) {
                        continue;
                    }
                    argsHolder = new ArgumentsHolder(explicitArgs);
                }
                else {
                    // 解析构造器参数值
                    String[] paramNames = null;
                    ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
                    if (pnd != null) {
                        paramNames = pnd.getParameterNames(candidate);
                    }
                    argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw,
                            paramTypes, paramNames, candidate, autowiring);
                }

                // 计算类型差异权重
                int typeDiffWeight = argsHolder.getTypeDifferenceWeight(paramTypes);
                if (typeDiffWeight < minTypeDiffWeight) {
                    constructorToUse = candidate;
                    argsHolderToUse = argsHolder;
                    argsToUse = argsHolder.arguments;
                    minTypeDiffWeight = typeDiffWeight;
                    ambiguousConstructors = null;
                }
                else if (constructorToUse != null && typeDiffWeight == minTypeDiffWeight) {
                    // 存在歧义
                    if (ambiguousConstructors == null) {
                        ambiguousConstructors = new LinkedHashSet<>();
                        ambiguousConstructors.add(constructorToUse);
                    }
                    ambiguousConstructors.add(candidate);
                }
            }

            // 4. 缓存解析结果
            if (explicitArgs == null && argsHolderToUse != null) {
                argsHolderToUse.storeCache(mbd, constructorToUse);
            }
        }

        // 5. 实例化
        try {
            Object beanInstance = this.beanFactory.getInstantiationStrategy().instantiate(
                    mbd, beanName, this.beanFactory, constructorToUse, argsToUse);
            bw.setWrappedInstance(beanInstance);
            return bw;
        }
        catch (Throwable ex) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "Bean instantiation via constructor failed", ex);
        }
    }
}
```

#### 工厂方法解析

```java
/**
 * 使用命名工厂方法实例化 bean
 */
public BeanWrapper instantiateUsingFactoryMethod(
        String beanName, RootBeanDefinition mbd, @Nullable Object[] explicitArgs) {

    BeanWrapperImpl bw = new BeanWrapperImpl();
    this.beanFactory.initBeanWrapper(bw);

    Object factoryBean;
    Class<?> factoryClass;
    boolean isStatic;

    // 1. 确定工厂 bean
    String factoryBeanName = mbd.getFactoryBeanName();
    if (factoryBeanName != null) {
        // 实例工厂方法
        factoryBean = this.beanFactory.getBean(factoryBeanName);
        factoryClass = factoryBean.getClass();
        isStatic = false;
    }
    else {
        // 静态工厂方法
        factoryBean = null;
        factoryClass = mbd.getBeanClass();
        isStatic = true;
    }

    // 2. 解析工厂方法
    Method factoryMethodToUse = null;
    ArgumentsHolder argsHolderToUse = null;
    Object[] argsToUse = null;

    // 3. 尝试从缓存获取
    if (explicitArgs == null) {
        // ... 缓存逻辑
    }

    // 4. 如果没有解析到，需要查找工厂方法
    if (factoryMethodToUse == null || argsToUse == null) {
        // 获取所有候选方法
        List<Method> candidates = new ArrayList<>();
        Method[] rawCandidates = (mbd.isNonPublicAccessAllowed() ?
                factoryClass.getDeclaredMethods() : factoryClass.getMethods());

        for (Method candidate : rawCandidates) {
            if (Modifier.isStatic(candidate.getModifiers()) == isStatic &&
                    mbd.isFactoryMethod(candidate)) {
                candidates.add(candidate);
            }
        }

        // 5. 选择最佳匹配方法
        // ... 类似构造器解析的逻辑
    }

    // 6. 实例化
    try {
        Object beanInstance = this.beanFactory.getInstantiationStrategy().instantiate(
                mbd, beanName, this.beanFactory, factoryBean, factoryMethodToUse, argsToUse);
        bw.setWrappedInstance(beanInstance);
        return bw;
    }
    catch (Throwable ex) {
        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                "Bean instantiation via factory method failed", ex);
    }
}
```

---

## 方法注入

### MethodOverride 体系

#### MethodOverride（抽象基类）

```java
public abstract class MethodOverride implements BeanMetadataElement {

    private final String methodName;  // 要重写的方法名

    private boolean overloaded = true;  // 是否重载

    @Nullable
    private Object source;

    protected MethodOverride(String methodName) {
        Assert.notNull(methodName, "Method name must not be null");
        this.methodName = methodName;
    }

    /**
     * 子类必须实现此方法来判断是否匹配给定方法
     */
    public abstract boolean matches(Method method);
}
```

#### LookupOverride

```java
/**
 * 表示一个 Lookup 方法重写
 * 每次调用该方法时，从容器的 IoC 上下文中查找对象
 */
public class LookupOverride extends MethodOverride {

    @Nullable
    private final String beanName;  // 要查找的 bean 名称

    @Nullable
    private Method method;

    public LookupOverride(String methodName, @Nullable String beanName) {
        super(methodName);
        this.beanName = beanName;
    }

    /**
     * 匹配方法
     */
    @Override
    public boolean matches(Method method) {
        if (this.method != null) {
            return method.equals(this.method);
        }
        else {
            return (method.getName().equals(getMethodName()) &&
                    (!isOverloaded() ||
                     Modifier.isAbstract(method.getModifiers()) ||
                     method.getParameterCount() == 0));
        }
    }

    @Nullable
    public String getBeanName() {
        return this.beanName;
    }
}
```

#### ReplaceOverride

```java
/**
 * 表示��个方法替换重写
 * 使用 MethodReplacer 替换方法的实现
 */
public class ReplaceOverride extends MethodOverride {

    private final String methodReplacerBeanName;  // MethodReplacer bean 名称

    private final List<String> typeIdentifiers = new ArrayList<>();  // 参数类型标识符

    public ReplaceOverride(String methodName, String methodReplacerBeanName) {
        super(methodName);
        Assert.notNull(methodReplacerBeanName, "Method replacer bean name must not be null");
        this.methodReplacerBeanName = methodReplacerBeanName;
    }

    public String getMethodReplacerBeanName() {
        return this.methodReplacerBeanName;
    }

    /**
     * 添加类型标识符（用于区分重载方法）
     */
    public void addTypeIdentifier(String identifier) {
        this.typeIdentifiers.add(identifier);
    }

    @Override
    public boolean matches(Method method) {
        if (!method.getName().equals(getMethodName())) {
            return false;
        }
        if (!isOverloaded()) {
            return true;
        }
        // 参数类型匹配
        if (this.typeIdentifiers.size() != method.getParameterCount()) {
            return false;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < this.typeIdentifiers.size(); i++) {
            String identifier = this.typeIdentifiers.get(i);
            if (!parameterTypes[i].getName().contains(identifier)) {
                return false;
            }
        }
        return true;
    }
}
```

#### MethodReplacer 接口

```java
/**
 * 方法替换器接口
 * 实现此接口的类可以替换 IoC 托管对象上的任意方法
 */
public interface MethodReplacer {

    /**
     * 重新实现给定方法
     * @param obj 要重新实现方法的对象实例
     * @param method 要重新实现的方法
     * @param args 方法参数
     * @return 方法的返回值
     */
    Object reimplement(Object obj, Method method, Object[] args) throws Throwable;
}
```

---

## BeanDefinition 合并

### 合并流程

当 Bean 定义有父定义时，Spring 需要合并父子定义以创建最终的 `RootBeanDefinition`。

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BeanDefinition 合并流程                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  GenericBeanDefinition / ChildBeanDefinition                                │
│         │                                                                   │
│         │  parentName = "parentBean"                                        │
│         ▼                                                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    getMergedBeanDefinition()                       │   │
│  │                         │                                          │   │
│  │                         ▼                                          │   │
│  │              ┌─────────────────────┐                               │   │
│  │              │  检查缓存是否存在    │                               │   │
│  │              │  mergedBeanDefinitions│                               │   │
│  │              └─────────────────────┘                               │   │
│  │                         │                                          │   │
│  │              ┌──────────┴──────────┐                               │   │
│  │              │ 缓存命中            │ 缓存未命中                    │   │
│  │              ▼                     ▼                               │   │
│  │         直接返回              执行合并                             │   │
│  │                                    │                               │   │
│  │                                    ▼                               │   │
│  │                         ┌─────────────────────┐                    │   │
│  │                         │  获取父 BeanDefinition │                  │   │
│  │                         │  getBeanDefinition()  │                  │   │
│  │                         └─────────────────────┘                    │   │
│  │                                    │                               │   │
│  │                                    ▼                               │   │
│  │                         ┌─────────────────────┐                    │   │
│  │                         │  递归合并父定义      │                    │   │
│  │                         │  （如果父也有父）    │                    │   │
│  │                         └─────────────────────┘                    │   │
│  │                                    │                               │   │
│  │                                    ▼                               │   │
│  │                         ┌─────────────────────┐                    │   │
│  │                         │  深拷贝父定义为     │                    │   │
│  │                         │  RootBeanDefinition │                    │   │
│  │                         └─────────────────────┘                    │   │
│  │                                    │                               │   │
│  │                                    ▼                               │   │
│  │                         ┌─────────────────────┐                    │   │
│  │                         │  覆盖子定义的属性    │                    │   │
│  │                         │  - beanClass        │                    │   │
│  │                         │  - scope            │                    │   │
│  │                         │  - lazyInit         │                    │   │
│  │                         │  - propertyValues   │                    │   │
│  │                         │  - constructorArgs  │                    │   │
│  │                         │  - methodOverrides  │                    │   │
│  │                         └─────────────────────┘                    │   │
│  │                                    │                               │   │
│  │                                    ▼                               │   │
│  │                         ┌─────────────────────┐                    │   │
│  │                         │  合并集合属性        │                    │   │
│  │                         │  （如果 merge=true） │                    │   │
│  │                         └─────────────────────┘                    │   │
│  │                                    │                               │   │
│  │                                    ▼                               │   │
│  │                         ┌─────────────────────┐                    │   │
│  │                         │  缓存合并结果        │                    │   │
│  │                         │  mergedBeanDefinitions│                  │   │
│  │                         └─────────────────────┘                    │   │
│  │                                    │                               │   │
│  │                                    ▼                               │   │
│  │                              返回 RootBeanDefinition               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 合并实现

```java
/**
 * 获取合并的 BeanDefinition
 */
protected RootBeanDefinition getMergedBeanDefinition(
        String beanName, BeanDefinition bd, @Nullable BeanDefinition containingBd)
        throws BeanDefinitionStoreException {

    synchronized (this.mergedBeanDefinitions) {
        RootBeanDefinition mbd = null;
        RootBeanDefinition previous = null;

        // 检查缓存
        if (containingBd == null) {
            mbd = this.mergedBeanDefinitions.get(beanName);
            previous = mbd;
        }

        // 如果缓存不存在或已过期，需要重新合并
        if (mbd == null || mbd.stale) {
            previous = mbd;

            // 获取父定义
            BeanDefinition pbd;
            String parentBeanName = bd.getParentName();

            if (parentBeanName != null) {
                // 有父定义，递归合并
                if (!beanName.equals(parentBeanName)) {
                    pbd = getMergedBeanDefinition(parentBeanName);
                }
                else {
                    // 父定义在父工厂中
                    BeanFactory parent = getParentBeanFactory();
                    if (parent instanceof ConfigurableBeanFactory) {
                        pbd = ((ConfigurableBeanFactory) parent).getMergedBeanDefinition(parentBeanName);
                    }
                    else {
                        throw new NoSuchBeanDefinitionException(parentBeanName,
                                "Parent name '" + parentBeanName + "' is equal to bean name '" +
                                beanName + "': cannot be resolved without a ConfigurableBeanFactory parent");
                    }
                }

                // 深拷贝父定义为 RootBeanDefinition
                mbd = new RootBeanDefinition(pbd);
                // 用子定义覆盖
                mbd.overrideFrom(bd);
            }
            else {
                // 没有父定义，直接拷贝
                mbd = new RootBeanDefinition(bd);
            }

            // 设置默认作用域
            if (!StringUtils.hasLength(mbd.getScope())) {
                mbd.setScope(SCOPE_SINGLETON);
            }

            // 缓存合并结果
            if (containingBd == null && isCacheBeanMetadata()) {
                this.mergedBeanDefinitions.put(beanName, mbd);
            }
        }

        return mbd;
    }
}
```

### 属性覆盖

```java
/**
 * 用给定的 BeanDefinition 覆盖当前定义的属性
 */
public void overrideFrom(BeanDefinition other) {
    // 覆盖基本属性
    if (StringUtils.hasLength(other.getBeanClassName())) {
        setBeanClassName(other.getBeanClassName());
    }
    if (StringUtils.hasLength(other.getScope())) {
        setScope(other.getScope());
    }
    setAbstract(other.isAbstract());
    setLazyInit(other.isLazyInit());

    // 覆盖构造器参数
    if (other instanceof AbstractBeanDefinition) {
        AbstractBeanDefinition otherAbd = (AbstractBeanDefinition) other;
        if (otherAbd.hasConstructorArgumentValues()) {
            getConstructorArgumentValues().addArgumentValues(other.getConstructorArgumentValues());
        }
    }

    // 覆盖属性值
    if (other.hasPropertyValues()) {
        getPropertyValues().addPropertyValues(other.getPropertyValues());
    }

    // 覆盖方法重写
    if (other instanceof AbstractBeanDefinition) {
        AbstractBeanDefinition otherAbd = (AbstractBeanDefinition) other;
        if (otherAbd.hasMethodOverrides()) {
            getMethodOverrides().addOverrides(otherAbd.getMethodOverrides());
        }
    }

    // 覆盖其他属性
    setFactoryBeanName(other.getFactoryBeanName());
    setFactoryMethodName(other.getFactoryMethodName());
    setInitMethodName(other.getInitMethodName());
    setDestroyMethodName(other.getDestroyMethodName());
    setDescription(other.getDescription());
    setResourceDescription(other.getResourceDescription());
    setRole(other.getRole());
}
```

---

## 其他重要组件

### BeanDefinitionValueResolver

`BeanDefinitionValueResolver` 用于解析 Bean 定义中的值，包括运行时 bean 引用、集合等。

```java
public class BeanDefinitionValueResolver {

    private final AbstractAutowireCapableBeanFactory beanFactory;
    private final String beanName;
    private final BeanDefinition beanDefinition;
    private final TypeConverter typeConverter;

    /**
     * 解析值（如果需要）
     */
    @Nullable
    public Object resolveValueIfNecessary(Object argName, @Nullable Object value) {
        // 运行时 bean 引用
        if (value instanceof RuntimeBeanReference ref) {
            return resolveReference(argName, ref);
        }
        // 运行时 bean 名称引用
        else if (value instanceof RuntimeBeanNameReference ref) {
            String refName = ref.getBeanName();
            refName = String.valueOf(doEvaluate(refName));
            if (!this.beanFactory.containsBean(refName)) {
                throw new BeanDefinitionStoreException(
                        "Invalid bean name '" + refName + "' in bean reference for " + argName);
            }
            return refName;
        }
        // BeanDefinitionHolder（内部 bean）
        else if (value instanceof BeanDefinitionHolder bdHolder) {
            return resolveInnerBean(bdHolder.getBeanName(), bdHolder.getBeanDefinition(),
                    (name, mbd) -> resolveInnerBeanValue(argName, name, mbd));
        }
        // BeanDefinition（内部 bean）
        else if (value instanceof BeanDefinition bd) {
            return resolveInnerBean(null, bd,
                    (name, mbd) -> resolveInnerBeanValue(argName, name, mbd));
        }
        // 依赖描述符（用于 @Autowired）
        else if (value instanceof DependencyDescriptor dependencyDescriptor) {
            Set<String> autowiredBeanNames = new LinkedHashSet<>(2);
            Object result = this.beanFactory.resolveDependency(
                    dependencyDescriptor, this.beanName, autowiredBeanNames, this.typeConverter);
            for (String autowiredBeanName : autowiredBeanNames) {
                if (this.beanFactory.containsBean(autowiredBeanName)) {
                    this.beanFactory.registerDependentBean(autowiredBeanName, this.beanName);
                }
            }
            return result;
        }
        // 托管数组
        else if (value instanceof ManagedArray managedArray) {
            return resolveManagedArray(argName, (List<?>) value, elementType);
        }
        // 托管列表
        else if (value instanceof ManagedList<?> managedList) {
            return resolveManagedList(argName, managedList);
        }
        // 托管 Set
        else if (value instanceof ManagedSet<?> managedSet) {
            return resolveManagedSet(argName, managedSet);
        }
        // 托管 Map
        else if (value instanceof ManagedMap<?, ?> managedMap) {
            return resolveManagedMap(argName, managedMap);
        }
        // 托管 Properties
        else if (value instanceof ManagedProperties original) {
            Properties copy = new Properties();
            original.forEach((propKey, propValue) -> {
                // 解析键值
                copy.put(propKey, propValue);
            });
            return copy;
        }
        // 类型化字符串值
        else if (value instanceof TypedStringValue typedStringValue) {
            Object valueObject = evaluate(typedStringValue);
            Class<?> resolvedTargetType = resolveTargetType(typedStringValue);
            if (resolvedTargetType != null) {
                return this.typeConverter.convertIfNecessary(valueObject, resolvedTargetType);
            }
            else {
                return valueObject;
            }
        }
        // NullBean
        else if (value instanceof NullBean) {
            return null;
        }
        else {
            return evaluate(value);
        }
    }
}
```

### DisposableBeanAdapter

`DisposableBeanAdapter` 实现了 `DisposableBean` 和 `Runnable` 接口，用于执行 bean 的销毁操作。

```java
class DisposableBeanAdapter implements DisposableBean, Runnable, Serializable {

    private final Object bean;
    private final String beanName;
    private final boolean invokeDisposableBean;
    private boolean invokeAutoCloseable;

    @Nullable
    private String[] destroyMethodNames;

    @Nullable
    private transient Method[] destroyMethods;

    @Nullable
    private final List<DestructionAwareBeanPostProcessor> beanPostProcessors;

    /**
     * 执行销毁操作
     */
    @Override
    public void destroy() {
        // 1. 调用 DestructionAwareBeanPostProcessor
        if (!CollectionUtils.isEmpty(this.beanPostProcessors)) {
            for (DestructionAwareBeanPostProcessor processor : this.beanPostProcessors) {
                processor.postProcessBeforeDestruction(this.bean, this.beanName);
            }
        }

        // 2. 调用 DisposableBean.destroy()
        if (this.invokeDisposableBean) {
            ((DisposableBean) this.bean).destroy();
        }

        // 3. 调用 AutoCloseable.close()
        if (this.invokeAutoCloseable) {
            ((AutoCloseable) this.bean).close();
        }
        // 4. 调用自定义 destroy 方法
        else if (this.destroyMethods != null) {
            for (Method destroyMethod : this.destroyMethods) {
                invokeCustomDestroyMethod(destroyMethod);
            }
        }
    }

    /**
     * 推断销毁方法
     */
    @Nullable
    static String[] inferDestroyMethodsIfNecessary(Class<?> target, RootBeanDefinition beanDefinition) {
        String[] destroyMethodNames = beanDefinition.getDestroyMethodNames();

        String destroyMethodName = beanDefinition.resolvedDestroyMethodName;
        if (destroyMethodName == null) {
            destroyMethodName = beanDefinition.getDestroyMethodName();
            boolean autoCloseable = AutoCloseable.class.isAssignableFrom(target);
            boolean executorService = ExecutorService.class.isAssignableFrom(target);

            if (AbstractBeanDefinition.INFER_METHOD.equals(destroyMethodName) ||
                    (destroyMethodName == null && (autoCloseable || executorService))) {

                destroyMethodName = null;
                if (!(DisposableBean.class.isAssignableFrom(target))) {
                    if (executorService) {
                        destroyMethodName = SHUTDOWN_METHOD_NAME;
                        // 检查是否有自定义 close 方法
                        try {
                            if (target.getMethod(CLOSE_METHOD_NAME).getDeclaringClass() != ExecutorService.class) {
                                destroyMethodName = CLOSE_METHOD_NAME;
                            }
                        }
                        catch (NoSuchMethodException ex) {
                            // 忽略
                        }
                    }
                    else if (autoCloseable) {
                        destroyMethodName = CLOSE_METHOD_NAME;
                    }
                    else {
                        // 尝试查找 close 或 shutdown 方法
                        try {
                            destroyMethodName = target.getMethod(CLOSE_METHOD_NAME).getName();
                        }
                        catch (NoSuchMethodException ex) {
                            try {
                                destroyMethodName = target.getMethod(SHUTDOWN_METHOD_NAME).getName();
                            }
                            catch (NoSuchMethodException ex2) {
                                // 没有找到销毁方法
                            }
                        }
                    }
                }
            }
            beanDefinition.resolvedDestroyMethodName = (destroyMethodName != null ? destroyMethodName : "");
        }
        return (StringUtils.hasLength(destroyMethodName) ? new String[] {destroyMethodName} : null);
    }
}
```

### AutowireCandidateResolver

`AutowireCandidateResolver` 策略接口用于确定特定的 bean 定义是否可作为特定依赖的自动装配候选。

```java
public interface AutowireCandidateResolver {

    /**
     * 确定给定的 bean 定义是否可作为自动装配候选
     */
    default boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
        return bdHolder.getBeanDefinition().isAutowireCandidate();
    }

    /**
     * 确定给定的描述符是否实际上是必需的
     */
    default boolean isRequired(DependencyDescriptor descriptor) {
        return descriptor.isRequired();
    }

    /**
     * 确定给定的描述符是否声明了限定符
     */
    default boolean hasQualifier(DependencyDescriptor descriptor) {
        return false;
    }

    /**
     * 获取建议的 bean 名称
     */
    @Nullable
    default String getSuggestedName(DependencyDescriptor descriptor) {
        return null;
    }

    /**
     * 获取建议的默认值
     */
    @Nullable
    default Object getSuggestedValue(DependencyDescriptor descriptor) {
        return null;
    }

    /**
     * 如果需要，构建延迟解析代理
     */
    @Nullable
    default Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {
        return null;
    }
}
```

---

## 总结

`org.springframework.beans.factory.support` 包是 Spring Bean 容器的核心实现，包含了以下关键功能：

### 核心类职责

| 类名 | 职责 |
|------|------|
| `DefaultListableBeanFactory` | 完整的 BeanFactory 实现，支持 BeanDefinition 注册和 Bean 实例获取 |
| `AbstractBeanFactory` | BeanFactory 抽象基类，实现通用的 Bean 获取逻辑 |
| `AbstractAutowireCapableBeanFactory` | 提供 Bean 的创建、自动装配、属性填充和初始化功能 |
| `DefaultSingletonBeanRegistry` | 单例 Bean 注册表，管理三级缓存解决循环依赖 |
| `AbstractBeanDefinition` | BeanDefinition 抽象基类，定义通用属性和方法 |
| `RootBeanDefinition` | 运行时合并后的 Bean 定义 |
| `GenericBeanDefinition` | 通用 Bean 定义，支持动态设置父定义 |
| `ConstructorResolver` | 解析和选择构造器或工厂方法 |
| `CglibSubclassingInstantiationStrategy` | 使用 CGLIB 支持方法注入的实例化策略 |
| `BeanDefinitionValueResolver` | 解析 Bean 定义中的值 |
| `DisposableBeanAdapter` | 适配器模式执行 Bean 销毁操作 |

### 三级缓存总结

| 缓存级别 | 字段名 | 用途 |
|----------|--------|------|
| 一级缓存 | `singletonObjects` | 存储完全初始化的单例 Bean |
| 二级缓存 | `earlySingletonObjects` | 存储提前暴露的早期 Bean（尚未填充属性） |
| 三级缓存 | `singletonFactories` | 存储生成早期 Bean 引用的 ObjectFactory |

三级缓存的设计使得 Spring 能够在 Bean 初始化完成前暴露其引用，从而解决单例 Bean 的循环依赖问题。

---

*文档生成时间：2026-02-27*
*基于 Spring Framework 6.2.x 源码分析*
