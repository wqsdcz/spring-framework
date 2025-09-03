/*
 * Copyright 2002-2016 the original author or authors.
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

package org.aopalliance.intercept;

/**
 * <p>拦截新对象的构造过程。</p>
 * <p>
 *     用户需要通过实现{@link #construct(ConstructorInvocation)}方法来修改原始行为。
 *     例如下列实现单例拦截器的类（确保被拦截类只能创建唯一实例）：
 * </p>
 * <pre class=code>
 *     class DebuggingInterceptor implements ConstructorInterceptor {
 *
 *         Object instance = null;
 *
 *         Object construct(ConstructorInvocation i) throws Throwable {
 *             if(instance == null) {
 *                 return instance = i.proceed();
 *             } else {
 *                 throw new Exception("singleton does not allow multiple instance");
 *             }
 *         }
 *     }
 * </pre>
 *
 * @author Rod Johnson
 */
public interface ConstructorInterceptor extends Interceptor  {

	/**
	 * 实现此方法可在新对象构造前后执行额外处理。规范的实现通常需要调用{@link Joinpoint#proceed()}方法。
	 * @param invocation 构造连接点
	 * @return 新创建的对象（该对象同时也是调用{@link Joinpoint#proceed()}的结果），拦截器可能替换该返回值
	 * @throws Throwable 当拦截器或目标对象抛出异常时
	 */
	Object construct(ConstructorInvocation invocation) throws Throwable;

}
