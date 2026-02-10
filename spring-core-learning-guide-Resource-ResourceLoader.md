# Spring 资源加载（Resource & ResourceLoader）深度解析

## 一、概述

Spring 的 **Resource** 和 **ResourceLoader** 构成了框架中资源访问的基础抽象层。它们提供了一种统一的方式来访问各种底层资源，包括文件、类路径资源、URL 资源等。

### 1.1 核心组件

```
Resource (资源描述符)
    ├── ClassPathResource      - 类路径资源
    ├── FileSystemResource     - 文件系统资源
    ├── UrlResource            - URL资源
    ├── ByteArrayResource      - 字节数组资源
    └── InputStreamResource    - 输入流资源

ResourceLoader (资源加载器)
    ├── DefaultResourceLoader  - 默认实现
    ├── FileSystemResourceLoader - 文件系统专用
    └── ServletContextResourceLoader - Web环境专用
```

---

## 二、Resource 接口详解

### 2.1 接口定义

```java
public interface Resource extends InputStreamSource {
    boolean exists();                          // 资源是否存在
    boolean isReadable();                      // 是否可读
    boolean isOpen();                          // 是否已打开
    boolean isFile();                          // 是否是文件

    URL getURL() throws IOException;           // 获取URL
    URI getURI() throws IOException;           // 获取URI
    File getFile() throws IOException;         // 获取File

    long contentLength() throws IOException;   // 内容长度
    long lastModified() throws IOException;    // 最后修改时间

    Resource createRelative(String relativePath) throws IOException;  // 创建相对资源
    String getFilename();                      // 获取文件名
    String getDescription();                   // 获取描述
}
```

### 2.2 核心方法说明

| 方法 | 说明 |
|-----|------|
| `exists()` | 检查资源是否物理存在 |
| `isReadable()` | 是否能通过 `getInputStream()` 读取 |
| `isOpen()` | 是否已经打开流（不能多次读取）|
| `getURL()` | 返回资源的 URL 句柄 |
| `getFile()` | 返回 File 对象（仅适用于文件系统资源）|
| `createRelative()` | 基于当前资源创建相对路径资源 |

---

## 三、ResourceLoader 接口详解

### 3.1 接口定义

```java
public interface ResourceLoader {
    String CLASSPATH_URL_PREFIX = "classpath:";  // 类路径前缀

    Resource getResource(String location);        // 根据位置加载资源
    ClassLoader getClassLoader();                 // 获取类加载器
}
```

### 3.2 支持的资源路径格式

```java
// 1. URL 格式
Resource urlResource = resourceLoader.getResource("https://example.com/file.txt");

// 2. 文件路径
Resource fileResource = resourceLoader.getResource("file:/path/to/file.txt");

// 3. 类路径（最常用）
Resource classpathResource = resourceLoader.getResource("classpath:application.properties");

// 4. 相对路径（取决于具体实现）
Resource relativeResource = resourceLoader.getResource("WEB-INF/web.xml");
```

---

## 四、核心实现类详解

### 4.1 ClassPathResource - 类路径资源

**用途**：加载类路径下的资源文件。

```java
public class ClassPathResource extends AbstractFileResolvingResource {
    private final String path;
    private final ClassLoader classLoader;
    private final Class<?> clazz;

    // 使用类加载器
    public ClassPathResource(String path) {
        this(path, (ClassLoader) null);
    }

    public ClassPathResource(String path, @Nullable ClassLoader classLoader) {
        // 处理前导斜杠：/config/app.properties → config/app.properties
        String pathToUse = StringUtils.cleanPath(path);
        if (pathToUse.startsWith("/")) {
            pathToUse = pathToUse.substring(1);
        }
        this.path = pathToUse;
        this.classLoader = classLoader;
    }

    // 使用类作为相对基础
    public ClassPathResource(String path, Class<?> clazz) {
        this.path = StringUtils.cleanPath(path);
        this.clazz = clazz;
    }
}
```

**使用示例**：

```java
// 方式1：使用类加载器（推荐）
ClassPathResource resource = new ClassPathResource("application.properties");

// 方式2：指定类加载器
ClassPathResource resource = new ClassPathResource(
    "config/database.properties",
    MyClass.class.getClassLoader()
);

// 方式3：相对于特定类
ClassPathResource resource = new ClassPathResource(
    "config.xml",
    MyClass.class  // 相对于 MyClass 所在包
);

// 读取内容
try (InputStream is = resource.getInputStream()) {
    // 处理输入流
}
```

### 4.2 FileSystemResource - 文件系统资源

**用途**：加载文件系统中的资源。

```java
public class FileSystemResource extends AbstractResource implements WritableResource {
    private final String path;
    private final File file;
    private final Path filePath;  // NIO.2 Path

    public FileSystemResource(String path) {
        this.path = StringUtils.cleanPath(path);
        this.file = new File(path);
        this.filePath = this.file.toPath();
    }

    public FileSystemResource(File file) {
        this.path = StringUtils.cleanPath(file.getPath());
        this.file = file;
        this.filePath = file.toPath();
    }

    public FileSystemResource(Path path) {
        this.path = StringUtils.cleanPath(path.toString());
        this.file = null;  // 延迟初始化
        this.filePath = path;
    }
}
```

**使用示例**：

```java
// 方式1：使用路径字符串
FileSystemResource resource = new FileSystemResource("/home/user/config.properties");

// 方式2：使用File对象
FileSystemResource resource = new FileSystemResource(new File("/home/user/data.xml"));

// 方式3：使用NIO Path
FileSystemResource resource = new FileSystemResource(
    Paths.get("/home/user", "config.properties")
);

// 写入内容（因为实现了WritableResource）
try (OutputStream os = resource.getOutputStream()) {
    os.write("content".getBytes());
}
```

### 4.3 UrlResource - URL资源

**用途**：通过URL访问远程资源。

```java
public class UrlResource extends AbstractFileResolvingResource {
    private final URI uri;
    private final URL url;
    private final URL cleanedUrl;

    public UrlResource(String path) throws MalformedURLException {
        this.uri = null;
        this.url = new URL(path);
        this.cleanedUrl = getCleanedUrl(this.url, path);
    }

    public UrlResource(URL url) {
        this.uri = null;
        this.url = url;
        this.cleanedUrl = getCleanedUrl(this.url, null);
    }
}
```

**使用示例**：

```java
// 访问网络资源
UrlResource resource = new UrlResource("https://example.com/data.json");

// 访问文件URL
UrlResource resource = new UrlResource("file:///home/user/config.properties");

// 检查资源是否存在
if (resource.exists()) {
    String content = resource.getContentAsString(StandardCharsets.UTF_8);
}
```

### 4.4 其他资源类型

| 资源类型 | 用途 | 示例 |
|:--------|------|:-----|
| `ByteArrayResource` | 字节数组 | `new ByteArrayResource("content".getBytes())` |
| `InputStreamResource` | 输入流 | `new InputStreamResource(inputStream)` |
| `VfsResource` | JBoss VFS | 用于JBoss/WildFly环境 |
| `ServletContextResource` | Web应用 | `new ServletContextResource(servletContext, "/WEB-INF/web.xml")` |

---

## 五、DefaultResourceLoader 详解

### 5.1 核心实现

```java
public class DefaultResourceLoader implements ResourceLoader {
    @Nullable
    private ClassLoader classLoader;

    private final Set<ProtocolResolver> protocolResolvers = new LinkedHashSet<>(4);

    public DefaultResourceLoader() {
    }

    public DefaultResourceLoader(@Nullable ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Resource getResource(String location) {
        Assert.notNull(location, "Location must not be null");

        // 1. 首先尝试自定义协议解析器
        for (ProtocolResolver protocolResolver : getProtocolResolvers()) {
            Resource resource = protocolResolver.resolve(location, this);
            if (resource != null) {
                return resource;
            }
        }

        // 2. 处理类路径前缀
        if (location.startsWith(CLASSPATH_URL_PREFIX)) {
            return new ClassPathResource(
                location.substring(CLASSPATH_URL_PREFIX.length()),
                getClassLoader()
            );
        }

        // 3. 尝试作为URL处理
        try {
            URL url = new URL(location);
            return new UrlResource(url);
        }
        catch (MalformedURLException ex) {
            // 不是URL，作为类路径资源处理
            return new ClassPathResource(location, getClassLoader());
        }
    }
}
```

### 5.2 资源加载策略

```
输入: location
    	↓
	1. 检查 ProtocolResolver（自定义协议）
  	    ↓ 如果返回null
	2. 检查是否以 "classpath:" 开头
	    ↓ 是 → 返回 ClassPathResource
 	    ↓ 否
	3. 尝试解析为 URL
	    ↓ 成功 → 返回 UrlResource
	    ↓ 失败（MalformedURLException）
	4. 回退为 ClassPathResource
```

### 5.3 自定义协议解析器

```java
// 自定义协议解析器
public class CustomProtocolResolver implements ProtocolResolver {
    @Override
    public Resource resolve(String location, ResourceLoader resourceLoader) {
        if (location.startsWith("custom:")) {
            String path = location.substring("custom:".length());
            // 返回自定义资源
            return new CustomResource(path);
        }
        return null;
    }
}

// 注册协议解析器
DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
resourceLoader.addProtocolResolver(new CustomProtocolResolver());

// 使用
Resource resource = resourceLoader.getResource("custom://my-resource");
```

---

## 六、实际应用场景

### 6.1 加载配置文件

```java
@Component
public class ConfigLoader {

    @Autowired
    private ResourceLoader resourceLoader;

    public Properties loadConfig(String configLocation) throws IOException {
        Resource resource = resourceLoader.getResource(configLocation);

        if (!resource.exists()) {
            throw new FileNotFoundException("Config not found: " + configLocation);
        }

        Properties props = new Properties();
        try (InputStream is = resource.getInputStream()) {
            props.load(is);
        }
        return props;
    }
}

// 使用
@Autowired
private ConfigLoader configLoader;

// 支持多种方式
Properties props1 = configLoader.loadConfig("classpath:application.properties");
Properties props2 = configLoader.loadConfig("file:/etc/app/config.properties");
Properties props3 = configLoader.loadConfig("https://config-server/app.properties");
```

### 6.2 模板文件加载

```java
@Service
public class TemplateService {

    private final ResourceLoader resourceLoader;

    public TemplateService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String loadTemplate(String templateName) throws IOException {
        String location = "classpath:templates/" + templateName + ".html";
        Resource resource = resourceLoader.getResource(location);

        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    public void saveTemplate(String templateName, String content) throws IOException {
        String location = "file:/var/app/templates/" + templateName + ".html";
        Resource resource = resourceLoader.getResource(location);

        if (resource instanceof WritableResource) {
            try (OutputStream os = ((WritableResource) resource).getOutputStream()) {
                os.write(content.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
```

### 6.3 静态资源处理（Web）

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 类路径资源
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        // 文件系统资源
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/var/www/uploads/");

        // 远程资源（CDN）
        registry.addResourceHandler("/cdn/**")
                .addResourceLocations("https://cdn.example.com/");
    }
}
```

### 6.4 批量资源加载

```java
@Service
public class ResourceScanner {

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    public List<Resource> scanResources(String pattern) throws IOException {
        // 支持Ant风格路径
        // classpath*:mapper/**/*.xml
        Resource[] resources = resourcePatternResolver.getResources(pattern);
        return Arrays.asList(resources);
    }

    public Map<String, String> loadAllProperties(String pattern) throws IOException {
        Map<String, String> allProps = new HashMap<>();

        Resource[] resources = resourcePatternResolver.getResources(pattern);
        for (Resource resource : resources) {
            Properties props = new Properties();
            try (InputStream is = resource.getInputStream()) {
                props.load(is);
            }

            props.forEach((k, v) -> allProps.put((String) k, (String) v));
        }

        return allProps;
    }
}
```

---

## 七、与 Spring 容器的集成

### 7.1 ApplicationContext 作为 ResourceLoader

```java
@SpringBootApplication
public class MyApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(MyApplication.class, args);

        // ApplicationContext 继承自 ResourceLoader
        Resource resource = context.getResource("classpath:application.yml");

        // 也支持 ResourcePatternResolver（支持通配符）
        ResourcePatternResolver resolver = context;
        Resource[] resources = resolver.getResources("classpath*:/*.properties");
    }
}
```

### 7.2 @Value 注解注入 Resource

```java
@Component
public class AppConfig {

    // 注入单个资源
    @Value("classpath:config/app.properties")
    private Resource configResource;

    // 注入为字符串
    @Value("classpath:templates/welcome.txt")
    private String welcomeTemplate;

    // 注入文件路径
    @Value("file:${user.home}/app/data.json")
    private Resource userDataResource;
}
```

### 7.3 ResourceLoaderAware 接口

```java
@Component
public class MyService implements ResourceLoaderAware {

    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void doSomething() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:data.xml");
        // 处理资源
    }
}
```

---

## 八、最佳实践

### 8.1 资源路径规范

```java
// ✅ 推荐：使用正斜杠（即使在Windows上）
Resource resource = resourceLoader.getResource("classpath:config/app.properties");

// ✅ 推荐：类路径资源不要前导斜杠
Resource resource = resourceLoader.getResource("classpath:config/app.properties");
// 而不是：classpath:/config/app.properties

// ✅ 推荐：使用URL格式明确类型
Resource resource = resourceLoader.getResource("file:/absolute/path/file.txt");
```

### 8.2 资源存在性检查

```java
public void safeReadResource(Resource resource) {
    if (!resource.exists()) {
        log.warn("Resource does not exist: {}", resource.getDescription());
        return;
    }

    if (!resource.isReadable()) {
        log.warn("Resource is not readable: {}", resource.getDescription());
        return;
    }

    try (InputStream is = resource.getInputStream()) {
        // 读取内容
    } catch (IOException e) {
        log.error("Failed to read resource", e);
    }
}
```

### 8.3 缓存资源内容

```java
@Component
public class CachedResourceLoader {

    private final ResourceLoader resourceLoader;
    private final Map<String, byte[]> cache = new ConcurrentHashMap<>();

    public byte[] loadResource(String location) throws IOException {
        return cache.computeIfAbsent(location, key -> {
            try {
                Resource resource = resourceLoader.getResource(key);
                return resource.getContentAsByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
```

### 8.4 处理不同环境

```java
@Configuration
public class ResourceConfig {

    @Bean
    @Profile("dev")
    public Resource configResource() {
        return new FileSystemResource("/home/dev/config.properties");
    }

    @Bean
    @Profile("prod")
    public Resource configResource() {
        return new ClassPathResource("config/production.properties");
    }
}
```

---

## 九、常见问题

### Q1: classpath: 和 classpath*: 有什么区别？

```java
// classpath: - 找到第一个匹配的资源
Resource resource = context.getResource("classpath:application.properties");

// classpath*: - 找到所有匹配的资源（包括jar包中的）
Resource[] resources = context.getResources("classpath*:/*.xml");
```

### Q2: 为什么 getFile() 在 JAR 中失败？

```java
ClassPathResource resource = new ClassPathResource("config.properties");

// ❌ 可能在 JAR 中失败
try {
    File file = resource.getFile();  // FileNotFoundException
} catch (FileNotFoundException e) {
    // 处理：使用 getInputStream() 代替
}

// ✅ 更安全的方式
try (InputStream is = resource.getInputStream()) {
    // 读取内容
}
```

### Q3: 如何加载项目根目录的文件？

```java
// 方式1：使用文件系统路径
Resource resource = new FileSystemResource("./config.properties");

// 方式2：使用用户目录
String userDir = System.getProperty("user.dir");
Resource resource = new FileSystemResource(userDir + "/config.properties");
```

### Q4: Resource 和 File 的区别？

| 特性 | Resource | File |
|-----|----------|------|
| 抽象层次 | 高（统一接口） | 低（具体实现）|
| 来源 | 类路径、文件、URL | 仅文件系统 |
| 存在性检查 | 可以检查不存在 | 必须存在 |
| Spring集成 | 完美支持 | 需要转换 |

---

## 十、总结

### 核心要点

1. **Resource 是统一抽象**：屏蔽了底层资源的具体类型（文件、类路径、URL）

2. **ResourceLoader 是工厂**：根据路径字符串创建相应的 Resource 实例

3. **路径前缀决定类型**：
   - `classpath:` → ClassPathResource
   - `file:` → UrlResource/FileSystemResource
   - `http/https:` → UrlResource
   - 其他 → 先尝试URL，失败后转为 ClassPathResource

4. **始终使用 try-with-resources**：确保输入流正确关闭

5. **不要假设资源一定存在**：始终调用 `exists()` 进行检查

### 代码模板

```java
// 基础使用
@Autowired
private ResourceLoader resourceLoader;

public void readResource(String location) throws IOException {
    Resource resource = resourceLoader.getResource(location);

    if (resource.exists()) {
        try (InputStream is = resource.getInputStream()) {
            // 处理内容
        }
    }
}

// Spring 方式
@Value("classpath:config.properties")
private Resource configResource;
```

---

## 参考资料

- [Spring Framework Reference - Resources](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#resources)
- [Java URL Class](https://docs.oracle.com/javase/8/docs/api/java/net/URL.html)
- [Java NIO.2 Path](https://docs.oracle.com/javase/8/docs/api/java/nio/file/Path.html)
