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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Generic registry for shared bean instances, implementing the
 * {@link org.springframework.beans.factory.config.SingletonBeanRegistry}.
 * Allows for registering singleton instances that should be shared
 * for all callers of the registry, to be obtained via bean name.
 *
 * <p>Also supports registration of
 * {@link org.springframework.beans.factory.DisposableBean} instances,
 * (which might or might not correspond to registered singletons),
 * to be destroyed on shutdown of the registry. Dependencies between
 * beans can be registered to enforce an appropriate shutdown order.
 *
 * <p>This class mainly serves as base class for
 * {@link org.springframework.beans.factory.BeanFactory} implementations,
 * factoring out the common management of singleton bean instances. Note that
 * the {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * interface extends the {@link SingletonBeanRegistry} interface.
 *
 * <p>Note that this class assumes neither a bean definition concept
 * nor a specific creation process for bean instances, in contrast to
 * {@link AbstractBeanFactory} and {@link DefaultListableBeanFactory}
 * (which inherit from it). Can alternatively also be used as a nested
 * helper to delegate to.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 */
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

	/** Maximum number of suppressed exceptions to preserve. */
	private static final int SUPPRESSED_EXCEPTIONS_LIMIT = 100;


	/** 【单例创建过程】使用的【公共锁】。 */
	final Lock singletonLock = new ReentrantLock();

	/** 单例对象的缓存: bean名称 到 bean实例。 */
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

	/** 在创建期间，单例工厂的注册表：bean名称 到 ObjectFactory实例。 */
	private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>(16);

	/** 单例在创建/注册过程中执行的自定义回调。 */
	private final Map<String, Consumer<Object>> singletonCallbacks = new ConcurrentHashMap<>(16);

	/** 提前实例化的单例对象的缓存: bean名称 到 bean实例。 */
	private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

	/** 已注册的单例集合，按注册顺序包含 bean 名称。 */
	private final Set<String> registeredSingletons = Collections.synchronizedSet(new LinkedHashSet<>(256));

	/** 当前正在创建中的 bean 名称。 */
	private final Set<String> singletonsCurrentlyInCreation = ConcurrentHashMap.newKeySet(16);

	/** 当前排除在创建检查之外的 bean 名称。 */
	private final Set<String> inCreationCheckExclusions = ConcurrentHashMap.newKeySet(16);

	/** 专门用于宽松创建跟踪的锁。 */
	private final Lock lenientCreationLock = new ReentrantLock();

	/** 专门用于宽松创建跟踪的锁条件。 */
	private final Condition lenientCreationFinished = this.lenientCreationLock.newCondition();

	/** 当前处于宽松创建模式的 bean 名称。 */
	private final Set<String> singletonsInLenientCreation = new HashSet<>();

	/** 从一个创建线程映射到等待宽松创建线程的映射。 */
	private final Map<Thread, Thread> lenientWaitingThreads = new HashMap<>();

	/** 从 bean 名称到当前正在创建的 bean 的实际创建线程的映射。 */
	private final Map<String, Thread> currentCreationThreads = new ConcurrentHashMap<>();

	/** 标志位，指示当前是否正在进行 destroySingletons 操作。 */
	private volatile boolean singletonsCurrentlyInDestruction = false;

	/** 收集被抑制的异常，可用于关联相关原因。 */
	@Nullable
	private Set<Exception> suppressedExceptions;

	/** 可销毁的 bean 实例：bean 名称到可销毁实例的映射。 */
	private final Map<String, DisposableBean> disposableBeans = new LinkedHashMap<>();

	/** 包含bean名称之间的映射：bean名称到该bean所包含的bean名称集合。 */
	private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<>(16);

	/** 映射依赖bean名称的关系：bean名称到依赖于该bean的bean名称集合。 */
	private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

	/** 映射被依赖bean名称的关系：bean名称到该bean所依赖的bean名称集合。 */
	private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);


	@Override
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		Assert.notNull(beanName, "Bean name must not be null");
		Assert.notNull(singletonObject, "Singleton object must not be null");
		this.singletonLock.lock();
		try {
			addSingleton(beanName, singletonObject);
		}
		finally {
			this.singletonLock.unlock();
		}
	}

	/**
	 * 1、如果存在同名的bean，则抛出IllegalStateException异常。
	 * 2、如果不存在同名的bean，则：
	 * 		（1）将【beanName -> bean对象】注册到【单例注册表】中。
	 * 		（2）将【beanName -> ObjectFactory】从【负责懒加载的单例工厂注册表】中移除。
	 * 		（3）将【beanName -> bean对象】从【负责提前暴露的单例注册表】中移除。（即已实例化，但未填充属性）
	 * 		（4）将【beanName】注册到 【已注册单例的beanName的注册表中】。
	 * 		（5）获取并执行【beanName】对应的【单例在创建/注册过程中执行的自定义回调】。
	 * Add the given singleton object to the singleton registry.
	 * <p>To be called for exposure of freshly registered/created singletons.
	 * @param beanName the name of the bean
	 * @param singletonObject the singleton object
	 */
	protected void addSingleton(String beanName, Object singletonObject) {
		Object oldObject = this.singletonObjects.putIfAbsent(beanName, singletonObject);
		if (oldObject != null) {
			throw new IllegalStateException("Could not register object [" + singletonObject +
					"] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
		}
		this.singletonFactories.remove(beanName);
		this.earlySingletonObjects.remove(beanName);
		this.registeredSingletons.add(beanName);

		Consumer<Object> callback = this.singletonCallbacks.get(beanName);
		if (callback != null) {
			callback.accept(singletonObject);
		}
	}

	/**
	 * 1、将【beanName -> 对象工厂】注册到【负责懒加载的单例工厂注册表】中。
	 * 2、将【beanName -> bean对象】从【负责提前暴露的单例注册表】中移除。
	 * 3、将【beanName】注册到 【已注册单例的beanName的注册表中】。
	 *
	 * @param beanName the name of the bean
	 * @param singletonFactory the factory for the singleton object
	 */
	protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		this.singletonFactories.put(beanName, singletonFactory);
		this.earlySingletonObjects.remove(beanName);
		this.registeredSingletons.add(beanName);
	}

	@Override
	public void addSingletonCallback(String beanName, Consumer<Object> singletonConsumer) {
		this.singletonCallbacks.put(beanName, singletonConsumer);
	}

	@Override
	@Nullable
	public Object getSingleton(String beanName) {
		return getSingleton(beanName, true);
	}

	/**
	 * 1、如果单例注册表中找到bean，则直接返回获取到的bean；
	 * 2、如果单例注册表中未找到bean，且bean处于正在创建中，则返回null；
	 * 3、如果单例注册表中未找到bean，且bean不处于创建中，且单例提前注册表中找到bean，则直接返回获取到的bean；
	 * 4、如果单例注册表中未找到bean，且bean不处于创建中，且单例提前注册表中未找到bean，且不许提前创建引用，则返回null；
	 * 5、如果单例注册表中未找到bean，且bean不处于创建中，且单例提前注册表中未找到bean，且允许提前创建引用，且未查询到了ObjectFactory对象，则返回null；
	 * 6、如果单例注册表中未找到bean，且bean不处于创建中，且单例提前注册表中未找到bean，且允许提前创建引用，且查询到了ObjectFactory对象，
	 *    则将ObjectFactory对象从延迟列表中移除，将ObjectFactory对象创建的bean添加到单例提前注册表中，并返回该bean；
	 * 返回以给定名称注册的（原始）单例对象。
	 * <p>检查已实例化的单例，并允许对当前正在创建的单例进行早期引用（解决循环引用问题）。
	 * @param beanName 要查找的 bean 名称
	 * @param allowEarlyReference 是否应提前创建bean的引用
	 * @return 注册的单例对象，如果未找到则返回 {@code null}
	 */
	@Nullable
	protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		// Quick check for existing instance without full singleton lock.
		Object singletonObject = this.singletonObjects.get(beanName);
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			singletonObject = this.earlySingletonObjects.get(beanName);
			if (singletonObject == null && allowEarlyReference) {
				if (!this.singletonLock.tryLock()) {
					// Avoid early singleton inference outside of original creation thread.
					return null;
				}
				try {
					// Consistent creation of early reference within full singleton lock.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						singletonObject = this.earlySingletonObjects.get(beanName);
						if (singletonObject == null) {
							ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
							if (singletonFactory != null) {
								singletonObject = singletonFactory.getObject();
								// Singleton could have been added or removed in the meantime.
								if (this.singletonFactories.remove(beanName) != null) {
									this.earlySingletonObjects.put(beanName, singletonObject);
								}
								else {
									singletonObject = this.singletonObjects.get(beanName);
								}
							}
						}
					}
				}
				finally {
					this.singletonLock.unlock();
				}
			}
		}
		return singletonObject;
	}

	/**
	 * 返回以给定名称注册的（原始）单例对象，
	 * 如果尚未注册，则创建并注册一个新的单例对象。
	 * @param beanName bean 的名称
	 * @param singletonFactory 用于延迟创建单例的 ObjectFactory（如有必要）
	 * @return 已注册的单例对象
	 */
	@SuppressWarnings("NullAway")
	public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "Bean name must not be null");

		Thread currentThread = Thread.currentThread();
		Boolean lockFlag = isCurrentThreadAllowedToHoldSingletonLock(); // 获取当前线程是否允许获取单例锁
		boolean acquireLock = !Boolean.FALSE.equals(lockFlag);	// 判断当前线程是否允许获取单例锁
		boolean locked = (acquireLock && this.singletonLock.tryLock()); // 尝试获取单例锁

		try {
			Object singletonObject = this.singletonObjects.get(beanName);
			if (singletonObject == null) { // 没有获取到单例对象
				if (acquireLock && !locked) { // 当前线程允许获取，但没获取单例锁
					if (Boolean.TRUE.equals(lockFlag)) { // lockFlag 为 true；
						// Another thread is busy in a singleton factory callback, potentially blocked.
						// Fallback as of 6.2: process given singleton bean outside of singleton lock.
						// Thread-safe exposure is still guaranteed, there is just a risk of collisions
						// when triggering creation of other beans as dependencies of the current bean.
						this.lenientCreationLock.lock();
						try {
							if (logger.isInfoEnabled()) {
								Set<String> lockedBeans = new HashSet<>(this.singletonsCurrentlyInCreation);
								lockedBeans.removeAll(this.singletonsInLenientCreation);
								logger.info("Obtaining singleton bean '" + beanName + "' in thread \"" +
										currentThread.getName() + "\" while other thread holds singleton " +
										"lock for other beans " + lockedBeans);
							}
							this.singletonsInLenientCreation.add(beanName);
						}
						finally {
							this.lenientCreationLock.unlock();
						}
					}
					else { // lockFlag 为 null；
						// No specific locking indication (outside a coordinated bootstrap) and
						// singleton lock currently held by some other creation method -> wait.
						this.singletonLock.lock();
						locked = true;
						// Singleton object might have possibly appeared in the meantime.
						singletonObject = this.singletonObjects.get(beanName);
						if (singletonObject != null) {
							return singletonObject;
						}
					}
				}

				if (this.singletonsCurrentlyInDestruction) {
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}

				try {
					beforeSingletonCreation(beanName);
				}
				catch (BeanCurrentlyInCreationException ex) {
					this.lenientCreationLock.lock();
					try {
						while ((singletonObject = this.singletonObjects.get(beanName)) == null) {
							Thread otherThread = this.currentCreationThreads.get(beanName);
							if (otherThread != null && (otherThread == currentThread ||
									checkDependentWaitingThreads(otherThread, currentThread))) {
								throw ex;
							}
							if (!this.singletonsInLenientCreation.contains(beanName)) {
								break;
							}
							if (otherThread != null) {
								this.lenientWaitingThreads.put(currentThread, otherThread);
							}
							try {
								this.lenientCreationFinished.await();
							}
							catch (InterruptedException ie) {
								currentThread.interrupt();
							}
							finally {
								if (otherThread != null) {
									this.lenientWaitingThreads.remove(currentThread);
								}
							}
						}
					}
					finally {
						this.lenientCreationLock.unlock();
					}
					if (singletonObject != null) {
						return singletonObject;
					}
					if (locked) {
						throw ex;
					}
					// Try late locking for waiting on specific bean to be finished.
					this.singletonLock.lock();
					locked = true;
					// Lock-created singleton object should have appeared in the meantime.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject != null) {
						return singletonObject;
					}
					beforeSingletonCreation(beanName);
				}

				boolean newSingleton = false;
				boolean recordSuppressedExceptions = (locked && this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<>();
				}
				try {
					// Leniently created singleton object could have appeared in the meantime.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						this.currentCreationThreads.put(beanName, currentThread);
						try {
							singletonObject = singletonFactory.getObject();
						}
						finally {
							this.currentCreationThreads.remove(beanName);
						}
						newSingleton = true;
					}
				}
				catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						throw ex;
					}
				}
				catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				}
				finally {
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					afterSingletonCreation(beanName);
				}

				if (newSingleton) {
					try {
						addSingleton(beanName, singletonObject);
					}
					catch (IllegalStateException ex) {
						// Leniently accept same instance if implicitly appeared.
						Object object = this.singletonObjects.get(beanName);
						if (singletonObject != object) {
							throw ex;
						}
					}
				}
			}
			return singletonObject;
		}
		finally {
			if (locked) {
				this.singletonLock.unlock();
			}
			this.lenientCreationLock.lock();
			try {
				this.singletonsInLenientCreation.remove(beanName);
				this.lenientWaitingThreads.entrySet().removeIf(
						entry -> entry.getValue() == currentThread);
				this.lenientCreationFinished.signalAll();
			}
			finally {
				this.lenientCreationLock.unlock();
			}
		}
	}

	private boolean checkDependentWaitingThreads(Thread waitingThread, Thread candidateThread) {
		Thread threadToCheck = waitingThread;
		while ((threadToCheck = this.lenientWaitingThreads.get(threadToCheck)) != null) {
			if (threadToCheck == candidateThread) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 确定当前线程是否被允许持有单例锁。
	 * <p>
	 *     默认情况下，所有线程都通过返回 {@code null} 被强制持有完整的锁。
	 *     {@link DefaultListableBeanFactory} 会覆盖此方法，以便在预实例化阶段专门处理其线程：
	 *     主线程返回 {@code true}，受管理的后台线程返回 {@code false}，对于非受管的线程则根据配置决定行为。
	 *
	 * @return {@code true} 表示当前线程明确被允许持有锁，但也接受宽松的回退行为；
	 *         {@code false} 表示当前线程明确不被允许持有锁，因此被迫使用宽松的回退行为；
	 *         或者 {@code null} 表示没有特定指示（传统行为：始终强制持有完整锁）
	 * @since 6.2
	 */
	@Nullable
	protected Boolean isCurrentThreadAllowedToHoldSingletonLock() {
		// 默认情况下，所有线程都被强制持有完整的锁（通过返回 null）。
		// 子类可以覆盖此方法以根据线程类型返回不同的策略：
		// - 返回 true：允许当前线程持有锁，但也接受宽松的回退行为。
		// - 返回 false：明确禁止当前线程持有锁，因此强制使用宽松的回退行为。
		// - 返回 null：无特定指示（传统行为：始终强制持有完整锁）。
		return null;
	}

	/**
	 * 注册在创建单例 bean 实例期间被抑制的异常，例如临时的循环引用解析问题。
	 * <p>
	 *     默认实现会在该注册表的抑制异常集合中保留任何给定的异常，最多保留 100 个异常，
	 *     并将其作为最终顶级 {@link BeanCreationException} 的相关原因添加。
	 * @param ex 要注册的异常
	 * @see BeanCreationException#getRelatedCauses()
	 */
	protected void onSuppressedException(Exception ex) {
		// 检查抑制异常集合是否存在且未达到上限
		if (this.suppressedExceptions != null && this.suppressedExceptions.size() < SUPPRESSED_EXCEPTIONS_LIMIT) {
			// 将异常添加到抑制异常集合中
			this.suppressedExceptions.add(ex);
		}
	}

	/**
	 * Remove the bean with the given name from the singleton registry, either on
	 * regular destruction or on cleanup after early exposure when creation failed.
	 * @param beanName the name of the bean
	 */
	protected void removeSingleton(String beanName) {
		// 从单例对象缓存中移除指定的bean
		this.singletonObjects.remove(beanName);
		// 从单例工厂缓存中移除指定的bean
		this.singletonFactories.remove(beanName);
		// 从提前暴露的单例对象缓存中移除指定的bean
		this.earlySingletonObjects.remove(beanName);
		// 从已注册的单例集合中移除指定的bean名称
		this.registeredSingletons.remove(beanName);
	}

	@Override
	public boolean containsSingleton(String beanName) {
		return this.singletonObjects.containsKey(beanName);
	}

	@Override
	public String[] getSingletonNames() {
		return StringUtils.toStringArray(this.registeredSingletons);
	}

	@Override
	public int getSingletonCount() {
		return this.registeredSingletons.size();
	}


	public void setCurrentlyInCreation(String beanName, boolean inCreation) {
		Assert.notNull(beanName, "Bean name must not be null");
		if (!inCreation) {
			this.inCreationCheckExclusions.add(beanName);
		}
		else {
			this.inCreationCheckExclusions.remove(beanName);
		}
	}

	public boolean isCurrentlyInCreation(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return (!this.inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName));
	}

	protected boolean isActuallyInCreation(String beanName) {
		return isSingletonCurrentlyInCreation(beanName);
	}

	/**
	 * Return whether the specified singleton bean is currently in creation
	 * (within the entire factory).
	 * @param beanName the name of the bean
	 */
	public boolean isSingletonCurrentlyInCreation(@Nullable String beanName) {
		return this.singletonsCurrentlyInCreation.contains(beanName);
	}

	/**
	 * Callback before singleton creation.
	 * <p>The default implementation register the singleton as currently in creation.
	 * @param beanName the name of the singleton about to be created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void beforeSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}

	/**
	 * 单例创建后的回调。
	 * <p>默认实现将单例标记为不再处于创建状态。
	 * @param beanName 已创建的单例的名称
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void afterSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
		}
	}


	/**
	 * Add the given bean to the list of disposable beans in this registry.
	 * <p>Disposable beans usually correspond to registered singletons,
	 * matching the bean name but potentially being a different instance
	 * (for example, a DisposableBean adapter for a singleton that does not
	 * naturally implement Spring's DisposableBean interface).
	 * @param beanName the name of the bean
	 * @param bean the bean instance
	 */
	public void registerDisposableBean(String beanName, DisposableBean bean) {
		synchronized (this.disposableBeans) {
			this.disposableBeans.put(beanName, bean);
		}
	}

	/**
	 * Register a containment relationship between two beans,
	 * for example, between an inner bean and its containing outer bean.
	 * <p>Also registers the containing bean as dependent on the contained bean
	 * in terms of destruction order.
	 * @param containedBeanName the name of the contained (inner) bean
	 * @param containingBeanName the name of the containing (outer) bean
	 * @see #registerDependentBean
	 */
	public void registerContainedBean(String containedBeanName, String containingBeanName) {
		synchronized (this.containedBeanMap) {
			Set<String> containedBeans =
					this.containedBeanMap.computeIfAbsent(containingBeanName, key -> new LinkedHashSet<>(8));
			if (!containedBeans.add(containedBeanName)) {
				return;
			}
		}
		registerDependentBean(containedBeanName, containingBeanName);
	}

	/**
	 * Register a dependent bean for the given bean,
	 * to be destroyed before the given bean is destroyed.
	 * @param beanName the name of the bean
	 * @param dependentBeanName the name of the dependent bean
	 */
	public void registerDependentBean(String beanName, String dependentBeanName) {
		String canonicalName = canonicalName(beanName);

		synchronized (this.dependentBeanMap) {
			Set<String> dependentBeans =
					this.dependentBeanMap.computeIfAbsent(canonicalName, key -> new LinkedHashSet<>(8));
			if (!dependentBeans.add(dependentBeanName)) {
				return;
			}
		}

		synchronized (this.dependenciesForBeanMap) {
			Set<String> dependenciesForBean =
					this.dependenciesForBeanMap.computeIfAbsent(dependentBeanName, key -> new LinkedHashSet<>(8));
			dependenciesForBean.add(canonicalName);
		}
	}

	/**
	 * Determine whether the specified dependent bean has been registered as
	 * dependent on the given bean or on any of its transitive dependencies.
	 * @param beanName the name of the bean to check
	 * @param dependentBeanName the name of the dependent bean
	 * @since 4.0
	 */
	protected boolean isDependent(String beanName, String dependentBeanName) {
		synchronized (this.dependentBeanMap) {
			return isDependent(beanName, dependentBeanName, null);
		}
	}

	private boolean isDependent(String beanName, String dependentBeanName, @Nullable Set<String> alreadySeen) {
		if (alreadySeen != null && alreadySeen.contains(beanName)) {
			return false;
		}
		String canonicalName = canonicalName(beanName);
		Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
		if (dependentBeans == null || dependentBeans.isEmpty()) {
			return false;
		}
		if (dependentBeans.contains(dependentBeanName)) {
			return true;
		}
		if (alreadySeen == null) {
			alreadySeen = new HashSet<>();
		}
		alreadySeen.add(beanName);
		for (String transitiveDependency : dependentBeans) {
			if (isDependent(transitiveDependency, dependentBeanName, alreadySeen)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether a dependent bean has been registered for the given name.
	 * @param beanName the name of the bean to check
	 */
	protected boolean hasDependentBean(String beanName) {
		return this.dependentBeanMap.containsKey(beanName);
	}

	/**
	 * Return the names of all beans which depend on the specified bean, if any.
	 * @param beanName the name of the bean
	 * @return the array of dependent bean names, or an empty array if none
	 */
	public String[] getDependentBeans(String beanName) {
		Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
		if (dependentBeans == null) {
			return new String[0];
		}
		synchronized (this.dependentBeanMap) {
			return StringUtils.toStringArray(dependentBeans);
		}
	}

	/**
	 * Return the names of all beans that the specified bean depends on, if any.
	 * @param beanName the name of the bean
	 * @return the array of names of beans which the bean depends on,
	 * or an empty array if none
	 */
	public String[] getDependenciesForBean(String beanName) {
		Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
		if (dependenciesForBean == null) {
			return new String[0];
		}
		synchronized (this.dependenciesForBeanMap) {
			return StringUtils.toStringArray(dependenciesForBean);
		}
	}

	public void destroySingletons() {
		if (logger.isTraceEnabled()) {
			logger.trace("Destroying singletons in " + this);
		}
		this.singletonsCurrentlyInDestruction = true;

		String[] disposableBeanNames;
		synchronized (this.disposableBeans) {
			disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
		}
		for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
			destroySingleton(disposableBeanNames[i]);
		}

		this.containedBeanMap.clear();
		this.dependentBeanMap.clear();
		this.dependenciesForBeanMap.clear();

		this.singletonLock.lock();
		try {
			clearSingletonCache();
		}
		finally {
			this.singletonLock.unlock();
		}
	}

	/**
	 * Clear all cached singleton instances in this registry.
	 * @since 4.3.15
	 */
	protected void clearSingletonCache() {
		this.singletonObjects.clear();
		this.singletonFactories.clear();
		this.earlySingletonObjects.clear();
		this.registeredSingletons.clear();
		this.singletonsCurrentlyInDestruction = false;
	}

	/**
	 * Destroy the given bean. Delegates to {@code destroyBean}
	 * if a corresponding disposable bean instance is found.
	 * @param beanName the name of the bean
	 * @see #destroyBean
	 */
	public void destroySingleton(String beanName) {
		// Destroy the corresponding DisposableBean instance.
		// This also triggers the destruction of dependent beans.
		DisposableBean disposableBean;
		synchronized (this.disposableBeans) {
			disposableBean = this.disposableBeans.remove(beanName);
		}
		destroyBean(beanName, disposableBean);

		// destroySingletons() removes all singleton instances at the end,
		// leniently tolerating late retrieval during the shutdown phase.
		if (!this.singletonsCurrentlyInDestruction) {
			// For an individual destruction, remove the registered instance now.
			// As of 6.2, this happens after the current bean's destruction step,
			// allowing for late bean retrieval by on-demand suppliers etc.
			if (this.currentCreationThreads.get(beanName) == Thread.currentThread()) {
				// Local remove after failed creation step -> without singleton lock
				// since bean creation may have happened leniently without any lock.
				removeSingleton(beanName);
			}
			else {
				this.singletonLock.lock();
				try {
					removeSingleton(beanName);
				}
				finally {
					this.singletonLock.unlock();
				}
			}
		}
	}

	/**
	 * Destroy the given bean. Must destroy beans that depend on the given
	 * bean before the bean itself. Should not throw any exceptions.
	 * @param beanName the name of the bean
	 * @param bean the bean instance to destroy
	 */
	protected void destroyBean(String beanName, @Nullable DisposableBean bean) {
		// Trigger destruction of dependent beans first...
		Set<String> dependentBeanNames;
		synchronized (this.dependentBeanMap) {
			// Within full synchronization in order to guarantee a disconnected Set
			dependentBeanNames = this.dependentBeanMap.remove(beanName);
		}
		if (dependentBeanNames != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Retrieved dependent beans for bean '" + beanName + "': " + dependentBeanNames);
			}
			for (String dependentBeanName : dependentBeanNames) {
				destroySingleton(dependentBeanName);
			}
		}

		// Actually destroy the bean now...
		if (bean != null) {
			try {
				bean.destroy();
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Destruction of bean with name '" + beanName + "' threw an exception", ex);
				}
			}
		}

		// Trigger destruction of contained beans...
		Set<String> containedBeans;
		synchronized (this.containedBeanMap) {
			// Within full synchronization in order to guarantee a disconnected Set
			containedBeans = this.containedBeanMap.remove(beanName);
		}
		if (containedBeans != null) {
			for (String containedBeanName : containedBeans) {
				destroySingleton(containedBeanName);
			}
		}

		// Remove destroyed bean from other beans' dependencies.
		synchronized (this.dependentBeanMap) {
			for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, Set<String>> entry = it.next();
				Set<String> dependenciesToClean = entry.getValue();
				dependenciesToClean.remove(beanName);
				if (dependenciesToClean.isEmpty()) {
					it.remove();
				}
			}
		}

		// Remove destroyed bean's prepared dependency information.
		this.dependenciesForBeanMap.remove(beanName);
	}

	@Deprecated(since = "6.2")
	@Override
	public final Object getSingletonMutex() {
		return new Object();
	}

}
