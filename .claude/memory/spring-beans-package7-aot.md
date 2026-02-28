# Spring AOT (Ahead-of-Time) 编译支持详解

## 目录

1. [Spring AOT 概述](#1-spring-aot-概述)
2. [AOT 处理器架构](#2-aot-处理器架构)
3. [代码生成机制](#3-代码生成机制)
4. [代码片段生成](#4-代码片段生成)
5. [AOT 服务与扩展](#5-aot-服务与扩展)
6. [AOT 与传统 BeanFactory 的关系](#6-aot-与传统-beanfactory-的关系)

---

## 1. Spring AOT 概述

### 1.1 什么是 AOT 编译

AOT (Ahead-of-Time) 编译是 Spring Framework 6.0 引入的核心特性，主要用于支持 **GraalVM 原生镜像 (Native Image)**。传统的 Spring 应用依赖运行时反射和动态代理，这在原生镜像中面临挑战，因为：

- 反射需要在构建时预先配置
- 动态类加载受限
- 启动时的类路径扫描耗时

### 1.2 AOT 的核心目标

```
┌─────────────────────────────────────────────────────────────┐
│                    传统 Spring 启动流程                       │
├─────────────────────────────────────────────────────────────┤
│  1. 扫描类路径 → 2. 解析配置类 → 3. 创建 BeanDefinition      │
│  4. 实例化 Bean → 5. 依赖注入 → 6. 初始化                      │
│                                                             │
│  问题：启动慢，反射多，不适合原生镜像                          │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    AOT 处理流程                              │
├─────────────────────────────────────────────────────────────┤
│  构建时：                                                     │
│  1. 分析 BeanDefinition → 2. 生成 Java 源代码                │
│  3. 注册运行时提示 (Runtime Hints)                           │
│                                                             │
│  运行时：                                                     │
│  1. 直接执行生成的代码 → 2. 无需反射扫描                       │
│                                                             │
│  优势：启动快，无反射，完美支持原生镜像                        │
└─────────────────────────────────────────────────────────────┘
```

### 1.3 包结构

```
org.springframework.beans.factory.aot
├── AOT 处理器接口
│   ├── BeanRegistrationAotProcessor          # Bean 注册 AOT 处理器
│   ├── BeanFactoryInitializationAotProcessor # BeanFactory 初始化 AOT 处理器
│   └── BeanRegistrationExcludeFilter         # Bean 排除过滤器
├── AOT 贡献接口
│   ├── BeanRegistrationAotContribution       # Bean 注册贡献
│   ├── BeanFactoryInitializationAotContribution
│   └── BeanRegistrationsAotContribution      # 批量 Bean 注册贡献
├── 代码生成器
│   ├── BeanDefinitionMethodGenerator         # BeanDefinition 方法生成器
│   ├── BeanDefinitionMethodGeneratorFactory  # 生成器工厂
│   ├── InstanceSupplierCodeGenerator         # 实例提供者代码生成器
│   ├── AutowiredArgumentsCodeGenerator       # 自动装配参数代码生成器
│   └── BeanDefinitionPropertiesCodeGenerator # BeanDefinition 属性代码生成器
├── 代码片段
│   ├── BeanRegistrationCodeFragments         # 代码片段接口
│   ├── DefaultBeanRegistrationCodeFragments  # 默认实现
│   └── BeanRegistrationCodeFragmentsDecorator # 装饰器基类
├── 运行时支持
│   ├── BeanInstanceSupplier                  # Bean 实例提供者
│   ├── AutowiredArguments                    # 自动装配参数
│   └── AutowiredElementResolver              # 自动装配元素解析器
└── 服务加载
    └── AotServices                           # AOT 服务加载工具
```

---

## 2. AOT 处理器架构

### 2.1 BeanRegistrationAotProcessor - Bean 注册 AOT 处理器

**源码位置**: `spring-beans/src/main/java/org/springframework/beans/factory/aot/BeanRegistrationAotProcessor.java`

```java
@FunctionalInterface
public interface BeanRegistrationAotProcessor {

    /**
     * 用于标记 BeanDefinition 应该被忽略的属性名
     * @since 6.2
     */
    String IGNORE_REGISTRATION_ATTRIBUTE = "aotProcessingIgnoreRegistration";

    /**
     * 在构建时处理 RegisteredBean，返回贡献或 null
     *
     * @param registeredBean 已注册的 Bean
     * @return BeanRegistrationAotContribution 或 null
     */
    @Nullable
    BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean);

    /**
     * 是否将处理器本身排除在 AOT 处理之外
     * 默认返回 true，因为处理器通常不需要在运行时注册
     */
    default boolean isBeanExcludedFromAotProcessing() {
        return true;
    }
}
```

**核心作用**：
- 在构建时分析单个 Bean 的定义
- 生成替代的运行时代码
- 通过 `BeanRegistrationAotContribution` 贡献生成的代码

**重要注意事项**：
```java
/**
 * 使用此接口会导致 Bean 及其所有依赖在 AOT 处理期间被初始化。
 * 建议仅用于基础设施 Bean（如 BeanPostProcessor），因为它们：
 * 1. 依赖有限
 * 2. 在 BeanFactory 生命周期早期就已初始化
 * 3. 如果使用工厂方法注册，应确保为 static，避免初始化 enclosing 类
 */
```

### 2.2 BeanFactoryInitializationAotProcessor - BeanFactory 初始化 AOT 处理器

**源码位置**: `spring-beans/src/main/java/org/springframework/beans/factory/aot/BeanFactoryInitializationAotProcessor.java`

```java
@FunctionalInterface
public interface BeanFactoryInitializationAotProcessor {

    /**
     * 处理整个 BeanFactory，返回初始化贡献
     *
     * @param beanFactory 要处理的 BeanFactory
     * @return BeanFactoryInitializationAotContribution 或 null
     */
    @Nullable
    BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory);
}
```

**与 BeanRegistrationAotProcessor 的区别**：

| 特性 | BeanRegistrationAotProcessor | BeanFactoryInitializationAotProcessor |
|------|------------------------------|---------------------------------------|
| 处理粒度 | 单个 Bean | 整个 BeanFactory |
| 使用场景 | Bean 级别的代码生成 | 全局初始化代码生成 |
| 典型实现 | 处理特定 Bean 的依赖注入 | 批量注册所有 Bean |

### 2.3 BeanRegistrationsAotProcessor - 核心实现

**源码位置**: `spring-beans/src/main/java/org/springframework/beans/factory/aot/BeanRegistrationsAotProcessor.java`

```java
class BeanRegistrationsAotProcessor implements BeanFactoryInitializationAotProcessor {

    @Override
    @Nullable
    public BeanRegistrationsAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        // 1. 创建 BeanDefinition 方法生成器工厂
        BeanDefinitionMethodGeneratorFactory beanDefinitionMethodGeneratorFactory =
                new BeanDefinitionMethodGeneratorFactory(beanFactory);

        List<Registration> registrations = new ArrayList<>();

        // 2. 遍历所有 BeanDefinition
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            RegisteredBean registeredBean = RegisteredBean.of(beanFactory, beanName);
            BeanDefinitionMethodGenerator beanDefinitionMethodGenerator =
                    beanDefinitionMethodGeneratorFactory.getBeanDefinitionMethodGenerator(registeredBean);
            if (beanDefinitionMethodGenerator != null) {
                registrations.add(new Registration(registeredBean, beanDefinitionMethodGenerator,
                        beanFactory.getAliases(beanName)));
            }
        }

        // 3. 返回贡献（如果存在需要注册的 Bean）
        if (registrations.isEmpty()) {
            return null;
        }
        return new BeanRegistrationsAotContribution(registrations);
    }
}
```

**处理流程解析**：

```
┌─────────────────────────────────────────────────────────────┐
│              BeanRegistrationsAotProcessor 处理流程          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 创建 BeanDefinitionMethodGeneratorFactory               │
│     └── 加载所有 BeanRegistrationAotProcessor               │
│     └── 加载所有 BeanRegistrationExcludeFilter              │
│                                                             │
│  2. 遍历 BeanFactory 中所有 BeanDefinition                  │
│     └── 为每个 Bean 创建 RegisteredBean 包装               │
│     └── 通过工厂获取 BeanDefinitionMethodGenerator         │
│     └── 检查是否被排除（ExcludeFilter）                     │
│                                                             │
│  3. 收集所有 Registration                                   │
│     └── RegisteredBean（Bean 信息）                        │
│     └── BeanDefinitionMethodGenerator（代码生成器）         │
│     └── 别名数组                                            │
│                                                             │
│  4. 返回 BeanRegistrationsAotContribution                   │
│     └── 包含所有注册信息，用于后续代码生成                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 代码生成机制

### 3.1 BeanDefinitionMethodGeneratorFactory - 生成器工厂

**源码位置**: `spring-beans/src/main/java/org/springframework/beans/factory/aot/BeanDefinitionMethodGeneratorFactory.java`

```java
class BeanDefinitionMethodGeneratorFactory {

    private final AotServices<BeanRegistrationAotProcessor> aotProcessors;
    private final AotServices<BeanRegistrationExcludeFilter> excludeFilters;

    BeanDefinitionMethodGeneratorFactory(ConfigurableListableBeanFactory beanFactory) {
        this(AotServices.factoriesAndBeans(beanFactory));
    }

    /**
     * 获取 BeanDefinition 方法生成器
     */
    @Nullable
    BeanDefinitionMethodGenerator getBeanDefinitionMethodGenerator(RegisteredBean registeredBean) {
        // 1. 检查是否被排除
        if (isExcluded(registeredBean)) {
            return null;
        }

        // 2. 收集所有 AOT 贡献
        List<BeanRegistrationAotContribution> contributions = getAotContributions(registeredBean);

        // 3. 创建生成器
        return new BeanDefinitionMethodGenerator(this, registeredBean, null, contributions);
    }

    private boolean isExcluded(RegisteredBean registeredBean) {
        // 检查忽略属性
        if (Boolean.TRUE.equals(registeredBean.getMergedBeanDefinition()
                .getAttribute(BeanRegistrationAotProcessor.IGNORE_REGISTRATION_ATTRIBUTE))) {
            return true;
        }

        // 隐式排除 AOT 处理器本身
        Class<?> beanClass = registeredBean.getBeanClass();
        if (BeanFactoryInitializationAotProcessor.class.isAssignableFrom(beanClass)) {
            return true;
        }
        if (BeanRegistrationAotProcessor.class.isAssignableFrom(beanClass)) {
            BeanRegistrationAotProcessor processor = this.aotProcessors.findByBeanName(registeredBean.getBeanName());
            return (processor == null || processor.isBeanExcludedFromAotProcessing());
        }

        // 应用排除过滤器
        for (BeanRegistrationExcludeFilter excludeFilter : this.excludeFilters) {
            if (excludeFilter.isExcludedFromAotProcessing(registeredBean)) {
                return true;
            }
        }
        return false;
    }

    private List<BeanRegistrationAotContribution> getAotContributions(RegisteredBean registeredBean) {
        List<BeanRegistrationAotContribution> contributions = new ArrayList<>();
        for (BeanRegistrationAotProcessor aotProcessor : this.aotProcessors) {
            BeanRegistrationAotContribution contribution = aotProcessor.processAheadOfTime(registeredBean);
            if (contribution != null) {
                contributions.add(contribution);
            }
        }
        return contributions;
    }
}
```

### 3.2 BeanDefinitionMethodGenerator - BeanDefinition 方法生成器

**源码位置**: `spring-beans/src/main/java/org/springframework/beans/factory/aot/BeanDefinitionMethodGenerator.java`

```java
class BeanDefinitionMethodGenerator {

    private final BeanDefinitionMethodGeneratorFactory methodGeneratorFactory;
    private final RegisteredBean registeredBean;
    private final List<BeanRegistrationAotContribution> aotContributions;

    /**
     * 生成返回 BeanDefinition 的方法
     *
     * 生成的代码示例：
     * public static BeanDefinition getBeanDefinition() {
     *     RootBeanDefinition beanDefinition = new RootBeanDefinition(MyService.class);
     *     beanDefinition.setInstanceSupplier(BeanInstanceSupplier.forConstructor()
     *         .withGenerator((registeredBean) -> new MyService(...)));
     *     return beanDefinition;
     * }
     */
    MethodReference generateBeanDefinitionMethod(GenerationContext generationContext,
            BeanRegistrationsCode beanRegistrationsCode) {

        // 1. 获取代码片段（支持贡献者自定义）
        BeanRegistrationCodeFragments codeFragments = getCodeFragments(generationContext, beanRegistrationsCode);

        // 2. 确定目标类（生成的代码将放在哪里）
        ClassName target = codeFragments.getTarget(this.registeredBean);

        // 3. 查找或创建生成的类
        GeneratedClass generatedClass = lookupGeneratedClass(generationContext, target);
        GeneratedMethods generatedMethods = generatedClass.getMethods().withPrefix(getName());

        // 4. 生成方法
        GeneratedMethod generatedMethod = generateBeanDefinitionMethod(generationContext,
                generatedClass.getName(), generatedMethods, codeFragments, Modifier.PUBLIC);

        return generatedMethod.toMethodReference();
    }

    private BeanRegistrationCodeFragments getCodeFragments(GenerationContext generationContext,
            BeanRegistrationsCode beanRegistrationsCode) {

        // 从默认代码片段开始
        BeanRegistrationCodeFragments codeFragments = new DefaultBeanRegistrationCodeFragments(
                beanRegistrationsCode, this.registeredBean, this.methodGeneratorFactory);

        // 允许贡献者自定义代码片段
        for (BeanRegistrationAotContribution aotContribution : this.aotContributions) {
            codeFragments = aotContribution.customizeBeanRegistrationCodeFragments(generationContext, codeFragments);
        }
        return codeFragments;
    }
}
```

### 3.3 InstanceSupplierCodeGenerator - 实例提供者代码生成器

**源码位置**: `spring-beans/src/main/java/org/springframework/beans/factory/aot/InstanceSupplierCodeGenerator.java`

```java
public class InstanceSupplierCodeGenerator {

    private final GenerationContext generationContext;
    private final ClassName className;
    private final GeneratedMethods generatedMethods;
    private final boolean allowDirectSupplierShortcut;

    /**
     * 生成 InstanceSupplier 代码
     *
     * 支持多种生成策略：
     * 1. 直接方法引用（无参构造函数）
     * 2. BeanInstanceSupplier.forConstructor()（需要参数解析）
     * 3. BeanInstanceSupplier.forFactoryMethod()（工厂方法）
     */
    public CodeBlock generateCode(RegisteredBean registeredBean, InstantiationDescriptor instantiationDescriptor) {
        Executable constructorOrFactoryMethod = instantiationDescriptor.executable();

        // 注册运行时提示（用于反射或代理）
        registerRuntimeHintsIfNecessary(registeredBean, constructorOrFactoryMethod);

        if (constructorOrFactoryMethod instanceof Constructor<?> constructor) {
            return generateCodeForConstructor(registeredBean, constructor);
        }
        if (constructorOrFactoryMethod instanceof Method method) {
            return generateCodeForFactoryMethod(registeredBean, method, instantiationDescriptor.targetClass());
        }
        throw new AotBeanProcessingException(registeredBean, "no suitable constructor or factory method found");
    }

    /**
     * 为可访问的构造函数生成代码
     *
     * 无参情况：
     * - allowDirectSupplierShortcut=true: MyClass::new
     * - allowDirectSupplierShortcut=false: InstanceSupplier.using(MyClass::new)
     *
     * 有参情况：
     * 生成私有静态方法，返回 BeanInstanceSupplier
     */
    private CodeBlock generateCodeForAccessibleConstructor(ConstructorDescriptor descriptor) {
        Constructor<?> constructor = descriptor.constructor();

        // 注册内省提示（用于读取参数注解）
        this.generationContext.getRuntimeHints().reflection().registerConstructor(
                constructor, ExecutableMode.INTROSPECT);

        if (constructor.getParameterCount() == 0) {
            // 无参构造函数 - 使用快捷方式
            if (!this.allowDirectSupplierShortcut) {
                return CodeBlock.of("$T.using($T::new)", InstanceSupplier.class, descriptor.actualType());
            }
            if (!isThrowingCheckedException(constructor)) {
                return CodeBlock.of("$T::new", descriptor.actualType());
            }
            return CodeBlock.of("$T.of($T::new)", ThrowingSupplier.class, descriptor.actualType());
        }

        // 有参构造函数 - 生成完整的方法
        GeneratedMethod generatedMethod = generateGetInstanceSupplierMethod(method ->
                buildGetInstanceMethodForConstructor(method, descriptor, PRIVATE_STATIC));
        return generateReturnStatement(generatedMethod);
    }

    /**
     * 为不可访问的构造函数生成代码（需要反射）
     */
    private CodeBlock generateCodeForInaccessibleConstructor(ConstructorDescriptor descriptor,
            Consumer<ReflectionHints> hints) {

        Constructor<?> constructor = descriptor.constructor();

        // 注册完整的调用提示（用于反射调用）
        hints.accept(this.generationContext.getRuntimeHints().reflection());

        GeneratedMethod generatedMethod = generateGetInstanceSupplierMethod(method -> {
            method.addJavadoc("Get the bean instance supplier for '$L'.", descriptor.beanName());
            method.addModifiers(PRIVATE_STATIC);
            method.returns(ParameterizedTypeName.get(BeanInstanceSupplier.class, descriptor.publicType()));
            method.addStatement(generateResolverForConstructor(descriptor));
        });

        return generateReturnStatement(generatedMethod);
    }

    /**
     * 构建获取实例供应者的方法体
     */
    private void buildGetInstanceMethodForConstructor(MethodSpec.Builder method, ConstructorDescriptor descriptor,
            javax.lang.model.element.Modifier... modifiers) {

        Constructor<?> constructor = descriptor.constructor();
        Class<?> publicType = descriptor.publicType();
        Class<?> actualType = descriptor.actualType();

        method.addJavadoc("Get the bean instance supplier for '$L'.", descriptor.beanName());
        method.addModifiers(modifiers);
        method.returns(ParameterizedTypeName.get(BeanInstanceSupplier.class, publicType));

        CodeBlock.Builder code = CodeBlock.builder();

        // 生成：return BeanInstanceSupplier.<PublicType>forConstructor(ParameterTypes.class)
        code.add(generateResolverForConstructor(descriptor));

        boolean hasArguments = constructor.getParameterCount() > 0;
        boolean onInnerClass = ClassUtils.isInnerClass(actualType);

        // 生成参数获取代码
        CodeBlock arguments = hasArguments ?
                new AutowiredArgumentsCodeGenerator(actualType, constructor)
                        .generateCode(constructor.getParameterTypes(), (onInnerClass ? 1 : 0)) : NO_ARGS;

        // 生成实例化代码
        CodeBlock newInstance = generateNewInstanceCodeForConstructor(actualType, arguments);
        code.add(generateWithGeneratorCode(hasArguments, newInstance));
        method.addStatement(code.build());
    }

    private CodeBlock generateResolverForConstructor(ConstructorDescriptor descriptor) {
        CodeBlock parameterTypes = generateParameterTypesCode(descriptor.constructor().getParameterTypes());
        return CodeBlock.of("return $T.<$T>forConstructor($L)", BeanInstanceSupplier.class,
                descriptor.publicType(), parameterTypes);
    }

    private CodeBlock generateNewInstanceCodeForConstructor(Class<?> declaringClass, CodeBlock args) {
        // 内部类需要特殊处理（需要外部类实例）
        if (ClassUtils.isInnerClass(declaringClass)) {
            return CodeBlock.of("$L.getBeanFactory().getBean($T.class).new $L($L)",
                    REGISTERED_BEAN_PARAMETER_NAME, declaringClass.getEnclosingClass(),
                    declaringClass.getSimpleName(), args);
        }
        return CodeBlock.of("new $T($L)", declaringClass, args);
    }
}
```

### 3.4 AutowiredArgumentsCodeGenerator - 自动装配参数代码生成器

**源码位置**: `spring-beans/src/main/java/org/springframework/beans/factory/aot/AutowiredArgumentsCodeGenerator.java`

```java
public class AutowiredArgumentsCodeGenerator {

    private final Class<?> target;
    private final Executable executable;

    /**
     * 生成 AutowiredArguments 获取代码
     *
     * 简单形式（无歧义）：args.get(0), args.get(1)
     * 类型安全形式（有歧义）：args.get(0, String.class), args.get(1, Integer.class)
     */
    public CodeBlock generateCode(Class<?>[] parameterTypes, int startIndex, String variableName) {
        boolean ambiguous = isAmbiguous();
        CodeBlock.Builder code = CodeBlock.builder();

        for (int i = startIndex; i < parameterTypes.length; i++) {
            code.add(i > startIndex ? ", " : "");
            if (!ambiguous) {
                // 无歧义，直接使用索引
                code.add("$L.get($L)", variableName, i);
            }
            else {
                // 有歧义，需要指定类型
                code.add("$L.get($L, $T.class)", variableName, i, parameterTypes[i]);
            }
        }
        return code.build();
    }

    /**
     * 判断是否存在参数歧义
     *
     * 例如：
     * - 多个相同名称的方法（重载）
     * - 多个相同参数数量的构造函数
     */
    private boolean isAmbiguous() {
        if (this.executable instanceof Constructor<?> constructor) {
            return Arrays.stream(this.target.getDeclaredConstructors())
                    .filter(Predicate.not(constructor::equals))
                    .anyMatch(this::hasSameParameterCount);
        }
        if (this.executable instanceof Method method) {
            return Arrays.stream(ReflectionUtils.getAllDeclaredMethods(this.target))
                    .filter(Predicate.not(method::equals))
                    .filter(candidate -> candidate.getName().equals(method.getName()))
                    .anyMatch(this::hasSameParameterCount);
        }
        return true;
    }

    private boolean hasSameParameterCount(Executable executable) {
        return this.executable.getParameterCount() == executable.getParameterCount();
    }
}
```

---

## 4. 代码片段生成

### 4.1 BeanRegistrationCodeFragments 接口

**源码位置**: `spring-beans/src/main/java/org/springframework/beans/factory/aot/BeanRegistrationCodeFragments.java`

```java
public interface BeanRegistrationCodeFragments {

    String BEAN_DEFINITION_VARIABLE = "beanDefinition";
    String INSTANCE_SUPPLIER_VARIABLE = "instanceSupplier";

    /**
     * 获取代码生成的目标类
     * 决定生成的代码应该放在哪个类中
     */
    ClassName getTarget(RegisteredBean registeredBean);

    /**
     * 生成创建 BeanDefinition 的代码
     * 例如：RootBeanDefinition beanDefinition = new RootBeanDefinition(MyClass.class);
     */
    CodeBlock generateNewBeanDefinitionCode(GenerationContext generationContext,
            ResolvableType beanType, BeanRegistrationCode beanRegistrationCode);

    /**
     * 生成设置 BeanDefinition 属性的代码
     * 例如：beanDefinition.setScope("singleton"); beanDefinition.setPrimary(true);
     */
    CodeBlock generateSetBeanDefinitionPropertiesCode(
            GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
            RootBeanDefinition beanDefinition, Predicate<String> attributeFilter);

    /**
     * 生成设置 InstanceSupplier 的代码
     */
    CodeBlock generateSetBeanInstanceSupplierCode(
            GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
            CodeBlock instanceSupplierCode, List<MethodReference> postProcessors);

    /**
     * 生成 InstanceSupplier 代码
     */
    CodeBlock generateInstanceSupplierCode(
            GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
            boolean allowDirectSupplierShortcut);

    /**
     * 生成返回语句
     */
    CodeBlock generateReturnCode(
            GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode);
}
```

### 4.2 DefaultBeanRegistrationCodeFragments - 默认实现

**源码位置**: `spring-beans/src/main/java/org/springframework/beans/factory/aot/DefaultBeanRegistrationCodeFragments.java`

```java
class DefaultBeanRegistrationCodeFragments implements BeanRegistrationCodeFragments {

    private static final ValueCodeGenerator valueCodeGenerator = ValueCodeGenerator.withDefaults();

    private final BeanRegistrationsCode beanRegistrationsCode;
    private final RegisteredBean registeredBean;
    private final BeanDefinitionMethodGeneratorFactory beanDefinitionMethodGeneratorFactory;
    private final Supplier<InstantiationDescriptor> instantiationDescriptor;

    DefaultBeanRegistrationCodeFragments(
            BeanRegistrationsCode beanRegistrationsCode, RegisteredBean registeredBean,
            BeanDefinitionMethodGeneratorFactory beanDefinitionMethodGeneratorFactory) {
        this.beanRegistrationsCode = beanRegistrationsCode;
        this.registeredBean = registeredBean;
        this.beanDefinitionMethodGeneratorFactory = beanDefinitionMethodGeneratorFactory;
        this.instantiationDescriptor = SingletonSupplier.of(registeredBean::resolveInstantiationDescriptor);
    }

    /**
     * 确定代码生成的目标类
     *
     * 策略：
     * 1. 使用 Bean 的声明类作为目标
     * 2. 如果是内部类，向上查找到非 java.* 包
     * 3. 如果是 FactoryBean，尝试提取目标类型
     */
    @Override
    public ClassName getTarget(RegisteredBean registeredBean) {
        if (hasInstanceSupplier()) {
            throw new AotBeanProcessingException(registeredBean, "instance supplier is not supported");
        }

        Class<?> target = extractDeclaringClass(registeredBean, this.instantiationDescriptor.get());

        // 如果是 java.* 包的内部类，向上查找父类
        while (target.getName().startsWith("java.") && registeredBean.isInnerBean()) {
            RegisteredBean parent = registeredBean.getParent();
            Assert.state(parent != null, "No parent available for inner bean");
            target = parent.getBeanClass();
        }

        return (target.isArray() ? ClassName.get(target.getComponentType()) : ClassName.get(target));
    }

    /**
     * 生成创建 BeanDefinition 的代码
     *
     * 示例输出：
     * RootBeanDefinition beanDefinition = new RootBeanDefinition(MyService.class);
     * beanDefinition.setTargetType(new ResolvableType...); // 如果有泛型
     */
    @Override
    public CodeBlock generateNewBeanDefinitionCode(GenerationContext generationContext,
            ResolvableType beanType, BeanRegistrationCode beanRegistrationCode) {

        CodeBlock.Builder code = CodeBlock.builder();
        RootBeanDefinition mbd = this.registeredBean.getMergedBeanDefinition();
        Class<?> beanClass = (mbd.hasBeanClass() ? ClassUtils.getUserClass(mbd.getBeanClass()) : null);

        // 生成 BeanClass 代码（处理包可见性）
        CodeBlock beanClassCode = generateBeanClassCode(
                beanRegistrationCode.getClassName().packageName(),
                (beanClass != null ? beanClass : beanType.toClass()));

        code.addStatement("$T $L = new $T($L)", RootBeanDefinition.class,
                BEAN_DEFINITION_VARIABLE, RootBeanDefinition.class, beanClassCode);

        // 如果需要，设置目标类型（处理泛型）
        if (targetTypeNecessary(beanType, beanClass)) {
            code.addStatement("$L.setTargetType($L)", BEAN_DEFINITION_VARIABLE, generateBeanTypeCode(beanType));
        }
        return code.build();
    }

    private CodeBlock generateBeanClassCode(String targetPackage, Class<?> beanClass) {
        // 如果是 public 或在同一包，使用 .class 语法
        if (Modifier.isPublic(beanClass.getModifiers()) || targetPackage.equals(beanClass.getPackageName())) {
            return CodeBlock.of("$T.class", beanClass);
        }
        // 否则使用类名字符串
        else {
            return CodeBlock.of("$S", beanClass.getName());
        }
    }

    /**
     * 生成设置 BeanDefinition 属性的代码
     *
     * 委托给 BeanDefinitionPropertiesCodeGenerator 处理：
     * - scope, lazyInit, primary, dependsOn 等标准属性
     * - constructor argument values
     * - property values
     * - qualifiers
     * - method overrides
     * - attributes
     */
    @Override
    public CodeBlock generateSetBeanDefinitionPropertiesCode(
            GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
            RootBeanDefinition beanDefinition, Predicate<String> attributeFilter) {

        Loader loader = AotServices.factories(this.registeredBean.getBeanFactory().getBeanClassLoader());
        List<Delegate> additionalDelegates = loader.load(Delegate.class).asList();

        return new BeanDefinitionPropertiesCodeGenerator(
                generationContext.getRuntimeHints(), attributeFilter,
                beanRegistrationCode.getMethods(), additionalDelegates,
                (name, value) -> generateValueCode(generationContext, name, value))
                .generateCode(beanDefinition);
    }

    /**
     * 处理内部 Bean 的值代码生成
     */
    @Nullable
    protected CodeBlock generateValueCode(GenerationContext generationContext, String name, Object value) {
        RegisteredBean innerRegisteredBean = getInnerRegisteredBean(value);
        if (innerRegisteredBean != null) {
            // 递归生成内部 Bean 的定义方法
            BeanDefinitionMethodGenerator methodGenerator = this.beanDefinitionMethodGeneratorFactory
                    .getBeanDefinitionMethodGenerator(innerRegisteredBean, name);
            MethodReference generatedMethod = methodGenerator
                    .generateBeanDefinitionMethod(generationContext, this.beanRegistrationsCode);
            return generatedMethod.toInvokeCodeBlock(ArgumentCodeGenerator.none());
        }
        return null;
    }

    /**
     * 生成设置 InstanceSupplier 的代码
     *
     * 简单情况：beanDefinition.setInstanceSupplier(instanceSupplierCode);
     * 有后处理器：使用 andThen 链式调用
     */
    @Override
    public CodeBlock generateSetBeanInstanceSupplierCode(
            GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
            CodeBlock instanceSupplierCode, List<MethodReference> postProcessors) {

        CodeBlock.Builder code = CodeBlock.builder();
        if (postProcessors.isEmpty()) {
            code.addStatement("$L.setInstanceSupplier($L)", BEAN_DEFINITION_VARIABLE, instanceSupplierCode);
            return code.build();
        }

        // 有后处理器，需要链式调用
        code.addStatement("$T $L = $L",
                ParameterizedTypeName.get(InstanceSupplier.class, this.registeredBean.getBeanClass()),
                INSTANCE_SUPPLIER_VARIABLE, instanceSupplierCode);

        for (MethodReference postProcessor : postProcessors) {
            code.addStatement("$L = $L.andThen($L)", INSTANCE_SUPPLIER_VARIABLE,
                    INSTANCE_SUPPLIER_VARIABLE, postProcessor.toCodeBlock());
        }
        code.addStatement("$L.setInstanceSupplier($L)", BEAN_DEFINITION_VARIABLE,
                INSTANCE_SUPPLIER_VARIABLE);
        return code.build();
    }

    /**
     * 生成 InstanceSupplier 代码
     */
    @Override
    public CodeBlock generateInstanceSupplierCode(
            GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
            boolean allowDirectSupplierShortcut) {

        if (hasInstanceSupplier()) {
            throw new AotBeanProcessingException(this.registeredBean, "instance supplier is not supported");
        }
        return new InstanceSupplierCodeGenerator(generationContext,
                beanRegistrationCode.getClassName(), beanRegistrationCode.getMethods(), allowDirectSupplierShortcut)
                .generateCode(this.registeredBean, this.instantiationDescriptor.get());
    }

    @Override
    public CodeBlock generateReturnCode(
            GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
        return CodeBlock.builder()
                .addStatement("return $L", BEAN_DEFINITION_VARIABLE)
                .build();
    }
}
```

### 4.3 BeanDefinitionPropertiesCodeGenerator - 属性代码生成器

**源码位置**: `spring-beans/src/main/java/org/springframework/beans/factory/aot/BeanDefinitionPropertiesCodeGenerator.java`

```java
class BeanDefinitionPropertiesCodeGenerator {

    private static final RootBeanDefinition DEFAULT_BEAN_DEFINITION = new RootBeanDefinition();
    private static final String BEAN_DEFINITION_VARIABLE = BeanRegistrationCodeFragments.BEAN_DEFINITION_VARIABLE;

    /**
     * 生成 BeanDefinition 属性的设置代码
     *
     * 生成的代码示例：
     * beanDefinition.setScope("prototype");
     * beanDefinition.setLazyInit(true);
     * beanDefinition.setPrimary(true);
     * beanDefinition.setDependsOn("beanA", "beanB");
     * beanDefinition.setInitMethodNames("init");
     * beanDefinition.getPropertyValues().addPropertyValue("name", "value");
     * ...
     */
    CodeBlock generateCode(RootBeanDefinition beanDefinition) {
        CodeBlock.Builder code = CodeBlock.builder();

        // 标准属性
        addStatementForValue(code, beanDefinition, BeanDefinition::getScope, this::hasScope, "$L.setScope($S)");
        addStatementForValue(code, beanDefinition, AbstractBeanDefinition::isBackgroundInit, "$L.setBackgroundInit($L)");
        addStatementForValue(code, beanDefinition, AbstractBeanDefinition::getLazyInit, "$L.setLazyInit($L)");
        addStatementForValue(code, beanDefinition, BeanDefinition::getDependsOn, this::hasDependsOn, "$L.setDependsOn($L)", this::toStringVarArgs);
        addStatementForValue(code, beanDefinition, BeanDefinition::isAutowireCandidate, "$L.setAutowireCandidate($L)");
        addStatementForValue(code, beanDefinition, AbstractBeanDefinition::isDefaultCandidate, "$L.setDefaultCandidate($L)");
        addStatementForValue(code, beanDefinition, BeanDefinition::isPrimary, "$L.setPrimary($L)");
        addStatementForValue(code, beanDefinition, BeanDefinition::isFallback, "$L.setFallback($L)");
        addStatementForValue(code, beanDefinition, AbstractBeanDefinition::isSynthetic, "$L.setSynthetic($L)");
        addStatementForValue(code, beanDefinition, BeanDefinition::getRole, this::hasRole, "$L.setRole($L)", this::toRole);

        // 初始化和销毁方法
        addInitDestroyMethods(code, beanDefinition, beanDefinition.getInitMethodNames(), "$L.setInitMethodNames($L)");
        addInitDestroyMethods(code, beanDefinition, beanDefinition.getDestroyMethodNames(), "$L.setDestroyMethodNames($L)");

        // 工厂 Bean
        if (beanDefinition.getFactoryBeanName() != null) {
            addStatementForValue(code, beanDefinition, BeanDefinition::getFactoryBeanName, "$L.setFactoryBeanName(\"$L\")");
        }

        // 构造函数参数
        addConstructorArgumentValues(code, beanDefinition);

        // 属性值
        addPropertyValues(code, beanDefinition);

        // 属性
        addAttributes(code, beanDefinition);

        // 限定符
        addQualifiers(code, beanDefinition);

        // 方法覆盖
        addMethodOverrides(code, beanDefinition);

        return code.build();
    }

    /**
     * 添加构造函数参数值代码
     */
    private void addConstructorArgumentValues(CodeBlock.Builder code, BeanDefinition beanDefinition) {
        ConstructorArgumentValues constructorValues = beanDefinition.getConstructorArgumentValues();

        // 索引参数值
        Map<Integer, ValueHolder> indexedValues = constructorValues.getIndexedArgumentValues();
        if (!indexedValues.isEmpty()) {
            indexedValues.forEach((index, valueHolder) -> {
                Object value = valueHolder.getValue();
                CodeBlock valueCode = castIfNecessary(value == null, Object.class,
                        generateValue(valueHolder.getName(), value));
                code.addStatement(
                        "$L.getConstructorArgumentValues().addIndexedArgumentValue($L, $L)",
                        BEAN_DEFINITION_VARIABLE, index, valueCode);
            });
        }

        // 通用参数值
        List<ValueHolder> genericValues = constructorValues.getGenericArgumentValues();
        if (!genericValues.isEmpty()) {
            genericValues.forEach(valueHolder -> {
                String valueName = valueHolder.getName();
                CodeBlock valueCode = generateValue(valueName, valueHolder.getValue());
                if (valueName != null) {
                    CodeBlock valueTypeCode = this.valueCodeGenerator.generateCode(valueHolder.getType());
                    code.addStatement(
                            "$L.getConstructorArgumentValues().addGenericArgumentValue(new $T($L, $L, $S))",
                            BEAN_DEFINITION_VARIABLE, ValueHolder.class, valueCode, valueTypeCode, valueName);
                }
                else if (valueHolder.getType() != null) {
                    code.addStatement("$L.getConstructorArgumentValues().addGenericArgumentValue($L, $S)",
                            BEAN_DEFINITION_VARIABLE, valueCode, valueHolder.getType());
                }
                else {
                    code.addStatement("$L.getConstructorArgumentValues().addGenericArgumentValue($L)",
                            BEAN_DEFINITION_VARIABLE, valueCode);
                }
            });
        }
    }

    /**
     * 添加属性值代码
     */
    private void addPropertyValues(CodeBlock.Builder code, RootBeanDefinition beanDefinition) {
        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
        if (!propertyValues.isEmpty()) {
            Class<?> infrastructureType = getInfrastructureType(beanDefinition);
            Map<String, Method> writeMethods = (infrastructureType != Object.class ?
                    getWriteMethods(infrastructureType) : Collections.emptyMap());

            for (PropertyValue propertyValue : propertyValues) {
                String name = propertyValue.getName();
                CodeBlock valueCode = generateValue(name, propertyValue.getValue());
                code.addStatement("$L.getPropertyValues().addPropertyValue($S, $L)",
                        BEAN_DEFINITION_VARIABLE, name, valueCode);

                // 注册反射提示（用于属性注入）
                Method writeMethod = writeMethods.get(name);
                if (writeMethod != null) {
                    registerReflectionHints(beanDefinition, writeMethod);
                }
            }
        }
    }

    /**
     * 注册反射提示
     */
    private void registerReflectionHints(RootBeanDefinition beanDefinition, Method writeMethod) {
        this.hints.reflection().registerMethod(writeMethod, ExecutableMode.INVOKE);

        // ReflectionUtils#findField 递归搜索类型层次结构
        Class<?> searchType = beanDefinition.getTargetType();
        while (searchType != null && searchType != writeMethod.getDeclaringClass()) {
            this.hints.reflection().registerType(searchType, MemberCategory.DECLARED_FIELDS);
            searchType = searchType.getSuperclass();
        }
        this.hints.reflection().registerType(writeMethod.getDeclaringClass(), MemberCategory.DECLARED_FIELDS);
    }
}
```

---

## 5. AOT 服务与扩展

### 5.1 AotServices - AOT 服务加载工具

**源码位置**: `spring-beans/src/main/java/org/springframework/beans/factory/aot/AotServices.java`

```java
public final class AotServices<T> implements Iterable<T> {

    /**
     * AOT 工厂资源位置
     * 与标准的 META-INF/spring.factories 不同，AOT 使用专门的文件
     */
    public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring/aot.factories";

    private final List<T> services;
    private final Map<String, T> beans;
    private final Map<T, Source> sources;

    /**
     * 从 SpringFactoriesLoader 加载服务
     */
    public static Loader factories() {
        return factories((ClassLoader) null);
    }

    public static Loader factories(@Nullable ClassLoader classLoader) {
        return factories(getSpringFactoriesLoader(classLoader));
    }

    /**
     * 从 SpringFactoriesLoader 和 BeanFactory 加载服务
     *
     * 这是主要入口，合并两种来源的服务：
     * 1. META-INF/spring/aot.factories 中定义的
     * 2. BeanFactory 中注册的 Bean
     */
    public static Loader factoriesAndBeans(ListableBeanFactory beanFactory) {
        ClassLoader classLoader = (beanFactory instanceof ConfigurableBeanFactory configurableBeanFactory ?
                configurableBeanFactory.getBeanClassLoader() : null);
        return factoriesAndBeans(getSpringFactoriesLoader(classLoader), beanFactory);
    }

    private static SpringFactoriesLoader getSpringFactoriesLoader(@Nullable ClassLoader classLoader) {
        return SpringFactoriesLoader.forResourceLocation(FACTORIES_RESOURCE_LOCATION, classLoader);
    }

    /**
     * 服务来源枚举
     */
    public enum Source {
        SPRING_FACTORIES_LOADER,  // 从 SpringFactoriesLoader 加载
        BEAN_FACTORY              // 从 BeanFactory 加载
    }

    /**
     * 加载器类
     */
    public static class Loader {
        private final SpringFactoriesLoader springFactoriesLoader;
        @Nullable
        private final ListableBeanFactory beanFactory;

        /**
         * 加载指定类型的所有 AOT 服务
         */
        public <T> AotServices<T> load(Class<T> type) {
            return new AotServices<>(this.springFactoriesLoader.load(type), loadBeans(type));
        }

        private <T> Map<String, T> loadBeans(Class<T> type) {
            return (this.beanFactory != null ?
                    BeanFactoryUtils.beansOfTypeIncludingAncestors(this.beanFactory, type, true, false) :
                    Collections.emptyMap());
        }
    }
}
```

### 5.2 BeanRegistrationExcludeFilter - Bean 排除过滤器

**源码位置**: `spring-beans/src/main/java/org/springframework/beans/factory/aot/BeanRegistrationExcludeFilter.java`

```java
@FunctionalInterface
public interface BeanRegistrationExcludeFilter {

    /**
     * 判断 Bean 是否应该被排除在 AOT 处理之外
     *
     * 使用场景：
     * 1. 某些 Bean 只在运行时有效
     * 2. 某些 Bean 无法被 AOT 处理（如依赖动态特性）
     * 3. 基础设施 Bean 不需要重复注册
     *
     * @param registeredBean 已注册的 Bean
     * @return 是否排除
     */
    boolean isExcludedFromAotProcessing(RegisteredBean registeredBean);
}
```

### 5.3 BeanRegistrationAotContribution - Bean 注册贡献

**源码位置**: `spring-beans/src/main/java/org/springframework/beans/factory/aot/BeanRegistrationAotContribution.java`

```java
@FunctionalInterface
public interface BeanRegistrationAotContribution {

    /**
     * 自定义代码片段
     * 允许贡献者修改默认的代码生成行为
     */
    default BeanRegistrationCodeFragments customizeBeanRegistrationCodeFragments(
            GenerationContext generationContext, BeanRegistrationCodeFragments codeFragments) {
        return codeFragments;
    }

    /**
     * 应用贡献到 Bean 注册代码
     * 这是主要的方法，用于添加额外的代码生成
     */
    void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode);

    /**
     * 创建只自定义代码片段的贡献
     */
    static BeanRegistrationAotContribution withCustomCodeFragments(
            UnaryOperator<BeanRegistrationCodeFragments> defaultCodeFragments) {
        // ...
    }

    /**
     * 连接两个贡献
     */
    @Nullable
    static BeanRegistrationAotContribution concat(@Nullable BeanRegistrationAotContribution a,
            @Nullable BeanRegistrationAotContribution b) {
        // ...
    }
}
```

---

## 6. AOT 与传统 BeanFactory 的关系

### 6.1 运行时支持类

#### BeanInstanceSupplier - Bean 实例提供者

**源码位置**: `spring-beans/src/main/java/org/springframework/beans/factory/aot/BeanInstanceSupplier.java`

```java
/**
 * 专门的 InstanceSupplier，提供用于实例化 Bean 的工厂方法。
 * 透明地处理 AutowiredArguments 的解析。
 *
 * 在 AOT 处理的应用中用作基于反射注入的有针对性替代方案。
 */
public final class BeanInstanceSupplier<T> extends AutowiredElementResolver implements InstanceSupplier<T> {

    private final ExecutableLookup lookup;
    @Nullable
    private final ThrowingFunction<RegisteredBean, T> generatorWithoutArguments;
    @Nullable
    private final ThrowingBiFunction<RegisteredBean, AutowiredArguments, T> generatorWithArguments;

    /**
     * 为指定构造函数创建 BeanInstanceSupplier
     */
    public static <T> BeanInstanceSupplier<T> forConstructor(Class<?>... parameterTypes) {
        return new BeanInstanceSupplier<>(new ConstructorLookup(parameterTypes), null, null, null);
    }

    /**
     * 为指定工厂方法创建 BeanInstanceSupplier
     */
    public static <T> BeanInstanceSupplier<T> forFactoryMethod(
            Class<?> declaringClass, String methodName, Class<?>... parameterTypes) {
        return new BeanInstanceSupplier<>(
                new FactoryMethodLookup(declaringClass, methodName, parameterTypes), null, null, null);
    }

    /**
     * 设置使用 RegisteredBean 和 AutowiredArguments 的生成器
     */
    public BeanInstanceSupplier<T> withGenerator(
            ThrowingBiFunction<RegisteredBean, AutowiredArguments, T> generator) {
        return new BeanInstanceSupplier<>(this.lookup, null, generator, this.shortcutBeanNames);
    }

    /**
     * 设置只使用 RegisteredBean 的生成器
     */
    public BeanInstanceSupplier<T> withGenerator(ThrowingFunction<RegisteredBean, T> generator) {
        return new BeanInstanceSupplier<>(this.lookup, generator, null, this.shortcutBeanNames);
    }

    /**
     * 获取 Bean 实例
     *
     * 执行流程：
     * 1. 如果有无参生成器，直接调用
     * 2. 如果有带参生成器，解析参数后调用
     * 3. 否则使用反射实例化
     */
    @Override
    @SuppressWarnings("unchecked")
    public T get(RegisteredBean registeredBean) {
        Assert.notNull(registeredBean, "'registeredBean' must not be null");

        if (this.generatorWithoutArguments != null) {
            Executable executable = getFactoryMethodForGenerator();
            return invokeBeanSupplier(executable, () -> this.generatorWithoutArguments.apply(registeredBean));
        }
        else if (this.generatorWithArguments != null) {
            Executable executable = getFactoryMethodForGenerator();
            AutowiredArguments arguments = resolveArguments(registeredBean,
                    executable != null ? executable : this.lookup.get(registeredBean));
            return invokeBeanSupplier(executable, () -> this.generatorWithArguments.apply(registeredBean, arguments));
        }
        else {
            // 回退到反射
            Executable executable = this.lookup.get(registeredBean);
            Object[] arguments = resolveArguments(registeredBean, executable).toArray();
            return invokeBeanSupplier(executable, () -> (T) instantiate(registeredBean, executable, arguments));
        }
    }

    /**
     * 解析构造函数或工厂方法的参数
     */
    private AutowiredArguments resolveArguments(RegisteredBean registeredBean, Executable executable) {
        int parameterCount = executable.getParameterCount();
        Object[] resolved = new Object[parameterCount];

        ValueHolder[] argumentValues = resolveArgumentValues(registeredBean, executable);
        Set<String> autowiredBeanNames = new LinkedHashSet<>(resolved.length * 2);

        // 内部类构造函数的第一个参数是 enclosing 实例
        int startIndex = (executable instanceof Constructor<?> constructor &&
                ClassUtils.isInnerClass(constructor.getDeclaringClass())) ? 1 : 0;

        for (int i = startIndex; i < parameterCount; i++) {
            MethodParameter parameter = getMethodParameter(executable, i);
            DependencyDescriptor descriptor = new DependencyDescriptor(parameter, true);

            // 支持快捷 Bean 名注入
            String shortcut = (this.shortcutBeanNames != null ? this.shortcutBeanNames[i] : null);
            if (shortcut != null) {
                descriptor = new ShortcutDependencyDescriptor(descriptor, shortcut);
            }

            ValueHolder argumentValue = argumentValues[i];
            resolved[i] = resolveAutowiredArgument(
                    registeredBean, descriptor, argumentValue, autowiredBeanNames);
        }

        registerDependentBeans(registeredBean.getBeanFactory(), registeredBean.getBeanName(), autowiredBeanNames);
        return AutowiredArguments.of(resolved);
    }
}
```

#### AutowiredArguments - 自动装配参数

**源码位置**: `spring-beans/src/main/java/org/springframework/beans/factory/aot/AutowiredArguments.java`

```java
/**
 * 已解析的自动装配参数
 *
 * 在生成的代码中使用，例如：
 * args.get(0), args.get(1, String.class)
 */
@FunctionalInterface
public interface AutowiredArguments {

    /**
     * 获取指定索引的参数（带类型检查）
     */
    @SuppressWarnings("unchecked")
    @Nullable
    default <T> T get(int index, Class<T> requiredType) {
        Object value = getObject(index);
        if (!ClassUtils.isAssignableValue(requiredType, value)) {
            throw new IllegalArgumentException("Argument type mismatch: expected '" +
                    ClassUtils.getQualifiedName(requiredType) + "' for value [" + value + "]");
        }
        return (T) value;
    }

    /**
     * 获取指定索引的参数
     */
    @SuppressWarnings("unchecked")
    @Nullable
    default <T> T get(int index) {
        return (T) getObject(index);
    }

    /**
     * 获取参数数组
     */
    Object[] toArray();

    /**
     * 工厂方法
     */
    static AutowiredArguments of(Object[] arguments) {
        Assert.notNull(arguments, "'arguments' must not be null");
        return () -> arguments;
    }
}
```

### 6.2 完整处理流程对比

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        传统 Spring 启动流程                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  启动时：                                                                    │
│  ┌──────────────┐                                                           │
│  │ 扫描类路径    │  查找 @Configuration, @Component 等                       │
│  └──────┬───────┘                                                           │
│         ↓                                                                   │
│  ┌──────────────┐                                                           │
│  │ 解析配置类    │  处理 @Bean, @Import, @ComponentScan                      │
│  └──────┬───────┘                                                           │
│         ↓                                                                   │
│  ┌──────────────┐                                                           │
│  │ 创建 BeanDefinition │  存储在 BeanFactory 中                               │
│  └──────┬───────┘                                                           │
│         ↓                                                                   │
│  ┌──────────────┐                                                           │
│  │ 实例化 Bean   │  使用反射调用构造函数                                       │
│  └──────┬───────┘                                                           │
│         ↓                                                                   │
│  ┌──────────────┐                                                           │
│  │ 依赖注入      │  使用反射设置字段/调用 setter                              │
│  └──────┬───────┘                                                           │
│         ↓                                                                   │
│  ┌──────────────┐                                                           │
│  │ 初始化        │  调用 @PostConstruct, InitializingBean 等                 │
│  └──────────────┘                                                           │
│                                                                             │
│  问题：启动慢（需要扫描），反射多（性能开销），类路径依赖（不适合原生镜像）       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

                                    ↓ AOT 处理

┌─────────────────────────────────────────────────────────────────────────────┐
│                        AOT 优化后的启动流程                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  构建时（AOT 处理）：                                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ BeanRegistrationsAotProcessor.processAheadOfTime()                  │   │
│  └────────────────────────────────┬────────────────────────────────────┘   │
│                                   ↓                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ 遍历所有 BeanDefinition                                              │   │
│  │ 为每个 Bean 创建 BeanDefinitionMethodGenerator                       │   │
│  │ 应用 BeanRegistrationAotProcessor 贡献                              │   │
│  └────────────────────────────────┬────────────────────────────────────┘   │
│                                   ↓                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ DefaultBeanRegistrationCodeFragments                                │   │
│  │  - getTarget(): 确定代码生成目标类                                   │   │
│  │  - generateNewBeanDefinitionCode(): 生成 BeanDefinition 创建代码     │   │
│  │  - generateInstanceSupplierCode(): 生成实例供应者代码                │   │
│  │  - generateSetBeanDefinitionPropertiesCode(): 生成属性设置代码       │   │
│  └────────────────────────────────┬────────────────────────────────────┘   │
│                                   ↓                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ InstanceSupplierCodeGenerator                                       │   │
│  │  - 分析构造函数/工厂方法                                             │   │
│  │  - 生成 BeanInstanceSupplier 代码                                    │   │
│  │  - 注册 RuntimeHints（反射提示）                                     │   │
│  └────────────────────────────────┬────────────────────────────────────┘   │
│                                   ↓                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ 输出：                                                              │   │
│  │  - 生成的 Java 源文件（如 MyClass__BeanDefinitions.java）            │   │
│  │  - RuntimeHints 配置（反射、代理、资源等）                           │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  运行时：                                                                    │
│  ┌──────────────┐                                                           │
│  │ 执行生成的代码 │  直接调用生成的 getBeanDefinition() 方法                  │
│  └──────┬───────┘                                                           │
│         ↓                                                                   │
│  ┌──────────────┐                                                           │
│  │ BeanInstance │  使用预生成的 InstanceSupplier 创建实例                   │
│  │ Supplier.get │  参数解析通过生成的代码完成，无需反射                       │
│  └──────┬───────┘                                                           │
│         ↓                                                                   │
│  ┌──────────────┐                                                           │
│  │ 初始化        │  与之前相同，但可能使用生成的初始化代码                     │
│  └──────────────┘                                                           │
│                                                                             │
│  优势：启动快（无扫描），反射少（性能好），无类路径依赖（适合原生镜像）          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.3 生成的代码示例

假设有以下配置类：

```java
@Configuration
public class MyConfig {

    @Bean
    public MyService myService(MyRepository repository) {
        return new MyService(repository);
    }

    @Bean
    public MyRepository myRepository() {
        return new MyRepository();
    }
}
```

生成的代码可能如下：

```java
// MyConfig__BeanDefinitions.java
public class MyConfig__BeanDefinitions {

    /**
     * Get the bean definition for 'myService'.
     */
    public static BeanDefinition getMyServiceBeanDefinition() {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(MyService.class);
        beanDefinition.setInstanceSupplier(getMyServiceInstanceSupplier());
        return beanDefinition;
    }

    private static BeanInstanceSupplier<MyService> getMyServiceInstanceSupplier() {
        return BeanInstanceSupplier.<MyService>forFactoryMethod(MyConfig.class, "myService", MyRepository.class)
            .withGenerator((registeredBean, args) ->
                registeredBean.getBeanFactory().getBean(MyConfig.class).myService(
                    args.get(0, MyRepository.class)));
    }

    /**
     * Get the bean definition for 'myRepository'.
     */
    public static BeanDefinition getMyRepositoryBeanDefinition() {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(MyRepository.class);
        beanDefinition.setInstanceSupplier(MyRepository::new);
        return beanDefinition;
    }
}

// BeanFactoryRegistrations.java
public class BeanFactoryRegistrations {

    public void registerBeanDefinitions(DefaultListableBeanFactory beanFactory) {
        beanFactory.registerBeanDefinition("myService",
            MyConfig__BeanDefinitions.getMyServiceBeanDefinition());
        beanFactory.registerBeanDefinition("myRepository",
            MyConfig__BeanDefinitions.getMyRepositoryBeanDefinition());
    }

    public void registerAliases(DefaultListableBeanFactory beanFactory) {
        // 注册别名（如果有）
    }
}
```

---

## 总结

Spring AOT 模块通过以下机制实现了构建时代码生成：

1. **处理器架构**：通过 `BeanRegistrationAotProcessor` 和 `BeanFactoryInitializationAotProcessor` 接口，允许在构建时分析和处理 Bean。

2. **代码生成**：使用 JavaPoet 库生成 Java 源代码，将运行时的反射操作转换为直接的代码调用。

3. **代码片段**：通过 `BeanRegistrationCodeFragments` 接口定义代码生成模板，`DefaultBeanRegistrationCodeFragments` 提供默认实现。

4. **运行时支持**：`BeanInstanceSupplier` 和 `AutowiredArguments` 等类在运行时支持生成的代码，处理依赖注入和实例化。

5. **服务加载**：`AotServices` 统一从 `META-INF/spring/aot.factories` 和 BeanFactory 加载 AOT 处理器。

这种设计使得 Spring 应用可以在构建时完成大部分准备工作，生成优化的代码，从而显著提高启动速度并支持 GraalVM 原生镜像。
