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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * <p>
 *     这是一个后处理器回调接口，在运行时，用于<i>已合并</i>的Bean定义。
 *     {@link BeanPostProcessor} 的实现类可以选择实现此子接口，
 *     以便对Spring {@code BeanFactory} 用于创建Bean实例的已合并Bean定义（原始Bean定义的处理后副本）进行后处理。
 * </p>
 *
 * <p>
 *     {@link #postProcessMergedBeanDefinition} 方法可以内省Bean定义，以便在实际处理Bean实例之前准备一些缓存元数据。
 *     也允许修改Bean定义，但<i>仅限</i>那些实际上允许并发修改的定义属性。
 *     本质上，这仅适用于在{@link RootBeanDefinition}自身上定义的操作，而不适用于其基类的属性。
 * </p>
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getMergedBeanDefinition
 */
public interface MergedBeanDefinitionPostProcessor extends BeanPostProcessor {

	/**
	 * 对指定bean的已合并Bean定义进行后处理。
	 * @param beanDefinition 该bean合并后的Bean定义
	 * @param beanType 被管理bean实例的实际类型
	 * @param beanName bean的名称
	 */
	void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName);

}
