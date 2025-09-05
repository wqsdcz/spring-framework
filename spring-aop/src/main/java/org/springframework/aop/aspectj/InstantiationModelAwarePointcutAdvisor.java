/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.aop.aspectj;

import org.springframework.aop.PointcutAdvisor;

/**
 * <p>需由Spring AOP通知器实现的接口，用于封装可能采用懒初始化策略的AspectJ切面。例如，perThis实例化模型即表示通知的懒初始化。<p>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
public interface InstantiationModelAwarePointcutAdvisor extends PointcutAdvisor {

	/**
	 * 返回此通知器是否惰性初始化其底层的通知。
	 */
	boolean isLazy();

	/**
	 * 返回此通知器是否已经实例化了其底层的通知。
	 */
	boolean isAdviceInstantiated();

}
