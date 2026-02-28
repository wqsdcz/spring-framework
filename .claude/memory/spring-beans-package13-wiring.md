# Spring Beans Wiring 包详解

## 目录
1. [概述](#概述)
2. [核心概念](#核心概念)
3. [BeanWiringInfo - 装配信息载体](#beanwiringinfo---装配信息载体)
4. [BeanWiringInfoResolver - 装配信息解析策略](#beanwiringinforesolver---装配信息解析策略)
5. [ClassNameBeanWiringInfoResolver - 类名解析实现](#classnamebeanwiringinforesolver---类名解析实现)
6. [BeanConfigurerSupport - 外部Bean依赖注入支持](#beanconfigurersupport---外部bean依赖注入支持)
7. [与 @Configurable 注解的配合](#与-configurable-注解的配合)
8. [使用示例](#使用示例)
9. [总结](#总结)

---

## 概述

`org.springframework.beans.factory.wiring` 包提供了**面向切面编程（AOP）驱动**的 Bean 配置机制。这个包的核心目标是解决一个特殊场景：**为那些不由 Spring 容器创建的 Bean 提供依赖注入能力**。

### 典型应用场景

1. **领域对象（Domain Objects）**：使用 `new` 关键字创建的业务实体类需要依赖注入
2. **反序列化对象**：从缓存或网络接收的对象需要重新注入依赖
3. **遗留系统集成**：无法修改创建逻辑但需要 Spring 管理的依赖

### 包结构

```
org.springframework.beans.factory.wiring
├── BeanWiringInfo.java                    # 装配信息持有者
├── BeanWiringInfoResolver.java            # 装配信息解析策略接口
├── ClassNameBeanWiringInfoResolver.java   # 基于类名的默认解析实现
├── BeanConfigurerSupport.java             # Bean配置支持基类
└── package-info.java                      # 包信息
```

---

## 核心概念

### 什么是 Bean 装配（Wiring）？

在 Spring 中，"Wiring"（装配）指的是**将 Bean 的依赖关系建立起来**的过程。通常有两种方式：

1. **显式装配（Explicit Wiring）**：基于 Bean 定义（XML 或注解）明确指定依赖
2. **自动装配（Autowiring）**：Spring 自动根据类型或名称匹配依赖

### 传统 Spring 容器 vs Wiring 包

| 特性 | 传统 Spring 容器 | Wiring 包 |
|------|-----------------|-----------|
| Bean 创建 | 由容器控制 | 外部创建（`new`、反序列化等） |
| 依赖注入时机 | 容器初始化时 | 对象创建后的任意时刻 |
| 实现方式 | 反射 + 代理 | AspectJ 切面拦截 |
| 适用范围 | 容器管理的 Bean | 任意对象 |

---

## BeanWiringInfo - 装配信息载体

### 源码分析

```java
package org.springframework.beans.factory.wiring;

/**
 * Holder for bean wiring metadata information about a particular class. Used in
 * conjunction with the {@link org.springframework.beans.factory.annotation.Configurable}
 * annotation and the AspectJ {@code AnnotationBeanConfigurerAspect}.
 *
 * 持有特定类的 Bean 装配元数据信息。与 @Configurable 注解和
 * AspectJ 的 AnnotationBeanConfigurerAspect 一起使用。
 */
public class BeanWiringInfo {

    // 自动装配模式常量 - 引用自 AutowireCapableBeanFactory
    public static final int AUTOWIRE_BY_NAME = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;  // 按名称装配 (1)
    public static final int AUTOWIRE_BY_TYPE = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;  // 按类型装配 (2)

    @Nullable
    private String beanName;           // 目标 Bean 名称（用于从 BeanFactory 获取定义）

    private boolean isDefaultBeanName = false;  // 是否为默认 Bean 名称（可能不存在实际定义）

    private int autowireMode = AutowireCapableBeanFactory.AUTOWIRE_NO;  // 自动装配模式（默认不自动装配）

    private boolean dependencyCheck = false;    // 是否进行依赖检查

    /**
     * 默认构造器 - 创建仅执行初始化的配置信息
     * 适用于：只需要执行 factory callbacks 和 post-processors 的场景
     */
    public BeanWiringInfo() {
    }

    /**
     * 基于 Bean 名称的构造器
     * @param beanName 要从中获取属性值的 Bean 定义名称
     */
    public BeanWiringInfo(String beanName) {
        this(beanName, false);
    }

    /**
     * 基于 Bean 名称的构造器（带默认标记）
     * @param beanName 要从中获取属性值的 Bean 定义名称
     * @param isDefaultBeanName 是否为建议的默认 Bean 名称（不一定匹配实际 Bean 定义）
     *
     * 当 isDefaultBeanName=true 且 BeanFactory 中不存在该名称的定义时，
     * 会回退到自动装配模式
     */
    public BeanWiringInfo(String beanName, boolean isDefaultBeanName) {
        Assert.hasText(beanName, "'beanName' must not be empty");
        this.beanName = beanName;
        this.isDefaultBeanName = isDefaultBeanName;
    }

    /**
     * 基于自动装配模式的构造器
     * @param autowireMode 自动装配模式：AUTOWIRE_BY_NAME 或 AUTOWIRE_BY_TYPE
     * @param dependencyCheck 是否在自动装配后执行依赖检查
     *
     * 注意：此构造器创建的 BeanWiringInfo 的 beanName 为 null，
     * 表示使用自动装配而非显式 Bean 定义
     */
    public BeanWiringInfo(int autowireMode, boolean dependencyCheck) {
        if (autowireMode != AUTOWIRE_BY_NAME && autowireMode != AUTOWIRE_BY_TYPE) {
            throw new IllegalArgumentException("Only constants AUTOWIRE_BY_NAME and AUTOWIRE_BY_TYPE supported");
        }
        this.autowireMode = autowireMode;
        this.dependencyCheck = dependencyCheck;
    }

    /**
     * 判断是否使用自动装配
     * 关键逻辑：当 beanName 为 null 时，表示使用自动装配
     */
    public boolean indicatesAutowiring() {
        return (this.beanName == null);
    }

    // Getters...
    @Nullable
    public String getBeanName() { return this.beanName; }

    public boolean isDefaultBeanName() { return this.isDefaultBeanName; }

    public int getAutowireMode() { return this.autowireMode; }

    public boolean getDependencyCheck() { return this.dependencyCheck; }
}
```

### BeanWiringInfo 的三种使用模式

```
┌─────────────────────────────────────────────────────────────────────┐
│                    BeanWiringInfo 使用模式                           │
├─────────────────────────────────────────────────────────────────────┤
│ 1. 默认初始化模式                                                    │
│    new BeanWiringInfo()                                             │
│    → 仅执行 factory/post-processor callbacks                        │
│                                                                     │
│ 2. 显式 Bean 定义模式                                                │
│    new BeanWiringInfo("myBean", false)                              │
│    → 从指定 Bean 定义复制属性值                                      │
│                                                                     │
│ 3. 自动装配模式                                                      │
│    new BeanWiringInfo(AUTOWIRE_BY_NAME, true)                       │
│    → 按名称或类型自动装配依赖                                        │
└─────────────────────────────────────────────────────────────────────┘
```

---

## BeanWiringInfoResolver - 装配信息解析策略

### 策略模式设计

```java
package org.springframework.beans.factory.wiring;

/**
 * Strategy interface to be implemented by objects than can resolve bean name
 * information, given a newly instantiated bean object.
 *
 * 策略接口：给定一个新实例化的 Bean 对象，解析其 Bean 名称信息。
 *
 * 该接口的调用由 AspectJ 切面中的切入点驱动。
 * 元数据解析策略是可插拔的。
 */
public interface BeanWiringInfoResolver {

    /**
     * 为给定的 Bean 实例解析 BeanWiringInfo
     * @param beanInstance 要解析信息的 Bean 实例
     * @return BeanWiringInfo，如果未找到则返回 null
     *
     * 返回 null 表示跳过该 Bean 的配置
     */
    @Nullable
    BeanWiringInfo resolveWiringInfo(Object beanInstance);
}
```

### 策略实现类图

```
                    BeanWiringInfoResolver
                           │ 接口
                           │ resolveWiringInfo(Object)
                           │
           ┌───────────────┴───────────────┐
           │                               │
           ▼                               ▼
ClassNameBeanWiringInfoResolver    AnnotationBeanWiringInfoResolver
           │                               │
           │ 基于类名解析                   │ 基于 @Configurable 注解解析
           │                               │
           │ 默认策略                       │ 注解驱动策略
           │                               │
    resolveWiringInfo()              resolveWiringInfo()
    └── 返回: class.getName()          └── 检查 @Configurable 存在性
                                       └── 解析 autowire/value 属性
```

---

## ClassNameBeanWiringInfoResolver - 类名解析实现

### 源码分析

```java
package org.springframework.beans.factory.wiring;

/**
 * Simple default implementation of the {@link BeanWiringInfoResolver} interface,
 * looking for a bean with the same name as the fully-qualified class name.
 *
 * BeanWiringInfoResolver 的简单默认实现，查找与完全限定类名同名的 Bean。
 *
 * 这与 Spring XML 文件中不使用 'id' 属性时的默认 Bean 名称匹配。
 */
public class ClassNameBeanWiringInfoResolver implements BeanWiringInfoResolver {

    @Override
    public BeanWiringInfo resolveWiringInfo(Object beanInstance) {
        Assert.notNull(beanInstance, "Bean instance must not be null");

        // ClassUtils.getUserClass() 用于获取原始类（处理 CGLIB 代理的情况）
        // 使用完全限定类名作为 Bean 名称，并标记为默认名称
        return new BeanWiringInfo(ClassUtils.getUserClass(beanInstance).getName(), true);
    }
}
```

### 类名到 Bean 名的映射规则

```java
// 示例：类名到 Bean 名称的映射

// 类定义
package com.example.service;
public class UserService {
    private UserRepository userRepository;
    // setter...
}

// ClassNameBeanWiringInfoResolver 解析结果
Object beanInstance = new UserService();
BeanWiringInfo info = resolver.resolveWiringInfo(beanInstance);

// info.getBeanName() = "com.example.service.UserService"
// info.isDefaultBeanName() = true
```

### 与 XML 配置的对应关系

```xml
<!-- 不使用 id 属性时，Spring 默认使用类名作为 Bean 名称 -->
<bean class="com.example.service.UserService">
    <property name="userRepository" ref="userRepository"/>
</bean>

<!-- 上述配置等效于 -->
<bean id="com.example.service.UserService" class="com.example.service.UserService">
    <property name="userRepository" ref="userRepository"/>
</bean>
```

---

## BeanConfigurerSupport - 外部Bean依赖注入支持

### 核心作用

`BeanConfigurerSupport` 是一个**便利的基类**，为 Bean 配置器提供依赖注入能力。它通常被 AspectJ 切面继承，用于在对象创建后（通过 `new` 或反序列化）执行 Spring 风格的依赖注入。

### 源码详细分析

```java
package org.springframework.beans.factory.wiring;

/**
 * Convenient base class for bean configurers that can perform Dependency Injection
 * on objects (however they may be created). Typically subclassed by AspectJ aspects.
 *
 * Bean 配置器的便利基类，可以对对象（无论它们如何被创建）执行依赖注入。
 * 通常被 AspectJ 切面继承。
 */
public class BeanConfigurerSupport implements BeanFactoryAware, InitializingBean, DisposableBean {

    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private volatile BeanWiringInfoResolver beanWiringInfoResolver;  // 装配信息解析器

    @Nullable
    private volatile ConfigurableListableBeanFactory beanFactory;    // Bean 工厂

    /**
     * 设置自定义的 BeanWiringInfoResolver
     * 默认使用 ClassNameBeanWiringInfoResolver
     */
    public void setBeanWiringInfoResolver(BeanWiringInfoResolver beanWiringInfoResolver) {
        Assert.notNull(beanWiringInfoResolver, "BeanWiringInfoResolver must not be null");
        this.beanWiringInfoResolver = beanWiringInfoResolver;
    }

    /**
     * 设置 BeanFactory - 实现 BeanFactoryAware 接口
     * 要求必须是 ConfigurableListableBeanFactory 类型
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory clbf)) {
            throw new IllegalArgumentException(
                "Bean configurer aspect needs to run in a ConfigurableListableBeanFactory: " + beanFactory);
        }
        this.beanFactory = clbf;

        // 如果没有显式设置解析器，创建默认解析器
        if (this.beanWiringInfoResolver == null) {
            this.beanWiringInfoResolver = createDefaultBeanWiringInfoResolver();
        }
    }

    /**
     * 创建默认的 BeanWiringInfoResolver
     * 子类可以覆盖此方法提供自定义默认解析器
     */
    @Nullable
    protected BeanWiringInfoResolver createDefaultBeanWiringInfoResolver() {
        return new ClassNameBeanWiringInfoResolver();
    }

    /**
     * 初始化后检查 - 确保 BeanFactory 已设置
     */
    @Override
    public void afterPropertiesSet() {
        Assert.notNull(this.beanFactory, "BeanFactory must be set");
    }

    /**
     * 销毁时释放引用
     */
    @Override
    public void destroy() {
        this.beanFactory = null;
        this.beanWiringInfoResolver = null;
    }

    /**
     * 配置 Bean 实例 - 核心方法
     *
     * 这是最关键的方法，由切面调用以执行实际的依赖注入。
     * 支持两种配置模式：
     * 1. 自动装配模式（autowiring）
     * 2. 显式 Bean 定义模式（explicit wiring）
     */
    public void configureBean(Object beanInstance) {
        // 1. 检查 BeanFactory 是否可用
        if (this.beanFactory == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("BeanFactory has not been set on " + ClassUtils.getShortName(getClass()) + ": " +
                        "Make sure this configurer runs in a Spring container. Unable to configure bean of type [" +
                        ClassUtils.getDescriptiveType(beanInstance) + "]. Proceeding without injection.");
            }
            return;
        }

        // 2. 解析 BeanWiringInfo
        BeanWiringInfoResolver bwiResolver = this.beanWiringInfoResolver;
        Assert.state(bwiResolver != null, "No BeanWiringInfoResolver available");
        BeanWiringInfo bwi = bwiResolver.resolveWiringInfo(beanInstance);

        // 如果解析器返回 null，跳过配置
        if (bwi == null) {
            return;
        }

        ConfigurableListableBeanFactory beanFactory = this.beanFactory;
        Assert.state(beanFactory != null, "No BeanFactory available");

        try {
            String beanName = bwi.getBeanName();

            // 3. 判断使用自动装配还是显式配置
            if (bwi.indicatesAutowiring() ||
                (bwi.isDefaultBeanName() && beanName != null && !beanFactory.containsBean(beanName))) {

                // 场景 A：自动装配模式
                // - indicatesAutowiring() 返回 true（beanName 为 null）
                // - 或者是默认 Bean 名称但工厂中不存在该定义

                // 执行自动装配属性
                beanFactory.autowireBeanProperties(beanInstance, bwi.getAutowireMode(), bwi.getDependencyCheck());

                // 执行标准工厂回调和后置处理器
                beanFactory.initializeBean(beanInstance, (beanName != null ? beanName : ""));
            }
            else {
                // 场景 B：显式 Bean 定义模式
                // 从指定的 Bean 定义复制配置
                beanFactory.configureBean(beanInstance, (beanName != null ? beanName : ""));
            }
        }
        catch (BeanCreationException ex) {
            // 4. 特殊处理循环引用情况
            Throwable rootCause = ex.getMostSpecificCause();
            if (rootCause instanceof BeanCurrentlyInCreationException bce) {
                String bceBeanName = bce.getBeanName();
                if (bceBeanName != null && beanFactory.isCurrentlyInCreation(bceBeanName)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Failed to create target bean '" + bce.getBeanName() +
                                "' while configuring object of type [" + beanInstance.getClass().getName() +
                                "] - probably due to a circular reference. This is a common startup situation " +
                                "and usually not fatal. Proceeding without injection. Original exception: " + ex);
                    }
                    return;  // 静默处理循环引用
                }
            }
            throw ex;  // 其他异常继续抛出
        }
    }
}
```

### 配置流程图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         configureBean 流程                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. 检查 BeanFactory                                                    │
│     └── 如果为 null，记录调试日志并返回                                  │
│                                                                         │
│  2. 解析 BeanWiringInfo                                                 │
│     └── 调用 BeanWiringInfoResolver.resolveWiringInfo()                 │
│     └── 如果返回 null，跳过配置                                          │
│                                                                         │
│  3. 判断配置模式                                                        │
│     │                                                                   │
│     ├─ indicatesAutowiring() == true ──→ 自动装配模式                   │
│     │                                     ├─ autowireBeanProperties()   │
│     │                                     └─ initializeBean()           │
│     │                                                                   │
│     ├─ isDefaultBeanName && !containsBean() ──→ 自动装配模式（回退）     │
│     │                                                                   │
│     └─ 其他情况 ──→ 显式 Bean 定义模式                                   │
│                     └─ configureBean()                                  │
│                                                                         │
│  4. 异常处理                                                            │
│     └─ BeanCurrentlyInCreationException → 静默处理（循环引用）           │
│     └─ 其他异常 → 抛出                                                  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 与 @Configurable 注解的配合

### @Configurable 注解定义

```java
package org.springframework.beans.factory.annotation;

/**
 * Marks a class as being eligible for Spring-driven configuration.
 *
 * 标记类有资格进行 Spring 驱动的配置。
 *
 * 通常与 AspectJ AnnotationBeanConfigurerAspect 一起使用。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Configurable {

    /**
     * 用作配置模板的 Bean 定义名称
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

### Autowire 枚举

```java
public enum Autowire {
    NO(AutowireCapableBeanFactory.AUTOWIRE_NO),           // 0 - 不自动装配
    BY_NAME(AutowireCapableBeanFactory.AUTOWIRE_BY_NAME), // 1 - 按名称装配
    BY_TYPE(AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE); // 2 - 按类型装配

    private final int value;

    Autowire(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public boolean isAutowire() {
        return (this == BY_NAME || this == BY_TYPE);
    }
}
```

### AnnotationBeanWiringInfoResolver - 注解驱动的解析器

```java
package org.springframework.beans.factory.annotation;

/**
 * 使用 @Configurable 注解识别需要自动装配的类的 BeanWiringInfoResolver。
 * 要查找的 Bean 名称从 @Configurable 注解获取；
 * 如果未指定，则默认为被配置类的完全限定名。
 */
public class AnnotationBeanWiringInfoResolver implements BeanWiringInfoResolver {

    @Override
    @Nullable
    public BeanWiringInfo resolveWiringInfo(Object beanInstance) {
        Assert.notNull(beanInstance, "Bean instance must not be null");

        // 获取类上的 @Configurable 注解
        Configurable annotation = beanInstance.getClass().getAnnotation(Configurable.class);

        // 如果没有注解，返回 null（跳过配置）
        return (annotation != null ? buildWiringInfo(beanInstance, annotation) : null);
    }

    /**
     * 根据 @Configurable 注解构建 BeanWiringInfo
     */
    protected BeanWiringInfo buildWiringInfo(Object beanInstance, Configurable annotation) {

        // 情况 1：配置了自动装配
        if (!Autowire.NO.equals(annotation.autowire())) {
            return new BeanWiringInfo(annotation.autowire().value(), annotation.dependencyCheck());
        }
        // 情况 2：显式指定了 Bean 名称
        else if (!annotation.value().isEmpty()) {
            return new BeanWiringInfo(annotation.value(), false);
        }
        // 情况 3：使用默认 Bean 名称（类名）
        else {
            return new BeanWiringInfo(getDefaultBeanName(beanInstance), true);
        }
    }

    /**
     * 获取默认 Bean 名称（处理 CGLIB 代理）
     */
    protected String getDefaultBeanName(Object beanInstance) {
        return ClassUtils.getUserClass(beanInstance).getName();
    }
}
```

### AnnotationBeanConfigurerAspect - AspectJ 切面实现

```java
package org.springframework.beans.factory.aspectj;

/**
 * 使用 @Configurable 注解识别需要自动装配的类的具体切面。
 */
public aspect AnnotationBeanConfigurerAspect extends AbstractInterfaceDrivenDependencyInjectionAspect
        implements BeanFactoryAware, InitializingBean, DisposableBean {

    // 委托给 BeanConfigurerSupport 执行实际配置
    private final BeanConfigurerSupport beanConfigurerSupport = new BeanConfigurerSupport();

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        // 使用注解驱动的解析器
        this.beanConfigurerSupport.setBeanWiringInfoResolver(new AnnotationBeanWiringInfoResolver());
        this.beanConfigurerSupport.setBeanFactory(beanFactory);
    }

    @Override
    public void afterPropertiesSet() {
        this.beanConfigurerSupport.afterPropertiesSet();
    }

    @Override
    public void configureBean(Object bean) {
        this.beanConfigurerSupport.configureBean(bean);
    }

    @Override
    public void destroy() {
        this.beanConfigurerSupport.destroy();
    }

    /**
     * 切入点：匹配带有 @Configurable 注解的类
     */
    public pointcut inConfigurableBean() : @this(Configurable);

    /**
     * 切入点：匹配配置了 preConstruction=true 的类
     */
    public pointcut preConstructionConfiguration() : preConstructionConfigurationSupport(*);

    /**
     * 声明：所有带有 @Configurable 的类都实现 ConfigurableObject 接口
     * 这使得它们可以被依赖注入切面处理
     */
    declare parents: @Configurable * implements ConfigurableObject;
}
```

### 依赖注入时机

```
┌─────────────────────────────────────────────────────────────────────┐
│                      依赖注入时机选择                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  @Configurable(preConstruction = false)  [默认]                     │
│  ═══════════════════════════════════════                            │
│                                                                     │
│  构造器执行                                                          │
│       │                                                             │
│       ▼                                                             │
│  对象创建完成                                                        │
│       │                                                             │
│       ▼                                                             │
│  ┌─────────────┐                                                    │
│  │ after 通知   │ ◄── 依赖注入发生在这里                              │
│  │ configureBean│    (可以安全地使用 final 字段)                      │
│  └─────────────┘                                                    │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  @Configurable(preConstruction = true)                              │
│  ═════════════════════════════════════                              │
│                                                                     │
│  ┌─────────────┐                                                    │
│  │ before 通知  │ ◄── 依赖注入发生在这里                              │
│  │ configureBean│    (构造器执行前，可用于构造器参数注入)              │
│  └─────────────┘                                                    │
│       │                                                             │
│       ▼                                                             │
│  构造器执行                                                          │
│       │                                                             │
│       ▼                                                             │
│  对象创建完成                                                        │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 使用示例

### 示例 1：基本使用（类名作为 Bean 名）

```java
// 领域对象 - 使用 new 创建但需要依赖注入
package com.example.domain;

public class Order {
    private OrderRepository orderRepository;  // 需要注入的依赖

    public void save() {
        orderRepository.save(this);
    }

    // setter 用于自动装配
    public void setOrderRepository(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
```

```xml
<!-- Spring XML 配置 -->
<bean class="com.example.domain.Order" autowire="byName"/>

<bean id="orderRepository" class="com.example.repository.OrderRepositoryImpl"/>
```

```java
// 配置 BeanConfigurerSupport
@Bean
public BeanConfigurerSupport beanConfigurerSupport(ConfigurableListableBeanFactory beanFactory) {
    BeanConfigurerSupport support = new BeanConfigurerSupport();
    support.setBeanFactory(beanFactory);
    support.afterPropertiesSet();
    return support;
}
```

```java
// 使用 - 即使使用 new 创建，依赖也会被注入
Order order = new Order();
// order.getOrderRepository() 现在不为 null！
order.save();
```

### 示例 2：使用 @Configurable 注解

```java
package com.example.domain;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Autowire;

/**
 * 使用 @Configurable 标记为可配置
 * 按名称自动装配依赖
 */
@Configurable(autowire = Autowire.BY_NAME)
public class Customer {
    private CustomerRepository customerRepository;
    private EmailService emailService;

    public void register() {
        customerRepository.save(this);
        emailService.sendWelcomeEmail(this);
    }

    // setters...
    public void setCustomerRepository(CustomerRepository repo) {
        this.customerRepository = repo;
    }

    public void setEmailService(EmailService service) {
        this.emailService = service;
    }
}
```

```java
/**
 * 使用显式 Bean 定义名称
 */
@Configurable("premiumCustomer")
public class PremiumCustomer extends Customer {
    private DiscountService discountService;

    // setter...
}
```

```xml
<!-- 对应的 Bean 定义 -->
<bean id="premiumCustomer" class="com.example.domain.PremiumCustomer">
    <property name="discountService" ref="premiumDiscountService"/>
</bean>
```

### 示例 3：自定义 BeanWiringInfoResolver

```java
/**
 * 基于包名的解析器 - 将特定包下的类映射到对应的 Bean 定义
 */
public class PackageBasedWiringInfoResolver implements BeanWiringInfoResolver {

    private final String basePackage;
    private final String beanNameSuffix;

    public PackageBasedWiringInfoResolver(String basePackage, String beanNameSuffix) {
        this.basePackage = basePackage;
        this.beanNameSuffix = beanNameSuffix;
    }

    @Override
    public BeanWiringInfo resolveWiringInfo(Object beanInstance) {
        Class<?> clazz = ClassUtils.getUserClass(beanInstance);
        String className = clazz.getName();

        // 只处理指定包下的类
        if (className.startsWith(basePackage)) {
            String simpleName = clazz.getSimpleName();
            String beanName = simpleName + beanNameSuffix;
            return new BeanWiringInfo(beanName, false);
        }

        return null;  // 其他类不处理
    }
}
```

```java
// 使用自定义解析器
@Bean
public BeanConfigurerSupport customConfigurerSupport(ConfigurableListableBeanFactory beanFactory) {
    BeanConfigurerSupport support = new BeanConfigurerSupport();
    support.setBeanWiringInfoResolver(
        new PackageBasedWiringInfoResolver("com.example.domain", "Service")
    );
    support.setBeanFactory(beanFactory);
    return support;
}
```

### 示例 4：AspectJ 完整配置

```java
/**
 * 启用 AspectJ 加载时织入（Load-Time Weaving）
 */
@Configuration
@EnableLoadTimeWeaving
public class AppConfig {

    @Bean
    public AnnotationBeanConfigurerAspect annotationBeanConfigurerAspect() {
        return AnnotationBeanConfigurerAspect.aspectOf();
    }
}
```

```java
/**
 * 领域对象 - 完全独立于 Spring 容器的创建方式
 */
@Configurable(autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class Product implements Serializable {

    private transient ProductRepository productRepository;
    private transient CacheManager cacheManager;

    public static Product createNew(String name, BigDecimal price) {
        // 使用 new 创建，但依赖会被自动注入
        Product product = new Product();
        product.name = name;
        product.price = price;
        return product;
    }

    // 反序列化后也会重新注入依赖
    private Object readResolve() {
        return this;
    }

    // setters for autowiring...
}
```

---

## 总结

### 核心组件关系图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Spring Wiring 架构                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────┐                                                    │
│  │   @Configurable     │  标记类需要 Spring 配置                             │
│  │   (Annotation)      │                                                    │
│  └──────────┬──────────┘                                                    │
│             │                                                               │
│             ▼                                                               │
│  ┌─────────────────────┐     ┌──────────────────────────────────────┐      │
│  │ AnnotationBean      │────▶│ AnnotationBeanWiringInfoResolver     │      │
│  │ ConfigurerAspect    │     │ (解析 @Configurable 注解)             │      │
│  │ (AspectJ 切面)       │     └──────────────────┬───────────────────┘      │
│  └──────────┬──────────┘                        │                           │
│             │                                   │ resolveWiringInfo()       │
│             │                                   ▼                           │
│             │                        ┌─────────────────────┐                │
│             │                        │   BeanWiringInfo    │                │
│             │                        │   (装配元数据)       │                │
│             │                        └──────────┬──────────┘                │
│             │                                   │                           │
│             │                                   │ 包含: beanName, autowireMode│
│             │                                   │       dependencyCheck      │
│             │                                   ▼                           │
│             │                        ┌─────────────────────┐                │
│             └───────────────────────▶│ BeanConfigurerSupport│               │
│                                      │   (配置执行器)       │               │
│                                      └──────────┬──────────┘                │
│                                                 │                           │
│                                                 │ configureBean()           │
│                                                 ▼                           │
│                                      ┌─────────────────────┐                │
│                                      │ ConfigurableListable│                │
│                                      │    BeanFactory      │                │
│                                      │   (执行实际 DI)      │                │
│                                      └─────────────────────┘                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 关键设计模式

1. **策略模式（Strategy Pattern）**
   - `BeanWiringInfoResolver` 接口允许不同的解析策略
   - `ClassNameBeanWiringInfoResolver` 和 `AnnotationBeanWiringInfoResolver` 是具体策略

2. **模板方法模式（Template Method Pattern）**
   - `BeanConfigurerSupport` 定义了配置 Bean 的标准流程
   - 子类可以覆盖 `createDefaultBeanWiringInfoResolver()` 方法

3. **依赖查找 vs 依赖注入**
   - 传统 DI：容器创建对象并注入依赖
   - Wiring 包：对象已存在，从容器查找依赖并注入

### 适用场景总结

| 场景 | 解决方案 |
|------|---------|
| 领域模型使用 `new` 创建 | `@Configurable` + AspectJ LTW |
| 反序列化对象需要重新注入依赖 | `@Configurable` + `readResolve()` |
| 第三方库创建的对象 | 自定义 `BeanWiringInfoResolver` |
| 单元测试中的领域对象 | `BeanConfigurerSupport.configureBean()` 手动调用 |

### 注意事项

1. **性能影响**：AspectJ 织入会带来一定的性能开销
2. **构建复杂性**：需要配置 AspectJ 编译器或加载时织入代理
3. **调试难度**：依赖注入发生在运行时，调试可能较复杂
4. **设计权衡**：考虑是否真的需要脱离容器创建对象，或者是否可以重构为工厂模式

---

## 参考文件

- `f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\factory\wiring\BeanWiringInfo.java`
- `f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\factory\wiring\BeanWiringInfoResolver.java`
- `f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\factory\wiring\ClassNameBeanWiringInfoResolver.java`
- `f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\factory\wiring\BeanConfigurerSupport.java`
- `f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\factory\annotation\Configurable.java`
- `f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\factory\annotation\AnnotationBeanWiringInfoResolver.java`
- `f:\GitHub\spring-framework\spring-aspects\src\main\java\org\springframework\beans\factory\aspectj\AnnotationBeanConfigurerAspect.aj`
