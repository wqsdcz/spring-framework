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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.springframework.util.concurrent.FutureUtils;

/**
 * Extended interface for asynchronous {@link TaskExecutor} implementations,
 * offering support for {@link java.util.concurrent.Callable}.
 *
 * <p>Note: The {@link java.util.concurrent.Executors} class includes a set of
 * methods that can convert some other common closure-like objects, for example,
 * {@link java.security.PrivilegedAction} to {@link Callable} before executing them.
 *
 * <p>Implementing this interface also indicates that the {@link #execute(Runnable)}
 * method will not execute its Runnable in the caller's thread but rather
 * asynchronously in some other thread.
 *
 * <p>异步{@link TaskExecutor}实现的扩展接口，
 * 提供对{@link java.util.concurrent.Callable}的支持。
 *
 * <p>注意：{@link java.util.concurrent.Executors}类包含一组
 * 可以在执行之前将其他常见闭包类对象转换为{@link Callable}的方法，
 * 例如，{@link java.security.PrivilegedAction}。
 *
 * <p>实现此接口还表明{@link #execute(Runnable)}
 * 方法不会在调用者的线程中执行其Runnable，
 * 而是在其他某个线程中异步执行。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see SimpleAsyncTaskExecutor
 * @see org.springframework.scheduling.SchedulingTaskExecutor
 * @see java.util.concurrent.Callable
 * @see java.util.concurrent.Executors
 */
public interface AsyncTaskExecutor extends TaskExecutor {

	/**
	 * Constant that indicates immediate execution.
	 *
	 * <p>表示立即执行的常量。
	 * @deprecated as of 5.3.16 along with {@link #execute(Runnable, long)}
	 */
	@Deprecated
	long TIMEOUT_IMMEDIATE = 0;

	/**
	 * Constant that indicates no time limit.
	 *
	 * <p>表示无时间限制的常量。
	 * @deprecated as of 5.3.16 along with {@link #execute(Runnable, long)}
	 */
	@Deprecated
	long TIMEOUT_INDEFINITE = Long.MAX_VALUE;


	/**
	 * Execute the given {@code task}.
	 * <p>As of 6.1, this method comes with a default implementation that simply
	 * delegates to {@link #execute(Runnable)}, ignoring the timeout completely.
	 * @param task the {@code Runnable} to execute (never {@code null})
	 * @param startTimeout the time duration (milliseconds) within which the task is
	 * supposed to start. This is intended as a hint to the executor, allowing for
	 * preferred handling of immediate tasks. Typical values are {@link #TIMEOUT_IMMEDIATE}
	 * or {@link #TIMEOUT_INDEFINITE} (the default as used by {@link #execute(Runnable)}).
	 * @throws TaskTimeoutException in case of the task being rejected because
	 * of the timeout (i.e. it cannot be started in time)
	 * @throws TaskRejectedException if the given task was not accepted
	 * @see #execute(Runnable)
	 * @deprecated as of 5.3.16 since the common executors do not support start timeouts
	 */
	@Deprecated
	default void execute(Runnable task, long startTimeout) {
		execute(task);
	}

	/**
	 * Submit a Runnable task for execution, receiving a Future representing that task.
	 * The Future will return a {@code null} result upon completion.
	 * <p>As of 6.1, this method comes with a default implementation that delegates
	 * to {@link #execute(Runnable)}.
	 *
	 * <p>提交Runnable任务以执行，接收代表该任务的Future。
	 * Future将在完成时返回{@code null}结果。
	 * <p>从6.1开始，此方法带有默认实现，委托给{@link #execute(Runnable)}。
	 * @param task 要执行的{@code Runnable}（永不为{@code null}）
	 * @return 代表任务待完成的Future
	 * @throws TaskRejectedException 如果给定任务未被接受
	 * @since 3.0
	 */
	default Future<?> submit(Runnable task) {
		FutureTask<Object> future = new FutureTask<>(task, null);
		execute(future);
		return future;
	}

	/**
	 * Submit a Callable task for execution, receiving a Future representing that task.
	 * The Future will return the Callable's result upon completion.
	 * <p>As of 6.1, this method comes with a default implementation that delegates
	 * to {@link #execute(Runnable)}.
	 *
	 * <p>提交Callable任务以执行，接收代表该任务的Future。
	 * Future将在完成时返回Callable的结果。
	 * <p>从6.1开始，此方法带有默认实现，委托给{@link #execute(Runnable)}。
	 * @param task 要执行的{@code Callable}（永不为{@code null}）
	 * @return 代表任务待完成的Future
	 * @throws TaskRejectedException 如果给定任务未被接受
	 * @since 3.0
	 */
	default <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<>(task);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	/**
	 * Submit a {@code Runnable} task for execution, receiving a {@code CompletableFuture}
	 * representing that task. The Future will return a {@code null} result upon completion.
	 *
	 * <p>提交{@code Runnable}任务以执行，接收代表该任务的{@code CompletableFuture}。
	 * Future将在完成时返回{@code null}结果。
	 * @param task 要执行的{@code Runnable}（永不为{@code null}）
	 * @return 代表任务待完成的{@code CompletableFuture}
	 * @throws TaskRejectedException 如果给定任务未被接受
	 * @since 6.0
	 */
	default CompletableFuture<Void> submitCompletable(Runnable task) {
		return CompletableFuture.runAsync(task, this);
	}

	/**
	 * Submit a {@code Callable} task for execution, receiving a {@code CompletableFuture}
	 * representing that task. The Future will return the Callable's result upon
	 * completion.
	 *
	 * <p>提交{@code Callable}任务以执行，接收代表该任务的{@code CompletableFuture}。
	 * Future将在完成时返回Callable的结果。
	 * @param task 要执行的{@code Callable}（永不为{@code null}）
	 * @return 代表任务待完成的{@code CompletableFuture}
	 * @throws TaskRejectedException 如果给定任务未被接受
	 * @since 6.0
	 */
	default <T> CompletableFuture<T> submitCompletable(Callable<T> task) {
		return FutureUtils.callAsync(task, this);
	}

}
