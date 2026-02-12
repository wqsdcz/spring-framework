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

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.core.OrderComparator;
import org.springframework.lang.Nullable;

/**
 * 专门为注入点设计的{@link ObjectFactory}变体，
 * 允许程序化可选性和宽松的不唯一处理。
 *
 * <p>在{@link BeanFactory}环境中，从工厂获取的每个{@code ObjectProvider}
 * 都将绑定到其{@code BeanFactory}以获取特定bean类型，
 * 将所有提供者调用与工厂注册的bean定义进行匹配。
 * 注意，所有这些调用都对底层工厂状态进行动态操作，
 * 在每次调用时重新解析请求的目标对象。
 *
 * <p>从5.1开始，此接口扩展了{@link Iterable}并提供了{@link Stream}
 * 支持。因此它可以在{@code for}循环中使用，提供{@link #forEach}
 * 迭代，并允许集合风格的{@link #stream}访问。
 *
 * <p>从6.2开始，此接口为所有方法声明了默认实现。
 * 这使得以自定义方式实现更容易，例如，用于单元测试。
 * 对于典型用途，实现{@link #stream()}以启用所有其他方法。
 * 或者，你可以实现调用者期望的特定方法，
 * 例如，仅{@link #getObject()}或{@link #getIfAvailable()}。
 *
 * <p>注意，{@link #getObject()}永远不会返回{@code null} - 而是会抛出
 * {@link NoSuchBeanDefinitionException} -，而{@link #getIfAvailable()}
 * 如果完全没有匹配的bean，则返回{@code null}。但是，如果找到多个匹配的bean
 * 而没有明确的唯一获胜者（见下文），两种方法都会抛出{@link NoUniqueBeanDefinitionException}。
 * 最后，{@link #getIfUnique()}将在找不到匹配bean以及找到多个匹配bean
 * 而没有唯一获胜者时都返回{@code null}。
 *
 * <p>唯一性通常取决于容器的候选解析算法，
 * 但始终遵守"primary"标志（只有一个候选bean标记为primary）
 * 和"fallback"标志（只有一个候选bean未标记为fallback）。
 * 默认候选标志也始终被考虑，即使对于非基于注解的注入点，
 * 在没有明确primary/fallback指示的情况下，单个默认候选获胜。
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @param <T> 对象类型
 * @see BeanFactory#getBeanProvider
 * @see org.springframework.beans.factory.annotation.Autowired
 */
public interface ObjectProvider<T> extends ObjectFactory<T>, Iterable<T> {

	/**
	 * 用于未过滤类型匹配的谓词，包括非默认候选，
	 * 但在用于注入点时仍排除非自动装配候选。
	 * @since 6.2.3
	 * @see #stream(Predicate)
	 * @see #orderedStream(Predicate)
	 * @see org.springframework.beans.factory.config.BeanDefinition#isAutowireCandidate()
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#isDefaultCandidate()
	 */
	Predicate<Class<?>> UNFILTERED = (clazz -> true);


	@Override
	default T getObject() throws BeansException {
		Iterator<T> it = iterator();
		if (!it.hasNext()) {
			throw new NoSuchBeanDefinitionException(Object.class);
		}
		T result = it.next();
		if (it.hasNext()) {
			throw new NoUniqueBeanDefinitionException(Object.class, 2, "more than 1 matching bean");
		}
		return result;
	}

	/**
	 * 返回由此工厂管理的对象的实例
	 * （可能是共享的或独立的）。
	 * <p>允许指定显式构造参数，类似于
	 * {@link BeanFactory#getBean(String, Object...)}。
	 * @param args 创建相应实例时要使用的参数
	 * @return bean的一个实例
	 * @throws BeansException 如果创建时出现错误
	 * @see #getObject()
	 */
	default T getObject(Object... args) throws BeansException {
		throw new UnsupportedOperationException("Retrieval with arguments not supported -" +
				"for custom ObjectProvider classes, implement getObject(Object...) for your purposes");
	}

	/**
	 * 返回由此工厂管理的对象的实例
	 * （可能是共享的或独立的）。
	 * @return bean的一个实例，如果不可用则返回{@code null}
	 * @throws BeansException 如果创建时出现错误
	 * @see #getObject()
	 */
	@Nullable
	default T getIfAvailable() throws BeansException {
		try {
			return getObject();
		}
		catch (NoUniqueBeanDefinitionException ex) {
			throw ex;
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}

	/**
	 * 返回由此工厂管理的对象的实例
	 * （可能是共享的或独立的）。
	 * @param defaultSupplier 如果工厂中没有则提供默认对象的回调
	 * @return bean的一个实例，或提供的默认对象
	 * 如果没有这样的bean可用
	 * @throws BeansException 如果创建时出现错误
	 * @since 5.0
	 * @see #getIfAvailable()
	 */
	default T getIfAvailable(Supplier<T> defaultSupplier) throws BeansException {
		T dependency = getIfAvailable();
		return (dependency != null ? dependency : defaultSupplier.get());
	}

	/**
	 * 如果可用，消费由此工厂管理的对象的实例
	 * （可能是共享的或独立的）。
	 * @param dependencyConsumer 处理目标对象的回调
	 * 如果可用（否则不调用）
	 * @throws BeansException 如果创建时出现错误
	 * @since 5.0
	 * @see #getIfAvailable()
	 */
	default void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
		T dependency = getIfAvailable();
		if (dependency != null) {
			dependencyConsumer.accept(dependency);
		}
	}

	/**
	 * 返回由此工厂管理的对象的实例
	 * （可能是共享的或独立的）。
	 * @return bean的一个实例，如果不可用或
	 * 不唯一（即找到多个候选但没有标记为primary的）则返回{@code null}
	 * @throws BeansException 如果创建时出现错误
	 * @see #getObject()
	 */
	@Nullable
	default T getIfUnique() throws BeansException {
		try {
			return getObject();
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}

	/**
	 * 返回由此工厂管理的对象的实例
	 * （可能是共享的或独立的）。
	 * @param defaultSupplier 在工厂中没有唯一候选时提供默认对象的回调
	 * @return bean的一个实例，或提供的默认对象
	 * 如果没有这样的bean可用或如果在工厂中不唯一
	 * （即找到多个候选但没有标记为primary的）
	 * @throws BeansException 如果创建时出现错误
	 * @since 5.0
	 * @see #getIfUnique()
	 */
	default T getIfUnique(Supplier<T> defaultSupplier) throws BeansException {
		T dependency = getIfUnique();
		return (dependency != null ? dependency : defaultSupplier.get());
	}

	/**
	 * 如果唯一，消费由此工厂管理的对象的实例
	 * （可能是共享的或独立的）。
	 * @param dependencyConsumer 处理目标对象的回调
	 * 如果唯一（否则不调用）
	 * @throws BeansException 如果创建时出现错误
	 * @since 5.0
	 * @see #getIfUnique()
	 */
	default void ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
		T dependency = getIfUnique();
		if (dependency != null) {
			dependencyConsumer.accept(dependency);
		}
	}

	/**
	 * 返回所有匹配对象实例的{@link Iterator}，
	 * 没有特定的排序保证（但通常是注册顺序）。
	 * @since 5.1
	 * @see #stream()
	 */
	@Override
	default Iterator<T> iterator() {
		return stream().iterator();
	}

	/**
	 * 返回所有匹配对象实例的顺序{@link Stream}，
	 * 没有特定的排序保证（但通常是注册顺序）。
	 * <p>注意：默认情况下，结果可能会根据注入点与目标bean的限定符以及匹配bean的一般自动装配候选状态进行过滤。
	 * 对于针对类型匹配候选的自定义过滤，请改用{@link #stream(Predicate)}（可能使用{@link #UNFILTERED}）。
	 * @since 5.1
	 * @see #iterator()
	 * @see #orderedStream()
	 */
	default Stream<T> stream() {
		throw new UnsupportedOperationException("Element access not supported - " +
				"for custom ObjectProvider classes, implement stream() to enable all other methods");
	}

	/**
	 * 返回所有匹配对象实例的顺序{@link Stream}，
	 * 根据工厂的通用顺序比较器预排序。
	 * <p>在标准Spring应用程序上下文中，这将根据
	 * {@link org.springframework.core.Ordered}约定进行排序，
	 * 并且在基于注解的配置情况下还考虑
	 * {@link org.springframework.core.annotation.Order}注解，
	 * 类似于list/array类型的多元素注入点。
	 * <p>默认方法将{@link OrderComparator}应用于
	 * {@link #stream()}方法。如果需要，你可以覆盖此方法以应用
	 * {@link org.springframework.core.annotation.AnnotationAwareOrderComparator}。
	 * <p>注意：默认情况下，结果可能会根据注入点与目标bean的限定符以及匹配bean的一般自动装配候选状态进行过滤。
	 * 对于针对类型匹配候选的自定义过滤，请改用{@link #stream(Predicate)}（可能使用{@link #UNFILTERED}）。
	 * @since 5.1
	 * @see #stream()
	 * @see org.springframework.core.OrderComparator
	 */
	default Stream<T> orderedStream() {
		return stream().sorted(OrderComparator.INSTANCE);
	}

	/**
	 * 返回所有匹配对象实例的自定义过滤{@link Stream}，
	 * 没有特定的排序保证（但通常是注册顺序）。
	 * @param customFilter 用于在原始bean类型匹配中选择bean的自定义类型过滤器
	 * （或{@link #UNFILTERED}用于所有原始类型匹配而无需任何默认过滤）
	 * @since 6.2.3
	 * @see #stream()
	 * @see #orderedStream(Predicate)
	 */
	default Stream<T> stream(Predicate<Class<?>> customFilter) {
		return stream(customFilter, true);
	}

	/**
	 * 返回所有匹配对象实例的自定义过滤{@link Stream}，
	 * 根据工厂的通用顺序比较器预排序。
	 * @param customFilter 用于在原始bean类型匹配中选择bean的自定义类型过滤器
	 * （或{@link #UNFILTERED}用于所有原始类型匹配而无需任何默认过滤）
	 * @since 6.2.3
	 * @see #orderedStream()
	 * @see #stream(Predicate)
	 */
	default Stream<T> orderedStream(Predicate<Class<?>> customFilter) {
		return orderedStream(customFilter, true);
	}

	/**
	 * 返回所有匹配对象实例的自定义过滤{@link Stream}，
	 * 没有特定的排序保证（但通常是注册顺序）。
	 * @param customFilter 用于在原始bean类型匹配中选择bean的自定义类型过滤器
	 * （或{@link #UNFILTERED}用于所有原始类型匹配而无需任何默认过滤）
	 * @param includeNonSingletons 是否也包括原型或作用域bean
	 * 或仅包括单例（也适用于FactoryBeans）
	 * @since 6.2.5
	 * @see #stream(Predicate)
	 * @see #orderedStream(Predicate, boolean)
	 */
	default Stream<T> stream(Predicate<Class<?>> customFilter, boolean includeNonSingletons) {
		if (!includeNonSingletons) {
			throw new UnsupportedOperationException("Only supports includeNonSingletons=true by default");
		}
		return stream().filter(obj -> customFilter.test(obj.getClass()));
	}

	/**
	 * 返回所有匹配对象实例的自定义过滤{@link Stream}，
	 * 根据工厂的通用顺序比较器预排序。
	 * @param customFilter 用于在原始bean类型匹配中选择bean的自定义类型过滤器
	 * （或{@link #UNFILTERED}用于所有原始类型匹配而无需任何默认过滤）
	 * @param includeNonSingletons 是否也包括原型或作用域bean
	 * 或仅包括单例（也适用于FactoryBeans）
	 * @since 6.2.5
	 * @see #orderedStream()
	 * @see #stream(Predicate)
	 */
	default Stream<T> orderedStream(Predicate<Class<?>> customFilter, boolean includeNonSingletons) {
		if (!includeNonSingletons) {
			throw new UnsupportedOperationException("Only supports includeNonSingletons=true by default");
		}
		return orderedStream().filter(obj -> customFilter.test(obj.getClass()));
	}

}
