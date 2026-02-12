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
 * 由希望在bean工厂中感知其bean名称的bean实现的接口。
 * 注意，通常不建议对象依赖于其bean名称，因为这代表了对外部配置的潜在脆弱依赖，以及对Spring API的可能不必要的依赖。
 *
 * <p>
 *     有关所有bean生命周期方法的列表，请参见{@link BeanFactory BeanFactory javadocs}。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 01.11.2003
 * @see BeanClassLoaderAware
 * @see BeanFactoryAware
 * @see InitializingBean
 */
public interface BeanNameAware extends Aware {

	/**
	 * 设置在创建此bean的bean工厂中的bean名称。
	 * <p>
	 *     在正常bean属性填充之后、初始化回调（如{@link InitializingBean#afterPropertiesSet()}或自定义init-method）之前调用。
	 *
	 * @param name 工厂中的bean名称。
	 * 注意，此名称是工厂中使用的实际bean名称，可能与最初指定的名称不同：
	 * 特别是对于内部bean名称，实际bean名称可能通过附加"#..."后缀使其唯一。
	 * 如果需要，可以使用{@link BeanFactoryUtils#originalBeanName(String)}方法提取原始bean名称（不带后缀）。
	 */
	void setBeanName(String name);

}
