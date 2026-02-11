# 阶段2：Bean定义体系 - 详细学习指导

## 学习目标

1. 理解BeanDefinition的继承体系
2. 掌握AbstractBeanDefinition的公共属性
3. 区分RootBeanDefinition、ChildBeanDefinition、GenericBeanDefinition的使用场景
4. 理解BeanDefinition的合并机制

预计学习时间：3-4天

---

## Day 1: AbstractBeanDefinition 抽象基类

### 核心文件
`spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractBeanDefinition.java`

### 类定位
```java
/**
 * Base class for concrete, full-fledged {@link BeanDefinition} classes,
 * factoring out common properties of {@link GenericBeanDefinition},
 * {@link RootBeanDefinition}, and {@link ChildBeanDefinition}.
 */
```
**理解**：AbstractBeanDefinition是BeanDefinition具体实现的抽象基类，抽取了GenericBeanDefinition、RootBeanDefinition和ChildBeanDefinition的公共属性。

### 自动装配常量（第73-101行）

```java
public static final int AUTOWIRE_NO = 0;           // 不自动装配
public static final int AUTOWIRE_BY_NAME = 1;      // 按名称自动装配
public static final int AUTOWIRE_BY_TYPE = 2;      // 按类型自动装配
public static final int AUTOWIRE_CONSTRUCTOR = 3;  // 按构造器自动装配
@Deprecated
public static final int AUTOWIRE_AUTODETECT = 4;   // 自动检测（已废弃）
```

**说明**：这些常量与XML配置中的`autowire`属性对应：
```xml
<bean id="userService" class="com.example.UserService" autowire="byName"/>
<!-- 对应 AUTOWIRE_BY_NAME -->
```

### 依赖检查常量（第107-127行）

```java
public static final int DEPENDENCY_CHECK_NONE = 0;     // 不检查
public static final int DEPENDENCY_CHECK_OBJECTS = 1;  // 检查对象引用
public static final int DEPENDENCY_CHECK_SIMPLE = 2;   // 检查简单属性
public static final int DEPENDENCY_CHECK_ALL = 3;      // 检查所有属性
```

### 核心字段（第168-200行）

| 字段 | 类型 | 说明 |
|------|------|------|
| `beanClass` | Object | Bean的类（可能是Class对象或类名字符串） |
| `scope` | String | 作用域（singleton/prototype等） |
| `abstractFlag` | boolean | 是否是抽象bean定义 |
| `lazyInit` | Boolean | 是否延迟初始化 |
| `autowireMode` | int | 自动装配模式（0-4） |
| `dependencyCheck` | int | 依赖检查模式（0-3） |
| `dependsOn` | String[] | 依赖的其他bean名称 |
| `autowireCandidate` | boolean | 是否作为自动装配候选 |
| `primary` | boolean | 是否是主要的 |
| `fallback` | boolean | 是否是备用的 |
| `instanceSupplier` | Supplier | 实例提供者（Java 8函数式） |

### 特殊常量

#### 推断方法名（第165行）
```java
public static final String INFER_METHOD = "(inferred)";
```
用于指示容器应该推断销毁方法名（如"close"、"shutdown"）。

#### 优先构造器属性（第141行）
```java
public static final String PREFERRED_CONSTRUCTORS_ATTRIBUTE = "preferredConstructors";
```
存储优先使用的构造器，类似`@Autowired`注解的构造器。

#### 排序属性（第153行）
```java
public static final String ORDER_ATTRIBUTE = "order";
```
类似`@Order`注解，用于指定bean的排序。

### 关键方法

#### 验证方法：`validate()`
验证bean定义是否有效，例如：
- 检查bean类是否设置
- 检查工厂方法和bean类的有效性

#### 克隆方法：`cloneBeanDefinition()`
抽象方法，由子类实现深拷贝。

### 今日实践任务

1. 查看AbstractBeanDefinition的所有字段，理解每个字段的作用
2. 对比自动装配常量和依赖检查常量
3. 思考：为什么`autowireMode`和`dependencyCheck`使用int而不是enum？

---

## Day 2: RootBeanDefinition 根定义

### 核心文件
`spring-beans/src/main/java/org/springframework/beans/factory/support/RootBeanDefinition.java`

### 类定位
```java
/**
 * A root bean definition represents the <b>merged bean definition at runtime</b>
 * that backs a specific bean in a Spring BeanFactory. It might have been created
 * from multiple original bean definitions that inherit from each other, for example,
 * {@link GenericBeanDefinition GenericBeanDefinitions} from XML declarations.
 * A root bean definition is essentially the 'unified' bean definition view at runtime.
 */
```

**核心概念**：
- RootBeanDefinition代表**运行时的合并bean定义**
- 可能由多个原始bean定义（继承关系）合并而成
- 是运行时的"统一"bean定义视图

### 使用场景

```java
/**
 * Root bean definitions may also be used for <b>registering individual bean
 * definitions in the configuration phase.</b> This is particularly applicable for
 * programmatic definitions derived from factory methods (for example, {@code @Bean} methods)
 * and instance suppliers (for example, lambda expressions)
 */
```

**两种使用方式**：
1. **运行时合并**：从父子定义合并而来
2. **编程式注册**：用于`@Bean`方法、lambda表达式等

### 核心字段（第66-136行）

| 字段 | 类型 | 说明 |
|------|------|------|
| `decoratedDefinition` | BeanDefinitionHolder | 装饰的定义 |
| `qualifiedElement` | AnnotatedElement | 限定的元素（用于泛型推断） |
| `stale` | boolean | 定义是否需要重新合并 |
| `targetType` | ResolvableType | 目标类型 |
| `resolvedTargetType` | Class | 解析后的目标类型 |
| `isFactoryBean` | Boolean | 是否是FactoryBean |
| `resolvedConstructorOrFactoryMethod` | Executable | 解析的构造器或工厂方法 |
| `constructorArgumentsResolved` | boolean | 构造参数是否已解析 |
| `resolvedConstructorArguments` | Object[] | 解析后的构造参数 |
| `postProcessed` | boolean | MergedBeanDefinitionPostProcessor是否已应用 |

### 缓存机制

RootBeanDefinition包含大量**volatile**字段用于缓存：
- `resolvedTargetType` - 缓存解析的类
- `isFactoryBean` - 缓存是否是FactoryBean的判断
- `factoryMethodReturnType` - 缓存工厂方法返回类型
- `resolvedConstructorOrFactoryMethod` - 缓存解析的构造器

**理解**：这些缓存字段是为了提高性能，避免重复反射计算。

### 构造器参数锁（第102行）
```java
final Object constructorArgumentLock = new Object();
```
用于同步构造器参数的解析和访问。

### 今日实践任务

1. 理解为什么RootBeanDefinition需要缓存机制
2. 对比RootBeanDefinition和AbstractBeanDefinition的字段差异
3. 思考：`stale`字段在什么情况下会被设置为true？

---

## Day 3: GenericBeanDefinition 通用定义

### 核心文件
`spring-beans/src/main/java/org/springframework/beans/factory/support/GenericBeanDefinition.java`

### 类定位
```java
/**
 * GenericBeanDefinition is a one-stop shop for declarative bean definition purposes.
 * Like all common bean definitions, it allows for specifying a class plus optionally
 * constructor argument values and property values. Additionally, deriving from a
 * parent bean definition can be flexibly configured through the "parentName" property.
 */
```

**核心特点**：
- **一站式**：声明式bean定义的首选
- **灵活配置parent**：通过`parentName`属性动态配置父定义

### 源码结构（完整）

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

    @Override
    public AbstractBeanDefinition cloneBeanDefinition() {
        return new GenericBeanDefinition(this);
    }

    @Override
    public String toString() {
        if (this.parentName != null) {
            return "Generic bean with parent '" + this.parentName + "': " + super.toString();
        }
        return "Generic bean: " + super.toString();
    }
}
```

**极简设计**：GenericBeanDefinition只增加了一个`parentName`字段！

### 使用建议对比

```java
/**
 * In general, use this {@code GenericBeanDefinition} class for the purpose of
 * registering declarative bean definitions (for example, XML definitions which a bean
 * post-processor might operate on, potentially even reconfiguring the parent name).
 * Use {@code RootBeanDefinition}/{@code ChildBeanDefinition} where parent/child
 * relationships happen to be pre-determined, and prefer {@link RootBeanDefinition}
 * specifically for programmatic definitions derived from factory methods/suppliers.
 */
```

| 场景 | 推荐类 |
|------|--------|
| XML声明式定义 | GenericBeanDefinition |
| 需要动态修改parent | GenericBeanDefinition |
| 预确定的父子关系 | RootBeanDefinition/ChildBeanDefinition |
| 编程式定义（@Bean、lambda） | RootBeanDefinition |

### 今日实践任务

1. 理解GenericBeanDefinition为什么如此简单
2. 对比GenericBeanDefinition和RootBeanDefinition的使用场景
3. 思考：为什么XML解析使用GenericBeanDefinition而不是RootBeanDefinition？

---

## Day 4: BeanDefinition合并机制与实践

### BeanDefinition继承体系

```
BeanDefinition (interface)
    ↑
AbstractBeanDefinition (abstract class)
    ↑
    ├── RootBeanDefinition
    ├── ChildBeanDefinition (已逐渐废弃)
    └── GenericBeanDefinition
```

### 合并流程

当存在父子bean定义时，Spring会进行**合并**：

```
GenericBeanDefinition (child)
    ↓ 继承parent属性
GenericBeanDefinition (parent)
    ↓ 合并为
RootBeanDefinition (merged)
```

**合并规则**：
1. 子定义覆盖父定义的同名属性
2. 子定义继承父定义未设置的属性
3. 最终生成RootBeanDefinition作为运行时定义

### 编程式创建BeanDefinition

```java
// 1. 使用GenericBeanDefinition（推荐用于声明式场景）
GenericBeanDefinition gbd = new GenericBeanDefinition();
gbd.setBeanClass(UserService.class);
gbd.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
gbd.setParentName("baseService");  // 可以动态设置parent

// 2. 使用RootBeanDefinition（推荐用于编程式场景）
RootBeanDefinition rbd = new RootBeanDefinition(UserService.class);
rbd.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);

// 3. 从@Bean方法创建（Spring内部使用）
RootBeanDefinition rbd2 = new RootBeanDefinition(userConfigClass);
rbd2.setFactoryMethodName("userService");
```

### 属性继承示例

```java
// 父定义
GenericBeanDefinition parent = new GenericBeanDefinition();
parent.setBeanClass(BaseService.class);
parent.setScope("singleton");
parent.setAbstract(true);
parent.getPropertyValues().add("timeout", 5000);

// 子定义
GenericBeanDefinition child = new GenericBeanDefinition();
child.setParentName("baseService");
child.setBeanClass(UserService.class);
// child继承了timeout=5000
```

### 实践任务：创建完整的BeanDefinition示例

```java
package com.example.beans.practice;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

public class BeanDefinitionPractice {

    public static void main(String[] args) {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        // 1. 创建抽象的父定义
        GenericBeanDefinition parentDef = new GenericBeanDefinition();
        parentDef.setBeanClass(BaseService.class);
        parentDef.setAbstract(true);
        parentDef.getPropertyValues().add("timeout", 5000);
        parentDef.getPropertyValues().add("retryCount", 3);
        factory.registerBeanDefinition("baseService", parentDef);

        // 2. 创建子定义，继承父定义
        GenericBeanDefinition childDef = new GenericBeanDefinition();
        childDef.setBeanClass(UserService.class);
        childDef.setParentName("baseService");
        // 覆盖parent的属性
        childDef.getPropertyValues().add("retryCount", 5);
        factory.registerBeanDefinition("userService", childDef);

        // 3. 使用RootBeanDefinition创建独立的bean
        RootBeanDefinition standaloneDef = new RootBeanDefinition(OrderService.class);
        standaloneDef.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);
        standaloneDef.setInitMethodName("init");
        factory.registerBeanDefinition("orderService", standaloneDef);

        // 4. 获取bean并验证
        UserService userService = factory.getBean(UserService.class);
        System.out.println("UserService timeout: " + userService.getTimeout());  // 5000
        System.out.println("UserService retryCount: " + userService.getRetryCount());  // 5

        // 5. 获取合并后的BeanDefinition
        RootBeanDefinition mergedDef = factory.getMergedBeanDefinition("userService");
        System.out.println("Merged definition class: " + mergedDef.getBeanClassName());
    }
}

class BaseService {
    private int timeout;
    private int retryCount;
    // getters and setters
    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
}

class UserService extends BaseService {
    // 继承BaseService
}

class OrderService {
    public void init() {
        System.out.println("OrderService initialized");
    }
}
```

### 检查清单

- [ ] 理解AbstractBeanDefinition的公共属性
- [ ] 理解RootBeanDefinition的缓存机制
- [ ] 理解GenericBeanDefinition的简洁设计
- [ ] 掌握父子bean定义的继承规则
- [ ] 能够根据场景选择合适的BeanDefinition类型

---

## 常见问题

### Q1: 为什么需要RootBeanDefinition和GenericBeanDefinition两种类？

**A**:
- **GenericBeanDefinition**：用于声明式配置阶段，支持动态设置parent
- **RootBeanDefinition**：
  - 用于运行时，表示合并后的最终定义
  - 用于编程式定义（如`@Bean`方法）
  - 包含更多运行时缓存信息

### Q2: ChildBeanDefinition还存在吗？

**A**: ChildBeanDefinition仍然存在，但已经**逐渐废弃**。GenericBeanDefinition提供了更灵活的方式（通过`parentName`属性），可以动态改变parent关系。

### Q3: 什么是合并后的BeanDefinition？

**A**: 当存在父子继承关系时，Spring会将父定义和子定义合并成一个**RootBeanDefinition**。子定义覆盖父定义的同名属性，继承父定义未设置的属性。

### Q4: abstract=true的BeanDefinition有什么特点？

**A**:
- 不能被实例化（getBean会报错）
- 只能作为父定义被继承
- 用于提取公共配置

### Q5: instanceSupplier是什么？

**A**: `instanceSupplier`是Java 8的`Supplier`函数式接口，用于提供bean实例的创建逻辑。例如：
```java
RootBeanDefinition rbd = new RootBeanDefinition();
rbd.setInstanceSupplier(() -> new UserService());
```
这种方式比反射创建实例更灵活、性能更好。

---

## 扩展阅读

### 下一阶段预告

阶段3将深入BeanFactory的实现原理：
- DefaultListableBeanFactory - 完整功能的BeanFactory
- AbstractBeanFactory - 模板方法模式
- AbstractAutowireCapableBeanFactory - 自动装配能力
- Bean创建的完整流程

### 推荐阅读源码

1. `BeanDefinitionReaderUtils` - 创建BeanDefinition的实用方法
2. `ConfigurationClassBeanDefinition` - `@Bean`方法的特殊实现
3. `AnnotatedGenericBeanDefinition` - 注解配置的BeanDefinition

---

## 总结

完成阶段2后，你应该能够：

1. ✅ 理解BeanDefinition的继承体系
2. ✅ 掌握AbstractBeanDefinition的公共属性
3. ✅ 区分RootBeanDefinition和GenericBeanDefinition的使用场景
4. ✅ 理解BeanDefinition的合并机制
5. ✅ 能够编程式创建和配置BeanDefinition

**关键记忆点**：
- GenericBeanDefinition = 声明式、灵活、可改parent
- RootBeanDefinition = 运行时、合并后、有缓存
- AbstractBeanDefinition = 公共属性抽取
