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


/**
 * {@code Ordered} is an interface that can be implemented by objects that
 * should be <em>orderable</em>, for example in a {@code Collection}.
 * <p>{@code Ordered} 是一个接口，可由应该具有<em>排序能力</em>的对象实现，例如在 {@code Collection} 中。
 *
 * <p>The actual {@link #getOrder() order} can be interpreted as prioritization,
 * with the first object (with the lowest order value) having the highest
 * priority.
 * <p>实际的 {@link #getOrder() 排序值} 可以解释为优先级，
 * 第一个对象（具有最低排序值）具有最高优先级。
 *
 * <p>Note that there is also a <em>priority</em> marker for this interface:
 * {@link PriorityOrdered}. Consult the Javadoc for {@code PriorityOrdered} for
 * details on how {@code PriorityOrdered} objects are ordered relative to
 * <em>plain</em> {@link Ordered} objects.
 * <p>注意，此接口还有一个<em>优先级</em>标记：
 * {@link PriorityOrdered}。有关如何相对于
 * <em>普通</em> {@link Ordered} 对象对 {@code PriorityOrdered} 对象进行排序的详细信息，
 * 请查阅 {@code PriorityOrdered} 的 Javadoc。
 *
 * <p>Consult the Javadoc for {@link OrderComparator} for details on the
 * sort semantics for non-ordered objects.
 * <p>有关未排序对象的排序语义的详细信息，请参阅 {@link OrderComparator} 的 Javadoc。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 07.04.2003
 * @see PriorityOrdered
 * @see OrderComparator
 * @see org.springframework.core.annotation.Order
 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
 */
public interface Ordered {


	/**
	 * Useful constant for the highest precedence value.
	 * <p>最高优先级值的有用常量。
	 * @see java.lang.Integer#MIN_VALUE
	 */
	int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;


	/**
	 * Useful constant for the lowest precedence value.
	 * <p>最低优先级值的有用常量。
	 * @see java.lang.Integer#MAX_VALUE
	 */
	int LOWEST_PRECEDENCE = Integer.MAX_VALUE;


	/**
	 * Get the order value of this object.
	 * <p>获取此对象的顺序值。
	 * <p>Higher values are interpreted as lower priority. As a consequence,
	 * the object with the lowest value has the highest priority (somewhat
	 * analogous to Servlet {@code load-on-startup} values).
	 * <p>较高的值被解释为较低的优先级。因此，
	 * 值最低的对象具有最高优先级（类似于 Servlet {@code load-on-startup} 值）。
	 * <p>Same order values will result in arbitrary sort positions for the
	 * affected objects.
	 * <p>相同的顺序值将导致受影响对象的排序位置任意。
	 * @return the order value
	 *         顺序值
	 * @see #HIGHEST_PRECEDENCE
	 * @see #LOWEST_PRECEDENCE
	 */
	int getOrder();

}
