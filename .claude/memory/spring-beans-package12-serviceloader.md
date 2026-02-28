# Spring Framework - ServiceLoader 集成详解

## 目录

1. [Java ServiceLoader 机制简介](#1-java-serviceloader-机制简介)
2. [包结构概览](#2-包结构概览)
3. [AbstractServiceLoaderBasedFactoryBean - 抽象基类](#3-abstractserviceloaderbasedfactorybean---抽象基类)
4. [ServiceFactoryBean - 加载单个服务](#4-servicefactorybean---加载单个服务)
5. [ServiceListFactoryBean - 加载服务列表](#5-servicelistfactorybean---加载服务列表)
6. [ServiceLoaderFactoryBean - 暴露 ServiceLoader](#6-serviceloaderfactorybean---暴露-serviceloader)
7. [使用示例](#7-使用示例)
8. [模板方法模式分析](#8-模板方法模式分析)
9. [总结](#9-总结)

---

## 1. Java ServiceLoader 机制简介

### 1.1 什么是 ServiceLoader

`java.util.ServiceLoader` 是 JDK 1.6 引入的一种**服务提供者加载机制**（Service Provider Interface，SPI）。它允许应用程序在运行时动态发现和加载服务实现，而无需在代码中硬编码具体的实现类。

### 1.2 ServiceLoader 的工作原理

```
┌─────────────────────────────────────────────────────────────────┐
│                     ServiceLoader 加载流程                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 定义服务接口 (Service Interface)                              │
│           │                                                     │
│           ▼                                                     │
│  2. 创建服务实现类 (Service Implementation)                       │
│           │                                                     │
│           ▼                                                     │
│  3. 在 META-INF/services/ 下创建配置文件                          │
│     文件名: 服务接口全限定名                                       │
│     内容: 实现类全限定名列表                                       │
│           │                                                     │
│           ▼                                                     │
│  4. 使用 ServiceLoader.load(接口.class) 加载服务                 │
│           │                                                     │
│           ▼                                                     │
│  5. 遍历获取所有实现实例                                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 1.3 标准 Java ServiceLoader 示例

```java
// 步骤1: 定义服务接口
public interface MessageService {
    String getMessage();
}

// 步骤2: 创建实现类
public class HelloMessageService implements MessageService {
    @Override
    public String getMessage() {
        return "Hello, World!";
    }
}

// 步骤3: 创建配置文件
// 文件路径: META-INF/services/com.example.MessageService
// 文件内容: com.example.HelloMessageService

// 步骤4: 加载服务
ServiceLoader<MessageService> loader = ServiceLoader.load(MessageService.class);
for (MessageService service : loader) {
    System.out.println(service.getMessage());
}
```

---

## 2. 包结构概览

`org.springframework.beans.factory.serviceloader` 包提供了 Spring 与 JDK ServiceLoader 的集成支持。

```
org.springframework.beans.factory.serviceloader
│
├── AbstractServiceLoaderBasedFactoryBean (抽象基类)
│       ├── 继承自 AbstractFactoryBean<Object>
│       ├── 实现 BeanClassLoaderAware
│       └── 定义模板方法 getObjectToExpose()
│
├── ServiceFactoryBean (具体实现1)
│       ├── 获取第一个可用服务
│       └── 返回类型: serviceType
│
├── ServiceListFactoryBean (具体实现2)
│       ├── 获取所有服务为 List
│       └── 返回类型: List.class
│
└── ServiceLoaderFactoryBean (具体实现3)
        ├── 直接暴露 ServiceLoader
        └── 返回类型: ServiceLoader.class
```

---

## 3. AbstractServiceLoaderBasedFactoryBean - 抽象基类

### 3.1 类定义与继承关系

```java
// 文件路径: spring-beans/src/main/java/org/springframework/beans/factory/serviceloader/AbstractServiceLoaderBasedFactoryBean.java

public abstract class AbstractServiceLoaderBasedFactoryBean
        extends AbstractFactoryBean<Object>  // 继承 Spring 的 FactoryBean 抽象基类
        implements BeanClassLoaderAware {    // 实现类加载器感知接口
```

### 3.2 核心属性

```java
@Nullable
private Class<?> serviceType;  // 服务类型（必需配置）

@Nullable
private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();  // 类加载器
```

### 3.3 核心方法详解

#### 3.3.1 设置服务类型

```java
/**
 * 指定所需的服务类型（通常是服务的公共 API 接口）
 * 这是使用此 FactoryBean 的必需配置
 */
public void setServiceType(@Nullable Class<?> serviceType) {
    this.serviceType = serviceType;
}

/**
 * 获取配置的服务类型
 */
@Nullable
public Class<?> getServiceType() {
    return this.serviceType;
}
```

#### 3.3.2 类加载器设置

```java
/**
 * 实现 BeanClassLoaderAware 接口
 * Spring 容器会自动注入当前 Bean 的类加载器
 */
@Override
public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
    this.beanClassLoader = beanClassLoader;
}
```

#### 3.3.3 创建实例（模板方法模式的核心）

```java
/**
 * 委托给子类实现的 getObjectToExpose() 方法
 * 这是 AbstractFactoryBean 要求的抽象方法实现
 *
 * @return 要暴露的对象
 */
@Override
protected Object createInstance() {
    // 断言检查: serviceType 必须已设置
    Assert.state(getServiceType() != null, "Property 'serviceType' is required");

    // 使用 JDK ServiceLoader 加载服务
    // ServiceLoader.load(服务类型, 类加载器)
    ServiceLoader<?> serviceLoader = ServiceLoader.load(getServiceType(), this.beanClassLoader);

    // 调用抽象方法，由子类决定如何处理 ServiceLoader
    return getObjectToExpose(serviceLoader);
}
```

#### 3.3.4 抽象模板方法

```java
/**
 * 确定要为给定 ServiceLoader 暴露的实际对象
 *
 * 这是模板方法模式的关键：
 * - 父类负责创建 ServiceLoader（固定流程）
 * - 子类决定如何处理 ServiceLoader（可变行为）
 *
 * @param serviceLoader 为配置的服务类创建的 ServiceLoader
 * @return 要暴露的对象
 */
protected abstract Object getObjectToExpose(ServiceLoader<?> serviceLoader);
```

---

## 4. ServiceFactoryBean - 加载单个服务

### 4.1 用途说明

`ServiceFactoryBean` 用于暴露配置服务类的**"主"服务**（即第一个可用的服务实现）。当只需要一个服务实例时，使用此类。

### 4.2 完整源码分析

```java
// 文件路径: spring-beans/src/main/java/org/springframework/beans/factory/serviceloader/ServiceFactoryBean.java

public class ServiceFactoryBean extends AbstractServiceLoaderBasedFactoryBean
        implements BeanClassLoaderAware {

    /**
     * 实现父类的抽象模板方法
     * 从 ServiceLoader 中获取第一个服务实例
     *
     * @param serviceLoader JDK ServiceLoader 实例
     * @return 第一个服务实现对象
     * @throws IllegalStateException 如果找不到任何服务实现
     */
    @Override
    protected Object getObjectToExpose(ServiceLoader<?> serviceLoader) {
        // 获取迭代器
        Iterator<?> it = serviceLoader.iterator();

        // 检查是否有服务可用
        if (!it.hasNext()) {
            throw new IllegalStateException(
                    "ServiceLoader could not find service for type [" + getServiceType() + "]");
        }

        // 返回第一个服务实例
        return it.next();
    }

    /**
     * 返回此 FactoryBean 创建的对象类型
     * 由于暴露的是单个服务，类型就是配置的服务类型
     *
     * @return 服务类型
     */
    @Override
    @Nullable
    public Class<?> getObjectType() {
        return getServiceType();
    }
}
```

### 4.3 使用场景

- 当只需要一个服务实现时
- 当服务有明确的"主"实现时
- 当不需要关心具体是哪个实现时（遵循"第一个可用"原则）

---

## 5. ServiceListFactoryBean - 加载服务列表

### 5.1 用途说明

`ServiceListFactoryBean` 用于暴露配置服务类的**所有服务实现**，以 `List` 形式返回。当需要获取所有可用服务实现时，使用此类。

### 5.2 完整源码分析

```java
// 文件路径: spring-beans/src/main/java/org/springframework/beans/factory/serviceloader/ServiceListFactoryBean.java

public class ServiceListFactoryBean extends AbstractServiceLoaderBasedFactoryBean
        implements BeanClassLoaderAware {

    /**
     * 实现父类的抽象模板方法
     * 将 ServiceLoader 中的所有服务收集到 List 中
     *
     * @param serviceLoader JDK ServiceLoader 实例
     * @return 包含所有服务实现的 List
     */
    @Override
    protected Object getObjectToExpose(ServiceLoader<?> serviceLoader) {
        // 创建结果列表
        List<Object> result = new ArrayList<>();

        // 遍历 ServiceLoader，将所有服务添加到列表
        for (Object loaderObject : serviceLoader) {
            result.add(loaderObject);
        }

        return result;
    }

    /**
     * 返回此 FactoryBean 创建的对象类型
     * 由于暴露的是 List，返回 List.class
     *
     * @return List.class
     */
    @Override
    public Class<?> getObjectType() {
        return List.class;
    }
}
```

### 5.3 使用场景

- 当需要所有服务实现时
- 当需要遍历所有可用实现时
- 当需要实现策略模式（Strategy Pattern）时
- 当需要插件化架构时

---

## 6. ServiceLoaderFactoryBean - 暴露 ServiceLoader

### 6.1 用途说明

`ServiceLoaderFactoryBean` 直接暴露 `ServiceLoader` 本身，而不是加载具体的服务实例。这提供了最大的灵活性，允许调用者自行决定如何使用 ServiceLoader。

### 6.2 完整源码分析

```java
// 文件路径: spring-beans/src/main/java/org/springframework/beans/factory/serviceloader/ServiceLoaderFactoryBean.java

public class ServiceLoaderFactoryBean extends AbstractServiceLoaderBasedFactoryBean
        implements BeanClassLoaderAware {

    /**
     * 实现父类的抽象模板方法
     * 直接返回 ServiceLoader 实例，不做任何包装
     *
     * @param serviceLoader JDK ServiceLoader 实例
     * @return ServiceLoader 实例本身
     */
    @Override
    protected Object getObjectToExpose(ServiceLoader<?> serviceLoader) {
        return serviceLoader;
    }

    /**
     * 返回此 FactoryBean 创建的对象类型
     * 返回 ServiceLoader.class
     *
     * @return ServiceLoader.class
     */
    @Override
    public Class<?> getObjectType() {
        return ServiceLoader.class;
    }
}
```

### 6.3 使用场景

- 当需要延迟加载服务时（ServiceLoader 是懒加载的）
- 当需要多次遍历服务时
- 当需要 ServiceLoader 的特定功能时（如 `reload()`）
- 当需要最细粒度的控制时

---

## 7. 使用示例

### 7.1 完整项目结构示例

```
my-spring-project/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           ├── service/
│   │   │           │   └── PaymentService.java          # 服务接口
│   │   │           ├── impl/
│   │   │           │   ├── AlipayPaymentService.java    # 实现1
│   │   │           │   ├── WechatPaymentService.java    # 实现2
│   │   │           │   └── BankCardPaymentService.java  # 实现3
│   │   │           └── Main.java
│   │   └── resources/
│   │       └── META-INF/
│   │           └── services/
│   │               └── com.example.service.PaymentService  # 配置文件
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── ServiceLoaderTest.java
└── pom.xml / build.gradle
```

### 7.2 服务接口定义

```java
// PaymentService.java
package com.example.service;

/**
 * 支付服务接口
 * 作为 ServiceLoader 的服务类型
 */
public interface PaymentService {

    /**
     * 获取支付方式名称
     */
    String getPaymentMethod();

    /**
     * 执行支付
     * @param amount 金额
     * @return 支付结果
     */
    boolean pay(double amount);

    /**
     * 是否可用
     */
    boolean isAvailable();
}
```

### 7.3 服务实现类

```java
// AlipayPaymentService.java
package com.example.impl;

import com.example.service.PaymentService;

public class AlipayPaymentService implements PaymentService {

    @Override
    public String getPaymentMethod() {
        return "Alipay";
    }

    @Override
    public boolean pay(double amount) {
        System.out.println("使用支付宝支付: " + amount + " 元");
        return true;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}

// WechatPaymentService.java
package com.example.impl;

import com.example.service.PaymentService;

public class WechatPaymentService implements PaymentService {

    @Override
    public String getPaymentMethod() {
        return "WeChat Pay";
    }

    @Override
    public boolean pay(double amount) {
        System.out.println("使用微信支付: " + amount + " 元");
        return true;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}

// BankCardPaymentService.java
package com.example.impl;

import com.example.service.PaymentService;

public class BankCardPaymentService implements PaymentService {

    @Override
    public String getPaymentMethod() {
        return "Bank Card";
    }

    @Override
    public boolean pay(double amount) {
        System.out.println("使用银行卡支付: " + amount + " 元");
        return true;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
```

### 7.4 META-INF/services 配置文件

```
# 文件路径: META-INF/services/com.example.service.PaymentService
# 文件内容: 列出所有实现类的全限定名

com.example.impl.AlipayPaymentService
com.example.impl.WechatPaymentService
com.example.impl.BankCardPaymentService
```

### 7.5 Spring XML 配置示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!--
        示例1: ServiceFactoryBean - 获取单个服务
        将返回第一个可用的 PaymentService 实现（AlipayPaymentService）
    -->
    <bean id="primaryPaymentService"
          class="org.springframework.beans.factory.serviceloader.ServiceFactoryBean">
        <property name="serviceType" value="com.example.service.PaymentService"/>
    </bean>

    <!--
        示例2: ServiceListFactoryBean - 获取所有服务
        将返回 List<PaymentService>，包含所有三个实现
    -->
    <bean id="allPaymentServices"
          class="org.springframework.beans.factory.serviceloader.ServiceListFactoryBean">
        <property name="serviceType" value="com.example.service.PaymentService"/>
    </bean>

    <!--
        示例3: ServiceLoaderFactoryBean - 获取 ServiceLoader
        将返回 ServiceLoader<PaymentService>
    -->
    <bean id="paymentServiceLoader"
          class="org.springframework.beans.factory.serviceloader.ServiceLoaderFactoryBean">
        <property name="serviceType" value="com.example.service.PaymentService"/>
    </bean>

    <!--
        示例4: 在业务类中注入服务列表
    -->
    <bean id="paymentProcessor" class="com.example.PaymentProcessor">
        <property name="paymentServices" ref="allPaymentServices"/>
    </bean>

</beans>
```

### 7.6 Spring Java Config 配置示例

```java
// AppConfig.java
package com.example.config;

import com.example.service.PaymentService;
import org.springframework.beans.factory.serviceloader.ServiceFactoryBean;
import org.springframework.beans.factory.serviceloader.ServiceListFactoryBean;
import org.springframework.beans.factory.serviceloader.ServiceLoaderFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AppConfig {

    /**
     * 配置 ServiceFactoryBean - 获取单个服务
     */
    @Bean
    public ServiceFactoryBean primaryPaymentService() {
        ServiceFactoryBean factoryBean = new ServiceFactoryBean();
        factoryBean.setServiceType(PaymentService.class);
        return factoryBean;
    }

    /**
     * 配置 ServiceListFactoryBean - 获取所有服务
     */
    @Bean
    public ServiceListFactoryBean allPaymentServices() {
        ServiceListFactoryBean factoryBean = new ServiceListFactoryBean();
        factoryBean.setServiceType(PaymentService.class);
        return factoryBean;
    }

    /**
     * 配置 ServiceLoaderFactoryBean - 获取 ServiceLoader
     */
    @Bean
    public ServiceLoaderFactoryBean paymentServiceLoader() {
        ServiceLoaderFactoryBean factoryBean = new ServiceLoaderFactoryBean();
        factoryBean.setServiceType(PaymentService.class);
        return factoryBean;
    }
}
```

### 7.7 业务类使用示例

```java
// PaymentProcessor.java
package com.example;

import com.example.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentProcessor {

    // 方式1: 注入单个服务
    @Autowired
    private PaymentService primaryPaymentService;

    // 方式2: 注入所有服务
    @Autowired
    private List<PaymentService> paymentServices;

    /**
     * 使用主支付方式
     */
    public boolean processWithPrimary(double amount) {
        System.out.println("使用主支付方式: " + primaryPaymentService.getPaymentMethod());
        return primaryPaymentService.pay(amount);
    }

    /**
     * 尝试所有支付方式，直到成功
     */
    public boolean processWithAnyAvailable(double amount) {
        for (PaymentService service : paymentServices) {
            if (service.isAvailable()) {
                System.out.println("使用支付方式: " + service.getPaymentMethod());
                return service.pay(amount);
            }
        }
        throw new IllegalStateException("没有可用的支付方式");
    }

    /**
     * 显示所有可用支付方式
     */
    public void listAllPaymentMethods() {
        System.out.println("可用的支付方式:");
        for (PaymentService service : paymentServices) {
            System.out.println("  - " + service.getPaymentMethod() +
                             " (可用: " + service.isAvailable() + ")");
        }
    }
}
```

### 7.8 测试代码示例

```java
// ServiceLoaderTest.java
package com.example;

import com.example.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.serviceloader.ServiceFactoryBean;
import org.springframework.beans.factory.serviceloader.ServiceListFactoryBean;
import org.springframework.beans.factory.serviceloader.ServiceLoaderFactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import java.util.List;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceLoaderTest {

    @Test
    void testServiceFactoryBean() {
        // 创建 BeanFactory
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        // 定义 ServiceFactoryBean
        RootBeanDefinition beanDefinition = new RootBeanDefinition(ServiceFactoryBean.class);
        beanDefinition.getPropertyValues().add("serviceType", PaymentService.class.getName());
        beanFactory.registerBeanDefinition("paymentService", beanDefinition);

        // 获取 Bean
        PaymentService service = (PaymentService) beanFactory.getBean("paymentService");

        // 验证
        assertThat(service).isNotNull();
        assertThat(service.getPaymentMethod()).isEqualTo("Alipay"); // 第一个实现
        System.out.println("主服务: " + service.getPaymentMethod());
    }

    @Test
    void testServiceListFactoryBean() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        RootBeanDefinition beanDefinition = new RootBeanDefinition(ServiceListFactoryBean.class);
        beanDefinition.getPropertyValues().add("serviceType", PaymentService.class.getName());
        beanFactory.registerBeanDefinition("paymentServices", beanDefinition);

        List<?> services = (List<?>) beanFactory.getBean("paymentServices");

        assertThat(services).hasSize(3);
        for (Object service : services) {
            PaymentService ps = (PaymentService) service;
            System.out.println("服务: " + ps.getPaymentMethod());
        }
    }

    @Test
    void testServiceLoaderFactoryBean() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        RootBeanDefinition beanDefinition = new RootBeanDefinition(ServiceLoaderFactoryBean.class);
        beanDefinition.getPropertyValues().add("serviceType", PaymentService.class.getName());
        beanFactory.registerBeanDefinition("serviceLoader", beanDefinition);

        ServiceLoader<?> loader = (ServiceLoader<?>) beanFactory.getBean("serviceLoader");

        assertThat(loader).isNotNull();
        int count = 0;
        for (Object service : loader) {
            PaymentService ps = (PaymentService) service;
            System.out.println("ServiceLoader 服务: " + ps.getPaymentMethod());
            count++;
        }
        assertThat(count).isEqualTo(3);
    }
}
```

---

## 8. 模板方法模式分析

### 8.1 模式结构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                    模板方法模式 (Template Method Pattern)             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │           AbstractFactoryBean<T> (Spring 框架层)             │   │
│  │  ┌───────────────────────────────────────────────────────┐  │   │
│  │  │ 模板方法: getObject()                                  │  │   │
│  │  │   - 处理单例/原型逻辑                                   │  │   │
│  │  │   - 调用 createInstance()                              │  │   │
│  │  └───────────────────────────────────────────────────────┘  │   │
│  │  ┌───────────────────────────────────────────────────────┐  │   │
│  │  │ 抽象方法: createInstance() ← 子类必须实现               │  │   │
│  │  └───────────────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              ▲                                      │
│                              │ 继承                                  │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │    AbstractServiceLoaderBasedFactoryBean (本包抽象层)        │   │
│  │  ┌───────────────────────────────────────────────────────┐  │   │
│  │  │ 实现 createInstance()                                  │  │   │
│  │  │   - 创建 ServiceLoader                                 │  │   │
│  │  │   - 调用 getObjectToExpose()                           │  │   │
│  │  └───────────────────────────────────────────────────────┘  │   │
│  │  ┌───────────────────────────────────────────────────────┐  │   │
│  │  │ 抽象方法: getObjectToExpose() ← 子类必须实现            │  │   │
│  │  └───────────────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              ▲                                      │
│              ┌───────────────┼───────────────┐                      │
│              │               │               │                      │
│     ┌────────┴────────┐ ┌────┴─────┐ ┌──────┴──────┐                │
│     │ ServiceFactoryBean│ServiceList│ServiceLoader│                │
│     │   (具体实现1)     │FactoryBean│FactoryBean  │                │
│     │                   │ (实现2)   │  (实现3)     │                │
│     │ - 返回第一个服务   │ - 返回List│ - 返回Loader │                │
│     └─────────────────┘ └─────────┘ └───────────┘                │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 8.2 模板方法调用链

```
Spring 容器请求 Bean
       │
       ▼
┌──────────────────┐
│ FactoryBean.     │
│ getObject()      │  ← AbstractFactoryBean 实现（模板方法）
└──────────────────┘
       │
       ▼
┌──────────────────┐
│ 检查单例/原型     │
│ 模式             │
└──────────────────┘
       │
       ▼
┌──────────────────┐
│ createInstance() │  ← AbstractFactoryBean 定义的抽象方法
└──────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ AbstractServiceLoaderBasedFactoryBean │
│ 的 createInstance() 实现              │
│                                       │
│ 1. 验证 serviceType 不为 null        │
│ 2. ServiceLoader.load(serviceType)   │
│ 3. 调用 getObjectToExpose(loader)    │
└──────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 子类实现 getObjectToExpose()          │
│                                       │
│ ServiceFactoryBean:                   │
│   return iterator.next() // 第一个    │
│                                       │
│ ServiceListFactoryBean:               │
│   return new ArrayList<>(loader)      │
│                                       │
│ ServiceLoaderFactoryBean:             │
│   return loader // 直接返回           │
└──────────────────────────────────────┘
```

### 8.3 模板方法模式的优点

1. **代码复用**: 父类 `AbstractServiceLoaderBasedFactoryBean` 封装了创建 ServiceLoader 的通用逻辑
2. **扩展性**: 子类只需实现 `getObjectToExpose()` 方法即可定义不同的行为
3. **控制反转**: 父类控制整体流程，子类提供具体实现
4. **一致性**: 所有子类都遵循相同的 ServiceLoader 创建流程

### 8.4 源码中的模板方法实现

```java
// 第一层模板: AbstractFactoryBean
public abstract class AbstractFactoryBean<T> implements FactoryBean<T> {

    // 模板方法 - 定义算法骨架
    @Override
    public final T getObject() throws Exception {
        if (isSingleton()) {
            return (this.initialized ? this.singletonInstance : getEarlySingletonInstance());
        }
        else {
            return createInstance();  // 调用抽象方法
        }
    }

    // 抽象方法 - 子类实现
    protected abstract T createInstance() throws Exception;
}

// 第二层模板: AbstractServiceLoaderBasedFactoryBean
public abstract class AbstractServiceLoaderBasedFactoryBean extends AbstractFactoryBean<Object> {

    // 实现父类的抽象方法，同时定义新的模板
    @Override
    protected Object createInstance() {
        Assert.state(getServiceType() != null, "Property 'serviceType' is required");

        // 固定步骤: 创建 ServiceLoader
        ServiceLoader<?> serviceLoader = ServiceLoader.load(getServiceType(), this.beanClassLoader);

        // 调用新的抽象方法 - 钩子方法
        return getObjectToExpose(serviceLoader);
    }

    // 新的抽象方法 - 子类实现
    protected abstract Object getObjectToExpose(ServiceLoader<?> serviceLoader);
}

// 具体实现: ServiceFactoryBean
public class ServiceFactoryBean extends AbstractServiceLoaderBasedFactoryBean {

    @Override
    protected Object getObjectToExpose(ServiceLoader<?> serviceLoader) {
        Iterator<?> it = serviceLoader.iterator();
        if (!it.hasNext()) {
            throw new IllegalStateException("ServiceLoader could not find service...");
        }
        return it.next();  // 返回第一个服务
    }

    @Override
    public Class<?> getObjectType() {
        return getServiceType();
    }
}
```

---

## 9. 总结

### 9.1 三个 FactoryBean 对比

| 特性 | ServiceFactoryBean | ServiceListFactoryBean | ServiceLoaderFactoryBean |
|------|-------------------|----------------------|------------------------|
| **返回类型** | 单个服务对象 | `List<Object>` | `ServiceLoader` |
| **服务数量** | 第一个可用服务 | 所有服务 | 懒加载迭代器 |
| **使用场景** | 只需要一个服务 | 需要所有服务 | 需要延迟加载 |
| **空服务处理** | 抛出异常 | 返回空列表 | 返回空迭代器 |
| **getObjectType()** | `serviceType` | `List.class` | `ServiceLoader.class` |

### 9.2 使用建议

1. **ServiceFactoryBean**: 当服务有明确的"主"实现，或只需要一个服务时使用
2. **ServiceListFactoryBean**: 当需要遍历所有服务实现，或实现插件化架构时使用
3. **ServiceLoaderFactoryBean**: 当需要延迟加载，或需要 ServiceLoader 的特定功能时使用

### 9.3 关键文件路径

- **AbstractServiceLoaderBasedFactoryBean**: `spring-beans/src/main/java/org/springframework/beans/factory/serviceloader/AbstractServiceLoaderBasedFactoryBean.java`
- **ServiceFactoryBean**: `spring-beans/src/main/java/org/springframework/beans/factory/serviceloader/ServiceFactoryBean.java`
- **ServiceListFactoryBean**: `spring-beans/src/main/java/org/springframework/beans/factory/serviceloader/ServiceListFactoryBean.java`
- **ServiceLoaderFactoryBean**: `spring-beans/src/main/java/org/springframework/beans/factory/serviceloader/ServiceLoaderFactoryBean.java`
- **测试类**: `spring-beans/src/test/java/org/springframework/beans/factory/serviceloader/ServiceLoaderTests.java`

### 9.4 设计亮点

1. **分层抽象**: 通过两层模板方法模式，实现了高度的代码复用和灵活性
2. **接口隔离**: 每个 FactoryBean 都有明确的职责和返回类型
3. **与 Spring 集成**: 完美融入 Spring 的 Bean 生命周期和依赖注入机制
4. **零侵入**: 服务实现类不需要任何 Spring 注解或依赖，保持纯 JDK SPI 风格
