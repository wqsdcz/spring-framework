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

import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;

/**
 * 用于对处理器执行链进行自定义的工作流接口。
 * 应用程序可以为某些处理器组注册任意数量的现有或自定义拦截器，以添加通用的预处理行为，而无需修改每个处理器的实现。
 *
 * <p>处理器拦截器会在相应的HandlerAdapter触发处理器本身执行之前被调用。
 * 这种机制可用于大量预处理场景，例如授权检查，或常见的处理器行为如区域设置或主题更改。
 * 其主要目的是允许提取重复的处理器代码。
 *
 * <p>在异步处理场景中，处理器可能在单独的线程中执行，而主线程退出时不渲染或不调用
 * {@code postHandle}和{@code afterCompletion}回调。当并发处理器执行完成时，
 * 请求会被重新派发以便继续渲染模型，并再次调用此契约的所有方法。
 * 更多选项和详细信息请参见
 * {@code org.springframework.web.servlet.AsyncHandlerInterceptor}
 *
 * <p>通常每个HandlerMapping bean都会定义一个拦截器链，共享其粒度。
 * 要将特定拦截器链应用于一组处理器，需要通过一个HandlerMapping bean来映射所需的处理器。
 * 拦截器本身在应用上下文中定义为bean，映射bean定义通过其"interceptors"属性
 * （在XML中：&lt;list&gt; of &lt;ref&gt;）引用这些拦截器。
 *
 * <p>HandlerInterceptor基本上类似于Servlet过滤器，但与后者不同的是，
 * 它只允许自定义预处理（可选择禁止处理器本身的执行）和自定义后处理。
 * 过滤器更强大，例如它们允许交换在链中传递的请求和响应对象。
 * 请注意，过滤器在web.xml中配置，而HandlerInterceptor在应用上下文中配置。
 *
 * <p>作为基本准则，细粒度的与处理器相关的预处理任务适合使用HandlerInterceptor实现，
 * 特别是提取出的通用处理器代码和授权检查。另一方面，过滤器非常适合处理请求内容和视图内容，
 * 如多部分表单和GZIP压缩。当需要将过滤器映射到特定内容类型（例如图像）或所有请求时，
 * 这一点通常很明显。
 *
 * @author Juergen Hoeller
 * @since 20.06.2003
 * @see HandlerExecutionChain#getInterceptors
 * @see org.springframework.web.servlet.handler.HandlerInterceptorAdapter
 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping#setInterceptors
 * @see org.springframework.web.servlet.handler.UserRoleAuthorizationInterceptor
 * @see org.springframework.web.servlet.i18n.LocaleChangeInterceptor
 * @see org.springframework.web.servlet.theme.ThemeChangeInterceptor
 * @see javax.servlet.Filter
 */
public interface HandlerInterceptor {

	/**
	 * <p>对处理器的执行过程进行拦截。在HandlerMapping确定了适配的处理器对象之后，但在HandlerAdapter调用处理器之前，被调用。</p>
	 * <p>
	 *     DispatcherServlet以执行链的形式处理处理器，该链由任意数量的拦截器组成，处理器本身位于链的末端。
	 *     通过此方法，每个拦截器都可以决定是否终止执行链——通常通过发送HTTP错误或写入自定义响应来实现。
	 * </p>
	 * <p>
	 *     <strong>注意：</strong>
	 *     异步请求处理场景下有特殊考量，更多细节请参阅 {@link org.springframework.web.servlet.AsyncHandlerInterceptor}。
	 * </p>
	 * <p>默认实现返回{@code true}。</p>
	 * @param request 当前HTTP请求对象
	 * @param response 当前HTTP响应对象
	 * @param handler 被选定的处理器对象，用于类型和/或实例评估
	 * @return 若应继续执行链中的下一个拦截器或处理器本身，则返回{@code true}；
	 * 否则DispatcherServlet将认定本拦截器已自行处理完响应
	 * @throws Exception 当发生错误时
	 */
	default boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return true;
	}

	/**
	 * <p>
	 *     对处理器的执行过程进行拦截。此方法在 HandlerAdapter 实际调用处理器之后，但在 DispatcherServlet 渲染视图之前被调用。
	 * 	   可通过给定的 ModelAndView 向视图暴露额外的模型对象。
	 * </p>
	 * <p>
	 *     DispatcherServlet 以执行链的形式处理处理器，该链由任意数量的拦截器组成，处理器本身位于链的末端。
	 *     通过此方法，每个拦截器都可以对执行过程进行后处理，这些后处理操作会按照执行链的逆序依次应用。
	 * </p>
	 * <p>
	 *     <strong>注意：</strong>
	 *     异步请求处理场景下有特殊考量，更多细节请参阅 {@link org.springframework.web.servlet.AsyncHandlerInterceptor}。
	 * </p>
	 * <p>默认实现为空。</p>
	 * @param request 当前HTTP请求对象
	 * @param response 当前HTTP响应对象
	 * @param handler 启动异步执行的处理器（或{@link HandlerMethod}），用于类型和/或实例检查
	 * @param modelAndView 处理器返回的{@code ModelAndView}对象（也可能为{@code null}）
	 * @throws Exception 当发生错误时
	 */
	default void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable ModelAndView modelAndView) throws Exception {
	}

	/**
	 * <p>
	 *     在请求处理完成后的回调方法，即在视图渲染之后调用。
	 *     无论处理器执行的结果如何（成功或异常），此方法都会被调用，从而允许进行适当的资源清理。
	 * </p>
	 * <p>注意：仅当该拦截器的{@code preHandle}方法成功完成并返回{@code true}时，此方法才会被调用！</p>
	 * <p>与{@code postHandle}方法类似，此方法会按拦截器链的逆序依次调用每个拦截器，因此第一个拦截器将会最后一个被调用。</p>
	 * <p>
	 *     <strong>注意：</strong>
	 *     异步请求处理场景下有特殊考量，更多细节请参阅 {@link org.springframework.web.servlet.AsyncHandlerInterceptor}。
	 * </p>
	 * <p>默认实现为空。</p>
	 * @param request 当前HTTP请求对象
	 * @param response 当前HTTP响应对象
	 * @param handler 启动异步执行的处理器（或{@link HandlerMethod}），用于类型和/或实例检查
	 * @param ex 处理器执行时抛出的异常（可能为null）
	 * @throws Exception 当处理过程中发生错误时
	 */
	default void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable Exception ex) throws Exception {
	}

}
