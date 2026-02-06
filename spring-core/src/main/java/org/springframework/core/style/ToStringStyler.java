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

import org.springframework.lang.Nullable;

/**
 * A strategy interface for pretty-printing {@code toString()} methods.
 * Encapsulates the print algorithms; some other object such as a builder
 * should provide the workflow.
 *
 * <p>用于美化打印{@code toString()}方法的策略接口。
 * 封装打印算法；某些其他对象（如构建器）应该提供工作流程。
 *
 * @author Keith Donald
 * @since 1.2.2
 */
public interface ToStringStyler {

	/**
	 * Style a {@code toString()}'ed object before its fields are styled.
	 *
	 * <p>在对象的字段被样式化之前，对{@code toString()}对象进行样式化。
	 * @param buffer 要打印到的缓冲区
	 * @param obj 要样式化的对象
	 */
	void styleStart(StringBuilder buffer, Object obj);

	/**
	 * Style a {@code toString()}'ed object after it's fields are styled.
	 *
	 * <p>在对象的字段被样式化之后，对{@code toString()}对象进行样式化。
	 * @param buffer 要打印到的缓冲区
	 * @param obj 要样式化的对象
	 */
	void styleEnd(StringBuilder buffer, Object obj);

	/**
	 * Style a field value as a string.
	 *
	 * <p>将字段值样式化为字符串。
	 * @param buffer 要打印到的缓冲区
	 * @param fieldName 字段名称
	 * @param value 字段值
	 */
	void styleField(StringBuilder buffer, String fieldName, @Nullable Object value);

	/**
	 * Style the given value.
	 *
	 * <p>样式化给定的值。
	 * @param buffer 要打印到的缓冲区
	 * @param value 字段值
	 */
	void styleValue(StringBuilder buffer, Object value);

	/**
	 * Style the field separator.
	 *
	 * <p>样式化字段分隔符。
	 * @param buffer 要打印到的缓冲区
	 */
	void styleFieldSeparator(StringBuilder buffer);

}
