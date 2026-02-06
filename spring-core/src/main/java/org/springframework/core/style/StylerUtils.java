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

package org.springframework.core.style;

/**
 * Simple utility class to allow for convenient access to value
 * styling logic, mainly to support descriptive logging messages.
 *
 * <p>For more sophisticated needs, use the {@link ValueStyler} abstraction
 * directly. This class simply uses a shared {@link DefaultValueStyler}
 * instance underneath.
 *
 * <p>简单的实用程序类，允许方便地访问值样式化逻辑，主要用于支持描述性日志消息。
 *
 * <p>对于更复杂的需求，请直接使用{@link ValueStyler}抽象。
 * 此类只是在底层使用共享的{@link DefaultValueStyler}实例。
 *
 * @author Keith Donald
 * @since 1.2.2
 * @see ValueStyler
 * @see DefaultValueStyler
 */
public abstract class StylerUtils {

	/**
	 * Default ValueStyler instance used by the {@code style} method.
	 * Also available for the {@link ToStringCreator} class in this package.
	 *
	 * <p>{@code style}方法使用的默认ValueStyler实例。
	 * 也适用于此包中的{@link ToStringCreator}类。
	 */
	static final ValueStyler DEFAULT_VALUE_STYLER = new DefaultValueStyler();

	/**
	 * Style the specified value according to default conventions.
	 *
	 * <p>{@code style}方法使用的默认ValueStyler实例。
	 * 根据默认约定样式化指定的值。
	 * @param value 要样式化的对象值
	 * @return 样式化的字符串
	 * @see DefaultValueStyler
	 */
	public static String style(Object value) {
		return DEFAULT_VALUE_STYLER.style(value);
	}

}
