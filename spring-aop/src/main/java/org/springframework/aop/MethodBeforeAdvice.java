/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop;

import java.lang.reflect.Method;

import org.springframework.lang.Nullable;

/**
 * 在方法被调用前执行的通知。此类通知无法阻止方法调用的继续执行，除非它们抛出异常。
 *
 * @see AfterReturningAdvice
 * @see ThrowsAdvice
 *
 * @author Rod Johnson
 */
public interface MethodBeforeAdvice extends BeforeAdvice {

	/**
	 * 在给定方法被调用前执行的回调。
	 * @param method 正在被调用的方法
	 * @param args 方法的参数
	 * @param target 方法调用的目标对象。允许为{@code null}
	 * @throws Throwable 如果该对象希望中止调用。抛出的任何异常都将返回给调用者（若方法签名允许），否则异常将被包装为运行时异常
	 */
	void before(Method method, Object[] args, @Nullable Object target) throws Throwable;

}
