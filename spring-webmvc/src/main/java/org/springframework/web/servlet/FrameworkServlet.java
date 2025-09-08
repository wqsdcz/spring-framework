/*
 * Copyright 2002-2018 the original author or authors.
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
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SourceFilteringListener;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.ServletRequestHandledEvent;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.WebUtils;

/**
 * <p>Spring Web框架的基础Servlet。它提供了与Spring应用上下文的集成，采用基于JavaBean的整体解决方案。</p>
 * <p>
 *     该类提供以下功能：
 *     <ul>
 *         <li>
 *             管理Servlet的{@link org.springframework.web.context.WebApplicationContext WebApplicationContext}实例。
 *             Servlet的配置由其命名空间中的Bean决定。
 *         </li>
 *         <li>
 *             在请求处理过程中发布事件，无论请求是否成功处理。
 *         </li>
 *     </ul>
 * </p>
 * <p>
 *     子类必须实现{@link #doService}方法来处理请求。
 *     由于该类直接继承自{@link HttpServletBean}而不是HttpServlet，因此Bean属性会自动映射到其上。
 *     子类可以重写{@link #initFrameworkServlet()}方法以进行自定义初始化。
 * </p>
 * <p>
 *     在Servlet初始化参数级别检测"contextClass"参数，如果未找到，则回退到默认上下文类{@link org.springframework.web.context.support.XmlWebApplicationContext XmlWebApplicationContext}。
 *     请注意，对于默认的{@code FrameworkServlet}，自定义上下文类需要实现{@link org.springframework.web.context.ConfigurableWebApplicationContext ConfigurableWebApplicationContext} SPI。
 * </p>
 * <p>
 *     接受一个可选的"contextInitializerClasses" Servlet初始化参数，该参数指定一个或多个{@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer}类。
 *     受管理的Web应用上下文将委托给这些初始化器，从而允许进行额外的编程配置，例如针对{@linkplain org.springframework.context.ConfigurableApplicationContext#getEnvironment() 上下文环境}添加属性源或激活配置文件。
 *     另请参阅{@link org.springframework.web.context.ContextLoader}，它支持具有相同语义的"contextInitializerClasses"上下文参数，用于"根"Web应用上下文。
 * </p>
 * <p>
 *     将"contextConfigLocation" Servlet初始化参数传递给上下文实例，将其解析为可能由任意数量的逗号和空格分隔的多个文件路径，例如"test-servlet.xml, myServlet.xml"。
 *     如果未明确指定，上下文实现应该从Servlet的命名空间构建默认位置。
 * </p>
 * <p>
 *     注意：在有多个配置位置的情况下，后续的Bean定义将覆盖先前加载文件中定义的Bean定义，至少在使用Spring的默认ApplicationContext实现时是如此。
 *     这可以通过额外的XML文件有意覆盖某些Bean定义。
 * </p>
 * <p>
 *     默认命名空间为"'servlet-name'-servlet"，例如，对于Servlet名称"test"，命名空间为"test-servlet"（对于XmlWebApplicationContext，默认位置为"/WEB-INF/test-servlet.xml"）。
 *     命名空间也可以通过"namespace" Servlet初始化参数显式设置。
 * </p>
 * <p>
 *     从Spring 3.1开始，{@code FrameworkServlet}现在可以注入Web应用上下文，而不是在内部创建自己的上下文。
 *     这在支持Servlet实例编程注册的Servlet 3.0+环境中非常有用。详细信息请参阅{@link #FrameworkServlet(WebApplicationContext)} Javadoc。
 * </p>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @see #doService
 * @see #setContextClass
 * @see #setContextConfigLocation
 * @see #setContextInitializerClasses
 * @see #setNamespace
 */
@SuppressWarnings("serial")
public abstract class FrameworkServlet extends HttpServletBean implements ApplicationContextAware {

	/**
	 * WebApplicationContext 命名空间的后缀。
	 * 若此类的某个 Servlet 在上下文中被赋予名称 "test"，则该 Servlet 使用的命名空间将解析为 "test-servlet"。
	 */
	public static final String DEFAULT_NAMESPACE_SUFFIX = "-servlet";

	/**
	 * FrameworkServlet 的默认上下文类。
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 */
	public static final Class<?> DEFAULT_CONTEXT_CLASS = XmlWebApplicationContext.class;

	/**
	 * WebApplicationContext 在 ServletContext 属性中的前缀。完整属性名由该前缀与 Servlet 名称组合而成。
	 */
	public static final String SERVLET_CONTEXT_PREFIX = FrameworkServlet.class.getName() + ".CONTEXT.";

	/**
	 * 在单个初始化参数（init-param）字符串值中，任意数量的这些字符都被视为多值之间的分隔符。
	 */
	private static final String INIT_PARAM_DELIMITERS = ",; \t\n";


	/** 用于查找 WebApplicationContext 的 ServletContext 属性 */
	@Nullable
	private String contextAttribute;

	/** 需创建WebApplicationContext时，使用的实现类 */
	private Class<?> contextClass = DEFAULT_CONTEXT_CLASS;

	/** 需要分配WebApplicationContext ID时，分配的WebApplicationContext ID值  */
	@Nullable
	private String contextId;

	/** 这个servlet的名称空间 */
	@Nullable
	private String namespace;

	/** 显式指定的上下文配置的位置 */
	@Nullable
	private String contextConfigLocation;

	/** 待应用于上下文的实际 ApplicationContextInitializer 实例 */
	private final List<ApplicationContextInitializer<ConfigurableApplicationContext>> contextInitializers =
			new ArrayList<>();

	/** 通过初始化参数设置的逗号分隔 ApplicationContextInitializer 类名的列表 */
	@Nullable
	private String contextInitializerClasses;

	/** 是否将上下文作为ServletContext属性发布？ */
	private boolean publishContext = true;

	/** 是否在每个请求结束时发布ServletRequestHandledEvent事件？*/
	private boolean publishEvents = true;

	/** 是否将 LocaleContext 和 RequestAttributes 作为子线程的可继承属性公开？*/
	private boolean threadContextInheritable = false;

	/** 是否将HTTP OPTIONS请求分派给{@link #doService}方法处理？ */
	private boolean dispatchOptionsRequest = false;

	/** 是否将HTTP TRACE请求分派给{@link #doService}方法处理？*/
	private boolean dispatchTraceRequest = false;

	/** 这个servlet的 WebApplicationContext */
	@Nullable
	private WebApplicationContext webApplicationContext;

	/** WebApplicationContext 是否是通过 {@link #setApplicationContext} 方法注入的 */
	private boolean webApplicationContextInjected = false;

	/** 用于检测 onRefresh 方法是否已被调用的标志位 */
	private volatile boolean refreshEventReceived = false;

	/** 用于同步执行onRefresh方法的监视器 */
	private final Object onRefreshMonitor = new Object();


	/**
	 * <p>
	 *     创建一个新的 {@code FrameworkServlet} 实例，该实例将基于默认值及通过servlet初始化参数提供的配置值，创建其内部Web应用上下文。
	 *     通常用于Servlet 2.5或更早版本的环境，此类环境中servlet注册的唯一方式是通过 {@code web.xml} 配置文件，且要求使用无参构造函数。
	 * </p>
	 * <p>
	 *     调用{@link #setContextConfigLocation}方法（对应初始化参数'contextConfigLocation'）
	 *     可指定由 {@linkplain #DEFAULT_CONTEXT_CLASS 默认XmlWebApplicationContext} 加载的XML配置文件。
	 * </p>
	 * <p>
	 *     调用{@link #setContextClass}方法（对应初始化参数'contextClass'）将覆盖默认的{@code XmlWebApplicationContext}，
	 *     允许指定替代类（例如{@code AnnotationConfigWebApplicationContext}）。
	 * </p>
	 * <p>
	 *     调用{@link #setContextInitializerClasses}方法（对应初始化参数'contextInitializerClasses'）可指定在refresh()方法执行前，
	 *     应使用哪些{@link ApplicationContextInitializer}类来进一步配置内部应用上下文。
	 * </p>
	 * @see #FrameworkServlet(WebApplicationContext)
	 */
	public FrameworkServlet() {
	}

	/**
	 * <p>
	 *     创建一个具有指定Web应用上下文的新 {@code FrameworkServlet} 实例。
	 *     此构造函数在Servlet 3.0+环境中特别有用，因为该环境支持通过 {@link ServletContext#addServlet} API实现基于实例的servlet注册。
	 * </p>
	 *
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
	 *     给定的Web应用上下文可能尚未被 {@linkplain ConfigurableApplicationContext#refresh() 刷新}。
	 *     如果该上下文同时满足：(a) 是{@link ConfigurableWebApplicationContext}的实现类，
	 *     且(b) <strong>尚未</strong>被刷新（推荐方式），则将执行以下操作：
	 *     <ul>
	 *         <li>如果给定上下文尚未设置{@linkplain ConfigurableApplicationContext#setParent 父级上下文}，则将根应用上下文设置为其父级</li>
	 *         <li>如果给定上下文尚未分配{@linkplain ConfigurableApplicationContext#setId ID}，将为其分配一个ID</li>
	 *         <li>将{@code ServletContext}和{@code ServletConfig}对象委托给应用上下文</li>
	 *         <li>调用{@link #postProcessWebApplicationContext}方法</li>
	 *         <li>调用通过"contextInitializerClasses"初始化参数或{@link #setContextInitializers}属性指定的所有{@link ApplicationContextInitializer}</li>
	 *         <li>调用{@link ConfigurableApplicationContext#refresh refresh()}方法</li>
	 *     </ul>
	 *  如果上下文已被刷新或不实现{@code ConfigurableWebApplicationContext}接口，则不会执行上述任何操作，因为假定用户已根据特定需求执行（或未执行）这些操作。
	 * </p>
	 * <p>使用示例请参见{@link org.springframework.web.WebApplicationInitializer}。</p>
	 * @param webApplicationContext 要使用的上下文实例
	 * @see #initWebApplicationContext
	 * @see #configureAndRefreshWebApplicationContext
	 * @see org.springframework.web.WebApplicationInitializer
	 */
	public FrameworkServlet(WebApplicationContext webApplicationContext) {
		this.webApplicationContext = webApplicationContext;
	}


	/**
	 * 设置用于检索当前Servlet应使用的{@link WebApplicationContext}的ServletContext属性名称。
	 */
	public void setContextAttribute(@Nullable String contextAttribute) {
		this.contextAttribute = contextAttribute;
	}

	/**
	 * 返回用于检索当前Servlet应使用的{@link WebApplicationContext}的ServletContext属性名称。
	 */
	@Nullable
	public String getContextAttribute() {
		return this.contextAttribute;
	}

	/**
	 * <p>设置自定义上下文类。此类必须是{@link org.springframework.web.context.WebApplicationContext}类型。</p>
	 * <p>当使用默认的FrameworkServlet实现时，上下文类还必须实现{@link org.springframework.web.context.ConfigurableWebApplicationContext}接口。</p>
	 * @see #createWebApplicationContext
	 */
	public void setContextClass(Class<?> contextClass) {
		this.contextClass = contextClass;
	}

	/**
	 * 返回自定义的上下文的Class实例。
	 * Return the custom context class.
	 */
	public Class<?> getContextClass() {
		return this.contextClass;
	}

	/**
	 * 设置自定义的 WebApplicationContext 标识符，该标识符将作为底层BeanFactory的序列化ID使用。
	 */
	public void setContextId(@Nullable String contextId) {
		this.contextId = contextId;
	}

	/**
	 * 返回自定义的WebApplicationContext标识符（如果存在）。
	 */
	@Nullable
	public String getContextId() {
		return this.contextId;
	}

	/**
	 * 设置此Servlet的自定义命名空间，该命名空间将用于构建默认的上下文配置的位置。
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * 返回此Servlet的命名空间，若未设置自定义命名空间则回退到默认命名方案：例如名为"test"的Servlet将返回"test-servlet"。
	 */
	public String getNamespace() {
		return (this.namespace != null ? this.namespace : getServletName() + DEFAULT_NAMESPACE_SUFFIX);
	}

	/**
	 * Set the context config location explicitly, instead of relying on the default
	 * location built from the namespace. This location string can consist of
	 * multiple locations separated by any number of commas and spaces.
	 */
	public void setContextConfigLocation(@Nullable String contextConfigLocation) {
		this.contextConfigLocation = contextConfigLocation;
	}

	/**
	 * Return the explicit context config location, if any.
	 */
	@Nullable
	public String getContextConfigLocation() {
		return this.contextConfigLocation;
	}

	/**
	 * Specify which {@link ApplicationContextInitializer} instances should be used
	 * to initialize the application context used by this {@code FrameworkServlet}.
	 * @see #configureAndRefreshWebApplicationContext
	 * @see #applyInitializers
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
	 * Specify the set of fully-qualified {@link ApplicationContextInitializer} class
	 * names, per the optional "contextInitializerClasses" servlet init-param.
	 * @see #configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext)
	 * @see #applyInitializers(ConfigurableApplicationContext)
	 */
	public void setContextInitializerClasses(String contextInitializerClasses) {
		this.contextInitializerClasses = contextInitializerClasses;
	}

	/**
	 * Set whether to publish this servlet's context as a ServletContext attribute,
	 * available to all objects in the web container. Default is "true".
	 * <p>This is especially handy during testing, although it is debatable whether
	 * it's good practice to let other application objects access the context this way.
	 */
	public void setPublishContext(boolean publishContext) {
		this.publishContext = publishContext;
	}

	/**
	 * Set whether this servlet should publish a ServletRequestHandledEvent at the end
	 * of each request. Default is "true"; can be turned off for a slight performance
	 * improvement, provided that no ApplicationListeners rely on such events.
	 * @see org.springframework.web.context.support.ServletRequestHandledEvent
	 */
	public void setPublishEvents(boolean publishEvents) {
		this.publishEvents = publishEvents;
	}

	/**
	 * Set whether to expose the LocaleContext and RequestAttributes as inheritable
	 * for child threads (using an {@link java.lang.InheritableThreadLocal}).
	 * <p>Default is "false", to avoid side effects on spawned background threads.
	 * Switch this to "true" to enable inheritance for custom child threads which
	 * are spawned during request processing and only used for this request
	 * (that is, ending after their initial task, without reuse of the thread).
	 * <p><b>WARNING:</b> Do not use inheritance for child threads if you are
	 * accessing a thread pool which is configured to potentially add new threads
	 * on demand (e.g. a JDK {@link java.util.concurrent.ThreadPoolExecutor}),
	 * since this will expose the inherited context to such a pooled thread.
	 */
	public void setThreadContextInheritable(boolean threadContextInheritable) {
		this.threadContextInheritable = threadContextInheritable;
	}

	/**
	 * <p>设置是否应将HTTP OPTIONS请求分派至{@link #doService}方法处理。</p>
	 * <p>
	 *     {@code FrameworkServlet}中的默认值为"false"，即采用{@link javax.servlet.http.HttpServlet}的默认行为
	 *     （枚举所有标准HTTP请求方法作为OPTIONS请求的响应）。
	 *     但请注意，自4.3版本起，{@code DispatcherServlet}因其内置对OPTIONS请求的支持，默认将此属性设置为"true"。
	 * </p>
	 * <p>
	 *     若希望OPTIONS请求像其他HTTP请求一样经过常规分派链处理，请启用此标志。
	 *     这通常意味着控制器将接收这些请求，请确保相关端点确实能够处理OPTIONS请求。
	 * </p>
	 * <p>
	 *     请注意，若控制器未按OPTIONS响应要求设置'Allow'头部，在任何情况下都将采用HttpServlet的默认OPTIONS处理逻辑。
	 * </p>
	 */
	public void setDispatchOptionsRequest(boolean dispatchOptionsRequest) {
		this.dispatchOptionsRequest = dispatchOptionsRequest;
	}

	/**
	 * Set whether this servlet should dispatch an HTTP TRACE request to
	 * the {@link #doService} method.
	 * <p>Default is "false", applying {@link javax.servlet.http.HttpServlet}'s
	 * default behavior (i.e. reflecting the message received back to the client).
	 * <p>Turn this flag on if you prefer TRACE requests to go through the
	 * regular dispatching chain, just like other HTTP requests. This usually
	 * means that your controllers will receive those requests; make sure
	 * that those endpoints are actually able to handle a TRACE request.
	 * <p>Note that HttpServlet's default TRACE processing will be applied
	 * in any case if your controllers happen to not generate a response
	 * of content type 'message/http' (as required for a TRACE response).
	 */
	public void setDispatchTraceRequest(boolean dispatchTraceRequest) {
		this.dispatchTraceRequest = dispatchTraceRequest;
	}

	/**
	 * Called by Spring via {@link ApplicationContextAware} to inject the current
	 * application context. This method allows FrameworkServlets to be registered as
	 * Spring beans inside an existing {@link WebApplicationContext} rather than
	 * {@link #findWebApplicationContext() finding} a
	 * {@link org.springframework.web.context.ContextLoaderListener bootstrapped} context.
	 * <p>Primarily added to support use in embedded servlet containers.
	 * @since 4.0
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		if (this.webApplicationContext == null && applicationContext instanceof WebApplicationContext) {
			this.webApplicationContext = (WebApplicationContext) applicationContext;
			this.webApplicationContextInjected = true;
		}
	}


	/**
	 * {@link HttpServletBean}的重写方法，在设置所有Bean属性后调用。创建此servlet的WebApplicationContext。
	 */
	@Override
	protected final void initServletBean() throws ServletException {
		getServletContext().log("Initializing Spring FrameworkServlet '" + getServletName() + "'");
		if (logger.isInfoEnabled()) {
			logger.info("FrameworkServlet '" + getServletName() + "': initialization started");
		}
		long startTime = System.currentTimeMillis();

		try {
			this.webApplicationContext = initWebApplicationContext();
			initFrameworkServlet();
		}
		catch (ServletException | RuntimeException ex) {
			logger.error("Context initialization failed", ex);
			throw ex;
		}

		if (logger.isInfoEnabled()) {
			long elapsedTime = System.currentTimeMillis() - startTime;
			logger.info("FrameworkServlet '" + getServletName() + "': initialization completed in " +
					elapsedTime + " ms");
		}
	}

	/**
	 * <p>初始化并发布此Servlet的WebApplicationContext实例。</p>
	 * <p>实际创建过程委托给{@link #createWebApplicationContext}方法执行。子类可重写此方法。</p>
	 * @return WebApplicationContext实例
	 * @see #FrameworkServlet(WebApplicationContext)
	 * @see #setContextClass
	 * @see #setContextConfigLocation
	 */
	protected WebApplicationContext initWebApplicationContext() {
		WebApplicationContext rootContext =
				WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		WebApplicationContext wac = null;

		/** 在构造时，已经注入了 WebApplicationContext 实例 */
		if (this.webApplicationContext != null) {
			// A context instance was injected at construction time -> use it
			wac = this.webApplicationContext;
			if (wac instanceof ConfigurableWebApplicationContext) {
				ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
				if (!cwac.isActive()) {
					// The context has not yet been refreshed -> provide services such as
					// setting the parent context, setting the application context id, etc
					if (cwac.getParent() == null) {
						// The context instance was injected without an explicit parent -> set
						// the root application context (if any; may be null) as the parent
						cwac.setParent(rootContext);
					}
					configureAndRefreshWebApplicationContext(cwac);
				}
			}
		}
		/** 在构造时，未注入 WebApplicationContext 实例。从ServletContext中，查询 WebApplicationContext 实例*/
		if (wac == null) {

			/**
			 * 在构造时未注入上下文实例 -> 检查是否存在已注册到servlet上下文中的实例。
			 * 若存在，则假定父上下文（如有）已设置完成，且用户已执行诸如设置上下文ID等初始化操作。
			 */
			wac = findWebApplicationContext();
		}
		/** 在构造时，未注入 WebApplicationContext 实例。从ServletContext中，未查询到 WebApplicationContext 实例 */
		if (wac == null) {
			/** 此 servlet 没有定义任何上下文实例 -> 请创建一个本地实例 */
			wac = createWebApplicationContext(rootContext);
		}

		/** 如果 WebApplicationContext 没有刷新过，那么需要执行刷新操作 */
		if (!this.refreshEventReceived) {
			// Either the context is not a ConfigurableApplicationContext with refresh
			// support or the context injected at construction time had already been
			// refreshed -> trigger initial onRefresh manually here.
			synchronized (this.onRefreshMonitor) {
				onRefresh(wac);
			}
		}
		/** 将当前servlet的 WebApplicationContext 发布到 ServletContext 上 */
		if (this.publishContext) {
			// Publish the context as a servlet context attribute.
			String attrName = getServletContextAttributeName();
			getServletContext().setAttribute(attrName, wac);
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Published WebApplicationContext of servlet '" + getServletName() +
						"' as ServletContext attribute with name [" + attrName + "]");
			}
		}

		return wac;
	}

	/**
	 * <p>
	 *     从{@code ServletContext}的属性中获取指定名称（通过{@link #setContextAttribute}配置）的{@code WebApplicationContext}实例。
	 *     该{@code WebApplicationContext}必须在此Servlet初始化（或调用）之前已完成加载并存储在{@code ServletContext}中。
	 * </p>
	 * <p>子类可重写此方法以提供不同的{@code WebApplicationContext}检索策略。</p>
	 * @return 当前Servlet对应的WebApplicationContext，如果未找到则返回{@code null}
	 * @see #getContextAttribute()
	 */
	@Nullable
	protected WebApplicationContext findWebApplicationContext() {
		String attrName = getContextAttribute();
		if (attrName == null) {
			return null;
		}
		WebApplicationContext wac =
				WebApplicationContextUtils.getWebApplicationContext(getServletContext(), attrName);
		if (wac == null) {
			throw new IllegalStateException("No WebApplicationContext found: initializer not registered?");
		}
		return wac;
	}

	/**
	 * <p>
	 *     为此Servlet实例化WebApplicationContext，可选择默认的 {@link org.springframework.web.context.support.XmlWebApplicationContext}
	 *     或指定的{@link #setContextClass 自定义上下文类}（若已设置）。
	 * </p>
	 * <p>本实现要求自定义上下文需实现 {@link org.springframework.web.context.ConfigurableWebApplicationContext} 接口。子类可重写此方法。</p>
	 * <p>
	 *     务必在创建的上下文中将此Servlet实例注册为应用程序监听器（用于触发其{@link #onRefresh 回调}），
	 *     并在返回上下文实例前调用 {@link org.springframework.context.ConfigurableApplicationContext#refresh()}。
	 * </p>
	 * @param parent 要使用的父级ApplicationContext，若无则传入{@code null}
	 * @return 此Servlet的WebApplicationContext
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 */
	protected WebApplicationContext createWebApplicationContext(@Nullable ApplicationContext parent) {
		Class<?> contextClass = getContextClass();
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Servlet with name '" + getServletName() +
					"' will try to create custom WebApplicationContext context of class '" +
					contextClass.getName() + "'" + ", using parent context [" + parent + "]");
		}
		if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
			throw new ApplicationContextException(
					"Fatal initialization error in servlet with name '" + getServletName() +
					"': custom WebApplicationContext class [" + contextClass.getName() +
					"] is not of type ConfigurableWebApplicationContext");
		}
		ConfigurableWebApplicationContext wac =
				(ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);

		wac.setEnvironment(getEnvironment());
		wac.setParent(parent);
		String configLocation = getContextConfigLocation();
		if (configLocation != null) {
			wac.setConfigLocation(configLocation);
		}
		configureAndRefreshWebApplicationContext(wac);

		return wac;
	}

	protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
		if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
			// The application context id is still set to its original default value
			// -> assign a more useful id based on available information
			if (this.contextId != null) {
				wac.setId(this.contextId);
			}
			else {
				// Generate default id...
				wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
						ObjectUtils.getDisplayString(getServletContext().getContextPath()) + '/' + getServletName());
			}
		}

		wac.setServletContext(getServletContext());
		wac.setServletConfig(getServletConfig());
		wac.setNamespace(getNamespace());
		wac.addApplicationListener(new SourceFilteringListener(wac, new ContextRefreshListener()));

		// The wac environment's #initPropertySources will be called in any case when the context
		// is refreshed; do it eagerly here to ensure servlet property sources are in place for
		// use in any post-processing or initialization that occurs below prior to #refresh
		ConfigurableEnvironment env = wac.getEnvironment();
		if (env instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment) env).initPropertySources(getServletContext(), getServletConfig());
		}

		postProcessWebApplicationContext(wac);
		applyInitializers(wac);
		wac.refresh();
	}

	/**
	 * <p>
	 *     为此Servlet实例化WebApplicationContext，可选择默认的 {@link org.springframework.web.context.support.XmlWebApplicationContext}
	 *     或指定的{@link #setContextClass 自定义上下文类}（若已设置）。
	 * </p>
	 * <p>委托给 #createWebApplicationContext(ApplicationContext) 方法处理。</p>
	 * @param parent 要使用的父级WebApplicationContext，若无则传入{@code null}
	 * @return 此Servlet的WebApplicationContext
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 * @see #createWebApplicationContext(ApplicationContext)
	 */
	protected WebApplicationContext createWebApplicationContext(@Nullable WebApplicationContext parent) {
		return createWebApplicationContext((ApplicationContext) parent);
	}

	/**
	 * <p>在给定的WebApplicationContext被刷新并激活为此Servlet的上下文之前，对其进行后处理。
	 * <p>默认实现为空方法。本方法返回后将自动调用{@code refresh()}。
	 * <p>请注意，此方法专为允许子类修改应用上下文而设计，而{@link #initWebApplicationContext}方法则用于允许最终用户通过{@link ApplicationContextInitializer}实例来修改上下文。
	 * @param wac 已配置但尚未刷新的WebApplicationContext
	 * @see #createWebApplicationContext
	 * @see #initWebApplicationContext
	 * @see ConfigurableWebApplicationContext#refresh()
	 */
	protected void postProcessWebApplicationContext(ConfigurableWebApplicationContext wac) {
	}

	/**
	 * 在WebApplicationContext刷新前，将其委托给通过servlet初始化参数"contextInitializerClasses"指定的所有{@link ApplicationContextInitializer}实例进行处理。
	 * <p>另请参阅{@link #postProcessWebApplicationContext}方法，该方法专为允许子类（与最终用户相对）修改应用上下文而设计，并在本方法调用前立即执行。
	 * @param wac 已配置但尚未刷新的WebApplicationContext
	 * @see #createWebApplicationContext;
	 * @see #postProcessWebApplicationContext;
	 * @see ConfigurableApplicationContext#refresh()
	 */
	protected void applyInitializers(ConfigurableApplicationContext wac) {
		String globalClassNames = getServletContext().getInitParameter(ContextLoader.GLOBAL_INITIALIZER_CLASSES_PARAM);
		if (globalClassNames != null) {
			for (String className : StringUtils.tokenizeToStringArray(globalClassNames, INIT_PARAM_DELIMITERS)) {
				this.contextInitializers.add(loadInitializer(className, wac));
			}
		}

		if (this.contextInitializerClasses != null) {
			for (String className : StringUtils.tokenizeToStringArray(this.contextInitializerClasses, INIT_PARAM_DELIMITERS)) {
				this.contextInitializers.add(loadInitializer(className, wac));
			}
		}

		AnnotationAwareOrderComparator.sort(this.contextInitializers);
		for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : this.contextInitializers) {
			initializer.initialize(wac);
		}
	}

	@SuppressWarnings("unchecked")
	private ApplicationContextInitializer<ConfigurableApplicationContext> loadInitializer(
			String className, ConfigurableApplicationContext wac) {
		try {
			Class<?> initializerClass = ClassUtils.forName(className, wac.getClassLoader());
			Class<?> initializerContextClass =
					GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
			if (initializerContextClass != null && !initializerContextClass.isInstance(wac)) {
				throw new ApplicationContextException(String.format(
						"Could not apply context initializer [%s] since its generic parameter [%s] " +
						"is not assignable from the type of application context used by this " +
						"framework servlet: [%s]", initializerClass.getName(), initializerContextClass.getName(),
						wac.getClass().getName()));
			}
			return BeanUtils.instantiateClass(initializerClass, ApplicationContextInitializer.class);
		}
		catch (ClassNotFoundException ex) {
			throw new ApplicationContextException(String.format("Could not load class [%s] specified " +
					"via 'contextInitializerClasses' init-param", className), ex);
		}
	}

	/**
	 * 返回此Servlet的WebApplicationContext在ServletContext中的属性名。
	 * <p>默认实现返回 {@code SERVLET_CONTEXT_PREFIX + servlet名称}。
	 * @see #SERVLET_CONTEXT_PREFIX
	 * @see #getServletName
	 */
	public String getServletContextAttributeName() {
		return SERVLET_CONTEXT_PREFIX + getServletName();
	}

	/**
	 * 返回这个 servlet 的 WebApplicationContext。
	 */
	@Nullable
	public final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}


	/**
	 * 在所有Bean属性设置完毕且WebApplicationContext加载完成后，此方法将被调用。
	 * 默认实现为空方法；子类可重写此方法以执行所需的初始化操作。
	 * @throws ServletException 初始化过程中发生异常时抛出
	 */
	protected void initFrameworkServlet() throws ServletException {
	}

	/**
	 * 刷新此 servlet 的应用程序上下文，以及该 servlet 的相关状态。
	 * @see #getWebApplicationContext()
	 * @see org.springframework.context.ConfigurableApplicationContext#refresh()
	 */
	public void refresh() {
		WebApplicationContext wac = getWebApplicationContext();
		if (!(wac instanceof ConfigurableApplicationContext)) {
			throw new IllegalStateException("这个 WebApplicationContext 不支持刷新: " + wac);
		}
		((ConfigurableApplicationContext) wac).refresh();
	}

	/**
	 * 用于接收来自此servlet的WebApplicationContext刷新事件的回调方法。
	 * <p>默认实现会调用{@link #onRefresh}方法，触发对此servlet中依赖上下文的状态的刷新。
	 * @param event 传入的ApplicationContext事件
	 */
	public void onApplicationEvent(ContextRefreshedEvent event) {
		this.refreshEventReceived = true;
		synchronized (this.onRefreshMonitor) {
			onRefresh(event.getApplicationContext());
		}
	}

	/**
	 * <p>这是一个可被重写的模板方法，用以添加特定servlet的刷新工作。
	 * <p>在上下文成功刷新后，被调用，用以刷新servlet。
	 * <p>此实现为空方法。
	 * @param context 当前的WebApplicationContext
	 * @see #refresh()
	 */
	protected void onRefresh(ApplicationContext context) {
		// For subclasses: do nothing by default.
	}

	/**
	 * <p>关闭这个 servlet 的 WebApplicationContext。</p>
	 * @see org.springframework.context.ConfigurableApplicationContext#close()
	 */
	@Override
	public void destroy() {
		getServletContext().log("Destroying Spring FrameworkServlet '" + getServletName() + "'");
		// Only call close() on WebApplicationContext if locally managed...
		if (this.webApplicationContext instanceof ConfigurableApplicationContext && !this.webApplicationContextInjected) {
			((ConfigurableApplicationContext) this.webApplicationContext).close();
		}
	}


	/**
	 * 重写父类实现以达到拦截PATCH请求的目的。
	 */
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		HttpMethod httpMethod = HttpMethod.resolve(request.getMethod());
		if (httpMethod == HttpMethod.PATCH || httpMethod == null) {
			processRequest(request, response);
		}
		else {
			super.service(request, response);
		}
	}

	/**
	 * <p>将GET请求委托给processRequest/doService处理。</p>
	 * <p>该方法也会被HttpServlet默认实现的{@code doHead}方法调用，调用时会使用仅捕获内容长度的{@code NoBodyResponse}。 </p>
	 * @see #doService
	 * @see #doHead
	 */
	@Override
	protected final void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * <p>将POST请求委托给{@link #processRequest}方法处理。</p>
	 * @see #doService
	 */
	@Override
	protected final void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * <p>将PUT请求委托给{@link #processRequest}方法处理。</p>
	 * @see #doService
	 */
	@Override
	protected final void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * <p>将DELETE请求委托给{@link #processRequest}方法处理。</p>
	 * @see #doService
	 */
	@Override
	protected final void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * <p>根据需要将OPTIONS请求委托给{@link #processRequest}方法处理。</p>
	 * <p>否则应用HttpServlet的标准OPTIONS处理流程，且在调度后若仍未设置'Allow'头部时同样采用该标准处理。</p>
	 * @see #doService
	 */
	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (this.dispatchOptionsRequest || CorsUtils.isPreFlightRequest(request)) {
			processRequest(request, response);
			if (response.containsHeader("Allow")) {
				// Proper OPTIONS response coming from a handler - we're done.
				return;
			}
		}

		// Use response wrapper in order to always add PATCH to the allowed methods
		super.doOptions(request, new HttpServletResponseWrapper(response) {
			@Override
			public void setHeader(String name, String value) {
				if ("Allow".equals(name)) {
					value = (StringUtils.hasLength(value) ? value + ", " : "") + HttpMethod.PATCH.name();
				}
				super.setHeader(name, value);
			}
		});
	}

	/**
	 * <p>根据需要将TRACE请求委托给{@link #processRequest}方法处理。</p>
	 * <p>否则应用HttpServlet的标准TRACE处理流程。</p>
	 * @see #doService
	 */
	@Override
	protected void doTrace(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (this.dispatchTraceRequest) {
			processRequest(request, response);
			if ("message/http".equals(response.getContentType())) {
				// Proper TRACE response coming from a handler - we're done.
				return;
			}
		}
		super.doTrace(request, response);
	}

	/**
	 * 处理此请求，无论执行结果如何都会发布事件。
	 *
	 * <p>具体的事件处理由抽象的{@link #doService}模板方法执行。
	 * Process this request, publishing an event regardless of the outcome.
	 * <p>The actual event handling is performed by the abstract
	 * {@link #doService} template method.
	 */
	protected final void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		long startTime = System.currentTimeMillis();
		Throwable failureCause = null;

		LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
		LocaleContext localeContext = buildLocaleContext(request);

		RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
		ServletRequestAttributes requestAttributes = buildRequestAttributes(request, response, previousAttributes);

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		asyncManager.registerCallableInterceptor(FrameworkServlet.class.getName(), new RequestBindingInterceptor());

		initContextHolders(request, localeContext, requestAttributes);

		try {
			doService(request, response);
		}
		catch (ServletException | IOException ex) {
			failureCause = ex;
			throw ex;
		}
		catch (Throwable ex) {
			failureCause = ex;
			throw new NestedServletException("Request processing failed", ex);
		}

		finally {
			resetContextHolders(request, previousLocaleContext, previousAttributes);
			if (requestAttributes != null) {
				requestAttributes.requestCompleted();
			}

			if (logger.isDebugEnabled()) {
				if (failureCause != null) {
					this.logger.debug("Could not complete request", failureCause);
				}
				else {
					if (asyncManager.isConcurrentHandlingStarted()) {
						logger.debug("Leaving response open for concurrent processing");
					}
					else {
						this.logger.debug("Successfully completed request");
					}
				}
			}

			publishRequestHandledEvent(request, response, startTime, failureCause);
		}
	}

	/**
	 * 为给定请求构建LocaleContext，将请求的主区域设置作为当前区域设置暴露。
	 * @param request 当前HTTP请求
	 * @return 对应的LocaleContext，如果无需绑定则返回{@code null}
	 * @see LocaleContextHolder#setLocaleContext
	 */
	@Nullable
	protected LocaleContext buildLocaleContext(HttpServletRequest request) {
		return new SimpleLocaleContext(request.getLocale());
	}

	/**
	 * 为给定请求构建ServletRequestAttributes（可能同时包含对响应的引用），并考虑预绑定的属性（及其类型）。
	 * @param request 当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param previousAttributes 预绑定的RequestAttributes实例（如果存在）
	 * @return 要绑定的ServletRequestAttributes，返回{@code null}则保留先前绑定的实例（如果之前未绑定任何实例则不进行绑定）
	 * @see RequestContextHolder#setRequestAttributes
	 */
	@Nullable
	protected ServletRequestAttributes buildRequestAttributes(HttpServletRequest request,
			@Nullable HttpServletResponse response, @Nullable RequestAttributes previousAttributes) {

		if (previousAttributes == null || previousAttributes instanceof ServletRequestAttributes) {
			return new ServletRequestAttributes(request, response);
		}
		else {
			return null;  // preserve the pre-bound RequestAttributes instance
		}
	}

	private void initContextHolders(HttpServletRequest request,
			@Nullable LocaleContext localeContext, @Nullable RequestAttributes requestAttributes) {

		if (localeContext != null) {
			LocaleContextHolder.setLocaleContext(localeContext, this.threadContextInheritable);
		}
		if (requestAttributes != null) {
			RequestContextHolder.setRequestAttributes(requestAttributes, this.threadContextInheritable);
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Bound request context to thread: " + request);
		}
	}

	private void resetContextHolders(HttpServletRequest request,
			@Nullable LocaleContext prevLocaleContext, @Nullable RequestAttributes previousAttributes) {

		LocaleContextHolder.setLocaleContext(prevLocaleContext, this.threadContextInheritable);
		RequestContextHolder.setRequestAttributes(previousAttributes, this.threadContextInheritable);
		if (logger.isTraceEnabled()) {
			logger.trace("Cleared thread-bound request context: " + request);
		}
	}

	private void publishRequestHandledEvent(HttpServletRequest request, HttpServletResponse response,
			long startTime, @Nullable Throwable failureCause) {

		if (this.publishEvents && this.webApplicationContext != null) {
			// Whether or not we succeeded, publish an event.
			long processingTime = System.currentTimeMillis() - startTime;
			this.webApplicationContext.publishEvent(
					new ServletRequestHandledEvent(this,
							request.getRequestURI(), request.getRemoteAddr(),
							request.getMethod(), getServletConfig().getServletName(),
							WebUtils.getSessionId(request), getUsernameForRequest(request),
							processingTime, failureCause, response.getStatus()));
		}
	}

	/**
	 * <p>确定给定请求的用户名。
	 * <p>默认实现会获取UserPrincipal的名称（如果存在）。子类可以重写此方法。
	 * @param request 当前HTTP请求
	 * @return 用户名，如果未找到则返回{@code null}
	 * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
	 */
	@Nullable
	protected String getUsernameForRequest(HttpServletRequest request) {
		Principal userPrincipal = request.getUserPrincipal();
		return (userPrincipal != null ? userPrincipal.getName() : null);
	}


	/**
	 * <p>子类必须实现此方法以处理请求工作，并接收针对GET、POST、PUT和DELETE的集中回调。</p>
	 * <p>该约定本质上与通常被重写的HttpServlet的{@code doGet}或{@code doPost}方法相同。</p>
	 * <p>此类会对调用进行拦截以确保异常处理和事件发布得以执行。</p>
	 * @param request 当前HTTP请求
	 * @param response 当前HTTP响应
	 * @throws Exception 处理失败时抛出
	 * @see javax.servlet.http.HttpServlet#doGet @see javax.servlet.http.HttpServlet#doPost
	 */
	protected abstract void doService(HttpServletRequest request, HttpServletResponse response)
			throws Exception;


	/**
	 * ApplicationListener endpoint that receives events from this servlet's WebApplicationContext
	 * only, delegating to {@code onApplicationEvent} on the FrameworkServlet instance.
	 */
	private class ContextRefreshListener implements ApplicationListener<ContextRefreshedEvent> {

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			FrameworkServlet.this.onApplicationEvent(event);
		}
	}


	/**
	 * CallableProcessingInterceptor implementation that initializes and resets
	 * FrameworkServlet's context holders, i.e. LocaleContextHolder and RequestContextHolder.
	 */
	private class RequestBindingInterceptor implements CallableProcessingInterceptor {

		@Override
		public <T> void preProcess(NativeWebRequest webRequest, Callable<T> task) {
			HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
			if (request != null) {
				HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
				initContextHolders(request, buildLocaleContext(request),
						buildRequestAttributes(request, response, null));
			}
		}
		@Override
		public <T> void postProcess(NativeWebRequest webRequest, Callable<T> task, Object concurrentResult) {
			HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
			if (request != null) {
				resetContextHolders(request, null, null);
			}
		}
	}

}
