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

import org.springframework.lang.Nullable;

/**
 * 由在{@link BeanFactory}中使用的对象实现的接口，这些对象本身是单个对象的工厂。
 * 如果一个bean实现了此接口，它将用作暴露对象的工厂，而不是直接作为将被暴露的bean实例本身。
 *
 * <p>
 *     <b>注意：实现此接口的bean不能用作普通bean。</b>
 *     FactoryBean以bean风格定义，但为bean引用暴露的对象({@link #getObject()})始终是它创建的对象。
 *
 * <p>
 *     FactoryBean可以支持单例和原型，可以按需延迟创建对象，也可以在启动时立即创建。
 *     {@link SmartFactoryBean}接口允许暴露更细粒度的行为元数据。
 *
 * <p>
 *     此接口在框架内部被大量使用，例如用于AOP的 {@link org.springframework.aop.framework.ProxyFactoryBean}或{@link org.springframework.jndi.JndiObjectFactoryBean}。
 *     它也可以用于自定义组件；然而，这只在基础设施代码中常见。
 *
 * <p>
 *     <b>{@code FactoryBean}是一个程序化契约。实现不应该依赖于注解驱动的注入或其他反射工具。</b>
 *     {@link #getObjectType()}和{@link #getObject()}的调用可能在引导过程的早期到达，甚至在任何后处理器设置之前。
 *     如果你需要访问其他bean，实现{@link BeanFactoryAware}并以编程方式获取它们。
 *
 * <p>
 *     <b>容器只负责管理FactoryBean实例的生命周期，而不是FactoryBean创建的对象的生命周期。</b>
 *     因此，暴露的bean对象上的销毁方法（如{@link java.io.Closeable#close()}）将<i>不会</i>被自动调用。
 *     相反，FactoryBean应该实现{@link DisposableBean}，并将任何此类关闭调用委托给底层对象。
 *
 * <p>
 *     最后，FactoryBean对象参与包含BeanFactory的bean创建同步。
 *     因此，除了FactoryBean本身的延迟初始化等目的外，通常不需要内部同步。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 08.03.2003
 * @param <T> bean类型
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.aop.framework.ProxyFactoryBean
 * @see org.springframework.jndi.JndiObjectFactoryBean
 */
public interface FactoryBean<T> {

	/**
	 * 可以在{@link org.springframework.beans.factory.config.BeanDefinition}上
	 * {@link org.springframework.core.AttributeAccessor#setAttribute 设置}的属性名称，
	 * 以便当无法从factory bean类推断出对象类型时，factory beans可以指示其对象类型。
	 * @since 5.2
	 */
	String OBJECT_TYPE_ATTRIBUTE = "factoryBeanObjectType";


	/**
	 * 返回由此工厂管理的对象的实例（可能是共享的或独立的）。
	 * <p>
	 *     与{@link BeanFactory}一样，这允许支持单例和原型设计模式。
	 * <p>
	 *     如果此FactoryBean在调用时尚未完全初始化（例如因为它涉及循环引用），则抛出相应的{@link FactoryBeanNotInitializedException}。
	 * <p>
	 *     允许FactoryBeans返回{@code null}对象。
	 *     bean工厂将把它视为要使用的正常值，在这种情况下不会抛出{@code FactoryBeanNotInitializedException}。
	 *     但是，鼓励FactoryBean实现根据需要自行抛出{@code FactoryBeanNotInitializedException}。
	 *
	 * @return bean的实例（可以为{@code null}）
	 * @throws Exception 如果发生创建错误
	 * @see FactoryBeanNotInitializedException
	 */
	@Nullable
	T getObject() throws Exception;

	/**
	 * 返回此FactoryBean创建的对象类型，如果事先不知道则返回{@code null}。
	 * <p>
	 *     允许本方法的实现，在不实例化对象的情况下，检查特定类型的bean，例如在自动装配时。
	 * <p>
	 *     对于创建单例对象的实现，此方法应尽量避免单例创建；它应该提前估计类型。
	 *     对于原型，建议在此处返回有意义的类型。
	 * <p>
	 *     此方法可以在FactoryBean完全初始化之<i>前</i>调用。
	 *     它不得依赖于初始化期间创建的状态；当然，如果可用，它仍然可以使用此类状态。
	 * <p>
	 *     <b>注意：</b>自动装配将简单地忽略在此处返回{@code null}的FactoryBean。
	 *     因此，强烈建议正确实现此方法，使用FactoryBean的当前状态。
	 *
	 * @return 此FactoryBean创建的对象类型，如果在调用时不知道则返回{@code null}
	 * @see ListableBeanFactory#getBeansOfType
	 */
	@Nullable
	Class<?> getObjectType();

	/**
	 * 此工厂管理的对象是单例吗？
	 * 也就是说，{@link #getObject()}是否会总是返回相同的对象（是否可以缓存的引用）？
	 * <p>
	 *     <b>注意：</b>如果FactoryBean表明自身持有的是单例对象，则从{@code getObject()}返回的对象可能会被所属的BeanFactory缓存。
	 *     因此，除非FactoryBean总是暴露相同的引用，否则不要返回{@code true}。
	 * <p>
	 *     在默认实现中，返回{@code true}，因为{@code FactoryBean}通常管理单例实例。
	 * <p>
	 *     FactoryBean本身的单例状态通常由所属的BeanFactory提供；通常，它必须在那里定义为单例。
	 * <p>
	 *     <b>注意：</b>此方法返回{@code false}并不意味FactoryBean持有的对象一定是独立实例。
	 *     对于{@link SmartFactoryBean}实现来说，以通过其{@link SmartFactoryBean#isPrototype()}方法明确指示独立实例。
	 *     对于普通的{@link FactoryBean}实现（不实现{@link SmartFactoryBean}）来说，则简单地假定如果{@code isSingleton()}实现返回{@code false}，则总是返回独立实例。
	 *
	 * @return 暴露的对象是否是单例
	 * @see #getObject()
	 * @see SmartFactoryBean#isPrototype()
	 */
	default boolean isSingleton() {
		return true;
	}

}
