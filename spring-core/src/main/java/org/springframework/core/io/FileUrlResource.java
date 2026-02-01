
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
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * {@link UrlResource} 的子类，假定文件解析，实现 {@link WritableResource} 接口。
 * 此资源变体还会从 {@link #getFile()} 缓存已解析的 {@link File} 句柄。
 *
 * <p>这是由 {@link DefaultResourceLoader} 解析的类，用于 "file:..." URL 位置，
 * 允许向下转换为此类的 {@link WritableResource}。
 *
 * <p>或者，对于从 {@link java.io.File} 句柄或 NIO {@link java.nio.file.Path} 直接构造，
 * 考虑使用 {@link FileSystemResource}。
 *
 * @author Juergen Hoeller
 * @since 5.0.2
 */
public class FileUrlResource extends UrlResource implements WritableResource {

	@Nullable
	private volatile File file;


	/**
	 * 基于给定的 URL 对象创建新的 {@code FileUrlResource}。
	 * <p>注意，这不会强制使用 "file" 作为 URL 协议。如果协议已知可以解析为文件，则适用于此目的。
	 * @param url 一个 URL
	 * @see ResourceUtils#isFileURL(URL)
	 * @see #getFile()
	 */
	public FileUrlResource(URL url) {
		super(url);
	}

	/**
	 * 基于给定的文件位置创建新的 {@code FileUrlResource}，
	 * 使用 URL 协议 "file"。
	 * <p>必要时，给定的部分将自动进行编码。
	 * @param location 位置（即该协议内的文件路径）
	 * @throws MalformedURLException 如果给定的 URL 规范无效
	 * @see UrlResource#UrlResource(String, String)
	 * @see ResourceUtils#URL_PROTOCOL_FILE
	 */
	public FileUrlResource(String location) throws MalformedURLException {
		super(ResourceUtils.URL_PROTOCOL_FILE, location);
	}


	@Override
	public File getFile() throws IOException {
		File file = this.file;
		if (file != null) {
			return file;
		}
		file = super.getFile();
		this.file = file;
		return file;
	}

	@Override
	public boolean isWritable() {
		try {
			File file = getFile();
			return (file.canWrite() && !file.isDirectory());
		}
		catch (IOException ex) {
			return false;
		}
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return Files.newOutputStream(getFile().toPath());
	}

	@Override
	public WritableByteChannel writableChannel() throws IOException {
		return FileChannel.open(getFile().toPath(), StandardOpenOption.WRITE);
	}

	@Override
	public Resource createRelative(String relativePath) throws MalformedURLException {
		FileUrlResource resource = new FileUrlResource(createRelativeURL(relativePath));
		resource.useCaches = this.useCaches;
		return resource;
	}

}