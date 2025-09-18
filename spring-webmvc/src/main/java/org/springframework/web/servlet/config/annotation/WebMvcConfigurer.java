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

package org.springframework.web.servlet.config.annotation;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * 定义回调方法，用于通过{@code @EnableWebMvc}启用Spring MVC时，自定义其基于Java的配置。
 *
 * <p>
 *     被{@code @EnableWebMvc}注解的配置类可以实现此接口，以便在初始化时被回调并有机会自定义默认配置。
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author David Syer
 * @since 3.1
 */
public interface WebMvcConfigurer {

	/**
	 * 辅助配置HandlerMappings的路径匹配选项，
	 * 例如：尾部斜杠匹配、后缀注册、路径匹配器（path matcher）和路径助手（path helper）。
	 * 已配置的路径匹配器和路径助手实例将在以下组件间共享：
	 * <ul>
	 * <li>请求映射（RequestMappings）</li>
	 * <li>视图控制器映射（ViewControllerMappings）</li>
	 * <li>资源映射（ResourcesMappings）</li>
	 * </ul>
	 * @since 4.0.3
	 */
	default void configurePathMatch(PathMatchConfigurer configurer) {
	}

	/**
	 * 配置内容协商选项。
	 */
	default void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
	}

	/**
	 * 配置异步请求处理选项。
	 */
	default void configureAsyncSupport(AsyncSupportConfigurer configurer) {
	}

	/**
	 * 通过将未处理的请求转发到Servlet容器的"默认"Servlet来配置处理程序。
	 * 一个常见的使用场景是当{@link DispatcherServlet}被映射到"/"时，此时会覆盖Servlet容器对静态资源的默认处理。
	 */
	default void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
	}

	/**
	 * 添加除默认注册外额外的{@link Converter}和{@link Formatter}。
	 */
	default void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * 添加Spring MVC生命周期拦截器，用于控制器方法调用和资源处理器请求的预处理和后处理。
	 * 拦截器可注册为适用于所有请求，或仅限于URL模式的子集。
	 */
	default void addInterceptors(InterceptorRegistry registry) {
	}

	/**
	 * 添加处理程序以提供静态资源服务（如图片、js和css文件），这些资源可来自Web应用程序根目录下的特定位置、类路径等其他位置。
	 */
	default void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	/**
	 * 配置跨域请求处理。
	 * @since 4.2
	 */
	default void addCorsMappings(CorsRegistry registry) {
	}

	/**
	 * Configure simple automated controllers pre-configured with the response
	 * status code and/or a view to render the response body. This is useful in
	 * cases where there is no need for custom controller logic -- e.g. render a
	 * home page, perform simple site URL redirects, return a 404 status with
	 * HTML content, a 204 with no content, and more.
	 */
	default void addViewControllers(ViewControllerRegistry registry) {
	}

	/**
	 * Configure view resolvers to translate String-based view names returned from
	 * controllers into concrete {@link org.springframework.web.servlet.View}
	 * implementations to perform rendering with.
	 * @since 4.1
	 */
	default void configureViewResolvers(ViewResolverRegistry registry) {
	}

	/**
	 * Add resolvers to support custom controller method argument types.
	 * <p>This does not override the built-in support for resolving handler
	 * method arguments. To customize the built-in support for argument
	 * resolution, configure {@link RequestMappingHandlerAdapter} directly.
	 * @param resolvers initially an empty list
	 */
	default void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
	}

	/**
	 * Add handlers to support custom controller method return value types.
	 * <p>Using this option does not override the built-in support for handling
	 * return values. To customize the built-in support for handling return
	 * values, configure RequestMappingHandlerAdapter directly.
	 * @param handlers initially an empty list
	 */
	default void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> handlers) {
	}

	/**
	 * Configure the {@link HttpMessageConverter}s to use for reading or writing
	 * to the body of the request or response. If no converters are added, a
	 * default list of converters is registered.
	 * <p><strong>Note</strong> that adding converters to the list, turns off
	 * default converter registration. To simply add a converter without impacting
	 * default registration, consider using the method
	 * {@link #extendMessageConverters(java.util.List)} instead.
	 * @param converters initially an empty list of converters
	 */
	default void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * A hook for extending or modifying the list of converters after it has been
	 * configured. This may be useful for example to allow default converters to
	 * be registered and then insert a custom converter through this method.
	 * @param converters the list of configured converters to extend.
	 * @since 4.1.3
	 */
	default void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * Configure exception resolvers.
	 * <p>The given list starts out empty. If it is left empty, the framework
	 * configures a default set of resolvers, see
	 * {@link WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers(List)}.
	 * Or if any exception resolvers are added to the list, then the application
	 * effectively takes over and must provide, fully initialized, exception
	 * resolvers.
	 * <p>Alternatively you can use
	 * {@link #extendHandlerExceptionResolvers(List)} which allows you to extend
	 * or modify the list of exception resolvers configured by default.
	 * @param resolvers initially an empty list
	 * @see #extendHandlerExceptionResolvers(List)
	 * @see WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers(List)
	 */
	default void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
	}

	/**
	 * Extending or modify the list of exception resolvers configured by default.
	 * This can be useful for inserting a custom exception resolver without
	 * interfering with default ones.
	 * @param resolvers the list of configured resolvers to extend
	 * @since 4.3
	 * @see WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers(List)
	 */
	default void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
	}

	/**
	 * Provide a custom {@link Validator} instead of the one created by default.
	 * The default implementation, assuming JSR-303 is on the classpath, is:
	 * {@link org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean}.
	 * Leave the return value as {@code null} to keep the default.
	 */
	@Nullable
	default Validator getValidator() {
		return null;
	}

	/**
	 * Provide a custom {@link MessageCodesResolver} for building message codes
	 * from data binding and validation error codes. Leave the return value as
	 * {@code null} to keep the default.
	 */
	@Nullable
	default MessageCodesResolver getMessageCodesResolver() {
		return null;
	}

}
