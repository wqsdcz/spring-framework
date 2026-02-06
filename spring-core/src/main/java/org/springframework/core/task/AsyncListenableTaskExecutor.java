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

import java.util.concurrent.Callable;

/**
 * Extension of the {@link AsyncTaskExecutor} interface, adding the capability to submit
 * tasks for {@code ListenableFutures}.
 *
 * {@link AsyncTaskExecutor}接口的扩展，增加了提交任务以获取
 * {@code ListenableFutures}的能力。
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @deprecated as of 6.0, in favor of
 * {@link AsyncTaskExecutor#submitCompletable(Runnable)} and
 * {@link AsyncTaskExecutor#submitCompletable(Callable)}
 */
@Deprecated(since = "6.0", forRemoval = true)
@SuppressWarnings("removal")
public interface AsyncListenableTaskExecutor extends AsyncTaskExecutor {

	/**
	 * Submit a {@code Runnable} task for execution, receiving a {@code ListenableFuture}
	 * representing that task. The Future will return a {@code null} result upon completion.
	 *
	 * 提交{@code Runnable}任务以执行，接收代表该任务的{@code ListenableFuture}。
	 * Future将在完成时返回{@code null}结果。
	 * @param task the {@code Runnable} to execute (never {@code null})
	 * @param task 要执行的{@code Runnable}（永不为{@code null}）
	 * @return a {@code ListenableFuture} representing pending completion of the task
	 * @return 代表任务待完成的{@code ListenableFuture}
	 * @throws TaskRejectedException if the given task was not accepted
	 * @throws TaskRejectedException 如果给定任务未被接受
	 * @deprecated in favor of {@link AsyncTaskExecutor#submitCompletable(Runnable)}
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	org.springframework.util.concurrent.ListenableFuture<?> submitListenable(Runnable task);

	/**
	 * Submit a {@code Callable} task for execution, receiving a {@code ListenableFuture}
	 * representing that task. The Future will return the Callable's result upon
	 * completion.
	 *
	 * 提交{@code Callable}任务以执行，接收代表该任务的{@code ListenableFuture}。
	 * Future将在完成时返回Callable的结果。
	 * @param task the {@code Callable} to execute (never {@code null})
	 * @param task 要执行的{@code Callable}（永不为{@code null}）
	 * @return a {@code ListenableFuture} representing pending completion of the task
	 * @return 代表任务待完成的{@code ListenableFuture}
	 * @throws TaskRejectedException if the given task was not accepted
	 * @throws TaskRejectedException 如果给定任务未被接受
	 * @deprecated in favor of {@link AsyncTaskExecutor#submitCompletable(Callable)}
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	<T> org.springframework.util.concurrent.ListenableFuture<T> submitListenable(Callable<T> task);

}
