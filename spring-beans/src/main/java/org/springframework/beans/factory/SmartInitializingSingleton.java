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
 * 在{@link BeanFactory}引导期间单例预实例化阶段结束时，触发的回调接口。
 * 单例bean可以实现此接口，以便在常规单例实例化算法之后执行一些初始化，
 * 避免意外早期初始化的副作用（例如，来自{@link ListableBeanFactory#getBeansOfType}调用）。
 * 从这个意义上说，它是{@link InitializingBean}的替代方案，后者在bean的本地构造阶段结束时触发。
 *
 * <p>
 *     此回调变体与{@link org.springframework.context.event.ContextRefreshedEvent}有些相似，
 *     但不需要实现{@link org.springframework.context.ApplicationListener}，不需要在上下文层次结构中过滤上下文引用等。
 *     它还意味着对{@code beans}包的更最小化依赖，并由独立的{@link ListableBeanFactory}实现支持，
 *     而不仅仅是在{@link org.springframework.context.ApplicationContext}环境中。
 *
 * <p>
 *     <b>注意：</b>如果你打算启动/管理异步任务，最好改为实现{@link org.springframework.context.Lifecycle}，
 *     它提供更丰富的运行时管理模型，并允许分阶段启动/关闭。
 *
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons()
 */
public interface SmartInitializingSingleton {

	/**
	 * 在单例预实例化阶段结束时调用，保证所有常规单例bean已经被创建。
	 * <p>
	 *     此方法中的{@link ListableBeanFactory#getBeansOfType}调用不会触发引导期间的意外副作用。
	 * <p>
	 *     <b>注意：</b>此回调不会为在{@link BeanFactory}引导后按需延迟初始化的单例bean触发，也不会为任何其他bean作用域触发。
	 *     仅对具有预期引导语义的bean小心使用它。
	 */
	void afterSingletonsInstantiated();

}
