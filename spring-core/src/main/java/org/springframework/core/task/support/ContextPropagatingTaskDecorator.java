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

package org.springframework.core.task.support;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;

import org.springframework.core.task.TaskDecorator;

/**
 * {@link TaskDecorator} that {@link ContextSnapshot#wrap(Runnable) wraps the execution}
 * of tasks, assisting with context propagation.
 *
 * <p>This operation is only useful when the task execution is scheduled on a different
 * thread than the original call stack; this depends on the choice of
 * {@link org.springframework.core.task.TaskExecutor}. This is particularly useful for
 * restoring a logging context or an observation context for the task execution. Note that
 * this decorator will cause some overhead for task execution and is not recommended for
 * applications that run lots of very small tasks.
 *
 * {@link TaskDecorator}，通过{@link ContextSnapshot#wrap(Runnable) wrap execution}任务，
 * 协助上下文传播。
 *
 * <p>此操作仅在任务执行被安排在与原始调用堆栈不同的线程上时才有用；
 * 这取决于{@link org.springframework.core.task.TaskExecutor}的选择。
 * 这对于恢复任务执行的日志上下文或观察上下文特别有用。
 * 请注意，此装饰器会导致任务执行的一些开销，
 * 不建议用于运行大量非常小任务的应用程序。
 *
 * @author Brian Clozel
 * @since 6.1
 * @see CompositeTaskDecorator
 */
public class ContextPropagatingTaskDecorator implements TaskDecorator {

	private final ContextSnapshotFactory factory;


	/**
	 * Create a new decorator that uses a default instance of the {@link ContextSnapshotFactory}.
	 *
	 * 创建使用{@link ContextSnapshotFactory}默认实例的新装饰器。
	 */
	public ContextPropagatingTaskDecorator() {
		this(ContextSnapshotFactory.builder().build());
	}

	/**
	 * Create a new decorator using the given {@link ContextSnapshotFactory}.
	 *
	 * 使用给定的{@link ContextSnapshotFactory}创建新装饰器。
	 * @param factory the context snapshot factory to use.
	 * @param factory 要使用的上下文快照工厂。
	 */
	public ContextPropagatingTaskDecorator(ContextSnapshotFactory factory) {
		this.factory = factory;
	}


	@Override
	public Runnable decorate(Runnable runnable) {
		return this.factory.captureAll().wrap(runnable);
	}

}
