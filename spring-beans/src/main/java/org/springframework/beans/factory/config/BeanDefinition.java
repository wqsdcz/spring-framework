/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.AttributeAccessor;
import org.springframework.lang.Nullable;

/**
 * <p>BeanDefinition 描述了一个bean实例，具有哪些属性值、哪些构造参数值、以及具体实现提供的扩展信息。
 * <p>此接口仅定义了最小化约定：其主要目的是允许{@link BeanFactoryPostProcessor}实现类（例如{@link PropertyPlaceholderConfigurer}）对属性值及其他bean元数据进行内省和修改。
 *
 * A BeanDefinition describes a bean instance, which has property values,
 * constructor argument values, and further information supplied by
 * concrete implementations.
 *
 * <p>This is just a minimal interface: The main intention is to allow a
 * {@link BeanFactoryPostProcessor} such as {@link PropertyPlaceholderConfigurer}
 * to introspect and modify property values and other bean metadata.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 19.03.2004
 * @see ConfigurableListableBeanFactory#getBeanDefinition
 * @see org.springframework.beans.factory.support.RootBeanDefinition
 * @see org.springframework.beans.factory.support.ChildBeanDefinition
 */
public interface BeanDefinition extends AttributeAccessor, BeanMetadataElement {

	/**
	 * <p>标准单例作用域的作用域标识符：“singleton”。
	 * <p>注意，扩展的bean工厂可能支持更多的作用域。
	 * @see #setScope
	 */
	String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;

	/**
	 * <p>标准原型作用域的作用域标识符：“prototype”。
	 * <p>注意，扩展的bean工厂可能支持更多的作用域。
	 * @see #setScope
	 */
	String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;


	/**
	 * 角色提示：表明该{@code BeanDefinition}是应用程序的主要组成部分。通常对应于用户自定义的bean。
	 */
	int ROLE_APPLICATION = 0;

	/**
	 * 角色提示：表明该{@code BeanDefinition}是某个更大配置（通常是外层的{@link org.springframework.beans.factory.parsing.ComponentDefinition}）的辅助组成部分。
	 * 当深入查看特定{@link org.springframework.beans.factory.parsing.ComponentDefinition}时，{@code SUPPORT}类型的bean具有重要参考价值；
	 * 但在审视应用程序整体配置时，则无需重点关注此类bean。
	 *
	 * Role hint indicating that a {@code BeanDefinition} is a supporting
	 * part of some larger configuration, typically an outer
	 * {@link org.springframework.beans.factory.parsing.ComponentDefinition}.
	 * {@code SUPPORT} beans are considered important enough to be aware
	 * of when looking more closely at a particular
	 * {@link org.springframework.beans.factory.parsing.ComponentDefinition},
	 * but not when looking at the overall configuration of an application.
	 */
	int ROLE_SUPPORT = 1;

	/**
	 * 角色提示：表明该{@code BeanDefinition}仅承担后台支持职能，与终端用户完全无关。
	 * 此提示用于注册那些完全属于{@link org.springframework.beans.factory.parsing.ComponentDefinition}内部运作机制的bean。
	 *
	 * Role hint indicating that a {@code BeanDefinition} is providing an
	 * entirely background role and has no relevance to the end-user. This hint is
	 * used when registering beans that are completely part of the internal workings
	 * of a {@link org.springframework.beans.factory.parsing.ComponentDefinition}.
	 */
	int ROLE_INFRASTRUCTURE = 2;


	// 可更改的attributes

	/**
	 * 设置此bean定义的父定义的名称（如果有的话）
	 */
	void setParentName(@Nullable String parentName);

	/**
	 * 返回此bean定义的父定义的名称（如果有的话）
	 */
	@Nullable
	String getParentName();

	/**
	 * <p>请指定此 bean 定义所对应的 bean 类的名称。
	 * <p>在 bean 工厂的后处理过程中，该类名可以进行修改，通常会将原始类名替换为对其进行解析后的变体形式。
	 *
	 * @see #setParentName
	 * @see #setFactoryBeanName
	 * @see #setFactoryMethodName
	 */
	void setBeanClassName(@Nullable String beanClassName);

	/**
	 * <p>返回此bean定义当前的bean类名。
	 * <p>请注意：由于子定义可能覆盖/继承父定义的类名，因此该返回值不一定是运行时实际使用的类名。
	 * 此外，这可能仅是被调用的工厂方法(静态方法)所在类的名称，在工厂bean引用一个被调用方法(实例方法)的场景下，甚至可能是空值。
	 * 因此，请勿将此值视为运行时的最终bean类型，而应仅将其用于单个bean定义层面的解析用途。
	 *
	 * Return the current bean class name of this bean definition.
	 * <p>Note that this does not have to be the actual class name used at runtime, in
	 * case of a child definition overriding/inheriting the class name from its parent.
	 * Also, this may just be the class that a factory method is called on, or it may
	 * even be empty in case of a factory bean reference that a method is called on.
	 * Hence, do <i>not</i> consider this to be the definitive bean type at runtime but
	 * rather only use it for parsing purposes at the individual bean definition level.
	 * @see #getParentName()
	 * @see #getFactoryBeanName()
	 * @see #getFactoryMethodName()
	 */
	@Nullable
	String getBeanClassName();

	/**
	 * 覆盖此bean的目标作用域，指定一个新的作用域名称。
	 *
	 * @see #SCOPE_SINGLETON
	 * @see #SCOPE_PROTOTYPE
	 */
	void setScope(@Nullable String scope);

	/**
	 * 返回此bean当前目标作用域的名称，如果还不知道，则返回{@code null}。
	 */
	@Nullable
	String getScope();

	/**
	 * <p>设置此 bean 是否应进行延迟初始化。
	 * <p>若设置为{@code false}，该bean将在执行单例bean立即初始化的bean工厂启动时被实例化。
	 */
	void setLazyInit(boolean lazyInit);

	/**
	 * <p>返回该 bean 是否应进行延迟初始化，即在启动时不应立即进行实例化。仅适用于单例 bean。
	 */
	boolean isLazyInit();

	/**
	 * 设置此bean所依赖的bean的名称（这些bean必须优先完成初始化）。
	 * bean工厂将确保这些依赖项会先完成初始化。
	 */
	void setDependsOn(@Nullable String... dependsOn);

	/**
	 * 返回此Bean所依赖的Bean的名称
	 */
	@Nullable
	String[] getDependsOn();

	/**
	 * <p>设置此bean是否可作为自动装配候选对象注入到其他bean中。
	 * <p>请注意：该标志仅影响基于类型的自动装配。它不会影响按名称的显式引用——即使指定bean未被标记为自动装配候选对象，按名称的显式引用仍会被正常解析。
	 * 其结果是，只要名称匹配，按名称自动装配仍会注入相应的bean。
	 */
	void setAutowireCandidate(boolean autowireCandidate);

	/**
	 * 返回此bean是否可作为自动装配候选对象注入到其他bean中。
	 */
	boolean isAutowireCandidate();

	/**
	 * <p>设置此bean是否作为首选的自动装配候选对象。
	 * <p>若在多个匹配的候选bean中有且仅有一个bean的该值为{@code true}， 该bean将作为自动装配的优先选择（决胜因素）。
	 */
	void setPrimary(boolean primary);

	/**
	 * 返回此bean是否作为首选的自动装配候选对象。
	 * Return whether this bean is a primary autowire candidate.
	 */
	boolean isPrimary();

	/**
	 * 指定要使用的工厂bean（如果有的话）。
	 * 即调用指定工厂方法时所依托的bean的名称。
	 * @see #setFactoryMethodName
	 */
	void setFactoryBeanName(@Nullable String factoryBeanName);

	/**
	 * 返回工厂Bean的名称（如果有的话）。
	 */
	@Nullable
	String getFactoryBeanName();

	/**
	 * <p>指定要使用的工厂方法的名称（如果有的话）。该方法可以以携带constructor参数的方式被调用，若未指定参数则采用无参方式调用。
	 * <p>该方法的调用方式取决于配置：当指定了工厂bean时，将在该工厂bean实例上调用；否则将作为本地bean类的静态方法调用。
	 * Specify a factory method, if any. This method will be invoked with
	 * constructor arguments, or with no arguments if none are specified.
	 * The method will be invoked on the specified factory bean, if any,
	 * or otherwise as a static method on the local bean class.
	 * @see #setFactoryBeanName
	 * @see #setBeanClassName
	 */
	void setFactoryMethodName(@Nullable String factoryMethodName);

	/**
	 * 返回工厂方法的名称（如果有的话）。
	 * Return a factory method, if any.
	 */
	@Nullable
	String getFactoryMethodName();

	/**
	 * <p>返回此bean的构造函数参数值。
	 * <p>返回的实例可在bean工厂后处理期间进行修改。
	 * @return ConstructorArgumentValues对象（绝不为{@code null}）
	 *
	 * Return the constructor argument values for this bean.
	 * <p>The returned instance can be modified during bean factory post-processing.
	 * @return the ConstructorArgumentValues object (never {@code null})
	 */
	ConstructorArgumentValues getConstructorArgumentValues();

	/**
	 * 如果为该bean定义了构造函数参数值，则返回。
	 * @since 5.0.2
	 */
	default boolean hasConstructorArgumentValues() {
		return !getConstructorArgumentValues().isEmpty();
	}

	/**
	 * <p>返回将应用于bean的新实例的属性值。
	 * <p>返回的实例可在Bean工厂后处理期间进行修改。
	 * @return MutablePropertyValues对象（绝不为{@code null}）
	 *
	 * Return the property values to be applied to a new instance of the bean.
	 * <p>The returned instance can be modified during bean factory post-processing.
	 * @return the MutablePropertyValues object (never {@code null})
	 */
	MutablePropertyValues getPropertyValues();

	/**
	 * <p>如果存在为该bean定义的property值，则返回。
	 * @since 5.0.2
	 */
	default boolean hasPropertyValues() {
		return !getPropertyValues().isEmpty();
	}


	// 只读的attributes

	/**
	 * Return whether this a <b>Singleton</b>, with a single, shared instance
	 * returned on all calls.
	 * @see #SCOPE_SINGLETON
	 */
	boolean isSingleton();

	/**
	 * Return whether this a <b>Prototype</b>, with an independent instance
	 * returned for each call.
	 * @since 3.0
	 * @see #SCOPE_PROTOTYPE
	 */
	boolean isPrototype();

	/**
	 * Return whether this bean is "abstract", that is, not meant to be instantiated.
	 */
	boolean isAbstract();

	/**
	 * Get the role hint for this {@code BeanDefinition}. The role hint
	 * provides the frameworks as well as tools with an indication of
	 * the role and importance of a particular {@code BeanDefinition}.
	 * @see #ROLE_APPLICATION
	 * @see #ROLE_SUPPORT
	 * @see #ROLE_INFRASTRUCTURE
	 */
	int getRole();

	/**
	 * Return a human-readable description of this bean definition.
	 */
	@Nullable
	String getDescription();

	/**
	 * Return a description of the resource that this bean definition
	 * came from (for the purpose of showing context in case of errors).
	 */
	@Nullable
	String getResourceDescription();

	/**
	 * Return the originating BeanDefinition, or {@code null} if none.
	 * Allows for retrieving the decorated bean definition, if any.
	 * <p>Note that this method returns the immediate originator. Iterate through the
	 * originator chain to find the original BeanDefinition as defined by the user.
	 */
	@Nullable
	BeanDefinition getOriginatingBeanDefinition();

}
