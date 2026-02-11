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

package org.springframework.beans;

import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;

/**
 * 在 beans 包及其子包中抛出的所有异常的抽象超类。
 *
 * <p>注意，这是运行时（非受检）异常。Beans 异常
 * 通常是致命的；没有理由让它们成为受检异常。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public abstract class BeansException extends NestedRuntimeException {

	/**
	 * 使用指定的消息创建新的 BeansException。
	 * @param msg 详细消息
	 */
	public BeansException(String msg) {
		super(msg);
	}

	/**
	 * 使用指定的消息和根本原因创建新的 BeansException。
	 * @param msg 详细消息
	 * @param cause 根本原因
	 */
	public BeansException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
