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

package org.springframework.beans.factory;

import org.springframework.beans.BeansException;

/**
 * <p>
 *     一种工厂接口，该接口在被调用时能够返回一个对象实例（该实例可能是共享的或独立的）。
 * </p>
 * <p>
 *     此接口通常用于封装通用工厂，该工厂在每次调用时返回某个目标对象的新实例（原型模式）。
 * </p>
 * <p>
 *     本接口与{@link FactoryBean}类似，但后者的实现通常作为SPI实例定义在{@link BeanFactory}中，
 *     而此接口的实现通常作为API提供给其他bean（通过注入方式）。
 *     正因如此，两者的{@code getObject()}方法具有不同的异常处理行为。
 * </p>
 * Defines a factory which can return an Object instance
 * (possibly shared or independent) when invoked.
 *
 * <p>This interface is typically used to encapsulate a generic factory which
 * returns a new instance (prototype) of some target object on each invocation.
 *
 * <p>This interface is similar to {@link FactoryBean}, but implementations
 * of the latter are normally meant to be defined as SPI instances in a
 * {@link BeanFactory}, while implementations of this class are normally meant
 * to be fed as an API to other beans (through injection). As such, the
 * {@code getObject()} method has different exception handling behavior.
 *
 * @author Colin Sampaleanu
 * @since 1.0.2
 * @see FactoryBean
 */
@FunctionalInterface
public interface ObjectFactory<T> {

	/**
	 * 返回此工厂管理的对象实例（该实例可能是共享的或独立的）。
	 * @return 生成的实例
	 * @throws BeansException 若创建过程中出现错误
	 */
	T getObject() throws BeansException;

}
