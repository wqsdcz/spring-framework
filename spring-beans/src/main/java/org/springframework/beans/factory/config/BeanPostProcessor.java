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
import org.springframework.lang.Nullable;

/**
 * <p>
 *     工厂钩子接口，允许对新创建的Bean实例进行自定义修改，例如：检查标记接口(例如：Aware接口)或使用代理对象进行包装。
 * </p>
 * <p>
 *     ApplicationContext能够自动检测其Bean定义中的Bean后处理器，并将其应用于后续创建的所有Bean。
 *     普通Bean工厂支持通过编程方式注册后处理器，这些处理器将应用于通过该工厂创建的所有Bean。
 * </p>
 * <p>
 *     通常，通过标记接口等方式填充Bean的后处理器将实现{@link #postProcessBeforeInitialization}方法，
 *     而使用代理包装Bean的后处理器通常会实现{@link #postProcessAfterInitialization}方法。
 * </p>
 *
 * @author Juergen Hoeller
 * @since 10.10.2003
 * @see InstantiationAwareBeanPostProcessor
 * @see DestructionAwareBeanPostProcessor
 * @see ConfigurableBeanFactory#addBeanPostProcessor
 * @see BeanFactoryPostProcessor
 */
public interface BeanPostProcessor {

	/**
	 * <p>
	 *     在任何Bean初始化回调（如InitializingBean的{@code afterPropertiesSet}或自定义init-method）执行<i>之前</i>，
	 *     将本Bean后处理器应用于给定的新bean实例。该bean此时已完成属性值的注入。返回的bean实例可能是原始对象的包装实例。
	 * </p>
	 * <p>
	 *     默认实现直接返回给定的{@code bean}对象。
	 * </p>
	 * @param bean 新创建的bean实例
	 * @param beanName bean的名称
	 * @return 经过处理的bean实例（可以是原始对象或包装对象）；若返回{@code null}，将不再调用后续的Bean后处理器
	 * @throws org.springframework.beans.BeansException 处理过程中发生错误时抛出
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 */
	@Nullable
	default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * <p>
	 *     在任意Bean初始化回调（例如InitializingBean的{@code afterPropertiesSet}或自定义初始化方法）执行<i>之后</i>，
	 *     将本Bean后处理器应用于给定的新Bean实例。此时该Bean已完成属性值注入。返回的Bean实例可以是原始对象的包装实例。
	 * </p>
	 * <p>
	 *     对于FactoryBean的情况，此回调将同时作用于FactoryBean实例和由FactoryBean创建的对象（自Spring 2.0起生效）。
	 *     后处理器可通过相应的{@code bean instanceof FactoryBean}检查来决定是应用于FactoryBean、创建的对象还是两者同时应用。
	 * </p>
	 * <p>
	 *     与其他所有Bean后处理器回调不同，此回调也会
	 *     在{@link InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation}方法触发短路实例化后被调用。
	 * </p>
	 * <p>
	 *     默认实现直接返回给定的{@code bean}对象。
	 * </p>
	 * @param bean 新创建的Bean实例
	 * @param beanName Bean的名称
	 * @return 要使用的Bean实例（可以是原始对象或包装对象）；若返回{@code null}，将不再调用后续的Bean后处理器
	 * @throws org.springframework.beans.BeansException 处理过程中发生错误时抛出
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.beans.factory.FactoryBean
	 */
	@Nullable
	default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
