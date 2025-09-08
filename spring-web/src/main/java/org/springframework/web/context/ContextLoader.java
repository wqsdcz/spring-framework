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

package org.springframework.web.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 执行root应用程序上下文的实际初始化工作的方法。由 {@link ContextLoaderListener} 调用。
 *
 * <p>首先在 {@code web.xml} 的 context-param 级别查找 {@link #CONTEXT_CLASS_PARAM "contextClass"} 参数以指定上下文类类型，
 * 如果未找到则回退到默认的 {@link org.springframework.web.context.support.XmlWebApplicationContext}。
 * 使用默认的 ContextLoader 实现时，任何指定的上下文类都需要实现 {@link ConfigurableWebApplicationContext} 接口。
 *
 * <p>处理 {@link #CONFIG_LOCATION_PARAM "contextConfigLocation"} 上下文参数，
 * 并将其值传递给上下文实例，解析为可能由任意数量的逗号和空格分隔的多个文件路径，
 * 例如："WEB-INF/applicationContext1.xml, WEB-INF/applicationContext2.xml"。
 * 同时支持 Ant 风格路径模式，例如："WEB-INF/*Context.xml,WEB-INF/spring*.xml" 或 "WEB-INF/&#42;&#42;/*Context.xml"。
 * 如果未明确指定，上下文实现应使用默认位置（对于 XmlWebApplicationContext："/WEB-INF/applicationContext.xml"）。
 *
 * <p>注意：在有多个配置位置的情况下，后加载的 bean 定义将覆盖先前加载文件中定义的 bean 定义
 * （至少在使用 Spring 默认的 ApplicationContext 实现时如此）。这使得可以通过额外的 XML 文件来有意覆盖某些 bean 定义。
 *
 * <p>除了加载根应用程序上下文之外，此类还可以选择性地加载或获取共享父上下文并将其挂接到根应用程序上下文。
 * 更多信息请参阅 {@link #loadParentContext(ServletContext)} 方法。
 *
 * <p>从 Spring 3.1 开始，{@code ContextLoader} 支持通过
 * {@link #ContextLoader(WebApplicationContext)} 构造函数注入根 Web 应用程序上下文，
 * 从而支持在 Servlet 3.0+ 环境中进行编程式配置。
 * 使用示例请参阅 {@link org.springframework.web.WebApplicationInitializer}。
 *
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @author Sam Brannen
 * @since 2003年2月17日
 * @see ContextLoaderListener
 * @see ConfigurableWebApplicationContext
 * @see org.springframework.web.context.support.XmlWebApplicationContext
 */
public class ContextLoader {

	/**
	 * 根WebApplicationContext ID的配置参数，将作为底层BeanFactory的序列化ID使用：{@value}
	 */
	public static final String CONTEXT_ID_PARAM = "contextId";

	/**
	 * Servlet上下文参数名称（即{@value}），该参数可用于指定根上下文的配置位置，否则将回退到实现的默认值。
	 * @see org.springframework.web.context.support.XmlWebApplicationContext#DEFAULT_CONFIG_LOCATION
	 */
	public static final String CONFIG_LOCATION_PARAM = "contextConfigLocation";

	/**
	 * 用于指定根WebApplicationContext实现类的配置参数：{@value}
	 * @see #determineContextClass(ServletContext)
	 */
	public static final String CONTEXT_CLASS_PARAM = "contextClass";

	/**
	 * 用于初始化根Web应用上下文的{@link ApplicationContextInitializer}类配置参数：{@value}
	 * @see #customizeContext(ServletContext, ConfigurableWebApplicationContext)
	 */
	public static final String CONTEXT_INITIALIZER_CLASSES_PARAM = "contextInitializerClasses";

	/**
	 * 用于全局{@link ApplicationContextInitializer}类的配置参数，
	 * 该参数将作用于当前应用中的所有Web应用上下文初始化过程：{@value}
	 * @see #customizeContext(ServletContext, ConfigurableWebApplicationContext)
	 */
	public static final String GLOBAL_INITIALIZER_CLASSES_PARAM = "globalInitializerClasses";

	/**
	 * 在单个初始化参数字符串值中，这些字符的任意数量都被视为多个值之间的分隔符。
	 */
	private static final String INIT_PARAM_DELIMITERS = ",; \t\n";

	/**
	 * 定义ContextLoader默认策略名称的类路径资源（相对于ContextLoader类）的名称。
	 */
	private static final String DEFAULT_STRATEGIES_PATH = "ContextLoader.properties";


	private static final Properties defaultStrategies;

	static {
		// 从属性文件中加载默认策略实现。
		// 目前这完全是内部机制，不应由应用程序开发者来自定义。
		try {
			ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, ContextLoader.class);
			defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load 'ContextLoader.properties': " + ex.getMessage());
		}
	}


	/**
	 * Map from (thread context) ClassLoader to corresponding 'current' WebApplicationContext.
	 */
	private static final Map<ClassLoader, WebApplicationContext> currentContextPerThread =
			new ConcurrentHashMap<>(1);

	/**
	 * The 'current' WebApplicationContext, if the ContextLoader class is
	 * deployed in the web app ClassLoader itself.
	 */
	@Nullable
	private static volatile WebApplicationContext currentContext;


	/**
	 * The root WebApplicationContext instance that this loader manages.
	 */
	@Nullable
	private WebApplicationContext context;

	/** Actual ApplicationContextInitializer instances to apply to the context */
	private final List<ApplicationContextInitializer<ConfigurableApplicationContext>> contextInitializers =
			new ArrayList<>();


	/**
	 * 创建一个新的{@code ContextLoader}实例，该实例将基于servlet上下文参数
	 * "contextClass"和"contextConfigLocation"创建Web应用上下文。
	 * 关于各参数的默认值，请参阅类级别文档。
	 *
	 * <p>此构造函数通常在将{@code ContextLoaderListener}子类声明为
	 * {@code web.xml}中的{@code <listener>}时使用，因为需要无参构造函数。
	 *
	 * <p>创建的应用上下文将以{@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}
	 * 为属性名注册到ServletContext中，子类可以在容器关闭时自由调用
	 * {@link #closeWebApplicationContext}方法来关闭应用上下文。
	 *
	 * @see #ContextLoader(WebApplicationContext)
	 * @see #initWebApplicationContext(ServletContext)
	 * @see #closeWebApplicationContext(ServletContext)
	 */
	public ContextLoader() {
	}

	/**
	 * 使用给定的应用上下文创建新的{@code ContextLoader}实例。
	 * 此构造函数适用于Servlet 3.0+环境，其中可通过{@link ServletContext#addListener} API
	 * 实现基于实例的监听器注册。
	 *
	 * <p>给定的上下文可能尚未被{@linkplain ConfigurableApplicationContext#refresh() 刷新}。
	 * 如果该上下文同时满足：(a) 是{@link ConfigurableWebApplicationContext}的实现，且(b) <strong>尚未</strong>
	 * 被刷新（推荐方式），则将执行以下操作：
	 * <ul>
	 * <li>如果给定上下文尚未分配{@linkplain ConfigurableApplicationContext#setId ID}，将为其分配一个</li>
	 * <li>将委托{@code ServletContext}和{@code ServletConfig}对象给应用上下文</li>
	 * <li>调用{@link #customizeContext}方法</li>
	 * <li>应用通过"contextInitializerClasses"初始化参数指定的任何{@link ApplicationContextInitializer}</li>
	 * <li>调用{@link ConfigurableApplicationContext#refresh refresh()}方法</li>
	 * </ul>
	 * 如果上下文已被刷新或不实现{@code ConfigurableWebApplicationContext}接口，
	 * 则不会执行上述任何操作，假定用户已根据其特定需求执行（或未执行）这些操作。
	 *
	 * <p>使用示例请参阅{@link org.springframework.web.WebApplicationInitializer}。
	 *
	 * <p>无论哪种情况，给定的应用上下文都将以{@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}
	 * 为属性名注册到ServletContext中，子类可以在容器关闭时自由调用{@link #closeWebApplicationContext}
	 * 方法来关闭应用上下文。
	 *
	 * @param context 要管理的应用上下文
	 * @see #initWebApplicationContext(ServletContext)
	 * @see #closeWebApplicationContext(ServletContext)
	 */
	public ContextLoader(WebApplicationContext context) {
		this.context = context;
	}


	/**
	 * 指定应使用哪些{@link ApplicationContextInitializer}实例
	 * 来初始化此{@code ContextLoader}所使用的应用上下文。
	 * @since 4.2
	 * @see #configureAndRefreshWebApplicationContext
	 * @see #customizeContext
	 */
	@SuppressWarnings("unchecked")
	public void setContextInitializers(@Nullable ApplicationContextInitializer<?>... initializers) {
		if (initializers != null) {
			for (ApplicationContextInitializer<?> initializer : initializers) {
				this.contextInitializers.add((ApplicationContextInitializer<ConfigurableApplicationContext>) initializer);
			}
		}
	}


	/**
	 * 初始化Spring的Web应用程序上下文（适用于给定的ServletContext），
	 * 使用构造时提供的应用程序上下文，或根据"@{@link #CONTEXT_CLASS_PARAM contextClass}"
	 * 和"@{@link #CONFIG_LOCATION_PARAM contextConfigLocation}"上下文参数创建新上下文。
	 * @param servletContext 当前Servlet上下文
	 * @return 新的WebApplicationContext
	 * @see #ContextLoader(WebApplicationContext)
	 * @see #CONTEXT_CLASS_PARAM
	 * @see #CONFIG_LOCATION_PARAM
	 */
	public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
		if (servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
			throw new IllegalStateException(
					"Cannot initialize context because there is already a root application context present - " +
					"check whether you have multiple ContextLoader* definitions in your web.xml!");
		}

		Log logger = LogFactory.getLog(ContextLoader.class);
		servletContext.log("Initializing Spring root WebApplicationContext");
		if (logger.isInfoEnabled()) {
			logger.info("Root WebApplicationContext: initialization started");
		}
		long startTime = System.currentTimeMillis();

		try {
			// Store context in local instance variable, to guarantee that
			// it is available on ServletContext shutdown.
			if (this.context == null) {
				this.context = createWebApplicationContext(servletContext);
			}
			if (this.context instanceof ConfigurableWebApplicationContext) {
				ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) this.context;
				if (!cwac.isActive()) {
					// The context has not yet been refreshed -> provide services such as
					// setting the parent context, setting the application context id, etc
					if (cwac.getParent() == null) {
						// The context instance was injected without an explicit parent ->
						// determine parent for root web application context, if any.
						ApplicationContext parent = loadParentContext(servletContext);
						cwac.setParent(parent);
					}
					configureAndRefreshWebApplicationContext(cwac, servletContext);
				}
			}
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);

			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			if (ccl == ContextLoader.class.getClassLoader()) {
				currentContext = this.context;
			}
			else if (ccl != null) {
				currentContextPerThread.put(ccl, this.context);
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Published root WebApplicationContext as ServletContext attribute with name [" +
						WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE + "]");
			}
			if (logger.isInfoEnabled()) {
				long elapsedTime = System.currentTimeMillis() - startTime;
				logger.info("Root WebApplicationContext: initialization completed in " + elapsedTime + " ms");
			}

			return this.context;
		}
		catch (RuntimeException ex) {
			logger.error("Context initialization failed", ex);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
			throw ex;
		}
		catch (Error err) {
			logger.error("Context initialization failed", err);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, err);
			throw err;
		}
	}
	/**
	 * 为此加载器实例化根WebApplicationContext，可使用默认上下文类，若指定了自定义上下文类则使用自定义类。
	 * <p>此实现要求自定义上下文实现{@link ConfigurableWebApplicationContext}接口。可在子类中重写此方法。
	 * <p>此外，在刷新上下文之前会调用{@link #customizeContext}方法，允许子类对上下文执行自定义修改。
	 * @param sc 当前servlet上下文
	 * @return 根WebApplicationContext
	 * @see ConfigurableWebApplicationContext
	 */
	protected WebApplicationContext createWebApplicationContext(ServletContext sc) {
		Class<?> contextClass = determineContextClass(sc);
		if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
			throw new ApplicationContextException("Custom context class [" + contextClass.getName() +
					"] is not of type [" + ConfigurableWebApplicationContext.class.getName() + "]");
		}
		return (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
	}

	/**
	 * 返回要使用的WebApplicationContext实现类：若未指定则使用默认的XmlWebApplicationContext，
	 * 若指定了自定义上下文类则使用自定义类。
	 * @param servletContext 当前servlet上下文
	 * @return 要使用的WebApplicationContext实现类
	 * @see #CONTEXT_CLASS_PARAM
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 */
	protected Class<?> determineContextClass(ServletContext servletContext) {
		String contextClassName = servletContext.getInitParameter(CONTEXT_CLASS_PARAM);
		if (contextClassName != null) {
			try {
				return ClassUtils.forName(contextClassName, ClassUtils.getDefaultClassLoader());
			}
			catch (ClassNotFoundException ex) {
				throw new ApplicationContextException(
						"Failed to load custom context class [" + contextClassName + "]", ex);
			}
		}
		else {
			contextClassName = defaultStrategies.getProperty(WebApplicationContext.class.getName());
			try {
				return ClassUtils.forName(contextClassName, ContextLoader.class.getClassLoader());
			}
			catch (ClassNotFoundException ex) {
				throw new ApplicationContextException(
						"Failed to load default context class [" + contextClassName + "]", ex);
			}
		}
	}

	protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac, ServletContext sc) {
		if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
			// The application context id is still set to its original default value
			// -> assign a more useful id based on available information
			String idParam = sc.getInitParameter(CONTEXT_ID_PARAM);
			if (idParam != null) {
				wac.setId(idParam);
			}
			else {
				// Generate default id...
				wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
						ObjectUtils.getDisplayString(sc.getContextPath()));
			}
		}

		wac.setServletContext(sc);
		String configLocationParam = sc.getInitParameter(CONFIG_LOCATION_PARAM);
		if (configLocationParam != null) {
			wac.setConfigLocation(configLocationParam);
		}

		// The wac environment's #initPropertySources will be called in any case when the context
		// is refreshed; do it eagerly here to ensure servlet property sources are in place for
		// use in any post-processing or initialization that occurs below prior to #refresh
		ConfigurableEnvironment env = wac.getEnvironment();
		if (env instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment) env).initPropertySources(sc, null);
		}

		customizeContext(sc, wac);
		wac.refresh();
	}

	/**
	 * 在向上下文提供配置位置之后，但在上下文<em>刷新</em>之前，
	 * 对此ContextLoader创建的{@link ConfigurableWebApplicationContext}进行自定义。
	 *
	 * <p>默认实现会{@linkplain #determineContextInitializerClasses(ServletContext) 判断}
	 * 是否通过{@linkplain #CONTEXT_INITIALIZER_CLASSES_PARAM 上下文初始化参数}指定了
	 * 上下文初始化器类（若有），并通过{@linkplain ApplicationContextInitializer#initialize 调用每个}
	 * 初始化器来初始化给定的Web应用上下文。
	 *
	 * <p>任何实现了{@link org.springframework.core.Ordered Ordered}接口或标注了
	 * @{@link org.springframework.core.annotation.Order Order}注解的
	 * {@code ApplicationContextInitializers}都将被正确排序。
	 *
	 * @param sc 当前servlet上下文
	 * @param wac 新创建的应用上下文
	 * @see #CONTEXT_INITIALIZER_CLASSES_PARAM
	 * @see ApplicationContextInitializer#initialize(ConfigurableApplicationContext)
	 */
	protected void customizeContext(ServletContext sc, ConfigurableWebApplicationContext wac) {
		List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> initializerClasses =
				determineContextInitializerClasses(sc);

		for (Class<ApplicationContextInitializer<ConfigurableApplicationContext>> initializerClass : initializerClasses) {
			Class<?> initializerContextClass =
					GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
			if (initializerContextClass != null && !initializerContextClass.isInstance(wac)) {
				throw new ApplicationContextException(String.format(
						"Could not apply context initializer [%s] since its generic parameter [%s] " +
						"is not assignable from the type of application context used by this " +
						"context loader: [%s]", initializerClass.getName(), initializerContextClass.getName(),
						wac.getClass().getName()));
			}
			this.contextInitializers.add(BeanUtils.instantiateClass(initializerClass));
		}

		AnnotationAwareOrderComparator.sort(this.contextInitializers);
		for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : this.contextInitializers) {
			initializer.initialize(wac);
		}
	}

	/**
	 * 返回要使用的{@link ApplicationContextInitializer}实现类（若已通过
	 * {@link #CONTEXT_INITIALIZER_CLASSES_PARAM}参数指定）。
	 * @param servletContext 当前servlet上下文
	 * @see #CONTEXT_INITIALIZER_CLASSES_PARAM
	 */
	protected List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>>
			determineContextInitializerClasses(ServletContext servletContext) {

		List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> classes =
				new ArrayList<>();

		String globalClassNames = servletContext.getInitParameter(GLOBAL_INITIALIZER_CLASSES_PARAM);
		if (globalClassNames != null) {
			for (String className : StringUtils.tokenizeToStringArray(globalClassNames, INIT_PARAM_DELIMITERS)) {
				classes.add(loadInitializerClass(className));
			}
		}

		String localClassNames = servletContext.getInitParameter(CONTEXT_INITIALIZER_CLASSES_PARAM);
		if (localClassNames != null) {
			for (String className : StringUtils.tokenizeToStringArray(localClassNames, INIT_PARAM_DELIMITERS)) {
				classes.add(loadInitializerClass(className));
			}
		}

		return classes;
	}

	@SuppressWarnings("unchecked")
	private Class<ApplicationContextInitializer<ConfigurableApplicationContext>> loadInitializerClass(String className) {
		try {
			Class<?> clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
			if (!ApplicationContextInitializer.class.isAssignableFrom(clazz)) {
				throw new ApplicationContextException(
						"Initializer class does not implement ApplicationContextInitializer interface: " + clazz);
			}
			return (Class<ApplicationContextInitializer<ConfigurableApplicationContext>>) clazz;
		}
		catch (ClassNotFoundException ex) {
			throw new ApplicationContextException("Failed to load context initializer class [" + className + "]", ex);
		}
	}

    /**
     * 模板方法（提供默认实现，可由子类重写），用于加载或获取将作为根WebApplicationContext父级上下文的应用上下文实例。
     * 若该方法返回值为null，则不设置父级上下文。
     *
     * <p>此处加载父级上下文的主要原因是：允许多个根Web应用上下文成为共享EAR上下文的子上下文，或使其与EJB可见的同一父级上下文共享。
	 * 对于纯Web应用程序，通常无需为根Web应用上下文设置父级上下文。
     *
     * <p>默认实现直接返回{@code null}（从5.0版本开始）。
     *
     * @param servletContext 当前servlet上下文
     * @return 父级应用上下文，不存在时返回{@code null}
     */
	@Nullable
	protected ApplicationContext loadParentContext(ServletContext servletContext) {
		return null;
	}

	/**
	 * 关闭给定servlet上下文中Spring的Web应用上下文。
	 * <p>如果重写了{@link #loadParentContext(ServletContext)}方法，可能也需要重写此方法。
	 * @param servletContext WebApplicationContext运行所在的ServletContext
	 */
	public void closeWebApplicationContext(ServletContext servletContext) {
		servletContext.log("Closing Spring root WebApplicationContext");
		try {
			if (this.context instanceof ConfigurableWebApplicationContext) {
				((ConfigurableWebApplicationContext) this.context).close();
			}
		}
		finally {
			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			if (ccl == ContextLoader.class.getClassLoader()) {
				currentContext = null;
			}
			else if (ccl != null) {
				currentContextPerThread.remove(ccl);
			}
			servletContext.removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
	}


	/**
	 * 获取当前线程的Spring根Web应用上下文（即当前线程的上下文ClassLoader，
	 * 该加载器需为Web应用的ClassLoader）。
	 * @return 当前根Web应用上下文，如果未找到则返回{@code null}
	 * @see org.springframework.web.context.support.SpringBeanAutowiringSupport
	 */
	@Nullable
	public static WebApplicationContext getCurrentWebApplicationContext() {
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		if (ccl != null) {
			WebApplicationContext ccpt = currentContextPerThread.get(ccl);
			if (ccpt != null) {
				return ccpt;
			}
		}
		return currentContext;
	}

}
