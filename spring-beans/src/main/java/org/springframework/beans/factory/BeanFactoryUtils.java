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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 在bean工厂上操作的便利方法，特别是在{@link ListableBeanFactory}接口上。
 *
 * <p>返回bean计数、bean名称或bean实例，
 * 考虑bean工厂的嵌套层次结构（ListableBeanFactory接口上定义的方法不这样做，
 * 与BeanFactory接口上定义的方法相反）。
 *
 * <p><b>注意：</b>通常最好使用{@link BeanFactory#getBeanProvider}通过{@link ObjectProvider#stream()}
 * 而不是此工具类。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 04.07.2003
 * @see BeanFactory#getBeanProvider
 */
public abstract class BeanFactoryUtils {

	/**
	 * 生成的bean名称的分隔符。如果类名或父名不唯一，
	 * 将附加"#1"、"#2"等，直到名称变为唯一。
	 */
	public static final String GENERATED_BEAN_NAME_SEPARATOR = "#";

	/**
	 * 从带有工厂bean前缀的名称缓存到不带解引用的剥离名称。
	 * @since 5.1
	 * @see BeanFactory#FACTORY_BEAN_PREFIX
	 */
	private static final Map<String, String> transformedBeanNameCache = new ConcurrentHashMap<>();


	/**
	 * 返回给定名称是否是工厂解引用
	 * （以工厂解引用前缀开头）。
	 * @param name bean的名称
	 * @return 给定名称是否是工厂解引用
	 * @see BeanFactory#FACTORY_BEAN_PREFIX
	 */
	public static boolean isFactoryDereference(@Nullable String name) {
		return (name != null && !name.isEmpty() && name.charAt(0) == BeanFactory.FACTORY_BEAN_PREFIX_CHAR);
	}

	/**
	 * 返回实际的bean名称，剥离工厂解引用
	 * 前缀（如果有，也剥离重复的工厂前缀）。
	 * @param name bean的名称
	 * @return 转换后的名称
	 * @see BeanFactory#FACTORY_BEAN_PREFIX
	 */
	public static String transformedBeanName(String name) {
		Assert.notNull(name, "'name' must not be null");
		if (name.isEmpty() || name.charAt(0) != BeanFactory.FACTORY_BEAN_PREFIX_CHAR) {
			return name;
		}
		return transformedBeanNameCache.computeIfAbsent(name, beanName -> {
			do {
				beanName = beanName.substring(1);  // length of '&'
			}
			while (beanName.charAt(0) == BeanFactory.FACTORY_BEAN_PREFIX_CHAR);
			return beanName;
		});
	}

	/**
	 * 返回给定名称是否是由默认命名策略生成的bean名称
	 * （包含"#..."部分）。
	 * @param name bean的名称
	 * @return 给定名称是否是生成的bean名称
	 * @see #GENERATED_BEAN_NAME_SEPARATOR
	 * @see org.springframework.beans.factory.support.BeanDefinitionReaderUtils#generateBeanName
	 * @see org.springframework.beans.factory.support.DefaultBeanNameGenerator
	 */
	public static boolean isGeneratedBeanName(@Nullable String name) {
		return (name != null && name.contains(GENERATED_BEAN_NAME_SEPARATOR));
	}

	/**
	 * 从给定的（可能生成的）bean名称中提取"原始"bean名称，
	 * 排除为唯一性而添加的任何"#..."后缀。
	 * @param name 可能生成的bean名称
	 * @return 原始bean名称
	 * @see #GENERATED_BEAN_NAME_SEPARATOR
	 */
	public static String originalBeanName(String name) {
		Assert.notNull(name, "'name' must not be null");
		int separatorIndex = name.indexOf(GENERATED_BEAN_NAME_SEPARATOR);
		return (separatorIndex != -1 ? name.substring(0, separatorIndex) : name);
	}


	// Retrieval of bean names

	/**
	 * 统计此工厂参与的任何层次结构中的所有bean。
	 * 包括祖先bean工厂的计数。
	 * <p>被"覆盖"的bean（在后代工厂中以相同名称指定）
	 * 只计算一次。
	 * @param lbf bean工厂
	 * @return bean计数，包括在祖先工厂中定义的bean
	 * @see #beanNamesIncludingAncestors
	 */
	public static int countBeansIncludingAncestors(ListableBeanFactory lbf) {
		return beanNamesIncludingAncestors(lbf).length;
	}

	/**
	 * 返回工厂中的所有bean名称，包括祖先工厂。
	 * @param lbf bean工厂
	 * @return 匹配的bean名称数组，如果没有则返回空数组
	 * @see #beanNamesForTypeIncludingAncestors
	 */
	public static String[] beanNamesIncludingAncestors(ListableBeanFactory lbf) {
		return beanNamesForTypeIncludingAncestors(lbf, Object.class);
	}

	/**
	 * 获取给定类型的所有bean名称，包括在祖先
	 * 工厂中定义的。在bean定义被覆盖的情况下将返回唯一名称。
	 * <p>确实考虑由FactoryBeans创建的对象，这意味着FactoryBeans
	 * 将被初始化。如果FactoryBean创建的对象不匹配，
	 * 原始FactoryBean本身将与类型匹配。
	 * <p>此版本的{@code beanNamesForTypeIncludingAncestors}自动
	 * 包括原型和FactoryBeans。
	 * @param lbf bean工厂
	 * @param type bean必须匹配的类型（作为{@code ResolvableType}）
	 * @return 匹配的bean名称数组，如果没有则返回空数组
	 * @since 4.2
	 * @see ListableBeanFactory#getBeanNamesForType(ResolvableType)
	 */
	public static String[] beanNamesForTypeIncludingAncestors(ListableBeanFactory lbf, ResolvableType type) {
		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForType(type);
		if (lbf instanceof HierarchicalBeanFactory hbf) {
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory pbf) {
				String[] parentResult = beanNamesForTypeIncludingAncestors(pbf, type);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}

	/**
	 * 获取给定类型的所有bean名称，包括在祖先
	 * 工厂中定义的。在bean定义被覆盖的情况下将返回唯一名称。
	 * <p>如果设置了"allowEagerInit"标志，则考虑由FactoryBeans创建的对象，
	 * 这意味着FactoryBeans将被初始化。如果FactoryBean创建的对象不匹配，
	 * 原始FactoryBean本身将与类型匹配。如果未设置"allowEagerInit"，
	 * 则只检查原始FactoryBeans（这不需要初始化每个FactoryBean）。
	 * @param lbf bean工厂
	 * @param type bean必须匹配的类型（作为{@code ResolvableType}）
	 * @param includeNonSingletons 是否也包括原型或作用域bean
	 * 或仅包括单例（也适用于FactoryBeans）
	 * @param allowEagerInit 是否为类型检查初始化<i>延迟初始化单例</i>和
	 * <i>由FactoryBeans创建的对象</i>（或由带有"factory-bean"引用的工厂方法创建的）。
	 * 注意，FactoryBeans需要立即初始化以确定其类型：因此请注意，为此标志传递"true"
	 * 将初始化FactoryBeans和"factory-bean"引用。
	 * @return 匹配的bean名称数组，如果没有则返回空数组
	 * @since 5.2
	 * @see ListableBeanFactory#getBeanNamesForType(ResolvableType, boolean, boolean)
	 */
	public static String[] beanNamesForTypeIncludingAncestors(
			ListableBeanFactory lbf, ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		if (lbf instanceof HierarchicalBeanFactory hbf) {
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory pbf) {
				String[] parentResult = beanNamesForTypeIncludingAncestors(
						pbf, type, includeNonSingletons, allowEagerInit);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}

	/**
	 * 获取给定类型的所有bean名称，包括在祖先
	 * 工厂中定义的。在bean定义被覆盖的情况下将返回唯一名称。
	 * <p>确实考虑由FactoryBeans创建的对象，这意味着FactoryBeans
	 * 将被初始化。如果FactoryBean创建的对象不匹配，
	 * 原始FactoryBean本身将与类型匹配。
	 * <p>此版本的{@code beanNamesForTypeIncludingAncestors}自动
	 * 包括原型和FactoryBeans。
	 * @param lbf bean工厂
	 * @param type bean必须匹配的类型（作为{@code Class}）
	 * @return 匹配的bean名称数组，如果没有则返回空数组
	 * @see ListableBeanFactory#getBeanNamesForType(Class)
	 */
	public static String[] beanNamesForTypeIncludingAncestors(ListableBeanFactory lbf, Class<?> type) {
		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForType(type);
		if (lbf instanceof HierarchicalBeanFactory hbf) {
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory pbf) {
				String[] parentResult = beanNamesForTypeIncludingAncestors(pbf, type);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}

	/**
	 * 获取给定类型的所有bean名称，包括在祖先工厂中定义的bean。在bean定义被覆盖的情况下将返回唯一名称。
	 * <p>
	 *     如果设置了"allowEagerInit"标志，则会考虑由FactoryBeans创建的对象，这意味着FactoryBeans将被初始化。
	 *     如果FactoryBean创建的对象不匹配，原始FactoryBean本身将与类型匹配。
	 *     如果未设置"allowEagerInit"，则只检查原始FactoryBeans（这不需要初始化每个FactoryBean）。
	 *
	 * @param lbf bean工厂
	 * @param includeNonSingletons 是否也包括原型或作用域bean或仅包括单例（也适用于FactoryBeans）
	 * @param allowEagerInit 是否为类型检查初始化<i>延迟初始化单例</i>和
	 * <i>由FactoryBeans创建的对象</i>（或由带有"factory-bean"引用的工厂方法创建的）。
	 * 注意，FactoryBeans需要立即初始化以确定其类型：因此请注意，为此标志传递"true"
	 * 将初始化FactoryBeans和"factory-bean"引用。
	 * @param type bean必须匹配的类型
	 * @return 匹配的bean名称数组，如果没有则返回空数组
	 * @see ListableBeanFactory#getBeanNamesForType(Class, boolean, boolean)
	 */
	public static String[] beanNamesForTypeIncludingAncestors(
			ListableBeanFactory lbf, Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		if (lbf instanceof HierarchicalBeanFactory hbf) {
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory pbf) {
				String[] parentResult = beanNamesForTypeIncludingAncestors(
						pbf, type, includeNonSingletons, allowEagerInit);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}

	/**
	 * 获取所有其 {@code Class} 具有提供的 {@link Annotation} 类型的 bean 名称，
	 * 包括在祖先工厂中定义的 bean，而不创建任何 bean 实例。
	 * 在 bean 定义被覆盖的情况下将返回唯一名称。
	 * @param lbf bean 工厂
	 * @param annotationType 要查找的注解类型
	 * @return 匹配的 bean 名称数组，如果没有则返回空数组
	 * @since 5.0
	 * @see ListableBeanFactory#getBeanNamesForAnnotation(Class)
	 */
	public static String[] beanNamesForAnnotationIncludingAncestors(
			ListableBeanFactory lbf, Class<? extends Annotation> annotationType) {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForAnnotation(annotationType);
		if (lbf instanceof HierarchicalBeanFactory hbf) {
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory pbf) {
				String[] parentResult = beanNamesForAnnotationIncludingAncestors(pbf, annotationType);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}


	// Retrieval of bean instances

	/**
	 * 返回给定类型或子类型的所有bean，如果当前bean工厂是HierarchicalBeanFactory，
	 * 还包括在祖先bean工厂中定义的bean。
	 * 返回的Map将只包含此类型的bean。
	 * <p>确实考虑由FactoryBeans创建的对象，这意味着FactoryBeans
	 * 将被初始化。如果FactoryBean创建的对象不匹配，
	 * 原始FactoryBean本身将与类型匹配。
	 * <p><b>注意：相同名称的bean将在"最低"工厂级别优先，
	 * 即此类bean将从找到它们的最低工厂返回，
	 * 隐藏祖先工厂中的相应bean。</b> 此功能允许通过在子工厂中显式选择相同的bean名称来"替换"bean；
	 * 然后祖先工厂中的bean将不可见，甚至对于按类型查找也是如此。
	 * @param lbf bean工厂
	 * @param type 要匹配的bean类型
	 * @return 匹配bean实例的Map，如果没有则返回空Map
	 * @throws BeansException 如果无法创建bean
	 * @see ListableBeanFactory#getBeansOfType(Class)
	 */
	public static <T> Map<String, T> beansOfTypeIncludingAncestors(ListableBeanFactory lbf, Class<T> type)
			throws BeansException {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		Map<String, T> result = new LinkedHashMap<>(4);
		result.putAll(lbf.getBeansOfType(type));
		if (lbf instanceof HierarchicalBeanFactory hbf) {
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory pbf) {
				Map<String, T> parentResult = beansOfTypeIncludingAncestors(pbf, type);
				parentResult.forEach((beanName, beanInstance) -> {
					if (!result.containsKey(beanName) && !hbf.containsLocalBean(beanName)) {
						result.put(beanName, beanInstance);
					}
				});
			}
		}
		return result;
	}

	/**
	 * 返回给定类型或子类型的所有bean，如果当前bean工厂是HierarchicalBeanFactory，还包括在祖先bean工厂中定义的bean。
	 * 返回的Map将只包含此类型的bean。
	 * <p>
	 *     如果设置了"allowEagerInit"标志，则考虑由FactoryBeans创建的对象，这意味着FactoryBeans将被初始化。
	 *     如果FactoryBean创建的对象不匹配，原始FactoryBean本身将与类型匹配。
	 *     如果未设置"allowEagerInit"，则只检查原始FactoryBeans（这不需要初始化每个FactoryBean）。
	 * <p>
	 *     <b>注意：相同名称的bean将在"最低"工厂级别优先，即此类bean将从找到它们的最低工厂返回，隐藏祖先工厂中的相应bean。</b>
	 *     此功能允许通过在子工厂中显式选择相同的bean名称来"替换"bean；然后祖先工厂中的bean将不可见，甚至对于按类型查找也是如此。
	 *
	 * @param lbf bean工厂
	 * @param type 要匹配的bean类型
	 * @param includeNonSingletons 是否也包括原型或作用域bean或仅包括单例（也适用于FactoryBeans）
	 * @param allowEagerInit 是否为类型检查初始化<i>延迟初始化单例</i>和<i>由FactoryBeans创建的对象</i>（或由带有"factory-bean"引用的工厂方法创建的）。
	 * 注意，FactoryBeans需要立即初始化以确定其类型：因此请注意，为此标志传递"true"将初始化FactoryBeans和"factory-bean"引用。
	 * @return 匹配bean实例的Map，如果没有则返回空Map
	 * @throws BeansException 如果无法创建bean
	 * @see ListableBeanFactory#getBeansOfType(Class, boolean, boolean)
	 */
	public static <T> Map<String, T> beansOfTypeIncludingAncestors(
			ListableBeanFactory lbf, Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		Map<String, T> result = new LinkedHashMap<>(4);
		result.putAll(lbf.getBeansOfType(type, includeNonSingletons, allowEagerInit));
		if (lbf instanceof HierarchicalBeanFactory hbf) {
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory pbf) {
				Map<String, T> parentResult = beansOfTypeIncludingAncestors(pbf, type, includeNonSingletons, allowEagerInit);
				parentResult.forEach((beanName, beanInstance) -> {
					if (!result.containsKey(beanName) && !hbf.containsLocalBean(beanName)) {
						result.put(beanName, beanInstance);
					}
				});
			}
		}
		return result;
	}

	/**
	 * 返回给定类型或子类型的一个bean，如果当前bean工厂是HierarchicalBeanFactory，也会获取在祖先bean工厂中定义的bean。
	 * 当我们期望一个bean且不关心bean名称时，这是一个有用的便捷方法。
	 * <p>
	 *     会考虑由FactoryBeans创建的对象，这意味着FactoryBeans将被初始化。
	 *     如果FactoryBean创建的对象不匹配，原始FactoryBean本身将与类型进行匹配。
	 * <p>
	 *     此版本的{@code beanOfTypeIncludingAncestors}自动包括原型和FactoryBeans。
	 * <p>
	 *     <b>注意：同名的bean将在“最低”工厂级别优先，即此类bean将从找到它们的最低工厂返回，隐藏祖先工厂中的相应bean。</b>
	 *     此功能允许通过在子工厂中显式选择相同的bean名称来“替换”bean；然后祖先工厂中的bean将不可见，甚至对于按类型查找也是如此。
	 *
	 * @param lbf bean工厂
	 * @param type 要匹配的bean类型
	 * @return 匹配的bean实例
	 * @throws NoSuchBeanDefinitionException 如果找不到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 * @throws BeansException 如果无法创建bean
	 * @see #beansOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	public static <T> T beanOfTypeIncludingAncestors(ListableBeanFactory lbf, Class<T> type)
			throws BeansException {

		Map<String, T> beansOfType = beansOfTypeIncludingAncestors(lbf, type);
		return uniqueBean(type, beansOfType);
	}
	/**
	 * 返回给定类型或子类型的一个bean，如果当前bean工厂是HierarchicalBeanFactory，也会获取在祖先bean工厂中定义的bean。
	 * 当我们期望一个bean且不关心bean名称时，这是一个有用的便捷方法。
	 * <p>
	 *     如果设置了"allowEagerInit"标志，则会考虑由FactoryBeans创建的对象，这意味着FactoryBeans将被初始化。
	 *     如果FactoryBean创建的对象不匹配，原始FactoryBean本身将与类型进行匹配。
	 *     如果未设置"allowEagerInit"，则只会检查原始FactoryBeans（这不需要初始化每个FactoryBean）。
	 * <p>
	 *     <b>注意：同名的bean将在“最低”工厂级别优先，即此类bean将从找到它们的最低工厂返回，隐藏祖先工厂中的相应bean。</b>
	 *     此功能允许通过在子工厂中显式选择相同的bean名称来“替换”bean；然后祖先工厂中的bean将不可见，甚至对于按类型查找也是如此。
	 *
	 * @param lbf bean工厂
	 * @param type 要匹配的bean类型
	 * @param includeNonSingletons 是否也包括原型或作用域bean，还是仅包括单例（也适用于FactoryBeans）
	 * @param allowEagerInit 是否为类型检查初始化<i>延迟初始化单例</i>和<i>由FactoryBeans创建的对象</i>
	 * （或由带有"factory-bean"引用的工厂方法创建的）。请注意，FactoryBeans需要立即初始化以确定其类型：
	 * 因此请注意，为此标志传递"true"将初始化FactoryBeans和"factory-bean"引用。
	 * @return 匹配的bean实例
	 * @throws NoSuchBeanDefinitionException 如果找不到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 * @throws BeansException 如果无法创建bean
	 * @see #beansOfTypeIncludingAncestors(ListableBeanFactory, Class, boolean, boolean)
	 */
	public static <T> T beanOfTypeIncludingAncestors(
			ListableBeanFactory lbf, Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		Map<String, T> beansOfType = beansOfTypeIncludingAncestors(lbf, type, includeNonSingletons, allowEagerInit);
		return uniqueBean(type, beansOfType);
	}

	/**
	 * 返回给定类型或子类型的一个bean，不查找祖先工厂。
	 * 当我们期望一个bean且不关心bean名称时，这是一个有用的便捷方法。
	 * <p>
	 *     会考虑由FactoryBeans创建的对象，这意味着FactoryBeans将被初始化。
	 *     如果FactoryBean创建的对象不匹配，原始FactoryBean本身将与类型进行匹配。
	 * <p>
	 *     此版本的{@code beanOfType}自动包括原型和FactoryBeans。
	 *
	 * @param lbf bean工厂
	 * @param type 要匹配的bean类型
	 * @return 匹配的bean实例
	 * @throws NoSuchBeanDefinitionException 如果找不到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 * @throws BeansException 如果无法创建bean
	 * @see ListableBeanFactory#getBeansOfType(Class)
	 */
	public static <T> T beanOfType(ListableBeanFactory lbf, Class<T> type) throws BeansException {
		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		Map<String, T> beansOfType = lbf.getBeansOfType(type);
		return uniqueBean(type, beansOfType);
	}

	/**
	 * 返回给定类型或子类型的一个bean，不查找祖先工厂。
	 * 当我们期望一个bean且不关心bean名称时，这是一个有用的便捷方法。
	 * <p>
	 *     如果设置了"allowEagerInit"标志，则会考虑由FactoryBeans创建的对象，这意味着FactoryBeans将被初始化。
	 *     如果FactoryBean创建的对象不匹配，原始FactoryBean本身将与类型进行匹配。
	 *     如果未设置"allowEagerInit"，则只会检查原始FactoryBeans（这不需要初始化每个FactoryBean）。
	 *
	 * @param lbf bean工厂
	 * @param type 要匹配的bean类型
	 * @param includeNonSingletons 是否也包括原型或作用域bean，还是仅包括单例（也适用于FactoryBeans）
	 * @param allowEagerInit 是否为类型检查初始化<i>延迟初始化单例</i>和<i>由FactoryBeans创建的对象</i>
	 * （或由带有"factory-bean"引用的工厂方法创建的）。请注意，FactoryBeans需要立即初始化以确定其类型：
	 * 因此请注意，为此标志传递"true"将初始化FactoryBeans和"factory-bean"引用。
	 * @return 匹配的bean实例
	 * @throws NoSuchBeanDefinitionException 如果找不到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 * @throws BeansException 如果无法创建bean
	 * @see ListableBeanFactory#getBeansOfType(Class, boolean, boolean)
	 */
	public static <T> T beanOfType(
			ListableBeanFactory lbf, Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		Map<String, T> beansOfType = lbf.getBeansOfType(type, includeNonSingletons, allowEagerInit);
		return uniqueBean(type, beansOfType);
	}


	/**
	 * 将给定的bean名称结果与给定的父结果合并。
	 * @param result 本地bean名称结果
	 * @param parentResult 父bean名称结果（可能为空）
	 * @param hbf 本地bean工厂
	 * @return 合并后的结果（可能是本地结果原样）
	 * @since 4.3.15
	 */
	private static String[] mergeNamesWithParent(String[] result, String[] parentResult, HierarchicalBeanFactory hbf) {
		if (parentResult.length == 0) {
			return result;
		}
		List<String> merged = new ArrayList<>(result.length + parentResult.length);
		merged.addAll(Arrays.asList(result));
		for (String beanName : parentResult) {
			if (!merged.contains(beanName) && !hbf.containsLocalBean(beanName)) {
				merged.add(beanName);
			}
		}
		return StringUtils.toStringArray(merged);
	}

	/**
	 * 从给定的匹配bean Map中提取给定类型的唯一bean。
	 * @param type 要匹配的bean类型
	 * @param matchingBeans 找到的所有匹配bean
	 * @return 唯一的bean实例
	 * @throws NoSuchBeanDefinitionException 如果没有找到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 */
	private static <T> T uniqueBean(Class<T> type, Map<String, T> matchingBeans) {
		int count = matchingBeans.size();
		if (count == 1) {
			return matchingBeans.values().iterator().next();
		}
		else if (count > 1) {
			throw new NoUniqueBeanDefinitionException(type, matchingBeans.keySet());
		}
		else {
			throw new NoSuchBeanDefinitionException(type);
		}
	}

}
