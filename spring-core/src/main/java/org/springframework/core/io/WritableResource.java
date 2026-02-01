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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * <p>支持写入的资源扩展接口。提供 {@link #getOutputStream()} 输出流访问器。
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see java.io.OutputStream
 */
public interface WritableResource extends Resource {

	/**
	 * <p>指示是否可以通过 {@link #getOutputStream()} 写入此资源的内容。
	 * <p>对于典型的资源描述符，将返回 {@code true}；
	 * 注意实际内容写入仍可能失败。但是，{@code false} 值明确指示资源内容无法修改。
	 * 
	 * @see #getOutputStream()
	 * @see #isReadable()
	 */
	default boolean isWritable() {
		return true;
	}

	/**
	 * <p>返回底层资源的 {@link OutputStream}，允许（覆盖）写入其内容。
	 * 
	 * @throws IOException 如果流无法打开
	 * @see #getInputStream()
	 */
	OutputStream getOutputStream() throws IOException;

	/**
	 * <p>返回 {@link WritableByteChannel}。
	 * <p>期望每次调用创建一个<i>新的</i>通道。
	 * <p>默认实现返回 {@link Channels#newChannel(OutputStream)} 并使用 {@link #getOutputStream()} 的结果。
	 * 
	 * @return 底层资源的字节通道（不能为 {@code null}）
	 * @throws java.io.FileNotFoundException 如果底层资源不存在
	 * @throws IOException 如果内容通道无法打开
	 * @since 5.0
	 * @see #getOutputStream()
	 */
	default WritableByteChannel writableChannel() throws IOException {
		return Channels.newChannel(getOutputStream());
	}

}
