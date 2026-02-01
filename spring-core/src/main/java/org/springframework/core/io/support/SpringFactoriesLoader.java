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

package org.springframework.core.io.support;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.KCallablesJvm;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.KotlinDetector;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.UrlResource;
import org.springframework.core.log.LogMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 框架内部使用的通用工厂加载机制。
 *
 * <p>{@code SpringFactoriesLoader} {@linkplain #loadFactories 加载} 并实例化
 * 来自 {@value #FACTORIES_RESOURCE_LOCATION} 文件的给定类型的工厂，这些文件
 * 可能存在于类路径中的多个 JAR 文件中。{@code spring.factories}
 * 文件必须是 {@link Properties} 格式，其中键是完全限定的
 * 接口或抽象类名称，值是用逗号分隔的实现类名列表。例如：
 *
 * <pre class="code">example.MyService=example.MyServiceImpl1,example.MyServiceImpl2</pre>
 *
 * 其中 {@code example.MyService} 是接口名称，而 {@code MyServiceImpl1}
 * 和 {@code MyServiceImpl2} 是两个实现。
 *
 * <p>实现类<b>必须</b>有一个可解析的构造函数，用于创建实例，要么是：
 * <ul>
 * <li>主构造函数或单一构造函数</li>
 * <li>单一公共构造函数</li>
 * <li>默认构造函数</li>
 * </ul>
 *
 * <p>如果可解析的构造函数有参数，应提供合适的 {@link ArgumentResolver
 * ArgumentResolver}。要自定义如何处理实例化失败，
 * 请考虑提供一个 {@link FailureHandler FailureHandler}。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 3.2
 */
public class SpringFactoriesLoader {

	/**
	 * 查找工厂的位置。
	 * <p>可能存在于多个 JAR 文件中。
	 */
	public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";

	private static final FailureHandler THROWING_FAILURE_HANDLER = FailureHandler.throwing();

	private static final Log logger = LogFactory.getLog(SpringFactoriesLoader.class);

	static final Map<ClassLoader, Map<String, Factories>> cache = new ConcurrentReferenceHashMap<>();


	@Nullable
	private final ClassLoader classLoader;

	private final Map<String, List<String>> factories;


	/**
	 * 创建一个新的 {@link SpringFactoriesLoader} 实例。
	 * @param classLoader 用于实例化工厂的类加载器
	 * @param factories 工厂类名到实现类名的映射
	 * @since 6.0
	 */
	protected SpringFactoriesLoader(@Nullable ClassLoader classLoader, Map<String, List<String>> factories) {
		this.classLoader = classLoader;
		this.factories = factories;
	}


	/**
	 * 从 {@value #FACTORIES_RESOURCE_LOCATION} 加载并实例化给定类型的工厂实现，
	 * 使用配置的类加载器和预期无参数构造函数的默认参数解析器。
	 * <p>返回的工厂使用 {@link AnnotationAwareOrderComparator} 进行排序。
	 * <p>如果需要自定义实例化策略，请使用带有自定义 {@link ArgumentResolver ArgumentResolver} 和/或
	 * {@link FailureHandler FailureHandler} 的 {@code load(...)}。
	 * <p>如果为给定的工厂类型发现重复的实现类名，则只会实例化重复实现类型的一个实例。
	 * @param factoryType 表示工厂的接口或抽象类
	 * @throws IllegalArgumentException 如果任何工厂实现类无法
	 * 加载或在实例化任何工厂时发生错误
	 * @since 6.0
	 */
	public <T> List<T> load(Class<T> factoryType) {
		return load(factoryType, null, null);
	}

	/**
	 * 从 {@value #FACTORIES_RESOURCE_LOCATION} 加载并实例化给定类型的工厂实现，
	 * 使用配置的类加载器和给定的参数解析器。
	 * <p>返回的工厂使用 {@link AnnotationAwareOrderComparator} 进行排序。
	 * <p>如果为给定的工厂类型发现重复的实现类名，则只会实例化重复实现类型的一个实例。
	 * @param factoryType 表示工厂的接口或抽象类
	 * @param argumentResolver 用于按类型解析构造函数参数的策略
	 * @throws IllegalArgumentException 如果任何工厂实现类无法
	 * 加载或在实例化任何工厂时发生错误
	 * @since 6.0
	 */
	public <T> List<T> load(Class<T> factoryType, @Nullable ArgumentResolver argumentResolver) {
		return load(factoryType, argumentResolver, null);
	}

	/**
	 * 从 {@value #FACTORIES_RESOURCE_LOCATION} 加载并实例化给定类型的工厂实现，
	 * 使用配置的类加载器和由给定故障处理器提供的自定义故障处理。
	 * <p>返回的工厂使用 {@link AnnotationAwareOrderComparator} 进行排序。
	 * <p>如果为给定的工厂类型发现重复的实现类名，则只会实例化重复实现类型的一个实例。
	 * <p>对于任何无法加载的工厂实现类或在实例化时发生的错误，将调用给定的故障处理器。
	 * @param factoryType 表示工厂的接口或抽象类
	 * @param failureHandler 用于处理工厂实例化失败的策略
	 * @since 6.0
	 */
	public <T> List<T> load(Class<T> factoryType, @Nullable FailureHandler failureHandler) {
		return load(factoryType, null, failureHandler);
	}

	/**
	 * 从 {@value #FACTORIES_RESOURCE_LOCATION} 加载并实例化给定类型的工厂实现，
	 * 使用配置的类加载器、给定的参数解析器和由给定故障处理器提供的自定义故障处理。
	 * <p>返回的工厂使用 {@link AnnotationAwareOrderComparator} 进行排序。
	 * <p>如果为给定的工厂类型发现重复的实现类名，则只会实例化重复实现类型的一个实例。
	 * <p>对于任何无法加载的工厂实现类或在实例化时发生的错误，将调用给定的故障处理器。
	 * @param factoryType 表示工厂的接口或抽象类
	 * @param argumentResolver 用于按类型解析构造函数参数的策略
	 * @param failureHandler 用于处理工厂实例化失败的策略
	 * @since 6.0
	 */
	public <T> List<T> load(Class<T> factoryType, @Nullable ArgumentResolver argumentResolver,
			@Nullable FailureHandler failureHandler) {

		Assert.notNull(factoryType, "'factoryType' must not be null");
		List<String> implementationNames = loadFactoryNames(factoryType);
		logger.trace(LogMessage.format("Loaded [%s] names: %s", factoryType.getName(), implementationNames));
		List<T> result = new ArrayList<>(implementationNames.size());
		FailureHandler failureHandlerToUse = (failureHandler != null) ? failureHandler : THROWING_FAILURE_HANDLER;
		for (String implementationName : implementationNames) {
			T factory = instantiateFactory(implementationName, factoryType, argumentResolver, failureHandlerToUse);
			if (factory != null) {
				result.add(factory);
			}
		}
		AnnotationAwareOrderComparator.sort(result);
		return result;
	}

	private List<String> loadFactoryNames(Class<?> factoryType) {
		return this.factories.getOrDefault(factoryType.getName(), Collections.emptyList());
	}

	@Nullable
	protected <T> T instantiateFactory(String implementationName, Class<T> type,
			@Nullable ArgumentResolver argumentResolver, FailureHandler failureHandler) {

		try {
			Class<?> factoryImplementationClass = ClassUtils.forName(implementationName, this.classLoader);
			Assert.isTrue(type.isAssignableFrom(factoryImplementationClass), () ->
					"Class [%s] is not assignable to factory type [%s]".formatted(implementationName, type.getName()));
			FactoryInstantiator<T> factoryInstantiator = FactoryInstantiator.forClass(factoryImplementationClass);
			return factoryInstantiator.instantiate(argumentResolver);
		}
		catch (Throwable ex) {
			failureHandler.handleFailure(type, implementationName, ex);
			return null;
		}
	}


	/**
	 * 从 {@value #FACTORIES_RESOURCE_LOCATION} 加载并实例化给定类型的工厂实现，
	 * 使用给定的类加载器。
	 * <p>返回的工厂使用 {@link AnnotationAwareOrderComparator} 进行排序。
	 * <p>如果为给定的工厂类型发现重复的实现类名，则只会实例化重复实现类型的一个实例。
	 * <p>要使用 {@link ArgumentResolver} 或
	 * {@link FailureHandler} 支持进行更高级的工厂加载，请使用 {@link #forDefaultResourceLocation(ClassLoader)}
	 * 来获取 {@link SpringFactoriesLoader} 实例。
	 * @param factoryType 表示工厂的接口或抽象类
	 * @param classLoader 用于加载的 ClassLoader（可以是 {@code null}
	 * 以使用默认值）
	 * @throws IllegalArgumentException 如果任何工厂实现类无法
	 * 加载或在实例化任何工厂时发生错误
	 */
	public static <T> List<T> loadFactories(Class<T> factoryType, @Nullable ClassLoader classLoader) {
		return forDefaultResourceLocation(classLoader).load(factoryType);
	}

	/**
	 * 从 {@value #FACTORIES_RESOURCE_LOCATION} 加载给定类型工厂实现的完全限定类名，
	 * 使用给定的类加载器。
	 * <p>如果给定工厂类型的特定实现类名被发现多次，
	 * 重复项将被忽略。
	 * @param factoryType 表示工厂的接口或抽象类
	 * @param classLoader 用于加载资源的 ClassLoader；可以是
	 * {@code null} 以使用默认值
	 * @throws IllegalArgumentException 如果在加载工厂名称时发生错误
	 * @see #loadFactories
	 * @deprecated 从 6.0 版本起，推荐使用 {@link #load(Class, ArgumentResolver, FailureHandler)}
	 */
	@Deprecated(since = "6.0")
	public static List<String> loadFactoryNames(Class<?> factoryType, @Nullable ClassLoader classLoader) {
		return forDefaultResourceLocation(classLoader).loadFactoryNames(factoryType);
	}

	/**
	 * 创建一个 {@link SpringFactoriesLoader} 实例，该实例将从
	 * {@value #FACTORIES_RESOURCE_LOCATION} 加载并实例化工厂实现，
	 * 使用默认类加载器。
	 * @return 一个 {@link SpringFactoriesLoader} 实例
	 * @since 6.0
	 * @see #forDefaultResourceLocation(ClassLoader)
	 */
	public static SpringFactoriesLoader forDefaultResourceLocation() {
		return forDefaultResourceLocation(null);
	}

	/**
	 * 创建一个 {@link SpringFactoriesLoader} 实例，该实例将从
	 * {@value #FACTORIES_RESOURCE_LOCATION} 加载并实例化工厂实现，
	 * 使用给定的类加载器。
	 * @param classLoader 用于加载资源的 ClassLoader；可以是
	 * {@code null} 以使用默认值
	 * @return 一个 {@link SpringFactoriesLoader} 实例
	 * @since 6.0
	 * @see #forDefaultResourceLocation()
	 */
	public static SpringFactoriesLoader forDefaultResourceLocation(@Nullable ClassLoader classLoader) {
		return forResourceLocation(FACTORIES_RESOURCE_LOCATION, classLoader);
	}

	/**
	 * 创建一个 {@link SpringFactoriesLoader} 实例，该实例将从
	 * 给定位置加载并实例化工厂实现，
	 * 使用默认类加载器。
	 * @param resourceLocation 查找工厂的资源位置
	 * @return 一个 {@link SpringFactoriesLoader} 实例
	 * @since 6.0
	 * @see #forResourceLocation(String, ClassLoader)
	 */
	public static SpringFactoriesLoader forResourceLocation(String resourceLocation) {
		return forResourceLocation(resourceLocation, null);
	}

	/**
	 * 创建一个 {@link SpringFactoriesLoader} 实例，该实例将从
	 * 给定位置加载并实例化工厂实现，
	 * 使用给定的类加载器。
	 * @param resourceLocation 查找工厂的资源位置
	 * @param classLoader 用于加载资源的 ClassLoader；
	 * 可以是 {@code null} 以使用默认值
	 * @return 一个 {@link SpringFactoriesLoader} 实例
	 * @since 6.0
	 * @see #forResourceLocation(String)
	 */
	public static SpringFactoriesLoader forResourceLocation(String resourceLocation, @Nullable ClassLoader classLoader) {
		Assert.hasText(resourceLocation, "'resourceLocation' must not be empty");
		ClassLoader resourceClassLoader = (classLoader != null ? classLoader :
				SpringFactoriesLoader.class.getClassLoader());
		Map<String, Factories> factoriesCache = cache.computeIfAbsent(
				resourceClassLoader, key -> new ConcurrentReferenceHashMap<>());
		Factories factories = factoriesCache.computeIfAbsent(resourceLocation,
				key -> new Factories(loadFactoriesResource(resourceClassLoader, resourceLocation)));
		return new SpringFactoriesLoader(classLoader, factories.byType());
	}

	protected static Map<String, List<String>> loadFactoriesResource(ClassLoader classLoader, String resourceLocation) {
		Map<String, List<String>> result = new LinkedHashMap<>();
		try {
			Enumeration<URL> urls = classLoader.getResources(resourceLocation);
			while (urls.hasMoreElements()) {
				UrlResource resource = new UrlResource(urls.nextElement());
				Properties properties = PropertiesLoaderUtils.loadProperties(resource);
				properties.forEach((name, value) -> {
					String[] factoryImplementationNames = StringUtils.commaDelimitedListToStringArray((String) value);
					List<String> implementations = result.computeIfAbsent(((String) name).trim(),
							key -> new ArrayList<>(factoryImplementationNames.length));
					Arrays.stream(factoryImplementationNames).map(String::trim).forEach(implementations::add);
				});
			}
			result.replaceAll(SpringFactoriesLoader::toDistinctUnmodifiableList);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Unable to load factories from location [" + resourceLocation + "]", ex);
		}
		return Collections.unmodifiableMap(result);
	}

	private static List<String> toDistinctUnmodifiableList(String factoryType, List<String> implementations) {
		return implementations.stream().distinct().toList();
	}


	/**
	 * 用于创建工厂实例的内部实例化器。
	 * @since 6.0
	 * @param <T> 实例实现类型
	 */
	static final class FactoryInstantiator<T> {

		private final Constructor<T> constructor;

		private FactoryInstantiator(Constructor<T> constructor) {
			ReflectionUtils.makeAccessible(constructor);
			this.constructor = constructor;
		}

		T instantiate(@Nullable ArgumentResolver argumentResolver) throws Exception {
			Object[] args = resolveArgs(argumentResolver);
			if (isKotlinType(this.constructor.getDeclaringClass())) {
				return KotlinDelegate.instantiate(this.constructor, args);
			}
			return this.constructor.newInstance(args);
		}

		private Object[] resolveArgs(@Nullable ArgumentResolver argumentResolver) {
			Class<?>[] types = this.constructor.getParameterTypes();
			return (argumentResolver != null ?
					Arrays.stream(types).map(argumentResolver::resolve).toArray() :
					new Object[types.length]);
		}

		@SuppressWarnings("unchecked")
		static <T> FactoryInstantiator<T> forClass(Class<?> factoryImplementationClass) {
			Constructor<?> constructor = findConstructor(factoryImplementationClass);
			Assert.state(constructor != null, () ->
					"Class [%s] has no suitable constructor".formatted(factoryImplementationClass.getName()));
			return new FactoryInstantiator<>((Constructor<T>) constructor);
		}

		@Nullable
		private static Constructor<?> findConstructor(Class<?> factoryImplementationClass) {
			// Same algorithm as BeanUtils.getResolvableConstructor
			Constructor<?> constructor = findPrimaryKotlinConstructor(factoryImplementationClass);
			constructor = (constructor != null ? constructor :
					findSingleConstructor(factoryImplementationClass.getConstructors()));
			constructor = (constructor != null ? constructor :
					findSingleConstructor(factoryImplementationClass.getDeclaredConstructors()));
			constructor = (constructor != null ? constructor :
					findDeclaredConstructor(factoryImplementationClass));
			return constructor;
		}

		@Nullable
		private static Constructor<?> findPrimaryKotlinConstructor(Class<?> factoryImplementationClass) {
			return (isKotlinType(factoryImplementationClass) ?
					KotlinDelegate.findPrimaryConstructor(factoryImplementationClass) : null);
		}

		private static boolean isKotlinType(Class<?> factoryImplementationClass) {
			return KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(factoryImplementationClass);
		}

		@Nullable
		private static Constructor<?> findSingleConstructor(Constructor<?>[] constructors) {
			return (constructors.length == 1 ? constructors[0] : null);
		}

		@Nullable
		private static Constructor<?> findDeclaredConstructor(Class<?> factoryImplementationClass) {
			try {
				return factoryImplementationClass.getDeclaredConstructor();
			}
			catch (NoSuchMethodException ex) {
				return null;
			}
		}
	}


	/**
	 * 嵌套类以避免在运行时对 Kotlin 的硬依赖。
	 * @since 6.0
	 */
	private static class KotlinDelegate {

		@Nullable
		static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz) {
			try {
				KFunction<T> primaryConstructor = KClasses.getPrimaryConstructor(JvmClassMappingKt.getKotlinClass(clazz));
				if (primaryConstructor != null) {
					Constructor<T> constructor = ReflectJvmMapping.getJavaConstructor(
							primaryConstructor);
					Assert.state(constructor != null, () ->
							"Failed to find Java constructor for Kotlin primary constructor: " + clazz.getName());
					return constructor;
				}
			}
			catch (UnsupportedOperationException ex) {
				// ignore
			}
			return null;
		}

		static <T> T instantiate(Constructor<T> constructor, Object[] args) throws Exception {
			KFunction<T> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(constructor);
			if (kotlinConstructor == null) {
				return constructor.newInstance(args);
			}
			makeAccessible(constructor, kotlinConstructor);
			return instantiate(kotlinConstructor, convertArgs(args, kotlinConstructor.getParameters()));
		}

		private static <T> void makeAccessible(Constructor<T> constructor,
				KFunction<T> kotlinConstructor) {
			if ((!Modifier.isPublic(constructor.getModifiers()) ||
					!Modifier.isPublic(constructor.getDeclaringClass().getModifiers()))) {
				KCallablesJvm.setAccessible(kotlinConstructor, true);
			}
		}

		private static Map<KParameter, Object> convertArgs(Object[] args, List<KParameter> parameters) {
			Map<KParameter, Object> result = CollectionUtils.newHashMap(parameters.size());
			Assert.isTrue(args.length <= parameters.size(),
					"Number of provided arguments should be less than or equal to the number of constructor parameters");
			for (int i = 0; i < args.length; i++) {
				if (!parameters.get(i).isOptional() || args[i] != null) {
					result.put(parameters.get(i), args[i]);
				}
			}
			return result;
		}

		private static <T> T instantiate(KFunction<T> kotlinConstructor, Map<KParameter, Object> args) {
			return kotlinConstructor.callBy(args);
		}
	}


	/**
	 * 基于类型解析构造函数参数的策略。
	 * @since 6.0
	 * @see ArgumentResolver#of(Class, Object)
	 * @see ArgumentResolver#ofSupplied(Class, Supplier)
	 * @see ArgumentResolver#from(Function)
	 */
	@FunctionalInterface
	public interface ArgumentResolver {

		/**
		 * 如果可能，解析给定参数。
		 * @param <T> 参数类型
		 * @param type 参数类型
		 * @return 解析的参数值或 {@code null}
		 */
		@Nullable
		<T> T resolve(Class<T> type);

		/**
		 * 通过将此解析器与给定类型和值组合来创建新的组合 {@link ArgumentResolver}。
		 * @param <T> 参数类型
		 * @param type 参数类型
		 * @param value 参数值
		 * @return 新的复合 {@link ArgumentResolver} 实例
		 */
		default <T> ArgumentResolver and(Class<T> type, T value) {
			return and(ArgumentResolver.of(type, value));
		}

		/**
		 * 通过将此解析器与给定类型和值组合来创建新的组合 {@link ArgumentResolver}。
		 * @param <T> 参数类型
		 * @param type 参数类型
		 * @param valueSupplier 参数值供应器
		 * @return 新的复合 {@link ArgumentResolver} 实例
		 */
		default <T> ArgumentResolver andSupplied(Class<T> type, Supplier<T> valueSupplier) {
			return and(ArgumentResolver.ofSupplied(type, valueSupplier));
		}

		/**
		 * 通过将此解析器与给定解析器组合来创建新的组合 {@link ArgumentResolver}。
		 * @param argumentResolver 要添加的参数解析器
		 * @return 新的复合 {@link ArgumentResolver} 实例
		 */
		default ArgumentResolver and(ArgumentResolver argumentResolver) {
			return from(type -> {
				Object resolved = resolve(type);
				return (resolved != null ? resolved : argumentResolver.resolve(type));
			});
		}

		/**
		 * 工厂方法，返回始终返回 {@code null} 的 {@link ArgumentResolver}。
		 * @return 新的 {@link ArgumentResolver} 实例
		 */
		static ArgumentResolver none() {
			return from(type -> null);
		}

		/**
		 * 工厂方法，可用于创建仅解析给定类型的 {@link ArgumentResolver}。
		 * @param <T> 参数类型
		 * @param type 参数类型
		 * @param value 参数值
		 * @return 新的 {@link ArgumentResolver} 实例
		 */
		static <T> ArgumentResolver of(Class<T> type, T value) {
			return ofSupplied(type, () -> value);
		}

		/**
		 * 工厂方法，可用于创建仅解析给定类型的 {@link ArgumentResolver}。
		 * @param <T> 参数类型
		 * @param type 参数类型
		 * @param valueSupplier 参数值供应器
		 * @return 新的 {@link ArgumentResolver} 实例
		 */
		static <T> ArgumentResolver ofSupplied(Class<T> type, Supplier<T> valueSupplier) {
			return from(candidateType -> (candidateType.equals(type) ? valueSupplier.get() : null));
		}

		/**
		 * 工厂方法，从 lambda 友好的函数创建新的 {@link ArgumentResolver}。给定函数提供
		 * 参数类型并必须提供该类型的实例或 {@code null}。
		 * @param function 解析器函数
		 * @return 由函数支持的新 {@link ArgumentResolver} 实例
		 */
		static ArgumentResolver from(Function<Class<?>, Object> function) {
			return new ArgumentResolver() {
				@SuppressWarnings("unchecked")
				@Override
				public <T> T resolve(Class<T> type) {
					return (T) function.apply(type);
				}
			};
		}
	}


	/**
	 * 处理实例化工厂时发生的故障的策略。
	 * @since 6.0
	 * @see FailureHandler#throwing()
	 * @see FailureHandler#logging(Log)
	 */
	@FunctionalInterface
	public interface FailureHandler {

		/**
		 * 处理由于实例化预期为给定 {@code factoryType} 的
		 * {@code factoryImplementationName} 时发生的 {@code failure}。
		 * @param factoryType 工厂的类型
		 * @param factoryImplementationName 工厂实现的名称
		 * @param failure 发生的故障
		 * @see #throwing()
		 * @see #logging
		 */
		void handleFailure(Class<?> factoryType, String factoryImplementationName, Throwable failure);


		/**
		 * 创建一个新的 {@link FailureHandler}，通过抛出
		 * {@link IllegalArgumentException} 来处理错误。
		 * @return 新的 {@link FailureHandler} 实例
		 * @see #throwing(BiFunction)
		 */
		static FailureHandler throwing() {
			return throwing(IllegalArgumentException::new);
		}

		/**
		 * 创建一个新的 {@link FailureHandler}，通过抛出异常来处理错误。
		 * @param exceptionFactory 用于创建异常的工厂
		 * @return 新的 {@link FailureHandler} 实例
		 */
		static FailureHandler throwing(BiFunction<String, Throwable, ? extends RuntimeException> exceptionFactory) {
			return handleMessage((messageSupplier, failure) -> {
				throw exceptionFactory.apply(messageSupplier.get(), failure);
			});
		}

		/**
		 * 创建一个新的 {@link FailureHandler}，通过记录跟踪消息来处理错误。
		 * @param logger 用于记录消息的日志记录器
		 * @return 新的 {@link FailureHandler} 实例
		 */
		static FailureHandler logging(Log logger) {
			return handleMessage((messageSupplier, failure) -> logger.trace(LogMessage.of(messageSupplier), failure));
		}

		/**
		 * 创建一个新的 {@link FailureHandler}，使用标准格式的消息处理错误。
		 * @param messageHandler 用于处理问题的消息处理器
		 * @return 新的 {@link FailureHandler} 实例
		 */
		static FailureHandler handleMessage(BiConsumer<Supplier<String>, Throwable> messageHandler) {
			return (factoryType, factoryImplementationName, failure) -> {
				Supplier<String> messageSupplier = () -> "Unable to instantiate factory class [%s] for factory type [%s]"
						.formatted(factoryImplementationName, factoryType.getName());
				messageHandler.accept(messageSupplier, failure);
			};
		}
	}


	private record Factories(Map<String, List<String>> byType) {

	}
}
