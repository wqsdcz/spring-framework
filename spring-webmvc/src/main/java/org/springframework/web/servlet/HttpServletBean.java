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

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * <p>
 *     这是对{@link javax.servlet.http.HttpServlet}的简单扩展，
 *     它将其配置参数（在{@code web.xml}的{@code servlet}标签内的{@code init-param}条目）视为Bean属性。
 * </p>
 * <p>
 *     这是一个适用于任何类型Servlet的便捷超类。
 *     配置参数的类型转换是自动进行的，相应的setter方法会被调用并传入转换后的值。
 *     子类还可以指定必需的属性。没有匹配Bean属性setter方法的参数将被忽略。
 * </p>
 * <p>该Servlet将请求处理留给子类，继承了HttpServlet的默认行为（{@code doGet}, {@code doPost}等）。</p>
 * <p>
 *     这个通用的Servlet基类不依赖于Spring的{@link org.springframework.context.ApplicationContext}概念。
 *     简单的Servlet通常不加载自己的上下文，而是从Spring根应用上下文访问服务Bean，
 *     这可以通过过滤器的{@link #getServletContext() ServletContext}来访问
 *     （参见{@link org.springframework.web.context.support.WebApplicationContextUtils}）。
 * </p>
 * <p>
 *     {@link FrameworkServlet}类是一个更具体的Servlet基类，它会加载自己的应用上下文。
 *     FrameworkServlet是Spring功能全面的{@link DispatcherServlet}的直接基类。
 * </p>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #addRequiredProperty
 * @see #initServletBean
 * @see #doGet
 * @see #doPost
 */
@SuppressWarnings("serial")
public abstract class HttpServletBean extends HttpServlet implements EnvironmentCapable, EnvironmentAware {

	/** 可用于子类的日志记录器 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private ConfigurableEnvironment environment;

	private final Set<String> requiredProperties = new HashSet<>(4);


	/**
	 * <p>
	 *     子类可以调用此方法来指定某个属性（必须与其公开的JavaBean属性相匹配）是必需的，并且必须作为配置参数提供。
	 *     此方法应在子类的构造函数中调用。
	 * </p>
	 * <p>此方法仅适用于由ServletConfig实例驱动的传统初始化场景。</p>
	 * @param property 必需属性的名称
	 */
	protected final void addRequiredProperty(String property) {
		this.requiredProperties.add(property);
	}

	/**
	 * <p>设置此Servlet运行所需的{@code Environment}。</p>
	 * <p>在此设置的任何环境将覆盖默认提供的{@link StandardServletEnvironment}。</p>
	 * @throws IllegalArgumentException 如果环境无法转换为{@code ConfigurableEnvironment}类型时抛出异常
	 */
	@Override
	public void setEnvironment(Environment environment) {
		Assert.isInstanceOf(ConfigurableEnvironment.class, environment, "ConfigurableEnvironment required");
		this.environment = (ConfigurableEnvironment) environment;
	}

	/**
	 * <p>返回与此Servlet关联的{@link Environment}。</p>
	 * <p>如果未指定环境，将通过{@link #createEnvironment()}方法初始化默认环境。</p>
	 */
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * <p>创建并返回一个新的{@link StandardServletEnvironment}。</p>
	 * <p>子类可以重写此方法，以便配置环境或指定返回的环境类型。</p>
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * <p>将配置参数映射到此Servlet的Bean属性上，并调用子类初始化方法。</p>
	 * @throws ServletException 如果Bean属性无效（或缺少必需属性），或子类初始化失败时抛出异常
	 */
	@Override
	public final void init() throws ServletException {
		if (logger.isDebugEnabled()) {
			logger.debug("Initializing servlet '" + getServletName() + "'");
		}

		// 从初始化参数设置bean属性。
		PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
		if (!pvs.isEmpty()) {
			try {
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
				ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());
				bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
				initBeanWrapper(bw);
				bw.setPropertyValues(pvs, true);
			}
			catch (BeansException ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
				}
				throw ex;
			}
		}

		// 让子类做任何他们希望的初始化。
		initServletBean();

		if (logger.isDebugEnabled()) {
			logger.debug("Servlet '" + getServletName() + "' configured successfully");
		}
	}

	/**
	 * <p>初始化此 HttpServletBean 的 BeanWrapper，可选择注册自定义编辑器。</p>
	 * <p>此默认实现为空方法。</p>
	 * @param bw 要初始化的 BeanWrapper 实例
	 * @throws BeansException 如果 BeanWrapper 的方法抛出异常时抛出
	 * @see org.springframework.beans.BeanWrapper#registerCustomEditor
	 */
	protected void initBeanWrapper(BeanWrapper bw) throws BeansException {
	}

	/**
	 * <p>子类可以重写此方法以执行自定义初始化。</p>
	 * <p>在执行此方法前，此Servlet的所有bean属性都将已完成设置。</p>
	 * <p>此默认实现为空方法。</p>
	 * @throws ServletException 如果子类初始化失败时抛出
	 */
	protected void initServletBean() throws ServletException {
	}

	/**
	 * <p>被重写的方法，当尚未设置ServletConfig时直接返回{@code null}。</p>
	 * @see #getServletConfig()
	 */
	@Override
	@Nullable
	public String getServletName() {
		return (getServletConfig() != null ? getServletConfig().getServletName() : null);
	}


	/**
	 * 基于ServletConfig初始化参数创建的PropertyValues实现。
	 */
	private static class ServletConfigPropertyValues extends MutablePropertyValues {

		/**
		 * 创建新的 ServletConfigPropertyValues 实例。
		 * @param config 用于获取属性值的 ServletConfig 对象
		 * @param requiredProperties 必须设置的属性名集合，此类属性不允许使用默认值
		 * @throws ServletException 当缺少任何必需属性时抛出
		 */
		public ServletConfigPropertyValues(ServletConfig config, Set<String> requiredProperties)
				throws ServletException {

			Set<String> missingProps = (!CollectionUtils.isEmpty(requiredProperties) ?
					new HashSet<>(requiredProperties) : null);

			Enumeration<String> paramNames = config.getInitParameterNames();
			while (paramNames.hasMoreElements()) {
				String property = paramNames.nextElement();
				Object value = config.getInitParameter(property);
				addPropertyValue(new PropertyValue(property, value));
				if (missingProps != null) {
					missingProps.remove(property);
				}
			}

			// Fail if we are still missing properties.
			if (!CollectionUtils.isEmpty(missingProps)) {
				throw new ServletException(
						"Initialization from ServletConfig for servlet '" + config.getServletName() +
						"' failed; the following required properties were missing: " +
						StringUtils.collectionToDelimitedString(missingProps, ", "));
			}
		}
	}

}
