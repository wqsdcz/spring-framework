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
import java.util.LinkedHashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.CorsUtils;

/**
 * 一个逻辑与（'&&'）请求条件，根据{@link RequestMapping#headers()}中定义的语法，将请求与一组头部表达式进行匹配。
 *
 * <p>
 *     传递给构造函数的头部名称为'Accept'或'Content-Type'的表达式将被忽略。
 *     相关处理请参见{@link ConsumesRequestCondition}和{@link ProducesRequestCondition}。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class HeadersRequestCondition extends AbstractRequestCondition<HeadersRequestCondition> {

	private static final HeadersRequestCondition PRE_FLIGHT_MATCH = new HeadersRequestCondition();


	private final Set<HeaderExpression> expressions;


	/**
	 * 根据给定的头部表达式创建新实例。头部名称为'Accept'或'Content-Type'的表达式将被忽略。
	 * 相关处理请参见{@link ConsumesRequestCondition}和{@link ProducesRequestCondition}。
	 *
	 * @param headers 使用{@link RequestMapping#headers()}中定义语法的媒体类型表达式；
	 *                如果为0个表达式，则该条件将匹配所有请求
	 */
	public HeadersRequestCondition(String... headers) {
		this(parseExpressions(headers));
	}

	private HeadersRequestCondition(Collection<HeaderExpression> conditions) {
		this.expressions = Collections.unmodifiableSet(new LinkedHashSet<>(conditions));
	}


	private static Collection<HeaderExpression> parseExpressions(String... headers) {
		Set<HeaderExpression> expressions = new LinkedHashSet<>();
		for (String header : headers) {
			HeaderExpression expr = new HeaderExpression(header);
			if ("Accept".equalsIgnoreCase(expr.name) || "Content-Type".equalsIgnoreCase(expr.name)) {
				continue;
			}
			expressions.add(expr);
		}
		return expressions;
	}

	/**
	 * 返回包含的请求头部表达式。
	 */
	public Set<NameValueExpression<String>> getExpressions() {
		return new LinkedHashSet<>(this.expressions);
	}

	@Override
	protected Collection<HeaderExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	/**
	 * 返回一个包含"this"实例和"other"实例中头部表达式并集的新实例。
	 */
	@Override
	public HeadersRequestCondition combine(HeadersRequestCondition other) {
		Set<HeaderExpression> set = new LinkedHashSet<>(this.expressions);
		set.addAll(other.expressions);
		return new HeadersRequestCondition(set);
	}

	/**
	 * 如果请求匹配所有表达式，则返回"this"实例；否则返回{@code null}。
	 */
	@Override
	@Nullable
	public HeadersRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (CorsUtils.isPreFlightRequest(request)) {
			return PRE_FLIGHT_MATCH;
		}
		for (HeaderExpression expression : expressions) {
			if (!expression.match(request)) {
				return null;
			}
		}
		return this;
	}

	/**
	 * 返回比较结果：
	 * <ul>
	 *      <li>0 - 如果两个条件具有相同数量的头部表达式</li>
	 *      <li>小于0 - 如果"this"实例具有更多头部表达式</li>
	 *      <li>大于0 - 如果"other"实例具有更多头部表达式</li>
	 * </ul>
	 * <p>
	 *     假定两个实例都是通过{@link #getMatchingCondition(HttpServletRequest)}方法获取，
	 *     且每个实例仅包含匹配的头部表达式，或者为空实例。
	 */
	@Override
	public int compareTo(HeadersRequestCondition other, HttpServletRequest request) {
		return other.expressions.size() - this.expressions.size();
	}


	/**
	 * 解析单个头部表达式并将其与请求进行匹配。
	 */
	static class HeaderExpression extends AbstractNameValueExpression<String> {

		public HeaderExpression(String expression) {
			super(expression);
		}

		@Override
		protected boolean isCaseSensitiveName() {
			return false;
		}

		@Override
		protected String parseValue(String valueExpression) {
			return valueExpression;
		}

		@Override
		protected boolean matchName(HttpServletRequest request) {
			return (request.getHeader(this.name) != null);
		}

		@Override
		protected boolean matchValue(HttpServletRequest request) {
			return ObjectUtils.nullSafeEquals(this.value, request.getHeader(this.name));
		}
	}

}
