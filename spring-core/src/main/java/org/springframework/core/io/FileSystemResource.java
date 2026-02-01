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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * <p>针对文件系统目标的 {@code java.io.File} 和 {@code java.nio.file.Path} 句柄的 {@link Resource} 实现。
 * 支持解析为 {@code File}，也支持解析为 {@code URL}。实现了扩展的 {@link WritableResource} 接口。
 *
 * <p>注意：此 {@link Resource} 实现使用 NIO.2 API 进行读/写交互，可以用 {@link java.nio.file.Path} 
 * 句柄构造，在这种情况下它将仅通过 NIO.2 执行所有文件系统交互，仅在 {@link #getFile()} 时回退到 {@link File}。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 28.12.2003
 * @see #FileSystemResource(String)
 * @see #FileSystemResource(File)
 * @see #FileSystemResource(Path)
 * @see java.io.File
 * @see java.nio.file.Files
 */
public class FileSystemResource extends AbstractResource implements WritableResource {

	private final String path;

	@Nullable
	private final File file;

	private final Path filePath;


	/**
	 * <p>从文件路径创建新的 {@code FileSystemResource}。
	 * <p>注意：当通过 {@link #createRelative} 构建相对资源时，此处指定的资源基本路径是否以斜杠结尾会有所不同。
	 * 如果是 "C:/dir1/"，相对路径将构建在该根目录下：例如相对路径 "dir2" → "C:/dir1/dir2"。
	 * 如果是 "C:/dir1"，相对路径将在同一目录级别应用：相对路径 "dir2" → "C:/dir2"。
	 *
	 * @param path 文件路径
	 * @see #FileSystemResource(Path)
	 */
	public FileSystemResource(String path) {
		Assert.notNull(path, "Path must not be null");
		this.path = StringUtils.cleanPath(path);
		this.file = new File(path);
		this.filePath = this.file.toPath();
	}

	/**
	 * <p>从 {@link File} 句柄创建新的 {@code FileSystemResource}。
	 * <p>注意：当通过 {@link #createRelative} 构建相对资源时，相对路径将在<i>同一目录级别</i>应用：
	 * 例如 new File("C:/dir1")，相对路径 "dir2" → "C:/dir2"！
	 * 如果你希望将相对路径构建在给定根目录下，请使用带有文件路径的 {@link #FileSystemResource(String) 构造函数}
	 * 将尾部斜杠附加到根路径："C:/dir1/"，这表示此目录为所有相对路径的根。
	 *
	 * @param file 文件句柄
	 * @see #FileSystemResource(Path)
	 * @see #getFile()
	 */
	public FileSystemResource(File file) {
		Assert.notNull(file, "File must not be null");
		this.path = StringUtils.cleanPath(file.getPath());
		this.file = file;
		this.filePath = file.toPath();
	}

	/**
	 * <p>从 {@link Path} 句柄创建新的 {@code FileSystemResource}，通过 NIO.2 而不是 {@link File} 执行所有文件系统交互。
	 * <p>与 {@link PathResource} 相比，此变体严格遵循通用的 {@link FileSystemResource} 约定，
	 * 特别是在路径清理和 {@link #createRelative(String)} 处理方面。
	 * <p>注意：当通过 {@link #createRelative} 构建相对资源时，相对路径将在<i>同一目录级别</i>应用：
	 * 例如 Paths.get("C:/dir1")，相对路径 "dir2" → "C:/dir2"！
	 * 如果你希望将相对路径构建在给定根目录下，请使用带有文件路径的 {@link #FileSystemResource(String) 构造函数}
	 * 将尾部斜杠附加到根路径："C:/dir1/"，这表示此目录为所有相对路径的根。
	 * 或者，考虑在 {@code createRelative} 中使用 {@link PathResource#PathResource(Path)} 进行 
	 * {@code java.nio.path.Path} 解析，始终嵌套相对路径。
	 *
	 * @param filePath 文件的 Path 句柄
	 * @since 5.1
	 * @see #FileSystemResource(File)
	 */
	public FileSystemResource(Path filePath) {
		Assert.notNull(filePath, "Path must not be null");
		this.path = StringUtils.cleanPath(filePath.toString());
		this.file = null;
		this.filePath = filePath;
	}

	/**
	 * <p>从 {@link FileSystem} 句柄创建新的 {@code FileSystemResource}，定位指定路径。
	 * <p>这是 {@link #FileSystemResource(String)} 的替代方案，通过 NIO.2 而不是 {@link File} 执行所有文件系统交互。
	 *
	 * @param fileSystem the FileSystem to locate the path within
	 *                   用于定位路径的 FileSystem
	 * @param path a file path
	 *             文件路径
	 * @since 5.1.1
	 * @see #FileSystemResource(File)
	 */
	public FileSystemResource(FileSystem fileSystem, String path) {
		Assert.notNull(fileSystem, "FileSystem must not be null");
		Assert.notNull(path, "Path must not be null");
		this.path = StringUtils.cleanPath(path);
		this.file = null;
		this.filePath = fileSystem.getPath(this.path).normalize();
	}


	/**
	 * <p>返回此资源的文件路径。
	 */
	public final String getPath() {
		return this.path;
	}

	/**
	 * <p>此实现返回底层文件是否存在。
	 *
	 * @see java.io.File#exists()
	 * @see java.nio.file.Files#exists(Path, java.nio.file.LinkOption...)
	 */
	@Override
	public boolean exists() {
		return (this.file != null ? this.file.exists() : Files.exists(this.filePath));
	}

	/**
	 * <p>此实现检查底层文件是否标记为可读（并对应于具有内容的实际文件，而不是目录）。
	 *
	 * @see java.io.File#canRead()
	 * @see java.io.File#isDirectory()
	 * @see java.nio.file.Files#isReadable(Path)
	 * @see java.nio.file.Files#isDirectory(Path, java.nio.file.LinkOption...)
	 */
	@Override
	public boolean isReadable() {
		return (this.file != null ? this.file.canRead() && !this.file.isDirectory() :
				Files.isReadable(this.filePath) && !Files.isDirectory(this.filePath));
	}

	/**
	 * <p>此实现为底层文件打开 NIO 文件流。
	 *
	 * @see java.nio.file.Files#newInputStream(Path, java.nio.file.OpenOption...)
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		try {
			return Files.newInputStream(this.filePath);
		}
		catch (NoSuchFileException ex) {
			throw new FileNotFoundException(ex.getMessage());
		}
	}

	@Override
	public byte[] getContentAsByteArray() throws IOException {
		try {
			return Files.readAllBytes(this.filePath);
		}
		catch (NoSuchFileException ex) {
			throw new FileNotFoundException(ex.getMessage());
		}
	}

	@Override
	public String getContentAsString(Charset charset) throws IOException {
		try {
			return Files.readString(this.filePath, charset);
		}
		catch (NoSuchFileException ex) {
			throw new FileNotFoundException(ex.getMessage());
		}
	}

	/**
	 * <p>此实现检查底层文件是否标记为可写（并对应于具有内容的实际文件，而不是目录）。
	 *
	 * @see java.io.File#canWrite()
	 * @see java.io.File#isDirectory()
	 * @see java.nio.file.Files#isWritable(Path)
	 * @see java.nio.file.Files#isDirectory(Path, java.nio.file.LinkOption...)
	 */
	@Override
	public boolean isWritable() {
		return (this.file != null ? this.file.canWrite() && !this.file.isDirectory() :
				Files.isWritable(this.filePath) && !Files.isDirectory(this.filePath));
	}

	/**
	 * <p>此实现为底层文件打开 FileOutputStream。
	 *
	 * @see java.nio.file.Files#newOutputStream(Path, java.nio.file.OpenOption...)
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		return Files.newOutputStream(this.filePath);
	}

	/**
	 * <p>此实现返回底层文件的 URL。
	 *
	 * @see java.io.File#toURI()
	 * @see java.nio.file.Path#toUri()
	 */
	@Override
	public URL getURL() throws IOException {
		return (this.file != null ? this.file.toURI().toURL() : this.filePath.toUri().toURL());
	}

	/**
	 * <p>此实现返回底层文件的 URI。
	 *
	 * @see java.io.File#toURI()
	 * @see java.nio.file.Path#toUri()
	 */
	@Override
	public URI getURI() throws IOException {
		if (this.file != null) {
			return this.file.toURI();
		}
		else {
			URI uri = this.filePath.toUri();
			// Normalize URI? See https://github.com/spring-projects/spring-framework/issues/29275
			String scheme = uri.getScheme();
			if (ResourceUtils.URL_PROTOCOL_FILE.equals(scheme)) {
				try {
					uri = new URI(scheme, uri.getPath(), null);
				}
				catch (URISyntaxException ex) {
					throw new IOException("Failed to normalize URI: " + uri, ex);
				}
			}
			return uri;
		}
	}

	/**
	 * <p>此实现始终指示为文件。
	 */
	@Override
	public boolean isFile() {
		return true;
	}

	/**
	 * <p>此实现返回底层 File 引用。
	 */
	@Override
	public File getFile() {
		return (this.file != null ? this.file : this.filePath.toFile());
	}

	/**
	 * <p>此实现为底层文件打开 FileChannel。
	 * @see java.nio.channels.FileChannel
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		try {
			return FileChannel.open(this.filePath, StandardOpenOption.READ);
		}
		catch (NoSuchFileException ex) {
			throw new FileNotFoundException(ex.getMessage());
		}
	}

	/**
	 * <p>此实现为底层文件打开 FileChannel。
	 * @see java.nio.channels.FileChannel
	 */
	@Override
	public WritableByteChannel writableChannel() throws IOException {
		return FileChannel.open(this.filePath, StandardOpenOption.WRITE);
	}

	/**
	 * <p>此实现返回底层 File/Path 长度。
	 */
	@Override
	public long contentLength() throws IOException {
		if (this.file != null) {
			long length = this.file.length();
			if (length == 0L && !this.file.exists()) {
				throw new FileNotFoundException(getDescription() +
						" cannot be resolved in the file system for checking its content length");
			}
			return length;
		}
		else {
			try {
				return Files.size(this.filePath);
			}
			catch (NoSuchFileException ex) {
				throw new FileNotFoundException(ex.getMessage());
			}
		}
	}

	/**
	 * <p>此实现返回底层 File/Path 的最后修改时间。
	 */
	@Override
	public long lastModified() throws IOException {
		if (this.file != null) {
			return super.lastModified();
		}
		else {
			try {
				return Files.getLastModifiedTime(this.filePath).toMillis();
			}
			catch (NoSuchFileException ex) {
				throw new FileNotFoundException(ex.getMessage());
			}
		}
	}

	/**
	 * <p>此实现创建 FileSystemResource，将给定路径应用于此资源描述符的底层文件路径的相对路径。
	 *
	 * @see org.springframework.util.StringUtils#applyRelativePath(String, String)
	 */
	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return (this.file != null ? new FileSystemResource(pathToUse) :
				new FileSystemResource(this.filePath.getFileSystem(), pathToUse));
	}

	/**
	 * <p>此实现返回文件名称。
	 *
	 * @see java.io.File#getName()
	 * @see java.nio.file.Path#getFileName()
	 */
	@Override
	public String getFilename() {
		return (this.file != null ? this.file.getName() : this.filePath.getFileName().toString());
	}

	/**
	 * <p>此实现返回包含文件绝对路径的描述。
	 *
	 * @see java.io.File#getAbsolutePath()
	 * @see java.nio.file.Path#toAbsolutePath()
	 */
	@Override
	public String getDescription() {
		return "file [" + (this.file != null ? this.file.getAbsolutePath() : this.filePath.toAbsolutePath()) + "]";
	}


	/**
	 * <p>此实现比较底层文件路径。
	 *
	 * @see #getPath()
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof FileSystemResource that && this.path.equals(that.path)));
	}

	/**
	 * <p>此实现返回底层文件路径的哈希码。
	 *
	 * @see #getPath()
	 */
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}
