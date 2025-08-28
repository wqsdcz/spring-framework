/*
 * Copyright 2002-2020 the original author or authors.
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
 * <p>
 *     这是一个接口，那些在{@link BeanFactory}中使用的对象工厂来实现，这些对象本身是单个对象的工厂。
 *     若某个Bean实现此接口，那么这个Bean将作为负责暴露对象的工厂，而非直接将自身作为将被暴露的Bean实例。
 * </p>
 * <p>
 *     <b>注意：实现此接口的bean不能作为普通bean使用。</b>
 *     FactoryBean以bean的形式定义，但通过bean引用对外的暴露的对象（{@link #getObject()}）始终是其创建的对象。
 * </p>
 * <p>
 *     FactoryBean支持单例和原型模式，既可按需延迟创建对象，也可在启动时立即创建。
 *     {@link SmartFactoryBean}接口允许暴露更细粒度的行为元数据。
 * </p>
 * <p>
 *     此接口在框架内部被广泛使用，例如AOP模块中的{@link org.springframework.aop.framework.ProxyFactoryBean}
 *     或{@link org.springframework.jndi.JndiObjectFactoryBean}。它也可用于自定义组件，但通常仅见于基础设施代码中。
 * </p>
 * <p>
 *     <b>{@code FactoryBean}是编程契约。实现类不应依赖于注解驱动的注入或其他反射机制。</b>
 *     {@link #getObjectType()}和{@link #getObject()}的调用可能发生在引导过程早期，甚至早于任何后处理器设置。
 *     如需访问其他bean，请实现{@link BeanFactoryAware}并通过编程方式获取它们。
 * </p>
 * <p>
 *     <b>容器仅负责管理FactoryBean实例的生命周期，不负责管理由FactoryBean创建的对象生命周期。</b>
 *     因此，暴露的bean对象上的销毁方法（如{@link java.io.Closeable#close()}）将<em>不会</em>被自动调用。
 *     如果要销毁暴露的bean对象，那么可以通过让FactoryBean实现{@link DisposableBean}，并将所有关闭调用委托给底层对象的方式来完成。
 * </p>
 * <p>
 *     最后，FactoryBean对象会参与所在BeanFactory创建bean的同步过程。
 *     通常不需要内部同步机制，除非用于FactoryBean本身内部的延迟初始化（或类似场景）。
 * </p>
 * Interface to be implemented by objects used within a {@link BeanFactory} which
 * are themselves factories for individual objects. If a bean implements this
 * interface, it is used as a factory for an object to expose, not directly as a
 * bean instance that will be exposed itself.
 *
 * <p><b>NB: A bean that implements this interface cannot be used as a normal bean.</b>
 * A FactoryBean is defined in a bean style, but the object exposed for bean
 * references ({@link #getObject()}) is always the object that it creates.
 *
 * <p>FactoryBeans can support singletons and prototypes, and can either create
 * objects lazily on demand or eagerly on startup. The {@link SmartFactoryBean}
 * interface allows for exposing more fine-grained behavioral metadata.
 *
 * <p>This interface is heavily used within the framework itself, for example for
 * the AOP {@link org.springframework.aop.framework.ProxyFactoryBean} or the
 * {@link org.springframework.jndi.JndiObjectFactoryBean}. It can be used for
 * custom components as well; however, this is only common for infrastructure code.
 *
 * <p><b>{@code FactoryBean} is a programmatic contract. Implementations are not
 * supposed to rely on annotation-driven injection or other reflective facilities.</b>
 * {@link #getObjectType()} {@link #getObject()} invocations may arrive early in the
 * bootstrap process, even ahead of any post-processor setup. If you need access to
 * other beans, implement {@link BeanFactoryAware} and obtain them programmatically.
 *
 * <p><b>The container is only responsible for managing the lifecycle of the FactoryBean
 * instance, not the lifecycle of the objects created by the FactoryBean.</b> Therefore,
 * a destroy method on an exposed bean object (such as {@link java.io.Closeable#close()}
 * will <i>not</i> be called automatically. Instead, a FactoryBean should implement
 * {@link DisposableBean} and delegate any such close call to the underlying object.
 *
 * <p>Finally, FactoryBean objects participate in the containing BeanFactory's
 * synchronization of bean creation. There is usually no need for internal
 * synchronization other than for purposes of lazy initialization within the
 * FactoryBean itself (or the like).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 08.03.2003
 * @param <T> the bean type
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.aop.framework.ProxyFactoryBean
 * @see org.springframework.jndi.JndiObjectFactoryBean
 */
public interface FactoryBean<T> {

	/**
	 * <p>
	 *     返回此工厂管理的对象实例（该实例可能是共享的或独立的）。
	 * </p>
	 * <p>
	 *     与{@link BeanFactory}一样，此方法同时支持单例（Singleton）和原型（Prototype）设计模式。
	 * </p>
	 * <p>
	 *     若调用时此FactoryBean尚未完成初始化（例如由于涉及循环引用），则应抛出相应的{@link FactoryBeanNotInitializedException}。
	 * </p>
	 * <p>
	 *     从Spring 2.0开始，FactoryBeans允许返回{@code null}对象。
	 *     工厂会将此视为正常值使用；这种情况下不再抛出FactoryBeanNotInitializedException。
	 *     现在鼓励FactoryBean实现根据情况自行抛出FactoryBeanNotInitializedException。
	 * </p>
	 *
	 * @return bean的一个实例（可以是{@code null}）
	 * @throws Exception 如果创建错误
	 * @see FactoryBeanNotInitializedException
	 */
	@Nullable
	T getObject() throws Exception;

	/**
	 * <p>返回此FactoryBean所创建对象的类型，如果无法提前获知则返回{@code null}。</p>
	 * <p>此方法允许在不实例化对象的情况下检查特定类型的bean，例如在自动装配过程中。</p>
	 * <p>对于创建单例对象的实现，此方法应尽量避免创建单例实例；而应该提前预估类型。 对于原型对象，同样建议在此返回有意义的类型。</p>
	 * <p>此方法可在FactoryBean<i>尚未完成初始化之前</i>被调用。它不能依赖于初始化过程中创建的状态； 当然，如果状态已可用，则仍可使用这些状态。</p>
	 * <p><b>注意：</b>自动装配将直接忽略在此返回{@code null}的FactoryBean。 因此强烈建议根据FactoryBean的当前状态正确实现此方法。</p>
	 * @return 此FactoryBean创建的对象类型，若调用时类型未知则返回{@code null}
	 *
	 * Return the type of object that this FactoryBean creates,
	 * or {@code null} if not known in advance.
	 * <p>This allows one to check for specific types of beans without
	 * instantiating objects, for example on autowiring.
	 * <p>In the case of implementations that are creating a singleton object,
	 * this method should try to avoid singleton creation as far as possible;
	 * it should rather estimate the type in advance.
	 * For prototypes, returning a meaningful type here is advisable too.
	 * <p>This method can be called <i>before</i> this FactoryBean has
	 * been fully initialized. It must not rely on state created during
	 * initialization; of course, it can still use such state if available.
	 * <p><b>NOTE:</b> Autowiring will simply ignore FactoryBeans that return
	 * {@code null} here. Therefore it is highly recommended to implement
	 * this method properly, using the current state of the FactoryBean.
	 * @return the type of object that this FactoryBean creates,
	 * or {@code null} if not known at the time of the call
	 * @see ListableBeanFactory#getBeansOfType
	 */
	@Nullable
	Class<?> getObjectType();

	/**
	 * <p>
	 *     此工厂管理的对象是单例吗？即，{@link #getObject()} 是否会始终返回相同的对象（可被缓存的引用）？
	 * </p>
	 * <p>
	 *     <b>注意：</b>
	 *     如果 FactoryBean 表明持有单例对象，则从 {@code getObject()} 返回的对象可能会被 所属的 BeanFactory 缓存。
	 *     因此，除非 FactoryBean 始终暴露相同的引用，否则不要返回 {@code true}。
	 * </p>
	 * <p>
	 *     FactoryBean 本身的单例状态通常由所属的 BeanFactory 提供； 通常，它必须在其中被定义为单例。
	 * </p>
	 * <p>
	 *     <b>注意：</b>
	 *     该方法返回 {@code false} 并不 一定表示返回的对象是独立实例。
	 *     扩展的 {@link SmartFactoryBean} 接口的实现 可以通过其 {@link SmartFactoryBean#isPrototype()} 方法 明确指示独立实例。
	 *     但对于未实现扩展的 {@link SmartFactoryBean} 接口的普通 {@link FactoryBean} 实现来说，
	 *     如果其 {@code isSingleton()} 实现返回 {@code false}，则通常假定它们始终返回独立实例。
	 * </p>
	 * <p>4
	 *     默认实现返回 {@code true}，因为 {@code FactoryBean} 通常管理一个单例实例。
	 * </p>
	 * @return 暴露的对象是否为单例
	 * @see #getObject()
	 * @see SmartFactoryBean#isPrototype()
	 */
	default boolean isSingleton() {
		return true;
	}

}
