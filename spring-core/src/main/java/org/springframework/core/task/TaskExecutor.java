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

import java.util.concurrent.Executor;

/**
 * Simple task executor interface that abstracts the execution
 * of a {@link Runnable}.
 *
 * <p>Implementations can use all sorts of different execution strategies,
 * such as: synchronous, asynchronous, using a thread pool, and more.
 *
 * <p>Equivalent to Java's {@link java.util.concurrent.Executor} interface,
 * so that clients may declare a dependency on an {@code Executor} and receive
 * any {@code TaskExecutor} implementation. This interface remains separate from
 * the standard {@code Executor} interface primarily for backwards compatibility
 * with older APIs that depend on the {@code TaskExecutor} interface.
 *
 * <p>简单的任务执行器接口，抽象了{@link Runnable}的执行。
 *
 * <p>实现可以使用各种不同的执行策略，
 * 例如：同步、异步、使用线程池等。
 *
 * <p>等同于Java的{@link java.util.concurrent.Executor}接口，
 * 使得客户端可以声明对{@code Executor}的依赖并接收
 * 任何{@code TaskExecutor}实现。此接口与标准的{@code Executor}接口保持分离，
 * 主要是为了向后兼容依赖{@code TaskExecutor}接口的旧API。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see java.util.concurrent.Executor
 */
@FunctionalInterface
public interface TaskExecutor extends Executor {

	/**
	 * Execute the given {@code task}.
	 * <p>The call might return immediately if the implementation uses
	 * an asynchronous execution strategy, or might block in the case
	 * of synchronous execution.
	 *
	 * <p>执行给定的{@code task}。
	 * <p>如果实现使用异步执行策略，调用可能会立即返回，
	 * 或者在同步执行的情况下可能会阻塞。
	 * @param task 要执行的{@code Runnable}（永不为{@code null}）
	 * @throws TaskRejectedException 如果给定任务未被接受
	 */
	@Override
	void execute(Runnable task);

}
