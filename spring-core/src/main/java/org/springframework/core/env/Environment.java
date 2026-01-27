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

package org.springframework.core.env;

/**
 * Interface representing the environment in which the current application is running.
 * Models two key aspects of the application environment: <em>profiles</em> and
 * <em>properties</em>. Methods related to property access are exposed via the
 * {@link PropertyResolver} superinterface.
 * <p>这个接口表示当前【应用程序运行环境】。它对【应用程序运行环境】的两个关键方面进行了建模：<em>profiles</em>和
 * <em>properties</em>。属性访问相关的方法 是 通过{@link PropertyResolver}父接口暴露的。
 *
 * <p>A <em>profile</em> is a named, logical group of bean definitions to be registered
 * with the container only if the given profile is <em>active</em>. Beans may be assigned
 * to a profile whether defined in XML or via annotations; see the spring-beans 3.1 schema
 * or the {@link org.springframework.context.annotation.Profile @Profile} annotation for
 * syntax details. The role of the {@code Environment} object with relation to profiles is
 * in determining which profiles (if any) are currently {@linkplain #getActiveProfiles
 * active}, and which profiles (if any) should be {@linkplain #getDefaultProfiles active
 * by default}.
 * <p><em>profile</em> 是bean定义的一个带名称的逻辑分组，只有当指定的profile处于<em>active</em> 状态时，
 * Bean才会被注册到容器中。无论是在XML中定义Bean，还是通过注解定义Bean，都可以将其分配到某个profile下；
 * 有关语法详细信息，请参见spring-beans 3.1模式或{@link org.springframework.context.annotation.Profile @Profile}注解。
 * {@code Environment}对象与profiles相关的作用是确定哪些profiles（如果有）当前处于
 * {@linkplain #getActiveProfiles active}状态，以及哪些profiles（如果有）应该
 * 默认处于{@linkplain #getDefaultProfiles active}状态。
 *
 * <p><em>Properties</em> play an important role in almost all applications, and may
 * originate from a variety of sources: properties files, JVM system properties, system
 * environment variables, JNDI, servlet context parameters, ad-hoc Properties objects,
 * Maps, and so on. The role of the {@code Environment} object with relation to properties
 * is to provide the user with a convenient service interface for configuring property
 * sources and resolving properties from them.
 * <p><em>Properties</em> 在几乎所有应用中都扮演着重要角色，它们可能来自各种来源：属性文件、JVM系统属性、
 * 系统环境变量、JNDI、servlet上下文参数、临时Properties对象、Maps等等。{@code Environment}对象
 * 与属性相关的作用是为用户提供一个便捷的服务接口，用于配置属性源并从中解析属性。
 *
 * <p>Beans managed within an {@code ApplicationContext} may register to be {@link
 * org.springframework.context.EnvironmentAware EnvironmentAware} or {@code @Inject} the
 * {@code Environment} in order to query profile state or resolve properties directly.
 * <p>在{@code ApplicationContext}中管理的Bean可以注册为{@link
 * org.springframework.context.EnvironmentAware EnvironmentAware}或使用
 * {@code @Inject}注入{@code Environment}以便直接查询profile状态或解析属性。
 *
 * <p>In most cases, however, application-level beans should not need to interact with the
 * {@code Environment} directly but instead may request to have {@code ${...}} property
 * values replaced by a property placeholder configurer such as
 * {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * PropertySourcesPlaceholderConfigurer}, which itself is {@code EnvironmentAware} and
 * registered by default when using {@code <context:property-placeholder/>}.
 * <p>然而，在大多数情况下，应用程序级的Bean不需要直接与{@code Environment}交互，而是可以请求
 * 通过属性占位符配置器（如{@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * PropertySourcesPlaceholderConfigurer}）替换{@code ${...}}属性值，该配置器本身是
 * {@code EnvironmentAware}，并在使用{@code <context:property-placeholder/>}时默认注册。
 *
 * <p>Configuration of the {@code Environment} object must be done through the
 * {@code ConfigurableEnvironment} interface, returned from all
 * {@code AbstractApplicationContext} subclass {@code getEnvironment()} methods. See
 * {@link ConfigurableEnvironment} Javadoc for usage examples demonstrating manipulation
 * of property sources prior to application context {@code refresh()}.
 * <p>{@code Environment}对象的配置必须通过{@code ConfigurableEnvironment}接口完成，该接口
 * 由所有{@code AbstractApplicationContext}子类的{@code getEnvironment()}方法返回。有关在应用
 * 上下文{@code refresh()}之前操作属性源的使用示例，请参见{@link ConfigurableEnvironment} Javadoc。
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.1
 * @see PropertyResolver
 * @see EnvironmentCapable
 * @see ConfigurableEnvironment
 * @see AbstractEnvironment
 * @see StandardEnvironment
 * @see org.springframework.context.EnvironmentAware
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#setEnvironment
 * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
 */
public interface Environment extends PropertyResolver {

	/**
	 * Return the set of profiles explicitly made active for this environment. Profiles
	 * are used for creating logical groupings of bean definitions to be registered
	 * conditionally, for example based on deployment environment. Profiles can be
	 * activated by setting {@linkplain AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 * "spring.profiles.active"} as a system property or by calling
	 * {@link ConfigurableEnvironment#setActiveProfiles(String...)}.
	 * <p>返回为此环境显式激活的profile集合。Profiles用于创建bean定义的逻辑分组，
	 * 这些bean定义可以有条件地注册，例如基于部署环境。可以通过设置系统属性
	 * {@linkplain AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME "spring.profiles.active"}
	 * 或调用{@link ConfigurableEnvironment#setActiveProfiles(String...)}来激活profiles。
	 * <p>If no profiles have explicitly been specified as active, then any
	 * {@linkplain #getDefaultProfiles() default profiles} will automatically be activated.
	 * <p>如果没有显式指定任何激活的profiles，那么任何{@linkplain #getDefaultProfiles() 默认profiles}
	 * 将自动被激活。
	 * @see #getDefaultProfiles
	 * @see ConfigurableEnvironment#setActiveProfiles
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	String[] getActiveProfiles();

	/**
	 * Return the set of profiles to be active by default when no active profiles have
	 * been set explicitly.
	 * <p>当没有显式设置激活的profiles时，返回默认激活的profile集合。
	 * @see #getActiveProfiles
	 * @see ConfigurableEnvironment#setDefaultProfiles
	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
	 */
	String[] getDefaultProfiles();

	/**
	 * Determine whether one of the given profile expressions matches the
	 * {@linkplain #getActiveProfiles() active profiles} &mdash; or in the case
	 * of no explicit active profiles, whether one of the given profile expressions
	 * matches the {@linkplain #getDefaultProfiles() default profiles}.
	 * <p>判断给定的profile表达式之一是否匹配{@linkplain #getActiveProfiles() 激活的profiles}——
	 * 或者在没有显式激活profiles的情况下，判断给定的profile表达式之一是否匹配
	 * {@linkplain #getDefaultProfiles() 默认profiles}。
	 * <p>Profile expressions allow for complex, boolean profile logic to be
	 * expressed &mdash; for example {@code "p1 & p2"}, {@code "(p1 & p2) | p3"},
	 * etc. See {@link Profiles#of(String...)} for details on the supported
	 * expression syntax.
	 * <p>Profile表达式允许表达复杂的布尔profile逻辑——例如{@code "p1 & p2"}、{@code "(p1 & p2) | p3"}等。
	 * 有关支持的表达式语法详细信息，请参见{@link Profiles#of(String...)}。
	 * <p>This method is a convenient shortcut for
	 * {@code env.acceptsProfiles(Profiles.of(profileExpressions))}.
	 * <p>此方法是{@code env.acceptsProfiles(Profiles.of(profileExpressions))}的便捷快捷方式。
	 * @since 5.3.28
	 * @see Profiles#of(String...)
	 * @see #acceptsProfiles(Profiles)
	 */
	default boolean matchesProfiles(String... profileExpressions) {
		return acceptsProfiles(Profiles.of(profileExpressions));
	}

	/**
	 * Determine whether one or more of the given profiles is active &mdash; or
	 * in the case of no explicit {@linkplain #getActiveProfiles() active profiles},
	 * whether one or more of the given profiles is included in the set of
	 * {@linkplain #getDefaultProfiles() default profiles}.
	 * <p>判断给定的一个或多个profile是否处于激活状态——或者在没有显式
	 * {@linkplain #getActiveProfiles() 激活profiles}的情况下，判断给定的一个或多个profile
	 * 是否包含在{@linkplain #getDefaultProfiles() 默认profiles}集合中。
	 * <p>If a profile begins with '!' the logic is inverted, meaning this method
	 * will return {@code true} if the given profile is <em>not</em> active. For
	 * example, {@code env.acceptsProfiles("p1", "!p2")} will return {@code true}
	 * if profile 'p1' is active or 'p2' is not active.
	 * <p>如果profile以'!'开头，则逻辑会反转，意味着如果给定的profile<em>不</em>处于激活状态，
	 * 此方法将返回{@code true}。例如，如果profile 'p1'处于激活状态或'p2'不处于激活状态，
	 * {@code env.acceptsProfiles("p1", "!p2")}将返回{@code true}。
	 * @throws IllegalArgumentException if called with a {@code null} array, an
	 * empty array, zero arguments or if any profile is {@code null}, empty, or
	 * whitespace only
	 * <p>如果使用{@code null}数组、空数组、零参数调用，或者任何profile为{@code null}、空或仅包含空白字符时抛出
	 * @see #getActiveProfiles
	 * @see #getDefaultProfiles
	 * @see #matchesProfiles(String...)
	 * @see #acceptsProfiles(Profiles)
	 * @deprecated as of 5.1 in favor of {@link #acceptsProfiles(Profiles)} or
	 * {@link #matchesProfiles(String...)}
	 * <p>自5.1版本起弃用，推荐使用{@link #acceptsProfiles(Profiles)}或{@link #matchesProfiles(String...)}
	 */
	@Deprecated
	boolean acceptsProfiles(String... profiles);

	/**
	 * Determine whether the given {@link Profiles} predicate matches the
	 * {@linkplain #getActiveProfiles() active profiles} &mdash; or in the case
	 * of no explicit active profiles, whether the given {@code Profiles} predicate
	 * matches the {@linkplain #getDefaultProfiles() default profiles}.
	 * <p>判断给定的{@link Profiles}谓词是否匹配{@linkplain #getActiveProfiles() 激活的profiles}——
	 * 或者在没有显式激活profiles的情况下，判断给定的{@code Profiles}谓词是否匹配
	 * {@linkplain #getDefaultProfiles() 默认profiles}。
	 * <p>If you wish provide profile expressions directly as strings, use
	 * {@link #matchesProfiles(String...)} instead.
	 * <p>如果您希望直接以字符串形式提供profile表达式，请使用{@link #matchesProfiles(String...)}代替。
	 * @since 5.1
	 * @see #matchesProfiles(String...)
	 * @see Profiles#of(String...)
	 */
	boolean acceptsProfiles(Profiles profiles);

}
