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

package org.springframework.context;

import java.io.Closeable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.lang.Nullable;

/**
 * 大多数（若非所有）应用上下文都需要实现的SPI接口。
 * 除了 {@link org.springframework.context.ApplicationContext} 接口中的应用上下文客户端方法外，还提供了【配置应用上下文】的工具。
 *
 * <p>
 *     【配置】和【生命周期】方法封装在此接口中，以避免暴露ApplicationContext客户端代码。
 *     本接口中的方法应仅由【启动】和【关闭】代码使用。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 03.11.2003
 */
public interface ConfigurableApplicationContext extends ApplicationContext, Lifecycle, Closeable {

	/**
	 * 这些字符中的任意数量均可作为单个字符串值中多个上下文配置路径之间的分隔符。
	 * @see org.springframework.context.support.AbstractXmlApplicationContext#setConfigLocation
	 * @see org.springframework.web.context.ContextLoader#CONFIG_LOCATION_PARAM
	 * @see org.springframework.web.servlet.FrameworkServlet#setContextConfigLocation
	 */
	String CONFIG_LOCATION_DELIMITERS = ",; \t\n";

	/**
	 * 工厂中，ConversionService bean的名称。如果未提供，则应用默认转换规则。
	 * @since 3.0
	 * @see org.springframework.core.convert.ConversionService
	 */
	String CONVERSION_SERVICE_BEAN_NAME = "conversionService";

	/**
	 * 工厂中LoadTimeWeaver bean的名称。
	 * 如果提供了此类bean，上下文将使用临时ClassLoader进行类型匹配，以便让LoadTimeWeaver处理所有实际的bean类。
	 * @since 2.5
	 * @see org.springframework.instrument.classloading.LoadTimeWeaver
	 */
	String LOAD_TIME_WEAVER_BEAN_NAME = "loadTimeWeaver";

	/**
	 * 工厂中，{@link Environment} bean的名称。
	 * @since 3.1
	 */
	String ENVIRONMENT_BEAN_NAME = "environment";

	/**
	 * 工厂中，系统属性 bean的名称。
	 * @see java.lang.System#getProperties()
	 */
	String SYSTEM_PROPERTIES_BEAN_NAME = "systemProperties";

	/**
	 * 工厂中，系统环境 bean的名称。
	 * @see java.lang.System#getenv()
	 */
	String SYSTEM_ENVIRONMENT_BEAN_NAME = "systemEnvironment";


	/**
	 * 设置此应用上下文的唯一标识符。
	 * @since 3.0
	 */
	void setId(String id);

	/**
	 * 设置此应用上下文的父上下文。
	 * <p>注意：父上下文不应被更改：只有在创建此类的对象时无法获得父上下文的情况下（例如在WebApplicationContext设置中），才应在构造函数外设置。
	 * @param parent 父上下文
	 * @see org.springframework.web.context.ConfigurableWebApplicationContext
	 */
	void setParent(@Nullable ApplicationContext parent);

	/**
	 * 为此应用上下文设置 {@code Environment}。
	 * @param environment 新的环境
	 * @since 3.1
	 */
	void setEnvironment(ConfigurableEnvironment environment);

	/**
	 * 以可配置的形式返回此应用上下文的 {@code Environment}，允许进一步自定义。
	 * @since 3.1
	 */
	@Override
	ConfigurableEnvironment getEnvironment();

	/**
	 * 添加一个新的 BeanFactoryPostProcessor，它将在刷新时应用于此应用上下文的内部 bean 工厂，在任何 bean 定义被评估之前。
	 * 应在上下文配置期间调用。
	 * @param postProcessor 要注册的工厂处理器
	 */
	void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor);

	/**
	 * 添加一个新的 ApplicationListener，它将在上下文事件（如上下文刷新和上下文关闭）时收到通知。
	 * <p>
	 *     注意，此处注册的任何 ApplicationListener 将在刷新时（如果上下文尚未激活）或通过当前事件多播器实时（对于已激活的上下文）应用。
	 * @param listener 要注册的 ApplicationListener
	 * @see org.springframework.context.event.ContextRefreshedEvent
	 * @see org.springframework.context.event.ContextClosedEvent
	 */
	void addApplicationListener(ApplicationListener<?> listener);

	/**
	 * 向此应用上下文注册给定的协议解析器，允许处理额外的资源协议。
	 * <p>
	 *     任何此类解析器都将在此上下文的标准解析规则之前被调用。因此它也可以覆盖任何默认规则。
	 * @since 4.3
	 */
	void addProtocolResolver(ProtocolResolver resolver);

	/**
	 * 加载或刷新配置的持久化表示，该配置可能来自基于Java的配置、XML文件、属性文件、关系数据库模式或其他格式。
	 * <p>
	 *     由于这是一个启动方法，如果失败则应销毁已创建的单例，以避免资源悬空。
	 *     换句话说，在调用此方法后，应该实例化所有单例或者完全不实例化任何单例。
	 *
	 * @throws BeansException 如果无法初始化bean工厂
	 * @throws IllegalStateException 如果已经初始化且不支持多次刷新尝试
	 */
	void refresh() throws BeansException, IllegalStateException;

	/**
	 * 向JVM运行时注册一个关闭钩子，在JVM关闭时关闭此上下文，除非此时它已经被关闭。
	 * <p>此方法可被多次调用。每个上下文实例最多只会注册一个关闭钩子。
	 * @see java.lang.Runtime#addShutdownHook
	 * @see #close()
	 */
	void registerShutdownHook();

	/**
	 * 关闭此应用上下文，释放实现可能持有的所有资源和锁。
	 * 这包括销毁所有缓存的单例bean。
	 * <p>注意：<i>不会</i>在父上下文上调用{@code close}；父上下文有自己独立的生命周期。
	 * <p>此方法可被多次调用而不会产生副作用：在已关闭的上下文上后续的{@code close}调用将被忽略。
	 */
	@Override
	void close();

	/**
	 * 判断此【应用程序上下文】是否处于激活状态，即：是否至少已被刷新一次且尚未被关闭。
	 * @return 上下文是否仍处于活跃状态
	 * @see #refresh()
	 * @see #close()
	 * @see #getBeanFactory()
	 */
	boolean isActive();

	/**
	 * 返回此应用程序上下文的内部bean工厂。可用于访问底层工厂的特定功能。
	 * <p>
	 *     注意：不要使用此方法来对bean工厂进行后处理；单例bean在此之前应该已经被实例化。
	 *     使用BeanFactoryPostProcessor在接触bean之前拦截BeanFactory设置过程。
	 *
	 * <p>
	 *     通常，此内部工厂仅在上下文处于活动状态时可访问，即在{@link #refresh()}和{@link #close()}之间。
	 *     可使用{@link #isActive()}标志检查上下文是否处于适当状态。
	 *
	 * @return 底层bean工厂
	 * @throws IllegalStateException 如果上下文不持有内部bean工厂（通常如果尚未调用{@link #refresh()}或已调用{@link #close()}）
	 * @see #isActive()
	 * @see #refresh()
	 * @see #close()
	 * @see #addBeanFactoryPostProcessor
	 */
	ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;

}
