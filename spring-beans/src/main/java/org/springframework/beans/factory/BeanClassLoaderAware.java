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
 * 指示bean有资格由Spring容器通过回调式方法以参数形式传入 {@link ClassLoader bean的类加载器} 的方式
 * 让bean感知到 {@link ClassLoader bean的类加载器}。
 * 即，当前bean工厂用于加载bean类的类加载器。
 *
 * <p>
 *     这主要用于由框架类实现，这些类必须通过名称获取应用程序类，
 *     尽管它们本身可能是从共享类加载器加载的。
 *
 * <p>有关所有bean生命周期方法的列表，请参见 {@link BeanFactory BeanFactory javadocs}。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.0
 * @see BeanNameAware
 * @see BeanFactoryAware
 * @see InitializingBean
 */
public interface BeanClassLoaderAware extends Aware {

	/**
	 * 向bean实例提供 {@link ClassLoader bean的类加载器} 的回调。
	 * <p>
	 *     在正常bean属性填充之后、初始化回调（如{@link InitializingBean#afterPropertiesSet()}或自定义init-method）之前调用。
	 *
	 * @param classLoader 所属的类加载器
	 */
	void setBeanClassLoader(ClassLoader classLoader);

}
