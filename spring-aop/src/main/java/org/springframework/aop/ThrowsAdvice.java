/*
 * Copyright 2002-2008 the original author or authors.
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

/**
 * 异常通知（Throws Advice）的标记接口。
 *
 * <p>此接口未定义任何方法，因为方法通过反射机制调用。实现类必须遵循以下形式的方法：  </p>
 * <pre class="code">
 *     void afterThrowing([Method, args, target], ThrowableSubclass);
 * </pre>
 * <p>有效方法示例如下：
 * <pre class="code">
 *     public void afterThrowing(Exception ex)
 *  </pre>
 *  <pre class="code">
 *     public void afterThrowing(RemoteException)
 *  </pre>
 *  <pre class="code">
 *     public void afterThrowing(Method method, Object[] args, Object target, Exception ex)
 *  </pre>
 *  <pre class="code">
 *     public void afterThrowing(Method method, Object[] args, Object target, ServletException ex)
 *  </pre>
 *  前三个参数为可选参数，仅当需要获取连接点的额外信息时才有意义（与AspectJ<b>异常返回后通知</b>的机制类似）。
 *  </p>
 *  <p>
 *      <b>注意：</b>若异常通知方法自身抛出异常，该异常将覆盖原始异常（即最终抛给用户的异常会被替换）。
 *      覆盖异常通常应为RuntimeException类型，因其兼容所有方法签名；但若抛出受检异常，则必须与目标方法的声明异常相匹配，这意味着会与特定目标方法签名形成耦合。
 *      <b>切勿抛出与目标方法签名不兼容的未声明受检异常！</b>
 *  </p>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AfterReturningAdvice
 * @see MethodBeforeAdvice
 */
public interface ThrowsAdvice extends AfterAdvice {

}
