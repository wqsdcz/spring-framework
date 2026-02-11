# Spring-Beans 模块学习计划

## 目录
- [概述](#概述)
- [阶段1：基础概念与架构](#阶段1基础概念与架构)
- [阶段2：Bean定义体系](#阶段2bean定义体系)
- [阶段3：BeanFactory实现原理](#阶段3beanfactory实现原理)
- [阶段4：依赖注入与自动装配](#阶段4依赖注入与自动装配)
- [阶段5：Bean生命周期与扩展点](#阶段5bean生命周期与扩展点)
- [阶段6：配置方式](#阶段6配置方式)
- [阶段7：高级特性与AOT](#阶段7高级特性与aot)
- [每周学习安排](#每周学习安排)
- [配套测试学习](#配套测试学习)
- [学习技巧](#学习技巧)

---

## 概述

Spring-Beans模块是Spring框架的核心，提供了依赖注入（DI）和控制反转（IoC）的基础实现。本学习计划将带你从基础概念到高级特性，系统掌握Spring Beans的工作原理。

**预计学习时间**：4-6周
**源码位置**：`spring-beans/src/main/java/org/springframework/beans/`

---

## 阶段1：基础概念与架构（3-4天）

### 学习目标
理解BeanFactory的核心接口体系和基本使用

### 关键接口

| 接口 | 源码路径 | 学习要点 |
|------|----------|----------|
| BeanFactory | `factory/BeanFactory.java` | 核心方法：getBean, containsBean, isSingleton, isPrototype |
| ListableBeanFactory | `factory/ListableBeanFactory.java` | 枚举bean的能力：getBeanDefinitionNames, getBeansOfType |
| BeanDefinition | `factory/config/BeanDefinition.java` | 元数据：scope, role, initMethod, destroyMethod |

### 实践任务

```java
// 手动创建BeanFactory并注册bean
DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
RootBeanDefinition bd = new RootBeanDefinition(MyService.class);
factory.registerBeanDefinition("myService", bd);
MyService service = factory.getBean(MyService.class);
```

### 检查清单
- [ ] 理解BeanFactory接口的每个方法
- [ ] 了解BeanDefinition中ROLE_APPLICATION/ROLE_SUPPORT/ROLE_INFRASTRUCTURE的区别
- [ ] 能够手动创建和注册bean定义
- [ ] 理解singleton和prototype的区别

---

## 阶段2：Bean定义体系（3-4天）

### 学习目标
掌握BeanDefinition的继承结构和属性配置

### 关键类

| 类 | 源码路径 | 学习要点 |
|------|----------|----------|
| AbstractBeanDefinition | `factory/support/AbstractBeanDefinition.java` | 抽象基类，通用属性定义 |
| RootBeanDefinition | `factory/support/RootBeanDefinition.java` | 最终的合并定义 |
| ChildBeanDefinition | `factory/support/ChildBeanDefinition.java` | 继承父bean的定义 |
| GenericBeanDefinition | `factory/support/GenericBeanDefinition.java` | 通用的标准定义 |
| BeanDefinitionHolder | `factory/config/BeanDefinitionHolder.java` | 包装定义和名称 |

### 类继承关系
```
BeanDefinition (interface)
    ↑
AbstractBeanDefinition (abstract class)
    ↑
    ├── RootBeanDefinition
    ├── ChildBeanDefinition
    └── GenericBeanDefinition
```

### 检查清单
- [ ] 理解AbstractBeanDefinition中的常用属性
- [ ] 掌握父子bean定义的继承规则
- [ ] 了解GenericBeanDefinition vs RootBeanDefinition的使用场景
- [ ] 理解BeanDefinitionHolder的作用

---

## 阶段3：BeanFactory实现原理（5-7天）⭐ 重点

### 学习目标
深入理解BeanFactory的内部工作机制

### 核心实现类

| 类 | 源码路径 | 学习要点 |
|------|----------|----------|
| DefaultSingletonBeanRegistry | `factory/support/DefaultSingletonBeanRegistry.java` | 单例缓存、循环依赖检测 |
| AbstractBeanFactory | `factory/support/AbstractBeanFactory.java` | getBean的完整流程、依赖查找 |
| AbstractAutowireCapableBeanFactory | `factory/support/AbstractAutowireCapableBeanFactory.java` | 创建bean、填充属性、初始化 |
| DefaultListableBeanFactory | `factory/support/DefaultListableBeanFactory.java` | 完整功能的实现、预实例化 |

### 核心流程

```
getBean() → doGetBean() → createBean() → doCreateBean()
    ↓
实例化(Instantiation) → 属性填充(Populate) → 初始化(Initialize)
```

### 关键方法

| 方法 | 所在类 | 作用 |
|------|--------|------|
| `resolveBeforeInstantiation` | AbstractAutowireCapableBeanFactory | 实例化前处理 |
| `createBeanInstance` | AbstractAutowireCapableBeanFactory | 创建实例 |
| `populateBean` | AbstractAutowireCapableBeanFactory | 填充属性 |
| `initializeBean` | AbstractAutowireCapableBeanFactory | 初始化bean |
| `applyBeanPostProcessorsBeforeInitialization` | AbstractAutowireCapableBeanFactory | 初始化前处理 |
| `applyBeanPostProcessorsAfterInitialization` | AbstractAutowireCapableBeanFactory | 初始化后处理 |

### 检查清单
- [ ] 跟踪getBean的完整调用链
- [ ] 理解单例缓存机制
- [ ] 掌握bean创建的三阶段（实例化、填充、初始化）
- [ ] 了解FactoryBean的特殊处理
- [ ] 理解别名解析机制

---

## 阶段4：依赖注入与自动装配（4-5天）

### 学习目标
理解@Autowired和依赖解析机制

### 核心注解

| 注解 | 源码路径 | 说明 |
|------|----------|------|
| @Autowired | `factory/annotation/Autowired.java` | 标记需要自动装配的元素 |
| @Qualifier | `factory/annotation/Qualifier.java` | 限定符，用于精确匹配 |
| @Value | `factory/annotation/Value.java` | 注入配置值或SpEL表达式 |
| @Lookup | `factory/annotation/Lookup.java` | 方法级别注入prototype bean |

### 处理器实现

| 类 | 源码路径 | 作用 |
|------|----------|------|
| AutowiredAnnotationBeanPostProcessor | `factory/annotation/AutowiredAnnotationBeanPostProcessor.java` | 处理@Autowired, @Value, @Inject |
| QualifierAnnotationAutowireCandidateResolver | `factory/annotation/QualifierAnnotationAutowireCandidateResolver.java` | 解析@Qualifier限定符 |

### 注入类型

- **构造器注入**：通过构造器参数自动装配
- **Setter注入**：通过setter方法自动装配
- **字段注入**：直接注入字段

### 检查清单
- [ ] 理解@Autowired的required属性
- [ ] 掌握@Qualifier的使用场景
- [ ] 了解@Value的SpEL表达式支持
- [ ] 跟踪AutowiredAnnotationBeanPostProcessor的处理流程
- [ ] 理解依赖查找和解析的区别

---

## 阶段5：Bean生命周期与扩展点（4-5天）

### 学习目标
掌握Bean的生命周期回调和扩展机制

### 生命周期接口

| 接口 | 源码路径 | 回调时机 |
|------|----------|----------|
| InitializingBean | `factory/InitializingBean.java` | 属性设置后初始化 |
| DisposableBean | `factory/DisposableBean.java` | bean销毁时 |
| BeanPostProcessor | `factory/config/BeanPostProcessor.java` | 初始化前后 |
| InstantiationAwareBeanPostProcessor | `factory/config/InstantiationAwareBeanPostProcessor.java` | 实例化阶段 |
| DestructionAwareBeanPostProcessor | `factory/config/DestructionAwareBeanPostProcessor.java` | 销毁前 |

### 注解处理器

| 类 | 源码路径 | 说明 |
|------|----------|------|
| InitDestroyAnnotationBeanPostProcessor | `factory/annotation/InitDestroyAnnotationBeanPostProcessor.java` | 处理@PostConstruct/@PreDestroy |

### 生命周期时序图

```
1. 实例化前: InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation
2. 实例化: 构造器调用
3. 实例化后: InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation
4. 属性处理: InstantiationAwareBeanPostProcessor.postProcessProperties
5. 属性填充: populateBean
6. 初始化前: BeanPostProcessor.postProcessBeforeInitialization
7. 初始化: InitializingBean.afterPropertiesSet / @PostConstruct / init-method
8. 初始化后: BeanPostProcessor.postProcessAfterInitialization
9. 销毁前: DestructionAwareBeanPostProcessor.postProcessBeforeDestruction
10. 销毁: DisposableBean.destroy / @PreDestroy / destroy-method
```

### Aware接口

| 接口 | 注入内容 |
|------|----------|
| BeanNameAware | bean的名称 |
| BeanClassLoaderAware | 类加载器 |
| BeanFactoryAware | BeanFactory引用 |

### 检查清单
- [ ] 掌握InitializingBean vs @PostConstruct vs init-method
- [ ] 理解BeanPostProcessor的强大作用
- [ ] 了解InstantiationAwareBeanPostProcessor如何控制实例化
- [ ] 掌握DisposableBean的销毁时机
- [ ] 理解Aware接口的注入顺序

---

## 阶段6：配置方式（3-4天）

### 学习目标
了解不同的配置方式实现

### 配置读取器

| 类 | 源码路径 | 说明 |
|------|----------|------|
| XmlBeanDefinitionReader | `factory/xml/XmlBeanDefinitionReader.java` | XML配置解析 |
| BeanDefinitionParserDelegate | `factory/xml/BeanDefinitionParserDelegate.java` | XML元素解析助手 |
| GroovyBeanDefinitionReader | `factory/groovy/GroovyBeanDefinitionReader.java` | Groovy DSL配置 |
| PropertiesBeanDefinitionReader | `factory/support/PropertiesBeanDefinitionReader.java` | 属性文件配置 |

### XML命名空间

| 处理器 | 位置 | 功能 |
|--------|------|------|
| UtilNamespaceHandler | spring-beans | util:list, util:set, util:map等 |
| ContextNamespaceHandler | spring-context | context:component-scan等 |

### 检查清单
- [ ] 了解XmlBeanDefinitionReader的解析流程
- [ ] 理解BeanDefinitionParserDelegate的作用
- [ ] 了解自定义XML命名空间的扩展方式
- [ ] 了解Groovy配置的优势

---

## 阶段7：高级特性与AOT（4-5天）

### 学习目标
了解循环依赖、类型转换和AOT支持

### 循环依赖

| 类 | 源码路径 | 说明 |
|------|----------|------|
| DefaultSingletonBeanRegistry | `factory/support/DefaultSingletonBeanRegistry.java` | 三级缓存解决循环依赖 |

**三级缓存**：
- `singletonObjects`：一级缓存，成品单例
- `earlySingletonObjects`：二级缓存，早期引用
- `singletonFactories`：三级缓存，单例工厂

### 类型转换

| 包/类 | 位置 | 说明 |
|--------|------|------|
| PropertyEditors | `propertyeditors/` 包 | 各种PropertyEditor实现 |
| BeanWrapper | `BeanWrapper.java` | JavaBeans操作接口 |
| BeanWrapperImpl | `BeanWrapperImpl.java` | BeanWrapper实现 |

### AOT支持（GraalVM原生镜像）

| 类 | 源码路径 | 说明 |
|------|----------|------|
| BeanRegistrationAotProcessor | `factory/aot/BeanRegistrationAotProcessor.java` | 生成bean注册代码 |
| BeanFactoryInitializationAotProcessor | `factory/aot/BeanFactoryInitializationAotProcessor.java` | 生成初始化代码 |
| BeanInstanceSupplier | `factory/aot/BeanInstanceSupplier.java` | 运行时bean供应 |

### 检查清单
- [ ] 理解循环依赖的检测和解决机制
- [ ] 了解PropertyEditor的作用
- [ ] 掌握BeanWrapper的使用
- [ ] 了解AOT的基本概念和作用

---

## 每周学习安排

### 第1周：基础与定义
- **Day 1-2**：阶段1 - 基础概念与架构
- **Day 3-4**：阶段2 - Bean定义体系
- **Day 5-7**：阅读测试代码，编写练习

### 第2周：核心实现 ⭐
- **Day 1-3**：阶段3 - 阅读DefaultListableBeanFactory
- **Day 4-5**：跟踪getBean调用链
- **Day 6-7**：画流程图，整理笔记

### 第3周：依赖注入与生命周期
- **Day 1-2**：阶段4 - 依赖注入与自动装配
- **Day 3-4**：阶段5 - Bean生命周期与扩展点
- **Day 5-7**：实践：自定义BeanPostProcessor

### 第4周：配置与高级特性
- **Day 1-2**：阶段6 - 配置方式
- **Day 3-4**：阶段7 - 高级特性与AOT
- **Day 5-7**：综合复习，查漏补缺

---

## 配套测试学习

| 测试类 | 位置 | 说明 |
|--------|------|------|
| DefaultListableBeanFactoryTests | `src/test/java/.../factory/support/` | BeanFactory核心测试 |
| AutowiredAnnotationBeanPostProcessorTests | `src/test/java/.../factory/annotation/` | 自动装配测试 |
| BeanLifecycleTests | `src/test/java/.../factory/` | 生命周期测试 |
| XmlBeanDefinitionReaderTests | `src/test/java/.../factory/xml/` | XML解析测试 |
| CircularReferenceTests | `src/test/java/.../factory/support/` | 循环依赖测试 |

### 阅读测试代码的方法

1. 找到对应功能的测试类
2. 从简单测试方法开始阅读
3. 使用IDE调试功能跟踪执行
4. 修改测试代码观察不同行为

---

## 学习技巧

### 1. 从接口到实现
- 先读接口定义理解契约
- 再读实现类理解具体逻辑
- 对比不同实现的差异

### 2. 画图辅助
- 画UML类图理解类关系
- 画时序图理解调用流程
- 画思维导图整理知识点

### 3. 调试跟踪
- 在关键方法打断点
- 使用IDE的调用栈查看
- 观察变量变化过程

### 4. 对比学习
- 对比Root/Child/GenericBeanDefinition
- 对比各种Aware接口
- 对比不同配置方式

### 5. 关注注释
Spring源码注释非常详细，特别是：
- 接口的JavaDoc
- 复杂方法的注释
- 常量的说明

### 6. 做笔记
- 记录关键流程
- 整理类关系图
- 总结自己的理解

---

## 推荐学习资源

1. **官方文档**：Spring Framework Reference Documentation
2. **书籍**：
   - 《Spring源码深度解析》
   - 《Spring技术内幕》
3. **博客**：Spring官方博客和核心开发者博客

---

## 常见问题

### Q: 为什么要学习spring-beans？
A: spring-beans是Spring框架的基石，掌握它有助于理解：
- 依赖注入的实现原理
- Spring的扩展机制
- 其他模块如何基于beans构建

### Q: 学习过程中遇到看不懂的代码怎么办？
A:
1. 先跳过，继续往后看
2. 查看相关的测试代码
3. 使用IDE的调用层次结构
4. 查阅官方文档或社区资源

### Q: 需要先把所有代码看完吗？
A: 不需要。建议：
1. 先掌握核心流程
2. 再根据需要深入特定功能
3. 重复阅读，每次聚焦不同方面

---

祝你学习顺利！如有疑问，可以在代码中添加注释或写学习笔记来加深理解。
