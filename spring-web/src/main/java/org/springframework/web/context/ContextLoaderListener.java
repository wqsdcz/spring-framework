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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * 用于启动和关闭Spring根{@link WebApplicationContext}的引导监听器。
 * 直接委托给{@link ContextLoader}和{@link ContextCleanupListener}处理。
 *
 * <p>自Spring 3.1起，{@code ContextLoaderListener}支持通过
 * {@link #ContextLoaderListener(WebApplicationContext)}构造函数注入根Web应用上下文，允许在Servlet 3.0+环境中进行编程式配置。
 * 使用示例请参阅{@link org.springframework.web.WebApplicationInitializer}。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2003年2月17日
 * @see #setContextInitializers
 * @see org.springframework.web.WebApplicationInitializer
 */
public class ContextLoaderListener extends ContextLoader implements ServletContextListener {

	/**
	 * 创建一个新的{@code ContextLoaderListener}实例，该监听器将基于servlet上下文参数
	 * "contextClass"和"contextConfigLocation"创建Web应用上下文。
	 * 关于各参数的默认值，请参阅{@link ContextLoader}超类文档。
	 *
	 * <p>此构造函数通常在将{@code ContextLoaderListener}声明为{@code web.xml}中的
	 * {@code <listener>}时使用，因为需要无参构造函数。
	 *
	 * <p>创建的应用上下文将以{@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}
	 * 为属性名注册到ServletContext中，并且当在此监听器上调用{@link #contextDestroyed}
	 * 生命周期方法时，Spring应用上下文将被关闭。
	 *
	 * @see ContextLoader
	 * @see #ContextLoaderListener(WebApplicationContext)
	 * @see #contextInitialized(ServletContextEvent)
	 * @see #contextDestroyed(ServletContextEvent)
	 */
	public ContextLoaderListener() {
	}

	/**
	 * 使用指定的应用上下文创建新的 {@code ContextLoaderListener} 实例。
	 * 此构造函数适用于 Servlet 3.0+ 环境，可通过 {@link javax.servlet.ServletContext#addListener}
	 * API 实现基于实例的监听器注册。
	 *
	 * <p>传入的上下文可能尚未被 {@linkplain org.springframework.context.ConfigurableApplicationContext#refresh() 刷新}。
	 * 如果该上下文同时满足：(a) 是 {@link ConfigurableWebApplicationContext} 的实现，且 (b) <strong>尚未</strong>
	 * 被刷新（推荐方式），则将执行以下操作：
	 * <ul>
	 * <li>如果给定上下文尚未分配 {@linkplain org.springframework.context.ConfigurableApplicationContext#setId ID}，
	 *     将为其分配一个</li>
	 * <li>将 {@code ServletContext} 和 {@code ServletConfig} 对象委托给应用上下文</li>
	 * <li>调用 {@link #customizeContext} 方法</li>
	 * <li>应用通过 "contextInitializerClasses" 初始化参数指定的任何
	 *     {@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer}</li>
	 * <li>调用 {@link org.springframework.context.ConfigurableApplicationContext#refresh refresh()} 方法</li>
	 * </ul>
	 * 如果上下文已被刷新或不实现 {@code ConfigurableWebApplicationContext} 接口，
	 * 则不会执行上述任何操作，假定用户已根据特定需求执行（或未执行）这些操作。
	 *
	 * <p>使用示例请参阅 {@link org.springframework.web.WebApplicationInitializer}。
	 *
	 * <p>无论哪种情况，给定的应用上下文都将以 {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}
	 * 为属性名注册到 ServletContext 中，并且当在此监听器上调用 {@link #contextDestroyed} 生命周期方法时，
	 * Spring 应用上下文将被关闭。
	 *
	 * @param context 要管理的应用上下文
	 * @see #contextInitialized(ServletContextEvent)
	 * @see #contextDestroyed(ServletContextEvent)
	 */
	public ContextLoaderListener(WebApplicationContext context) {
		super(context);
	}


	/**
	 * 初始化根Web应用上下文。
	 */
	@Override
	public void contextInitialized(ServletContextEvent event) {
		initWebApplicationContext(event.getServletContext());
	}


	/**
	 * 关闭根Web应用上下文。
	 */
	@Override
	public void contextDestroyed(ServletContextEvent event) {
		closeWebApplicationContext(event.getServletContext());
		ContextCleanupListener.cleanupAttributes(event.getServletContext());
	}

}
