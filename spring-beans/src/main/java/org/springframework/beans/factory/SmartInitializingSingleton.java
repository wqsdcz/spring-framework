/*
 * Copyright 2002-2014 the original author or authors.
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
 *     在{@link BeanFactory}的启动过程中，单例的预实例化阶段结束时触发的回调接口。
 *     单例bean可实现此接口，以便在常规单例实例化算法完成后执行某些初始化操作，
 *     从而避免因意外提前初始化（例如通过{@link ListableBeanFactory#getBeansOfType}调用）产生的副作用。
 *     从这个意义上说，它是{@link InitializingBean}的替代方案，后者在bean本地构建阶段结束时立即触发。
 * </p>
 *
 * <p>
 *     此回调机制与{@link org.springframework.context.event.ContextRefreshedEvent}有些相似，
 *     但不需要实现{@link org.springframework.context.ApplicationListener}接口，也无需在上下文层次结构中过滤上下文引用等。
 *     同时它仅依赖最简化的{@code beans}包，且不仅在{@link org.springframework.context.ApplicationContext}环境中，
 *     在独立的{@link ListableBeanFactory}实现中同样有效。
 * </p>
 * <p>
 *     <b>特别注意：</b>如需启动/管理异步任务，建议改为实现{@link org.springframework.context.Lifecycle}接口，
 *     该接口提供了更丰富的运行时管理模型并支持分阶段启动/关闭。
 * </p>
 *
 * Callback interface triggered at the end of the singleton pre-instantiation phase
 * during {@link BeanFactory} bootstrap. This interface can be implemented by
 * singleton beans in order to perform some initialization after the regular
 * singleton instantiation algorithm, avoiding side effects with accidental early
 * initialization (e.g. from {@link ListableBeanFactory#getBeansOfType} calls).
 * In that sense, it is an alternative to {@link InitializingBean} which gets
 * triggered right at the end of a bean's local construction phase.
 *
 * <p>This callback variant is somewhat similar to
 * {@link org.springframework.context.event.ContextRefreshedEvent} but doesn't
 * require an implementation of {@link org.springframework.context.ApplicationListener},
 * with no need to filter context references across a context hierarchy etc.
 * It also implies a more minimal dependency on just the {@code beans} package
 * and is being honored by standalone {@link ListableBeanFactory} implementations,
 * not just in an {@link org.springframework.context.ApplicationContext} environment.
 *
 * <p><b>NOTE:</b> If you intend to start/manage asynchronous tasks, preferably
 * implement {@link org.springframework.context.Lifecycle} instead which offers
 * a richer model for runtime management and allows for phased startup/shutdown.
 *
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons()
 */
public interface SmartInitializingSingleton {

	/**
	 * <p>
	 *     在单例预实例化阶段完全结束时调用，并保证所有常规单例bean都已完成创建。
	 *     在此方法中调用{@link ListableBeanFactory#getBeansOfType}不会在引导过程中引发意外副作用。
	 * </p>
	 * <p>
	 *     <b>特别注意：</b>此回调不会在{@link BeanFactory}引导完成后按需延迟初始化的单例bean中触发，也不会适用于任何其他作用域的bean。
	 *     请仅在具有明确引导语义的bean中谨慎使用此功能。
	 * </p>
	 */
	void afterSingletonsInstantiated();

}
