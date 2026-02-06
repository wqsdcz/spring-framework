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

import java.io.Serializable;

import org.springframework.util.Assert;

/**
 * {@link TaskExecutor} implementation that executes each task <i>synchronously</i>
 * in the calling thread. Mainly intended for testing scenarios.
 *
 * <p>Execution in the calling thread does have the advantage of participating
 * in its thread context, for example the thread context class loader or the
 * thread's current transaction association. That said, in many cases,
 * asynchronous execution will be preferable: choose an asynchronous
 * {@code TaskExecutor} instead for such scenarios.
 *
 * {@link TaskExecutor}实现在调用线程中同步执行每个任务。主要用于测试场景。
 *
 * <p>在调用线程中执行的优势在于能够参与其线程上下文，
 * 例如线程上下文类加载器或线程的当前事务关联。
 * 话虽如此，在许多情况下，异步执行更为可取：
 * 在这种场景下选择异步的{@code TaskExecutor}。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SimpleAsyncTaskExecutor
 */
@SuppressWarnings("serial")
public class SyncTaskExecutor implements TaskExecutor, Serializable {

	/**
	 * Execute the given {@code task} synchronously, through direct
	 * invocation of its {@link Runnable#run() run()} method.
	 *
	 * 通过直接调用其{@link Runnable#run() run()}方法同步执行给定的{@code task}。
	 * @throws RuntimeException if propagated from the given {@code Runnable}
	 * @throws RuntimeException 如果从给定的{@code Runnable}传播而来
	 */
	@Override
	public void execute(Runnable task) {
		Assert.notNull(task, "Task must not be null");
		task.run();
	}

}
