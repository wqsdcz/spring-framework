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

package org.springframework.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import kotlin.Unit;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Helper class that encapsulates the specification of a method parameter, i.e. a {@link Method}
 * or {@link Constructor} plus a parameter index and a nested type index for a declared generic
 * type. Useful as a specification object to pass along.
 * <p>这是一个帮助类，用于封装方法参数的规范（"规范"指的是参数的完整描述信息），
 * 即 {@link Method} 或 {@link Constructor} 、参数索引、嵌套类型索引（声明的泛型类型）。
 * 用作传递的规范对象很有用。
 *
 * <p>As of 4.2, there is a {@link org.springframework.core.annotation.SynthesizingMethodParameter}
 * subclass available which synthesizes annotations with attribute aliases. That subclass is used
 * for web and message endpoint processing, in particular.
 * <p>从4.2开始，有一个可用的子类 {@link org.springframework.core.annotation.SynthesizingMethodParameter}
 * 它合成带有属性别名的注解。特别是，该子类用于web和消息端点处理。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Andy Clement
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Phillip Webb
 * @since 2.0
 * @see org.springframework.core.annotation.SynthesizingMethodParameter
 */
public class MethodParameter {

	private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];


	private final Executable executable; // 方法或构造函数

	private final int parameterIndex; // 参数索引

	@Nullable
	private volatile Parameter parameter; // 参数：-1 表示方法返回类型；0 表示第一个方法参数；1 表示第二个方法参数

	private int nestingLevel; // 嵌套级别

	/** Map from Integer level to Integer type index. */
	@Nullable
	Map<Integer, Integer> typeIndexesPerLevel;

	/** The containing class. Could also be supplied by overriding {@link #getContainingClass()} */
	@Nullable
	private volatile Class<?> containingClass; // 包含类

	@Nullable
	private volatile Class<?> parameterType; // 参数类型

	@Nullable
	private volatile Type genericParameterType; // 参数的泛型类型

	@Nullable
	private volatile Annotation[] parameterAnnotations; // 参数的注解

	@Nullable
	private volatile ParameterNameDiscoverer parameterNameDiscoverer; // 参数名称解析器

	@Nullable
	volatile String parameterName; // 参数名称

	@Nullable
	private volatile MethodParameter nestedMethodParameter; // 嵌套的MethodParameter


	/**
	 * <p>为给定方法创建一个新的 {@code MethodParameter}，嵌套级别为1。
	 * @param method 要指定的参数的方法
	 * @param parameterIndex 参数的索引：-1 表示方法返回类型；0 表示第一个方法参数；1 表示第二个方法参数，等等。
	 */
	public MethodParameter(Method method, int parameterIndex) {
		this(method, parameterIndex, 1);
	}

	/**
	 * <p>为给定方法创建一个新的 {@code MethodParameter}。
	 * @param method 要指定参数的方法
	 * @param parameterIndex 参数的索引：-1 表示方法返回类型；0 表示第一个方法参数；1 表示第二个方法参数，等等。
	 * @param nestingLevel 目标类型的嵌套级别（通常为1；例如，在List of Lists的情况下，1表示嵌套的List，而2表示嵌套List的元素）
	 */
	public MethodParameter(Method method, int parameterIndex, int nestingLevel) {
		Assert.notNull(method, "Method must not be null");
		this.executable = method;
		this.parameterIndex = validateIndex(method, parameterIndex);
		this.nestingLevel = nestingLevel;
	}

	/**
	 * <p>为给定构造函数创建一个新的MethodParameter，嵌套级别为1。
	 * @param constructor 要指定参数的构造函数
	 * @param parameterIndex 参数的索引
	 */
	public MethodParameter(Constructor<?> constructor, int parameterIndex) {
		this(constructor, parameterIndex, 1);
	}

	/**
	 * <p>为给定构造函数创建一个新的MethodParameter。
	 * @param constructor 要指定参数的构造函数
	 * @param parameterIndex 参数的索引
	 * @param nestingLevel 目标类型的嵌套级别（通常为1；例如，在List of Lists的情况下，1表示嵌套的List，而2表示嵌套List的元素）
	 */
	public MethodParameter(Constructor<?> constructor, int parameterIndex, int nestingLevel) {
		Assert.notNull(constructor, "Constructor must not be null");
		this.executable = constructor;
		this.parameterIndex = validateIndex(constructor, parameterIndex);
		this.nestingLevel = nestingLevel;
	}


	/**
	 * <p>内部构造函数，用于创建一个已经设置了包含类的 {@link MethodParameter}。
	 * 
	 * @param executable 要指定参数的可执行对象
	 * @param parameterIndex 参数的索引
	 * @param containingClass 包含类
	 * @since 5.2
	 */
	MethodParameter(Executable executable, int parameterIndex, @Nullable Class<?> containingClass) {
		Assert.notNull(executable, "Executable must not be null");
		this.executable = executable;
		this.parameterIndex = validateIndex(executable, parameterIndex);
		this.nestingLevel = 1;
		this.containingClass = containingClass;
	}


	/**
	 * Copy constructor, resulting in an independent MethodParameter object
	 * based on the same metadata and cache state that the original object was in.
	 * <p>复制构造函数，基于原始对象所处的相同元数据和缓存状态，生成一个独立的 MethodParameter 对象。
	 * @param original the original MethodParameter object to copy from 要复制的原始 MethodParameter 对象
	 */
	public MethodParameter(MethodParameter original) {
		Assert.notNull(original, "Original must not be null");
		this.executable = original.executable;
		this.parameterIndex = original.parameterIndex;
		this.parameter = original.parameter;
		this.nestingLevel = original.nestingLevel;
		this.typeIndexesPerLevel = original.typeIndexesPerLevel;
		this.containingClass = original.containingClass;
		this.parameterType = original.parameterType;
		this.genericParameterType = original.genericParameterType;
		this.parameterAnnotations = original.parameterAnnotations;
		this.parameterNameDiscoverer = original.parameterNameDiscoverer;
		this.parameterName = original.parameterName;
	}


	/**
	 * Return the wrapped Method, if any.
	 * <p>返回包装的Method（如果有）。
	 * <p>Note: Either Method or Constructor is available.
	 * <p>注意：Method或Constructor中有一个可用。
	 * @return the Method, or {@code null} if none 对应的Method，如果没有则返回 {@code null}
	 */
	@Nullable
	public Method getMethod() {
		return (this.executable instanceof Method method ? method : null);
	}

	/**
	 * Return the wrapped Constructor, if any.
	 * <p>返回包装的Constructor（如果有）。
	 * <p>Note: Either Method or Constructor is available.
	 * <p>注意：Method或Constructor中有一个可用。
	 * @return the Constructor, or {@code null} if none  对应的Constructor，如果没有则返回 {@code null}
	 */
	@Nullable
	public Constructor<?> getConstructor() {
		return (this.executable instanceof Constructor<?> constructor ? constructor : null);
	}

	/**
	 * Return the class that declares the underlying Method or Constructor.
	 * <p>返回声明底层Method或Constructor的类。
	 */
	public Class<?> getDeclaringClass() {
		return this.executable.getDeclaringClass();
	}

	/**
	 * Return the wrapped member.
	 * <p>返回其中包装的成员。
	 * @return the Method or Constructor as Member  作为Member的Method或Constructor
	 */
	public Member getMember() {
		return this.executable;
	}

	/**
	 * Return the wrapped annotated element.
	 * <p>返回包装的带注解元素。
	 * <p>Note: This method exposes the annotations declared on the method/constructor
	 * itself (i.e. at the method/constructor level, not at the parameter level).
	 * <p>注意：此方法暴露在方法/构造函数本身上声明的注解
	 * （即在方法/构造函数级别，而不是在参数级别）。
	 * <p>To get the {@link AnnotatedElement} at the parameter level, use
	 * {@link #getParameter()}.
	 * <p>要获取参数级别的 {@link AnnotatedElement}，使用
	 * {@link #getParameter()}。
	 * @return the Method or Constructor as AnnotatedElement  作为AnnotatedElement的Method或Constructor
	 */
	public AnnotatedElement getAnnotatedElement() {
		return this.executable;
	}

	/**
	 * Return the wrapped executable.
	 * <p>返回包装的可执行对象。
	 * @return the Method or Constructor as Executable  作为Executable的Method或Constructor
	 * @since 5.0
	 */
	public Executable getExecutable() {
		return this.executable;
	}

	/**
	 * Return the {@link Parameter} descriptor for method/constructor parameter.
	 * <p>返回方法/构造函数参数的 {@link Parameter} 描述符。
	 * @since 5.0
	 */
	public Parameter getParameter() {
		if (this.parameterIndex < 0) {
			throw new IllegalStateException("Cannot retrieve Parameter descriptor for method return type");
		}
		Parameter parameter = this.parameter;
		if (parameter == null) {
			parameter = getExecutable().getParameters()[this.parameterIndex];
			this.parameter = parameter;
		}
		return parameter;
	}

	/**
	 * Return the index of the method/constructor parameter.
	 * <p>返回方法/构造函数参数的索引。
	 * @return the parameter index (-1 in case of the return type)  参数索引（如果是返回类型则为-1）
	 */
	public int getParameterIndex() {
		return this.parameterIndex;
	}

	/**
	 * Increase this parameter's nesting level.
	 * <p>增加此参数的嵌套级别。
	 * @see #getNestingLevel()
	 * @deprecated since 5.2 in favor of {@link #nested(Integer)}
	 * <p>自 5.2 版本起不再推荐使用，建议使用 {@link #nested(Integer)}
	 */
	@Deprecated
	public void increaseNestingLevel() {
		this.nestingLevel++;
	}

	/**
	 * Decrease this parameter's nesting level.
	 * <p>降低此参数的嵌套级别。
	 * @see #getNestingLevel()
	 * @deprecated since 5.2 in favor of retaining the original MethodParameter and
	 * using {@link #nested(Integer)} if nesting is required
	 * <p> 自 5.2 版本起不推荐使用，建议保留原始的 MethodParameter，
	 * 如果需要嵌套则使用 {@link #nested(Integer)}
	 */
	@Deprecated
	public void decreaseNestingLevel() {
		getTypeIndexesPerLevel().remove(this.nestingLevel);
		this.nestingLevel--;
	}


	/**
	 * Return the nesting level of the target type
	 * (typically 1; for example, in case of a List of Lists, 1 would indicate the
	 * nested List, whereas 2 would indicate the element of the nested List).
	 * <p>返回目标类型的嵌套级别
	 * （通常为1；例如，在List of Lists的情况下，1表示嵌套的List，而2表示嵌套List的元素）。
	 */
	public int getNestingLevel() {
		return this.nestingLevel;
	}


	/**
	 * Return a variant of this {@code MethodParameter} with the type
	 * for the current level set to the specified value.
	 * <p>返回此 {@code MethodParameter} 的一个变体，其当前级别的类型设置为指定值。
	 * @param typeIndex the new type index
	 * <p>新的类型索引
	 * @since 5.2
	 */
	public MethodParameter withTypeIndex(int typeIndex) {
		return nested(this.nestingLevel, typeIndex);
	}


	/**
	 * Set the type index for the current nesting level.
	 * <p>设置当前嵌套级别的类型索引。
	 * @param typeIndex the corresponding type index
	 * <p>相应的类型索引
	 * (or {@code null} for the default type index)
	 * <p>（或 {@code null} 表示默认类型索引）
	 * @see #getNestingLevel()
	 * <p>参见 #getNestingLevel()
	 * @deprecated since 5.2 in favor of {@link #withTypeIndex}
	 * <p>自 5.2 版本起已废弃，建议使用 {@link #withTypeIndex}
	 */
	@Deprecated
	public void setTypeIndexForCurrentLevel(int typeIndex) {
		getTypeIndexesPerLevel().put(this.nestingLevel, typeIndex);
	}


	/**
	 * Return the type index for the current nesting level.
	 * <p>返回当前嵌套级别的类型索引。
	 * @return the corresponding type index, or {@code null}
	 * if none specified (indicating the default type index)
	 * <p>返回相应的类型索引，如果没有指定则返回 {@code null}（表示默认类型索引）
	 * @see #getNestingLevel()
	 */
	@Nullable
	public Integer getTypeIndexForCurrentLevel() {
		return getTypeIndexForLevel(this.nestingLevel);
	}


	/**
	 * Return the type index for the specified nesting level.
	 * <p>返回指定嵌套级别的类型索引。
	 * @param nestingLevel the nesting level to check
	 * <p>要检查的嵌套级别
	 * @return the corresponding type index, or {@code null}
	 * if none specified (indicating the default type index)
	 * <p>相应的类型索引，或  如果没有指定（表示默认类型索引），则为{@code null}
	 */
	@Nullable
	public Integer getTypeIndexForLevel(int nestingLevel) {
		return getTypeIndexesPerLevel().get(nestingLevel);
	}


	/**
	 * Obtain the (lazily constructed) type-indexes-per-level Map.
	 * <p>获取（延迟构建的）每层类型索引映射。
	 */
	private Map<Integer, Integer> getTypeIndexesPerLevel() {
		if (this.typeIndexesPerLevel == null) {
			this.typeIndexesPerLevel = new HashMap<>(4);
		}
		return this.typeIndexesPerLevel;
	}


	/**
	 * Return a variant of this {@code MethodParameter} which points to the
	 * same parameter but one nesting level deeper.
	 * <p>返回此 {@code MethodParameter} 的一个变体，指向相同的参数但嵌套级别更深一级。
	 * @since 4.3
	 */
	public MethodParameter nested() {
		return nested(null);
	}


	/**
	 * Return a variant of this {@code MethodParameter} which points to the
	 * same parameter but one nesting level deeper.
	 * <p>返回此 {@code MethodParameter} 的一个变体，指向相同的参数但嵌套级别更深一级。
	 * @param typeIndex the type index for the new nesting level
	 * <p>新嵌套级别的类型索引
	 * @since 5.2
	 */
	public MethodParameter nested(@Nullable Integer typeIndex) {
		MethodParameter nestedParam = this.nestedMethodParameter;
		if (nestedParam != null && typeIndex == null) {
			return nestedParam;
		}
		nestedParam = nested(this.nestingLevel + 1, typeIndex);
		if (typeIndex == null) {
			this.nestedMethodParameter = nestedParam;
		}
		return nestedParam;
	}

	private MethodParameter nested(int nestingLevel, @Nullable Integer typeIndex) {
		MethodParameter copy = clone();
		copy.nestingLevel = nestingLevel;
		if (this.typeIndexesPerLevel != null) {
			copy.typeIndexesPerLevel = new HashMap<>(this.typeIndexesPerLevel);
		}
		if (typeIndex != null) {
			copy.getTypeIndexesPerLevel().put(copy.nestingLevel, typeIndex);
		}
		copy.parameterType = null;
		copy.genericParameterType = null;
		return copy;
	}


	/**
	 * Return whether this method indicates a parameter which is not required:
	 * either in the form of Java 8's {@link java.util.Optional}, any variant
	 * of a parameter-level {@code Nullable} annotation (such as from JSR-305
	 * or the FindBugs set of annotations), or a language-level nullable type
	 * declaration or {@code Continuation} parameter in Kotlin.
	 * <p>返回此方法是否指示一个非必需的参数：
	 * 以 Java 8 的 {@link java.util.Optional} 形式，任何参数级 {@code Nullable} 注解的变体
	 * （例如来自 JSR-305 或 FindBugs 注解集合），或 Kotlin 中的语言级可空类型
	 * 声明或 {@code Continuation} 参数。
	 * @since 4.3
	 */
	public boolean isOptional() {
		return (getParameterType() == Optional.class || hasNullableAnnotation() ||
				(KotlinDetector.isKotlinReflectPresent() &&
						KotlinDetector.isKotlinType(getContainingClass()) &&
						KotlinDelegate.isOptional(this)));
	}


	/**
	 * Check whether this method parameter is annotated with any variant of a
	 * {@code Nullable} annotation, for example, {@code jakarta.annotation.Nullable} or
	 * {@code edu.umd.cs.findbugs.annotations.Nullable}.
	 * <p>检查此方法参数是否使用了 {@code Nullable} 注解的任何变体进行注解，
	 * 例如 {@code jakarta.annotation.Nullable} 或 {@code edu.umd.cs.findbugs.annotations.Nullable}。
	 */
	private boolean hasNullableAnnotation() {
		for (Annotation ann : getParameterAnnotations()) {
			if ("Nullable".equals(ann.annotationType().getSimpleName())) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Return a variant of this {@code MethodParameter} which points to
	 * the same parameter but one nesting level deeper in case of a
	 * {@link java.util.Optional} declaration.
	 * <p>返回这个 {@code MethodParameter} 的一个变体，指向同一个参数，
	 * 但在 {@link java.util.Optional} 声明的情况下嵌套层级更深一级。
	 * @since 4.3
	 * @see #isOptional()
	 * @see #nested()
	 */
	public MethodParameter nestedIfOptional() {
		return (getParameterType() == Optional.class ? nested() : this);
	}


	/**
	 * Return a variant of this {@code MethodParameter} which refers to the
	 * given containing class.
	 * <p>返回此 {@code MethodParameter} 的一个变体，它引用给定的包含类。
	 * @param containingClass a specific containing class (potentially a
	 * subclass of the declaring class, for example, substituting a type variable)
	 * <p>一个特定的包含类（可能是声明类的子类，例如替换类型变量）
	 * @since 5.2
	 * @see #getParameterType()
	 */
	public MethodParameter withContainingClass(@Nullable Class<?> containingClass) {
		MethodParameter result = clone();
		result.containingClass = containingClass;
		result.parameterType = null;
		return result;
	}

	/**
	 * Set a containing class to resolve the parameter type against.
	 * <p>设置一个包含类来解析参数类型。
	 */
	@Deprecated
	void setContainingClass(Class<?> containingClass) {
		this.containingClass = containingClass;
		this.parameterType = null;
	}

	/**
	 * Return the containing class for this method parameter.
	 * <p>返回此方法参数的包含类。</p>
	 * @return a specific containing class (potentially a subclass of the
	 * declaring class), or otherwise simply the declaring class itself
	 * <p>一个特定的包含类（可能是声明类的子类），否则就是声明类本身</p>
	 * @see #getDeclaringClass()
	 * <p>参见 #getDeclaringClass()</p>
	 */
	public Class<?> getContainingClass() {
		Class<?> containingClass = this.containingClass;
		return (containingClass != null ? containingClass : getDeclaringClass());
	}

	/**
	 * Set a resolved (generic) parameter type.
	 * <p>设置已解析的（泛型）参数类型。
	 */
	@Deprecated
	void setParameterType(@Nullable Class<?> parameterType) {
		this.parameterType = parameterType;
	}

	/**
	 * Return the type of the method/constructor parameter.
	 * <p>返回方法/构造函数参数的类型。</p>
	 * @return the parameter type (never {@code null})
	 * <p>参数类型（永远不会是 {@code null}）</p>
	 */
	public Class<?> getParameterType() {
		Class<?> paramType = this.parameterType;
		if (paramType != null) {
			return paramType;
		}
		if (getContainingClass() != getDeclaringClass()) {
			paramType = ResolvableType.forMethodParameter(this, null, 1).resolve();
		}
		if (paramType == null) {
			paramType = computeParameterType();
		}
		this.parameterType = paramType;
		return paramType;
	}

	/**
	 * Return the generic type of the method/constructor parameter.
	 * <p>返回方法/构造函数参数的泛型类型。</p>
	 * @return the parameter type (never {@code null})
	 * <p>参数类型（永远不会是 {@code null}）</p>
	 * @since 3.0
	 */
	public Type getGenericParameterType() {
		Type paramType = this.genericParameterType;
		if (paramType == null) {
			if (this.parameterIndex < 0) {
				Method method = getMethod();
				paramType = (method != null ?
						(KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(getContainingClass()) ?
						KotlinDelegate.getGenericReturnType(method) : method.getGenericReturnType()) : void.class);
			}
			else {
				Type[] genericParameterTypes = this.executable.getGenericParameterTypes();
				int index = this.parameterIndex;
				if (this.executable instanceof Constructor &&
						ClassUtils.isInnerClass(this.executable.getDeclaringClass()) &&
						genericParameterTypes.length == this.executable.getParameterCount() - 1) {
					// Bug in javac: type array excludes enclosing instance parameter
					// for inner classes with at least one generic constructor parameter,
					// so access it with the actual parameter index lowered by 1
					index = this.parameterIndex - 1;
				}
				paramType = (index >= 0 && index < genericParameterTypes.length ?
						genericParameterTypes[index] : computeParameterType());
			}
			this.genericParameterType = paramType;
		}
		return paramType;
	}

	private Class<?> computeParameterType() {
		if (this.parameterIndex < 0) {
			Method method = getMethod();
			if (method == null) {
				return void.class;
			}
			if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(getContainingClass())) {
				return KotlinDelegate.getReturnType(method);
			}
			return method.getReturnType();
		}
		return this.executable.getParameterTypes()[this.parameterIndex];
	}


	/**
	 * Return the nested type of the method/constructor parameter.
	 * <p>返回方法/构造函数参数的嵌套类型。
	 * @return the parameter type (never {@code null})
	 * <p>返回参数类型（永远不会是 {@code null}）
	 * @since 3.1
	 * @see #getNestingLevel()
	 * <p>参见 #getNestingLevel()
	 */
	public Class<?> getNestedParameterType() {
		if (this.nestingLevel > 1) {
			Type type = getGenericParameterType();
			for (int i = 2; i <= this.nestingLevel; i++) {
				if (type instanceof ParameterizedType parameterizedType) {
					Type[] args = parameterizedType.getActualTypeArguments();
					Integer index = getTypeIndexForLevel(i);
					type = args[index != null ? index : args.length - 1];
				}
				// TODO: Object.class if unresolvable
			}
			if (type instanceof Class<?> clazz) {
				return clazz;
			}
			else if (type instanceof ParameterizedType parameterizedType) {
				Type arg = parameterizedType.getRawType();
				if (arg instanceof Class<?> clazz) {
					return clazz;
				}
			}
			return Object.class;
		}
		else {
			return getParameterType();
		}
	}


	/**
	 * Return the nested generic type of the method/constructor parameter.
	 * <p>返回方法/构造函数参数的嵌套泛型类型。</p>
	 * @return the parameter type (never {@code null})
	 * <p>返回参数类型（永远不会是 {@code null}）</p>
	 * @since 4.2
	 * @see #getNestingLevel()
	 */
	public Type getNestedGenericParameterType() {
		if (this.nestingLevel > 1) {
			Type type = getGenericParameterType();
			for (int i = 2; i <= this.nestingLevel; i++) {
				if (type instanceof ParameterizedType parameterizedType) {
					Type[] args = parameterizedType.getActualTypeArguments();
					Integer index = getTypeIndexForLevel(i);
					type = args[index != null ? index : args.length - 1];
				}
			}
			return type;
		}
		else {
			return getGenericParameterType();
		}
	}


	/**
	 * Return the annotations associated with the target method/constructor itself.
	 * <p>返回与目标方法/构造函数本身关联的注解。
	 */
	public Annotation[] getMethodAnnotations() {
		return adaptAnnotationArray(getAnnotatedElement().getAnnotations());
	}

	/**
	 * Return the method/constructor annotation of the given type, if available.
	 * <p>返回给定类型的 方法/构造函数 注解，如果可用的话。
	 * @param annotationType the annotation type to look for
	 * <p>要查找的注解类型
	 * @return the annotation object, or {@code null} if not found
	 * <p>注解对象，如果未找到则返回 {@code null}
	 */
	@Nullable
	public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
		A annotation = getAnnotatedElement().getAnnotation(annotationType);
		return (annotation != null ? adaptAnnotation(annotation) : null);
	}


	/**
	 * Return whether the method/constructor is annotated with the given type.
	 * <p>返回方法/构造函数是否使用给定类型进行注解。
	 * @param annotationType the annotation type to look for
	 * <p>要查找的注解类型
	 * @since 4.3
	 * @see #getMethodAnnotation(Class)
	 */
	public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
		return getAnnotatedElement().isAnnotationPresent(annotationType);
	}


	/**
	 * Return the annotations associated with the specific method/constructor parameter.
	 * <p>返回与特定方法/构造函数参数相关联的注解。
	 */
	public Annotation[] getParameterAnnotations() {
		Annotation[] paramAnns = this.parameterAnnotations;
		if (paramAnns == null) {
			Annotation[][] annotationArray = this.executable.getParameterAnnotations();
			int index = this.parameterIndex;
			if (this.executable instanceof Constructor &&
					ClassUtils.isInnerClass(this.executable.getDeclaringClass()) &&
					annotationArray.length == this.executable.getParameterCount() - 1) {
				// Bug in javac in JDK <9: annotation array excludes enclosing instance parameter
				// for inner classes, so access it with the actual parameter index lowered by 1
				index = this.parameterIndex - 1;
			}
			paramAnns = (index >= 0 && index < annotationArray.length && annotationArray[index].length > 0 ?
					adaptAnnotationArray(annotationArray[index]) : EMPTY_ANNOTATION_ARRAY);
			this.parameterAnnotations = paramAnns;
		}
		return paramAnns;
	}


	/**
	 * Return {@code true} if the parameter has at least one annotation,
	 * {@code false} if it has none.
	 * <p>如果参数至少有一个注解，则返回 {@code true}，
	 * 如果没有任何注解，则返回 {@code false}。
	 * @see #getParameterAnnotations()
	 */
	public boolean hasParameterAnnotations() {
		return (getParameterAnnotations().length != 0);
	}

	/**
	 * Return the parameter annotation of the given type, if available.
	 * <p>返回给定类型的参数注解（如果可用）。
	 * @param annotationType the annotation type to look for
	 * <p>要查找的注解类型
	 * @return the annotation object, or {@code null} if not found
	 * <p>注解对象，如果未找到则返回 {@code null}
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public <A extends Annotation> A getParameterAnnotation(Class<A> annotationType) {
		Annotation[] anns = getParameterAnnotations();
		for (Annotation ann : anns) {
			if (annotationType.isInstance(ann)) {
				return (A) ann;
			}
		}
		return null;
	}


	/**
	 * Return whether the parameter is declared with the given annotation type.
	 * <p>返回参数是否使用给定注解类型声明。
	 * @param annotationType the annotation type to look for
	 * <p>要查找的注解类型
	 * @see #getParameterAnnotation(Class)
	 */
	public <A extends Annotation> boolean hasParameterAnnotation(Class<A> annotationType) {
		return (getParameterAnnotation(annotationType) != null);
	}


	/**
	 * Initialize parameter name discovery for this method parameter.
	 * <p>初始化此方法参数的参数名发现。
	 * <p>This method does not actually try to retrieve the parameter name at
	 * this point; it just allows discovery to happen when the application calls
	 * {@link #getParameterName()} (if ever).
	 * <p>此时此方法实际上并不尝试检索参数名；它只允许在应用程序调用
	 * {@link #getParameterName()} 时进行发现（如果有的话）。
	 */
	public void initParameterNameDiscovery(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}


	/**
	 * Return the name of the method/constructor parameter.
	 * <p>返回方法/构造函数参数的名称。
	 * @return the parameter name (may be {@code null} if no
	 * parameter name metadata is contained in the class file or no
	 * {@link #initParameterNameDiscovery ParameterNameDiscoverer}
	 * has been set to begin with)
	 * <p>返回参数名称（如果类文件中不包含参数名称元数据或没有设置
	 * {@link #initParameterNameDiscovery ParameterNameDiscoverer}
	 * 则可能是 {@code null}）
	 */
	@Nullable
	public String getParameterName() {
		if (this.parameterIndex < 0) {
			return null;
		}
		ParameterNameDiscoverer discoverer = this.parameterNameDiscoverer;
		if (discoverer != null) {
			String[] parameterNames = null;
			if (this.executable instanceof Method method) {
				parameterNames = discoverer.getParameterNames(method);
			}
			else if (this.executable instanceof Constructor<?> constructor) {
				parameterNames = discoverer.getParameterNames(constructor);
			}
			if (parameterNames != null && this.parameterIndex < parameterNames.length) {
				this.parameterName = parameterNames[this.parameterIndex];
			}
			this.parameterNameDiscoverer = null;
		}
		return this.parameterName;
	}



	/**
	 * A template method to post-process a given annotation instance before
	 * returning it to the caller.
	 * <p>一个模板方法，在将给定的注解实例返回给调用者之前对其进行后处理。
	 * <p>The default implementation simply returns the given annotation as-is.
	 * <p>默认实现只是简单地按原样返回给定的注解。
	 * @param annotation the annotation about to be returned
	 * <p>annotation 即将返回的注解
	 * @return the post-processed annotation (or simply the original one)
	 * <p>返回后处理后的注解（或仅仅是原始注解）
	 * @since 4.2
	 */
	protected <A extends Annotation> A adaptAnnotation(A annotation) {
		return annotation;
	}


	/**
	 * A template method to post-process a given annotation array before
	 * returning it to the caller.
	 * <p>一个模板方法，在将给定的注解数组返回给调用者之前进行后处理。
	 * <p>The default implementation simply returns the given annotation array as-is.
	 * <p>默认实现只是简单地按原样返回给定的注解数组。
	 * @param annotations the annotation array about to be returned
	 * <p>annotations 即将返回的注解数组
	 * @return the post-processed annotation array (or simply the original one)
	 * <p>返回后处理后的注解数组（或仅仅是原始数组）
	 * @since 4.2
	 */
	protected Annotation[] adaptAnnotationArray(Annotation[] annotations) {
		return annotations;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof MethodParameter that &&
				getContainingClass() == that.getContainingClass() &&
				ObjectUtils.nullSafeEquals(this.typeIndexesPerLevel, that.typeIndexesPerLevel) &&
				this.nestingLevel == that.nestingLevel &&
				this.parameterIndex == that.parameterIndex &&
				this.executable.equals(that.executable)));
	}

	@Override
	public int hashCode() {
		return (31 * this.executable.hashCode() + this.parameterIndex);
	}

	@Override
	public String toString() {
		Method method = getMethod();
		return (method != null ? "method '" + method.getName() + "'" : "constructor") +
				" parameter " + this.parameterIndex;
	}

	@Override
	public MethodParameter clone() {
		return new MethodParameter(this);
	}



	/**
	 * Create a new MethodParameter for the given method or constructor.
	 * <p>为给定的方法或构造函数创建一个新的 MethodParameter。
	 * <p>This is a convenience factory method for scenarios where a
	 * Method or Constructor reference is treated in a generic fashion.
	 * <p>这是一个便利的工厂方法，适用于以通用方式处理方法或构造函数引用的场景。
	 * @param methodOrConstructor the Method or Constructor to specify a parameter for
	 * <p>要为其指定参数的方法或构造函数
	 * @param parameterIndex the index of the parameter
	 * <p>参数的索引
	 * @return the corresponding MethodParameter instance
	 * <p>对应的 MethodParameter 实例
	 * @deprecated as of 5.0, in favor of {@link #forExecutable}
	 * <p>自 5.0 版本弃用，建议使用 {@link #forExecutable}
	 */
	@Deprecated
	public static MethodParameter forMethodOrConstructor(Object methodOrConstructor, int parameterIndex) {
		if (!(methodOrConstructor instanceof Executable executable)) {
			throw new IllegalArgumentException(
					"Given object [" + methodOrConstructor + "] is neither a Method nor a Constructor");
		}
		return forExecutable(executable, parameterIndex);
	}


	/**
	 * Create a new MethodParameter for the given method or constructor.
	 * <p>为给定的方法或构造函数创建一个新的 MethodParameter。
	 * <p>This is a convenience factory method for scenarios where a
	 * Method or Constructor reference is treated in a generic fashion.
	 * <p>这是一个便利的工厂方法，适用于以通用方式处理方法或构造函数引用的场景。
	 * @param executable the Method or Constructor to specify a parameter for
	 * <p>要为其指定参数的方法或构造函数
	 * @param parameterIndex the index of the parameter
	 * <p>参数的索引
	 * @return the corresponding MethodParameter instance
	 * <p>对应的 MethodParameter 实例
	 * @since 5.0
	 */

	public static MethodParameter forExecutable(Executable executable, int parameterIndex) {
		if (executable instanceof Method method) {
			return new MethodParameter(method, parameterIndex);
		}
		else if (executable instanceof Constructor<?> constructor) {
			return new MethodParameter(constructor, parameterIndex);
		}
		else {
			throw new IllegalArgumentException("Not a Method/Constructor: " + executable);
		}
	}


	/**
	 * Create a new MethodParameter for the given parameter descriptor.
	 * <p>为给定的参数描述符创建一个新的 MethodParameter。
	 * <p>This is a convenience factory method for scenarios where a
	 * Java 8 {@link Parameter} descriptor is already available.
	 * <p>这是一个便利的工厂方法，适用于已经存在 Java 8 {@link Parameter} 描述符的场景。
	 * @param parameter the parameter descriptor
	 * <p>参数描述符
	 * @return the corresponding MethodParameter instance
	 * <p>对应的 MethodParameter 实例
	 * @since 5.0
	 */
	public static MethodParameter forParameter(Parameter parameter) {
		return forExecutable(parameter.getDeclaringExecutable(), findParameterIndex(parameter));
	}

	protected static int findParameterIndex(Parameter parameter) {
		Executable executable = parameter.getDeclaringExecutable();
		Parameter[] allParams = executable.getParameters();
		// Try first with identity checks for greater performance.
		for (int i = 0; i < allParams.length; i++) {
			if (parameter == allParams[i]) {
				return i;
			}
		}
		// Potentially try again with object equality checks in order to avoid race
		// conditions while invoking java.lang.reflect.Executable.getParameters().
		for (int i = 0; i < allParams.length; i++) {
			if (parameter.equals(allParams[i])) {
				return i;
			}
		}
		throw new IllegalArgumentException("Given parameter [" + parameter +
				"] does not match any parameter in the declaring executable");
	}

	private static int validateIndex(Executable executable, int parameterIndex) {
		int count = executable.getParameterCount();
		Assert.isTrue(parameterIndex >= -1 && parameterIndex < count,
				() -> "Parameter index needs to be between -1 and " + (count - 1));
		return parameterIndex;
	}


	/**
	 * Create a new MethodParameter for the given field-aware constructor,
	 * for example, on a data class or record type.
	 * <p>为给定的感知字段的构造函数创建一个新的 MethodParameter，
	 * 例如，在数据类或记录类型上。
	 * <p>A field-aware method parameter will detect field annotations as well,
	 * as long as the field name matches the parameter name.
	 * <p>只要字段名称与参数名称匹配，感知字段的方法参数也将检测字段注解。
	 * @param ctor the Constructor to specify a parameter for
	 * <p>要指定参数的构造函数
	 * @param parameterIndex the index of the parameter
	 * <p>参数的索引
	 * @param fieldName the name of the underlying field,
	 * matching the constructor's parameter name
	 * <p>基础字段的名称，与构造函数的参数名称匹配
	 * @return the corresponding MethodParameter instance
	 * <p>对应的 MethodParameter 实例
	 * @since 6.1
	 */
	public static MethodParameter forFieldAwareConstructor(Constructor<?> ctor, int parameterIndex, String fieldName) {
		return new FieldAwareConstructorParameter(ctor, parameterIndex, fieldName);
	}



	/**
	 * {@link MethodParameter} subclass which detects field annotations as well.
	 * <p>{@link MethodParameter} 的子类，也可以检测字段注解。
	 */
	private static class FieldAwareConstructorParameter extends MethodParameter {

		@Nullable
		private volatile Annotation[] combinedAnnotations;

		public FieldAwareConstructorParameter(Constructor<?> constructor, int parameterIndex, String fieldName) {
			super(constructor, parameterIndex);
			this.parameterName = fieldName;
		}

		@Override
		public Annotation[] getParameterAnnotations() {
			String parameterName = this.parameterName;
			Assert.state(parameterName != null, "Parameter name not initialized");

			Annotation[] anns = this.combinedAnnotations;
			if (anns == null) {
				anns = super.getParameterAnnotations();
				try {
					Field field = getDeclaringClass().getDeclaredField(parameterName);
					Annotation[] fieldAnns = field.getAnnotations();
					if (fieldAnns.length > 0) {
						List<Annotation> merged = new ArrayList<>(anns.length + fieldAnns.length);
						merged.addAll(Arrays.asList(anns));
						for (Annotation fieldAnn : fieldAnns) {
							boolean existingType = false;
							for (Annotation ann : anns) {
								if (ann.annotationType() == fieldAnn.annotationType()) {
									existingType = true;
									break;
								}
							}
							if (!existingType) {
								merged.add(fieldAnn);
							}
						}
						anns = merged.toArray(EMPTY_ANNOTATION_ARRAY);
					}
				}
				catch (NoSuchFieldException | SecurityException ex) {
					// ignore
				}
				this.combinedAnnotations = anns;
			}
			return anns;
		}
	}


	/**
	 * Inner class to avoid a hard dependency on Kotlin at runtime.
	 * <p>内部类，避免在运行时对 Kotlin 产生硬依赖。
	 */
	private static class KotlinDelegate {

		/**
		 * Check whether the specified {@link MethodParameter} represents a nullable Kotlin type,
		 * an optional parameter (with a default value in the Kotlin declaration) or a
		 * {@code Continuation} parameter used in suspending functions.
		 * <p>检查指定的 {@link MethodParameter} 是否表示可空的Kotlin类型，
		 * 可选参数（在Kotlin声明中有默认值）或在挂起函数中使用的 {@code Continuation} 参数。
		 */
		public static boolean isOptional(MethodParameter param) {
			Method method = param.getMethod();
			int index = param.getParameterIndex();
			if (method != null && index == -1) {
				KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
				return (function != null && function.getReturnType().isMarkedNullable());
			}
			KFunction<?> function;
			Predicate<KParameter> predicate;
			if (method != null) {
				if (param.getParameterType().getName().equals("kotlin.coroutines.Continuation")) {
					return true;
				}
				function = ReflectJvmMapping.getKotlinFunction(method);
				predicate = p -> KParameter.Kind.VALUE.equals(p.getKind());
			}
			else {
				Constructor<?> ctor = param.getConstructor();
				Assert.state(ctor != null, "Neither method nor constructor found");
				function = ReflectJvmMapping.getKotlinFunction(ctor);
				predicate = p -> (KParameter.Kind.VALUE.equals(p.getKind()) ||
						KParameter.Kind.INSTANCE.equals(p.getKind()));
			}
			if (function != null) {
				int i = 0;
				for (KParameter kParameter : function.getParameters()) {
					if (predicate.test(kParameter)) {
						if (index == i++) {
							return (kParameter.getType().isMarkedNullable() || kParameter.isOptional());
						}
					}
				}
			}
			return false;
		}


		/**
		 * Return the generic return type of the method, with support of suspending
		 * functions via Kotlin reflection.
		 * <p>返回方法的泛型返回类型，通过 Kotlin 反射支持挂起函数。
		 */
		private static Type getGenericReturnType(Method method) {
			try {
				KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
				if (function != null && function.isSuspend()) {
					return ReflectJvmMapping.getJavaType(function.getReturnType());
				}
			}
			catch (UnsupportedOperationException ex) {
				// probably a synthetic class - let's use java reflection instead
			}
			return method.getGenericReturnType();
		}


		/**
		 * Return the return type of the method, with support of suspending
		 * functions via Kotlin reflection.
		 * <p>返回方法的返回类型，通过 Kotlin 反射支持挂起函数。
		 */
		private static Class<?> getReturnType(Method method) {
			try {
				KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
				if (function != null && function.isSuspend()) {
					Type paramType = ReflectJvmMapping.getJavaType(function.getReturnType());
					if (paramType == Unit.class) {
						paramType = void.class;
					}
					return ResolvableType.forType(paramType).resolve(method.getReturnType());
				}
			}
			catch (UnsupportedOperationException ex) {
				// probably a synthetic class - let's use java reflection instead
			}
			return method.getReturnType();
		}
	}

}
