# org.springframework.beans.support 包详解

## 概述

`org.springframework.beans.support` 包提供了对 Spring Beans 模块的支持类，主要包括分页、排序和资源编辑器注册等实用工具。这些类主要用于 Web UI 场景，支持数据绑定和会话管理。

---

## 一、分页功能

### 1.1 PagedListHolder 类

**文件位置**: `spring-beans/src/main/java/org/springframework/beans/support/PagedListHolder.java`

`PagedListHolder` 是一个简单的状态持有者，用于处理对象列表的分页。页码从 0 开始计数。

#### 核心属性

```java
public class PagedListHolder<E> implements Serializable {
    // 默认每页大小为 10
    public static final int DEFAULT_PAGE_SIZE = 10;
    // 默认最大链接页数为 10
    public static final int DEFAULT_MAX_LINKED_PAGES = 10;

    private List<E> source = Collections.emptyList();  // 源数据列表
    @Nullable
    private Date refreshDate;                          // 刷新时间
    @Nullable
    private SortDefinition sort;                       // 排序定义
    @Nullable
    private SortDefinition sortUsed;                   // 上次使用的排序定义
    private int pageSize = DEFAULT_PAGE_SIZE;          // 每页大小
    private int page = 0;                              // 当前页码
    private boolean newPageSet;                        // 是否设置了新页码
    private int maxLinkedPages = DEFAULT_MAX_LINKED_PAGES; // 最大链接页数
}
```

#### 分页机制详解

**1. 页数计算**

```java
public int getPageCount() {
    float nrOfPages = (float) getNrOfElements() / getPageSize();
    return (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages);
}
```

计算方法：
- 将元素总数除以每页大小，得到浮点数页数
- 如果有余数（即页数不是整数）或页数为 0，则向上取整（加 1）
- 否则直接取整

**2. 当前页数据范围计算**

```java
// 获取当前页第一个元素的索引
public int getFirstElementOnPage() {
    return (getPageSize() * getPage());
}

// 获取当前页最后一个元素的索引
public int getLastElementOnPage() {
    int endIndex = getPageSize() * (getPage() + 1);
    int size = getNrOfElements();
    return (endIndex > size ? size : endIndex) - 1;
}
```

**3. 获取当前页数据列表**

```java
public List<E> getPageList() {
    return getSource().subList(getFirstElementOnPage(), getLastElementOnPage() + 1);
}
```

使用 `List.subList()` 方法从源列表中提取当前页的数据，这是一个视图操作，不会创建新的列表。

**4. 页面链接范围计算**

```java
// 获取要显示的第一个页码链接
public int getFirstLinkedPage() {
    return Math.max(0, getPage() - (getMaxLinkedPages() / 2));
}

// 获取要显示的最后一个页码链接
public int getLastLinkedPage() {
    return Math.min(getFirstLinkedPage() + getMaxLinkedPages() - 1, getPageCount() - 1);
}
```

这种计算方式确保页码链接以当前页为中心，最多显示 `maxLinkedPages` 个链接。

#### 排序集成

```java
public void resort() {
    SortDefinition sort = getSort();
    // 只有当排序定义发生变化时才重新排序
    if (sort != null && !sort.equals(this.sortUsed)) {
        this.sortUsed = copySortDefinition(sort);
        doSort(getSource(), sort);
        setPage(0);  // 排序后重置到第一页
    }
}

protected void doSort(List<E> source, SortDefinition sort) {
    PropertyComparator.sort(source, sort);
}
```

#### 使用示例

```java
// 创建测试数据
List<TestBean> users = new ArrayList<>();
users.add(new TestBean("eva", 25));
users.add(new TestBean("juergen", 99));
users.add(new TestBean("Rod", 32));

// 创建分页持有者
PagedListHolder<TestBean> holder = new PagedListHolder<>(users);

// 设置每页大小
holder.setPageSize(2);

// 获取分页信息
System.out.println("总页数: " + holder.getPageCount());  // 2
System.out.println("当前页: " + holder.getPage());      // 0
System.out.println("总元素数: " + holder.getNrOfElements());  // 3

// 获取当前页数据
List<TestBean> currentPage = holder.getPageList();
System.out.println("当前页大小: " + currentPage.size());  // 2

// 切换到下一页
holder.nextPage();
System.out.println("当前页: " + holder.getPage());  // 1
System.out.println("当前页大小: " + holder.getPageList().size());  // 1

// 检查是否是首尾页
System.out.println("是否首页: " + holder.isFirstPage());  // false
System.out.println("是否末页: " + holder.isLastPage());   // true

// 排序
MutableSortDefinition sort = new MutableSortDefinition("name", true, true);
holder.setSort(sort);
holder.resort();  // 按名称排序

// 获取页码链接范围（用于 UI 分页导航）
System.out.println("第一个链接页: " + holder.getFirstLinkedPage());
System.out.println("最后一个链接页: " + holder.getLastLinkedPage());
```

---

## 二、排序功能

### 2.1 SortDefinition 接口

**文件位置**: `spring-beans/src/main/java/org/springframework/beans/support/SortDefinition.java`

排序定义接口，定义了排序所需的基本属性。

```java
public interface SortDefinition {
    /**
     * 返回要比较的 Bean 属性名
     * 可以是嵌套的属性路径
     */
    String getProperty();

    /**
     * 返回是否忽略字符串值的大小写
     */
    boolean isIgnoreCase();

    /**
     * 返回是否升序排序（true）或降序排序（false）
     */
    boolean isAscending();
}
```

### 2.2 MutableSortDefinition 类

**文件位置**: `spring-beans/src/main/java/org/springframework/beans/support/MutableSortDefinition.java`

`SortDefinition` 的可变实现，支持在设置相同属性时切换升序/降序。

#### 核心属性

```java
public class MutableSortDefinition implements SortDefinition, Serializable {
    private String property = "";           // 排序属性名
    private boolean ignoreCase = true;      // 是否忽略大小写
    private boolean ascending = true;       // 是否升序
    private boolean toggleAscendingOnProperty = false;  // 是否自动切换排序方向
}
```

#### 自动切换排序方向机制

```java
public void setProperty(String property) {
    if (!StringUtils.hasLength(property)) {
        this.property = "";
    }
    else {
        // 如果启用了 toggleAscendingOnProperty
        // 当设置相同属性时，自动切换升序/降序
        if (isToggleAscendingOnProperty()) {
            this.ascending = (!property.equals(this.property) || !this.ascending);
        }
        this.property = property;
    }
}
```

**逻辑解析**：
- 如果属性名不同，保持当前排序方向
- 如果属性名相同且当前是升序，则变为降序
- 如果属性名相同且当前是降序，则变为升序

#### 使用示例

```java
// 创建排序定义（不启用自动切换）
MutableSortDefinition sort1 = new MutableSortDefinition("name", true, true);

// 创建排序定义（启用自动切换）
MutableSortDefinition sort2 = new MutableSortDefinition(true);
sort2.setProperty("name");

// 第一次设置属性
sort2.setProperty("name");  // ascending = false（切换为降序）
// 第二次设置相同属性
sort2.setProperty("name");  // ascending = true（切换回升序）

// 设置不同属性
sort2.setProperty("age");   // ascending = true（保持升序，因为属性不同）
```

### 2.3 PropertyComparator 类

**文件位置**: `spring-beans/src/main/java/org/springframework/beans/support/PropertyComparator.java`

基于 Bean 属性的比较器，使用 `BeanWrapper` 访问属性值。

#### 核心实现

```java
public class PropertyComparator<T> implements Comparator<T> {
    protected final Log logger = LogFactory.getLog(getClass());
    private final SortDefinition sortDefinition;

    @Override
    @SuppressWarnings("unchecked")
    public int compare(T o1, T o2) {
        // 获取两个对象的属性值
        Object v1 = getPropertyValue(o1);
        Object v2 = getPropertyValue(o2);

        // 如果忽略大小写且值为字符串，转换为小写
        if (this.sortDefinition.isIgnoreCase() && (v1 instanceof String text1) && (v2 instanceof String text2)) {
            v1 = text1.toLowerCase(Locale.ROOT);
            v2 = text2.toLowerCase(Locale.ROOT);
        }

        int result;
        try {
            if (v1 != null) {
                // v1 不为空时，与 v2 比较
                result = (v2 != null ? ((Comparable<Object>) v1).compareTo(v2) : -1);
            }
            else {
                // v1 为空时，如果 v2 也不为空，则 v1 排在后面
                result = (v2 != null ? 1 : 0);
            }
        }
        catch (RuntimeException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not sort objects [" + o1 + "] and [" + o2 + "]", ex);
            }
            return 0;
        }

        // 根据升序/降序返回结果
        return (this.sortDefinition.isAscending() ? result : -result);
    }
}
```

#### 空值处理策略

```java
// 空值排在最后
if (v1 != null) {
    result = (v2 != null ? ((Comparable<Object>) v1).compareTo(v2) : -1);
    // v1 不为空，v2 为空：v1 排在前面（result = -1）
}
else {
    result = (v2 != null ? 1 : 0);
    // v1 为空，v2 不为空：v1 排在后面（result = 1）
    // 两者都为空：相等（result = 0）
}
```

#### 属性值获取

```java
@Nullable
private Object getPropertyValue(Object obj) {
    try {
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(false);
        beanWrapper.setWrappedInstance(obj);
        return beanWrapper.getPropertyValue(this.sortDefinition.getProperty());
    }
    catch (BeansException ex) {
        logger.debug("PropertyComparator could not access property - treating as null for sorting", ex);
        return null;
    }
}
```

使用 `BeanWrapperImpl` 访问对象属性，支持嵌套属性路径（如 `address.city`）。

#### 静态排序方法

```java
// 对 List 进行排序
public static void sort(List<?> source, SortDefinition sortDefinition) throws BeansException {
    if (StringUtils.hasText(sortDefinition.getProperty())) {
        source.sort(new PropertyComparator<>(sortDefinition));
    }
}

// 对数组进行排序
public static void sort(Object[] source, SortDefinition sortDefinition) throws BeansException {
    if (StringUtils.hasText(sortDefinition.getProperty())) {
        Arrays.sort(source, new PropertyComparator<>(sortDefinition));
    }
}
```

#### 使用示例

```java
// 定义实体类
public class User {
    private String name;
    private int age;
    private String email;

    // getters and setters
}

// 创建测试数据
List<User> users = Arrays.asList(
    new User("Charlie", 30, "charlie@example.com"),
    new User("alice", 25, "alice@example.com"),
    new User("Bob", 35, "bob@example.com"),
    new User(null, 28, "null@example.com")  // 空名称
);

// 方式1：使用 SortDefinition 排序
SortDefinition sortDef = new MutableSortDefinition("name", true, true);
PropertyComparator.sort(users, sortDef);
// 结果: null, alice, Bob, Charlie（忽略大小写升序）

// 方式2：直接创建 PropertyComparator
PropertyComparator<User> comparator = new PropertyComparator<>("age", false, false);
users.sort(comparator);
// 结果: Bob(35), Charlie(30), null(28), alice(25)（降序）

// 方式3：链式比较器
Comparator<User> chainedComparator = new PropertyComparator<User>("name", true, true)
    .thenComparing(new PropertyComparator<>("age", false, true));
users.sort(chainedComparator);
```

---

## 三、资源编辑器注册

### 3.1 ResourceEditorRegistrar 类

**文件位置**: `spring-beans/src/main/java/org/springframework/beans/support/ResourceEditorRegistrar.java`

`PropertyEditorRegistrar` 的实现，用于向 `PropertyEditorRegistry` 注册资源相关的属性编辑器。

#### 核心属性

```java
public class ResourceEditorRegistrar implements PropertyEditorRegistrar {
    private final PropertyResolver propertyResolver;  // 属性解析器（通常是 Environment）
    private final ResourceLoader resourceLoader;      // 资源加载器（通常是 ApplicationContext）
}
```

#### 注册的编辑器类型

```java
@Override
public void registerCustomEditors(PropertyEditorRegistry registry) {
    // 基础资源编辑器
    ResourceEditor baseEditor = new ResourceEditor(this.resourceLoader, this.propertyResolver);

    // 注册各种资源类型的编辑器
    doRegisterEditor(registry, Resource.class, baseEditor);
    doRegisterEditor(registry, ContextResource.class, baseEditor);
    doRegisterEditor(registry, WritableResource.class, baseEditor);
    doRegisterEditor(registry, InputStream.class, new InputStreamEditor(baseEditor));
    doRegisterEditor(registry, InputSource.class, new InputSourceEditor(baseEditor));
    doRegisterEditor(registry, File.class, new FileEditor(baseEditor));
    doRegisterEditor(registry, Path.class, new PathEditor(baseEditor));
    doRegisterEditor(registry, Reader.class, new ReaderEditor(baseEditor));
    doRegisterEditor(registry, URL.class, new URLEditor(baseEditor));

    ClassLoader classLoader = this.resourceLoader.getClassLoader();
    doRegisterEditor(registry, URI.class, new URIEditor(classLoader));
    doRegisterEditor(registry, Class.class, new ClassEditor(classLoader));
    doRegisterEditor(registry, Class[].class, new ClassArrayEditor(classLoader));

    // 如果支持资源模式解析，注册数组资源编辑器
    if (this.resourceLoader instanceof ResourcePatternResolver resourcePatternResolver) {
        doRegisterEditor(registry, Resource[].class,
                new ResourceArrayPropertyEditor(resourcePatternResolver, this.propertyResolver));
    }
}
```

#### 编辑器注册方式

```java
private void doRegisterEditor(PropertyEditorRegistry registry, Class<?> requiredType, PropertyEditor editor) {
    if (registry instanceof PropertyEditorRegistrySupport registrySupport) {
        // 优先覆盖默认编辑器
        registrySupport.overrideDefaultEditor(requiredType, editor);
    }
    else {
        // 否则注册为自定义编辑器
        registry.registerCustomEditor(requiredType, editor);
    }
}
```

#### 支持的资源类型

| 类型 | 编辑器 | 说明 |
|------|--------|------|
| `Resource` | `ResourceEditor` | Spring 资源抽象 |
| `ContextResource` | `ResourceEditor` | 上下文相关资源 |
| `WritableResource` | `ResourceEditor` | 可写资源 |
| `InputStream` | `InputStreamEditor` | 输入流 |
| `InputSource` | `InputSourceEditor` | SAX 输入源 |
| `File` | `FileEditor` | 文件 |
| `Path` | `PathEditor` | NIO 路径 |
| `Reader` | `ReaderEditor` | 字符读取器 |
| `URL` | `URLEditor` | URL |
| `URI` | `URIEditor` | URI |
| `Class` | `ClassEditor` | Java 类 |
| `Class[]` | `ClassArrayEditor` | 类数组 |
| `Resource[]` | `ResourceArrayPropertyEditor` | 资源数组（模式匹配） |

#### 使用场景

```java
// 在 Spring 配置中，可以直接使用字符串配置资源类型属性
@Configuration
public class AppConfig {

    @Value("classpath:config.properties")
    private Resource configFile;

    @Value("classpath*:/*.xml")
    private Resource[] xmlFiles;

    @Value("file:/tmp/data.txt")
    private File dataFile;

    @Value("https://example.com/data.json")
    private URL remoteUrl;
}

// XML 配置示例
<bean id="myBean" class="com.example.MyBean">
    <property name="configFile" value="classpath:config.properties"/>
    <property name="dataFile" value="file:/tmp/data.txt"/>
</bean>
```

---

## 四、方法调用支持

### 4.1 ArgumentConvertingMethodInvoker 类

**文件位置**: `spring-beans/src/main/java/org/springframework/beans/support/ArgumentConvertingMethodInvoker.java`

`MethodInvoker` 的子类，支持通过 `TypeConverter` 转换参数以匹配目标方法。

#### 核心功能

```java
public class ArgumentConvertingMethodInvoker extends MethodInvoker {
    @Nullable
    private TypeConverter typeConverter;
    private boolean useDefaultConverter = true;
}
```

#### 方法查找与参数转换

```java
@Nullable
protected Method doFindMatchingMethod(Object[] arguments) {
    TypeConverter converter = getTypeConverter();
    if (converter != null) {
        String targetMethod = getTargetMethod();
        Method matchingMethod = null;
        int argCount = arguments.length;
        Class<?> targetClass = getTargetClass();
        Method[] candidates = ReflectionUtils.getAllDeclaredMethods(targetClass);
        int minTypeDiffWeight = Integer.MAX_VALUE;
        Object[] argumentsToUse = null;

        for (Method candidate : candidates) {
            if (candidate.getName().equals(targetMethod)) {
                int parameterCount = candidate.getParameterCount();
                if (parameterCount == argCount) {
                    Class<?>[] paramTypes = candidate.getParameterTypes();
                    Object[] convertedArguments = new Object[argCount];
                    boolean match = true;

                    // 尝试转换每个参数
                    for (int j = 0; j < argCount && match; j++) {
                        try {
                            convertedArguments[j] = converter.convertIfNecessary(arguments[j], paramTypes[j]);
                        }
                        catch (TypeMismatchException ex) {
                            match = false;  // 转换失败，不匹配
                        }
                    }

                    if (match) {
                        // 计算类型差异权重，选择最匹配的方法
                        int typeDiffWeight = getTypeDifferenceWeight(paramTypes, convertedArguments);
                        if (typeDiffWeight < minTypeDiffWeight) {
                            minTypeDiffWeight = typeDiffWeight;
                            matchingMethod = candidate;
                            argumentsToUse = convertedArguments;
                        }
                    }
                }
            }
        }

        if (matchingMethod != null) {
            setArguments(argumentsToUse);
            return matchingMethod;
        }
    }
    return null;
}
```

#### 使用示例

```java
public class UserService {
    public void createUser(String name, int age) {
        // ...
    }

    public void createUser(String name, Integer age, String email) {
        // ...
    }
}

// 使用 ArgumentConvertingMethodInvoker
ArgumentConvertingMethodInvoker invoker = new ArgumentConvertingMethodInvoker();
invoker.setTargetClass(UserService.class);
invoker.setTargetMethod("createUser");
invoker.setArguments("John", "30");  // 字符串 "30" 会自动转换为 int

// 查找匹配的方法并调用
Method method = invoker.findMatchingMethod();
invoker.invoke();
```

---

## 五、综合使用示例

### 5.1 Web 分页与排序

```java
@Controller
public class UserController {

    @GetMapping("/users")
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(defaultValue = "true") boolean ascending,
            Model model,
            HttpSession session) {

        // 从数据库获取所有用户
        List<User> allUsers = userService.findAll();

        // 创建分页持有者
        PagedListHolder<User> pagedListHolder = new PagedListHolder<>(allUsers);
        pagedListHolder.setPageSize(pageSize);
        pagedListHolder.setPage(page);

        // 设置排序
        if (sortProperty != null) {
            MutableSortDefinition sort = new MutableSortDefinition(true);
            sort.setProperty(sortProperty);
            sort.setAscending(ascending);
            pagedListHolder.setSort(sort);
            pagedListHolder.resort();
        }

        // 添加到模型
        model.addAttribute("pagedList", pagedListHolder);
        model.addAttribute("users", pagedListHolder.getPageList());
        model.addAttribute("pageCount", pagedListHolder.getPageCount());
        model.addAttribute("currentPage", pagedListHolder.getPage());
        model.addAttribute("firstLinkedPage", pagedListHolder.getFirstLinkedPage());
        model.addAttribute("lastLinkedPage", pagedListHolder.getLastLinkedPage());

        return "users/list";
    }
}
```

### 5.2 Thymeleaf 分页视图

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><title>用户列表</title></head>
<body>
    <table>
        <thead>
            <tr>
                <th><a th:href="@{/users(sortProperty='name', ascending=${!sort.ascending})}">名称</a></th>
                <th><a th:href="@{/users(sortProperty='age', ascending=${!sort.ascending})}">年龄</a></th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="user : ${users}">
                <td th:text="${user.name}"></td>
                <td th:text="${user.age}"></td>
            </tr>
        </tbody>
    </table>

    <!-- 分页导航 -->
    <div class="pagination">
        <a th:if="${!pagedList.firstPage}" th:href="@{/users(page=${pagedList.page - 1})}">上一页</a>

        <span th:each="i : ${#numbers.sequence(firstLinkedPage, lastLinkedPage)}">
            <a th:if="${i != pagedList.page}" th:href="@{/users(page=${i})}" th:text="${i + 1}"></a>
            <span th:if="${i == pagedList.page}" th:text="${i + 1}" class="current"></span>
        </span>

        <a th:if="${!pagedList.lastPage}" th:href="@{/users(page=${pagedList.page + 1})}">下一页</a>
    </div>
</body>
</html>
```

### 5.3 服务层排序工具

```java
@Service
public class UserService {

    public List<User> findAllSorted(String property, boolean ignoreCase, boolean ascending) {
        List<User> users = userRepository.findAll();

        SortDefinition sortDef = new MutableSortDefinition(property, ignoreCase, ascending);
        PropertyComparator.sort(users, sortDef);

        return users;
    }

    public List<User> findAllSortedMultiple(SortDefinition... sortDefinitions) {
        List<User> users = userRepository.findAll();

        if (sortDefinitions.length > 0) {
            Comparator<User> comparator = new PropertyComparator<>(sortDefinitions[0]);
            for (int i = 1; i < sortDefinitions.length; i++) {
                comparator = comparator.thenComparing(new PropertyComparator<>(sortDefinitions[i]));
            }
            users.sort(comparator);
        }

        return users;
    }
}
```

---

## 六、总结

### 6.1 类关系图

```
SortDefinition (interface)
    |
    +-- MutableSortDefinition (实现类)

PropertyComparator<T> implements Comparator<T>
    - 使用 SortDefinition
    - 使用 BeanWrapperImpl 访问属性

PagedListHolder<E> implements Serializable
    - 使用 SortDefinition
    - 使用 PropertyComparator 排序

ResourceEditorRegistrar implements PropertyEditorRegistrar
    - 使用 ResourceLoader
    - 使用 PropertyResolver

ArgumentConvertingMethodInvoker extends MethodInvoker
    - 使用 TypeConverter
```

### 6.2 使用场景总结

| 类 | 主要用途 | 典型场景 |
|----|---------|---------|
| `PagedListHolder` | 内存分页 | Web 列表展示、数据分页处理 |
| `SortDefinition` | 定义排序规则 | 动态排序配置 |
| `MutableSortDefinition` | 可变的排序定义 | 用户交互式排序（点击表头切换） |
| `PropertyComparator` | 基于属性的比较 | 列表排序、多字段排序 |
| `ResourceEditorRegistrar` | 注册资源编辑器 | Spring 配置中的资源类型转换 |
| `ArgumentConvertingMethodInvoker` | 带转换的方法调用 | 动态方法调用、参数类型转换 |

### 6.3 注意事项

1. **PagedListHolder** 适用于内存中的列表分页，不适合大数据量场景
2. **PropertyComparator** 要求被比较的对象实现 `Comparable` 接口，或属性值本身可比较
3. **MutableSortDefinition** 的 `toggleAscendingOnProperty` 功能适合 Web 场景中的表头点击排序
4. 所有排序操作都会修改原始列表的顺序，如需保留原顺序请先复制列表
