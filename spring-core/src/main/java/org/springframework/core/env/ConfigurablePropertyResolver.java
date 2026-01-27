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

import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.lang.Nullable;

/**
 * Configuration interface to be implemented by most if not all {@link PropertyResolver}
 * types. Provides facilities for accessing and customizing the
 * {@link org.springframework.core.convert.ConversionService ConversionService}
 * used when converting property values from one type to another.
 * <p>几乎所有的 {@link PropertyResolver} 类型都需要实现的配置接口。提供了访问和自定义
 * {@link org.springframework.core.convert.ConversionService ConversionService}
 * 的功能，用于将属性值从一种类型转换为另一种类型。</p>
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1
 */
public interface ConfigurablePropertyResolver extends PropertyResolver {

	/**
	 * Return the {@link ConfigurableConversionService} used when performing type
	 * conversions on properties.
	 * <p>返回执行属性类型转换时使用的 {@link ConfigurableConversionService}。
	 * <p>The configurable nature of the returned conversion service allows for
	 * the convenient addition and removal of individual {@code Converter} instances:
	 * <p>返回的转换服务的可配置特性允许方便地添加和删除单个 {@code Converter} 实例：
	 * <pre class="code">
	 * <pre class="code">
	 * ConfigurableConversionService cs = env.getConversionService();
	 * cs.addConverter(new FooConverter());
	 * </pre>
	 * @see PropertyResolver#getProperty(String, Class)
	 * @see org.springframework.core.convert.converter.ConverterRegistry#addConverter
	 */
	ConfigurableConversionService getConversionService();

	/**
	 * Set the {@link ConfigurableConversionService} to be used when performing type
	 * conversions on properties.
	 * <p>设置执行属性类型转换时要使用的 {@link ConfigurableConversionService}。
	 * <p><strong>Note:</strong> as an alternative to fully replacing the
	 * {@code ConversionService}, consider adding or removing individual
	 * {@code Converter} instances by drilling into {@link #getConversionService()}
	 * and calling methods such as {@code #addConverter}.
	 * <p><strong>注意：</strong>作为完全替换 {@code ConversionService} 的替代方法，
	 * 考虑通过深入到 {@link #getConversionService()} 并调用诸如 {@code #addConverter} 
	 * 之类的方法来添加或删除单个 {@code Converter} 实例。
	 * @see PropertyResolver#getProperty(String, Class)
	 * @see #getConversionService()
	 * @see org.springframework.core.convert.converter.ConverterRegistry#addConverter
	 */
	void setConversionService(ConfigurableConversionService conversionService);

	/**
	 * Set the prefix that placeholders replaced by this resolver must begin with.
	 * <p>设置此解析器替换的占位符必须以什么前缀开头。</p>
	 */
	void setPlaceholderPrefix(String placeholderPrefix);

	/**
	 * Set the suffix that placeholders replaced by this resolver must end with.
	 * <p>设置此解析器替换的占位符必须以什么后缀结尾。</p>
	 */
	void setPlaceholderSuffix(String placeholderSuffix);

	/**
	 * Set the separating character to be honored between placeholders replaced by
	 * this resolver and their associated default values, or {@code null} if no such
	 * special character should be processed as a value separator.
	 * <p>设置此解析器替换的占位符与其关联的默认值之间要遵循的分隔字符，如果不应将任何特殊字符处理为值分隔符，则为 {@code null}。</p>
	 */
	void setValueSeparator(@Nullable String valueSeparator);

	/**
	 * Set the escape character to use to ignore the
	 * {@linkplain #setPlaceholderPrefix(String) placeholder prefix} and the
	 * {@linkplain #setValueSeparator(String) value separator}, or {@code null}
	 * if no escaping should take place.
	 * <p>设置用于忽略 {@linkplain #setPlaceholderPrefix(String) 占位符前缀} 和
	 * {@linkplain #setValueSeparator(String) 值分隔符} 的转义字符，如果不需进行转义则为 {@code null}。</p>
	 * @since 6.2
	 */
	void setEscapeCharacter(@Nullable Character escapeCharacter);

	/**
	 * Specify whether to throw an exception when encountering an unresolvable placeholder
	 * nested within the value of a given property. A {@code false} value indicates strict
	 * resolution, i.e. that an exception will be thrown. A {@code true} value indicates
	 * that unresolvable nested placeholders should be passed through in their unresolved
	 * ${...} form.
	 * <p>指定在遇到嵌套在给定属性值内的不可解析占位符时是否抛出异常。{@code false} 值表示严格解析，
	 * 即将抛出异常。{@code true} 值表示未解析的嵌套占位符应该以其未解析的 ${...} 形式传递。
	 * <p>Implementations of {@link #getProperty(String)} and its variants must inspect
	 * the value set here to determine correct behavior when property values contain
	 * unresolvable placeholders.
	 * <p>{@link #getProperty(String)} 及其变体的实现必须检查此处设置的值，
	 * 以确定属性值包含不可解析占位符时的正确行为。
	 * @since 3.2
	 */
	void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders);

	/**
	 * Specify which properties must be present, to be verified by
	 * {@link #validateRequiredProperties()}.
	 * <p>指定哪些属性必须存在，由 {@link #validateRequiredProperties()} 进行验证。</p>
	 */
	void setRequiredProperties(String... requiredProperties);

	/**
	 * Validate that each of the properties specified by
	 * {@link #setRequiredProperties} is present and resolves to a
	 * non-{@code null} value.
	 * <p>验证通过 {@link #setRequiredProperties} 指定的每个属性是否存在并且解析为非 {@code null} 值。</p>
	 * @throws MissingRequiredPropertiesException if any of the required
	 * properties are not resolvable
	 * <p>如果任何必需的属性无法解析，则抛出 MissingRequiredPropertiesException 异常。</p>
	 */
	void validateRequiredProperties() throws MissingRequiredPropertiesException;

}
