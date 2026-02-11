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

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * {@link BeanFactory} 接口的扩展，由能够枚举所有 bean 实例的 bean 工厂实现，而不是像客户端请求那样逐个按名称查找 bean。
 * 预加载所有 bean 定义的 BeanFactory 实现（例如基于 XML 的工厂）可以实现此接口。
 *
 * <p>
 *     如果这是一个 {@link HierarchicalBeanFactory}，返回值将<i>不会</i>考虑任何 BeanFactory 层次结构，而只会关联到当前工厂中定义的 bean。
 *     使用 {@link BeanFactoryUtils} 辅助类也可以考虑祖先工厂中的 bean。
 *
 * <p>
 *     此接口中的方法只会尊重此工厂的 bean 定义。
 *     它们会忽略通过其他方式（如{@link org.springframework.beans.factory.config.ConfigurableBeanFactory} 的 {@code registerSingleton} 方法）注册的任何单例 bean，
 *     但 {@code getBeanNamesForType} 和 {@code getBeansOfType} 除外，它们也会检查此类手动注册的单例。
 *     当然，BeanFactory 的 {@code getBean} 也允许透明访问这些特殊 bean。
 *     然而，在典型场景中，所有 bean 都将由外部 bean 定义定义，因此大多数应用程序无需担心这种区分。
 *
 * <p>
 *     <b>注意：</b>除了 {@code getBeanDefinitionCount} 和 {@code containsBeanDefinition} 之外，此接口中的方法并非设计用于频繁调用。实现可能会较慢。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2001年4月16日
 * @see HierarchicalBeanFactory
 * @see BeanFactoryUtils
 */
public interface ListableBeanFactory extends BeanFactory {

	/**
	 * 检查此bean工厂是否包含具有给定名称的bean定义。
	 * <p>
	 *     不考虑此工厂可能参与的任何层次结构，并忽略通过除bean定义以外的其他方式注册的任何单例bean。
	 *
	 * @param beanName 要查找的bean的名称
	 * @return 如果此bean工厂包含具有给定名称的bean定义
	 * @see #containsBean
	 */
	boolean containsBeanDefinition(String beanName);

	/**
	 * 返回工厂中定义的bean数量。
	 * <p>
	 *     不考虑此工厂可能参与的任何层次结构，并忽略通过除bean定义以外的其他方式注册的任何单例bean。
	 *
	 * @return 工厂中定义的bean数量
	 */
	int getBeanDefinitionCount();

	/**
	 * 返回此工厂中定义的所有bean的名称。
	 * <p>
	 *     不考虑此工厂可能参与的任何层次结构，并忽略通过除bean定义以外的其他方式注册的任何单例bean。
	 *
	 * @return 此工厂中定义的所有bean的名称，如果没有定义则返回空数组
	 */
	String[] getBeanDefinitionNames();

	/**
	 * 返回指定 bean 的提供者，允许延迟按需检索实例，包括可用性和唯一性选项。
	 * @param requiredType bean 必须匹配的类型；可以是接口或超类
	 * @param allowEagerInit 是否允许流式访问内省<i>延迟初始化的单例</i>和<i>由 FactoryBeans 创建的对象</i>——或通过带有 "factory-bean" 引用的工厂方法进行类型检查。
	 *                       请注意，FactoryBeans 需要急切初始化以确定其类型：因此请注意，为此标志传入 "true" 将初始化 FactoryBeans 和 "factory-bean" 引用。
	 *                       仅会执行类型检查目的所需的实际必要初始化；构造函数和方法调用将尽可能避免。
	 * @return 相应的提供者句柄
	 * @since 5.3
	 * @see #getBeanProvider(ResolvableType, boolean)
	 * @see #getBeanProvider(Class)
	 * @see #getBeansOfType(Class, boolean, boolean)
	 * @see #getBeanNamesForType(Class, boolean, boolean)
	 */
	<T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit);

	/**
	 * 返回指定 bean 的提供者，允许延迟按需检索实例，包括可用性和唯一性选项。
	 * @param requiredType bean 必须匹配的类型；可以是泛型类型声明。
	 *                     注意，此处不支持集合类型，这与反射注入点不同。
	 *                     若要以编程方式检索匹配特定类型的 bean 列表，请在此处指定实际的 bean 类型作为参数，
	 *                     然后使用 {@link ObjectProvider#orderedStream()} 或其延迟流式处理/迭代选项。
	 * @param allowEagerInit 是否允许流式访问内省<i>延迟初始化的单例</i>和<i>由 FactoryBeans 创建的对象</i>——或通过带有 "factory-bean" 引用的工厂方法进行类型检查。
	 *                       请注意，FactoryBeans 需要急切初始化以确定其类型：因此请注意，为此标志传入 "true" 将初始化 FactoryBeans 和 "factory-bean" 引用。
	 *                       仅会执行类型检查目的所需的实际必要初始化；构造函数和方法调用将尽可能避免。
	 * @return 相应的提供者句柄
	 * @since 5.3
	 * @see #getBeanProvider(ResolvableType)
	 * @see ObjectProvider#iterator()
	 * @see ObjectProvider#stream()
	 * @see ObjectProvider#orderedStream()
	 * @see #getBeanNamesForType(ResolvableType, boolean, boolean)
	 */
	<T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit);

	/**
	 * 返回与给定类型（包括子类）匹配的 bean 名称，根据 bean 定义或 FactoryBeans 情况下的 {@code getObjectType} 值进行判断。
	 * <p>
	 *     <b>注意：此方法仅内省顶级 bean。</b>它<i>不会</i>检查可能也匹配指定类型的嵌套 bean。
	 * <p>
	 *     会考虑由 FactoryBeans 创建的对象，这意味着 FactoryBeans 将被初始化。
	 *     如果 FactoryBean 创建的对象不匹配，则原始 FactoryBean 本身将与类型进行匹配。
	 * <p>
	 *     不考虑此工厂可能参与的任何层次结构。
	 *     使用 BeanFactoryUtils 的 {@code beanNamesForTypeIncludingAncestors} 来包含祖先工厂中的 bean。
	 * <p>
	 *     此版本的 {@code getBeanNamesForType} 匹配所有类型的 bean，无论是单例、原型还是 FactoryBeans。
	 *     在大多数实现中，结果将与 {@code getBeanNamesForType(type, true, true)} 相同。
	 * <p>
	 *     此方法返回的 bean 名称应始终尽可能按照后端配置中的<i>定义顺序</i>返回。
	 *
	 * @param type 要匹配的泛型类型化类或接口
	 * @return 与给定对象类型（包括子类）匹配的 bean 名称（或由 FactoryBeans 创建的对象），如果没有则返回空数组
	 * @since 4.2
	 * @see #isTypeMatch(String, ResolvableType)
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, ResolvableType)
	 */
	String[] getBeanNamesForType(ResolvableType type);

	/**
	 * 返回与给定类型（包括子类）匹配的 bean 名称，根据 bean 定义或 FactoryBeans 情况下的 {@code getObjectType} 值进行判断。
	 * <p>
	 *     <b>注意：此方法仅内省顶级 bean。</b>它<i>不会</i>检查可能也匹配指定类型的嵌套 bean。
	 * <p>
	 *     如果设置了 "allowEagerInit" 标志，则会考虑由 FactoryBeans 创建的对象，这意味着 FactoryBeans 将被初始化。
	 *     如果 FactoryBean 创建的对象不匹配，则原始 FactoryBean 本身将与类型进行匹配。
	 *     如果未设置 "allowEagerInit"，则只检查原始 FactoryBeans（不需要初始化每个 FactoryBean）。
	 * <p>
	 *     不考虑此工厂可能参与的任何层次结构。
	 *     使用 BeanFactoryUtils 的 {@code beanNamesForTypeIncludingAncestors} 来包含祖先工厂中的 bean。
	 * <p>
	 *     此方法返回的 bean 名称应始终尽可能按照后端配置中的<i>定义顺序</i>返回。
	 *
	 * @param type 要匹配的泛型类型化类或接口
	 * @param includeNonSingletons 是否包含原型或作用域 bean，而不仅仅是单例（也适用于 FactoryBeans）
	 * @param allowEagerInit 是否内省<i>延迟初始化的单例</i>和<i>由 FactoryBeans 创建的对象</i> ——或通过带有 "factory-bean" 引用的工厂方法进行类型检查。
	 *                       请注意，FactoryBeans 需要急切初始化以确定其类型：因此请注意，为此标志传入 "true" 将初始化 FactoryBeans 和 "factory-bean" 引用。
	 *                       仅会执行类型检查目的所需的实际必要初始化；构造函数和方法调用将尽可能避免。
	 * @return 与给定对象类型（包括子类）匹配的 bean 名称（或由 FactoryBeans 创建的对象），如果没有则返回空数组
	 * @since 5.2
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, ResolvableType, boolean, boolean)
	 */
	String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit);

	/**
	 * 返回与给定类型（包括子类）匹配的 bean 名称，根据 bean 定义或 FactoryBeans 情况下的 {@code getObjectType} 值进行判断。
	 * <p>
	 *     <b>注意：此方法仅内省顶级 bean。</b>它<i>不会</i>检查可能也匹配指定类型的嵌套 bean。
	 * <p>
	 *     会考虑由 FactoryBeans 创建的对象，这意味着 FactoryBeans 将被初始化。
	 *     如果 FactoryBean 创建的对象不匹配，则原始 FactoryBean 本身将与类型进行匹配。
	 * <p>
	 *     不考虑此工厂可能参与的任何层次结构。
	 *     使用 BeanFactoryUtils 的 {@code beanNamesForTypeIncludingAncestors} 来包含祖先工厂中的 bean。
	 * <p>
	 *     此版本的 {@code getBeanNamesForType} 匹配所有类型的 bean，无论是单例、原型还是 FactoryBeans。
	 *     在大多数实现中，结果将与 {@code getBeanNamesForType(type, true, true)} 相同。
	 * <p>
	 *     此方法返回的 bean 名称应始终尽可能按照后端配置中的<i>定义顺序</i>返回。
	 *
	 * @param type 要匹配的类或接口，或 {@code null} 表示所有 bean 名称
	 * @return 与给定对象类型（包括子类）匹配的 bean 名称（或由 FactoryBeans 创建的对象），如果没有则返回空数组
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	String[] getBeanNamesForType(@Nullable Class<?> type);

	/**
	 * 返回与给定类型（包括子类）匹配的 bean 名称，
	 * 根据 bean 定义或 FactoryBeans 情况下的 {@code getObjectType} 值进行判断。
	 * <p><b>注意：此方法仅内省顶级 bean。</b>它<i>不会</i>检查可能也匹配指定类型的嵌套 bean。
	 * <p>如果设置了 "allowEagerInit" 标志，则会考虑由 FactoryBeans 创建的对象，
	 * 这意味着 FactoryBeans 将被初始化。如果 FactoryBean 创建的对象不匹配，
	 * 则原始 FactoryBean 本身将与类型进行匹配。如果未设置 "allowEagerInit"，
	 * 则只检查原始 FactoryBeans（不需要初始化每个 FactoryBean）。
	 * <p>不考虑此工厂可能参与的任何层次结构。
	 * 使用 BeanFactoryUtils 的 {@code beanNamesForTypeIncludingAncestors}
	 * 来包含祖先工厂中的 bean。
	 * <p>此方法返回的 bean 名称应始终尽可能按照后端配置中的<i>定义顺序</i>返回。
	 * @param type 要匹配的类或接口，或 {@code null} 表示所有 bean 名称
	 * @param includeNonSingletons 是否包含原型或作用域 bean，而不仅仅是单例（也适用于 FactoryBeans）
	 * @param allowEagerInit 是否内省<i>延迟初始化的单例</i>和<i>由 FactoryBeans 创建的对象</i>
	 * ——或通过带有 "factory-bean" 引用的工厂方法进行类型检查。请注意，FactoryBeans 需要急切初始化
	 * 以确定其类型：因此请注意，为此标志传入 "true" 将初始化 FactoryBeans 和 "factory-bean" 引用。
	 * 仅会执行类型检查目的所需的实际必要初始化；构造函数和方法调用将尽可能避免。
	 * @return 与给定对象类型（包括子类）匹配的 bean 名称（或由 FactoryBeans 创建的对象），
	 * 如果没有则返回空数组
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, Class, boolean, boolean)
	 */
	String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit);

	/**
	 * 返回与给定对象类型（包括子类）匹配的 bean 实例，根据 bean 定义或 FactoryBeans 情况下的 {@code getObjectType} 值进行判断。
	 * <p>
	 *     <b>注意：此方法仅内省顶级 bean。</b>它<i>不会</i>检查可能也匹配指定类型的嵌套 bean。
	 *     此外，它<b>会抑制在循环引用场景中当前正在创建的 bean 的异常：</b>通常是对此方法调用者的引用。
	 * <p>
	 *     会考虑由 FactoryBeans 创建的对象，这意味着 FactoryBeans 将被初始化。
	 *     如果 FactoryBean 创建的对象不匹配，则原始 FactoryBean 本身将与类型进行匹配。
	 * <p>
	 *     不考虑此工厂可能参与的任何层次结构。
	 *     使用 BeanFactoryUtils 的 {@code beansOfTypeIncludingAncestors} 来包含祖先工厂中的 bean。
	 * <p>
	 *     此版本的 getBeansOfType 匹配所有类型的 bean，无论是单例、原型还是 FactoryBeans。
	 *     在大多数实现中，结果将与 {@code getBeansOfType(type, true, true)} 相同。
	 * <p>
	 *     此方法返回的 Map 应始终尽可能按照后端配置中的<i>定义顺序</i>返回 bean 名称和对应的 bean 实例。
	 * <p>
	 *     <b>建议优先使用 {@link #getBeanNamesForType(Class)} 配合选择性的 {@link #getBean} 调用来获取特定 bean 名称，而不是使用此基于 Map 的检索方法。
	 *     </b>除了延迟实例化的好处外，这还可以避免任何异常抑制。
	 *
	 * @param type 要匹配的类或接口，或 {@code null} 表示所有具体 bean
	 * @return 包含匹配 bean 的 Map，键为 bean 名称，值为对应的 bean 实例
	 * @throws BeansException 如果无法创建 bean
	 * @since 1.1.2
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	<T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException;

	/**
	 * 返回与给定对象类型（包括子类）匹配的 bean 实例，根据 bean 定义或 FactoryBeans 情况下的 {@code getObjectType} 值进行判断。
	 * <p>
	 *     <b>注意：此方法仅内省顶级 bean。
	 *     </b>它<i>不会</i>检查可能也匹配指定类型的嵌套 bean。
	 *     此外，它<b>会抑制在循环引用场景中当前正在创建的 bean 的异常：</b>通常是对此方法调用者的引用。
	 * <p>
	 *     如果设置了 "allowEagerInit" 标志，则会考虑由 FactoryBeans 创建的对象，这意味着 FactoryBeans 将被初始化。
	 *     如果 FactoryBean 创建的对象不匹配，则原始 FactoryBean 本身将与类型进行匹配。
	 *     如果未设置 "allowEagerInit"，则只检查原始 FactoryBeans（不需要初始化每个 FactoryBean）。
	 * <p>
	 *     不考虑此工厂可能参与的任何层次结构。
	 *     使用 BeanFactoryUtils 的 {@code beansOfTypeIncludingAncestors} 来包含祖先工厂中的 bean。
	 * <p>
	 *     此方法返回的 Map 应始终尽可能按照后端配置中的<i>定义顺序</i>返回 bean 名称和对应的 bean 实例。
	 * <p>
	 *     <b>建议优先使用 {@link #getBeanNamesForType(Class)} 配合选择性的 {@link #getBean}调用来获取特定 bean 名称，而不是使用此基于 Map 的检索方法。</b>
	 *     除了延迟实例化的好处外，这还可以避免任何异常抑制。
	 *
	 * @param type 要匹配的类或接口，或 {@code null} 表示所有具体 bean
	 * @param includeNonSingletons 是否包含原型或作用域 bean，而不仅仅是单例（也适用于 FactoryBeans）
	 * @param allowEagerInit 是否内省<i>延迟初始化的单例</i>和<i>由 FactoryBeans 创建的对象</i>——或通过带有 "factory-bean" 引用的工厂方法进行类型检查。
	 *                       请注意，FactoryBeans 需要急切初始化以确定其类型：因此请注意，为此标志传入 "true" 将初始化 FactoryBeans 和 "factory-bean" 引用。
	 *                       仅会执行类型检查目的所需的实际必要初始化；构造函数和方法调用将尽可能避免。
	 * @return 包含匹配 bean 的 Map，键为 bean 名称，值为对应的 bean 实例
	 * @throws BeansException 如果无法创建 bean
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class, boolean, boolean)
	 */
	<T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException;

	/**
	 * 查找所有使用提供的 {@link Annotation} 类型进行注解的 bean 名称，而不创建相应的 bean 实例。
	 * <p>
	 *     请注意，此方法会考虑由 FactoryBeans 创建的对象，这意味着 FactoryBeans 将被初始化以确定其对象类型。
	 *     
	 * @param annotationType 要查找的注解类型（在指定 bean 的类、接口或工厂方法级别）
	 * @return 所有匹配 bean 的名称
	 * @since 4.0
	 * @see #getBeansWithAnnotation(Class)
	 * @see #findAnnotationOnBean(String, Class)
	 */
	String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType);

	/**
	 * 查找所有使用提供的 {@link Annotation} 类型进行注解的 bean，返回一个包含 bean 名称和对应 bean 实例的映射。
	 * <p>
	 *     请注意，此方法会考虑由 FactoryBeans 创建的对象，这意味着 FactoryBeans 将被初始化以确定其对象类型。
	 *
	 * @param annotationType 要查找的注解类型（在指定 bean 的类、接口或工厂方法级别）
	 * @return 包含匹配 bean 的映射，其中键为 bean 名称，值为对应的 bean 实例
	 * @throws BeansException 如果无法创建 bean
	 * @since 3.0
	 * @see #findAnnotationOnBean(String, Class)
	 * @see #findAnnotationOnBean(String, Class, boolean)
	 * @see #findAllAnnotationsOnBean(String, Class, boolean)
	 */
	Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException;

	/**
	 * 查找指定 bean 上 {@code annotationType} 类型的 {@link Annotation}，如果在给定类本身上找不到注解，
	 * 则遍历其接口和超类，同时检查 bean 的工厂方法（如果有）。
	 * @param beanName 要查找注解的 bean 名称
	 * @param annotationType 要查找的注解类型（在指定 bean 的类、接口或工厂方法级别）
	 * @return 如果找到则返回给定类型的注解，否则返回 {@code null}
	 * @throws NoSuchBeanDefinitionException 如果不存在具有给定名称的 bean
	 * @since 3.0
	 * @see #findAnnotationOnBean(String, Class, boolean)
	 * @see #findAllAnnotationsOnBean(String, Class, boolean)
	 * @see #getBeanNamesForAnnotation(Class)
	 * @see #getBeansWithAnnotation(Class)
	 * @see #getType(String)
	 */
	@Nullable
	<A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException;

	/**
	 * 查找指定 bean 上 {@code annotationType} 类型的 {@link Annotation}，如果在给定类本身上找不到注解，
	 * 则遍历其接口和超类，同时检查 bean 的工厂方法（如果有）。
	 * @param beanName 要查找注解的 bean 名称
	 * @param annotationType 要查找的注解类型（在指定 bean 的类、接口或工厂方法级别）
	 * @param allowFactoryBeanInit 是否允许 {@code FactoryBean} 仅为确定其对象类型而初始化
	 * @return 如果找到则返回给定类型的注解，否则返回 {@code null}
	 * @throws NoSuchBeanDefinitionException 如果不存在具有给定名称的 bean
	 * @since 5.3.14
	 * @see #findAnnotationOnBean(String, Class)
	 * @see #findAllAnnotationsOnBean(String, Class, boolean)
	 * @see #getBeanNamesForAnnotation(Class)
	 * @see #getBeansWithAnnotation(Class)
	 * @see #getType(String, boolean)
	 */
	@Nullable
	<A extends Annotation> A findAnnotationOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException;

	/**
	 * 查找指定 bean 上所有 {@code annotationType} 类型的 {@link Annotation} 实例，
	 * 如果在给定类本身上找不到注解，则遍历其接口和超类，同时检查 bean 的工厂方法（如果有）。
	 * @param beanName 要查找注解的 bean 名称
	 * @param annotationType 要查找的注解类型（在指定 bean 的类、接口或工厂方法级别）
	 * @param allowFactoryBeanInit 是否允许 {@code FactoryBean} 仅为确定其对象类型而初始化
	 * @return 找到的给定类型注解集合（可能为空）
	 * @throws NoSuchBeanDefinitionException 如果不存在具有给定名称的 bean
	 * @since 6.0
	 * @see #getBeanNamesForAnnotation(Class)
	 * @see #findAnnotationOnBean(String, Class, boolean)
	 * @see #getType(String, boolean)
	 */
	<A extends Annotation> Set<A> findAllAnnotationsOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException;

}
