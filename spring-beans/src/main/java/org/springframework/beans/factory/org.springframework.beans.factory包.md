# Aware系列接口
Aware 是一个 **标记超级接口**（marker superinterface），本身没有任何方法。它指示实现类有资格通过回调式方法从 Spring 容器接收特定的框架对象。

核心思想：让 Bean "感知"（Aware）到它所处的容器环境和资源。

Aware 接口需要显式处理（如 BeanPostProcessor 回调 setter 方法注入框架对象）才会生效。

#### Aware 的子接口
- BeanNameAware：
  - 用途：让 Bean 知道自己注册的名称
  - 触发时机：属性填充后、初始化回调前
  - 注意：内部 bean 名称可能带有 #1 后缀，可用 BeanFactoryUtils.originalBeanName() 获取原始名称

- BeanClassLoaderAware
  - 用途：获取加载 bean 类的 ClassLoader
  - 触发时机：属性填充后、初始化回调前
  - 适用场景：框架类需要通过名称动态加载应用类

- BeanFactoryAware
  - 用途：获取当前 BeanFactory 引用
  - 触发时机：属性填充后、初始化回调前
  - 功能：可进行依赖查找（Dependency Lookup）
  - 对比：推荐优先使用依赖注入（DI）而非直接访问 BeanFactory

#### 生命周期中的触发顺序

1. 实例化 Bean
2. 属性填充（依赖注入@Autowired、@Resource、XML <property> 等）
3. Aware 接口回调
   1. 触发 BeanNameAware  ->  触发 BeanClassLoaderAware  ->  触发 BeanFactoryAware  ->  触发 EnvironmentAware  ->  触发 EmbeddedValueResolverAware 
   2. 触发 ResourceLoaderAware  ->  触发 ApplicationEventPublisherAware  ->  触发 MessageSourceAware  ->  触发 ApplicationContextAware
   3. 触发 ServletContextAware 
4. 触发 BeanPostProcessor Before 
5. 触发 初始化回调 （InitializingBean、init-method方法） 
6. 触发 BeanPostProcessor After  

#### 总结

Aware 系列接口是 Spring 依赖注入的补充机制，允许 Bean 在必要时主动获取容器资源。它是 Spring 灵活扩展点之一，体现了"好莱坞原则"（Don't call us, we'll call you）——容器在适当时机回调 Bean，让 Bean 感知到所需的环境信息。

# InitializingBean 和 DisposableBean 接口

InitializingBean 和 DisposableBean 是 Spring 提供的接口，用于在 Bean 初始化和销毁时执行一些额外的逻辑。

这两个接口构成了 Spring Bean 生命周期的首尾呼应——一个负责出生时的准备，一个负责临终时的善后，确保应用稳定运行和优雅关闭。

1. InitializingBean - 初始化回调

   在 BeanFactory 完成所有属性填充、执行完Aware 接口回调后，执行自定义初始化逻辑，如：
   - 验证必需属性是否已正确设置
   - 执行资源初始化（数据库连接、缓存预热等）
   - 启动后台线程或定时任务
   - 计算派生值
  
   替换方案：
   - @PostConstruct 标记的方法 （推荐使用）
   - 自定义的初始化方法（XML 或 @Bean(initMethod = "")）
   
   组合使用时的执行顺序：
   1. @PostConstruct 标记的方法
   2. InitializingBean 的回调方法
   3. 自定义的初始化方法（XML 或 @Bean(initMethod = "")）

2. DisposableBean - 销毁回调

   在 Bean 被销毁时，释放资源，防止内存泄漏，如：
   - 关闭数据库连接
   - 释放文件句柄
   - 停止后台线程/定时任务
   - 清理临时文件
   - 注销事件监听器
   
   替换方案：
   - @PreDestroy 标记的方法 （推荐使用）
   - 自定义的销毁方法（XML 或 @Bean(destroyMethod = "")）

   组合使用时的执行顺序：
   1. @PreDestroy 标记的方法 （推荐使用）
   2. DisposableBean 的回调方法
   3. 自定义的销毁方法（XML 或 @Bean(destroyMethod = "")）

#### 注意事项

- **作用域限制**：DisposableBean 只对单例和作用域 Bean 有效，原型 Bean 不触发销毁
- **异常处理**：destroy() 中的异常会被记录，但不传播，确保其他 Bean 也能释放资源
- **幂等性**：实现应保证多次调用不会出错（防御性编程）
- **超时处理**：销毁操作应尽快完成，避免阻塞容器关闭 

# BeanFactoryInitializer 接口

在进入单例预实例化阶段之前，对 ListableBeanFactory 进行自定义初始化。

典型场景：需要在常规单例 bean 之前，提前触发某些特定 bean 的初始化。

**触发时机与生命周期对比：**

1. 加载 BeanDefinition（加载配置、扫描组件） 
2. 【触发】BeanFactoryInitializer.initialize()  ←  在此处插入
3. 单例预实例化阶段（preInstantiateSingletons） ←  实例化所有非延迟单例 bean 
4. SmartInitializingSingleton.afterSingletonsInstantiated()
5. 容器就绪，开始服务

**与其他初始化机制的对比：**
BeanFactoryInitializer      →  （单例预实例化之前）  →  BeanFactory层面的初始化、早期 bean 实例化
SmartInitializingSingleton  →  （单例预实例化之后）  →  所有单例创建完成后的回调
InitializingBean            →  （单个 bean 属性填充后）  →  单个 bean 的初始化
@PostConstruct              →  （单个 bean 属性填充后）  →  单个 bean 的初始化

#### 典型应用场景

1. 提前初始化基础设施 Bean
2. 动态修改 BeanDefinition
3. 注册额外的 BeanPostProcessor

#### 注意事项

1. 执行次数：每个初始化器通常只执行一次（在工厂生命周期早期）
2. 幂等性：实现应保证多次调用不会出错（防御性编程）
3. 延迟初始化影响：此回调在预实例化之前，此时 @Lazy 标记的 bean 尚未创建
4. Bean 可用性：在此阶段，只能安全访问非延迟单例的定义，不能假设其他 bean 已实例化
5. ApplicationContext 自动检测：在纯 BeanFactory 环境中需手动调用，在 ApplicationContext 中才会自动检测

#### 总结

BeanFactoryInitializer 是 Spring 6.2 引入的一个早期介入点，允许开发者在单例预实例化之前对 BeanFactory 进行自定义配置或触发特定 bean 的初始化。它是 Spring
生命周期管理工具箱中的新成员，填补了"工厂准备就绪"到"单例开始创建"之间的空白，适用于需要提前准备基础设施的场景。

# SmartInitializingSingleton 接口

在单例预实例化阶段结束时触发，此时保证所有常规单例 Bean 已经被创建。
关键价值：可以安全地访问其他单例 Bean，而不会触发意外的早期初始化副作用。

**触发时机与BeanFactory 启动流程：**
1. 加载 BeanDefinition（加载配置、扫描组件）
2. BeanFactoryInitializer 回调（可选）
3. 【单例预实例化阶段】preInstantiateSingletons()
    - 实例化所有非延迟单例
    - 填充属性、执行初始化
4. 【触发】SmartInitializingSingleton.afterSingletonsInstantiated()  ← 所有单例已就绪，可以安全地获取其他 bean
5. 容器就绪

**与 InitializingBean 的关键区别：**
1. InitializingBean 是单个 bean 构造完成时触发；SmartInitializingSingleton 是所有单例 bean 创建完成后触发。
2. InitializingBean 是只能看到自己；SmartInitializingSingleton 可以看到所有单例 bean。
3. InitializingBean 适用于单个 Bean的自初始化；SmartInitializingSingleton 适用于跨多个 Bean 的协调初始化。

#### 总结

SmartInitializingSingleton 是 Spring 提供的**"最后的机会"回调**——在所有单例 Bean 创建完成后、容器正式就绪前，执行一些跨 Bean 的协调工作。
它解决了 InitializingBean 中无法安全访问其他 Bean 的问题，是构建需要全局视野的初始化逻辑的理想选择。

# NamedBean 接口

返回对象在 Spring BeanFactory 中的名称。

与 BeanNameAware 的关系：NamedBean 是 BeanNameAware 的对应接口（Counterpart）。
  - BeanNameAware：容器 → setBeanName() → Bean（被动接收）
  - NamedBean：Bean → getBeanName() → 调用者（主动暴露）

# ObjectProvider 与 ObjectFactory 接口

ObjectFactory 核心的作用是延迟获取对象实例。
ObjectProvider 是 ObjectFactory 的专门化变体，为注入点设计，提供：
- 程序化可选性：可选择性获取对象
- 宽松的不唯一处理：优雅处理多个候选者的情况
- Stream 支持：集合式访问匹配的对象
- 默认实现：所有方法都有默认实现（6.2+）











