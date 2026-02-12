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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 当{@code BeanFactory}被要求提供一个bean实例，但找到多个匹配候选时抛出的异常，
 * 而预期只有一个匹配bean。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.2.1
 * @see BeanFactory#getBean(Class)
 */
@SuppressWarnings("serial")
public class NoUniqueBeanDefinitionException extends NoSuchBeanDefinitionException {

	private final int numberOfBeansFound;

	@Nullable
	private final Collection<String> beanNamesFound;


	/**
	 * 创建一个新的{@code NoUniqueBeanDefinitionException}。
	 * @param type 非唯一bean所需类型
	 * @param beanNamesFound 所有匹配bean的名称（作为Collection）
	 * @param message 描述问题的详细消息
	 * @since 6.2
	 */
	public NoUniqueBeanDefinitionException(Class<?> type, Collection<String> beanNamesFound, String message) {
		super(type, message);
		this.numberOfBeansFound = beanNamesFound.size();
		this.beanNamesFound = new ArrayList<>(beanNamesFound);
	}

	/**
	 * 创建一个新的{@code NoUniqueBeanDefinitionException}。
	 * @param type 非唯一bean所需类型
	 * @param numberOfBeansFound 匹配bean的数量
	 * @param message 描述问题的详细消息
	 */
	public NoUniqueBeanDefinitionException(Class<?> type, int numberOfBeansFound, String message) {
		super(type, message);
		this.numberOfBeansFound = numberOfBeansFound;
		this.beanNamesFound = null;
	}

	/**
	 * 创建一个新的{@code NoUniqueBeanDefinitionException}。
	 * @param type 非唯一bean所需类型
	 * @param beanNamesFound 所有匹配bean的名称（作为Collection）
	 */
	public NoUniqueBeanDefinitionException(Class<?> type, Collection<String> beanNamesFound) {
		this(type, beanNamesFound, "expected single matching bean but found " + beanNamesFound.size() + ": " +
				StringUtils.collectionToCommaDelimitedString(beanNamesFound));
	}

	/**
	 * 创建一个新的{@code NoUniqueBeanDefinitionException}。
	 * @param type 非唯一bean所需类型
	 * @param beanNamesFound 所有匹配bean的名称（作为数组）
	 */
	public NoUniqueBeanDefinitionException(Class<?> type, String... beanNamesFound) {
		this(type, Arrays.asList(beanNamesFound));
	}

	/**
	 * 创建一个新的{@code NoUniqueBeanDefinitionException}。
	 * @param type 非唯一bean所需类型
	 * @param beanNamesFound 所有匹配bean的名称（作为Collection）
	 * @since 5.1
	 */
	public NoUniqueBeanDefinitionException(ResolvableType type, Collection<String> beanNamesFound) {
		super(type, "expected single matching bean but found " + beanNamesFound.size() + ": " +
				StringUtils.collectionToCommaDelimitedString(beanNamesFound));
		this.numberOfBeansFound = beanNamesFound.size();
		this.beanNamesFound = new ArrayList<>(beanNamesFound);
	}

	/**
	 * 创建一个新的{@code NoUniqueBeanDefinitionException}。
	 * @param type 非唯一bean所需类型
	 * @param beanNamesFound 所有匹配bean的名称（作为数组）
	 * @since 5.1
	 */
	public NoUniqueBeanDefinitionException(ResolvableType type, String... beanNamesFound) {
		this(type, Arrays.asList(beanNamesFound));
	}


	/**
	 * 返回当期望只有一个匹配bean时找到的bean数量。
	 * 对于NoUniqueBeanDefinitionException，这通常会大于1。
	 * @see #getBeanType()
	 */
	@Override
	public int getNumberOfBeansFound() {
		return this.numberOfBeansFound;
	}

	/**
	 * 返回当期望只有一个匹配bean时找到的所有bean名称。
	 * 注意，如果在构造时未指定，这可能为{@code null}。
	 * @since 4.3
	 * @see #getBeanType()
	 */
	@Nullable
	public Collection<String> getBeanNamesFound() {
		return this.beanNamesFound;
	}

}
