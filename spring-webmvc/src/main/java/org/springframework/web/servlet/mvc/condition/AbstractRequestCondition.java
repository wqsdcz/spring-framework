/*
 * Copyright 2002-2019 the original author or authors.
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
import java.util.Iterator;

import org.springframework.lang.Nullable;

/**
 * {@link RequestCondition} 类型的抽象基类，提供{@link #equals(Object)}、{@link #hashCode()} 和 {@link #toString()} 的默认实现。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @param <T> 可与此 RequestCondition 进行组合和比较的对象类型
 */
public abstract class AbstractRequestCondition<T extends AbstractRequestCondition<T>> implements RequestCondition<T> {

	/**
	 * 指示该条件是否为空，即是否包含任何离散项。
	 * @return 如果为空则返回 {@code true}；否则返回 {@code false}
	 */
	public boolean isEmpty() {
		return getContent().isEmpty();
	}

	/**
	 * 返回该请求条件所包含的离散项集合。
	 * <p>例如：URL模式、HTTP请求方法、参数表达式等。
	 * @return 对象集合（永远不为 {@code null}）
	 */
	protected abstract Collection<?> getContent();

	/**
	 * 当打印内容的离散项时使用的分隔符符号。
	 * <p>例如：URL模式使用 {@code " || "}，参数表达式使用 {@code " && "}。
	 */
	protected abstract String getToStringInfix();


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		return getContent().equals(((AbstractRequestCondition<?>) other).getContent());
	}

	@Override
	public int hashCode() {
		return getContent().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("[");
		for (Iterator<?> iterator = getContent().iterator(); iterator.hasNext();) {
			Object expression = iterator.next();
			builder.append(expression.toString());
			if (iterator.hasNext()) {
				builder.append(getToStringInfix());
			}
		}
		builder.append("]");
		return builder.toString();
	}

}
