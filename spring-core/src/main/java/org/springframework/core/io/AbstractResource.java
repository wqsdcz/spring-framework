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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * <p>{@link Resource} 实现的便利基类，预实现典型行为。
 *
 * <p>"exists" 方法将检查是否可以打开 File 或 InputStream；
 * "isOpen" 始终返回 false；"getURL" 和 "getFile" 抛出异常；
 * "toString" 将返回描述。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 28.12.2003
 */
public abstract class AbstractResource implements Resource {

	/**
	 * <p>此实现检查是否可以打开 File，如果不能则回退到检查是否可以打开 InputStream。
	 * <p>这将涵盖目录和内容资源。
	 */
	@Override
	public boolean exists() {
		// Try file existence: can we find the file in the file system?
		if (isFile()) {
			try {
				return getFile().exists();
			}
			catch (IOException ex) {
				debug(() -> "Could not retrieve File for existence check of " + getDescription(), ex);
			}
		}
		// Fall back to stream existence: can we open the stream?
		try {
			getInputStream().close();
			return true;
		}
		catch (Throwable ex) {
			debug(() -> "Could not retrieve InputStream for existence check of " + getDescription(), ex);
			return false;
		}
	}

	/**
	 * <p>此实现对存在的资源始终返回 {@code true}（自 5.1 修订）。
	 */
	@Override
	public boolean isReadable() {
		return exists();
	}

	/**
	 * 此实现始终返回 {@code false}。
	 */
	@Override
	public boolean isOpen() {
		return false;
	}

	/**
	 * 此实现始终返回 {@code false}。
	 */
	@Override
	public boolean isFile() {
		return false;
	}

	/**
	 * 此实现预设为资源无法解析为 URL，抛出 FileNotFoundException。
	 */
	@Override
	public URL getURL() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to URL");
	}

	/**
	 * <p>此实现基于 {@link #getURL()} 返回的 URL 构建 URI。
	 */
	@Override
	public URI getURI() throws IOException {
		URL url = getURL();
		try {
			return ResourceUtils.toURI(url);
		}
		catch (URISyntaxException ex) {
			throw new IOException("Invalid URI [" + url + "]", ex);
		}
	}

	/**
	 * <p>此实现预设为资源无法解析为绝对文件路径，抛出 FileNotFoundException。
	 */
	@Override
	public File getFile() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to absolute file path");
	}

	/**
	 * <p>此实现返回 {@link Channels#newChannel(InputStream)} 并使用 {@link #getInputStream()} 的结果。
	 * <p>这与 {@link Resource} 中相应的默认方法相同，但在此处镜像以便在类层次结构中高效地进行 JVM 级调度。
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	/**
	 * <p>此方法读取整个 InputStream 以确定内容长度。
	 * <p>对于 {@code InputStreamResource} 的自定义子类，我们强烈建议用更优化的实现覆盖此方法，
	 * 例如检查文件长度，或者在流只能读取一次的情况下简单返回 -1。
	 * 
	 * @see #getInputStream()
	 */
	@Override
	public long contentLength() throws IOException {
		InputStream is = getInputStream();
		try {
			long size = 0;
			byte[] buf = new byte[256];
			int read;
			while ((read = is.read(buf)) != -1) {
				size += read;
			}
			return size;
		}
		finally {
			try {
				is.close();
			}
			catch (IOException ex) {
				debug(() -> "Could not close content-length InputStream for " + getDescription(), ex);
			}
		}
	}

	/**
	 * <p>此实现检查底层文件的时间戳（如果可用）。
	 * 
	 * @see #getFileForLastModifiedCheck()
	 */
	@Override
	public long lastModified() throws IOException {
		File fileToCheck = getFileForLastModifiedCheck();
		long lastModified = fileToCheck.lastModified();
		if (lastModified == 0L && !fileToCheck.exists()) {
			throw new FileNotFoundException(getDescription() +
					" cannot be resolved in the file system for checking its last-modified timestamp");
		}
		return lastModified;
	}

	/**
	 * <p>确定用于时间戳检查的文件。
	 * <p>默认实现委托给 {@link #getFile()}。
	 * 
	 * @return 用于时间戳检查的文件（永不返回 {@code null}）
	 * @throws FileNotFoundException 如果资源无法解析为绝对文件路径（即文件系统中不可用）
	 * @throws IOException 如果出现常规解析/读取失败
	 */
	protected File getFileForLastModifiedCheck() throws IOException {
		return getFile();
	}

	/**
	 * <p>此实现抛出 FileNotFoundException，假设无法为此资源创建相对资源。
	 */
	@Override
	public Resource createRelative(String relativePath) throws IOException {
		throw new FileNotFoundException("Cannot create a relative resource for " + getDescription());
	}

	/**
	 * <p>此实现始终返回 {@code null}，假设此资源类型没有文件名。
	 */
	@Override
	@Nullable
	public String getFilename() {
		return null;
	}

	/**
	 * <p>延迟访问日志记录器以便在发生异常时进行调试日志记录。
	 */
	private void debug(Supplier<String> message, Throwable ex) {
		Log logger = LogFactory.getLog(getClass());
		if (logger.isDebugEnabled()) {
			logger.debug(message.get(), ex);
		}
	}


	/**
	 * <p>此实现比较描述字符串。
	 * 
	 * @see #getDescription()
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof Resource that &&
				getDescription().equals(that.getDescription())));
	}

	/**
	 * <p>此实现返回描述的哈希码。
	 * 
	 * @see #getDescription()
	 */
	@Override
	public int hashCode() {
		return getDescription().hashCode();
	}

	/**
	 * <p>此实现返回此资源的描述。
	 * 
	 * @see #getDescription()
	 */
	@Override
	public String toString() {
		return getDescription();
	}

}
