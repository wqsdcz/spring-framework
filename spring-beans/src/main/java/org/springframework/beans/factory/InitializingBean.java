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

package org.springframework.beans.factory;

/**
 * 由需要在{@link BeanFactory}设置完所有属性后做出反应的bean实现的接口：
 * 例如，执行自定义初始化，或仅仅检查所有必需属性是否已设置。
 *
 * <p>实现{@code InitializingBean}的替代方案是指定自定义
 * 初始化方法，例如在XML bean定义中。有关所有bean
 * 生命周期方法的列表，请参见{@link BeanFactory BeanFactory javadocs}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see DisposableBean
 * @see org.springframework.beans.factory.config.BeanDefinition#getPropertyValues()
 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getInitMethodName()
 */
public interface InitializingBean {

	/**
	 * 在包含的{@code BeanFactory}设置了所有bean属性并满足
	 * {@link BeanFactoryAware}、{@code ApplicationContextAware}等之后由其调用。
	 * <p>此方法允许bean实例对其整体配置进行验证
	 * 并在设置完所有bean属性后进行最终初始化。
	 * @throws Exception 如果配置错误（例如未能设置必要属性）
	 * 或因任何其他原因初始化失败
	 */
	void afterPropertiesSet() throws Exception;

}
