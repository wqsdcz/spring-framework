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

package org.springframework.beans.factory;

/**
 * <p>
 *     {@link FactoryBean}接口的扩展实现。实现类可以明确指示是否始终返回独立实例
 *     ——针对那些{@link #isSingleton()}实现返回{@code false}但未能清晰表明实例独立性的场景。
 * </p>
 * <p>
 *     对于未实现此扩展接口的普通{@link FactoryBean}实现，当其{@link #isSingleton()}实现返回{@code false}时，
 *     默认假定始终返回独立实例；暴露的对象仅按需被访问。
 * </p>
 * <p>
 *     <b>注意：</b>此接口属于特殊用途接口，主要供框架内部及协作框架内部使用。
 *     通常应用程序提供的FactoryBean只需实现基础{@link FactoryBean}接口。
 *     此扩展接口可能会在点版本更新中增加新方法。
 * </p>
 * Extension of the {@link FactoryBean} interface. Implementations may
 * indicate whether they always return independent instances, for the
 * case where their {@link #isSingleton()} implementation returning
 * {@code false} does not clearly indicate independent instances.
 *
 * <p>Plain {@link FactoryBean} implementations which do not implement
 * this extended interface are simply assumed to always return independent
 * instances if their {@link #isSingleton()} implementation returns
 * {@code false}; the exposed object is only accessed on demand.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework and within collaborating frameworks.
 * In general, application-provided FactoryBeans should simply implement
 * the plain {@link FactoryBean} interface. New methods might be added
 * to this extended interface even in point releases.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #isPrototype()
 * @see #isSingleton()
 */
public interface SmartFactoryBean<T> extends FactoryBean<T> {

	/**
	 * <p>
	 *     判断此工厂管理的对象是否为原型（prototype）？
	 *     即：{@link #getObject()} 是否始终返回独立实例？
	 * </p>
	 * <p>
	 *     FactoryBean本身的原型状态通常由所属的{@link BeanFactory}提供；
	 *     一般情况下，在BeanFactory中FactoryBean必须被定义为单例。
	 * </p>
	 * <p>
	 *     此方法应当严格检查是否为独立实例；对于作用域对象或其他类型的非单例非独立对象，不应返回{@code true}。
	 *     因此，它并非简单是{@link #isSingleton()}的反向判断。
	 * </p>
	 * <p>
	 *     默认实现返回{@code false}。
	 * </p>
	 * @return 暴露的对象是否为原型模式
	 * @see #getObject()
	 * @see #isSingleton()
	 */
	default boolean isPrototype() {
		return false;
	}

	/**
	 * <p>
	 *     此FactoryBean是否需要急切初始化（eager initialization），
	 *     即: 是否需立即初始化自身及其单例对象（若存在）？
	 * </p>
	 * <p>
	 *     标准FactoryBean通常不需要急切初始化： 即使对于单例对象，其{@link #getObject()}方法也仅在实际访问时才会被调用。
	 *     从此方法返回{@code true}意味着应该立即调用{@link #getObject()}， 并同步应用后处理器。
	 *     这对于{@link #isSingleton() 单例}对象可能很有意义， 特别是在后处理器需要在启动阶段立即生效的场景下。
	 * </p>
	 * <p>
	 *     默认实现返回{@code false}。
	 * </p>
	 * @return 是否适用急切初始化
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons()
	 */
	default boolean isEagerInit() {
		return false;
	}

}
