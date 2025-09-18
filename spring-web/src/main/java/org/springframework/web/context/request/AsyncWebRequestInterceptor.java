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
package org.springframework.web.context.request;

/**
 * 扩展了 {@code WebRequestInterceptor} 接口，提供了在异步请求处理期间调用的回调方法。
 *
 * <p>
 *     当处理器开始异步请求处理时，
 *     DispatcherServlet 将退出而不调用{@code postHandle} 和 {@code afterCompletion} 方法（这与常规处理不同），
 *     因为请求处理的结果（例如 ModelAndView）在当前线程中不可用且处理尚未完成。
 *     在此类场景中，将调用 {@link #afterConcurrentHandlingStarted(WebRequest)} 方法，允许实现执行诸如清理线程绑定属性等任务。
 *
 * <p>
 *     当异步处理完成时，请求将被分派到容器进行进一步处理。
 *     在此阶段，DispatcherServlet 会像平常一样调用 {@code preHandle}、{@code postHandle} 和 {@code afterCompletion} 方法。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 *
 * @see org.springframework.web.context.request.async.WebAsyncManager
 */
public interface AsyncWebRequestInterceptor extends WebRequestInterceptor{

	/**
	 * 当处理器开始并发处理请求时，此方法将被调用，以替代 {@code postHandle} 和 {@code afterCompletion} 方法。
	 *
	 * @param request 当前请求
	 */
	void afterConcurrentHandlingStarted(WebRequest request);

}
