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

package org.springframework.beans.factory.config;

import java.util.Iterator;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.lang.Nullable;

/**
 * 大多数可列出的bean工厂需要实现的配置接口。
 * 除了{@link ConfigurableBeanFactory}之外，它还提供了分析和修改bean定义，以及预实例化单例对象的功能。
 *
 * <p>
 *     这个{@link org.springframework.beans.factory.BeanFactory}的子接口并不打算在普通应用程序代码中使用：
 *     在典型用例中，请坚持使用{@link org.springframework.beans.factory.BeanFactory}或{@link org.springframework.beans.factory.ListableBeanFactory}。
 *     此接口仅用于允许框架内部即使需要访问bean工厂配置方法时也能进行插拔式操作。
 *
 * @author Juergen Hoeller
 * @since 03.11.2003
 * @see org.springframework.context.support.AbstractApplicationContext#getBeanFactory()
 */
public interface ConfigurableListableBeanFactory
		extends ListableBeanFactory, AutowireCapableBeanFactory, ConfigurableBeanFactory {

	/**
	 * 忽略指定的依赖类型，使其不参与自动装配：
	 * 例如，String 类型。默认情况下不忽略任何类型。
	 * @param type 要忽略的依赖类型
	 */
	void ignoreDependencyType(Class<?> type);

	/**
	 * 忽略指定的依赖接口，使其不参与自动装配。
	 * <p>
	 *     这通常由应用程序上下文用来注册以其他方式解析的依赖项，
	 *     例如通过 BeanFactoryAware 解析 BeanFactory，或通过 ApplicationContextAware 解析 ApplicationContext。
	 * <p>
	 *     默认情况下，仅忽略 BeanFactoryAware 接口。
	 *     对于要忽略的其他类型，请为每种类型调用此方法。
	 *
	 * @param ifc 要忽略的依赖接口
	 * @see org.springframework.beans.factory.BeanFactoryAware
	 * @see org.springframework.context.ApplicationContextAware
	 */
	void ignoreDependencyInterface(Class<?> ifc);

	/**
	 * 注册具有相应自动装配值的特殊依赖类型。
	 * <p>
	 *     这适用于那些本应可自动装配但未在工厂中定义为 bean 的工厂/上下文引用：
	 *     例如，类型为 ApplicationContext 的依赖项解析为 bean 所在的 ApplicationContext 实例。
	 * <p>
	 *     注意：在普通的 BeanFactory 中没有注册此类默认类型，甚至 BeanFactory 接口本身也没有。
	 *
	 * @param dependencyType 要注册的依赖类型。
	 *                       这通常是像 BeanFactory 这样的基础接口，如果声明为自动装配依赖项，
	 *                       其扩展接口也会被解析（例如 ListableBeanFactory），只要给定的值实际实现了扩展接口。
	 * @param autowiredValue 相应的自动装配值。
	 *                       这也可能是 {@link org.springframework.beans.factory.ObjectFactory} 接口的实现，允许延迟解析实际的目标值。
	 */
	void registerResolvableDependency(Class<?> dependencyType, @Nullable Object autowiredValue);

	/**
	 * 确定指定的bean是否符合自动装配候选条件，以便注入到声明了匹配类型依赖的其他bean中。
	 * <p>
	 *     此方法还会检查祖先工厂。
	 *
	 * @param beanName 要检查的bean的名称
	 * @param descriptor 要解析的依赖项的描述符
	 * @return 该bean是否应被视为自动装配候选
	 * @throws NoSuchBeanDefinitionException 如果不存在具有给定名称的bean
	 */
	boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
			throws NoSuchBeanDefinitionException;

	/**
	 * 返回指定 bean 的已注册 BeanDefinition，允许访问其属性值和构造函数参数值（这些值可以在 bean 工厂后处理期间被修改）。
	 * <p>
	 *     返回的 BeanDefinition 对象不应是副本，而应该是工厂中注册的原始定义对象。
	 *     这意味着在必要时应该可以将其转换为更具体的实现类型。
	 * <p>
	 *     <b>注意：</b>此方法<i>不</i>考虑祖先工厂。
	 *     它仅用于访问此工厂的本地 bean 定义。
	 *
	 * @param beanName bean 的名称
	 * @return 已注册的 BeanDefinition
	 * @throws NoSuchBeanDefinitionException 如果此工厂中没有定义具有给定名称的 bean
	 */
	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 返回此工厂管理的所有 bean 名称的统一视图。
	 * <p>
	 *     包括 bean 定义名称以及手动注册的单例实例名称，bean 定义名称始终排在前面，类似于按类型/注解特定检索 bean 名称的方式。
	 *
	 * @return bean 名称视图的复合迭代器
	 * @since 4.1.2
	 * @see #containsBeanDefinition
	 * @see #registerSingleton
	 * @see #getBeanNamesForType
	 * @see #getBeanNamesForAnnotation
	 */
	Iterator<String> getBeanNamesIterator();

	/**
	 * 清除合并的 bean 定义缓存，移除尚未被认为有资格进行完整元数据缓存的 bean 条目。
	 * <p>
	 *     通常在原始 bean 定义发生更改后触发，例如，在应用 {@link BeanFactoryPostProcessor} 之后。
	 *     请注意，此时已经创建的 bean 的元数据将被保留。
	 *
	 * @since 4.2
	 * @see #getBeanDefinition
	 * @see #getMergedBeanDefinition
	 */
	void clearMetadataCache();

	/**
	 * 冻结所有 bean 定义，表示已注册的 bean 定义将不再被修改或进一步后处理。
	 * <p>
	 *     这允许工厂在清除初始临时元数据缓存后，积极地缓存 bean 定义元数据。
	 *
	 * @see #clearMetadataCache()
	 * @see #isConfigurationFrozen()
	 */
	void freezeConfiguration();

	/**
	 * 返回此工厂的 bean 定义是否已被冻结，即是否不应再被修改或进一步后处理。
	 *
	 * @return 如果工厂的配置被认为是冻结的，则返回 {@code true}
	 * @see #freezeConfiguration()
	 */
	boolean isConfigurationFrozen();

	/**
	 * 将当前线程标记为单例实例化的主引导线程，后台线程应用宽松的引导锁定。
	 * <p>
	 *     任何此类标记都应在受管引导结束时移除，即在 {@link #preInstantiateSingletons()} 中。
	 *
	 * @since 6.2.12
	 * @see #setBootstrapExecutor
	 * @see #preInstantiateSingletons()
	 */
	default void prepareSingletonBootstrap() {
	}

	/**
	 * 确保所有非懒加载的单例都被实例化，同时考虑{@link org.springframework.beans.factory.FactoryBean FactoryBeans}。
	 * 通常在工厂设置结束时调用（如果需要的话）。
	 *
	 * @throws BeansException 如果某个单例 bean 无法创建。
	 * 						  注意：这种情况下工厂中可能已经有一些 bean 被初始化了！
	 *   					  在这种情况下调用 {@link #destroySingletons()} 进行完全清理。
	 * @see #prepareSingletonBootstrap()
	 * @see #destroySingletons()
	 */
	void preInstantiateSingletons() throws BeansException;

}
