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
 * 定义一个工厂，该工厂，在被调用时，可以返回Object实例（可能是共享的或独立的）。
 *
 * <p>
 *     此接口通常用作通用的封装工厂，在每次调用时，该工厂返回目标对象的新实例（原型）。
 *
 * <p>
 *     此接口与{@link FactoryBean}相似，但后者的实现通常意味着在{@link BeanFactory}中定义为SPI实例，
 *     而此接口的实现通常意味着作为API提供给其他bean（通过注入）。
 *     因此，{@code getObject()}方法具有不同的异常处理行为。
 *
 * @author Colin Sampaleanu
 * @since 1.0.2
 * @param <T> 对象类型
 * @see FactoryBean
 */
@FunctionalInterface
public interface ObjectFactory<T> {

	/**
	 * 返回由此工厂管理的对象的实例（可能是共享的或独立的）。
	 * @return 结果实例
	 * @throws BeansException 如果创建时出现错误
	 */
	T getObject() throws BeansException;

}
