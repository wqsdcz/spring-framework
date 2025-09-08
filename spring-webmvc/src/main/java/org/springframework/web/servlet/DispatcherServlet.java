/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.ThemeSource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.WebUtils;

/**
 * <p>
 *     负责HTTP请求处理器/控制器的调度工作的中央调度器，例如：用于Web UI控制器或基于HTTP的远程服务导出器。
 *     它将Web请求分派给已注册的处理器进行处理，并提供便捷的映射和异常处理机制。
 * </p>
 * <p>
 *     该Servlet非常灵活：只需安装适当的适配器类，即可用于几乎所有工作流程。
 *     它具有以下功能，使其区别于其他基于请求驱动的Web MVC框架：
 *     <ul>
 *         <li>它基于JavaBeans配置机制。</li>
 *         <li>
 *             可以使用任何{@link HandlerMapping}实现（预构建的或作为应用程序的一部分提供的）来控制请求到处理器对象的路由。
 *             默认实现为{@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping}和
 *             {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping}。
 *             HandlerMapping对象可以在Servlet的应用上下文中定义为Bean，实现HandlerMapping接口，从而覆盖默认的HandlerMapping（如果存在）
 *             。HandlerMapping可以指定任何Bean名称（它们按类型进行检测）。
 *         </li>
 *         <li>
 *             可以使用任何{@link HandlerAdapter}；这允许使用任何处理器接口。
 *             默认适配器为{@link org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter}和
 *             {@link org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter}，
 *             分别用于Spring的{@link org.springframework.web.HttpRequestHandler}和
 *             {@link org.springframework.web.servlet.mvc.Controller}接口。
 *             默认情况下还会注册{@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter}。
 *             HandlerAdapter对象可以作为Bean添加到应用上下文中，以覆盖默认的HandlerAdapter。
 *             与HandlerMapping类似，HandlerAdapter可以指定任何Bean名称（它们按类型进行检测）。
 *         </li>
 *         <li>
 *             调度器的异常解析策略可以通过{@link HandlerExceptionResolver}指定，例如将某些异常映射到错误页面。
 *             默认实现为{@link org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver}、
 *             {@link org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver}和
 *             {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver}。
 *             这些HandlerExceptionResolver可以通过应用上下文进行覆盖。
 *             HandlerExceptionResolver可以指定任何Bean名称（它们按类型进行检测）。
 *         </li>
 *         <li>
 *             其视图解析策略可以通过{@link ViewResolver}实现指定，将符号视图名称解析为View对象。
 *             默认实现为{@link org.springframework.web.servlet.view.InternalResourceViewResolver}。
 *             ViewResolver对象可以作为Bean添加到应用上下文中，以覆盖默认的ViewResolver。
 *             ViewResolver可以指定任何Bean名称（它们按类型进行检测）。
 *         </li>
 *         <li>
 *             如果用户未提供{@link View}或视图名称，则配置的{@link RequestToViewNameTranslator}会将当前请求转换为视图名称。
 *             对应的Bean名称为"viewNameTranslator"；默认实现为{@link org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator}。
 *         </li>
 *         <li>
 *             调度器解析多部分请求的策略由{@link org.springframework.web.multipart.MultipartResolver}实现决定。
 *             包含Apache Commons FileUpload和Servlet 3的实现；典型选择是{@link org.springframework.web.multipart.commons.CommonsMultipartResolver}。
 *             MultipartResolver的Bean名称为"multipartResolver"；默认情况下未设置。
 *         </li>
 *         <li>
 *             其区域解析策略由{@link LocaleResolver}决定。开箱即用的实现通过HTTP接受头、Cookie或会话工作。
 *             LocaleResolver的Bean名称为"localeResolver"；默认实现为{@link org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver}。
 *         </li>
 *         <li>
 *             其主题解析策略由{@link ThemeResolver}决定。包含固定主题以及Cookie和会话存储的实现。
 *             ThemeResolver的Bean名称为"themeResolver"；默认实现为{@link org.springframework.web.servlet.theme.FixedThemeResolver}。
 *         </li>
 *     </ul>
 * <p>
 *     <b>注意：仅当调度器中存在相应的{@code HandlerMapping}（用于类型级别注解）和/或{@code HandlerAdapter}（用于方法级别注解）时，{@code @RequestMapping}注解才会被处理。</b>
 *     默认情况下即是如此。但是，如果您正在定义自定义的{@code HandlerMapping}或{@code HandlerAdapter}，
 *     则需要确保也定义了相应的自定义{@code RequestMappingHandlerMapping}和/或{@code RequestMappingHandlerAdapter}——前提是您打算使用{@code @RequestMapping}。
 * </p>
 * <p>
 *     <b>Web应用程序可以定义任意数量的DispatcherServlet。</b>
 *     每个Servlet将在其自己的命名空间中运行，加载其自己的应用上下文（包含映射、处理器等）。
 *     只有由{@link org.springframework.web.context.ContextLoaderListener}加载的根应用上下文（如果有）会被共享。
 * </p>
 * <p>
 *     从Spring 3.1开始，{@code DispatcherServlet}现在可以注入Web应用上下文，而不是在内部创建自己的上下文。
 *     这在支持Servlet实例编程注册的Servlet 3.0+环境中非常有用。
 *     详细信息请参阅{@link #DispatcherServlet(WebApplicationContext)}的Javadoc。
 * </p>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @see org.springframework.web.HttpRequestHandler
 * @see org.springframework.web.servlet.mvc.Controller
 * @see org.springframework.web.context.ContextLoaderListener
 */
@SuppressWarnings("serial")
public class DispatcherServlet extends FrameworkServlet {

	/** Well-known name for the MultipartResolver object in the bean factory for this namespace. */
	public static final String MULTIPART_RESOLVER_BEAN_NAME = "multipartResolver";

	/** Well-known name for the LocaleResolver object in the bean factory for this namespace. */
	public static final String LOCALE_RESOLVER_BEAN_NAME = "localeResolver";

	/** Well-known name for the ThemeResolver object in the bean factory for this namespace. */
	public static final String THEME_RESOLVER_BEAN_NAME = "themeResolver";

	/**
	 * Well-known name for the HandlerMapping object in the bean factory for this namespace.
	 * Only used when "detectAllHandlerMappings" is turned off.
	 * @see #setDetectAllHandlerMappings
	 */
	public static final String HANDLER_MAPPING_BEAN_NAME = "handlerMapping";

	/**
	 * Well-known name for the HandlerAdapter object in the bean factory for this namespace.
	 * Only used when "detectAllHandlerAdapters" is turned off.
	 * @see #setDetectAllHandlerAdapters
	 */
	public static final String HANDLER_ADAPTER_BEAN_NAME = "handlerAdapter";

	/**
	 * Well-known name for the HandlerExceptionResolver object in the bean factory for this namespace.
	 * Only used when "detectAllHandlerExceptionResolvers" is turned off.
	 * @see #setDetectAllHandlerExceptionResolvers
	 */
	public static final String HANDLER_EXCEPTION_RESOLVER_BEAN_NAME = "handlerExceptionResolver";

	/**
	 * Well-known name for the RequestToViewNameTranslator object in the bean factory for this namespace.
	 */
	public static final String REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME = "viewNameTranslator";

	/**
	 * Well-known name for the ViewResolver object in the bean factory for this namespace.
	 * Only used when "detectAllViewResolvers" is turned off.
	 * @see #setDetectAllViewResolvers
	 */
	public static final String VIEW_RESOLVER_BEAN_NAME = "viewResolver";

	/**
	 * Well-known name for the FlashMapManager object in the bean factory for this namespace.
	 */
	public static final String FLASH_MAP_MANAGER_BEAN_NAME = "flashMapManager";

	/**
	 * Request attribute to hold the current web application context.
	 * Otherwise only the global web app context is obtainable by tags etc.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#findWebApplicationContext
	 */
	public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = DispatcherServlet.class.getName() + ".CONTEXT";

	/**
	 * Request attribute to hold the current LocaleResolver, retrievable by views.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocaleResolver
	 */
	public static final String LOCALE_RESOLVER_ATTRIBUTE = DispatcherServlet.class.getName() + ".LOCALE_RESOLVER";

	/**
	 * Request attribute to hold the current ThemeResolver, retrievable by views.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getThemeResolver
	 */
	public static final String THEME_RESOLVER_ATTRIBUTE = DispatcherServlet.class.getName() + ".THEME_RESOLVER";

	/**
	 * Request attribute to hold the current ThemeSource, retrievable by views.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getThemeSource
	 */
	public static final String THEME_SOURCE_ATTRIBUTE = DispatcherServlet.class.getName() + ".THEME_SOURCE";

	/**
	 * Name of request attribute that holds a read-only {@code Map<String,?>}
	 * with "input" flash attributes saved by a previous request, if any.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getInputFlashMap(HttpServletRequest)
	 */
	public static final String INPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".INPUT_FLASH_MAP";

	/**
	 * Name of request attribute that holds the "output" {@link FlashMap} with
	 * attributes to save for a subsequent request.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getOutputFlashMap(HttpServletRequest)
	 */
	public static final String OUTPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".OUTPUT_FLASH_MAP";

	/**
	 * Name of request attribute that holds the {@link FlashMapManager}.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getFlashMapManager(HttpServletRequest)
	 */
	public static final String FLASH_MAP_MANAGER_ATTRIBUTE = DispatcherServlet.class.getName() + ".FLASH_MAP_MANAGER";

	/**
	 * Name of request attribute that exposes an Exception resolved with an
	 * {@link HandlerExceptionResolver} but where no view was rendered
	 * (e.g. setting the status code).
	 */
	public static final String EXCEPTION_ATTRIBUTE = DispatcherServlet.class.getName() + ".EXCEPTION";

	/** Log category to use when no mapped handler is found for a request. */
	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";

	/**
	 * Name of the class path resource (relative to the DispatcherServlet class)
	 * that defines DispatcherServlet's default strategy names.
	 */
	private static final String DEFAULT_STRATEGIES_PATH = "DispatcherServlet.properties";

	/**
	 * Common prefix that DispatcherServlet's default strategy attributes start with.
	 */
	private static final String DEFAULT_STRATEGIES_PREFIX = "org.springframework.web.servlet";

	/** Additional logger to use when no mapped handler is found for a request. */
	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);

	private static final Properties defaultStrategies;

	static {
		// 从properties文件中加载默认策略实现。当前，这是严格属于内部的实现，不意味着可由应用程序开发者自定义。
		try {
			ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, DispatcherServlet.class);
			defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load '" + DEFAULT_STRATEGIES_PATH + "': " + ex.getMessage());
		}
	}

	/** 检测所有HandlerMapping类型的bean，还是仅期望找到名为“handlerMapping”这个 bean 呢？ */
	private boolean detectAllHandlerMappings = true;

	/** 检测所有HandlerAdapter类型的bean，还是仅期望找到名为“handlerAdapter”这个 bean 呢？ */
	private boolean detectAllHandlerAdapters = true;

	/** 检测所有HandlerExceptionResolver类型的bean，还是仅期望找到名为“handlerExceptionResolver”这个 bean 呢？ */
	private boolean detectAllHandlerExceptionResolvers = true;

	/** 检测所有ViewResolver类型的bean，还是仅期望找到名为“viewResolver”这个 bean 呢？ */
	private boolean detectAllViewResolvers = true;

	/** Throw a NoHandlerFoundException if no Handler was found to process this request? **/
	private boolean throwExceptionIfNoHandlerFound = false;

	/** Perform cleanup of request attributes after include request? */
	private boolean cleanupAfterInclude = true;

	/** MultipartResolver used by this servlet */
	@Nullable
	private MultipartResolver multipartResolver;

	/** LocaleResolver used by this servlet */
	@Nullable
	private LocaleResolver localeResolver;

	/** ThemeResolver used by this servlet */
	@Nullable
	private ThemeResolver themeResolver;

	/** List of HandlerMappings used by this servlet */
	@Nullable
	private List<HandlerMapping> handlerMappings;

	/** List of HandlerAdapters used by this servlet */
	@Nullable
	private List<HandlerAdapter> handlerAdapters;

	/** List of HandlerExceptionResolvers used by this servlet */
	@Nullable
	private List<HandlerExceptionResolver> handlerExceptionResolvers;

	/** RequestToViewNameTranslator used by this servlet */
	@Nullable
	private RequestToViewNameTranslator viewNameTranslator;

	/** FlashMapManager used by this servlet */
	@Nullable
	private FlashMapManager flashMapManager;

	/** List of ViewResolvers used by this servlet */
	@Nullable
	private List<ViewResolver> viewResolvers;


	/**
	 * <p>
	 *     创建一个新的{@code DispatcherServlet}实例，该实例将基于默认值及通过servlet初始化参数提供的值来创建其内部的Web应用程序上下文。
	 *     通常用于Servlet 2.5或更早版本的环境，此类环境中注册servlet的唯一方式是通过{@code web.xml}配置文件，且要求使用无参构造函数。
	 * </p>
	 * <p>
	 *     通过调用{@link #setContextConfigLocation}（初始化参数'contextConfigLocation'）
	 *     可指定由 {@linkplain #DEFAULT_CONTEXT_CLASS 默认XmlWebApplicationContext} 加载的XML配置文件。
	 * </p>
	 * <p>
	 *     通过调用{@link #setContextClass}（初始化参数'contextClass'）可覆盖默认的{@code XmlWebApplicationContext}，
	 *     允许指定替代类（例如{@code AnnotationConfigWebApplicationContext}）。
	 * </p>
	 * <p>
	 *     通过调用{@link #setContextInitializerClasses}（初始化参数'contextInitializerClasses'）
	 *     可指定在refresh()方法执行前，应使用哪些{@code ApplicationContextInitializer}类 来进一步配置内部应用程序上下文。
	 * </p>
	 * @see #DispatcherServlet(WebApplicationContext)
	 */
	public DispatcherServlet() {
		super();
		setDispatchOptionsRequest(true);
	}

	/**
	 * <p>
	 *     创建一个带有指定Web应用程序上下文的新{@code DispatcherServlet}实例。
	 *     此构造函数在Servlet 3.0+环境中特别有用，该类环境支持通过{@link ServletContext#addServlet} API实现基于实例的servlet注册。
	 * </p>
	 * <p>
	 *     使用此构造函数意味着以下属性/初始化参数将被忽略：
	 *     <ul>
	 *         <li>{@link #setContextClass(Class)} / 'contextClass'</li>
	 *         <li>{@link #setContextConfigLocation(String)} / 'contextConfigLocation'</li>
	 *         <li>{@link #setContextAttribute(String)} / 'contextAttribute'</li>
	 *         <li>{@link #setNamespace(String)} / 'namespace'</li>
	 *     </ul>
	 * </p>
	 * <p>
	 *     给定的Web应用程序上下文可能尚未被{@linkplain ConfigurableApplicationContext#refresh() 刷新}。
	 *     如果<strong>尚未</strong>刷新（推荐方式），则将执行以下操作：
	 *     <ul>
	 *         <li>如果指定上下文尚未设置{@linkplain ConfigurableApplicationContext#setParent 父级上下文}，根应用程序上下文将被设置为父级</li>
	 *         <li>如果指定上下文尚未分配{@linkplain ConfigurableApplicationContext#setId ID}，系统将为其分配ID</li>
	 *         <li>{@code ServletContext}和{@code ServletConfig}对象将委托给应用程序上下文</li>
	 *         <li>将调用{@link #postProcessWebApplicationContext}方法</li>
	 *         <li>将通过"contextInitializerClasses"初始化参数或{@link #setContextInitializers}属性指定的所有{@code ApplicationContextInitializer}都将被应用</li>
	 *         <li>如果上下文实现了{@link ConfigurableApplicationContext}接口，将调用其{@link ConfigurableApplicationContext#refresh refresh()}方法</li>
	 *     </ul>
	 *     如果上下文已被刷新，则不会执行上述任何操作，系统将假定用户已根据实际需求执行（或未执行）这些操作。
	 * </p>
	 * <p>
	 *     使用示例请参阅{@link org.springframework.web.WebApplicationInitializer}。
	 * </p>
	 * @param webApplicationContext 要使用的上下文
	 * @see #initWebApplicationContext
	 * @see #configureAndRefreshWebApplicationContext
	 * @see org.springframework.web.WebApplicationInitializer
	 */
	public DispatcherServlet(WebApplicationContext webApplicationContext) {
		super(webApplicationContext);
		setDispatchOptionsRequest(true);
	}


	/**
	 * <p>设置是否检测此servlet上下文中的所有HandlerMapping bean。若关闭此选项，则只会查找名为"handlerMapping"的单个bean。</p>
	 * <p>默认值为"true"。如果希望此servlet使用单个HandlerMapping（即使上下文中定义了多个HandlerMapping bean），请关闭此选项。</p>
	 */
	public void setDetectAllHandlerMappings(boolean detectAllHandlerMappings) {
		this.detectAllHandlerMappings = detectAllHandlerMappings;
	}

	/**
	 * Set whether to detect all HandlerAdapter beans in this servlet's context. Otherwise,
	 * just a single bean with name "handlerAdapter" will be expected.
	 * <p>Default is "true". Turn this off if you want this servlet to use a single
	 * HandlerAdapter, despite multiple HandlerAdapter beans being defined in the context.
	 */
	public void setDetectAllHandlerAdapters(boolean detectAllHandlerAdapters) {
		this.detectAllHandlerAdapters = detectAllHandlerAdapters;
	}

	/**
	 * Set whether to detect all HandlerExceptionResolver beans in this servlet's context. Otherwise,
	 * just a single bean with name "handlerExceptionResolver" will be expected.
	 * <p>Default is "true". Turn this off if you want this servlet to use a single
	 * HandlerExceptionResolver, despite multiple HandlerExceptionResolver beans being defined in the context.
	 */
	public void setDetectAllHandlerExceptionResolvers(boolean detectAllHandlerExceptionResolvers) {
		this.detectAllHandlerExceptionResolvers = detectAllHandlerExceptionResolvers;
	}

	/**
	 * Set whether to detect all ViewResolver beans in this servlet's context. Otherwise,
	 * just a single bean with name "viewResolver" will be expected.
	 * <p>Default is "true". Turn this off if you want this servlet to use a single
	 * ViewResolver, despite multiple ViewResolver beans being defined in the context.
	 */
	public void setDetectAllViewResolvers(boolean detectAllViewResolvers) {
		this.detectAllViewResolvers = detectAllViewResolvers;
	}

	/**
	 * Set whether to throw a NoHandlerFoundException when no Handler was found for this request.
	 * This exception can then be caught with a HandlerExceptionResolver or an
	 * {@code @ExceptionHandler} controller method.
	 * <p>Note that if {@link org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler}
	 * is used, then requests will always be forwarded to the default servlet and a
	 * NoHandlerFoundException would never be thrown in that case.
	 * <p>Default is "false", meaning the DispatcherServlet sends a NOT_FOUND error through the
	 * Servlet response.
	 * @since 4.0
	 */
	public void setThrowExceptionIfNoHandlerFound(boolean throwExceptionIfNoHandlerFound) {
		this.throwExceptionIfNoHandlerFound = throwExceptionIfNoHandlerFound;
	}

	/**
	 * <p>设置是否在执行包含请求后清理请求属性，即是否在DispatcherServlet处理包含请求后重置所有请求属性的原始状态。若设置为false，则仅重置DispatcherServlet自身的请求属性，但不会重置JSP的模型属性或视图设置的特殊属性（例如JSTL属性）。
	 *
	 * <p>默认值为"true"（强烈建议保留）。视图不应依赖通过（动态）包含设置的请求属性。这使得由被包含控制器渲染的JSP视图可以使用任何模型属性（即使与主JSP中的属性同名），而不会产生副作用。仅在有特殊需求时关闭此功能，例如刻意允许主JSP访问由被包含控制器渲染的JSP视图中的属性。
	 */
	public void setCleanupAfterInclude(boolean cleanupAfterInclude) {
		this.cleanupAfterInclude = cleanupAfterInclude;
	}


	/**
	 * 当前实现类中，实现为调用 {@link #initStrategies}。
	 */
	@Override
	protected void onRefresh(ApplicationContext context) {
		initStrategies(context);
	}

	/**
	 * <p>初始化此 servlet 所使用的策略对象。</p>
	 * <p>在子类中可以对其进行重写以初始化更多的策略对象。</p>
	 */
	protected void initStrategies(ApplicationContext context) {
		initMultipartResolver(context);
		initLocaleResolver(context);
		initThemeResolver(context);
		initHandlerMappings(context);
		initHandlerAdapters(context);
		initHandlerExceptionResolvers(context);
		initRequestToViewNameTranslator(context);
		initViewResolvers(context);
		initFlashMapManager(context);
	}

	/**
	 * <p>初始化此类使用的MultipartResolver（多部分解析器）。</p>
	 * <p>如果在此命名空间的BeanFactory中未定义指定名称的bean，则不提供多部分处理功能。</p>
	 */
	private void initMultipartResolver(ApplicationContext context) {
		try {
			this.multipartResolver = context.getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using MultipartResolver [" + this.multipartResolver + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Default is no multipart resolver.
			this.multipartResolver = null;
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate MultipartResolver with name '" + MULTIPART_RESOLVER_BEAN_NAME +
						"': no multipart request handling provided");
			}
		}
	}

	/**
	 * <p>初始化此类使用的LocaleResolver（区域解析器）。</p>
	 * <p>如果在此命名空间的BeanFactory中未定义指定名称的bean，则默认使用AcceptHeaderLocaleResolver。</p>
	 */
	private void initLocaleResolver(ApplicationContext context) {
		try {
			this.localeResolver = context.getBean(LOCALE_RESOLVER_BEAN_NAME, LocaleResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using LocaleResolver [" + this.localeResolver + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// 我们需要使用的默认值。
			this.localeResolver = getDefaultStrategy(context, LocaleResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate LocaleResolver with name '" + LOCALE_RESOLVER_BEAN_NAME +
						"': using default [" + this.localeResolver + "]");
			}
		}
	}

	/**
	 * <p>初始化此类使用的ThemeResolver（主题解析器）。</p>
	 * <p>如果在此命名空间的BeanFactory中未定义指定名称的bean，则默认使用FixedThemeResolver。</p>
	 */
	private void initThemeResolver(ApplicationContext context) {
		try {
			this.themeResolver = context.getBean(THEME_RESOLVER_BEAN_NAME, ThemeResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using ThemeResolver [" + this.themeResolver + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// 我们需要使用的默认值。
			this.themeResolver = getDefaultStrategy(context, ThemeResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate ThemeResolver with name '" + THEME_RESOLVER_BEAN_NAME +
						"': using default [" + this.themeResolver + "]");
			}
		}
	}

	/**
	 * <p>初始化此类使用的HandlerMappings（处理器映射）。</p>
	 * <p>如果在此命名空间的BeanFactory中未定义任何HandlerMapping bean，则默认使用BeanNameUrlHandlerMapping。</p>
	 */
	private void initHandlerMappings(ApplicationContext context) {
		this.handlerMappings = null;

		if (this.detectAllHandlerMappings) {
			// 查找ApplicationContext中的所有HandlerMapping，包括祖先上下文。
			Map<String, HandlerMapping> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerMappings = new ArrayList<>(matchingBeans.values());
				// 我们将保持集合中，HandlerMapping的有序性。
				AnnotationAwareOrderComparator.sort(this.handlerMappings);
			}
		}
		else {
			try {
				HandlerMapping hm = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);
				this.handlerMappings = Collections.singletonList(hm);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// 忽略这个异常，稍后我们将添加一个默认的HandlerMapping。
			}
		}

		// 如果没有找到其他HandlerMapping，通过注册一个默认的HandlerMapping来确保我们至少有一个HandlerMapping。
		if (this.handlerMappings == null) {
			this.handlerMappings = getDefaultStrategies(context, HandlerMapping.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No HandlerMappings found in servlet '" + getServletName() + "': using default");
			}
		}
	}

	/**
	 * <p>初始化此类使用的HandlerAdapters（处理器适配器）。</p>
	 * <p>如果在此命名空间的BeanFactory中未定义任何HandlerAdapter bean，则默认使用SimpleControllerHandlerAdapter。</p>
	 */
	private void initHandlerAdapters(ApplicationContext context) {
		this.handlerAdapters = null;

		if (this.detectAllHandlerAdapters) {
			// 查找ApplicationContext中的所有HandlerAdapter，包括祖先上下文。
			Map<String, HandlerAdapter> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerAdapter.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerAdapters = new ArrayList<>(matchingBeans.values());
				// 我们将保持集合中，HandlerAdapter的有序性。
				AnnotationAwareOrderComparator.sort(this.handlerAdapters);
			}
		}
		else {
			try {
				HandlerAdapter ha = context.getBean(HANDLER_ADAPTER_BEAN_NAME, HandlerAdapter.class);
				this.handlerAdapters = Collections.singletonList(ha);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// 忽略这个异常，稍后我们将添加一个默认的HandlerAdapter。
			}
		}

		// 如果没有找到其他HandlerAdapter，通过注册一个默认的HandlerAdapter来确保我们至少有一个HandlerAdapter。
		if (this.handlerAdapters == null) {
			this.handlerAdapters = getDefaultStrategies(context, HandlerAdapter.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No HandlerAdapters found in servlet '" + getServletName() + "': using default");
			}
		}
	}

	/**
	 * <p>初始化此类使用的HandlerExceptionResolver（异常解析器）。</p>
	 * <p>如果在此命名空间的BeanFactory中未定义指定名称的bean，则不提供任何异常解析器。</p>
	 */
	private void initHandlerExceptionResolvers(ApplicationContext context) {
		this.handlerExceptionResolvers = null;

		if (this.detectAllHandlerExceptionResolvers) {
			// 查找ApplicationContext中的所有HandlerExceptionResolver，包括祖先上下文。
			Map<String, HandlerExceptionResolver> matchingBeans = BeanFactoryUtils
					.beansOfTypeIncludingAncestors(context, HandlerExceptionResolver.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerExceptionResolvers = new ArrayList<>(matchingBeans.values());
				// We keep HandlerExceptionResolvers in sorted order.
				AnnotationAwareOrderComparator.sort(this.handlerExceptionResolvers);
			}
		}
		else {
			try {
				HandlerExceptionResolver her =
						context.getBean(HANDLER_EXCEPTION_RESOLVER_BEAN_NAME, HandlerExceptionResolver.class);
				this.handlerExceptionResolvers = Collections.singletonList(her);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// 忽略这个异常，稍后我们将添加一个默认的HandlerExceptionResolver。
			}
		}

		// 如果没有找到其他HandlerExceptionResolver，通过注册一个默认的HandlerExceptionResolver来确保我们至少有一个HandlerExceptionResolver。
		if (this.handlerExceptionResolvers == null) {
			this.handlerExceptionResolvers = getDefaultStrategies(context, HandlerExceptionResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No HandlerExceptionResolvers found in servlet '" + getServletName() + "': using default");
			}
		}
	}

	/**
	 * <p>初始化此servlet实例使用的RequestToViewNameTranslator（请求到视图名转换器）。</p>
	 * <p>如果未配置任何实现，则默认使用DefaultRequestToViewNameTranslator。</p>
	 */
	private void initRequestToViewNameTranslator(ApplicationContext context) {
		try {
			this.viewNameTranslator =
					context.getBean(REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME, RequestToViewNameTranslator.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using RequestToViewNameTranslator [" + this.viewNameTranslator + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// 我们需要使用的默认值。
			this.viewNameTranslator = getDefaultStrategy(context, RequestToViewNameTranslator.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate RequestToViewNameTranslator with name '" +
						REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME + "': using default [" + this.viewNameTranslator +
						"]");
			}
		}
	}

	/**
	 * <p>初始化此类使用的ViewResolvers（视图解析器）。</p>
	 * <p>如果在此命名空间的BeanFactory中未定义任何ViewResolver bean，则默认使用InternalResourceViewResolver。</p>
	 */
	private void initViewResolvers(ApplicationContext context) {
		this.viewResolvers = null;

		if (this.detectAllViewResolvers) {
			// 查找ApplicationContext中的所有ViewResolver，包括祖先上下文。
			Map<String, ViewResolver> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, ViewResolver.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.viewResolvers = new ArrayList<>(matchingBeans.values());
				// We keep ViewResolvers in sorted order.
				AnnotationAwareOrderComparator.sort(this.viewResolvers);
			}
		}
		else {
			try {
				ViewResolver vr = context.getBean(VIEW_RESOLVER_BEAN_NAME, ViewResolver.class);
				this.viewResolvers = Collections.singletonList(vr);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// 忽略这个异常，稍后我们将添加一个默认的ViewResolver。
			}
		}

		// 如果没有找到其他ViewResolver，通过注册一个默认的ViewResolver来确保我们至少有一个ViewResolver。
		if (this.viewResolvers == null) {
			this.viewResolvers = getDefaultStrategies(context, ViewResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No ViewResolvers found in servlet '" + getServletName() + "': using default");
			}
		}
	}

	/**
	 * <p>初始化此servlet实例使用的{@link FlashMapManager}（FlashMap管理器）。</p>
	 * <p>如果未配置任何实现，则默认使用{@code org.springframework.web.servlet.support.DefaultFlashMapManager}。</p>
	 */
	private void initFlashMapManager(ApplicationContext context) {
		try {
			this.flashMapManager = context.getBean(FLASH_MAP_MANAGER_BEAN_NAME, FlashMapManager.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using FlashMapManager [" + this.flashMapManager + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// 我们需要使用的默认值。
			this.flashMapManager = getDefaultStrategy(context, FlashMapManager.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate FlashMapManager with name '" +
						FLASH_MAP_MANAGER_BEAN_NAME + "': using default [" + this.flashMapManager + "]");
			}
		}
	}

	/**
	 * Return this servlet's ThemeSource, if any; else return {@code null}.
	 * <p>Default is to return the WebApplicationContext as ThemeSource,
	 * provided that it implements the ThemeSource interface.
	 * @return the ThemeSource, if any
	 * @see #getWebApplicationContext()
	 */
	@Nullable
	public final ThemeSource getThemeSource() {
		return (getWebApplicationContext() instanceof ThemeSource ? (ThemeSource) getWebApplicationContext() : null);
	}

	/**
	 * <p>获取此servlet的MultipartResolver（如果存在）。</p>
	 * @return 此servlet使用的MultipartResolver，如果不存在则返回{@code null}（指示不提供多部分支持）
	 */
	@Nullable
	public final MultipartResolver getMultipartResolver() {
		return this.multipartResolver;
	}

	/**
	 * <p>返回通过类型在{@link WebApplicationContext}中检测到或基于{@literal DispatcherServlet.properties}默认策略集合初始化的已配置{@link HandlerMapping} bean。</p>
	 * <p><strong>注意：</strong>如果在{@link #onRefresh(ApplicationContext)}方法之前调用，此方法可能返回{@code null}。</p>
	 * @return 包含已配置映射的不可修改列表，若尚未初始化则返回{@code null}
	 * @since 5.0
	 */
	@Nullable
	public final List<HandlerMapping> getHandlerMappings() {
		return (this.handlerMappings != null ? Collections.unmodifiableList(this.handlerMappings) : null);
	}

	/**
	 * <p>返回指定策略接口的默认策略对象。</p>
	 * <p>默认实现委托给{@link #getDefaultStrategies}方法，并期望列表中包含单个对象。</p>
	 * @param context 当前的WebApplicationContext
	 * @param strategyInterface 策略接口
	 * @return 对应的策略对象
	 * @see #getDefaultStrategies
	 */
	protected <T> T getDefaultStrategy(ApplicationContext context, Class<T> strategyInterface) {
		List<T> strategies = getDefaultStrategies(context, strategyInterface);
		if (strategies.size() != 1) {
			throw new BeanInitializationException(
					"DispatcherServlet needs exactly 1 strategy for interface [" + strategyInterface.getName() + "]");
		}
		return strategies.get(0);
	}

	/**
	 * <p>为指定策略接口创建默认策略对象列表。</p>
	 * <p>默认实现使用与DispatcherServlet类同包下的"DispatcherServlet.properties"文件来确定类名，并通过上下文的BeanFactory实例化策略对象。</p>
	 * @param context 当前的WebApplicationContext
	 * @param strategyInterface 策略接口
	 * @return 对应的策略对象列表
	 */
	@SuppressWarnings("unchecked")
	protected <T> List<T> getDefaultStrategies(ApplicationContext context, Class<T> strategyInterface) {
		String key = strategyInterface.getName();
		String value = defaultStrategies.getProperty(key);
		if (value != null) {
			String[] classNames = StringUtils.commaDelimitedListToStringArray(value);
			List<T> strategies = new ArrayList<>(classNames.length);
			for (String className : classNames) {
				try {
					Class<?> clazz = ClassUtils.forName(className, DispatcherServlet.class.getClassLoader());
					Object strategy = createDefaultStrategy(context, clazz);
					strategies.add((T) strategy);
				}
				catch (ClassNotFoundException ex) {
					throw new BeanInitializationException(
							"Could not find DispatcherServlet's default strategy class [" + className +
							"] for interface [" + key + "]", ex);
				}
				catch (LinkageError err) {
					throw new BeanInitializationException(
							"Unresolvable class definition for DispatcherServlet's default strategy class [" +
							className + "] for interface [" + key + "]", err);
				}
			}
			return strategies;
		}
		else {
			return new LinkedList<>();
		}
	}

	/**
	 * <p>创建默认策略实例。</p>
	 * <p>默认实现使用{@link org.springframework.beans.factory.config.AutowireCapableBeanFactory#createBean}方法。</p>
	 * @param context 当前的WebApplicationContext
	 * @param clazz 需要实例化的策略实现类
	 * @return 完整配置的策略实例
	 * @see org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#createBean
	 */
	protected Object createDefaultStrategy(ApplicationContext context, Class<?> clazz) {
		return context.getAutowireCapableBeanFactory().createBean(clazz);
	}


	/**
	 * Exposes the DispatcherServlet-specific request attributes and delegates to {@link #doDispatch}
	 * for the actual dispatching.
	 */
	@Override
	protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (logger.isDebugEnabled()) {
			String resumed = WebAsyncUtils.getAsyncManager(request).hasConcurrentResult() ? " resumed" : "";
			logger.debug("DispatcherServlet with name '" + getServletName() + "'" + resumed +
					" processing " + request.getMethod() + " request for [" + getRequestUri(request) + "]");
		}

		// Keep a snapshot of the request attributes in case of an include,
		// to be able to restore the original attributes after the include.
		Map<String, Object> attributesSnapshot = null;
		if (WebUtils.isIncludeRequest(request)) {
			attributesSnapshot = new HashMap<>();
			Enumeration<?> attrNames = request.getAttributeNames();
			while (attrNames.hasMoreElements()) {
				String attrName = (String) attrNames.nextElement();
				if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
					attributesSnapshot.put(attrName, request.getAttribute(attrName));
				}
			}
		}

		// Make framework objects available to handlers and view objects.
		request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
		request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
		request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
		request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());

		if (this.flashMapManager != null) {
			FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
			if (inputFlashMap != null) {
				request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
			}
			request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
			request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);
		}

		try {
			doDispatch(request, response);
		}
		finally {
			if (!WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
				// Restore the original attribute snapshot, in case of an include.
				if (attributesSnapshot != null) {
					restoreAttributesAfterInclude(request, attributesSnapshot);
				}
			}
		}
	}

	/**
	 * Process the actual dispatching to the handler.
	 * <p>The handler will be obtained by applying the servlet's HandlerMappings in order.
	 * The HandlerAdapter will be obtained by querying the servlet's installed HandlerAdapters
	 * to find the first that supports the handler class.
	 * <p>All HTTP methods are handled by this method. It's up to HandlerAdapters or handlers
	 * themselves to decide which methods are acceptable.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception in case of any kind of processing failure
	 */
	protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpServletRequest processedRequest = request;
		HandlerExecutionChain mappedHandler = null;
		boolean multipartRequestParsed = false;

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

		try {
			ModelAndView mv = null;
			Exception dispatchException = null;

			try {
				processedRequest = checkMultipart(request);
				multipartRequestParsed = (processedRequest != request);

				// 确定当前请求的处理器
				mappedHandler = getHandler(processedRequest);
				if (mappedHandler == null) {
					noHandlerFound(processedRequest, response);
					return;
				}

				// 确定当前请求的处理器的适配器
				HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

				// 如果处理器支持，则处理Last-Modified头。
				String method = request.getMethod();
				boolean isGet = "GET".equals(method);
				if (isGet || "HEAD".equals(method)) {
					long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
					if (logger.isDebugEnabled()) {
						logger.debug("Last-Modified value for [" + getRequestUri(request) + "] is: " + lastModified);
					}
					if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
						return;
					}
				}

				if (!mappedHandler.applyPreHandle(processedRequest, response)) {
					return;
				}

				// 正真地调用处理器。
				mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

				if (asyncManager.isConcurrentHandlingStarted()) {
					return;
				}

				applyDefaultViewName(processedRequest, mv);
				mappedHandler.applyPostHandle(processedRequest, response, mv);
			}
			catch (Exception ex) {
				dispatchException = ex;
			}
			catch (Throwable err) {
				// As of 4.3, we're processing Errors thrown from handler methods as well,
				// making them available for @ExceptionHandler methods and other scenarios.
				dispatchException = new NestedServletException("Handler dispatch failed", err);
			}
			processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
		}
		catch (Exception ex) {
			triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
		}
		catch (Throwable err) {
			triggerAfterCompletion(processedRequest, response, mappedHandler,
					new NestedServletException("Handler processing failed", err));
		}
		finally {
			if (asyncManager.isConcurrentHandlingStarted()) {
				// Instead of postHandle and afterCompletion
				if (mappedHandler != null) {
					mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
				}
			}
			else {
				// Clean up any resources used by a multipart request.
				if (multipartRequestParsed) {
					cleanupMultipart(processedRequest);
				}
			}
		}
	}

	/**
	 * 是否需要进行 视图名称转换？
	 */
	private void applyDefaultViewName(HttpServletRequest request, @Nullable ModelAndView mv) throws Exception {
		if (mv != null && !mv.hasView()) {
			String defaultViewName = getDefaultViewName(request);
			if (defaultViewName != null) {
				mv.setViewName(defaultViewName);
			}
		}
	}

	/**
	 * 处理处理器选择及调用后的结果，该结果可能是需要被解析为ModelAndView对象的模型视图，也可能是需要被转换为ModelAndView的异常。
	 *
	 * Handle the result of handler selection and handler invocation, which is
	 * either a ModelAndView or an Exception to be resolved to a ModelAndView.
	 */
	private void processDispatchResult(HttpServletRequest request, HttpServletResponse response,
			@Nullable HandlerExecutionChain mappedHandler, @Nullable ModelAndView mv,
			@Nullable Exception exception) throws Exception {

		boolean errorView = false;

		if (exception != null) {
			if (exception instanceof ModelAndViewDefiningException) {
				logger.debug("ModelAndViewDefiningException encountered", exception);
				mv = ((ModelAndViewDefiningException) exception).getModelAndView();
			}
			else {
				Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
				mv = processHandlerException(request, response, handler, exception);
				errorView = (mv != null);
			}
		}

		// Did the handler return a view to render?
		if (mv != null && !mv.wasCleared()) {
			render(mv, request, response);
			if (errorView) {
				WebUtils.clearErrorRequestAttributes(request);
			}
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Null ModelAndView returned to DispatcherServlet with name '" + getServletName() +
						"': assuming HandlerAdapter completed request handling");
			}
		}

		if (WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
			// Concurrent handling started during a forward
			return;
		}

		if (mappedHandler != null) {
			mappedHandler.triggerAfterCompletion(request, response, null);
		}
	}

	/**
	 * Build a LocaleContext for the given request, exposing the request's primary locale as current locale.
	 * <p>The default implementation uses the dispatcher's LocaleResolver to obtain the current locale,
	 * which might change during a request.
	 * @param request current HTTP request
	 * @return the corresponding LocaleContext
	 */
	@Override
	protected LocaleContext buildLocaleContext(final HttpServletRequest request) {
		LocaleResolver lr = this.localeResolver;
		if (lr instanceof LocaleContextResolver) {
			return ((LocaleContextResolver) lr).resolveLocaleContext(request);
		}
		else {
			return () -> (lr != null ? lr.resolveLocale(request) : request.getLocale());
		}
	}

	/**
	 * <p>将请求转换为多部分请求，并使多部分解析器可用。</p>
	 * <p>如果未设置多部分解析器，则直接使用现有请求。</p>
	 * @param request 当前HTTP请求
	 * @return 处理后的请求（必要时使用多部分包装器）
	 * @see MultipartResolver#resolveMultipart
	 */
	protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
		// 有multipart resolver可用，并且请求是 multipart request。
		if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
			if (WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class) != null) {
				logger.debug("Request is already a MultipartHttpServletRequest - if not in a forward, " +
						"this typically results from an additional MultipartFilter in web.xml");
			}
			else if (hasMultipartException(request)) {
				logger.debug("Multipart resolution previously failed for current request - " +
						"skipping re-resolution for undisturbed error rendering");
			}
			else {
				try {
					return this.multipartResolver.resolveMultipart(request);
				}
				catch (MultipartException ex) {
					if (request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) != null) {
						logger.debug("Multipart resolution failed for error dispatch", ex);
						// Keep processing error dispatch with regular request handle below
					}
					else {
						throw ex;
					}
				}
			}
		}
		// 若前面的代码没有返回，则返回原始请求。
		return request;
	}

	/**
	 * 检查"javax.servlet.error.exception"属性中是否存在多部分异常。
	 */
	private boolean hasMultipartException(HttpServletRequest request) {
		Throwable error = (Throwable) request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE);
		while (error != null) {
			if (error instanceof MultipartException) {
				return true;
			}
			error = error.getCause();
		}
		return false;
	}

	/**
	 * Clean up any resources used by the given multipart request (if any).
	 * @param request current HTTP request
	 * @see MultipartResolver#cleanupMultipart
	 */
	protected void cleanupMultipart(HttpServletRequest request) {
		if (this.multipartResolver != null) {
			MultipartHttpServletRequest multipartRequest =
					WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
			if (multipartRequest != null) {
				this.multipartResolver.cleanupMultipart(multipartRequest);
			}
		}
	}

	/**
	 * <p>返回当前请求的HandlerExecutionChain。</p>
	 * <p>按顺序尝试所有处理器映射。</p>
	 * @param request 当前HTTP请求
	 * @return HandlerExecutionChain对象，如果找不到对应处理器则返回{@code null}
	 */
	@Nullable
	protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		if (this.handlerMappings != null) {
			for (HandlerMapping hm : this.handlerMappings) {
				if (logger.isTraceEnabled()) {
					logger.trace(
							"Testing handler map [" + hm + "] in DispatcherServlet with name '" + getServletName() + "'");
				}
				HandlerExecutionChain handler = hm.getHandler(request);
				if (handler != null) {
					return handler;
				}
			}
		}
		return null;
	}

	/**
	 * 未找到处理器 -> 设置相应的HTTP响应状态。
	 * @param request 当前HTTP请求
	 * @param response 当前HTTP响应
	 * @throws Exception 如果准备响应失败
	 */
	protected void noHandlerFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (pageNotFoundLogger.isWarnEnabled()) {
			pageNotFoundLogger.warn("No mapping found for HTTP request with URI [" + getRequestUri(request) +
					"] in DispatcherServlet with name '" + getServletName() + "'");
		}
		if (this.throwExceptionIfNoHandlerFound) {
			throw new NoHandlerFoundException(request.getMethod(), getRequestUri(request),
					new ServletServerHttpRequest(request).getHeaders());
		}
		else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	/**
	 * Return the HandlerAdapter for this handler object.
	 * @param handler the handler object to find an adapter for
	 * @throws ServletException if no HandlerAdapter can be found for the handler. This is a fatal error.
	 */
	protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
		if (this.handlerAdapters != null) {
			for (HandlerAdapter ha : this.handlerAdapters) {
				if (logger.isTraceEnabled()) {
					logger.trace("Testing handler adapter [" + ha + "]");
				}
				if (ha.supports(handler)) {
					return ha;
				}
			}
		}
		throw new ServletException("No adapter for handler [" + handler +
				"]: The DispatcherServlet configuration needs to include a HandlerAdapter that supports this handler");
	}

	/**
	 * Determine an error ModelAndView via the registered HandlerExceptionResolvers.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or {@code null} if none chosen at the time of the exception
	 * (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding ModelAndView to forward to
	 * @throws Exception if no error ModelAndView found
	 */
	@Nullable
	protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
			@Nullable Object handler, Exception ex) throws Exception {

		// Check registered HandlerExceptionResolvers...
		ModelAndView exMv = null;
		if (this.handlerExceptionResolvers != null) {
			for (HandlerExceptionResolver handlerExceptionResolver : this.handlerExceptionResolvers) {
				exMv = handlerExceptionResolver.resolveException(request, response, handler, ex);
				if (exMv != null) {
					break;
				}
			}
		}
		if (exMv != null) {
			if (exMv.isEmpty()) {
				request.setAttribute(EXCEPTION_ATTRIBUTE, ex);
				return null;
			}
			// We might still need view name translation for a plain error model...
			if (!exMv.hasView()) {
				String defaultViewName = getDefaultViewName(request);
				if (defaultViewName != null) {
					exMv.setViewName(defaultViewName);
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Handler execution resulted in exception - forwarding to resolved error view: " + exMv, ex);
			}
			WebUtils.exposeErrorRequestAttributes(request, ex, getServletName());
			return exMv;
		}

		throw ex;
	}

	/**
	 * Render the given ModelAndView.
	 * <p>This is the last stage in handling a request. It may involve resolving the view by name.
	 * @param mv the ModelAndView to render
	 * @param request current HTTP servlet request
	 * @param response current HTTP servlet response
	 * @throws ServletException if view is missing or cannot be resolved
	 * @throws Exception if there's a problem rendering the view
	 */
	protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// Determine locale for request and apply it to the response.
		Locale locale =
				(this.localeResolver != null ? this.localeResolver.resolveLocale(request) : request.getLocale());
		response.setLocale(locale);

		View view;
		String viewName = mv.getViewName();
		if (viewName != null) {
			// We need to resolve the view name.
			view = resolveViewName(viewName, mv.getModelInternal(), locale, request);
			if (view == null) {
				throw new ServletException("Could not resolve view with name '" + mv.getViewName() +
						"' in servlet with name '" + getServletName() + "'");
			}
		}
		else {
			// No need to lookup: the ModelAndView object contains the actual View object.
			view = mv.getView();
			if (view == null) {
				throw new ServletException("ModelAndView [" + mv + "] neither contains a view name nor a " +
						"View object in servlet with name '" + getServletName() + "'");
			}
		}

		// Delegate to the View object for rendering.
		if (logger.isDebugEnabled()) {
			logger.debug("Rendering view [" + view + "] in DispatcherServlet with name '" + getServletName() + "'");
		}
		try {
			if (mv.getStatus() != null) {
				response.setStatus(mv.getStatus().value());
			}
			view.render(mv.getModelInternal(), request, response);
		}
		catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Error rendering view [" + view + "] in DispatcherServlet with name '" +
						getServletName() + "'", ex);
			}
			throw ex;
		}
	}

	/**
	 * 将提供的请求转换为默认视图名称。
	 * @param request 当前HTTP servlet请求
	 * @return 视图名称（如果未找到默认值则返回{@code null}）
	 * @throws Exception 如果视图名称转换失败
	 */
	@Nullable
	protected String getDefaultViewName(HttpServletRequest request) throws Exception {
		return (this.viewNameTranslator != null ? this.viewNameTranslator.getViewName(request) : null);
	}

	/**
	 * Resolve the given view name into a View object (to be rendered).
	 * <p>The default implementations asks all ViewResolvers of this dispatcher.
	 * Can be overridden for custom resolution strategies, potentially based on
	 * specific model attributes or request parameters.
	 * @param viewName the name of the view to resolve
	 * @param model the model to be passed to the view
	 * @param locale the current locale
	 * @param request current HTTP servlet request
	 * @return the View object, or {@code null} if none found
	 * @throws Exception if the view cannot be resolved
	 * (typically in case of problems creating an actual View object)
	 * @see ViewResolver#resolveViewName
	 */
	@Nullable
	protected View resolveViewName(String viewName, @Nullable Map<String, Object> model,
			Locale locale, HttpServletRequest request) throws Exception {

		if (this.viewResolvers != null) {
			for (ViewResolver viewResolver : this.viewResolvers) {
				View view = viewResolver.resolveViewName(viewName, locale);
				if (view != null) {
					return view;
				}
			}
		}
		return null;
	}

	private void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response,
			@Nullable HandlerExecutionChain mappedHandler, Exception ex) throws Exception {

		if (mappedHandler != null) {
			mappedHandler.triggerAfterCompletion(request, response, ex);
		}
		throw ex;
	}

	/**
	 * Restore the request attributes after an include.
	 * @param request current HTTP request
	 * @param attributesSnapshot the snapshot of the request attributes before the include
	 */
	@SuppressWarnings("unchecked")
	private void restoreAttributesAfterInclude(HttpServletRequest request, Map<?, ?> attributesSnapshot) {
		// Need to copy into separate Collection here, to avoid side effects
		// on the Enumeration when removing attributes.
		Set<String> attrsToCheck = new HashSet<>();
		Enumeration<?> attrNames = request.getAttributeNames();
		while (attrNames.hasMoreElements()) {
			String attrName = (String) attrNames.nextElement();
			if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
				attrsToCheck.add(attrName);
			}
		}

		// Add attributes that may have been removed
		attrsToCheck.addAll((Set<String>) attributesSnapshot.keySet());

		// Iterate over the attributes to check, restoring the original value
		// or removing the attribute, respectively, if appropriate.
		for (String attrName : attrsToCheck) {
			Object attrValue = attributesSnapshot.get(attrName);
			if (attrValue == null) {
				request.removeAttribute(attrName);
			}
			else if (attrValue != request.getAttribute(attrName)) {
				request.setAttribute(attrName, attrValue);
			}
		}
	}

	private static String getRequestUri(HttpServletRequest request) {
		String uri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
		if (uri == null) {
			uri = request.getRequestURI();
		}
		return uri;
	}
/**
 * <p>定义了所有Servlet必须实现的方法。</p>
 * <p>Servlet是一种运行在Web服务器内的小型Java程序，能够接收并响应Web客户端的请求（通常通过超文本传输协议HTTP）。</p>
 * <p>
 *     要实现此接口，可以编写扩展<code>javax.servlet.GenericServlet</code>的通用Servlet，
 *     或扩展<code>javax.servlet.http.HttpServlet</code>的HTTP Servlet。
 * </p>
 * <p>
 *     此接口定义了初始化Servlet、处理请求以及从服务器移除Servlet的方法，这些称为生命周期方法，按以下顺序调用：
 *     <ol>
 *         <li>构建Servlet实例，随后通过<code>init</code>方法进行初始化；
 *         <li>处理客户端对<code>service</code>方法的所有调用；
 *         <li>Servlet停止服务后，通过<code>destroy</code>方法销毁实例，最终被垃圾回收并终结。
 *     </ol>
 * </p>
 * <p>
 *     除生命周期方法外，本接口还提供<code>getServletConfig</code>方法（用于获取启动信息）
 *     和 <code>getServletInfo</code>方法（用于返回Servlet基本信息，如作者、版本和版权声明）。
 * </p>
 * @author 多方贡献
 * @see GenericServlet
 * @see javax.servlet.http.HttpServlet
 */
}
