/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@link org.springframework.web.servlet.HandlerMapping}实现的抽象基类。
 * 支持排序、默认处理器、处理器拦截器，包括按路径模式映射的处理器拦截器。
 *
 * <p>
 *     注意：此基类<i>不</i>支持{@link #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE}属性的暴露。
 *     对该属性的支持由具体子类实现，通常基于请求URL映射。
 *
 * 笔记部分：
 * 1、可以访问到ApplicationContext和ServletContext，因为间接实现了ApplicationContextAware、ServletContextAware 两个接口
 * 2、ApplicationContextAware、ServletContextAware 两个接口，引出了两个初始化方法。
 * 3、多个AbstractHandlerMapping子类对象之间是有序的，因为实现了Ordered接口
 * 4、定义了获取handler的基本流程，如下：
 *   1) 获取抽象层面的handler。如果handler没有获取到，则获取默认handler。如果还是没有获取到，则返回null。
 *   2) 如果获取到了抽象层面的handler，则判断handler是否是String。
 *      如果handler是String，则将其作为beanName从ApplicationContext获取bean对象作为handler。
 *   3) 在确保了handler是非String的对象后，会判断handler是否是HandlerExecutionChain对象。
 *      如果是，则直接将handler视为当前请求的HandlerExecutionChain对象。
 *      如果不是，则使用handler为参数，创建一个新的HandlerExecutionChain对象。
 *   4) 当请求有了自己的HandlerExecutionChain对象后，就可以对其进行初始化了。
 *      遍历AbstractHandlerMapping中的拦截器集合，将拦截器添加到HandlerExecutionChain对象中，构成真正意义上的Chain。
 *      - 非MappedInterceptor类型：一定会添加到HandlerExecutionChain对象中；
 *      -   MappedInterceptor类型：如果MappedInterceptor对象的url模式和请求的lookupPath匹配，才被添加到HandlerExecutionChain对象中；
 *   5) 经过上述步骤HandlerExecutionChain对象已经构建完成了。但是对于CORS请求来说还需要做一些额外的处理。
 *   6) 如果是CORS请求（请求头Origin有值），则合并全局、局部的Cors配置；根据CORS请求的类型进行如下处理：
 *      - CORS实际请求：基于CORS配置创建一个CorsInterceptor对象，并添加到HandlerExecutionChain对象中。
 *   	- CORS预检请求：基于CORS配置创建一个PreFlightHandler对象(预检请求的处理器)，在与原有拦截器集合一起作为参数，
 *                     重新创建一个新的HandlerExecutionChain对象，替换原有的HandlerExecutionChain对象。
 *
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 07.04.2003
 * @see #getHandlerInternal
 * @see #setDefaultHandler
 * @see #setAlwaysUseFullPath
 * @see #setUrlDecode
 * @see org.springframework.util.AntPathMatcher
 * @see #setInterceptors
 * @see org.springframework.web.servlet.HandlerInterceptor
 */
public abstract class AbstractHandlerMapping extends WebApplicationObjectSupport implements HandlerMapping, Ordered {

	@Nullable
	private Object defaultHandler;

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private PathMatcher pathMatcher = new AntPathMatcher();

	private final List<Object> interceptors = new ArrayList<>();

	private final List<HandlerInterceptor> adaptedInterceptors = new ArrayList<>();

	private final UrlBasedCorsConfigurationSource globalCorsConfigSource = new UrlBasedCorsConfigurationSource();

	private CorsProcessor corsProcessor = new DefaultCorsProcessor();

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered


	/**
	 * 设置此处理器映射的默认处理器。
	 * 如果未找到特定映射，则将返回此处理器。
	 * <p>默认为 {@code null}，表示没有默认处理器。
	 */
	public void setDefaultHandler(@Nullable Object defaultHandler) {
		this.defaultHandler = defaultHandler;
	}

	/**
	 * 返回此处理器映射的默认处理器，如果不存在则返回 {@code null}。
	 */
	@Nullable
	public Object getDefaultHandler() {
		return this.defaultHandler;
	}

	/**
	 * 底层 {@link #setUrlPathHelper UrlPathHelper} 相同属性的快捷设置方式。
	 * @see org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath(boolean)
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
		this.globalCorsConfigSource.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * 底层 {@link #setUrlPathHelper UrlPathHelper} 相同属性的快捷设置方式。
	 * @see org.springframework.web.util.UrlPathHelper#setUrlDecode(boolean)
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
		this.globalCorsConfigSource.setUrlDecode(urlDecode);
	}

	/**
	 * 底层 {@link #setUrlPathHelper UrlPathHelper} 相同属性的快捷设置方式。
	 * @see org.springframework.web.util.UrlPathHelper#setRemoveSemicolonContent(boolean)
	 */
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
		this.globalCorsConfigSource.setRemoveSemicolonContent(removeSemicolonContent);
	}

	/**
	 * 设置用于解析查找路径的UrlPathHelper。
	 * <p>可通过此方法使用自定义子类覆盖默认的UrlPathHelper，或在多个HandlerMappings和MethodNameResolvers之间共享通用的UrlPathHelper设置。
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
		this.globalCorsConfigSource.setUrlPathHelper(urlPathHelper);
	}

	/**
	 * 返回用于解析查找路径的UrlPathHelper实现。
	 */
	public UrlPathHelper getUrlPathHelper() {
		return urlPathHelper;
	}

	/**
	 * 设置用于将URL路径与注册的URL模式进行匹配的PathMatcher实现。默认为AntPathMatcher。
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
		this.globalCorsConfigSource.setPathMatcher(pathMatcher);
	}

	/**
	 * 返回用于将URL路径与注册的URL模式进行匹配的PathMatcher实现。
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 设置应用于通过此处理器映射的所有处理器的拦截器。
	 * <p>
	 *     支持的拦截器类型包括HandlerInterceptor、WebRequestInterceptor和MappedInterceptor。
	 *     映射的拦截器仅适用于与其路径模式匹配的请求URL。
	 *     映射的拦截器bean在初始化过程中也会通过类型检测被自动识别。
	 *
	 * @param interceptors 处理器拦截器数组
	 * @see #adaptInterceptor
	 * @see org.springframework.web.servlet.HandlerInterceptor
	 * @see org.springframework.web.context.request.WebRequestInterceptor
	 */
	public void setInterceptors(Object... interceptors) {
		this.interceptors.addAll(Arrays.asList(interceptors));
	}

	/**
	 * 设置基于URL模式的“全局”CORS配置。
	 * 默认情况下，第一个匹配的URL模式会与处理器的CORS配置（如果存在）结合使用。
	 * @since 4.2
	 */
	public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
		this.globalCorsConfigSource.setCorsConfigurations(corsConfigurations);
	}

	/**
	 * 获取“全局”CORS配置。
	 */
	public Map<String, CorsConfiguration> getCorsConfigurations() {
		return this.globalCorsConfigSource.getCorsConfigurations();
	}

	/**
	 * 配置用于对请求应用匹配的{@link CorsConfiguration}的自定义{@link CorsProcessor}。
	 * <p>默认使用{@link DefaultCorsProcessor}。
	 * @since 4.2
	 */
	public void setCorsProcessor(CorsProcessor corsProcessor) {
		Assert.notNull(corsProcessor, "CorsProcessor must not be null");
		this.corsProcessor = corsProcessor;
	}

	/**
	 * 返回已配置的{@link CorsProcessor}。
	 */
	public CorsProcessor getCorsProcessor() {
		return this.corsProcessor;
	}

	/**
	 * 指定此HandlerMapping bean的顺序值。
	 * <p>默认值为{@code Ordered.LOWEST_PRECEDENCE}，表示无序。
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	/**
	 * 初始化拦截器。
	 * @see #extendInterceptors(java.util.List)
	 * @see #initInterceptors()
	 */
	@Override
	protected void initApplicationContext() throws BeansException {
		extendInterceptors(this.interceptors);
		detectMappedInterceptors(this.adaptedInterceptors);
		initInterceptors();
	}

	/**
	 * 子类可以重写的扩展钩子，用于在已配置的拦截器基础上注册额外的拦截器(参见 {@link #setInterceptors})。
	 * <p>
	 *     将在 {@link #initInterceptors()} 将指定拦截器适配为 {@link HandlerInterceptor} 实例之前调用。
	 * <p>默认实现为空。
	 * @param interceptors 已配置的拦截器列表（永远不会为 {@code null}），允许在现有拦截器的前后添加更多拦截器
	 */
	protected void extendInterceptors(List<Object> interceptors) {
	}

	/**
	 * 检测类型为 {@link MappedInterceptor} 的bean并将其添加到映射拦截器列表中。
	 * <p>
	 *     除了可能通过 {@link #setInterceptors} 提供的 {@link MappedInterceptor} 外，
	 *     还会调用此方法，默认情况下会从当前上下文及其祖先上下文添加所有类型为{@link MappedInterceptor} 的bean。
	 *     子类可以重写并优化此策略。
	 * @param mappedInterceptors 要添加到的空列表
	 */
	protected void detectMappedInterceptors(List<HandlerInterceptor> mappedInterceptors) {
		mappedInterceptors.addAll(BeanFactoryUtils.beansOfTypeIncludingAncestors(
				obtainApplicationContext(), MappedInterceptor.class, true, false).values());
	}

	/**
	 * 初始化指定的拦截器，将{@link WebRequestInterceptor} 适配为 {@link HandlerInterceptor}。
	 * @see #setInterceptors
	 * @see #adaptInterceptor
	 */
	protected void initInterceptors() {
		if (!this.interceptors.isEmpty()) {
			for (int i = 0; i < this.interceptors.size(); i++) {
				Object interceptor = this.interceptors.get(i);
				if (interceptor == null) {
					throw new IllegalArgumentException("Entry number " + i + " in interceptors array is null");
				}
				this.adaptedInterceptors.add(adaptInterceptor(interceptor));
			}
		}
	}

	/**
	 * 将给定的拦截器对象适配为 {@link HandlerInterceptor}。
	 * <p>
	 *     默认支持的拦截器类型为：{@link HandlerInterceptor} 和 {@link WebRequestInterceptor}。
	 *     每个给定的 {@link WebRequestInterceptor} 都会使用 {@link WebRequestHandlerInterceptorAdapter} 进行包装适配。
	 *
	 * @param interceptor 要适配的拦截器对象
	 * @return 经向下转型或适配后的 HandlerInterceptor 实例
	 * @see org.springframework.web.servlet.HandlerInterceptor
	 * @see org.springframework.web.context.request.WebRequestInterceptor
	 * @see WebRequestHandlerInterceptorAdapter
	 */
	protected HandlerInterceptor adaptInterceptor(Object interceptor) {
		if (interceptor instanceof HandlerInterceptor) {
			return (HandlerInterceptor) interceptor;
		}
		else if (interceptor instanceof WebRequestInterceptor) {
			return new WebRequestHandlerInterceptorAdapter((WebRequestInterceptor) interceptor);
		}
		else {
			throw new IllegalArgumentException("Interceptor type not supported: " + interceptor.getClass().getName());
		}
	}

	/**
	 * 以 {@link HandlerInterceptor} 数组形式返回已适配的拦截器。
	 * @return {@link HandlerInterceptor} 拦截器数组，如果没有则返回 {@code null}
	 */
	@Nullable
	protected final HandlerInterceptor[] getAdaptedInterceptors() {
		return (!this.adaptedInterceptors.isEmpty() ?
				this.adaptedInterceptors.toArray(new HandlerInterceptor[0]) : null);
	}

	/**
	 * 将所有已配置的 {@link MappedInterceptor} 以数组形式返回。
	 * @return {@link MappedInterceptor} 数组，如果没有则返回 {@code null}
	 */
	@Nullable
	protected final MappedInterceptor[] getMappedInterceptors() {
		List<MappedInterceptor> mappedInterceptors = new ArrayList<>(this.adaptedInterceptors.size());
		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
			if (interceptor instanceof MappedInterceptor) {
				mappedInterceptors.add((MappedInterceptor) interceptor);
			}
		}
		return (!mappedInterceptors.isEmpty() ? mappedInterceptors.toArray(new MappedInterceptor[0]) : null);
	}


	/**
	 * 查找给定请求对应的处理器，若未找到特定处理器则回退到默认处理器。
	 * @param request 当前HTTP请求
	 * @return 对应的处理器实例，或默认处理器
	 * @see #getHandlerInternal
	 */
	@Override
	@Nullable
	public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		// 获取 handler
		Object handler = getHandlerInternal(request);
		if (handler == null) {
			handler = getDefaultHandler();
		}
		if (handler == null) {
			return null;
		}
		// Bean name or resolved handler?
		if (handler instanceof String) {
			String handlerName = (String) handler;
			handler = obtainApplicationContext().getBean(handlerName);
		}
		// 创建HandlerExecutionChain实例
		HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);
		// Cors
		if (CorsUtils.isCorsRequest(request)) {
			CorsConfiguration globalConfig = this.globalCorsConfigSource.getCorsConfiguration(request);
			CorsConfiguration handlerConfig = getCorsConfiguration(handler, request);
			CorsConfiguration config = (globalConfig != null ? globalConfig.combine(handlerConfig) : handlerConfig);
			executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
		}
		return executionChain;
	}

	/**
	 * 查找给定请求的处理器，如果未找到特定处理器则返回 {@code null}。
	 * 此方法由 {@link #getHandler} 调用；返回 {@code null} 值时（如果设置了默认处理器）将转而使用默认处理器。
	 * <p>
	 *     对于 CORS 预检请求，此方法返回的匹配不应针对预检请求本身，
	 *     而应基于 URL 路径、"Access-Control-Request-Method" 头中的 HTTP 方法以及 "Access-Control-Request-Headers" 头中的头部信息，
	 *     返回预期实际请求的匹配，从而允许通过 {@link #getCorsConfigurations} 获取 CORS 配置。
	 * <p>
	 *     注意：此方法也可能返回预构建的 {@link HandlerExecutionChain}，该执行链将处理器对象与动态确定的拦截器相结合。
	 *     静态指定的拦截器会被合并到此类现有链中。
	 * @param request 当前 HTTP 请求
	 * @return 相应的处理器实例，如果未找到则返回 {@code null}
	 * @throws Exception 如果发生内部错误
	 */
	@Nullable
	protected abstract Object getHandlerInternal(HttpServletRequest request) throws Exception;

	/**
	 * 为给定的处理器构建一个包含适用拦截器的{@link HandlerExecutionChain}。
	 * <p>
	 *     默认实现使用给定的处理器、处理器映射的通用拦截器以及任何与当前请求URL匹配的{@link MappedInterceptor 映射拦截器}构建一个标准的{@link HandlerExecutionChain}。
	 *     拦截器按照注册的顺序添加。子类可以重写此方法以扩展/重新排列拦截器列表。
	 * <p>
	 *     <b>注意：</b>传入的处理器对象可能是原始处理器或预构建的{@link HandlerExecutionChain}。
	 *     此方法应显式处理这两种情况，要么构建一个新的{@link HandlerExecutionChain}，要么扩展现有链。
	 * <p>
	 *     对于在自定义子类中简单添加拦截器的情况，
	 *     考虑调用{@code super.getHandlerExecutionChain(handler, request)}并在返回的链对象上调用{@link HandlerExecutionChain#addInterceptor}。
	 * @param handler 已解析的处理器实例（永远不会为{@code null}）
	 * @param request 当前HTTP请求
	 * @return HandlerExecutionChain（永远不会为{@code null}）
	 * @see #getAdaptedInterceptors()
	 */
	protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
		HandlerExecutionChain chain = (handler instanceof HandlerExecutionChain ?
				(HandlerExecutionChain) handler : new HandlerExecutionChain(handler));

		String lookupPath = this.urlPathHelper.getLookupPathForRequest(request);
		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
			if (interceptor instanceof MappedInterceptor) {
				MappedInterceptor mappedInterceptor = (MappedInterceptor) interceptor;
				if (mappedInterceptor.matches(lookupPath, this.pathMatcher)) {
					chain.addInterceptor(mappedInterceptor.getInterceptor());
				}
			}
			else {
				chain.addInterceptor(interceptor);
			}
		}
		return chain;
	}

	/**
	 * 获取给定处理器的CORS配置。
	 * @param handler 要检查的处理器（永远不为{@code null}）
	 * @param request 当前请求
	 * @return 处理器的CORS配置，如果不存在则返回{@code null}
	 * @since 4.2
	 */
	@Nullable
	protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
		Object resolvedHandler = handler;
		if (handler instanceof HandlerExecutionChain) {
			resolvedHandler = ((HandlerExecutionChain) handler).getHandler();
		}
		if (resolvedHandler instanceof CorsConfigurationSource) {
			return ((CorsConfigurationSource) resolvedHandler).getCorsConfiguration(request);
		}
		return null;
	}

	/**
	 * 更新用于CORS相关处理的HandlerExecutionChain。
	 * <p>针对预检请求，默认实现会将选定的处理器替换为一个简单的HttpRequestHandler，该处理器会调用已配置的{@link #setCorsProcessor}。
	 * <p>针对实际请求，默认实现会插入一个进行CORS相关检查并添加CORS头部的HandlerInterceptor。
	 * @param request 当前请求
	 * @param chain 处理器执行链
	 * @param config 适用的CORS配置（可能为{@code null}）
	 * @since 4.2
	 */
	protected HandlerExecutionChain getCorsHandlerExecutionChain(HttpServletRequest request,
			HandlerExecutionChain chain, @Nullable CorsConfiguration config) {

		if (CorsUtils.isPreFlightRequest(request)) {
			HandlerInterceptor[] interceptors = chain.getInterceptors();
			return new HandlerExecutionChain(new PreFlightHandler(config), interceptors);
		}
		else {
			chain.addInterceptor(new CorsInterceptor(config));
			return chain;
		}
	}


	private class PreFlightHandler implements HttpRequestHandler, CorsConfigurationSource {

		@Nullable
		private final CorsConfiguration config;

		public PreFlightHandler(@Nullable CorsConfiguration config) {
			this.config = config;
		}

		@Override
		public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
			corsProcessor.processRequest(this.config, request, response);
		}

		@Override
		@Nullable
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			return this.config;
		}
	}


	private class CorsInterceptor extends HandlerInterceptorAdapter implements CorsConfigurationSource {

		@Nullable
		private final CorsConfiguration config;

		public CorsInterceptor(@Nullable CorsConfiguration config) {
			this.config = config;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
				throws Exception {

			return corsProcessor.processRequest(this.config, request, response);
		}

		@Override
		@Nullable
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			return this.config;
		}
	}

}
