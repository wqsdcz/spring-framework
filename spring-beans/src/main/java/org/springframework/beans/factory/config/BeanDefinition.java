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

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * BeanDefinition 描述了一个 bean 实例，它具有属性值、构造函数参数值，
 * 以及具体实现提供的进一步信息。
 *
 * <p>这只是一个最小接口：主要目的是允许{@link BeanFactoryPostProcessor} 内省和修改属性值以及其他 bean 元数据。
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
	 * 标准单例作用域的作用域标识符：{@value}。
	 * <p>请注意，扩展的 bean 工厂可能支持更多的作用域。
	 * <p>请注意，扩展的 bean 工厂可能支持更多的作用域。
	 * @see #setScope
	 * @see ConfigurableBeanFactory#SCOPE_SINGLETON
	 */
	String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;

	/**
	 * 标准原型作用域的作用域标识符：{@value}。
	 * <p>请注意，扩展的 bean 工厂可能支持更多的作用域。
	 * @see #setScope
	 * @see ConfigurableBeanFactory#SCOPE_PROTOTYPE
	 */
	String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;


	/**
	 * 角色提示，表明 {@code BeanDefinition} 是应用程序的主要部分。
	 * 通常对应于用户定义的 bean。
	 */
	int ROLE_APPLICATION = 0;

	/**
	 * 角色提示，表明 {@code BeanDefinition} 是某些较大配置的支持部分，
	 * 通常是外部的 {@link org.springframework.beans.factory.parsing.ComponentDefinition}。
	 * {@code SUPPORT} bean 在更仔细查看特定的{@link org.springframework.beans.factory.parsing.ComponentDefinition} 时被认为足够重要，
	 * 但在查看应用程序的整体配置时不考虑。
	 */
	int ROLE_SUPPORT = 1;

	/**
	 * 角色提示，表明 {@code BeanDefinition} 提供完全的后台角色，对最终用户没有相关性。
	 * 当注册完全属于{@link org.springframework.beans.factory.parsing.ComponentDefinition} 内部工作的 bean 时使用此提示。
	 */
	int ROLE_INFRASTRUCTURE = 2;


	// 可修改的属性

	/**
	 * 设置此 bean 定义的父定义的名称（如果有）。
	 */
	void setParentName(@Nullable String parentName);

	/**
	 * 返回此 bean 定义的父定义的名称（如果有）。
	 */
	@Nullable
	String getParentName();

	/**
	 * 指定此 bean 定义的 bean 类名。
	 * <p>
	 *     类名可以在 bean 工厂后处理期间修改，通常用解析后的变体替换原始类名。
	 * @see #setParentName
	 * @see #setFactoryBeanName
	 * @see #setFactoryMethodName
	 */
	void setBeanClassName(@Nullable String beanClassName);

	/**
	 * 返回此 bean 定义的当前 bean 类名。
	 * <p>请注意，这不一定是运行时使用的实际类名，
	 * 特别是在子定义覆盖/继承其父类的类名时。
	 * 此外，这可能只是调用工厂方法的类，或者在调用方法的工厂 bean 引用的情况下甚至可能是空的。
	 * 因此，<i>不要</i>将其视为运行时的确定 bean 类型，
	 * 而只在单个 bean 定义级别用于解析目的。
	 * @see #getParentName()
	 * @see #getFactoryBeanName()
	 * @see #getFactoryMethodName()
	 */
	@Nullable
	String getBeanClassName();

	/**
	 * 覆盖此 bean 的目标作用域，指定新的作用域名称。
	 * @see #SCOPE_SINGLETON
	 * @see #SCOPE_PROTOTYPE
	 */
	void setScope(@Nullable String scope);

	/**
	 * 返回此 bean 的当前目标作用域名称，
	 * 如果尚不知道则返回 {@code null}。
	 */
	@Nullable
	String getScope();

	/**
	 * 设置此 bean 是否应该延迟初始化。
	 * <p>如果 {@code false}，执行单例急切初始化的 bean 工厂将在启动时实例化该 bean。
	 */
	void setLazyInit(boolean lazyInit);

	/**
	 * 返回此 bean 是否应该延迟初始化，即不在启动时急切实例化。
	 * 仅适用于单例 bean。
	 */
	boolean isLazyInit();

	/**
	 * 设置此 bean 依赖于其初始化的 bean 名称。
	 * bean 工厂将保证这些 bean 首先被初始化。
	 * <p>请注意，依赖关系通常通过 bean 属性或构造函数参数来表达。
	 * 此属性应该只在其他类型的依赖关系（如静态变量(*ugh*)或启动时的数据库准备）时才必要。
	 */
	void setDependsOn(@Nullable String... dependsOn);

	/**
	 * 返回此 bean 依赖的 bean 名称。
	 */
	@Nullable
	String[] getDependsOn();

	/**
	 * 设置此 bean 是否是自动装配到其他 bean 中的候选者。
	 * <p>请注意，此标志旨在仅影响基于类型的自动装配。
	 * 它不影响按名称的显式引用，即使指定的 bean 未标记为自动装配候选者也会被解析。
	 * 因此，按名称自动装配仍将注入 bean（如果名称匹配）。
	 */
	void setAutowireCandidate(boolean autowireCandidate);

	/**
	 * 返回此 bean 是否是自动装配到其他 bean 中的候选者。
	 */
	boolean isAutowireCandidate();

	/**
	 * 设置此 bean 是否为主要的自动装配候选者。
	 * <p>如果在多个匹配候选者中恰好有一个 bean 的此值为 {@code true}，
	 * 它将作为决胜者。
	 * @see #setFallback
	 */
	void setPrimary(boolean primary);

	/**
	 * 返回此 bean 是否为主要的自动装配候选者。
	 */
	boolean isPrimary();

	/**
	 * 设置此 bean 是否为备用自动装配候选者。
	 * <p>如果在多个匹配候选者中除一个外的所有 bean 的此值都为 {@code true}，
	 * 将选择剩余的 bean。
	 * @since 6.2
	 * @see #setPrimary
	 */
	void setFallback(boolean fallback);

	/**
	 * 返回此 bean 是否为备用自动装配候选者。
	 * @since 6.2
	 */
	boolean isFallback();

	/**
	 * 指定要使用的工厂 bean（如果有）。
	 * 这是要调用指定工厂方法的 bean 名称。
	 * <p>工厂 bean 名称仅对基于实例的工厂方法是必要的。
	 * 对于静态工厂方法，将从 bean 类派生方法。
	 * @see #setFactoryMethodName
	 * @see #setBeanClassName
	 */
	void setFactoryBeanName(@Nullable String factoryBeanName);

	/**
	 * 返回工厂 bean 名称（如果有）。
	 * <p>对于将从 bean 类派生的静态工厂方法，这将是 {@code null}。
	 * @see #getFactoryMethodName()
	 * @see #getBeanClassName()
	 */
	@Nullable
	String getFactoryBeanName();

	/**
	 * 指定工厂方法（如果有）。此方法将使用构造函数参数调用，
	 * 或者如果没有指定参数则不带参数调用。
	 * 该方法将在指定的工厂 bean 上调用（如果有），
	 * 或者否则作为本地 bean 类上的静态方法调用。
	 * @see #setFactoryBeanName
	 * @see #setBeanClassName
	 */
	void setFactoryMethodName(@Nullable String factoryMethodName);

	/**
	 * 返回工厂方法（如果有）。
	 * @see #getFactoryBeanName()
	 * @see #getBeanClassName()
	 */
	@Nullable
	String getFactoryMethodName();

	/**
	 * 返回此 bean 的构造函数参数值。
	 * <p>返回的实例可以在 bean 工厂后处理期间修改。
	 * @return ConstructorArgumentValues 对象（永不为 {@code null}）
	 */
	ConstructorArgumentValues getConstructorArgumentValues();

	/**
	 * 返回是否为此 bean 定义了构造函数参数值。
	 * @since 5.0.2
	 * @see #getConstructorArgumentValues()
	 */
	default boolean hasConstructorArgumentValues() {
		return !getConstructorArgumentValues().isEmpty();
	}

	/**
	 * 返回要应用于 bean 新实例的属性值。
	 * <p>返回的实例可以在 bean 工厂后处理期间修改。
	 * @return MutablePropertyValues 对象（永不为 {@code null}）
	 */
	MutablePropertyValues getPropertyValues();

	/**
	 * 返回是否为此 bean 定义了属性值。
	 * @since 5.0.2
	 * @see #getPropertyValues()
	 */
	default boolean hasPropertyValues() {
		return !getPropertyValues().isEmpty();
	}

	/**
	 * 设置初始化方法的名称。
	 * @since 5.1
	 */
	void setInitMethodName(@Nullable String initMethodName);

	/**
	 * 返回初始化方法的名称。
	 * @since 5.1
	 */
	@Nullable
	String getInitMethodName();

	/**
	 * 设置销毁方法的名称。
	 * @since 5.1
	 */
	void setDestroyMethodName(@Nullable String destroyMethodName);

	/**
	 * 返回销毁方法的名称。
	 * @since 5.1
	 */
	@Nullable
	String getDestroyMethodName();

	/**
	 * 设置此 {@code BeanDefinition} 的角色提示。
	 * 角色提示为框架以及工具提供了特定 {@code BeanDefinition} 的角色和重要性的指示。
	 * @since 5.1
	 * @see #ROLE_APPLICATION
	 * @see #ROLE_SUPPORT
	 * @see #ROLE_INFRASTRUCTURE
	 */
	void setRole(int role);

	/**
	 * 获取此 {@code BeanDefinition} 的角色提示。
	 * 角色提示为框架以及工具提供了特定 {@code BeanDefinition} 的角色和重要性的指示。
	 * @see #ROLE_APPLICATION
	 * @see #ROLE_SUPPORT
	 * @see #ROLE_INFRASTRUCTURE
	 */
	int getRole();

	/**
	 * 设置此 bean 定义的人类可读描述。
	 * @since 5.1
	 */
	void setDescription(@Nullable String description);

	/**
	 * 返回此 bean 定义的人类可读描述。
	 */
	@Nullable
	String getDescription();


	// 只读属性

	/**
	 * 基于 bean 类或其他特定元数据返回此 bean 定义的可解析类型。
	 * <p>这通常在运行时合并的 bean 定义上完全解析，但在配置时定义实例上不一定如此。
	 * @return 可解析类型（可能是 {@link ResolvableType#NONE}）
	 * @since 5.2
	 * @see ConfigurableBeanFactory#getMergedBeanDefinition
	 */
	ResolvableType getResolvableType();

	/**
	 * 返回这是否是一个 <b>单例</b>，在所有调用中返回单个共享实例。
	 * @see #SCOPE_SINGLETON
	 */
	boolean isSingleton();

	/**
	 * 返回这是否是一个 <b>原型</b>，每次调用返回独立实例。
	 * @since 3.0
	 * @see #SCOPE_PROTOTYPE
	 */
	boolean isPrototype();

	/**
	 * 返回此 bean 是否是"抽象的"，即不打算自己实例化，
	 * 而只是作为具体子 bean 定义的父级。
	 */
	boolean isAbstract();

	/**
	 * 返回此 bean 定义来源的资源描述（用于在出现错误时显示上下文）。
	 */
	@Nullable
	String getResourceDescription();

	/**
	 * 返回原始的 BeanDefinition，如果没有则返回 {@code null}。
	 * <p>允许检索装饰的 bean 定义（如果有）。
	 * <p>请注意，此方法返回直接的原始者。遍历原始者链以找到用户定义的原始 BeanDefinition。
	 */
	@Nullable
	BeanDefinition getOriginatingBeanDefinition();

}
