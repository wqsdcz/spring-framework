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
 * 重点关联：切入点、方法匹配
 * <p>所有由切入点驱动的Advisor的父接口。</p>
 * <p>这涵盖了几乎所有的Advisor（除了IntroductionAdvisor，因为对于IntroductionAdvisor，方法级别的匹配不适用）。</p>
 *
 * @author Rod Johnson
 */
public interface PointcutAdvisor extends Advisor {

	/** 获取驱动此Advisor的切入点。*/
	Pointcut getPointcut();

}
