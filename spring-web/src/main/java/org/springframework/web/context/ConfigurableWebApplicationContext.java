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
 *     注意：此接口的setter方法必须在调用{@link #refresh}方法
 *     （从{@link org.springframework.context.ConfigurableApplicationContext}继承的）之前调用。
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
	 * Set the namespace for this web application context,
	 * to be used for building a default context config location.
	 * The root web application context does not have a namespace.
	 */
	void setNamespace(@Nullable String namespace);

	/**
	 * Return the namespace for this web application context, if any.
	 */
	@Nullable
	String getNamespace();

	/**
	 * Set the config locations for this web application context in init-param style,
	 * i.e. with distinct locations separated by commas, semicolons or whitespace.
	 * <p>If not set, the implementation is supposed to use a default for the
	 * given namespace or the root web application context, as appropriate.
	 */
	void setConfigLocation(String configLocation);

	/**
	 * Set the config locations for this web application context.
	 * <p>If not set, the implementation is supposed to use a default for the
	 * given namespace or the root web application context, as appropriate.
	 */
	void setConfigLocations(String... configLocations);

	/**
	 * Return the config locations for this web application context,
	 * or {@code null} if none specified.
	 */
	@Nullable
	String[] getConfigLocations();

}
