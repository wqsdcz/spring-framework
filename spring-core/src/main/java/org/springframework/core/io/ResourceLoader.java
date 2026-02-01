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

package org.springframework.core.io;

import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * <p>用于加载资源（例如类路径或文件系统资源）的策略接口。
 * {@link org.springframework.context.ApplicationContext} 需要提供此功能以及扩展的
 * {@link org.springframework.core.io.support.ResourcePatternResolver} 支持。
 *
 * <p>{@link DefaultResourceLoader} 是一个可在 ApplicationContext 外部使用的独立实现，
 * 也被 {@link ResourceEditor} 使用。
 *
 * <p>在 ApplicationContext 中运行时，类型为 {@code Resource} 和 {@code Resource[]} 的 
 * Bean 属性可以使用特定上下文的资源加载策略从字符串填充。
 *
 * @author Juergen Hoeller
 * @since 10.03.2004
 * @see Resource
 * @see org.springframework.core.io.support.ResourcePatternResolver
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 */
public interface ResourceLoader {

	/**
	 * 从类路径加载的伪 URL 前缀："classpath:"。
	 */
	String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;


	/**
	 * <p>返回指定资源位置的 {@code Resource} 句柄。
	 * <p>句柄应始终是可重用的资源描述符，允许多次调用 {@link Resource#getInputStream()}。
	 * <p><ul>
	 * <li>必须支持完全限定的 URL，例如 "file:C:/test.dat"。
	 * <li>必须支持类路径伪 URL，例如 "classpath:test.dat"。
	 * <li>应该支持相对文件路径，例如 "WEB-INF/test.dat"（这将是特定于实现的，
	 * 通常由 ApplicationContext 实现提供。）
	 * </ul>
	 * <p>注意 {@code Resource} 句柄不意味着资源存在；你需要调用 {@link Resource#exists} 来检查存在性。
	 *
	 * @param location 资源位置
	 * @return 相应的 {@code Resource} 句柄（永不返回 {@code null}）
	 * @see #CLASSPATH_URL_PREFIX
	 * @see Resource#exists()
	 * @see Resource#getInputStream()
	 */
	Resource getResource(String location);

	/**
	 * <p>公开此 {@code ResourceLoader} 使用的 {@link ClassLoader}。
	 * <p>需要直接访问 {@code ClassLoader} 的客户端可以以统一的方式使用 
	 * {@code ResourceLoader}，而不是依赖线程上下文 {@code ClassLoader}。
	 *
	 * @return {@code ClassLoader}（仅当系统 {@code ClassLoader} 也无法访问时才返回 {@code null}）
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
	 */
	@Nullable
	ClassLoader getClassLoader();

}
