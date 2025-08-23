/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Map;

/**
 * <p>
 *     该配置接口应该被大多数（即使不是全部）{@link Environment}类型实现。
 *     它提供设置激活配置文件和默认配置文件、操作底层属性源的功能，
 *     并通过其超接口{@link ConfigurablePropertyResolver}支持客户端设置与验证必需属性、定制转换服务等更多功能。
 * </p>
 * <p/>
 * <h2>操控属性源</h2>
 * <p>
 *     属性源可被移除、重新排序或替换；同时可以通过{@link #getPropertySources()}方法返回的{@link MutablePropertySources}实例添加额外的属性源。
 *     以下示例虽然针对{@link StandardEnvironment}对{@code ConfigurableEnvironment}接口的实现，但原则上适用于所有实现——尽管不同实现的默认属性源可能存在差异。
 * </p>
 * <p/>
 * <h4>示例: 添加具有最高搜索优先级的新属性源</h4>
 * <p>
 * <pre class="code">
 * ConfigurableEnvironment environment = new StandardEnvironment();
 * MutablePropertySources propertySources = environment.getPropertySources();
 * Map&lt;String, String&gt; myMap = new HashMap&lt;&gt;();
 * myMap.put("xyz", "myValue");
 * propertySources.addFirst(new MapPropertySource("MY_MAP", myMap));
 * </pre>
 * </p>
 * <p/>
 * <h4>示例: 删除默认的系统属性的属性源</h4>
 * <p>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
 * </pre>
 * </p>
 * <p/>
 * <h4>示例: 模拟系统环境以进行测试</h4>
 * <p>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * MockPropertySource mockEnvVars = new MockPropertySource().withProperty("xyz", "myValue");
 * propertySources.replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, mockEnvVars);
 * </pre>
 * </p>
 * <p/>
 * <p>
 *    当 {@link Environment} 被 {@code ApplicationContext} 使用时，关键点在于所有 {@code PropertySource} 操作都<em>必须</em>在上下文
 *    调用{@link org.springframework.context.support.AbstractApplicationContext#refresh() refresh()}方法<em>之前</em>完成。
 *    这能确保在容器引导启动过程中所有属性源均已就绪可用，包括供
 *    {@linkplain org.springframework.context.support.PropertySourcesPlaceholderConfigurer 属性占位符配置器}使用的场景。
 * </p>
 * <p/>
 * Configuration interface to be implemented by most if not all {@link Environment} types.
 * Provides facilities for setting active and default profiles and manipulating underlying
 * property sources. Allows clients to set and validate required properties, customize the
 * conversion service and more through the {@link ConfigurablePropertyResolver}
 * superinterface.
 *
 * <h2>Manipulating property sources</h2>
 * <p>Property sources may be removed, reordered, or replaced; and additional
 * property sources may be added using the {@link MutablePropertySources}
 * instance returned from {@link #getPropertySources()}. The following examples
 * are against the {@link StandardEnvironment} implementation of
 * {@code ConfigurableEnvironment}, but are generally applicable to any implementation,
 * though particular default property sources may differ.
 *
 * <h4>Example: adding a new property source with highest search priority</h4>
 * <pre class="code">
 * ConfigurableEnvironment environment = new StandardEnvironment();
 * MutablePropertySources propertySources = environment.getPropertySources();
 * Map&lt;String, String&gt; myMap = new HashMap&lt;&gt;();
 * myMap.put("xyz", "myValue");
 * propertySources.addFirst(new MapPropertySource("MY_MAP", myMap));
 * </pre>
 *
 * <h4>Example: removing the default system properties property source</h4>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
 * </pre>
 *
 * <h4>Example: mocking the system environment for testing purposes</h4>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * MockPropertySource mockEnvVars = new MockPropertySource().withProperty("xyz", "myValue");
 * propertySources.replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, mockEnvVars);
 * </pre>
 *
 * When an {@link Environment} is being used by an {@code ApplicationContext}, it is
 * important that any such {@code PropertySource} manipulations be performed
 * <em>before</em> the context's {@link
 * org.springframework.context.support.AbstractApplicationContext#refresh() refresh()}
 * method is called. This ensures that all property sources are available during the
 * container bootstrap process, including use by {@linkplain
 * org.springframework.context.support.PropertySourcesPlaceholderConfigurer property
 * placeholder configurers}.
 *
 * @author Chris Beams
 * @since 3.1
 * @see StandardEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment
 */
public interface ConfigurableEnvironment extends Environment, ConfigurablePropertyResolver {

	/**
	 * Specify the set of profiles active for this {@code Environment}. Profiles are
	 * evaluated during container bootstrap to determine whether bean definitions
	 * should be registered with the container.
	 * <p>Any existing active profiles will be replaced with the given arguments; call
	 * with zero arguments to clear the current set of active profiles. Use
	 * {@link #addActiveProfile} to add a profile while preserving the existing set.
	 * @throws IllegalArgumentException if any profile is null, empty or whitespace-only
	 * @see #addActiveProfile
	 * @see #setDefaultProfiles
	 * @see org.springframework.context.annotation.Profile
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	void setActiveProfiles(String... profiles);

	/**
	 * Add a profile to the current set of active profiles.
	 * @throws IllegalArgumentException if the profile is null, empty or whitespace-only
	 * @see #setActiveProfiles
	 */
	void addActiveProfile(String profile);

	/**
	 * Specify the set of profiles to be made active by default if no other profiles
	 * are explicitly made active through {@link #setActiveProfiles}.
	 * @throws IllegalArgumentException if any profile is null, empty or whitespace-only
	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
	 */
	void setDefaultProfiles(String... profiles);

	/**
	 * Return the {@link PropertySources} for this {@code Environment} in mutable form,
	 * allowing for manipulation of the set of {@link PropertySource} objects that should
	 * be searched when resolving properties against this {@code Environment} object.
	 * The various {@link MutablePropertySources} methods such as
	 * {@link MutablePropertySources#addFirst addFirst},
	 * {@link MutablePropertySources#addLast addLast},
	 * {@link MutablePropertySources#addBefore addBefore} and
	 * {@link MutablePropertySources#addAfter addAfter} allow for fine-grained control
	 * over property source ordering. This is useful, for example, in ensuring that
	 * certain user-defined property sources have search precedence over default property
	 * sources such as the set of system properties or the set of system environment
	 * variables.
	 * @see AbstractEnvironment#customizePropertySources
	 */
	MutablePropertySources getPropertySources();

	/**
	 * Return the value of {@link System#getProperties()} if allowed by the current
	 * {@link SecurityManager}, otherwise return a map implementation that will attempt
	 * to access individual keys using calls to {@link System#getProperty(String)}.
	 * <p>Note that most {@code Environment} implementations will include this system
	 * properties map as a default {@link PropertySource} to be searched. Therefore, it is
	 * recommended that this method not be used directly unless bypassing other property
	 * sources is expressly intended.
	 * <p>Calls to {@link Map#get(Object)} on the Map returned will never throw
	 * {@link IllegalAccessException}; in cases where the SecurityManager forbids access
	 * to a property, {@code null} will be returned and an INFO-level log message will be
	 * issued noting the exception.
	 */
	Map<String, Object> getSystemProperties();

	/**
	 * Return the value of {@link System#getenv()} if allowed by the current
	 * {@link SecurityManager}, otherwise return a map implementation that will attempt
	 * to access individual keys using calls to {@link System#getenv(String)}.
	 * <p>Note that most {@link Environment} implementations will include this system
	 * environment map as a default {@link PropertySource} to be searched. Therefore, it
	 * is recommended that this method not be used directly unless bypassing other
	 * property sources is expressly intended.
	 * <p>Calls to {@link Map#get(Object)} on the Map returned will never throw
	 * {@link IllegalAccessException}; in cases where the SecurityManager forbids access
	 * to a property, {@code null} will be returned and an INFO-level log message will be
	 * issued noting the exception.
	 */
	Map<String, Object> getSystemEnvironment();

	/**
	 * Append the given parent environment's active profiles, default profiles and
	 * property sources to this (child) environment's respective collections of each.
	 * <p>For any identically-named {@code PropertySource} instance existing in both
	 * parent and child, the child instance is to be preserved and the parent instance
	 * discarded. This has the effect of allowing overriding of property sources by the
	 * child as well as avoiding redundant searches through common property source types,
	 * e.g. system environment and system properties.
	 * <p>Active and default profile names are also filtered for duplicates, to avoid
	 * confusion and redundant storage.
	 * <p>The parent environment remains unmodified in any case. Note that any changes to
	 * the parent environment occurring after the call to {@code merge} will not be
	 * reflected in the child. Therefore, care should be taken to configure parent
	 * property sources and profile information prior to calling {@code merge}.
	 * @param parent the environment to merge with
	 * @since 3.1.2
	 * @see org.springframework.context.support.AbstractApplicationContext#setParent
	 */
	void merge(ConfigurableEnvironment parent);

}
