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

package org.springframework.beans;

import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;

/**
 * 定义属性访问器（PropertyAccessor）配置方法的接口，同时继承用于管理属性编辑器（PropertyEditor）的PropertyEditorRegistry接口。
 *
 * <p>该接口是{@link BeanWrapper}的基础接口。
 *
 * Interface that encapsulates configuration methods for a PropertyAccessor.
 * Also extends the PropertyEditorRegistry interface, which defines methods
 * for PropertyEditor management.
 *
 * <p>Serves as base interface for {@link BeanWrapper}.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.0
 * @see BeanWrapper
 */
public interface ConfigurablePropertyAccessor extends PropertyAccessor, PropertyEditorRegistry, TypeConverter {

	/**
	 * 指定用于属性值转换的 Spring 3.0 ConversionService，作为 JavaBeans PropertyEditors 的替代方案。
	 *
	 * Specify a Spring 3.0 ConversionService to use for converting
	 * property values, as an alternative to JavaBeans PropertyEditors.
	 */
	void setConversionService(@Nullable ConversionService conversionService);

	/**
	 * Return the associated ConversionService, if any.
	 */
	@Nullable
	ConversionService getConversionService();

	/**
	 * Set whether to extract the old property value when applying a
	 * property editor to a new value for a property.
	 */
	void setExtractOldValueForEditor(boolean extractOldValueForEditor);

	/**
	 * 用于指定在使用属性编辑器处理 属性的新值时，是否提取该属性的旧值。
	 *
	 * Return whether to extract the old property value when applying a
	 * property editor to a new value for a property.
	 */
	boolean isExtractOldValueForEditor();

	/**
	 * 设置此实例是否应尝试对包含 {@code null} 值的嵌套路径进行"自动扩展"。
	 * <p>若设为 {@code true}，遇到为 {@code null} 的路径位置时，将使用默认对象值进行填充并继续遍历，而不会抛出 {@link NullValueInNestedPathException} 异常。
	 * <p>在普通 PropertyAccessor 实例上，默认值为 {@code false}。
	 *
	 * Set whether this instance should attempt to "auto-grow" a
	 * nested path that contains a {@code null} value.
	 * <p>If {@code true}, a {@code null} path location will be populated
	 * with a default object value and traversed instead of resulting in a
	 * {@link NullValueInNestedPathException}.
	 * <p>Default is {@code false} on a plain PropertyAccessor instance.
	 */
	void setAutoGrowNestedPaths(boolean autoGrowNestedPaths);

	/**
	 * Return whether "auto-growing" of nested paths has been activated.
	 */
	boolean isAutoGrowNestedPaths();

}
