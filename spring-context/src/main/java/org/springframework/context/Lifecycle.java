/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context;

/**
 * 定义启动/停止生命周期控制方法的通用接口。
 * 典型用例是控制异步处理。
 * <b>注意：此接口不暗示特定的自动启动语义。为实现该目的，请考虑实现 {@link SmartLifecycle}。</b>
 *
 * <p>
 *     可由组件（通常是Spring上下文中定义的Spring bean）和容器（通常是Spring {@link ApplicationContext} 本身）实现。
 *     容器会将启动/停止信号传播给每个容器内适用的所有组件，例如在运行时的停止/重启场景中。
 *
 * <p>
 *     可用于直接调用或通过JMX进行管理操作。
 *     在后一种情况下，{@link org.springframework.jmx.export.MBeanExporter}
 *     通常使用 {@link org.springframework.jmx.export.assembler.InterfaceBasedMBeanInfoAssembler}进行定义，
 *     将活动控制组件的可见性限制为Lifecycle接口。
 *
 * <p>
 *     请注意，当前的 {@code Lifecycle} 接口仅支持<b>顶级单例bean</b>。
 *     在任何其他组件上，{@code Lifecycle} 接口将保持未被检测状态，因此会被忽略。
 *     另外请注意，扩展的 {@link SmartLifecycle} 接口提供了与应用程序上下文启动和关闭阶段的复杂集成。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SmartLifecycle
 * @see ConfigurableApplicationContext
 * @see org.springframework.jms.listener.AbstractMessageListenerContainer
 * @see org.springframework.scheduling.quartz.SchedulerFactoryBean
 */
public interface Lifecycle {

	/**
	 * 启动该组件。
	 * <p>如果组件已在运行，不应抛出异常。
	 * <p>对于容器而言，这将把启动信号传播给所有适用的组件。
	 * @see SmartLifecycle#isAutoStartup()
	 */
	void start();

	/**
	 * 停止该组件，通常以同步方式进行，以便在方法返回时组件完全停止。
	 * 当需要异步停止行为时，考虑实现 {@link SmartLifecycle} 及其 {@code stop(Runnable)} 变体。
	 * <p>
	 *     请注意，不能保证停止通知在销毁之前到达：
	 *     在正常关闭时，{@code Lifecycle} bean 会在通用销毁回调传播之前首先收到停止通知；
	 *     但是，在上下文生命周期内的热刷新或中止的刷新尝试期间，给定 bean 的销毁方法将在不考虑停止信号的情况下被调用。
	 * <p>如果组件未运行（尚未启动），不应抛出异常。
	 * <p>对于容器而言，这将把停止信号传播给所有适用的组件。
	 * @see SmartLifecycle#stop(Runnable)
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	void stop();

	/**
	 * 检查该组件当前是否正在运行。
	 * <p>对于容器而言，只有当<i>所有</i>适用的组件当前都在运行时才会返回 {@code true}。
	 * @return 组件当前是否正在运行
	 */
	boolean isRunning();

}
