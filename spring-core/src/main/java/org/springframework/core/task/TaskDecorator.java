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

package org.springframework.core.task;

/**
 * <p>这是一个回调接口，用于将装饰器应用于即将执行的任何 {@link Runnable} 。
 * <p>请注意，这样的装饰器不一定应用于用户提供的 {@code Runnable}/{@code Callable} ，而是应用于实际的执行回调（它可能是一个围绕用户提供的任务的包装器）。
 * <p>主要用途是围绕任务的调用设置一些执行上下文，或者为任务执行提供一些监控/统计信息。
 * <p><b>注意：</b> 在 {@code TaskDecorator} 实现中，异常处理可能有限。
 * 特别是对于基于 {@code Future} 的操作，暴露的 {@code Runnable} 将是一个包装器，不会传播其 {@code run} 方法中的任何异常。
 *
 * A callback interface for a decorator to be applied to any {@link Runnable}
 * about to be executed.
 *
 * <p>Note that such a decorator is not necessarily being applied to the
 * user-supplied {@code Runnable}/{@code Callable} but rather to the actual
 * execution callback (which may be a wrapper around the user-supplied task).
 *
 * <p>The primary use case is to set some execution context around the task's
 * invocation, or to provide some monitoring/statistics for task execution.
 *
 * <p><b>NOTE:</b> Exception handling in {@code TaskDecorator} implementations
 * may be limited. Specifically in case of a {@code Future}-based operation,
 * the exposed {@code Runnable} will be a wrapper which does not propagate
 * any exceptions from its {@code run} method.
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see TaskExecutor#execute(Runnable)
 * @see SimpleAsyncTaskExecutor#setTaskDecorator
 * @see org.springframework.core.task.support.TaskExecutorAdapter#setTaskDecorator
 */
@FunctionalInterface
public interface TaskDecorator {

	/**
	 * Decorate the given {@code Runnable}, returning a potentially wrapped
	 * {@code Runnable} for actual execution, internally delegating to the
	 * original {@link Runnable#run()} implementation.
	 * @param runnable the original {@code Runnable}
	 * @return the decorated {@code Runnable}
	 */
	Runnable decorate(Runnable runnable);

}
