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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsUtils;

/**
 * 一种逻辑析取（' || '）请求条件，用于将请求与一组{@link RequestMethod}进行匹配。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class RequestMethodsRequestCondition extends AbstractRequestCondition<RequestMethodsRequestCondition> {

	private static final RequestMethodsRequestCondition GET_CONDITION =
			new RequestMethodsRequestCondition(RequestMethod.GET);

	private final Set<RequestMethod> methods;


	/**
	 * 使用给定的请求方法创建新实例。
	 * @param requestMethods 0个或多个HTTP请求方法；如果为0，则该条件将匹配所有请求
	 */
	public RequestMethodsRequestCondition(RequestMethod... requestMethods) {
		this(Arrays.asList(requestMethods));
	}

	private RequestMethodsRequestCondition(Collection<RequestMethod> requestMethods) {
		this.methods = Collections.unmodifiableSet(new LinkedHashSet<>(requestMethods));
	}


	/**
	 * 返回此条件中包含的所有{@link RequestMethod}。
	 */
	public Set<RequestMethod> getMethods() {
		return this.methods;
	}

	@Override
	protected Collection<RequestMethod> getContent() {
		return this.methods;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 返回一个新实例，包含"this"和"other"实例中HTTP请求方法的并集。
	 */
	@Override
	public RequestMethodsRequestCondition combine(RequestMethodsRequestCondition other) {
		Set<RequestMethod> set = new LinkedHashSet<>(this.methods);
		set.addAll(other.methods);
		return new RequestMethodsRequestCondition(set);
	}

	/**
	 * 检查是否存在与给定请求匹配的HTTP请求方法，并返回仅包含匹配HTTP请求方法的新实例。
	 * @param request 当前请求
	 * @return 如果条件为空，则返回相同实例（除非请求方法是HTTP OPTIONS），包含匹配请求方法的新条件实例，
	 *         如果没有匹配项或条件为空且请求方法是OPTIONS，则返回{@code null}
	 */
	@Override
	@Nullable
	public RequestMethodsRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (CorsUtils.isPreFlightRequest(request)) {
			return matchPreFlight(request);
		}

		if (getMethods().isEmpty()) {
			if (RequestMethod.OPTIONS.name().equals(request.getMethod()) &&
					!DispatcherType.ERROR.equals(request.getDispatcherType())) {

				return null; // No implicit match for OPTIONS (we handle it)
			}
			return this;
		}

		return matchRequestMethod(request.getMethod());
	}

	/**
	 * 在预检请求中，需匹配到预期的实际请求。
	 * 因此空条件也会被视为匹配，否则尝试与"Access-Control-Request-Method"头中的HTTP方法进行匹配。
	 */
	@Nullable
	private RequestMethodsRequestCondition matchPreFlight(HttpServletRequest request) {
		if (getMethods().isEmpty()) {
			return this;
		}
		String expectedMethod = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
		return matchRequestMethod(expectedMethod);
	}

	@Nullable
	private RequestMethodsRequestCondition matchRequestMethod(String httpMethodValue) {
		HttpMethod httpMethod = HttpMethod.resolve(httpMethodValue);
		if (httpMethod != null) {
			for (RequestMethod method : getMethods()) {
				if (httpMethod.matches(method.name())) {
					return new RequestMethodsRequestCondition(method);
				}
			}
			if (httpMethod == HttpMethod.HEAD && getMethods().contains(RequestMethod.GET)) {
				return GET_CONDITION;
			}
		}
		return null;
	}

	/**
	 * 返回比较结果：
	 * <ul>
	 *     <li>如果两个条件包含相同数量的HTTP请求方法，返回0</li>
	 *     <li>如果"this"实例有HTTP请求方法而"other"没有，返回小于0的值</li>
	 *     <li>如果"other"有HTTP请求方法而"this"没有，返回大于0的值</li>
	 * </ul>
	 * <p>
	 *     假定两个实例都是通过{@link #getMatchingCondition(HttpServletRequest)}获取的，因此每个实例要么仅包含匹配的HTTP请求方法，要么为空。
	 */
	@Override
	public int compareTo(RequestMethodsRequestCondition other, HttpServletRequest request) {
		if (other.methods.size() != this.methods.size()) {
			return other.methods.size() - this.methods.size();
		}
		else if (this.methods.size() == 1) {
			if (this.methods.contains(RequestMethod.HEAD) && other.methods.contains(RequestMethod.GET)) {
				return -1;
			}
			else if (this.methods.contains(RequestMethod.GET) && other.methods.contains(RequestMethod.HEAD)) {
				return 1;
			}
		}
		return 0;
	}

}
