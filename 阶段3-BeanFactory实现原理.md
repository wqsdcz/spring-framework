# 阶段3：BeanFactory实现原理 - 详细学习指导

## 学习目标

1. 理解DefaultListableBeanFactory的核心结构
2. 掌握Bean创建的完整流程
3. 理解三级缓存解决循环依赖的原理
4. 跟踪getBean方法的调用链

预计学习时间：5-7天 ⭐ 重点

---

## Day 1: DefaultListableBeanFactory 概览

### 核心文件
`spring-beans/src/main/java/org/springframework/beans/factory/support/DefaultListableBeanFactory.java`

### 类定位
```java
/**
 * Spring's default implementation of the {@link ConfigurableListableBeanFactory}
 * and {@link BeanDefinitionRegistry} interfaces: a full-fledged bean factory
 * based on bean definition metadata, extensible through post-processors.
 */
```

**核心角色**：
- 实现`ConfigurableListableBeanFactory` - 可配置的列表BeanFactory
- 实现`BeanDefinitionRegistry` - Bean定义注册表
- Spring的**默认**、**完整功能**的BeanFactory实现

### 类继承关系
```
DefaultListableBeanFactory
    ← AbstractAutowireCapableBeanFactory
        ← AbstractBeanFactory
            ← FactoryBeanRegistrySupport
                ← DefaultSingletonBeanRegistry
                    ← SimpleAliasRegistry
```

### 核心数据结构（第195-200行）

```java
/** Map of bean definition objects, keyed by bean name. */
private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

/** Map from bean name to merged BeanDefinitionHolder. */
private final Map<String, BeanDefinitionHolder> mergedBeanDefinitionHolders = new ConcurrentHashMap<>(256);
```

| 字段 | 说明 |
|------|------|
| `beanDefinitionMap` | bean名称到BeanDefinition的映射 |
| `mergedBeanDefinitionHolders` | 合并后的BeanDefinitionHolder缓存 |
| `resolvableDependencies` | 可解析的依赖类型映射 |
| `autowireCandidateResolver` | 自动装配候选者解析器 |

### 注册Bean定义

```java
@Override
public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
        throws BeanDefinitionStoreException {
    // 1. 验证bean定义
    // 2. 检查是否允许覆盖
    // 3. 存入beanDefinitionMap
    // 4. 清除合并定义的缓存
}
```

### 今日实践任务

1. 查看DefaultListableBeanFactory的所有字段
2. 理解`BeanDefinitionRegistry`接口的作用
3. 思考：为什么使用ConcurrentHashMap？

---

## Day 2: Bean创建流程 - 入口

### getBean调用链

```
getBean(String name)
    ↓
doGetBean(String name, @Nullable Class<T> requiredType, ...)
    ↓
createBean(String beanName, RootBeanDefinition mbd, Object[] args)
    ↓
doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args)
```

### doGetBean核心逻辑（AbstractBeanFactory）

```java
protected <T> T doGetBean(
        String name, @Nullable Class<T> requiredType,
        @Nullable Object[] args, boolean typeCheckOnly) throws BeansException {

    // 1. 转换bean名称（处理&前缀和别名）
    final String beanName = transformedBeanName(name);

    // 2. 尝试从缓存获取单例
    Object sharedInstance = getSingleton(beanName);

    if (sharedInstance != null && args == null) {
        // 缓存命中，获取真正的bean实例（处理FactoryBean）
        bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
    } else {
        // 3. 检查父工厂
        BeanFactory parentBeanFactory = getParentBeanFactory();
        if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
            return parentBeanFactory.getBean(name, requiredType);
        }

        // 4. 获取合并的BeanDefinition
        final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

        // 5. 检查依赖，先创建依赖的bean
        String[] dependsOn = mbd.getDependsOn();
        if (dependsOn != null) {
            for (String dep : dependsOn) {
                registerDependentBean(dep, beanName);
                getBean(dep);  // 递归创建依赖
            }
        }

        // 6. 创建bean实例
        if (mbd.isSingleton()) {
            sharedInstance = getSingleton(beanName, () -> {
                try {
                    return createBean(beanName, mbd, args);
                } catch (BeansException ex) {
                    destroySingleton(beanName);
                    throw ex;
                }
            });
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
        }
        // ... prototype和其他scope的处理
    }
    return (T) bean;
}
```

### 关键点解析

1. **名称转换**：`transformedBeanName`处理`&`前缀和别名解析
2. **单例缓存**：先查缓存，命中则直接返回
3. **依赖处理**：`depends-on`属性指定的依赖先创建
4. **合并定义**：获取合并后的RootBeanDefinition

### 今日实践任务

1. 跟踪doGetBean的代码流程
2. 理解单例缓存的获取逻辑
3. 思考：为什么FactoryBean需要特殊处理？

---

## Day 3: Bean创建流程 - 实例化

### createBean流程（AbstractAutowireCapableBeanFactory）

```java
@Override
protected Object createBean(String beanName, RootBeanDefinition mbd, Object[] args)
        throws BeanCreationException {

    // 1. 解析bean类
    Class<?> resolvedClass = resolveBeanClass(mbd, beanName);

    // 2. 准备方法重写（lookup-method, replaced-method）
    mbd.prepareMethodOverrides();

    // 3. 给BeanPostProcessor机会返回代理
    Object bean = resolveBeforeInstantiation(beanName, mbd);
    if (bean != null) {
        return bean;  // 短路，直接返回代理
    }

    // 4. 实际创建bean
    Object beanInstance = doCreateBean(beanName, mbd, args);
    return beanInstance;
}
```

### doCreateBean完整流程

```java
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) {

    // 1. 实例化（创建原始对象）
    BeanWrapper instanceWrapper = createBeanInstance(beanName, mbd, args);
    Object bean = instanceWrapper.getWrappedInstance();

    // 2. 解决循环依赖：暴露早期引用
    boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
            isSingletonCurrentlyInCreation(beanName));
    if (earlySingletonExposure) {
        addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
    }

    // 3. 填充属性（依赖注入）
    populateBean(beanName, mbd, instanceWrapper);

    // 4. 初始化
    Object exposedObject = initializeBean(beanName, bean, mbd);

    // 5. 处理循环依赖的后续
    if (earlySingletonExposure) {
        Object earlySingletonReference = getSingleton(beanName, false);
        if (earlySingletonReference != null) {
            if (exposedObject == bean) {
                exposedObject = earlySingletonReference;
            }
        }
    }

    // 6. 注册销毁回调
    registerDisposableBeanIfNecessary(beanName, bean, mbd);

    return exposedObject;
}
```

### 实例化策略

```java
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, Object[] args) {
    // 1. 使用instanceSupplier（如果存在）
    if (mbd.getInstanceSupplier() != null) {
        return obtainFromSupplier(mbd.getInstanceSupplier(), beanName);
    }

    // 2. 使用工厂方法
    if (mbd.getFactoryMethodName() != null) {
        return instantiateUsingFactoryMethod(beanName, mbd, args);
    }

    // 3. 使用构造器
    Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
    if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR) {
        return autowireConstructor(beanName, mbd, ctors, args);
    }

    // 4. 使用默认无参构造器
    return instantiateBean(beanName, mbd);
}
```

### 今日实践任务

1. 理解doCreateBean的6个步骤
2. 重点关注`createBeanInstance`的多种实例化方式
3. 思考：早期引用（early reference）的作用是什么？

---

## Day 4: Bean创建流程 - 属性填充与初始化

### populateBean 属性填充

```java
protected void populateBean(String beanName, RootBeanDefinition mbd, BeanWrapper bw) {
    PropertyValues pvs = mbd.getPropertyValues();

    // 1. 给InstantiationAwareBeanPostProcessor机会修改属性
    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
            if (!bp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                return;
            }
        }
    }

    // 2. 按名称自动装配
    if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME) {
        autowireByName(beanName, mbd, bw, newPvs);
    }

    // 3. 按类型自动装配
    if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
        autowireByType(beanName, mbd, bw, newPvs);
    }

    // 4. 处理注解注入（@Autowired等）
    PropertyDescriptor[] filteredPds = filterPropertyDescriptorsForDependencyCheck(bw);
    if (hasInstAwareBpps || needsDepCheck) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {
                pvs = ((InstantiationAwareBeanPostProcessor) bp).postProcessProperties(pvs, bw.getWrappedInstance(), beanName);
            }
        }
    }

    // 5. 应用属性值
    applyPropertyValues(beanName, mbd, bw, pvs);
}
```

### initializeBean 初始化

```java
protected Object initializeBean(String beanName, Object bean, RootBeanDefinition mbd) {
    // 1. 注入Aware接口
    invokeAwareMethods(beanName, bean);

    // 2. 初始化前处理
    Object wrappedBean = bean;
    if (mbd == null || !mbd.isSynthetic()) {
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
    }

    // 3. 调用初始化方法
    try {
        invokeInitMethods(beanName, wrappedBean, mbd);
    } catch (Throwable ex) {
        throw new BeanCreationException(mbd.getResourceDescription(), beanName, ex.getMessage(), ex);
    }

    // 4. 初始化后处理
    if (mbd == null || !mbd.isSynthetic()) {
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    }

    return wrappedBean;
}
```

### invokeAwareMethods

```java
private void invokeAwareMethods(String beanName, Object bean) {
    if (bean instanceof Aware) {
        if (bean instanceof BeanNameAware) {
            ((BeanNameAware) bean).setBeanName(beanName);
        }
        if (bean instanceof BeanClassLoaderAware) {
            ((BeanClassLoaderAware) bean).setBeanClassLoader(getBeanClassLoader());
        }
        if (bean instanceof BeanFactoryAware) {
            ((BeanFactoryAware) bean).setBeanFactory(AbstractAutowireCapableBeanFactory.this);
        }
    }
}
```

### invokeInitMethods

```java
protected void invokeInitMethods(String beanName, Object bean, RootBeanDefinition mbd) {
    // 1. 调用InitializingBean.afterPropertiesSet()
    boolean isInitializingBean = (bean instanceof InitializingBean);
    if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
        ((InitializingBean) bean).afterPropertiesSet();
    }

    // 2. 调用自定义init-method
    String initMethodName = (mbd != null ? mbd.getInitMethodName() : null);
    if (StringUtils.hasLength(initMethodName) &&
            !(isInitializingBean && "afterPropertiesSet".equals(initMethodName))) {
        invokeCustomInitMethod(beanName, bean, mbd);
    }
}
```

### 今日实践任务

1. 理解populateBean的5个步骤
2. 理解initializeBean的4个步骤
3. 重点关注@Autowired注入的时机

---

## Day 5: 循环依赖解决方案

### 循环依赖示例

```java
@Component
public class A {
    @Autowired
    private B b;
}

@Component
public class B {
    @Autowired
    private A a;
}
```

### 三级缓存

```java
/** Cache of singleton objects: bean name to bean instance. */
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

/** Cache of singleton factories: bean name to ObjectFactory. */
private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

/** Cache of early singleton objects: bean name to bean instance. */
private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);
```

| 缓存级别 | 字段名 | 用途 |
|---------|--------|------|
| 一级缓存 | `singletonObjects` | 成品单例（完全初始化） |
| 二级缓存 | `earlySingletonObjects` | 早期单例（已实例化，未填充属性） |
| 三级缓存 | `singletonFactories` | 单例工厂（用于创建早期引用） |

### 解决流程

```
创建A
    ↓
实例化A（调用构造器）→ 将A的ObjectFactory放入三级缓存
    ↓
填充A的属性（需要B）
    ↓
    创建B
        ↓
    实例化B → 将B的ObjectFactory放入三级缓存
        ↓
    填充B的属性（需要A）
        ↓
        从三级缓存获取A的ObjectFactory
        ↓
        创建A的早期引用 → 放入二级缓存，删除三级缓存
        ↓
    B继续初始化 → B完成
    ↓
A继续初始化 → A完成
```

### 关键代码

```java
// 1. 暴露早期引用（doCreateBean中）
if (earlySingletonExposure) {
    addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
}

// 2. 获取早期引用
protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
    Object exposedObject = bean;
    for (BeanPostProcessor bp : getBeanPostProcessors()) {
        if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
            exposedObject = ((SmartInstantiationAwareBeanPostProcessor) bp).getEarlyBeanReference(exposedObject, beanName);
        }
    }
    return exposedObject;
}

// 3. 获取单例时检查三级缓存
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    Object singletonObject = this.singletonObjects.get(beanName);
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        singletonObject = this.earlySingletonObjects.get(beanName);
        if (singletonObject == null && allowEarlyReference) {
            ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
            if (singletonFactory != null) {
                singletonObject = singletonFactory.getObject();
                this.earlySingletonObjects.put(beanName, singletonObject);
                this.singletonFactories.remove(beanName);
            }
        }
    }
    return singletonObject;
}
```

### 为什么需要三级缓存？

**只用一级缓存的问题**：
- 如果A依赖B，B依赖A
- A实例化后还没填充属性就放入一级缓存
- 此时从缓存获取的A是不完整的（属性未填充）

**三级缓存的作用**：
1. **singletonObjects**：存放完全可用的成品bean
2. **earlySingletonObjects**：存放已实例化但未填充属性的bean（用于解决循环依赖）
3. **singletonFactories**：存放创建早期引用的工厂（延迟创建，按需调用）

### 今日实践任务

1. 理解三级缓存各自的作用
2. 画出循环依赖解决的时序图
3. 思考：为什么构造器循环依赖无法解决？

---

## Day 6-7: 实践与调试

### 调试练习

在以下方法打断点，跟踪bean创建：

1. `DefaultListableBeanFactory.getBean()` - 入口
2. `AbstractBeanFactory.doGetBean()` - 核心逻辑
3. `DefaultSingletonBeanRegistry.getSingleton()` - 单例获取
4. `AbstractAutowireCapableBeanFactory.createBean()` - 创建bean
5. `AbstractAutowireCapableBeanFactory.doCreateBean()` - 实际创建
6. `AbstractAutowireCapableBeanFactory.populateBean()` - 属性填充
7. `AbstractAutowireCapableBeanFactory.initializeBean()` - 初始化

### 观察内部状态

在调试时观察：

```java
// 查看beanDefinitionMap
factory.getBeanDefinitionCount();
factory.getBeanDefinitionNames();

// 查看单例缓存（通过反射或调试器）
singletonObjects  // ConcurrentHashMap
earlySingletonObjects  // ConcurrentHashMap
singletonFactories  // HashMap
```

### 循环依赖测试

```java
@Component
public class CircularA {
    @Autowired
    private CircularB b;
}

@Component
public class CircularB {
    @Autowired
    private CircularA a;
}

// 测试
public class CircularTest {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(CircularA.class, CircularB.class);
        CircularA a = ctx.getBean(CircularA.class);
        System.out.println("Circular dependency resolved!");
    }
}
```

### 验证检查清单

- [ ] 能画出getBean的完整调用链
- [ ] 理解doCreateBean的6个步骤
- [ ] 理解三级缓存的作用
- [ ] 能解释循环依赖的解决原理
- [ ] 通过调试跟踪过bean创建流程

---

## 常见问题

### Q1: 为什么构造器注入的循环依赖无法解决？

**A**: 因为构造器注入时，bean还没实例化完成就需要依赖对象。而三级缓存机制需要bean先实例化（调用构造器后）才能暴露早期引用。

**解决方案**：
- 改用Setter注入
- 使用`@Lazy`延迟注入

### Q2: FactoryBean和普通Bean的区别？

**A**:
- **普通Bean**：直接返回bean实例
- **FactoryBean**：返回`getObject()`方法创建的对象
- 获取FactoryBean本身需要在名称前加`&`

### Q3: 什么是早期引用（early reference）？

**A**: 早期引用是指在bean实例化后、属性填充前暴露的bean引用。用于解决循环依赖，让其他bean可以引用到这个还未完全初始化的bean。

### Q4: 为什么需要`singletonFactories`这个三级缓存？

**A**: 为了延迟创建代理对象。如果bean需要AOP代理，`ObjectFactory`可以在需要时才创建代理，避免过早创建不必要的代理。

### Q5: `allowCircularReferences`属性有什么用？

**A**: 控制是否允许循环依赖。设为`false`可以禁止循环依赖，帮助发现设计问题。

---

## 扩展阅读

### 下一阶段预告

阶段4将学习依赖注入与自动装配：
- @Autowired处理机制
- AutowiredAnnotationBeanPostProcessor
- 依赖解析过程

### 推荐阅读源码

1. `DefaultSingletonBeanRegistry` - 单例注册表实现
2. `AbstractBeanFactory` - BeanFactory抽象实现
3. `FactoryBean` - 工厂Bean接口

---

## 总结

完成阶段3后，你应该能够：

1. ✅ 理解DefaultListableBeanFactory的核心结构
2. ✅ 掌握Bean创建的完整流程（6个步骤）
3. ✅ 理解三级缓存解决循环依赖的原理
4. ✅ 能够跟踪getBean的调用链
5. ✅ 理解populateBean和initializeBean的细节

**核心流程记忆**：
```
getBean → doGetBean → createBean → doCreateBean
    → createBeanInstance（实例化）
    → populateBean（填充属性）
    → initializeBean（初始化）
```
