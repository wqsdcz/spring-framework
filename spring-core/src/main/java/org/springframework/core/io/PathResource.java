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
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * <p>{@link java.nio.file.Path} 句柄的 {@link Resource} 实现，通过 {@code Path} API 执行所有操作和转换。
 * 支持解析为 {@link File}，也支持解析为 {@link URL}。实现了扩展的 {@link WritableResource} 接口。
 *
 * <p>注意：自 5.1 起，{@link java.nio.file.Path} 支持也在 
 * {@link FileSystemResource#FileSystemResource(Path) FileSystemResource} 中可用，
 * 应用 Spring 标准的基于字符串的路径转换，但通过 {@link java.nio.file.Files} API 执行所有操作。
 * 此 {@code PathResource} 实际上是一个纯 {@code java.nio.path.Path} 替代方案，具有不同的 {@code createRelative} 行为。
 *
 * @author Philippe Marschall
 * @author Juergen Hoeller
 * @since 4.0
 * @see java.nio.file.Path
 * @see java.nio.file.Files
 * @see FileSystemResource
 */
public class PathResource extends AbstractResource implements WritableResource {

	private final Path path;


	/**
	 * <p>从 {@link Path} 句柄创建新的 {@code PathResource}。
	 * <p>注意：与 {@link FileSystemResource} 不同，当通过 {@link #createRelative} 构建相对资源时，
	 * 相对路径将构建在给定根的<i>下方</i>：例如 Paths.get("C:/dir1/")，相对路径 "dir2" → "C:/dir1/dir2"！
	 *
	 * @param path Path 句柄
	 */
	public PathResource(Path path) {
		Assert.notNull(path, "Path must not be null");
		this.path = path.normalize();
	}

	/**
	 * <p>从路径字符串创建新的 {@code PathResource}。
	 * <p>注意：与 {@link FileSystemResource} 不同，当通过 {@link #createRelative} 构建相对资源时，
	 * 相对路径将构建在给定根的<i>下方</i>：例如 Paths.get("C:/dir1/")，相对路径 "dir2" → "C:/dir1/dir2"！
	 *
	 * @param path 路径
	 * @see java.nio.file.Paths#get(String, String...)
	 */
	public PathResource(String path) {
		Assert.notNull(path, "Path must not be null");
		this.path = Paths.get(path).normalize();
	}

	/**
	 * <p>从 {@link URI} 创建新的 {@code PathResource}。
	 * <p>注意：与 {@link FileSystemResource} 不同，当通过 {@link #createRelative} 构建相对资源时，
	 * 相对路径将构建在给定根的<i>下方</i>：例如 Paths.get("C:/dir1/")，相对路径 "dir2" → "C:/dir1/dir2"！
	 *
	 * @param uri 路径 URI
	 * @see java.nio.file.Paths#get(URI)
	 */
	public PathResource(URI uri) {
		Assert.notNull(uri, "URI must not be null");
		this.path = Paths.get(uri).normalize();
	}


	/**
	 * <p>返回此资源的文件路径。
	 */
	public final String getPath() {
		return this.path.toString();
	}

	/**
	 * <p>此实现返回底层文件是否存在。
	 *
	 * @see java.nio.file.Files#exists(Path, java.nio.file.LinkOption...)
	 */
	@Override
	public boolean exists() {
		return Files.exists(this.path);
	}

	/**
	 * <p>此实现检查底层文件是否标记为可读（并对应于具有内容的实际文件，而不是目录）。
	 *
	 * @see java.nio.file.Files#isReadable(Path)
	 * @see java.nio.file.Files#isDirectory(Path, java.nio.file.LinkOption...)
	 */
	@Override
	public boolean isReadable() {
		return (Files.isReadable(this.path) && !Files.isDirectory(this.path));
	}

	/**
	 * <p>此实现为底层文件打开 {@link InputStream}。
	 *
	 * @see java.nio.file.spi.FileSystemProvider#newInputStream(Path, OpenOption...)
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		if (!exists()) {
			throw new FileNotFoundException(getPath() + " (no such file or directory)");
		}
		if (Files.isDirectory(this.path)) {
			throw new FileNotFoundException(getPath() + " (is a directory)");
		}
		return Files.newInputStream(this.path);
	}

	@Override
	public byte[] getContentAsByteArray() throws IOException {
		try {
			return Files.readAllBytes(this.path);
		}
		catch (NoSuchFileException ex) {
			throw new FileNotFoundException(ex.getMessage());
		}
	}

	@Override
	public String getContentAsString(Charset charset) throws IOException {
		try {
			return Files.readString(this.path, charset);
		}
		catch (NoSuchFileException ex) {
			throw new FileNotFoundException(ex.getMessage());
		}
	}

	/**
	 * <p>此实现检查底层文件是否标记为可写（并对应于具有内容的实际文件，而不是目录）。
	 *
	 * @see java.nio.file.Files#isWritable(Path)
	 * @see java.nio.file.Files#isDirectory(Path, java.nio.file.LinkOption...)
	 */
	@Override
	public boolean isWritable() {
		return (Files.isWritable(this.path) && !Files.isDirectory(this.path));
	}

	/**
	 * <p>此实现为底层文件打开 {@link OutputStream}。
	 *
	 * @see java.nio.file.spi.FileSystemProvider#newOutputStream(Path, OpenOption...)
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		if (Files.isDirectory(this.path)) {
			throw new FileNotFoundException(getPath() + " (is a directory)");
		}
		return Files.newOutputStream(this.path);
	}

	/**
	 * <p>此实现返回底层文件的 {@link URL}。
	 *
	 * @see java.nio.file.Path#toUri()
	 * @see java.net.URI#toURL()
	 */
	@Override
	public URL getURL() throws IOException {
		return this.path.toUri().toURL();
	}

	/**
	 * <p>此实现返回底层文件的 {@link URI}。
	 *
	 * @see java.nio.file.Path#toUri()
	 */
	@Override
	public URI getURI() throws IOException {
		return this.path.toUri();
	}

	/**
	 * <p>此实现始终指示为文件。
	 */
	@Override
	public boolean isFile() {
		return true;
	}

	/**
	 * <p>此实现返回底层 {@link File} 引用。
	 */
	@Override
	public File getFile() throws IOException {
		try {
			return this.path.toFile();
		}
		catch (UnsupportedOperationException ex) {
			// Only paths on the default file system can be converted to a File:
			// Do exception translation for cases where conversion is not possible.
			throw new FileNotFoundException(this.path + " cannot be resolved to absolute file path");
		}
	}

	/**
	 * <p>此实现为底层文件打开 {@link ReadableByteChannel}。
	 *
	 * @see Files#newByteChannel(Path, OpenOption...)
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		try {
			return Files.newByteChannel(this.path, StandardOpenOption.READ);
		}
		catch (NoSuchFileException ex) {
			throw new FileNotFoundException(ex.getMessage());
		}
	}

	/**
	 * <p>此实现为底层文件打开 {@link WritableByteChannel}。
	 *
	 * @see Files#newByteChannel(Path, OpenOption...)
	 */
	@Override
	public WritableByteChannel writableChannel() throws IOException {
		return Files.newByteChannel(this.path, StandardOpenOption.WRITE);
	}

	/**
	 * <p>此实现返回底层文件的长度。
	 */
	@Override
	public long contentLength() throws IOException {
		return Files.size(this.path);
	}

	/**
	 * <p>此实现返回底层文件的时间戳。
	 *
	 * @see java.nio.file.Files#getLastModifiedTime(Path, java.nio.file.LinkOption...)
	 */
	@Override
	public long lastModified() throws IOException {
		// We can not use the superclass method since it uses conversion to a File and
		// only a Path on the default file system can be converted to a File...
		return Files.getLastModifiedTime(this.path).toMillis();
	}

	/**
	 * <p>此实现创建 {@link PathResource}，将给定路径应用于此资源描述符的底层文件路径的相对路径。
	 *
	 * @see java.nio.file.Path#resolve(String)
	 */
	@Override
	public Resource createRelative(String relativePath) {
		return new PathResource(this.path.resolve(relativePath));
	}

	/**
	 * <p>此实现返回文件名称。
	 *
	 * @see java.nio.file.Path#getFileName()
	 */
	@Override
	public String getFilename() {
		return this.path.getFileName().toString();
	}

	@Override
	public String getDescription() {
		return "path [" + this.path.toAbsolutePath() + "]";
	}


	/**
	 * <p>此实现比较底层 {@link Path} 引用。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof PathResource that && this.path.equals(that.path)));
	}

	/**
	 * <p>此实现返回底层 {@link Path} 引用的哈希码。
	 */
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}
