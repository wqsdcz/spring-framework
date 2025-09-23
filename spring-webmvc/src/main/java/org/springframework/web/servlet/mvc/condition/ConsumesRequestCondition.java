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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition.HeaderExpression;

/**
 * 一个逻辑或（'||'）请求条件，用于将请求的'Content-Type'头部与媒体类型表达式列表进行匹配。
 * 支持两种媒体类型表达式，其描述详见：
 * {@link RequestMapping#consumes()}和{@link RequestMapping#headers()}（当标头名称为'Content-Type'时）。
 * 无论使用哪种语法，语义都是相同的。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ConsumesRequestCondition extends AbstractRequestCondition<ConsumesRequestCondition> {

	private static final ConsumesRequestCondition PRE_FLIGHT_MATCH = new ConsumesRequestCondition();

	private final List<ConsumeMediaTypeExpression> expressions;


	/**
	 * 从0个或多个"consumes"表达式创建新实例。
	 * @param consumes 使用{@link RequestMapping#consumes()}中描述的语法表达式；
	 * 如果提供0个表达式，则该条件将匹配所有请求
	 */
	public ConsumesRequestCondition(String... consumes) {
		this(consumes, null);
	}

	/**
	 * 使用"consumes"和"header"表达式创建新实例。
	 * 头部名称不是'Content-Type'或未定义头部值的"header"表达式将被忽略。
	 * 如果总共提供0个表达式，则该条件将匹配所有请求
	 *
	 * @param consumes 如{@link RequestMapping#consumes()}中所述
	 * @param headers 如{@link RequestMapping#headers()}中所述
	 */
	public ConsumesRequestCondition(String[] consumes, @Nullable String[] headers) {
		this(parseExpressions(consumes, headers));
	}

	/**
	 * 接受已解析的媒体类型表达式的私有构造函数。
	 */
	private ConsumesRequestCondition(Collection<ConsumeMediaTypeExpression> expressions) {
		this.expressions = new ArrayList<>(expressions);
		Collections.sort(this.expressions);
	}


	private static Set<ConsumeMediaTypeExpression> parseExpressions(String[] consumes, @Nullable String[] headers) {
		Set<ConsumeMediaTypeExpression> result = new LinkedHashSet<>();
		if (headers != null) {
			for (String header : headers) {
				HeaderExpression expr = new HeaderExpression(header);
				if ("Content-Type".equalsIgnoreCase(expr.name) && expr.value != null) {
					for (MediaType mediaType : MediaType.parseMediaTypes(expr.value)) {
						result.add(new ConsumeMediaTypeExpression(mediaType, expr.isNegated));
					}
				}
			}
		}
		for (String consume : consumes) {
			result.add(new ConsumeMediaTypeExpression(consume));
		}
		return result;
	}


	/**
	 * 返回包含的媒体类型表达式。
	 */
	public Set<MediaTypeExpression> getExpressions() {
		return new LinkedHashSet<>(this.expressions);
	}

	/**
	 * 返回此条件中的媒体类型（不包括否定表达式）。
	 */
	public Set<MediaType> getConsumableMediaTypes() {
		Set<MediaType> result = new LinkedHashSet<>();
		for (ConsumeMediaTypeExpression expression : this.expressions) {
			if (!expression.isNegated()) {
				result.add(expression.getMediaType());
			}
		}
		return result;
	}

	/**
	 * 判断该条件是否包含任何媒体类型表达式。
	 */
	@Override
	public boolean isEmpty() {
		return this.expressions.isEmpty();
	}

	@Override
	protected Collection<ConsumeMediaTypeExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 如果"other"实例包含任何表达式则返回该实例；否则返回"this"实例。
	 * 实际上这意味着方法级别的"consumes"条件会覆盖类型级别的"consumes"条件。
	 */
	@Override
	public ConsumesRequestCondition combine(ConsumesRequestCondition other) {
		return (!other.expressions.isEmpty() ? other : this);
	}

	/**
	 * 检查包含的媒体类型表达式是否与给定的请求'Content-Type'头部匹配，并返回确保仅包含匹配表达式的实例。
	 * 匹配通过{@link MediaType#includes(MediaType)}方法执行。
	 *
	 * @param request 当前请求
	 * @return 如果该条件不包含表达式，则返回相同实例；
	 *         或者返回仅包含匹配表达式的新条件；
	 *         或者当无表达式匹配时返回{@code null}
	 */
	@Override
	@Nullable
	public ConsumesRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (CorsUtils.isPreFlightRequest(request)) {
			return PRE_FLIGHT_MATCH;
		}
		if (isEmpty()) {
			return this;
		}

		MediaType contentType;
		try {
			contentType = (StringUtils.hasLength(request.getContentType()) ?
					MediaType.parseMediaType(request.getContentType()) :
					MediaType.APPLICATION_OCTET_STREAM);
		}
		catch (InvalidMediaTypeException ex) {
			return null;
		}

		Set<ConsumeMediaTypeExpression> result = new LinkedHashSet<>(this.expressions);
		result.removeIf(expression -> !expression.match(contentType));
		return (!result.isEmpty() ? new ConsumesRequestCondition(result) : null);
	}

	/**
	 * 返回比较结果：
	 * <ul>
	 *      <li>0 - 如果两个条件具有相同数量的表达式</li>
	 *      <li>小于0 - 如果"this"条件具有更多或更具体的媒体类型表达式</li>
	 *      <li>大于0 - 如果"other"条件具有更多或更具体的媒体类型表达式</li>
	 * </ul>
	 * <p>
	 *     假定两个实例都是通过{@link #getMatchingCondition(HttpServletRequest)}方法获取，
	 *     且每个实例仅包含匹配的消费媒体类型表达式，或者为空实例。
	 */
	@Override
	public int compareTo(ConsumesRequestCondition other, HttpServletRequest request) {
		if (this.expressions.isEmpty() && other.expressions.isEmpty()) {
			return 0;
		}
		else if (this.expressions.isEmpty()) {
			return 1;
		}
		else if (other.expressions.isEmpty()) {
			return -1;
		}
		else {
			return this.expressions.get(0).compareTo(other.expressions.get(0));
		}
	}


	/**
	 * 解析单个媒体类型表达式并将其与请求的'Content-Type'头部进行匹配。
	 */
	static class ConsumeMediaTypeExpression extends AbstractMediaTypeExpression {

		ConsumeMediaTypeExpression(String expression) {
			super(expression);
		}

		ConsumeMediaTypeExpression(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		public final boolean match(MediaType contentType) {
			boolean match = getMediaType().includes(contentType);
			return (!isNegated() ? match : !match);
		}
	}

}
