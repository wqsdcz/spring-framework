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

package org.springframework.web.servlet.mvc.condition;

import java.util.Collection;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;

/**
 * 用于持有 {@link RequestCondition} 的包装类，在请求条件类型事先未知时（例如：自定义条件）非常有用。
 * 由于此类同样实现了 {@code RequestCondition} 接口，它实际上对被持有的请求条件进行了装饰，
 * 使其能够以类型安全且空值安全的方式与其他请求条件进行组合和比较。
 *
 * <p>
 *     当两个 {@code RequestConditionHolder} 实例相互组合或比较时，要求它们所持有的条件必须是相同类型。
 *     若类型不一致，将会抛出 {@link ClassCastException}。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class RequestConditionHolder extends AbstractRequestCondition<RequestConditionHolder> {

	@Nullable
	private final RequestCondition<Object> condition;


	/**
	 * 创建一个新的包装器来持有指定的请求条件。
	 * @param requestCondition 要持有的条件，可以为 {@code null}
	 */
	@SuppressWarnings("unchecked")
	public RequestConditionHolder(@Nullable RequestCondition<?> requestCondition) {
		this.condition = (RequestCondition<Object>) requestCondition;
	}


	/**
	 * 返回持有的请求条件，如果未持有任何条件则返回 {@code null}。
	 */
	@Nullable
	public RequestCondition<?> getCondition() {
		return this.condition;
	}

	@Override
	protected Collection<?> getContent() {
		return (this.condition != null ? Collections.singleton(this.condition) : Collections.emptyList());
	}

	@Override
	protected String getToStringInfix() {
		return " ";
	}

	/**
	 * 确保两个RequestConditionHolder实例持有的请求条件类型相同后，将它们所持有的条件进行组合。
	 * 若其中一个持有器为空，则直接返回另一个持有器。
	 */
	@Override
	public RequestConditionHolder combine(RequestConditionHolder other) {
		if (this.condition == null && other.condition == null) {
			return this;
		}
		else if (this.condition == null) {
			return other;
		}
		else if (other.condition == null) {
			return this;
		}
		else {
			assertEqualConditionTypes(this.condition, other.condition);
			RequestCondition<?> combined = (RequestCondition<?>) this.condition.combine(other.condition);
			return new RequestConditionHolder(combined);
		}
	}

	/**
	 * 确保所持有的请求条件具有相同类型。
	 */
	private void assertEqualConditionTypes(RequestCondition<?> thisCondition, RequestCondition<?> otherCondition) {
		Class<?> clazz = thisCondition.getClass();
		Class<?> otherClazz = otherCondition.getClass();
		if (!clazz.equals(otherClazz)) {
			throw new ClassCastException("Incompatible request conditions: " + clazz + " and " + otherClazz);
		}
	}

	/**
	 * 获取被持有请求条件的匹配条件，并将其包装到新的RequestConditionHolder实例中。
	 * 如果当前是空持有器，则返回相同的持有器实例。
	 */
	@Override
	@Nullable
	public RequestConditionHolder getMatchingCondition(HttpServletRequest request) {
		if (this.condition == null) {
			return this;
		}
		RequestCondition<?> match = (RequestCondition<?>) this.condition.getMatchingCondition(request);
		return (match != null ? new RequestConditionHolder(match) : null);
	}

	/**
	 * 确保两个RequestConditionHolder实例持有的请求条件类型相同后，比较它们所持有的条件。
	 * 若其中一个持有器为空，则优先选择另一个持有器。
	 */
	@Override
	public int compareTo(RequestConditionHolder other, HttpServletRequest request) {
		if (this.condition == null && other.condition == null) {
			return 0;
		}
		else if (this.condition == null) {
			return 1;
		}
		else if (other.condition == null) {
			return -1;
		}
		else {
			assertEqualConditionTypes(this.condition, other.condition);
			return this.condition.compareTo(other.condition, request);
		}
	}

}
