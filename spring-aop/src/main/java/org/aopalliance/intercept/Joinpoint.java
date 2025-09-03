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

import java.lang.reflect.AccessibleObject;

/**
 * <p>该接口表示的通用的【运行时连接点】（在AOP术语中）。</p>
 * <p>
 *     【运行时连接点】是指发生在【静态连接点】（即程序中的某个位置）上的一个<i>【事件】</i>。
 *     例如，方法调用（【运行时连接点】）是发生在方法（【静态连接点】）上的一个【事件】。
 *     通过调用{@link #getStaticPart()}方法可泛型获取指定连接点的静态部分。
 * </p>
 * <p>
 *     在拦截器框架上下文中，【运行时连接点】是对可访问对象（方法、构造函数、字段，即连接点的静态部分）访问操作的具象化。
 *     【运行时连接点】会被传递给安装在【静态连接点】上的拦截器。
 * </p>
 *
 * @author Rod Johnson
 * @see Interceptor
 */
public interface Joinpoint {

	/**
	 * <p>执行在链中的下一个拦截器。</p>
	 * <p>此方法的实现和语义取决于具体的连接点类型（参见子接口定义）。</p>
	 * @return 参考各子接口中proceed方法的定义
	 * @throws Throwable 当连接点抛出异常时
	 */
	Object proceed() throws Throwable;

	/**
	 * <p>返回持有当前连接点静态部分的对象。</p>
	 * <p>例如：方法调用的目标对象。</p>
	 * @return 该对象（若可访问对象为静态，则可能返回null）
	 */
	Object getThis();

	/**
	 * <p>返回此连接点的静态部分。</p>
	 * <p>静态部分是一个可访问对象，拦截器链会安装在该对象上。</p>
	 */
	AccessibleObject getStaticPart();

}
