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

import org.springframework.lang.Nullable;

/**

 * Interface for resolving properties against any underlying source.
 * <p>针对任何底层的源，来解析属性的接口。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see Environment
 * @see PropertySourcesPropertyResolver
 */
public interface PropertyResolver {

	/**
	 * Determine whether the given property key is available for resolution
	 * &mdash; for example, if the value for the given key is not {@code null}.
	 * <p>确定给定的属性键是否可用于解析 —— 例如，如果给定键的值不为 {@code null}。
	 */
	boolean containsProperty(String key);

	/**
	 * Resolve the property value associated with the given key,
	 * or {@code null} if the key cannot be resolved.
	 * <p>解析与给定键关联的属性值，如果无法解析该键，则返回 {@code null}。
	 * @param key the property name to resolve
	 * <p> 要解析的属性名称
	 * @see #getProperty(String, String)
	 * @see #getProperty(String, Class)
	 * @see #getRequiredProperty(String)
	 */
	@Nullable
	String getProperty(String key);

	/**
	 * Resolve the property value associated with the given key, or
	 * {@code defaultValue} if the key cannot be resolved.
	 * <p>解析与给定键关联的属性值，如果无法解析该键，则返回 {@code defaultValue}。
	 * @param key the property name to resolve
	 * <p> 要解析的属性名称
	 * @param defaultValue the default value to return if no value is found
	 * <p> 如果未找到值则返回的默认值
	 * @see #getRequiredProperty(String)
	 * @see #getProperty(String, Class)
	 */
	String getProperty(String key, String defaultValue);

	/**
	 * Resolve the property value associated with the given key,
	 * or {@code null} if the key cannot be resolved.
	 * <p>解析与给定键关联的属性值，如果无法解析该键，则返回 {@code null}。
	 * @param key the property name to resolve
	 * <p> 要解析的属性名称
	 * @param targetType the expected type of the property value
	 * <p> 属性值的预期类型
	 * @see #getRequiredProperty(String, Class)
	 */
	@Nullable
	<T> T getProperty(String key, Class<T> targetType);

	/**
	 * Resolve the property value associated with the given key,
	 * or {@code defaultValue} if the key cannot be resolved.
	 * <p>解析与给定键关联的属性值，如果无法解析该键，则返回 {@code defaultValue}。
	 * @param key the property name to resolve
	 * <p> 要解析的属性名称
	 * @param targetType the expected type of the property value
	 * <p> 属性值的预期类型
	 * @param defaultValue the default value to return if no value is found
	 * <p> 如果未找到值则返回的默认值
	 * @see #getRequiredProperty(String, Class)
	 */
	<T> T getProperty(String key, Class<T> targetType, T defaultValue);

	/**
	 * Resolve the property value associated with the given key (never {@code null}).
	 * <p>解析与给定键关联的属性值（永远不会为 {@code null}）。
	 * @throws IllegalStateException if the key cannot be resolved
	 * <p>如果无法解析该键，则抛出IllegalStateException
	 * @see #getRequiredProperty(String, Class)
	 */
	String getRequiredProperty(String key) throws IllegalStateException;

	/**
	 * Resolve the property value associated with the given key, converted to the given
	 * targetType (never {@code null}).
	 * <p>解析与给定键关联的属性值，转换为给定的目标类型（永远不会为 {@code null}）。
	 * @throws IllegalStateException if the given key cannot be resolved
	 * <p>如果给定的键无法解析，则抛出IllegalStateException
	 */
	<T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException;

	/**
	 * Resolve ${...} placeholders in the given text, replacing them with corresponding
	 * property values as resolved by {@link #getProperty}. Unresolvable placeholders with
	 * no default value are ignored and passed through unchanged.
	 * <p> 解析给定文本中的 ${...} 占位符，用 {@link #getProperty} 解析出的相应属性值替换。无法解析且
	 * 没有默认值的占位符将被忽略，并保持不变。
	 * @param text the String to resolve
	 * <p> 要解析的字符串
	 * @return the resolved String (never {@code null})
	 * <p> 解析后的字符串（永远不会为 {@code null}）
	 * @throws IllegalArgumentException if given text is {@code null}
	 * <p> 如果给定的文本为 {@code null} 则抛出 IllegalArgumentException
	 * @see #resolveRequiredPlaceholders
	 */
	String resolvePlaceholders(String text);

	/**
	 * Resolve ${...} placeholders in the given text, replacing them with corresponding
	 * property values as resolved by {@link #getProperty}. Unresolvable placeholders with
	 * no default value will cause an IllegalArgumentException to be thrown.
	 * <p> 解析给定文本中的 ${...} 占位符，用 {@link #getProperty} 解析出的相应属性值替换。无法解析且
	 * 没有默认值的占位符将导致抛出 IllegalArgumentException。
	 * @return the resolved String (never {@code null})
	 * <p> 解析后的字符串（永远不会为 {@code null}）
	 * @throws IllegalArgumentException if given text is {@code null}
	 * or if any placeholders are unresolvable
	 * <p> 如果给定文本为 {@code null} 或者有任何占位符无法解析，则抛出 IllegalArgumentException
	 */
	String resolveRequiredPlaceholders(String text) throws IllegalArgumentException;

}
