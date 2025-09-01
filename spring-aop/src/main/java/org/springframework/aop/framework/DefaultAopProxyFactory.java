/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.Proxy;

import org.springframework.aop.SpringProxy;

/**
 * <p>默认的{@link AopProxyFactory}实现，可创建CGLIB代理或JDK动态代理。</p>
 * <p>
 *     当{@link AdvisedSupport}实例满足以下任一条件时，将创建CGLIB代理：
 *     <ul>
 *         <li>{@code optimize}标志设置为true</li>
 *         <li>{@code proxyTargetClass}标志设置为true</li>
 *         <li>未指定任何代理接口</li>
 *     </ul>
 * </p>
 * <p>通常应通过设置{@code proxyTargetClass}强制使用CGLIB代理， 或指定一个及以上接口来使用JDK动态代理。</p>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 12.03.2004
 * @see AdvisedSupport#setOptimize
 * @see AdvisedSupport#setProxyTargetClass
 * @see AdvisedSupport#setInterfaces
 */
@SuppressWarnings("serial")
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {

	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		/**
		 * config.isOptimize(): 是否启用了代理优化
		 * config.isProxyTargetClass(): 是否强制使用基于类的代理（CGLIB）
		 * hasNoUserSuppliedProxyInterfaces(config): 目标对象是否没有用户自定义的接口
		 */
		if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
			/**
			 * 如果目标类是接口；
			 * 如果目标类已经是 JDK 代理类；
			 */
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
				return new JdkDynamicAopProxy(config);
			}
			return new ObjenesisCglibAopProxy(config);
		}
		else {
			return new JdkDynamicAopProxy(config);
		}
	}

	/**
	 * 判断所提供的 {@link AdvisedSupport} 是否仅指定了 {@link org.springframework.aop.SpringProxy} 接口（或者根本没有指定任何代理接口）。
	 * Determine whether the supplied {@link AdvisedSupport} has only the
	 * {@link org.springframework.aop.SpringProxy} interface specified
	 * (or no proxy interfaces specified at all).
	 */
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}

}
