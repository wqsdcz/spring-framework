/*
 * Copyright 2002-2016 the original author or authors.
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
 * <p>
 *     该接口代表当前应用程序运行所处的环境。
 *     它模拟了应用程序环境的两个关键方面：<b>profiles</b> 和 <b>properties</b>。
 *	   与属性访问相关的方法通过{@link PropertyResolver}超接口进行公开。
 * </p>
 * <p/>
 * <p>
 *     每个 profile 都代表一个具有名称且逻辑上相关的 bean 定义集合，只有在指定的 profile 处于“<b>active</b>”状态时，才会被注册到容器中。
 *     无论这些 bean 是通过 XML 定义，还是通过注解定义的，都可以将其分配到某个profile；
 *     有关语法细节，请参阅 spring-beans 3.1 模式 或 {@link org.springframework.context.annotation.Profile @Profile} 注解。
 *     {@code Environment} 对象在与profile的关系中所起的作用在于确定当前哪些 profile（如果有的话）是处于 {@linkplain #getActiveProfiles active状态} ，
 *     以及哪些 profile（如果有的话）是 {@linkplain #getDefaultProfiles 默认active状态}。
 * </p>
 * <p/>
 * <p>
 *     <b>Properties</b>在几乎所有应用中都扮演着重要角色，这些属性可能来自多种来源：
 *     属性文件、JVM 系统属性、系统环境变量、JNDI、Servlet 上下文参数、临时的属性对象、映射等等。
 *     环境对象与属性的关系在于为用户提供一个用于 配置属性源 并 从这些源中解析属性 的便捷服务接口。
 * </p>
 * <p/>
 * <p>
 *     在 {@code ApplicationContext} 中管理的 Bean 可以注册为{@link org.springframework.context.EnvironmentAware EnvironmentAware}
 *     或通过{@code @Inject}注入{@code Environment}对象，从而直接[查询profile状态]或[解析properties]。
 * </p>
 * <p/>
 * <p>
 *     然而在大多数情况下，应用级Bean无需直接与 {@code Environment} 交互，
 *     而是应当通过<b>属性占位符配置器</b>（例如{@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer PropertySourcesPlaceholderConfigurer}）
 *     来替换 {@code ${...}} 属性值。
 *     该配置器本身实现了{@code EnvironmentAware}接口，且从Spring 3.1开始，当使用{@code context:property-placeholder/}配置时会默认注册。
 * </p>
 * <p/>
 * <p>
 *     环境对象的配置必须通过 {@code ConfigurableEnvironment} 接口完成，
 *     该接口由所有{@code AbstractApplicationContext}子类的{@code getEnvironment()}方法返回。
 *     具体使用示例（如：在应用上下文执行{@code refresh()}刷新操作之前如何操作属性源）请参阅{@link ConfigurableEnvironment}的Javadoc文档。
 * </p>
 * <p/>
 * Interface representing the environment in which the current application is running.
 * Models two key aspects of the application environment: <em>profiles</em> and
 * <em>properties</em>. Methods related to property access are exposed via the
 * {@link PropertyResolver} superinterface.
 *
 * <p>A <em>profile</em> is a named, logical group of bean definitions to be registered
 * with the container only if the given profile is <em>active</em>. Beans may be assigned
 * to a profile whether defined in XML or via annotations; see the spring-beans 3.1 schema
 * or the {@link org.springframework.context.annotation.Profile @Profile} annotation for
 * syntax details. The role of the {@code Environment} object with relation to profiles is
 * in determining which profiles (if any) are currently {@linkplain #getActiveProfiles
 * active}, and which profiles (if any) should be {@linkplain #getDefaultProfiles active
 * by default}.
 *
 * <p><em>Properties</em> play an important role in almost all applications, and may
 * originate from a variety of sources: properties files, JVM system properties, system
 * environment variables, JNDI, servlet context parameters, ad-hoc Properties objects,
 * Maps, and so on. The role of the environment object with relation to properties is to
 * provide the user with a convenient service interface for configuring property sources
 * and resolving properties from them.
 *
 * <p>Beans managed within an {@code ApplicationContext} may register to be {@link
 * org.springframework.context.EnvironmentAware EnvironmentAware} or {@code @Inject} the
 * {@code Environment} in order to query profile state or resolve properties directly.
 *
 * <p>In most cases, however, application-level beans should not need to interact with the
 * {@code Environment} directly but instead may have to have {@code ${...}} property
 * values replaced by a property placeholder configurer such as
 * {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * PropertySourcesPlaceholderConfigurer}, which itself is {@code EnvironmentAware} and
 * as of Spring 3.1 is registered by default when using
 * {@code <context:property-placeholder/>}.
 *
 * <p>Configuration of the environment object must be done through the
 * {@code ConfigurableEnvironment} interface, returned from all
 * {@code AbstractApplicationContext} subclass {@code getEnvironment()} methods. See
 * {@link ConfigurableEnvironment} Javadoc for usage examples demonstrating manipulation
 * of property sources prior to application context {@code refresh()}.
 *
 * @author Chris Beams
 * @see PropertyResolver
 * @see EnvironmentCapable
 * @see ConfigurableEnvironment
 * @see AbstractEnvironment
 * @see StandardEnvironment
 * @see org.springframework.context.EnvironmentAware
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#setEnvironment
 * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
 * @since 3.1
 */
public interface Environment extends PropertyResolver {

	/**
	 * Return the set of profiles explicitly made active for this environment. Profiles
	 * are used for creating logical groupings of bean definitions to be registered
	 * conditionally, for example based on deployment environment.  Profiles can be
	 * activated by setting {@linkplain AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 * "spring.profiles.active"} as a system property or by calling
	 * {@link ConfigurableEnvironment#setActiveProfiles(String...)}.
	 * <p>If no profiles have explicitly been specified as active, then any
	 * {@linkplain #getDefaultProfiles() default profiles} will automatically be activated.
	 *
	 * @see #getDefaultProfiles
	 * @see ConfigurableEnvironment#setActiveProfiles
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	String[] getActiveProfiles();

	/**
	 * Return the set of profiles to be active by default when no active profiles have
	 * been set explicitly.
	 *
	 * @see #getActiveProfiles
	 * @see ConfigurableEnvironment#setDefaultProfiles
	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
	 */
	String[] getDefaultProfiles();

	/**
	 * Return whether one or more of the given profiles is active or, in the case of no
	 * explicit active profiles, whether one or more of the given profiles is included in
	 * the set of default profiles. If a profile begins with '!' the logic is inverted,
	 * i.e. the method will return true if the given profile is <em>not</em> active.
	 * For example, <pre class="code">env.acceptsProfiles("p1", "!p2")</pre> will
	 * return {@code true} if profile 'p1' is active or 'p2' is not active.
	 *
	 * @throws IllegalArgumentException if called with zero arguments
	 *                                  or if any profile is {@code null}, empty or whitespace-only
	 * @see #getActiveProfiles
	 * @see #getDefaultProfiles
	 */
	boolean acceptsProfiles(String... profiles);

}
