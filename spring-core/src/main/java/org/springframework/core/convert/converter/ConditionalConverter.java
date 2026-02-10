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

package org.springframework.core.convert.converter;

import org.springframework.core.convert.TypeDescriptor;

/**
 * <p>允许 {@link Converter}、{@link GenericConverter} 或 {@link ConverterFactory}
 * 基于 {@link TypeDescriptor} {@code source} 和 {@link TypeDescriptor} {@code target} 的属性有条件地执行。
 *
 * <p>通常用于基于字段或类级别特征（如注解或方法）的存在来选择性匹配自定义转换逻辑。
 * 例如，当从String字段转换到Date字段时，如果目标字段也被注解为 {@code @DateTimeFormat}，
 * 实现可能会返回 {@code true}。
 *
 * <p>另一个例子，当从String字段转换到 {@code Account} 字段时，如果目标Account类定义了
 * {@code public static findAccount(String)} 方法，实现可能会返回 {@code true}。
 *
 * @author Phillip Webb
 * @author Keith Donald
 * @since 3.2
 * @see Converter
 * @see GenericConverter
 * @see ConverterFactory
 * @see ConditionalGenericConverter
 */
public interface ConditionalConverter {

	/**
	 * 是否应该选择【当前正在考虑的从 {@code sourceType} 到 {@code targetType} 的转换】？
	 * @param sourceType 我们正在从中转换的字段的类型描述符
	 * @param targetType 我们正在转换到的字段的类型描述符
	 * @return 如果应该执行转换则返回true，否则返回false
	 */
	boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType);

}
