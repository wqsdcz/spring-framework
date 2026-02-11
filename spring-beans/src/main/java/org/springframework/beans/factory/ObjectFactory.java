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
 * 定义一个工厂，该工厂在被调用时可以返回Object实例
 * （可能是共享的或独立的）。
 * Defines a factory which can return an Object instance
 * (possibly shared or independent) when invoked.
 *
 * <p>此接口通常用于封装通用工厂，该工厂
 * 在每次调用时返回目标对象的新实例（原型）。
 * This interface is typically used to encapsulate a generic factory which
 * returns a new instance (prototype) of some target object on each invocation.
 *
 * <p>此接口与{@link FactoryBean}相似，但后者的实现
 * 通常意味着在
 * {@link BeanFactory}中定义为SPI实例，而此接口的实现通常意味着
 * 作为API提供给其他bean（通过注入）。因此，
 * {@code getObject()}方法具有不同的异常处理行为。
 * This interface is similar to {@link FactoryBean}, but implementations
 * of the latter are normally meant to be defined as SPI instances in a
 * {@link BeanFactory}, while implementations of this class are normally meant
 * to be fed as an API to other beans (through injection). As such, the
 * {@code getObject()} method has different exception handling behavior.
 *
 * @author Colin Sampaleanu
 * @since 1.0.2
 * @param <T> the object type
 * @see FactoryBean
 */
@FunctionalInterface
public interface ObjectFactory<T> {

	/**
	 * 返回由此工厂管理的对象的实例
	 * （可能是共享的或独立的）。
	 * Return an instance (possibly shared or independent)
	 * of the object managed by this factory.
	 * @return 结果实例
	 * @return the resulting instance
	 * @throws BeansException 如果创建时出现错误
	 * @throws BeansException in case of creation errors
	 */
	T getObject() throws BeansException;

}
