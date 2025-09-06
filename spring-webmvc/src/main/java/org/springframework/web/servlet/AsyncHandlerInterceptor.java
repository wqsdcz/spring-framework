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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.method.HandlerMethod;

/**
 * <p>扩展 {@code HandlerInterceptor} 接口，新增在异步请求处理开始后调用的回调方法。</p>
 * <p>
 *     当处理器启动异步请求时，{@link DispatcherServlet} 会退出而不像处理同步请求时那样调用 {@code postHandle} 和 {@code afterCompletion} 方法，
 *     因为此时请求处理的结果（如 ModelAndView）很可能尚未准备就绪，而是由另一个线程并发产生。
 *     在此类场景下，将改为调用 {@link #afterConcurrentHandlingStarted} 方法，允许实现类在执行诸如清理线程绑定属性等任务后再将线程释放给 Servlet 容器。
 * </p>
 * <p>
 *     当异步处理完成时，请求会被分派给容器进行后续处理。
 *     此时 {@code DispatcherServlet} 会调用 {@code preHandle}、{@code postHandle} 和 {@code afterCompletion} 方法。
 *     为区分初始请求和异步处理完成后的后续分派，拦截器可检查 {@link javax.servlet.ServletRequest} 的 {@code javax.servlet.DispatcherType}
 *     是 {@code "REQUEST"} 还是 {@code "ASYNC"}。
 * <p>
 * </p>
 *     请注意，当异步请求超时或因网络错误完成时，{@code HandlerInterceptor} 实现可能需要执行某些操作。
 *     在这种情况下，Servlet 容器不会进行分派，因此不会调用 {@code postHandle} 和 {@code afterCompletion} 方法。
 *     此时，拦截器可以通过 {@link org.springframework.web.context.request.async.WebAsyncManager WebAsyncManager} 上的
 *     {@code registerCallbackInterceptor} 和 {@code registerDeferredResultInterceptor} 方法注册以跟踪异步请求。
 *     无论异步请求处理是否启动，都可以在每次请求时通过 {@code preHandle} 方法主动执行此操作。
 * </p>
 * Extends {@code HandlerInterceptor} with a callback method invoked after the
 * start of asynchronous request handling.
 *
 * <p>When a handler starts an asynchronous request, the {@link DispatcherServlet}
 * exits without invoking {@code postHandle} and {@code afterCompletion} as it
 * normally does for a synchronous request, since the result of request handling
 * (e.g. ModelAndView) is likely not yet ready and will be produced concurrently
 * from another thread. In such scenarios, {@link #afterConcurrentHandlingStarted}
 * is invoked instead, allowing implementations to perform tasks such as cleaning
 * up thread-bound attributes before releasing the thread to the Servlet container.
 *
 * <p>When asynchronous handling completes, the request is dispatched to the
 * container for further processing. At this stage the {@code DispatcherServlet}
 * invokes {@code preHandle}, {@code postHandle}, and {@code afterCompletion}.
 * To distinguish between the initial request and the subsequent dispatch
 * after asynchronous handling completes, interceptors can check whether the
 * {@code javax.servlet.DispatcherType} of {@link javax.servlet.ServletRequest}
 * is {@code "REQUEST"} or {@code "ASYNC"}.
 *
 * <p>Note that {@code HandlerInterceptor} implementations may need to do work
 * when an async request times out or completes with a network error. For such
 * cases the Servlet container does not dispatch and therefore the
 * {@code postHandle} and {@code afterCompletion} methods will not be invoked.
 * Instead, interceptors can register to track an asynchronous request through
 * the {@code registerCallbackInterceptor} and {@code registerDeferredResultInterceptor}
 * methods on {@link org.springframework.web.context.request.async.WebAsyncManager
 * WebAsyncManager}. This can be done proactively on every request from
 * {@code preHandle} regardless of whether async request processing will start.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 * @see org.springframework.web.context.request.async.WebAsyncManager
 * @see org.springframework.web.context.request.async.CallableProcessingInterceptor
 * @see org.springframework.web.context.request.async.DeferredResultProcessingInterceptor
 */
public interface AsyncHandlerInterceptor extends HandlerInterceptor {

	/**
	 * <p>当处理器正在并发执行时，此方法将替代 {@code postHandle} 和 {@code afterCompletion} 方法被调用。</p>
	 * <p>实现类可以使用提供的请求和响应对象，但应避免以与处理器并发执行相冲突的方式修改这些对象。此方法的典型用途是清理线程局部变量。</p>
	 * @param request 当前请求对象
	 * @param response 当前响应对象
	 * @param handler 启动异步执行的处理器（或 {@link HandlerMethod}），用于类型和/或实例检查
	 * @throws Exception 当处理过程中发生错误时
	 */
	default void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response,
			Object handler) throws Exception {
	}

}
