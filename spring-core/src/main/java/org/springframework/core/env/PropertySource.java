/*
 * Copyright 2002-2017 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * <p>表示[名称/值]属性对来源的抽象基类。
 * 其底层{@linkplain #getSource() 源对象}可以是任意封装属性的类型{@code T}，典型示例包括：
 * {@link java.util.Properties}对象、{@link java.util.Map}对象、{@code ServletContext}与{@code ServletConfig}对象（用于访问初始化参数）。
 * 请探索{@code PropertySource}的类型体系以查看框架提供的具体实现。<p/>
 *
 * <p>{@code PropertySource} 对象通常不单独使用，而是通过聚合多个属性源的 {@link PropertySources} 对象进行操作，
 * 并与 {@link PropertyResolver} 实现协同工作 —— 该解析器能在{@code PropertySources}集合上执行<strong>基于优先级的搜索</strong>。<p/>
 *
 * <p>{@code PropertySource} 的身份标识<strong>不基于封装属性的内容</strong>，而<strong>仅取决于其 {@link #getName() 名称}</strong>。
 * 此特性在集合操作场景中操控 {@code PropertySource} 对象时尤为实用，具体操作详见 {@link MutablePropertySources} 中的方法，
 * 以及 {@link #named(String)} 和 {@link #toString()} 方法的实现细节。<p/>
 *
 * <p> 请注意：当处理带 @{@link org.springframework.context.annotation.Configuration Configuration} 注解的配置类时，
 * @{@link org.springframework.context.annotation.PropertySource PropertySource} 注解提供了一种便捷且声明式的手段，
 * 可将属性源添加到其所属的 {@code Environment} 环境中。<p/>
 *
 * Abstract base class representing a source of name/value property pairs. The underlying
 * {@linkplain #getSource() source object} may be of any type {@code T} that encapsulates
 * properties. Examples include {@link java.util.Properties} objects, {@link java.util.Map}
 * objects, {@code ServletContext} and {@code ServletConfig} objects (for access to init
 * parameters). Explore the {@code PropertySource} type hierarchy to see provided
 * implementations.
 *
 * <p>{@code PropertySource} objects are not typically used in isolation, but rather
 * through a {@link PropertySources} object, which aggregates property sources and in
 * conjunction with a {@link PropertyResolver} implementation that can perform
 * precedence-based searches across the set of {@code PropertySources}.
 *
 * <p>{@code PropertySource} identity is determined not based on the content of
 * encapsulated properties, but rather based on the {@link #getName() name} of the
 * {@code PropertySource} alone. This is useful for manipulating {@code PropertySource}
 * objects when in collection contexts. See operations in {@link MutablePropertySources}
 * as well as the {@link #named(String)} and {@link #toString()} methods for details.
 *
 * <p>Note that when working with @{@link
 * org.springframework.context.annotation.Configuration Configuration} classes that
 * the @{@link org.springframework.context.annotation.PropertySource PropertySource}
 * annotation provides a convenient and declarative way of adding property sources to the
 * enclosing {@code Environment}.
 *
 * @author Chris Beams
 * @since 3.1
 * @see PropertySources
 * @see PropertyResolver
 * @see PropertySourcesPropertyResolver
 * @see MutablePropertySources
 * @see org.springframework.context.annotation.PropertySource
 */
public abstract class PropertySource<T> {

	protected final Log logger = LogFactory.getLog(getClass());

	protected final String name;

	protected final T source;


	/**
	 * <p>创建一个新的 {@code PropertySource} 对象，该对象具有给定的 名称 和 源对象。<p/>
	 * Create a new {@code PropertySource} with the given name and source object.
	 */
	public PropertySource(String name, T source) {
		Assert.hasText(name, "Property source name must contain at least one character");
		Assert.notNull(source, "Property source must not be null");
		this.name = name;
		this.source = source;
	}

	/**
	 * <p>创建一个新的 {@code PropertySource} 对象，该对象具有指定 名称 且底层源为一个新 {@code Object} 实例。<p/>
	 * <p>此方法在测试场景中尤为实用——当需要创建永不查询真实数据源、仅返回硬编码值的匿名实现时。<p/>
	 *
	 * Create a new {@code PropertySource} with the given name and with a new
	 * {@code Object} instance as the underlying source.
	 * <p>Often useful in testing scenarios when creating anonymous implementations
	 * that never query an actual source but rather return hard-coded values.
	 */
	@SuppressWarnings("unchecked")
	public PropertySource(String name) {
		this(name, (T) new Object());
	}


	/**
	 * <p>返回这个 {@code PropertySource} 的名称。<p/>
	 * Return the name of this {@code PropertySource}
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * <p>返回这个 {@code PropertySource} 的底层源对象。<p/>
	 * Return the underlying source object for this {@code PropertySource}.
	 */
	public T getSource() {
		return this.source;
	}

	/**
	 * <p>返回这个 {@code PropertySource} 是否含有给定名称的属性。<p/>
	 * <p>这个实现简单地检查{@link #getProperty(String)}返回的值是否为{@code null}。如果可能的话，子类可能希望实现一个更有效的算法。<p/>
	 * Return whether this {@code PropertySource} contains the given name.
	 * <p>This implementation simply checks for a {@code null} return value
	 * from {@link #getProperty(String)}. Subclasses may wish to implement
	 * a more efficient algorithm if possible.
	 * @param name the property name to find
	 */
	public boolean containsProperty(String name) {
		return (getProperty(name) != null);
	}

	/**
	 * <p>返回与给定名称关联的值，如果没有发现，则返回{@code null}。<p/>
	 * Return the value associated with the given name,
	 * or {@code null} if not found.
	 * @param name the property to find
	 * @see PropertyResolver#getRequiredProperty(String)
	 */
	@Nullable
	public abstract Object getProperty(String name);


	/**
	 * This {@code PropertySource} object is equal to the given object if:
	 * <ul>
	 * <li>they are the same instance
	 * <li>the {@code name} properties for both objects are equal
	 * </ul>
	 * <p>No properties other than {@code name} are evaluated.
	 */
	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof PropertySource &&
				ObjectUtils.nullSafeEquals(this.name, ((PropertySource<?>) other).name)));
	}

	/**
	 * Return a hash code derived from the {@code name} property
	 * of this {@code PropertySource} object.
	 */
	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.name);
	}

	/**
	 * Produce concise output (type and name) if the current log level does not include
	 * debug. If debug is enabled, produce verbose output including the hash code of the
	 * PropertySource instance and every name/value property pair.
	 * <p>This variable verbosity is useful as a property source such as system properties
	 * or environment variables may contain an arbitrary number of property pairs,
	 * potentially leading to difficult to read exception and log messages.
	 * @see Log#isDebugEnabled()
	 */
	@Override
	public String toString() {
		if (logger.isDebugEnabled()) {
			return getClass().getSimpleName() + "@" + System.identityHashCode(this) +
					" {name='" + this.name + "', properties=" + this.source + "}";
		}
		else {
			return getClass().getSimpleName() + " {name='" + this.name + "'}";
		}
	}


	/**
	 * <p>返回一个专门用于集合比较目的的 {@code PropertySource} 实现。<p/>
	 * <p>该方法主要供内部使用，但若存在 {@code PropertySource} 对象集合时，可如下使用：
	 * <pre class="code">
	 *     {@code List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
	 *     sources.add(new MapPropertySource("sourceA", mapA));
	 *     sources.add(new MapPropertySource("sourceB", mapB));
	 *     assert sources.contains(PropertySource.named("sourceA"));
	 *     assert sources.contains(PropertySource.named("sourceB"));
	 *     assert !sources.contains(PropertySource.named("sourceC")); }
	 * </pre>
	 * <p/>
	 * <p>返回的 {@code PropertySource} 在调用除 {@code equals(Object)}、{@code hashCode()} 及 {@code toString()} 之外的方法时，
	 * 将抛出 {@code UnsupportedOperationException}。<p/>
	 *
	 * Return a {@code PropertySource} implementation intended for collection comparison purposes only.
	 * <p>Primarily for internal use, but given a collection of {@code PropertySource} objects, may be
	 * used as follows:
	 * <pre class="code">
	 * {@code List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
	 * sources.add(new MapPropertySource("sourceA", mapA));
	 * sources.add(new MapPropertySource("sourceB", mapB));
	 * assert sources.contains(PropertySource.named("sourceA"));
	 * assert sources.contains(PropertySource.named("sourceB"));
	 * assert !sources.contains(PropertySource.named("sourceC"));
	 * }</pre>
	 * The returned {@code PropertySource} will throw {@code UnsupportedOperationException}
	 * if any methods other than {@code equals(Object)}, {@code hashCode()}, and {@code toString()}
	 * are called.
	 * @param name the name of the comparison {@code PropertySource} to be created and returned.
	 */
	public static PropertySource<?> named(String name) {
		return new ComparisonPropertySource(name);
	}


	/**
	 * {@code PropertySource} to be used as a placeholder in cases where an actual
	 * property source cannot be eagerly initialized at application context
	 * creation time.  For example, a {@code ServletContext}-based property source
	 * must wait until the {@code ServletContext} object is available to its enclosing
	 * {@code ApplicationContext}.  In such cases, a stub should be used to hold the
	 * intended default position/order of the property source, then be replaced
	 * during context refresh.
	 * @see org.springframework.context.support.AbstractApplicationContext#initPropertySources()
	 * @see org.springframework.web.context.support.StandardServletEnvironment
	 * @see org.springframework.web.context.support.ServletContextPropertySource
	 */
	public static class StubPropertySource extends PropertySource<Object> {

		public StubPropertySource(String name) {
			super(name, new Object());
		}

		/**
		 * Always returns {@code null}.
		 */
		@Override
		@Nullable
		public String getProperty(String name) {
			return null;
		}
	}


	/**
	 * @see PropertySource#named(String)
	 */
	static class ComparisonPropertySource extends StubPropertySource {

		private static final String USAGE_ERROR =
				"ComparisonPropertySource instances are for use with collection comparison only";

		public ComparisonPropertySource(String name) {
			super(name);
		}

		@Override
		public Object getSource() {
			throw new UnsupportedOperationException(USAGE_ERROR);
		}

		@Override
		public boolean containsProperty(String name) {
			throw new UnsupportedOperationException(USAGE_ERROR);
		}

		@Override
		@Nullable
		public String getProperty(String name) {
			throw new UnsupportedOperationException(USAGE_ERROR);
		}
	}

}
