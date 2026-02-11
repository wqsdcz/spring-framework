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

package org.springframework.beans.factory.support;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanIsNotAFactoryException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 静态 {@link org.springframework.beans.factory.BeanFactory} 实现，允许以编程方式注册现有的单例实例。
 *
 * <p>
 *     不支持原型 bean 或别名。
 * <p>
 *     作为 {@link org.springframework.beans.factory.ListableBeanFactory} 接口的简单实现示例，
 *     管理现有的 bean 实例而不是基于 bean 定义创建新实例，并且不实现任何扩展的 SPI 接口
 *     （例如{@link org.springframework.beans.factory.config.ConfigurableBeanFactory}）。
 * <p>
 *     对于基于 bean 定义的完整功能工厂，请参见{@link DefaultListableBeanFactory}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 06.01.2003
 * @see DefaultListableBeanFactory
 */
public class StaticListableBeanFactory implements ListableBeanFactory {

	/** 从 bean 名称到 bean 实例的映射。 */
	private final Map<String, Object> beans;


	/**
	 * 创建一个常规的 {@code StaticListableBeanFactory}，通过 {@link #addBean} 调用来填充单例 bean 实例。
	 */
	public StaticListableBeanFactory() {
		this.beans = new LinkedHashMap<>();
	}

	/**
	 * 创建一个包装给定 {@code Map} 的 {@code StaticListableBeanFactory}。
	 * <p>
	 *     请注意，给定的 {@code Map} 可能已预先填充了 bean；
	 *     或者是新的，仍然允许通过 {@link #addBean} 注册 bean；
	 *     或者是 {@link java.util.Collections#emptyMap()} 用于强制对空 bean 集合进行操作的虚拟工厂。
	 *
	 * @param beans 用于保存此工厂 bean 的 {@code Map}，键为 bean 名称，值为对应的单例对象
	 * @since 4.3
	 */
	public StaticListableBeanFactory(Map<String, Object> beans) {
		Assert.notNull(beans, "Beans Map must not be null");
		this.beans = beans;
	}


	/**
	 * 添加一个新的单例 bean。
	 * <p>将会覆盖给定名称的任何现有实例。
	 * @param name bean 的名称
	 * @param bean bean 实例
	 */
	public void addBean(String name, Object bean) {
		this.beans.put(name, bean);
	}


	//---------------------------------------------------------------------
	// BeanFactory接口的实现
	//---------------------------------------------------------------------

	@Override
	public Object getBean(String name) throws BeansException {
		String beanName = BeanFactoryUtils.transformedBeanName(name);
		Object bean = obtainBean(beanName);

		if (BeanFactoryUtils.isFactoryDereference(name) && !(bean instanceof FactoryBean)) {
			throw new BeanIsNotAFactoryException(beanName, bean.getClass());
		}

		if (bean instanceof FactoryBean<?> factoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
			try {
				Object exposedObject = factoryBean.getObject();
				if (exposedObject == null) {
					throw new BeanCreationException(beanName, "FactoryBean exposed null object");
				}
				return exposedObject;
			}
			catch (Exception ex) {
				throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
			}
		}
		else {
			return bean;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getBean(String name, @Nullable Class<T> requiredType) throws BeansException {
		Object bean = getBean(name);
		if (requiredType != null && !requiredType.isInstance(bean)) {
			throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
		}
		return (T) bean;
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		if (!ObjectUtils.isEmpty(args)) {
			throw new UnsupportedOperationException(
					"StaticListableBeanFactory does not support explicit bean creation arguments");
		}
		return getBean(name);
	}

	private Object obtainBean(String beanName) {
		Object bean = this.beans.get(beanName);
		if (bean == null) {
			throw new NoSuchBeanDefinitionException(beanName,
					"Defined beans are [" + StringUtils.collectionToCommaDelimitedString(this.beans.keySet()) + "]");
		}
		return bean;
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		String[] beanNames = getBeanNamesForType(requiredType);
		if (beanNames.length == 1) {
			return getBean(beanNames[0], requiredType);
		}
		else if (beanNames.length > 1) {
			throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
		}
		else {
			throw new NoSuchBeanDefinitionException(requiredType);
		}
	}

	@Override
	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		if (!ObjectUtils.isEmpty(args)) {
			throw new UnsupportedOperationException(
					"StaticListableBeanFactory does not support explicit bean creation arguments");
		}
		return getBean(requiredType);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) throws BeansException {
		return getBeanProvider(ResolvableType.forRawClass(requiredType), true);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		return getBeanProvider(requiredType, true);
	}

	@Override
	public boolean containsBean(String name) {
		return this.beans.containsKey(name);
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		String beanName = BeanFactoryUtils.transformedBeanName(name);
		Object bean = obtainBean(beanName);
		if (bean instanceof FactoryBean<?> factoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
			return factoryBean.isSingleton();
		}
		return true;
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		String beanName = BeanFactoryUtils.transformedBeanName(name);
		Object bean = obtainBean(beanName);
		return (!BeanFactoryUtils.isFactoryDereference(name) &&
				((bean instanceof SmartFactoryBean<?> smartFactoryBean && smartFactoryBean.isPrototype()) ||
				(bean instanceof FactoryBean<?> factoryBean && !factoryBean.isSingleton())));
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		Class<?> type = getType(name);
		return (type != null && typeToMatch.isAssignableFrom(type));
	}

	@Override
	public boolean isTypeMatch(String name, @Nullable Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		Class<?> type = getType(name);
		return (typeToMatch == null || (type != null && typeToMatch.isAssignableFrom(type)));
	}

	@Override
	@Nullable
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return getType(name, true);
	}

	@Override
	@Nullable
	public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
		String beanName = BeanFactoryUtils.transformedBeanName(name);
		Object bean = obtainBean(beanName);
		if (bean instanceof FactoryBean<?> factoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
			return factoryBean.getObjectType();
		}
		return bean.getClass();
	}

	@Override
	public String[] getAliases(String name) {
		return new String[0];
	}


	//---------------------------------------------------------------------
	// ListableBeanFactory接口的实现
	//---------------------------------------------------------------------

	@Override
	public boolean containsBeanDefinition(String name) {
		return this.beans.containsKey(name);
	}

	@Override
	public int getBeanDefinitionCount() {
		return this.beans.size();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		return StringUtils.toStringArray(this.beans.keySet());
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
		return getBeanProvider(ResolvableType.forRawClass(requiredType), allowEagerInit);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
		return new ObjectProvider<>() {
			@Override
			public T getObject() throws BeansException {
				String[] beanNames = getBeanNamesForType(requiredType);
				if (beanNames.length == 1) {
					return (T) getBean(beanNames[0]);
				}
				else if (beanNames.length > 1) {
					throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
				}
				else {
					throw new NoSuchBeanDefinitionException(requiredType);
				}
			}
			@Override
			public T getObject(Object... args) throws BeansException {
				String[] beanNames = getBeanNamesForType(requiredType);
				if (beanNames.length == 1) {
					return (T) getBean(beanNames[0], args);
				}
				else if (beanNames.length > 1) {
					throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
				}
				else {
					throw new NoSuchBeanDefinitionException(requiredType);
				}
			}
			@Override
			@Nullable
			public T getIfAvailable() throws BeansException {
				String[] beanNames = getBeanNamesForType(requiredType);
				if (beanNames.length == 1) {
					return (T) getBean(beanNames[0]);
				}
				else if (beanNames.length > 1) {
					throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
				}
				else {
					return null;
				}
			}
			@Override
			@Nullable
			public T getIfUnique() throws BeansException {
				String[] beanNames = getBeanNamesForType(requiredType);
				if (beanNames.length == 1) {
					return (T) getBean(beanNames[0]);
				}
				else {
					return null;
				}
			}
			@Override
			public Stream<T> stream() {
				return Arrays.stream(getBeanNamesForType(requiredType)).map(name -> (T) getBean(name));
			}
		};
	}

	@Override
	public String[] getBeanNamesForType(@Nullable ResolvableType type) {
		return getBeanNamesForType(type, true, true);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable ResolvableType type,
			boolean includeNonSingletons, boolean allowEagerInit) {

		Class<?> resolved = (type != null ? type.resolve() : null);
		boolean isFactoryType = (resolved != null && FactoryBean.class.isAssignableFrom(resolved));
		List<String> matches = new ArrayList<>();

		for (Map.Entry<String, Object> entry : this.beans.entrySet()) {
			String beanName = entry.getKey();
			Object beanInstance = entry.getValue();
			if (beanInstance instanceof FactoryBean<?> factoryBean && !isFactoryType) {
				Class<?> objectType = factoryBean.getObjectType();
				if ((includeNonSingletons || factoryBean.isSingleton()) &&
						(type == null || (objectType != null && type.isAssignableFrom(objectType)))) {
					matches.add(beanName);
				}
			}
			else {
				if (type == null || type.isInstance(beanInstance)) {
					matches.add(beanName);
				}
			}
		}
		return StringUtils.toStringArray(matches);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type) {
		return getBeanNamesForType(ResolvableType.forClass(type));
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		return getBeanNamesForType(ResolvableType.forClass(type), includeNonSingletons, allowEagerInit);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
		return getBeansOfType(type, true, true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		boolean isFactoryType = (type != null && FactoryBean.class.isAssignableFrom(type));
		Map<String, T> matches = new LinkedHashMap<>();

		for (Map.Entry<String, Object> entry : this.beans.entrySet()) {
			String beanName = entry.getKey();
			Object beanInstance = entry.getValue();
			if (beanInstance instanceof FactoryBean<?> factoryBean && !isFactoryType) {
				Class<?> objectType = factoryBean.getObjectType();
				if ((includeNonSingletons || factoryBean.isSingleton()) &&
						(type == null || (objectType != null && type.isAssignableFrom(objectType)))) {
					matches.put(beanName, getBean(beanName, type));
				}
			}
			else {
				if (type == null || type.isInstance(beanInstance)) {
					if (isFactoryType) {
						beanName = FACTORY_BEAN_PREFIX + beanName;
					}
					matches.put(beanName, (T) beanInstance);
				}
			}
		}
		return matches;
	}

	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		List<String> results = new ArrayList<>();
		for (String beanName : this.beans.keySet()) {
			if (findAnnotationOnBean(beanName, annotationType) != null) {
				results.add(beanName);
			}
		}
		return StringUtils.toStringArray(results);
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {

		Map<String, Object> results = new LinkedHashMap<>();
		for (String beanName : this.beans.keySet()) {
			if (findAnnotationOnBean(beanName, annotationType) != null) {
				results.put(beanName, getBean(beanName));
			}
		}
		return results;
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException {

		return findAnnotationOnBean(beanName, annotationType, true);
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {

		Class<?> beanType = getType(beanName, allowFactoryBeanInit);
		return (beanType != null ? AnnotatedElementUtils.findMergedAnnotation(beanType, annotationType) : null);
	}

	@Override
	public <A extends Annotation> Set<A> findAllAnnotationsOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {

		Class<?> beanType = getType(beanName, allowFactoryBeanInit);
		return (beanType != null ?
				AnnotatedElementUtils.findAllMergedAnnotations(beanType, annotationType) : Collections.emptySet());
	}

}
