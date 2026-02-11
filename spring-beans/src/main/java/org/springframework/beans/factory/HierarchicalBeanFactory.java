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

import org.springframework.lang.Nullable;

/**
 * 层次bean工厂接口，扩展{@link BeanFactory}。
 * 如果一个bean工厂实现了此接口，则表示该工厂是层次结构中的子工厂。
 *
 * <p>
 *     允许以可配置方式设置父级的bean工厂（对应的{@code setParentBeanFactory}方法），可以在ConfigurableBeanFactory接口中找到。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 07.07.2003
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#setParentBeanFactory
 */
public interface HierarchicalBeanFactory extends BeanFactory {

	/**
	 * 获取父级bean工厂，如果没有，则返回{@code null}。
	 */
	@Nullable
	BeanFactory getParentBeanFactory();

	/**
	 * 判断本地bean工厂是否包含给定名称的bean，忽略在祖先上下文中定义的bean。
	 * <p>
	 *     这是{@code containsBean}的替代方法，忽略来自祖先bean工厂的同名bean。
	 *
	 * @param name 要查询的bean的名称
	 * @return 本地工厂中是否定义了具有给定名称的bean
	 * @see BeanFactory#containsBean
	 */
	boolean containsLocalBean(String name);

}
