/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.codec;

import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

/**
 * <p>{@code Decoder} 实现类的抽象基类，支持将 {@code DataBuffer} 直接解码为目标元素类型。<p/>
 *
 * <p>子类必须实现 {@link #decodeDataBuffer} 以提供 {@code DataBuffer} 到目标数据类型的转换逻辑。
 * 默认的 {@link #decode} 实现会**逐个转换**数据缓冲区，
 * 而 {@link #decodeToMono} 执行 **"聚合归约"** 后转换**聚合缓冲区**。
 *
 * <p>子类可重写 {@link #decode} 实现：
 * • 按不同边界分割输入流（如 {@code String} 的换行符）
 * • 或始终归约为单个缓冲区（如 {@code Resource}）。
 *
 * Abstract base class for {@code Decoder} implementations that can decode
 * a {@code DataBuffer} directly to the target element type.
 *
 * <p>Sub-classes must implement {@link #decodeDataBuffer} to provide a way to
 * transform a {@code DataBuffer} to the target data type. The default
 * {@link #decode} implementation transforms each individual data buffer while
 * {@link #decodeToMono} applies "reduce" and transforms the aggregated buffer.
 *
 * <p>Sub-classes can override {@link #decode} in order to split the input stream
 * along different boundaries (e.g. on new line characters for {@code String})
 * or always reduce to a single data buffer (e.g. {@code Resource}).
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractDataBufferDecoder<T> extends AbstractDecoder<T> {


	protected AbstractDataBufferDecoder(MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
	}


	@Override
	public Flux<T> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return Flux.from(inputStream).map(buffer -> decodeDataBuffer(buffer, elementType, mimeType, hints));
	}

	@Override
	public Mono<T> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return DataBufferUtils.join(inputStream)
				.map(buffer -> decodeDataBuffer(buffer, elementType, mimeType, hints));
	}

	/**
	 * How to decode a {@code DataBuffer} to the target element type.
	 */
	protected abstract T decodeDataBuffer(DataBuffer buffer, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

}
