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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.lang.Nullable;

/**
 * <p>{@code AnnotationAttributeExtractor} 负责从底层的{@linkplain #getSource source}（例如: {@code Annotation} 或 {@code Map}）中
 * {@linkplain #getAttributeValue 提取}注解属性值。<p/>
 *
 * An {@code AnnotationAttributeExtractor} is responsible for
 * {@linkplain #getAttributeValue extracting} annotation attribute values
 * from an underlying {@linkplain #getSource source} such as an
 * {@code Annotation} or a {@code Map}.
 *
 * @author Sam Brannen
 * @since 4.2
 * @param <S> the type of source supported by this extractor
 * @see SynthesizedAnnotationInvocationHandler
 */
interface AnnotationAttributeExtractor<S> {

	/**
	 * 获取此提取器支持提取属性值的注释类型。
	 *
	 * Get the type of annotation that this extractor extracts attribute
	 * values for.
	 */
	Class<? extends Annotation> getAnnotationType();

	/**
	 * 获取使用这个提取器所支持的注解类型的注解标注的元素。
	 * @return 被标注的元素；如果未知，则为{@code null}；
	 *
	 * Get the element that is annotated with an annotation of the annotation
	 * type supported by this extractor.
	 * @return the annotated element, or {@code null} if unknown
	 */
	@Nullable
	Object getAnnotatedElement();

	/**
	 * 获取注解属性的底层源。
	 *
	 * Get the underlying source of annotation attributes.
	 */
	S getSource();

	/**
	 * 从底层 {@linkplain #getSource 源}中获取属性值，该属性值与指定的属性方法相对应。
	 * @param attributeMethod 此提取器所支持注解类型的属性方法
	 * @return 注解属性值
	 *
	 * Get the attribute value from the underlying {@linkplain #getSource source}
	 * that corresponds to the supplied attribute method.
	 * @param attributeMethod an attribute method from the annotation type
	 * supported by this extractor
	 * @return the value of the annotation attribute
	 */
	@Nullable
	Object getAttributeValue(Method attributeMethod);

}
