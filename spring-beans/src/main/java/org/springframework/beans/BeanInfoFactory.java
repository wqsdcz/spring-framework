/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;

import org.springframework.lang.Nullable;

/**
 * Spring Bean 的 {@link BeanInfo} 实例创建策略接口。该接口可用于：
 *  1、插入自定义的 Bean 属性解析策略（如支持 JVM 上的其他语言）
 *  2、实现更高效的 {@link BeanInfo} 检索算法
 *
 * <p>{@link CachedIntrospectionResults} 通过 {@link org.springframework.core.io.support.SpringFactoriesLoader} 工具类实例化 BeanInfoFactory。
 *
 * 当需要创建 {@link BeanInfo} 时，{@code CachedIntrospectionResults} 将遍历所有发现的工厂，依次调用各工厂的 {@link #getBeanInfo(Class)} 方法。
 * 若返回 {@code null}， 则继续查询下一个工厂；若所有工厂均不支持该 class，则将创建标准的 {@link BeanInfo} 作为默认实现。
 *
 * <p>注意：{@link org.springframework.core.io.support.SpringFactoriesLoader} 会按照 {@link org.springframework.core.annotation.Order @Order} 注解
 * 对 {@code BeanInfoFactory} 实例进行排序，优先级高的工厂将优先执行。
 *
 * Strategy interface for creating {@link BeanInfo} instances for Spring beans.
 * Can be used to plug in custom bean property resolution strategies (e.g. for other
 * languages on the JVM) or more efficient {@link BeanInfo} retrieval algorithms.
 *
 * <p>BeanInfoFactories are instantiated by the {@link CachedIntrospectionResults},
 * by using the {@link org.springframework.core.io.support.SpringFactoriesLoader}
 * utility class.
 *
 * When a {@link BeanInfo} is to be created, the {@code CachedIntrospectionResults}
 * will iterate through the discovered factories, calling {@link #getBeanInfo(Class)}
 * on each one. If {@code null} is returned, the next factory will be queried.
 * If none of the factories support the class, a standard {@link BeanInfo} will be
 * created as a default.
 *
 * <p>Note that the {@link org.springframework.core.io.support.SpringFactoriesLoader}
 * sorts the {@code BeanInfoFactory} instances by
 * {@link org.springframework.core.annotation.Order @Order}, so that ones with a
 * higher precedence come first.
 *
 * @author Arjen Poutsma
 * @since 3.2
 * @see CachedIntrospectionResults
 * @see org.springframework.core.io.support.SpringFactoriesLoader
 */
public interface BeanInfoFactory {

	/**
	 * 如果支持，则返回给定class的Bean信息。
	 *
	 * Return the bean info for the given class, if supported.
	 * @param beanClass the bean class
	 * @return the BeanInfo, or {@code null} if the given class is not supported
	 * @throws IntrospectionException in case of exceptions
	 */
	@Nullable
	BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException;

}
