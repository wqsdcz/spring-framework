# Spring Beans Groovy 包详解

## 目录
1. [概述](#概述)
2. [GroovyBeanDefinitionReader](#groovybeandefinitionreader)
3. [GroovyBeanDefinitionWrapper](#groovybeandefinitionwrapper)
4. [GroovyDynamicElementReader](#grodynamicelementreader)
5. [Groovy DSL 配置示例](#groovy-dsl-配置示例)
6. [与 XML/JavaConfig 对比](#与-xmljavaconfig-对比)

---

## 概述

`org.springframework.beans.factory.groovy` 包提供了使用 Groovy DSL（领域特定语言）配置 Spring Bean 定义的支持。这是 Spring 4.0 引入的特性，允许开发者使用 Groovy 的闭包语法来定义 Bean，相比 XML 配置更加简洁和类型安全。

### 核心组件

| 类名 | 作用 |
|------|------|
| `GroovyBeanDefinitionReader` | 核心读取器，解析 Groovy DSL 并注册 Bean 定义 |
| `GroovyBeanDefinitionWrapper` | Bean 定义的包装器，支持 Groovy 风格的属性访问 |
| `GroovyDynamicElementReader` | 处理 Spring XML 命名空间在 Groovy DSL 中的使用 |

---

## GroovyBeanDefinitionReader

### 类定义

```java
public class GroovyBeanDefinitionReader extends AbstractBeanDefinitionReader implements GroovyObject
```

`GroovyBeanDefinitionReader` 是 Groovy Bean 定义的核心读取器，它：
- 继承自 `AbstractBeanDefinitionReader`，复用 Spring 的 Bean 定义读取基础设施
- 实现 `GroovyObject` 接口，支持 Groovy 的元编程特性
- 同时支持 Groovy DSL 和 XML 配置文件（`.xml` 后缀的文件会被当作 XML 处理）

### 核心属性

```java
// 标准 XML 读取器，用于处理 .xml 文件
private final XmlBeanDefinitionReader standardXmlBeanDefinitionReader;

// Groovy DSL 专用的 XML 读取器，通常禁用 XML 验证
private final XmlBeanDefinitionReader groovyDslXmlBeanDefinitionReader;

// 命名空间映射（前缀 -> URI）
private final Map<String, String> namespaces = new HashMap<>();

// 延迟处理的属性（用于处理包含 RuntimeBeanReference 的 List/Map）
private final Map<String, DeferredProperty> deferredProperties = new HashMap<>();

// 当前正在处理的 Bean 定义
@Nullable
private GroovyBeanDefinitionWrapper currentBeanDefinition;

// Groovy 绑定对象，用于变量传递
@Nullable
private Binding binding;
```

### 构造方法

```java
/**
 * 基于 BeanDefinitionRegistry 创建读取器
 * 同时创建两个 XmlBeanDefinitionReader：
 * - standardXmlBeanDefinitionReader: 用于标准 XML 文件
 * - groovyDslXmlBeanDefinitionReader: 用于 Groovy DSL，禁用验证
 */
public GroovyBeanDefinitionReader(BeanDefinitionRegistry registry) {
    super(registry);
    this.standardXmlBeanDefinitionReader = new XmlBeanDefinitionReader(registry);
    this.groovyDslXmlBeanDefinitionReader = new XmlBeanDefinitionReader(registry);
    this.groovyDslXmlBeanDefinitionReader.setValidating(false);
}

/**
 * 基于现有的 XmlBeanDefinitionReader 创建
 * 用于复用已有的 XML 读取器配置
 */
public GroovyBeanDefinitionReader(XmlBeanDefinitionReader xmlBeanDefinitionReader) {
    super(xmlBeanDefinitionReader.getRegistry());
    this.standardXmlBeanDefinitionReader = new XmlBeanDefinitionReader(xmlBeanDefinitionReader.getRegistry());
    this.groovyDslXmlBeanDefinitionReader = xmlBeanDefinitionReader;
}
```

### 加载 Bean 定义

```java
/**
 * 从资源加载 Bean 定义
 * .xml 文件使用标准 XML 读取器，其他文件作为 Groovy 脚本处理
 */
@Override
public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
    return loadBeanDefinitions(new EncodedResource(resource));
}

public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
    // 1. 检查是否为 XML 文件
    String filename = encodedResource.getResource().getFilename();
    if (StringUtils.endsWithIgnoreCase(filename, ".xml")) {
        return this.standardXmlBeanDefinitionReader.loadBeanDefinitions(encodedResource);
    }

    // 2. 创建 Groovy 闭包作为 beans 变量
    @SuppressWarnings("serial")
    Closure<Object> beans = new Closure<>(this) {
        @Override
        @Nullable
        public Object call(Object... args) {
            invokeBeanDefiningClosure((Closure<?>) args[0]);
            return null;
        }
    };

    // 3. 创建自定义 Binding，拦截变量设置
    Binding binding = new Binding() {
        @Override
        public void setVariable(String name, Object value) {
            if (currentBeanDefinition != null) {
                // 如果在 Bean 定义闭包内，将变量作为属性设置
                applyPropertyToBeanDefinition(name, value);
            }
            else {
                super.setVariable(name, value);
            }
        }
    };
    binding.setVariable("beans", beans);

    // 4. 使用 GroovyShell 执行脚本
    int countBefore = getRegistry().getBeanDefinitionCount();
    try {
        GroovyShell shell = new GroovyShell(getBeanClassLoader(), binding);
        shell.evaluate(encodedResource.getReader(), "beans");
    }
    catch (Throwable ex) {
        throw new BeanDefinitionParsingException(new Problem("Error evaluating Groovy script: " + ex.getMessage(),
                new Location(encodedResource.getResource()), null, ex));
    }

    // 5. 返回加载的 Bean 定义数量
    return getRegistry().getBeanDefinitionCount() - countBefore;
}
```

### invokeMethod - 方法调用处理

```java
/**
 * 重写方法调用，处理 Groovy DSL 中的各种语法形式
 */
@Override
public Object invokeMethod(String name, Object arg) {
    Object[] args = (Object[])arg;

    // 1. 处理 beans { ... } 调用
    if ("beans".equals(name) && args.length == 1 && args[0] instanceof Closure<?> closure) {
        return beans(closure);
    }
    // 2. 处理 ref() 方法，创建 Bean 引用
    else if ("ref".equals(name)) {
        String refName;
        if (args[0] instanceof RuntimeBeanReference runtimeBeanReference) {
            refName = runtimeBeanReference.getBeanName();
        }
        else {
            refName = args[0].toString();
        }
        boolean parentRef = false;
        if (args.length > 1 && args[1] instanceof Boolean bool) {
            parentRef = bool;
        }
        return new RuntimeBeanReference(refName, parentRef);
    }
    // 3. 处理命名空间调用（如 aop { ... }）
    else if (this.namespaces.containsKey(name) && args.length > 0 && args[0] instanceof Closure) {
        GroovyDynamicElementReader reader = createDynamicElementReader(name);
        reader.invokeMethod("doCall", args);
    }
    // 4. 处理 Bean 定义方法（如 dataSource(BasicDataSource) { ... }）
    else if (args.length > 0 && args[0] instanceof Closure) {
        return invokeBeanDefiningMethod(name, args);
    }
    else if (args.length > 0 &&
            (args[0] instanceof Class || args[0] instanceof RuntimeBeanReference || args[0] instanceof Map)) {
        return invokeBeanDefiningMethod(name, args);
    }
    else if (args.length > 1 && args[args.length -1] instanceof Closure) {
        return invokeBeanDefiningMethod(name, args);
    }

    // 5. 委托给 registry 的其他方法
    MetaClass mc = DefaultGroovyMethods.getMetaClass(getRegistry());
    if (!mc.respondsTo(getRegistry(), name, args).isEmpty()){
        return mc.invokeMethod(getRegistry(), name, args);
    }
    return this;
}
```

### invokeBeanDefiningMethod - Bean 定义方法解析

```java
/**
 * 解析 Bean 定义方法调用，支持多种语法形式
 */
private GroovyBeanDefinitionWrapper invokeBeanDefiningMethod(String beanName, Object[] args) {
    boolean hasClosureArgument = (args[args.length - 1] instanceof Closure);

    // 形式 1: beanName(BeanClass) { ... }
    if (args[0] instanceof Class<?> beanClass) {
        if (hasClosureArgument) {
            if (args.length - 1 != 1) {
                // 带构造参数: beanName(BeanClass, arg1, arg2) { ... }
                this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(
                        beanName, beanClass, resolveConstructorArguments(args, 1, args.length - 1));
            }
            else {
                // 无构造参数: beanName(BeanClass) { ... }
                this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, beanClass);
            }
        }
        else {
            // 无闭包配置: beanName(BeanClass, arg1, arg2)
            this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(
                    beanName, beanClass, resolveConstructorArguments(args, 1, args.length));
        }
    }
    // 形式 2: beanName(ref('otherBean')) - 工厂 Bean 引用
    else if (args[0] instanceof RuntimeBeanReference runtimeBeanReference) {
        this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
        this.currentBeanDefinition.getBeanDefinition().setFactoryBeanName(runtimeBeanReference.getBeanName());
    }
    // 形式 3: beanName(prop: value, BeanClass, args...) { ... } - 命名构造参数
    else if (args[0] instanceof Map<?, ?> namedArgs) {
        if (args.length > 1 && args[1] instanceof Class<?> clazz) {
            // 命名构造参数 + 类 + 构造参数
            List<Object> constructorArgs =
                    resolveConstructorArguments(args, 2, (hasClosureArgument ? args.length - 1 : args.length));
            this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, clazz, constructorArgs);
            for (Map.Entry<?, ?> entity : namedArgs.entrySet()) {
                String propName = (String) entity.getKey();
                setProperty(propName, entity.getValue());
            }
        }
        // 形式 4: beanName(factoryBean: 'factoryMethod', args...) { ... }
        else {
            this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
            Map.Entry<?, ?> factoryBeanEntry = namedArgs.entrySet().iterator().next();
            int constructorArgsTest = (hasClosureArgument ? 2 : 1);
            if (args.length > constructorArgsTest){
                int endOfConstructArgs = (hasClosureArgument ? args.length - 1 : args.length);
                this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, null,
                        resolveConstructorArguments(args, 1, endOfConstructArgs));
            }
            else {
                this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
            }
            this.currentBeanDefinition.getBeanDefinition().setFactoryBeanName(factoryBeanEntry.getKey().toString());
            this.currentBeanDefinition.getBeanDefinition().setFactoryMethodName(factoryBeanEntry.getValue().toString());
        }
    }
    // 形式 5: beanName { ... } - 抽象 Bean 定义
    else if (args[0] instanceof Closure) {
        this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
        this.currentBeanDefinition.getBeanDefinition().setAbstract(true);
    }
    // 形式 6: beanName(arg1, arg2) { ... } - 仅构造参数
    else {
        List<Object> constructorArgs =
                resolveConstructorArguments(args, 0, (hasClosureArgument ? args.length - 1 : args.length));
        this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, null, constructorArgs);
    }

    // 执行闭包配置
    if (hasClosureArgument) {
        Closure<?> callable = (Closure<?>) args[args.length - 1];
        callable.setDelegate(this);
        callable.setResolveStrategy(Closure.DELEGATE_FIRST);
        callable.call(this.currentBeanDefinition);
    }

    // 注册 Bean 定义
    GroovyBeanDefinitionWrapper beanDefinition = this.currentBeanDefinition;
    this.currentBeanDefinition = null;
    beanDefinition.getBeanDefinition().setAttribute(GroovyBeanDefinitionWrapper.class.getName(), beanDefinition);
    getRegistry().registerBeanDefinition(beanName, beanDefinition.getBeanDefinition());
    return beanDefinition;
}
```

### setProperty - 属性设置处理

```java
/**
 * 重写属性设置，将属性应用到当前 Bean 定义
 */
@Override
public void setProperty(String name, Object value) {
    if (this.currentBeanDefinition != null) {
        applyPropertyToBeanDefinition(name, value);
    }
}

protected void applyPropertyToBeanDefinition(String name, Object value) {
    // 转换 GString 为普通字符串
    if (value instanceof GString) {
        value = value.toString();
    }

    // 延迟处理 List/Map 属性（可能后续会添加 RuntimeBeanReference）
    if (addDeferredProperty(name, value)) {
        return;
    }
    // 处理闭包形式的嵌套 Bean
    else if (value instanceof Closure<?> callable) {
        GroovyBeanDefinitionWrapper current = this.currentBeanDefinition;
        try {
            Class<?> parameterType = callable.getParameterTypes()[0];
            if (Object.class == parameterType) {
                // 无类型闭包: { bean -> ... }
                this.currentBeanDefinition = new GroovyBeanDefinitionWrapper("");
                callable.call(this.currentBeanDefinition);
            }
            else {
                // 有类型闭包: { BeanType bean -> ... }
                this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(null, parameterType);
                callable.call((Object) null);
            }
            value = this.currentBeanDefinition.getBeanDefinition();
        }
        finally {
            this.currentBeanDefinition = current;
        }
    }

    // 添加属性到 Bean 定义
    this.currentBeanDefinition.addProperty(name, value);
}
```

### getProperty - 属性获取处理

```java
/**
 * 重写属性获取，支持：
 * 1. 从 Binding 获取变量
 * 2. 获取 Bean 引用（RuntimeBeanReference）
 * 3. 获取当前 Bean 的属性值
 */
@Override
@Nullable
public Object getProperty(String name) {
    Binding binding = getBinding();
    // 1. 从 Binding 获取
    if (binding != null && binding.hasVariable(name)) {
        return binding.getVariable(name);
    }
    else {
        // 2. 返回命名空间读取器
        if (this.namespaces.containsKey(name)) {
            return createDynamicElementReader(name);
        }
        // 3. 返回已注册 Bean 的引用
        if (getRegistry().containsBeanDefinition(name)) {
            GroovyBeanDefinitionWrapper beanDefinition = (GroovyBeanDefinitionWrapper)
                    getRegistry().getBeanDefinition(name).getAttribute(GroovyBeanDefinitionWrapper.class.getName());
            if (beanDefinition != null) {
                return new GroovyRuntimeBeanReference(name, beanDefinition, false);
            }
            else {
                return new RuntimeBeanReference(name, false);
            }
        }
        // 4. 获取当前 Bean 的属性值
        else if (this.currentBeanDefinition != null) {
            MutablePropertyValues pvs = this.currentBeanDefinition.getBeanDefinition().getPropertyValues();
            if (pvs.contains(name)) {
                return pvs.get(name);
            }
            else {
                DeferredProperty dp = this.deferredProperties.get(this.currentBeanDefinition.getBeanName() + name);
                if (dp != null) {
                    return dp.value;
                }
            }
        }
        // 5. 委托给 MetaClass
        return getMetaClass().getProperty(this, name);
    }
}
```

---

## GroovyBeanDefinitionWrapper

### 类定义

```java
/**
 * BeanDefinition 的内部包装器，支持 Groovy 风格的属性访问
 * 继承 GroovyObjectSupport 以获得 Groovy 对象的能力
 */
class GroovyBeanDefinitionWrapper extends GroovyObjectSupport
```

### 支持的动态属性

```java
// 定义动态属性常量
private static final String PARENT = "parent";
private static final String AUTOWIRE = "autowire";
private static final String CONSTRUCTOR_ARGS = "constructorArgs";
private static final String FACTORY_BEAN = "factoryBean";
private static final String FACTORY_METHOD = "factoryMethod";
private static final String INIT_METHOD = "initMethod";
private static final String DESTROY_METHOD = "destroyMethod";
private static final String SINGLETON = "singleton";

// 动态属性集合
private static final Set<String> dynamicProperties = Set.of(PARENT, AUTOWIRE, CONSTRUCTOR_ARGS,
        FACTORY_BEAN, FACTORY_METHOD, INIT_METHOD, DESTROY_METHOD, SINGLETON);
```

### 核心属性

```java
@Nullable
private String beanName;                    // Bean 名称

@Nullable
private final Class<?> clazz;               // Bean 类

@Nullable
private final Collection<?> constructorArgs; // 构造参数

@Nullable
private AbstractBeanDefinition definition;   // Bean 定义

@Nullable
private BeanWrapper definitionWrapper;       // 用于属性访问的包装器

@Nullable
private String parentName;                   // 父 Bean 名称
```

### 构造方法

```java
// 仅指定 Bean 名称
GroovyBeanDefinitionWrapper(String beanName) {
    this(beanName, null);
}

// 指定名称和类
GroovyBeanDefinitionWrapper(@Nullable String beanName, @Nullable Class<?> clazz) {
    this(beanName, clazz, null);
}

// 完整构造
GroovyBeanDefinitionWrapper(@Nullable String beanName, @Nullable Class<?> clazz,
        @Nullable Collection<?> constructorArgs) {
    this.beanName = beanName;
    this.clazz = clazz;
    this.constructorArgs = constructorArgs;
}
```

### Bean 定义创建

```java
/**
 * 获取或创建 BeanDefinition
 */
AbstractBeanDefinition getBeanDefinition() {
    if (this.definition == null) {
        this.definition = createBeanDefinition();
    }
    return this.definition;
}

protected AbstractBeanDefinition createBeanDefinition() {
    // 创建 GenericBeanDefinition
    AbstractBeanDefinition bd = new GenericBeanDefinition();
    bd.setBeanClass(this.clazz);

    // 设置构造参数
    if (!CollectionUtils.isEmpty(this.constructorArgs)) {
        ConstructorArgumentValues cav = new ConstructorArgumentValues();
        for (Object constructorArg : this.constructorArgs) {
            cav.addGenericArgumentValue(constructorArg);
        }
        bd.setConstructorArgumentValues(cav);
    }

    // 设置父 Bean
    if (this.parentName != null) {
        bd.setParentName(this.parentName);
    }

    // 创建 BeanWrapper 用于后续属性访问
    this.definitionWrapper = new BeanWrapperImpl(bd);
    return bd;
}
```

### 属性设置 - setProperty

```java
/**
 * 重写属性设置，支持动态属性和标准 BeanDefinition 属性
 */
@Override
public void setProperty(String property, @Nullable Object newValue) {
    // 1. 处理 parent 属性
    if (PARENT.equals(property)) {
        setParent(newValue);
    }
    else {
        AbstractBeanDefinition bd = getBeanDefinition();
        Assert.state(this.definitionWrapper != null, "BeanDefinition wrapper not initialized");

        // 2. 处理 autowire 属性
        if (AUTOWIRE.equals(property)) {
            if ("byName".equals(newValue)) {
                bd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
            }
            else if ("byType".equals(newValue)) {
                bd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            }
            else if ("constructor".equals(newValue)) {
                bd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            }
            else if (Boolean.TRUE.equals(newValue)) {
                bd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
            }
        }
        // 3. 处理 constructorArgs 属性
        else if (CONSTRUCTOR_ARGS.equals(property) && newValue instanceof List<?> args) {
            ConstructorArgumentValues cav = new ConstructorArgumentValues();
            for (Object arg : args) {
                cav.addGenericArgumentValue(arg);
            }
            bd.setConstructorArgumentValues(cav);
        }
        // 4. 处理 factoryBean 属性
        else if (FACTORY_BEAN.equals(property)) {
            if (newValue != null) {
                bd.setFactoryBeanName(newValue.toString());
            }
        }
        // 5. 处理 factoryMethod 属性
        else if (FACTORY_METHOD.equals(property)) {
            if (newValue != null) {
                bd.setFactoryMethodName(newValue.toString());
            }
        }
        // 6. 处理 initMethod 属性
        else if (INIT_METHOD.equals(property)) {
            if (newValue != null) {
                bd.setInitMethodName(newValue.toString());
            }
        }
        // 7. 处理 destroyMethod 属性
        else if (DESTROY_METHOD.equals(property)) {
            if (newValue != null) {
                bd.setDestroyMethodName(newValue.toString());
            }
        }
        // 8. 处理 singleton 属性
        else if (SINGLETON.equals(property)) {
            bd.setScope(Boolean.TRUE.equals(newValue) ?
                    BeanDefinition.SCOPE_SINGLETON : BeanDefinition.SCOPE_PROTOTYPE);
        }
        // 9. 其他可写属性直接通过 BeanWrapper 设置
        else if (this.definitionWrapper.isWritableProperty(property)) {
            this.definitionWrapper.setPropertyValue(property, newValue);
        }
        else {
            super.setProperty(property, newValue);
        }
    }
}
```

### 父 Bean 设置

```java
/**
 * 设置父 Bean，支持多种形式
 */
void setParent(@Nullable Object obj) {
    Assert.notNull(obj, "Parent bean cannot be set to a null runtime bean reference");
    if (obj instanceof String name) {
        // 字符串形式: parent = "parentBeanName"
        this.parentName = name;
    }
    else if (obj instanceof RuntimeBeanReference runtimeBeanReference) {
        // 引用形式: parent = ref("parentBeanName")
        this.parentName = runtimeBeanReference.getBeanName();
    }
    else if (obj instanceof GroovyBeanDefinitionWrapper wrapper) {
        // 包装器形式: parent = parentBeanWrapper
        this.parentName = wrapper.getBeanName();
    }
    getBeanDefinition().setParentName(this.parentName);
    getBeanDefinition().setAbstract(false);
}
```

### 添加属性

```java
/**
 * 添加属性到 Bean 定义
 */
GroovyBeanDefinitionWrapper addProperty(String propertyName, @Nullable Object propertyValue) {
    // 如果属性值是包装器，提取其中的 BeanDefinition（用于嵌套 Bean）
    if (propertyValue instanceof GroovyBeanDefinitionWrapper wrapper) {
        propertyValue = wrapper.getBeanDefinition();
    }
    getBeanDefinition().getPropertyValues().add(propertyName, propertyValue);
    return this;
}
```

---

## GroovyDynamicElementReader

### 类定义

```java
/**
 * 用于在 Groovy DSL 中读取 Spring XML 命名空间表达式
 * 允许在 Groovy DSL 中使用 Spring 的 XML 命名空间（如 aop:, context: 等）
 */
class GroovyDynamicElementReader extends GroovyObjectSupport
```

### 核心属性

```java
private final String rootNamespace;                    // 根命名空间前缀
private final Map<String, String> xmlNamespaces;       // 命名空间 URI 映射
private final BeanDefinitionParserDelegate delegate;   // XML 解析委托器
private final GroovyBeanDefinitionWrapper beanDefinition; // 当前 Bean 定义
protected final boolean decorating;                    // 是否为装饰模式
private boolean callAfterInvocation = true;            // 是否调用后置钩子
```

### invokeMethod - 处理命名空间元素

```java
/**
 * 处理方法调用，将 Groovy DSL 转换为 XML 元素并解析
 */
@Override
@Nullable
public Object invokeMethod(String name, Object obj) {
    Object[] args = (Object[]) obj;

    // 1. 处理 doCall 调用（入口方法）
    if (name.equals("doCall")) {
        @SuppressWarnings("unchecked")
        Closure<Object> callable = (Closure<Object>) args[0];
        callable.setResolveStrategy(Closure.DELEGATE_FIRST);
        callable.setDelegate(this);
        Object result = callable.call();

        if (this.callAfterInvocation) {
            afterInvocation();
            this.callAfterInvocation = false;
        }
        return result;
    }
    // 2. 处理命名空间元素（如 aop.config { ... }）
    else {
        // 使用 StreamingMarkupBuilder 构建 XML
        StreamingMarkupBuilder builder = new StreamingMarkupBuilder();
        String myNamespace = this.rootNamespace;
        Map<String, String> myNamespaces = this.xmlNamespaces;

        // 创建闭包来生成 XML
        @SuppressWarnings("serial")
        Closure<Object> callable = new Closure<>(this) {
            @Override
            public Object call(Object... arguments) {
                // 声明命名空间
                ((GroovyObject) getProperty("mkp")).invokeMethod("declareNamespace", new Object[] {myNamespaces});
                int len = args.length;
                if (len > 0 && args[len-1] instanceof Closure<?> callable) {
                    callable.setResolveStrategy(Closure.DELEGATE_FIRST);
                    callable.setDelegate(builder);
                }
                // 调用命名空间方法
                return ((GroovyObject) ((GroovyObject) getDelegate()).getProperty(myNamespace)).invokeMethod(name, args);
            }
        };

        callable.setResolveStrategy(Closure.DELEGATE_FIRST);
        callable.setDelegate(builder);

        // 生成 XML 字符串
        Writable writable = (Writable) builder.bind(callable);
        StringWriter sw = new StringWriter();
        try {
            writable.writeTo(sw);
        }
        catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        // 解析 XML 为 DOM 元素
        Element element = this.delegate.getReaderContext().readDocumentFromString(sw.toString()).getDocumentElement();
        this.delegate.initDefaults(element);

        // 根据模式处理：装饰或解析
        if (this.decorating) {
            // 装饰模式：增强现有 Bean 定义
            BeanDefinitionHolder holder = this.beanDefinition.getBeanDefinitionHolder();
            holder = this.delegate.decorateIfRequired(element, holder, null);
            this.beanDefinition.setBeanDefinitionHolder(holder);
        }
        else {
            // 解析模式：创建新 Bean 定义
            BeanDefinition beanDefinition = this.delegate.parseCustomElement(element);
            if (beanDefinition != null) {
                this.beanDefinition.setBeanDefinition((AbstractBeanDefinition) beanDefinition);
            }
        }

        if (this.callAfterInvocation) {
            afterInvocation();
            this.callAfterInvocation = false;
        }
        return element;
    }
}
```

---

## Groovy DSL 配置示例

### 基本 Bean 定义

```groovy
import org.springframework.context.support.GenericGroovyApplicationContext

def context = new GenericGroovyApplicationContext()
context.reader.beans {
    // 简单 Bean 定义
    dataSource(BasicDataSource) {
        driverClassName = "org.hsqldb.jdbcDriver"
        url = "jdbc:hsqldb:mem:grailsDB"
        username = "sa"
        password = ""
    }
}
context.refresh()
```

### 构造参数

```groovy
beans {
    // 位置构造参数
    knights(KnightsOfTheRoundTable, "Camelot") {
        quest = holyGrail
    }

    // 命名构造参数
    knights(KnightsOfTheRoundTable, "Camelot", leader: "lancelot", quest: holyGrail)
}
```

### Bean 引用

```groovy
beans {
    holyGrail(HolyGrailQuest)

    // 直接引用
    knights(KnightsOfTheRoundTable) {
        quest = holyGrail
    }

    // 使用 ref() 方法
    anotherKnight(KnightsOfTheRoundTable) {
        quest = ref("holyGrail")
    }

    // 引用父容器 Bean
    childBean(ChildClass) {
        parent = ref("parentBean", true)  // true 表示从父容器查找
    }
}
```

### 工厂方法和工厂 Bean

```groovy
beans {
    // 工厂 Bean
    myFactory(Bean1Factory)

    // 使用工厂 Bean 和工厂方法
    homer(myFactory) { bean ->
        bean.factoryMethod = "newInstance"
        person = "homer"
        age = 45
    }

    // 简写形式
    marge(myFactory: "newInstance") { bean ->
        person = "marge"
    }
}
```

### 抽象 Bean 和继承

```groovy
beans {
    // 抽象 Bean（仅闭包，无类）
    abstractBean {
        leader = "Lancelot"
    }

    // 抽象 Bean（带类）
    abstractKnight(KnightsOfTheRoundTable) { bean ->
        bean.'abstract' = true  // 注意：abstract 是 Groovy 关键字，需要用引号
        leader = "King Arthur"
    }

    // 继承抽象 Bean
    lancelot("lancelot") { bean ->
        bean.parent = ref("abstractKnight")
    }

    // 继承简单抽象 Bean
    concreteBean(ConcreteClass) { bean ->
        bean.parent = abstractBean
    }
}
```

### 嵌套 Bean

```groovy
beans {
    // 使用闭包定义嵌套 Bean
    marge(Bean2) {
        person = "marge"
        bean1 = { Bean1Impl b ->
            person = "homer"
            age = 45
        }
    }

    // 使用 bean() 方法定义嵌套 Bean
    lisa(Bean2) {
        bean1 = bean(Bean1Impl) {
            person = "lisa"
            age = 9
        }
    }

    // 带构造参数的嵌套 Bean
    bart(Bean2) {
        bean3 = bean(Bean3, "homer", ref("lisa")) {
            age = 45
        }
    }
}
```

### 集合属性

```groovy
beans {
    bart(Bean1Impl) {
        person = "bart"
        age = 11
    }

    lisa(Bean1Impl) {
        person = "lisa"
        age = 9
    }

    // List 属性
    marge(Bean2) {
        children = [bart, lisa]  // 直接引用其他 Bean
    }

    // Map 属性
    homer(Bean1Impl) {
        props = [overweight: "true", height: "1.8m"]
    }

    // 构造参数中的集合
    beanWithList(Bean5, [bart, lisa])
    beanWithMap(Bean6, [bart: bart, lisa: ref('lisa')])
}
```

### 作用域配置

```groovy
beans {
    // 单例（默认）
    singletonBean(Bean1Impl) { bean ->
        bean.singleton = true
    }

    // 原型
    prototypeBean(Bean1Impl) { bean ->
        bean.singleton = false  // 或 bean.scope = "prototype"
    }

    // 自定义作用域
    customScopedBean(Bean1Impl) { bean ->
        bean.scope = "request"
    }
}
```

### 生命周期方法

```groovy
beans {
    myBean(MyClass) { bean ->
        bean.initMethod = "init"
        bean.destroyMethod = "cleanup"
    }
}
```

### 自动装配

```groovy
beans {
    // 按名称自动装配
    bean1(Bean1Impl) { bean ->
        bean.autowire = "byName"
    }

    // 按类型自动装配
    bean2(Bean2) { bean ->
        bean.autowire = "byType"
    }

    // 构造器自动装配
    bean3(Bean3) { bean ->
        bean.autowire = "constructor"
    }
}
```

### 使用 Spring 命名空间

```groovy
beans {
    // 声明命名空间
    xmlns context: "http://www.springframework.org/schema/context"
    xmlns aop: "http://www.springframework.org/schema/aop"
    xmlns util: "http://www.springframework.org/schema/util"

    // 组件扫描
    context.'component-scan'( 'base-package': "com.example" )

    // AOP 配置
    aop.config("proxy-target-class": false) {
        aspect(id: "myAspect", ref: "aspectBean") {
            after method: "afterAdvice",
                  pointcut: "execution(* com.example.*.*(..))"
        }
    }

    // 作用域代理
    scopedBean(Bean1Impl) { bean ->
        bean.scope = "session"
        aop.'scoped-proxy'('proxy-target-class': false)
    }

    // util 命名空间
    util.list(id: 'myList') {
        value 'one'
        value 'two'
    }
}
```

### 导入其他配置文件

```groovy
beans {
    // 导入 XML 配置
    importBeans "classpath:config/application-context.xml"

    // 导入 Groovy 配置
    importBeans "classpath:config/services.groovy"

    // 使用 Ant 风格路径
    importBeans "classpath*:config/**/*.groovy"
}
```

### 完整示例

```groovy
import org.hibernate.SessionFactory
import org.apache.commons.dbcp.BasicDataSource

beans {
    // 声明命名空间
    xmlns context: "http://www.springframework.org/schema/context"
    xmlns tx: "http://www.springframework.org/schema/tx"

    // 组件扫描
    context.'component-scan'( 'base-package': "com.example.app" )

    // 数据源配置
    dataSource(BasicDataSource) { bean ->
        bean.destroyMethod = "close"
        driverClassName = "org.hsqldb.jdbcDriver"
        url = "jdbc:hsqldb:mem:testDB"
        username = "sa"
        password = ""
    }

    // SessionFactory
    sessionFactory(SessionFactory) {
        dataSource = dataSource
    }

    // 事务管理器
    transactionManager(HibernateTransactionManager) {
        sessionFactory = sessionFactory
    }

    // 事务注解支持
    tx.annotationDriven()

    // 服务层 Bean
    userService(UserServiceImpl) {
        userDao = ref("userDao")
        mailSender = ref("mailSender")
    }

    // 使用工厂方法的 Bean
    mailSender(JavaMailSenderImpl) { bean ->
        bean.factoryMethod = "getInstance"
        host = "smtp.example.com"
    }
}
```

---

## 与 XML/JavaConfig 对比

### 语法对比

| 特性 | XML 配置 | Groovy DSL | JavaConfig |
|------|----------|------------|------------|
| **Bean 定义** | `<bean id="x" class="Y"/>` | `x(Y) { }` | `@Bean public Y x() { return new Y(); }` |
| **属性设置** | `<property name="p" value="v"/>` | `p = v` | `y.setP(v);` |
| **构造参数** | `<constructor-arg value="v"/>` | `BeanClass(v)` | `new BeanClass(v)` |
| **Bean 引用** | `<property ref="other"/>` | `prop = other` | `y.setProp(other());` |
| **集合** | `<list><ref bean="x"/></list>` | `[x, y]` | `Arrays.asList(x(), y())` |
| **条件配置** | Spring Profiles | Groovy 条件 | `@Profile` + if 语句 |
| **类型安全** | 运行时 | 编译时（部分） | 编译时 |

### 代码量对比

**XML 配置：**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource" destroy-method="close">
        <property name="driverClassName" value="org.hsqldb.jdbcDriver"/>
        <property name="url" value="jdbc:hsqldb:mem:grailsDB"/>
        <property name="username" value="sa"/>
        <property name="password" value=""/>
    </bean>

    <bean id="sessionFactory" class="org.hibernate.SessionFactory">
        <property name="dataSource" ref="dataSource"/>
    </bean>

    <bean id="myService" class="com.example.MyService">
        <property name="sessionFactory" ref="sessionFactory"/>
    </bean>
</beans>
```

**Groovy DSL：**
```groovy
beans {
    dataSource(BasicDataSource) { bean ->
        bean.destroyMethod = "close"
        driverClassName = "org.hsqldb.jdbcDriver"
        url = "jdbc:hsqldb:mem:grailsDB"
        username = "sa"
        password = ""
    }

    sessionFactory(SessionFactory) {
        dataSource = dataSource
    }

    myService(MyService) {
        sessionFactory = sessionFactory
    }
}
```

**JavaConfig：**
```java
@Configuration
public class AppConfig {

    @Bean(destroyMethod = "close")
    public BasicDataSource dataSource() {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.hsqldb.jdbcDriver");
        ds.setUrl("jdbc:hsqldb:mem:grailsDB");
        ds.setUsername("sa");
        ds.setPassword("");
        return ds;
    }

    @Bean
    public SessionFactory sessionFactory() {
        SessionFactory sf = new SessionFactory();
        sf.setDataSource(dataSource());
        return sf;
    }

    @Bean
    public MyService myService() {
        MyService service = new MyService();
        service.setSessionFactory(sessionFactory());
        return service;
    }
}
```

### 优缺点对比

**Groovy DSL 优点：**
1. **简洁** - 比 XML 更少的样板代码
2. **可读性** - 类似自然语言的配置语法
3. **灵活性** - 可以使用 Groovy 的全部语言特性（条件、循环、闭包等）
4. **类型安全** - 相比 XML 有更好的编译时检查
5. **与 XML 兼容** - 可以在同一个项目中混合使用

**Groovy DSL 缺点：**
1. **学习成本** - 需要了解 Groovy 语言
2. **IDE 支持** - 相比 JavaConfig，IDE 的智能提示可能不够完善
3. **运行时开销** - 需要 Groovy 运行时环境
4. **调试困难** - 配置错误可能在运行时才能发现

**适用场景：**
- 需要高度灵活的配置逻辑时
- 团队熟悉 Groovy 时
- 从 Grails 框架迁移的项目
- 需要与遗留 XML 配置混合使用时

---

## 相关类

- `GenericGroovyApplicationContext` - 支持 Groovy DSL 的 ApplicationContext 实现
- `GroovyWebApplicationContext` - Web 环境下的 Groovy ApplicationContext
- `GenericGroovyXmlContextLoader` - 测试框架中的 Groovy 上下文加载器

## 源码文件位置

- `spring-beans/src/main/java/org/springframework/beans/factory/groovy/GroovyBeanDefinitionReader.java`
- `spring-beans/src/main/java/org/springframework/beans/factory/groovy/GroovyBeanDefinitionWrapper.java`
- `spring-beans/src/main/java/org/springframework/beans/factory/groovy/GroovyDynamicElementReader.java`
- `spring-beans/src/main/java/org/springframework/beans/factory/groovy/package-info.java`
