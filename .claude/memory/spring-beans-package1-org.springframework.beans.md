# org.springframework.beans 包详解

## 概述

`org.springframework.beans` 包是 Spring Beans 模块的基础包，提供了操作 JavaBean 的核心基础设施。该包主要包含以下功能：

1. **BeanWrapper** - Bean 包装和属性访问
2. **PropertyAccessor** - 属性访问基础设施
3. **TypeConverter** - 类型转换系统
4. **PropertyEditor** - 属性编辑器注册和管理
5. **属性值管理** - PropertyValue 和 MutablePropertyValues

---

## 1. 核心接口和类层次结构

### 1.1 PropertyAccessor 接口

**位置**: `PropertyAccessor.java`

`PropertyAccessor` 是能够访问命名属性的类的通用接口，作为 `BeanWrapper` 的基础接口。

```
PropertyAccessor (接口)
    └── ConfigurablePropertyAccessor (接口)
            └── AbstractPropertyAccessor (抽象类)
                    └── TypeConverterSupport (抽象类)
                            └── AbstractNestablePropertyAccessor (抽象类)
                                    └── BeanWrapperImpl (实现类)
                                    └── DirectFieldAccessor (实现类)
```

**核心常量定义**:
```java
// 嵌套属性路径分隔符，例如: getFoo().getBar() 对应 "foo.bar"
String NESTED_PROPERTY_SEPARATOR = ".";
char NESTED_PROPERTY_SEPARATOR_CHAR = '.';

// 索引或映射属性键的标记，例如: "person.addresses[0]"
String PROPERTY_KEY_PREFIX = "[";
char PROPERTY_KEY_PREFIX_CHAR = '[';
String PROPERTY_KEY_SUFFIX = "]";
char PROPERTY_KEY_SUFFIX_CHAR = ']';
```

**核心方法**:

| 方法 | 说明 |
|------|------|
| `isReadableProperty(String propertyName)` | 判断属性是否可读 |
| `isWritableProperty(String propertyName)` | 判断属性是否可写 |
| `getPropertyType(String propertyName)` | 获取属性类型 |
| `getPropertyTypeDescriptor(String propertyName)` | 获取属性的类型描述符 |
| `getPropertyValue(String propertyName)` | 获取属性值 |
| `setPropertyValue(String propertyName, Object value)` | 设置属性值 |
| `setPropertyValue(PropertyValue pv)` | 通过 PropertyValue 设置属性 |
| `setPropertyValues(PropertyValues pvs)` | 批量设置属性值 |
| `setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid)` | 批量设置（带控制选项）|

---

## 2. BeanWrapper 系统

### 2.1 BeanWrapper 接口

**位置**: `BeanWrapper.java`

`BeanWrapper` 是 Spring 底层 JavaBeans 基础设施的核心接口，提供分析和操作标准 JavaBeans 的操作。

**特点**:
- 支持**嵌套属性**，允许在子属性上设置属性至无限深度
- 默认 `extractOldValueForEditor` 为 `false`，避免 getter 方法调用产生的副作用
- 通常不直接使用，而是通过 `BeanFactory` 或 `DataBinder` 隐式使用

**核心方法**:

```java
// 设置数组和集合自动增长的上限（默认为无限制）
void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);
int getAutoGrowCollectionLimit();

// 获取被包装的 bean 实例和类型
Object getWrappedInstance();
Class<?> getWrappedClass();

// 获取属性描述符
PropertyDescriptor[] getPropertyDescriptors();
PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException;
```

### 2.2 BeanWrapperImpl 类

**位置**: `BeanWrapperImpl.java`

`BeanWrapperImpl` 是 `BeanWrapper` 接口的默认实现，缓存内省结果以提高效率。

**构造器**:

```java
// 创建空的 BeanWrapperImpl，之后需要设置被包装实例
public BeanWrapperImpl()

// 为给定对象创建 BeanWrapperImpl
public BeanWrapperImpl(Object object)

// 包装指定类的新实例
public BeanWrapperImpl(Class<?> clazz)

// 为嵌套路径创建 BeanWrapperImpl
public BeanWrapperImpl(Object object, String nestedPath, Object rootObject)
```

**关键实现机制**:

1. **缓存内省结果**:
```java
@Nullable
private CachedIntrospectionResults cachedIntrospectionResults;

private CachedIntrospectionResults getCachedIntrospectionResults() {
    if (this.cachedIntrospectionResults == null) {
        this.cachedIntrospectionResults = CachedIntrospectionResults.forClass(getWrappedClass());
    }
    return this.cachedIntrospectionResults;
}
```

2. **BeanPropertyHandler 内部类**:
- 继承 `PropertyHandler`
- 封装 `PropertyDescriptor` 的读取/写入方法调用
- 使用 `ReflectionUtils.makeAccessible()` 确保方法可访问

---

## 3. AbstractNestablePropertyAccessor 类

**位置**: `AbstractNestablePropertyAccessor.java`

这是所有属性访问器的基类，提供嵌套属性访问的基础设施。

### 3.1 核心属性

```java
private int autoGrowCollectionLimit = Integer.MAX_VALUE;  // 集合自动增长上限
@Nullable
Object wrappedObject;  // 被包装的对象
private String nestedPath = "";  // 嵌套路径
@Nullable
Object rootObject;  // 根对象
@Nullable
private Map<String, AbstractNestablePropertyAccessor> nestedPropertyAccessors;  // 嵌套访问器缓存
```

### 3.2 嵌套属性处理机制

**属性名解析** (`PropertyTokenHolder`):
```java
protected static class PropertyTokenHolder {
    public String actualName;      // 实际属性名
    public String canonicalName;   // 规范属性名
    @Nullable
    public String[] keys;          // 索引/键数组（用于集合/数组/Map）
}
```

**示例**:
- `person.name` → `actualName="person"`, 需要进一步解析嵌套
- `addresses[0]` → `actualName="addresses"`, `keys=["0"]`
- `map['key']` → `actualName="map"`, `keys=["key"]`

### 3.3 集合自动增长

当 `autoGrowNestedPaths = true` 时，访问器会自动创建和增长集合:

```java
// 数组自动增长
private Object growArrayIfNecessary(Object array, int index, String name) {
    if (index >= length && index < this.autoGrowCollectionLimit) {
        Object newArray = Array.newInstance(componentType, index + 1);
        System.arraycopy(propValue, 0, newArray, 0, length);
        // ...
    }
}

// 列表自动增长
private void growCollectionIfNecessary(Collection<Object> collection, int index, String name,
        PropertyHandler ph, int nestingLevel) {
    // 自动添加 null 元素直到达到目标索引
}
```

### 3.4 嵌套属性访问器获取

```java
protected AbstractNestablePropertyAccessor getPropertyAccessorForPropertyPath(String propertyPath) {
    int pos = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(propertyPath);
    if (pos > -1) {
        String nestedProperty = propertyPath.substring(0, pos);
        String nestedPath = propertyPath.substring(pos + 1);
        AbstractNestablePropertyAccessor nestedPa = getNestedPropertyAccessor(nestedProperty);
        return nestedPa.getPropertyAccessorForPropertyPath(nestedPath);
    }
    else {
        return this;
    }
}
```

---

## 4. DirectFieldAccessor 类

**位置**: `DirectFieldAccessor.java`

`DirectFieldAccessor` 是 `ConfigurablePropertyAccessor` 的实现，直接通过反射访问实例字段，而不是通过 JavaBean setter 方法。

**特点**:
- 直接绑定到字段，绕过 getter/setter 方法
- 自 Spring 4.2 起支持属性遍历、集合和 Map 访问
- 默认 `extractOldValueForEditor = true`（因为字段读取没有副作用）

**使用场景**:
```java
// 使用 PropertyAccessorFactory 获取 DirectFieldAccessor
DirectFieldAccessor accessor = new DirectFieldAccessor(myObject);
accessor.setPropertyValue("fieldName", value);

// 或在 DataBinder 中使用
DataBinder binder = new DataBinder(target);
binder.initDirectFieldAccess();  // 切换到字段访问模式
```

**FieldPropertyHandler 内部类**:
```java
private class FieldPropertyHandler extends PropertyHandler {
    private final Field field;
    private final ResolvableType resolvableType;

    @Override
    @Nullable
    public Object getValue() throws Exception {
        ReflectionUtils.makeAccessible(this.field);
        return this.field.get(getWrappedInstance());
    }

    @Override
    public void setValue(@Nullable Object value) throws Exception {
        ReflectionUtils.makeAccessible(this.field);
        this.field.set(getWrappedInstance(), value);
    }
}
```

---

## 5. 类型转换系统

### 5.1 TypeConverter 接口

**位置**: `TypeConverter.java`

定义类型转换方法的接口，通常与 `PropertyEditorRegistry` 一起实现。

**注意**: TypeConverter 实现**不是线程安全的**（因为基于 PropertyEditor）。

**核心方法**:

```java
// 基本类型转换
<T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType)
        throws TypeMismatchException;

// 带方法参数的转换（用于泛型类型分析）
<T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
        @Nullable MethodParameter methodParam) throws TypeMismatchException;

// 带字段的转换
<T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
        @Nullable Field field) throws TypeMismatchException;

// 带类型描述符的转换
<T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
        @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException;
```

### 5.2 SimpleTypeConverter 类

**位置**: `SimpleTypeConverter.java`

`TypeConverter` 的简单实现，不操作特定目标对象，用于任意类型转换需求。

```java
public class SimpleTypeConverter extends TypeConverterSupport {
    public SimpleTypeConverter() {
        this.typeConverterDelegate = new TypeConverterDelegate(this);
        registerDefaultEditors();  // 注册默认属性编辑器
    }
}
```

**使用示例**:
```java
SimpleTypeConverter converter = new SimpleTypeConverter();
Integer value = converter.convertIfNecessary("123", Integer.class);
```

---

## 6. 属性编辑器注册系统

### 6.1 PropertyEditorRegistry 接口

**位置**: `PropertyEditorRegistry.java`

封装注册 JavaBeans PropertyEditor 的方法，是 `PropertyEditorRegistrar` 操作的核心接口。

**核心方法**:

```java
// 为所有指定类型的属性注册自定义编辑器
void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor);

// 为指定类型和属性（或所有该类型的属性）注册编辑器
void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath,
        PropertyEditor propertyEditor);

// 查找自定义属性编辑器
@Nullable
PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath);
```

### 6.2 PropertyEditorRegistrySupport 类

**位置**: `PropertyEditorRegistrySupport.java`

`PropertyEditorRegistry` 的基类实现，管理默认编辑器和自定义编辑器。

**编辑器管理层次**:

```
PropertyEditorRegistrySupport
├── defaultEditors (Map<Class<?>, PropertyEditor>)
│   └── 延迟初始化，包含 Spring 提供的标准编辑器
├── overriddenDefaultEditors (Map<Class<?>, PropertyEditor>)
│   └── 覆盖的默认编辑器
├── customEditors (Map<Class<?>, PropertyEditor>)
│   └── 按类型注册的自定义编辑器
├── customEditorsForPath (Map<String, CustomEditorHolder>)
│   └── 按属性路径注册的自定义编辑器
└── customEditorCache (Map<Class<?>, PropertyEditor>)
    └── 缓存的超类/接口编辑器
```

**默认编辑器注册** (createDefaultEditors 方法):

```java
// 简单编辑器（JDK 不包含默认编辑器的类型）
defaultEditors.put(Charset.class, new CharsetEditor());
defaultEditors.put(Class.class, new ClassEditor());
defaultEditors.put(File.class, new FileEditor());
defaultEditors.put(Locale.class, new LocaleEditor());
defaultEditors.put(URI.class, new URIEditor());
defaultEditors.put(URL.class, new URLEditor());
defaultEditors.put(UUID.class, new UUIDEditor());
// ... 更多编辑器

// 集合编辑器
defaultEditors.put(Collection.class, new CustomCollectionEditor(Collection.class));
defaultEditors.put(Set.class, new CustomCollectionEditor(Set.class));
defaultEditors.put(List.class, new CustomCollectionEditor(List.class));

// 原始类型包装编辑器
defaultEditors.put(boolean.class, new CustomBooleanEditor(false));
defaultEditors.put(Boolean.class, new CustomBooleanEditor(true));
defaultEditors.put(int.class, new CustomNumberEditor(Integer.class, false));
defaultEditors.put(Integer.class, new CustomNumberEditor(Integer.class, true));
// ... 其他数字类型
```

**编辑器查找顺序**:
1. 属性路径特定的编辑器 (`customEditorsForPath`)
2. 类型特定的编辑器 (`customEditors`)
3. 默认编辑器 (`defaultEditors`)
4. 超类/接口编辑器的缓存 (`customEditorCache`)

---

## 7. 属性值管理

### 7.1 PropertyValue 类

**位置**: `PropertyValue.java`

用于保存单个 bean 属性的信息和值的对象。

**核心属性**:

```java
private final String name;           // 属性名
@Nullable
private final Object value;          // 属性值（原始值，未转换）
private boolean optional = false;    // 是否为可选值
private boolean converted = false;   // 是否已转换
@Nullable
private Object convertedValue;       // 转换后的值
@Nullable
volatile Boolean conversionNecessary;  // 是否需要转换
@Nullable
transient volatile Object resolvedTokens;  // 缓存的解析令牌
```

**特点**:
- 值不需要是最终所需类型，转换由 BeanWrapper 处理
- 支持合并（通过 `Mergeable` 接口）
- 支持可选值（找不到属性时忽略）

### 7.2 MutablePropertyValues 类

**位置**: `MutablePropertyValues.java`

`PropertyValues` 接口的默认实现，允许简单操作属性。

**核心特性**:

```java
public class MutablePropertyValues implements PropertyValues, Serializable {
    private final List<PropertyValue> propertyValueList;  // PropertyValue 列表
    @Nullable
    private Set<String> processedProperties;  // 已处理的属性标记
    private volatile boolean converted;       // 是否所有值都已转换
}
```

**主要操作**:

```java
// 构造器
public MutablePropertyValues()  // 空构造器
public MutablePropertyValues(@Nullable PropertyValues original)  // 深拷贝
public MutablePropertyValues(@Nullable Map<?, ?> original)  // 从 Map 构造

// 添加属性值
public MutablePropertyValues add(String propertyName, @Nullable Object propertyValue)
public MutablePropertyValues addPropertyValue(PropertyValue pv)
public MutablePropertyValues addPropertyValues(@Nullable PropertyValues other)
public MutablePropertyValues addPropertyValues(@Nullable Map<?, ?> other)

// 查询
@Nullable
public PropertyValue getPropertyValue(String propertyName)
public boolean contains(String propertyName)
public boolean isEmpty()

// 比较
public PropertyValues changesSince(PropertyValues old)
```

**合并支持** (mergeIfRequired):
```java
private PropertyValue mergeIfRequired(PropertyValue newPv, PropertyValue currentPv) {
    Object value = newPv.getValue();
    if (value instanceof Mergeable mergeable) {
        if (mergeable.isMergeEnabled()) {
            Object merged = mergeable.merge(currentPv.getValue());
            return new PropertyValue(newPv.getName(), merged);
        }
    }
    return newPv;
}
```

---

## 8. CachedIntrospectionResults 类

**位置**: `CachedIntrospectionResults.java`

内部类，缓存 Java 类的 `PropertyDescriptor` 信息，避免重复内省带来的性能开销。

**缓存策略**:

```java
// 强引用缓存 - 用于 cache-safe 的 bean 类
static final ConcurrentMap<Class<?>, CachedIntrospectionResults> strongClassCache =
        new ConcurrentHashMap<>(64);

// 软引用缓存 - 用于非 cache-safe 的 bean 类
static final ConcurrentMap<Class<?>, CachedIntrospectionResults> softClassCache =
        new ConcurrentReferenceHashMap<>(64);
```

**工厂方法**:
```java
static CachedIntrospectionResults forClass(Class<?> beanClass) throws BeansException {
    // 1. 先查强引用缓存
    CachedIntrospectionResults results = strongClassCache.get(beanClass);
    if (results != null) return results;

    // 2. 再查软引用缓存
    results = softClassCache.get(beanClass);
    if (results != null) return results;

    // 3. 创建新实例
    results = new CachedIntrospectionResults(beanClass);

    // 4. 选择合适的缓存
    if (ClassUtils.isCacheSafe(beanClass, CachedIntrospectionResults.class.getClassLoader())
            || isClassLoaderAccepted(beanClass.getClassLoader())) {
        classCacheToUse = strongClassCache;
    } else {
        classCacheToUse = softClassCache;
    }

    // 5. 放入缓存
    CachedIntrospectionResults existing = classCacheToUse.putIfAbsent(beanClass, results);
    return (existing != null ? existing : results);
}
```

**内省处理**:
1. 使用 `BeanInfoFactory` 获取 `BeanInfo`（支持通过 spring.factories 扩展）
2. 处理基本属性描述符
3. 检查实现的接口（包括 Java 8 默认方法）
4. 检查 Record 风格的访问器（Java 15+）

---

## 9. BeanUtils 工具类

**位置**: `BeanUtils.java`

JavaBeans 的静态便利方法，用于实例化 bean、检查 bean 属性类型、复制 bean 属性等。

### 9.1 实例化方法

```java
// 使用默认构造函数实例化
public static <T> T instantiateClass(Class<T> clazz) throws BeanInstantiationException

// 使用指定构造函数实例化
public static <T> T instantiateClass(Constructor<T> ctor, Object... args)
        throws BeanInstantiationException

// 查找主构造函数（支持 Kotlin 和 Java Records）
@Nullable
public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz)

// 获取可解析的构造函数
public static <T> Constructor<T> getResolvableConstructor(Class<T> clazz)
```

**Kotlin 支持**:
```java
// 检测 Kotlin 类型
if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(clazz)) {
    return KotlinDelegate.findPrimaryConstructor(clazz);
}

// Kotlin 委托内部类
private static class KotlinDelegate {
    public static <T> T instantiateClass(Constructor<T> ctor, Object... args)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        KFunction<T> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(ctor);
        if (kotlinConstructor == null) {
            return ctor.newInstance(args);
        }
        // 处理 Kotlin 默认参数
        // ...
        return kotlinConstructor.callBy(argParameters);
    }
}
```

### 9.2 属性复制

```java
// 基本复制
public static void copyProperties(Object source, Object target) throws BeansException

// 限制可编辑类
public static void copyProperties(Object source, Object target, Class<?> editable)
        throws BeansException

// 忽略指定属性
public static void copyProperties(Object source, Object target, String... ignoreProperties)
        throws BeansException
```

**泛型类型感知**:
- 自 Spring Framework 5.3 起，支持泛型类型信息
- 例如：`List<Integer>` 可以复制到 `List<? extends Number>`，但不能复制到 `List<Long>`

### 9.3 方法查找

```java
// 查找方法
@Nullable
public static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes)

// 查找最小参数的方法
@Nullable
public static Method findMethodWithMinimalParameters(Class<?> clazz, String methodName)

// 解析方法签名
@Nullable
public static Method resolveSignature(String signature, Class<?> clazz)
```

### 9.4 PropertyDescriptor 操作

```java
// 获取所有 PropertyDescriptor
public static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz)

// 获取指定属性的 PropertyDescriptor
@Nullable
public static PropertyDescriptor getPropertyDescriptor(Class<?> clazz, String propertyName)

// 为方法查找对应的 PropertyDescriptor
@Nullable
public static PropertyDescriptor findPropertyForMethod(Method method)
```

### 9.5 PropertyEditor 查找

```java
// 按约定查找 PropertyEditor（类名 + "Editor" 后缀）
@Nullable
public static PropertyEditor findEditorByConvention(@Nullable Class<?> targetType)

// 示例: mypackage.MyDomainClass → mypackage.MyDomainClassEditor
```

---

## 10. 异常类体系

```
BeansException (Spring 核心异常)
├── FatalBeanException
│   ├── BeanInstantiationException      // Bean 实例化失败
│   └── CannotLoadBeanClassException    // 无法加载 Bean 类
├── TypeMismatchException               // 类型不匹配
├── ConversionNotSupportedException     // 不支持转换
├── InvalidPropertyException            // 无效属性
│   ├── NotReadablePropertyException    // 属性不可读
│   └── NotWritablePropertyException    // 属性不可写
├── MethodInvocationException           // 方法调用异常
├── PropertyAccessException             // 属性访问异常
│   └── PropertyBatchUpdateException    // 批量更新异常
└── NullValueInNestedPathException      // 嵌套路径空值
```

---

## 11. 使用示例

### 11.1 基本 BeanWrapper 使用

```java
// 创建 BeanWrapper
Person person = new Person();
BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(person);

// 设置简单属性
wrapper.setPropertyValue("name", "John");
wrapper.setPropertyValue("age", 30);

// 设置嵌套属性
wrapper.setPropertyValue("address.city", "New York");

// 获取属性值
String name = (String) wrapper.getPropertyValue("name");
```

### 11.2 使用 DirectFieldAccessor

```java
Person person = new Person();
DirectFieldAccessor accessor = new DirectFieldAccessor(person);

// 直接设置字段（绕过 setter）
accessor.setPropertyValue("privateField", "value");
```

### 11.3 自定义属性编辑器

```java
// 注册自定义编辑器
BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(myBean);
wrapper.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), false));

// 现在字符串可以自动转换为 Date
wrapper.setPropertyValue("birthDate", "1990-01-01");
```

### 11.4 使用 MutablePropertyValues

```java
MutablePropertyValues pvs = new MutablePropertyValues();
pvs.add("name", "John")
   .add("age", 30)
   .add("address.city", "New York");

BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(new Person());
wrapper.setPropertyValues(pvs);
```

---

## 12. 总结

`org.springframework.beans` 包提供了 Spring 框架中操作 JavaBeans 的基础基础设施：

1. **BeanWrapper** - 封装了 JavaBean 的操作，支持嵌套属性和类型转换
2. **PropertyAccessor** 层次结构 - 提供灵活的属性访问机制
3. **类型转换系统** - 集成 PropertyEditor 和 ConversionService
4. **缓存机制** - CachedIntrospectionResults 避免重复内省的性能开销
5. **工具类** - BeanUtils 提供便捷的静态方法

这个包是整个 Spring Beans 模块的基石，上层的 `BeanFactory` 和依赖注入功能都建立在这些基础之上。
