/*
 * Copyright 2002-2016 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;

/**
 * 请求映射条件的契约接口。
 *
 * <p>
 *     请求条件可以通过 {@link #combine(Object)} 进行组合，
 *     通过 {@link #getMatchingCondition(HttpServletRequest)} 与请求进行匹配，
 *     并通过 {@link #compareTo(Object, HttpServletRequest)} 相互比较，
 *     以确定哪个条件与给定请求的匹配度更高。
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 3.1
 * @param <T> 可与此RequestCondition组合和比较的对象类型
 */
public interface RequestCondition<T> {

	/**
	 * 将此条件与另一个条件（例如：来自类型级别和方法级别 {@code @RequestMapping} 注解的条件）进行组合。
	 * @param other 要与之组合的条件
	 * @return 组合两个条件实例后得到的请求条件实例
	 */
	T combine(T other);

	/**
	 * 检查该条件是否与请求匹配，并返回为当前请求创建的新实例（可能包含更新后的状态）。
	 * 例如，具有多个URL模式的条件可能仅返回那些与请求匹配的模式组成的新实例。
	 * <p>
	 *     对于CORS预检请求，条件应匹配预期的实际请求
	 *     （例如URL模式、查询参数、以及来自"Access-Control-Request-Method"头的HTTP方法）。
	 *     如果条件无法与预检请求匹配，则应返回一个内容为空的条件实例，以避免导致匹配失败。
	 * @return 匹配时返回条件实例，否则返回{@code null}
	 */
	@Nullable
	T getMatchingCondition(HttpServletRequest request);

	/**
	 * 在特定请求的上下文中，将此条件与另一个条件进行比较。
	 * 此方法假定两个实例都是通过{@link #getMatchingCondition(HttpServletRequest)}获取的，以确保它们仅包含与当前请求相关的内容。
	 */
	int compareTo(T other, HttpServletRequest request);

}
