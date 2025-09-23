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

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition.HeaderExpression;

/**
 * 一种逻辑析取（' || '）请求条件，用于将请求的 'Accept' 头部与一系列媒体类型表达式进行匹配。
 * 支持两种媒体类型表达式，其描述详见 {@link RequestMapping#produces()} 和{@link RequestMapping#headers()}（其中头部名称为 'Accept'）。
 * 无论使用哪种语法，语义都是相同的。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ProducesRequestCondition extends AbstractRequestCondition<ProducesRequestCondition> {

	private static final ProducesRequestCondition PRE_FLIGHT_MATCH = new ProducesRequestCondition();

	private static final ProducesRequestCondition EMPTY_CONDITION = new ProducesRequestCondition();

	private static final List<ProduceMediaTypeExpression> MEDIA_TYPE_ALL_LIST =
			Collections.singletonList(new ProduceMediaTypeExpression(MediaType.ALL_VALUE));


	private final List<ProduceMediaTypeExpression> expressions;

	private final ContentNegotiationManager contentNegotiationManager;


	/**
	 * 根据"produces"表达式创建新实例。如果总共提供0个表达式，此条件将匹配任何请求。
	 * @param produces 符合{@link RequestMapping#produces()}语法定义的表达式
	 */
	public ProducesRequestCondition(String... produces) {
		this(produces, null, null);
	}

	/**
	 * 使用"produces"和"header"表达式创建新实例。
	 * 头部名称不是'Accept'或未定义头部值的"header"表达式将被忽略。
	 * 如果总共提供0个表达式，此条件将匹配任何请求。
	 * @param produces 符合{@link RequestMapping#produces()}语法定义的表达式
	 * @param headers 符合{@link RequestMapping#headers()}语法定义的表达式
	 */
	public ProducesRequestCondition(String[] produces, @Nullable String[] headers) {
		this(produces, headers, null);
	}

	/**
	 * 与{@link #ProducesRequestCondition(String[], String[])}相同，但额外接受{@link ContentNegotiationManager}参数。
	 *
	 * @param produces 符合{@link RequestMapping#produces()}语法定义的表达式
	 * @param headers 符合{@link RequestMapping#headers()}语法定义的表达式
	 * @param manager 用于确定请求的媒体类型
	 */
	public ProducesRequestCondition(String[] produces, @Nullable String[] headers,
			@Nullable ContentNegotiationManager manager) {

		this.expressions = new ArrayList<>(parseExpressions(produces, headers));
		Collections.sort(this.expressions);
		this.contentNegotiationManager = (manager != null ? manager : new ContentNegotiationManager());
	}

	/**
	 * 使用已解析的媒体类型表达式的私有构造函数。
	 */
	private ProducesRequestCondition(Collection<ProduceMediaTypeExpression> expressions,
			@Nullable ContentNegotiationManager manager) {

		this.expressions = new ArrayList<>(expressions);
		Collections.sort(this.expressions);
		this.contentNegotiationManager = (manager != null ? manager : new ContentNegotiationManager());
	}


	private Set<ProduceMediaTypeExpression> parseExpressions(String[] produces, @Nullable String[] headers) {
		Set<ProduceMediaTypeExpression> result = new LinkedHashSet<>();
		if (headers != null) {
			for (String header : headers) {
				HeaderExpression expr = new HeaderExpression(header);
				if ("Accept".equalsIgnoreCase(expr.name) && expr.value != null) {
					for (MediaType mediaType : MediaType.parseMediaTypes(expr.value)) {
						result.add(new ProduceMediaTypeExpression(mediaType, expr.isNegated));
					}
				}
			}
		}
		for (String produce : produces) {
			result.add(new ProduceMediaTypeExpression(produce));
		}
		return result;
	}

	/**
	 * 返回包含的"produces"表达式。
	 */
	public Set<MediaTypeExpression> getExpressions() {
		return new LinkedHashSet<>(this.expressions);
	}

	/**
	 * 返回包含的可生产媒体类型（排除否定表达式）。
	 */
	public Set<MediaType> getProducibleMediaTypes() {
		Set<MediaType> result = new LinkedHashSet<>();
		for (ProduceMediaTypeExpression expression : this.expressions) {
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
	protected List<ProduceMediaTypeExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 如果"other"实例包含任何表达式，则返回"other"实例；否则返回"this"实例。
	 * 实际上这意味着方法级别的"produces"条件会覆盖类型级别的"produces"条件。
	 */
	@Override
	public ProducesRequestCondition combine(ProducesRequestCondition other) {
		return (!other.expressions.isEmpty() ? other : this);
	}

	/**
	 * 检查包含的媒体类型表达式是否与给定请求的'Content-Type'头部匹配，
	 * 并返回保证仅包含匹配表达式的新实例。通过
	 * {@link MediaType#isCompatibleWith(MediaType)}方法执行匹配。
	 * @param request 当前请求
	 * @return 如果没有表达式，则返回相同实例；
	 *         或者返回包含匹配表达式的新条件；
	 *         或者如果没有表达式匹配，则返回{@code null}。
	 */
	@Override
	@Nullable
	public ProducesRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (CorsUtils.isPreFlightRequest(request)) {
			return PRE_FLIGHT_MATCH;
		}
		if (isEmpty()) {
			return this;
		}

		List<MediaType> acceptedMediaTypes;
		try {
			acceptedMediaTypes = getAcceptedMediaTypes(request);
		}
		catch (HttpMediaTypeException ex) {
			return null;
		}

		Set<ProduceMediaTypeExpression> result = new LinkedHashSet<>(this.expressions);
		result.removeIf(expression -> !expression.match(acceptedMediaTypes));
		if (!result.isEmpty()) {
			return new ProducesRequestCondition(result, this.contentNegotiationManager);
		}
		else if (acceptedMediaTypes.contains(MediaType.ALL)) {
			return EMPTY_CONDITION;
		}
		else {
			return null;
		}
	}

	/**
	 * 按以下方式比较此"produces"条件与另一个"produces"条件：
	 * <ol>
	 * <li>通过{@link MediaType#sortByQualityValue(List)}按质量值对'Accept'头部媒体类型排序并遍历列表
	 * <li>在每个"produces"条件中首先通过{@link MediaType#equals(Object)}匹配，
	 *     然后通过{@link MediaType#includes(MediaType)}匹配，获取匹配媒体类型的第一个索引
	 * <li>如果找到较低索引，则该索引处的条件获胜
	 * <li>如果两个索引相等，则使用{@link MediaType#SPECIFICITY_COMPARATOR}
	 *     进一步比较该索引处的媒体类型
	 * </ol>
	 * <p>假定两个实例都是通过{@link #getMatchingCondition(HttpServletRequest)}获取的，
	 * 且每个实例仅包含匹配的可生产媒体类型表达式，或者为空。
	 */
	@Override
	public int compareTo(ProducesRequestCondition other, HttpServletRequest request) {
		try {
			List<MediaType> acceptedMediaTypes = getAcceptedMediaTypes(request);
			for (MediaType acceptedMediaType : acceptedMediaTypes) {
				int thisIndex = this.indexOfEqualMediaType(acceptedMediaType);
				int otherIndex = other.indexOfEqualMediaType(acceptedMediaType);
				int result = compareMatchingMediaTypes(this, thisIndex, other, otherIndex);
				if (result != 0) {
					return result;
				}
				thisIndex = this.indexOfIncludedMediaType(acceptedMediaType);
				otherIndex = other.indexOfIncludedMediaType(acceptedMediaType);
				result = compareMatchingMediaTypes(this, thisIndex, other, otherIndex);
				if (result != 0) {
					return result;
				}
			}
			return 0;
		}
		catch (HttpMediaTypeNotAcceptableException ex) {
			// should never happen
			throw new IllegalStateException("Cannot compare without having any requested media types", ex);
		}
	}

	private List<MediaType> getAcceptedMediaTypes(HttpServletRequest request) throws HttpMediaTypeNotAcceptableException {
		return this.contentNegotiationManager.resolveMediaTypes(new ServletWebRequest(request));
	}

	private int indexOfEqualMediaType(MediaType mediaType) {
		for (int i = 0; i < getExpressionsToCompare().size(); i++) {
			MediaType currentMediaType = getExpressionsToCompare().get(i).getMediaType();
			if (mediaType.getType().equalsIgnoreCase(currentMediaType.getType()) &&
					mediaType.getSubtype().equalsIgnoreCase(currentMediaType.getSubtype())) {
				return i;
			}
		}
		return -1;
	}

	private int indexOfIncludedMediaType(MediaType mediaType) {
		for (int i = 0; i < getExpressionsToCompare().size(); i++) {
			if (mediaType.includes(getExpressionsToCompare().get(i).getMediaType())) {
				return i;
			}
		}
		return -1;
	}

	private int compareMatchingMediaTypes(ProducesRequestCondition condition1, int index1,
			ProducesRequestCondition condition2, int index2) {

		int result = 0;
		if (index1 != index2) {
			result = index2 - index1;
		}
		else if (index1 != -1) {
			ProduceMediaTypeExpression expr1 = condition1.getExpressionsToCompare().get(index1);
			ProduceMediaTypeExpression expr2 = condition2.getExpressionsToCompare().get(index2);
			result = expr1.compareTo(expr2);
			result = (result != 0) ? result : expr1.getMediaType().compareTo(expr2.getMediaType());
		}
		return result;
	}

	/**
	 * 返回包含的"produces"表达式；如果为空，则返回包含
	 * {@value MediaType#ALL_VALUE}表达式的列表。
	 */
	private List<ProduceMediaTypeExpression> getExpressionsToCompare() {
		return (this.expressions.isEmpty() ? MEDIA_TYPE_ALL_LIST : this.expressions);
	}


	/**
	 * 解析单个媒体类型表达式并与请求的'Accept'头部进行匹配。
	 */
	static class ProduceMediaTypeExpression extends AbstractMediaTypeExpression {

		ProduceMediaTypeExpression(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		ProduceMediaTypeExpression(String expression) {
			super(expression);
		}

		public final boolean match(List<MediaType> acceptedMediaTypes) {
			boolean match = matchMediaType(acceptedMediaTypes);
			return (!isNegated() ? match : !match);
		}

		private boolean matchMediaType(List<MediaType> acceptedMediaTypes) {
			for (MediaType acceptedMediaType : acceptedMediaTypes) {
				if (getMediaType().isCompatibleWith(acceptedMediaType)) {
					return true;
				}
			}
			return false;
		}
	}

}
