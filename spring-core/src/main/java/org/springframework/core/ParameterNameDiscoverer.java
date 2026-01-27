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

package org.springframework.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.springframework.lang.Nullable;


/**
 * Interface to discover parameter names for methods and constructors.
 * <p>接口用于发现方法和构造函数的参数名称。</p>
 *
 * <p>Parameter name discovery is not always possible, but various strategies exist
 * &mdash; for example, using the JDK's reflection facilities for introspecting
 * parameter names (based on the "-parameters" compiler flag), looking for
 * {@code argNames} annotation attributes optionally configured for AspectJ
 * annotated methods, etc.
 * <p>参数名发现并不总是可行的，但是存在多种策略——例如，使用JDK的反射功能来检查参数名称（基于"-parameters"编译器标志），
 * 查找为AspectJ注解方法可选配置的{@code argNames}注解属性等。</p>
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0
 */
public interface ParameterNameDiscoverer {

	/**
	 * Return parameter names for a method, or {@code null} if they cannot be determined.
	 * <p>返回方法的参数名称，如果无法确定则返回 {@code null}。
	 * <p>Individual entries in the array may be {@code null} if parameter names are only
	 * available for some parameters of the given method but not for others. However,
	 * it is recommended to use stub parameter names instead wherever feasible.
	 * <p>如果参数名称只对给定方法的部分参数可用而并非全部，则数组中的各个条目可能是 {@code null}。然而，
	 * 建议尽可能使用存根参数名称。(stub parameter names 指的是占位符名称，如 "arg0", "arg1", "arg2" 等)
	 * @param method the method to find parameter names for
	 * <p>要查找参数名称的方法</p>
	 * @return an array of parameter names if the names can be resolved,
	 * or {@code null} if they cannot
	 * <p>如果可以解析参数名称则返回参数名称数组，否则返回 {@code null}</p>
	 */
	@Nullable
	String[] getParameterNames(Method method);

	/**
	 * Return parameter names for a constructor, or {@code null} if they cannot be determined.
	 * <p>返回构造函数的参数名称，如果无法确定则返回 {@code null}。</p>
	 * <p>Individual entries in the array may be {@code null} if parameter names are only
	 * available for some parameters of the given constructor but not for others. However,
	 * it is recommended to use stub parameter names instead wherever feasible.
	 * <p>如果参数名称只对给定构造函数的部分参数可用而并非全部，则数组中的各个条目可能是 {@code null}。然而，
	 * 建议尽可能使用存根参数名称。(stub parameter names 指的是占位符名称，如 "arg0", "arg1", "arg2" 等)</p>
	 * @param ctor the constructor to find parameter names for
	 * <p>要查找参数名称的构造函数</p>
	 * @return an array of parameter names if the names can be resolved,
	 * or {@code null} if they cannot
	 * <p>如果可以解析参数名称则返回参数名称数组，否则返回 {@code null}</p>
	 */
	@Nullable
	String[] getParameterNames(Constructor<?> ctor);

}
