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
 * <p>拦截发往目标接口的调用。这些调用会以嵌套形式作用于目标对象之上。</p>
 * <p>
 *     用户需要通过实现{@link #invoke(MethodInvocation)}方法来修改原始行为。
 *     例如下列实现追踪拦截器的类（记录被拦截方法的所有调用信息）：
 * </p>
 * <pre class=code>
 *     class TracingInterceptor implements MethodInterceptor {
 *         Object invoke(MethodInvocation i) throws Throwable {
 *             System.out.println("方法 "+i.getMethod()+" 被 "+i.getThis()+" 调用，参数为 "+i.getArguments());
 *             Object ret=i.proceed();
 *             System.out.println("方法 "+i.getMethod()+" 返回 "+ret);
 *             return ret;
 *         }
 *     }
 * </pre>
 *
 * @author Rod Johnson
 */
@FunctionalInterface
public interface MethodInterceptor extends Interceptor {
	
	/**
	 * 实现此方法可在方法调用前后执行额外处理。规范的实现通常需要调用{@link Joinpoint#proceed()}方法。
	 * @param invocation 方法调用连接点
	 * @return 调用 {@link Joinpoint#proceed()} 的结果，该结果可能被拦截器替换
	 * @throws Throwable 当拦截器或目标对象抛出异常时
	 */
	Object invoke(MethodInvocation invocation) throws Throwable;

}
