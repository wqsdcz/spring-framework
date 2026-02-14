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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import org.springframework.lang.Contract;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;

/**
 * 用于确定作为其他注解容器的注解的策略。
 * {@link #standardRepeatables()} 方法提供了一个默认策略，该策略遵循 Java 的 {@link Repeatable @Repeatable} 支持，适用于大多数情况。
 * <p>
 *     {@link #of} 方法可用于为不希望使用 {@link Repeatable @Repeatable} 的注解注册关系。
 * <p>
 *     要完全禁用可重复注解支持，请使用 {@link #none()}。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2
 */
public abstract class RepeatableContainers {

	static final Map<Class<? extends Annotation>, Object> cache = new ConcurrentReferenceHashMap<>();

	@Nullable
	private final RepeatableContainers parent;


	private RepeatableContainers(@Nullable RepeatableContainers parent) {
		this.parent = parent;
	}


	/**
	 * 添加【容器注解】和【可重复注解】之间的显式关系。
	 * <p>
	 *     警告：此方法提供的参数顺序与 {@link #of(Class, Class)} 方法相反。
	 *
	 * @param container 容器注解类型
	 * @param repeatable 可重复注解类型
	 * @return 新的 {@link RepeatableContainers} 实例
	 */
	public RepeatableContainers and(Class<? extends Annotation> container,
			Class<? extends Annotation> repeatable) {

		return new ExplicitRepeatableContainer(this, repeatable, container);
	}

	@Nullable
	Annotation[] findRepeatedAnnotations(Annotation annotation) {
		if (this.parent == null) {
			return null;
		}
		return this.parent.findRepeatedAnnotations(annotation);
	}


	@Override
	@Contract("null -> false")
	public boolean equals(@Nullable Object other) {
		if (other == this) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		return ObjectUtils.nullSafeEquals(this.parent, ((RepeatableContainers) other).parent);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.parent);
	}


	/**
	 * Create a {@link RepeatableContainers} instance that searches using Java's
	 * {@link Repeatable @Repeatable} annotation.
	 * @return a {@link RepeatableContainers} instance
	 */
	public static RepeatableContainers standardRepeatables() {
		return StandardRepeatableContainers.INSTANCE;
	}

	/**
	 * 创建一个使用预定义可重复注解和容器类型的 {@link RepeatableContainers} 实例。
	 * <p>
	 *     警告：此方法提供的参数顺序与 {@link #and(Class, Class)} 方法相反。
	 *
	 * @param repeatable 可重复注解类型
	 * @param container 容器注解类型或 {@code null}。
	 *                  <p>如果指定，此注解必须声明一个返回可重复注解数组的 {@code value} 属性。
	 * 					<p>如果未指定，则通过检查 {@code repeatable} 上的 {@code @Repeatable} 注解来推断容器。
	 * @return 一个 {@link RepeatableContainers} 实例
	 * @throws IllegalArgumentException 如果提供的容器类型为 {@code null}，且注解类型不是可重复注解
	 * @throws AnnotationConfigurationException 如果提供的容器类型，不是为可重复注解正确配置的容器
	 */
	public static RepeatableContainers of(
			Class<? extends Annotation> repeatable, @Nullable Class<? extends Annotation> container) {

		return new ExplicitRepeatableContainer(null, repeatable, container);
	}

	/**
	 * 创建一个不支持任何可重复注解的 {@link RepeatableContainers} 实例。
	 * @return 一个 {@link RepeatableContainers} 实例
	 */
	public static RepeatableContainers none() {
		return NoRepeatableContainers.INSTANCE;
	}


	/**
	 * 标准的 {@link RepeatableContainers} 实现，
	 * 使用 Java 的 {@link Repeatable @Repeatable} 注解进行搜索。
	 */
	private static class StandardRepeatableContainers extends RepeatableContainers {

		private static final Object NONE = new Object();

		private static final StandardRepeatableContainers INSTANCE = new StandardRepeatableContainers();

		StandardRepeatableContainers() {
			super(null);
		}

		@Override
		@Nullable
		Annotation[] findRepeatedAnnotations(Annotation annotation) {
			Method method = getRepeatedAnnotationsMethod(annotation.annotationType());
			if (method != null) {
				return (Annotation[]) AnnotationUtils.invokeAnnotationMethod(method, annotation);
			}
			return super.findRepeatedAnnotations(annotation);
		}

		/**
		 * 获取 可重复注解的 容器方法
		 * @param annotationType 可重复注解
		 * @return
		 */
		@Nullable
		private static Method getRepeatedAnnotationsMethod(Class<? extends Annotation> annotationType) {
			Object result = cache.computeIfAbsent(annotationType,
					StandardRepeatableContainers::computeRepeatedAnnotationsMethod);
			return (result != NONE ? (Method) result : null);
		}

		/**
		 * 获取 可重复注解的 容器方法
		 * @param annotationType 容器注解
		 * @return
		 */
		private static Object computeRepeatedAnnotationsMethod(Class<? extends Annotation> annotationType) {
			// 获取 容器注解中的 属性方法集
			// 获取 返回可重复注解的 属性方法
			// 获取 返回可重复注解的 属性方法的返回类型
			// 获取 可重复注解的 属性方
			AttributeMethods methods = AttributeMethods.forAnnotationType(annotationType);
			Method method = methods.get(MergedAnnotation.VALUE);
			if (method != null) {
				Class<?> returnType = method.getReturnType();
				if (returnType.isArray()) {
					Class<?> componentType = returnType.componentType();
					if (Annotation.class.isAssignableFrom(componentType) &&
							componentType.isAnnotationPresent(Repeatable.class)) {
						return method;
					}
				}
			}
			return NONE;
		}
	}


	/**
	 * 单个显式映射。
	 */
	private static class ExplicitRepeatableContainer extends RepeatableContainers {

		private final Class<? extends Annotation> repeatable;

		private final Class<? extends Annotation> container;

		private final Method valueMethod;

		ExplicitRepeatableContainer(@Nullable RepeatableContainers parent,
				Class<? extends Annotation> repeatable, @Nullable Class<? extends Annotation> container) {

			super(parent);
			Assert.notNull(repeatable, "Repeatable must not be null");
			if (container == null) {
				container = deduceContainer(repeatable);
			}
			Method valueMethod = AttributeMethods.forAnnotationType(container).get(MergedAnnotation.VALUE);
			try {
				if (valueMethod == null) {
					throw new NoSuchMethodException("No value method found");
				}
				Class<?> returnType = valueMethod.getReturnType();
				if (!returnType.isArray() || returnType.componentType() != repeatable) {
					throw new AnnotationConfigurationException(
							"Container type [%s] must declare a 'value' attribute for an array of type [%s]"
								.formatted(container.getName(), repeatable.getName()));
				}
			}
			catch (AnnotationConfigurationException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new AnnotationConfigurationException(
						"Invalid declaration of container type [%s] for repeatable annotation [%s]"
							.formatted(container.getName(), repeatable.getName()), ex);
			}
			this.repeatable = repeatable;
			this.container = container;
			this.valueMethod = valueMethod;
		}

		private Class<? extends Annotation> deduceContainer(Class<? extends Annotation> repeatable) {
			Repeatable annotation = repeatable.getAnnotation(Repeatable.class);
			Assert.notNull(annotation, () -> "Annotation type must be a repeatable annotation: " +
						"failed to resolve container type for " + repeatable.getName());
			return annotation.value();
		}

		@Override
		@Nullable
		Annotation[] findRepeatedAnnotations(Annotation annotation) {
			if (this.container.isAssignableFrom(annotation.annotationType())) {
				return (Annotation[]) AnnotationUtils.invokeAnnotationMethod(this.valueMethod, annotation);
			}
			return super.findRepeatedAnnotations(annotation);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (!super.equals(other)) {
				return false;
			}
			ExplicitRepeatableContainer otherErc = (ExplicitRepeatableContainer) other;
			return (this.container.equals(otherErc.container) && this.repeatable.equals(otherErc.repeatable));
		}

		@Override
		public int hashCode() {
			int hashCode = super.hashCode();
			hashCode = 31 * hashCode + this.container.hashCode();
			hashCode = 31 * hashCode + this.repeatable.hashCode();
			return hashCode;
		}
	}


	/**
	 * 没有可重复的容器。
	 */
	private static class NoRepeatableContainers extends RepeatableContainers {

		private static final NoRepeatableContainers INSTANCE = new NoRepeatableContainers();

		NoRepeatableContainers() {
			super(null);
		}
	}

}
