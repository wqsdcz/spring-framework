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

/**
 * <p>核心的Spring切点抽象。</p>
 * <p>
 *     一个切点由{@link ClassFilter}（类过滤器）和{@link MethodMatcher}（方法匹配器）组成。
 *     这些基本术语与切点本身（ClassFilter, MethodMatcher, Pointcut）可以通过组合方式构建复杂规则
 *     （例如通过{@link org.springframework.aop.support.ComposablePointcut}实现）。
 *     译者注：支持通过组合模式来灵活地定义非常精确和复杂的拦截规则。
 * </p>
 *
 * @author Rod Johnson
 * @see ClassFilter
 * @see MethodMatcher
 * @see org.springframework.aop.support.Pointcuts
 * @see org.springframework.aop.support.ClassFilters
 * @see org.springframework.aop.support.MethodMatchers
 */
public interface Pointcut {

	/**
	 * 返回这个切入点的 ClassFilter
	 * @return ClassFilter（从不{@code null}）
	 */
	ClassFilter getClassFilter();

	/**
	 * 返回这个切入点的 MethodMatcher
	 * @return MethodMatcher（从不{@code null}）
	 */
	MethodMatcher getMethodMatcher();


	/**
	 * 这是一个规范化切入点实例，该实例总能匹配成功的。
	 */
	Pointcut TRUE = TruePointcut.INSTANCE;

}
