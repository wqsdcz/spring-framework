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

package org.springframework.web.context.request;

import org.springframework.lang.Nullable;
import org.springframework.ui.ModelMap;

/**
 * 通用Web请求拦截器接口。
 * 通过构建在{@link WebRequest}抽象之上，可同时应用于Servlet请求和Portlet请求环境。
 *
 * <p>
 *     本接口采用MVC风格的请求处理：
 *     处理器执行后暴露一组模型对象，然后基于该模型渲染视图。
 *     或者，处理器也可完成处理请求的所有工作而无需渲染视图。
 *
 * <p>
 *     在异步处理场景中，处理器可能在单独线程中执行，而主线程退出时不进行渲染也不调用{@code postHandle}和{@code afterCompletion}回调。
 *     当并发处理器执行完成时，请求会被重新分派以继续模型渲染，此时将再次调用本契约的所有方法。
 *     更多选项和说明请参阅 {@code org.springframework.web.context.request.async.AsyncWebRequestInterceptor}
 *
 * <p>本接口刻意保持最小化，以使通用请求拦截器的依赖尽可能最小。
 *
 * <p>
 *     <b>注意：</b>
 *     虽然此拦截器在Servlet环境中应用于整个请求处理过程，
 *     但在Portlet环境中默认仅应用于<i>渲染</i>阶段（准备和渲染Portlet视图）。
 *     如需将WebRequestInterceptors同时应用于<i>操作</i>阶段，
 *     请将HandlerMapping的"applyWebRequestInterceptorsToRenderPhaseOnly"标志设置为"false"。
 *     或者，考虑使用Portlet特定的HandlerInterceptor机制满足此类需求。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ServletWebRequest
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping#setInterceptors
 * @see org.springframework.web.servlet.HandlerInterceptor
 */
public interface WebRequestInterceptor {

	/**
	 * 在请求处理器调用<i>之前</i>拦截其执行。
	 *
	 * <p>
	 *     允许准备上下文资源（如Hibernate Session），并将其暴露为请求属性或线程本地对象。
	 *
	 * @param request 当前Web请求
	 * @throws Exception 发生错误时
	 */
	void preHandle(WebRequest request) throws Exception;

	/**
	 * 在请求处理器成功调用<i>之后</i>、视图渲染之前（如有）进行拦截。
	 *
	 * <p>
	 *     允许在处理器成功执行后修改上下文资源（例如，刷新Hibernate Session）。
	 *
	 * @param request 当前Web请求
	 * @param model 将暴露给视图的模型对象映射（可能为{@code null}）。可用于分析暴露的模型和/或添加更多模型属性（如需要）。
	 * @throws Exception 发生错误时
	 */
	void postHandle(WebRequest request, @Nullable ModelMap model) throws Exception;

	/**
	 * 请求处理完成后的回调（即在视图渲染之后）。
	 * 无论处理器执行结果如何都会调用，因此允许进行适当的资源清理。
	 *
	 * <p>
	 *     注意：仅在此拦截器的{@code preHandle}方法成功完成后才会调用！
	 *
	 * @param request 当前Web请求
	 * @param ex 处理器执行时抛出的异常（如有）
	 * @throws Exception 发生错误时
	 */
	void afterCompletion(WebRequest request, @Nullable Exception ex) throws Exception;

}
