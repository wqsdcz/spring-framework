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

import org.springframework.beans.BeansException;

/**
 * 由那些希望感知其所属{@link BeanFactory}的bean实现的接口。
 *
 * <p>例如，bean可以通过工厂查找协作bean（依赖查找）。
 * 请注意，大多数bean会选择通过相应的bean属性或构造函数参数接收协作bean的引用（依赖注入）。
 *
 * <p>有关所有bean生命周期方法的列表，请参见 {@link BeanFactory BeanFactory javadocs}。
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @since 11.03.2003
 * @see BeanNameAware
 * @see BeanClassLoaderAware
 * @see InitializingBean
 * @see org.springframework.context.ApplicationContextAware
 */
public interface BeanFactoryAware extends Aware {

	/**
	 * 向bean实例提供所属工厂的回调。
	 * <p>在填充普通bean属性之后但在初始化回调（如{@link InitializingBean#afterPropertiesSet()}或自定义init-method）之前调用。
	 * @param beanFactory 所属BeanFactory（永不为{@code null}）。bean可以立即调用工厂上的方法。
	 * @throws BeansException 如果初始化时出现错误
	 * @see BeanInitializationException
	 */
	void setBeanFactory(BeanFactory beanFactory) throws BeansException;

}
