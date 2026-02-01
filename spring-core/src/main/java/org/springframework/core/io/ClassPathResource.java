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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * <p>类路径资源的 {@link Resource} 实现。使用给定的 {@link ClassLoader} 或给定的 {@link Class} 加载资源。
 *
 * <p>如果类路径资源位于文件系统中，则支持解析为 {@code java.io.File}，但对于 JAR 中的资源则不支持。
 * 始终支持解析为 {@code java.net.URL}。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 28.12.2003
 * @see ClassLoader#getResourceAsStream(String)
 * @see ClassLoader#getResource(String)
 * @see Class#getResourceAsStream(String)
 * @see Class#getResource(String)
 */
public class ClassPathResource extends AbstractFileResolvingResource {

	/**
	 * <p>用户提供的原始路径的内部表示，用于创建相对路径和解析 URL 及 InputStream。
	 */
	private final String path;

	private final String absolutePath;

	@Nullable
	private final ClassLoader classLoader;

	@Nullable
	private final Class<?> clazz;


	/**
	 * <p>为 {@code ClassLoader} 使用创建新的 {@code ClassPathResource}。
	 * <p>前导斜杠将被移除，因为 {@code ClassLoader} 资源访问方法不接受它。
	 * <p>将使用默认类加载器加载资源。
	 *
	 * @param path 类路径中的绝对路径
	 * @see ClassUtils#getDefaultClassLoader()
	 */
	public ClassPathResource(String path) {
		this(path, (ClassLoader) null);
	}

	/**
	 * <p>为 {@code ClassLoader} 使用创建新的 {@code ClassPathResource}。
	 * <p>前导斜杠将被移除，因为 {@code ClassLoader} 资源访问方法不接受它。
	 * <p>如果提供的 {@code ClassLoader} 为 {@code null}，将使用默认类加载器加载资源。
	 *
	 * @param path  类路径中的绝对路径
	 * @param classLoader 用于加载资源的类加载器
	 * @see ClassUtils#getDefaultClassLoader()
	 */
	public ClassPathResource(String path, @Nullable ClassLoader classLoader) {
		Assert.notNull(path, "Path must not be null");
		String pathToUse = StringUtils.cleanPath(path);
		if (pathToUse.startsWith("/")) {
			pathToUse = pathToUse.substring(1);
		}
		this.path = pathToUse;
		this.absolutePath = pathToUse;
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
		this.clazz = null;
	}

	/**
	 * <p>为 {@code Class} 使用创建新的 {@code ClassPathResource}。
	 * <p>路径可以是相对于给定类的，或者通过前导斜杠在类路径中绝对定位。
	 * <p>如果提供的 {@code Class} 为 {@code null}，将使用默认类加载器加载资源。
	 * <p>这也适用于模块系统中的资源访问，从给定 {@code Class} 的包含模块加载资源。
	 * 参见 {@link ModuleResource} 及其 javadoc。
	 *
	 * @param path  类路径中的相对或绝对路径
	 * @param clazz  用于加载资源的类
	 * @see ClassUtils#getDefaultClassLoader()
	 * @see ModuleResource
	 */
	public ClassPathResource(String path, @Nullable Class<?> clazz) {
		Assert.notNull(path, "Path must not be null");
		this.path = StringUtils.cleanPath(path);

		String absolutePath = this.path;
		if (clazz != null && !absolutePath.startsWith("/")) {
			absolutePath = ClassUtils.classPackageAsResourcePath(clazz) + "/" + absolutePath;
		}
		else if (absolutePath.startsWith("/")) {
			absolutePath = absolutePath.substring(1);
		}
		this.absolutePath = absolutePath;

		this.classLoader = null;
		this.clazz = clazz;
	}


	/**
	 * <p>返回此资源的<em>绝对路径</em>，作为类路径中已清理的资源路径。
	 * <p>此方法返回的路径没有前导斜杠，适用于与 {@link ClassLoader#getResource(String)} 一起使用。
	 */
	public final String getPath() {
		return this.absolutePath;
	}

	/**
	 * <p>返回将从中获取此资源的 {@link ClassLoader}。
	 */
	@Nullable
	public final ClassLoader getClassLoader() {
		return (this.clazz != null ? this.clazz.getClassLoader() : this.classLoader);
	}


	/**
	 * <p>此实现检查资源 URL 的解析。
	 *
	 * @see ClassLoader#getResource(String)
	 * @see Class#getResource(String)
	 */
	@Override
	public boolean exists() {
		return (resolveURL() != null);
	}

	/**
	 * <p>此实现预先检查资源 URL 的解析，然后继续 {@link AbstractFileResolvingResource} 的长度检查。
	 *
	 * @see ClassLoader#getResource(String)
	 * @see Class#getResource(String)
	 */
	@Override
	public boolean isReadable() {
		URL url = resolveURL();
		return (url != null && checkReadable(url));
	}

	/**
	 * <p>解析底层类路径资源的 {@link URL}。
	 *
	 * @return 已解析的 URL，如果无法解析则返回 {@code null}
	 */
	@Nullable
	protected URL resolveURL() {
		try {
			if (this.clazz != null) {
				return this.clazz.getResource(this.path);
			}
			else if (this.classLoader != null) {
				return this.classLoader.getResource(this.absolutePath);
			}
			else {
				return ClassLoader.getSystemResource(this.absolutePath);
			}
		}
		catch (IllegalArgumentException ex) {
			// Should not happen according to the JDK's contract:
			// see https://github.com/openjdk/jdk/pull/2662
			return null;
		}
	}

	/**
	 * <p>此实现为底层类路径资源打开 {@link InputStream}（如果可用）。
	 *
	 * @see ClassLoader#getResourceAsStream(String)
	 * @see Class#getResourceAsStream(String)
	 * @see ClassLoader#getSystemResourceAsStream(String)
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is;
		if (this.clazz != null) {
			is = this.clazz.getResourceAsStream(this.path);
		}
		else if (this.classLoader != null) {
			is = this.classLoader.getResourceAsStream(this.absolutePath);
		}
		else {
			is = ClassLoader.getSystemResourceAsStream(this.absolutePath);
		}
		if (is == null) {
			throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
		}
		return is;
	}

	/**
	 * <p>此实现返回底层类路径资源的 URL（如果可用）。
	 *
	 * @see ClassLoader#getResource(String)
	 * @see Class#getResource(String)
	 */
	@Override
	public URL getURL() throws IOException {
		URL url = resolveURL();
		if (url == null) {
			throw new FileNotFoundException(getDescription() + " cannot be resolved to URL because it does not exist");
		}
		return url;
	}

	/**
	 * <p>此实现创建一个 {@code ClassPathResource}，将给定路径应用于此描述符用于创建的路径的相对路径。
	 *
	 * @see StringUtils#applyRelativePath(String, String)
	 */
	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return (this.clazz != null ? new ClassPathResource(pathToUse, this.clazz) :
				new ClassPathResource(pathToUse, this.classLoader));
	}

	/**
	 * <p>此实现返回此类路径资源引用的文件名称。
	 *
	 * @see StringUtils#getFilename(String)
	 */
	@Override
	@Nullable
	public String getFilename() {
		return StringUtils.getFilename(this.absolutePath);
	}

	/**
	 * <p>此实现返回包含绝对类路径位置的描述。
	 */
	@Override
	public String getDescription() {
		return "class path resource [" + this.absolutePath + "]";
	}


	/**
	 * <p>此实现比较底层类路径位置和相关联的类加载器。
	 *
	 * @see #getPath()
	 * @see #getClassLoader()
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof ClassPathResource that &&
				this.absolutePath.equals(that.absolutePath) &&
				ObjectUtils.nullSafeEquals(getClassLoader(), that.getClassLoader())));
	}

	/**
	 * <p>此实现返回底层类路径位置的哈希码。
	 *
	 * @see #getPath()
	 */
	@Override
	public int hashCode() {
		return this.absolutePath.hashCode();
	}

}
