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

package org.springframework.core;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * <p>针对 {@link Ordered} 对象的 {@link Comparator} 实现，按顺序值升序排序，或者按优先级降序排序（目前getPriority方法还没有提供实现）。
 * <p>
 * <h3>{@code PriorityOrdered} Objects</h3>
 * {@link PriorityOrdered} 对象将比普通 {@code Ordered} 对象具有更高的优先级进行排序。
 * <p>
 * <h3>Same Order Objects</h3>
 . * 具有相同顺序值的对象将相对于其他具有相同顺序值的对象进行任意排序。
 * <p>
 * <h3>Non-ordered Objects</h3>
 * 任何不提供自身顺序值的对象都被隐式分配一个 {@link Ordered#LOWEST_PRECEDENCE} 值，
 * 因此在排序集合的末尾与其他具有相同顺序值的对象按任意顺序排列。
 * <p>
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 07.04.2003
 * @see Ordered
 * @see PriorityOrdered
 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
 * @see java.util.List#sort(java.util.Comparator)
 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
 */
public class OrderComparator implements Comparator<Object> {

	/**
	 * Shared default instance of {@code OrderComparator}.
	 * 共享的 {@code OrderComparator} 默认实例。
	 */
	public static final OrderComparator INSTANCE = new OrderComparator();


	/**
	 * Build an adapted order comparator with the given source provider.
	 * <p>使用给定的源提供程序构建适配的顺序比较器。
	 * @param sourceProvider the order source provider to use 要使用的顺序源提供程序
	 * @return the adapted comparator 适配的比较器
	 * @since 4.1
	 */
	public Comparator<Object> withSourceProvider(OrderSourceProvider sourceProvider) {
		// 返回一个使用指定源提供程序的比较器实现
		return (o1, o2) -> doCompare(o1, o2, sourceProvider);
	}

	@Override
	public int compare(@Nullable Object o1, @Nullable Object o2) {
		return doCompare(o1, o2, null);
	}

	private int doCompare(@Nullable Object o1, @Nullable Object o2, @Nullable OrderSourceProvider sourceProvider) {
		boolean p1 = (o1 instanceof PriorityOrdered);
		boolean p2 = (o2 instanceof PriorityOrdered);
		if (p1 && !p2) {
			return -1;
		}
		else if (p2 && !p1) {
			return 1;
		}

		int i1 = getOrder(o1, sourceProvider);
		int i2 = getOrder(o2, sourceProvider);
		return Integer.compare(i1, i2);
	}

	/**
	 * Determine the order value for the given object.
	 * <p>确定给定对象的顺序值。
	 * <p>The default implementation checks against the given {@link OrderSourceProvider}
	 * using {@link #findOrder} and falls back to a regular {@link #getOrder(Object)} call.
	 * <p>默认实现使用 {@link #findOrder} 检查给定的 {@link OrderSourceProvider}，
	 * 并回退到常规的 {@link #getOrder(Object)} 调用。
	 * @param obj the object to check  要检查的对象
	 * @return the order value, or {@code Ordered.LOWEST_PRECEDENCE} as fallback  顺序值，或作为回退的 {@code Ordered.LOWEST_PRECEDENCE}
	 */
	private int getOrder(@Nullable Object obj, @Nullable OrderSourceProvider sourceProvider) {
		Integer order = null;
		if (obj != null && sourceProvider != null) {
			// 从源提供程序获取顺序源对象
			Object orderSource = sourceProvider.getOrderSource(obj);
			if (orderSource != null) {
				// 如果顺序源是数组，则遍历数组寻找顺序值
				if (orderSource.getClass().isArray()) {
					for (Object source : ObjectUtils.toObjectArray(orderSource)) {
						order = findOrder(source);
						if (order != null) {
							// 找到顺序值则跳出循环
							break;
						}
					}
				}
				else {
					// 如果顺序源不是数组，则直接查找顺序值
					order = findOrder(orderSource);
				}
			}
		}
		// 如果找到顺序值则返回，否则使用常规的getOrder方法获取
		return (order != null ? order : getOrder(obj));
	}

	/**
	 * Determine the order value for the given object.
	 * <p>确定给定对象的顺序值。
	 * <p>The default implementation checks against the {@link Ordered} interface
	 * through delegating to {@link #findOrder}. Can be overridden in subclasses.
	 * <p>默认实现通过委托给 {@link #findOrder} 来检查 {@link Ordered} 接口。可以在子类中重写。
	 * @param obj the object to check 要检查的对象
	 * @return the order value, or {@code Ordered.LOWEST_PRECEDENCE} as fallback  顺序值，或作为回退的 {@code Ordered.LOWEST_PRECEDENCE}
	 */
	protected int getOrder(@Nullable Object obj) {
		if (obj != null) {
			// 查找对象的顺序值
			Integer order = findOrder(obj);
			if (order != null) {
				// 如果找到顺序值则返回
				return order;
			}
		}
		// 如果没有找到顺序值，则返回最低优先级
		return Ordered.LOWEST_PRECEDENCE;
	}

	/**
	 * Find an order value indicated by the given object.
	 * <p>查找由给定对象指示的顺序值。
	 * <p>The default implementation checks against the {@link Ordered} interface.
	 * <p>默认实现检查 {@link Ordered} 接口。
	 * Can be overridden in subclasses.
	 * 可以在子类中重写。
	 * @param obj the object to check 要检查的对象
	 * @return the order value, or {@code null} if none found  顺序值，如果未找到则为 {@code null}
	 */
	@Nullable
	protected Integer findOrder(Object obj) {
		// 检查对象是否实现了Ordered接口，如果是则返回其顺序值，否则返回null
		return (obj instanceof Ordered ordered ? ordered.getOrder() : null);
	}

	/**
	 * Determine a priority value for the given object, if any.
	 * <p>确定给定对象的优先级值（如果存在）。
	 * <p>The default implementation always returns {@code null}.
	 * <p>默认实现始终返回 {@code null}。
	 * <p>Subclasses may override this to give specific kinds of values a
	 * 'priority' characteristic, in addition to their 'order' semantics.
	 * <p>子类可以重写此方法，除了'顺序'语义外，还为特定类型的值提供'优先级'特性。
	 * <p>A priority indicates that it may be used for selecting one object over
	 * another, in addition to serving for ordering purposes in a list/array.
	 * <p>优先级表示它可用于在列表/数组的排序目的之外选择一个对象而不是另一个对象。
	 * 与排序相关的优先级处理，优先级高的对象会被优先选择。
	 * @param obj the object to check 要检查的对象
	 * @return the priority value, or {@code null} if none   优先级值，如果没有则为 {@code null}
	 * @since 4.1
	 */
	@Nullable
	public Integer getPriority(Object obj) {
		return null;
	}


	/**
	 * Sort the given List with a default OrderComparator.
	 * <p>使用默认的OrderComparator对给定的List进行排序。
	 * <p>Optimized to skip sorting for lists with size 0 or 1,
	 * in order to avoid unnecessary array extraction.
	 * <p>经过优化，跳过大小为0或1的列表的排序，以避免不必要的数组提取。
	 * @param list the List to sort 要排序的List
	 * @see java.util.List#sort(java.util.Comparator)
	 */
	public static void sort(List<?> list) {
		// 只有当列表大小大于1时才进行排序，优化性能
		if (list.size() > 1) {
			list.sort(INSTANCE);
		}
	}

	/**
	 * Sort the given array with a default OrderComparator.
	 * <p>使用默认的OrderComparator对给定的List进行排序。
	 * <p>Optimized to skip sorting for lists with size 0 or 1,
	 * in order to avoid unnecessary array extraction.
	 * <p>经过优化，跳过大小为0或1的列表的排序，以避免不必要的数组提取。
	 * @param array the array to sort 要排序的List
	 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
	 */
	public static void sort(Object[] array) {
		if (array.length > 1) {
			Arrays.sort(array, INSTANCE);
		}
	}

	/**
	 * Sort the given array or List with a default OrderComparator,
	 * if necessary. Simply skips sorting when given any other value.
	 * <p>如果有必要，使用默认的OrderComparator对给定的数组或List进行排序。如果给定其他值，则简单地跳过排序。
	 * <p>Optimized to skip sorting for lists with size 0 or 1,
	 * in order to avoid unnecessary array extraction.
	 * <p>经过优化，跳过大小为0或1的列表的排序，以避免不必要的数组提取。
	 * @param value the array or List to sort
	 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
	 */
	public static void sortIfNecessary(Object value) {
		// 检查输入值是否为数组，如果是则进行排序
		if (value instanceof Object[] objects) {
			sort(objects);
		}
		// 检查输入值是否为List，如果是则进行排序
		else if (value instanceof List<?> list) {
			sort(list);
		}
	}


	/**
	 * Strategy interface to provide an order source for a given object.
	 * <p>策略接口，为给定对象提供顺序源。
	 * @since 4.1
	 */
	@FunctionalInterface
	public interface OrderSourceProvider {

		/**
		 * <p>返回指定对象的顺序源，即应该检查顺序值以替代给定对象的对象。
		 * <p>也可以是顺序源对象的数组。
		 * <p>如果返回的对象不表示任何顺序，则比较器将回退到检查原始对象。
		 * @param obj the object to find an order source for
		 * @return 该对象的顺序源，如果未找到则为 {@code null}
		 */
		@Nullable
		Object getOrderSource(Object obj);
	}

}
