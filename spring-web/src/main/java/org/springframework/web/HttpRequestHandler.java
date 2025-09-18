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

package org.springframework.web;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用于处理HTTP请求的组件的简单处理器接口，类似于Servlet。
 * 仅声明了{@link javax.servlet.ServletException}和{@link java.io.IOException}，以便在任何{@link javax.servlet.http.HttpServlet}中使用。
 * 该接口本质上相当于一个精简到仅保留核心处理方法的HttpServlet。
 *
 * <p>
 *     以Spring风格暴露HttpRequestHandler bean的最简单方式是在Spring的根Web应用上下文中定义它，
 *     并在{@code web.xml}中配置{@link org.springframework.web.context.support.HttpRequestHandlerServlet}，
 *     通过其{@code servlet-name}指向目标HttpRequestHandler bean（需与目标bean名称匹配）。
 *
 * <p>
 *     作为处理器类型受到Spring的{@link org.springframework.web.servlet.DispatcherServlet}支持，能够与调度器的高级映射和拦截设施进行交互。
 *     这是暴露HttpRequestHandler的推荐方式，同时保持处理器实现不直接依赖于DispatcherServlet环境。
 *
 * <p>
 *     通常直接生成二进制响应而无需涉及独立的视图资源，这使其与Spring Web MVC框架中的{@link org.springframework.web.servlet.mvc.Controller}区分开来。
 *     缺少{@link org.springframework.web.servlet.ModelAndView}返回值为DispatcherServlet之外的调用方提供了更清晰的方法签名，表明永远不会渲染视图。
 *
 * <p>
 *     从Spring 2.0开始，基于HTTP的远程导出器（如{@link org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter}
 *     和{@link org.springframework.remoting.caucho.HessianServiceExporter}）为实现最小化对Spring特定Web基础设施的依赖，已实现此接口而非更复杂的Controller接口。
 *
 * <p>
 *     请注意HttpRequestHandler可选择实现{@link org.springframework.web.servlet.mvc.LastModified}接口（就像Controller一样），
 *     <i>前提是在Spring的DispatcherServlet中运行</i>。
 *     但这通常非必需，因为HttpRequestHandler通常最初仅支持POST请求。
 *     或者，处理器可在其{@code handle}方法中手动实现"If-Modified-Since" HTTP标头处理。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.web.context.support.HttpRequestHandlerServlet
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.web.servlet.ModelAndView
 * @see org.springframework.web.servlet.mvc.Controller
 * @see org.springframework.web.servlet.mvc.LastModified
 * @see org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter
 * @see org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter
 * @see org.springframework.remoting.caucho.HessianServiceExporter
 */
@FunctionalInterface
public interface HttpRequestHandler {

	/**
	 * 处理给定请求并生成响应。
	 * @param request 当前HTTP请求
	 * @param response 当前HTTP响应
	 * @throws ServletException 发生常规错误时抛出
	 * @throws IOException 发生I/O错误时抛出
	 */
	void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException;

}
