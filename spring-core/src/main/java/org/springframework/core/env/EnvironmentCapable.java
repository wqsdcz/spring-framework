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

package org.springframework.core.env;

/**
 * <p>该接口表明组件持有并暴露{@link Environment}环境引用。<p/>
 *
 * <p>所有Spring应用上下文均实现 EnvironmentCapable（具备环境能力），
 * 该接口主要用于框架方法中执行{@code instanceof}类型检查——当方法接收可能是（也可能不是）
 * ApplicationContext实例的BeanFactory实例时，若确实存在可用环境，则通过此接口与环境交互。<p/>
 *
 * <p>如前所述，{@link org.springframework.context.ApplicationContext ApplicationContext}继承EnvironmentCapable接口，
 * 因而暴露{@link #getEnvironment()}方法；
 * 然而{@link org.springframework.context.ConfigurableApplicationContext ConfigurableApplicationContext}
 * 重定义了{@link org.springframework.context.ConfigurableApplicationContext#getEnvironment getEnvironment()}方法，
 * 收窄方法签名以返回{@link ConfigurableEnvironment}类型。
 * 这意味着Environment对象在通过ConfigurableApplicationContext访问之前处于'只读'状态，而当通过该上下文访问时，环境对象方可被配置。<p/>
 *
 * Interface indicating a component that contains and exposes an {@link Environment} reference.
 *
 * <p>All Spring application contexts are EnvironmentCapable, and the interface is used primarily
 * for performing {@code instanceof} checks in framework methods that accept BeanFactory
 * instances that may or may not actually be ApplicationContext instances in order to interact
 * with the environment if indeed it is available.
 *
 * <p>As mentioned, {@link org.springframework.context.ApplicationContext ApplicationContext}
 * extends EnvironmentCapable, and thus exposes a {@link #getEnvironment()} method; however,
 * {@link org.springframework.context.ConfigurableApplicationContext ConfigurableApplicationContext}
 * redefines {@link org.springframework.context.ConfigurableApplicationContext#getEnvironment
 * getEnvironment()} and narrows the signature to return a {@link ConfigurableEnvironment}.
 * The effect is that an Environment object is 'read-only' until it is being accessed from
 * a ConfigurableApplicationContext, at which point it too may be configured.
 *
 * @author Chris Beams
 * @since 3.1
 * @see Environment
 * @see ConfigurableEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment()
 */
public interface EnvironmentCapable {

	/**
	 * Return the {@link Environment} associated with this component.
	 */
	Environment getEnvironment();

}
