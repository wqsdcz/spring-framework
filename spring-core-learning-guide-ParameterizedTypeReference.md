# ParameterizedTypeReference 深度解析

## 一、什么是 ParameterizedTypeReference？

`ParameterizedTypeReference` 是 Spring 提供的一个**抽象类**，用于在运行时**保留泛型类型信息**。

### 1.1 核心定义

```java
public abstract class ParameterizedTypeReference<T> {
    private final Type type;

    protected ParameterizedTypeReference() {
        // 通过反射获取子类的泛型父类类型
        Type type = getClass().getGenericSuperclass();
        ParameterizedType parameterizedType = (ParameterizedType) type;
        this.type = parameterizedType.getActualTypeArguments()[0];
    }

    public Type getType() {
        return this.type;
    }
}
```

### 1.2 一句话理解

通过创建**匿名子类**的方式，让 Java 编译器将泛型信息保留在字节码的 `Signature` 属性中，从而在运行时通过反射获取。

---

## 二、为什么需要它？

### 2.1 Java 泛型擦除问题

Java 的泛型在编译时会被**擦除**（Type Erasure）：

```java
// 编译前
List<String> list = new ArrayList<>();

// 编译后（字节码层面）
List list = new ArrayList();  // String 被擦除了
```

### 2.2 无法直接获取泛型参数

```java
// 错误！Java 不支持这种语法
Type type = List<String>.class;  // 编译错误！

//  Field.getGenericType() 可以获取，但仅限于类成员
public class MyClass {
    private List<String> myList;  // 字段有泛型信息
}

// 但方法局部变量或返回类型无法直接传递泛型信息
public List<String> getList() {
    return new ArrayList<>();
}
// 调用者无法知道返回的是 List<String> 还是 List<Integer>
```

### 2.3 解决方案对比

| 方案 | 优点 | 缺点 |
|-----|------|------|
| 传递 `Class<T>` | 简单 | 无法表示泛型（如 `List<String>`）|
| 传递 `Type` | 可以表示泛型 | 构造复杂，易出错 |
| **ParameterizedTypeReference** | 简洁、类型安全 | 需要创建匿名类 |

---

## 三、核心原理：Super Type Token 模式

### 3.1 模式由来

这个技巧最早由 Google 的 **Neal Gafter** 在 2006 年提出，称为 **"Super Type Token"**（超级类型令牌）。

**核心思想**：
- 创建抽象类的**匿名子类**
- 子类在字节码中会保留父类的泛型参数信息
- 通过反射获取这些信息

### 3.2 原理解析

#### 步骤 1：创建匿名子类

```java
// 创建 ParameterizedTypeReference 的匿名子类
new ParameterizedTypeReference<List<String>>() {}
```

编译器生成的字节码类似于：

```java
class MyClass$1 extends ParameterizedTypeReference<List<String>> {
    MyClass$1() {}
}
```

#### 步骤 2：获取泛型父类

```java
// 在构造函数中
Type type = getClass().getGenericSuperclass();
// 返回：ParameterizedTypeReference<List<String>>
```

#### 步骤 3：提取类型参数

```java
ParameterizedType parameterizedType = (ParameterizedType) type;
Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
// actualTypeArguments[0] = List<String>
```

### 3.3 为什么必须是抽象类？

**强制用户创建子类**：

```java
// 正确：创建匿名子类，保留泛型信息
new ParameterizedTypeReference<List<String>>() {}

// 错误：直接 new 会报错
new ParameterizedTypeReference<List<String>>();  // 编译错误！
```

抽象类的构造函数受保护，强制子类化，确保泛型信息被保留。

---

## 四、源码深度解析

### 4.1 构造函数

```java
protected ParameterizedTypeReference() {
    // 1. 找到直接的 ParameterizedTypeReference 子类
    Class<?> parameterizedTypeReferenceSubclass = findParameterizedTypeReferenceSubclass(getClass());

    // 2. 获取子类的泛型父类类型
    Type type = parameterizedTypeReferenceSubclass.getGenericSuperclass();

    // 3. 必须是 ParameterizedType（即带泛型的父类）
    Assert.isInstanceOf(ParameterizedType.class, type, "Type must be a parameterized type");
    ParameterizedType parameterizedType = (ParameterizedType) type;

    // 4. 获取实际类型参数
    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
    Assert.isTrue(actualTypeArguments.length == 1, "Number of type arguments must be 1");

    // 5. 保存类型
    this.type = actualTypeArguments[0];
}
```

### 4.2 查找子类方法

```java
private static Class<?> findParameterizedTypeReferenceSubclass(Class<?> child) {
    Class<?> parent = child.getSuperclass();

    if (Object.class == parent) {
        // 找不到 ParameterizedTypeReference 父类，报错
        throw new IllegalStateException("Expected ParameterizedTypeReference superclass");
    }
    else if (ParameterizedTypeReference.class == parent) {
        // 找到了直接的子类
        return child;
    }
    else {
        // 继续向上查找（处理多级继承）
        return findParameterizedTypeReferenceSubclass(parent);
    }
}
```

**处理多级继承**：

```java
// 自定义子类
class MyTypeRef<T> extends ParameterizedTypeReference<T> {}

// 使用时
new MyTypeRef<List<String>>() {}

// findParameterizedTypeReferenceSubclass 会找到 MyTypeRef，
// 然后获取 MyTypeRef<List<String>> 的泛型信息
```

### 4.3 forType 工厂方法

```java
public static <T> ParameterizedTypeReference<T> forType(Type type) {
    return new ParameterizedTypeReference<>(type) {};
}

// 私有构造函数
private ParameterizedTypeReference(Type type) {
    this.type = type;
}
```

**用途**：从已有的 `Type` 对象创建引用（比如从方法返回类型获取）。

---

## 五、使用方法

### 5.1 基本用法

```java
// 1. 创建匿名子类
ParameterizedTypeReference<List<String>> typeRef =
    new ParameterizedTypeReference<List<String>>() {};

// 2. 获取 Type
Type type = typeRef.getType();
// type = java.util.List<java.lang.String>

// 3. 可以转换为 ParameterizedType
if (type instanceof ParameterizedType) {
    ParameterizedType pt = (ParameterizedType) type;
    Type rawType = pt.getRawType();           // List
    Type[] args = pt.getActualTypeArguments(); // [String]
}
```

### 5.2 嵌套泛型

```java
// Map<String, List<Integer>>
ParameterizedTypeReference<Map<String, List<Integer>>> typeRef =
    new ParameterizedTypeReference<Map<String, List<Integer>>>() {};

Type type = typeRef.getType();
// type = java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>
```

### 5.3 使用 forType 工厂方法

```java
// 从方法返回类型创建
Method method = MyClass.class.getMethod("getList");
Type returnType = method.getGenericReturnType();

ParameterizedTypeReference<?> typeRef = ParameterizedTypeReference.forType(returnType);
```

---

## 六、Spring 中的实际应用

### 6.1 RestTemplate 交换方法

**最常见的使用场景**：

```java
@RestController
public class UserController {
    @GetMapping("/users")
    public List<User> getUsers() {
        return userService.findAll();
    }
}

// 客户端调用
RestTemplate restTemplate = new RestTemplate();

// 问题：如何告诉 RestTemplate 返回的是 List<User> 而不是 List？
// 解决：使用 ParameterizedTypeReference

ResponseEntity<List<User>> response = restTemplate.exchange(
    "http://api.example.com/users",
    HttpMethod.GET,
    null,
    new ParameterizedTypeReference<List<User>>() {}  // 关键！
);

List<User> users = response.getBody();
```

**为什么不能用 Class？**

```java
// 错误！List.class 无法携带泛型信息
restTemplate.exchange(url, HttpMethod.GET, null, List.class);
// 返回的是 List，但元素类型未知

// 正确！ParameterizedTypeReference 携带完整的泛型信息
new ParameterizedTypeReference<List<User>>() {}
```

### 6.2 WebClient（WebFlux）

```java
WebClient webClient = WebClient.create();

Mono<List<User>> result = webClient.get()
    .uri("/users")
    .retrieve()
    .bodyToMono(new ParameterizedTypeReference<List<User>>() {});
```

### 6.3 与 ResolvableType 结合

```java
ParameterizedTypeReference<List<String>> typeRef =
    new ParameterizedTypeReference<List<String>>() {};

// 转换为 ResolvableType
ResolvableType resolvableType = ResolvableType.forType(typeRef.getType());

// 然后可以使用 ResolvableType 的丰富功能
Class<?> resolved = resolvableType.resolve();  // List
ResolvableType generic = resolvableType.getGeneric(0);  // String
```

---

## 七、完整实战示例

### 7.1 自定义泛型响应处理

```java
public class GenericResponse<T> {
    private int code;
    private String message;
    private T data;

    // getters and setters
}

// 使用 ParameterizedTypeReference 处理
public class ApiClient {
    private RestTemplate restTemplate = new RestTemplate();

    public <T> T get(String url, ParameterizedTypeReference<GenericResponse<T>> typeRef) {
        ResponseEntity<GenericResponse<T>> response =
            restTemplate.exchange(url, HttpMethod.GET, null, typeRef);
        return response.getBody().getData();
    }
}

// 调用
ApiClient client = new ApiClient();
List<User> users = client.get(
    "http://api.example.com/users",
    new ParameterizedTypeReference<GenericResponse<List<User>>>() {}
);
```

### 7.2 泛型工具类

```java
public class TypeRefUtil {

    /**
     * 获取参数化类型的原始类
     */
    public static Class<?> getRawType(ParameterizedTypeReference<?> typeRef) {
        Type type = typeRef.getType();
        if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            return (Class<?>) rawType;
        }
        return (Class<?>) type;
    }

    /**
     * 获取第 n 个泛型参数
     */
    public static Type getGeneric(ParameterizedTypeReference<?> typeRef, int index) {
        Type type = typeRef.getType();
        if (type instanceof ParameterizedType) {
            Type[] args = ((ParameterizedType) type).getActualTypeArguments();
            return index < args.length ? args[index] : null;
        }
        return null;
    }

    public static void main(String[] args) {
        ParameterizedTypeReference<Map<String, List<Integer>>> typeRef =
            new ParameterizedTypeReference<Map<String, List<Integer>>>() {};

        System.out.println("Raw type: " + getRawType(typeRef));  // interface java.util.Map
        System.out.println("Generic 0: " + getGeneric(typeRef, 0));  // class java.lang.String
        System.out.println("Generic 1: " + getGeneric(typeRef, 1));  // java.util.List<java.lang.Integer>
    }
}
```

---

## 八、与 ResolvableType 的区别

| 特性 | ParameterizedTypeReference | ResolvableType |
|-----|---------------------------|----------------|
| **设计目的** | 传递泛型类型信息 | 解析和操作泛型类型 |
| **使用方式** | 创建匿名子类 | 调用静态工厂方法 |
| **数据来源** | 编译时确定的类型 | 运行时反射获取 |
| **功能丰富度** | 仅封装 Type | 提供丰富操作方法 |
| **典型场景** | RestTemplate、WebClient | Bean 定义、依赖注入 |
| **可变性** | 不可变（final） | 可变（可嵌套解析）|

### 8.1 如何选择？

**使用 ParameterizedTypeReference 当**：
- 需要传递泛型类型信息（如 HTTP 请求）
- 类型在编译时已知
- 需要类型安全的方式

**使用 ResolvableType 当**：
- 需要解析字段、方法参数的泛型
- 需要操作嵌套泛型（getGeneric, resolve 等）
- 需要处理类型变量（T）

### 8.2 两者结合

```java
// 1. 使用 ParameterizedTypeReference 捕获类型
ParameterizedTypeReference<List<String>> typeRef =
    new ParameterizedTypeReference<List<String>>() {};

// 2. 转换为 ResolvableType 进行操作
ResolvableType resolvableType = ResolvableType.forType(typeRef.getType());

// 3. 使用 ResolvableType 的功能
resolvableType.getGeneric(0).resolve();  // String.class
```

---

## 九、常见问题

### Q1: 为什么要用 `{}`？

```java
// 正确 - 创建匿名子类
new ParameterizedTypeReference<List<String>>() {}

// 错误 - 直接 new 抽象类
new ParameterizedTypeReference<List<String>>();  // 编译错误！
```

`{}` 创建匿名子类，让编译器在字节码中保留 `List<String>` 信息。

### Q2: 可以传递变量类型吗？

```java
// 错误！T 是运行时擦除的
public <T> void process(T item) {
    new ParameterizedTypeReference<T>() {};  // T 被擦除为 Object！
}

// 正确 - 通过方法参数传递 TypeReference
public <T> void process(ParameterizedTypeReference<T> typeRef) {
    Type type = typeRef.getType();  // 可以获取具体类型
}
```

### Q3: 与 Java 的 ParameterizedType 关系？

```java
// ParameterizedTypeReference 内部封装了 ParameterizedType
Type type = typeRef.getType();

if (type instanceof ParameterizedType) {
    ParameterizedType pt = (ParameterizedType) type;
    // 这是 Java 标准接口
}
```

### Q4: 如何处理通配符？

```java
// 支持通配符
ParameterizedTypeReference<List<? extends Number>> typeRef =
    new ParameterizedTypeReference<List<? extends Number>>() {};

Type type = typeRef.getType();
// type = java.util.List<? extends java.lang.Number>
```

---

## 十、最佳实践

### 10.1 预定义常用类型

```java
public final class TypeRefs {
    public static final ParameterizedTypeReference<List<String>> STRING_LIST =
        new ParameterizedTypeReference<List<String>>() {};

    public static final ParameterizedTypeReference<Map<String, Object>> STRING_OBJECT_MAP =
        new ParameterizedTypeReference<Map<String, Object>>() {};

    private TypeRefs() {}  // 工具类
}

// 使用
restTemplate.exchange(url, HttpMethod.GET, null, TypeRefs.STRING_LIST);
```

### 10.2 封装 HTTP 客户端

```java
public class GenericHttpClient {
    private final RestTemplate restTemplate;

    public <T> T get(String url, ParameterizedTypeReference<T> typeRef) {
        return restTemplate.exchange(url, HttpMethod.GET, null, typeRef).getBody();
    }

    public <T> List<T> getList(String url, Class<T> elementType) {
        // 动态构建 ParameterizedTypeReference
        ResolvableType resolvableType = ResolvableType.forClassWithGenerics(
            List.class, elementType);

        ParameterizedTypeReference<List<T>> typeRef =
            ParameterizedTypeReference.forType(resolvableType.getType());

        return get(url, typeRef);
    }
}
```

### 10.3 注意事项

1. **必须创建匿名子类**，否则泛型信息丢失
2. **类型必须是确定的**，不能是运行时变量
3. **可以嵌套使用**，支持任意层级的泛型
4. **是线程安全的**，可以定义为静态常量

---

## 十一、本课小结

### 核心要点

1. **ParameterizedTypeReference 是"超级类型令牌"模式的实现**，通过匿名子类保留泛型信息

2. **核心原理**：
   - 创建匿名子类 → 编译器保留泛型参数到字节码
   - 反射获取 `getGenericSuperclass()` → 得到 `ParameterizedType`
   - 提取 `actualTypeArguments` → 获取具体类型

3. **主要用途**：
   - `RestTemplate.exchange()` 传递响应类型
   - `WebClient.bodyToMono()` 指定返回类型
   - 任何需要在运行时传递泛型信息的场景

4. **与 ResolvableType 的关系**：
   - ParameterizedTypeReference：静态捕获类型
   - ResolvableType：动态解析类型
   - 两者可以相互转换

### 代码模板

```java
// 基本用法
new ParameterizedTypeReference<List<String>>() {}

// 嵌套泛型
new ParameterizedTypeReference<Map<String, List<Integer>>>() {}

// 与 ResolvableType 结合
ResolvableType rt = ResolvableType.forType(typeRef.getType());
```

---

## 参考资料

- [Neal Gafter's Blog: Super Type Tokens](https://gafter.blogspot.nl/2006/12/super-type-tokens.html)
- [Spring Framework Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/)
- Java Reflection Tutorial: [ParameterizedType](https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/ParameterizedType.html)
