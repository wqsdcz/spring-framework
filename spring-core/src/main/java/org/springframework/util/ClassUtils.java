/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.util;

import java.beans.Introspector;
import java.io.Closeable;
import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.lang.Nullable;

/**
 * 单词本：
 * primitive 原始的、原生的、基本的、简陋的、本能的、初始的、自然的
 * primitive type 原始类型
 * present 存在的、在场的、现在时的
 * Visible 可见的
 * Necessary 必要的、不可避免的
 * resolve 解决、转化、分解、消散
 * Assignable 可分配的、可转让的、可赋值的、可指定的
 * Composite 混合的，复合的、综合的
 * Qualified 有限制的，胜任的，有资格的
 * Available 有效的，可用的
 * Specific 明确的，具体的，特定的，
 *
 * 各种{@code java.lang.Class}工具方法。
 * 主要用于框架内部使用。
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 1.1
 * @see TypeUtils
 * @see ReflectionUtils
 */
public abstract class ClassUtils {

	/** 数组类名的后缀："[]" */
	public static final String ARRAY_SUFFIX = "[]";

	/** 内部的数组类名的前缀："[" */
	private static final String INTERNAL_ARRAY_PREFIX = "[";

	/** 内部的非基本类型数组类名的前缀："[L" */
	private static final String NON_PRIMITIVE_ARRAY_PREFIX = "[L";

	/** 包分隔符：'.' */
	private static final char PACKAGE_SEPARATOR = '.';

	/** 路径分隔符：'/' */
	private static final char PATH_SEPARATOR = '/';

	/** 内部类分隔符：'$' */
	private static final char INNER_CLASS_SEPARATOR = '$';

	/** CGLIB 类分隔符："$$" */
	public static final String CGLIB_CLASS_SEPARATOR = "$$";

	/** ".class" 文件后缀 */
	public static final String CLASS_FILE_SUFFIX = ".class";


	/**
	 * 以【基本类型的包装类】为键，【对应基本类型】为值的映射，
	 * 例如：Integer.class -> int.class。
	 */
	private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<>(8);

	/**
	 * 以【基本类型】为键，【对应包装类】为值的映射，
	 * 例如：int.class -> Integer.class。
	 */
	private static final Map<Class<?>, Class<?>> primitiveTypeToWrapperMap = new IdentityHashMap<>(8);

	/**
	 * 以【基本类型名称】为键，【对应基本类型】为值的映射，
	 * 例如："int" -> "int.class"。
	 */
	private static final Map<String, Class<?>> primitiveTypeNameMap = new HashMap<>(32);

	/**
	 * 以【常见Java语言类名】为键，【对应Class】为值的映射。
	 * 主要用于远程调用的高效反序列化。
	 */
	private static final Map<String, Class<?>> commonClassCache = new HashMap<>(64);

	/**
	 * 常见的Java语言接口，在搜索'主要（primary）'用户级接口时应被忽略。
	 */
	private static final Set<Class<?>> javaLanguageInterfaces;


	static {
		primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
		primitiveWrapperTypeMap.put(Byte.class, byte.class);
		primitiveWrapperTypeMap.put(Character.class, char.class);
		primitiveWrapperTypeMap.put(Double.class, double.class);
		primitiveWrapperTypeMap.put(Float.class, float.class);
		primitiveWrapperTypeMap.put(Integer.class, int.class);
		primitiveWrapperTypeMap.put(Long.class, long.class);
		primitiveWrapperTypeMap.put(Short.class, short.class);

		// Map entry iteration is less expensive to initialize than forEach with lambdas
		for (Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperTypeMap.entrySet()) {
			primitiveTypeToWrapperMap.put(entry.getValue(), entry.getKey());
			registerCommonClasses(entry.getKey());
		}

		Set<Class<?>> primitiveTypes = new HashSet<>(32);
		primitiveTypes.addAll(primitiveWrapperTypeMap.values());
		Collections.addAll(primitiveTypes, boolean[].class, byte[].class, char[].class,
				double[].class, float[].class, int[].class, long[].class, short[].class);
		primitiveTypes.add(void.class);
		for (Class<?> primitiveType : primitiveTypes) {
			primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
		}

		registerCommonClasses(Boolean[].class, Byte[].class, Character[].class, Double[].class,
				Float[].class, Integer[].class, Long[].class, Short[].class);
		registerCommonClasses(Number.class, Number[].class, String.class, String[].class,
				Class.class, Class[].class, Object.class, Object[].class);
		registerCommonClasses(Throwable.class, Exception.class, RuntimeException.class,
				Error.class, StackTraceElement.class, StackTraceElement[].class);
		registerCommonClasses(Enum.class, Iterable.class, Iterator.class, Enumeration.class,
				Collection.class, List.class, Set.class, Map.class, Map.Entry.class, Optional.class);

		Class<?>[] javaLanguageInterfaceArray = {Serializable.class, Externalizable.class,
				Closeable.class, AutoCloseable.class, Cloneable.class, Comparable.class};
		registerCommonClasses(javaLanguageInterfaceArray);
		javaLanguageInterfaces = new HashSet<>(Arrays.asList(javaLanguageInterfaceArray));
	}


	/**
	 * 将给定的常见类注册到ClassUtils缓存中。
	 */
	private static void registerCommonClasses(Class<?>... commonClasses) {
		for (Class<?> clazz : commonClasses) {
			commonClassCache.put(clazz.getName(), clazz);
		}
	}

	/**
	 * 返回要使用的默认ClassLoader：通常是线程上下文ClassLoader（如果可用）；
	 * 将使用加载ClassUtils类的ClassLoader作为后备。
	 * <p>
	 *     在明确希望使用非空ClassLoader引用的场景中调用此方法，
	 *     例如：用于类路径资源加载（但不一定用于{@code Class.forName}，
	 *     它也接受{@code null} ClassLoader引用）。
	 * @return 默认ClassLoader（即使系统ClassLoader不可访问时也只会返回{@code null}）
	 * @see Thread#getContextClassLoader()
	 * @see ClassLoader#getSystemClassLoader()
	 */
	@Nullable
	public static ClassLoader getDefaultClassLoader() {
		ClassLoader cl = null;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		}
		catch (Throwable ex) {
			// 无法访问线程上下文ClassLoader - 回退到...
		}
		if (cl == null) {
            // 没有线程上下文类加载器 -> 使用此类的类加载器
			cl = ClassUtils.class.getClassLoader();
			if (cl == null) {
                // getClassLoader()返回null表示启动类加载器
				try {
					cl = ClassLoader.getSystemClassLoader();
				}
				catch (Throwable ex) {
					// 无法访问系统类加载器 - 好吧，也许调用者可以接受null...
				}
			}
		}
		return cl;
	}

	/**
	 * 如有必要，使用环境中的bean类加载器覆盖线程上下文类加载器，即当bean类加载器与当前线程上下文类加载器不一致时。
	 * @param classLoaderToUse 用于线程上下文实际使用的类加载器
	 * @return 原始线程上下文类加载器，如果未被覆盖则返回{@code null}
	 */
	@Nullable
	public static ClassLoader overrideThreadContextClassLoader(@Nullable ClassLoader classLoaderToUse) {
		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
		if (classLoaderToUse != null && !classLoaderToUse.equals(threadContextClassLoader)) {
			currentThread.setContextClassLoader(classLoaderToUse);
			return threadContextClassLoader;
		}
		else {
			return null;
		}
	}

	/**
	 * 替代{@code Class.forName()}的方法，同时支持基本类型（如"int"）和数组类名（如"String[]")返回Class实例。
	 * 此外，还能够解析Java源码风格的内部类名（例如"java.lang.Thread.State"而非"java.lang.Thread$State"）。
	 * @param name 类名称
	 * @param classLoader 使用的类加载器（可为{@code null}，表示使用默认类加载器）
	 * @return 对应名称的类实例
	 * @throws ClassNotFoundException 如果类未找到
	 * @throws LinkageError 如果类文件无法加载
	 * @see Class#forName(String, boolean, ClassLoader)
	 */
	public static Class<?> forName(String name, @Nullable ClassLoader classLoader)
			throws ClassNotFoundException, LinkageError {

		Assert.notNull(name, "Name must not be null");

		Class<?> clazz = resolvePrimitiveClassName(name);
		if (clazz == null) {
			clazz = commonClassCache.get(name);
		}
		if (clazz != null) {
			return clazz;
		}

		// "java.lang.String[]" style arrays
		if (name.endsWith(ARRAY_SUFFIX)) {
			String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
			Class<?> elementClass = forName(elementClassName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		// "[Ljava.lang.String;" style arrays
		if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
			String elementName = name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
			Class<?> elementClass = forName(elementName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		// "[[I" or "[[Ljava.lang.String;" style arrays
		if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
			String elementName = name.substring(INTERNAL_ARRAY_PREFIX.length());
			Class<?> elementClass = forName(elementName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		ClassLoader clToUse = classLoader;
		if (clToUse == null) {
			clToUse = getDefaultClassLoader();
		}
		try {
			return (clToUse != null ? clToUse.loadClass(name) : Class.forName(name));
		}
		catch (ClassNotFoundException ex) {
			int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
			if (lastDotIndex != -1) {
				String innerClassName =
						name.substring(0, lastDotIndex) + INNER_CLASS_SEPARATOR + name.substring(lastDotIndex + 1);
				try {
					return (clToUse != null ? clToUse.loadClass(innerClassName) : Class.forName(innerClassName));
				}
				catch (ClassNotFoundException ex2) {
					// Swallow - let original exception get through
				}
			}
			throw ex;
		}
	}

	/**
	 * 将给定的类名解析为Class实例。支持基本类型（如"int"）和数组类名（如"String[]"）。
	 * <p>这实际上等同于具有相同参数的{@code forName}方法，唯一区别在于类加载失败时抛出的异常不同。
	 * @param className 类名称
	 * @param classLoader 使用的类加载器（可为{@code null}，表示使用默认类加载器）
	 * @return 对应名称的类实例
	 * @throws IllegalArgumentException 如果类名无法解析（即找不到类或无法加载类文件）
	 * @see #forName(String, ClassLoader)
	 */
	public static Class<?> resolveClassName(String className, @Nullable ClassLoader classLoader)
			throws IllegalArgumentException {

		try {
			return forName(className, classLoader);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("Could not find class [" + className + "]", ex);
		}
		catch (LinkageError err) {
			throw new IllegalArgumentException("Unresolvable class definition for class [" + className + "]", err);
		}
	}

	/**
	 * 判断指定名称的{@link Class}是否存在并可被加载。
	 * 如果该类或其依赖项不存在或无法加载，将返回{@code false}。
	 * @param className 要检查的类名称
	 * @param classLoader 使用的类加载器（可为{@code null}，表示使用默认类加载器）
	 * @return 指定的类是否存在
	 */
	public static boolean isPresent(String className, @Nullable ClassLoader classLoader) {
		try {
			forName(className, classLoader);
			return true;
		}
		catch (Throwable ex) {
			// Class or one of its dependencies is not present...
			return false;
		}
	}

	/**
	 * 检查给定类在指定ClassLoader中是否可见。
	 * @param clazz 要检查的类（通常是一个接口）
	 * @param classLoader 要检查的ClassLoader（可能为{@code null}，这种情况下该方法将始终返回{@code true}）
	 */
	public static boolean isVisible(Class<?> clazz, @Nullable ClassLoader classLoader) {
		if (classLoader == null) {
			return true;
		}
		try {
			if (clazz.getClassLoader() == classLoader) {
				return true;
			}
		}
		catch (SecurityException ex) {
			// 继续执行下面的可加载检查
		}

        // 如果可以从给定ClassLoader加载相同的Class，则可见
		return isLoadable(clazz, classLoader);
	}

	/**
	 * 检查给定类在指定上下文中是否是缓存安全的，即是否由给定ClassLoader或其父ClassLoader加载。
	 * @param clazz 要分析的类
	 * @param classLoader 可能缓存元数据的ClassLoader（可能为{@code null}，表示系统类加载器）
	 */
	public static boolean isCacheSafe(Class<?> clazz, @Nullable ClassLoader classLoader) {
		Assert.notNull(clazz, "Class must not be null");
		try {
			ClassLoader target = clazz.getClassLoader();
			// Common cases
			if (target == classLoader || target == null) {
				return true;
			}
			if (classLoader == null) {
				return false;
			}
			// Check for match in ancestors -> positive
			ClassLoader current = classLoader;
			while (current != null) {
				current = current.getParent();
				if (current == target) {
					return true;
				}
			}
			// Check for match in children -> negative
			while (target != null) {
				target = target.getParent();
				if (target == classLoader) {
					return false;
				}
			}
		}
		catch (SecurityException ex) {
			// Fall through to loadable check below
		}

		// Fallback for ClassLoaders without parent/child relationship:
		// safe if same Class can be loaded from given ClassLoader
		return (classLoader != null && isLoadable(clazz, classLoader));
	}

	/**
	 * 检查给定类在指定ClassLoader中是否可加载。
	 * @param clazz 要检查的类（通常是一个接口）
	 * @param classLoader 要检查的ClassLoader
	 * @since 5.0.6
	 */
	private static boolean isLoadable(Class<?> clazz, ClassLoader classLoader) {
		try {
			return (clazz == classLoader.loadClass(clazz.getName()));
			// Else: different class with same name found
		}
		catch (ClassNotFoundException ex) {
			// No corresponding class found at all
			return false;
		}
	}

	/**
	 * 根据JVM对原始类的命名规则，将给定的类名解析为原始类（如果适用）。
	 * <p>同时支持JVM内部对原始数组类的命名。
	 * <i>不</i>支持原始数组的"[]"后缀表示法；该表示法仅由 {@link #forName(String, ClassLoader)} 支持。
	 * @param name 可能为原始类的名称
	 * @return 原始类，如果名称不表示原始类或原始数组类则返回 {@code null}
	 */
	@Nullable
	public static Class<?> resolvePrimitiveClassName(@Nullable String name) {
		Class<?> result = null;
		// Most class names will be quite long, considering that they
		// SHOULD sit in a package, so a length check is worthwhile.
		if (name != null && name.length() <= 8) {
			// Could be a primitive - likely.
			result = primitiveTypeNameMap.get(name);
		}
		return result;
	}


	/**
	 * 检查给定的类是否表示原始包装类，
	 * 即 Boolean、Byte、Character、Short、Integer、Long、Float 或 Double。
	 * @param clazz 要检查的类
	 * @return 给定的类是否为原始包装类
	 */
	public static boolean isPrimitiveWrapper(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return primitiveWrapperTypeMap.containsKey(clazz);
	}

	/**
	 * 检查给定的类是否表示原始类型（即 boolean、byte、char、short、int、long、float 或 double）
	 * 或原始包装类（即 Boolean、Byte、Character、Short、Integer、Long、Float 或 Double）。
	 * @param clazz 要检查的类
	 * @return 给定的类是否为原始类型或原始包装类
	 */
	public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
	}

	/**
	 * 检查给定的类是否表示原始类型的数组，即 boolean、byte、char、short、int、long、float 或 double 的数组。
	 * @param clazz 要检查的类
	 * @return 给定的类是否为原始类型数组类
	 */
	public static boolean isPrimitiveArray(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isArray() && clazz.getComponentType().isPrimitive());
	}

	/**
	 * 检查给定的类是否表示原始包装类型的数组，即 Boolean、Byte、Character、Short、Integer、Long、Float 或 Double 的数组。
	 * @param clazz 要检查的类
	 * @return 给定的类是否为原始包装类型数组类
	 */
	public static boolean isPrimitiveWrapperArray(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isArray() && isPrimitiveWrapper(clazz.getComponentType()));
	}

	/**
	 * 如果给定的类是原始类型，则解析并返回对应的原始包装类型。
	 * @param clazz 要检查的类
	 * @return 原始类本身，或原始类型对应的原始包装类型
	 */
	public static Class<?> resolvePrimitiveIfNecessary(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isPrimitive() && clazz != void.class ? primitiveTypeToWrapperMap.get(clazz) : clazz);
	}

	/**
	 * 检查右侧类型是否可赋值给左侧类型（基于反射赋值场景考虑）。
	 * 考虑原始类型包装类可赋值给对应的原始类型。
	 * @param lhsType 目标类型
	 * @param rhsType 需要赋值给目标类型的值类型
	 * @return 如果目标类型可从值类型赋值则返回true
	 * @see TypeUtils#isAssignable(java.lang.reflect.Type, java.lang.reflect.Type)
	 */
	public static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
		Assert.notNull(lhsType, "Left-hand side type must not be null");
		Assert.notNull(rhsType, "Right-hand side type must not be null");
		if (lhsType.isAssignableFrom(rhsType)) {
			return true;
		}
		if (lhsType.isPrimitive()) {
			Class<?> resolvedPrimitive = primitiveWrapperTypeMap.get(rhsType);
			return (lhsType == resolvedPrimitive);
		}
		else {
			Class<?> resolvedWrapper = primitiveTypeToWrapperMap.get(rhsType);
			return (resolvedWrapper != null && lhsType.isAssignableFrom(resolvedWrapper));
		}
	}

	/**
	 * 判断给定值否可以赋值给给定类型的变量（基于反射赋值场景考虑）。
	 * 考虑原始类型包装类可赋值给对应的原始类型。
	 * @param type 目标类型
	 * @param value 需要赋值给目标类型的值
	 * @return 如果类型可从值赋值则返回true
	 */
	public static boolean isAssignableValue(Class<?> type, @Nullable Object value) {
		Assert.notNull(type, "Type must not be null");
		return (value != null ? isAssignable(type, value.getClass()) : !type.isPrimitive());
	}

	/**
	 * 将基于斜杠"/"的资源路径转换为基于点"."的完全限定类名。
	 * @param resourcePath 指向类的资源路径
	 * @return 对应的完全限定类名
	 */
	public static String convertResourcePathToClassName(String resourcePath) {
		Assert.notNull(resourcePath, "Resource path must not be null");
		return resourcePath.replace(PATH_SEPARATOR, PACKAGE_SEPARATOR);
	}

	/**
	 * 将基于点"."的完全限定类名转换为基于斜杠"/"的资源路径。
	 * @param className 完全限定类名
	 * @return 对应的资源路径，指向类文件
	 */
	public static String convertClassNameToResourcePath(String className) {
		Assert.notNull(className, "Class name must not be null");
		return className.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
	}

	/**
	 * 返回适用于 {@code ClassLoader.getResource} 方法的路径（通过在返回值前添加斜杠'/'也可适用于 {@code Class.getResource} 方法）。
	 * 构建方式：获取指定类文件所在的包路径，将所有点('.')转换为斜杠('/')，必要时添加尾部斜杠，并将指定的资源名称拼接至此路径。
	 * <br/>因此，本方法可用于构建与类文件同包下的资源文件加载路径，尽管通常使用 {@link org.springframework.core.io.ClassPathResource} 更为便捷。
	 * @param clazz 将使用其包路径作为基础路径的Class对象
	 * @param resourceName 要追加的资源名称。开头的斜杠是可选的。
	 * @return 构建完成的资源路径
	 * @see ClassLoader#getResource
	 * @see Class#getResource
	 */
	public static String addResourcePathToPackagePath(Class<?> clazz, String resourceName) {
		Assert.notNull(resourceName, "Resource name must not be null");
		if (!resourceName.startsWith("/")) {
			return classPackageAsResourcePath(clazz) + '/' + resourceName;
		}
		return classPackageAsResourcePath(clazz) + resourceName;
	}

	/**
	 * 给定输入类对象，返回由类的包名组成的路径名字符串，即所有点('.')替换为斜杠('/')。
	 * 不添加前导或尾随斜杠。结果可以与斜杠和资源名称连接后直接用于{@code ClassLoader.getResource()}。
	 * 如果要用于{@code Class.getResource}，则需要在返回值前添加前导斜杠。
	 * @param clazz 输入类。{@code null}值或默认（空）包将导致返回空字符串("")。
	 * @return 表示包名的路径
	 * @see ClassLoader#getResource
	 * @see Class#getResource
	 */
	public static String classPackageAsResourcePath(@Nullable Class<?> clazz) {
		if (clazz == null) {
			return "";
		}
		String className = clazz.getName();
		int packageEndIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		if (packageEndIndex == -1) {
			return "";
		}
		String packageName = className.substring(0, packageEndIndex);
		return packageName.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
	}

	/**
	 * 构建一个由给定数组中类/接口名称组成的字符串。
	 * <p>基本类似于{@code AbstractCollection.toString()}，但会在每个类名之前去除"class "/"interface "前缀。
	 * @param classes Class对象数组
	 * @return 形式为"[com.foo.Bar, com.foo.Baz]"的字符串
	 * @see java.util.AbstractCollection#toString()
	 */
	public static String classNamesToString(Class<?>... classes) {
		return classNamesToString(Arrays.asList(classes));
	}

	/**
	 * 构建一个由给定集合中类/接口名称组成的字符串。
	 * <p>基本类似于{@code AbstractCollection.toString()}，但会在每个类名之前去除"class "/"interface "前缀。
	 * @param classes Class对象的集合（可能为{@code null}）
	 * @return 形式为"[com.foo.Bar, com.foo.Baz]"的字符串
	 * @see java.util.AbstractCollection#toString()
	 */
	public static String classNamesToString(@Nullable Collection<Class<?>> classes) {
		if (CollectionUtils.isEmpty(classes)) {
			return "[]";
		}
		StringBuilder sb = new StringBuilder("[");
		for (Iterator<Class<?>> it = classes.iterator(); it.hasNext(); ) {
			Class<?> clazz = it.next();
			sb.append(clazz.getName());
			if (it.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * 将给定的{@code Collection}复制到{@code Class}数组中。
	 * <p>{@code Collection}必须仅包含{@code Class}元素。
	 *
	 * @param collection 要复制的{@code Collection}
	 * @return {@code Class}数组
	 * @since 3.1
	 * @see StringUtils#toStringArray
	 */
	public static Class<?>[] toClassArray(Collection<Class<?>> collection) {
		return collection.toArray(new Class<?>[0]);
	}

	/**
	 * 返回给定实例实现的所有接口（包括超类实现的接口）作为数组。
	 * @param instance 要分析接口的实例
	 * @return 给定实例实现的所有接口的数组
	 */
	public static Class<?>[] getAllInterfaces(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getAllInterfacesForClass(instance.getClass());
	}

	/**
	 * 返回给定类实现的所有接口（包括超类实现的接口）作为数组。
	 * <p>如果类本身是接口，则将其作为唯一接口返回。
	 * @param clazz 要分析接口的类
	 * @return 给定对象实现的所有接口的数组
	 */
	public static Class<?>[] getAllInterfacesForClass(Class<?> clazz) {
		return getAllInterfacesForClass(clazz, null);
	}

	/**
	 * 返回给定类实现的所有接口（包括超类实现的接口）作为数组。
	 * <p>如果类本身是接口，则将其作为唯一接口返回。
	 * @param clazz 要分析接口的类
	 * @param classLoader 接口需要在其内可见的ClassLoader（可为{@code null}，表示接受所有声明的接口）
	 * @return 给定对象实现的所有接口的数组
	 */
	public static Class<?>[] getAllInterfacesForClass(Class<?> clazz, @Nullable ClassLoader classLoader) {
		return toClassArray(getAllInterfacesForClassAsSet(clazz, classLoader));
	}

	/**
	 * 返回给定实例实现的所有接口（包括超类实现的接口）作为Set集合。
	 * @param instance 要分析接口的实例
	 * @return 给定实例实现的所有接口的Set集合
	 */
	public static Set<Class<?>> getAllInterfacesAsSet(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getAllInterfacesForClassAsSet(instance.getClass());
	}

	/**
	 * 返回给定类实现的所有接口（包括超类实现的接口）作为Set集合。
	 * <p>如果类本身是接口，则将其作为唯一接口返回。
	 * @param clazz 要分析接口的类
	 * @return 给定对象实现的所有接口的Set集合
	 */
	public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz) {
		return getAllInterfacesForClassAsSet(clazz, null);
	}

	/**
	 * 返回给定类实现的所有接口（包括超类实现的接口）作为Set集合。
	 * <p>如果类本身是接口，则将其作为唯一接口返回。
	 * @param clazz 要分析接口的类
	 * @param classLoader 接口需要在其内可见的ClassLoader
	 *                  （可为{@code null}，表示接受所有声明的接口）
	 * @return 给定对象实现的所有接口的Set集合
	 */
		public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz, @Nullable ClassLoader classLoader) {
		Assert.notNull(clazz, "Class must not be null");
		if (clazz.isInterface() && isVisible(clazz, classLoader)) {
			return Collections.singleton(clazz);
		}
		Set<Class<?>> interfaces = new LinkedHashSet<>();
		Class<?> current = clazz;
		while (current != null) {
			Class<?>[] ifcs = current.getInterfaces();
			for (Class<?> ifc : ifcs) {
				if (isVisible(ifc, classLoader)) {
					interfaces.add(ifc);
				}
			}
			current = current.getSuperclass();
		}
		return interfaces;
	}

	/**
	 * 为给定接口集创建复合接口类，在单个类中实现所有给定接口。
	 * <p>此实现为给定接口构建JDK代理类。
	 * @param interfaces 要合并的接口
	 * @param classLoader 在其中创建复合类的ClassLoader
	 * @return 合并后的接口Class
	 * @throws IllegalArgumentException 如果指定的接口存在冲突的方法签名（或违反类似约束）
	 * @see java.lang.reflect.Proxy#getProxyClass
	 */
	@SuppressWarnings("deprecation")  // on JDK 9
	public static Class<?> createCompositeInterface(Class<?>[] interfaces, @Nullable ClassLoader classLoader) {
		Assert.notEmpty(interfaces, "Interface array must not be empty");
		return Proxy.getProxyClass(classLoader, interfaces);
	}

	/**
	 * 确定给定类的公共祖先（如果存在）。
	 * @param clazz1 要内省的类
	 * @param clazz2 要内省的另一个类
	 * @return 公共祖先（即公共超类，或一个接口扩展另一个接口），如果未找到则返回{@code null}。
	 *         如果任意给定的类为{@code null}，则返回另一个类。
	 * @since 3.2.6
	 */
	@Nullable
	public static Class<?> determineCommonAncestor(@Nullable Class<?> clazz1, @Nullable Class<?> clazz2) {
		if (clazz1 == null) {
			return clazz2;
		}
		if (clazz2 == null) {
			return clazz1;
		}
		if (clazz1.isAssignableFrom(clazz2)) {
			return clazz1;
		}
		if (clazz2.isAssignableFrom(clazz1)) {
			return clazz2;
		}
		Class<?> ancestor = clazz1;
		do {
			ancestor = ancestor.getSuperclass();
			if (ancestor == null || Object.class == ancestor) {
				return null;
			}
		}
		while (!ancestor.isAssignableFrom(clazz2));
		return ancestor;
	}

	/**
	 * 判断给定的接口是否为常见的Java语言接口：
	 * {@link Serializable}, {@link Externalizable}, {@link Closeable}, {@link AutoCloseable},
	 * {@link Cloneable}, {@link Comparable} - 在寻找'主要'用户级接口时都可以忽略这些接口。
	 * 共同特征：无服务级操作，无bean属性方法，无默认方法。
	 * @param ifc 要检查的接口
	 * @since 5.0.3
	 */
	public static boolean isJavaLanguageInterface(Class<?> ifc) {
		return javaLanguageInterfaces.contains(ifc);
	}

	/**
	 * 判断提供的类是否为<em>内部类</em>，即外部类的非静态成员类。
	 * @return 如果提供的类是内部类则返回{@code true}
	 * @since 5.0.5
	 * @see Class#isMemberClass()
	 */
	public static boolean isInnerClass(Class<?> clazz) {
		return (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers()));
	}

	/**
	 * 检查给定对象是否为CGLIB代理。
	 * @param object 要检查的对象
	 * @see #isCglibProxyClass(Class)
	 * @see org.springframework.aop.support.AopUtils#isCglibProxy(Object)
	 */
	public static boolean isCglibProxy(Object object) {
		return isCglibProxyClass(object.getClass());
	}

	/**
	 * 检查指定的类是否为CGLIB生成的类。
	 * @param clazz 要检查的类
	 * @see #isCglibProxyClassName(String)
	 */
	public static boolean isCglibProxyClass(@Nullable Class<?> clazz) {
		return (clazz != null && isCglibProxyClassName(clazz.getName()));
	}

	/**
	 * 检查指定的类名是否为CGLIB生成的类。
	 * @param className 要检查的类名
	 */
	public static boolean isCglibProxyClassName(@Nullable String className) {
		return (className != null && className.contains(CGLIB_CLASS_SEPARATOR));
	}

	/**
	 * 返回给定实例的用户定义类：通常就是实例的类，但对于CGLIB生成的子类则返回原始类。
	 * @param instance 要检查的实例
	 * @return 用户定义的类
	 */
	public static Class<?> getUserClass(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getUserClass(instance.getClass());
	}

	/**
	 * 返回给定类的用户定义类：
	 * 通常就是给定的类，但对于CGLIB生成的子类则返回原始类。
	 * @param clazz 要检查的类
	 * @return 用户定义的类
	 */
	public static Class<?> getUserClass(Class<?> clazz) {
		if (clazz.getName().contains(CGLIB_CLASS_SEPARATOR)) {
			Class<?> superclass = clazz.getSuperclass();
			if (superclass != null && superclass != Object.class) {
				return superclass;
			}
		}
		return clazz;
	}

	/**
	 * 返回给定对象类型的描述性名称：通常就是类名，但对于数组则为组件类型类名 + "[]"，对于JDK代理则附加实现的接口列表。
	 * @param value 要内省的对象值
	 * @return 类的限定名
	 */
	@Nullable
	public static String getDescriptiveType(@Nullable Object value) {
		if (value == null) {
			return null;
		}
		Class<?> clazz = value.getClass();
		if (Proxy.isProxyClass(clazz)) {
			StringBuilder result = new StringBuilder(clazz.getName());
			result.append(" implementing ");
			Class<?>[] ifcs = clazz.getInterfaces();
			for (int i = 0; i < ifcs.length; i++) {
				result.append(ifcs[i].getName());
				if (i < ifcs.length - 1) {
					result.append(',');
				}
			}
			return result.toString();
		}
		else {
			return clazz.getTypeName();
		}
	}

	/**
	 * 检查给定类是否与用户指定的类型名称匹配。
	 * @param clazz 要检查的类
	 * @param typeName 要匹配的类型名称
	 */
	public static boolean matchesTypeName(Class<?> clazz, @Nullable String typeName) {
		return (typeName != null &&
				(typeName.equals(clazz.getTypeName()) || typeName.equals(clazz.getSimpleName())));
	}

	/**
	 * 获取不包含包名的类名。
	 * @param className 要获取短名称的类名
	 * @return 不包含包名的类名
	 * @throws IllegalArgumentException 如果类名为空
	 */
	public static String getShortName(String className) {
		Assert.hasLength(className, "Class name must not be empty");
		int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		int nameEndIndex = className.indexOf(CGLIB_CLASS_SEPARATOR);
		if (nameEndIndex == -1) {
			nameEndIndex = className.length();
		}
		String shortName = className.substring(lastDotIndex + 1, nameEndIndex);
		shortName = shortName.replace(INNER_CLASS_SEPARATOR, PACKAGE_SEPARATOR);
		return shortName;
	}

	/**
	 * 获取不包含包名的类名。
	 * @param clazz 要获取短名称的类
	 * @return 不包含包名的类名
	 */
	public static String getShortName(Class<?> clazz) {
		return getShortName(getQualifiedName(clazz));
	}

	/**
	 * 返回Java类的短字符串名称，以非首字母大写的JavaBeans属性格式。
	 * 如果是内部类，则去除外部类名称。
	 * @param clazz 类对象
	 * @return 以标准JavaBeans属性格式呈现的短名称
	 * @see java.beans.Introspector#decapitalize(String)
	 */
	public static String getShortNameAsProperty(Class<?> clazz) {
		String shortName = getShortName(clazz);
		int dotIndex = shortName.lastIndexOf(PACKAGE_SEPARATOR);
		shortName = (dotIndex != -1 ? shortName.substring(dotIndex + 1) : shortName);
		return Introspector.decapitalize(shortName);
	}

	/**
	 * 确定类文件名（相对于所在包路径）：例如："String.class"
	 * @param clazz 类对象
	 * @return ".class" 文件的文件名
	 */
	public static String getClassFileName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		String className = clazz.getName();
		int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		return className.substring(lastDotIndex + 1) + CLASS_FILE_SUFFIX;
	}

	/**
	 * 确定给定类的包名，例如：{@code java.lang.String} 类的包名为 "java.lang"。
	 * @param clazz 类对象
	 * @return 包名，如果类定义在默认包中则返回空字符串
	 */
	public static String getPackageName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return getPackageName(clazz.getName());
	}

	/**
	 * 确定给定完全限定类名的包名，例如：{@code java.lang.String} 类名的包名为 "java.lang"。
	 * @param fqClassName 完全限定的类名
	 * @return 包名，如果类定义在默认包中则返回空字符串
	 */
	public static String getPackageName(String fqClassName) {
		Assert.notNull(fqClassName, "Class name must not be null");
		int lastDotIndex = fqClassName.lastIndexOf(PACKAGE_SEPARATOR);
		return (lastDotIndex != -1 ? fqClassName.substring(0, lastDotIndex) : "");
	}

	/**
	 * 返回给定类的限定名：通常就是类名，但对于数组则为组件类型类名 + "[]"。
	 * @param clazz 类对象
	 * @return 类的限定名
	 */
	public static String getQualifiedName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return clazz.getTypeName();
	}

	/**
	 * 返回给定方法的限定名，由完全限定的接口/类名 + "." + 方法名组成。
	 * @param method 方法对象
	 * @return 方法的限定名
	 */
	public static String getQualifiedMethodName(Method method) {
		return getQualifiedMethodName(method, null);
	}

	/**
	 * 返回给定方法的限定名，由完全限定的接口/类名 + "." + 方法名组成。
	 * @param method 方法对象
	 * @param clazz 调用该方法的类（可为{@code null}，表示使用方法的声明类）
	 * @return 方法的限定名
	 * @since 4.3.4
	 */
	public static String getQualifiedMethodName(Method method, @Nullable Class<?> clazz) {
		Assert.notNull(method, "Method must not be null");
		return (clazz != null ? clazz : method.getDeclaringClass()).getName() + '.' + method.getName();
	}

	/**
	 * 判断给定类是否具有指定签名的公共构造方法。
	 * <p>本质上将{@code NoSuchMethodException}转换为"false"。
	 * @param clazz 要分析的类
	 * @param paramTypes 方法的参数类型
	 * @return 类是否具有对应的构造方法
	 * @see Class#getConstructor
	 */
	public static boolean hasConstructor(Class<?> clazz, Class<?>... paramTypes) {
		return (getConstructorIfAvailable(clazz, paramTypes) != null);
	}

	/**
	 * 判断给定类是否具有指定签名的公共构造方法，如果存在则返回该方法（否则返回{@code null}）。
	 * <p>本质上将{@code NoSuchMethodException}转换为{@code null}。
	 * @param clazz 要分析的类
	 * @param paramTypes 方法的参数类型
	 * @return 构造方法，如果未找到则返回{@code null}
	 * @see Class#getConstructor
	 */
	@Nullable
	public static <T> Constructor<T> getConstructorIfAvailable(Class<T> clazz, Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		try {
			return clazz.getConstructor(paramTypes);
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	/**
	 * 判断给定类是否具有指定签名的公共方法。
	 * <p>本质上将{@code NoSuchMethodException}转换为"false"。
	 * @param clazz 要分析的类
	 * @param methodName 方法名称
	 * @param paramTypes 方法的参数类型
	 * @return 类是否具有对应的方法
	 * @see Class#getMethod
	 */
	public static boolean hasMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		return (getMethodIfAvailable(clazz, methodName, paramTypes) != null);
	}

	/**
	 * 判断给定类是否具有指定签名的公共方法，如果存在则返回该方法（否则抛出{@code IllegalStateException}）。
	 * <p>在指定签名的情况下，仅当存在唯一候选方法（即具有指定名称的单个公共方法）时才会返回。
	 * <p>本质上将{@code NoSuchMethodException}转换为{@code IllegalStateException}。
	 * @param clazz 要分析的类
	 * @param methodName 方法名称
	 * @param paramTypes 方法的参数类型（可为{@code null}，表示接受任何签名）
	 * @return 方法（永不返回{@code null}）
	 * @throws IllegalStateException 如果未找到方法
	 * @see Class#getMethod
	 */
	public static Method getMethod(Class<?> clazz, String methodName, @Nullable Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class 不可以为空");
		Assert.notNull(methodName, "Method名称 不可以为空");
		if (paramTypes != null) {
			try {
				return clazz.getMethod(methodName, paramTypes);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Expected method not found: " + ex);
			}
		}
		else {
			Set<Method> candidates = new HashSet<>(1);
			Method[] methods = clazz.getMethods();
			for (Method method : methods) {
				if (methodName.equals(method.getName())) {
					candidates.add(method);
				}
			}
			if (candidates.size() == 1) {
				return candidates.iterator().next();
			}
			else if (candidates.isEmpty()) {
				throw new IllegalStateException("Expected method not found: " + clazz.getName() + '.' + methodName);
			}
			else {
				throw new IllegalStateException("No unique method found: " + clazz.getName() + '.' + methodName);
			}
		}
	}

	/**
	 * <p>判断给定的类中是否存在具有指定签名的公共方法，并在存在时返回该方法（否则返回 {@code null}）。</p>
	 * <p>对于任何指定的签名，仅在存在唯一候选方法（即具有指定名称的单个公共方法）时才返回该方法。</p>
	 * <p>实质上，将 {@code NoSuchMethodException} 转换为 {@code null}。</p>
	 * @param clazz 要分析的类
	 * @param methodName 方法的名称
	 * @param paramTypes 方法的参数类型（可以为 {@code null} 表示任何签名）
	 * @return 方法，若未找到则返回 {@code null}
	 * @see Class#getMethod
	 */
	@Nullable
	public static Method getMethodIfAvailable(Class<?> clazz, String methodName, @Nullable Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class 不可以为空");
		Assert.notNull(methodName, "Method名称 不可以为空");
		if (paramTypes != null) {
			try {
				return clazz.getMethod(methodName, paramTypes);
			}
			catch (NoSuchMethodException ex) {
				return null;
			}
		}
		else {
			Set<Method> candidates = new HashSet<>(1);
			Method[] methods = clazz.getMethods();
			for (Method method : methods) {
				if (methodName.equals(method.getName())) {
					candidates.add(method);
				}
			}
			if (candidates.size() == 1) {
				return candidates.iterator().next();
			}
			return null;
		}
	}

	/**
	 * 返回给定类及其超类中具有指定名称的方法数量（参数类型不限）。
	 * 包括非公共方法。
	 * @param clazz 要检查的类
	 * @param methodName 方法名称
	 * @return 具有给定名称的方法数量
	 */
	public static int getMethodCountForName(Class<?> clazz, String methodName) {
		Assert.notNull(clazz, "Class 不可以为空");
		Assert.notNull(methodName, "Method名称 不可以为空");
		int count = 0;
		Method[] declaredMethods = clazz.getDeclaredMethods();
		for (Method method : declaredMethods) {
			if (methodName.equals(method.getName())) {
				count++;
			}
		}
		Class<?>[] ifcs = clazz.getInterfaces();
		for (Class<?> ifc : ifcs) {
			count += getMethodCountForName(ifc, methodName);
		}
		if (clazz.getSuperclass() != null) {
			count += getMethodCountForName(clazz.getSuperclass(), methodName);
		}
		return count;
	}

	/**
	 * 给定类或其超类是否至少拥有一个或多个指定名称的方法（参数类型不限）？
	 * 包括非公共方法。
	 * @param clazz 要检查的类
	 * @param methodName 方法名称
	 * @return 是否至少存在一个具有给定名称的方法
	 */
	public static boolean hasAtLeastOneMethodWithName(Class<?> clazz, String methodName) {
		Assert.notNull(clazz, "Class 不可以为空");
		Assert.notNull(methodName, "Method名称 不可以为空");
		Method[] declaredMethods = clazz.getDeclaredMethods();
		for (Method method : declaredMethods) {
			if (method.getName().equals(methodName)) {
				return true;
			}
		}
		Class<?>[] ifcs = clazz.getInterfaces();
		for (Class<?> ifc : ifcs) {
			if (hasAtLeastOneMethodWithName(ifc, methodName)) {
				return true;
			}
		}
		return (clazz.getSuperclass() != null && hasAtLeastOneMethodWithName(clazz.getSuperclass(), methodName));
	}

	/**
	 * 给定一个可能来自接口的方法，以及当前反射调用中使用的目标类，查找对应的目标方法（如果存在）。
	 * 例如，方法可能是{@code IFoo.bar()}，目标类可能是{@code DefaultFoo}。
	 * 在这种情况下，方法可能是{@code DefaultFoo.bar()}。这样可以找到该方法上的属性。
	 * <p>
	 *     <b>注意：</b>
	 *     与{@link org.springframework.aop.support.AopUtils#getMostSpecificMethod}不同，此方法<i>不会</i>自动解析Java 5桥接方法。
	 *     如果需要解析桥接方法（例如从原始方法定义获取元数据），请调用{@link org.springframework.core.BridgeMethodResolver#findBridgedMethod}。
	 * <p><b>注意：</b>从Spring 3.1.1开始，如果Java安全设置不允许反射访问
	 * （例如调用{@code Class#getDeclaredMethods}等），此实现将回退到返回最初提供的方法。
	 * @param method 要调用的方法，可能来自接口
	 * @param targetClass 当前调用的目标类（可能为{@code null}，或者甚至没有实现该方法）
	 * @return 特定的目标方法，如果{@code targetClass}没有实现该方法则返回原始方法
	 */
	public static Method getMostSpecificMethod(Method method, @Nullable Class<?> targetClass) {
		if (targetClass != null && targetClass != method.getDeclaringClass() && isOverridable(method, targetClass)) {
			try {
				if (Modifier.isPublic(method.getModifiers())) {
					try {
						return targetClass.getMethod(method.getName(), method.getParameterTypes());
					}
					catch (NoSuchMethodException ex) {
						return method;
					}
				}
				else {
					Method specificMethod =
							ReflectionUtils.findMethod(targetClass, method.getName(), method.getParameterTypes());
					return (specificMethod != null ? specificMethod : method);
				}
			}
			catch (SecurityException ex) {
				// Security settings are disallowing reflective access; fall back to 'method' below.
			}
		}
		return method;
	}

	/**
	 * 判断给定方法是否由用户声明或至少指向用户声明的方法。
	 * <p>
	 *     检查{@link Method#isSynthetic()}（用于实现方法）以及{@code GroovyObject}接口
	 *     （用于接口方法；在实现类上，{@code GroovyObject}方法的实现将被标记为合成方法）。
	 *     注意，尽管是合成方法，桥接方法（{@link Method#isBridge()}）被视为用户级方法，因为它们最终指向用户声明的泛型方法。
	 * @param method 要检查的方法
	 * @return 如果方法可被视为用户声明则返回{@code true}；否则返回{@code false}
	 */
	public static boolean isUserLevelMethod(Method method) {
		Assert.notNull(method, "Method must not be null");
		return (method.isBridge() || (!method.isSynthetic() && !isGroovyObjectMethod(method)));
	}

	private static boolean isGroovyObjectMethod(Method method) {
		return method.getDeclaringClass().getName().equals("groovy.lang.GroovyObject");
	}

	/**
	 * 判断给定方法在指定目标类中是否可被重写。
	 * @param method 要检查的方法
	 * @param targetClass 要检查的目标类
	 */
	private static boolean isOverridable(Method method, @Nullable Class<?> targetClass) {
		if (Modifier.isPrivate(method.getModifiers())) {
			return false;
		}
		if (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
			return true;
		}
		return (targetClass == null ||
				getPackageName(method.getDeclaringClass()).equals(getPackageName(targetClass)));
	}

	/**
	 * 返回类的公共静态方法。
	 * @param clazz 定义方法的类
	 * @param methodName 静态方法名
	 * @param args 方法的参数类型
	 * @return 静态方法，如果未找到静态方法则返回{@code null}
	 * @throws IllegalArgumentException 如果方法名为空或clazz为null
	 */
	@Nullable
	public static Method getStaticMethod(Class<?> clazz, String methodName, Class<?>... args) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		try {
			Method method = clazz.getMethod(methodName, args);
			return Modifier.isStatic(method.getModifiers()) ? method : null;
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

}
