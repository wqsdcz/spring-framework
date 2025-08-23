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

package org.springframework.core.env;

/**
 * <p>{@link Environment} 接口的标准实现，适用于'常规'（即非Web）应用场景。<p/>
 *
 * <p>除具备{@link ConfigurableEnvironment}的常规功能（如属性解析和配置集相关操作）外，该实现默认配置两个属性源，将按以下顺序搜索：
 * <ul>
 *     <li>通过{@linkplain AbstractEnvironment#getSystemProperties() 系统属性}</li>
 *     <li>通过{@linkplain AbstractEnvironment#getSystemEnvironment() 系统环境变量}</li>
 * </ul>
 * <p/>
 * <p>也就是说，如果键"xyz"同时存在于[JVM系统属性]和[当前进程的环境变量]集合中，通过调用 {@code environment.getProperty("xyz")} 将返回[系统属性]中的值。
 * 此优先级顺序是默认设定的，因为[系统属性]是每个JVM的专属属性，而[环境变量]在同一系统内多个JVM间可能相同。让[系统属性]优先允许在每个JVM基础上覆盖[环境变量]。<p/>
 *
 * <p>这些默认属性源可被移除、重排序或替换；同时可通过 {@link #getPropertySources()} 获取的 {@link MutablePropertySources} 实例添加额外属性源。
 * 具体用法示例请参阅 {@link ConfigurableEnvironment} 的Javadoc文档。<p/>
 *
 * <p>关于shell环境（如Bash）中禁止使用句点字符的变量名特殊处理机制，详见 {@link SystemEnvironmentPropertySource} 的javadoc说明。<p/>
 *
 * {@link Environment} implementation suitable for use in 'standard' (i.e. non-web)
 * applications.
 *
 * <p>In addition to the usual functions of a {@link ConfigurableEnvironment} such as
 * property resolution and profile-related operations, this implementation configures two
 * default property sources, to be searched in the following order:
 * <ul>
 * <li>{@linkplain AbstractEnvironment#getSystemProperties() system properties}
 * <li>{@linkplain AbstractEnvironment#getSystemEnvironment() system environment variables}
 * </ul>
 *
 * That is, if the key "xyz" is present both in the JVM system properties as well as in
 * the set of environment variables for the current process, the value of key "xyz" from
 * system properties will return from a call to {@code environment.getProperty("xyz")}.
 * This ordering is chosen by default because system properties are per-JVM, while
 * environment variables may be the same across many JVMs on a given system.  Giving
 * system properties precedence allows for overriding of environment variables on a
 * per-JVM basis.
 *
 * <p>These default property sources may be removed, reordered, or replaced; and
 * additional property sources may be added using the {@link MutablePropertySources}
 * instance available from {@link #getPropertySources()}. See
 * {@link ConfigurableEnvironment} Javadoc for usage examples.
 *
 * <p>See {@link SystemEnvironmentPropertySource} javadoc for details on special handling
 * of property names in shell environments (e.g. Bash) that disallow period characters in
 * variable names.
 *
 * @author Chris Beams
 * @since 3.1
 * @see ConfigurableEnvironment
 * @see SystemEnvironmentPropertySource
 * @see org.springframework.web.context.support.StandardServletEnvironment
 */
public class StandardEnvironment extends AbstractEnvironment {

	/** System environment property source name: {@value} */
	public static final String SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME = "systemEnvironment";

	/** JVM system properties property source name: {@value} */
	public static final String SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME = "systemProperties";


	/**
	 * Customize the set of property sources with those appropriate for any standard
	 * Java environment:
	 * <ul>
	 * <li>{@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME}
	 * <li>{@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}
	 * </ul>
	 * <p>Properties present in {@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME} will
	 * take precedence over those in {@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}.
	 * @see AbstractEnvironment#customizePropertySources(MutablePropertySources)
	 * @see #getSystemProperties()
	 * @see #getSystemEnvironment()
	 */
	@Override
	protected void customizePropertySources(MutablePropertySources propertySources) {
		propertySources.addLast(
				new PropertiesPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, getSystemProperties()));
		propertySources.addLast(
				new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, getSystemEnvironment()));
	}

}
