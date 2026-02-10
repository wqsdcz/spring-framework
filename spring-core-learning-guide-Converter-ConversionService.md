# Spring 类型转换（Converter & ConversionService）深度解析

## 一、概述

Spring 的类型转换系统提供了强大而灵活的机制来处理不同数据类型之间的转换。它替代了早期的 PropertyEditor 机制，提供了更好的类型安全性和性能。

### 1.1 核心组件

```
Converter<S, T>              - 简单类型转换器（单对单）
ConverterFactory<S, R>       - 转换器工厂（一对多）
GenericConverter             - 通用转换器（多对多）
ConditionalConverter         - 条件转换器

ConversionService            - 类型转换服务接口
├── ConfigurableConversionService
└── GenericConversionService
    └── DefaultConversionService  - 默认实现，内置常用转换器

TypeDescriptor               - 类型描述符（支持泛型）
```

### 1.2 与 PropertyEditor 的对比

| 特性 | PropertyEditor | Converter |
|-----|----------------|-----------|
| 线程安全 | 否（有状态） | 是（无状态） |
| 泛型支持 | 弱 | 强 |
| 性能 | 较低 | 较高 |
| 使用方式 | 传统 JavaBeans | 函数式接口 |

---

## 二、Converter 接口详解

### 2.1 基本定义

```java
@FunctionalInterface
public interface Converter<S, T> {
    @Nullable
    T convert(S source);

    // 5.3+ 支持组合
    default <U> Converter<S, U> andThen(Converter<? super T, ? extends U> after) {
        return (S s) -> {
            T initialResult = convert(s);
            return (initialResult != null ? after.convert(initialResult) : null);
        };
    }
}
```

**核心特点**：
- 函数式接口，可用 Lambda 实现
- 线程安全（无状态）
- 支持转换器组合（andThen）

### 2.2 自定义 Converter 示例

```java
// 1. 基本转换器：String -> Integer
public class StringToIntegerConverter implements Converter<String, Integer> {
    @Override
    public Integer convert(String source) {
        return Integer.valueOf(source);
    }
}

// 2. Lambda 实现
Converter<String, Integer> converter = source -> Integer.valueOf(source);

// 3. 复杂对象转换
public class StringToPersonConverter implements Converter<String, Person> {
    @Override
    public Person convert(String source) {
        // 假设格式："name,age"
        String[] parts = source.split(",");
        Person person = new Person();
        person.setName(parts[0]);
        person.setAge(Integer.parseInt(parts[1]));
        return person;
    }
}
```

### 2.3 转换器组合

```java
// 定义多个转换器
Converter<String, Integer> stringToInt = Integer::valueOf;
Converter<Integer, Long> intToLong = Integer::longValue;
Converter<Long, String> longToString = String::valueOf;

// 组合转换器
Converter<String, String> stringToString = stringToInt
    .andThen(intToLong)
    .andThen(longToString);

// 使用
String result = stringToString.convert("123");  // "123"
```

---

## 三、ConversionService 接口详解

### 3.1 核心方法

```java
public interface ConversionService {
    // 检查是否可以转换
    boolean canConvert(@Nullable Class<?> sourceType, Class<?> targetType);
    boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

    // 执行转换
    @Nullable
    <T> T convert(@Nullable Object source, Class<T> targetType);

    @Nullable
    Object convert(@Nullable Object source,
                   @Nullable TypeDescriptor sourceType,
                   TypeDescriptor targetType);
}
```

### 3.2 使用示例

```java
// 获取 ConversionService（Spring 中自动注入）
@Autowired
private ConversionService conversionService;

public void demo() {
    // 1. 基本转换
    Integer num = conversionService.convert("123", Integer.class);

    // 2. 检查再转换
    if (conversionService.canConvert(String.class, LocalDate.class)) {
        LocalDate date = conversionService.convert("2024-01-01", LocalDate.class);
    }

    // 3. 集合转换
    List<String> strings = Arrays.asList("1", "2", "3");
    List<Integer> integers = (List<Integer>) conversionService.convert(
        strings,
        TypeDescriptor.forObject(strings),
        TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(Integer.class))
    );
}
```

---

## 四、ConverterRegistry 详解

### 4.1 接口定义

```java
public interface ConverterRegistry {
    // 注册 Converter（自动解析泛型类型）
    void addConverter(Converter<?, ?> converter);

    // 显式指定类型注册
    <S, T> void addConverter(Class<S> sourceType, Class<T> targetType,
                             Converter<? super S, ? extends T> converter);

    // 注册 GenericConverter
    void addConverter(GenericConverter converter);

    // 注册 ConverterFactory
    void addConverterFactory(ConverterFactory<?, ?> factory);

    // 移除转换器
    void removeConvertible(Class<?> sourceType, Class<?> targetType);
}
```

### 4.2 注册转换器

```java
@Configuration
public class ConversionConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 1. 注册简单转换器
        registry.addConverter(new StringToPersonConverter());

        // 2. Lambda 方式注册
        registry.addConverter(String.class, BigDecimal.class, BigDecimal::new);

        // 3. 注册转换器工厂
        registry.addConverterFactory(new StringToEnumConverterFactory());

        // 4. 注册通用转换器
        registry.addConverter(new ObjectToObjectConverter());
    }
}
```

---

## 五、GenericConversionService 详解

### 5.1 核心实现机制

```java
public class GenericConversionService implements ConfigurableConversionService {

    private final Converters converters = new Converters();
    private final Map<ConverterCacheKey, GenericConverter> converterCache =
        new ConcurrentReferenceHashMap<>(64);

    // 转换器注册
    @Override
    public void addConverter(Converter<?, ?> converter) {
        // 通过 ResolvableType 解析泛型参数
        ResolvableType[] typeInfo = getRequiredTypeInfo(converter.getClass(), Converter.class);
        addConverter(new ConverterAdapter(converter, typeInfo[0], typeInfo[1]));
    }

    // 执行转换
    @Override
    public Object convert(@Nullable Object source,
                          @Nullable TypeDescriptor sourceType,
                          TypeDescriptor targetType) {
        // 1. 查找合适的转换器
        GenericConverter converter = getConverter(sourceType, targetType);

        // 2. 执行转换
        if (converter != null) {
            return ConversionUtils.invokeConverter(converter, source, sourceType, targetType);
        }

        // 3. 处理未找到的情况
        return handleConverterNotFound(source, sourceType, targetType);
    }
}
```

### 5.2 转换器查找策略

```
1. 精确匹配：sourceType -> targetType
2. 继承匹配：sourceType 的子类 -> targetType
3. 包装类型匹配：S -> Wrapper<T>
4. 数组/集合转换：S[] -> List<T>
5. 级联转换：S -> X -> T
```

---

## 六、DefaultConversionService 详解

### 6.1 默认转换器列表

DefaultConversionService 预先注册了大量常用转换器：

```java
public static void addDefaultConverters(ConverterRegistry converterRegistry) {
    // 标量转换
    addScalarConverters(converterRegistry);

    // 集合转换
    addCollectionConverters(converterRegistry);

    // 时间类型转换
    converterRegistry.addConverter(new DateToInstantConverter());
    converterRegistry.addConverter(new InstantToDateConverter());
    converterRegistry.addConverter(new StringToTimeZoneConverter());

    // 对象转换
    converterRegistry.addConverter(new ObjectToObjectConverter());
    converterRegistry.addConverter(new IdToEntityConverter((ConversionService) converterRegistry));
}
```

### 6.2 支持的转换类型

| 源类型 | 目标类型 | 示例 |
|--------|---------|------|
| String | Integer/Long/Double | "123" -> 123 |
| String | Boolean | "true" -> true |
| String | Character | "A" -> 'A' |
| String | Locale | "en_US" -> Locale |
| String | Charset | "UTF-8" -> Charset |
| String | Pattern | 正则表达式字符串 |
| String | Currency | "USD" -> Currency |
| String | UUID | UUID 字符串 |
| Number | Number | Integer -> Long |
| String | Enum | "ACTIVE" -> Status.ACTIVE |
| String | Date/Time | "2024-01-01" -> LocalDate |
| Collection -> Array | List<String> -> String[] |
| Array -> Collection | String[] -> List<String> |
| Map -> Map | Map<String, String> -> Map<String, Integer> |
| Object -> String | 调用 toString() |
| Object -> Optional<T> | 包装为 Optional |

### 6.3 获取共享实例

```java
// 使用共享的默认实例（懒加载，线程安全）
ConversionService conversionService = DefaultConversionService.getSharedInstance();

// 创建自定义实例（推荐用于自定义需求）
DefaultConversionService conversionService = new DefaultConversionService();
conversionService.addConverter(new MyCustomConverter());
```

---

## 七、TypeDescriptor 详解

### 7.1 核心作用

TypeDescriptor 用于在运行时描述 Java 类型，特别支持泛型信息：

```java
public class TypeDescriptor implements Serializable {
    private final Class<?> type;
    private final ResolvableType resolvableType;
    private final AnnotatedElementSupplier annotatedElementSupplier;
}
```

### 7.2 创建方式

```java
// 1. 从 Class 创建
TypeDescriptor intType = TypeDescriptor.valueOf(int.class);
TypeDescriptor stringType = TypeDescriptor.valueOf(String.class);

// 2. 从字段创建（保留泛型信息）
public class MyClass {
    private List<String> names;
}
Field namesField = MyClass.class.getDeclaredField("names");
TypeDescriptor fieldType = new TypeDescriptor(namesField);
// fieldType.getElementTypeDescriptor() -> String

// 3. 从方法参数创建
Method method = MyClass.class.getMethod("setValue", Integer.class);
TypeDescriptor paramType = new TypeDescriptor(
    new MethodParameter(method, 0)
);

// 4. 创建集合类型
TypeDescriptor listType = TypeDescriptor.collection(
    List.class,
    TypeDescriptor.valueOf(String.class)
);
// List<String>
```

### 7.3 泛型支持

```java
// 解析 List<String>
TypeDescriptor listOfString = TypeDescriptor.forObject(Arrays.asList("a", "b"));
TypeDescriptor elementType = listOfString.getElementTypeDescriptor();
// elementType -> String

// 解析 Map<String, Integer>
TypeDescriptor mapType = TypeDescriptor.map(
    Map.class,
    TypeDescriptor.valueOf(String.class),
    TypeDescriptor.valueOf(Integer.class)
);
```

---

## 八、实际应用场景

### 8.1 Web 请求参数绑定

```java
@RestController
public class UserController {

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        // Spring 自动将 String 路径变量转换为 Long
        return userService.findById(id);
    }

    @GetMapping("/search")
    public List<User> search(
            @RequestParam LocalDate startDate,
            @RequestParam Status status) {
        // 自动转换 String 请求参数为 LocalDate 和 Enum
        return userService.search(startDate, status);
    }
}
```

### 8.2 配置文件属性绑定

```java
@Component
@ConfigurationProperties("app")
public class AppProperties {
    private String name;
    private Integer port;
    private List<String> servers;
    private Map<String, Integer> timeouts;

    // 自动类型转换：
    // "8080" -> 8080 (String -> Integer)
    // "server1,server2" -> List<String>
    // "key1:100,key2:200" -> Map<String, Integer>
}
```

### 8.3 自定义转换器实战

```java
// 1. 定义转换器
@Component
public class StringToPhoneNumberConverter implements Converter<String, PhoneNumber> {
    @Override
    public PhoneNumber convert(String source) {
        // 解析各种格式：+86-138-0013-8000, 13800138000, 138-0013-8000
        String normalized = source.replaceAll("[^\\d]", "");
        if (normalized.length() == 11) {
            PhoneNumber phone = new PhoneNumber();
            phone.setCountryCode("86");
            phone.setNumber(normalized);
            return phone;
        }
        throw new IllegalArgumentException("Invalid phone number: " + source);
    }
}

// 2. 注册转换器
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private StringToPhoneNumberConverter phoneConverter;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(phoneConverter);
    }
}

// 3. 使用
@RestController
public class OrderController {
    @PostMapping("/orders")
    public Order createOrder(@RequestParam PhoneNumber phone) {
        // 自动转换 "138-0013-8000" -> PhoneNumber 对象
        return orderService.create(phone);
    }
}
```

### 8.4 枚举转换器工厂

```java
public class StringToEnumConverterFactory implements ConverterFactory<String, Enum<?>> {

    @Override
    public <T extends Enum<?>> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToEnumConverter<>(targetType);
    }

    private static class StringToEnumConverter<T extends Enum<?>> implements Converter<String, T> {
        private final Class<T> enumType;

        public StringToEnumConverter(Class<T> enumType) {
            this.enumType = enumType;
        }

        @Override
        public T convert(String source) {
            if (source.isEmpty()) {
                return null;
            }
            return Enum.valueOf(enumType, source.trim().toUpperCase());
        }
    }
}

// 注册后支持所有枚举类型的转换
```

### 8.5 复杂对象转换

```java
// DTO -> Entity 转换器
@Component
public class UserDtoToEntityConverter implements Converter<UserDto, User> {

    @Override
    public User convert(UserDto dto) {
        User user = new User();
        user.setId(dto.getId());
        user.setName(dto.getFirstName() + " " + dto.getLastName());
        user.setEmail(dto.getEmail());

        // 使用 ConversionService 转换嵌套对象
        if (dto.getAddress() != null) {
            user.setAddress(conversionService.convert(dto.getAddress(), Address.class));
        }

        return user;
    }
}
```

---

## 九、最佳实践

### 9.1 转换器设计原则

```java
// ✅ 好的实践：无状态、线程安全
public class GoodConverter implements Converter<String, Integer> {
    @Override
    public Integer convert(String source) {
        return Integer.valueOf(source);
    }
}

// ❌ 避免：有状态、非线程安全
public class BadConverter implements Converter<String, Integer> {
    private int counter = 0;  // 有状态！

    @Override
    public Integer convert(String source) {
        counter++;  // 线程不安全
        return Integer.valueOf(source);
    }
}
```

### 9.2 错误处理

```java
public class SafeConverter implements Converter<String, LocalDate> {

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public LocalDate convert(String source) {
        try {
            return LocalDate.parse(source, formatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                "Invalid date format: " + source + ", expected: yyyy-MM-dd", e);
        }
    }
}
```

### 9.3 性能优化

```java
// 缓存转换器实例
@Configuration
public class ConversionConfig {

    @Bean
    public ConversionService conversionService() {
        DefaultConversionService service = new DefaultConversionService();

        // 预注册常用转换器
        service.addConverter(new StringToPhoneNumberConverter());
        service.addConverter(new UserDtoToEntityConverter());

        return service;
    }
}

// 复用 TypeDescriptor
public class TypeDescriptorCache {
    private static final Map<Class<?>, TypeDescriptor> cache = new ConcurrentHashMap<>();

    public static TypeDescriptor get(Class<?> type) {
        return cache.computeIfAbsent(type, TypeDescriptor::valueOf);
    }
}
```

### 9.4 测试转换器

```java
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ConversionConfig.class)
public class ConverterTest {

    @Autowired
    private ConversionService conversionService;

    @Test
    public void testStringToPhoneNumber() {
        PhoneNumber phone = conversionService.convert(
            "138-0013-8000", PhoneNumber.class);

        assertEquals("86", phone.getCountryCode());
        assertEquals("13800138000", phone.getNumber());
    }

    @Test
    public void testInvalidPhoneNumber() {
        assertThrows(IllegalArgumentException.class, () -> {
            conversionService.convert("invalid", PhoneNumber.class);
        });
    }
}
```

---

## 十、常见问题

### Q1: Converter 和 Formatter 的区别？

```java
// Converter：任意类型之间的转换
public interface Converter<S, T> {
    T convert(S source);
}

// Formatter：String <-> Object 的双向转换（用于Web层）
public interface Formatter<T> extends Printer<T>, Parser<T> {
    String print(T object, Locale locale);
    T parse(String text, Locale locale) throws ParseException;
}

// 使用场景
// Converter：任何类型转换场景
// Formatter：Web请求/响应中的字符串格式化
```

### Q2: 如何处理转换失败？

```java
// 方式1：抛出异常（推荐）
public class StrictConverter implements Converter<String, Integer> {
    @Override
    public Integer convert(String source) {
        try {
            return Integer.parseInt(source);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Cannot convert '" + source + "' to Integer", e);
        }
    }
}

// 方式2：返回默认值
public class LenientConverter implements Converter<String, Integer> {
    @Override
    public Integer convert(String source) {
        try {
            return Integer.parseInt(source);
        } catch (NumberFormatException e) {
            return 0;  // 或 null
        }
    }
}
```

### Q3: 如何支持双向转换？

```java
// 分别注册两个方向的转换器
@Configuration
public class BidirectionalConfig implements WebMvcConfigurer {
    @Override
    public void addFormatters(FormatterRegistry registry) {
        // String -> Entity
        registry.addConverter(new StringToEntityConverter());

        // Entity -> String
        registry.addConverter(new EntityToStringConverter());
    }
}
```

### Q4: 如何调试转换过程？

```java
@Configuration
public class DebugConfig {

    @Bean
    public ConversionService conversionService() {
        DefaultConversionService service = new DefaultConversionService();

        // 包装转换器以添加日志
        service.addConverter(String.class, Integer.class, source -> {
            System.out.println("Converting: " + source);
            Integer result = Integer.valueOf(source);
            System.out.println("Result: " + result);
            return result;
        });

        return service;
    }
}
```

---

## 十一、总结

### 核心要点

1. **Converter 是类型转换的基础单元**：简单、无状态、线程安全

2. **ConversionService 是统一入口**：管理和执行所有类型转换

3. **TypeDescriptor 支持泛型**：在运行时保留完整的泛型信息

4. **DefaultConversionService 提供开箱即用的转换**：涵盖大多数常见场景

5. **转换器注册方式多样**：Converter、ConverterFactory、GenericConverter

### 代码模板

```java
// 自定义转换器
public class MyConverter implements Converter<Source, Target> {
    @Override
    public Target convert(Source source) {
        // 转换逻辑
        return target;
    }
}

// 注册和使用
@Configuration
public class Config implements WebMvcConfigurer {
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new MyConverter());
    }
}

// 直接使用
@Autowired
private ConversionService conversionService;

Target target = conversionService.convert(source, Target.class);
```

---

## 参考资料

- [Spring Framework Reference - Type Conversion](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#core-convert)
- [Spring Framework Reference - Formatter SPI](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#format)
- [Spring Boot Properties Binding](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.typesafe-configuration-properties)
