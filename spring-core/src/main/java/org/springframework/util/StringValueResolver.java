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

package org.springframework.util;

import org.springframework.lang.Nullable;

/**
 * 用于解析String值的简单策略接口。
 *
 * Simple strategy interface for resolving a String value.
 * Used by {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#resolveAliases
 * @see org.springframework.beans.factory.config.BeanDefinitionVisitor#BeanDefinitionVisitor(StringValueResolver)
 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
 */
@FunctionalInterface
public interface StringValueResolver {

	/**
	 * 解析给定的String值，例如：解析占位符。
	 * @param strVal 原始的String值（不可能为{@code null}）
	 * @return 已解析的String值（当解析出一个null值时，可能为{@code null}），也
	 * 可能是原始的String值（当没有占位符 或 忽略无法解析的占位符时）
	 * @throws IllegalArgumentException 在无法解析的字符串值的情况下
	 * <p></p>
	 * Resolve the given String value, for example parsing placeholders.
	 * @param strVal the original String value (never {@code null})
	 * @return the resolved String value (may be {@code null} when resolved to a null
	 * value), possibly the original String value itself (in case of no placeholders
	 * to resolve or when ignoring unresolvable placeholders)
	 * @throws IllegalArgumentException in case of an unresolvable String value
	 */
	@Nullable
	String resolveStringValue(String strVal);

}
