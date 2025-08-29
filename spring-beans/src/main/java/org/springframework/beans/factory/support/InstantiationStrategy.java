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

package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.lang.Nullable;

/**
 * <p>该接口负责创建与根bean定义对应的实例。</p>
 *
 * <p>此功能被抽取为独立策略，因为存在多种可能的实现方式，包括使用CGLIB动态创建子类以支持方法注入（Method Injection）。</p>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.1
 */
public interface InstantiationStrategy {

	/**
	 * 返回此工厂中给定名称的bean实例。
	 * @param bd bean定义
	 * @param beanName 在此上下文中创建bean时的名称。如果要自动装配不属于此工厂的bean，该名称可以为{@code null}。
	 * @param owner 所属的BeanFactory
	 * @return 此bean定义的bean实例
	 * @throws BeansException 如果实例化尝试失败
	 */
	Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner)
			throws BeansException;

	/**
	 * 返回此工厂中给定名称的bean实例，通过指定的构造函数创建它。
	 * @param bd bean定义
	 * @param beanName 在此上下文中创建bean时的名称。如果要自动装配不属于此工厂的bean，该名称可以为{@code null}
	 * @param owner 所属的BeanFactory
	 * @param ctor 要使用的构造函数
	 * @param args 要应用的构造函数参数
	 * @return 此bean定义的bean实例
	 * @throws BeansException 如果实例化尝试失败
	 */
	Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
			Constructor<?> ctor, @Nullable Object... args) throws BeansException;

	/**
	 * 返回此工厂中给定名称的bean实例，通过指定的工厂方法创建它。
	 * @param bd bean定义
	 * @param beanName 在此上下文中创建bean时的名称。如果要自动装配不属于此工厂的bean，该名称可以为{@code null}
	 * @param owner 所属的BeanFactory
	 * @param factoryBean 调用工厂方法的工厂bean实例，如果是静态工厂方法则为{@code null}
	 * @param factoryMethod 要使用的工厂方法
	 * @param args 要应用的工厂方法参数
	 * @return 此bean定义的bean实例
	 * @throws BeansException 如果实例化尝试失败
	 */
	Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
			@Nullable Object factoryBean, Method factoryMethod, @Nullable Object... args)
			throws BeansException;

}
