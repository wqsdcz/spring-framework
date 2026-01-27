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

import org.springframework.util.ObjectUtils;

/**
 * A {@link PropertySource} implementation capable of interrogating its
 * underlying source object to enumerate all possible property name/value
 * pairs. Exposes the {@link #getPropertyNames()} method to allow callers
 * to introspect available properties without having to access the underlying
 * source object. This also facilitates a more efficient implementation of
 * {@link #containsProperty(String)}, in that it can call {@link #getPropertyNames()}
 * and iterate through the returned array rather than attempting a call to
 * {@link #getProperty(String)} which may be more expensive. Implementations may
 * consider caching the result of {@link #getPropertyNames()} to fully exploit this
 * performance opportunity.
 * <p>一种 {@link PropertySource} 实现，能够查询其底层源对象以枚举所有可能的属性名/值对。
 * 公开 {@link #getPropertyNames()} 方法，允许调用者在不访问底层源对象的情况下检查可用属性。
 * 这也促进了 {@link #containsProperty(String)} 的更高效实现，因为它可以调用 {@link #getPropertyNames()}
 * 并遍历返回的数组，而不是尝试调用可能更昂贵的 {@link #getProperty(String)}。
 * 实现可以考虑缓存 {@link #getPropertyNames()} 的结果以充分利用此性能机会。
 *
 * <p>Most framework-provided {@code PropertySource} implementations are enumerable;
 * a counter-example would be {@code JndiPropertySource} where, due to the
 * nature of JNDI it is not possible to determine all possible property names at
 * any given time; rather it is only possible to try to access a property
 * (via {@link #getProperty(String)}) in order to evaluate whether it is present
 * or not.
 * <p>大多数框架提供的 {@code PropertySource} 实现都是可枚举的；
 * 一个反例是 {@code JndiPropertySource}，由于JNDI的性质，在任何给定时间都无法确定所有可能的属性名称；
 * 而只能尝试访问属性（通过 {@link #getProperty(String)}）来评估它是否存在。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @param <T> the source type
 */
public abstract class EnumerablePropertySource<T> extends PropertySource<T> {

	/**
	 * Create a new {@code EnumerablePropertySource} with the given name and source object.
	 * <p>使用给定的名称和源对象创建一个新的 {@code EnumerablePropertySource}。
	 * @param name the associated name
	 * <p>关联的名称
	 * @param source the source object
	 * <p>源对象
	 */
	public EnumerablePropertySource(String name, T source) {
		super(name, source);
	}

	/**
	 * Create a new {@code EnumerablePropertySource} with the given name and with a new
	 * {@code Object} instance as the underlying source.
	 * <p>使用给定的名称和一个新的 {@code Object} 实例作为底层源创建一个新的 {@code EnumerablePropertySource}。
	 * @param name the associated name
	 * <p>关联的名称
	 */
	protected EnumerablePropertySource(String name) {
		super(name);
	}


	/**
	 * Return whether this {@code PropertySource} contains a property with the given name.
	 * <p>这个实现检查给定名称是否存在于 {@link #getPropertyNames()} 数组中。
	 * <p>判断此 {@code PropertySource} 是否包含具有给定名称的属性。
	 * @param name the name of the property to find
	 * <p>要查找的属性的名称
	 */
	@Override
	public boolean containsProperty(String name) {
		return ObjectUtils.containsElement(getPropertyNames(), name);
	}

	/**
	 * Return the names of all properties contained by the
	 * {@linkplain #getSource() source} object (never {@code null}).
	 * <p>返回由 {@linkplain #getSource() 源} 对象包含的所有属性的名称（永远不会为 {@code null}）。
	 */
	public abstract String[] getPropertyNames();

}
