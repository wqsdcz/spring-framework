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

package org.springframework.core.convert;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Contract;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 关于类型转换的上下文描述符。
 * <p>能够表示数组和泛型集合类型。
 *
 * @author Keith Donald
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 3.0
 * @see ConversionService#canConvert(TypeDescriptor, TypeDescriptor)
 * @see ConversionService#convert(Object, TypeDescriptor, TypeDescriptor)
 */
@SuppressWarnings("serial")
public class TypeDescriptor implements Serializable {

	private static final Map<Class<?>, TypeDescriptor> commonTypesCache = new HashMap<>(32);

	private static final Class<?>[] CACHED_COMMON_TYPES = {
			boolean.class, Boolean.class, byte.class, Byte.class, char.class, Character.class,
			double.class, Double.class, float.class, Float.class, int.class, Integer.class,
			long.class, Long.class, short.class, Short.class, String.class, Object.class};

	static {
		for (Class<?> preCachedClass : CACHED_COMMON_TYPES) {
			commonTypesCache.put(preCachedClass, valueOf(preCachedClass));
		}
	}


	private final Class<?> type;

	private final ResolvableType resolvableType;

	private final AnnotatedElementSupplier annotatedElementSupplier;

	@Nullable
	private volatile AnnotatedElementAdapter annotatedElement;


	/**
	 * 从 {@link MethodParameter} 创建新的类型描述符。
	 * <p>当源或目标转换点是构造函数参数、方法参数或方法返回值时使用此构造函数。
	 * @param methodParameter 方法参数
	 */
	public TypeDescriptor(MethodParameter methodParameter) {
		this.resolvableType = ResolvableType.forMethodParameter(methodParameter);
		this.type = this.resolvableType.resolve(methodParameter.getNestedParameterType());
		this.annotatedElementSupplier = () -> AnnotatedElementAdapter.from(methodParameter.getParameterIndex() == -1 ?
				methodParameter.getMethodAnnotations() : methodParameter.getParameterAnnotations());
	}

	/**
	 * 从 {@link Field} 创建新的类型描述符。
	 * <p>当源或目标转换点是字段时使用此构造函数。
	 * @param field 字段
	 */
	public TypeDescriptor(Field field) {
		this.resolvableType = ResolvableType.forField(field);
		this.type = this.resolvableType.resolve(field.getType());
		this.annotatedElementSupplier = () -> AnnotatedElementAdapter.from(field.getAnnotations());
	}

	/**
	 * 从 {@link Property} 创建新的类型描述符。
	 * <p>当源或目标转换点是Java类上的属性时使用此构造函数。
	 * @param property 属性
	 */
	public TypeDescriptor(Property property) {
		Assert.notNull(property, "Property must not be null");
		this.resolvableType = ResolvableType.forMethodParameter(property.getMethodParameter());
		this.type = this.resolvableType.resolve(property.getType());
		this.annotatedElementSupplier = () -> AnnotatedElementAdapter.from(property.getAnnotations());
	}

	/**
	 * 从 {@link ResolvableType} 创建新的类型描述符。
	 * <p>此构造函数在内部使用，也可由支持具有扩展类型系统的非Java语言的子类使用。
	 * 从5.1.4版本开始是公共的，而之前是受保护的。
	 * @param resolvableType 可解析类型
	 * @param type 支持类型（如果应该被解析则为 {@code null}）
	 * @param annotations 类型注解
	 * @since 4.0
	 */
	public TypeDescriptor(ResolvableType resolvableType, @Nullable Class<?> type, @Nullable Annotation[] annotations) {
		this.resolvableType = resolvableType;
		this.type = (type != null ? type : resolvableType.toClass());
		this.annotatedElementSupplier = () -> AnnotatedElementAdapter.from(annotations);
	}


	/**
	 * {@link #getType()} 的变体，通过返回其对象包装类型来处理基本类型。
	 * <p>这对于希望规范化为基于对象的类型而不直接使用基本类型的转换服务实现很有用。
	 */
	public Class<?> getObjectType() {
		return ClassUtils.resolvePrimitiveIfNecessary(getType());
	}

	/**
	 * 此TypeDescriptor描述的支持类、方法参数、字段或属性的类型。
	 * <p>原样返回基本类型。有关此操作的变体，如有必要将基本类型解析为其相应的对象类型，
	 * 请参见 {@link #getObjectType()}。
	 * @see #getObjectType()
	 */
	public Class<?> getType() {
		return this.type;
	}

	/**
	 * 返回底层的 {@link ResolvableType}。
	 * @since 4.0
	 */
	public ResolvableType getResolvableType() {
		return this.resolvableType;
	}

	/**
	 * 返回描述符的底层源。将根据 {@link TypeDescriptor} 的构造方式返回 {@link Field}、
	 * {@link MethodParameter} 或 {@link Type}。此方法主要用于提供对替代JVM语言
	 * 可能提供的附加类型信息或元数据的访问。
	 * @since 4.0
	 */
	public Object getSource() {
		return this.resolvableType.getSource();
	}


	/**
	 * 为在此描述符内声明的嵌套类型创建类型描述符。
	 * @param nestingLevel 集合/数组元素或映射键/值声明在属性内的嵌套层级
	 * @return 指定嵌套层级的嵌套类型描述符，如果无法获取则返回 {@code null}
	 * @since 6.1
	 */
	@Nullable
	public TypeDescriptor nested(int nestingLevel) {
		ResolvableType nested = this.resolvableType;
		for (int i = 0; i < nestingLevel; i++) {
			if (Object.class == nested.getType()) {
				// Could be a collection type but we don't know about its element type,
				// so let's just assume there is an element type of type Object...
			}
			else {
				nested = nested.getNested(2);
			}
		}
		if (nested == ResolvableType.NONE) {
			return null;
		}
		return getRelatedIfResolvable(nested);
	}

	/**
	 * 通过将此 {@link TypeDescriptor} 的类型设置为所提供值的类来缩小其范围。
	 * <p>如果值为 {@code null}，则不执行缩小操作，此 TypeDescriptor 将保持不变地返回。
	 * <p>设计用于绑定框架在读取属性、字段或方法返回值时调用。允许此类框架缩小从声明的属性、字段或方法返回值类型构建的 TypeDescriptor。
	 * 例如，声明为 {@code java.lang.Object} 的字段如果被设置为 {@code java.util.HashMap} 值，则会被缩小为 {@code java.util.HashMap}。
	 * 然后可以使用缩小后的 TypeDescriptor 将 HashMap 转换为其他类型。缩小后的副本保留注解和嵌套类型上下文。
	 * @param value 用于缩小此类型描述符的值
	 * @return 此 TypeDescriptor 已缩小（返回一个类型更新为所提供值的类的副本）
	 */
	public TypeDescriptor narrow(@Nullable Object value) {
		if (value == null) {
			return this;
		}
		ResolvableType narrowed = ResolvableType.forType(value.getClass(), getResolvableType());
		return new TypeDescriptor(narrowed, value.getClass(), getAnnotations());
	}

	/**
	 * 将此 {@link TypeDescriptor} 转换为超类或已实现的接口，
	 * 同时保留注解和嵌套类型上下文。
	 * @param superType 要转换到的超类型（可以为 {@code null}）
	 * @return 转换后类型的新的 TypeDescriptor
	 * @throws IllegalArgumentException 如果此类型不可分配给超类型
	 * @since 3.2
	 */
	@Nullable
	public TypeDescriptor upcast(@Nullable Class<?> superType) {
		if (superType == null) {
			return null;
		}
		Assert.isAssignable(superType, getType());
		return new TypeDescriptor(getResolvableType().as(superType), superType, getAnnotations());
	}

	/**
	 * 返回此类型的名称：完全限定的类名。
	 */
	public String getName() {
		return ClassUtils.getQualifiedName(getType());
	}

	/**
	 * 此类型是否为基本类型？
	 */
	public boolean isPrimitive() {
		return getType().isPrimitive();
	}

	private AnnotatedElementAdapter getAnnotatedElement() {
		AnnotatedElementAdapter annotatedElement = this.annotatedElement;
		if (annotatedElement == null) {
			annotatedElement = this.annotatedElementSupplier.get();
			this.annotatedElement = annotatedElement;
		}
		return annotatedElement;
	}

	/**
	 * 返回与此类型描述符关联的注解（如果有）。
	 * @return 注解数组，如果没有则返回空数组
	 */
	public Annotation[] getAnnotations() {
		return getAnnotatedElement().getAnnotations();
	}

	/**
	 * 确定此类型描述符是否具有指定的注解。
	 * <p>从Spring Framework 4.2开始，此方法支持任意级别的元注解。
	 * @param annotationType 注解类型
	 * @return 如果存在注解则返回 {@code true}
	 */
	public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
		AnnotatedElementAdapter annotatedElement = getAnnotatedElement();
		if (annotatedElement.isEmpty()) {
			// Shortcut: AnnotatedElementUtils would have to expect AnnotatedElement.getAnnotations()
			// to return a copy of the array, whereas we can do it more efficiently here.
			return false;
		}
		return AnnotatedElementUtils.isAnnotated(annotatedElement, annotationType);
	}

	/**
	 * 获取此类型描述符上指定 {@code annotationType} 的注解。
	 * <p>从 Spring Framework 4.2 开始，此方法支持任意级别的元注解。
	 * @param annotationType 注解类型
	 * @return 注解，如果此类型描述符上不存在此类注解则返回 {@code null}
	 */
	@Nullable
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		AnnotatedElementAdapter annotatedElement = getAnnotatedElement();
		if (annotatedElement.isEmpty()) {
			// Shortcut: AnnotatedElementUtils would have to expect AnnotatedElement.getAnnotations()
			// to return a copy of the array, whereas we can do it more efficiently here.
			return null;
		}
		return AnnotatedElementUtils.getMergedAnnotation(annotatedElement, annotationType);
	}

	/**
	 * 如果此类型描述符的对象可以赋值给给定类型描述符所描述的位置，则返回true。
	 * <p>例如，{@code valueOf(String.class).isAssignableTo(valueOf(CharSequence.class))}
	 * 返回 {@code true}，因为String值可以赋值给CharSequence变量。
	 * 另一方面，{@code valueOf(Number.class).isAssignableTo(valueOf(Integer.class))}
	 * 返回 {@code false}，因为虽然所有Integer都是Number，但并非所有Number都是Integer。
	 * <p>对于数组、集合和映射，如果声明了元素和键/值类型，则会进行检查。
	 * 例如，{@code List<String>}字段值可以赋值给{@code Collection<CharSequence>}字段，
	 * 但{@code List<Number>}不能赋值给{@code List<Integer>}。
	 * @return {@code true} 如果此类型可以赋值给提供的类型描述符所表示的类型
	 * @see #getObjectType()
	 */
	public boolean isAssignableTo(TypeDescriptor typeDescriptor) {
		boolean typesAssignable = typeDescriptor.getObjectType().isAssignableFrom(getObjectType());
		if (!typesAssignable) {
			return false;
		}
		if (isArray() && typeDescriptor.isArray()) {
			return isNestedAssignable(getElementTypeDescriptor(), typeDescriptor.getElementTypeDescriptor());
		}
		else if (isCollection() && typeDescriptor.isCollection()) {
			return isNestedAssignable(getElementTypeDescriptor(), typeDescriptor.getElementTypeDescriptor());
		}
		else if (isMap() && typeDescriptor.isMap()) {
			return isNestedAssignable(getMapKeyTypeDescriptor(), typeDescriptor.getMapKeyTypeDescriptor()) &&
				isNestedAssignable(getMapValueTypeDescriptor(), typeDescriptor.getMapValueTypeDescriptor());
		}
		else {
			return true;
		}
	}

	private boolean isNestedAssignable(@Nullable TypeDescriptor nestedTypeDescriptor,
			@Nullable TypeDescriptor otherNestedTypeDescriptor) {

		return (nestedTypeDescriptor == null || otherNestedTypeDescriptor == null ||
				nestedTypeDescriptor.isAssignableTo(otherNestedTypeDescriptor));
	}

	/**
	 * 此类型是否为 {@link Collection} 类型？
	 */
	public boolean isCollection() {
		return Collection.class.isAssignableFrom(getType());
	}

	/**
	 * 此类型是否为数组类型？
	 */
	public boolean isArray() {
		return getType().isArray();
	}

	/**
	 * 如果此类型是数组，则返回数组的组件类型。
	 * 如果此类型是 {@code Stream}，则返回流的组件类型。
	 * 如果此类型是 {@link Collection} 且已参数化，则返回集合的元素类型。
	 * 如果集合未参数化，则返回 {@code null}，表示元素类型未声明。
	 * @return 数组组件类型或集合元素类型，如果此类型不是数组类型或 {@code java.util.Collection}，
	 * 或者其元素类型未参数化，则返回 {@code null}
	 * @see #elementTypeDescriptor(Object)
	 */
	@Nullable
	public TypeDescriptor getElementTypeDescriptor() {
		if (getResolvableType().isArray()) {
			return new TypeDescriptor(getResolvableType().getComponentType(), null, getAnnotations());
		}
		if (Stream.class.isAssignableFrom(getType())) {
			return getRelatedIfResolvable(getResolvableType().as(Stream.class).getGeneric(0));
		}
		return getRelatedIfResolvable(getResolvableType().asCollection().getGeneric(0));
	}

	/**
	 * 如果此类型是 {@link Collection} 或数组，则从提供的集合或数组元素创建元素 TypeDescriptor。
	 * <p>将 {@link #getElementTypeDescriptor() elementType} 属性缩小到所提供集合或数组元素的类。
	 * 例如，如果此描述的是 {@code java.util.List<java.lang.Number>}，而元素参数是 {@code java.lang.Integer}，
	 * 则返回的 TypeDescriptor 将是 {@code java.lang.Integer}。
	 * 如果此描述的是 {@code java.util.List<?>}，而元素参数是 {@code java.lang.Integer}，
	 * 则返回的 TypeDescriptor 也将是 {@code java.lang.Integer}。
	 * <p>注解和嵌套类型上下文将在返回的缩小后的 TypeDescriptor 中保留。
	 * @param element 集合或数组元素
	 * @return 元素类型描述符，缩小到所提供元素的类型
	 * @see #getElementTypeDescriptor()
	 * @see #narrow(Object)
	 */
	@Nullable
	public TypeDescriptor elementTypeDescriptor(Object element) {
		return narrow(element, getElementTypeDescriptor());
	}

	/**
	 * 此类型是否为 {@link Map} 类型？
	 */
	public boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}

	/**
	 * 如果此类型是 {@link Map} 且其键类型已被参数化，
	 * 则返回映射的键类型。如果映射的键类型未被参数化，
	 * 则返回 {@code null}，表示键类型未声明。
	 * @return 映射的键类型，如果此类型是映射但其键类型未被参数化，则返回 {@code null}
	 * @throws IllegalStateException 如果此类型不是 {@code java.util.Map}
	 */
	@Nullable
	public TypeDescriptor getMapKeyTypeDescriptor() {
		Assert.state(isMap(), "Not a [java.util.Map]");
		return getRelatedIfResolvable(getResolvableType().asMap().getGeneric(0));
	}

	/**
	 * 如果此类型是 {@link Map}，则从提供的映射键创建一个 mapKey {@link TypeDescriptor}。
	 * <p>将 {@link #getMapKeyTypeDescriptor() mapKeyType} 属性缩小到所提供映射键的类。
	 * 例如，如果此描述的是 {@code java.util.Map<java.lang.Number, java.lang.String>}
	 * 并且键参数是 {@code java.lang.Integer}，则返回的 TypeDescriptor 将是 {@code java.lang.Integer}。
	 * 如果此描述的是 {@code java.util.Map<?, ?>} 并且键参数是 {@code java.lang.Integer}，
	 * 则返回的 TypeDescriptor 也将是 {@code java.lang.Integer}。
	 * <p>注解和嵌套类型上下文将在返回的缩小后的 TypeDescriptor 中保留。
	 * @param mapKey 映射键
	 * @return 映射键类型描述符
	 * @throws IllegalStateException 如果此类型不是 {@code java.util.Map}
	 * @see #narrow(Object)
	 */
	@Nullable
	public TypeDescriptor getMapKeyTypeDescriptor(Object mapKey) {
		return narrow(mapKey, getMapKeyTypeDescriptor());
	}

	/**
	 * 如果此类型是 {@link Map} 且其值类型已被参数化，
	 * 则返回映射的值类型。
	 * <p>如果映射的值类型未被参数化，则返回 {@code null}，
	 * 表示值类型未声明。
	 * @return 映射的值类型，如果此类型是映射但其值类型未被参数化，则返回 {@code null}
	 * @throws IllegalStateException 如果此类型不是 {@code java.util.Map}
	 */
	@Nullable
	public TypeDescriptor getMapValueTypeDescriptor() {
		Assert.state(isMap(), "Not a [java.util.Map]");
		return getRelatedIfResolvable(getResolvableType().asMap().getGeneric(1));
	}

	/**
	 * 如果此类型是 {@link Map}，则从提供的映射值创建一个 mapValue {@link TypeDescriptor}。
	 * <p>将 {@link #getMapValueTypeDescriptor() mapValueType} 属性缩小到所提供映射值的类。
	 * 例如，如果此描述的是 {@code java.util.Map<java.lang.String, java.lang.Number>}
	 * 并且值参数是 {@code java.lang.Integer}，则返回的 TypeDescriptor 将是 {@code java.lang.Integer}。
	 * 如果此描述的是 {@code java.util.Map<?, ?>} 并且值参数是 {@code java.lang.Integer}，
	 * 则返回的 TypeDescriptor 也将是 {@code java.lang.Integer}。
	 * <p>注解和嵌套类型上下文将在返回的缩小后的 TypeDescriptor 中保留。
	 * @param mapValue 映射值
	 * @return 映射值类型描述符
	 * @throws IllegalStateException 如果此类型不是 {@code java.util.Map}
	 * @see #narrow(Object)
	 */
	@Nullable
	public TypeDescriptor getMapValueTypeDescriptor(@Nullable Object mapValue) {
		return narrow(mapValue, getMapValueTypeDescriptor());
	}

	@Nullable
	private TypeDescriptor getRelatedIfResolvable(ResolvableType type) {
		if (type.resolve() == null) {
			return null;
		}
		return new TypeDescriptor(type, null, getAnnotations());
	}

	@Nullable
	private TypeDescriptor narrow(@Nullable Object value, @Nullable TypeDescriptor typeDescriptor) {
		if (typeDescriptor != null) {
			return typeDescriptor.narrow(value);
		}
		if (value != null) {
			return narrow(value);
		}
		return null;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TypeDescriptor otherDesc)) {
			return false;
		}
		if (getType() != otherDesc.getType()) {
			return false;
		}
		if (!annotationsMatch(otherDesc)) {
			return false;
		}
		return Arrays.equals(getResolvableType().getGenerics(), otherDesc.getResolvableType().getGenerics());
	}

	private boolean annotationsMatch(TypeDescriptor otherDesc) {
		Annotation[] anns = getAnnotations();
		Annotation[] otherAnns = otherDesc.getAnnotations();
		if (anns == otherAnns) {
			return true;
		}
		if (anns.length != otherAnns.length) {
			return false;
		}
		if (anns.length > 0) {
			for (int i = 0; i < anns.length; i++) {
				if (!annotationEquals(anns[i], otherAnns[i])) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean annotationEquals(Annotation ann, Annotation otherAnn) {
		// Annotation.equals is reflective and pretty slow, so let's check identity and proxy type first.
		return (ann == otherAnn || (ann.getClass() == otherAnn.getClass() && ann.equals(otherAnn)));
	}

	@Override
	public int hashCode() {
		return getType().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Annotation ann : getAnnotations()) {
			builder.append('@').append(getName(ann.annotationType())).append(' ');
		}
		builder.append(getResolvableType());
		return builder.toString();
	}


	/**
	 * 为对象创建新的类型描述符。
	 * <p>在要求转换系统将其转换为其他类型之前，使用此工厂方法来内省源对象。
	 * <p>如果提供的对象为 {@code null}，返回 {@code null}，否则调用 {@link #valueOf(Class)}
	 * 从对象的类构建TypeDescriptor。
	 * @param source 源对象
	 * @return 类型描述符
	 */
	@Nullable
	@Contract("!null -> !null; null -> null")
	public static TypeDescriptor forObject(@Nullable Object source) {
		return (source != null ? valueOf(source.getClass()) : null);
	}

	/**
	 * 从给定类型创建新的类型描述符。
	 * <p>当没有方法参数或字段等类型位置可提供额外转换上下文时，使用此方法指示转换系统
	 * 将对象转换为特定目标类型。
	 * <p>通常更倾向于使用 {@link #forObject(Object)} 从源对象构建类型描述符，
	 * 因为它处理 {@code null} 对象的情况。
	 * @param type 类（可以为 {@code null} 表示 {@code Object.class}）
	 * @return 相应的类型描述符
	 */
	public static TypeDescriptor valueOf(@Nullable Class<?> type) {
		if (type == null) {
			type = Object.class;
		}
		TypeDescriptor desc = commonTypesCache.get(type);
		return (desc != null ? desc : new TypeDescriptor(ResolvableType.forClass(type), null, null));
	}

	/**
	 * 从 {@link java.util.Collection} 类型创建新的类型描述符。
	 * <p>对于转换为类型化集合很有用。
	 * <p>例如，可以通过转换为此方法构建的目标类型将 {@code List<String>} 转换为{@code List<EmailAddress>}。
	 * 构建这样的 {@code TypeDescriptor} 的方法调用看起来像：
	 * {@code collection(List.class, TypeDescriptor.valueOf(EmailAddress.class));}
	 * @param collectionType 集合类型，必须实现 {@link Collection}。
	 * @param elementTypeDescriptor 集合元素类型的描述符，用于转换集合元素
	 * @return 集合类型描述符
	 */
	public static TypeDescriptor collection(Class<?> collectionType, @Nullable TypeDescriptor elementTypeDescriptor) {
		Assert.notNull(collectionType, "Collection type must not be null");
		if (!Collection.class.isAssignableFrom(collectionType)) {
			throw new IllegalArgumentException("Collection type must be a [java.util.Collection]");
		}
		ResolvableType element = (elementTypeDescriptor != null ? elementTypeDescriptor.resolvableType : null);
		return new TypeDescriptor(ResolvableType.forClassWithGenerics(collectionType, element), null, null);
	}

	/**
	 * 从 {@link java.util.Map} 类型创建新的类型描述符。
	 * <p>对于转换为类型化映射很有用。
	 * <p>例如，可以通过转换为此方法构建的目标类型将 {@code Map<String, String>} 转换为 {@code Map<Id, EmailAddress>}：
	 * 构建这样的 TypeDescriptor 的方法调用看起来像：
	 * <pre class="code">
	 * map(Map.class, TypeDescriptor.valueOf(Id.class), TypeDescriptor.valueOf(EmailAddress.class));
	 * </pre>
	 * @param mapType 映射类型，必须实现 {@link Map}
	 * @param keyTypeDescriptor 映射键类型的描述符，用于转换映射键
	 * @param valueTypeDescriptor 映射值类型，用于转换映射值
	 * @return 映射类型描述符
	 */
	public static TypeDescriptor map(Class<?> mapType, @Nullable TypeDescriptor keyTypeDescriptor,
			@Nullable TypeDescriptor valueTypeDescriptor) {

		Assert.notNull(mapType, "Map type must not be null");
		if (!Map.class.isAssignableFrom(mapType)) {
			throw new IllegalArgumentException("Map type must be a [java.util.Map]");
		}
		ResolvableType key = (keyTypeDescriptor != null ? keyTypeDescriptor.resolvableType : null);
		ResolvableType value = (valueTypeDescriptor != null ? valueTypeDescriptor.resolvableType : null);
		return new TypeDescriptor(ResolvableType.forClassWithGenerics(mapType, key, value), null, null);
	}

	/**
	 * 创建指定类型的新数组类型描述符。
	 * <p>例如，要创建 {@code Map<String,String>[]}，请使用：
	 * <pre class="code">
	 * TypeDescriptor.array(TypeDescriptor.map(Map.class, TypeDescriptor.value(String.class), TypeDescriptor.value(String.class)));
	 * </pre>
	 * @param elementTypeDescriptor 数组元素的 {@link TypeDescriptor}，如果为 {@code null} 则返回 {@code null}
	 * @return 数组的 {@link TypeDescriptor}，如果 {@code elementTypeDescriptor} 为 {@code null} 则返回 {@code null}
	 * @since 3.2.1
	 */
	@Nullable
	@Contract("!null -> !null; null -> null")
	public static TypeDescriptor array(@Nullable TypeDescriptor elementTypeDescriptor) {
		if (elementTypeDescriptor == null) {
			return null;
		}
		return new TypeDescriptor(ResolvableType.forArrayComponent(elementTypeDescriptor.resolvableType),
				null, elementTypeDescriptor.getAnnotations());
	}

	/**
	 * 为方法参数内声明的嵌套类型创建类型描述符。
	 * <p>例如，如果 methodParameter 是 {@code List<String>} 且嵌套层级为 1，
	 * 则嵌套类型描述符将是 String.class。
	 * <p>如果 methodParameter 是 {@code List<List<String>>} 且嵌套层级为 2，
	 * 则嵌套类型描述符也将是 String.class。
	 * <p>如果 methodParameter 是 {@code Map<Integer, String>} 且嵌套层级为 1，
	 * 则嵌套类型描述符将是 String，来源于映射值。
	 * <p>如果 methodParameter 是 {@code List<Map<Integer, String>>} 且嵌套层级为 2，
	 * 则嵌套类型描述符将是 String，来源于映射值。
	 * <p>如果无法获取嵌套类型（因为它未被声明），则返回 {@code null}。
	 * 例如，如果方法参数是 {@code List<?>}，则返回的嵌套类型描述符将为 {@code null}。
	 * @param methodParameter 嵌套层级为 1 的方法参数
	 * @param nestingLevel 方法参数内集合/数组元素或映射键/值声明的嵌套层级
	 * @return 指定嵌套层级的嵌套类型描述符，如果无法获取则返回 {@code null}
	 * @throws IllegalArgumentException 如果输入的 {@link MethodParameter} 参数的嵌套层级不是 1，
	 * 或者直到指定嵌套层级的类型不是集合、数组或映射类型
	 */
	@Nullable
	public static TypeDescriptor nested(MethodParameter methodParameter, int nestingLevel) {
		if (methodParameter.getNestingLevel() != 1) {
			throw new IllegalArgumentException("MethodParameter nesting level must be 1: " +
					"use the nestingLevel parameter to specify the desired nestingLevel for nested type traversal");
		}
		return new TypeDescriptor(methodParameter).nested(nestingLevel);
	}

	/**
	 * 为字段内声明的嵌套类型创建类型描述符。
	 * <p>例如，如果字段是 {@code List<String>} 且嵌套层级为 1，
	 * 则嵌套类型描述符将是 {@code String.class}。
	 * <p>如果字段是 {@code List<List<String>>} 且嵌套层级为 2，
	 * 则嵌套类型描述符也将是 {@code String.class}。
	 * <p>如果字段是 {@code Map<Integer, String>} 且嵌套层级为 1，
	 * 则嵌套类型描述符将是 String，来源于映射值。
	 * <p>如果字段是 {@code List<Map<Integer, String>>} 且嵌套层级为 2，
	 * 则嵌套类型描述符将是 String，来源于映射值。
	 * <p>如果无法获取嵌套类型（因为它未被声明），则返回 {@code null}。
	 * 例如，如果字段是 {@code List<?>}，则返回的嵌套类型描述符将为 {@code null}。
	 * @param field 字段
	 * @param nestingLevel 字段内集合/数组元素或映射键/值声明的嵌套层级
	 * @return 指定嵌套层级的嵌套类型描述符，如果无法获取则返回 {@code null}
	 * @throws IllegalArgumentException 如果直到指定嵌套层级的类型不是集合、数组或映射类型
	 */
	@Nullable
	public static TypeDescriptor nested(Field field, int nestingLevel) {
		return new TypeDescriptor(field).nested(nestingLevel);
	}

	/**
	 * 为属性内声明的嵌套类型创建类型描述符。
	 * <p>例如，如果属性是 {@code List<String>} 且嵌套层级为 1，
	 * 则嵌套类型描述符将是 {@code String.class}。
	 * <p>如果属性是 {@code List<List<String>>} 且嵌套层级为 2，
	 * 则嵌套类型描述符也将是 {@code String.class}。
	 * <p>如果属性是 {@code Map<Integer, String>} 且嵌套层级为 1，
	 * 则嵌套类型描述符将是 String，来源于映射值。
	 * <p>如果属性是 {@code List<Map<Integer, String>>} 且嵌套层级为 2，
	 * 则嵌套类型描述符将是 String，来源于映射值。
	 * <p>如果无法获取嵌套类型（因为它未被声明），则返回 {@code null}。
	 * 例如，如果属性是 {@code List<?>}，则返回的嵌套类型描述符将为 {@code null}。
	 * @param property 属性
	 * @param nestingLevel 属性内集合/数组元素或映射键/值声明的嵌套层级
	 * @return 指定嵌套层级的嵌套类型描述符，如果无法获取则返回 {@code null}
	 * @throws IllegalArgumentException 如果直到指定嵌套层级的类型不是集合、数组或映射类型
	 */
	@Nullable
	public static TypeDescriptor nested(Property property, int nestingLevel) {
		return new TypeDescriptor(property).nested(nestingLevel);
	}

	private static String getName(Class<?> clazz) {
		String canonicalName = clazz.getCanonicalName();
		return (canonicalName != null ? canonicalName : clazz.getName());
	}


	/**
	 * 适配器类，用于将 {@code TypeDescriptor} 的注解暴露为 {@link AnnotatedElement}，
	 * 特别是供 {@link AnnotatedElementUtils} 使用。
	 * @see AnnotatedElementUtils#isAnnotated(AnnotatedElement, Class)
	 * @see AnnotatedElementUtils#getMergedAnnotation(AnnotatedElement, Class)
	 */
	private static final class AnnotatedElementAdapter implements AnnotatedElement, Serializable {

		private static final AnnotatedElementAdapter EMPTY = new AnnotatedElementAdapter(new Annotation[0]);

		private final Annotation[] annotations;

		private AnnotatedElementAdapter(Annotation[] annotations) {
			this.annotations = annotations;
		}

		private static AnnotatedElementAdapter from(@Nullable Annotation[] annotations) {
			if (annotations == null || annotations.length == 0) {
				return EMPTY;
			}
			return new AnnotatedElementAdapter(annotations);
		}

		@Override
		public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
			for (Annotation annotation : this.annotations) {
				if (annotation.annotationType() == annotationClass) {
					return true;
				}
			}
			return false;
		}

		@Override
		@Nullable
		@SuppressWarnings("unchecked")
		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			for (Annotation annotation : this.annotations) {
				if (annotation.annotationType() == annotationClass) {
					return (T) annotation;
				}
			}
			return null;
		}

		@Override
		public Annotation[] getAnnotations() {
			return (isEmpty() ? this.annotations : this.annotations.clone());
		}

		@Override
		public Annotation[] getDeclaredAnnotations() {
			return getAnnotations();
		}

		public boolean isEmpty() {
			return (this.annotations.length == 0);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof AnnotatedElementAdapter that &&
					Arrays.equals(this.annotations, that.annotations)));
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(this.annotations);
		}

		@Override
		public String toString() {
			return Arrays.toString(this.annotations);
		}
	}


	private interface AnnotatedElementSupplier extends Supplier<AnnotatedElementAdapter>, Serializable {
	}

}
