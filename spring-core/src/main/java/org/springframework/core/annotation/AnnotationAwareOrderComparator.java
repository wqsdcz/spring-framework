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

package org.springframework.core.annotation;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.DecoratingProxy;
import org.springframework.core.OrderComparator;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;


/**
 * <p>AnnotationAwareOrderComparator 是 OrderComparator 的扩展，支持 Spring 的 Ordered 接口
 * 以及 @Order 和 @Priority 注解，其中 Ordered 实例提供的顺序值会覆盖静态定义的注解值（如果存在）。
 *
 * <p>有关非有序对象的排序语义详细信息，请参阅 OrderComparator 的 JavaDoc。
 *
 * @author Juergen Hoeller
 * @author Oliver Gierke
 * @author Stephane Nicoll
 * @since 2.0.1
 * @see org.springframework.core.Ordered
 * @see org.springframework.core.annotation.Order
 * @see jakarta.annotation.Priority
 */

public class AnnotationAwareOrderComparator extends OrderComparator {


	/**
	 * Shared default instance of {@code AnnotationAwareOrderComparator}.
	 * 
	 * <p>AnnotationAwareOrderComparator 的共享默认实例。
	 */
	public static final AnnotationAwareOrderComparator INSTANCE = new AnnotationAwareOrderComparator();



	/**
	 * <p>此实现检查各种元素上的 {@link Order @Order} 或 {@link jakarta.annotation.Priority @Priority} 注解，
	 * 以及父类中的 {@link org.springframework.core.Ordered} 检查。
	 */
	@Override
	@Nullable
	protected Integer findOrder(Object obj) {
		Integer order = super.findOrder(obj);
		if (order != null) {
			return order;
		}
		return findOrderFromAnnotation(obj);
	}

	@Nullable
	private Integer findOrderFromAnnotation(Object obj) {
		AnnotatedElement element = (obj instanceof AnnotatedElement ae ? ae : obj.getClass());
		MergedAnnotations annotations = MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY);
		Integer order = OrderUtils.getOrderFromAnnotations(element, annotations);
		if (order == null && obj instanceof DecoratingProxy decoratingProxy) {
			return findOrderFromAnnotation(decoratingProxy.getDecoratedClass());
		}
		return order;
	}


	/**
	 * This implementation retrieves an @{@link jakarta.annotation.Priority}
	 * value, allowing for additional semantics over the regular @{@link Order}
	 * annotation: typically, selecting one object over another in case of
	 * multiple matches but only one object to be returned.
	 * 
	 * <p>此实现获取 @{@link jakarta.annotation.Priority Priority} 值，允许比常规 @{@link Order}
	 * 注解更多的语义：通常在多个匹配项的情况下只选择一个对象返回。
	 */
	@Override
	@Nullable
	public Integer getPriority(Object obj) {
		if (obj instanceof Class<?> clazz) {
			return OrderUtils.getPriority(clazz);
		}
		Integer priority = OrderUtils.getPriority(obj.getClass());
		if (priority == null && obj instanceof DecoratingProxy decoratingProxy) {
			return getPriority(decoratingProxy.getDecoratedClass());
		}
		return priority;
	}



	/**
	 * Sort the given list with a default {@link AnnotationAwareOrderComparator}.
	 * <p>使用默认的 {@link AnnotationAwareOrderComparator} 对给定列表进行排序。
	 * <p>Optimized to skip sorting for lists with size 0 or 1,
	 * in order to avoid unnecessary array extraction.
	 * <p>优化为跳过大小为 0 或 1 的列表的排序，
	 * 以避免不必要的数组提取。
	 * @param list the List to sort 要排序的列表
	 * @see java.util.List#sort(java.util.Comparator)
	 */
	public static void sort(List<?> list) {
		if (list.size() > 1) {
			list.sort(INSTANCE);
		}
	}


	/**
	 * Sort the given array with a default AnnotationAwareOrderComparator.
	 * <p>使用默认的 AnnotationAwareOrderComparator 对给定数组进行排序。
	 * <p>Optimized to skip sorting for lists with size 0 or 1,
	 * in order to avoid unnecessary array extraction.
	 * <p>优化为跳过大小为 0 或 1 的列表的排序，
	 * 以避免不必要的数组提取。
	 * @param array the array to sort 要排序的数组
	 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
	 */
	public static void sort(Object[] array) {
		if (array.length > 1) {
			Arrays.sort(array, INSTANCE);
		}
	}


	/**
	 * Sort the given array or List with a default AnnotationAwareOrderComparator,
	 * if necessary. Simply skips sorting when given any other value.
	 * <p>如有必要，使用默认的 AnnotationAwareOrderComparator 对给定的数组或列表进行排序。
	 * 如果给定其他值，则直接跳过排序。
	 * <p>Optimized to skip sorting for lists with size 0 or 1,
	 * in order to avoid unnecessary array extraction.
	 * <p>优化为跳过大小为 0 或 1 的列表的排序，
	 * 以避免不必要的数组提取。
	 * @param value the array or List to sort 要排序的数组或列表
	 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
	 */
	public static void sortIfNecessary(Object value) {
		if (value instanceof Object[] objects) {
			sort(objects);
		}
		else if (value instanceof List<?> list) {
			sort(list);
		}
	}

}
