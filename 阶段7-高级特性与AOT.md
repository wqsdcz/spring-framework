# 阶段7：高级特性与AOT - 详细学习指导

## 学习目标

1. 理解循环依赖的三级缓存机制
2. 掌握PropertyEditors类型转换
3. 了解BeanWrapper属性访问
4. 理解AOT编译支持

预计学习时间：4-5天

---

## Day 1: 循环依赖深度解析

### 什么是循环依赖

```java
@Component
public class A {
    @Autowired
    private B b;  // A依赖B
}

@Component
public class B {
    @Autowired
    private A a;  // B依赖A
}
```

### Spring解决循环依赖的方式

Spring通过**三级缓存**解决**单例setter注入**的循环依赖。

**注意**：
- ✅ 单例 + setter注入：可以解决
- ❌ 单例 + 构造器注入：无法解决
- ❌ Prototype：无法解决

### 三级缓存源码

```java
public class DefaultSingletonBeanRegistry {
    // 一级缓存：成品单例
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

    // 二级缓存：早期单例（未填充属性）
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

    // 三级缓存：单例工厂
    private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

    // 正在创建的bean（用于检测循环依赖）
    private final Set<String> singletonsCurrentlyInCreation = Collections.newSetFromMap(new ConcurrentHashMap<>(16));
}
```

### 三级缓存工作流程

```
创建A
    ↓
标记A为正在创建（singletonsCurrentlyInCreation.add("a")）
    ↓
实例化A（调用构造器）→ 得到原始对象A@1234
    ↓
将A的ObjectFactory放入三级缓存
    singletonFactories.put("a", () -> getEarlyBeanReference("a", mbd, A@1234))
    ↓
填充A的属性（需要B）
    ↓
    创建B
        ↓
    标记B为正在创建
        ↓
    实例化B → 得到原始对象B@5678
        ↓
    将B的ObjectFactory放入三级缓存
        ↓
    填充B的属性（需要A）
        ↓
        从一级缓存获取A → null
        从二级缓存获取A → null
        从三级缓存获取A的ObjectFactory → 存在！
        ↓
        调用ObjectFactory.getObject()
        创建A的早期引用 → A@1234（或代理对象）
        将A放入二级缓存，从三级缓存移除
        ↓
    B继续初始化 → B完成
    将B放入一级缓存，从二级缓存移除
    ↓
A继续初始化
    ↓
A完成
将A放入一级缓存，从二级缓存移除
```

### getSingleton核心代码

```java
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    // 1. 从一级缓存获取
    Object singletonObject = this.singletonObjects.get(beanName);

    // 2. 一级缓存不存在，且正在创建中
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        // 从二级缓存获取
        singletonObject = this.earlySingletonObjects.get(beanName);

        // 3. 二级缓存不存在，且允许早期引用
        if (singletonObject == null && allowEarlyReference) {
            // 从三级缓存获取工厂
            ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
            if (singletonFactory != null) {
                // 创建早期引用
                singletonObject = singletonFactory.getObject();
                // 放入二级缓存
                this.earlySingletonObjects.put(beanName, singletonObject);
                // 从三级缓存移除
                this.singletonFactories.remove(beanName);
            }
        }
    }
    return singletonObject;
}
```

### 为什么要三级缓存？

| 级别 | 作用 | 为什么需要 |
|------|------|------------|
| singletonObjects | 存放完整bean | 保证获取的bean是完整的 |
| earlySingletonObjects | 存放早期bean | 解决循环依赖，避免重复创建 |
| singletonFactories | 延迟创建代理 | 需要时才创建代理对象 |

**如果只使用二级缓存**：
- 正常bean和早期bean混在一起
- 无法区分bean是否初始化完成
- 代理对象的创建时机难以控制

### 构造器循环依赖为什么无法解决？

```java
@Component
public class A {
    private final B b;

    @Autowired
    public A(B b) {  // 构造器需要B
        this.b = b;
    }
}

@Component
public class B {
    private final A a;

    @Autowired
    public B(A a) {  // 构造器需要A
        this.a = a;
    }
}
```

**原因**：
1. 创建A需要先调用构造器
2. 调用构造器需要先创建B
3. 创建B需要先调用构造器
4. 调用构造器需要先创建A
5. 死锁！

**解决方案**：
1. 改用setter注入
2. 使用`@Lazy`延迟注入

```java
@Component
public class A {
    private final B b;

    @Autowired
    public A(@Lazy B b) {  // 使用@Lazy
        this.b = b;  // 注入的是代理对象，延迟到使用时才创建
    }
}
```

### 今日实践任务

1. 创建循环依赖的测试用例
2. 在getSingleton方法打断点，观察三级缓存的变化
3. 测试构造器循环依赖，观察报错信息

---

## Day 2: PropertyEditors类型转换

### 什么是PropertyEditor

JavaBeans规范定义的类型转换接口，Spring用它将字符串转换为对象类型。

```java
public interface PropertyEditor {
    void setValue(Object value);
    Object getValue();
    String getAsText();
    void setAsText(String text) throws IllegalArgumentException;
}
```

### Spring内置的PropertyEditors

| 类 | 转换目标类型 | 说明 |
|-----|-------------|------|
| ClassEditor | Class | 类名字符串转Class |
| CustomBooleanEditor | Boolean | 支持true/false/on/off等 |
| CustomNumberEditor | Number | 字符串转数字 |
| CustomDateEditor | Date | 字符串转日期 |
| CustomCollectionEditor | Collection | 字符串转集合 |
| CustomMapEditor | Map | 字符串转Map |
| FileEditor | File | 字符串转File |
| PathEditor | Path | 字符串转Path |
| URLEditor | URL | 字符串转URL |
| URLEditor | URI | 字符串转URI |
| PropertiesEditor | Properties | 字符串转Properties |
| CharsetEditor | Charset | 字符串转字符集 |
| LocaleEditor | Locale | 字符串转区域设置 |
| TimeZoneEditor | TimeZone | 字符串转时区 |
| ZoneIdEditor | ZoneId | 字符串转ZoneId |

### 使用PropertyEditor

```java
// XML配置
<bean id="userService" class="com.example.UserService">
    <property name="timeout" value="5000"/>  <!-- String -> int -->
    <property name="startDate" value="2024-01-01"/>  <!-- String -> Date -->
    <property name="active" value="true"/>  <!-- String -> boolean -->
</bean>
```

### 注册自定义PropertyEditor

```java
public class MyCustomType {
    private String value;
    // ...
}

public class MyCustomTypeEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        MyCustomType obj = new MyCustomType();
        obj.setValue(text);
        setValue(obj);
    }
}
```

### 注册方式

```java
// 方式1：实现PropertyEditorRegistrar
public class CustomPropertyEditorRegistrar implements PropertyEditorRegistrar {
    @Override
    public void registerCustomEditors(PropertyEditorRegistry registry) {
        registry.registerCustomEditor(MyCustomType.class, new MyCustomTypeEditor());
    }
}

// 在XML中注册
<bean class="org.springframework.beans.factory.config.CustomEditorConfigurer">
    <property name="propertyEditorRegistrars">
        <list>
            <bean class="com.example.CustomPropertyEditorRegistrar"/>
        </list>
    </property>
</bean>

// 方式2：直接注册
<bean class="org.springframework.beans.factory.config.CustomEditorConfigurer">
    <property name="customEditors">
        <map>
            <entry key="com.example.MyCustomType"
                   value="com.example.MyCustomTypeEditor"/>
        </map>
    </property>
</bean>
```

### PropertyEditor vs Converter

| 特性 | PropertyEditor | Converter |
|------|----------------|-----------|
| 来源 | JavaBeans | Spring 3.0 |
| 推荐度 | 旧（不推荐） | 新（推荐） |
| 功能 | 双向转换 | 单向转换 |
| 线程安全 | 否（有状态） | 是（无状态） |
| 使用场景 | 遗留代码 | 新代码 |

**现代Spring推荐使用Converter**：
```java
@Component
public class StringToMyCustomTypeConverter implements Converter<String, MyCustomType> {
    @Override
    public MyCustomType convert(String source) {
        MyCustomType obj = new MyCustomType();
        obj.setValue(source);
        return obj;
    }
}
```

### 今日实践任务

1. 了解Spring内置的PropertyEditors
2. 创建自定义PropertyEditor
3. 对比PropertyEditor和Converter

---

## Day 3: BeanWrapper属性访问

### BeanWrapper接口

```java
public interface BeanWrapper extends ConfigurablePropertyAccessor {
    void setWrappedInstance(Object obj);
    Object getWrappedInstance();
    Class<?> getWrappedClass();
    PropertyDescriptor[] getPropertyDescriptors();
    PropertyDescriptor getPropertyDescriptor(String propertyName);
    void setPropertyValue(String propertyName, Object value);
    Object getPropertyValue(String propertyName);
}
```

**作用**：
- 包装JavaBean对象
- 提供统一的方式访问属性
- 支持嵌套属性（如`user.address.city`）

### 使用BeanWrapper

```java
public class User {
    private String name;
    private int age;
    private Address address;
    // getters and setters
}

public class Address {
    private String city;
    // getters and setters
}

// 使用BeanWrapper
public class BeanWrapperDemo {
    public static void main(String[] args) {
        User user = new User();
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(user);

        // 设置简单属性
        wrapper.setPropertyValue("name", "John");
        wrapper.setPropertyValue("age", 30);

        // 设置嵌套属性（自动创建中间对象）
        wrapper.setPropertyValue("address.city", "Beijing");

        // 获取属性
        String name = (String) wrapper.getPropertyValue("name");
        String city = (String) wrapper.getPropertyValue("address.city");

        System.out.println(user.getName());  // John
        System.out.println(user.getAddress().getCity());  // Beijing
    }
}
```

### PropertyValues批量设置

```java
MutablePropertyValues pvs = new MutablePropertyValues();
pvs.add("name", "John");
pvs.add("age", 30);
pvs.add("address.city", "Beijing");

BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(user);
wrapper.setPropertyValues(pvs);
```

### 类型转换

```java
BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(user);

// 自动类型转换
wrapper.setPropertyValue("age", "30");  // String -> int
wrapper.setPropertyValue("active", "true");  // String -> boolean
```

### BeanWrapper在Spring中的应用

```java
// 1. 属性填充时使用（AbstractAutowireCapableBeanFactory）
protected void populateBean(String beanName, RootBeanDefinition mbd, BeanWrapper bw) {
    PropertyValues pvs = mbd.getPropertyValues();
    // ...
    applyPropertyValues(beanName, mbd, bw, pvs);
}

// 2. 数据绑定（Web MVC）
WebDataBinder binder = new WebDataBinder(target);
binder.registerCustomEditor(Date.class, new CustomDateEditor());
binder.bind(request);
```

### DirectFieldAccessor直接字段访问

```java
// 绕过getter/setter，直接访问字段
User user = new User();
DirectFieldAccessor accessor = new DirectFieldAccessor(user);
accessor.setPropertyValue("name", "John");  // 直接设置字段值
```

### 今日实践任务

1. 使用BeanWrapper操作JavaBean
2. 测试嵌套属性设置
3. 了解BeanWrapper在Spring内部的使用

---

## Day 4: AOT（Ahead-of-Time）编译支持

### 什么是AOT

AOT（Ahead-of-Time）编译是在**运行时之前**将代码编译为本地机器码的技术。

**Spring AOT的目的**：
- 支持GraalVM原生镜像
- 减少运行时反射
- 加快启动速度
- 降低内存占用

### GraalVM原生镜像

```
传统JVM应用：
Java源码 → 编译 → 字节码 → JVM解释执行/JIT编译

GraalVM原生镜像：
Java源码 → 编译 → 字节码 → AOT编译 → 本地可执行文件
```

**优势**：
- 启动速度快（毫秒级）
- 内存占用低
- 不依赖JVM
- 适合容器和Serverless

**限制**：
- 反射需要显式配置
- 动态代理需要显式配置
- 资源加载需要显式配置
- JNI需要显式配置

### Spring的AOT支持

```java
// AOT处理器接口
public interface BeanRegistrationAotProcessor {
    @Nullable
    BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean);
}

// 生成bean注册代码
public interface BeanRegistrationAotContribution {
    void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode);
}
```

### AOT处理流程

```
1. 分析应用上下文
    ↓
2. 识别需要反射的类/方法/字段
    ↓
3. 识别动态代理
    ↓
4. 识别资源
    ↓
5. 生成：
   - Java代码（替代反射）
   - 反射配置文件（reflect-config.json）
   - 代理配置文件（proxy-config.json）
   - 资源配置文件（resource-config.json）
    ↓
6. 编译为原生镜像
```

### RuntimeHints

```java
// 声明反射需求
RuntimeHints hints = new RuntimeHints();

// 注册类反射
hints.reflection().registerType(UserService.class,
    MemberCategory.INVOKE_PUBLIC_METHODS);

// 注册资源
hints.resources().registerPattern("application.properties");

// 注册代理
hints.proxies().registerJdkProxy(MyInterface.class);

// 注册序列化
hints.serialization().registerType(User.class);
```

### 自动生成RuntimeHints

Spring通过`RuntimeHintsAgent`在测试时自动检测：

```java
@Test
@EnabledIfRuntimeHintsAgent  // 需要添加-javaagent参数
void shouldRegisterHints() {
    // 运行时会自动检测反射、代理、资源使用
    // 生成对应的RuntimeHints配置
}
```

### AOT相关源码

```java
// spring-beans中的AOT支持
org.springframework.beans.factory.aot
    ├── BeanRegistrationAotProcessor      // 注册处理器
    ├── BeanFactoryInitializationAotProcessor  // 初始化处理器
    ├── BeanInstanceSupplier              // 实例供应器
    ├── InstanceSupplierCodeGenerator     // 代码生成器
    └── AutowiredFieldValueResolver       // 字段值解析器

// spring-context中的AOT支持
org.springframework.context.aot
    ├── ContextAotProcessor               // 上下文处理器
    └── ApplicationContextAotGenerator    // 代码生成器
```

### 使用Spring Boot AOT

```bash
# 1. 添加GraalVM Native Image支持
# spring-boot-starter-parent已包含

# 2. 生成AOT代码
./mvnw spring-boot:process-aot

# 3. 编译原生镜像
./mvnw native:compile

# 4. 运行原生可执行文件
./target/myapplication
```

### 今日实践任务

1. 了解GraalVM原生镜像的概念
2. 了解Spring AOT的支持机制
3. 了解RuntimeHints的作用

---

## Day 5: 综合实践

### 完整测试用例

```java
@SpringJUnitConfig
@ContextConfiguration(classes = AdvancedFeaturesConfig.class)
public class AdvancedFeaturesTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void testCircularDependency() {
        // 测试循环依赖解决
        ServiceA a = context.getBean(ServiceA.class);
        ServiceB b = context.getBean(ServiceB.class);

        assertNotNull(a.getServiceB());
        assertNotNull(b.getServiceA());
    }

    @Test
    public void testPropertyEditor() {
        // 测试类型转换
        ConfigBean config = context.getBean(ConfigBean.class);
        assertEquals(5000, config.getTimeout());
        assertTrue(config.isEnabled());
    }

    @Test
    public void testBeanWrapper() {
        // 测试BeanWrapper
        User user = new User();
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(user);

        wrapper.setPropertyValue("name", "John");
        wrapper.setPropertyValue("address.city", "Beijing");

        assertEquals("John", user.getName());
        assertEquals("Beijing", user.getAddress().getCity());
    }
}
```

### 验证检查清单

- [ ] 理解三级缓存解决循环依赖的原理
- [ ] 了解PropertyEditors类型转换
- [ ] 了解BeanWrapper属性访问
- [ ] 了解AOT编译支持
- [ ] 通过调试观察过相关机制

---

## 常见问题

### Q1: 为什么循环依赖只在单例模式下解决？

**A**: Prototype模式下，每次getBean都会创建新实例，无法缓存早期引用。如果Prototype A依赖Prototype B，B又依赖A，会导致无限递归创建。

### Q2: PropertyEditor和PropertyEditorSupport的关系？

**A**: PropertyEditor是接口，PropertyEditorSupport是Spring提供的适配器类，继承它可以简化自定义PropertyEditor的实现。

### Q3: BeanWrapper和DirectFieldAccessor的区别？

**A**:
- BeanWrapper：通过getter/setter访问属性，支持类型转换
- DirectFieldAccessor：直接访问字段，绕过getter/setter

### Q4: AOT编译后还能使用动态代理吗？

**A**: 可以，但需要在AOT阶段声明所有可能被代理的接口。Spring会生成对应的代理类，而不是在运行时动态生成。

### Q5: RuntimeHints和reflect-config.json的关系？

**A**: RuntimeHints是Spring的API，reflect-config.json是GraalVM的配置文件格式。Spring AOT会将RuntimeHints转换为GraalVM需要的配置文件。

---

## 扩展阅读

### 推荐资料

1. **GraalVM官方文档** - https://www.graalvm.org/
2. **Spring Boot Native文档** - https://docs.spring.io/spring-boot/docs/current/reference/html/native-image.html
3. **Spring Framework AOT** - https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aot

### 推荐阅读源码

1. `DefaultSingletonBeanRegistry` - 三级缓存实现
2. `PropertyEditorRegistrySupport` - PropertyEditor注册
3. `BeanWrapperImpl` - BeanWrapper实现
4. `RuntimeHints` - AOT运行时提示

---

## 总结

完成阶段7后，你应该能够：

1. ✅ 理解循环依赖的三级缓存机制
2. ✅ 掌握PropertyEditors类型转换
3. ✅ 了解BeanWrapper属性访问
4. ✅ 理解AOT编译支持
5. ✅ 具备完整的Spring Beans知识体系

**整体学习路径回顾**：
```
阶段1：基础概念（BeanFactory、BeanDefinition）
    ↓
阶段2：Bean定义体系（AbstractBeanDefinition、RootBeanDefinition等）
    ↓
阶段3：BeanFactory实现（DefaultListableBeanFactory、创建流程）
    ↓
阶段4：依赖注入（@Autowired、依赖解析）
    ↓
阶段5：生命周期（Aware、InitializingBean、BeanPostProcessor）
    ↓
阶段6：配置方式（XML、Properties、Groovy）
    ↓
阶段7：高级特性（循环依赖、PropertyEditors、AOT）
```

祝贺你完成了Spring-Beans模块的完整学习！
