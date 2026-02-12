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

/**
 * {@link FactoryBean}接口的扩展。
 * 实现可以指示它们是否总是返回独立实例，以应对其{@link #isSingleton()}实现返回{@code false}，而不能明确表示独立实例的情况。
 *
 * <p>
 *     不实现此扩展接口的普通{@link FactoryBean}实现被简单地假定为，在其{@link #isSingleton()}实现返回{@code false}时，总是返回独立实例；
 *     暴露的对象仅在需要时访问。
 *
 * <p>
 *     <b>注意：</b>此接口是一个专用接口，主要用于框架内部和协作框架中。
 *     一般来说，应用程序提供的FactoryBean应该只实现普通的{@link FactoryBean}接口。
 *     即使在次要版本中，也可能向此扩展接口添加新方法。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @param <T> bean类型
 * @see #isPrototype()
 * @see #isSingleton()
 */
public interface SmartFactoryBean<T> extends FactoryBean<T> {

	/**
	 * 此工厂管理的对象是原型吗？
	 * 也就是说，{@link #getObject()}是否总是返回独立实例？
	 * <p>
	 *     FactoryBean本身的原型状态，通常由所属的{@link BeanFactory}提供；通常，它必须在那里定义为单例。
	 * <p>
	 *     此方法应严格检查独立实例；对于作用域对象或其他类型的非单例、非独立对象，它不应返回{@code true}。
	 *     因此，这不是{@link #isSingleton()}的简单反向形式。
	 * <p>
	 *     默认实现返回{@code false}。
	 *
	 * @return 暴露的对象是否是原型
	 * @see #getObject()
	 * @see #isSingleton()
	 */
	default boolean isPrototype() {
		return false;
	}

	/**
	 * 此FactoryBean是否期望立即初始化，即，立即初始化自身以及期望立即初始化其单例对象（如果有）？
	 *
	 * <p>
	 *     标准FactoryBean不期望立即初始化：即使在单例对象的情况下，也只有在实际访问时才调用其{@link #getObject()}。
	 *     从此方法返回{@code true}表明应立即调用{@link #getObject()}，同时也应立即应用后处理器。
	 *     这在{@link #isSingleton() 单例}对象的情况下可能是有意义的，特别是如果后处理器期望在启动时应用。
	 * <p>
	 *     默认实现返回{@code false}。
	 *
	 * @return 是否应用立即初始化
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons()
	 */
	default boolean isEagerInit() {
		return false;
	}

}
