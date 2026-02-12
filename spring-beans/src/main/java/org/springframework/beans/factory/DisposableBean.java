/*
 * Copyright 2002-present the original author or authors.
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
 * 由希望在销毁时释放资源的bean实现的接口。
 * {@link BeanFactory}将在销毁作用域bean时对单个bean调用destroy方法。
 * {@link org.springframework.context.ApplicationContext}应该
 * 在关闭时处理其所有单例，由应用程序生命周期驱动。
 *
 * <p>Spring管理的bean也可以实现Java的{@link AutoCloseable}接口
 * 达到相同目的。实现接口的替代方案是指定
 * 自定义销毁方法，例如在XML bean定义中。有关所有
 * bean生命周期方法的列表，请参见{@link BeanFactory BeanFactory javadocs}。
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
	 * 由包含的{@code BeanFactory}在销毁bean时调用。
	 * @throws Exception 如果关闭时出现错误。异常会被记录
	 * 但不会重新抛出，以允许其他bean也能释放其资源。
	 */
	void destroy() throws Exception;

}
