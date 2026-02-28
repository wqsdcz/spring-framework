# Spring PropertyEditors 详细文档

## 目录

1. [概述](#概述)
2. [PropertyEditor 接口详解](#propertyeditor-接口详解)
3. [PropertyEditor 分类详解](#propertyeditor-分类详解)
   - [布尔/数字类型](#布尔数字类型)
   - [字符类型](#字符类型)
   - [字节数组](#字节数组)
   - [字符串数组](#字符串数组)
   - [集合类型](#集合类型)
   - [日期类型](#日期类型)
   - [类相关类型](#类相关类型)
   - [文件/路径类型](#文件路径类型)
   - [区域设置类型](#区域设置类型)
   - [其他类型](#其他类型)
4. [PropertyEditor 注册机制](#propertyeditor-注册机制)
5. [自定义 PropertyEditor 实现](#自定义-propertyeditor-实现)
6. [使用示例](#使用示例)

---

## 概述

`org.springframework.beans.propertyeditors` 包提供了 28+ 个 PropertyEditor 实现，用于在 Spring 框架中进行字符串到各种 Java 对象类型的转换。这些编辑器是 Spring 数据绑定机制的核心组件，广泛应用于：

- **Bean 属性注入**：将 XML 或注解中的字符串值转换为实际的 Java 对象
- **Web 数据绑定**：在 Spring MVC 中将请求参数绑定到控制器方法的参数
- **配置解析**：解析配置文件中的各种类型值

### 包说明

```java
/**
 * Properties editors used to convert from String values to object
 * types such as java.util.Properties.
 *
 * <p>Some of these editors are registered automatically by BeanWrapperImpl.
 * "CustomXxxEditor" classes are intended for manual registration in
 * specific binding processes, as they are localized or the like.
 */
```

---

## PropertyEditor 接口详解

### JavaBeans PropertyEditor 接口

PropertyEditor 是 JavaBeans 规范定义的接口，用于提供属性编辑功能：

```java
public interface PropertyEditor {
    // 设置属性值
    void setValue(Object value);

    // 获取属性值
    Object getValue();

    // 将字符串转换为属性值
    void setAsText(String text) throws IllegalArgumentException;

    // 将属性值转换为字符串
    String getAsText();

    // 其他方法...
}
```

### PropertyEditorSupport 基类

Spring 的所有 PropertyEditor 都继承自 `java.beans.PropertyEditorSupport`，它提供了：

- **值存储**：内部维护属性值
- **事件支持**：支持属性变更事件监听
- **默认实现**：提供默认的方法实现

```java
public class PropertyEditorSupport {
    private Object value;

    public void setValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setAsText(String text) throws IllegalArgumentException {
        throw new IllegalArgumentException("Cannot convert string to target type");
    }

    public String getAsText() {
        return (value != null) ? value.toString() : null;
    }
}
```

---

## PropertyEditor 分类详解

### 布尔/数字类型

#### 1. CustomBooleanEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\CustomBooleanEditor.java`

**用途**：将字符串转换为 Boolean/boolean 类型。

**支持的字符串值**：
- `true`, `on`, `yes`, `1` → `Boolean.TRUE`
- `false`, `off`, `no`, `0` → `Boolean.FALSE`

**特性**：
- 支持自定义 true/false 字符串值
- 支持空字符串处理（可配置为 null）
- 不区分大小写

**构造器**：
```java
// 使用默认字符串值（true/false/on/yes/off/no/1/0）
public CustomBooleanEditor(boolean allowEmpty)

// 使用自定义字符串值
public CustomBooleanEditor(String trueString, String falseString, boolean allowEmpty)
```

**使用场景**：
- Web 表单中的复选框值绑定
- 配置文件中的布尔值解析

---

#### 2. CustomNumberEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\CustomNumberEditor.java`

**用途**：将字符串转换为任意 Number 子类（Short, Integer, Long, BigInteger, Float, Double, BigDecimal）。

**特性**：
- 支持使用 NumberFormat 进行本地化解析和渲染
- 支持空字符串处理
- 自动类型转换

**构造器**：
```java
// 使用默认 valueOf 方法
public CustomNumberEditor(Class<? extends Number> numberClass, boolean allowEmpty)

// 使用指定的 NumberFormat
public CustomNumberEditor(Class<? extends Number> numberClass, NumberFormat numberFormat, boolean allowEmpty)
```

**使用场景**：
- 本地化数字输入
- 货币金额解析
- 科学计数法数字

---

### 字符类型

#### 3. CharacterEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\CharacterEditor.java`

**用途**：将字符串转换为 Character/char 类型。

**特性**：
- 支持 Unicode 字符序列（如 `\u0041` 表示 'A'）
- 支持空字符串处理
- JDK 没有提供默认的 char PropertyEditor

**构造器**：
```java
public CharacterEditor(boolean allowEmpty)
```

**示例**：
```java
// 单字符
editor.setAsText("A");  // 结果: 'A'

// Unicode 序列
editor.setAsText("\\u0041");  // 结果: 'A'
```

---

#### 4. CharArrayPropertyEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\CharArrayPropertyEditor.java`

**用途**：将字符串转换为 char[] 数组。

**实现**：直接使用 `String.toCharArray()` 方法。

**示例**：
```java
editor.setAsText("Hello");  // 结果: ['H', 'e', 'l', 'l', 'o']
```

---

### 字节数组

#### 5. ByteArrayPropertyEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\ByteArrayPropertyEditor.java`

**用途**：将字符串转换为 byte[] 数组。

**实现**：使用 `String.getBytes()` 方法。

**注意**：使用平台默认字符集编码。

---

### 字符串数组

#### 6. StringArrayPropertyEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\StringArrayPropertyEditor.java`

**用途**：将 CSV 格式字符串转换为 String[] 数组。

**特性**：
- 可自定义分隔符（默认为逗号）
- 支持删除特定字符
- 支持空数组转为 null
- 支持自动 trim 元素

**构造器**：
```java
public StringArrayPropertyEditor()
public StringArrayPropertyEditor(String separator)
public StringArrayPropertyEditor(String separator, boolean emptyArrayAsNull)
public StringArrayPropertyEditor(String separator, String charsToDelete, boolean emptyArrayAsNull, boolean trimValues)
```

**示例**：
```java
// 默认逗号分隔
editor.setAsText("a, b, c");  // 结果: ["a", "b", "c"]

// 自定义分隔符
editor.setAsText("a;b;c");  // 使用分号分隔
```

---

#### 7. StringTrimmerEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\StringTrimmerEditor.java`

**用途**：修剪字符串两端的空白字符。

**特性**：
- 支持将空字符串转为 null
- 支持删除特定字符

**构造器**：
```java
public StringTrimmerEditor(boolean emptyAsNull)
public StringTrimmerEditor(String charsToDelete, boolean emptyAsNull)
```

**使用场景**：
- Web 表单输入清理
- 去除换行符等特殊字符

---

### 集合类型

#### 8. CustomCollectionEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\CustomCollectionEditor.java`

**用途**：将任意源 Collection 转换为目标 Collection 类型。

**支持的类型**：
- `Collection`
- `Set`
- `SortedSet`（默认实现：TreeSet）
- `List`（默认实现：ArrayList）

**特性**：
- 支持 null 转为空集合
- 支持数组元素转换
- 提供钩子方法 `convertElement()` 用于元素转换

**构造器**：
```java
public CustomCollectionEditor(Class<? extends Collection> collectionType)
public CustomCollectionEditor(Class<? extends Collection> collectionType, boolean nullAsEmptyCollection)
```

**扩展示例**：
```java
public class IntegerSetEditor extends CustomCollectionEditor {
    public IntegerSetEditor() {
        super(Set.class);
    }

    @Override
    protected Object convertElement(Object element) {
        if (element instanceof String) {
            return Integer.valueOf((String) element);
        }
        return element;
    }
}
```

---

#### 9. CustomMapEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\CustomMapEditor.java`

**用途**：将任意源 Map 转换为目标 Map 类型。

**支持的类型**：
- `Map`（默认实现：LinkedHashMap）
- `SortedMap`（默认实现：TreeMap）

**特性**：
- 支持 null 转为空 Map
- 提供 `convertKey()` 和 `convertValue()` 钩子方法

**构造器**：
```java
public CustomMapEditor(Class<? extends Map> mapType)
public CustomMapEditor(Class<? extends Map> mapType, boolean nullAsEmptyMap)
```

---

### 日期类型

#### 10. CustomDateEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\CustomDateEditor.java`

**用途**：使用自定义 DateFormat 解析和渲染 java.util.Date。

**特性**：
- 支持指定 DateFormat
- 支持空字符串处理
- 支持精确日期长度验证

**构造器**：
```java
public CustomDateEditor(DateFormat dateFormat, boolean allowEmpty)
public CustomDateEditor(DateFormat dateFormat, boolean allowEmpty, int exactDateLength)
```

**使用场景**：
- 本地化日期输入
- 严格日期格式验证

**示例**：
```java
SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
CustomDateEditor editor = new CustomDateEditor(dateFormat, true);

editor.setAsText("2024-01-15");  // 解析为 Date 对象
```

---

### 类相关类型

#### 11. ClassEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\ClassEditor.java`

**用途**：将类名字符串转换为 Class 对象。

**特性**：
- 支持数组类名（如 `java.lang.String[]`）
- 可指定 ClassLoader

**构造器**：
```java
public ClassEditor()  // 使用线程上下文 ClassLoader
public ClassEditor(ClassLoader classLoader)
```

**示例**：
```java
editor.setAsText("java.lang.String");     // 结果: String.class
editor.setAsText("java.lang.String[]");   // 结果: String[].class
```

---

#### 12. ClassArrayEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\ClassArrayEditor.java`

**用途**：将逗号分隔的类名字符串转换为 Class[] 数组。

**示例**：
```java
editor.setAsText("java.lang.String, java.lang.Integer");
// 结果: [String.class, Integer.class]
```

---

### 文件/路径类型

#### 13. FileEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\FileEditor.java`

**用途**：将 Spring 资源位置字符串转换为 File 对象。

**支持的格式**：
- 标准 URL（`file:`, `http:` 等）
- Spring 特殊 URL（`classpath:`）
- 绝对路径
- 相对路径

**构造器**：
```java
public FileEditor()  // 使用默认 ResourceEditor
public FileEditor(ResourceEditor resourceEditor)
```

**示例**：
```java
editor.setAsText("classpath:config.properties");
editor.setAsText("file:/path/to/file.txt");
editor.setAsText("/absolute/path/file.txt");
```

---

#### 14. PathEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\PathEditor.java`

**用途**：将字符串转换为 java.nio.file.Path 对象。

**特性**：
- 支持 NIO 文件系统提供者
- 支持 Spring 资源位置
- 自动规范化路径

**示例**：
```java
editor.setAsText("/path/to/file.txt");  // 结果: Path 对象
```

---

#### 15. URLEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\URLEditor.java`

**用途**：将字符串转换为 java.net.URL 对象。

**特性**：
- 支持 Spring 的 `classpath:` 伪 URL
- URL 必须指定有效协议

---

#### 16. URIEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\URIEditor.java`

**用途**：将字符串转换为 java.net.URI 对象。

**特性**：
- 支持字符串编码（默认启用）
- 支持 `classpath:` 位置解析
- URI 语法比 URL 更宽松

**构造器**：
```java
public URIEditor()  // 默认编码
public URIEditor(boolean encode)
public URIEditor(ClassLoader classLoader)
public URIEditor(ClassLoader classLoader, boolean encode)
```

---

#### 17. InputStreamEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\InputStreamEditor.java`

**用途**：将资源位置字符串转换为 InputStream。

**注意**：单向编辑器（只支持字符串到 InputStream），返回的流需要手动关闭。

---

#### 18. ReaderEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\ReaderEditor.java`

**用途**：将资源位置字符串转换为 Reader。

**特性**：
- 使用 EncodedResource 处理字符编码
- 单向编辑器

---

### 区域设置类型

#### 19. LocaleEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\LocaleEditor.java`

**用途**：将字符串转换为 java.util.Locale。

**支持的格式**：
- `en`（语言）
- `en_US`（语言_国家）
- `en_US_variant`（语言_国家_变体）
- 支持空格作为分隔符

**示例**：
```java
editor.setAsText("en");       // 结果: Locale.ENGLISH
editor.setAsText("en_US");    // 结果: Locale.US
editor.setAsText("zh_CN");    // 结果: Locale.CHINA
```

---

#### 20. TimeZoneEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\TimeZoneEditor.java`

**用途**：将时区 ID 字符串转换为 TimeZone 对象。

**示例**：
```java
editor.setAsText("America/New_York");
editor.setAsText("GMT+8");
```

---

#### 21. ZoneIdEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\ZoneIdEditor.java`

**用途**：将字符串转换为 java.time.ZoneId（Java 8 日期时间 API）。

**示例**：
```java
editor.setAsText("Asia/Shanghai");
editor.setAsText("Europe/Paris");
```

---

#### 22. CurrencyEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\CurrencyEditor.java`

**用途**：将货币代码字符串转换为 Currency 对象。

**示例**：
```java
editor.setAsText("USD");  // 美元
editor.setAsText("CNY");  // 人民币
editor.setAsText("EUR");  // 欧元
```

---

### 其他类型

#### 23. PatternEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\PatternEditor.java`

**用途**：将字符串转换为 java.util.regex.Pattern。

**特性**：
- 支持正则表达式标志位

**构造器**：
```java
public PatternEditor()
public PatternEditor(int flags)  // 如 Pattern.CASE_INSENSITIVE
```

**示例**：
```java
editor.setAsText("[a-zA-Z]+");  // 字母匹配模式
```

---

#### 24. PropertiesEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\PropertiesEditor.java`

**用途**：将字符串转换为 java.util.Properties。

**特性**：
- 支持标准 Properties 文件格式
- 支持 Map 到 Properties 的转换

**示例**：
```java
String props = "key1=value1\nkey2=value2";
editor.setAsText(props);
```

---

#### 25. UUIDEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\UUIDEditor.java`

**用途**：将字符串转换为 java.util.UUID。

**示例**：
```java
editor.setAsText("550e8400-e29b-41d4-a716-446655440000");
```

---

#### 26. CharsetEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\CharsetEditor.java`

**用途**：将字符集名称字符串转换为 Charset 对象。

**示例**：
```java
editor.setAsText("UTF-8");
editor.setAsText("ISO-8859-1");
```

---

#### 27. InputSourceEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\InputSourceEditor.java`

**用途**：将资源位置字符串转换为 org.xml.sax.InputSource。

**用途**：XML 解析场景。

---

#### 28. ResourceBundleEditor

**位置**：`f:\GitHub\spring-framework\spring-beans\src\main\java\org\springframework\beans\propertyeditors\ResourceBundleEditor.java`

**用途**：将字符串转换为 java.util.ResourceBundle。

**特性**：
- 支持基本名称和区域设置
- 使用 `_` 分隔基本名称和区域设置

**示例**：
```java
editor.setAsText("messages");           // 加载 messages.properties
editor.setAsText("messages_en_US");     // 加载 messages_en_US.properties
```

---

## PropertyEditor 注册机制

### 默认编辑器注册

`PropertyEditorRegistrySupport` 类在 `createDefaultEditors()` 方法中注册了所有默认编辑器：

```java
private void createDefaultEditors() {
    this.defaultEditors = new HashMap<>(64);

    // 简单编辑器
    this.defaultEditors.put(Charset.class, new CharsetEditor());
    this.defaultEditors.put(Class.class, new ClassEditor());
    this.defaultEditors.put(Class[].class, new ClassArrayEditor());
    this.defaultEditors.put(Currency.class, new CurrencyEditor());
    this.defaultEditors.put(File.class, new FileEditor());
    this.defaultEditors.put(InputStream.class, new InputStreamEditor());
    this.defaultEditors.put(InputSource.class, new InputSourceEditor());
    this.defaultEditors.put(Locale.class, new LocaleEditor());
    this.defaultEditors.put(Path.class, new PathEditor());
    this.defaultEditors.put(Pattern.class, new PatternEditor());
    this.defaultEditors.put(Properties.class, new PropertiesEditor());
    this.defaultEditors.put(Reader.class, new ReaderEditor());
    this.defaultEditors.put(TimeZone.class, new TimeZoneEditor());
    this.defaultEditors.put(URI.class, new URIEditor());
    this.defaultEditors.put(URL.class, new URLEditor());
    this.defaultEditors.put(UUID.class, new UUIDEditor());
    this.defaultEditors.put(ZoneId.class, new ZoneIdEditor());

    // 集合编辑器
    this.defaultEditors.put(Collection.class, new CustomCollectionEditor(Collection.class));
    this.defaultEditors.put(Set.class, new CustomCollectionEditor(Set.class));
    this.defaultEditors.put(SortedSet.class, new CustomCollectionEditor(SortedSet.class));
    this.defaultEditors.put(List.class, new CustomCollectionEditor(List.class));
    this.defaultEditors.put(SortedMap.class, new CustomMapEditor(SortedMap.class));

    // 基本类型数组
    this.defaultEditors.put(byte[].class, new ByteArrayPropertyEditor());
    this.defaultEditors.put(char[].class, new CharArrayPropertyEditor());

    // 字符类型
    this.defaultEditors.put(char.class, new CharacterEditor(false));
    this.defaultEditors.put(Character.class, new CharacterEditor(true));

    // 布尔类型
    this.defaultEditors.put(boolean.class, new CustomBooleanEditor(false));
    this.defaultEditors.put(Boolean.class, new CustomBooleanEditor(true));

    // 数字类型
    this.defaultEditors.put(byte.class, new CustomNumberEditor(Byte.class, false));
    this.defaultEditors.put(Byte.class, new CustomNumberEditor(Byte.class, true));
    this.defaultEditors.put(int.class, new CustomNumberEditor(Integer.class, false));
    this.defaultEditors.put(Integer.class, new CustomNumberEditor(Integer.class, true));
    // ... 其他数字类型
}
```

### PropertyEditorRegistry 接口

```java
public interface PropertyEditorRegistry {
    // 为指定类型注册编辑器
    void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor);

    // 为指定路径和类型注册编辑器
    void registerCustomEditor(Class<?> requiredType, String propertyPath, PropertyEditor propertyEditor);

    // 查找编辑器
    PropertyEditor findCustomEditor(Class<?> requiredType, String propertyPath);
}
```

### PropertyEditorRegistrar 接口

```java
public interface PropertyEditorRegistrar {
    void registerCustomEditors(PropertyEditorRegistry registry);
}
```

### CustomEditorConfigurer

用于在 Spring 配置中注册自定义编辑器：

```java
public class CustomEditorConfigurer implements BeanFactoryPostProcessor {
    private PropertyEditorRegistrar[] propertyEditorRegistrars;
    private Map<Class<?>, Class<? extends PropertyEditor>> customEditors;

    // setter 方法...
}
```

---

## 自定义 PropertyEditor 实现

### 实现步骤

1. **继承 PropertyEditorSupport**
2. **重写 setAsText() 方法**：字符串到对象的转换
3. **重写 getAsText() 方法**：对象到字符串的转换（可选）

### 简单示例

```java
public class PhoneNumberEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null || text.isEmpty()) {
            setValue(null);
            return;
        }

        // 移除所有非数字字符
        String digits = text.replaceAll("\\D", "");

        if (digits.length() != 10 && digits.length() != 11) {
            throw new IllegalArgumentException("Invalid phone number: " + text);
        }

        setValue(digits);
    }

    @Override
    public String getAsText() {
        Object value = getValue();
        if (value == null) {
            return "";
        }

        String digits = (String) value;
        // 格式化为 (xxx) xxx-xxxx
        return String.format("(%s) %s-%s",
            digits.substring(0, 3),
            digits.substring(3, 6),
            digits.substring(6));
    }
}
```

### 复杂对象示例

```java
public class AddressEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (!StringUtils.hasText(text)) {
            setValue(null);
            return;
        }

        // 格式: "街道,城市,省份,邮编"
        String[] parts = text.split(",");
        if (parts.length != 4) {
            throw new IllegalArgumentException(
                "Address format should be: street,city,province,zipCode");
        }

        Address address = new Address();
        address.setStreet(parts[0].trim());
        address.setCity(parts[1].trim());
        address.setProvince(parts[2].trim());
        address.setZipCode(parts[3].trim());

        setValue(address);
    }

    @Override
    public String getAsText() {
        Address address = (Address) getValue();
        if (address == null) {
            return "";
        }
        return String.format("%s,%s,%s,%s",
            address.getStreet(),
            address.getCity(),
            address.getProvince(),
            address.getZipCode());
    }
}
```

---

## 使用示例

### XML 配置示例

#### 1. 注册自定义编辑器

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- 方式1: 使用 CustomEditorConfigurer -->
    <bean class="org.springframework.beans.factory.config.CustomEditorConfigurer">
        <property name="customEditors">
            <map>
                <entry key="java.util.Date" value="com.example.CustomDateEditor"/>
                <entry key="com.example.PhoneNumber" value="com.example.PhoneNumberEditor"/>
            </map>
        </property>
    </bean>

    <!-- 方式2: 使用 PropertyEditorRegistrar -->
    <bean class="org.springframework.beans.factory.config.CustomEditorConfigurer">
        <property name="propertyEditorRegistrars">
            <list>
                <bean class="com.example.MyPropertyEditorRegistrar"/>
            </list>
        </property>
    </bean>

    <!-- 使用自定义类型的 Bean -->
    <bean id="user" class="com.example.User">
        <property name="birthDate" value="2024-01-15"/>
        <property name="phone" value="(123) 456-7890"/>
    </bean>

</beans>
```

#### 2. PropertyEditorRegistrar 实现

```java
public class MyPropertyEditorRegistrar implements PropertyEditorRegistrar {

    @Override
    public void registerCustomEditors(PropertyEditorRegistry registry) {
        // 注册日期编辑器
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        registry.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));

        // 注册电话号码编辑器
        registry.registerCustomEditor(PhoneNumber.class, new PhoneNumberEditor());

        // 注册地址编辑器
        registry.registerCustomEditor(Address.class, new AddressEditor());
    }
}
```

### Java 配置示例

#### 1. 使用 @InitBinder（Spring MVC）

```java
@Controller
public class UserController {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // 注册日期编辑器
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));

        // 注册字符串修剪编辑器
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));

        // 注册自定义布尔编辑器
        binder.registerCustomEditor(Boolean.class,
            new CustomBooleanEditor("yes", "no", true));
    }

    @PostMapping("/users")
    public String createUser(@ModelAttribute User user) {
        // 绑定后的 user 对象已经包含正确类型的属性值
        return "success";
    }
}
```

#### 2. 全局配置（@ControllerAdvice）

```java
@ControllerAdvice
public class GlobalBindingInitializer {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // 全局注册编辑器
        binder.registerCustomEditor(Date.class,
            new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), true));

        binder.registerCustomEditor(String.class,
            new StringTrimmerEditor(true));
    }
}
```

#### 3. 使用 Formatter（现代推荐方式）

```java
@Component
public class DateFormatter implements Formatter<Date> {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public Date parse(String text, Locale locale) throws ParseException {
        return dateFormat.parse(text);
    }

    @Override
    public String print(Date object, Locale locale) {
        return dateFormat.format(object);
    }
}

// 注册 Formatter
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private DateFormatter dateFormatter;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(dateFormatter);
    }
}
```

### 程序化使用示例

```java
public class PropertyEditorDemo {

    public static void main(String[] args) {
        // 创建 BeanWrapper
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(new User());

        // 注册自定义编辑器
        wrapper.registerCustomEditor(Date.class,
            new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), true));

        // 设置属性（字符串自动转换为 Date）
        wrapper.setPropertyValue("birthDate", "2024-01-15");

        // 获取转换后的值
        User user = (User) wrapper.getWrappedInstance();
        System.out.println(user.getBirthDate());  // Mon Jan 15 00:00:00 CST 2024
    }
}
```

---

## 总结

Spring 的 PropertyEditors 提供了强大的类型转换能力：

1. **开箱即用**：28+ 个内置编辑器覆盖常见类型
2. **可扩展**：通过继承 PropertyEditorSupport 轻松实现自定义编辑器
3. **灵活注册**：支持 XML、注解和程序化注册方式
4. **与数据绑定集成**：自动应用于 Bean 属性注入和 Web 数据绑定

### 注意事项

- PropertyEditors 是有状态的，不是线程安全的
- 在 Web 应用中，建议使用 Formatter 替代 PropertyEditor（Spring 3.0+）
- 对于复杂类型转换，考虑使用 Spring 的 ConversionService
