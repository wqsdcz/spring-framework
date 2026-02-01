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
import java.io.InputStream;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * <p>给定 {@link InputStream} 或给定 {@link InputStreamSource}（可以作为 lambda 表达式提供）的 {@link Resource} 实现，
 * 用于按需延迟获取 {@link InputStream}。
 *
 * <p>仅在没有其他特定的 {@code Resource} 实现适用时才应使用。如果可能，优先使用 {@link ByteArrayResource} 
 * 或任何基于文件的 {@code Resource} 实现。如果你需要多次获取自定义流，请使用带有相应 {@code getInputStream()} 
 * 实现的自定义 {@link AbstractResource} 子类。
 *
 * <p>与其他 {@code Resource} 实现相比，这是<i>已打开</i>资源的描述符——因此从 {@link #isOpen()} 返回 
 * {@code true}。如果你需要将资源描述符保留在某处，或者需要从流中多次读取，请不要使用 {@code InputStreamResource}。
 * 这也适用于使用 {@code InputStreamSource} 构造的情况，它延迟获取流但仅允许单次访问。
 *
 * <p><b>注意：此类不提供独立的 {@link #contentLength()} 实现：任何此类调用都将消费给定的 {@code InputStream}！</b>
 * 如果可能，考虑用自定义实现覆盖 {@code #contentLength()}。出于任何其他目的，不建议从此类扩展；
 * 当与 Spring 的 Web 资源渲染一起使用时尤其如此，它专门为这个确切的类跳过 {@code #contentLength()}。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 28.12.2003
 * @see ByteArrayResource
 * @see ClassPathResource
 * @see FileSystemResource
 * @see UrlResource
 */
public class InputStreamResource extends AbstractResource {

	private final InputStreamSource inputStreamSource;

	private final String description;

	private final Object equality;

	private boolean read = false;


	/**
	 * <p>使用延迟的 {@code InputStream} 创建新的 {@code InputStreamResource} 供单次使用。
	 *
	 * @param inputStreamSource 单次使用 InputStream 的按需源
	 * @since 6.1.7
	 */
	public InputStreamResource(InputStreamSource inputStreamSource) {
		this(inputStreamSource, "resource loaded from InputStreamSource");
	}

	/**
	 * <p>使用延迟的 {@code InputStream} 创建新的 {@code InputStreamResource} 供单次使用。
	 *
	 * @param inputStreamSource 单次使用 InputStream 的按需源
	 * @param description InputStream 的来源
	 * @since 6.1.7
	 */
	public InputStreamResource(InputStreamSource inputStreamSource, @Nullable String description) {
		Assert.notNull(inputStreamSource, "InputStreamSource must not be null");
		this.inputStreamSource = inputStreamSource;
		this.description = (description != null ? description : "");
		this.equality = inputStreamSource;
	}

	/**
	 * <p>为现有的 {@code InputStream} 创建新的 {@code InputStreamResource}。
	 * <p>如果可能，考虑按需检索 InputStream，减少其生命周期并通过常规的 
	 * {@link InputStreamSource#getInputStream()} 使用可靠地打开和关闭它。
	 *
	 * @param inputStream 要使用的 InputStream
	 * @see #InputStreamResource(InputStreamSource)
	 */
	public InputStreamResource(InputStream inputStream) {
		this(inputStream, "resource loaded through InputStream");
	}

	/**
	 * <p>为现有的 {@code InputStream} 创建新的 {@code InputStreamResource}。
	 *
	 * @param inputStream 要使用的 InputStream
	 * @param description InputStream 的来源
	 * @see #InputStreamResource(InputStreamSource, String)
	 */
	public InputStreamResource(InputStream inputStream, @Nullable String description) {
		Assert.notNull(inputStream, "InputStream must not be null");
		this.inputStreamSource = () -> inputStream;
		this.description = (description != null ? description : "");
		this.equality = inputStream;
	}


	/**
	 * <p>此实现始终返回 {@code true}。
	 */
	@Override
	public boolean exists() {
		return true;
	}

	/**
	 * <p>此实现始终返回 {@code true}。
	 */
	@Override
	public boolean isOpen() {
		return true;
	}

	/**
	 * <p>此实现如果尝试多次读取底层流，则抛出 IllegalStateException。
	 */
	@Override
	public InputStream getInputStream() throws IOException, IllegalStateException {
		if (this.read) {
			throw new IllegalStateException("InputStream has already been read (possibly for early content length " +
					"determination) - do not use InputStreamResource if a stream needs to be read multiple times");
		}
		this.read = true;
		return this.inputStreamSource.getInputStream();
	}

	/**
	 * <p>此实现返回包含传入的描述（如果有）的描述。
	 */
	@Override
	public String getDescription() {
		return "InputStream resource [" + this.description + "]";
	}


	/**
	 * <p>此实现比较底层 InputStream。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof InputStreamResource that &&
				this.equality.equals(that.equality)));
	}

	/**
	 * <p>此实现返回底层 InputStream 的哈希码。
	 */
	@Override
	public int hashCode() {
		return this.equality.hashCode();
	}

}
