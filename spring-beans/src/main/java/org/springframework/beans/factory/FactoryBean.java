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
 * Invocations of {@link #getObjectType()} and {@link #getObject()} may arrive early
 * in the bootstrap process, even ahead of any post-processor setup. If you need access
 * to other beans, implement {@link BeanFactoryAware} and obtain them programmatically.
 *
 * <p><b>The container is only responsible for managing the lifecycle of the FactoryBean
 * instance, not the lifecycle of the objects created by the FactoryBean.</b> Therefore,
 * a destroy method on an exposed bean object (such as {@link java.io.Closeable#close()})
 * will <i>not</i> be called automatically. Instead, a FactoryBean should implement
 * {@link DisposableBean} and delegate any such close call to the underlying object.
 *
 * <p>Finally, FactoryBean objects participate in the containing BeanFactory's
 * synchronization of bean creation. Thus, there is usually no need for internal
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
	 * The name of an attribute that can be
	 * {@link org.springframework.core.AttributeAccessor#setAttribute set} on a
	 * {@link org.springframework.beans.factory.config.BeanDefinition} so that
	 * factory beans can signal their object type when it cannot be deduced from
	 * the factory bean class.
	 * @since 5.2
	 */
	String OBJECT_TYPE_ATTRIBUTE = "factoryBeanObjectType";


	/**
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 * <p>As with a {@link BeanFactory}, this allows support for both the
	 * Singleton and Prototype design patterns.
	 * <p>If this FactoryBean is not fully initialized yet at the time of
	 * the call (for example because it is involved in a circular reference),
	 * throw a corresponding {@link FactoryBeanNotInitializedException}.
	 * <p>FactoryBeans are allowed to return {@code null} objects. The bean
	 * factory will consider this as a normal value to be used and will not throw
	 * a {@code FactoryBeanNotInitializedException} in this case. However,
	 * FactoryBean implementations are encouraged to throw
	 * {@code FactoryBeanNotInitializedException} themselves, as appropriate.
	 * @return an instance of the bean (can be {@code null})
	 * @throws Exception in case of creation errors
	 * @see FactoryBeanNotInitializedException
	 */
	@Nullable
	T getObject() throws Exception;

	/**
	 * 返回此FactoryBean创建的对象类型，
	 * 如果事先不知道则返回{@code null}。
	 * Return the type of object that this FactoryBean creates,
	 * or {@code null} if not known in advance.
	 * <p>这允许在不实例化对象的情况下检查特定类型的bean，
	 * 例如在自动装配时。
	 * This allows one to check for specific types of beans without
	 * instantiating objects, for example on autowiring.
	 * <p>对于创建单例对象的实现，
	 * 此方法应尽量避免单例创建；
	 * 它应该提前估计类型。
	 * For prototypes, returning a meaningful type here is advisable too.
	 * 对于原型，建议在此处返回有意义的类型。
	 * <p>此方法可以在<i>之前</i>调用此FactoryBean
	 * 完全初始化。它不得依赖于在
	 * 初始化期间创建的状态；当然，如果可用，它仍然可以使用此类状态。
	 * This method can be called <i>before</i> this FactoryBean has
	 * been fully initialized. It must not rely on state created during
	 * initialization; of course, it can still use such state if available.
	 * <p><b>注意：</b>自动装配将简单地忽略在此处返回
	 * {@code null}的FactoryBean。因此，强烈建议正确实现
	 * 此方法，使用FactoryBean的当前状态。
	 * <b>NOTE:</b> Autowiring will simply ignore FactoryBeans that return
	 * {@code null} here. Therefore, it is highly recommended to implement
	 * this method properly, using the current state of the FactoryBean.
	 * @return 此FactoryBean创建的对象类型，
	 * 如果在调用时不知道则返回{@code null}
	 * @return the type of object that this FactoryBean creates,
	 * or {@code null} if not known at the time of the call
	 * @see ListableBeanFactory#getBeansOfType
	 */
	@Nullable
	Class<?> getObjectType();

	/**
	 * 此工厂管理的对象是单例吗？也就是说，
	 * {@link #getObject()}是否会总是返回相同的对象
	 * （可以缓存的引用）？
	 * Is the object managed by this factory a singleton? That is,
	 * will {@link #getObject()} always return the same object
	 * (a reference that can be cached)?
	 * <p><b>注意：</b>如果FactoryBean指示它持有单例
	 * 对象，则从{@code getObject()}返回的对象可能会被
	 * 拥有的BeanFactory缓存。因此，除非FactoryBean总是暴露相同的引用，
	 * 否则不要返回{@code true}。
	 * <b>NOTE:</b> If a FactoryBean indicates that it holds a singleton
	 * object, the object returned from {@code getObject()} might get cached
	 * by the owning BeanFactory. Hence, do not return {@code true}
	 * unless the FactoryBean always exposes the same reference.
	 * <p>FactoryBean本身的单例状态通常
	 * 由拥有的BeanFactory提供；通常，它必须
	 * 在那里定义为单例。
	 * The singleton status of the FactoryBean itself will generally
	 * be provided by the owning BeanFactory; usually, it has to be
	 * defined as singleton there.
	 * <p><b>注意：</b>此方法返回{@code false}不一定
	 * 表示返回的对象是独立实例。
	 * 扩展的{@link SmartFactoryBean}接口的实现
	 * 可以通过其
	 * {@link SmartFactoryBean#isPrototype()}方法明确指示独立实例。
	 * Plain {@link FactoryBean}实现如果不实现此扩展接口，则
	 * 简单地假定如果
	 * {@code isSingleton()}实现返回{@code false}，则总是返回独立实例。
	 * <p>默认实现返回{@code true}，因为
	 * {@code FactoryBean}通常管理单例实例。
	 * The default implementation returns {@code true}, since a
	 * {@code FactoryBean} typically manages a singleton instance.
	 * @return 暴露的对象是否是单例
	 * @return whether the exposed object is a singleton
	 * @see #getObject()
	 * @see SmartFactoryBean#isPrototype()
	 */
	default boolean isSingleton() {
		return true;
	}

}
