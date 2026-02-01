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

package org.springframework.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.springframework.lang.Nullable;

/**
 * <p>本地Spring属性的静态持有者，即在Spring库级别定义的属性。
 *
 * <p>从类路径根目录读取 {@code spring.properties} 文件并也允许通过 {@link #setProperty} 以编程方式设置属性。
 * 检索属性时，首先检查本地条目，然后通过 {@link System#getProperty} 检查JVM级系统属性作为备选。
 *
 * <p>这是设置Spring相关系统属性的替代方法，如 {@code spring.getenv.ignore} 和 {@code spring.beaninfo.ignore}，
 * 特别是在JVM系统属性在目标平台上被锁定的场景中(例如WebSphere)。
 * 参见 {@link #setFlag} 以本地方式将这些标志设置为 {@code "true"} 的便捷方法。
 *
 * @author Juergen Hoeller
 * @since 3.2.7
 * @see org.springframework.aot.AotDetector#AOT_ENABLED
 * @see org.springframework.beans.StandardBeanInfoFactory#IGNORE_BEANINFO_PROPERTY_NAME
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#STRICT_LOCKING_PROPERTY_NAME
 * @see org.springframework.core.env.AbstractEnvironment#IGNORE_GETENV_PROPERTY_NAME
 * @see org.springframework.expression.spel.SpelParserConfiguration#SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
 * @see org.springframework.jdbc.core.StatementCreatorUtils#IGNORE_GETPARAMETERTYPE_PROPERTY_NAME
 * @see org.springframework.jndi.JndiLocatorDelegate#IGNORE_JNDI_PROPERTY_NAME
 * @see org.springframework.objenesis.SpringObjenesis#IGNORE_OBJENESIS_PROPERTY_NAME
 * @see org.springframework.test.context.NestedTestConfiguration#ENCLOSING_CONFIGURATION_PROPERTY_NAME
 * @see org.springframework.test.context.TestConstructor#TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME
 * @see org.springframework.test.context.cache.ContextCache#MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME
 */
public final class SpringProperties {

	private static final String PROPERTIES_RESOURCE_LOCATION = "spring.properties";

	private static final Properties localProperties = new Properties();


	static {
		try {
			ClassLoader cl = SpringProperties.class.getClassLoader();
			URL url = (cl != null ? cl.getResource(PROPERTIES_RESOURCE_LOCATION) :
					ClassLoader.getSystemResource(PROPERTIES_RESOURCE_LOCATION));
			if (url != null) {
				try (InputStream is = url.openStream()) {
					localProperties.load(is);
				}
			}
		}
		catch (IOException ex) {
			System.err.println("Could not load 'spring.properties' file from local classpath: " + ex);
		}
	}


	private SpringProperties() {
	}


	/**
	 * <p>以编程方式设置本地属性，覆盖{@code spring.properties} 文件中的条目(如果有)。
	 * @param key the property key
	 * <p>属性键
	 * @param value the associated property value, or {@code null} to reset it
	 * <p>相关的属性值，或 {@code null} 以重置它
	 */
	public static void setProperty(String key, @Nullable String value) {
		if (value != null) {
			localProperties.setProperty(key, value);
		}
		else {
			localProperties.remove(key);
		}
	}

	/**
	 * <p>检索给定键的属性值，首先检查本地Spring属性，然后回退到JVM级系统属性。
	 * @param key the property key
	 * <p> 属性键
	 * @return the associated property value, or {@code null} if none found
	 * <p> 相关的属性值，如果未找到则返回 {@code null}
	 */
	@Nullable
	public static String getProperty(String key) {
		String value = localProperties.getProperty(key);
		if (value == null) {
			try {
				value = System.getProperty(key);
			}
			catch (Throwable ex) {
				System.err.println("Could not retrieve system property '" + key + "': " + ex);
			}
		}
		return value;
	}

	/**
	 * <p>以编程方式将本地标志设置为 "true"，覆盖{@code spring.properties} 文件中的条目(如果有)。
	 * @param key the property key
	 * <p> 属性键
	 */
	public static void setFlag(String key) {
		localProperties.setProperty(key, Boolean.TRUE.toString());
	}

	/**
	 * Programmatically set a local flag to the given value, overriding
	 * an entry in the {@code spring.properties} file (if any).
	 * <p>以编程方式将本地标志设置为给定值，覆盖
	 * {@code spring.properties} 文件中的条目(如果有)。
	 * @param key the property key
	 * <p> 属性键
	 * @param value the associated boolean value
	 * <p> 相关的布尔值
	 * @since 6.2.6
	 */
	public static void setFlag(String key, boolean value) {
		localProperties.setProperty(key, Boolean.toString(value));
	}

	/**
	 * Retrieve the flag for the given property key.
	 * <p>检索给定属性键的标志。
	 * @param key the property key
	 * <p> 属性键
	 * @return {@code true} if the property is set to the string "true"
	 * (ignoring case), {@code} false otherwise
	 * <p> 如果属性设置为字符串 "true" 则返回 {@code true}
	 * (忽略大小写)，否则返回 {@code} false
	 */
	public static boolean getFlag(String key) {
		return Boolean.parseBoolean(getProperty(key));
	}

	/**
	 * Retrieve the flag for the given property key, returning {@code null}
	 * instead of {@code false} in case of no actual flag set.
	 * <p>检索给定属性键的标志，如果没有实际设置标志
	 * 则返回 {@code null} 而不是 {@code false}。
	 * @param key the property key
	 * <p> 属性键
	 * @return {@code true} if the property is set to the string "true"
	 * (ignoring case), {@code} false if it is set to any other value,
	 * {@code null} if it is not set at all
	 * <p> 如果属性设置为字符串 "true" 则返回 {@code true}
	 * (忽略大小写)，如果设置为任何其他值则返回 {@code false}，
	 * 如果根本没有设置则返回 {@code null}
	 * @since 6.2.6
	 */
	@Nullable
	public static Boolean checkFlag(String key) {
		String flag = getProperty(key);
		return (flag != null ? Boolean.valueOf(flag) : null);
	}

}
