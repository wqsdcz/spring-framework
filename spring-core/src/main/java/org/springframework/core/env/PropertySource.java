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

import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;


/**
 * Abstract base class representing a source of name/value property pairs. The underlying
 * {@linkplain #getSource() source object} may be of any type {@code T} that encapsulates
 * properties. Examples include {@link java.util.Properties} objects, {@link java.util.Map}
 * objects, {@code ServletContext} and {@code ServletConfig} objects (for access to init
 * parameters). Explore the {@code PropertySource} type hierarchy to see provided
 * implementations.
 *
 * <p>表示名称/值属性对源的抽象基类。底层的 {@linkplain #getSource() 源对象} 可以是任何可以封装属性的类型 {@code T}。
 * 例如包括 {@link java.util.Properties} 对象、{@link java.util.Map}对象、{@code ServletContext}
 * 和 {@code ServletConfig} 对象（用于访问初始化参数）。查看{@code PropertySource} 类型层次结构以了解提供的实现。
 *
 * <p>{@code PropertySource} objects are not typically used in isolation, but rather
 * through a {@link PropertySources} object, which aggregates property sources and in
 * conjunction with a {@link PropertyResolver} implementation that can perform
 * precedence-based searches across the set of {@code PropertySources}.
 *
 * <p>{@code PropertySource} 对象通常不是单独使用，而是通过 {@link PropertySources} 对象，
 * 它聚合属性源并与 {@link PropertyResolver} 实现结合使用，可以在 {@code PropertySources} 集合中执行基于优先级的搜索。
 *
 * <p>{@code PropertySource} identity is determined not based on the content of
 * encapsulated properties, but rather based on the {@link #getName() name} of the
 * {@code PropertySource} alone. This is useful for manipulating {@code PropertySource}
 * objects when in collection contexts. See operations in {@link MutablePropertySources}
 * as well as the {@link #named(String)} and {@link #toString()} methods for details.
 *
 * <p>{@code PropertySource} 的标识不是基于封装属性的内容，而是仅基于 {@code PropertySource}
 * 的 {@link #getName() 名称}。这在集合上下文中操作 {@code PropertySource} 对象时非常有用。详情请参见
 * {@link MutablePropertySources} 中的操作以及 {@link #named(String)} 和 {@link #toString()} 方法。
 *
 * <p>Note that when working with @{@link
 * org.springframework.context.annotation.Configuration Configuration} classes that
 * the @{@link org.springframework.context.annotation.PropertySource PropertySource}
 * annotation provides a convenient and declarative way of adding property sources to the
 * enclosing {@code Environment}.
 *
 * <p>请注意，在使用 {@link org.springframework.context.annotation.Configuration @Configuration} 类时，
 * {@link org.springframework.context.annotation.PropertySource @PropertySource}
 * 注解提供了一种方便且声明式的方法来向封闭的 {@code Environment} 添加属性源。
 *
 * @author Chris Beams
 * @since 3.1
 * @param <T> the source type
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
	 * Create a new {@code PropertySource} with the given name and source object.
	 * <p>使用给定的名称和源对象创建一个新的 {@code PropertySource}。
	 * @param name the associated name
	 * <p>相关的名称
	 * @param source the source object
	 * <p>源对象
	 */
	public PropertySource(String name, T source) {
		Assert.hasText(name, "Property source name must contain at least one character");
		Assert.notNull(source, "Property source must not be null");
		this.name = name;
		this.source = source;
	}

	/**
	 * Create a new {@code PropertySource} with the given name and with a new
	 * {@code Object} instance as the underlying source.
	 * <p>使用给定的名称和一个新的 {@code Object} 实例作为底层源创建一个新的 {@code PropertySource}。
	 * <p>Often useful in testing scenarios when creating anonymous implementations
	 * that never query an actual source but rather return hard-coded values.
	 * <p>在创建匿名实现时通常很有用，这种实现在测试场景中不会查询实际源，而是返回硬编码的值。
	 */
	@SuppressWarnings("unchecked")
	public PropertySource(String name) {
		this(name, (T) new Object());
	}


	/**
	 * Return the name of this {@code PropertySource}.
	 * <p>返回此 {@code PropertySource} 的名称。
	 * <p>See the {@linkplain PropertySource class-level Javadoc} for details
	 * on property source identity and names.
	 * <p>有关属性源标识和名称的详细信息，请参见 {@linkplain PropertySource 类级别的Javadoc}。
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the underlying source object for this {@code PropertySource}.
	 * <p>返回此 {@code PropertySource} 的底层源对象。
	 */
	public T getSource() {
		return this.source;
	}

	/**
	 * Return whether this {@code PropertySource} contains the given name.
	 * <p>返回此 {@code PropertySource} 是否包含给定名称。
	 * <p>This implementation simply checks for a {@code null} return value
	 * from {@link #getProperty(String)}. Subclasses may wish to implement
	 * a more efficient algorithm if possible.
	 * <p>此实现仅检查从 {@link #getProperty(String)} 返回的值是否为 {@code null}。子类可能希望在可能的情况下实现更高效的算法。
	 * @param name the property name to find
	 * <p>要查找的属性名称
	 */
	public boolean containsProperty(String name) {
		return (getProperty(name) != null);
	}

	/**
	 * Return the value associated with the given name,
	 * or {@code null} if not found.
	 * <p>返回与给定名称关联的值，如果未找到则返回 {@code null}。
	 * @param name the property to find
	 * <p>要查找的属性
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
	 * <p>当且仅当以下条件满足时，此 {@code PropertySource} 对象等于给定对象：
	 * <ul>
	 * <li>它们是同一个实例
	 * <li>两个对象的 {@code name} 属性相等
	 * </ul>
	 * <p>不评估除 {@code name} 之外的其他属性。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof PropertySource<?> that &&
				ObjectUtils.nullSafeEquals(getName(), that.getName())));
	}

	/**
	 * Return a hash code derived from the {@code name} property
	 * of this {@code PropertySource} object.
	 * <p>返回从此 {@code PropertySource} 对象的 {@code name} 属性派生的哈希码。
	 */
	@Override
	public int hashCode() {
		return Objects.hashCode(getName());
	}

	/**
	 * Produce concise output (type and name) if the current log level does not include
	 * debug. If debug is enabled, produce verbose output including the hash code of the
	 * PropertySource instance and every name/value property pair.
	 * <p>如果当前日志级别不包括调试，则生成简洁输出（类型和名称）。如果启用了调试，则生成详细输出，
	 * 包括 PropertySource 实例的哈希码和每个名称/值属性对。
	 * <p>This variable verbosity is useful as a property source such as system properties
	 * or environment variables may contain an arbitrary number of property pairs,
	 * potentially leading to difficulties to read exception and log messages.
	 * <p>这种可变的详细程度很有用，因为诸如系统属性或环境变量之类的属性源可能包含任意数量的属性对，
	 * 这可能导致难以阅读异常和日志消息。
	 * @see Log#isDebugEnabled()
	 */
	@Override
	public String toString() {
		if (logger.isDebugEnabled()) {
			return getClass().getSimpleName() + "@" + System.identityHashCode(this) +
					" {name='" + getName() + "', properties=" + getSource() + "}";
		}
		else {
			return getClass().getSimpleName() + " {name='" + getName() + "'}";
		}
	}


	/**
	 * Return a {@code PropertySource} implementation intended for collection
	 * comparison purposes only.
	 * <p>仅用于集合比较目的的 {@code PropertySource} 实现。
	 * <p>Primarily for internal use, but given a collection of {@code PropertySource}
	 * objects, may be used as follows:
	 * <p>主要用于内部使用，但是如果给定一个 {@code PropertySource} 对象集合，可以按以下方式使用：
	 * <pre class="code">
	 * List&lt;PropertySource&lt;?&gt;&gt; sources = new ArrayList&lt;&gt;();
	 * sources.add(new MapPropertySource("sourceA", mapA));
	 * sources.add(new MapPropertySource("sourceB", mapB));
	 * assert sources.contains(PropertySource.named("sourceA"));
	 * assert sources.contains(PropertySource.named("sourceB"));
	 * assert !sources.contains(PropertySource.named("sourceC"));</pre>
	 * <p>The returned {@code PropertySource} will throw {@code UnsupportedOperationException}
	 * if any methods other than {@code equals(Object)}, {@code hashCode()}, and {@code toString()}
	 * are called.
	 * <p>返回的 {@code PropertySource} 将抛出 {@code UnsupportedOperationException}
	 * 如果调用了除 {@code equals(Object)}, {@code hashCode()} 和 {@code toString()}
	 * 以外的任何方法。
	 * @param name the name of the comparison {@code PropertySource} to be created
	 * and returned
	 * <p>要创建并返回的比较用 {@code PropertySource} 的名称
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
	 * <p>在实际属性源无法在应用上下文创建时立即初始化的情况下，用作占位符的 {@code PropertySource}。
	 * 例如，基于 {@code ServletContext} 的属性源必须等待 {@code ServletContext} 对象对其封闭的
	 * {@code ApplicationContext} 可用。在这种情况下，应该使用存根来保持属性源的预期默认位置/顺序，然后在上下文刷新期间替换。
	 * @see org.springframework.context.support.AbstractApplicationContext#initPropertySources()
	 * @see org.springframework.web.context.support.StandardServletEnvironment
	 * @see org.springframework.web.context.support.ServletContextPropertySource
	 */
	public static class StubPropertySource extends PropertySource<Object> {

		public StubPropertySource(String name) {
			super(name);
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
	 * A {@code PropertySource} implementation intended for collection comparison
	 * purposes.
	 * <p>用于集合比较目的的 {@code PropertySource} 实现。
	 *
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
