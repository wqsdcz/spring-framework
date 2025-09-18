/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.lang.Nullable;
import org.springframework.web.WebApplicationInitializer;

/**
 * 用于实现{@link WebApplicationInitializer}的便利基类，
 * 在Servlet上下文中注册{@link ContextLoaderListener}。
 *
 * <p>
 *     子类需要实现的唯一方法是{@link #createRootApplicationContext()}，
 *     该方法将从{@link #registerContextLoaderListener(ServletContext)}中调用。
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.2
 */
public abstract class AbstractContextLoaderInitializer implements WebApplicationInitializer {

	/** 可用于子类的日志记录器 */
	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		registerContextLoaderListener(servletContext);
	}

	/**
	 * 向给定的ServletContext中注册一个{@link ContextLoaderListener}。
	 * 该{@code ContextLoaderListener}使用从模板方法{@link #createRootApplicationContext()}返回的ApplicationContext进行初始化。
	 *
	 * @param servletContext 要注册监听器的servlet上下文
	 */
	protected void registerContextLoaderListener(ServletContext servletContext) {
		WebApplicationContext rootAppContext = createRootApplicationContext();
		if (rootAppContext != null) {
			ContextLoaderListener listener = new ContextLoaderListener(rootAppContext);
			listener.setContextInitializers(getRootApplicationContextInitializers());
			servletContext.addListener(listener);
		}
		else {
			logger.debug("因为createRootApplicationContext()方法没有返回ApplicationContext对象，所以本方法未注册 ContextLoaderListener。");
		}
	}

	/**
	 * 创建要提供给{@code ContextLoaderListener}的 "<strong>root</strong>" ApplicationContext。
	 * <p>
	 *     返回的ApplicationContext对象将通过{@link ContextLoaderListener#ContextLoaderListener(WebApplicationContext)}方法传入，
	 *     并将其作为所有{@code DispatcherServlet}内的ApplicationContext对象的"<strong>父</strong>"ApplicationContext。
	 *     因此，它通常包含中间层服务、数据源等组件。
	 *
	 * @return 根应用上下文，如果不需要根上下文，则可返回{@code null}
	 * @see org.springframework.web.servlet.support.AbstractDispatcherServletInitializer
	 */
	@Nullable
	protected abstract WebApplicationContext createRootApplicationContext();

	/**
	 * 获取应用于 <strong>root</strong>" ApplicationContext的ApplicationContextInitializer对象集合。
	 * <strong>root</strong>" ApplicationContext是创建{@code ContextLoaderListener}时指定的那个上下文。
	 *
	 * @since 4.2
	 * @see #createRootApplicationContext()
	 * @see ContextLoaderListener#setContextInitializers
	 */
	@Nullable
	protected ApplicationContextInitializer<?>[] getRootApplicationContextInitializers() {
		return null;
	}

}
