# Spring 环境抽象（Environment & PropertySource）深度解析

## 一、概述

Spring 的 `Environment` 和 `PropertySource` 构成了框架中**配置管理的核心抽象层**。它们提供了统一的方式来管理应用程序运行时的配置属性，支持多种配置源（配置文件、系统属性、环境变量等）和动态配置切换（Profiles）。

### 1.1 核心组件

```
Environment (环境)
├── PropertyResolver (属性解析)
│   └── getProperty(), resolvePlaceholders()
├── Profiles (配置文件管理)
│   ├── getActiveProfiles()
│   └── acceptsProfiles()
└── PropertySources (属性源集合)
    ├── PropertySource 1 (systemProperties)
    ├── PropertySource 2 (systemEnvironment)
    ├── PropertySource 3 (application.properties)
    └── ...

PropertySource (属性源)
├── MapPropertySource
├── PropertiesPropertySource
├── SystemEnvironmentPropertySource
├── ResourcePropertySource
└── StubPropertySource
```

### 1.2 设计目标

1. **统一配置访问**：无论配置来自哪里（文件、环境变量、命令行），都通过统一 API 访问
2. **优先级管理**：支持配置源的优先级排序，高优先级覆盖低优先级
3. **动态 Profile 切换**：根据运行环境（dev/test/prod）加载不同配置
4. **占位符解析**：支持 `${...}` 占位符的嵌套解析

---

## 二、Environment 接口详解

### 2.1 接口继承关系

```java
public interface Environment extends PropertyResolver {
    // Profile 管理
    String[] getActiveProfiles();
    String[] getDefaultProfiles();
    boolean matchesProfiles(String... profileExpressions);
    boolean acceptsProfiles(Profiles profiles);
}

public interface PropertyResolver {
    // 属性访问
    String getProperty(String key);
    <T> T getProperty(String key, Class<T> targetType);
    <T> T getRequiredProperty(String key, Class<T> targetType);

    // 占位符解析
    String resolvePlaceholders(String text);
    String resolveRequiredPlaceholders(String text);
}
```

### 2.2 Profile 管理

Profile 是 Spring 提供的**环境隔离机制**，用于区分不同运行环境：

| Profile | 典型用途 |
|---------|---------|
| `default` | 默认配置，无指定时使用 |
| `dev` | 开发环境，启用调试功能 |
| `test` | 测试环境，使用内存数据库 |
| `prod` | 生产环境，启用缓存和优化 |

**激活方式**：

```bash
# 1. 命令行参数
java -Dspring.profiles.active=dev -jar app.jar

# 2. 环境变量
export SPRING_PROFILES_ACTIVE=prod

# 3. application.properties
spring.profiles.active=dev

# 4. 编程式
ConfigurableEnvironment env = new StandardEnvironment();
env.setActiveProfiles("dev", "test");
```

### 2.3 Profile 表达式

Spring 5.3.28+ 支持复杂的布尔表达式：

```java
// 与运算：p1 和 p2 都激活
environment.matchesProfiles("p1 & p2");

// 或运算：p1 或 p2 激活
environment.matchesProfiles("p1 | p2");

// 非运算：p1 未激活
environment.matchesProfiles("!p1");

// 复杂组合
environment.matchesProfiles("(dev | test) & !prod");
```

---

## 三、PropertySource 详解

### 3.1 核心概念

PropertySource 是对**配置源的抽象封装**，每个 PropertySource 都有一个名称和一个底层数据源。

```java
public abstract class PropertySource<T> {
    protected final String name;  // 属性源名称（唯一标识）
    protected final T source;      // 底层数据源

    public abstract Object getProperty(String name);
    public boolean containsProperty(String name);
}
```

**身份标识规则**：
- PropertySource 的 `equals()` 只比较 `name`，不比较内容
- 这意味着同名 PropertySource 在集合中会被视为同一个

### 3.2 常见实现类

| 实现类 | 数据源 | 用途 |
|--------|--------|------|
| `MapPropertySource` | `Map<String, Object>` | 内存配置 |
| `PropertiesPropertySource` | `Properties` | 属性文件 |
| `SystemEnvironmentPropertySource` | `Map<String, String>` | 环境变量 |
| `ResourcePropertySource` | `Resource` | 外部文件 |
| `CommandLinePropertySource` | `CommandLineArgs` | 命令行参数 |

### 3.3 使用示例

```java
// 创建 MapPropertySource
Map<String, Object> myMap = new HashMap<>();
myMap.put("app.name", "MyApplication");
myMap.put("app.version", "1.0.0");

MapPropertySource mapSource = new MapPropertySource("myMap", myMap);

// 读取属性
String appName = (String) mapSource.getProperty("app.name");

// 检查是否存在
boolean hasVersion = mapSource.containsProperty("app.version");
```

---

## 四、属性源优先级与 MutablePropertySources

### 4.1 优先级概念

在 Spring Environment 中，**属性源的顺序决定了优先级**：
- 先搜索的属性源优先级更高
- 高优先级属性会覆盖低优先级的同名属性

### 4.2 默认优先级（从高到低）

```
1. 命令行参数 (Command line arguments)
   --server.port=8081
   
2. SPRING_APPLICATION_JSON 中的 JSON 属性
   {"server":{"port":8082}}
   
1. ServletConfig 参数（Web环境）
2. ServletContext 参数（Web环境）
3. JNDI 属性
4. Java 系统属性（System.getProperties()）
5. 系统环境变量（System.getenv()）
6. RandomValuePropertySource
7. Profile-specific 配置文件（application-{profile}.properties、application-{profile}.yml）
8. 非 Profile-specific 配置文件（application.properties、application.yml）
9. @Configuration 类上的 @PropertySource 注解定义的属性
10. SpringApplication.setDefaultProperties 设置的默认属性
```

```
1. 当前目录的 /config 子目录
2. 当前目录
3. 类路径的 /config 包
4. 类路径根目录

示例优先级（外部覆盖内部）：
file:./config/application.yml          # 最高
file:./application.yml
classpath:/config/application.yml
classpath:/application.yml             # 最低
```

### 4.3 MutablePropertySources 操作

```java
public class MutablePropertySources implements PropertySources {

    // 添加到最高优先级（头部）
    void addFirst(PropertySource<?> propertySource);

    // 添加到最低优先级（尾部）
    void addLast(PropertySource<?> propertySource);

    // 添加到指定属性源之前
    void addBefore(String relativePropertySourceName, PropertySource<?> propertySource);

    // 添加到指定属性源之后
    void addAfter(String relativePropertySourceName, PropertySource<?> propertySource);

    // 替换指定属性源
    void replace(String name, PropertySource<?> propertySource);

    // 移除属性源
    void remove(String name);
}
```

### 4.4 自定义属性源优先级

```java
ConfigurableEnvironment environment = new StandardEnvironment();
MutablePropertySources propertySources = environment.getPropertySources();

// 添加高优先级属性源（会覆盖系统属性）
Map<String, Object> myProps = new HashMap<>();
myProps.put("server.port", 8081);
propertySources.addFirst(new MapPropertySource("myProps", myProps));

// 现在 environment.getProperty("server.port") 返回 "8081"
```

---

## 五、StandardEnvironment 详解

### 5.1 默认属性源配置

StandardEnvironment 为非 Web 应用提供了默认配置：

```java
public class StandardEnvironment extends AbstractEnvironment {

    @Override
    protected void customizePropertySources(MutablePropertySources propertySources) {
        // 1. 系统属性（System.getProperties()）
        propertySources.addLast(
            new PropertiesPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
                getSystemProperties()));

        // 2. 环境变量（System.getenv()）
        propertySources.addLast(
            new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                getSystemEnvironment()));
    }
}
```

### 5.2 Web 环境的扩展

```java
// StandardServletEnvironment 添加了更多属性源
public class StandardServletEnvironment extends StandardEnvironment {

    @Override
    protected void customizePropertySources(MutablePropertySources propertySources) {
        // 1. ServletConfig 初始化参数
        // 2. ServletContext 初始化参数
        // 3. JNDI 属性
        // 4. 系统属性
        // 5. 环境变量
    }
}
```

---

## 六、实际应用场景

### 6.1 多环境配置管理

```java
@Configuration
@Profile("dev")
public class DevConfig {
    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .build();
    }
}

@Configuration
@Profile("prod")
public class ProdConfig {
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }
}
```

### 6.2 动态属性源添加

```java
@Component
public class DynamicPropertySourceConfig implements EnvironmentAware {

    @Override
    public void setEnvironment(Environment environment) {
        if (environment instanceof ConfigurableEnvironment) {
            ConfigurableEnvironment configurableEnv = (ConfigurableEnvironment) environment;
            MutablePropertySources propertySources = configurableEnv.getPropertySources();

            // 从数据库加载配置
            Map<String, Object> dbProps = loadPropertiesFromDatabase();

            // 添加到属性源（高优先级）
            propertySources.addFirst(
                new MapPropertySource("dbProperties", dbProps));
        }
    }

    private Map<String, Object> loadPropertiesFromDatabase() {
        // 从数据库读取配置
        Map<String, Object> props = new HashMap<>();
        props.put("custom.cache.enabled", true);
        props.put("custom.cache.ttl", 3600);
        return props;
    }
}
```

### 6.3 加密属性处理

```java
public class EncryptedPropertySource extends PropertySource<String> {

    private final PropertySource<?> delegate;
    private final StringEncryptor encryptor;

    public EncryptedPropertySource(PropertySource<?> delegate, StringEncryptor encryptor) {
        super("encrypted_" + delegate.getName(), delegate.getName());
        this.delegate = delegate;
        this.encryptor = encryptor;
    }

    @Override
    public Object getProperty(String name) {
        Object value = delegate.getProperty(name);
        if (value instanceof String && isEncrypted((String) value)) {
            return encryptor.decrypt(removePrefix((String) value));
        }
        return value;
    }

    private boolean isEncrypted(String value) {
        return value != null && value.startsWith("ENC(") && value.endsWith(")");
    }
}

// 使用
ConfigurableEnvironment env = new StandardEnvironment();
MutablePropertySources propertySources = env.getPropertySources();

PropertySource<?> originalSource = propertySources.get("systemProperties");
propertySources.replace("systemProperties",
    new EncryptedPropertySource(originalSource, encryptor));
```

### 6.4 属性变更监听

```java
@Component
public class PropertyChangeListener implements EnvironmentAware {

    private ConfigurableEnvironment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @EventListener
    public void onRefresh(RefreshScopeRefreshedEvent event) {
        // 重新加载配置
        String newValue = environment.getProperty("dynamic.config");
        System.out.println("Config changed to: " + newValue);
    }
}
```

---

## 七、与 Spring Boot 的集成

### 7.1 application.properties 加载机制

```java
// Spring Boot 自动添加的属性源
1. application-default.properties
2. application-{profile}.properties
3. application.properties
4. @PropertySource 注解
```

### 7.2 配置属性绑定

```java
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String name;
    private String version;
    private Database database;

    public static class Database {
        private String url;
        private String username;
        private String password;
    }
}
```

### 7.3 多文件配置

```yaml
# application.yml
spring:
  profiles:
    active: dev
  config:
    import:
      - classpath:custom-config.yml
      - optional:file:/etc/app/external-config.yml
```

---

## 八、最佳实践

### 8.1 配置分层设计

```
src/main/resources/
├── application.yml              # 默认配置
├── application-dev.yml          # 开发环境
├── application-test.yml         # 测试环境
├── application-prod.yml         # 生产环境
└── config/
    ├── database.yml             # 数据库配置
    ├── cache.yml                # 缓存配置
    └── security.yml             # 安全配置
```

### 8.2 敏感信息处理

```java
// 使用环境变量注入敏感信息
@Value("${DATABASE_PASSWORD:${env.DATABASE_PASSWORD}}")
private String dbPassword;

// 或使用 Spring Boot 的加密支持
spring.datasource.password=${ENC(encryptedPassword)}
```

### 8.3 配置验证

```java
@Component
@ConfigurationProperties("app")
@Validated
public class AppProperties {

    @NotNull
    @Size(min = 3, max = 50)
    private String name;

    @Min(1)
    @Max(65535)
    private int port;
}
```

### 8.4 测试环境配置

```java
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.profiles.active=test"
})
@SpringBootTest
public class MyServiceTest {
    // 测试代码
}
```

---

## 九、常见问题

### Q1: 属性优先级不生效？

```java
// 错误：直接使用 StandardEnvironment
Environment env = new StandardEnvironment();

// 正确：在 ApplicationContext 刷新前配置
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MyApp.class);

        ConfigurableEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(myPropertySource);

        app.setEnvironment(env);
        app.run(args);
    }
}
```

### Q2: 如何获取所有属性？

```java
@Autowired
private ConfigurableEnvironment env;

public void printAllProperties() {
    for (PropertySource<?> source : env.getPropertySources()) {
        System.out.println("Source: " + source.getName());
        if (source instanceof EnumerablePropertySource) {
            EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) source;
            for (String name : enumerable.getPropertyNames()) {
                System.out.println("  " + name + " = " + source.getProperty(name));
            }
        }
    }
}
```

### Q3: 占位符解析失败？

```java
// 使用 resolvePlaceholders 解析嵌套占位符
String value = environment.resolvePlaceholders("${app.name:defaultName}");

// 使用 resolveRequiredPlaceholders（不存在时抛异常）
String required = environment.resolveRequiredPlaceholders("${app.required}");
```

### Q4: Profile 在测试中的使用？

```java
// 方式1：注解
@ActiveProfiles("test")
@SpringBootTest
public class MyTest { }

// 方式2：动态设置
@Test
public void testWithProfile() {
    System.setProperty("spring.profiles.active", "test");
    // 重新加载上下文
}
```

---

## 十、总结

### 核心要点

1. **Environment 是统一入口**：整合所有配置源，提供统一的属性访问接口

2. **PropertySource 是配置抽象**：每种配置来源对应一个 PropertySource

3. **优先级决定覆盖关系**：先搜索的属性源优先级更高，同名属性会被覆盖

4. **Profile 实现环境隔离**：通过激活不同 Profile 加载不同配置

5. **占位符支持动态解析**：支持 `${...}` 和 `#{...}` 占位符

### 代码模板

```java
// 访问属性
@Autowired
private Environment env;

public void demo() {
    // 基本访问
    String value = env.getProperty("key");

    // 带默认值
    String withDefault = env.getProperty("key", "default");

    // 类型转换
    Integer port = env.getProperty("server.port", Integer.class);

    // 检查 Profile
    if (env.matchesProfiles("dev", "test")) {
        // 开发或测试环境
    }
}

// 自定义属性源
ConfigurableEnvironment env = new StandardEnvironment();
env.getPropertySources().addFirst(
    new MapPropertySource("custom", Collections.singletonMap("key", "value"))
);
```

---

## 参考资料

- [Spring Framework Reference - Environment Abstraction](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-environment)
- [Spring Boot Reference - Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Spring Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
