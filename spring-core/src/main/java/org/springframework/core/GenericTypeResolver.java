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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Helper class for resolving generic types against type variables.
 * 针对类型变量解析泛型的帮助类。
 *
 * <p>Mainly intended for usage within the framework, resolving method
 * parameter types even when they are declared generically.
 * <p>主要用于框架内部使用，即使方法参数类型是以泛型方式声明的也要解析它们。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @author Phillip Webb
 * @author Yanming Zhou
 * @since 2.5.2
 */
public final class GenericTypeResolver {

	/** Cache from Class to TypeVariable Map. */
	@SuppressWarnings("rawtypes")
	private static final Map<Class<?>, Map<TypeVariable, Type>> typeVariableCache = new ConcurrentReferenceHashMap<>();


	private GenericTypeResolver() {
	}


	/**
	 * Determine the target type for the given generic parameter type.
	 * 确定给定泛型参数类型的目 标类型。
	 * @param methodParameter the method parameter specification
	 * @param implementationClass the class to resolve type variables against
	 * @param methodParameter 方法参数规范
	 * @param implementationClass 要针对其解析类型变量的类
	 * @return the corresponding generic parameter or return type
	 * @return 相应的泛型参数或返回类型
	 * @deprecated since 5.2 in favor of {@code methodParameter.withContainingClass(implementationClass).getParameterType()}
	 */
	@Deprecated
	public static Class<?> resolveParameterType(MethodParameter methodParameter, Class<?> implementationClass) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		Assert.notNull(implementationClass, "Class must not be null");
		methodParameter.setContainingClass(implementationClass);
		return methodParameter.getParameterType();
	}

	/**
	 * Determine the target type for the generic return type of the given method,
	 * where formal type variables are declared on the given class.
	 * 确定给定方法的泛型返回类型的目 标类型，
	 * 其中正式类型变量在给定类上声明。
	 * @param method the method to introspect
	 * @param clazz the class to resolve type variables against
	 * @param method 要内省的方法
	 * @param clazz 要针对其解析类型变量的类
	 * @return the corresponding generic parameter or return type
	 * @return 相应的泛型参数或返回类型
	 */
	public static Class<?> resolveReturnType(Method method, Class<?> clazz) {
		Assert.notNull(method, "Method must not be null");
		Assert.notNull(clazz, "Class must not be null");
		return ResolvableType.forMethodReturnType(method, clazz).resolve(method.getReturnType());
	}

	/**
	 * Resolve the single type argument of the given generic type against the given
	 * target method which is assumed to return the given type or an implementation
	 * of it.
	 * 针对给定目标方法解析给定泛型类型的单一类型参数，
	 * 该方法假定返回给定类型或其实现。
	 * @param method the target method to check the return type of
	 * @param genericType the generic interface or superclass to resolve the type argument from
	 * @param method 要检查返回类型的目 标方法
	 * @param genericType 要从中解析类型参数的泛型接口或超类
	 * @return the resolved parameter type of the method return type, or {@code null}
	 * if not resolvable or if the single argument is of type {@link WildcardType}.
	 * @return 方法返回类型的已解析参数类型，如果不可解析或单一参数为 {@link WildcardType} 类型则返回 {@code null}。
	 */
	@Nullable
	public static Class<?> resolveReturnTypeArgument(Method method, Class<?> genericType) {
		Assert.notNull(method, "Method must not be null");
		ResolvableType resolvableType = ResolvableType.forMethodReturnType(method).as(genericType);
		if (!resolvableType.hasGenerics() || resolvableType.getType() instanceof WildcardType) {
			return null;
		}
		return getSingleGeneric(resolvableType);
	}

	/**
	 * Resolve the single type argument of the given generic type against
	 * the given target class which is assumed to implement the given type
	 * and possibly declare a concrete type for its type variable.
	 * 针对给定目 标类解析给定泛型类型的单一类型参数，
	 * 该类假定实现了给定类型并可能为其类型变量声明具体类型。
	 * @param clazz the target class to check against
	 * @param genericType the generic interface or superclass to resolve the type argument from
	 * @param clazz 要针对其检查的目 标类
	 * @param genericType 要从中解析类型参数的泛型接口或超类
	 * @return the resolved type of the argument, or {@code null} if not resolvable
	 * @return 参数的已解析类型，如果不可解析则返回 {@code null}
	 */
	@Nullable
	public static Class<?> resolveTypeArgument(Class<?> clazz, Class<?> genericType) {
		ResolvableType resolvableType = ResolvableType.forClass(clazz).as(genericType);
		if (!resolvableType.hasGenerics()) {
			return null;
		}
		return getSingleGeneric(resolvableType);
	}

	@Nullable
	private static Class<?> getSingleGeneric(ResolvableType resolvableType) {
		Assert.isTrue(resolvableType.getGenerics().length == 1,
				() -> "Expected 1 type argument on generic interface [" + resolvableType +
				"] but found " + resolvableType.getGenerics().length);
		return resolvableType.getGeneric().resolve();
	}


	/**
	 * Resolve the type arguments of the given generic type against the given
	 * target class which is assumed to implement or extend from the given type
	 * and possibly declare concrete types for its type variables.
	 * 针对给定目 标类解析给定泛型类型的类型参数，
	 * 该类假定实现了给定类型或从给定类型继承，并可能为其类型变量声明具体类型。
	 * @param clazz the target class to check against
	 * @param genericType the generic interface or superclass to resolve the type argument from
	 * @param clazz 要针对其检查的目 标类
	 * @param genericType 要从中解析类型参数的泛型接口或超类
	 * @return the resolved type of each argument, with the array size matching the
	 * number of actual type arguments, or {@code null} if not resolvable
	 * @return 每个参数的已解析类型，数组大小与实际类型参数数量匹配，如果不可解析则返回 {@code null}
	 */
	@Nullable
	public static Class<?>[] resolveTypeArguments(Class<?> clazz, Class<?> genericType) {
		ResolvableType type = ResolvableType.forClass(clazz).as(genericType);
		if (!type.hasGenerics() || !type.hasResolvableGenerics()) {
			return null;
		}
		return type.resolveGenerics(Object.class);
	}

	/**
	 * Resolve the given generic type against the given context class,
	 * substituting type variables as far as possible.
	 * 针对给定上下文类解析给定泛型类型，
	 * 尽可能替换类型变量。
	 * <p>As of 6.2, this method resolves type variables recursively.
	 * <p>从6.2开始，此方法递归解析类型变量。
	 * @param genericType the (potentially) generic type
	 * @param contextClass a context class for the target type, for example a class
	 * in which the target type appears in a method signature (can be {@code null})
	 * @param genericType (可能的) 泛型类型
	 * @param contextClass 目 标类型的上下文类，例如目 标类型出现在方法签名中的类(可以为 {@code null})
	 * @return the resolved type (possibly the given generic type as-is)
	 * @return 已解析的类型(可能是给定的泛型类型本身)
	 * @since 5.0
	 */
	public static Type resolveType(Type genericType, @Nullable Class<?> contextClass) {
		if (contextClass != null) {
			if (genericType instanceof TypeVariable<?> typeVariable) {
				ResolvableType resolvedTypeVariable = resolveVariable(
						typeVariable, ResolvableType.forClass(contextClass));
				if (resolvedTypeVariable == ResolvableType.NONE) {
					resolvedTypeVariable = ResolvableType.forVariableBounds(typeVariable);
				}
				if (resolvedTypeVariable != ResolvableType.NONE) {
					Class<?> resolved = resolvedTypeVariable.resolve();
					if (resolved != null) {
						return resolved;
					}
				}
			}
			else if (genericType instanceof ParameterizedType parameterizedType) {
				ResolvableType resolvedType = ResolvableType.forType(genericType);
				if (resolvedType.hasUnresolvableGenerics()) {
					Type[] typeArguments = parameterizedType.getActualTypeArguments();
					ResolvableType[] generics = new ResolvableType[typeArguments.length];
					ResolvableType contextType = ResolvableType.forClass(contextClass);
					for (int i = 0; i < typeArguments.length; i++) {
						Type typeArgument = typeArguments[i];
						if (typeArgument instanceof TypeVariable<?> typeVariable) {
							ResolvableType resolvedTypeArgument = resolveVariable(typeVariable, contextType);
							if (resolvedTypeArgument == ResolvableType.NONE) {
								resolvedTypeArgument = ResolvableType.forVariableBounds(typeVariable);
							}
							if (resolvedTypeArgument != ResolvableType.NONE) {
								generics[i] = resolvedTypeArgument;
							}
							else {
								generics[i] = ResolvableType.forType(typeArgument);
							}
						}
						else if (typeArgument instanceof ParameterizedType) {
							generics[i] = ResolvableType.forType(resolveType(typeArgument, contextClass));
						}
						else {
							generics[i] = ResolvableType.forType(typeArgument);
						}
					}
					Class<?> rawClass = resolvedType.getRawClass();
					if (rawClass != null) {
						return ResolvableType.forClassWithGenerics(rawClass, generics).getType();
					}
				}
			}
		}
		return genericType;
	}

	private static ResolvableType resolveVariable(TypeVariable<?> typeVariable, ResolvableType contextType) {
		ResolvableType resolvedType;
		if (contextType.hasGenerics()) {
			ResolvableType.VariableResolver variableResolver = contextType.asVariableResolver();
			if (variableResolver == null) {
				return ResolvableType.NONE;
			}
			resolvedType = variableResolver.resolveVariable(typeVariable);
			if (resolvedType != null) {
				while (resolvedType.getType() instanceof TypeVariable<?>) {
					resolvedType = resolvedType.resolveType();
				}
				return resolvedType;
			}
		}

		ResolvableType superType = contextType.getSuperType();
		if (superType != ResolvableType.NONE) {
			resolvedType = resolveVariable(typeVariable, superType);
			if (resolvedType != ResolvableType.NONE) {
				return resolvedType;
			}
		}
		for (ResolvableType ifc : contextType.getInterfaces()) {
			resolvedType = resolveVariable(typeVariable, ifc);
			if (resolvedType != ResolvableType.NONE) {
				return resolvedType;
			}
		}
		return ResolvableType.NONE;
	}

	/**
	 * Resolve the specified generic type against the given TypeVariable map.
	 * <p>Used by Spring Data.
	 * @param genericType the generic type to resolve
	 * @param map the TypeVariable Map to resolved against
	 * @return the type if it resolves to a Class, or {@code Object.class} otherwise
	 */
	@SuppressWarnings("rawtypes")
	public static Class<?> resolveType(Type genericType, Map<TypeVariable, Type> map) {
		return ResolvableType.forType(genericType, new TypeVariableMapVariableResolver(map)).toClass();
	}

	/**
	 * Build a mapping of {@link TypeVariable#getName TypeVariable names} to
	 * {@link Class concrete classes} for the specified {@link Class}.
	 * Searches all supertypes, enclosing types and interfaces.
	 * @see #resolveType(Type, Map)
	 */
	@SuppressWarnings("rawtypes")
	public static Map<TypeVariable, Type> getTypeVariableMap(Class<?> clazz) {
		Map<TypeVariable, Type> typeVariableMap = typeVariableCache.get(clazz);
		if (typeVariableMap == null) {
			typeVariableMap = new HashMap<>();
			buildTypeVariableMap(ResolvableType.forClass(clazz), typeVariableMap);
			typeVariableCache.put(clazz, Collections.unmodifiableMap(typeVariableMap));
		}
		return typeVariableMap;
	}

	@SuppressWarnings("rawtypes")
	private static void buildTypeVariableMap(ResolvableType type, Map<TypeVariable, Type> typeVariableMap) {
		if (type != ResolvableType.NONE) {
			Class<?> resolved = type.resolve();
			if (resolved != null && type.getType() instanceof ParameterizedType) {
				TypeVariable<?>[] variables = resolved.getTypeParameters();
				for (int i = 0; i < variables.length; i++) {
					ResolvableType generic = type.getGeneric(i);
					while (generic.getType() instanceof TypeVariable<?>) {
						generic = generic.resolveType();
					}
					if (generic != ResolvableType.NONE) {
						typeVariableMap.put(variables[i], generic.getType());
					}
				}
			}
			buildTypeVariableMap(type.getSuperType(), typeVariableMap);
			for (ResolvableType interfaceType : type.getInterfaces()) {
				buildTypeVariableMap(interfaceType, typeVariableMap);
			}
			if (resolved != null && resolved.isMemberClass()) {
				buildTypeVariableMap(ResolvableType.forClass(resolved.getEnclosingClass()), typeVariableMap);
			}
		}
	}


	@SuppressWarnings({"serial", "rawtypes"})
	private static class TypeVariableMapVariableResolver implements ResolvableType.VariableResolver {

		private final Map<TypeVariable, Type> typeVariableMap;

		public TypeVariableMapVariableResolver(Map<TypeVariable, Type> typeVariableMap) {
			this.typeVariableMap = typeVariableMap;
		}

		@Override
		@Nullable
		public ResolvableType resolveVariable(TypeVariable<?> variable) {
			Type type = this.typeVariableMap.get(variable);
			return (type != null ? ResolvableType.forType(type) : null);
		}

		@Override
		public Object getSource() {
			return this.typeVariableMap;
		}
	}

}
