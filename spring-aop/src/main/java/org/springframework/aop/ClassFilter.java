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

package org.springframework.aop;

/**
 * <p>将切点或引入限制在一组给定目标类中匹配的过滤器。</p>
 * <p>既可作为{@link Pointcut}的组成部分使用，也可用于确定{@link IntroductionAdvisor}的完整应用范围。</p>
 *
 * @author Rod Johnson
 * @see Pointcut
 * @see MethodMatcher
 */
@FunctionalInterface
public interface ClassFilter {

	/**
	 * 该切点是否应应用于给定的接口或目标类？
	 * @param clazz 候选目标类
	 * @return 增强是否应应用于给定的目标类
	 */
	boolean matches(Class<?> clazz);


	/**
	 * 匹配所有类的ClassFilter的规范实例。
	 */
	ClassFilter TRUE = TrueClassFilter.INSTANCE;

}
