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

import java.util.concurrent.ThreadFactory;

/**
 * A {@link TaskExecutor} implementation based on virtual threads in JDK 21+.
 * The only configuration option is a thread name prefix.
 *
 * <p>For additional features such as concurrency limiting or task decoration,
 * consider using {@link SimpleAsyncTaskExecutor#setVirtualThreads} instead.
 *
 * 基于JDK 21+中虚拟线程的{@link TaskExecutor}实现。
 * 唯一的配置选项是线程名称前缀。
 *
 * <p>对于并发限制或任务装饰等附加功能，
 * 请考虑使用{@link SimpleAsyncTaskExecutor#setVirtualThreads}。
 *
 * @author Juergen Hoeller
 * @since 6.1
 * @see SimpleAsyncTaskExecutor#setVirtualThreads
 */
public class VirtualThreadTaskExecutor implements AsyncTaskExecutor {

	private final ThreadFactory virtualThreadFactory;


	/**
	 * Create a new {@code VirtualThreadTaskExecutor} without thread naming.
	 *
	 * 创建不带线程命名的新{@code VirtualThreadTaskExecutor}。
	 */
	public VirtualThreadTaskExecutor() {
		this.virtualThreadFactory = new VirtualThreadDelegate().virtualThreadFactory();
	}

	/**
	 * Create a new {@code VirtualThreadTaskExecutor} with thread names based
	 * on the given thread name prefix followed by a counter (for example, "test-0").
	 *
	 * 创建基于给定线程名称前缀后跟计数器的新{@code VirtualThreadTaskExecutor}
	 * （例如，"test-0"）。
	 * @param threadNamePrefix the prefix for thread names (for example, "test-")
	 * @param threadNamePrefix 线程名称的前缀（例如，"test-"）
	 */
	public VirtualThreadTaskExecutor(String threadNamePrefix) {
		this.virtualThreadFactory = new VirtualThreadDelegate().virtualThreadFactory(threadNamePrefix);
	}


	/**
	 * Return the underlying virtual {@link ThreadFactory}.
	 * Can also be used for custom thread creation elsewhere.
	 *
	 * 返回底层的虚拟{@link ThreadFactory}。
	 * 也可用于其他地方的自定义线程创建。
	 */
	public final ThreadFactory getVirtualThreadFactory() {
		return this.virtualThreadFactory;
	}

	@Override
	public void execute(Runnable task) {
		this.virtualThreadFactory.newThread(task).start();
	}

}
