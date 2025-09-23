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

package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;

/**
 * 控制器基础接口，表示一个类似于{@code HttpServlet}的组件，
 * 能够接收{@code HttpServletRequest}和{@code HttpServletResponse}实例，同时可参与MVC工作流程。
 * 控制器的概念类似于Struts中的{@code Action}。
 *
 * <p>
 *     Controller接口的任何实现都应该是<i>可重用、线程安全</i>的类，能够处理应用程序生命周期中的多个HTTP请求。
 *     为了便于配置控制器，鼓励控制器实现成为（通常也是）JavaBean。
 *
 * <h3><a name="workflow">工作流程</a></h3>
 *
 * <p>
 *     {@code DispatcherServlet}接收请求并完成区域设置、主题等解析工作后，
 *     会使用{@link org.springframework.web.servlet.HandlerMapping HandlerMapping}尝试解析控制器。
 *     当找到处理请求的控制器后，将调用定位到的控制器的{@link #handleRequest(HttpServletRequest, HttpServletResponse) handleRequest}方法；
 *     该控制器负责处理实际请求并（如果适用）返回相应的{@link org.springframework.web.servlet.ModelAndView ModelAndView}。
 *     实际上，这个方法是{@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}将请求委托给控制器的主要入口点。
 *
 * <p>
 *     因此，任何对{@code Controller}接口的<i>直接</i>实现都只是处理HttpServletRequest，并应返回一个ModelAndView，由DispatcherServlet进一步解释。
 *     任何额外功能（如可选验证、表单处理等）应通过扩展{@link org.springframework.web.servlet.mvc.AbstractController AbstractController}或其子类来获得。
 *
 * <h3>设计与测试说明</h3>
 *
 * <p>
 *     Controller接口明确设计为像HttpServlet一样操作HttpServletRequest和HttpServletResponse对象。
 *     与WebWork、JSF或Tapestry等框架不同，它并不旨在与Servlet API解耦。
 *     相反，它可以充分利用Servlet API的全部功能，使控制器具有通用性：控制器不仅能够处理Web用户界面请求，还能处理远程协议或按需生成报告。
 *
 * <p>
 *     通过将HttpServletRequest和HttpServletResponse的模拟对象作为参数传递给
 *     {@link #handleRequest(HttpServletRequest, HttpServletResponse) handleRequest}方法，可以轻松测试控制器。
 *     为方便起见，Spring提供了一组适用于测试任何类型Web组件（尤其适合测试Spring Web控制器）的Servlet API模拟对象。
 *     与Struts Action不同，不需要模拟ActionServlet或任何其他基础架构；模拟HttpServletRequest和HttpServletResponse就足够了。
 *
 * <p>
 *     如果控制器需要感知特定的环境引用，它们可以选择实现特定的感知接口，就像Spring（Web）应用程序上下文中的任何其他bean一样，例如：
 *     <ul>
 *          <li>{@code org.springframework.context.ApplicationContextAware}</li>
 *          <li>{@code org.springframework.context.ResourceLoaderAware}</li>
 *          <li>{@code org.springframework.web.context.ServletContextAware}</li>
 *     </ul>
 *
 * <p>
 *     通过相应感知接口中定义的setter方法，可以轻松在测试环境中传入这些环境引用。
 *     通常建议保持依赖尽可能最小化：例如，如果只需要资源加载，仅实现ResourceLoaderAware即可。
 *     或者，从WebApplicationObjectSupport基类派生，它通过便捷的访问器提供所有引用，但需要在初始化时提供ApplicationContext引用。
 *
 * <p>控制器可以选择实现{@link LastModified}接口。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see LastModified
 * @see SimpleControllerHandlerAdapter
 * @see AbstractController
 * @see org.springframework.mock.web.MockHttpServletRequest
 * @see org.springframework.mock.web.MockHttpServletResponse
 * @see org.springframework.context.ApplicationContextAware
 * @see org.springframework.context.ResourceLoaderAware
 * @see org.springframework.web.context.ServletContextAware
 * @see org.springframework.web.context.support.WebApplicationObjectSupport
 */
@FunctionalInterface
public interface Controller {

	/**
	 * 处理请求并返回一个将由 DispatcherServlet 渲染的 ModelAndView 对象。
	 * 返回 {@code null} 值不属于错误情况：它表示此对象已完成请求处理本身，因此没有需要渲染的 ModelAndView。
	 *
	 * @param request 当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @return 要渲染的 ModelAndView，若已直接处理则返回 {@code null}
	 * @throws Exception 处理过程中发生错误时
	 */
	@Nullable
	ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception;

}
