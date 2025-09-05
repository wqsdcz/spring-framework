/*
 * Copyright 2002-2013 the original author or authors.
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
 * <p>
 *     一种特殊类型的{@link MethodMatcher}，其在匹配方法时会纳入引入（introductions）的考量。
 *     例如，若目标类上未定义任何引入，方法匹配器则能够实现更高效的匹配优化。
 * </p>
 *
 * A specialized type of {@link MethodMatcher} that takes into account introductions
 * when matching methods. If there are no introductions on the target class,
 * a method matcher may be able to optimize matching more effectively for example.
 *
 * @author Adrian Colyer
 * @since 2.0
 */
public interface IntroductionAwareMethodMatcher extends MethodMatcher {

	/**
	 * 对给定方法是否匹配执行静态检查。当调用方支持扩展的 IntroductionAwareMethodMatcher 接口时，
	 * 可能会调用本方法替代双参数方法 {@link #matches(java.lang.reflect.Method, Class)}。
	 * @param method 待检测的候选方法
	 * @param targetClass 目标类（允许为 {@code null}，此时应将候选类视为方法的声明类）
	 * @param hasIntroductions 若当前调用对象承载了一个或多个引入，则该值为 {@code true}；否则为 {@code false}
	 * @return 此方法是否满足静态匹配条件
	 */
	boolean matches(Method method, @Nullable Class<?> targetClass, boolean hasIntroductions);

}
