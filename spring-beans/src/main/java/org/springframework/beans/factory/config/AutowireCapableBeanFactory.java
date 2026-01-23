/*
 * Copyright 2002-2019 the original author or authors.
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
 * Extension of the {@link org.springframework.beans.factory.BeanFactory}
 * interface to be implemented by bean factories that are capable of
 * autowiring, provided that they want to expose this functionality for
 * existing bean instances.
 *
 * <p>This subinterface of BeanFactory is not meant to be used in normal
 * application code: stick to {@link org.springframework.beans.factory.BeanFactory}
 * or {@link org.springframework.beans.factory.ListableBeanFactory} for
 * typical use cases.
 *
 * <p>Integration code for other frameworks can leverage this interface to
 * wire and populate existing bean instances that Spring does not control
 * the lifecycle of. This is particularly useful for WebWork Actions and
 * Tapestry Page objects, for example.
 *
 * <p>Note that this interface is not implemented by
 * {@link org.springframework.context.ApplicationContext} facades,
 * as it is hardly ever used by application code. That said, it is available
 * from an application context too, accessible through ApplicationContext's
 * {@link org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()}
 * method.
 *
 * <p>You may also implement the {@link org.springframework.beans.factory.BeanFactoryAware}
 * interface, which exposes the internal BeanFactory even when running in an
 * ApplicationContext, to get access to an AutowireCapableBeanFactory:
 * simply cast the passed-in BeanFactory to AutowireCapableBeanFactory.
 *
 * @author Juergen Hoeller
 * @since 04.12.2003
 * @see org.springframework.beans.factory.BeanFactoryAware
 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory
 * @see org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()
 */
public interface AutowireCapableBeanFactory extends BeanFactory {

	/**
	 * Constant that indicates no externally defined autowiring. Note that
	 * BeanFactoryAware etc and annotation-driven injection will still be applied.
	 * @see #createBean
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_NO = 0;

	/**
	 * Constant that indicates autowiring bean properties by name
	 * (applying to all bean property setters).
	 * @see #createBean
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_BY_NAME = 1;

	/**
	 * Constant that indicates autowiring bean properties by type
	 * (applying to all bean property setters).
	 * @see #createBean
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_BY_TYPE = 2;

	/**
	 * Constant that indicates autowiring the greediest constructor that
	 * can be satisfied (involves resolving the appropriate constructor).
	 * @see #createBean
	 * @see #autowire
	 */
	int AUTOWIRE_CONSTRUCTOR = 3;

	/**
	 * Constant that indicates determining an appropriate autowire strategy
	 * through introspection of the bean class.
	 * @see #createBean
	 * @see #autowire
	 * @deprecated as of Spring 3.0: If you are using mixed autowiring strategies,
	 * prefer annotation-based autowiring for clearer demarcation of autowiring needs.
	 */
	@Deprecated
	int AUTOWIRE_AUTODETECT = 4;


	//-------------------------------------------------------------------------
	// Typical methods for creating and populating external bean instances
	//-------------------------------------------------------------------------

	/**
	 * 完整地创建一个指定class的新的Bean实例。
	 * <p>执行Bean的完整初始化过程，包括所有适用的{@link BeanPostProcessor BeanPostProcessors}。</p>
	 * <p>
	 *     备注：这是为了创建一个新的实例，填充带注解的字段和方法，以及应用所有的标准Bean的初始化回调。
	 *     它并不意味着采用传统的按名称或按类型的方式进行属性的自动绑定；使用{@link #createBean(Class, int, boolean)}来实现这些目的。
	 * </p>
	 * Fully create a new bean instance of the given class.
	 * <p>Performs full initialization of the bean, including all applicable
	 * {@link BeanPostProcessor BeanPostProcessors}.
	 * <p>Note: This is intended for creating a fresh instance, populating annotated
	 * fields and methods as well as applying all standard bean initialization callbacks.
	 * It does <i>not</i> imply traditional by-name or by-type autowiring of properties;
	 * use {@link #createBean(Class, int, boolean)} for those purposes.
	 * @param beanClass the class of the bean to create
	 * @return the new bean instance
	 * @throws BeansException if instantiation or wiring failed
	 */
	<T> T createBean(Class<T> beanClass) throws BeansException;

	/**
	 * Populate the given bean instance through applying after-instantiation callbacks
	 * and bean property post-processing (e.g. for annotation-driven injection).
	 * <p>Note: This is essentially intended for (re-)populating annotated fields and
	 * methods, either for new instances or for deserialized instances. It does
	 * <i>not</i> imply traditional by-name or by-type autowiring of properties;
	 * use {@link #autowireBeanProperties} for those purposes.
	 * @param existingBean the existing bean instance
	 * @throws BeansException if wiring failed
	 */
	void autowireBean(Object existingBean) throws BeansException;

	/**
	 * Configure the given raw bean: autowiring bean properties, applying
	 * bean property values, applying factory callbacks such as {@code setBeanName}
	 * and {@code setBeanFactory}, and also applying all bean post processors
	 * (including ones which might wrap the given raw bean).
	 * <p>This is effectively a superset of what {@link #initializeBean} provides,
	 * fully applying the configuration specified by the corresponding bean definition.
	 * <b>Note: This method requires a bean definition for the given name!</b>
	 * @param existingBean the existing bean instance
	 * @param beanName the name of the bean, to be passed to it if necessary
	 * (a bean definition of that name has to be available)
	 * @return the bean instance to use, either the original or a wrapped one
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException
	 * if there is no bean definition with the given name
	 * @throws BeansException if the initialization failed
	 * @see #initializeBean
	 */
	Object configureBean(Object existingBean, String beanName) throws BeansException;


	//-------------------------------------------------------------------------
	// Specialized methods for fine-grained control over the bean lifecycle
	//-------------------------------------------------------------------------

	/**
	 * 使用指定的自动装配策略完整地创建给定类的新bean实例。
	 * 此接口定义的所有常量在此处均受支持。
	 * <p>
	 *     执行Bean的完整初始化过程，包括所有适用的{@link BeanPostProcessor BeanPostProcessors}。
	 *     这实际上是{@link #autowire}提供的功能的超集，增加了{@link #initializeBean}行为。
	 * </p>
	 *
	 * @param beanClass 要创建的bean的类
	 * @param autowireMode 按名称或类型，使用此接口中的常量
	 * @param dependencyCheck 是否对对象执行依赖检查（不适用于构造函数的自动装配，因此在那里被忽略）
	 * @return 新的bean实例
	 * @throws BeansException 如果实例化或装配失败
	 * @see #AUTOWIRE_NO
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_CONSTRUCTOR
	 */
	Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;

	/**
	 * 使用指定的自动装配策略实例化给定类的新bean实例。
	 * 此接口定义的所有常量在此处均受支持。
	 * 也可以使用{@code AUTOWIRE_NO}调用，以便仅应用实例化前回调（例如，用于注解驱动的注入）。
	 * <p>
	 *     <i>不</i>应用标准的{@link BeanPostProcessor BeanPostProcessors}回调或执行bean的任何进一步初始化。
	 *     此接口为这些目的提供了不同的细粒度操作，例如：{@link #initializeBean}。
	 *     但是，如果适用于实例的构建，则会应用{@link InstantiationAwareBeanPostProcessor}回调。
	 * </p>
	 *
	 * @param beanClass 要实例化的bean的类
	 * @param autowireMode 按名称或类型，使用此接口中的常量
	 * @param dependencyCheck 是否对bean实例中的对象引用执行依赖检查（不适用于构造函数的自动装配，因此在那里被忽略）
	 * @return 新的bean实例
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
	 * 按名称或类型自动装配给定bean实例的bean属性。
	 * 也可以使用{@code AUTOWIRE_NO}调用，以便仅应用实例化后回调（例如，用于注解驱动的注入）。
	 * <p>
	 *     <i>不</i>应用标准的{@link BeanPostProcessor BeanPostProcessors}回调或执行bean的任何进一步初始化。
	 *     此接口为这些目的提供了不同的细粒度操作，例如：{@link #initializeBean}。
	 *     但是，如果适用于实例的配置，则会应用{@link InstantiationAwareBeanPostProcessor}回调。
	 * </p>
	 *
	 * @param existingBean 现有的bean实例
	 * @param autowireMode 按名称或类型，使用此接口中的常量
	 * @param dependencyCheck 是否对bean实例中的对象引用执行依赖检查
	 * @throws BeansException 如果装配失败
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_NO
	 */
	void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck)
			throws BeansException;

	/**
	 * 将给定名称的bean定义中的属性值应用到给定的bean实例。
	 * bean定义可以定义一个完全自包含的bean，重用其属性值，也可以定义仅用于现有bean实例的属性值。
	 * <p>此方法<i>不</i>自动装配bean属性；它仅应用明确定义的属性值。
	 * 使用{@link #autowireBeanProperties}方法来自动装配现有的bean实例。
	 * <b>注意：此方法需要给定名称的bean定义！</b>
	 * <p>
	 *     <i>不</i>应用标准的{@link BeanPostProcessor BeanPostProcessors}回调或执行bean的任何进一步初始化。
	 *     此接口为这些目的提供了不同的细粒度操作，例如：{@link #initializeBean}。
	 *     但是，如果适用于实例的配置，则会应用{@link InstantiationAwareBeanPostProcessor}回调。
	 * </p>
	 *
	 * @param existingBean 现有的bean实例
	 * @param beanName bean工厂中bean定义的名称（必须存在该名称的bean定义）
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException 如果没有给定名称的bean定义
	 * @throws BeansException 如果应用属性值失败
	 * @see #autowireBeanProperties
	 */
	void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException;

	/**
	 * 初始化给定的原始bean，应用工厂回调（例如{@code setBeanName}和{@code setBeanFactory}），
	 * 并应用所有bean后处理器（包括可能包装给定原始bean的后处理器）。
	 * <p>注意，bean工厂中不必存在给定名称的bean定义。
	 * 传入的bean名称将仅用于回调，而不与已注册的bean定义进行核对。
	 * @param existingBean 现有的bean实例
	 * @param beanName bean的名称，如有必要将传递给bean（仅传递给{@link BeanPostProcessor BeanPostProcessors}）
	 * @return 要使用的bean实例，可能是原始实例或包装后的实例
	 * @throws BeansException 如果初始化失败
	 */
	Object initializeBean(Object existingBean, String beanName) throws BeansException;

	/**
	 * 将{@link BeanPostProcessor BeanPostProcessors}应用于给定的现有bean实例，
	 * 调用它们的{@code postProcessBeforeInitialization}方法。
	 * 返回的bean实例可能是原始实例的包装器。
	 * @param existingBean 现有的bean实例
	 * @param beanName bean的名称，如有必要将传递给bean（仅传递给{@link BeanPostProcessor BeanPostProcessors}）
	 * @return 要使用的bean实例，可能是原始实例或包装后的实例
	 * @throws BeansException 如果任何后处理失败
	 * @see BeanPostProcessor#postProcessBeforeInitialization
	 */
	Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
			throws BeansException;

	/**
	 * 将{@link BeanPostProcessor BeanPostProcessors}应用于给定的现有bean实例，
	 * 调用它们的{@code postProcessAfterInitialization}方法。
	 * 返回的bean实例可能是原始实例的包装器。
	 * @param existingBean 现有的bean实例
	 * @param beanName bean的名称，如有必要将传递给bean（仅传递给{@link BeanPostProcessor BeanPostProcessors}）
	 * @return 要使用的bean实例，可能是原始实例或包装后的实例
	 * @throws BeansException 如果任何后处理失败
	 * @see BeanPostProcessor#postProcessAfterInitialization
	 */
	Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
			throws BeansException;

	/**
	 * 销毁给定的bean实例（通常来自{@link #createBean}），
	 * 应用{@link org.springframework.beans.factory.DisposableBean}契约
	 * 以及已注册的{@link DestructionAwareBeanPostProcessor DestructionAwareBeanPostProcessors}。
	 * <p>在销毁过程中出现的任何异常都应该被捕获并记录，而不是传播给此方法的调用者。
	 * @param existingBean 要销毁的bean实例
	 */
	void destroyBean(Object existingBean);


	//-------------------------------------------------------------------------
	// Delegate methods for resolving injection points
	// 用于解析注入点的委托方法
	//-------------------------------------------------------------------------

	/**
	 * 解析与给定对象类型唯一匹配的bean实例（如果存在），包括其bean名称。
	 * <p>这实际上是{@link #getBean(Class)}方法的一个变体，保留了匹配实例的bean名称。
	 * @param requiredType bean必须匹配的类型；可以是接口或超类
	 * @return bean名称和bean实例
	 * @throws NoSuchBeanDefinitionException 如果未找到匹配的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个匹配的bean
	 * @throws BeansException 如果无法创建bean
	 * @since 4.3.3
	 * @see #getBean(Class)
	 */
	<T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException;

	/**
	 * 根据工厂中定义的bean，确定指定的依赖对象。
	 * @param descriptor 依赖的描述符（字段/方法/构造函数）
	 * @param requestingBeanName 声明给定依赖的bean的名称
	 * @return 解析后的对象，如果未找到则返回{@code null}
	 * @throws NoSuchBeanDefinitionException 如果未找到匹配的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个匹配的bean
	 * @throws BeansException 如果由于其他原因导致依赖解析失败
	 * @since 2.5
	 * @see #resolveDependency(DependencyDescriptor, String, Set, TypeConverter)
	 */
	@Nullable
	Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName) throws BeansException;

	/**
	 * 根据工厂中定义的bean，确定指定的依赖对象。
	 * @param descriptor 依赖的描述符（字段/方法/构造函数）
	 * @param requestingBeanName 声明给定依赖的bean的名称
	 * @param autowiredBeanNames 一个Set，所有自动装配的bean名称（用于解析给定依赖）都应添加到其中
	 * @param typeConverter 用于填充数组和集合的TypeConverter
	 * @return 解析后的对象，如果未找到则返回{@code null}
	 * @throws NoSuchBeanDefinitionException 如果未找到匹配的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个匹配的bean
	 * @throws BeansException 如果由于其他原因导致依赖解析失败
	 * @since 2.5
	 * @see DependencyDescriptor
	 */
	@Nullable
	Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException;

}
