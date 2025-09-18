/*
 * Copyright 2002-2012 the original author or authors.
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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * 在Servlet 3.0+环境中实现的接口，用于以编程方式配置{@link ServletContext}——
 * 这是与传统基于{@code web.xml}的方法相对（或可能结合使用）的替代方案。
 *
 * <p>
 *     此SPI的实现将被{@link SpringServletContainerInitializer}自动检测，而它本身会被任何Servlet 3.0容器自动引导启动。
 *     有关此引导机制的详细信息，请参阅{@linkplain SpringServletContainerInitializer 其Javadoc}。
 *
 * <h2>示例</h2>
 * <h3>传统的基于XML的方法</h3>
 * 大多数构建Web应用的Spring用户都需要注册Spring的{@code DispatcherServlet}。
 * 作为参考，在WEB-INF/web.xml中，通常按以下方式完成：
 * <pre class="code">
 * {@code
 * <servlet>
 *   <servlet-name>dispatcher</servlet-name>
 *   <servlet-class>
 *     org.springframework.web.servlet.DispatcherServlet
 *   </servlet-class>
 *   <init-param>
 *     <param-name>contextConfigLocation</param-name>
 *     <param-value>/WEB-INF/spring/dispatcher-config.xml</param-value>
 *   </init-param>
 *   <load-on-startup>1</load-on-startup>
 * </servlet>
 *
 * <servlet-mapping>
 *   <servlet-name>dispatcher</servlet-name>
 *   <url-pattern>/</url-pattern>
 * </servlet-mapping>}</pre>
 *
 * <h3>使用{@code WebApplicationInitializer}的基于代码的方法</h3>
 * 以下是等效的{@code DispatcherServlet}注册逻辑（采用{@code WebApplicationInitializer}风格）：
 * <pre class="code">
 * public class MyWebAppInitializer implements WebApplicationInitializer {
 *
 *    &#064;Override
 *    public void onStartup(ServletContext container) {
 *      XmlWebApplicationContext appContext = new XmlWebApplicationContext();
 *      appContext.setConfigLocation("/WEB-INF/spring/dispatcher-config.xml");
 *
 *      ServletRegistration.Dynamic dispatcher =
 *        container.addServlet("dispatcher", new DispatcherServlet(appContext));
 *      dispatcher.setLoadOnStartup(1);
 *      dispatcher.addMapping("/");
 *    }
 *
 * }</pre>
 *
 * 作为上述方法的替代方案，您也可以扩展{@link org.springframework.web.servlet.support.AbstractDispatcherServletInitializer}。
 *
 * 如您所见，借助Servlet 3.0的新{@link ServletContext#addServlet}方法，
 * 我们实际上注册的是{@code DispatcherServlet}的一个<em>实例</em>，
 * 这意味着{@code DispatcherServlet}现在可以像任何其他对象一样被对待——
 * 在此案例中通过构造函数注入其应用上下文。
 *
 * <p>
 *     这种方式更简单且更简洁。无需处理初始化参数等，只需普通的JavaBean风格属性和构造函数参数。
 *     您可以在将Spring应用上下文注入{@code DispatcherServlet}之前自由创建和处理它们。
 *
 * <p>大多数主要的Spring Web组件都已更新以支持此注册风格。
 * 您会发现{@code DispatcherServlet}、{@code FrameworkServlet}、
 * {@code ContextLoaderListener}和{@code DelegatingFilterProxy}现在都支持构造函数参数。
 * 即使某个组件（例如非Spring的其他第三方组件）未专门更新以在{@code WebApplicationInitializer}中使用，
 * 它们仍然可以在任何情况下使用。Servlet 3.0的{@code ServletContext} API允许以编程方式设置初始化参数、上下文参数等。
 *
 * <h2>100%基于代码的配置方法</h2>
 * 在上面的示例中，{@code WEB-INF/web.xml}已成功被{@code WebApplicationInitializer}形式的代码替换，
 * 但实际的{@code dispatcher-config.xml} Spring配置仍然基于XML。
 * {@code WebApplicationInitializer}非常适合与Spring基于代码的{@code @Configuration}类一起使用。
 * 有关完整详细信息，请参阅{@link org.springframework.context.annotation.Configuration Configuration}的Javadoc，
 * 但以下示例演示了重构使用Spring的{@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext AnnotationConfigWebApplicationContext}
 * 代替{@code XmlWebApplicationContext}，以及使用用户定义的{@code @Configuration}类{@code AppConfig}和
 * {@code DispatcherConfig}代替Spring XML文件。此示例还稍微超出上述范围，演示了"根"应用上下文的典型配置以及{@code ContextLoaderListener}的注册：
 * <pre class="code">
 * public class MyWebAppInitializer implements WebApplicationInitializer {
 *
 *    &#064;Override
 *    public void onStartup(ServletContext container) {
 *      // 创建"根"Spring应用上下文
 *      AnnotationConfigWebApplicationContext rootContext =
 *        new AnnotationConfigWebApplicationContext();
 *      rootContext.register(AppConfig.class);
 *
 *      // 管理根应用上下文的生命周期
 *      container.addListener(new ContextLoaderListener(rootContext));
 *
 *      // 创建分发器servlet的Spring应用上下文
 *      AnnotationConfigWebApplicationContext dispatcherContext =
 *        new AnnotationConfigWebApplicationContext();
 *      dispatcherContext.register(DispatcherConfig.class);
 *
 *      // 注册并映射分发器servlet
 *      ServletRegistration.Dynamic dispatcher =
 *        container.addServlet("dispatcher", new DispatcherServlet(dispatcherContext));
 *      dispatcher.setLoadOnStartup(1);
 *      dispatcher.addMapping("/");
 *    }
 *
 * }</pre>
 *
 * 作为上述方法的替代方案，您也可以扩展{@link org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer}。
 *
 * 请记住，{@code WebApplicationInitializer}实现是<em>自动检测</em>的——
 * 因此您可以随意将它们打包到应用程序中。
 *
 * <h2>排序{@code WebApplicationInitializer}执行</h2>
 * {@code WebApplicationInitializer}实现可以选择在类级别使用Spring的@{@link org.springframework.core.annotation.Order Order}
 * 注解进行标注，或者可以实现Spring的{@link org.springframework.core.Ordered Ordered}接口。
 * 如果是这样，初始化程序将在调用之前进行排序。这为用户提供了一种确保servlet容器初始化顺序的机制。
 * 此功能的使用预计会很罕见，因为典型的应用程序可能会将所有容器初始化集中在一个{@code WebApplicationInitializer}中。
 *
 * <h2>注意事项</h2>
 *
 * <h3>web.xml版本控制</h3>
 * <p>{@code WEB-INF/web.xml}和{@code WebApplicationInitializer}的使用并不互斥；
 * 例如，web.xml可以注册一个servlet，而{@code WebApplicationInitializer}可以注册另一个。
 * 初始化程序甚至可以通过{@link ServletContext#getServletRegistration(String)}等方法<em>修改</em>
 * 在{@code web.xml}中执行的注册。<strong>但是，如果应用程序中存在{@code WEB-INF/web.xml}，
 * 则其{@code version}属性必须设置为"3.0"或更高，否则servlet容器将忽略{@code ServletContainerInitializer}引导。</strong>
 *
 * <h3>在Tomcat中映射到'/'</h3>
 * <p>Apache Tomcat将其内部{@code DefaultServlet}映射到"/"，在Tomcat版本<=7.0.14中，
 * 此servlet映射<em>无法以编程方式覆盖</em>。7.0.15修复了此问题。
 * 在GlassFish 3.1下也成功测试了覆盖"/"servlet映射。</p>
 *
 * @author Chris Beams
 * @since 3.1
 * @see SpringServletContainerInitializer
 * @see org.springframework.web.context.AbstractContextLoaderInitializer
 * @see org.springframework.web.servlet.support.AbstractDispatcherServletInitializer
 * @see org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer
 */
public interface WebApplicationInitializer {

	/**
	 * 使用初始化此Web应用所需的任何servlet、过滤器、监听器、上下文参数和属性来配置给定的{@link ServletContext}。
	 * 请参阅{@linkplain WebApplicationInitializer 上文}中的示例。
	 *
	 * @param servletContext 要初始化的{@code ServletContext}
	 * @throws ServletException 如果对给定{@code ServletContext}的任何调用抛出{@code ServletException}
	 */
	void onStartup(ServletContext servletContext) throws ServletException;

}
