/*
 * Copyright 2002-2017 the original author or authors.
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
 * <p>策略接口，用于通过将值作为表达式求值（如果适用）来解析值。
 * <p>一个原始的{@link org.springframework.beans.factory.BeanFactory}不含有这个策略的默认实现。
 * 然而，{@link org.springframework.context.ApplicationContext}实现类将提供开箱即用的表达式支持。
 *
 * Strategy interface for resolving a value through evaluating it
 * as an expression, if applicable.
 *
 * <p>A raw {@link org.springframework.beans.factory.BeanFactory} does not
 * contain a default implementation of this strategy. However,
 * {@link org.springframework.context.ApplicationContext} implementations
 * will provide expression support out of the box.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public interface BeanExpressionResolver {

	/**
	 * 将给定的值作为一个表达式（如果适用）进行评估。否则按原样返回值。
	 * Evaluate the given value as an expression, if applicable;
	 * return the value as-is otherwise.
	 *
	 * @param value 要检查的值
	 * @param evalContext 评估上下文
	 * @return 已解析的值（可能是给定的值）
	 * @throws BeansException 如果评估失败
	 */
	@Nullable
	Object evaluate(@Nullable String value, BeanExpressionContext evalContext) throws BeansException;

}
