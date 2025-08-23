/*
 * Copyright 2002-2015 the original author or authors.
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
 * <p>这是一个能够探查其底层源对象以列举所有可能的属性名/值对的 {@link PropertySource} 实现。
 * 该实现通过暴露 {@link #getPropertyNames()} 方法，允许调用者在无需访问底层源对象的情况下探查可用属性。
 * 这也优化了 {@link #containsProperty(String)} 的实现效率——该方法可调用 {@link #getPropertyNames()} 并遍历返回的数组，
 * 而非尝试调用可能开销更大的 {@link #getProperty(String)}。实现类可考虑缓存 {@link #getPropertyNames()} 的结果以充分提升性能。<p/>
 *
 * <p>大多数框架提供的 {@code PropertySource} 实现都是可枚举的；典型反例是 {@code JndiPropertySource}：由于 JNDI 的特性限制，
 * 无法在任何给定时间确定所有可能的属性名；仅能通过尝试访问属性（通过 {@link #getProperty(String)}）来判断其是否存在。<p/>
 *
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
 *
 * <p>Most framework-provided {@code PropertySource} implementations are enumerable;
 * a counter-example would be {@code JndiPropertySource} where, due to the
 * nature of JNDI it is not possible to determine all possible property names at
 * any given time; rather it is only possible to try to access a property
 * (via {@link #getProperty(String)}) in order to evaluate whether it is present
 * or not.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class EnumerablePropertySource<T> extends PropertySource<T> {

	public EnumerablePropertySource(String name, T source) {
		super(name, source);
	}

	protected EnumerablePropertySource(String name) {
		super(name);
	}


	/**
	 * Return whether this {@code PropertySource} contains a property with the given name.
	 * <p>This implementation checks for the presence of the given name within the
	 * {@link #getPropertyNames()} array.
	 * @param name the name of the property to find
	 */
	@Override
	public boolean containsProperty(String name) {
		return ObjectUtils.containsElement(getPropertyNames(), name);
	}

	/**
	 * 返回{@linkplain #getSource() source}对象包含的所有属性的名称（从不{@code null}）。
	 * Return the names of all properties contained by the
	 * {@linkplain #getSource() source} object (never {@code null}).
	 */
	public abstract String[] getPropertyNames();

}
