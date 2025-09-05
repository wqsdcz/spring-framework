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
 * 重点关联：目标类、额外接口、类匹配
 * <p>用于执行一个或多个AOP<b>引入操作</b>的通知器的超级接口。</p>
 * <p>此接口不能直接实现；子接口必须提供实现引入功能的通知类型。</p>
 * <p>引入是通过AOP通知实现额外接口（目标类本身未实现的接口）的增强方式。</p>
 *
 * @author Rod Johnson
 * @since 04.04.2003
 * @see IntroductionInterceptor
 */
public interface IntroductionAdvisor extends Advisor, IntroductionInfo {

	/**
	 * <p>返回确定此引入应应用于哪些目标类的过滤器。</p>
	 * <p>这代表了切入点中的类匹配部分。请注意，【方法匹配】对【引入】来说没有实际意义。</p>
	 * @return ClassFilter实例
	 */
	ClassFilter getClassFilter();

	/**
	 * <p>校验【引入通知】能否实现【被通知的接口】？</p>
	 * <p>该方法在添加IntroductionAdvisor之前调用。</p>
	 * @throws IllegalArgumentException 如果【被通知的接口】无法由【引入通知】实现
	 */
	void validateInterfaces() throws IllegalArgumentException;

}
