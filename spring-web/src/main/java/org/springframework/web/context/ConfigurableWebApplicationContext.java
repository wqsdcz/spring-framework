/*
 * Copyright 2002-2014 the original author or authors.
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;

/**
 * 可由可配置的Web应用上下文实现的接口。
 * 由{@link ContextLoader}和{@link org.springframework.web.servlet.FrameworkServlet}支持。
 *
 * <p>
 *     注意：此接口的setter方法必须在调用{@link #refresh}方法（从{@link org.springframework.context.ConfigurableApplicationContext}继承的）之前调用。
 *     它们不会自行引起上下文的初始化。
 *
 * @author Juergen Hoeller
 * @since 2003年12月5日
 * @see #refresh
 * @see ContextLoader#createWebApplicationContext
 * @see org.springframework.web.servlet.FrameworkServlet#createWebApplicationContext
 */
public interface ConfigurableWebApplicationContext extends WebApplicationContext, ConfigurableApplicationContext {

	/** 用于表示 ApplicationContext ID 的前缀，这些ID指的是上下文路径和/或 servlet 名称。 */
	String APPLICATION_CONTEXT_ID_PREFIX = WebApplicationContext.class.getName() + ":";


	/**
	 * 工厂中ServletConfig环境bean的名称。
	 * @see javax.servlet.ServletConfig
	 */
	String SERVLET_CONFIG_BEAN_NAME = "servletConfig";


	/**
	 * 设置此 WebApplicationContext 的 ServletContext。
	 * <p>不会导致上下文的初始化：需要在设置所有配置属性后调用refresh方法。
	 * @see #refresh()
	 */
	void setServletContext(@Nullable ServletContext servletContext);

	/**
	 * 设置此 WebApplicationContext 的 ServletConfig。
	 * 仅适用于属于特定Servlet的WebApplicationContext。
	 * @see #refresh()
	 */
	void setServletConfig(@Nullable ServletConfig servletConfig);

	/**
	 * 返回此 WebApplicationContext 的ServletConfig（如果存在）。
	 */
	@Nullable
	ServletConfig getServletConfig();

	/**
	 * 设置此Web应用上下文的命名空间，用于构建默认的上下文配置位置。
	 * 根Web应用上下文没有命名空间。
	 */
	void setNamespace(@Nullable String namespace);

	/**
	 * 返回此Web应用上下文的命名空间（如果有）。
	 */
	@Nullable
	String getNamespace();

	/**
	 * 以初始化参数样式设置此Web应用上下文的配置位置，即用逗号、分号或空格分隔的不同位置。
	 * <p>如果未设置，实现应使用给定命名空间或根Web应用上下文的默认值（视情况而定）。
	 */
	void setConfigLocation(String configLocation);

	/**
	 * 设置此Web应用上下文的配置位置。
	 * <p>如果未设置，实现应使用给定命名空间或根Web应用上下文的默认值（视情况而定）。
	 */
	void setConfigLocations(String... configLocations);

	/**
	 * 返回此Web应用上下文的配置位置，如果未指定则返回{@code null}。
	 */
	@Nullable
	String[] getConfigLocations();

}
