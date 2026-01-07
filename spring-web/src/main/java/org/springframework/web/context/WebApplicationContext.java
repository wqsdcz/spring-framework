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

package org.springframework.web.context;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

/**
 * 为Web应用程序提供配置的接口。在应用程序运行时这是只读的，但如果实现支持，可以重新加载。
 *
 * <p>
 *     此接口在通用的 ApplicationContext 接口中添加了一个名为 {@code getServletContext()} 的方法，
 *     并定义了一个众所周知的应用程序属性名称，该名称在启动过程中必须被根上下文绑定。
 *
 * <p>
 *     与通用应用程序上下文一样，Web应用程序上下文也是层次结构的。
 *     每个应用程序有一个根上下文，而应用程序中的每个Servlet（包括MVC框架中的分发器servlet）都有自己的子上下文。
 *
 * <p>
 *     除了标准的应用程序上下文生命周期功能外，WebApplicationContext实现需要检测{@link ServletContextAware} Bean并相应地调用{@code setServletContext}方法。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since January 19, 2001
 * @see ServletContextAware#setServletContext
 */
public interface WebApplicationContext extends ApplicationContext {

	/**
	 * 用于在成功启动时将根 WebApplicationContext 绑定到的上下文属性。
	 *
	 * <p>
	 *     注意：如果根上下文启动失败，此属性可能包含异常或错误作为值。
	 *     可使用 WebApplicationContextUtils 工具类方便地查找根 WebApplicationContext。
	 *
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#getWebApplicationContext
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#getRequiredWebApplicationContext
	 */
	String ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE = WebApplicationContext.class.getName() + ".ROOT";

	/**
	 * 请求作用域的标识符："request"。
	 * 除标准作用域"singleton"和"prototype"外额外支持的作用域。
	 */
	String SCOPE_REQUEST = "request";

	/**
	 * 会话作用域的标识符："session"。
	 * 除标准作用域"singleton"和"prototype"外额外支持的作用域。
	 */
	String SCOPE_SESSION = "session";

	/**
	 * 全局Web应用程序作用域的标识符："application"。
	 * 除标准作用域"singleton"和"prototype"外额外支持的作用域。
	 */
	String SCOPE_APPLICATION = "application";

	/**
	 * 工厂中ServletContext环境bean的名称。
	 * @see javax.servlet.ServletContext
	 */
	String SERVLET_CONTEXT_BEAN_NAME = "servletContext";

	/**
	 * 工厂中ServletContext/PortletContext初始化参数环境bean的名称。
	 * <p>
	 *     注意：可能会与ServletConfig/PortletConfig参数合并。
	 *     ServletConfig参数会覆盖同名的ServletContext参数。
	 *
	 * @see javax.servlet.ServletContext#getInitParameterNames()
	 * @see javax.servlet.ServletContext#getInitParameter(String)
	 * @see javax.servlet.ServletConfig#getInitParameterNames()
	 * @see javax.servlet.ServletConfig#getInitParameter(String)
	 */
	String CONTEXT_PARAMETERS_BEAN_NAME = "contextParameters";

	/**
	 * 工厂中ServletContext/PortletContext属性环境bean的名称。
	 * @see javax.servlet.ServletContext#getAttributeNames()
	 * @see javax.servlet.ServletContext#getAttribute(String)
	 */
	String CONTEXT_ATTRIBUTES_BEAN_NAME = "contextAttributes";


	/**
	 * 返回此应用程序的标准 Servlet API ServletContext。
	 */
	@Nullable
	ServletContext getServletContext();

}
