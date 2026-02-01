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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * <p>{@link ResourceLoader} 接口的默认实现。
 *
 * <p>被 {@link ResourceEditor} 使用，并作为 
 * {@link org.springframework.context.support.AbstractApplicationContext} 的基类。
 * 也可以独立使用。
 *
 * <p>如果位置值是 URL，则返回 {@link UrlResource}；
 * 如果是非 URL 路径或 "classpath:" 伪 URL，则返回 {@link ClassPathResource}。
 *
 * @author Juergen Hoeller
 * @since 10.03.2004
 * @see FileSystemResourceLoader
 * @see org.springframework.context.support.ClassPathXmlApplicationContext
 */
public class DefaultResourceLoader implements ResourceLoader {

	@Nullable
	private ClassLoader classLoader;

	private final Set<ProtocolResolver> protocolResolvers = new LinkedHashSet<>(4);

	private final Map<Class<?>, Map<Resource, ?>> resourceCaches = new ConcurrentHashMap<>(4);


	/**
	 * <p>创建新的 DefaultResourceLoader。
	 * <p>类加载器访问将在实际资源访问时使用线程上下文类加载器。
	 * 要获得更多控制，请将特定的 ClassLoader 传递给 {@link #DefaultResourceLoader(ClassLoader)}。
	 *
	 * @see java.lang.Thread#getContextClassLoader()
	 */
	public DefaultResourceLoader() {
	}

	/**
	 * <p>创建新的 DefaultResourceLoader。
	 *
	 * @param classLoader 用于加载类路径资源的 ClassLoader，或 {@code null} 以在实际资源访问时使用线程上下文类加载器
	 */
	public DefaultResourceLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * <p>指定用于加载类路径资源的 ClassLoader，或 {@code null} 以在实际资源访问时使用线程上下文类加载器。
	 * <p>默认情况下，类加载器访问将在实际资源访问时使用线程上下文类加载器。
	 */
	public void setClassLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * <p>返回用于加载类路径资源的 ClassLoader。
	 * <p>将传递给由此资源加载器创建的所有 ClassPathResource 对象的构造函数。
	 *
	 * @see ClassPathResource
	 */
	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		return (this.classLoader != null ? this.classLoader : ClassUtils.getDefaultClassLoader());
	}

	/**
	 * <p>向此资源加载器注册给定的解析器，允许处理额外的协议。
	 * <p>任何此类解析器将在此加载器的标准解析规则之前被调用。因此它也可以覆盖任何默认规则。
	 *
	 * @since 4.3
	 * @see #getProtocolResolvers()
	 */
	public void addProtocolResolver(ProtocolResolver resolver) {
		Assert.notNull(resolver, "ProtocolResolver must not be null");
		this.protocolResolvers.add(resolver);
	}

	/**
	 * <p>返回当前注册的协议解析器集合，允许内省和修改。
	 *
	 * @since 4.3
	 * @see #addProtocolResolver(ProtocolResolver)
	 */
	public Collection<ProtocolResolver> getProtocolResolvers() {
		return this.protocolResolvers;
	}

	/**
	 * <p>获取给定值类型的缓存，以 {@link Resource} 为键。
	 *
	 * @param valueType 值类型，例如 ASM {@code MetadataReader}
	 * @return 缓存 {@link Map}，在 {@code ResourceLoader} 级别共享
	 * @since 5.0
	 */
	@SuppressWarnings("unchecked")
	public <T> Map<Resource, T> getResourceCache(Class<T> valueType) {
		return (Map<Resource, T>) this.resourceCaches.computeIfAbsent(valueType, key -> new ConcurrentHashMap<>());
	}

	/**
	 * <p>清除此资源加载器中的所有资源缓存。
	 *
	 * @since 5.0
	 * @see #getResourceCache
	 */
	public void clearResourceCaches() {
		this.resourceCaches.clear();
	}


	@Override
	public Resource getResource(String location) {
		Assert.notNull(location, "Location must not be null");

		for (ProtocolResolver protocolResolver : getProtocolResolvers()) {
			Resource resource = protocolResolver.resolve(location, this);
			if (resource != null) {
				return resource;
			}
		}

		if (location.startsWith("/")) {
			return getResourceByPath(location);
		}
		else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
			return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
		}
		else {
			try {
				// Try to parse the location as a URL...
				URL url = ResourceUtils.toURL(location);
				return (ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));
			}
			catch (MalformedURLException ex) {
				// No URL -> resolve as resource path.
				return getResourceByPath(location);
			}
		}
	}

	/**
	 * <p>返回给定路径处资源的 Resource 句柄。
	 * <p>默认实现支持类路径位置。这应该适合独立实现，但可以覆盖，例如：针对 Servlet 容器的实现。
	 *
	 * @param path  资源路径
	 * @return  相应的 Resource 句柄
	 * @see ClassPathResource
	 * @see org.springframework.context.support.FileSystemXmlApplicationContext#getResourceByPath
	 * @see org.springframework.web.context.support.XmlWebApplicationContext#getResourceByPath
	 */
	protected Resource getResourceByPath(String path) {
		return new ClassPathContextResource(path, getClassLoader());
	}


	/**
	 * <p>通过实现 ContextResource 接口来明确表示上下文相对路径的 ClassPathResource。
	 */
	protected static class ClassPathContextResource extends ClassPathResource implements ContextResource {

		public ClassPathContextResource(String path, @Nullable ClassLoader classLoader) {
			super(path, classLoader);
		}

		@Override
		public String getPathWithinContext() {
			return getPath();
		}

		@Override
		public Resource createRelative(String relativePath) {
			String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
			return new ClassPathContextResource(pathToUse, getClassLoader());
		}
	}

}
