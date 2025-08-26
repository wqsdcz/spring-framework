/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

/**
 * <p>允许对应用程序上下文中的 bean 定义进行自定义修改，从而调整该上下文底层 bean 工厂的 bean 属性值。
 * <p>应用程序上下文能够在其 bean 定义中自动检测 BeanFactoryPostProcessor 类型的 bean，并在任何其他 bean 创建之前应用它们。
 * <p>该功能特别适用于面向系统管理员的自定义配置文件，这些文件可覆盖应用上下文中已配置的bean属性。
 * <p>请参阅 PropertyResourceConfigurer 及其具体实现，以获取解决此类配置需求的现成解决方案。
 * <p>BeanFactoryPostProcessor 可以与 bean 定义进行交互并对其进行修改，但绝不能修改 bean 实例。
 * 这样做可能会导致过早的 bean 实例化，违反容器并产生意外的副作用。如果需要与 bean 实例进行交互，请考虑实现 {@link BeanPostProcessor} 替代方案。
 *
 * Allows for custom modification of an application context's bean definitions,
 * adapting the bean property values of the context's underlying bean factory.
 *
 * <p>Application contexts can auto-detect BeanFactoryPostProcessor beans in
 * their bean definitions and apply them before any other beans get created.
 *
 * <p>Useful for custom config files targeted at system administrators that
 * override bean properties configured in the application context.
 *
 * <p>See PropertyResourceConfigurer and its concrete implementations
 * for out-of-the-box solutions that address such configuration needs.
 *
 * <p>A BeanFactoryPostProcessor may interact with and modify bean
 * definitions, but never bean instances. Doing so may cause premature bean
 * instantiation, violating the container and causing unintended side-effects.
 * If bean instance interaction is required, consider implementing
 * {@link BeanPostProcessor} instead.
 *
 * @author Juergen Hoeller
 * @since 06.07.2003
 * @see BeanPostProcessor
 * @see PropertyResourceConfigurer
 */
@FunctionalInterface
public interface BeanFactoryPostProcessor {

	/**
	 * Modify the application context's internal bean factory after its standard
	 * initialization. All bean definitions will have been loaded, but no beans
	 * will have been instantiated yet. This allows for overriding or adding
	 * properties even to eager-initializing beans.
	 * @param beanFactory 在应用程序上下文中，使用的bean工厂
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
