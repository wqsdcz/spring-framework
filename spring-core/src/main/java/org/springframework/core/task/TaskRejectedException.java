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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * Exception thrown when a {@link TaskExecutor} rejects to accept
 * a given task for execution.
 *
 * 当{@link TaskExecutor}拒绝接受给定任务执行时抛出的异常。
 *
 * @author Juergen Hoeller
 * @since 2.0.1
 * @see TaskExecutor#execute(Runnable)
 */
@SuppressWarnings("serial")
public class TaskRejectedException extends RejectedExecutionException {

	/**
	 * Create a new {@code TaskRejectedException}
	 * with the specified detail message and no root cause.
	 *
	 * 使用指定的详细消息和无根本原因创建新的{@code TaskRejectedException}。
	 * @param msg the detail message
	 * @param msg 详细消息
	 */
	public TaskRejectedException(String msg) {
		super(msg);
	}

	/**
	 * Create a new {@code TaskRejectedException}
	 * with the specified detail message and the given root cause.
	 *
	 * 使用指定的详细消息和给定的根本原因创建新的{@code TaskRejectedException}。
	 * @param msg the detail message
	 * @param msg 详细消息
	 * @param cause the root cause (usually from using an underlying
	 * API such as the {@code java.util.concurrent} package)
	 * @param cause 根本原因（通常来自使用底层API，如{@code java.util.concurrent}包）
	 * @see java.util.concurrent.RejectedExecutionException
	 */
	public TaskRejectedException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * Create a new {@code TaskRejectedException}
	 * with a default message for the given executor and task.
	 *
	 * 为给定的执行器和任务创建带有默认消息的新{@code TaskRejectedException}。
	 * @param executor the {@code Executor} that rejected the task
	 * @param executor 拒绝任务的{@code Executor}
	 * @param task the task object that got rejected
	 * @param task 被拒绝的任务对象
	 * @param cause the original {@link RejectedExecutionException}
	 * @param cause 原始的{@link RejectedExecutionException}
	 * @since 6.1
	 * @see ExecutorService#isShutdown()
	 * @see java.util.concurrent.RejectedExecutionException
	 */
	public TaskRejectedException(Executor executor, Object task, RejectedExecutionException cause) {
		super(executorDescription(executor) + " did not accept task: " + task, cause);
	}


	private static String executorDescription(Executor executor) {
		if (executor instanceof ExecutorService executorService) {
			try {
				return "ExecutorService in " + (executorService.isShutdown() ? "shutdown" : "active") + " state";
			}
			catch (Exception ex) {
				// UnsupportedOperationException/IllegalStateException from ManagedExecutorService.isShutdown()
				// Falling back to toString() below.
			}
		}
		return executor.toString();
	}

}
