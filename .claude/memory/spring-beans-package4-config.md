# org.springframework.beans.factory.config 包详解

## 目录

1. [概述](#概述)
2. [BeanDefinition 接口体系](#beandefinition-接口体系)
3. [BeanFactory 配置接口](#beanfactory-配置接口)
4. [BeanPostProcessor 扩展点体系](#beanpostprocessor-扩展点体系)
5. [FactoryBean 实现模式](#factorybean-实现模式)
6. [配置器 (Configurers)](#配置器-configurers)
7. [作用域 (Scope) 机制](#作用域-scope-机制)
8. [构造参数与依赖描述](#构造参数与依赖描述)
9. [Bean 引用与运行时值](#bean-引用与运行时值)
10. [总结](#总结)

---

## 概述

`org.springframework.beans.factory.config` 包是 Spring Framework 中 beans 模块的核心配置包，提供了：

- **BeanDefinition 定义与管理**：描述 Bean 的元数据（类名、作用域、依赖等）
- **BeanFactory 配置能力**：扩展 BeanFactory 的配置接口
- **扩展点机制**：BeanPostProcessor、BeanFactoryPostProcessor 等扩展接口
- **FactoryBean 模式**：用于创建复杂对象的工厂模式实现
- **属性配置**：占位符解析、属性覆盖等配置功能
- **作用域机制**：自定义作用域的 SPI

---

## BeanDefinition 接口体系

### BeanDefinition 接口

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/BeanDefinition.java`

`BeanDefinition` 是 Spring 中描述 Bean 实例的核心接口，它定义了 Bean 的所有元数据。

#### 核心源码分析

```java
public interface BeanDefinition extends AttributeAccessor, BeanMetadataElement {

    // ========== 作用域常量 ==========
    String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;  // "singleton"
    String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;  // "prototype"

    // ========== 角色常量 ==========
    int ROLE_APPLICATION = 0;      // 应用程序主要部分（用户定义的 Bean）
    int ROLE_SUPPORT = 1;          // 支持部分（外部组件定义）
    int ROLE_INFRASTRUCTURE = 2;   // 基础设施（完全后台角色）

    // ========== 可修改属性 ==========
    void setParentName(@Nullable String parentName);     // 设置父 Bean 名称
    void setBeanClassName(@Nullable String beanClassName); // 设置 Bean 类名
    void setScope(@Nullable String scope);               // 设置作用域
    void setLazyInit(boolean lazyInit);                  // 设置是否延迟初始化
    void setDependsOn(@Nullable String... dependsOn);    // 设置依赖的 Bean
    void setAutowireCandidate(boolean autowireCandidate); // 设置是否自动装配候选
    void setPrimary(boolean primary);                    // 设置是否为主要候选
    void setFallback(boolean fallback);                  // 设置是否为备用候选（6.2+）
    void setFactoryBeanName(@Nullable String factoryBeanName); // 设置工厂 Bean 名称
    void setFactoryMethodName(@Nullable String factoryMethodName); // 设置工厂方法名
    void setInitMethodName(@Nullable String initMethodName);     // 设置初始化方法
    void setDestroyMethodName(@Nullable String destroyMethodName); // 设置销毁方法
    void setRole(int role);                              // 设置角色
    void setDescription(@Nullable String description);   // 设置描述

    // ========== 获取属性值 ==========
    ConstructorArgumentValues getConstructorArgumentValues(); // 构造参数值
    MutablePropertyValues getPropertyValues();                // 属性值

    // ========== 只读属性 ==========
    ResolvableType getResolvableType();  // 可解析类型
    boolean isSingleton();               // 是否为单例
    boolean isPrototype();               // 是否为原型
    boolean isAbstract();                // 是否为抽象（仅作为父定义）
}
```

#### 属性详解

| 属性类别 | 属性名 | 说明 |
|---------|-------|------|
| **标识** | beanClassName | Bean 的类名，可被后处理器修改 |
| **继承** | parentName | 父 BeanDefinition 名称，用于继承配置 |
| **作用域** | scope | singleton/prototype/自定义作用域 |
| **生命周期** | lazyInit | 是否延迟初始化（仅对单例有效） |
| **依赖** | dependsOn | 显式依赖，确保指定 Bean 先初始化 |
| **自动装配** | autowireCandidate | 是否参与自动装配 |
| **自动装配** | primary | 多个候选时优先选择 |
| **自动装配** | fallback | 6.2+ 新增，作为备用候选 |
| **工厂方法** | factoryBeanName | 工厂 Bean 名称（实例工厂） |
| **工厂方法** | factoryMethodName | 工厂方法名（静态/实例） |
| **回调** | initMethodName | 初始化方法名 |
| **回调** | destroyMethodName | 销毁方法名 |

#### 角色 (Role) 机制

```java
// ROLE_APPLICATION (0) - 用户定义的 Bean
@Bean
public MyService myService() { }

// ROLE_SUPPORT (1) - 组件定义的支持 Bean
// 例如：@ComponentScan 生成的内部 Bean

// ROLE_INFRASTRUCTURE (2) - 基础设施 Bean
// 例如：Spring 内部使用的 BeanPostProcessor
```

#### 使用示例

```java
// 编程式创建 BeanDefinition
RootBeanDefinition beanDef = new RootBeanDefinition(MyService.class);
beanDef.setScope(BeanDefinition.SCOPE_SINGLETON);
beanDef.setLazyInit(false);
beanDef.setPrimary(true);
beanDef.setRole(BeanDefinition.ROLE_APPLICATION);

// 设置构造参数
ConstructorArgumentValues cav = beanDef.getConstructorArgumentValues();
cav.addIndexedArgumentValue(0, "constructorArg1");
cav.addGenericArgumentValue(new RuntimeBeanReference("anotherBean"));

// 设置属性值
MutablePropertyValues pvs = beanDef.getPropertyValues();
pvs.add("propertyName", "propertyValue");
pvs.add("dependency", new RuntimeBeanReference("dependencyBean"));

// 注册到 BeanFactory
DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
factory.registerBeanDefinition("myService", beanDef);
```

### BeanDefinitionHolder

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/BeanDefinitionHolder.java`

`BeanDefinitionHolder` 是 BeanDefinition 的包装器，包含 BeanDefinition、beanName 和 aliases。

```java
public class BeanDefinitionHolder implements BeanMetadataElement {
    private final BeanDefinition beanDefinition;
    private final String beanName;
    @Nullable
    private final String[] aliases;

    // 检查名称是否匹配（包括别名）
    public boolean matchesName(@Nullable String candidateName) {
        return (candidateName != null && (candidateName.equals(this.beanName) ||
                ObjectUtils.containsElement(this.aliases, candidateName)));
    }
}
```

---

## BeanFactory 配置接口

### ConfigurableBeanFactory

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/ConfigurableBeanFactory.java`

`ConfigurableBeanFactory` 是大多数 BeanFactory 需要实现的配置接口，继承自 `HierarchicalBeanFactory` 和 `SingletonBeanRegistry`。

#### 核心功能

```java
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {

    String SCOPE_SINGLETON = "singleton";
    String SCOPE_PROTOTYPE = "prototype";

    // ========== 配置设置 ==========
    void setParentBeanFactory(BeanFactory parentBeanFactory);  // 设置父工厂
    void setBeanClassLoader(@Nullable ClassLoader beanClassLoader);  // 设置类加载器
    void setTempClassLoader(@Nullable ClassLoader tempClassLoader);  // 临时类加载器（用于织入）
    void setCacheBeanMetadata(boolean cacheBeanMetadata);  // 是否缓存元数据
    void setBeanExpressionResolver(@Nullable BeanExpressionResolver resolver);  // SpEL 解析器
    void setConversionService(@Nullable ConversionService conversionService);  // 类型转换服务
    void setBootstrapExecutor(@Nullable Executor executor);  // 后台初始化执行器（6.2+）

    // ========== 属性编辑器 ==========
    void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar);
    void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass);

    // ========== 嵌入值解析 ==========
    void addEmbeddedValueResolver(StringValueResolver valueResolver);
    boolean hasEmbeddedValueResolver();
    String resolveEmbeddedValue(String value);

    // ========== BeanPostProcessor ==========
    void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);
    int getBeanPostProcessorCount();

    // ========== 作用域注册 ==========
    void registerScope(String scopeName, Scope scope);
    String[] getRegisteredScopeNames();
    @Nullable Scope getRegisteredScope(String scopeName);

    // ========== 别名管理 ==========
    void registerAlias(String beanName, String alias);
    void resolveAliases(StringValueResolver valueResolver);

    // ========== 合并 BeanDefinition ==========
    BeanDefinition getMergedBeanDefinition(String beanName);

    // ========== 依赖管理 ==========
    void registerDependentBean(String beanName, String dependentBeanName);
    String[] getDependentBeans(String beanName);
    String[] getDependenciesForBean(String beanName);

    // ========== 销毁 ==========
    void destroyBean(String beanName, Object beanInstance);
    void destroyScopedBean(String beanName);
    void destroySingletons();
}
```

### ConfigurableListableBeanFactory

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/ConfigurableListableBeanFactory.java`

`ConfigurableListableBeanFactory` 是 `ListableBeanFactory`、`AutowireCapableBeanFactory` 和 `ConfigurableBeanFactory` 的合集。

```java
public interface ConfigurableListableBeanFactory
        extends ListableBeanFactory, AutowireCapableBeanFactory, ConfigurableBeanFactory {

    // 忽略指定类型的依赖（不参与自动装配）
    void ignoreDependencyType(Class<?> type);

    // 忽略指定接口的依赖
    void ignoreDependencyInterface(Class<?> ifc);

    // 注册可解析的依赖（如 ApplicationContext）
    void registerResolvableDependency(Class<?> dependencyType, @Nullable Object autowiredValue);

    // 检查是否为自动装配候选
    boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor);

    // 获取 BeanDefinition（不合并父定义）
    BeanDefinition getBeanDefinition(String beanName);

    // 冻结配置（不再修改 BeanDefinition）
    void freezeConfiguration();
    boolean isConfigurationFrozen();

    // 预实例化所有非懒加载单例
    void preInstantiateSingletons() throws BeansException;
}
```

### AutowireCapableBeanFactory

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/AutowireCapableBeanFactory.java`

`AutowireCapableBeanFactory` 提供自动装配能力，主要用于集成第三方框架。

#### 自动装配模式常量

```java
public interface AutowireCapableBeanFactory extends BeanFactory {

    int AUTOWIRE_NO = 0;           // 不自动装配
    int AUTOWIRE_BY_NAME = 1;      // 按名称自动装配
    int AUTOWIRE_BY_TYPE = 2;      // 按类型自动装配
    int AUTOWIRE_CONSTRUCTOR = 3;  // 按构造函数自动装配
    @Deprecated
    int AUTOWIRE_AUTODETECT = 4;   // 自动检测（已弃用）

    String ORIGINAL_INSTANCE_SUFFIX = ".ORIGINAL";  // 强制返回原始实例（不使用代理）
}
```

#### 核心方法

```java
// 创建并完全初始化 Bean
<T> T createBean(Class<T> beanClass) throws BeansException;

// 自动装配现有 Bean 实例
void autowireBean(Object existingBean) throws BeansException;

// 配置现有 Bean（应用 BeanDefinition 中的配置）
Object configureBean(Object existingBean, String beanName) throws BeansException;

// 初始化 Bean（应用工厂回调和后处理器）
Object initializeBean(Object existingBean, String beanName) throws BeansException;

// 销毁 Bean
void destroyBean(Object existingBean);

// 解析依赖
Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
        @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException;
```

#### 使用示例

```java
// 获取 AutowireCapableBeanFactory
AutowireCapableBeanFactory autowireFactory = applicationContext.getAutowireCapableBeanFactory();

// 创建并自动装配新实例
MyService service = autowireFactory.createBean(MyService.class);

// 对现有对象进行自动装配
MyLegacyObject legacy = new MyLegacyObject();
autowireFactory.autowireBean(legacy);

// 初始化外部对象
ExternalObject external = new ExternalObject();
autowireFactory.initializeBean(external, "externalBean");
```

### SingletonBeanRegistry

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/SingletonBeanRegistry.java`

```java
public interface SingletonBeanRegistry {

    // 注册单例实例（已完全初始化）
    void registerSingleton(String beanName, Object singletonObject);

    // 添加单例回调（6.2+）
    void addSingletonCallback(String beanName, Consumer<Object> singletonConsumer);

    // 获取单例（仅已实例化的）
    @Nullable Object getSingleton(String beanName);

    // 检查是否包含单例
    boolean containsSingleton(String beanName);

    // 获取所有单例名称
    String[] getSingletonNames();

    // 获取单例数量
    int getSingletonCount();
}
```

---

## BeanPostProcessor 扩展点体系

### BeanPostProcessor（基础接口）

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/BeanPostProcessor.java`

`BeanPostProcessor` 是 Spring 提供的核心扩展接口，允许在 Bean 初始化前后进行自定义处理。

#### 核心源码分析

```java
public interface BeanPostProcessor {

    /**
     * 在 Bean 初始化回调之前调用（如 afterPropertiesSet 或 init-method）
     * 此时 Bean 的属性已经填充完成
     * 返回的 Bean 实例可能是原始实例的包装器
     */
    @Nullable
    default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * 在 Bean 初始化回调之后调用
     * 这是创建代理对象的理想位置（如 AOP 代理）
     * 对于 FactoryBean，此方法会被调用两次：
     * 1. FactoryBean 实例本身
     * 2. FactoryBean 创建的对象
     */
    @Nullable
    default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
```

#### 执行时机

```
Bean 生命周期中的执行顺序：

1. 实例化（Instantiation）
   ↓
2. 属性填充（Population）
   ↓
3. postProcessBeforeInitialization()  ← 第一个扩展点
   ↓
4. 初始化回调（InitializingBean.afterPropertiesSet() / @PostConstruct / init-method）
   ↓
5. postProcessAfterInitialization()   ← 第二个扩展点（AOP 代理在此创建）
   ↓
6. Bean 就绪
```

#### 使用示例

```java
@Component
public class MyBeanPostProcessor implements BeanPostProcessor, Ordered {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof MyService) {
            System.out.println("Before initialization: " + beanName);
            // 可以修改 Bean 的属性或返回包装器
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof MyService) {
            System.out.println("After initialization: " + beanName);
            // 通常用于创建代理
        }
        return bean;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;  // 指定执行顺序
    }
}
```

### InstantiationAwareBeanPostProcessor

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/InstantiationAwareBeanPostProcessor.java`

`InstantiationAwareBeanPostProcessor` 扩展了 `BeanPostProcessor`，添加了实例化前后的回调。

```java
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

    /**
     * 在目标 Bean 实例化之前调用
     * 返回非空对象会短路默认实例化过程（跳过默认构造函数）
     * 用于创建代理对象替代目标 Bean
     */
    @Nullable
    default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        return null;
    }

    /**
     * 在 Bean 实例化之后、属性填充之前调用
     * 返回 false 会跳过属性填充
     */
    default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        return true;
    }

    /**
     * 在工厂应用属性值之前对属性值进行后处理
     * 用于自定义属性注入（如 @Autowired 处理）
     */
    @Nullable
    default PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
            throws BeansException {
        return pvs;
    }
}
```

#### 执行时机详解

```
完整生命周期中的 InstantiationAwareBeanPostProcessor：

1. postProcessBeforeInstantiation()  ← 实例化前（可返回代理短路默认实例化）
   ↓
2. 实例化（构造函数）
   ↓
3. postProcessAfterInstantiation()   ← 实例化后，返回 false 跳过属性填充
   ↓
4. postProcessProperties()           ← 属性填充前，可修改属性值
   ↓
5. 属性填充
   ↓
6. postProcessBeforeInitialization() ← BeanPostProcessor 方法
   ↓
7. 初始化回调
   ↓
8. postProcessAfterInitialization()  ← BeanPostProcessor 方法
```

### SmartInstantiationAwareBeanPostProcessor

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/SmartInstantiationAwareBeanPostProcessor.java`

```java
public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {

    /**
     * 预测 Bean 的最终类型
     * 用于在实例化前确定 Bean 的类型（如代理类型）
     */
    @Nullable
    default Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {
        return null;
    }

    /**
     * 确定 Bean 类型（6.0+）
     * 完全评估处理步骤以创建/初始化代理类
     */
    default Class<?> determineBeanType(Class<?> beanClass, String beanName) throws BeansException {
        return beanClass;
    }

    /**
     * 确定候选构造函数
     * 用于 @Autowired 构造函数选择
     */
    @Nullable
    default Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName)
            throws BeansException {
        return null;
    }

    /**
     * 获取早期 Bean 引用
     * 用于解决循环引用，在 Bean 完全初始化前暴露包装器
     */
    default Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
```

### DestructionAwareBeanPostProcessor

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/DestructionAwareBeanPostProcessor.java`

```java
public interface DestructionAwareBeanPostProcessor extends BeanPostProcessor {

    /**
     * 在 Bean 销毁之前调用
     * 用于调用自定义销毁回调
     */
    void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException;

    /**
     * 确定 Bean 是否需要由此后处理器销毁
     */
    default boolean requiresDestruction(Object bean) {
        return true;
    }
}
```

### MergedBeanDefinitionPostProcessor

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/support/MergedBeanDefinitionPostProcessor.java`

```java
public interface MergedBeanDefinitionPostProcessor extends BeanPostProcessor {

    /**
     * 后处理合并后的 BeanDefinition
     * 在 Bean 实例化后、属性填充前调用
     * 用于准备缓存的元数据（如 @Autowired 注入点）
     */
    void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName);

    /**
     * 通知 BeanDefinition 已被重置
     * 用于清除受影响 Bean 的元数据
     */
    default void resetBeanDefinition(String beanName) {
    }
}
```

### BeanFactoryPostProcessor

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/BeanFactoryPostProcessor.java`

`BeanFactoryPostProcessor` 在 BeanDefinition 加载完成后、Bean 实例化之前执行，用于修改 BeanDefinition。

```java
@FunctionalInterface
public interface BeanFactoryPostProcessor {

    /**
     * 修改应用上下文的内部 BeanFactory
     * 所有 BeanDefinition 已加载，但尚未实例化任何 Bean
     */
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;
}
```

#### 与 BeanPostProcessor 的区别

| 特性 | BeanFactoryPostProcessor | BeanPostProcessor |
|------|-------------------------|-------------------|
| **处理对象** | BeanDefinition（元数据） | Bean 实例 |
| **执行时机** | BeanDefinition 加载后，实例化前 | Bean 实例化后，初始化前后 |
| **典型用途** | 修改属性值、添加/移除 BeanDefinition | 创建代理、属性检查、初始化逻辑 |
| **示例实现** | PropertyPlaceholderConfigurer | AutowiredAnnotationBeanPostProcessor |

### BeanDefinitionRegistryPostProcessor

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/support/BeanDefinitionRegistryPostProcessor.java`

```java
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {

    /**
     * 修改应用上下文的内部 BeanDefinitionRegistry
     * 标准初始化后执行，在所有常规 BeanFactoryPostProcessor 检测之前
     * 允许添加更多的 BeanDefinition，这些定义又可以定义 BeanFactoryPostProcessor
     */
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException;

    @Override
    default void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }
}
```

#### 执行顺序

```
1. BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry()
   ↓
2. BeanFactoryPostProcessor.postProcessBeanFactory()
   ↓
3. Bean 实例化
   ↓
4. BeanPostProcessor.postProcessBeforeInitialization()
   ↓
5. BeanPostProcessor.postProcessAfterInitialization()
```

---

## FactoryBean 实现模式

### AbstractFactoryBean

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/AbstractFactoryBean.java`

`AbstractFactoryBean` 是 `FactoryBean` 的简单模板超类，支持单例和原型两种模式。

#### 核心源码分析

```java
public abstract class AbstractFactoryBean<T>
        implements FactoryBean<T>, BeanClassLoaderAware, BeanFactoryAware, InitializingBean, DisposableBean {

    private boolean singleton = true;           // 默认单例
    private boolean initialized = false;        // 初始化标志

    @Nullable
    private T singletonInstance;                // 单例实例

    @Nullable
    private T earlySingletonInstance;           // 早期单例实例（用于循环引用）

    /**
     * 设置是否为单例模式
     */
    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    /**
     * 初始化回调（InitializingBean）
     * 单例模式下在此创建实例
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (isSingleton()) {
            this.initialized = true;
            this.singletonInstance = createInstance();  // 模板方法
            this.earlySingletonInstance = null;
        }
    }

    /**
     * 获取对象（FactoryBean 核心方法）
     */
    @Override
    public final T getObject() throws Exception {
        if (isSingleton()) {
            // 单例：返回已创建的实例或早期代理
            return (this.initialized ? this.singletonInstance : getEarlySingletonInstance());
        }
        else {
            // 原型：每次创建新实例
            return createInstance();
        }
    }

    /**
     * 子类必须实现的模板方法：实际创建对象
     */
    protected abstract T createInstance() throws Exception;

    /**
     * 子类可覆盖：销毁单例实例
     */
    protected void destroyInstance(@Nullable T instance) throws Exception {
    }
}
```

#### 使用示例

```java
@Component
public class MyFactoryBean extends AbstractFactoryBean<MyService> {

    @Autowired
    private Dependency dependency;

    @Override
    public Class<?> getObjectType() {
        return MyService.class;
    }

    @Override
    protected MyService createInstance() throws Exception {
        // 复杂的对象创建逻辑
        MyService service = new MyService();
        service.setDependency(dependency);
        service.initialize();
        return service;
    }

    @Override
    protected void destroyInstance(MyService instance) throws Exception {
        // 清理资源
        instance.cleanup();
    }
}
```

### MethodInvokingFactoryBean

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/MethodInvokingFactoryBean.java`

`MethodInvokingFactoryBean` 通过调用静态或实例方法返回值作为 Bean。

```java
public class MethodInvokingFactoryBean extends MethodInvokingBean implements FactoryBean<Object> {

    private boolean singleton = true;
    private boolean initialized = false;
    @Nullable
    private Object singletonObject;

    /**
     * 设置是否为单例
     */
    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        prepare();
        if (this.singleton) {
            this.initialized = true;
            this.singletonObject = invokeWithTargetException();  // 调用目标方法
        }
    }

    @Override
    @Nullable
    public Object getObject() throws Exception {
        if (this.singleton) {
            if (!this.initialized) {
                throw new FactoryBeanNotInitializedException();
            }
            return this.singletonObject;
        }
        else {
            return invokeWithTargetException();
        }
    }

    @Override
    @Nullable
    public Class<?> getObjectType() {
        if (!isPrepared()) {
            return null;
        }
        return getPreparedMethod().getReturnType();
    }
}
```

#### XML 配置示例

```xml
<!-- 调用静态工厂方法 -->
<bean id="myObject" class="org.springframework.beans.factory.config.MethodInvokingFactoryBean">
    <property name="staticMethod" value="com.example.MyFactory.getInstance"/>
    <property name="arguments">
        <list>
            <value>arg1</value>
            <value>arg2</value>
        </list>
    </property>
</bean>

<!-- 调用实例方法 -->
<bean id="sysProps" class="org.springframework.beans.factory.config.MethodInvokingFactoryBean">
    <property name="targetClass" value="java.lang.System"/>
    <property name="targetMethod" value="getProperties"/>
</bean>

<bean id="javaVersion" class="org.springframework.beans.factory.config.MethodInvokingFactoryBean">
    <property name="targetObject" ref="sysProps"/>
    <property name="targetMethod" value="getProperty"/>
    <property name="arguments" value="java.version"/>
</bean>
```

### FieldRetrievingFactoryBean

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/FieldRetrievingFactoryBean.java`

`FieldRetrievingFactoryBean` 用于获取静态或非静态字段的值，常用于获取常量。

```java
public class FieldRetrievingFactoryBean
        implements FactoryBean<Object>, BeanNameAware, BeanClassLoaderAware, InitializingBean {

    @Nullable
    private Class<?> targetClass;       // 目标类（静态字段）

    @Nullable
    private Object targetObject;        // 目标对象（实例字段）

    @Nullable
    private String targetField;         // 字段名

    @Nullable
    private String staticField;         // 完整静态字段名（类名.字段名）

    @Nullable
    private Field fieldObject;          // 反射获取的 Field 对象

    @Override
    public void afterPropertiesSet() throws ClassNotFoundException, NoSuchFieldException {
        // 解析目标类和字段
        if (this.staticField != null) {
            int lastDotIndex = this.staticField.lastIndexOf('.');
            String className = this.staticField.substring(0, lastDotIndex);
            String fieldName = this.staticField.substring(lastDotIndex + 1);
            this.targetClass = ClassUtils.forName(className, this.beanClassLoader);
            this.targetField = fieldName;
        }

        Class<?> targetClass = (this.targetObject != null ? this.targetObject.getClass() : this.targetClass);
        this.fieldObject = targetClass.getField(this.targetField);
    }

    @Override
    @Nullable
    public Object getObject() throws IllegalAccessException {
        ReflectionUtils.makeAccessible(this.fieldObject);
        if (this.targetObject != null) {
            return this.fieldObject.get(this.targetObject);  // 实例字段
        }
        else {
            return this.fieldObject.get(null);  // 静态字段
        }
    }
}
```

#### 使用示例

```xml
<!-- 获取 JDBC 事务隔离级别常量 -->
<bean id="transactionSerializable"
      class="org.springframework.beans.factory.config.FieldRetrievingFactoryBean">
    <property name="staticField" value="java.sql.Connection.TRANSACTION_SERIALIZABLE"/>
</bean>

<!-- 简洁写法：使用 bean 名称作为 staticField -->
<bean id="java.sql.Connection.TRANSACTION_SERIALIZABLE"
      class="org.springframework.beans.factory.config.FieldRetrievingFactoryBean"/>
```

### PropertiesFactoryBean

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/PropertiesFactoryBean.java`

```java
public class PropertiesFactoryBean extends PropertiesLoaderSupport
        implements FactoryBean<Properties>, InitializingBean {

    private boolean singleton = true;
    @Nullable
    private Properties singletonInstance;

    @Override
    public final void afterPropertiesSet() throws IOException {
        if (this.singleton) {
            this.singletonInstance = createProperties();
        }
    }

    @Override
    @Nullable
    public final Properties getObject() throws IOException {
        if (this.singleton) {
            return this.singletonInstance;
        }
        else {
            return createProperties();
        }
    }

    @Override
    public Class<Properties> getObjectType() {
        return Properties.class;
    }

    protected Properties createProperties() throws IOException {
        return mergeProperties();  // 合并本地属性和加载的属性
    }
}
```

#### 使用示例

```xml
<bean id="jdbcProperties" class="org.springframework.beans.factory.config.PropertiesFactoryBean">
    <property name="location" value="classpath:jdbc.properties"/>
</bean>

<bean id="dataSource" class="com.example.DataSource">
    <property name="properties" ref="jdbcProperties"/>
</bean>
```

### YamlPropertiesFactoryBean

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/YamlPropertiesFactoryBean.java`

```java
public class YamlPropertiesFactoryBean extends YamlProcessor implements FactoryBean<Properties>, InitializingBean {

    private boolean singleton = true;
    @Nullable
    private Properties properties;

    @Override
    public void afterPropertiesSet() {
        if (isSingleton()) {
            this.properties = createProperties();
        }
    }

    @Override
    @Nullable
    public Properties getObject() {
        return (this.properties != null ? this.properties : createProperties());
    }

    protected Properties createProperties() {
        Properties result = CollectionFactory.createStringAdaptingProperties();
        process((properties, map) -> result.putAll(properties));
        return result;
    }
}
```

#### YAML 转换示例

```yaml
# application.yml
environments:
  dev:
    url: https://dev.bar.com
    name: Developer Setup
  prod:
    url: https://foo.bar.com
    name: My Cool App

servers:
  - dev.bar.com
  - foo.bar.com
```

转换后的 Properties：

```properties
environments.dev.url=https://dev.bar.com
environments.dev.name=Developer Setup
environments.prod.url=https://foo.bar.com
environments.prod.name=My Cool App
servers[0]=dev.bar.com
servers[1]=foo.bar.com
```

---

## 配置器 (Configurers)

### PropertyPlaceholderConfigurer

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/PropertyPlaceholderConfigurer.java`

`PropertyPlaceholderConfigurer` 用于解析 `${...}` 占位符，从属性文件、系统属性或环境变量中获取值。

#### 系统属性模式

```java
@Deprecated  // 5.2 起弃用，推荐使用 PropertySourcesPlaceholderConfigurer
public class PropertyPlaceholderConfigurer extends PlaceholderConfigurerSupport {

    /** 从不检查系统属性 */
    public static final int SYSTEM_PROPERTIES_MODE_NEVER = 0;

    /** 在指定属性不可解析时回退到系统属性（默认） */
    public static final int SYSTEM_PROPERTIES_MODE_FALLBACK = 1;

    /** 首先检查系统属性，覆盖指定属性 */
    public static final int SYSTEM_PROPERTIES_MODE_OVERRIDE = 2;

    private int systemPropertiesMode = SYSTEM_PROPERTIES_MODE_FALLBACK;
    private boolean searchSystemEnvironment = true;  // 是否搜索系统环境变量
}
```

#### 占位符解析流程

```java
@Nullable
protected String resolvePlaceholder(String placeholder, Properties props, int systemPropertiesMode) {
    String propVal = null;

    // OVERRIDE 模式：先检查系统属性
    if (systemPropertiesMode == SYSTEM_PROPERTIES_MODE_OVERRIDE) {
        propVal = resolveSystemProperty(placeholder);
    }

    // 检查配置的 properties
    if (propVal == null) {
        propVal = resolvePlaceholder(placeholder, props);
    }

    // FALLBACK 模式：properties 中找不到时检查系统属性
    if (propVal == null && systemPropertiesMode == SYSTEM_PROPERTIES_MODE_FALLBACK) {
        propVal = resolveSystemProperty(placeholder);
    }

    return propVal;
}

@Nullable
protected String resolveSystemProperty(String key) {
    try {
        // 先检查 JVM 系统属性
        String value = System.getProperty(key);

        // 再检查系统环境变量
        if (value == null && this.searchSystemEnvironment) {
            value = System.getenv(key);
        }
        return value;
    }
    catch (Throwable ex) {
        return null;
    }
}
```

#### 使用示例

```xml
<!-- 基本配置 -->
<bean class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer">
    <property name="location" value="classpath:application.properties"/>
</bean>

<!-- 多属性文件 + 系统属性覆盖 -->
<bean class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer">
    <property name="locations">
        <list>
            <value>classpath:default.properties</value>
            <value>classpath:override.properties</value>
        </list>
    </property>
    <property name="systemPropertiesModeName" value="SYSTEM_PROPERTIES_MODE_OVERRIDE"/>
    <property name="ignoreUnresolvablePlaceholders" value="true"/>
    <property name="valueSeparator" value=":"/>  <!-- 默认值分隔符 ${key:default} -->
</bean>

<!-- 使用占位符 -->
<bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
    <property name="url" value="${jdbc.url:jdbc:mysql://localhost:3306/default}"/>
    <property name="username" value="${jdbc.username}"/>
    <property name="password" value="${jdbc.password}"/>
</bean>
```

#### 占位符语法

| 语法 | 说明 | 示例 |
|------|------|------|
| `${key}` | 基本占位符 | `${jdbc.url}` |
| `${key:default}` | 带默认值 | `${jdbc.url:jdbc:h2:mem:test}` |
| `\${key}` | 转义（不解析） | `\${literal}` |

### PlaceholderConfigurerSupport

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/PlaceholderConfigurerSupport.java`

`PlaceholderConfigurerSupport` 是属性占位符配置器的抽象基类。

```java
public abstract class PlaceholderConfigurerSupport extends PropertyResourceConfigurer
        implements BeanNameAware, BeanFactoryAware {

    /** 默认占位符前缀: "${" */
    public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";

    /** 默认占位符后缀: "}" */
    public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";

    /** 默认值分隔符: ":" */
    public static final String DEFAULT_VALUE_SEPARATOR = ":";

    /** 默认转义字符: '\' */
    public static final Character DEFAULT_ESCAPE_CHARACTER = '\\';

    protected String placeholderPrefix = DEFAULT_PLACEHOLDER_PREFIX;
    protected String placeholderSuffix = DEFAULT_PLACEHOLDER_SUFFIX;
    @Nullable
    protected String valueSeparator = DEFAULT_VALUE_SEPARATOR;

    protected boolean trimValues = false;           // 是否修剪值
    protected boolean ignoreUnresolvablePlaceholders = false;  // 是否忽略不可解析的占位符
    @Nullable
    protected String nullValue;                     // 视为 null 的值
}
```

### CustomScopeConfigurer

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/CustomScopeConfigurer.java`

`CustomScopeConfigurer` 用于声明式注册自定义作用域。

```java
public class CustomScopeConfigurer implements BeanFactoryPostProcessor, BeanClassLoaderAware, Ordered {

    @Nullable
    private Map<String, Object> scopes;  // scopeName -> Scope 实例/类/类名

    private int order = Ordered.LOWEST_PRECEDENCE;

    /**
     * 添加作用域
     */
    public void addScope(String scopeName, Scope scope) {
        if (this.scopes == null) {
            this.scopes = new LinkedHashMap<>(1);
        }
        this.scopes.put(scopeName, scope);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (this.scopes != null) {
            this.scopes.forEach((scopeKey, value) -> {
                if (value instanceof Scope scope) {
                    beanFactory.registerScope(scopeKey, scope);
                }
                else if (value instanceof Class<?> scopeClass) {
                    beanFactory.registerScope(scopeKey, (Scope) BeanUtils.instantiateClass(scopeClass));
                }
                else if (value instanceof String scopeClassName) {
                    Class<?> scopeClass = ClassUtils.resolveClassName(scopeClassName, this.beanClassLoader);
                    beanFactory.registerScope(scopeKey, (Scope) BeanUtils.instantiateClass(scopeClass));
                }
            });
        }
    }
}
```

#### 使用示例

```xml
<bean class="org.springframework.beans.factory.config.CustomScopeConfigurer">
    <property name="scopes">
        <map>
            <entry key="thread">
                <bean class="org.springframework.context.support.SimpleThreadScope"/>
            </entry>
            <entry key="custom">
                <ref bean="myCustomScope"/>
            </entry>
        </map>
    </property>
</bean>

<!-- 使用自定义作用域 -->
<bean id="scopedBean" class="com.example.MyBean" scope="thread">
    <aop:scoped-proxy/>
</bean>
```

---

## 作用域 (Scope) 机制

### Scope 接口

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/Scope.java`

`Scope` 是策略接口，用于定义 Bean 实例的作用域存储策略。

```java
public interface Scope {

    /**
     * 从底层作用域获取对象，如果不存在则使用 ObjectFactory 创建
     * 这是 Scope 的核心操作
     */
    Object get(String name, ObjectFactory<?> objectFactory);

    /**
     * 从底层作用域移除对象
     * 可选操作
     */
    @Nullable
    Object remove(String name);

    /**
     * 注册销毁回调
     * 当作用域内的对象被销毁时执行
     */
    void registerDestructionCallback(String name, Runnable callback);

    /**
     * 解析上下文对象
     * 例如：request 作用域返回 HttpServletRequest
     */
    @Nullable
    Object resolveContextualObject(String key);

    /**
     * 获取会话 ID
     * 例如：session 作用域返回 session ID
     */
    @Nullable
    String getConversationId();
}
```

#### 作用域实现类图

```
Scope (接口)
    ├── SimpleThreadScope        # 线程作用域（同一线程共享实例）
    ├── RequestScope             # HTTP 请求作用域
    ├── SessionScope             # HTTP 会话作用域
    ├── ApplicationScope         # ServletContext 作用域
    └── 自定义 Scope 实现
```

#### 自定义 Scope 实现示例

```java
/**
 * 自定义对话作用域（Conversation Scope）
 * 用于在多个请求间保持状态（如向导式表单）
 */
public class ConversationScope implements Scope {

    private final ThreadLocal<Map<String, Object>> conversationHolder = new ThreadLocal<>();
    private final ThreadLocal<Map<String, Runnable>> destructionCallbacks = new ThreadLocal<>();

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        Map<String, Object> conversation = conversationHolder.get();
        if (conversation == null) {
            conversation = new ConcurrentHashMap<>();
            conversationHolder.set(conversation);
        }

        Object scopedObject = conversation.get(name);
        if (scopedObject == null) {
            scopedObject = objectFactory.getObject();
            conversation.put(name, scopedObject);
        }
        return scopedObject;
    }

    @Override
    @Nullable
    public Object remove(String name) {
        Map<String, Object> conversation = conversationHolder.get();
        if (conversation != null) {
            Object removed = conversation.remove(name);
            if (removed != null) {
                Map<String, Runnable> callbacks = destructionCallbacks.get();
                if (callbacks != null) {
                    callbacks.remove(name);
                }
            }
            return removed;
        }
        return null;
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        Map<String, Runnable> callbacks = destructionCallbacks.get();
        if (callbacks == null) {
            callbacks = new ConcurrentHashMap<>();
            destructionCallbacks.set(callbacks);
        }
        callbacks.put(name, callback);
    }

    @Override
    @Nullable
    public Object resolveContextualObject(String key) {
        return null;
    }

    @Override
    @Nullable
    public String getConversationId() {
        Map<String, Object> conversation = conversationHolder.get();
        return conversation != null ? Integer.toHexString(System.identityHashCode(conversation)) : null;
    }

    /**
     * 开始新对话
     */
    public void beginConversation() {
        endConversation();  // 清理旧对话
        conversationHolder.set(new ConcurrentHashMap<>());
        destructionCallbacks.set(new ConcurrentHashMap<>());
    }

    /**
     * 结束对话（执行销毁回调）
     */
    public void endConversation() {
        Map<String, Runnable> callbacks = destructionCallbacks.get();
        if (callbacks != null) {
            callbacks.values().forEach(Runnable::run);
        }
        destructionCallbacks.remove();
        conversationHolder.remove();
    }
}
```

#### 注册和使用自定义 Scope

```java
@Configuration
public class ScopeConfig {

    @Bean
    public static CustomScopeConfigurer customScopeConfigurer() {
        CustomScopeConfigurer configurer = new CustomScopeConfigurer();
        configurer.addScope("conversation", new ConversationScope());
        return configurer;
    }
}

// 使用自定义作用域
@Component
@Scope(value = "conversation", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class WizardData {
    // 向导数据，跨多个请求保持
}
```

---

## 构造参数与依赖描述

### ConstructorArgumentValues

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/ConstructorArgumentValues.java`

`ConstructorArgumentValues` 持有构造参数值，支持按索引和按类型两种匹配方式。

```java
public class ConstructorArgumentValues {

    // 按索引的构造参数值
    private final Map<Integer, ValueHolder> indexedArgumentValues = new LinkedHashMap<>();

    // 通用的构造参数值（按类型匹配）
    private final List<ValueHolder> genericArgumentValues = new ArrayList<>();

    /**
     * 添加按索引的构造参数值
     */
    public void addIndexedArgumentValue(int index, @Nullable Object value) {
        addIndexedArgumentValue(index, new ValueHolder(value));
    }

    public void addIndexedArgumentValue(int index, @Nullable Object value, String type) {
        addIndexedArgumentValue(index, new ValueHolder(value, type));
    }

    /**
     * 添加通用的构造参数值（按类型匹配）
     */
    public void addGenericArgumentValue(@Nullable Object value) {
        this.genericArgumentValues.add(new ValueHolder(value));
    }

    /**
     * 获取构造参数值
     */
    @Nullable
    public ValueHolder getArgumentValue(int index, @Nullable Class<?> requiredType,
            @Nullable String requiredName, @Nullable Set<ValueHolder> usedValueHolders) {

        // 先尝试按索引获取
        ValueHolder valueHolder = getIndexedArgumentValue(index, requiredType, requiredName);

        // 再尝试按类型获取
        if (valueHolder == null) {
            valueHolder = getGenericArgumentValue(requiredType, requiredName, usedValueHolders);
        }
        return valueHolder;
    }

    /**
     * 值持有者内部类
     */
    public static class ValueHolder implements BeanMetadataElement {
        @Nullable
        private Object value;       // 值（可能是 TypedStringValue、RuntimeBeanReference 等）
        @Nullable
        private String type;        // 目标类型
        @Nullable
        private String name;        // 参数名（Java 8+ 编译带参数名时可用）
        @Nullable
        private Object source;      // 配置源

        private boolean converted = false;      // 是否已转换
        @Nullable
        private Object convertedValue;          // 转换后的值
    }
}
```

#### 使用示例

```java
RootBeanDefinition beanDef = new RootBeanDefinition(MyService.class);
ConstructorArgumentValues cav = beanDef.getConstructorArgumentValues();

// 按索引设置
 cav.addIndexedArgumentValue(0, "stringArg");
cav.addIndexedArgumentValue(1, new RuntimeBeanReference("anotherBean"));
cav.addIndexedArgumentValue(2, 123, "int");  // 带类型

// 按类型设置（不指定索引）
cav.addGenericArgumentValue("genericString");
cav.addGenericArgumentValue(new TypedStringValue("123", Integer.class));

// XML 等效配置
/*
<bean id="myService" class="com.example.MyService">
    <constructor-arg index="0" value="stringArg"/>
    <constructor-arg index="1" ref="anotherBean"/>
    <constructor-arg index="2" value="123" type="int"/>
    <constructor-arg value="genericString"/>
</bean>
*/
```

### DependencyDescriptor

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/DependencyDescriptor.java`

`DependencyDescriptor` 描述即将注入的特定依赖，包装构造函数参数、方法参数或字段。

```java
@SuppressWarnings("serial")
public class DependencyDescriptor extends InjectionPoint implements Serializable {

    private final Class<?> declaringClass;      // 声明类
    @Nullable
    private String methodName;                  // 方法名（方法参数时）
    @Nullable
    private Class<?>[] parameterTypes;          // 参数类型
    private int parameterIndex;                 // 参数索引
    @Nullable
    private String fieldName;                   // 字段名（字段注入时）

    private final boolean required;             // 是否必需
    private final boolean eager;                // 是否急切解析

    private int nestingLevel = 1;               // 嵌套级别（用于集合元素）
    @Nullable
    private Class<?> containingClass;           // 包含类（可能是子类）

    /**
     * 检查依赖是否必需
     * 考虑 Optional、@Nullable、Kotlin 可空性
     */
    public boolean isRequired() {
        if (!this.required) {
            return false;
        }

        if (this.field != null) {
            return !(this.field.getType() == Optional.class || hasNullableAnnotation() ||
                    (KotlinDetector.isKotlinReflectPresent() &&
                            KotlinDetector.isKotlinType(this.field.getDeclaringClass()) &&
                            KotlinDelegate.isNullable(this.field)));
        }
        else {
            return !obtainMethodParameter().isOptional();
        }
    }

    /**
     * 获取可解析类型
     */
    public ResolvableType getResolvableType() {
        ResolvableType resolvableType = this.resolvableType;
        if (resolvableType == null) {
            resolvableType = (this.field != null ?
                    ResolvableType.forField(this.field, this.nestingLevel, this.containingClass) :
                    ResolvableType.forMethodParameter(obtainMethodParameter()));
            this.resolvableType = resolvableType;
        }
        return resolvableType;
    }

    /**
     * 解析候选 Bean
     */
    public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory)
            throws BeansException {
        return beanFactory.getBean(beanName);
    }

    /**
     * 获取依赖名称
     */
    @Nullable
    public String getDependencyName() {
        return (this.field != null ? this.field.getName() : obtainMethodParameter().getParameterName());
    }

    /**
     * 获取依赖类型
     */
    public Class<?> getDependencyType() {
        if (this.field != null) {
            if (this.nestingLevel > 1) {
                Class<?> clazz = getResolvableType().getRawClass();
                return (clazz != null ? clazz : Object.class);
            }
            else {
                return this.field.getType();
            }
        }
        else {
            return obtainMethodParameter().getNestedParameterType();
        }
    }
}
```

#### 使用场景

```java
// 在 AutowireCandidateResolver 中使用
public class ContextAnnotationAutowireCandidateResolver extends QualifierAnnotationAutowireCandidateResolver {

    @Override
    @Nullable
    public Object getSuggestedValue(DependencyDescriptor descriptor) {
        // 解析 @Value 注解
        Object value = findValue(descriptor.getAnnotations());
        if (value == null) {
            MethodParameter methodParam = descriptor.getMethodParameter();
            if (methodParam != null) {
                value = findValue(methodParam.getMethodAnnotations());
            }
        }
        return value;
    }

    @Override
    protected boolean checkQualifier(BeanDefinitionHolder bdHolder, Annotation annotation,
            TypeConverter typeConverter, DependencyDescriptor descriptor) {
        // 检查 @Qualifier 注解
        // ...
    }
}
```

---

## Bean 引用与运行时值

### BeanReference 接口

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/BeanReference.java`

```java
public interface BeanReference extends BeanMetadataElement {

    /**
     * 返回此引用指向的目标 Bean 名称
     */
    String getBeanName();
}
```

### RuntimeBeanReference

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/RuntimeBeanReference.java`

`RuntimeBeanReference` 是不可变的占位符类，表示对另一个 Bean 的引用，在运行时解析。

```java
public class RuntimeBeanReference implements BeanReference {

    private final String beanName;          // Bean 名称
    @Nullable
    private final Class<?> beanType;        // Bean 类型（按类型解析时使用）
    private final boolean toParent;         // 是否引用父工厂中的 Bean
    @Nullable
    private Object source;                  // 配置源

    /**
     * 按名称创建引用
     */
    public RuntimeBeanReference(String beanName) {
        this(beanName, false);
    }

    /**
     * 按类型创建引用（5.2+）
     */
    public RuntimeBeanReference(Class<?> beanType) {
        this(beanType, false);
    }

    /**
     * 创建引用父工厂的引用
     */
    public RuntimeBeanReference(String beanName, boolean toParent) {
        Assert.hasText(beanName, "'beanName' must not be empty");
        this.beanName = beanName;
        this.beanType = null;
        this.toParent = toParent;
    }

    @Override
    public String getBeanName() {
        return this.beanName;
    }

    public boolean isToParent() {
        return this.toParent;
    }
}
```

#### 使用示例

```java
// 编程式设置 Bean 引用
RootBeanDefinition beanDef = new RootBeanDefinition(MyService.class);
MutablePropertyValues pvs = beanDef.getPropertyValues();

// 引用另一个 Bean
pvs.add("dependency", new RuntimeBeanReference("dependencyBean"));

// 引用父工厂的 Bean
pvs.add("parentService", new RuntimeBeanReference("parentService", true));

// XML 等效配置
/*
<bean id="myService" class="com.example.MyService">
    <property name="dependency" ref="dependencyBean"/>
    <property name="parentService" ref="parentService"/>
</bean>
*/
```

### TypedStringValue

**文件位置**：`spring-beans/src/main/java/org/springframework/beans/factory/config/TypedStringValue.java`

`TypedStringValue` 包装字符串值，并指定目标类型，用于类型转换。

```java
public class TypedStringValue implements BeanMetadataElement {

    @Nullable
    private String value;           // 字符串值
    @Nullable
    private Object targetType;      // 目标类型（Class 或 String 类名）
    @Nullable
    private Object source;          // 配置源

    private boolean dynamic;        // 是否动态（包含占位符）
    @Nullable
    private Object resolvedValue;   // 解析后的值
    @Nullable
    private Object resolvedTargetType;  // 解析后的目标类型

    public TypedStringValue(@Nullable String value) {
        this.value = value;
    }

    public TypedStringValue(@Nullable String value, Class<?> targetType) {
        this.value = value;
        this.targetType = targetType;
    }

    public TypedStringValue(@Nullable String value, String targetTypeName) {
        this.value = value;
        this.targetType = targetTypeName;
    }
}
```

#### 使用示例

```java
// 编程式设置带类型的属性值
MutablePropertyValues pvs = new MutablePropertyValues();

// 字符串值会被转换为 Integer
 pvs.add("port", new TypedStringValue("8080", Integer.class));

// 字符串值会被转换为 Class
pvs.add("targetClass", new TypedStringValue("com.example.Target", Class.class));

// XML 等效配置
/*
<bean id="server" class="com.example.Server">
    <property name="port" value="8080" type="java.lang.Integer"/>
    <property name="targetClass" value="com.example.Target" type="java.lang.Class"/>
</bean>
*/
```

---

## 总结

### 核心接口关系图

```
org.springframework.beans.factory.config
│
├── BeanDefinition (接口)
│   ├── 描述 Bean 的元数据
│   └── 实现：RootBeanDefinition, ChildBeanDefinition
│
├── BeanFactory 配置接口
│   ├── SingletonBeanRegistry (单例注册)
│   ├── ConfigurableBeanFactory (基本配置)
│   ├── AutowireCapableBeanFactory (自动装配)
│   └── ConfigurableListableBeanFactory (完整配置)
│
├── BeanPostProcessor 体系
│   ├── BeanPostProcessor (基础)
│   ├── InstantiationAwareBeanPostProcessor (实例化感知)
│   ├── SmartInstantiationAwareBeanPostProcessor (智能感知)
│   ├── DestructionAwareBeanPostProcessor (销毁感知)
│   ├── MergedBeanDefinitionPostProcessor (合并定义)
│   ├── BeanFactoryPostProcessor (工厂后处理)
│   └── BeanDefinitionRegistryPostProcessor (注册表后处理)
│
├── FactoryBean 实现
│   ├── AbstractFactoryBean (抽象模板)
│   ├── MethodInvokingFactoryBean (方法调用)
│   ├── FieldRetrievingFactoryBean (字段获取)
│   ├── PropertiesFactoryBean (属性文件)
│   └── YamlPropertiesFactoryBean (YAML 文件)
│
├── 配置器 (Configurers)
│   ├── PlaceholderConfigurerSupport (占位符支持)
│   ├── PropertyPlaceholderConfigurer (属性占位符)
│   ├── PropertyOverrideConfigurer (属性覆盖)
│   └── CustomScopeConfigurer (自定义作用域)
│
├── 作用域 (Scope)
│   ├── Scope (接口)
│   └── 实现：RequestScope, SessionScope, SimpleThreadScope
│
└── 支持类
    ├── BeanDefinitionHolder (定义持有者)
    ├── ConstructorArgumentValues (构造参数值)
    ├── DependencyDescriptor (依赖描述符)
    ├── RuntimeBeanReference (运行时 Bean 引用)
    └── TypedStringValue (带类型的字符串值)
```

### 关键设计模式

| 模式 | 应用 |
|------|------|
| **模板方法模式** | `AbstractFactoryBean` 定义创建流程，子类实现 `createInstance()` |
| **策略模式** | `Scope` 接口允许不同的作用域实现 |
| **责任链模式** | `BeanPostProcessor` 形成处理链 |
| **访问者模式** | `BeanDefinitionVisitor` 访问 BeanDefinition |
| **工厂模式** | `FactoryBean` 创建复杂对象 |

### 扩展点总结

| 扩展点 | 执行时机 | 用途 |
|--------|---------|------|
| `BeanDefinitionRegistryPostProcessor` | BeanDefinition 加载后 | 动态注册 BeanDefinition |
| `BeanFactoryPostProcessor` | BeanDefinition 加载后 | 修改 BeanDefinition 属性 |
| `InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation` | 实例化前 | 返回代理短路默认实例化 |
| `MergedBeanDefinitionPostProcessor.postProcessMergedBeanDefinition` | 实例化后 | 准备注入点元数据 |
| `InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation` | 实例化后 | 控制是否进行属性填充 |
| `InstantiationAwareBeanPostProcessor.postProcessProperties` | 属性填充前 | 自定义属性注入 |
| `BeanPostProcessor.postProcessBeforeInitialization` | 初始化前 | 修改 Bean 状态 |
| `BeanPostProcessor.postProcessAfterInitialization` | 初始化后 | 创建代理（AOP） |
| `DestructionAwareBeanPostProcessor.postProcessBeforeDestruction` | 销毁前 | 自定义销毁逻辑 |

---

*文档生成日期：2026-02-27*
*基于 Spring Framework 6.2.x 源码分析*
