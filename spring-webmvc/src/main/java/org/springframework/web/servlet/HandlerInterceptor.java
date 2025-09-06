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
 * <p>
 *     允许定制化处理器执行链的工作流（Workflow）接口。
 *     应用程序可以为特定处理器组注册任意数量的现有或自定义拦截器，从而添加通用的预处理行为，而无需修改每个处理器的实现。
 * </p>
 * <p>
 *     在适配的 HandlerAdapter 触发处理器执行之前， HandlerInterceptor 会被调用。
 *     这种机制可用于各种预处理场景，例如：权限检查 或 诸如区域设置/主题变更等通用处理器行为。
 *     其主要目的是将重复的处理器代码的分离出来，实现复用。
 * </p>
 * <p>
 *     在异步处理场景中，处理器可能会在独立线程中执行，而主线程则在不进行渲染且不调用{@code postHandle}和{@code afterCompletion}回调的情况下退出。
 *     当并发处理器执行完成时，请求会被重新派发以便继续模型渲染，此时将再次调用本契约的所有方法。
 *     更多选项和细节请参阅{@code org.springframework.web.servlet.AsyncHandlerInterceptor}。
 * </p>
 * <p>
 *     通常每个HandlerMapping Bean都会定义独立的拦截器链，并共享其粒度配置。
 *     若要将特定拦截器链应用于一组处理器，需要通过同一个HandlerMapping Bean来映射目标处理器。
 *     拦截器本身在应用上下文中定义为Bean，并通过映射Bean定义的"interceptors"属性（XML配置中为&lt;list&gt;包含&lt;ref&gt;）进行引用。
 * </p>
 * <p>
 *     HandlerInterceptor基本类似于Servlet过滤器，但不同的是它仅支持：
 *     1）可禁止处理器本身执行的自定义预处理
 *     2）自定义后处理。过滤器功能更强大，例如允许替换在整个调用链中传递的请求和响应对象。请注意过滤器配置于web.xml中，而HandlerInterceptor配置于应用上下文中。
 * </p>
 * <p>
 *     根据基本指导原则，细粒度的处理器相关预处理任务（特别是抽离复用的通用处理器代码和权限检查）适合采用HandlerInterceptor实现。
 *     而过滤器更适用于请求内容与视图内容处理（如多部分表单和GZIP压缩），这在需要将过滤器映射到特定内容类型（如图片）或所有请求时尤为明显。
 * </p>
 * Workflow interface that allows for customized handler execution chains.
 * Applications can register any number of existing or custom interceptors
 * for certain groups of handlers, to add common preprocessing behavior
 * without needing to modify each handler implementation.
 *
 * <p>A HandlerInterceptor gets called before the appropriate HandlerAdapter
 * triggers the execution of the handler itself. This mechanism can be used
 * for a large field of preprocessing aspects, e.g. for authorization checks,
 * or common handler behavior like locale or theme changes. Its main purpose
 * is to allow for factoring out repetitive handler code.
 *
 * <p>In an asynchronous processing scenario, the handler may be executed in a
 * separate thread while the main thread exits without rendering or invoking the
 * {@code postHandle} and {@code afterCompletion} callbacks. When concurrent
 * handler execution completes, the request is dispatched back in order to
 * proceed with rendering the model and all methods of this contract are invoked
 * again. For further options and details see
 * {@code org.springframework.web.servlet.AsyncHandlerInterceptor}
 *
 * <p>Typically an interceptor chain is defined per HandlerMapping bean,
 * sharing its granularity. To be able to apply a certain interceptor chain
 * to a group of handlers, one needs to map the desired handlers via one
 * HandlerMapping bean. The interceptors themselves are defined as beans
 * in the application context, referenced by the mapping bean definition
 * via its "interceptors" property (in XML: a &lt;list&gt; of &lt;ref&gt;).
 *
 * <p>HandlerInterceptor is basically similar to a Servlet Filter, but in
 * contrast to the latter it just allows custom pre-processing with the option
 * of prohibiting the execution of the handler itself, and custom post-processing.
 * Filters are more powerful, for example they allow for exchanging the request
 * and response objects that are handed down the chain. Note that a filter
 * gets configured in web.xml, a HandlerInterceptor in the application context.
 *
 * <p>As a basic guideline, fine-grained handler-related preprocessing tasks are
 * candidates for HandlerInterceptor implementations, especially factored-out
 * common handler code and authorization checks. On the other hand, a Filter
 * is well-suited for request content and view content handling, like multipart
 * forms and GZIP compression. This typically shows when one needs to map the
 * filter to certain content types (e.g. images), or to all requests.
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
