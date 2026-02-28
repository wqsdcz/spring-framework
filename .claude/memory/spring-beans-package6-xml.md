# Spring Framework - org.springframework.beans.factory.xml 包详解

## 目录

1. [概述](#概述)
2. [核心组件架构](#核心组件架构)
3. [XML 读取与解析流程](#xml-读取与解析流程)
4. [命名空间处理机制](#命名空间处理机制)
5. [标准命名空间实现](#标准命名空间实现)
6. [核心源码分析](#核心源码分析)
7. [扩展自定义命名空间](#扩展自定义命名空间)

---

## 概述

`org.springframework.beans.factory.xml` 包是 Spring Framework 中负责 XML 配置文件解析的核心包。它提供了将 Spring XML 配置文件转换为 `BeanDefinition` 对象的完整机制，支持标准 beans 命名空间以及可扩展的自定义命名空间。

### 主要功能

- **XML 文档加载**：使用 JAXP 加载和验证 XML 文档
- **Bean 定义解析**：将 XML 元素转换为 `BeanDefinition` 对象
- **命名空间扩展**：支持自定义 XML 命名空间（如 `context:`, `util:`, `p:` 等）
- **配置文件导入**：支持 `<import>` 标签导入其他配置文件
- **属性占位符**：支持 `${...}` 形式的属性占位符解析

---

## 核心组件架构

### 1. 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        XmlBeanDefinitionReader                              │
│  - 入口类，负责加载 XML 资源并启动解析流程                                     │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     DefaultBeanDefinitionDocumentReader                     │
│  - 实际的文档解析器，遍历 DOM 树并处理每个元素                                  │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
┌────────────────────────┐ ┌──────────────┐ ┌────────────────────────┐
│ BeanDefinitionParser   │ │ Namespace    │ │ BeanDefinitionParser   │
│ Delegate               │ │ Handler      │ │ (Custom)               │
│                        │ │ Resolver     │ │                        │
│ - parseBeanDefinition  │ │              │ │ - parse()              │
│ - parseCustomElement   │ │ - resolve()  │ │ - decorate()           │
│ - parseList/Set/Map    │ │ - init()     │ │                        │
└────────────────────────┘ └──────────────┘ └────────────────────────┘
```

### 2. 核心类职责表

| 类名 | 职责 | 关键方法 |
|------|------|----------|
| `XmlBeanDefinitionReader` | XML 资源读取入口 | `loadBeanDefinitions()`, `doLoadBeanDefinitions()` |
| `DefaultBeanDefinitionDocumentReader` | DOM 文档遍历解析 | `registerBeanDefinitions()`, `parseBeanDefinitions()` |
| `BeanDefinitionParserDelegate` | 标准 beans 命名空间解析 | `parseBeanDefinitionElement()`, `parseCustomElement()` |
| `NamespaceHandlerResolver` | 命名空间处理器解析 | `resolve()` |
| `DefaultNamespaceHandlerResolver` | 默认实现，从 spring.handlers 加载 | `getHandlerMappings()` |
| `NamespaceHandler` | 自定义命名空间处理接口 | `init()`, `parse()`, `decorate()` |
| `NamespaceHandlerSupport` | 命名空间处理器基类 | `registerBeanDefinitionParser()` |
| `BeanDefinitionParser` | 元素解析器接口 | `parse()` |
| `BeanDefinitionDecorator` | 定义装饰器接口 | `decorate()` |
| `ParserContext` | 解析上下文 | 包含 readerContext 和 delegate |
| `XmlReaderContext` | XML 读取上下文 | 包含 NamespaceHandlerResolver |

---

## XML 读取与解析流程

### 1. 完整解析流程图

```
ApplicationContext
       │
       ▼
┌─────────────────────────────────────┐
│  XmlBeanDefinitionReader            │
│  loadBeanDefinitions(Resource)      │
└─────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  1. 检测循环导入                      │
│     (resourcesCurrentlyBeingLoaded) │
└─────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  2. 加载 XML 为 DOM Document        │
│     doLoadDocument()                │
│     - DocumentLoader.loadDocument() │
│     - JAXP DocumentBuilder.parse()  │
└─────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  3. 注册 Bean 定义                   │
│     registerBeanDefinitions()       │
│     - 创建 BeanDefinitionDocumentReader
│     - 创建 XmlReaderContext         │
└─────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  DefaultBeanDefinitionDocumentReader│
│  registerBeanDefinitions()          │
└─────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  4. 解析根元素                       │
│     doRegisterBeanDefinitions()     │
│     - 处理 profile 属性              │
│     - 创建 BeanDefinitionParserDelegate
└─────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  5. 遍历子元素                       │
│     parseBeanDefinitions()          │
│     - 判断默认/自定义命名空间         │
└─────────────────────────────────────┘
       │
       ├── 默认命名空间 ───────────────┐
       │                               ▼
       │              ┌─────────────────────────────────────┐
       │              │  parseDefaultElement()              │
       │              │  - import: 导入其他配置文件          │
       │              │  - alias: 注册别名                   │
       │              │  - bean: 解析 Bean 定义              │
       │              │  - beans: 递归解析嵌套 beans         │
       │              └─────────────────────────────────────┘
       │                               │
       │                               ▼
       │              ┌─────────────────────────────────────┐
       │              │  delegate.parseBeanDefinitionElement│
       │              │  - 解析 id, name, class 属性         │
       │              │  - 解析 scope, lazy-init 等          │
       │              │  - 解析 constructor-arg              │
       │              │  - 解析 property                     │
       │              └─────────────────────────────────────┘
       │
       └── 自定义命名空间 ─────────────┐
                                       ▼
                      ┌─────────────────────────────────────┐
                      │  delegate.parseCustomElement()      │
                      │  - 获取 namespaceUri                │
                      │  - NamespaceHandlerResolver.resolve │
                      │  - NamespaceHandler.parse()         │
                      └─────────────────────────────────────┘
```

### 2. XmlBeanDefinitionReader 核心源码分析

```java
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

    // 验证模式常量
    public static final int VALIDATION_NONE = XmlValidationModeDetector.VALIDATION_NONE;  // 不验证
    public static final int VALIDATION_AUTO = XmlValidationModeDetector.VALIDATION_AUTO;  // 自动检测
    public static final int VALIDATION_DTD = XmlValidationModeDetector.VALIDATION_DTD;    // DTD 验证
    public static final int VALIDATION_XSD = XmlValidationModeDetector.VALIDATION_XSD;    // XSD 验证

    // 核心组件
    private int validationMode = VALIDATION_AUTO;           // 默认自动检测验证模式
    private boolean namespaceAware = false;                 // 是否命名空间感知
    private Class<? extends BeanDefinitionDocumentReader> documentReaderClass =
            DefaultBeanDefinitionDocumentReader.class;      // 文档读取器类
    private DocumentLoader documentLoader = new DefaultDocumentLoader();  // 文档加载器
    private NamespaceHandlerResolver namespaceHandlerResolver;  // 命名空间处理器解析器

    /**
     * 加载 Bean 定义的入口方法
     * 步骤：
     * 1. 检测循环导入（使用 ThreadLocal 存储当前正在加载的资源）
     * 2. 将 Resource 包装为 EncodedResource（支持编码）
     * 3. 获取 InputStream 并创建 SAX InputSource
     * 4. 调用 doLoadBeanDefinitions 实际加载
     */
    public int loadBeanDefinitions(EncodedResource encodedResource) {
        Assert.notNull(encodedResource, "EncodedResource must not be null");

        // 获取当前线程正在加载的资源集合
        Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();

        // 检测循环导入：如果资源已经在加载中，抛出异常
        if (!currentResources.add(encodedResource)) {
            throw new BeanDefinitionStoreException(
                "Detected cyclic loading of " + encodedResource + " - check your import definitions!");
        }

        try (InputStream inputStream = encodedResource.getResource().getInputStream()) {
            InputSource inputSource = new InputSource(inputStream);
            if (encodedResource.getEncoding() != null) {
                inputSource.setEncoding(encodedResource.getEncoding());
            }
            // 实际加载 Bean 定义
            return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
        }
        finally {
            // 清理 ThreadLocal
            currentResources.remove(encodedResource);
            if (currentResources.isEmpty()) {
                this.resourcesCurrentlyBeingLoaded.remove();
            }
        }
    }

    /**
     * 实际加载 Bean 定义的核心方法
     * 步骤：
     * 1. 加载 XML 文档为 DOM Document
     * 2. 注册文档中的 Bean 定义
     */
    protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource) {
        try {
            // 1. 加载 XML 为 DOM Document
            Document doc = doLoadDocument(inputSource, resource);

            // 2. 注册 Bean 定义，返回注册的 Bean 定义数量
            int count = registerBeanDefinitions(doc, resource);

            if (logger.isDebugEnabled()) {
                logger.debug("Loaded " + count + " bean definitions from " + resource);
            }
            return count;
        }
        catch (BeanDefinitionStoreException ex) {
            throw ex;
        }
        catch (SAXParseException ex) {
            // 包装 SAX 解析异常，添加行号信息
            throw new XmlBeanDefinitionStoreException(resource.getDescription(),
                "Line " + ex.getLineNumber() + " in XML document from " + resource + " is invalid", ex);
        }
        // ... 其他异常处理
    }

    /**
     * 加载 XML 文档
     * 使用配置的 DocumentLoader（默认 DefaultDocumentLoader）
     */
    protected Document doLoadDocument(InputSource inputSource, Resource resource) throws Exception {
        return this.documentLoader.loadDocument(
            inputSource,                    // XML 输入源
            getEntityResolver(),            // 实体解析器（用于解析 DTD/XSD）
            this.errorHandler,              // 错误处理器
            getValidationModeForResource(resource),  // 验证模式
            isNamespaceAware()              // 是否命名空间感知
        );
    }

    /**
     * 注册 Bean 定义
     * 创建 BeanDefinitionDocumentReader 并调用其 registerBeanDefinitions 方法
     */
    public int registerBeanDefinitions(Document doc, Resource resource) {
        // 创建文档读取器（默认 DefaultBeanDefinitionDocumentReader）
        BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();

        // 记录注册前的 Bean 定义数量
        int countBefore = getRegistry().getBeanDefinitionCount();

        // 创建读取上下文并注册 Bean 定义
        documentReader.registerBeanDefinitions(doc, createReaderContext(resource));

        // 返回本次注册的 Bean 定义数量
        return getRegistry().getBeanDefinitionCount() - countBefore;
    }

    /**
     * 创建 XmlReaderContext
     * 包含解析过程中需要的所有上下文信息
     */
    public XmlReaderContext createReaderContext(Resource resource) {
        return new XmlReaderContext(
            resource,                       // 当前资源
            this.problemReporter,           // 问题报告器
            this.eventListener,             // 事件监听器
            this.sourceExtractor,           // 源提取器
            this,                           // 当前 reader
            getNamespaceHandlerResolver()   // 命名空间处理器解析器
        );
    }
}
```

### 3. DefaultBeanDefinitionDocumentReader 核心源码分析

```java
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {

    // 标准元素名称常量
    public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;  // "bean"
    public static final String NESTED_BEANS_ELEMENT = "beans";   // 嵌套 beans
    public static final String ALIAS_ELEMENT = "alias";          // 别名
    public static final String IMPORT_ELEMENT = "import";        // 导入
    public static final String RESOURCE_ATTRIBUTE = "resource";  // 资源属性
    public static final String PROFILE_ATTRIBUTE = "profile";    // profile 属性

    @Nullable
    private XmlReaderContext readerContext;

    @Nullable
    private BeanDefinitionParserDelegate delegate;

    /**
     * 注册 Bean 定义的入口
     */
    @Override
    public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
        this.readerContext = readerContext;
        // 从文档根元素开始解析
        doRegisterBeanDefinitions(doc.getDocumentElement());
    }

    /**
     * 递归注册 Bean 定义
     * 支持嵌套的 <beans> 元素
     */
    protected void doRegisterBeanDefinitions(Element root) {
        // 保存父 delegate（支持嵌套 <beans> 时的默认属性继承）
        BeanDefinitionParserDelegate parent = this.delegate;

        // 创建新的 delegate，传入父 delegate 用于默认属性继承
        BeanDefinitionParserDelegate current = createDelegate(getReaderContext(), root, parent);
        this.delegate = current;

        // 处理 profile 属性（仅对默认命名空间有效）
        if (current.isDefaultNamespace(root)) {
            String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
            if (StringUtils.hasText(profileSpec)) {
                String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
                    profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);

                // 检查当前环境是否匹配指定的 profile
                if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Skipped XML bean definition file due to specified profiles [" +
                            profileSpec + "] not matching: " + getReaderContext().getResource());
                    }
                    return;  // profile 不匹配，跳过此文件
                }
            }
        }

        // 前置处理（供子类扩展）
        preProcessXml(root);

        // 解析 Bean 定义
        parseBeanDefinitions(root, current);

        // 后置处理（供子类扩展）
        postProcessXml(root);

        // 恢复父 delegate
        this.delegate = parent;
    }

    /**
     * 解析 Bean 定义
     * 区分默认命名空间和自定义命名空间
     */
    protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
        // 如果根元素是默认命名空间（beans）
        if (delegate.isDefaultNamespace(root)) {
            NodeList nl = root.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if (node instanceof Element ele) {
                    // 判断子元素是默认命名空间还是自定义命名空间
                    if (delegate.isDefaultNamespace(ele)) {
                        parseDefaultElement(ele, delegate);  // 解析标准元素
                    }
                    else {
                        delegate.parseCustomElement(ele);     // 解析自定义元素
                    }
                }
            }
        }
        else {
            // 根元素本身就是自定义命名空间
            delegate.parseCustomElement(root);
        }
    }

    /**
     * 解析标准命名空间元素
     * 包括：import, alias, bean, beans
     */
    private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
        if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
            // <import resource="..."/>
            importBeanDefinitionResource(ele);
        }
        else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
            // <alias name="..." alias="..."/>
            processAliasRegistration(ele);
        }
        else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
            // <bean id="..." class="..."/>
            processBeanDefinition(ele, delegate);
        }
        else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
            // 嵌套 <beans>，递归解析
            doRegisterBeanDefinitions(ele);
        }
    }

    /**
     * 处理 <import> 元素
     * 支持导入其他 XML 配置文件
     */
    protected void importBeanDefinitionResource(Element ele) {
        String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
        if (!StringUtils.hasText(location)) {
            getReaderContext().error("Resource location must not be empty", ele);
            return;
        }

        // 解析占位符，如 ${user.dir}
        location = getReaderContext().getEnvironment().resolveRequiredPlaceholders(location);

        Set<Resource> actualResources = new LinkedHashSet<>(4);

        // 判断是绝对路径还是相对路径
        boolean absoluteLocation = false;
        try {
            absoluteLocation = ResourcePatternUtils.isUrl(location) ||
                ResourceUtils.toURI(location).isAbsolute();
        }
        catch (URISyntaxException ex) {
            // 无法转换为 URI，视为相对路径
        }

        if (absoluteLocation) {
            // 绝对路径：直接加载
            try {
                int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
                if (logger.isTraceEnabled()) {
                    logger.trace("Imported " + importCount + " bean definitions from URL location [" + location + "]");
                }
            }
            catch (BeanDefinitionStoreException ex) {
                getReaderContext().error(
                    "Failed to import bean definitions from URL location [" + location + "]", ele, ex);
            }
        }
        else {
            // 相对路径：相对于当前文件
            try {
                int importCount;
                Resource relativeResource = getReaderContext().getResource().createRelative(location);
                if (relativeResource.exists()) {
                    importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
                    actualResources.add(relativeResource);
                }
                else {
                    // 尝试使用绝对路径加载
                    String baseLocation = getReaderContext().getResource().getURL().toString();
                    importCount = getReaderContext().getReader().loadBeanDefinitions(
                        StringUtils.applyRelativePath(baseLocation, location), actualResources);
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("Imported " + importCount + " bean definitions from relative location [" + location + "]");
                }
            }
            catch (IOException ex) {
                getReaderContext().error("Failed to resolve current resource location", ele, ex);
            }
        }

        // 触发导入处理事件
        Resource[] actResArray = actualResources.toArray(new Resource[0]);
        getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
    }

    /**
     * 处理 <bean> 元素
     * 解析 Bean 定义并注册到容器
     */
    protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
        // 1. 解析 Bean 定义元素，返回 BeanDefinitionHolder
        BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);

        if (bdHolder != null) {
            // 2. 如果需要，装饰 Bean 定义（处理自定义属性/子元素）
            bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);

            try {
                // 3. 注册最终的 Bean 定义
                BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
            }
            catch (BeanDefinitionStoreException ex) {
                getReaderContext().error("Failed to register bean definition with name '" +
                    bdHolder.getBeanName() + "'", ele, ex);
            }

            // 4. 触发组件注册事件
            getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
        }
    }
}
```

---

## 命名空间处理机制

### 1. 命名空间架构

Spring XML 配置支持两种命名空间：

- **默认命名空间**：`http://www.springframework.org/schema/beans`，处理标准 `<bean>`、`<import>`、`<alias>` 等元素
- **自定义命名空间**：如 `context:`、`util:`、`p:`、`c:` 等，通过 `NamespaceHandler` 扩展

### 2. 命名空间解析流程

```
XML Element
     │
     ▼
isDefaultNamespace()?
     │
     ├── Yes ──────────────────────┐
     │                              ▼
     │              ┌─────────────────────────────┐
     │              │ parseDefaultElement()       │
     │              │ - importBeanDefinitionResource
     │              │ - processAliasRegistration  │
     │              │ - processBeanDefinition     │
     │              └─────────────────────────────┘
     │                              │
     │                              ▼
     │              ┌─────────────────────────────┐
     │              │ delegate.parseBeanDefinition│
     │              │ parseBeanDefinitionElement()│
     │              │ - 解析 id, name             │
     │              │ - 解析 class, parent        │
     │              │ - 解析 scope, abstract...   │
     │              └─────────────────────────────┘
     │
     └── No ───────────────────────┐
                                   ▼
                   ┌─────────────────────────────┐
                   │ delegate.parseCustomElement()│
                   │                              │
                   │ 1. 获取 namespaceUri          │
                   │ 2. 获取 readerContext         │
                   │ 3. 调用 resolver.resolve()   │
                   └─────────────────────────────┘
                                   │
                                   ▼
                   ┌─────────────────────────────┐
                   │ DefaultNamespaceHandlerResolver
                   │ resolve(namespaceUri)        │
                   │                              │
                   │ 1. 从 handlerMappings 查找   │
                   │ 2. 如果找到类名，实例化       │
                   │ 3. 调用 handler.init()       │
                   │ 4. 缓存 handler 实例         │
                   └─────────────────────────────┘
                                   │
                                   ▼
                   ┌─────────────────────────────┐
                   │ NamespaceHandler.parse()     │
                   │ 或 decorate()                │
                   │                              │
                   │ (如 UtilNamespaceHandler,    │
                   │  ContextNamespaceHandler)    │
                   └─────────────────────────────┘
```

### 3. DefaultNamespaceHandlerResolver 核心源码

```java
public class DefaultNamespaceHandlerResolver implements NamespaceHandlerResolver {

    // 默认映射文件位置
    public static final String DEFAULT_HANDLER_MAPPINGS_LOCATION = "META-INF/spring.handlers";

    @Nullable
    private volatile Map<String, Object> handlerMappings;  // 延迟加载的处理器映射

    /**
     * 解析命名空间 URI 对应的 NamespaceHandler
     * 使用延迟加载和缓存机制
     */
    @Override
    @Nullable
    public NamespaceHandler resolve(String namespaceUri) {
        // 获取处理器映射（延迟加载）
        Map<String, Object> handlerMappings = getHandlerMappings();

        // 查找对应的处理器
        Object handlerOrClassName = handlerMappings.get(namespaceUri);

        if (handlerOrClassName == null) {
            return null;  // 未找到处理器
        }
        else if (handlerOrClassName instanceof NamespaceHandler namespaceHandler) {
            // 已经是实例，直接返回（缓存）
            return namespaceHandler;
        }
        else {
            // 是类名字符串，需要实例化
            String className = (String) handlerOrClassName;
            try {
                Class<?> handlerClass = ClassUtils.forName(className, this.classLoader);

                // 检查是否实现了 NamespaceHandler 接口
                if (!NamespaceHandler.class.isAssignableFrom(handlerClass)) {
                    throw new FatalBeanException("Class [" + className + "] for namespace [" +
                        namespaceUri + "] does not implement the [" +
                        NamespaceHandler.class.getName() + "] interface");
                }

                // 实例化处理器
                NamespaceHandler namespaceHandler = (NamespaceHandler) BeanUtils.instantiateClass(handlerClass);

                // 初始化处理器（注册各个元素的解析器）
                namespaceHandler.init();

                // 缓存实例
                handlerMappings.put(namespaceUri, namespaceHandler);

                return namespaceHandler;
            }
            catch (ClassNotFoundException ex) {
                throw new FatalBeanException("Could not find NamespaceHandler class [" +
                    className + "] for namespace [" + namespaceUri + "]", ex);
            }
        }
    }

    /**
     * 延迟加载处理器映射
     * 从 classpath 中所有 META-INF/spring.handlers 文件加载
     */
    private Map<String, Object> getHandlerMappings() {
        Map<String, Object> handlerMappings = this.handlerMappings;

        if (handlerMappings == null) {
            synchronized (this) {
                handlerMappings = this.handlerMappings;
                if (handlerMappings == null) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Loading NamespaceHandler mappings from [" +
                            this.handlerMappingsLocation + "]");
                    }

                    try {
                        // 加载所有 spring.handlers 文件
                        Properties mappings = PropertiesLoaderUtils.loadAllProperties(
                            this.handlerMappingsLocation, this.classLoader);

                        if (logger.isTraceEnabled()) {
                            logger.trace("Loaded NamespaceHandler mappings: " + mappings);
                        }

                        // 转换为 ConcurrentHashMap（线程安全）
                        handlerMappings = new ConcurrentHashMap<>(mappings.size());
                        CollectionUtils.mergePropertiesIntoMap(mappings, handlerMappings);

                        this.handlerMappings = handlerMappings;
                    }
                    catch (IOException ex) {
                        throw new IllegalStateException(
                            "Unable to load NamespaceHandler mappings from location [" +
                            this.handlerMappingsLocation + "]", ex);
                    }
                }
            }
        }

        return handlerMappings;
    }
}
```

### 4. NamespaceHandlerSupport 核心源码

```java
public abstract class NamespaceHandlerSupport implements NamespaceHandler {

    // 存储元素名到 BeanDefinitionParser 的映射
    private final Map<String, BeanDefinitionParser> parsers = new HashMap<>();

    // 存储元素名到 BeanDefinitionDecorator 的映射
    private final Map<String, BeanDefinitionDecorator> decorators = new HashMap<>();

    // 存储属性名到 BeanDefinitionDecorator 的映射
    private final Map<String, BeanDefinitionDecorator> attributeDecorators = new HashMap<>();

    /**
     * 解析元素
     * 根据元素名查找对应的 BeanDefinitionParser 进行解析
     */
    @Override
    @Nullable
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        BeanDefinitionParser parser = findParserForElement(element, parserContext);
        return (parser != null ? parser.parse(element, parserContext) : null);
    }

    @Nullable
    private BeanDefinitionParser findParserForElement(Element element, ParserContext parserContext) {
        // 获取元素的本地名（不含命名空间前缀）
        String localName = parserContext.getDelegate().getLocalName(element);

        // 从映射中查找解析器
        BeanDefinitionParser parser = this.parsers.get(localName);

        if (parser == null) {
            parserContext.getReaderContext().fatal(
                "Cannot locate BeanDefinitionParser for element [" + localName + "]", element);
        }

        return parser;
    }

    /**
     * 装饰 BeanDefinition
     * 支持元素装饰和属性装饰
     */
    @Override
    @Nullable
    public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
        BeanDefinitionDecorator decorator = findDecoratorForNode(node, parserContext);
        return (decorator != null ? decorator.decorate(node, definition, parserContext) : null);
    }

    @Nullable
    private BeanDefinitionDecorator findDecoratorForNode(Node node, ParserContext parserContext) {
        BeanDefinitionDecorator decorator = null;
        String localName = parserContext.getDelegate().getLocalName(node);

        if (node instanceof Element) {
            decorator = this.decorators.get(localName);
        }
        else if (node instanceof Attr) {
            decorator = this.attributeDecorators.get(localName);
        }
        else {
            parserContext.getReaderContext().fatal(
                "Cannot decorate based on Nodes of type [" + node.getClass().getName() + "]", node);
        }

        if (decorator == null) {
            parserContext.getReaderContext().fatal("Cannot locate BeanDefinitionDecorator for " +
                (node instanceof Element ? "element" : "attribute") + " [" + localName + "]", node);
        }

        return decorator;
    }

    /**
     * 注册 BeanDefinitionParser
     * 子类在 init() 方法中调用此方法注册各个元素的解析器
     */
    protected final void registerBeanDefinitionParser(String elementName, BeanDefinitionParser parser) {
        this.parsers.put(elementName, parser);
    }

    /**
     * 注册 BeanDefinitionDecorator（用于元素装饰）
     */
    protected final void registerBeanDefinitionDecorator(String elementName, BeanDefinitionDecorator dec) {
        this.decorators.put(elementName, dec);
    }

    /**
     * 注册 BeanDefinitionDecorator（用于属性装饰）
     */
    protected final void registerBeanDefinitionDecoratorForAttribute(String attrName, BeanDefinitionDecorator dec) {
        this.attributeDecorators.put(attrName, dec);
    }
}
```

---

## 标准命名空间实现

### 1. util 命名空间

`UtilNamespaceHandler` 提供了对集合类型的便捷配置支持。

#### spring.handlers 配置

```properties
http\://www.springframework.org/schema/util=org.springframework.beans.factory.xml.UtilNamespaceHandler
```

#### 支持的元素

| 元素 | 对应类 | 功能 |
|------|--------|------|
| `<util:constant>` | `FieldRetrievingFactoryBean` | 引用类的静态常量 |
| `<util:property-path>` | `PropertyPathFactoryBean` | 引用其他 Bean 的属性路径 |
| `<util:list>` | `ListFactoryBean` | 创建 List 集合 |
| `<util:set>` | `SetFactoryBean` | 创建 Set 集合 |
| `<util:map>` | `MapFactoryBean` | 创建 Map 集合 |
| `<util:properties>` | `PropertiesFactoryBean` | 创建 Properties 对象 |

#### UtilNamespaceHandler 源码

```java
public class UtilNamespaceHandler extends NamespaceHandlerSupport {

    private static final String SCOPE_ATTRIBUTE = "scope";

    @Override
    public void init() {
        // 注册各个元素的解析器
        registerBeanDefinitionParser("constant", new ConstantBeanDefinitionParser());
        registerBeanDefinitionParser("property-path", new PropertyPathBeanDefinitionParser());
        registerBeanDefinitionParser("list", new ListBeanDefinitionParser());
        registerBeanDefinitionParser("set", new SetBeanDefinitionParser());
        registerBeanDefinitionParser("map", new MapBeanDefinitionParser());
        registerBeanDefinitionParser("properties", new PropertiesBeanDefinitionParser());
    }

    /**
     * <util:list> 解析器
     * 示例：<util:list id="myList" list-class="java.util.ArrayList">
     *          <value>item1</value>
     *          <ref bean="anotherBean"/>
     *      </util:list>
     */
    private static class ListBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

        @Override
        protected Class<?> getBeanClass(Element element) {
            return ListFactoryBean.class;  // 使用 ListFactoryBean 创建 List
        }

        @Override
        protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
            // 解析子元素为 List
            List<Object> parsedList = parserContext.getDelegate().parseListElement(
                element, builder.getRawBeanDefinition());
            builder.addPropertyValue("sourceList", parsedList);

            // 解析 list-class 属性（目标 List 类型）
            String listClass = element.getAttribute("list-class");
            if (StringUtils.hasText(listClass)) {
                builder.addPropertyValue("targetListClass", listClass);
            }

            // 解析 scope 属性
            String scope = element.getAttribute(SCOPE_ATTRIBUTE);
            if (StringUtils.hasLength(scope)) {
                builder.setScope(scope);
            }
        }
    }

    /**
     * <util:constant> 解析器
     * 示例：<util:constant id="maxValue" static-field="java.lang.Integer.MAX_VALUE"/>
     */
    private static class ConstantBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

        @Override
        protected Class<?> getBeanClass(Element element) {
            return FieldRetrievingFactoryBean.class;
        }

        @Override
        protected String resolveId(Element element, AbstractBeanDefinition definition,
                ParserContext parserContext) {
            String id = super.resolveId(element, definition, parserContext);
            if (!StringUtils.hasText(id)) {
                // 如果没有指定 id，使用 static-field 值作为 id
                id = element.getAttribute("static-field");
            }
            return id;
        }
    }

    // 其他解析器实现类似...
}
```

#### XML 配置示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:util="http://www.springframework.org/schema/util"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           https://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/util
           https://www.springframework.org/schema/util/spring-util.xsd">

    <!-- 常量引用 -->
    <util:constant id="maxConnections"
                   static-field="com.example.Constants.MAX_CONNECTIONS"/>

    <!-- List 集合 -->
    <util:list id="emailList" list-class="java.util.ArrayList">
        <value>admin@example.com</value>
        <value>support@example.com</value>
        <ref bean="customEmail"/>
    </util:list>

    <!-- Set 集合 -->
    <util:set id="uniqueNames" set-class="java.util.HashSet">
        <value>Alice</value>
        <value>Bob</value>
        <value>Alice</value>  <!-- 重复值会被过滤 -->
    </util:set>

    <!-- Map 集合 -->
    <util:map id="configMap" map-class="java.util.HashMap">
        <entry key="host" value="localhost"/>
        <entry key="port" value="8080"/>
        <entry key="dataSource" value-ref="dataSource"/>
    </util:map>

    <!-- Properties -->
    <util:properties id="jdbcProps" location="classpath:jdbc.properties"/>

    <!-- 属性路径引用 -->
    <util:property-path id="dbHost" path="dataSource.host"/>

</beans>
```

### 2. context 命名空间

`ContextNamespaceHandler` 位于 `spring-context` 模块，提供应用上下文相关的配置支持。

#### spring.handlers 配置

```properties
http\://www.springframework.org/schema/context=org.springframework.context.config.ContextNamespaceHandler
```

#### 支持的元素

| 元素 | 功能 |
|------|------|
| `<context:property-placeholder>` | 配置属性占位符解析器 |
| `<context:property-override>` | 配置属性覆盖器 |
| `<context:annotation-config>` | 启用注解配置（@Autowired, @PostConstruct 等）|
| `<context:component-scan>` | 启用组件扫描（@Component, @Service 等）|
| `<context:load-time-weaver>` | 配置加载时织入 |
| `<context:spring-configured>` | 启用 Spring 配置的 AspectJ 支持 |
| `<context:mbean-export>` | 导出 MBean 到 JMX |
| `<context:mbean-server>` | 配置 MBean 服务器 |

#### ContextNamespaceHandler 源码

```java
public class ContextNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        // 属性占位符解析器
        registerBeanDefinitionParser("property-placeholder", new PropertyPlaceholderBeanDefinitionParser());

        // 属性覆盖器
        registerBeanDefinitionParser("property-override", new PropertyOverrideBeanDefinitionParser());

        // 注解配置（处理 @Autowired, @Required, @PostConstruct, @PreDestroy 等）
        registerBeanDefinitionParser("annotation-config", new AnnotationConfigBeanDefinitionParser());

        // 组件扫描（处理 @Component, @Repository, @Service, @Controller 等）
        registerBeanDefinitionParser("component-scan", new ComponentScanBeanDefinitionParser());

        // 加载时织入
        registerBeanDefinitionParser("load-time-weaver", new LoadTimeWeaverBeanDefinitionParser());

        // Spring 配置的 AspectJ 支持
        registerBeanDefinitionParser("spring-configured", new SpringConfiguredBeanDefinitionParser());

        // JMX MBean 导出
        registerBeanDefinitionParser("mbean-export", new MBeanExportBeanDefinitionParser());
        registerBeanDefinitionParser("mbean-server", new MBeanServerBeanDefinitionParser());
    }
}
```

#### XML 配置示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           https://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/context
           https://www.springframework.org/schema/context/spring-context.xsd">

    <!-- 属性占位符 -->
    <context:property-placeholder
        location="classpath:app.properties,classpath:jdbc.properties"
        ignore-unresolvable="true"
        system-properties-mode="OVERRIDE"/>

    <!-- 启用注解配置 -->
    <context:annotation-config/>

    <!-- 组件扫描 -->
    <context:component-scan
        base-package="com.example.service, com.example.dao"
        use-default-filters="true">
        <context:include-filter type="annotation" expression="org.springframework.stereotype.Service"/>
        <context:exclude-filter type="annotation" expression="org.springframework.stereotype.Controller"/>
    </context:component-scan>

    <!-- 加载时织入 -->
    <context:load-time-weaver/>

</beans>
```

### 3. p 命名空间（属性简写）

`SimplePropertyNamespaceHandler` 提供了简洁的属性注入语法。

#### 工作原理

```java
public class SimplePropertyNamespaceHandler implements NamespaceHandler {

    private static final String REF_SUFFIX = "-ref";

    @Override
    public void init() {
        // 无需初始化
    }

    @Override
    public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
        if (node instanceof Attr attr) {
            String propertyName = parserContext.getDelegate().getLocalName(attr);
            String propertyValue = attr.getValue();
            MutablePropertyValues pvs = definition.getBeanDefinition().getPropertyValues();

            // 检查属性是否已定义
            if (pvs.contains(propertyName)) {
                parserContext.getReaderContext().error(
                    "Property '" + propertyName + "' is already defined", attr);
            }

            // 处理 -ref 后缀（引用其他 Bean）
            if (propertyName.endsWith(REF_SUFFIX)) {
                propertyName = propertyName.substring(0, propertyName.length() - REF_SUFFIX.length());
                pvs.add(Conventions.attributeNameToPropertyName(propertyName),
                    new RuntimeBeanReference(propertyValue));
            }
            else {
                // 普通属性值
                pvs.add(Conventions.attributeNameToPropertyName(propertyName), propertyValue);
            }
        }
        return definition;
    }
}
```

#### XML 配置示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:p="http://www.springframework.org/schema/p"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
           https://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- 传统方式 -->
    <bean id="userService" class="com.example.UserService">
        <property name="userDao" ref="userDao"/>
        <property name="maxResults" value="100"/>
        <property name="cacheEnabled" value="true"/>
    </bean>

    <!-- 使用 p 命名空间（等效） -->
    <bean id="userService" class="com.example.UserService"
          p:userDao-ref="userDao"
          p:maxResults="100"
          p:cacheEnabled="true"/>

</beans>
```

### 4. c 命名空间（构造参数简写）

`SimpleConstructorNamespaceHandler` 提供了简洁的构造参数注入语法。

#### 工作原理

```java
public class SimpleConstructorNamespaceHandler implements NamespaceHandler {

    private static final String REF_SUFFIX = "-ref";
    private static final String DELIMITER_PREFIX = "_";  // 用于索引参数

    @Override
    public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
        if (node instanceof Attr attr) {
            String argName = parserContext.getDelegate().getLocalName(attr).strip();
            String argValue = attr.getValue().strip();
            ConstructorArgumentValues cvs = definition.getBeanDefinition().getConstructorArgumentValues();
            boolean ref = false;

            // 处理 -ref 后缀
            if (argName.endsWith(REF_SUFFIX)) {
                ref = true;
                argName = argName.substring(0, argName.length() - REF_SUFFIX.length());
            }

            ValueHolder valueHolder = new ValueHolder(
                ref ? new RuntimeBeanReference(argValue) : argValue);
            valueHolder.setSource(parserContext.getReaderContext().extractSource(attr));

            // 处理索引参数（如 c:_0, c:_1）
            if (argName.startsWith(DELIMITER_PREFIX)) {
                String arg = argName.substring(1).trim();

                if (!StringUtils.hasText(arg)) {
                    // c:_ 表示通用参数
                    cvs.addGenericArgumentValue(valueHolder);
                }
                else {
                    // c:_0, c:_1 等表示索引参数
                    int index = Integer.parseInt(arg);
                    cvs.addIndexedArgumentValue(index, valueHolder);
                }
            }
            else {
                // 命名参数（如 c:host, c:port）
                valueHolder.setName(Conventions.attributeNameToPropertyName(argName));
                cvs.addGenericArgumentValue(valueHolder);
            }
        }
        return definition;
    }
}
```

#### XML 配置示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:c="http://www.springframework.org/schema/c"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
           https://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- 传统方式 -->
    <bean id="dataSource" class="com.example.DataSource">
        <constructor-arg name="url" value="jdbc:mysql://localhost/test"/>
        <constructor-arg name="username" value="root"/>
        <constructor-arg name="password" value="secret"/>
    </bean>

    <!-- 使用 c 命名空间（命名参数） -->
    <bean id="dataSource" class="com.example.DataSource"
          c:url="jdbc:mysql://localhost/test"
          c:username="root"
          c:password="secret"/>

    <!-- 使用 c 命名空间（索引参数） -->
    <bean id="dataSource" class="com.example.DataSource"
          c:_0="jdbc:mysql://localhost/test"
          c:_1="root"
          c:_2="secret"/>

    <!-- 使用 c 命名空间（混合引用） -->
    <bean id="userService" class="com.example.UserService"
          c:userDao-ref="userDao"
          c:maxResults="100"/>

</beans>
```

---

## 核心源码分析

### 1. BeanDefinitionParserDelegate 解析核心

`BeanDefinitionParserDelegate` 是解析标准 beans 命名空间的核心类，负责将 XML 元素转换为 `BeanDefinition`。

```java
public class BeanDefinitionParserDelegate {

    // 标准属性常量
    public static final String BEANS_NAMESPACE_URI = "http://www.springframework.org/schema/beans";
    public static final String BEAN_ELEMENT = "bean";
    public static final String ID_ATTRIBUTE = "id";
    public static final String NAME_ATTRIBUTE = "name";
    public static final String CLASS_ATTRIBUTE = "class";
    public static final String PARENT_ATTRIBUTE = "parent";
    public static final String SCOPE_ATTRIBUTE = "scope";
    public static final String LAZY_INIT_ATTRIBUTE = "lazy-init";
    public static final String AUTOWIRE_ATTRIBUTE = "autowire";
    public static final String DEPENDS_ON_ATTRIBUTE = "depends-on";
    public static final String INIT_METHOD_ATTRIBUTE = "init-method";
    public static final String DESTROY_METHOD_ATTRIBUTE = "destroy-method";
    public static final String FACTORY_METHOD_ATTRIBUTE = "factory-method";
    public static final String FACTORY_BEAN_ATTRIBUTE = "factory-bean";

    /**
     * 解析 <bean> 元素
     * 这是解析 Bean 定义的核心方法
     */
    @Nullable
    public BeanDefinitionHolder parseBeanDefinitionElement(Element ele) {
        return parseBeanDefinitionElement(ele, null);
    }

    /**
     * 解析 <bean> 元素（支持包含 Bean 定义）
     * @param ele <bean> 元素
     * @param containingBean 包含此 Bean 的父 Bean（用于内部 Bean）
     */
    @Nullable
    public BeanDefinitionHolder parseBeanDefinitionElement(Element ele, @Nullable BeanDefinition containingBean) {
        // 1. 解析 id 和 name 属性
        String id = ele.getAttribute(ID_ATTRIBUTE);
        String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);

        // 2. 处理 name 属性中的别名（逗号或分号分隔）
        List<String> aliases = new ArrayList<>();
        if (StringUtils.hasLength(nameAttr)) {
            String[] nameArr = StringUtils.tokenizeToStringArray(
                nameAttr, MULTI_VALUE_ATTRIBUTE_DELIMITERS);
            aliases.addAll(Arrays.asList(nameArr));
        }

        // 3. 确定 Bean 名称
        String beanName = id;
        if (!StringUtils.hasText(beanName) && !aliases.isEmpty()) {
            // 如果没有 id，使用第一个 name 作为 beanName
            beanName = aliases.remove(0);
            if (logger.isTraceEnabled()) {
                logger.trace("No XML 'id' specified - using '" + beanName +
                    "' as bean name and " + aliases + " as aliases");
            }
        }

        // 4. 检查包含 Bean 的名称唯一性
        if (containingBean != null) {
            beanName = BeanDefinitionReaderUtils.generateBeanName(
                new GenericBeanDefinition(), this.readerContext.getRegistry(), true);
        }

        // 5. 解析 Bean 定义的主体
        AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(
            ele, beanName, containingBean);

        if (beanDefinition != null) {
            // 6. 如果没有指定 id 或 name，生成默认名称
            if (!StringUtils.hasText(beanName)) {
                try {
                    if (containingBean != null) {
                        // 内部 Bean：使用生成器生成名称
                        beanName = BeanDefinitionReaderUtils.generateBeanName(
                            beanDefinition, this.readerContext.getRegistry(), true);
                    }
                    else {
                        // 顶级 Bean：生成名称，并可能使用类名作为别名
                        beanName = this.readerContext.generateBeanName(beanDefinition);
                        String beanClassName = beanDefinition.getBeanClassName();
                        if (beanClassName != null &&
                            beanName.startsWith(beanClassName) &&
                            beanName.length() > beanClassName.length() &&
                            !this.readerContext.getRegistry().isBeanNameInUse(beanClassName)) {
                            aliases.add(beanClassName);  // 添加类名作为别名
                        }
                    }
                    if (logger.isTraceEnabled()) {
                        logger.trace("Neither XML 'id' nor 'name' specified - using generated bean name [" + beanName + "]");
                    }
                }
                catch (Exception ex) {
                    error(ex.getMessage(), ele);
                    return null;
                }
            }

            // 7. 转换为数组并创建 BeanDefinitionHolder
            String[] aliasesArray = StringUtils.toStringArray(aliases);
            return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);
        }

        return null;
    }

    /**
     * 解析 Bean 定义的具体属性
     */
    public AbstractBeanDefinition parseBeanDefinitionElement(
            Element ele, String beanName, @Nullable BeanDefinition containingBean) {

        // 记录解析状态
        this.parseState.push(new BeanEntry(beanName));

        // 1. 解析 class 属性
        String className = null;
        if (ele.hasAttribute(CLASS_ATTRIBUTE)) {
            className = ele.getAttribute(CLASS_ATTRIBUTE).trim();
        }

        // 2. 解析 parent 属性（支持 Bean 继承）
        String parent = null;
        if (ele.hasAttribute(PARENT_ATTRIBUTE)) {
            parent = ele.getAttribute(PARENT_ATTRIBUTE);
        }

        try {
            // 3. 创建 BeanDefinition 构建器
            AbstractBeanDefinition bd = createBeanDefinition(className, parent);

            // 4. 解析各种属性
            parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);

            // 5. 解析 <description> 子元素
            bd.setDescription(DomUtils.getChildElementValueByTagName(ele, DESCRIPTION_ELEMENT));

            // 6. 解析 <meta> 子元素（元数据）
            parseMetaElements(ele, bd);

            // 7. 解析 <lookup-method> 子元素（方法注入）
            parseLookupOverrideSubElements(ele, bd.getMethodOverrides());

            // 8. 解析 <replaced-method> 子元素（方法替换）
            parseReplacedMethodSubElements(ele, bd.getMethodOverrides());

            // 9. 解析 <constructor-arg> 子元素
            parseConstructorArgElements(ele, bd);

            // 10. 解析 <property> 子元素
            parsePropertyElements(ele, bd);

            // 11. 解析 <qualifier> 子元素（用于 @Autowired 限定）
            parseQualifierElements(ele, bd);

            bd.setResource(this.readerContext.getResource());
            bd.setSource(extractSource(ele));

            return bd;
        }
        catch (ClassNotFoundException ex) {
            error("Bean class [" + className + "] not found", ele, ex);
        }
        catch (NoClassDefFoundError err) {
            error("Class that bean class [" + className + "] depends on not found", ele, err);
        }
        catch (Throwable ex) {
            error("Unexpected failure during bean definition parsing", ele, ex);
        }
        finally {
            this.parseState.pop();
        }

        return null;
    }

    /**
     * 解析 Bean 定义属性
     */
    public void parseBeanDefinitionAttributes(Element ele, String beanName,
            @Nullable BeanDefinition containingBean, AbstractBeanDefinition bd) {

        // 解析 singleton 属性（已废弃，使用 scope 替代）
        if (ele.hasAttribute(SINGLETON_ATTRIBUTE)) {
            error("Old 1.x 'singleton' attribute in use - upgrade to 'scope' declaration", ele);
        }
        // 解析 scope 属性
        else if (ele.hasAttribute(SCOPE_ATTRIBUTE)) {
            bd.setScope(ele.getAttribute(SCOPE_ATTRIBUTE));
        }
        else if (containingBean != null) {
            // 内部 Bean 继承包含 Bean 的 scope
            bd.setScope(containingBean.getScope());
        }

        // 解析 abstract 属性
        if (ele.hasAttribute(ABSTRACT_ATTRIBUTE)) {
            bd.setAbstract(TRUE_VALUE.equals(ele.getAttribute(ABSTRACT_ATTRIBUTE)));
        }

        // 解析 lazy-init 属性（支持 default 继承）
        String lazyInit = ele.getAttribute(LAZY_INIT_ATTRIBUTE);
        if (isDefaultValue(lazyInit)) {
            lazyInit = this.defaults.getLazyInit();
        }
        bd.setLazyInit(TRUE_VALUE.equals(lazyInit));

        // 解析 autowire 属性
        String autowire = ele.getAttribute(AUTOWIRE_ATTRIBUTE);
        bd.setAutowireMode(getAutowireMode(autowire));

        // 解析 depends-on 属性
        if (ele.hasAttribute(DEPENDS_ON_ATTRIBUTE)) {
            String dependsOn = ele.getAttribute(DEPENDS_ON_ATTRIBUTE);
            bd.setDependsOn(StringUtils.tokenizeToStringArray(dependsOn, MULTI_VALUE_ATTRIBUTE_DELIMITERS));
        }

        // 解析 autowire-candidate 属性
        String autowireCandidate = ele.getAttribute(AUTOWIRE_CANDIDATE_ATTRIBUTE);
        if (isDefaultValue(autowireCandidate)) {
            String candidatePattern = this.defaults.getAutowireCandidates();
            if (candidatePattern != null) {
                String[] patterns = StringUtils.commaDelimitedListToStringArray(candidatePattern);
                bd.setAutowireCandidate(PatternMatchUtils.simpleMatch(patterns, beanName));
            }
        }
        else {
            bd.setAutowireCandidate(TRUE_VALUE.equals(autowireCandidate));
        }

        // 解析 primary 属性
        if (ele.hasAttribute(PRIMARY_ATTRIBUTE)) {
            bd.setPrimary(TRUE_VALUE.equals(ele.getAttribute(PRIMARY_ATTRIBUTE)));
        }

        // 解析 init-method 属性
        if (ele.hasAttribute(INIT_METHOD_ATTRIBUTE)) {
            String initMethodName = ele.getAttribute(INIT_METHOD_ATTRIBUTE);
            bd.setInitMethodName(initMethodName);
        }
        else if (this.defaults.getInitMethod() != null) {
            bd.setInitMethodName(this.defaults.getInitMethod());
            bd.setEnforceInitMethod(false);
        }

        // 解析 destroy-method 属性
        if (ele.hasAttribute(DESTROY_METHOD_ATTRIBUTE)) {
            String destroyMethodName = ele.getAttribute(DESTROY_METHOD_ATTRIBUTE);
            bd.setDestroyMethodName(destroyMethodName);
        }
        else if (this.defaults.getDestroyMethod() != null) {
            bd.setDestroyMethodName(this.defaults.getDestroyMethod());
            bd.setEnforceDestroyMethod(false);
        }

        // 解析 factory-method 属性
        if (ele.hasAttribute(FACTORY_METHOD_ATTRIBUTE)) {
            bd.setFactoryMethodName(ele.getAttribute(FACTORY_METHOD_ATTRIBUTE));
        }

        // 解析 factory-bean 属性
        if (ele.hasAttribute(FACTORY_BEAN_ATTRIBUTE)) {
            bd.setFactoryBeanName(ele.getAttribute(FACTORY_BEAN_ATTRIBUTE));
        }
    }

    /**
     * 解析自定义元素
     * 这是命名空间扩展机制的入口
     */
    @Nullable
    public BeanDefinition parseCustomElement(Element ele) {
        return parseCustomElement(ele, null);
    }

    @Nullable
    public BeanDefinition parseCustomElement(Element ele, @Nullable BeanDefinition containingBd) {
        // 1. 获取元素的命名空间 URI
        String namespaceUri = getNamespaceURI(ele);

        if (namespaceUri == null) {
            return null;
        }

        // 2. 使用 NamespaceHandlerResolver 查找对应的 NamespaceHandler
        NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);

        if (handler == null) {
            error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", ele);
            return null;
        }

        // 3. 调用 NamespaceHandler 解析元素
        return handler.parse(ele, new ParserContext(this.readerContext, this, containingBd));
    }
}
```

### 2. ParserContext 解析上下文

```java
public final class ParserContext {

    private final XmlReaderContext readerContext;           // XML 读取上下文
    private final BeanDefinitionParserDelegate delegate;    // 解析委托器
    @Nullable
    private BeanDefinition containingBeanDefinition;        // 包含的 Bean 定义（用于内部 Bean）
    private final Deque<CompositeComponentDefinition> containingComponents = new ArrayDeque<>();

    public ParserContext(XmlReaderContext readerContext, BeanDefinitionParserDelegate delegate,
            @Nullable BeanDefinition containingBeanDefinition) {
        this.readerContext = readerContext;
        this.delegate = delegate;
        this.containingBeanDefinition = containingBeanDefinition;
    }

    public BeanDefinitionRegistry getRegistry() {
        return this.readerContext.getRegistry();
    }

    public BeanDefinitionParserDelegate getDelegate() {
        return this.delegate;
    }

    /**
     * 判断是否是嵌套解析（内部 Bean）
     */
    public boolean isNested() {
        return (this.containingBeanDefinition != null);
    }

    /**
     * 获取默认的 lazy-init 设置
     */
    public boolean isDefaultLazyInit() {
        return BeanDefinitionParserDelegate.TRUE_VALUE.equals(
            this.delegate.getDefaults().getLazyInit());
    }

    /**
     * 注册组件定义
     */
    public void registerComponent(ComponentDefinition component) {
        CompositeComponentDefinition containingComponent = getContainingComponent();
        if (containingComponent != null) {
            containingComponent.addNestedComponent(component);
        }
        else {
            this.readerContext.fireComponentRegistered(component);
        }
    }

    /**
     * 注册 Bean 组件
     */
    public void registerBeanComponent(BeanComponentDefinition component) {
        BeanDefinitionReaderUtils.registerBeanDefinition(component, getRegistry());
        registerComponent(component);
    }
}
```

---

## 扩展自定义命名空间

### 1. 实现步骤

扩展 Spring XML 配置以支持自定义命名空间需要以下步骤：

```
步骤 1: 定义 XSD Schema
    └── 描述自定义元素的 XML 结构

步骤 2: 实现 NamespaceHandler
    └── 继承 NamespaceHandlerSupport
    └── 在 init() 中注册 BeanDefinitionParser

步骤 3: 实现 BeanDefinitionParser
    └── 实现 parse() 方法
    └── 或使用 AbstractSingleBeanDefinitionParser 简化

步骤 4: 配置 spring.handlers
    └── META-INF/spring.handlers
    └── 映射命名空间 URI 到 NamespaceHandler 类

步骤 5: 配置 spring.schemas
    └── META-INF/spring.schemas
    └── 映射命名空间到 XSD 文件位置
```

### 2. 完整示例

#### 步骤 1: 定义 XSD Schema (myns.xsd)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<xsd:schema xmlns="http://www.example.com/schema/myns"
            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            xmlns:beans="http://www.springframework.org/schema/beans"
            targetNamespace="http://www.example.com/schema/myns"
            elementFormDefault="qualified">

    <xsd:import namespace="http://www.springframework.org/schema/beans"/>

    <!-- 定义 <myns:cache> 元素 -->
    <xsd:element name="cache">
        <xsd:complexType>
            <xsd:attribute name="id" type="xsd:string" use="required"/>
            <xsd:attribute name="name" type="xsd:string" use="required"/>
            <xsd:attribute name="maxSize" type="xsd:int" default="1000"/>
            <xsd:attribute name="ttl" type="xsd:long" default="3600"/>
        </xsd:complexType>
    </xsd:element>

</xsd:schema>
```

#### 步骤 2: 实现 NamespaceHandler

```java
package com.example.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class MyNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        // 注册 <myns:cache> 元素的解析器
        registerBeanDefinitionParser("cache", new CacheBeanDefinitionParser());
    }
}
```

#### 步骤 3: 实现 BeanDefinitionParser

```java
package com.example.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

import com.example.cache.CacheManager;

public class CacheBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /**
     * 指定要创建的 Bean 类
     */
    @Override
    protected Class<?> getBeanClass(Element element) {
        return CacheManager.class;
    }

    /**
     * 解析元素属性并设置到 BeanDefinition
     */
    @Override
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        // 解析 name 属性（必需）
        String name = element.getAttribute("name");
        if (StringUtils.hasText(name)) {
            builder.addPropertyValue("cacheName", name);
        }

        // 解析 maxSize 属性（有默认值）
        String maxSize = element.getAttribute("maxSize");
        if (StringUtils.hasText(maxSize)) {
            builder.addPropertyValue("maxSize", Integer.parseInt(maxSize));
        }

        // 解析 ttl 属性（有默认值）
        String ttl = element.getAttribute("ttl");
        if (StringUtils.hasText(ttl)) {
            builder.addPropertyValue("timeToLive", Long.parseLong(ttl));
        }
    }
}
```

#### 步骤 4: 配置 spring.handlers

创建文件 `META-INF/spring.handlers`：

```properties
http\://www.example.com/schema/myns=com.example.config.MyNamespaceHandler
```

#### 步骤 5: 配置 spring.schemas

创建文件 `META-INF/spring.schemas`：

```properties
http\://www.example.com/schema/myns/myns.xsd=com/example/config/myns.xsd
```

### 3. 使用自定义命名空间

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:myns="http://www.example.com/schema/myns"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           https://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.example.com/schema/myns
           http://www.example.com/schema/myns/myns.xsd">

    <!-- 使用自定义命名空间 -->
    <myns:cache id="userCache"
                name="users"
                maxSize="5000"
                ttl="7200"/>

    <myns:cache id="productCache"
                name="products"/>
                <!-- maxSize 和 ttl 使用默认值 -->

</beans>
```

### 4. 更复杂的解析器示例

如果需要解析嵌套元素，可以实现更复杂的解析器：

```java
public class AdvancedCacheBeanDefinitionParser implements BeanDefinitionParser {

    @Override
    @Nullable
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        // 创建 BeanDefinition 构建器
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
        builder.getRawBeanDefinition().setBeanClass(CacheManager.class);

        // 解析基本属性
        String name = element.getAttribute("name");
        builder.addPropertyValue("cacheName", name);

        // 解析嵌套的 <myns:eviction-policy> 元素
        Element evictionPolicyEle = DomUtils.getChildElementByTagName(element, "eviction-policy");
        if (evictionPolicyEle != null) {
            String policy = evictionPolicyEle.getAttribute("type");
            builder.addPropertyValue("evictionPolicy", policy);
        }

        // 解析嵌套的 <myns:serializer> 元素（可能是自定义元素）
        Element serializerEle = DomUtils.getChildElementByTagName(element, "serializer");
        if (serializerEle != null) {
            // 递归解析子元素
            BeanDefinition serializerDef = parserContext.getDelegate().parseCustomElement(serializerEle);
            if (serializerDef != null) {
                builder.addPropertyValue("serializer", serializerDef);
            }
        }

        // 获取或生成 Bean 名称
        String id = element.getAttribute("id");
        if (!StringUtils.hasText(id)) {
            id = parserContext.getReaderContext().generateBeanName(builder.getRawBeanDefinition());
        }

        // 注册 Bean 定义
        parserContext.getRegistry().registerBeanDefinition(id, builder.getBeanDefinition());

        return builder.getBeanDefinition();
    }
}
```

---

## 总结

### 核心流程回顾

1. **XmlBeanDefinitionReader** 作为入口，负责加载 XML 资源
2. **DefaultDocumentLoader** 使用 JAXP 将 XML 加载为 DOM Document
3. **DefaultBeanDefinitionDocumentReader** 遍历 DOM 树，区分默认和自定义命名空间
4. **BeanDefinitionParserDelegate** 解析标准 beans 命名空间的元素
5. **DefaultNamespaceHandlerResolver** 从 `META-INF/spring.handlers` 加载命名空间处理器映射
6. **NamespaceHandlerSupport** 提供便捷的解析器注册机制
7. **BeanDefinitionParser** 实现类负责将特定 XML 元素转换为 BeanDefinition

### 关键扩展点

| 扩展点 | 用途 | 示例 |
|--------|------|------|
| `NamespaceHandler` | 处理自定义命名空间 | `UtilNamespaceHandler` |
| `BeanDefinitionParser` | 解析自定义元素 | `CacheBeanDefinitionParser` |
| `BeanDefinitionDecorator` | 装饰 Bean 定义 | `SimplePropertyNamespaceHandler` |
| `DocumentLoader` | 自定义 XML 加载 | 支持加密 XML |
| `EntityResolver` | 自定义实体解析 | 从数据库加载 XSD |

### 文件位置汇总

| 文件 | 位置 | 用途 |
|------|------|------|
| `spring.handlers` | `META-INF/spring.handlers` | 命名空间 URI 到 Handler 类的映射 |
| `spring.schemas` | `META-INF/spring.schemas` | 命名空间到 XSD 文件的映射 |
| `*.xsd` | `META-INF/` 或 URL | XML Schema 定义 |
