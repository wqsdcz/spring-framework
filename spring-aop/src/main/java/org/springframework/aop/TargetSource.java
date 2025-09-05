/*<
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

package org.springframework.aop;

import org.springframework.lang.Nullable;

/**
 * <p>{@code TargetSource} 用于获取AOP调用的当前"目标对象"。若没有任何环绕增强选择终止拦截器链，则将通过反射机制调用该目标对象。</p>
 * <p>若{@code TargetSource}是"静态"的，它将始终返回相同目标对象，这使得AOP框架可以进行相关优化。动态目标源则支持池化、热交换等高级特性。</p>
 * <p>应用开发者通常无需直接操作{@code TargetSources}：这是AOP框架的内部接口。</p>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface TargetSource extends TargetClassAware {

	/**
	 * <p>返回此{@link TargetSource}所返回目标对象的类型。</p>
	 * <p>可能返回{@code null}，但某些{@link TargetSource}的使用场景可能仅需使用预定义的目标类。</p>
	 * @return 此目标源返回的目标类型
	 */
	@Override
	@Nullable
	Class<?> getTargetClass();

	/**
	 * <p>调用{@link #getTarget()}方法是否始终返回同一对象？</p>
	 *
	 * <p>若返回{@code true}，则无需调用{@link #releaseTarget(Object)}方法， 且AOP框架可对{@link #getTarget()}的返回值进行缓存。 </p>
	 * @return 若目标对象不可变则返回{@code true}
	 * @see #getTarget
	 * Will all calls to {@link #getTarget()} return the same object?
	 * <p>In that case, there will be no need to invoke {@link #releaseTarget(Object)},
	 * and the AOP framework can cache the return value of {@link #getTarget()}.
	 * @return {@code true} if the target is immutable
	 * @see #getTarget
	 */
	boolean isStatic();

	/**
	 * 返回目标实例。在 AOP 框架调用 AOP 方法的“目标”之前立即执行此操作。
	 * @return 包含连接点的目标对象，若不存在实际目标实例则返回 {@code null}
	 * @throws Exception 当目标对象无法被解析时抛出
	 */
	@Nullable
	Object getTarget() throws Exception;

	/**
	 * 释放通过{@link #getTarget()}方法获取的目标对象（如果存在）。
	 * @param target 通过调用{@link #getTarget()}方法获得的目标对象
	 * @throws Exception 当对象无法被释放时抛出
	 */
	void releaseTarget(Object target) throws Exception;

}
