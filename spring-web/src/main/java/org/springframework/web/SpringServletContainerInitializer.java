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

package org.springframework.web;

import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * Servlet 3.0 规范中的 {@link ServletContainerInitializer} 接口的实现，用于支持基于代码的servlet容器配置方式
 * （通过Spring的{@link WebApplicationInitializer} SPI机制），替代传统的基于{@code web.xml}的方式。
 *
 * <h2>运行机制</h2>
 * 任何符合Servlet 3.0标准的容器在启动时都会加载并实例化该类，调用其{@link #onStartup}方法（前提是classpath中存在{@code spring-web}模块JAR）。
 * 这是通过JAR Services API的{@link ServiceLoader#load(Class)}方法检测{@code spring-web}模块的
 * {@code META-INF/services/javax.servlet.ServletContainerInitializer}服务提供者配置文件实现的。
 * 详见<a href="https://download.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Service%20Provider">
 * JAR Services API文档</a>以及Servlet 3.0最终草案规范的第<em>8.2.4</em>节。
 *
 * <h3>与{@code web.xml}结合使用</h3>
 * Web应用程序可以通过{@code web.xml}中的{@code metadata-complete}属性（控制Servlet注解扫描）
 * 或同样在{@code web.xml}中的{@code <absolute-ordering>}元素（控制哪些web片段（即jar）允许执行{@code ServletContainerInitializer}扫描）
 * 来限制Servlet容器在启动时执行的类路径扫描量。使用此功能时，可以通过在{@code web.xml}中添加"spring_web"到命名web片段列表来启用
 * {@link SpringServletContainerInitializer}，如下所示：
 *
 *
 * <pre>
 * <code>
 *     &lt;absolute-ordering&gt;
 *       &lt;name>some_web_fragment&lt;/name&gt;
 *       &lt;name>spring_web&lt;/name&gt;
 *     &lt;/absolute-ordering&gt;
 * </code>
 * </pre>
 *
 * <h2>与Spring的{@code WebApplicationInitializer}的关系</h2>
 * Spring的{@code WebApplicationInitializer} SPI仅包含一个方法：
 * {@link WebApplicationInitializer#onStartup(ServletContext)}。
 * 其签名故意设计为与{@link ServletContainerInitializer#onStartup(Set, ServletContext)}非常相似：
 * 简而言之，{@code SpringServletContainerInitializer}负责实例化并将{@code ServletContext}委托给
 * 任何用户定义的{@code WebApplicationInitializer}实现。然后，每个{@code WebApplicationInitializer}
 * 负责执行初始化{@code ServletContext}的实际工作。下文{@link #onStartup onStartup}文档中详细描述了确切的委托过程。
 *
 * <h2>一般说明</h2>
 * 通常，该类应视为更重要且面向用户的{@code WebApplicationInitializer} SPI的<em>支持基础设施</em>。
 * 利用此容器初始化器也是完全<em>可选的</em>：虽然此初始化器确实会在所有Servlet 3.0+运行时中被加载和调用，
 * 但是否在类路径中提供任何{@code WebApplicationInitializer}实现仍然是用户的选择。
 * 如果未检测到任何{@code WebApplicationInitializer}类型，此容器初始化器将不起作用。
 *
 * <p>请注意，使用此容器初始化器和{@code WebApplicationInitializer}并不以任何方式"绑定"到Spring MVC，
 * 除了这些类型随{@code spring-web}模块JAR提供这一事实。相反，它们可视为通用工具，
 * 能够方便地实现基于代码的{@code ServletContext}配置。换句话说，任何servlet、listener或filter都可以在
 * {@code WebApplicationInitializer}中注册，而不仅仅是Spring MVC特定的组件。
 *
 * <p>此类既非为扩展而设计，也无意被扩展。它应被视为内部类型，而{@code WebApplicationInitializer}是面向公众的SPI。
 *
 * <h2>另请参阅</h2>
 * 有关示例和详细用法建议，请参见{@link WebApplicationInitializer} Javadoc。<p>
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see #onStartup(Set, ServletContext)
 * @see WebApplicationInitializer
 */
@HandlesTypes(WebApplicationInitializer.class)
public class SpringServletContainerInitializer implements ServletContainerInitializer {

	/**
	 * 将 {@code ServletContext} 委托给应用程序类路径中存在的所有 {@link WebApplicationInitializer} 实现。
	 * <p>由于此类声明了 @{@code HandlesTypes(WebApplicationInitializer.class)}，
	 * Servlet 3.0+ 容器将自动扫描类路径以查找 Spring 的 {@code WebApplicationInitializer} 接口的实现，
	 * 并将所有此类类型的集合提供给本方法的 {@code webAppInitializerClasses} 参数。
	 * <p>如果在类路径上未找到任何 {@code WebApplicationInitializer} 实现，
	 * 则此方法实际上不执行任何操作（no-op）。将发出 INFO 级别的日志消息，通知用户
	 * {@code ServletContainerInitializer} 已被调用，但未找到任何 {@code WebApplicationInitializer} 实现。
	 * <p>假设检测到一个或多个 {@code WebApplicationInitializer} 类型，
	 * 它们将被实例化（如果存在 @{@link org.springframework.core.annotation.Order @Order} 注解
	 * 或实现了 {@link org.springframework.core.Ordered Ordered} 接口，则会进行<em>排序</em>）。
	 * 随后将在每个实例上调用 {@link WebApplicationInitializer#onStartup(ServletContext)} 方法，
	 * 从而将 {@code ServletContext} 委托给每个实例，以便它们可以注册和配置 Servlet（例如 Spring 的
	 * {@code DispatcherServlet}）、监听器（例如 Spring 的 {@code ContextLoaderListener}），
	 * 或任何其他 Servlet API 组件（例如过滤器）。
	 * @param webAppInitializerClasses 在应用程序类路径上找到的所有
	 * {@link WebApplicationInitializer} 的实现类
	 * @param servletContext 待初始化的 Servlet 上下文
	 * @see WebApplicationInitializer#onStartup(ServletContext)
	 * @see AnnotationAwareOrderComparator
	 */
	@Override
	public void onStartup(@Nullable Set<Class<?>> webAppInitializerClasses, ServletContext servletContext)
			throws ServletException {

		List<WebApplicationInitializer> initializers = new LinkedList<>();

		if (webAppInitializerClasses != null) {
			for (Class<?> waiClass : webAppInitializerClasses) {
				// Be defensive: Some servlet containers provide us with invalid classes,
				// no matter what @HandlesTypes says...
				if (!waiClass.isInterface() && !Modifier.isAbstract(waiClass.getModifiers()) &&
						WebApplicationInitializer.class.isAssignableFrom(waiClass)) {
					try {
						initializers.add((WebApplicationInitializer)
								ReflectionUtils.accessibleConstructor(waiClass).newInstance());
					}
					catch (Throwable ex) {
						throw new ServletException("实例化 WebApplicationInitializer 类失败。", ex);
					}
				}
			}
		}

		if (initializers.isEmpty()) {
			servletContext.log("在类路径上，没有检测到 Spring WebApplicationInitializer 类型的实现。");
			return;
		}

		servletContext.log("在类路径上，检测到" + initializers.size() + "个 Spring WebApplicationInitializer 类型的实现。");
		AnnotationAwareOrderComparator.sort(initializers);
		for (WebApplicationInitializer initializer : initializers) {
			initializer.onStartup(servletContext);
		}
	}

}
