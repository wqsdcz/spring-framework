/*
 * Copyright 2002-2018 the original author or authors.
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
 * <p>{@link Pointcut} 的组成部分：用于检查目标方法是否符合增强条件。</p>
 *
 * <p>
 *     方法匹配器（MethodMatcher）可分为<b>静态</b>匹配与<b>运行时</b>（动态）匹配两种模式。
 *     静态匹配关注方法本身及其（可能的）方法属性，而动态匹配还需考虑特定调用的实际参数， 以及应用于该连接点的前期增强所产生的任何状态影响。
 * </p>
 * <p>
 *     若实现类从其 {@link #isRuntime()} 方法返回 {@code false}，则可进行静态评估， 且对该方法的所有调用都将返回相同结果（无论参数如何变化）。
 *     这意味着当 {@link #isRuntime()} 返回 {@code false} 时，三参数方法 {@link #matches(java.lang.reflect.Method, Class, Object[])} 将永远不会被调用。
 * </p>
 * <p>
 *     若实现类从双参数方法 {@link #matches(java.lang.reflect.Method, Class)} 返回 {@code true}，且其 {@link #isRuntime()} 方法返回 {@code true}，
 *     则<b>在每次潜在增强执行前</b>都会调用三参数方法 {@link #matches(java.lang.reflect.Method, Class, Object[])} 以决定是否执行增强。
 *     此时所有前期增强 （例如拦截器链中的早期拦截器）都已执行，因此这些增强对参数或ThreadLocal状态造成的任何变更，在匹配评估时都处于可见状态。
 * </p>
 *
 * Part of a {@link Pointcut}: Checks whether the target method is eligible for advice.
 *
 * <p>A MethodMatcher may be evaluated <b>statically</b> or at <b>runtime</b> (dynamically).
 * Static matching involves method and (possibly) method attributes. Dynamic matching
 * also makes arguments for a particular call available, and any effects of running
 * previous advice applying to the joinpoint.
 *
 * <p>If an implementation returns {@code false} from its {@link #isRuntime()}
 * method, evaluation can be performed statically, and the result will be the same
 * for all invocations of this method, whatever their arguments. This means that
 * if the {@link #isRuntime()} method returns {@code false}, the 3-arg
 * {@link #matches(java.lang.reflect.Method, Class, Object[])} method will never be invoked.
 *
 * <p>If an implementation returns {@code true} from its 2-arg
 * {@link #matches(java.lang.reflect.Method, Class)} method and its {@link #isRuntime()} method
 * returns {@code true}, the 3-arg {@link #matches(java.lang.reflect.Method, Class, Object[])}
 * method will be invoked <i>immediately before each potential execution of the related advice</i>,
 * to decide whether the advice should run. All previous advice, such as earlier interceptors
 * in an interceptor chain, will have run, so any state changes they have produced in
 * parameters or ThreadLocal state will be available at the time of evaluation.
 *
 * @author Rod Johnson
 * @since 11.11.2003
 * @see Pointcut
 * @see ClassFilter
 */
public interface MethodMatcher {

	/**
	 * <p>对给定方法是否匹配执行静态检查。</p>
	 *
	 * <p>
	 *     若本方法返回 {@code false}，或 {@link #isRuntime()} 方法返回 {@code false}，
	 *     则将不会执行运行时检查（即不会调用 {@link #matches(java.lang.reflect.Method, Class, Object[])} 方法）。
	 * </p>
	 * @param method 待检测的候选方法
	 * @param targetClass 目标类（允许为 {@code null}，此时应将候选类视为方法的声明类）
	 * @return 此方法是否满足静态匹配条件
	 */
	boolean matches(Method method, @Nullable Class<?> targetClass);

	/**
	 * <p>
	 *     此MethodMatcher是否为动态匹配器？即当双参数匹配方法返回{@code true}时，是否仍必须在运行时对
	 *     {@link #matches(java.lang.reflect.Method, Class, Object[])}方法进行最终调用？
	 * </p>
	 * <p>该方法可在创建AOP代理时调用，且无需在每次方法调用前重复调用</p>
	 * @return 若静态匹配已通过，是否仍需通过三参数方法 {@link #matches(java.lang.reflect.Method, Class, Object[])}进行运行时匹配
	 */
	boolean isRuntime();

	/**
	 * <p>检查该方法是否存在运行时（动态）匹配，此方法必须已通过静态匹配。</p>
	 * <p>
	 *     仅当双参数匹配方法对给定方法和目标类返回 {@code true}，且 {@link #isRuntime()} 方法也返回 {@code true} 时，才会调用本方法。
	 *     该方法将在通知链中更早的增强被执行完毕后，更晚的增强被执行前，被调用。
	 * </p>
	 * @param method 待检测的候选方法
	 * @param targetClass 目标类（允许为 {@code null}，此时应将候选类视为方法的声明类）
	 * @param args 方法参数
	 * @return 是否存在运行时匹配
	 * @see MethodMatcher#matches(Method, Class)
	 */
	boolean matches(Method method, @Nullable Class<?> targetClass, Object... args);


	/**
	 * 匹配所有方法的规范化的实例。
	 */
	MethodMatcher TRUE = TrueMethodMatcher.INSTANCE;

}
