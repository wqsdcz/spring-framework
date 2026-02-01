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
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;

/**
 * <p>资源描述符接口，对底层资源的各种实际类型（如文件或类路径资源）中进行抽象。
 *
 * <p>如果资源以物理形式存在，那么每个资源都可以打开 InputStream，但只能为某些资源返回 URL 或 File 句柄。实际行为是特定于实现的。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 28.12.2003
 * @see #getInputStream()
 * @see #getURL()
 * @see #getURI()
 * @see #getFile()
 * @see WritableResource
 * @see ContextResource
 * @see UrlResource
 * @see FileUrlResource
 * @see FileSystemResource
 * @see ClassPathResource
 * @see ByteArrayResource
 * @see InputStreamResource
 */
public interface Resource extends InputStreamSource {

	/**
	 * <p>确定此资源是否以物理形式实际存在。
	 * <p>此方法执行确定性的存在性检查，而 Resource 句柄的存在仅保证有效的描述符句柄。
	 */
	boolean exists();

	/**
	 * <p>指示是否可以通过 {@link #getInputStream()} 读取此资源的非空内容。
	 * <p>对于存在的典型资源描述符，将返回 {@code true}，
	 * 因为从 5.1 开始它严格意味着 {@link #exists()} 的语义。
	 * 注意实际内容读取仍可能失败。但是，{@code false} 值明确指示资源内容无法读取。
	 * 
	 * @see #getInputStream()
	 * @see #exists()
	 */
	default boolean isReadable() {
		return exists();
	}

	/**
	 * <p>指示此资源是否表示带有已打开流的句柄。如果 {@code true}，则 InputStream 不能被多次读取，必须读取并关闭以避免资源泄漏。
	 * <p>对于典型的资源描述符，将返回 {@code false}。
	 */
	default boolean isOpen() {
		return false;
	}

	/**
	 * <p>确定此资源是否表示文件系统中的文件。
	 * <p>{@code true} 值强烈建议（但不保证）{@link #getFile()} 调用将成功。
	 * <p>默认情况下保守地返回 {@code false}。
	 * 
	 * @since 5.0
	 * @see #getFile()
	 */
	default boolean isFile() {
		return false;
	}

	/**
	 * <p>返回此资源的 URL 句柄。
	 * 
	 * @throws IOException 如果资源无法解析为 URL（即资源不可用为描述符）
	 */
	URL getURL() throws IOException;

	/**
	 * <p>返回此资源的 URI 句柄。
	 * 
	 * @throws IOException 如果资源无法解析为 URI（即资源不可用为描述符）
	 * @since 2.5
	 */
	URI getURI() throws IOException;

	/**
	 * <p>返回此资源的 File 句柄。
	 * <p>注意：这只适用于默认文件系统中的文件。
	 * 
	 * @throws UnsupportedOperationException 如果资源是文件但不能公开为 {@code java.io.File}
	 * @throws java.io.FileNotFoundException 如果资源无法解析为文件
	 * @throws IOException 如果出现常规解析/读取失败
	 * @see #getInputStream()
	 */
	File getFile() throws IOException;

	/**
	 * <p>返回 {@link ReadableByteChannel}。
	 * <p>期望每次调用创建一个<i>新的</i>通道。
	 * <p>默认实现返回 {@link Channels#newChannel(InputStream)} 并使用 {@link #getInputStream()} 的结果。
	 * 
	 * @return 底层资源的字节通道（不能为 {@code null}）
	 * @throws java.io.FileNotFoundException 如果底层资源不存在
	 * @throws IOException 如果内容通道无法打开
	 * @since 5.0
	 * @see #getInputStream()
	 */
	default ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	/**
	 * <p>将此资源的内容作为字节数组返回。
	 * 
	 * @return 此资源的内容作为字节数组
	 * @throws java.io.FileNotFoundException 如果资源无法解析为绝对文件路径（即资源在文件系统中不可用）
	 * @throws IOException 如果出现常规解析/读取失败
	 * @since 6.0.5
	 */
	default byte[] getContentAsByteArray() throws IOException {
		return FileCopyUtils.copyToByteArray(getInputStream());
	}

	/**
	 * <p>将此资源的内容作为字符串返回，使用指定的字符集。
	 * 
	 * @param charset 用于解码的字符集
	 * @return 此资源的内容作为 {@code String}
	 * @throws java.io.FileNotFoundException 如果资源无法解析为绝对文件路径（即资源在文件系统中不可用）
	 * @throws IOException 如果出现常规解析/读取失败
	 * @since 6.0.5
	 */
	default String getContentAsString(Charset charset) throws IOException {
		return FileCopyUtils.copyToString(new InputStreamReader(getInputStream(), charset));
	}

	/**
	 * <p>确定此资源的内容长度。
	 * 
	 * @throws IOException 如果资源无法解析（在文件系统中或作为其他已知物理资源类型）
	 */
	long contentLength() throws IOException;

	/**
	 * <p>确定此资源的最后修改时间戳。
	 * 
	 * @throws IOException 如果资源无法解析（在文件系统中或作为其他已知物理资源类型）
	 */
	long lastModified() throws IOException;

	/**
	 * <p>创建相对于此资源的资源。
	 * 
	 * @param relativePath 相对路径（相对于此资源）
	 * @return 相对资源的资源句柄
	 * @throws IOException 如果无法确定相对资源
	 */
	Resource createRelative(String relativePath) throws IOException;

	/**
	 * <p>确定此资源的文件名——通常是路径的最后部分——例如 {@code "myfile.txt"}。
	 * <p>如果此类型的资源没有文件名，则返回 {@code null}。
	 * <p>鼓励实现返回未编码的文件名。
	 */
	@Nullable
	String getFilename();

	/**
	 * <p>返回此资源的描述，用于处理资源时的错误输出。
	 * <p>鼓励实现也从其 {@code toString} 方法返回此值。
	 * 
	 * @see Object#toString()
	 */
	String getDescription();

}
