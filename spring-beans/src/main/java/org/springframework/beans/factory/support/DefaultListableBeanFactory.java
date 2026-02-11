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

package org.springframework.beans.factory.support;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.inject.Provider;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.NamedBeanHolder;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.SpringProperties;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.log.LogMessage;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Contract;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.CompositeIterator;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Spring 对 {@link ConfigurableListableBeanFactory} 和 {@link BeanDefinitionRegistry} 接口的默认实现：
 * 一个基于 bean 定义元数据的成熟 bean 工厂，可通过后处理器进行扩展。
 *
 * <p>
 *     典型用法是首先注册所有 bean 定义（可能从 bean 定义文件中读取），然后再访问 bean。
 *     因此，通过名称查找 bean 在本地 bean 定义表中是一项低成本的操作，该操作基于预先解析的 bean 定义元数据对象进行。
 *
 * <p>
 *     请注意，特定 bean 定义格式的读取器通常是单独实现的，而不是作为 bean 工厂的子类实现：
 *     例如，请参见 {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}。
 *
 * <p>
 *     对于 {@link org.springframework.beans.factory.ListableBeanFactory} 接口的替代实现，
 *     请查看 {@link StaticListableBeanFactory}，它管理现有的 bean 实例，而不是根据 bean 定义创建新的实例。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Costin Leau
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @since 2001年4月16日
 * @see #registerBeanDefinition
 * @see #addBeanPostProcessor
 * @see #getBean
 * @see #resolveDependency
 */
@SuppressWarnings("serial")
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
		implements ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {

	/**
	 * 系统属性，指示 Spring 在 bean 创建期间强制执行严格的锁定，而不是 6.2 版本默认应用的严格和宽松锁定的混合模式。
	 * 将此标志设置为 "true" 可在预实例化阶段恢复 6.1.x 风格的锁定。
	 * <p>
	 *     默认情况下，工厂会根据遇到的线程名称推断严格的锁定：
	 *     如果额外的线程名称与主线程引导线程的线程前缀匹配，则认为它们是外部线程（多个外部引导线程调用工厂），因此会对它们应用严格的锁定。
	 *     可以通过显式将此标志设置为 "false" 来关闭此推断，而不是将其保留为未指定状态。
	 *
	 * @since 6.2.6
	 * @see #preInstantiateSingletons()
	 */
	public static final String STRICT_LOCKING_PROPERTY_NAME = "spring.locking.strict";

	@Nullable
	private static Class<?> jakartaInjectProviderClass;

	static {
		try {
			jakartaInjectProviderClass =
					ClassUtils.forName("jakarta.inject.Provider", DefaultListableBeanFactory.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			// JSR-330 API not available - Provider interface simply not supported then.
			jakartaInjectProviderClass = null;
		}
	}


	/** 从序列化ID到工厂实例的映射。 */
	private static final Map<String, Reference<DefaultListableBeanFactory>> serializableFactories =
			new ConcurrentHashMap<>(8);

	/** 此工厂中是否强制执行严格锁定或宽松锁定。 */
	@Nullable
	private final Boolean strictLocking = SpringProperties.checkFlag(STRICT_LOCKING_PROPERTY_NAME);

	/** 此工厂的可选 ID，用于序列化目的。 */
	@Nullable
	private String serializationId;

	/** 是否允许使用相同名称重新注册不同的定义。 */
	@Nullable
	private Boolean allowBeanDefinitionOverriding;

	/** 是否允许对懒加载的 bean 进行饥饿类加载。 */
	private boolean allowEagerClassLoading = true;

	@Nullable
	private Executor bootstrapExecutor;

	/** Optional OrderComparator for dependency Lists and arrays. */
	@Nullable
	private Comparator<Object> dependencyComparator;

	/** Resolver to use for checking if a bean definition is an autowire candidate. */
	private AutowireCandidateResolver autowireCandidateResolver = SimpleAutowireCandidateResolver.INSTANCE;

	/** Map from dependency type to corresponding autowired value. */
	private final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<>(16);

	/** Map of bean definition objects, keyed by bean name. */
	private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

	/** Map from bean name to merged BeanDefinitionHolder. */
	private final Map<String, BeanDefinitionHolder> mergedBeanDefinitionHolders = new ConcurrentHashMap<>(256);

	/** Map of bean definition names with a primary marker plus corresponding type. */
	private final Map<String, Class<?>> primaryBeanNamesWithType = new ConcurrentHashMap<>(16);

	/** Map of singleton and non-singleton bean names, keyed by dependency type. */
	private final Map<Class<?>, String[]> allBeanNamesByType = new ConcurrentHashMap<>(64);

	/** Map of singleton-only bean names, keyed by dependency type. */
	private final Map<Class<?>, String[]> singletonBeanNamesByType = new ConcurrentHashMap<>(64);

	/** List of bean definition names, in registration order. */
	private volatile List<String> beanDefinitionNames = new ArrayList<>(256);

	/** List of names of manually registered singletons, in registration order. */
	private volatile Set<String> manualSingletonNames = new LinkedHashSet<>(16);

	/** Cached array of bean definition names in case of frozen configuration. */
	@Nullable
	private volatile String[] frozenBeanDefinitionNames;

	/** Whether bean definition metadata may be cached for all beans. */
	private volatile boolean configurationFrozen;

	/** Name prefix of main thread: only set during pre-instantiation phase. */
	@Nullable
	private volatile String mainThreadPrefix;

	private final NamedThreadLocal<PreInstantiation> preInstantiationThread =
			new NamedThreadLocal<>("Pre-instantiation thread marker");


	/**
	 * Create a new DefaultListableBeanFactory.
	 */
	public DefaultListableBeanFactory() {
		super();
	}

	/**
	 * Create a new DefaultListableBeanFactory with the given parent.
	 * @param parentBeanFactory the parent BeanFactory
	 */
	public DefaultListableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
		super(parentBeanFactory);
	}


	/**
	 * Specify an id for serialization purposes, allowing this BeanFactory to be
	 * deserialized from this id back into the BeanFactory object, if needed.
	 */
	public void setSerializationId(@Nullable String serializationId) {
		if (serializationId != null) {
			serializableFactories.put(serializationId, new WeakReference<>(this));
		}
		else if (this.serializationId != null) {
			serializableFactories.remove(this.serializationId);
		}
		this.serializationId = serializationId;
	}

	/**
	 * Return an id for serialization purposes, if specified, allowing this BeanFactory
	 * to be deserialized from this id back into the BeanFactory object, if needed.
	 * @since 4.1.2
	 */
	@Nullable
	public String getSerializationId() {
		return this.serializationId;
	}

	/**
	 * Set whether it should be allowed to override bean definitions by registering
	 * a different definition with the same name, automatically replacing the former.
	 * If not, an exception will be thrown. This also applies to overriding aliases.
	 * <p>Default is "true".
	 * @see #registerBeanDefinition
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}

	/**
	 * Return whether it should be allowed to override bean definitions by registering
	 * a different definition with the same name, automatically replacing the former.
	 * @since 4.1.2
	 */
	public boolean isAllowBeanDefinitionOverriding() {
		return !Boolean.FALSE.equals(this.allowBeanDefinitionOverriding);
	}

	/**
	 * Set whether the factory is allowed to eagerly load bean classes
	 * even for bean definitions that are marked as "lazy-init".
	 * <p>Default is "true". Turn this flag off to suppress class loading
	 * for lazy-init beans unless such a bean is explicitly requested.
	 * In particular, by-type lookups will then simply ignore bean definitions
	 * without resolved class name, instead of loading the bean classes on
	 * demand just to perform a type check.
	 * @see AbstractBeanDefinition#setLazyInit
	 */
	public void setAllowEagerClassLoading(boolean allowEagerClassLoading) {
		this.allowEagerClassLoading = allowEagerClassLoading;
	}

	/**
	 * Return whether the factory is allowed to eagerly load bean classes
	 * even for bean definitions that are marked as "lazy-init".
	 * @since 4.1.2
	 */
	public boolean isAllowEagerClassLoading() {
		return this.allowEagerClassLoading;
	}

	@Override
	public void setBootstrapExecutor(@Nullable Executor bootstrapExecutor) {
		this.bootstrapExecutor = bootstrapExecutor;
	}

	@Override
	@Nullable
	public Executor getBootstrapExecutor() {
		return this.bootstrapExecutor;
	}

	/**
	 * Set a {@link java.util.Comparator} for dependency Lists and arrays.
	 * @since 4.0
	 * @see org.springframework.core.OrderComparator
	 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
	 */
	public void setDependencyComparator(@Nullable Comparator<Object> dependencyComparator) {
		this.dependencyComparator = dependencyComparator;
	}

	/**
	 * 返回此 BeanFactory 的依赖项比较器（可能为 {@code null}）。
	 * @since 4.0
	 */
	@Nullable
	public Comparator<Object> getDependencyComparator() {
		return this.dependencyComparator;
	}

	/**
	 * 为此 BeanFactory 设置一个自定义的自动装配候选解析器，用于决定某个 bean 定义是否应被视为自动装配的候选者。
	 */
	public void setAutowireCandidateResolver(AutowireCandidateResolver autowireCandidateResolver) {
		Assert.notNull(autowireCandidateResolver, "AutowireCandidateResolver must not be null");
		if (autowireCandidateResolver instanceof BeanFactoryAware beanFactoryAware) {
			beanFactoryAware.setBeanFactory(this);
		}
		this.autowireCandidateResolver = autowireCandidateResolver;
	}

	/**
	 * 返回此 BeanFactory 的自动装配候选解析器（永不为 {@code null}）。
	 */
	public AutowireCandidateResolver getAutowireCandidateResolver() {
		return this.autowireCandidateResolver;
	}


	@Override
	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
		super.copyConfigurationFrom(otherFactory);
		if (otherFactory instanceof DefaultListableBeanFactory otherListableFactory) {
			this.allowBeanDefinitionOverriding = otherListableFactory.allowBeanDefinitionOverriding;
			this.allowEagerClassLoading = otherListableFactory.allowEagerClassLoading;
			this.bootstrapExecutor = otherListableFactory.bootstrapExecutor;
			this.dependencyComparator = otherListableFactory.dependencyComparator;
			// A clone of the AutowireCandidateResolver since it is potentially BeanFactoryAware
			setAutowireCandidateResolver(otherListableFactory.getAutowireCandidateResolver().cloneIfNecessary());
			// Make resolvable dependencies (for example, ResourceLoader) available here as well
			this.resolvableDependencies.putAll(otherListableFactory.resolvableDependencies);
		}
	}


	//---------------------------------------------------------------------
	// BeanFactory 剩余方法的实现
	//---------------------------------------------------------------------

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return getBean(requiredType, (Object[]) null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getBean(Class<T> requiredType, @Nullable Object... args) throws BeansException {
		Assert.notNull(requiredType, "Required type must not be null");
		Object resolved = resolveBean(ResolvableType.forRawClass(requiredType), args, false);
		if (resolved == null) {
			throw new NoSuchBeanDefinitionException(requiredType);
		}
		return (T) resolved;
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
		Assert.notNull(requiredType, "Required type must not be null");
		return getBeanProvider(ResolvableType.forRawClass(requiredType), true);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		return getBeanProvider(requiredType, true);
	}


	//---------------------------------------------------------------------
	// ListableBeanFactory 接口的实现
	//---------------------------------------------------------------------

	@Override
	public boolean containsBeanDefinition(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return this.beanDefinitionMap.containsKey(beanName);
	}

	@Override
	public int getBeanDefinitionCount() {
		return this.beanDefinitionMap.size();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		String[] frozenNames = this.frozenBeanDefinitionNames;
		if (frozenNames != null) {
			return frozenNames.clone();
		}
		else {
			return StringUtils.toStringArray(this.beanDefinitionNames);
		}
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
		Assert.notNull(requiredType, "Required type must not be null");
		return getBeanProvider(ResolvableType.forRawClass(requiredType), allowEagerInit);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
		return new BeanObjectProvider<>() {
			@Override
			public T getObject() throws BeansException {
				T resolved = resolveBean(requiredType, null, false);
				if (resolved == null) {
					throw new NoSuchBeanDefinitionException(requiredType);
				}
				return resolved;
			}
			@Override
			public T getObject(Object... args) throws BeansException {
				T resolved = resolveBean(requiredType, args, false);
				if (resolved == null) {
					throw new NoSuchBeanDefinitionException(requiredType);
				}
				return resolved;
			}
			@Override
			@Nullable
			public T getIfAvailable() throws BeansException {
				try {
					return resolveBean(requiredType, null, false);
				}
				catch (ScopeNotActiveException ex) {
					// Ignore resolved bean in non-active scope
					return null;
				}
			}
			@Override
			public void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
				T dependency = getIfAvailable();
				if (dependency != null) {
					try {
						dependencyConsumer.accept(dependency);
					}
					catch (ScopeNotActiveException ex) {
						// Ignore resolved bean in non-active scope, even on scoped proxy invocation
					}
				}
			}
			@Override
			@Nullable
			public T getIfUnique() throws BeansException {
				try {
					return resolveBean(requiredType, null, true);
				}
				catch (ScopeNotActiveException ex) {
					// Ignore resolved bean in non-active scope
					return null;
				}
			}
			@Override
			public void ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
				T dependency = getIfUnique();
				if (dependency != null) {
					try {
						dependencyConsumer.accept(dependency);
					}
					catch (ScopeNotActiveException ex) {
						// Ignore resolved bean in non-active scope, even on scoped proxy invocation
					}
				}
			}
			@SuppressWarnings("unchecked")
			@Override
			public Stream<T> stream() {
				return Arrays.stream(beanNamesForStream(requiredType, true, allowEagerInit))
						.map(name -> (T) getBean(name))
						.filter(bean -> !(bean instanceof NullBean));
			}
			@SuppressWarnings("unchecked")
			@Override
			public Stream<T> orderedStream() {
				String[] beanNames = beanNamesForStream(requiredType, true, allowEagerInit);
				if (beanNames.length == 0) {
					return Stream.empty();
				}
				Map<String, T> matchingBeans = CollectionUtils.newLinkedHashMap(beanNames.length);
				for (String beanName : beanNames) {
					Object beanInstance = getBean(beanName);
					if (!(beanInstance instanceof NullBean)) {
						matchingBeans.put(beanName, (T) beanInstance);
					}
				}
				Stream<T> stream = matchingBeans.values().stream();
				return stream.sorted(adaptOrderComparator(matchingBeans));
			}
			@SuppressWarnings("unchecked")
			@Override
			public Stream<T> stream(Predicate<Class<?>> customFilter, boolean includeNonSingletons) {
				return Arrays.stream(beanNamesForStream(requiredType, includeNonSingletons, allowEagerInit))
						.filter(name -> customFilter.test(getType(name)))
						.map(name -> (T) getBean(name))
						.filter(bean -> !(bean instanceof NullBean));
			}
			@SuppressWarnings("unchecked")
			@Override
			public Stream<T> orderedStream(Predicate<Class<?>> customFilter, boolean includeNonSingletons) {
				String[] beanNames = beanNamesForStream(requiredType, includeNonSingletons, allowEagerInit);
				if (beanNames.length == 0) {
					return Stream.empty();
				}
				Map<String, T> matchingBeans = CollectionUtils.newLinkedHashMap(beanNames.length);
				for (String beanName : beanNames) {
					if (customFilter.test(getType(beanName))) {
						Object beanInstance = getBean(beanName);
						if (!(beanInstance instanceof NullBean)) {
							matchingBeans.put(beanName, (T) beanInstance);
						}
					}
				}
				return matchingBeans.values().stream().sorted(adaptOrderComparator(matchingBeans));
			}
		};
	}

	@Nullable
	private <T> T resolveBean(ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) {
		NamedBeanHolder<T> namedBean = resolveNamedBean(requiredType, args, nonUniqueAsNull);
		if (namedBean != null) {
			return namedBean.getBeanInstance();
		}
		BeanFactory parent = getParentBeanFactory();
		if (parent instanceof DefaultListableBeanFactory dlbf) {
			return dlbf.resolveBean(requiredType, args, nonUniqueAsNull);
		}
		else if (parent != null) {
			ObjectProvider<T> parentProvider = parent.getBeanProvider(requiredType);
			if (args != null) {
				return parentProvider.getObject(args);
			}
			else {
				return (nonUniqueAsNull ? parentProvider.getIfUnique() : parentProvider.getIfAvailable());
			}
		}
		return null;
	}

	private String[] beanNamesForStream(ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
		return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this, requiredType, includeNonSingletons, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type) {
		return getBeanNamesForType(type, true, true);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
		Class<?> resolved = type.resolve();
		if (resolved != null && !type.hasGenerics()) {
			return getBeanNamesForType(resolved, includeNonSingletons, allowEagerInit);
		}
		else {
			return doGetBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		}
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type) {
		return getBeanNamesForType(type, true, true);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		if (!isConfigurationFrozen() || type == null || !allowEagerInit) {
			return doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, allowEagerInit);
		}
		Map<Class<?>, String[]> cache =
				(includeNonSingletons ? this.allBeanNamesByType : this.singletonBeanNamesByType);
		String[] resolvedBeanNames = cache.get(type);
		if (resolvedBeanNames != null) {
			return resolvedBeanNames;
		}
		resolvedBeanNames = doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, true);
		if (ClassUtils.isCacheSafe(type, getBeanClassLoader())) {
			cache.put(type, resolvedBeanNames);
		}
		return resolvedBeanNames;
	}

	private String[] doGetBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
		List<String> result = new ArrayList<>();

		// Check all bean definitions.
		for (String beanName : this.beanDefinitionNames) {
			// Only consider bean as eligible if the bean name is not defined as alias for some other bean.
			if (!isAlias(beanName)) {
				try {
					RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
					// Only check bean definition if it is complete.
					if (!mbd.isAbstract() && (allowEagerInit ||
							(mbd.hasBeanClass() || !mbd.isLazyInit() || isAllowEagerClassLoading()) &&
									!requiresEagerInitForType(mbd.getFactoryBeanName()))) {
						boolean isFactoryBean = isFactoryBean(beanName, mbd);
						BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
						boolean matchFound = false;
						boolean allowFactoryBeanInit = (allowEagerInit || containsSingleton(beanName));
						boolean isNonLazyDecorated = (dbd != null && !mbd.isLazyInit());
						if (!isFactoryBean) {
							if (includeNonSingletons || isSingleton(beanName, mbd, dbd)) {
								matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
							}
						}
						else {
							if (includeNonSingletons || isNonLazyDecorated) {
								matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
							}
							else if (allowFactoryBeanInit) {
								// Type check before singleton check, avoiding FactoryBean instantiation
								// for early FactoryBean.isSingleton() calls on non-matching beans.
								matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit) &&
										isSingleton(beanName, mbd, dbd);
							}
							if (!matchFound) {
								// In case of FactoryBean, try to match FactoryBean instance itself next.
								beanName = FACTORY_BEAN_PREFIX + beanName;
								if (includeNonSingletons || isSingleton(beanName, mbd, dbd)) {
									matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
								}
							}
						}
						if (matchFound) {
							result.add(beanName);
						}
					}
				}
				catch (CannotLoadBeanClassException | BeanDefinitionStoreException ex) {
					if (allowEagerInit) {
						throw ex;
					}
					// Probably a placeholder: let's ignore it for type matching purposes.
					LogMessage message = (ex instanceof CannotLoadBeanClassException ?
							LogMessage.format("Ignoring bean class loading failure for bean '%s'", beanName) :
							LogMessage.format("Ignoring unresolvable metadata in bean definition '%s'", beanName));
					logger.trace(message, ex);
					// Register exception, in case the bean was accidentally unresolvable.
					onSuppressedException(ex);
				}
				catch (NoSuchBeanDefinitionException ex) {
					// Bean definition got removed while we were iterating -> ignore.
				}
			}
		}

		// Check manually registered singletons too.
		for (String beanName : this.manualSingletonNames) {
			try {
				// In case of FactoryBean, match object created by FactoryBean.
				if (isFactoryBean(beanName)) {
					if ((includeNonSingletons || isSingleton(beanName)) && isTypeMatch(beanName, type)) {
						result.add(beanName);
						// Match found for this bean: do not match FactoryBean itself anymore.
						continue;
					}
					// In case of FactoryBean, try to match FactoryBean itself next.
					beanName = FACTORY_BEAN_PREFIX + beanName;
				}
				// Match raw bean instance (might be raw FactoryBean).
				if (isTypeMatch(beanName, type)) {
					result.add(beanName);
				}
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Shouldn't happen - probably a result of circular reference resolution...
				logger.trace(LogMessage.format(
						"Failed to check manually registered singleton with name '%s'", beanName), ex);
			}
		}

		return StringUtils.toStringArray(result);
	}

	private boolean isSingleton(String beanName, RootBeanDefinition mbd, @Nullable BeanDefinitionHolder dbd) {
		return (dbd != null ? mbd.isSingleton() : isSingleton(beanName));
	}

	/**
	 * 检查指定的 bean 是否需要急切初始化以确定其类型。
	 * @param factoryBeanName bean 定义为其定义工厂方法的工厂 bean 引用
	 * @return 是否需要急切初始化
	 */
	private boolean requiresEagerInitForType(@Nullable String factoryBeanName) {
		return (factoryBeanName != null && isFactoryBean(factoryBeanName) && !containsSingleton(factoryBeanName));
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
		return getBeansOfType(type, true, true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Map<String, T> getBeansOfType(
			@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {

		String[] beanNames = getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		Map<String, T> result = CollectionUtils.newLinkedHashMap(beanNames.length);
		for (String beanName : beanNames) {
			try {
				Object beanInstance = getBean(beanName);
				if (!(beanInstance instanceof NullBean)) {
					result.put(beanName, (T) beanInstance);
				}
			}
			catch (BeanCreationException ex) {
				Throwable rootCause = ex.getMostSpecificCause();
				if (rootCause instanceof BeanCurrentlyInCreationException bce) {
					String exBeanName = bce.getBeanName();
					if (exBeanName != null && isCurrentlyInCreation(exBeanName)) {
						if (logger.isTraceEnabled()) {
							logger.trace("Ignoring match to currently created bean '" + exBeanName + "': " +
									ex.getMessage());
						}
						onSuppressedException(ex);
						// Ignore: indicates a circular reference when autowiring constructors.
						// We want to find matches other than the currently created bean itself.
						continue;
					}
				}
				throw ex;
			}
		}
		return result;
	}

	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		List<String> result = new ArrayList<>();
		for (String beanName : this.beanDefinitionNames) {
			BeanDefinition bd = this.beanDefinitionMap.get(beanName);
			if (bd != null && !bd.isAbstract() && findAnnotationOnBean(beanName, annotationType) != null) {
				result.add(beanName);
			}
		}
		for (String beanName : this.manualSingletonNames) {
			if (!result.contains(beanName) && findAnnotationOnBean(beanName, annotationType) != null) {
				result.add(beanName);
			}
		}
		return StringUtils.toStringArray(result);
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
		String[] beanNames = getBeanNamesForAnnotation(annotationType);
		Map<String, Object> result = CollectionUtils.newLinkedHashMap(beanNames.length);
		for (String beanName : beanNames) {
			Object beanInstance = getBean(beanName);
			if (!(beanInstance instanceof NullBean)) {
				result.put(beanName, beanInstance);
			}
		}
		return result;
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException {

		return findAnnotationOnBean(beanName, annotationType, true);
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {

		Class<?> beanType = getType(beanName, allowFactoryBeanInit);
		if (beanType != null) {
			MergedAnnotation<A> annotation =
					MergedAnnotations.from(beanType, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
			if (annotation.isPresent()) {
				return annotation.synthesize();
			}
		}
		if (containsBeanDefinition(beanName)) {
			RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
			// Check raw bean class, for example, in case of a proxy.
			if (bd.hasBeanClass() && bd.getFactoryMethodName() == null) {
				Class<?> beanClass = bd.getBeanClass();
				if (beanClass != beanType) {
					MergedAnnotation<A> annotation =
							MergedAnnotations.from(beanClass, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
					if (annotation.isPresent()) {
						return annotation.synthesize();
					}
				}
			}
			// Check annotations declared on factory method, if any.
			Method factoryMethod = bd.getResolvedFactoryMethod();
			if (factoryMethod != null) {
				MergedAnnotation<A> annotation =
						MergedAnnotations.from(factoryMethod, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
				if (annotation.isPresent()) {
					return annotation.synthesize();
				}
			}
		}
		return null;
	}

	@Override
	public <A extends Annotation> Set<A> findAllAnnotationsOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {

		Set<A> annotations = new LinkedHashSet<>();
		Class<?> beanType = getType(beanName, allowFactoryBeanInit);
		if (beanType != null) {
			MergedAnnotations.from(beanType, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
					.stream(annotationType)
					.filter(MergedAnnotation::isPresent)
					.forEach(mergedAnnotation -> annotations.add(mergedAnnotation.synthesize()));
		}
		if (containsBeanDefinition(beanName)) {
			RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
			// Check raw bean class, for example, in case of a proxy.
			if (bd.hasBeanClass() && bd.getFactoryMethodName() == null) {
				Class<?> beanClass = bd.getBeanClass();
				if (beanClass != beanType) {
					MergedAnnotations.from(beanClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
							.stream(annotationType)
							.filter(MergedAnnotation::isPresent)
							.forEach(mergedAnnotation -> annotations.add(mergedAnnotation.synthesize()));
				}
			}
			// Check annotations declared on factory method, if any.
			Method factoryMethod = bd.getResolvedFactoryMethod();
			if (factoryMethod != null) {
				MergedAnnotations.from(factoryMethod, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
						.stream(annotationType)
						.filter(MergedAnnotation::isPresent)
						.forEach(mergedAnnotation -> annotations.add(mergedAnnotation.synthesize()));
			}
		}
		return annotations;
	}


	//---------------------------------------------------------------------
	// ConfigurableListableBeanFactory 接口的实现
	//---------------------------------------------------------------------

	@Override
	public void registerResolvableDependency(Class<?> dependencyType, @Nullable Object autowiredValue) {
		Assert.notNull(dependencyType, "Dependency type must not be null");
		if (autowiredValue != null) {
			if (!(autowiredValue instanceof ObjectFactory || dependencyType.isInstance(autowiredValue))) {
				throw new IllegalArgumentException("Value [" + autowiredValue +
						"] does not implement specified dependency type [" + dependencyType.getName() + "]");
			}
			this.resolvableDependencies.put(dependencyType, autowiredValue);
		}
	}

	@Override
	public boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
			throws NoSuchBeanDefinitionException {

		return isAutowireCandidate(beanName, descriptor, getAutowireCandidateResolver());
	}

	/**
	 * 确定指定的 bean 定义是否符合自动装配候选者的条件，以便注入到声明了匹配类型依赖的其他 bean 中。
	 * @param beanName 要检查的 bean 定义的名称
	 * @param descriptor 要解析的依赖项描述符
	 * @param resolver 用于实际解析算法的 AutowireCandidateResolver
	 * @return 该 bean 是否应被视为自动装配候选者
	 */
	protected boolean isAutowireCandidate(
			String beanName, DependencyDescriptor descriptor, AutowireCandidateResolver resolver)
			throws NoSuchBeanDefinitionException {

		String bdName = transformedBeanName(beanName);
		if (containsBeanDefinition(bdName)) {
			return isAutowireCandidate(beanName, getMergedLocalBeanDefinition(bdName), descriptor, resolver);
		}
		else if (containsSingleton(beanName)) {
			return isAutowireCandidate(beanName, new RootBeanDefinition(getType(beanName)), descriptor, resolver);
		}

		BeanFactory parent = getParentBeanFactory();
		if (parent instanceof DefaultListableBeanFactory dlbf) {
			// No bean definition found in this factory -> delegate to parent.
			return dlbf.isAutowireCandidate(beanName, descriptor, resolver);
		}
		else if (parent instanceof ConfigurableListableBeanFactory clbf) {
			// If no DefaultListableBeanFactory, can't pass the resolver along.
			return clbf.isAutowireCandidate(beanName, descriptor);
		}
		else {
			return true;
		}
	}

	/**
	 * 确定指定的 bean 定义是否符合自动装配候选者的条件，以便注入到声明了匹配类型依赖的其他 bean 中。
	 * @param beanName 要检查的 bean 定义的名称
	 * @param mbd 要检查的合并后的 bean 定义
	 * @param descriptor 要解析的依赖项描述符
	 * @param resolver 用于实际解析算法的 AutowireCandidateResolver
	 * @return 该 bean 是否应被视为自动装配候选者
	 */
	protected boolean isAutowireCandidate(String beanName, RootBeanDefinition mbd,
			DependencyDescriptor descriptor, AutowireCandidateResolver resolver) {

		String bdName = transformedBeanName(beanName);
		resolveBeanClass(mbd, bdName);
		if (mbd.isFactoryMethodUnique && mbd.factoryMethodToIntrospect == null) {
			new ConstructorResolver(this).resolveFactoryMethodIfPossible(mbd);
		}
		BeanDefinitionHolder holder = (beanName.equals(bdName) ?
				this.mergedBeanDefinitionHolders.computeIfAbsent(beanName,
						key -> new BeanDefinitionHolder(mbd, beanName, getAliases(bdName))) :
				new BeanDefinitionHolder(mbd, beanName, getAliases(bdName)));
		return resolver.isAutowireCandidate(holder, descriptor);
	}

	@Override
	public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		BeanDefinition bd = this.beanDefinitionMap.get(beanName);
		if (bd == null) {
			if (logger.isTraceEnabled()) {
				logger.trace("No bean named '" + beanName + "' found in " + this);
			}
			throw new NoSuchBeanDefinitionException(beanName);
		}
		return bd;
	}

	@Override
	public Iterator<String> getBeanNamesIterator() {
		CompositeIterator<String> iterator = new CompositeIterator<>();
		iterator.add(this.beanDefinitionNames.iterator());
		iterator.add(this.manualSingletonNames.iterator());
		return iterator;
	}

	@Override
	protected void clearMergedBeanDefinition(String beanName) {
		super.clearMergedBeanDefinition(beanName);
		this.mergedBeanDefinitionHolders.remove(beanName);
	}

	@Override
	public void clearMetadataCache() {
		super.clearMetadataCache();
		this.mergedBeanDefinitionHolders.clear();
		clearByTypeCache();
	}

	@Override
	public void freezeConfiguration() {
		clearMetadataCache();
		this.configurationFrozen = true;
		this.frozenBeanDefinitionNames = StringUtils.toStringArray(this.beanDefinitionNames);
	}

	@Override
	public boolean isConfigurationFrozen() {
		return this.configurationFrozen;
	}

	/**
	 * 如果工厂的配置已被标记为冻结，则认为所有 bean 都符合元数据缓存的条件。
	 * @see #freezeConfiguration()
	 */
	@Override
	protected boolean isBeanEligibleForMetadataCaching(String beanName) {
		return (this.configurationFrozen || super.isBeanEligibleForMetadataCaching(beanName));
	}

	@Override
	@Nullable
	protected Object obtainInstanceFromSupplier(Supplier<?> supplier, String beanName, RootBeanDefinition mbd)
			throws Exception {

		if (supplier instanceof InstanceSupplier<?> instanceSupplier) {
			return instanceSupplier.get(RegisteredBean.of(this, beanName, mbd));
		}
		return super.obtainInstanceFromSupplier(supplier, beanName, mbd);
	}

	@Override
	protected void cacheMergedBeanDefinition(RootBeanDefinition mbd, String beanName) {
		super.cacheMergedBeanDefinition(mbd, beanName);
		if (mbd.isPrimary()) {
			this.primaryBeanNamesWithType.put(beanName, Void.class);
		}
	}

	@Override
	protected void checkMergedBeanDefinition(RootBeanDefinition mbd, String beanName, @Nullable Object[] args) {
		super.checkMergedBeanDefinition(mbd, beanName, args);

		if (mbd.isBackgroundInit()) {
			if (this.preInstantiationThread.get() == PreInstantiation.MAIN && getBootstrapExecutor() != null) {
				throw new BeanCurrentlyInCreationException(beanName, "Bean marked for background " +
						"initialization but requested in mainline thread - declare ObjectProvider " +
						"or lazy injection point in dependent mainline beans");
			}
		}
		else {
			// Bean intended to be initialized in main bootstrap thread.
			if (this.preInstantiationThread.get() == PreInstantiation.BACKGROUND) {
				throw new BeanCurrentlyInCreationException(beanName, "Bean marked for mainline initialization " +
						"but requested in background thread - enforce early instantiation in mainline thread " +
						"through depends-on '" + beanName + "' declaration for dependent background beans");
			}
		}
	}

	@Override
	@Nullable
	protected Boolean isCurrentThreadAllowedToHoldSingletonLock() {
		String mainThreadPrefix = this.mainThreadPrefix;
		if (mainThreadPrefix != null) {
			// We only differentiate in the preInstantiateSingletons phase, using
			// the volatile mainThreadPrefix field as an indicator for that phase.

			PreInstantiation preInstantiation = this.preInstantiationThread.get();
			if (preInstantiation != null) {
				// A Spring-managed bootstrap thread:
				// MAIN is allowed to lock (true) or even forced to lock (null),
				// BACKGROUND is never allowed to lock (false).
				return switch (preInstantiation) {
					case MAIN -> (Boolean.TRUE.equals(this.strictLocking) ? null : true);
					case BACKGROUND -> false;
				};
			}

			// Not a Spring-managed bootstrap thread...
			if (Boolean.FALSE.equals(this.strictLocking)) {
				// Explicitly configured to use lenient locking wherever possible.
				return true;
			}
			else if (this.strictLocking == null) {
				// No explicit locking configuration -> infer appropriate locking.
				if (!getThreadNamePrefix().equals(mainThreadPrefix)) {
					// An unmanaged thread (assumed to be application-internal) with lenient locking,
					// and not part of the same thread pool that provided the main bootstrap thread
					// (excluding scenarios where we are hit by multiple external bootstrap threads).
					return true;
				}
			}
		}

		// Traditional behavior: forced to always hold a full lock.
		return null;
	}

	@Override
	public void prepareSingletonBootstrap() {
		this.mainThreadPrefix = getThreadNamePrefix();
	}

	@Override
	public void preInstantiateSingletons() throws BeansException {
		if (logger.isTraceEnabled()) {
			logger.trace("Pre-instantiating singletons in " + this);
		}

		// Iterate over a copy to allow for init methods which in turn register new bean definitions.
		// While this may not be part of the regular factory bootstrap, it does otherwise work fine.
		List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

		// Trigger initialization of all non-lazy singleton beans...
		this.preInstantiationThread.set(PreInstantiation.MAIN);
		if (this.mainThreadPrefix == null) {
			this.mainThreadPrefix = getThreadNamePrefix();
		}
		try {
			List<CompletableFuture<?>> futures = new ArrayList<>();
			for (String beanName : beanNames) {
				RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
				if (!mbd.isAbstract() && mbd.isSingleton()) {
					CompletableFuture<?> future = preInstantiateSingleton(beanName, mbd);
					if (future != null) {
						futures.add(future);
					}
				}
			}
			if (!futures.isEmpty()) {
				try {
					CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
				}
				catch (CompletionException ex) {
					ReflectionUtils.rethrowRuntimeException(ex.getCause());
				}
			}
		}
		finally {
			this.mainThreadPrefix = null;
			this.preInstantiationThread.remove();
		}

		// Trigger post-initialization callback for all applicable beans...
		for (String beanName : beanNames) {
			Object singletonInstance = getSingleton(beanName, false);
			if (singletonInstance instanceof SmartInitializingSingleton smartSingleton) {
				StartupStep smartInitialize = getApplicationStartup().start("spring.beans.smart-initialize")
						.tag("beanName", beanName);
				smartSingleton.afterSingletonsInstantiated();
				smartInitialize.end();
			}
		}
	}

	@Nullable
	private CompletableFuture<?> preInstantiateSingleton(String beanName, RootBeanDefinition mbd) {
		if (mbd.isBackgroundInit()) {
			Executor executor = getBootstrapExecutor();
			if (executor != null) {
				String[] dependsOn = mbd.getDependsOn();
				if (dependsOn != null) {
					for (String dep : dependsOn) {
						getBean(dep);
					}
				}
				CompletableFuture<?> future = CompletableFuture.runAsync(
						() -> instantiateSingletonInBackgroundThread(beanName), executor);
				addSingletonFactory(beanName, () -> {
					try {
						future.join();
					}
					catch (CompletionException ex) {
						ReflectionUtils.rethrowRuntimeException(ex.getCause());
					}
					return future;  // not to be exposed, just to lead to ClassCastException in case of mismatch
				});
				return (!mbd.isLazyInit() ? future : null);
			}
			else if (logger.isInfoEnabled()) {
				logger.info("Bean '" + beanName + "' marked for background initialization " +
						"without bootstrap executor configured - falling back to mainline initialization");
			}
		}

		if (!mbd.isLazyInit()) {
			try {
				instantiateSingleton(beanName);
			}
			catch (BeanCurrentlyInCreationException ex) {
				logger.info("Bean '" + beanName + "' marked for pre-instantiation (not lazy-init) " +
						"but currently initialized by other thread - skipping it in mainline thread");
			}
		}
		return null;
	}

	private void instantiateSingletonInBackgroundThread(String beanName) {
		this.preInstantiationThread.set(PreInstantiation.BACKGROUND);
		try {
			instantiateSingleton(beanName);
		}
		catch (RuntimeException | Error ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to instantiate singleton bean '" + beanName + "' in background thread", ex);
			}
			throw ex;
		}
		finally {
			this.preInstantiationThread.remove();
		}
	}

	private void instantiateSingleton(String beanName) {
		if (isFactoryBean(beanName)) {
			Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
			if (bean instanceof SmartFactoryBean<?> smartFactoryBean && smartFactoryBean.isEagerInit()) {
				getBean(beanName);
			}
		}
		else {
			getBean(beanName);
		}
	}

	private static String getThreadNamePrefix() {
		String name = Thread.currentThread().getName();
		int numberSeparator = name.lastIndexOf('-');
		return (numberSeparator >= 0 ? name.substring(0, numberSeparator) : name);
	}


	//---------------------------------------------------------------------
	// BeanDefinitionRegistry 接口的实现
	//---------------------------------------------------------------------

	@Override
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {

		Assert.hasText(beanName, "Bean name must not be empty");
		Assert.notNull(beanDefinition, "BeanDefinition must not be null");

		if (beanDefinition instanceof AbstractBeanDefinition abd) {
			try {
				abd.validate();
			}
			catch (BeanDefinitionValidationException ex) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
						"Validation of bean definition failed", ex);
			}
		}

		BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
		if (existingDefinition != null) {
			if (!isBeanDefinitionOverridable(beanName)) {
				throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);
			}
			else {
				logBeanDefinitionOverriding(beanName, beanDefinition, existingDefinition);
			}
			this.beanDefinitionMap.put(beanName, beanDefinition);
		}
		else {
			if (isAlias(beanName)) {
				String aliasedName = canonicalName(beanName);
				if (!isBeanDefinitionOverridable(aliasedName)) {
					if (containsBeanDefinition(aliasedName)) {  // alias for existing bean definition
						throw new BeanDefinitionOverrideException(
								beanName, beanDefinition, getBeanDefinition(aliasedName));
					}
					else {  // alias pointing to non-existing bean definition
						throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
								"Cannot register bean definition for bean '" + beanName +
								"' since there is already an alias for bean '" + aliasedName + "' bound.");
					}
				}
				else {
					if (logger.isInfoEnabled()) {
						logger.info("Removing alias '" + beanName + "' for bean '" + aliasedName +
								"' due to registration of bean definition for bean '" + beanName + "': [" +
								beanDefinition + "]");
					}
					removeAlias(beanName);
				}
			}
			if (hasBeanCreationStarted()) {
				// Cannot modify startup-time collection elements anymore (for stable iteration)
				synchronized (this.beanDefinitionMap) {
					this.beanDefinitionMap.put(beanName, beanDefinition);
					List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
					updatedDefinitions.addAll(this.beanDefinitionNames);
					updatedDefinitions.add(beanName);
					this.beanDefinitionNames = updatedDefinitions;
					removeManualSingletonName(beanName);
				}
			}
			else {
				// Still in startup registration phase
				this.beanDefinitionMap.put(beanName, beanDefinition);
				this.beanDefinitionNames.add(beanName);
				removeManualSingletonName(beanName);
			}
			this.frozenBeanDefinitionNames = null;
		}

		if (existingDefinition != null || containsSingleton(beanName)) {
			resetBeanDefinition(beanName);
		}
		else if (isConfigurationFrozen()) {
			clearByTypeCache();
		}

		// Cache a primary marker for the given bean.
		if (beanDefinition.isPrimary()) {
			this.primaryBeanNamesWithType.put(beanName, Void.class);
		}
	}

	private void logBeanDefinitionOverriding(String beanName, BeanDefinition beanDefinition,
			BeanDefinition existingDefinition) {

		boolean explicitBeanOverride = (this.allowBeanDefinitionOverriding != null);
		if (existingDefinition.getRole() < beanDefinition.getRole()) {
			// for example, was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
			if (logger.isInfoEnabled()) {
				logger.info("Overriding user-defined bean definition for bean '" + beanName +
						"' with a framework-generated bean definition: replacing [" +
						existingDefinition + "] with [" + beanDefinition + "]");
			}
		}
		else if (!beanDefinition.equals(existingDefinition)) {
			if (explicitBeanOverride && logger.isInfoEnabled()) {
				logger.info("Overriding bean definition for bean '" + beanName +
						"' with a different definition: replacing [" + existingDefinition +
						"] with [" + beanDefinition + "]");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Overriding bean definition for bean '" + beanName +
						"' with a different definition: replacing [" + existingDefinition +
						"] with [" + beanDefinition + "]");
			}
		}
		else {
			if (explicitBeanOverride && logger.isInfoEnabled()) {
				logger.info("Overriding bean definition for bean '" + beanName +
						"' with an equivalent definition: replacing [" + existingDefinition +
						"] with [" + beanDefinition + "]");
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Overriding bean definition for bean '" + beanName +
						"' with an equivalent definition: replacing [" + existingDefinition +
						"] with [" + beanDefinition + "]");
			}
		}
	}

	@Override
	public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		Assert.hasText(beanName, "'beanName' must not be empty");

		BeanDefinition bd = this.beanDefinitionMap.remove(beanName);
		if (bd == null) {
			if (logger.isTraceEnabled()) {
				logger.trace("No bean named '" + beanName + "' found in " + this);
			}
			throw new NoSuchBeanDefinitionException(beanName);
		}

		if (hasBeanCreationStarted()) {
			// Cannot modify startup-time collection elements anymore (for stable iteration)
			synchronized (this.beanDefinitionMap) {
				List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames);
				updatedDefinitions.remove(beanName);
				this.beanDefinitionNames = updatedDefinitions;
			}
		}
		else {
			// Still in startup registration phase
			this.beanDefinitionNames.remove(beanName);
		}
		this.frozenBeanDefinitionNames = null;

		resetBeanDefinition(beanName);
	}

	/**
	 * 重置给定 bean 的所有 bean 定义缓存，包括从该 bean 派生的 bean 的缓存。
	 * <p>
	 *     在现有 bean 定义被替换或移除后调用，
	 *     触发 {@link #clearMergedBeanDefinition}、{@link #destroySingleton} 以及 {@link MergedBeanDefinitionPostProcessor#resetBeanDefinition}，
	 *     作用于给定 bean 以及所有以该 bean 为父级的 bean 定义。
	 *
	 * @param beanName 要重置的 bean 的名称
	 * @see #registerBeanDefinition
	 * @see #removeBeanDefinition
	 */
	protected void resetBeanDefinition(String beanName) {
		// Remove the merged bean definition for the given bean, if already created.
		clearMergedBeanDefinition(beanName);

		// Remove corresponding bean from singleton cache, if any. Shouldn't usually
		// be necessary, rather just meant for overriding a context's default beans
		// (for example, the default StaticMessageSource in a StaticApplicationContext).
		destroySingleton(beanName);

		// Remove a cached primary marker for the given bean.
		this.primaryBeanNamesWithType.remove(beanName);

		// Notify all post-processors that the specified bean definition has been reset.
		for (MergedBeanDefinitionPostProcessor processor : getBeanPostProcessorCache().mergedDefinition) {
			processor.resetBeanDefinition(beanName);
		}

		// Reset all bean definitions that have the given bean as parent (recursively).
		for (String bdName : this.beanDefinitionNames) {
			if (!beanName.equals(bdName)) {
				BeanDefinition bd = this.beanDefinitionMap.get(bdName);
				// Ensure bd is non-null due to potential concurrent modification of beanDefinitionMap.
				if (bd != null && beanName.equals(bd.getParentName())) {
					resetBeanDefinition(bdName);
				}
			}
		}
	}

	/**
	 * 如果通常允许覆盖 bean 定义，则此实现返回 {@code true}。
	 * @see #setAllowBeanDefinitionOverriding
	 */
	@Override
	public boolean isBeanDefinitionOverridable(String beanName) {
		return isAllowBeanDefinitionOverriding();
	}

	/**
	 * 仅在允许 bean 定义覆盖时才允许别名覆盖。
	 * @see #setAllowBeanDefinitionOverriding
	 */
	@Override
	protected boolean allowAliasOverriding() {
		return isAllowBeanDefinitionOverriding();
	}

	/**
	 * 同时检查别名是否覆盖了同名的 bean 定义。
	 */
	@Override
	protected void checkForAliasCircle(String name, String alias) {
		super.checkForAliasCircle(name, alias);
		if (!isBeanDefinitionOverridable(alias) && containsBeanDefinition(alias)) {
			throw new IllegalStateException("Cannot register alias '" + alias +
					"' for name '" + name + "': Alias would override bean definition '" + alias + "'");
		}
	}

	@Override
	protected void addSingleton(String beanName, Object singletonObject) {
		super.addSingleton(beanName, singletonObject);

		Predicate<Class<?>> filter = (beanType -> beanType != Object.class && beanType.isInstance(singletonObject));
		this.allBeanNamesByType.keySet().removeIf(filter);
		this.singletonBeanNamesByType.keySet().removeIf(filter);

		if (this.primaryBeanNamesWithType.containsKey(beanName) && singletonObject.getClass() != NullBean.class) {
			Class<?> beanType = (singletonObject instanceof FactoryBean<?> fb ?
					getTypeForFactoryBean(fb) : singletonObject.getClass());
			if (beanType != null) {
				this.primaryBeanNamesWithType.put(beanName, beanType);
			}
		}
	}

	@Override
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		super.registerSingleton(beanName, singletonObject);

		updateManualSingletonNames(set -> set.add(beanName), set -> !this.beanDefinitionMap.containsKey(beanName));
		this.allBeanNamesByType.remove(Object.class);
		this.singletonBeanNamesByType.remove(Object.class);
	}

	@Override
	public void destroySingletons() {
		super.destroySingletons();
		updateManualSingletonNames(Set::clear, set -> !set.isEmpty());
		clearByTypeCache();
	}

	@Override
	public void destroySingleton(String beanName) {
		super.destroySingleton(beanName);
		removeManualSingletonName(beanName);
		clearByTypeCache();
	}

	private void removeManualSingletonName(String beanName) {
		updateManualSingletonNames(set -> set.remove(beanName), set -> set.contains(beanName));
	}

	/**
	 * 更新工厂内部的手动单例名称集合。
	 * @param action 修改操作
	 * @param condition 修改操作的前置条件（如果此条件不满足，则可以跳过该操作）
	 */
	private void updateManualSingletonNames(Consumer<Set<String>> action, Predicate<Set<String>> condition) {
		if (hasBeanCreationStarted()) {
			// Cannot modify startup-time collection elements anymore (for stable iteration)
			synchronized (this.beanDefinitionMap) {
				if (condition.test(this.manualSingletonNames)) {
					Set<String> updatedSingletons = new LinkedHashSet<>(this.manualSingletonNames);
					action.accept(updatedSingletons);
					this.manualSingletonNames = updatedSingletons;
				}
			}
		}
		else {
			// Still in startup registration phase
			if (condition.test(this.manualSingletonNames)) {
				action.accept(this.manualSingletonNames);
			}
		}
	}

	/**
	 * 移除关于按类型映射的所有假设。
	 */
	private void clearByTypeCache() {
		this.allBeanNamesByType.clear();
		this.singletonBeanNamesByType.clear();
	}


	//---------------------------------------------------------------------
	// 依赖解析功能
	//---------------------------------------------------------------------

	@Override
	public <T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException {
		Assert.notNull(requiredType, "Required type must not be null");
		NamedBeanHolder<T> namedBean = resolveNamedBean(ResolvableType.forRawClass(requiredType), null, false);
		if (namedBean != null) {
			return namedBean;
		}
		BeanFactory parent = getParentBeanFactory();
		if (parent instanceof AutowireCapableBeanFactory acbf) {
			return acbf.resolveNamedBean(requiredType);
		}
		throw new NoSuchBeanDefinitionException(requiredType);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private <T> NamedBeanHolder<T> resolveNamedBean(
			ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) throws BeansException {

		Assert.notNull(requiredType, "Required type must not be null");
		String[] candidateNames = getBeanNamesForType(requiredType);

		if (candidateNames.length > 1) {
			List<String> autowireCandidates = new ArrayList<>(candidateNames.length);
			for (String beanName : candidateNames) {
				if (!containsBeanDefinition(beanName) || getBeanDefinition(beanName).isAutowireCandidate()) {
					autowireCandidates.add(beanName);
				}
			}
			if (!autowireCandidates.isEmpty()) {
				candidateNames = StringUtils.toStringArray(autowireCandidates);
			}
		}

		if (candidateNames.length == 1) {
			return resolveNamedBean(candidateNames[0], requiredType, args);
		}
		else if (candidateNames.length > 1) {
			Map<String, Object> candidates = CollectionUtils.newLinkedHashMap(candidateNames.length);
			for (String beanName : candidateNames) {
				if (containsSingleton(beanName) && args == null) {
					Object beanInstance = getBean(beanName);
					candidates.put(beanName, (beanInstance instanceof NullBean ? null : beanInstance));
				}
				else {
					candidates.put(beanName, getType(beanName));
				}
			}
			String candidateName = determinePrimaryCandidate(candidates, requiredType.toClass());
			if (candidateName == null) {
				candidateName = determineHighestPriorityCandidate(candidates, requiredType.toClass());
			}
			if (candidateName == null) {
				candidateName = determineDefaultCandidate(candidates);
			}
			if (candidateName != null) {
				Object beanInstance = candidates.get(candidateName);
				if (beanInstance == null) {
					return null;
				}
				if (beanInstance instanceof Class) {
					return resolveNamedBean(candidateName, requiredType, args);
				}
				return new NamedBeanHolder<>(candidateName, (T) beanInstance);
			}
			if (!nonUniqueAsNull) {
				throw new NoUniqueBeanDefinitionException(requiredType, candidates.keySet());
			}
		}

		return null;
	}

	@Nullable
	private <T> NamedBeanHolder<T> resolveNamedBean(
			String beanName, ResolvableType requiredType, @Nullable Object[] args) throws BeansException {

		Object bean = getBean(beanName, null, args);
		if (bean instanceof NullBean) {
			return null;
		}
		return new NamedBeanHolder<>(beanName, adaptBeanInstance(beanName, bean, requiredType.toClass()));
	}

	@Override
	@Nullable
	public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {

		descriptor.initParameterNameDiscovery(getParameterNameDiscoverer());
		if (Optional.class == descriptor.getDependencyType()) {
			return createOptionalDependency(descriptor, requestingBeanName);
		}
		else if (ObjectFactory.class == descriptor.getDependencyType() ||
				ObjectProvider.class == descriptor.getDependencyType()) {
			return new DependencyObjectProvider(descriptor, requestingBeanName);
		}
		else if (jakartaInjectProviderClass == descriptor.getDependencyType()) {
			return new Jsr330Factory().createDependencyProvider(descriptor, requestingBeanName);
		}
		else if (descriptor.supportsLazyResolution()) {
			Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(
					descriptor, requestingBeanName);
			if (result != null) {
				return result;
			}
		}
		return doResolveDependency(descriptor, requestingBeanName, autowiredBeanNames, typeConverter);
	}

	@SuppressWarnings("NullAway")  // Dataflow analysis limitation
	@Nullable
	public Object doResolveDependency(DependencyDescriptor descriptor, @Nullable String beanName,
			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {

		InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
		try {
			// Step 1: pre-resolved shortcut for single bean match, for example, from @Autowired
			Object shortcut = descriptor.resolveShortcut(this);
			if (shortcut != null) {
				return shortcut;
			}

			Class<?> type = descriptor.getDependencyType();

			// Step 2: pre-defined value or expression, for example, from @Value
			Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);
			if (value != null) {
				if (value instanceof String strValue) {
					String resolvedValue = resolveEmbeddedValue(strValue);
					BeanDefinition bd = (beanName != null && containsBean(beanName) ?
							getMergedBeanDefinition(beanName) : null);
					value = evaluateBeanDefinitionString(resolvedValue, bd);
				}
				TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
				try {
					return converter.convertIfNecessary(value, type, descriptor.getTypeDescriptor());
				}
				catch (UnsupportedOperationException ex) {
					// A custom TypeConverter which does not support TypeDescriptor resolution...
					return (descriptor.getField() != null ?
							converter.convertIfNecessary(value, type, descriptor.getField()) :
							converter.convertIfNecessary(value, type, descriptor.getMethodParameter()));
				}
			}

			// Step 3: shortcut for declared dependency name or qualifier-suggested name matching target bean name
			if (descriptor.usesStandardBeanLookup()) {
				String dependencyName = descriptor.getDependencyName();
				if (dependencyName == null || !containsBean(dependencyName)) {
					String suggestedName = getAutowireCandidateResolver().getSuggestedName(descriptor);
					dependencyName = (suggestedName != null && containsBean(suggestedName) ? suggestedName : null);
				}
				if (dependencyName != null) {
					dependencyName = canonicalName(dependencyName);  // dependency name can be alias of target name
					if (isTypeMatch(dependencyName, type) && isAutowireCandidate(dependencyName, descriptor) &&
							!isFallback(dependencyName) && !hasPrimaryConflict(dependencyName, type) &&
							!isSelfReference(beanName, dependencyName)) {
						if (autowiredBeanNames != null) {
							autowiredBeanNames.add(dependencyName);
						}
						boolean preExisting = containsSingleton(dependencyName);
						Object dependencyBean = getBean(dependencyName);
						if (preExisting && dependencyBean instanceof NullBean) {
							// for backwards compatibility with addCandidateEntry in the regular code path
							dependencyBean = null;
						}
						return resolveInstance(dependencyBean, descriptor, type, dependencyName);
					}
				}
			}

			// Step 4a: multiple beans as stream / array / standard collection / plain map
			Object multipleBeans = resolveMultipleBeans(descriptor, beanName, autowiredBeanNames, typeConverter);
			if (multipleBeans != null) {
				return multipleBeans;
			}
			// Step 4b: direct bean matches, possibly direct beans of type Collection / Map
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
			if (matchingBeans.isEmpty()) {
				// Step 4c (fallback): custom Collection / Map declarations for collecting multiple beans
				multipleBeans = resolveMultipleBeansFallback(descriptor, beanName, autowiredBeanNames, typeConverter);
				if (multipleBeans != null) {
					return multipleBeans;
				}
				// Raise exception if nothing found for required injection point
				if (isRequired(descriptor)) {
					raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
				}
				return null;
			}

			String autowiredBeanName;
			Object instanceCandidate;

			// Step 5: determine single candidate
			if (matchingBeans.size() > 1) {
				autowiredBeanName = determineAutowireCandidate(matchingBeans, descriptor);
				if (autowiredBeanName == null) {
					if (isRequired(descriptor) || !indicatesArrayCollectionOrMap(type)) {
						// Raise exception if no clear match found for required injection point
						return descriptor.resolveNotUnique(descriptor.getResolvableType(), matchingBeans);
					}
					else {
						// In case of an optional Collection/Map, silently ignore a non-unique case:
						// possibly it was meant to be an empty collection of multiple regular beans
						// (before 4.3 in particular when we didn't even look for collection beans).
						return null;
					}
				}
				instanceCandidate = matchingBeans.get(autowiredBeanName);
			}
			else {
				// We have exactly one match.
				Map.Entry<String, Object> entry = matchingBeans.entrySet().iterator().next();
				autowiredBeanName = entry.getKey();
				instanceCandidate = entry.getValue();
			}

			// Step 6: validate single result
			if (autowiredBeanNames != null) {
				autowiredBeanNames.add(autowiredBeanName);
			}
			if (instanceCandidate instanceof Class) {
				instanceCandidate = descriptor.resolveCandidate(autowiredBeanName, type, this);
			}
			return resolveInstance(instanceCandidate, descriptor, type, autowiredBeanName);
		}
		finally {
			ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
		}
	}

	@Nullable
	private Object resolveInstance(Object candidate, DependencyDescriptor descriptor, Class<?> type, String name) {
		Object result = candidate;
		if (result instanceof NullBean) {
			// Raise exception if null encountered for required injection point
			if (isRequired(descriptor)) {
				raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
			}
			result = null;
		}
		if (!ClassUtils.isAssignableValue(type, result)) {
			throw new BeanNotOfRequiredTypeException(name, type, candidate.getClass());
		}
		return result;
	}

	@Nullable
	private Object resolveMultipleBeans(DependencyDescriptor descriptor, @Nullable String beanName,
			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {

		Class<?> type = descriptor.getDependencyType();

		if (descriptor instanceof StreamDependencyDescriptor streamDependencyDescriptor) {
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
			if (autowiredBeanNames != null) {
				autowiredBeanNames.addAll(matchingBeans.keySet());
			}
			Stream<Object> stream = matchingBeans.keySet().stream()
					.map(name -> descriptor.resolveCandidate(name, type, this))
					.filter(bean -> !(bean instanceof NullBean));
			if (streamDependencyDescriptor.isOrdered()) {
				stream = stream.sorted(adaptOrderComparator(matchingBeans));
			}
			return stream;
		}
		else if (type.isArray()) {
			Class<?> componentType = type.componentType();
			ResolvableType resolvableType = descriptor.getResolvableType();
			Class<?> resolvedArrayType = resolvableType.resolve(type);
			if (resolvedArrayType != type) {
				componentType = resolvableType.getComponentType().resolve();
			}
			if (componentType == null) {
				return null;
			}
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, componentType,
					new MultiElementDescriptor(descriptor));
			if (matchingBeans.isEmpty()) {
				return null;
			}
			if (autowiredBeanNames != null) {
				autowiredBeanNames.addAll(matchingBeans.keySet());
			}
			TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
			Object result = converter.convertIfNecessary(matchingBeans.values(), resolvedArrayType);
			if (result instanceof Object[] array && array.length > 1) {
				Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
				if (comparator != null) {
					Arrays.sort(array, comparator);
				}
			}
			return result;
		}
		else if (Collection.class == type || Set.class == type || List.class == type) {
			return resolveMultipleBeanCollection(descriptor, beanName, autowiredBeanNames, typeConverter);
		}
		else if (Map.class == type) {
			return resolveMultipleBeanMap(descriptor, beanName, autowiredBeanNames, typeConverter);
		}
		return null;
	}


	@Nullable
	private Object resolveMultipleBeansFallback(DependencyDescriptor descriptor, @Nullable String beanName,
			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {

		Class<?> type = descriptor.getDependencyType();

		if (Collection.class.isAssignableFrom(type) && type.isInterface()) {
			return resolveMultipleBeanCollection(descriptor, beanName, autowiredBeanNames, typeConverter);
		}
		else if (Map.class.isAssignableFrom(type) && type.isInterface()) {
			return resolveMultipleBeanMap(descriptor, beanName, autowiredBeanNames, typeConverter);
		}
		return null;
	}

	@Nullable
	private Object resolveMultipleBeanCollection(DependencyDescriptor descriptor, @Nullable String beanName,
			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {

		Class<?> elementType = descriptor.getResolvableType().asCollection().resolveGeneric();
		if (elementType == null) {
			return null;
		}
		Map<String, Object> matchingBeans = findAutowireCandidates(beanName, elementType,
				new MultiElementDescriptor(descriptor));
		if (matchingBeans.isEmpty()) {
			return null;
		}
		if (autowiredBeanNames != null) {
			autowiredBeanNames.addAll(matchingBeans.keySet());
		}
		TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
		Object result = converter.convertIfNecessary(matchingBeans.values(), descriptor.getDependencyType());
		if (result instanceof List<?> list && list.size() > 1) {
			Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
			if (comparator != null) {
				list.sort(comparator);
			}
		}
		return result;
	}

	@Nullable
	private Object resolveMultipleBeanMap(DependencyDescriptor descriptor, @Nullable String beanName,
			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {

		ResolvableType mapType = descriptor.getResolvableType().asMap();
		Class<?> keyType = mapType.resolveGeneric(0);
		if (String.class != keyType) {
			return null;
		}
		Class<?> valueType = mapType.resolveGeneric(1);
		if (valueType == null) {
			return null;
		}
		Map<String, Object> matchingBeans = findAutowireCandidates(beanName, valueType,
				new MultiElementDescriptor(descriptor));
		if (matchingBeans.isEmpty()) {
			return null;
		}
		if (autowiredBeanNames != null) {
			autowiredBeanNames.addAll(matchingBeans.keySet());
		}
		TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
		return converter.convertIfNecessary(matchingBeans, descriptor.getDependencyType());
	}

	private boolean indicatesArrayCollectionOrMap(Class<?> type) {
		return (type.isArray() || (type.isInterface() &&
				(Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))));
	}

	private boolean isRequired(DependencyDescriptor descriptor) {
		return getAutowireCandidateResolver().isRequired(descriptor);
	}

	@Nullable
	private Comparator<Object> adaptDependencyComparator(Map<String, ?> matchingBeans) {
		Comparator<Object> comparator = getDependencyComparator();
		if (comparator instanceof OrderComparator orderComparator) {
			return orderComparator.withSourceProvider(
					createFactoryAwareOrderSourceProvider(matchingBeans));
		}
		else {
			return comparator;
		}
	}

	private Comparator<Object> adaptOrderComparator(Map<String, ?> matchingBeans) {
		Comparator<Object> dependencyComparator = getDependencyComparator();
		OrderComparator comparator = (dependencyComparator instanceof OrderComparator orderComparator ?
				orderComparator : OrderComparator.INSTANCE);
		return comparator.withSourceProvider(createFactoryAwareOrderSourceProvider(matchingBeans));
	}

	private OrderComparator.OrderSourceProvider createFactoryAwareOrderSourceProvider(Map<String, ?> beans) {
		IdentityHashMap<Object, String> instancesToBeanNames = new IdentityHashMap<>();
		beans.forEach((beanName, instance) -> instancesToBeanNames.put(instance, beanName));
		return new FactoryAwareOrderSourceProvider(instancesToBeanNames);
	}

	/**
	 * 查找与所需类型匹配的 bean 实例。
	 * 在为指定 bean 进行自动装配时调用。
	 * @param beanName 即将被装配的 bean 的名称
	 * @param requiredType 要查找的实际 bean 类型（可能是数组组件类型或集合元素类型）
	 * @param descriptor 要解析的依赖项描述符
	 * @return 匹配所需类型的候选项名称和候选项实例的映射（永不为 {@code null}）
	 * @throws BeansException 出错时抛出
	 * @see #autowireByType
	 * @see #autowireConstructor
	 */
	protected Map<String, Object> findAutowireCandidates(
			@Nullable String beanName, Class<?> requiredType, DependencyDescriptor descriptor) {

		String[] candidateNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
				this, requiredType, true, descriptor.isEager());
		Map<String, Object> result = CollectionUtils.newLinkedHashMap(candidateNames.length);
		for (Map.Entry<Class<?>, Object> classObjectEntry : this.resolvableDependencies.entrySet()) {
			Class<?> autowiringType = classObjectEntry.getKey();
			if (autowiringType.isAssignableFrom(requiredType)) {
				Object autowiringValue = classObjectEntry.getValue();
				autowiringValue = AutowireUtils.resolveAutowiringValue(autowiringValue, requiredType);
				if (requiredType.isInstance(autowiringValue)) {
					result.put(ObjectUtils.identityToString(autowiringValue), autowiringValue);
					break;
				}
			}
		}
		for (String candidate : candidateNames) {
			if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, descriptor)) {
				addCandidateEntry(result, candidate, descriptor, requiredType);
			}
		}
		if (result.isEmpty()) {
			boolean multiple = indicatesArrayCollectionOrMap(requiredType);
			// Consider fallback matches if the first pass failed to find anything...
			DependencyDescriptor fallbackDescriptor = descriptor.forFallbackMatch();
			for (String candidate : candidateNames) {
				if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, fallbackDescriptor) &&
						(!multiple || matchesBeanName(candidate, descriptor.getDependencyName()) ||
								getAutowireCandidateResolver().hasQualifier(descriptor))) {
					addCandidateEntry(result, candidate, descriptor, requiredType);
				}
			}
			if (result.isEmpty() && !multiple) {
				// Consider self references as a final pass...
				// but in the case of a dependency collection, not the very same bean itself.
				for (String candidate : candidateNames) {
					if (isSelfReference(beanName, candidate) &&
							(!(descriptor instanceof MultiElementDescriptor) || !beanName.equals(candidate)) &&
							isAutowireCandidate(candidate, fallbackDescriptor)) {
						addCandidateEntry(result, candidate, descriptor, requiredType);
					}
				}
			}
		}
		return result;
	}

	/**
	 * 向候选项映射中添加条目：
	 * 如果可用则添加 bean 实例，否则仅添加已解析的类型，以防止在选择主要候选项之前过早初始化 bean。
	 */
	private void addCandidateEntry(Map<String, Object> candidates, String candidateName,
			DependencyDescriptor descriptor, Class<?> requiredType) {

		if (descriptor instanceof MultiElementDescriptor) {
			Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
			if (!(beanInstance instanceof NullBean)) {
				candidates.put(candidateName, beanInstance);
			}
		}
		else if (containsSingleton(candidateName) ||
				(descriptor instanceof StreamDependencyDescriptor streamDescriptor && streamDescriptor.isOrdered())) {
			Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
			candidates.put(candidateName, (beanInstance instanceof NullBean ? null : beanInstance));
		}
		else {
			candidates.put(candidateName, getType(candidateName));
		}
	}

	/**
	 * 确定给定 bean 集合中的自动装配候选项。
	 * <p>
	 *     按顺序查找 {@code @Primary} 和 {@code @Priority} 注解。
	 *
	 * @param candidates 候选名称和候选实例的映射，这些候选项与所需类型匹配，由 {@link #findAutowireCandidates} 返回
	 * @param descriptor 要匹配的目标依赖项
	 * @return 自动装配候选项的名称，如果未找到则返回 {@code null}
	 */
	@Nullable
	protected String determineAutowireCandidate(Map<String, Object> candidates, DependencyDescriptor descriptor) {
		Class<?> requiredType = descriptor.getDependencyType();
		// Step 1: check primary candidate
		String primaryCandidate = determinePrimaryCandidate(candidates, requiredType);
		if (primaryCandidate != null) {
			return primaryCandidate;
		}
		// Step 2a: match bean name against declared dependency name
		String dependencyName = descriptor.getDependencyName();
		if (dependencyName != null) {
			for (String beanName : candidates.keySet()) {
				if (matchesBeanName(beanName, dependencyName)) {
					return beanName;
				}
			}
		}
		// Step 2b: match bean name against qualifier-suggested name
		String suggestedName = getAutowireCandidateResolver().getSuggestedName(descriptor);
		if (suggestedName != null) {
			for (String beanName : candidates.keySet()) {
				if (matchesBeanName(beanName, suggestedName)) {
					return beanName;
				}
			}
		}
		// Step 3: check highest priority candidate
		String priorityCandidate = determineHighestPriorityCandidate(candidates, requiredType);
		if (priorityCandidate != null) {
			return priorityCandidate;
		}
		// Step 4: pick unique default-candidate
		String defaultCandidate = determineDefaultCandidate(candidates);
		if (defaultCandidate != null) {
			return defaultCandidate;
		}
		// Step 5: pick directly registered dependency
		for (Map.Entry<String, Object> entry : candidates.entrySet()) {
			String candidateName = entry.getKey();
			Object beanInstance = entry.getValue();
			if (beanInstance != null && this.resolvableDependencies.containsValue(beanInstance)) {
				return candidateName;
			}
		}
		return null;
	}

	/**
	 * 确定给定 bean 集合中的主要候选项。
	 * @param candidates 候选名称和候选实例（如果尚未创建，则为候选类）的映射，这些候选项与所需类型匹配
	 * @param requiredType 要匹配的目标依赖类型
	 * @return 主要候选项的名称，如果未找到则返回 {@code null}
	 * @see #isPrimary(String, Object)
	 */
	@Nullable
	protected String determinePrimaryCandidate(Map<String, Object> candidates, Class<?> requiredType) {
		String primaryBeanName = null;
		// First pass: identify unique primary candidate
		for (Map.Entry<String, Object> entry : candidates.entrySet()) {
			String candidateBeanName = entry.getKey();
			Object beanInstance = entry.getValue();
			if (isPrimary(candidateBeanName, beanInstance)) {
				if (primaryBeanName != null) {
					boolean candidateLocal = containsBeanDefinition(candidateBeanName);
					boolean primaryLocal = containsBeanDefinition(primaryBeanName);
					if (candidateLocal == primaryLocal) {
						String message = "more than one 'primary' bean found among candidates: " + candidates.keySet();
						logger.trace(message);
						throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(), message);
					}
					else if (candidateLocal) {
						primaryBeanName = candidateBeanName;
					}
				}
				else {
					primaryBeanName = candidateBeanName;
				}
			}
		}
		// Second pass: identify unique non-fallback candidate
		if (primaryBeanName == null) {
			for (String candidateBeanName : candidates.keySet()) {
				if (!isFallback(candidateBeanName)) {
					if (primaryBeanName != null) {
						return null;
					}
					primaryBeanName = candidateBeanName;
				}
			}
		}
		return primaryBeanName;
	}

	/**
	 * 确定给定 bean 集合中具有最高优先级的候选项。
	 * <p>
	 *     基于 {@code @jakarta.annotation.Priority} 注解。
	 *     根据相关的 {@link org.springframework.core.Ordered} 接口定义，值越小优先级越高。
	 *
	 * @param candidates 候选名称和候选实例（如果尚未创建，则为候选类）的映射，这些候选项与所需类型匹配
	 * @param requiredType 要匹配的目标依赖类型
	 * @return 具有最高优先级的候选项名称，如果未找到则返回 {@code null}
	 * @throws NoUniqueBeanDefinitionException 如果检测到多个具有相同最高优先级值的 bean
	 * @see #getPriority(Object)
	 */
	@Nullable
	protected String determineHighestPriorityCandidate(Map<String, Object> candidates, Class<?> requiredType) {
		String highestPriorityBeanName = null;
		Integer highestPriority = null;
		boolean highestPriorityConflictDetected = false;
		for (Map.Entry<String, Object> entry : candidates.entrySet()) {
			String candidateBeanName = entry.getKey();
			Object beanInstance = entry.getValue();
			if (beanInstance != null) {
				Integer candidatePriority = getPriority(beanInstance);
				if (candidatePriority != null) {
					if (highestPriority != null) {
						if (candidatePriority.equals(highestPriority)) {
							highestPriorityConflictDetected = true;
						}
						else if (candidatePriority < highestPriority) {
							highestPriorityBeanName = candidateBeanName;
							highestPriority = candidatePriority;
							highestPriorityConflictDetected = false;
						}
					}
					else {
						highestPriorityBeanName = candidateBeanName;
						highestPriority = candidatePriority;
					}
				}
			}
		}

		if (highestPriorityConflictDetected) {
			throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(),
					"Multiple beans found with the same highest priority (" + highestPriority +
					") among candidates: " + candidates.keySet());

		}
		return highestPriorityBeanName;
	}

	/**
	 * 返回给定 bean 名称的 bean 定义是否已被标记为主 bean。
	 * @param beanName bean 的名称
	 * @param beanInstance 相应的 bean 实例（可以为 {@code null}）
	 * @return 给定的 bean 是否符合主 bean 的条件
	 */
	protected boolean isPrimary(String beanName, Object beanInstance) {
		String transformedBeanName = transformedBeanName(beanName);
		if (containsBeanDefinition(transformedBeanName)) {
			return getMergedLocalBeanDefinition(transformedBeanName).isPrimary();
		}
		return (getParentBeanFactory() instanceof DefaultListableBeanFactory parent &&
				parent.isPrimary(transformedBeanName, beanInstance));
	}

	/**
	 * 返回给定 bean 名称的 bean 定义是否已被标记为回退 bean。
	 * @param beanName bean 的名称
	 * @since 6.2
	 */
	private boolean isFallback(String beanName) {
		String transformedBeanName = transformedBeanName(beanName);
		if (containsBeanDefinition(transformedBeanName)) {
			return getMergedLocalBeanDefinition(transformedBeanName).isFallback();
		}
		return (getParentBeanFactory() instanceof DefaultListableBeanFactory parent &&
				parent.isFallback(transformedBeanName));
	}

	/**
	 * 返回由 {@code jakarta.annotation.Priority} 注解为给定 bean 实例分配的优先级。
	 * <p>
	 *     默认实现委托给指定的 {@link #setDependencyComparator 依赖比较器}，
	 *     检查其 {@link OrderComparator#getPriority 方法}
	 *     （如果它是 Spring 常用 {@link OrderComparator} 的扩展 —— 通常是{@link org.springframework.core.annotation.AnnotationAwareOrderComparator}）。
	 *     如果不存在这样的比较器，则此实现返回 {@code null}。
	 *
	 * @param beanInstance 要检查的 bean 实例（可以为 {@code null}）
	 * @return 分配给该 bean 的优先级，如果未设置则返回 {@code null}
	 */
	@Nullable
	protected Integer getPriority(Object beanInstance) {
		Comparator<Object> comparator = getDependencyComparator();
		if (comparator instanceof OrderComparator orderComparator) {
			return orderComparator.getPriority(beanInstance);
		}
		return null;
	}

	/**
	 * 在剩余的非默认候选项中返回唯一的“默认候选项”。
	 * @param candidates 候选名称和候选实例的映射（如果尚未创建，则为候选类），这些候选项与所需类型匹配
	 * @return 默认候选项的名称，如果未找到则返回 {@code null}
	 * @since 6.2.4
	 * @see AbstractBeanDefinition#isDefaultCandidate()
	 */
	@Nullable
	private String determineDefaultCandidate(Map<String, Object> candidates) {
		String defaultBeanName = null;
		for (String candidateBeanName : candidates.keySet()) {
			if (AutowireUtils.isDefaultCandidate(this, candidateBeanName)) {
				if (defaultBeanName != null) {
					return null;
				}
				defaultBeanName = candidateBeanName;
			}
		}
		return defaultBeanName;
	}

	/**
	 * 确定给定的依赖名称是否与 bean 名称或存储在此 bean 定义中的别名匹配。
	 */
	protected boolean matchesBeanName(String beanName, @Nullable String dependencyName) {
		return (dependencyName != null &&
				(dependencyName.equals(beanName) || ObjectUtils.containsElement(getAliases(beanName), dependencyName)));
	}

	/**
	 * 确定给定的 beanName/candidateName 对是否表示自引用，即候选者是否指向原始 bean 或原始 bean 上的工厂方法。
	 */
	@Contract("null, _ -> false; _, null -> false;")
	private boolean isSelfReference(@Nullable String beanName, @Nullable String candidateName) {
		return (beanName != null && candidateName != null &&
				(beanName.equals(candidateName) || (containsBeanDefinition(candidateName) &&
						beanName.equals(getMergedLocalBeanDefinition(candidateName).getFactoryBeanName()))));
	}

	/**
	 * 确定是否存在与给定依赖类型匹配的主 bean，但不匹配给定的 bean 名称。
	 */
	private boolean hasPrimaryConflict(String beanName, Class<?> dependencyType) {
		for (Map.Entry<String, Class<?>> candidate : this.primaryBeanNamesWithType.entrySet()) {
			String candidateName = candidate.getKey();
			Class<?> candidateType = candidate.getValue();
			if (!candidateName.equals(beanName) && (candidateType != Void.class ?
					dependencyType.isAssignableFrom(candidateType) :  // cached singleton class for primary bean
					isTypeMatch(candidateName, dependencyType))) {  // not instantiated yet or not a singleton
				return true;
			}
		}
		return (getParentBeanFactory() instanceof DefaultListableBeanFactory parent &&
				parent.hasPrimaryConflict(beanName, dependencyType));
	}

	/**
	 * 为无法解析的依赖项抛出 NoSuchBeanDefinitionException 或 BeanNotOfRequiredTypeException。
	 */
	private void raiseNoMatchingBeanFound(
			Class<?> type, ResolvableType resolvableType, DependencyDescriptor descriptor) throws BeansException {

		checkBeanNotOfRequiredType(type, descriptor);

		throw new NoSuchBeanDefinitionException(resolvableType,
				"expected at least 1 bean which qualifies as autowire candidate. " +
				"Dependency annotations: " + ObjectUtils.nullSafeToString(descriptor.getAnnotations()));
	}

	/**
	 * 如果适用，为无法解析的依赖项抛出 BeanNotOfRequiredTypeException，即如果 bean 的目标类型匹配但暴露的代理不匹配时。
	 */
	private void checkBeanNotOfRequiredType(Class<?> type, DependencyDescriptor descriptor) {
		for (String beanName : this.beanDefinitionNames) {
			try {
				RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
				Class<?> targetType = mbd.getTargetType();
				if (targetType != null && type.isAssignableFrom(targetType) &&
						isAutowireCandidate(beanName, mbd, descriptor, getAutowireCandidateResolver())) {
					// Probably a proxy interfering with target type match -> throw meaningful exception.
					Object beanInstance = getSingleton(beanName, false);
					Class<?> beanType = (beanInstance != null && beanInstance.getClass() != NullBean.class ?
							beanInstance.getClass() : predictBeanType(beanName, mbd));
					if (beanType != null && !type.isAssignableFrom(beanType)) {
						throw new BeanNotOfRequiredTypeException(beanName, type, beanType);
					}
				}
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Bean definition got removed while we were iterating -> ignore.
			}
		}

		if (getParentBeanFactory() instanceof DefaultListableBeanFactory parent) {
			parent.checkBeanNotOfRequiredType(type, descriptor);
		}
	}

	/**
	 * 为指定的依赖项创建一个 {@link Optional} 包装器。
	 */
	private Optional<?> createOptionalDependency(
			DependencyDescriptor descriptor, @Nullable String beanName, final Object... args) {

		DependencyDescriptor descriptorToUse = new NestedDependencyDescriptor(descriptor) {
			@Override
			public boolean isRequired() {
				return false;
			}
			@Override
			public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
				return (!ObjectUtils.isEmpty(args) ? beanFactory.getBean(beanName, args) :
						super.resolveCandidate(beanName, requiredType, beanFactory));
			}
			@Override
			public boolean usesStandardBeanLookup() {
				return ObjectUtils.isEmpty(args);
			}
		};
		Object result = doResolveDependency(descriptorToUse, beanName, null, null);
		return (result instanceof Optional<?> optional ? optional : Optional.ofNullable(result));
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(ObjectUtils.identityToString(this));
		sb.append(": defining beans [");
		sb.append(StringUtils.collectionToCommaDelimitedString(this.beanDefinitionNames));
		sb.append("]; ");
		BeanFactory parent = getParentBeanFactory();
		if (parent == null) {
			sb.append("root of factory hierarchy");
		}
		else {
			sb.append("parent: ").append(ObjectUtils.identityToString(parent));
		}
		return sb.toString();
	}


	//---------------------------------------------------------------------
	// 序列化支持
	//---------------------------------------------------------------------

	@Serial
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		throw new NotSerializableException("DefaultListableBeanFactory itself is not deserializable - " +
				"just a SerializedBeanFactoryReference is");
	}

	@Serial
	protected Object writeReplace() throws ObjectStreamException {
		if (this.serializationId != null) {
			return new SerializedBeanFactoryReference(this.serializationId);
		}
		else {
			throw new NotSerializableException("DefaultListableBeanFactory has no serialization id");
		}
	}


	/**
	 * 工厂的最小 ID 引用。
	 * 在反序列化时解析为实际的工厂实例。
	 */
	private static class SerializedBeanFactoryReference implements Serializable {

		private final String id;

		public SerializedBeanFactoryReference(String id) {
			this.id = id;
		}

		private Object readResolve() {
			Reference<?> ref = serializableFactories.get(this.id);
			if (ref != null) {
				Object result = ref.get();
				if (result != null) {
					return result;
				}
			}
			// Lenient fallback: dummy factory in case of original factory not found...
			DefaultListableBeanFactory dummyFactory = new DefaultListableBeanFactory();
			dummyFactory.serializationId = this.id;
			return dummyFactory;
		}
	}


	/**
	 * 用于嵌套元素的依赖描述符标记。
	 */
	private static class NestedDependencyDescriptor extends DependencyDescriptor {

		public NestedDependencyDescriptor(DependencyDescriptor original) {
			super(original);
			increaseNestingLevel();
		}

		@Override
		public boolean usesStandardBeanLookup() {
			return true;
		}
	}


	/**
	 * 用于多元素声明且包含嵌套元素的依赖描述符。
	 */
	private static class MultiElementDescriptor extends NestedDependencyDescriptor {

		public MultiElementDescriptor(DependencyDescriptor original) {
			super(original);
		}
	}


	/**
	 * 用于流式访问多个元素的依赖描述符标记。
	 */
	private static class StreamDependencyDescriptor extends DependencyDescriptor {

		private final boolean ordered;

		public StreamDependencyDescriptor(DependencyDescriptor original, boolean ordered) {
			super(original);
			this.ordered = ordered;
		}

		public boolean isOrdered() {
			return this.ordered;
		}
	}


	private interface BeanObjectProvider<T> extends ObjectProvider<T>, Serializable {
	}

	/**
	 * 可序列化的 ObjectFactory/ObjectProvider，用于延迟解析依赖项。
	 */
	private class DependencyObjectProvider implements BeanObjectProvider<Object> {

		private final DependencyDescriptor descriptor;

		private final boolean optional;

		@Nullable
		private final String beanName;

		public DependencyObjectProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
			this.descriptor = new NestedDependencyDescriptor(descriptor);
			this.optional = (this.descriptor.getDependencyType() == Optional.class);
			this.beanName = beanName;
		}

		@Override
		public Object getObject() throws BeansException {
			if (this.optional) {
				return createOptionalDependency(this.descriptor, this.beanName);
			}
			else {
				Object result = doResolveDependency(this.descriptor, this.beanName, null, null);
				if (result == null) {
					throw new NoSuchBeanDefinitionException(this.descriptor.getResolvableType());
				}
				return result;
			}
		}

		@Override
		public Object getObject(final Object... args) throws BeansException {
			if (this.optional) {
				return createOptionalDependency(this.descriptor, this.beanName, args);
			}
			else {
				DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
					@Override
					public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
						return beanFactory.getBean(beanName, args);
					}
				};
				Object result = doResolveDependency(descriptorToUse, this.beanName, null, null);
				if (result == null) {
					throw new NoSuchBeanDefinitionException(this.descriptor.getResolvableType());
				}
				return result;
			}
		}

		@Override
		@Nullable
		public Object getIfAvailable() throws BeansException {
			try {
				if (this.optional) {
					return createOptionalDependency(this.descriptor, this.beanName);
				}
				else {
					DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
						@Override
						public boolean isRequired() {
							return false;
						}
						@Override
						public boolean usesStandardBeanLookup() {
							return true;
						}
					};
					return doResolveDependency(descriptorToUse, this.beanName, null, null);
				}
			}
			catch (ScopeNotActiveException ex) {
				// Ignore resolved bean in non-active scope
				return null;
			}
		}

		@Override
		public void ifAvailable(Consumer<Object> dependencyConsumer) throws BeansException {
			Object dependency = getIfAvailable();
			if (dependency != null) {
				try {
					dependencyConsumer.accept(dependency);
				}
				catch (ScopeNotActiveException ex) {
					// Ignore resolved bean in non-active scope, even on scoped proxy invocation
				}
			}
		}

		@Override
		@Nullable
		public Object getIfUnique() throws BeansException {
			DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
				@Override
				public boolean isRequired() {
					return false;
				}
				@Override
				public boolean usesStandardBeanLookup() {
					return true;
				}
				@Override
				@Nullable
				public Object resolveNotUnique(ResolvableType type, Map<String, Object> matchingBeans) {
					return null;
				}
			};
			try {
				if (this.optional) {
					return createOptionalDependency(descriptorToUse, this.beanName);
				}
				else {
					return doResolveDependency(descriptorToUse, this.beanName, null, null);
				}
			}
			catch (ScopeNotActiveException ex) {
				// Ignore resolved bean in non-active scope
				return null;
			}
		}

		@Override
		public void ifUnique(Consumer<Object> dependencyConsumer) throws BeansException {
			Object dependency = getIfUnique();
			if (dependency != null) {
				try {
					dependencyConsumer.accept(dependency);
				}
				catch (ScopeNotActiveException ex) {
					// Ignore resolved bean in non-active scope, even on scoped proxy invocation
				}
			}
		}

		@Nullable
		protected Object getValue() throws BeansException {
			if (this.optional) {
				return createOptionalDependency(this.descriptor, this.beanName);
			}
			else {
				return doResolveDependency(this.descriptor, this.beanName, null, null);
			}
		}

		@Override
		public Stream<Object> stream() {
			return resolveStream(false);
		}

		@Override
		public Stream<Object> orderedStream() {
			return resolveStream(true);
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		private Stream<Object> resolveStream(boolean ordered) {
			DependencyDescriptor descriptorToUse = new StreamDependencyDescriptor(this.descriptor, ordered);
			Object result = doResolveDependency(descriptorToUse, this.beanName, null, null);
			return (result instanceof Stream stream ? stream : Stream.of(result));
		}

		@Override
		public Stream<Object> stream(Predicate<Class<?>> customFilter, boolean includeNonSingletons) {
			return Arrays.stream(beanNamesForStream(this.descriptor.getResolvableType(), includeNonSingletons, true))
					.filter(name -> AutowireUtils.isAutowireCandidate(DefaultListableBeanFactory.this, name))
					.filter(name -> customFilter.test(getType(name)))
					.map(name -> getBean(name))
					.filter(bean -> !(bean instanceof NullBean));
		}

		@Override
		public Stream<Object> orderedStream(Predicate<Class<?>> customFilter, boolean includeNonSingletons) {
			String[] beanNames = beanNamesForStream(this.descriptor.getResolvableType(), includeNonSingletons, true);
			if (beanNames.length == 0) {
				return Stream.empty();
			}
			Map<String, Object> matchingBeans = CollectionUtils.newLinkedHashMap(beanNames.length);
			for (String beanName : beanNames) {
				if (AutowireUtils.isAutowireCandidate(DefaultListableBeanFactory.this, beanName) &&
						customFilter.test(getType(beanName))) {
					Object beanInstance = getBean(beanName);
					if (!(beanInstance instanceof NullBean)) {
						matchingBeans.put(beanName, beanInstance);
					}
				}
			}
			return matchingBeans.values().stream().sorted(adaptOrderComparator(matchingBeans));
		}
	}


	/**
	 * 单独的内部类，用于避免对 {@code jakarta.inject} API 的硬依赖。
	 * 实际的 {@code jakarta.inject.Provider} 实现在此处嵌套，以使其对 Graal 的 DefaultListableBeanFactory 嵌套类内省不可见。
	 */
	private class Jsr330Factory implements Serializable {

		public Object createDependencyProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
			return new Jsr330Provider(descriptor, beanName);
		}

		private class Jsr330Provider extends DependencyObjectProvider implements Provider<Object> {

			public Jsr330Provider(DependencyDescriptor descriptor, @Nullable String beanName) {
				super(descriptor, beanName);
			}

			@Override
			@Nullable
			public Object get() throws BeansException {
				return getValue();
			}
		}
	}


	/**
	 * 一个 {@link org.springframework.core.OrderComparator.OrderSourceProvider} 实现，能够感知待排序实例的 bean 元数据。
	 * <p>
	 *     查找待排序实例的方法工厂（如果存在），并让比较器检索其上定义的 {@link org.springframework.core.annotation.Order} 值。
	 * <p>
	 *     从 6.1.2 版本开始，此类会考虑 {@link AbstractBeanDefinition#ORDER_ATTRIBUTE} 属性。
	 */
	private class FactoryAwareOrderSourceProvider implements OrderComparator.OrderSourceProvider {

		private final Map<Object, String> instancesToBeanNames;

		public FactoryAwareOrderSourceProvider(Map<Object, String> instancesToBeanNames) {
			this.instancesToBeanNames = instancesToBeanNames;
		}

		@Override
		@Nullable
		public Object getOrderSource(Object obj) {
			String beanName = this.instancesToBeanNames.get(obj);
			if (beanName == null) {
				return null;
			}
			try {
				BeanDefinition beanDefinition = getMergedBeanDefinition(beanName);
				List<Object> sources = new ArrayList<>(3);
				Object orderAttribute = beanDefinition.getAttribute(AbstractBeanDefinition.ORDER_ATTRIBUTE);
				if (orderAttribute != null) {
					if (orderAttribute instanceof Integer order) {
						sources.add((Ordered) () -> order);
					}
					else {
						throw new IllegalStateException("Invalid value type for attribute '" +
								AbstractBeanDefinition.ORDER_ATTRIBUTE + "': " + orderAttribute.getClass().getName());
					}
				}
				if (beanDefinition instanceof RootBeanDefinition rootBeanDefinition) {
					Method factoryMethod = rootBeanDefinition.getResolvedFactoryMethod();
					if (factoryMethod != null) {
						sources.add(factoryMethod);
					}
					Class<?> targetType = rootBeanDefinition.getTargetType();
					if (targetType != null && targetType != obj.getClass()) {
						sources.add(targetType);
					}
				}
				return sources.toArray();
			}
			catch (NoSuchBeanDefinitionException ex) {
				return null;
			}
		}
	}


	private enum PreInstantiation {

		MAIN, BACKGROUND
	}

}
