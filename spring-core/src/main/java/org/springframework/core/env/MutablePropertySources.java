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

package org.springframework.core.env;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;

/**
 * The default implementation of the {@link PropertySources} interface.
 * Allows manipulation of contained property sources and provides a constructor
 * for copying an existing {@code PropertySources} instance.
 * <p>{@link PropertySources}接口的默认实现。
 * 允许操作包含的属性源，并提供复制现有{@code PropertySources}实例的构造函数。
 *
 * <p>Where <em>precedence</em> is mentioned in methods such as {@link #addFirst}
 * and {@link #addLast}, this is with regard to the order in which property sources
 * will be searched when resolving a given property with a {@link PropertyResolver}.
 * <p>在诸如{@link #addFirst}和{@link #addLast}等方法中提到的<em>优先级</em>，
 * 是指使用{@link PropertyResolver}解析给定属性时搜索属性源的顺序。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see PropertySourcesPropertyResolver
 */
public class MutablePropertySources implements PropertySources {

	private final List<PropertySource<?>> propertySourceList = new CopyOnWriteArrayList<>();



	/**
	 * Create a new {@link MutablePropertySources} object.
	 * <p>创建一个新的 {@link MutablePropertySources} 对象。
	 */
	public MutablePropertySources() {
	}


	/**
	 * Create a new {@code MutablePropertySources} from the given propertySources
	 * object, preserving the original order of contained {@code PropertySource} objects.
	 * <p>根据给定的 propertySources 对象创建一个新的 {@code MutablePropertySources}，
	 * 保留其中包含的 {@code PropertySource} 对象的原始顺序。
	 */
	public MutablePropertySources(PropertySources propertySources) {
		this();
		for (PropertySource<?> propertySource : propertySources) {
			addLast(propertySource);
		}
	}


	@Override
	public Iterator<PropertySource<?>> iterator() {
		return this.propertySourceList.iterator();
	}

	@Override
	public Spliterator<PropertySource<?>> spliterator() {
		return this.propertySourceList.spliterator();
	}

	@Override
	public Stream<PropertySource<?>> stream() {
		return this.propertySourceList.stream();
	}

	@Override
	public boolean contains(String name) {
		for (PropertySource<?> propertySource : this.propertySourceList) {
			if (propertySource.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	@Nullable
	public PropertySource<?> get(String name) {
		for (PropertySource<?> propertySource : this.propertySourceList) {
			if (propertySource.getName().equals(name)) {
				return propertySource;
			}
		}
		return null;
	}


	/**
	 * Add the given property source object with the highest precedence.
	 * <p>以最高优先级添加给定的属性源对象。
	 */
	public void addFirst(PropertySource<?> propertySource) {
		synchronized (this.propertySourceList) {
			removeIfPresent(propertySource);
			this.propertySourceList.add(0, propertySource);
		}
	}

	/**
	 * Add the given property source object with the lowest precedence.
	 * <p>以最低优先级添加给定的属性源对象。
	 */
	public void addLast(PropertySource<?> propertySource) {
		synchronized (this.propertySourceList) {
			removeIfPresent(propertySource);
			this.propertySourceList.add(propertySource);
		}
	}

	/**
	 * Add the given property source object with precedence immediately higher
	 * than the named relative property source.
	 * <p>以比指定的相对属性源更高的优先级添加给定的属性源对象。
	 */
	public void addBefore(String relativePropertySourceName, PropertySource<?> propertySource) {
		assertLegalRelativeAddition(relativePropertySourceName, propertySource);
		synchronized (this.propertySourceList) {
			removeIfPresent(propertySource);
			int index = assertPresentAndGetIndex(relativePropertySourceName);
			addAtIndex(index, propertySource);
		}
	}

	/**
	 * Add the given property source object with precedence immediately lower
	 * than the named relative property source.
	 * <p>以比指定的相对属性源更低的优先级添加给定的属性源对象。
	 */
	public void addAfter(String relativePropertySourceName, PropertySource<?> propertySource) {
		assertLegalRelativeAddition(relativePropertySourceName, propertySource);
		synchronized (this.propertySourceList) {
			removeIfPresent(propertySource);
			int index = assertPresentAndGetIndex(relativePropertySourceName);
			addAtIndex(index + 1, propertySource);
		}
	}

	/**
	 * Return the precedence of the given property source, {@code -1} if not found.
	 * <p>返回给定属性源的优先级，如果未找到则返回 {@code -1}。
	 */
	public int precedenceOf(PropertySource<?> propertySource) {
		return this.propertySourceList.indexOf(propertySource);
	}

	/**
	 * Remove and return the property source with the given name, {@code null} if not found.
	 * <p>移除并返回具有给定名称的属性源，如果未找到则返回 {@code null}。
	 * @param name the name of the property source to find and remove
	 *           <br>要查找和删除的属性源的名称
	 */
	@Nullable
	public PropertySource<?> remove(String name) {
		synchronized (this.propertySourceList) {
			int index = this.propertySourceList.indexOf(PropertySource.named(name));
			return (index != -1 ? this.propertySourceList.remove(index) : null);
		}
	}


	/**
	 * Replace the property source with the given name with the given property source object.
	 * <p>用给定的属性源对象替换具有指定名称的属性源。
	 * @param name the name of the property source to find and replace
	 *           <br>要查找和替换的属性源的名称
	 * @param propertySource the replacement property source
	 *                      <br>替换的属性源
	 * @throws IllegalArgumentException if no property source with the given name is present
	 *                                <br>如果不存在具有给定名称的属性源，则抛出此异常
	 * @see #contains
	 */
	public void replace(String name, PropertySource<?> propertySource) {
		synchronized (this.propertySourceList) {
			int index = assertPresentAndGetIndex(name);
			this.propertySourceList.set(index, propertySource);
		}
	}

	/**
	 * Return the number of {@link PropertySource} objects contained.
	 * <p>返回包含的 {@link PropertySource} 对象的数量。
	 */
	public int size() {
		return this.propertySourceList.size();
	}

	@Override
	public String toString() {
		return this.propertySourceList.toString();
	}


	/**
	 * Ensure that the given property source is not being added relative to itself.
	 * <p>确保不会相对于自身添加给定的属性源。
	 */
	protected void assertLegalRelativeAddition(String relativePropertySourceName, PropertySource<?> propertySource) {
		String newPropertySourceName = propertySource.getName();
		if (relativePropertySourceName.equals(newPropertySourceName)) {
			throw new IllegalArgumentException(
					"PropertySource named '" + newPropertySourceName + "' cannot be added relative to itself");
		}
	}

	/**
	 * Remove the given property source if it is present.
	 * <p>如果存在，则移除给定的属性源。
	 */
	protected void removeIfPresent(PropertySource<?> propertySource) {
		this.propertySourceList.remove(propertySource);
	}

	/**
	 * Add the given property source at a particular index in the list.
	 * <p>在列表中的特定索引位置添加给定的属性源。
	 */
	private void addAtIndex(int index, PropertySource<?> propertySource) {
		removeIfPresent(propertySource);
		this.propertySourceList.add(index, propertySource);
	}

	/**
	 * Assert that the named property source is present and return its index.
	 * <p>断言指定名称的属性源存在并返回其索引。
	 * @param name {@linkplain PropertySource#getName() name of the property source} to find
	 *           <br>要查找的{@linkplain PropertySource#getName() 属性源名称}
	 * @throws IllegalArgumentException if the named property source is not present
	 *                                <br>如果指定名称的属性源不存在则抛出此异常
	 */
	private int assertPresentAndGetIndex(String name) {
		int index = this.propertySourceList.indexOf(PropertySource.named(name));
		if (index == -1) {
			throw new IllegalArgumentException("PropertySource named '" + name + "' does not exist");
		}
		return index;
	}

}
