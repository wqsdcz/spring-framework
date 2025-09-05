/*
 * Copyright 2002-2017 the original author or authors.
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

import org.aopalliance.aop.Advice;

/**
 * <p>
 *     持有AOP通知和AOP切入点的基础接口。
 *     <ol>
 *         <li>AOP通知: 在连接点执行的操作</li>
 *         <li>AOP切入点: 确定通知适用范围的过滤器</li>
 *     </ol>
 *     <i>本接口并非为了供Spring用户直接使用，而是为了给不同类型通知提供通用性。</i>
 * </p>
 * <p>
 *     Spring AOP的核心是通过方法<b>拦截</b>实现的<b>环绕通知</b>， 其符合AOP联盟拦截API。
 *     Advisor接口允许支持多种类型的通知， 例如：<b>前置通知</b>和<b>后置通知</b>（这些通知无需通过拦截方式实现）。
 * </p>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface Advisor {

	/**
	 * 当尚未配置任何有效的通知时，用于在调用 {@link #getAdvice()} 时，返回一个空含义的 {@code Advice} 的通用占位符。
	 * @since 5.0
	 */
	Advice EMPTY_ADVICE = new Advice() {};


	/**
	 * 返回此切面中的通知部分。通知可以是拦截器、前置通知、异常通知等。
	 *
	 * @return 当切入点匹配时应应用的通知
	 * @see org.aopalliance.intercept.MethodInterceptor
	 * @see BeforeAdvice
	 * @see ThrowsAdvice
	 * @see AfterReturningAdvice
	 */
	Advice getAdvice();

	/**
	 * 返回此通知是否与特定实例关联（例如：创建Mixin）。
	 * 如果不是，那么此通知被从同一Spring bean工厂获取的目标类的所有实例所共享。
	 *
	 * <p>
	 *     <b>请注意：框架当前并未使用此方法。</b>
	 *     典型的Advisor实现总是返回{@code true}。
	 *     请通过单例/原型bean定义或适当的编程代理的创建方式，确保Advisor具有正确的生命周期模型。
	 * </p>
	 * @return 此通知是否与特定目标实例关联
	 */
	boolean isPerInstance();

}
