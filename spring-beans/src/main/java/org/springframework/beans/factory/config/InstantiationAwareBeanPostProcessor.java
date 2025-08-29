/*
 * Copyright 2002-2019 the original author or authors.
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

import java.beans.PropertyDescriptor;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.lang.Nullable;

/**
 * <p>
 *     作为{@link BeanPostProcessor}的子接口，该接口新增了两个回调时机：
 *     一是实例化前的回调，
 *     二是实例化之后的回调（但在 设置显式属性 或 自动装配 发生之前）。
 * </p>
 * <p>
 *     通常用于抑制特定目标bean的默认实例化过程，例如：创建具有特殊TargetSource（如池化目标、延迟初始化目标等）的代理，或实现额外的注入策略（如字段注入）。
 * </p>
 * <p>
 *     <b>注意：</b>
 *     此接口是特殊用途接口，主要供框架内部使用。
 *     建议开发者尽可能实现标准的{@link BeanPostProcessor}接口，或继承{@link InstantiationAwareBeanPostProcessorAdapter}类，以避免受此接口后续扩展的影响。
 * </p>
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 1.2
 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#setCustomTargetSourceCreators
 * @see org.springframework.aop.framework.autoproxy.target.LazyInitTargetSourceCreator
 */
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

	/**
	 * <p>
	 *     在<i>目标bean被实例化之前</i>应用此BeanPostProcessor。
	 *     返回的bean对象可以是一个用于替代目标bean的代理，从而有效抑制目标bean的默认实例化过程。
	 * </p>
	 * <p>
	 *     若该方法返回非空对象，bean创建流程将被短路。
	 *     唯一会继续执行的只有配置的{@link BeanPostProcessor BeanPostProcessors}中的{@link #postProcessAfterInitialization}回调。
	 * </p>
	 * <p>
	 *     此回调将应用于带有beanClass的bean定义，以及工厂方法定义（这种情况下返回的bean类型将在此处传入）。
	 * </p>
	 * <p>
	 *     该后处理器可以实现扩展的{@link SmartInstantiationAwareBeanPostProcessor}接口，以预测它们将在此处返回的bean对象类型。
	 * </p>
	 * <p>
	 *     默认实现返回{@code null}。
	 * </p>
	 *
	 * @param beanClass 要实例化的bean的类
	 * @param beanName bean的名称
	 * @return 要暴露的bean对象（将替代目标bean的默认实例），或返回{@code null}以继续执行默认实例化
	 * @throws org.springframework.beans.BeansException 在错误的情况下
	 * @see #postProcessAfterInstantiation
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getBeanClass()
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getFactoryMethodName()
	 */
	@Nullable
	default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * <p>
	 *     在bean通过构造函数或工厂方法实例化之后，但在Spring属性填充（无论是通过显式属性设置还是自动装配）发生之前，执行操作。
	 * </p>
	 * <p>
	 *     这是在Spring自动装配启动之前，对给定bean实例执行自定义字段注入的理想回调点。
	 * </p>
	 * <p>
	 *     默认实现返回{@code true}。
	 * </p>
	 * @param bean 已创建但尚未设置属性的bean实例
	 * @param beanName bean的名称
	 * @return 如果应在bean上设置属性则返回{@code true}；如果应跳过属性填充则返回{@code false}。
	 * 正常实现应返回{@code true}。返回{@code false}还将阻止任何后续的InstantiationAwareBeanPostProcessor实例对此bean实例的调用。
	 * @throws org.springframework.beans.BeansException 在错误的情况下
	 * @see #postProcessBeforeInstantiation
	 */
	default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}

	/**
	 * <p>
	 *     在工厂将给定的属性值应用到目标bean之前，对这些属性值进行后处理。
	 *     此方法允许检查所有依赖是否已满足，例如：基于bean属性setter上的"Required"注解进行检查。
	 * </p>
	 * <p>
	 *     同时允许替换要应用的属性值，通常基于原始PropertyValues创建一个新的MutablePropertyValues实例，并添加或删除特定值来实现。
	 * </p>
	 * <p>
	 *     默认实现直接返回给定的{@code pvs}。
	 * </p>
	 * @param pvs 工厂即将应用的属性值（永不为{@code null}）
	 * @param pds 目标bean的相关属性描述符（已过滤掉被忽略的依赖类型 - 这些类型由工厂特殊处理）
	 * @param bean 已创建但尚未设置属性的bean实例
	 * @param beanName bean的名称
	 * @return 要应用到给定bean的实际属性值（可以是传入的PropertyValues实例），或返回{@code null}以跳过属性填充
	 * @throws org.springframework.beans.BeansException 在错误的情况下
	 * @see org.springframework.beans.MutablePropertyValues
	 */
	@Nullable
	default PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

		return pvs;
	}

}
