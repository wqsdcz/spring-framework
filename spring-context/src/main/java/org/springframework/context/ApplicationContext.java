/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;

/**
 * 为应用程序提供配置的核心接口。在应用程序运行时这是只读的，但如果实现支持，可以重新加载。
 *
 * <p>
 *     ApplicationContext 提供：
 * <ul>
 *     <li>
 *         用于访问应用程序组件的 Bean 工厂方法。
 *         继承自 {@link org.springframework.beans.factory.ListableBeanFactory}。
 *     </li>
 *     <li>
 *         以通用方式加载文件资源的能力。
 *         继承自 {@link org.springframework.core.io.ResourceLoader} 接口。
 *     </li>
 *     <li>
 *         向注册的监听器发布事件的能力。
 *         继承自 {@link ApplicationEventPublisher} 接口。
 *     </li>
 *     <li>
 *         解析消息的能力，支持国际化。
 *         继承自 {@link MessageSource} 接口。
 *     </li>
 *     <li>
 *         从父上下文继承。后代上下文中的定义总是具有优先权。
 *         这意味着，例如，整个 Web 应用程序可以使用单个父上下文，而每个 Servlet 都有自己独立的子上下文，与其他 Servlet 的上下文无关。
 *     </li>
 * </ul>
 *
 * <p>
 *     除了标准的 {@link org.springframework.beans.factory.BeanFactory} 生命周期功能外，
 *     ApplicationContext 实现还会检测并调用 {@link ApplicationContextAware} Bean，
 *     以及 {@link ResourceLoaderAware}、{@link ApplicationEventPublisherAware} 和 {@link MessageSourceAware} Bean。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see ConfigurableApplicationContext
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.core.io.ResourceLoader
 */
public interface ApplicationContext extends EnvironmentCapable, ListableBeanFactory, HierarchicalBeanFactory,
		MessageSource, ApplicationEventPublisher, ResourcePatternResolver {

	/**
	 * 返回【此应用上下文】的【唯一标识符】。
	 * @return 【应用上下文】的【唯一标识符】，如果没有，则返回 {@code null}
	 */
	@Nullable
	String getId();

	/**
	 * 返回【此应用上下文】所属的【已部署应用程序的名称】。
	 * @return 【已部署应用程序的名称】，默认为空字符串
	 */
	String getApplicationName();

	/**
	 * 返回【此应用上下文】的【友好名称】。
	 * @return 【此应用上下文】的【显示名称】（永远不会为 {@code null}）
	 */
	String getDisplayName();

	/**
	 * 返回此【应用上下文】首次加载的时间戳。
	 * @return 【此应用上下文】首次加载的时间戳（毫秒）
	 */
	long getStartupDate();

	/**
	 * 返回【父应用上下文】，如果没有【父应用上下文】且此【应用上下文】是【应用上下文层次结构的根】，则返回 {@code null}。
	 * @return 【父应用上下文】，如果没有【父应用上下文】，则返回 {@code null}
	 */
	@Nullable
	ApplicationContext getParent();

	/**
	 * 为此上下文暴露 AutowireCapableBeanFactory 的功能。
	 *
	 * <p>
	 *     应用程序代码通常不会使用此功能，除非是为了初始化存在于应用上下文之外的 bean 实例，并向它们应用 Spring bean 的生命周期（全部或部分）。
	 *
	 * <p>
	 *     另外，{@link ConfigurableApplicationContext} 接口暴露的内部 BeanFactory也提供了对 {@link AutowireCapableBeanFactory} 接口的访问。
	 *     本方法主要作为ApplicationContext 接口上一个便捷的特定设施。
	 *
	 * <p>
	 *     <b>注意：从 4.2 开始，此方法在应用上下文关闭后将一致地抛出 IllegalStateException。</b>
	 *     在当前 Spring Framework 版本中，只有可刷新的应用上下文会这样行为；从 4.2 开始，所有应用上下文实现都将被要求遵守此规则。
	 *
	 * @return 此上下文的 AutowireCapableBeanFactory
	 * @throws IllegalStateException 如果上下文不支持 {@link AutowireCapableBeanFactory} 接口，
	 *                               或尚未持有支持自动装配的 bean 工厂（例如，如果从未调用过 {@code refresh()}），
	 *                               或者上下文已被关闭
	 * @see ConfigurableApplicationContext#refresh()
	 * @see ConfigurableApplicationContext#getBeanFactory()
	 */
	AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException;

}
