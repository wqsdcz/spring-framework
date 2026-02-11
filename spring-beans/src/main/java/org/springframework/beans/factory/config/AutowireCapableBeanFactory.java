/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.config;

import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.beans.factory.BeanFactory} 接口的扩展，
 * 由能够自动装配的 bean 工厂实现，前提是它们希望为现有的 bean 实例暴露此功能。
 *
 * <p>
 *     这个 BeanFactory 的子接口并非设计用于常规应用程序代码中：
 *     对于典型用例，请坚持使用 {@link org.springframework.beans.factory.BeanFactory}
 *     或 {@link org.springframework.beans.factory.ListableBeanFactory}。
 *
 * <p>
 *     其他框架的集成代码可以利用此接口来连接和填充 Spring 不控制其生命周期的现有 bean 实例。
 *     例如，这对于 WebWork Actions 和 Tapestry Page 对象特别有用。
 *
 * <p>
 *     请注意，此接口并未由 {@link org.springframework.context.ApplicationContext} 外观实现，因为应用程序代码很少使用它。
 *     话虽如此，它也可以从应用程序上下文中获得，通过 ApplicationContext 的
 *     {@link org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()} 方法访问。
 *
 * <p>
 *     您还可以实现 {@link org.springframework.beans.factory.BeanFactoryAware} 接口，
 *     即使在 ApplicationContext 中运行时也能暴露内部 BeanFactory，从而获取对 AutowireCapableBeanFactory 的访问权限：
 *     只需将传入的 BeanFactory 转换为 AutowireCapableBeanFactory 即可。
 *
 * @author Juergen Hoeller
 * @since 04.12.2003
 * @see org.springframework.beans.factory.BeanFactoryAware
 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory
 * @see org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()
 */
public interface AutowireCapableBeanFactory extends BeanFactory {

	/**
	 * 常量，表示没有外部定义的自动装配。
	 * 请注意，BeanFactoryAware 等以及注解驱动的注入仍然会被应用。
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_NO = 0;

	/**
	 * 常量，表示按名称自动装配 bean 属性（应用于所有 bean 属性的 setter 方法）。
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_BY_NAME = 1;

	/**
	 * 常量，表示按类型自动装配 bean 属性（应用于所有 bean 属性的 setter 方法）。
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_BY_TYPE = 2;

	/**
	 * 常量，表示自动装配能满足条件的最贪婪构造函数（涉及解析适当的构造函数）。
	 * @see #autowire
	 */
	int AUTOWIRE_CONSTRUCTOR = 3;

	/**
	 * 常量，表示通过内省 bean 类来确定合适的自动装配策略。
	 * @see #autowire
	 * @deprecated 自 Spring 3.0 起已弃用：如果您使用混合的自动装配策略，请优先使用基于注解的自动装配，以便更清晰地划分自动装配需求。
	 */
	@Deprecated
	int AUTOWIRE_AUTODETECT = 4;

	/**
	 * 初始化现有 bean 实例时“原始实例”约定的后缀：要附加到完全限定的 bean 类名上，
	 * 例如 "com.mypackage.MyClass.ORIGINAL"，以强制返回给定实例，即不使用代理等。
	 *
	 * @since 5.1
	 * @see #initializeBean(Object, String)
	 * @see #applyBeanPostProcessorsBeforeInitialization(Object, String)
	 * @see #applyBeanPostProcessorsAfterInitialization(Object, String)
	 */
	String ORIGINAL_INSTANCE_SUFFIX = ".ORIGINAL";


	//-------------------------------------------------------------------------
	// 用于创建和填充外部 bean 实例的典型方法
	//-------------------------------------------------------------------------

	/**
	 * 完全创建给定类的新 bean 实例。
	 * <p>
	 *     执行 bean 的完整初始化，包括所有适用的{@link BeanPostProcessor BeanPostProcessors}。
	 * <p>
	 *     注意：此方法旨在创建一个新的实例，填充带注解的字段和方法，并应用所有标准的 bean 初始化回调。
	 *     构造函数解析基于 Kotlin 主构造函数 / 单个公共构造函数 / 单个非公共构造函数，在模糊场景下回退到默认构造函数，
	 *     同时也受 {@link SmartInstantiationAwareBeanPostProcessor#determineCandidateConstructors} 的影响（例如，用于注解驱动的构造函数选择）。
	 *
	 * @param beanClass 要创建的 bean 的类
	 * @return 新的 bean 实例
	 * @throws BeansException 如果实例化或装配失败
	 */
	<T> T createBean(Class<T> beanClass) throws BeansException;

	/**
	 * 通过应用实例化后的回调和 bean 属性后处理（例如，用于注解驱动的注入）来填充给定的 bean 实例。
	 * <p>
	 *     注意：这本质上是为（重新）填充注解字段和方法而设计的，无论是对于新实例还是反序列化实例。
	 *     它并<i>不</i>意味着传统的按名称或按类型的属性自动装配；如需此类目的，请使用 {@link #autowireBeanProperties}。
	 * @param existingBean 现有的 bean 实例
	 * @throws BeansException 如果装配失败
	 */
	void autowireBean(Object existingBean) throws BeansException;

	/**
	 * 配置给定的原始 bean：自动装配 bean 属性，应用 bean 属性值，应用工厂回调（例如 {@code setBeanName}
	 * 和 {@code setBeanFactory}），并应用所有 bean 后处理器（包括可能包装给定原始 bean 的后处理器）。
	 * <p>
	 *     这实际上是 {@link #initializeBean} 提供功能的超集，完全应用相应 bean 定义指定的配置。
	 *     <b>注意：此方法需要给定名称的 bean 定义！</b>
	 *
	 * @param existingBean 现有的 bean 实例
	 * @param beanName bean 的名称，如有必要将传递给它（必须存在该名称的 bean 定义）
	 * @return 要使用的 bean 实例，可以是原始实例或包装后的实例
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException 如果不存在具有给定名称的 bean 定义
	 * @throws BeansException 如果初始化失败
	 * @see #initializeBean
	 */
	Object configureBean(Object existingBean, String beanName) throws BeansException;


	//-------------------------------------------------------------------------
	// 用于对 bean 生命周期进行细粒度控制的专门方法
	//-------------------------------------------------------------------------

	/**
	 * 使用指定的自动装配策略完全创建给定类的新 bean 实例。
	 * 此接口中定义的所有常量均支持此处使用。
	 * <p>
	 *     执行 bean 的完整初始化，包括所有适用的 {@link BeanPostProcessor BeanPostProcessors}。
	 *     这实际上是 {@link #autowire} 提供功能的超集，增加了 {@link #initializeBean} 的行为。
	 *
	 * @param beanClass 要创建的 bean 的类
	 * @param autowireMode 按名称或类型，使用此接口中的常量
	 * @param dependencyCheck 是否对对象执行依赖项检查
	 * （不适用于构造函数自动装配，因此在那里被忽略）
	 * @return 新的 bean 实例
	 * @throws BeansException 如果实例化或装配失败
	 * @see #AUTOWIRE_NO
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_CONSTRUCTOR
	 * @deprecated 自 6.1 起已弃用，推荐使用 {@link #createBean(Class)}
	 */
	@Deprecated(since = "6.1")
	Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;

	/**
	 * 使用指定的自动装配策略实例化给定类的新 bean 实例。
	 * 此接口中定义的所有常量均支持此处使用。
	 * 也可以使用 {@code AUTOWIRE_NO} 调用，以便仅应用实例化前的回调（例如，用于注解驱动的注入）。
	 * <p>
	 *     <i>不会</i>应用标准的 {@link BeanPostProcessor BeanPostProcessors} 回调或执行 bean 的进一步初始化。
	 *     此接口为此类目的提供了不同的、细粒度的操作，例如 {@link #initializeBean}。
	 *     但是，如果适用于实例构造，则会应用 {@link InstantiationAwareBeanPostProcessor} 回调。
	 * @param beanClass 要实例化的 bean 的类
	 * @param autowireMode 按名称或类型，使用此接口中的常量
	 * @param dependencyCheck 是否对 bean 实例中的对象引用执行依赖项检查（不适用于构造函数自动装配，因此在那里被忽略）
	 * @return 新的 bean 实例
	 * @throws BeansException 如果实例化或装配失败
	 * @see #AUTOWIRE_NO
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_CONSTRUCTOR
	 * @see #AUTOWIRE_AUTODETECT
	 * @see #initializeBean
	 * @see #applyBeanPostProcessorsBeforeInitialization
	 * @see #applyBeanPostProcessorsAfterInitialization
	 */
	Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;

	/**
	 * 通过名称或类型自动装配给定 bean 实例的属性。
	 * 也可以使用 {@code AUTOWIRE_NO} 调用，以便仅应用实例化后的回调（例如，用于注解驱动的注入）。
	 * <p>
	 *     <i>不会</i>应用标准的 {@link BeanPostProcessor BeanPostProcessors} 回调或执行 bean 的进一步初始化。
	 *     此接口为这些目的提供了不同的、细粒度的操作，例如 {@link #initializeBean}。
	 *     但是，如果适用于实例配置，则会应用 {@link InstantiationAwareBeanPostProcessor} 回调。
	 *
	 * @param existingBean 现有的 bean 实例
	 * @param autowireMode 按名称或类型，使用此接口中的常量
	 * @param dependencyCheck 是否对 bean 实例中的对象引用执行依赖项检查
	 * @throws BeansException 如果装配失败
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_NO
	 */
	void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck)
			throws BeansException;

	/**
	 * 将具有给定名称的 bean 定义的属性值应用于给定的 bean 实例。
	 * bean 定义可以定义一个完全自包含的 bean，重用其属性值，或者仅为现有 bean 实例使用的属性值。
	 * <p>
	 *     此方法<i>不会</i>自动装配 bean 属性；它只应用显式定义的属性值。
	 *     使用 {@link #autowireBeanProperties} 方法来自动装配现有的 bean 实例。
	 *     <b>注意：此方法要求给定名称存在 bean 定义！</b>
	 * <p>
	 *     <i>不会</i>应用标准的 {@link BeanPostProcessor BeanPostProcessors} 回调或执行 bean 的进一步初始化。
	 *     此接口为这些目的提供了不同的、细粒度的操作，例如 {@link #initializeBean}。
	 *     但是，如果适用于实例配置，则会应用 {@link InstantiationAwareBeanPostProcessor} 回调。
	 *
	 * @param existingBean 现有的 bean 实例
	 * @param beanName bean 工厂中 bean 定义的名称（必须存在该名称的 bean 定义）
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException 如果不存在具有给定名称的 bean 定义
	 * @throws BeansException 如果应用属性值失败
	 * @see #autowireBeanProperties
	 */
	void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException;

	/**
	 * 初始化给定的原始 bean，应用工厂回调，例如 {@code setBeanName} 和 {@code setBeanFactory}，
	 * 同时应用所有的 bean 后处理器（包括可能包装给定原始 bean 的后处理器）。
	 * <p>
	 *     请注意，bean 工厂中不需要存在给定名称的 bean 定义。
	 *     传入的 bean 名称将仅用于回调，而不会与已注册的 bean 定义进行检查。
	 *
	 * @param existingBean 现有的 bean 实例
	 * @param beanName bean 的名称，如有必要将传递给它（仅传递给 {@link BeanPostProcessor BeanPostProcessors}；
	 *                 可以遵循 {@link #ORIGINAL_INSTANCE_SUFFIX} 约定，以强制返回给定实例，即不使用代理等）
	 * @return 要使用的 bean 实例，可以是原始实例或包装后的实例
	 * @throws BeansException 如果初始化失败
	 * @see #ORIGINAL_INSTANCE_SUFFIX
	 */
	Object initializeBean(Object existingBean, String beanName) throws BeansException;

	/**
	 * 将 {@link BeanPostProcessor BeanPostProcessors} 应用于给定的现有 bean 实例，调用它们的 {@code postProcessBeforeInitialization} 方法。
	 * 返回的 bean 实例可能是原始实例的包装器。
	 * @param existingBean 现有的 bean 实例
	 * @param beanName bean 的名称，如有必要将传递给它（仅传递给 {@link BeanPostProcessor BeanPostProcessors}；
	 *                 可以遵循 {@link #ORIGINAL_INSTANCE_SUFFIX} 约定，以强制返回给定实例，即不使用代理等）
	 * @return 要使用的 bean 实例，可以是原始实例或包装后的实例
	 * @throws BeansException 如果任何后处理失败
	 * @see BeanPostProcessor#postProcessBeforeInitialization
	 * @see #ORIGINAL_INSTANCE_SUFFIX
	 * @deprecated 自 6.1 起已弃用，推荐使用通过 {@link #initializeBean(Object, String)} 进行隐式后处理
	 */
	@Deprecated(since = "6.1")
	Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
			throws BeansException;

	/**
	 * 将 {@link BeanPostProcessor BeanPostProcessors} 应用于给定的现有 bean 实例，调用它们的 {@code postProcessAfterInitialization} 方法。
	 * 返回的 bean 实例可能是原始实例的包装器。
	 *
	 * @param existingBean 现有的 bean 实例
	 * @param beanName bean 的名称，如有必要将传递给它（仅传递给 {@link BeanPostProcessor BeanPostProcessors}；
	 *                 可以遵循 {@link #ORIGINAL_INSTANCE_SUFFIX} 约定，以强制返回给定实例，即不使用代理等）
	 * @return 要使用的 bean 实例，可以是原始实例或包装后的实例
	 * @throws BeansException 如果任何后处理失败
	 * @see BeanPostProcessor#postProcessAfterInitialization
	 * @see #ORIGINAL_INSTANCE_SUFFIX
	 * @deprecated 自 6.1 起已弃用，推荐使用通过 {@link #initializeBean(Object, String)} 进行隐式后处理
	 */
	@Deprecated(since = "6.1")
	Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
			throws BeansException;

	/**
	 * 销毁给定的 bean 实例（通常来自 {@link #createBean(Class)}），
	 * 应用 {@link org.springframework.beans.factory.DisposableBean} 合约以及
	 * 已注册的 {@link DestructionAwareBeanPostProcessor DestructionAwareBeanPostProcessors}。
	 * <p>
	 *     销毁过程中产生的任何异常都应被捕获并记录日志，而不应传播给此方法的调用者。
	 * @param existingBean 要销毁的 bean 实例
	 */
	void destroyBean(Object existingBean);


	//-------------------------------------------------------------------------
	// 用于解析注入点的委托方法
	//-------------------------------------------------------------------------

	/**
	 * 解析与给定对象类型唯一匹配的 bean 实例（如果存在），包括其 bean 名称。
	 * <p>
	 *     这实际上是 {@link #getBean(Class)} 的变体，它保留了匹配实例的 bean 名称。
	 *
	 * @param requiredType bean 必须匹配的类型；可以是接口或超类
	 * @return bean 名称加上 bean 实例
	 * @throws NoSuchBeanDefinitionException 如果未找到匹配的 bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个匹配的 bean
	 * @throws BeansException 如果无法创建 bean
	 * @since 4.3.3
	 * @see #getBean(Class)
	 */
	<T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException;

	/**
	 * 根据给定的 bean 名称解析 bean 实例，并提供依赖项描述符以暴露给目标工厂方法。
	 * <p>
	 *     这实际上是 {@link #getBean(String, Class)} 的变体，
	 *     支持带有 {@link org.springframework.beans.factory.InjectionPoint} 参数的工厂方法。
	 *
	 * @param name 要查找的 bean 名称
	 * @param descriptor 请求注入点的依赖项描述符
	 * @return 相应的 bean 实例
	 * @throws NoSuchBeanDefinitionException 如果没有指定名称的 bean
	 * @throws BeansException 如果无法创建 bean
	 * @since 5.1.5
	 * @see #getBean(String, Class)
	 */
	Object resolveBeanByName(String name, DependencyDescriptor descriptor) throws BeansException;

	/**
	 * 根据在此工厂中定义的 bean 解析指定的依赖项。
	 *
	 * @param descriptor 依赖项的描述符（字段/方法/构造函数）
	 * @param requestingBeanName 声明给定依赖项的 bean 名称
	 * @return 解析后的对象，如果未找到则返回 {@code null}
	 * @throws NoSuchBeanDefinitionException 如果未找到匹配的 bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个匹配的 bean
	 * @throws BeansException 如果因其他原因导致依赖项解析失败
	 * @since 2.5
	 * @see #resolveDependency(DependencyDescriptor, String, Set, TypeConverter)
	 */
	@Nullable
	Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName) throws BeansException;

	/**
	 * 根据在此工厂中定义的 bean 解析指定的依赖项。
	 *
	 * @param descriptor 依赖项的描述符（字段/方法/构造函数）
	 * @param requestingBeanName 声明给定依赖项的 bean 名称
	 * @param autowiredBeanNames 一个 Set，所有自动装配的 bean 名称（用于解析给定依赖项）都应添加到其中
	 * @param typeConverter 用于填充数组和集合的 TypeConverter
	 * @return 解析后的对象，如果未找到则返回 {@code null}
	 * @throws NoSuchBeanDefinitionException 如果未找到匹配的 bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个匹配的 bean
	 * @throws BeansException 如果因其他原因导致依赖项解析失败
	 * @since 2.5
	 * @see DependencyDescriptor
	 */
	@Nullable
	Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException;

}
