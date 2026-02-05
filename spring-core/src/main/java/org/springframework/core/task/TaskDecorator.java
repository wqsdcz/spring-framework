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

package org.springframework.core.task;

/**
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
 * <p>应用于即将执行的任何{@link Runnable}的装饰器回调接口。
 *
 * <p>请注意，这样的装饰器不一定应用于用户提供的
 * {@code Runnable}/{@code Callable}，而是应用于实际的
 * 执行回调（可能是围绕用户提供的任务的包装器）。
 *
 * <p>主要用例是在任务调用周围设置一些执行上下文，
 * 或为任务执行提供一些监控/统计信息。
 *
 * <p><b>注意：</b>{@code TaskDecorator}实现中的异常处理
 * 可能有限。特别是在基于{@code Future}的操作中，
 * 暴露的{@code Runnable}将是一个不传播其
 * {@code run}方法中任何异常的包装器。
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
	 *
	 * <p>装饰给定的{@code Runnable}，返回可能包装的
	 * {@code Runnable}用于实际执行，内部委托给
	 * 原始的{@link Runnable#run()}实现。
	 * @param runnable 原始的{@code Runnable}
	 * @return 装饰后的{@code Runnable}
	 */
	Runnable decorate(Runnable runnable);

}
