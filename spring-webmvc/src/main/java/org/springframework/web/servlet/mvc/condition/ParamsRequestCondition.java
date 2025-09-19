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
import org.springframework.web.util.WebUtils;

/**
 * 一种逻辑与（' && '）请求条件，用于将请求与一组参数表达式进行匹配，参数表达式的语法在 {@link RequestMapping#params()} 中定义。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ParamsRequestCondition extends AbstractRequestCondition<ParamsRequestCondition> {

	private final Set<ParamExpression> expressions;


	/**
	 * 根据给定的参数表达式创建新实例。
	 * @param params 符合 {@link RequestMapping#params()} 语法定义的表达式；
	 *               如果为0，则该条件将匹配所有请求。
	 */
	public ParamsRequestCondition(String... params) {
		this(parseExpressions(params));
	}

	private ParamsRequestCondition(Collection<ParamExpression> conditions) {
		this.expressions = Collections.unmodifiableSet(new LinkedHashSet<>(conditions));
	}


	private static Collection<ParamExpression> parseExpressions(String... params) {
		Set<ParamExpression> expressions = new LinkedHashSet<>();
		for (String param : params) {
			expressions.add(new ParamExpression(param));
		}
		return expressions;
	}


	/**
	 * 返回包含的请求参数表达式。
	 */
	public Set<NameValueExpression<String>> getExpressions() {
		return new LinkedHashSet<>(this.expressions);
	}

	@Override
	protected Collection<ParamExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	/**
	 * 返回一个新实例，包含"this"和"other"实例中参数表达式的并集。
	 */
	@Override
	public ParamsRequestCondition combine(ParamsRequestCondition other) {
		Set<ParamExpression> set = new LinkedHashSet<>(this.expressions);
		set.addAll(other.expressions);
		return new ParamsRequestCondition(set);
	}

	/**
	 * 如果请求匹配所有参数表达式，则返回"this"实例；否则返回{@code null}。
	 */
	@Override
	@Nullable
	public ParamsRequestCondition getMatchingCondition(HttpServletRequest request) {
		for (ParamExpression expression : expressions) {
			if (!expression.match(request)) {
				return null;
			}
		}
		return this;
	}

	/**
	 * 返回比较结果：
	 * <ul>
	 *     <li>如果两个条件具有相同数量的参数表达式，返回0</li>
	 *     <li>如果"this"实例具有更多参数表达式，返回小于0的值</li>
	 *     <li>如果"other"实例具有更多参数表达式，返回大于0的值</li>
	 * </ul>
	 *
	 * <p>
	 *     假定两个实例都是通过{@link #getMatchingCondition(HttpServletRequest)}获取的，
	 *     且每个实例仅包含匹配的参数表达式，或者为空。
	 */
	@Override
	public int compareTo(ParamsRequestCondition other, HttpServletRequest request) {
		return (other.expressions.size() - this.expressions.size());
	}


	/**
	 * 解析单个参数表达式并与请求进行匹配。
	 */
	static class ParamExpression extends AbstractNameValueExpression<String> {

		ParamExpression(String expression) {
			super(expression);
		}

		@Override
		protected boolean isCaseSensitiveName() {
			return true;
		}

		@Override
		protected String parseValue(String valueExpression) {
			return valueExpression;
		}

		@Override
		protected boolean matchName(HttpServletRequest request) {
			return (WebUtils.hasSubmitParameter(request, this.name) ||
					request.getParameterMap().containsKey(this.name));
		}

		@Override
		protected boolean matchValue(HttpServletRequest request) {
			return ObjectUtils.nullSafeEquals(this.value, request.getParameter(this.name));
		}
	}

}
