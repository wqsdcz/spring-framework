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

package org.springframework.beans.factory.config;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.lang.Nullable;

/**
 * {@link ConfigurableBeanFactory} 使用的策略接口，代表用于持有bean实例的目标作用域。
 * 这允许通过{@link ConfigurableBeanFactory#registerScope(String, Scope) 特定键}注册自定义作用域，
 * 从而扩展BeanFactory的标准作用域{@link ConfigurableBeanFactory#SCOPE_SINGLETON "singleton"}
 * 和 {@link ConfigurableBeanFactory#SCOPE_PROTOTYPE "prototype"}。
 *
 * <p>
 *     {@link org.springframework.context.ApplicationContext} 实现（如{@link org.springframework.web.context.WebApplicationContext}）
 *     可基于此SPI注册特定于其环境的附加标准作用域，例如：
 *     {@link org.springframework.web.context.WebApplicationContext#SCOPE_REQUEST "request"}
 *     和{@link org.springframework.web.context.WebApplicationContext#SCOPE_SESSION "session"}。
 *
 * <p>
 *     尽管其主要用途是在Web环境中扩展作用域，但此SPI是完全通用的：
 *     它提供了从任何底层存储机制（如HTTP会话或自定义会话机制）获取和存储对象的能力。
 *     传入此类{@code get}和{@code remove}方法的名称将标识当前作用域中的目标对象。
 *
 * <p>
 *     {@code Scope} 实现应确保线程安全。
 *     一个{@code Scope}实例可同时用于多个bean工厂（除非显式需要感知包含的BeanFactory），允许多个线程从任意数量的工厂并发访问该{@code Scope}。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0
 * @see ConfigurableBeanFactory#registerScope
 * @see CustomScopeConfigurer
 * @see org.springframework.aop.scope.ScopedProxyFactoryBean
 * @see org.springframework.web.context.request.RequestScope
 * @see org.springframework.web.context.request.SessionScope
 */
public interface Scope {

	/**
	 * 从底层作用域返回指定名称的对象，如果在底层存储机制中未找到，则通过{@link org.springframework.beans.factory.ObjectFactory#getObject()}创建该对象。
	 *
	 * <p>
	 *     这是作用域的核心操作，也是唯一必须实现的操作。
	 *
	 * @param name 要检索的对象名称
	 * @param objectFactory 用于在底层存储机制中不存在时创建作用域对象的{@link ObjectFactory}
	 * @return 所需对象（永不返回{@code null}）
	 * @throws IllegalStateException 如果底层作用域当前未处于活动状态
	 */
	Object get(String name, ObjectFactory<?> objectFactory);

	/**
	 * 从底层作用域中移除指定名称 {@code name} 的对象。
	 *
	 * <p>
	 *     如果未找到对象则返回 {@code null}；否则返回被移除的 {@code Object}。
	 *
	 * <p>
	 *     注意：
	 *     实现方应同时移除该对象注册的销毁回调（如果存在）。
	 *     但在此场景下<i>不需要</i>执行已注册的销毁回调，因为对象将由调用方负责销毁（如适用）。
	 *
	 * <p>
	 *     <b>注意：此为可选操作。</b>
	 *     如果实现不支持显式移除对象，可以抛出 {@link UnsupportedOperationException}。
	 *
	 * @param name 要移除的对象名称
	 * @return 被移除的对象，如果不存在则返回 {@code null}
	 * @throws IllegalStateException 如果底层作用域当前未处于活动状态
	 * @see #registerDestructionCallback
	 */
	@Nullable
	Object remove(String name);

	/**
	 * 注册在作用域内指定对象销毁时（或在作用域整体销毁时——如果该作用域不销毁单个对象而仅整体终止）执行的回调函数。
	 * <p>
	 *     <b>注意：此为可选操作。</b>
	 *     本方法仅针对具有实际销毁配置的作用域bean（如DisposableBean、destroy-method、DestructionAwareBeanPostProcessor）调用。
	 *     实现方应确保在适当时机执行给定的回调。如果底层运行时环境完全不支持此类回调，则<i>必须忽略该回调并记录相应警告</i>。
	 *
	 * <p>
	 *     请注意：'销毁'指的是作为作用域自身生命周期一部分的对象自动销毁，而非应用程序显式移除单个作用域对象。
	 *     如果通过此门面的{@link #remove(String)}方法移除作用域对象，则应同时移除所有已注册的销毁回调——此处假定被移除的对象将被重用或手动销毁。
	 *
	 * @param name     要执行销毁回调的对象名称
	 * @param callback 要执行的销毁回调。注意传入的Runnable永远不会抛出异常，因此可安全执行而无需包裹try-catch块。
	 *                 此外，只要目标对象可序列化，该Runnable通常也可序列化。
	 * @throws IllegalStateException 如果底层作用域当前未处于活动状态
	 * @see org.springframework.beans.factory.DisposableBean
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getDestroyMethodName()
	 * @see DestructionAwareBeanPostProcessor
	 */
	void registerDestructionCallback(String name, Runnable callback);

	/**
	 * 解析指定键对应的上下文对象（如果存在）。
	 * 例如：键为"request"时返回HttpServletRequest对象。
	 *
	 * @param key 上下文键
	 * @return 对应的对象，未找到时返回 {@code null}
	 * @throws IllegalStateException 如果底层作用域当前未处于活动状态
	 */
	@Nullable
	Object resolveContextualObject(String key);

	/**
	 * 返回当前底层作用域的<em>会话ID</em>（如果存在）。
	 * <p>
	 *     会话ID的具体含义取决于底层存储机制。
	 *     对于会话作用域的对象，会话ID通常等于（或派生自）{@link javax.servlet.http.HttpSession#getId() 会话ID}；
	 *     对于位于整体会话内的自定义会话，则应返回当前会话的特定ID。
	 *
	 * <p>
	 *     <b>注意：此为可选操作。</b>
	 *     如果底层存储机制没有明显的ID候选，在此方法的实现中返回{@code null}是完全有效的。
	 *
	 * @return 会话ID，如果当前作用域没有会话ID则返回 {@code null}
	 * @throws IllegalStateException 如果底层作用域当前未处于活动状态
	 */
	@Nullable
	String getConversationId();

}
