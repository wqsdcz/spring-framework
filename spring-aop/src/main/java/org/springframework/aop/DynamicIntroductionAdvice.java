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

import org.aopalliance.aop.Advice;

/**
 * <p>
 *     AOP联盟Advice接口的子接口，它允许一个通知（Advice）实现额外的接口，并通过使用该拦截器的代理使其可用。
 *     这一机制被称为<b>引入（introduction）</b>，是AOP的核心概念。
 * </p>
 * <p>引入通常表现为<b>混入（mixins）</b>模式，能够构建符合对象，从而实现Java中多重继承的诸多特性。</p>
 *
 * <p>
 *     与{@link IntroductionInfo}相比，此接口允许通知实现一系列未必预先定义的接口。
 *     因此{@link IntroductionAdvisor}可用于指定哪些接口将在被代理对象中暴露。
 * </p>
 *
 * @author Rod Johnson
 * @since 1.1.1
 * @see IntroductionInfo
 * @see IntroductionAdvisor
 */
public interface DynamicIntroductionAdvice extends Advice {

	/**
	 * 此【引入通知】是否可以实现指定接口？
	 * @param intf 要检查的接口
	 * @return 指示该【引入通知】可以实现指定接口
	 */
	boolean implementsInterface(Class<?> intf);

}
