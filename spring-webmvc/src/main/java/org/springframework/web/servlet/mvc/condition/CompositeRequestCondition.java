/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.mvc.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 通过委托多个 {@code RequestCondition} 类型并使用逻辑与（' && '）操作来确保所有条件都匹配给定请求，
 * 从而实现 {@link RequestCondition} 契约。
 *
 * <p>
 *     当对 {@code CompositeRequestCondition} 实例进行组合或比较时，要求它们必须满足：
 *     (a) 包含相同数量的条件；
 *     (b) 对应索引位置的条件具有相同类型。
 *     允许向构造函数传递 {@code null} 条件或不提供任何条件。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class CompositeRequestCondition extends AbstractRequestCondition<CompositeRequestCondition> {

	private final RequestConditionHolder[] requestConditions;


	/**
	 * 创建一个包含0个或多个{@code RequestCondition}类型的实例。
	 * 必须确保创建的{@code CompositeRequestCondition}实例具有相同数量的条件，以便进行对比和组合。
	 * 允许传入{@code null}条件。
	 */
	public CompositeRequestCondition(RequestCondition<?>... requestConditions) {
		this.requestConditions = wrap(requestConditions);
	}

	private CompositeRequestCondition(RequestConditionHolder[] requestConditions) {
		this.requestConditions = requestConditions;
	}


	private RequestConditionHolder[] wrap(RequestCondition<?>... rawConditions) {
		RequestConditionHolder[] wrappedConditions = new RequestConditionHolder[rawConditions.length];
		for (int i = 0; i < rawConditions.length; i++) {
			wrappedConditions[i] = new RequestConditionHolder(rawConditions[i]);
		}
		return wrappedConditions;
	}

	/**
	 * 判断该实例是否包含0个条件。
	 */
	@Override
	public boolean isEmpty() {
		return ObjectUtils.isEmpty(this.requestConditions);
	}

	/**
	 * 返回底层条件集合（可能为空但绝不会为{@code null}）。
	 */
	public List<RequestCondition<?>> getConditions() {
		return unwrap();
	}

	private List<RequestCondition<?>> unwrap() {
		List<RequestCondition<?>> result = new ArrayList<>();
		for (RequestConditionHolder holder : this.requestConditions) {
			result.add(holder.getCondition());
		}
		return result;
	}

	@Override
	protected Collection<?> getContent() {
		return (!isEmpty() ? getConditions() : Collections.emptyList());
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	private int getLength() {
		return this.requestConditions.length;
	}

	/**
	 * 如果其中一个实例为空，则返回另一个实例。
	 * 如果两个实例都包含条件，则在确保条件类型和数量相同后，对各个条件进行组合。
	 */
	@Override
	public CompositeRequestCondition combine(CompositeRequestCondition other) {
		if (isEmpty() && other.isEmpty()) {
			return this;
		}
		else if (other.isEmpty()) {
			return this;
		}
		else if (isEmpty()) {
			return other;
		}
		else {
			assertNumberOfConditions(other);
			RequestConditionHolder[] combinedConditions = new RequestConditionHolder[getLength()];
			for (int i = 0; i < getLength(); i++) {
				combinedConditions[i] = this.requestConditions[i].combine(other.requestConditions[i]);
			}
			return new CompositeRequestCondition(combinedConditions);
		}
	}

	private void assertNumberOfConditions(CompositeRequestCondition other) {
		Assert.isTrue(getLength() == other.getLength(),
				"Cannot combine CompositeRequestConditions with a different number of conditions. " +
				ObjectUtils.nullSafeToString(this.requestConditions) + " and  " +
				ObjectUtils.nullSafeToString(other.requestConditions));
	}

	/**
	 * 委托给<em>所有</em>包含的条件来匹配请求，并返回最终的"匹配"条件实例。
	 * <p>空的{@code CompositeRequestCondition}会匹配所有请求。
	 */
	@Override
	@Nullable
	public CompositeRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (isEmpty()) {
			return this;
		}
		RequestConditionHolder[] matchingConditions = new RequestConditionHolder[getLength()];
		for (int i = 0; i < getLength(); i++) {
			matchingConditions[i] = this.requestConditions[i].getMatchingCondition(request);
			if (matchingConditions[i] == null) {
				return null;
			}
		}
		return new CompositeRequestCondition(matchingConditions);
	}

	/**
	 * 如果一个实例为空，则另一个实例"胜出"。如果两个实例都包含条件，则按照它们被提供的顺序进行比较。
	 */
	@Override
	public int compareTo(CompositeRequestCondition other, HttpServletRequest request) {
		if (isEmpty() && other.isEmpty()) {
			return 0;
		}
		else if (isEmpty()) {
			return 1;
		}
		else if (other.isEmpty()) {
			return -1;
		}
		else {
			assertNumberOfConditions(other);
			for (int i = 0; i < getLength(); i++) {
				int result = this.requestConditions[i].compareTo(other.requestConditions[i], request);
				if (result != 0) {
					return result;
				}
			}
			return 0;
		}
	}

}
