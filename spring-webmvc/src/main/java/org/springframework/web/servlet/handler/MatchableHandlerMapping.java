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

package org.springframework.web.servlet.handler;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerMapping;

/**
 * {@link HandlerMapping} 可实现的附加接口，用于对外提供与其内部请求匹配配置及实现保持一致的请求匹配API。
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 * @see HandlerMappingIntrospector
 */
public interface MatchableHandlerMapping extends HandlerMapping {

	/**
	 * 判断给定请求是否匹配请求条件。
	 *
	 * @param request 当前请求
	 * @param pattern 要匹配的模式
	 * @return 请求匹配的结果，若无匹配则返回 {@code null}
	 */
	@Nullable
	RequestMatchResult match(HttpServletRequest request, String pattern);

}
