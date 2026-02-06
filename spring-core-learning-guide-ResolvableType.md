# Spring ResolvableType 深度解析指南

## 一、ResolvableType 简介

`ResolvableType` 是 Spring 4.0 引入的泛型解析工具类，用于解决 Java 泛型擦除问题，让 Spring 能够在运行时获取完整的泛型信息。

### 1.1 为什么需要 ResolvableType？

Java 泛型在编译后会被擦除（Type Erasure）：

```java
private List<String> myList;
// 编译后 Field.getType() 只能返回 List.class，无法获取 String 泛型参数
```

**ResolvableType 的作用**：封装 `java.lang.reflect.Type`，提供对泛型的完整访问能力。

### 1.2 核心结构

```
ResolvableType
├── type: Type                    # 底层的 Java Type
├── resolved: Class<?>            # 解析后的 Class
├── componentType: ResolvableType # 数组的元素类型
├── typeProvider: TypeProvider    # 类型提供者
└── variableResolver: VariableResolver  # 类型变量解析器
```

---

## 二、创建 ResolvableType 的 8 种方式

### 2.1 从 Class 创建
```java
// 最基础的方式
ResolvableType type1 = ResolvableType.forClass(ArrayList.class);

// 带预声明泛型的 Class
ResolvableType type2 = ResolvableType.forClassWithGenerics(
    Map.class, String.class, Integer.class
);
```

### 2.2 从字段创建（最常用）
```java
Field field = MyClass.class.getDeclaredField("myList");
ResolvableType type = ResolvableType.forField(field);

// 带实现类的字段解析（用于解析父类中的泛型字段）
ResolvableType type2 = ResolvableType.forField(field, ImplementationClass.class);
```

### 2.3 从方法创建
```java
Method method = MyClass.class.getMethod("process", List.class);

// 方法参数
ResolvableType paramType = ResolvableType.forMethodParameter(method, 0);

// 方法返回类型
ResolvableType returnType = ResolvableType.forMethodReturnType(method);

// 带实现类的方法参数
ResolvableType paramType2 = ResolvableType.forMethodParameter(method, 0, ImplClass.class);
```

### 2.4 从构造器创建
```java
Constructor<?> ctor = MyClass.class.getConstructor(List.class);
ResolvableType type = ResolvableType.forConstructorParameter(ctor, 0);
```

### 2.5 从实例创建
```java
ResolvableType type = ResolvableType.forInstance(new MyClass());

// 如果对象实现了 ResolvableTypeProvider，会获取更精确的类型
```

### 2.6 从底层 Type 创建
```java
Type genericType = field.getGenericType();
ResolvableType type = ResolvableType.forType(genericType);
```

---

## 三、核心方法详解

### 3.1 类型解析方法

| 方法 | 返回值 | 说明 | 示例 |
|-----|--------|------|------|
| `resolve()` | `Class<?>` | 解析为 Class，失败返回 null | `List<String>` → `List.class` |
| `resolve(Class<?> fallback)` | `Class<?>` | 解析失败返回 fallback | `resolve(Object.class)` |
| `toClass()` | `Class<?>` | 解析失败返回 Object.class | - |
| `getRawClass()` | `Class<?>` | 获取原始 Class（不带泛型）| - |
| `getType()` | `Type` | 获取底层 Java Type | - |

```java
ResolvableType type = ResolvableType.forField(
    getClass().getDeclaredField("stringList")
);
// private List<String> stringList;

type.resolve();           // List.class
type.toClass();           // List.class
type.getRawClass();       // List.class
type.getType();           // java.util.List<java.lang.String>
```

### 3.2 泛型操作方法

| 方法 | 返回值 | 说明 |
|-----|--------|------|
| `getGenerics()` | `ResolvableType[]` | 获取所有泛型参数 |
| `getGeneric(int... indexes)` | `ResolvableType` | 获取指定位置的泛型 |
| `hasGenerics()` | `boolean` | 是否有泛型参数 |
| `getGenericCount()` | `int` | 泛型参数数量 |
| `resolveGeneric(int... indexes)` | `Class<?>` | 直接解析泛型为 Class |
| `resolveGenerics()` | `Class<?>[]` | 解析所有泛型为 Class |

```java
// 假设有：private Map<String, List<Integer>> complexMap;

ResolvableType type = ResolvableType.forField(
    getClass().getDeclaredField("complexMap")
);

// 获取泛型
type.getGenerics();                    // [String, List<Integer>]
type.getGeneric(0);                    // String
type.getGeneric(1);                    // List<Integer>
type.getGeneric(1, 0);                 // Integer (嵌套获取)

// 直接解析
type.resolveGeneric(0);                // String.class
type.resolveGeneric(1, 0);             // Integer.class
```

### 3.3 嵌套类型导航

| 方法 | 返回值 | 说明 |
|-----|--------|------|
| `getNested(int nestingLevel)` | `ResolvableType` | 获取指定嵌套层级 |
| `getSuperType()` | `ResolvableType` | 获取父类型 |
| `getInterfaces()` | `ResolvableType[]` | 获取实现的接口 |
| `isArray()` | `boolean` | 是否是数组 |
| `getComponentType()` | `ResolvableType` | 获取数组元素类型 |

```java
// List<List<String>>[] myArray;

ResolvableType type = ResolvableType.forField(
    getClass().getDeclaredField("myArray")
);

type.isArray();                        // true
type.getComponentType();               // List<List<String>>

// 嵌套层级：1=当前, 2=第一层泛型, 3=第二层泛型
type.getNested(1);                     // List<List<String>>[]
type.getNested(2);                     // List<List<String>>
type.getNested(3);                     // List<String>
type.getNested(4);                     // String
```

### 3.4 类型转换方法

| 方法 | 返回值 | 说明 |
|-----|--------|------|
| `as(Class<?> type)` | `ResolvableType` | 转为指定类型视角 |
| `asCollection()` | `ResolvableType` | 转为 Collection 视角 |
| `asMap()` | `ResolvableType` | 转为 Map 视角 |
| `asArray()` | `ResolvableType` | 转为数组视角 |

```java
// class MyList extends ArrayList<String> {}

ResolvableType type = ResolvableType.forClass(MyList.class);

type.as(List.class);       // List<String>
type.as(Collection.class); // Collection<String>
type.as(ArrayList.class);  // ArrayList<String>

// 获取 String
Class<?> generic = type.as(List.class).getGeneric(0).resolve();
```

### 3.5 类型检查方法

| 方法 | 返回值 | 说明 |
|-----|--------|------|
| `isAssignableFrom(Class<?> other)` | `boolean` | 是否可赋值 |
| `isAssignableFrom(ResolvableType other)` | `boolean` | 是否可赋值 |
| `isInstance(Object obj)` | `boolean` | 对象是否是该类型实例 |
| `isArray()` | `boolean` | 是否是数组 |
| `isEnum()` | `boolean` | 是否是枚举 |
| `isInterface()` | `boolean` | 是否是接口 |

```java
ResolvableType listType = ResolvableType.forClass(List.class);
ResolvableType arrayListType = ResolvableType.forClass(ArrayList.class);

// 类型检查
listType.isAssignableFrom(arrayListType);  // true
arrayListType.isAssignableFrom(listType);  // false

// 实例检查
arrayListType.isInstance(new ArrayList<>()); // true
arrayListType.isInstance(new LinkedList<>()); // false
```

---

## 四、高级用法

### 4.1 处理类型变量（泛型参数）

```java
// class Service<T> {
//     private List<T> items;
// }
// class UserService extends Service<User> {}

// 解析 T 的具体类型
ResolvableType serviceType = ResolvableType.forClass(UserService.class);
ResolvableType baseType = serviceType.as(Service.class);

// 获取 Service<T> 中的 T
ResolvableType tType = baseType.getGeneric(0);  // User
```

### 4.2 处理通配符

```java
// private List<? extends Number> numbers;
// private List<? super Integer> intSuper;

ResolvableType numbers = ResolvableType.forField(
    getClass().getDeclaredField("numbers")
);
ResolvableType intSuper = ResolvableType.forField(
    getClass().getDeclaredField("intSuper")
);

numbers.getGeneric(0).resolve();      // Number.class
numbers.getGeneric(0).getType();      // ? extends java.lang.Number

intSuper.getGeneric(0).resolve();     // Integer.class
intSuper.getGeneric(0).getType();     // ? super java.lang.Integer
```

### 4.3 带实现类的字段解析（关键！）

```java
// 父类
class BaseService<T> {
    private T entity;
}

// 子类
class UserService extends BaseService<User> {}

// 解析子类中的 entity 字段
Field field = BaseService.class.getDeclaredField("entity");

// 不带实现类 - 只能知道是 T
ResolvableType type1 = ResolvableType.forField(field);
type1.resolve();  // null (不知道 T 是什么)

// 带实现类 - 能解析出 User
ResolvableType type2 = ResolvableType.forField(field, UserService.class);
type2.resolve();  // User.class
```

---

## 五、Spring 内部使用场景

### 5.1 依赖注入确定泛型类型

```java
@Autowired
private List<Validator<User>> validators;

// Spring 内部使用 ResolvableType：
ResolvableType type = ResolvableType.forField(field);
Class<?> generic = type.getGeneric(0).resolve();      // Validator
Class<?> nested = type.getGeneric(0).getGeneric(0).resolve();  // User
```

### 5.2 类型转换

```java
// Converter 接口
public interface Converter<S, T> {
    T convert(S source);
}

// Spring 查找 Converter 时匹配泛型
ResolvableType type = ResolvableType.forClass(converter.getClass());
ResolvableType[] generics = type.as(Converter.class).getGenerics();
// generics[0] = S, generics[1] = T
```

### 5.3 处理 @EventListener

```java
@EventListener
public void onEvent(UserCreatedEvent event) { }

// Spring 解析方法参数类型
ResolvableType type = ResolvableType.forMethodParameter(method, 0);
Class<?> eventType = type.resolve();  // UserCreatedEvent
```

---

## 六、实用代码片段

### 6.1 获取方法参数的完整泛型信息

```java
public static void printMethodGenerics(Method method) {
    for (int i = 0; i < method.getParameterCount(); i++) {
        ResolvableType paramType = ResolvableType.forMethodParameter(method, i);
        System.out.println("参数 " + i + ": " + paramType);

        // 打印所有泛型
        ResolvableType[] generics = paramType.getGenerics();
        for (int j = 0; j < generics.length; j++) {
            System.out.println("  泛型 " + j + ": " + generics[j]);
        }
    }
}
```

### 6.2 判断是否是某个泛型接口的实现

```java
public boolean isImplementationOf(Class<?> clazz, Class<?> interfaceClass, Class<?> genericType) {
    ResolvableType type = ResolvableType.forClass(clazz);
    ResolvableType asInterface = type.as(interfaceClass);

    if (asInterface == ResolvableType.NONE) {
        return false;
    }

    Class<?> actualGeneric = asInterface.getGeneric(0).resolve();
    return actualGeneric != null && actualGeneric.equals(genericType);
}

// 使用
isImplementationOf(UserService.class, Service.class, User.class);
```

### 6.3 安全的泛型解析

```java
public static <T> Class<T> resolveGenericType(
        ResolvableType type,
        int... indexes) {
    Class<?> resolved = type.resolveGeneric(indexes);
    if (resolved == null) {
        throw new IllegalStateException(
            "无法解析泛型: " + Arrays.toString(indexes));
    }
    @SuppressWarnings("unchecked")
    Class<T> result = (Class<T>) resolved;
    return result;
}
```

---

## 七、常见问题 FAQ

### Q1: resolve() 返回 null 怎么办？

```java
// 使用带默认值的版本
Class<?> clazz = type.resolve(Object.class);
```

### Q2: 如何解析多层嵌套？

```java
// Map<String, List<Integer>>
type.getGeneric(1).getGeneric(0).resolve();  // Integer
// 或
type.resolveGeneric(1, 0);  // Integer（推荐）
```

### Q3: 如何处理未知类型 T？

```java
// 需要传入实现类
ResolvableType.forField(field, implementationClass);
```

### Q4: ResolvableType.NONE 是什么？

```java
// 表示空类型，用于避免 null 检查
ResolvableType type = ResolvableType.NONE;
type.resolve();  // null
type.getGenerics();  // 空数组
```

---

## 八、完整示例类

```java
public class ResolvableTypeDemo {

    // 各种泛型字段
    private List<String> simpleList;
    private Map<String, Integer> simpleMap;
    private Map<String, List<Integer>> nestedMap;
    private List<String>[] arrayOfList;
    private List<List<String>> listOfList;

    // 泛型方法
    public <T> T process(List<T> items) {
        return null;
    }

    public void demonstrate() throws Exception {
        // 1. 简单泛型
        ResolvableType listType = ResolvableType.forField(
            getClass().getDeclaredField("simpleList")
        );
        System.out.println(listType.resolve());                    // List
        System.out.println(listType.getGeneric(0).resolve());      // String

        // 2. Map 泛型
        ResolvableType mapType = ResolvableType.forField(
            getClass().getDeclaredField("simpleMap")
        );
        System.out.println(mapType.asMap().getGeneric(0).resolve());  // String
        System.out.println(mapType.asMap().getGeneric(1).resolve());  // Integer

        // 3. 嵌套泛型
        ResolvableType nestedType = ResolvableType.forField(
            getClass().getDeclaredField("nestedMap")
        );
        System.out.println(nestedType.resolveGeneric(1, 0));  // Integer

        // 4. 方法参数
        Method method = getClass().getMethod("process", List.class);
        ResolvableType paramType = ResolvableType.forMethodParameter(method, 0);
        System.out.println(paramType.getGeneric(0));  // T（变量）
    }
}
```

---

## 九、学习建议

1. **从简单开始**：先掌握 `forField()` 和 `resolve()`
2. **理解类型擦除**：明确 Java 泛型在运行时的表现
3. **实践嵌套解析**：多练习 `getGeneric()` 和 `resolveGeneric()`
4. **阅读 Spring 源码**：看 Spring 如何在实际中使用 ResolvableType

---

## 十、相关类

- `MethodParameter`：封装方法参数信息
- `ParameterizedTypeReference`：用于保留泛型信息的类型引用
- `TypeDescriptor`：Spring 类型描述（基于 ResolvableType）
