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

import java.io.Serializable;

/**
 * 这是一个规范化切入点实例，该实例总能匹配成功的。
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
class TruePointcut implements Pointcut, Serializable {

	public static final TruePointcut INSTANCE = new TruePointcut();

	/**
	 * 私有的构造器——强制单例模式。
	 */
	private TruePointcut() {
	}

	@Override
	public ClassFilter getClassFilter() {
		return ClassFilter.TRUE;
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		return MethodMatcher.TRUE;
	}

	/**
	 * 支持序列化所必需。在反序列化时替换为规范实例，从而确保单例模式不被破坏。
	 * 这是重写 {@code equals()} 方法的一种替代方案。
	 */
	private Object readResolve() {
		return INSTANCE;
	}

	@Override
	public String toString() {
		return "Pointcut.TRUE";
	}

}
