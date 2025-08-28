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

package org.springframework.beans.factory;

/**
 * <p>
 *     需通过实现此接口使Bean能够在销毁时释放资源。{@link BeanFactory}会在单独销毁作用域Bean时调用其destroy方法。
 *     而{@link org.springframework.context.ApplicationContext}则会在应用生命周期结束时，随着容器关闭而销毁所有单例Bean。
 * </p>
 * <p>
 *     出于相同目的，Spring托管的Bean也可实现Java的{@link AutoCloseable}接口。
 *     实现接口的替代方案是指定自定义的destroy方法，例如在XML bean定义中进行配置。
 * </p>
 * <p>
 *     有关bean生命周期方法的完整列表，请参阅{@link BeanFactory BeanFactory javadocs}。
 * </p>
 *
 * @author Juergen Hoeller
 * @since 12.08.2003
 * @see InitializingBean
 * @see org.springframework.beans.factory.support.RootBeanDefinition#getDestroyMethodName()
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons()
 * @see org.springframework.context.ConfigurableApplicationContext#close()
 */
public interface DisposableBean {

	/**
	 * Invoked by the containing {@code BeanFactory} on destruction of a bean.
	 * @throws Exception in case of shutdown errors. Exceptions will get logged
	 * but not rethrown to allow other beans to release their resources as well.
	 */
	void destroy() throws Exception;

}
