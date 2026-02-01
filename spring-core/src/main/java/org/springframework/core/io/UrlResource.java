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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * <p>{@code java.net.URL} 定位器的 {@link Resource} 实现。支持解析为 {@code URL}，
 * 也支持在 {@code "file:"} 协议的情况下解析为 {@code File}。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 28.12.2003
 * @see java.net.URL
 */
public class UrlResource extends AbstractFileResolvingResource {

	private static final String AUTHORIZATION = "Authorization";


	/**
	 * <p>原始 URI（如果可用）；用于 URI 和文件访问。
	 */
	@Nullable
	private final URI uri;

	/**
	 * <p>原始 URL，用于实际访问。
	 */
	private final URL url;

	/**
	 * <p>清理后的 URL 字符串（带有规范化路径），用于比较。
	 */
	@Nullable
	private volatile String cleanedUrl;

	/**
	 * <p>是否使用 URLConnection 缓存（{@code null} 表示默认）。
	 */
	@Nullable
	volatile Boolean useCaches;


	/**
	 * <p>基于给定的 URL 对象创建新的 {@code UrlResource}。
	 *
	 * @param url  URL
	 * @see #UrlResource(URI)
	 * @see #UrlResource(String)
	 */
	public UrlResource(URL url) {
		Assert.notNull(url, "URL must not be null");
		this.uri = null;
		this.url = url;
	}

	/**
	 * <p>基于给定的 URI 对象创建新的 {@code UrlResource}。
	 *
	 * @param uri  URI
	 * @throws MalformedURLException  如果给定的 URL 路径无效
	 * @since 2.5
	 */
	public UrlResource(URI uri) throws MalformedURLException {
		Assert.notNull(uri, "URI must not be null");
		this.uri = uri;
		this.url = uri.toURL();
	}

	/**
	 * <p>基于 URI 路径创建新的 {@code UrlResource}。
	 * <p>注意：如有必要，给定的路径需要预先编码。
	 *
	 * @param path  URI 路径
	 * @throws MalformedURLException 如果给定的 URI 路径无效
	 * @see ResourceUtils#toURI(String)
	 */
	public UrlResource(String path) throws MalformedURLException {
		Assert.notNull(path, "Path must not be null");
		String cleanedPath = StringUtils.cleanPath(path);
		URI uri;
		URL url;

		try {
			// Prefer URI construction with toURL conversion (as of 6.1)
			uri = ResourceUtils.toURI(cleanedPath);
			url = uri.toURL();
		}
		catch (URISyntaxException | IllegalArgumentException ex) {
			uri = null;
			url = ResourceUtils.toURL(path);
		}

		this.uri = uri;
		this.url = url;
		this.cleanedUrl = cleanedPath;
	}

	/**
	 * <p>基于 URI 规范创建新的 {@code UrlResource}。
	 * <p>给定的部分将自动编码（如有必要）。
	 *
	 * @param protocol  要使用的 URL 协议（例如，"jar" 或 "file" - 不带冒号）；也称为 "scheme"
	 * @param location  位置（例如，该协议中的文件路径）；也称为 "scheme-specific part"
	 * @throws MalformedURLException  如果给定的 URL 规范无效
	 * @see java.net.URI#URI(String, String, String)
	 */
	public UrlResource(String protocol, String location) throws MalformedURLException {
		this(protocol, location, null);
	}

	/**
	 * <p>基于 URI 规范创建新的 {@code UrlResource}。
	 * <p>给定的部分将自动编码（如有必要）。
	 *
	 * @param protocol  要使用的 URL 协议（例如，"jar" 或 "file" - 不带冒号）；也称为 "scheme"
	 * @param location  位置（例如，该协议中的文件路径）；也称为 "scheme-specific part"
	 * @param fragment  该位置中的片段（例如，HTML 页面上的锚点，跟在 "#" 分隔符之后）
	 * @throws MalformedURLException  如果给定的 URL 规范无效
	 * @see java.net.URI#URI(String, String, String)
	 */
	public UrlResource(String protocol, String location, @Nullable String fragment) throws MalformedURLException {
		try {
			this.uri = new URI(protocol, location, fragment);
			this.url = this.uri.toURL();
		}
		catch (URISyntaxException ex) {
			MalformedURLException exToThrow = new MalformedURLException(ex.getMessage());
			exToThrow.initCause(ex);
			throw exToThrow;
		}
	}


	/**
	 * 从给定的 {@link URI} 创建一个新的 {@code UrlResource}。
	 * <p>此工厂方法是对 {@link #UrlResource(URI)} 的便利封装，
	 * 它捕获任何 {@link MalformedURLException} 并将其包装在{@link UncheckedIOException} 中重新抛出；
	 * 适用于 {@link java.util.stream.Stream} 和 {@link java.util.Optional} API 或其他不希望出现检查型 {@link IOException} 的场景。
	 * @param uri 一个 URI
	 * @throws UncheckedIOException 如果给定的 URL 路径无效
	 * @since 6.0
	 * @see #UrlResource(URI)
	 */
	public static UrlResource from(URI uri) throws UncheckedIOException {
		try {
			return new UrlResource(uri);
		}
		catch (MalformedURLException ex) {
			throw new UncheckedIOException(ex);
		}
	}


	/**
	 * 从给定的 URL 路径创建一个新的 {@code UrlResource}。
	 * <p>此工厂方法是对 {@link #UrlResource(String)} 的便利封装，
	 * 它捕获任何 {@link MalformedURLException} 并将其包装在
	 * {@link UncheckedIOException} 中重新抛出；适用于 {@link java.util.stream.Stream}
	 * 和 {@link java.util.Optional} API 或其他不希望出现检查型
	 * {@link IOException} 的场景。
	 * @param path URL 路径
	 * @throws UncheckedIOException 如果给定的 URL 路径无效
	 * @since 6.0
	 * @see #UrlResource(String)
	 */
	public static UrlResource from(String path) throws UncheckedIOException {
		try {
			return new UrlResource(path);
		}
		catch (MalformedURLException ex) {
			throw new UncheckedIOException(ex);
		}
	}


	/**
	 * <p>惰性确定给定原始 URL 的清理后的 URL。
	 */
	private String getCleanedUrl() {
		String cleanedUrl = this.cleanedUrl;
		if (cleanedUrl != null) {
			return cleanedUrl;
		}
		String originalPath = (this.uri != null ? this.uri : this.url).toString();
		cleanedUrl = StringUtils.cleanPath(originalPath);
		this.cleanedUrl = cleanedUrl;
		return cleanedUrl;
	}

	/**
	 * <p>为 {@link URLConnection#setUseCaches} 设置显式标志，以应用于此资源中的任何 {@link URLConnection} 操作。
	 * <p>默认情况下，缓存将仅应用于 JAR 资源。显式的 {@code true} 标志将缓存应用于所有资源，
	 * 而显式的 {@code false} 标志也将关闭 JAR 资源的缓存。
	 *
	 * @since 6.2.10
	 * @see ResourceUtils#useCachesIfNecessary
	 */
	public void setUseCaches(boolean useCaches) {
		this.useCaches = useCaches;
	}


	/**
	 * <p>此实现为给定 URL 打开 InputStream。
	 *
	 * @see java.net.URL#openConnection()
	 * @see java.net.URLConnection#setUseCaches(boolean)
	 * @see java.net.URLConnection#getInputStream()
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		URLConnection con = this.url.openConnection();
		customizeConnection(con);
		try {
			return con.getInputStream();
		}
		catch (IOException ex) {
			// Close the HTTP connection (if applicable).
			if (con instanceof HttpURLConnection httpCon) {
				httpCon.disconnect();
			}
			throw ex;
		}
	}

	@Override
	protected void customizeConnection(URLConnection con) throws IOException {
		super.customizeConnection(con);
		String userInfo = this.url.getUserInfo();
		if (userInfo != null) {
			String encodedCredentials = Base64.getUrlEncoder().encodeToString(userInfo.getBytes());
			con.setRequestProperty(AUTHORIZATION, "Basic " + encodedCredentials);
		}
	}

	@Override
	void useCachesIfNecessary(URLConnection con) {
		Boolean useCaches = this.useCaches;
		if (useCaches != null) {
			con.setUseCaches(useCaches);
		}
		else {
			super.useCachesIfNecessary(con);
		}
	}

	/**
	 * <p>此实现返回底层 URL 引用。
	 */
	@Override
	public URL getURL() {
		return this.url;
	}

	/**
	 * <p>此实现直接返回底层 URI（如果可能）。
	 */
	@Override
	public URI getURI() throws IOException {
		if (this.uri != null) {
			return this.uri;
		}
		else {
			return super.getURI();
		}
	}

	@Override
	public boolean isFile() {
		if (this.uri != null) {
			return super.isFile(this.uri);
		}
		else {
			return super.isFile();
		}
	}

	/**
	 * <p>此实现返回底层 URL/URI 的 File 引用，前提是指向文件系统中的文件。
	 *
	 * @see org.springframework.util.ResourceUtils#getFile(java.net.URL, String)
	 */
	@Override
	public File getFile() throws IOException {
		if (this.uri != null) {
			return super.getFile(this.uri);
		}
		else {
			return super.getFile();
		}
	}

	/**
	 * <p>此实现创建一个 {@code UrlResource}，委托给 {@link #createRelativeURL(String)} 来调整相对路径。
	 *
	 * @see #createRelativeURL(String)
	 */
	@Override
	public Resource createRelative(String relativePath) throws MalformedURLException {
		UrlResource resource = new UrlResource(createRelativeURL(relativePath));
		resource.useCaches = this.useCaches;
		return resource;
	}

	/**
	 * <p>此委托创建一个 {@code java.net.URL}，将给定路径应用于此资源描述符的底层 URL 路径的相对路径。
	 * <p>前导斜杠将被删除；"#" 符号将被编码。注意，自 6.1 起，此方法实际上会清理组合路径。
	 *
	 * @since 5.2
	 * @see #createRelative(String)
	 * @see ResourceUtils#toRelativeURL(URL, String)
	 */
	protected URL createRelativeURL(String relativePath) throws MalformedURLException {
		if (relativePath.startsWith("/")) {
			relativePath = relativePath.substring(1);
		}
		return ResourceUtils.toRelativeURL(this.url, relativePath);
	}

	/**
	 * <p>此实现返回此 URL 引用的文件的 URL 解码后的名称。
	 *
	 * @see java.net.URL#getPath()
	 * @see java.net.URLDecoder#decode(String, java.nio.charset.Charset)
	 */
	@Override
	@Nullable
	public String getFilename() {
		if (this.uri != null) {
			String path = this.uri.getPath();
			if (path != null) {
				// Prefer URI path: decoded and has standard separators
				return StringUtils.getFilename(this.uri.getPath());
			}
		}
		// Otherwise, process URL path
		String filename = StringUtils.getFilename(StringUtils.cleanPath(this.url.getPath()));
		return (filename != null ? URLDecoder.decode(filename, StandardCharsets.UTF_8) : null);
	}

	/**
	 * <p>此实现返回包含 URL 的描述。
	 */
	@Override
	public String getDescription() {
		return "URL [" + (this.uri != null ? this.uri : this.url) + "]";
	}


	/**
	 * <p>此实现比较底层 URL 引用。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof UrlResource that &&
				getCleanedUrl().equals(that.getCleanedUrl())));
	}

	/**
	 * <p>此实现返回底层 URL 引用的哈希码。
	 */
	@Override
	public int hashCode() {
		return getCleanedUrl().hashCode();
	}

}
