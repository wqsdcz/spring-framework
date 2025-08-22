/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.core.type;

import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * 定义对特定类型（{@link AnnotationMetadata 类}或{@link MethodMetadata 方法}）注解的访问机制。
 * 在这种形式下，不一定需要类加载。
 *
 * Defines access to the annotations of a specific type ({@link AnnotationMetadata class}
 * or {@link MethodMetadata method}), in a form that does not necessarily require the
 * class-loading.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Mark Pollack
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 * @see AnnotationMetadata
 * @see MethodMetadata
 */
public interface AnnotatedTypeMetadata {

	/**
	 * <p>判断底层元素是否定义了给定类型的注解或元注解。<p/>
	 * <p>若本方法返回 {@code true}，则 {@link #getAnnotationAttributes} 必返回非空 Map。<p/>
	 *
	 * @param annotationName 待查找注解的全限定类名
	 * @return 存在匹配注解时返回 true
	 * <p/>
	 * Determine whether the underlying element has an annotation or meta-annotation
	 * of the given type defined.
	 * <p>If this method returns {@code true}, then
	 * {@link #getAnnotationAttributes} will return a non-null Map.
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for
	 * @return whether a matching annotation is defined
	 */
	boolean isAnnotated(String annotationName);

	/**
	 * 获取给定类型的注解的属性（若存在），无论该注解是直接标注于底层元素还是通过元注解继承，且自动处理组合注解中的属性覆写。
	 *
	 * Retrieve the attributes of the annotation of the given type, if any (i.e. if
	 * defined on the underlying element, as direct annotation or meta-annotation),
	 * also taking attribute overrides on composed annotations into account.
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for
	 * @return a Map of attributes, with the attribute name as key (e.g. "value")
	 * and the defined attribute value as Map value. This return value will be
	 * {@code null} if no matching annotation is defined.
	 */
	@Nullable
	Map<String, Object> getAnnotationAttributes(String annotationName);

	/**
	 * 获取给定类型的注解的属性（若存在），无论该注解是直接标注于底层元素还是通过元注解继承，且自动处理组合注解中的属性覆写。
	 * @param annotationName 待查找注解的全限定类名
	 * @param classValuesAsString 是否将类引用转为字符串类名（以避免类加载），而非需预先加载的 Class 引用
	 * @return 属性映射集（键为属性名如"value"，值为处理后的属性值），当且仅当注解不存在时返回 {@code null}
	 *
	 * Retrieve the attributes of the annotation of the given type, if any (i.e. if
	 * defined on the underlying element, as direct annotation or meta-annotation),
	 * also taking attribute overrides on composed annotations into account.
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for
	 * @param classValuesAsString whether to convert class references to String
	 * class names for exposure as values in the returned Map, instead of Class
	 * references which might potentially have to be loaded first
	 * @return a Map of attributes, with the attribute name as key (e.g. "value")
	 * and the defined attribute value as Map value. This return value will be
	 * {@code null} if no matching annotation is defined.
	 */
	@Nullable
	Map<String, Object> getAnnotationAttributes(String annotationName, boolean classValuesAsString);

	/**
	 * 获取【给定类型】的【所有注解】的【全部属性】（如果有的话，即如果该注解已定义在基础元素上，无论是作为直接注解还是元注解）。
	 * 请注意，此变体不考虑属性覆盖情况。
	 *
	 * @param annotationName 需要查找的注解类型的完整限定类名
	 * @return 一个包含属性的多映射，其中属性名称作为键（例如“value”），而定义的属性值则作为 Map 值。如果未定义匹配的注解，则此返回值将为 {@code null}。
	 * @see #getAllAnnotationAttributes(String， boolean)
	 *
	 * Retrieve all attributes of all annotations of the given type, if any (i.e. if
	 * defined on the underlying element, as direct annotation or meta-annotation).
	 * Note that this variant does <i>not</i> take attribute overrides into account.
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for
	 * @return a MultiMap of attributes, with the attribute name as key (e.g. "value")
	 * and a list of the defined attribute values as Map value. This return value will
	 * be {@code null} if no matching annotation is defined.
	 * @see #getAllAnnotationAttributes(String, boolean)
	 */
	@Nullable
	MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName);

	/**
	 * Retrieve all attributes of all annotations of the given type, if any (i.e. if
	 * defined on the underlying element, as direct annotation or meta-annotation).
	 * Note that this variant does <i>not</i> take attribute overrides into account.
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for
	 * @param classValuesAsString  whether to convert class references to String
	 * @return a MultiMap of attributes, with the attribute name as key (e.g. "value")
	 * and a list of the defined attribute values as Map value. This return value will
	 * be {@code null} if no matching annotation is defined.
	 * @see #getAllAnnotationAttributes(String)
	 */
	@Nullable
	MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName, boolean classValuesAsString);

}
