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

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * 访问Spring bean容器的根本接口。
 *
 * <p>
 *     这是bean容器的基本客户端视图；
 *     进一步的接口如 {@link ListableBeanFactory} 和 {@link org.springframework.beans.factory.config.ConfigurableBeanFactory} 可用于特定目的。
 *
 * <p>
 *     此接口由持有多个bean定义的对象实现，每个bean定义由字符串名称唯一标识。
 *     根据bean定义，工厂将返回包含对象的独立实例（原型设计模式），或单个共享实例（优于单例设计模式的替代方案，在该模式中实例是工厂范围内的单例）。
 *     返回哪种类型的实例取决于bean工厂配置：API是相同的。
 *     从Spring 2.0开始，根据具体的应用程序上下文提供了更多作用域（例如，Web环境中的"request"和"session"作用域）。
 *
 * <p>
 *     这种方法的重点在于BeanFactory是应用程序组件的中央注册表，并集中管理应用程序组件的配置（例如，不再需要单个对象读取属性文件）。
 *     有关这种方法优势的讨论，请参见Rod Johnson所著《Expert One-on-One J2EE Design and Development》一书的第4章和第11章。
 *
 * <p>
 *     请注意，通常最好依靠依赖注入是通过setter或构造函数来配置应用程序对象（即"推送"配置），而不是使用任何形式的"拉取"配置如BeanFactory查找。
 *     Spring的依赖注入功能使用此BeanFactory接口及其子接口实现。
 *
 * <p>
 *     通常BeanFactory会加载存储在配置源（如XML文档）中的bean定义，并使用{@code org.springframework.beans}包来配置bean。
 *     然而，实现也可以简单地直接在Java代码中返回必要时创建的Java对象。
 *     对于如何存储定义没有限制：LDAP、RDBMS、XML、属性文件等。
 *     鼓励实现支持bean之间的引用（依赖注入）。
 *
 * <p>
 *     与 {@link ListableBeanFactory} 中的方法不同，如果当前工厂是 {@link HierarchicalBeanFactory} 时，那么{@link BeanFactory}接口中的所有操作还会检查父工厂。
 *     如果在此工厂实例中找不到bean， 将询问直接的父工厂。
 *     此工厂实例中的bean应该覆盖任何父工厂中同名的bean。
 *
 * <p>
 *     Bean工厂实现应尽可能支持标准的bean生命周期接口。
 *     完整的初始化方法集及其标准顺序是：
 * 	   <ol>
 * 	   		<li>BeanNameAware的{@code setBeanName}
 * 			<li>BeanClassLoaderAware的{@code setBeanClassLoader}
 * 			<li>BeanFactoryAware的{@code setBeanFactory}
 * 			<li>EnvironmentAware的{@code setEnvironment}
 * 			<li>EmbeddedValueResolverAware的{@code setEmbeddedValueResolver}
 * 			<li>ResourceLoaderAware的{@code setResourceLoader} (仅在应用程序上下文中运行时适用)
 * 			<li>ApplicationEventPublisherAware的{@code setApplicationEventPublisher} (仅在应用程序上下文中运行时适用)
 * 			<li>MessageSourceAware的{@code setMessageSource} (仅在应用程序上下文中运行时适用)
 * 			<li>ApplicationContextAware的{@code setApplicationContext} (仅在应用程序上下文中运行时适用)
 * 			<li>ServletContextAware的{@code setServletContext} (仅在Web应用程序上下文中运行时适用)
 * 			<li>BeanPostProcessors的{@code postProcessBeforeInitialization}方法
 * 			<li>InitializingBean的{@code afterPropertiesSet}
 * 			<li>自定义的{@code init-method}定义
 * 			<li>BeanPostProcessors的{@code postProcessAfterInitialization}方法
 * 	   </ol>
 *
 * <p>
 *     在bean工厂关闭时，以下生命周期方法适用：
 * 	   <ol>
 * 	   		<li>DestructionAwareBeanPostProcessors的{@code postProcessBeforeDestruction}方法
 *     		<li>DisposableBean的{@code destroy}
 *     		<li>自定义的{@code destroy-method}定义
 *     </ol>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 13 April 2001
 * @see BeanNameAware#setBeanName
 * @see BeanClassLoaderAware#setBeanClassLoader
 * @see BeanFactoryAware#setBeanFactory
 * @see org.springframework.context.EnvironmentAware#setEnvironment
 * @see org.springframework.context.EmbeddedValueResolverAware#setEmbeddedValueResolver
 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader
 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher
 * @see org.springframework.context.MessageSourceAware#setMessageSource
 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
 * @see org.springframework.web.context.ServletContextAware#setServletContext
 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization
 * @see InitializingBean#afterPropertiesSet
 * @see org.springframework.beans.factory.support.RootBeanDefinition#getInitMethodName
 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization
 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor#postProcessBeforeDestruction
 * @see DisposableBean#destroy
 * @see org.springframework.beans.factory.support.RootBeanDefinition#getDestroyMethodName
 */
public interface BeanFactory {

	/**
	 * 用于引用{@link FactoryBean}实例，并将其与FactoryBean<i>创建</i>的Bean实例区分开来。
	 * 例如，如果名为{@code myJndiObject}的bean是一个FactoryBean，
	 * 获取{@code &myJndiObject}将得到{@link FactoryBean}实例，而不是{@link FactoryBean}实例创建的Bean实例。
	 * 获取{@code myJndiObject}将得到{@link FactoryBean}实例创建的Bean实例。
	 * @see #FACTORY_BEAN_PREFIX_CHAR
	 */
	String FACTORY_BEAN_PREFIX = "&";

	/**
	 * {@link #FACTORY_BEAN_PREFIX}的字符变体。
	 * @since 6.2.6
	 */
	char FACTORY_BEAN_PREFIX_CHAR = '&';


	/**
	 * 返回指定bean的实例，该实例可能是共享的或独立的。
	 * <p>
	 *     此方法允许Spring BeanFactory用作单例或原型设计模式的替代品。
	 *     调用者可以在单例bean的情况下保留对返回对象的引用。
	 * <p>
	 *     将别名转换回相应的规范bean名称。
	 * <p>
	 *     如果在此工厂实例中找不到bean，将询问父工厂。
	 *
	 * @param name 要检索的bean的名称
	 * @return bean的一个实例。
	 * 请注意，返回值永远不会是{@code null}，但可能是工厂方法返回的{@code null}的存根，
	 * 需通过{@code equals(null)}检查。
	 * 考虑使用{@link #getBeanProvider(Class)}来解析可选依赖项。
	 * @throws NoSuchBeanDefinitionException 如果没有指定名称的bean
	 * @throws BeansException 如果无法获取bean
	 */
	Object getBean(String name) throws BeansException;

	/**
	 * 返回指定bean的实例，该实例可能是共享的或独立的。
	 * <p>
	 *     行为与 {@link #getBean(String)} 相同，但如果bean不是所需的类型，则通过抛出 BeanNotOfRequiredTypeException 来提供一定程度的类型安全性。
	 *     这意味着在正确转换结果时不会抛出 ClassCastException，而这在使用 {@link #getBean(String)} 时可能会发生。
	 * <p>
	 *     将别名转换回相应的规范bean名称。
	 * <p>
	 *     如果在此工厂实例中找不到bean，将询问父工厂。
	 *
	 * @param name 要检索的bean的名称
	 * @param requiredType bean必须匹配的类型；可以是接口或超类
	 * @return bean的一个实例。
	 * 请注意，返回值永远不会是 {@code null}。如果为请求的bean解析了来自工厂方法的 {@code null} 存根，
	 * 则将引发针对 NullBean 存根的 {@code BeanNotOfRequiredTypeException}。
	 * 考虑使用 {@link #getBeanProvider(Class)} 来解析可选依赖项。
	 * @throws NoSuchBeanDefinitionException 如果没有这样的bean定义
	 * @throws BeanNotOfRequiredTypeException 如果bean不是所需的类型
	 * @throws BeansException 如果无法创建bean
	 */
	<T> T getBean(String name, Class<T> requiredType) throws BeansException;

	/**
	 * 返回指定bean的实例，该实例可能是共享的或独立的。
	 * <p>
	 *     允许指定显式的构造函数参数/工厂方法参数，覆盖bean定义中指定的默认参数（如果有）。
	 *     请注意，提供的参数需要按照声明参数的顺序匹配特定的候选构造函数/工厂方法。
	 *
	 * @param name 要检索的bean的名称
	 * @param args 创建bean实例时使用的显式参数（仅在创建新实例而不是检索现有实例时应用）
	 * @return bean的一个实例
	 * @throws NoSuchBeanDefinitionException 如果没有这样的bean定义
	 * @throws BeanDefinitionStoreException 如果提供了参数，但受影响的bean不是原型
	 * @throws BeansException 如果无法创建bean
	 * @since 2.5
	 */
	Object getBean(String name, Object... args) throws BeansException;

	/**
	 * 返回唯一匹配给定对象类型的bean实例（如果有的话）。
	 * <p>
	 *     此方法进入{@link ListableBeanFactory}的按类型查找领域但也可能转换为基于给定类型名称的传统按名称查找。
	 *     对于跨bean集合的更广泛检索操作，使用{@link ListableBeanFactory}和/或{@link BeanFactoryUtils}。
	 *
	 * @param requiredType bean必须匹配的类型；可以是接口或超类
	 * @return 匹配所需类型的单个bean的实例
	 * @throws NoSuchBeanDefinitionException 如果未找到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 * @throws BeansException 如果无法创建bean
	 * @since 3.0
	 * @see ListableBeanFactory
	 */
	<T> T getBean(Class<T> requiredType) throws BeansException;

	/**
	 * 返回指定bean的实例，该实例可能是共享的或独立的。
	 * <p>
	 *     允许指定显式的构造函数参数/工厂方法参数，覆盖bean定义中指定的默认参数（如果有）。
	 *     请注意，提供的参数需要按照声明参数的顺序匹配特定的候选构造函数/工厂方法。
	 * <p>
	 *     此方法进入{@link ListableBeanFactory}的按类型查找领域但也可能转换为基于给定类型名称的传统按名称查找。
	 *     对于跨bean集合的更广泛检索操作，使用{@link ListableBeanFactory}和/或{@link BeanFactoryUtils}。
	 *
	 * @param requiredType bean必须匹配的类型；可以是接口或超类
	 * @param args 创建bean实例时使用的显式参数(仅在创建新实例而不是检索现有实例时应用)
	 * @return bean的一个实例
	 * @throws NoSuchBeanDefinitionException 如果没有这样的bean定义
	 * @throws BeanDefinitionStoreException 如果提供了参数但受影响的bean不是原型
	 * @throws BeansException 如果无法创建bean
	 * @since 4.1
	 */
	<T> T getBean(Class<T> requiredType, Object... args) throws BeansException;

	/**
	 * 返回指定bean的提供者，允许延迟按需检索实例，包括可用性和唯一性选项。
	 * <p>
	 *     对于匹配泛型类型，请考虑使用 {@link #getBeanProvider(ResolvableType)}。
	 *
	 * @param requiredType bean必须匹配的类型；可以是接口或超类
	 * @return 相应的提供者句柄
	 * @since 5.1
	 * @see #getBeanProvider(ResolvableType)
	 */
	<T> ObjectProvider<T> getBeanProvider(Class<T> requiredType);

	/**
	 * 返回指定bean的提供者，允许延迟按需检索实例，包括可用性和唯一性选项。
	 * 此变体允许指定要匹配的泛型类型，类似于方法/构造函数参数中带有泛型类型声明的反射注入点。
	 * <p>请注意，此处不支持bean集合，这与反射注入点不同。
	 * 要以编程方式检索匹配特定类型的bean列表，请在此处指定实际的bean类型，
	 * 然后使用 {@link ObjectProvider#orderedStream()} 或其延迟流式处理/迭代选项。
	 * <p>
	 *     此外，此处的泛型匹配是严格的，遵循Java赋值规则。
	 *     对于宽松的回退匹配（类似于'unchecked' Java编译器警告的未检查语义），如果使用此变体无法获得完全的泛型匹配，
	 *     则可以考虑调用 {@link #getBeanProvider(Class)}，将原始类型作为第二步的入参。
	 * @param requiredType bean必须匹配的类型；可以是泛型类型声明
	 * @return 相应的提供者句柄
	 * @since 5.1
	 * @see ObjectProvider#iterator()
	 * @see ObjectProvider#stream()
	 * @see ObjectProvider#orderedStream()
	 */
	<T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType);

	/**
	 * 此bean工厂是否包含具有给定名称的bean定义或外部注册的单例实例？
	 * <p>
	 *     如果给定名称是别名，它将被转换回相应的规范bean名称。
	 * <p>
	 *     如果此工厂是分层的，当在此工厂实例中找不到bean时，将询问任何父工厂。
	 * <p>
	 *     如果找到与给定名称匹配的bean定义或单例实例，无论命名的bean定义是具体的还是抽象的、懒加载的还是急加载的、有作用域的还是无作用域的，此方法都将返回{@code true}。
	 *     因此，请注意，从此方法返回{@code true}并不一定表示{@link #getBean}能够获取到相同名称的实例。
	 *
	 * @param name 要查询的bean的名称
	 * @return 是否存在具有给定名称的bean
	 */
	boolean containsBean(String name);

	/**
	 * 这个bean是共享的单例吗？也就是说，{@link #getBean} 是否总是返回相同的实例？
	 * <p>
	 *     注意：此方法返回 {@code false} 并不明确表示这是独立的实例。
	 *     它表示非单例实例，这也可能对应于作用域bean。
	 *     使用 {@link #isPrototype} 操作来显式检查独立实例。
	 * <p>
	 *     将别名转换回相应的规范bean名称。
	 * <p>
	 *     如果在此工厂实例中找不到bean，将询问父工厂。
	 *
	 * @param name 要查询的bean的名称
	 * @return 此bean是否对应于单例实例
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 * @see #getBean
	 * @see #isPrototype
	 */
	boolean isSingleton(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 这个bean是原型吗？也就是说，{@link #getBean} 是否总是返回独立的实例？
	 * <p>
	 *     注意：此方法返回 {@code false} 并不明确表示这是一个单例对象。
	 * 	   它表示非独立实例，这也可能对应于作用域bean。
	 *     使用 {@link #isSingleton} 操作来显式检查共享的单例实例。
	 * <p>
	 *     将别名转换回相应的规范bean名称。
	 * <p>
	 *     如果在此工厂实例中找不到bean，将询问父工厂。
	 *
	 * @param name 要查询的bean的名称
	 * @return 此bean是否总是提供独立的实例
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 * @since 2.0.3
	 * @see #getBean
	 * @see #isSingleton
	 */
	boolean isPrototype(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 检查具有给定名称的bean是否与指定类型匹配。
	 * 更具体地说，检查对给定名称的{@link #getBean}调用是否会返回一个可赋值给指定目标类型的对象。
	 * <p>
	 *     将别名转换回相应的规范bean名称。
	 * <p>
	 *     如果在此工厂实例中找不到bean，将询问父工厂。
	 *
	 * @param name 要查询的bean的名称
	 * @param typeToMatch 要匹配的类型（作为{@code ResolvableType}）
	 * @return 如果bean类型匹配则返回{@code true}，如果不匹配或尚无法确定则返回{@code false}
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 * @since 4.2
	 * @see #getBean
	 * @see #getType
	 */
	boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException;

	/**
	 * 检查具有给定名称的bean是否与指定类型匹配。
	 * 更具体地说，检查对给定名称的{@link #getBean}调用是否会返回一个可赋值给指定目标类型的对象。
	 * <p>
	 *     将别名转换回相应的规范bean名称。
	 * <p>
	 *     如果在此工厂实例中找不到bean，将询问父工厂。
	 *
	 * @param name 要查询的bean的名称
	 * @param typeToMatch 要匹配的类型（作为{@code Class}）
	 * @return 如果bean类型匹配则返回{@code true}，如果不匹配或尚无法确定则返回{@code false}
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 * @since 2.0.1
	 * @see #getBean
	 * @see #getType
	 */
	boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException;

	/**
	 * 确定具有给定名称的bean的类型。更具体地说，确定{@link #getBean}对给定名称会返回的对象的类型。
	 * <p>
	 *     对于{@link FactoryBean}，返回FactoryBean创建的对象类型，如{@link FactoryBean#getObjectType()}所暴露的。
	 *     这可能导致先前未初始化的{@code FactoryBean}的初始化（参见{@link #getType(String, boolean)}）。
	 * <p>
	 *     将别名转换回相应的规范bean名称。
	 * <p>
	 *     如果在此工厂实例中找不到bean，将询问父工厂。
	 *
	 * @param name 要查询的bean的名称
	 * @return bean的类型，如果无法确定则返回{@code null}
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 * @since 1.1.2
	 * @see #getBean
	 * @see #isTypeMatch
	 */
	@Nullable
	Class<?> getType(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 确定具有给定名称的bean的类型。更具体地说，确定{@link #getBean}对给定名称会返回的对象的类型。
	 * <p>
	 *     对于{@link FactoryBean}，返回FactoryBean创建的对象类型，如{@link FactoryBean#getObjectType()}所暴露的。
	 *     根据{@code allowFactoryBeanInit}标志，如果早期类型信息不可用，这可能导致先前未初始化的{@code FactoryBean}的初始化。
	 * <p>
	 *     将别名转换回相应的规范bean名称。
	 * <p>
	 *     如果在此工厂实例中找不到bean，将询问父工厂。
	 *
	 * @param name 要查询的bean的名称
	 * @param allowFactoryBeanInit 是否允许{@code FactoryBean}仅为确定其对象类型而初始化
	 * @return bean的类型，如果无法确定则返回{@code null}
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 * @since 5.2
	 * @see #getBean
	 * @see #isTypeMatch
	 */
	@Nullable
	Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException;

	/**
	 * 返回给定bean名称的别名（如果有的话）。
	 * <p>
	 *     所有这些别名在{@link #getBean}调用中都指向同一个bean。
	 * <p>
	 *     如果给定名称是别名，则相应的原始bean名称和其他别名（如果有的话）将被返回，原始bean名称是数组中的第一个元素。
	 * <p>
	 *     如果在此工厂实例中找不到bean，将询问父工厂。
	 *
	 * @param name 要检查别名的bean名称
	 * @return 别名，如果没有则返回空数组
	 * @see #getBean
	 */
	String[] getAliases(String name);

}
