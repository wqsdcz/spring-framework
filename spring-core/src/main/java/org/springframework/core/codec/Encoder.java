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

import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

/**
 * <p>将 {@code <T>} 类型对象流编码为字节输出流的策略。<p/>
 *
 * Strategy to encode a stream of Objects of type {@code <T>} into an output
 * stream of bytes.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> the type of elements in the input stream
 */
public interface Encoder<T> {

	/**
	 * 判断该编码器是否支持给定的源流元素类型及输出流 MIME 类型。
	 * @param elementType 源流的元素类型
	 * @param mimeType 输出流的 MIME 类型（未指定时可传 {@code null}）
	 * @return {@code true} 支持，{@code false} 不支持
	 *
	 * Whether the encoder supports the given source element type and the MIME
	 * type for the output stream.
	 * @param elementType the type of elements in the source stream
	 * @param mimeType the MIME type for the output stream
	 * (can be {@code null} if not specified)
	 * @return {@code true} if supported, {@code false} otherwise
	 */
	boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType);

	/**
	 * 将 {@code T} 类型对象流编码为 {@link DataBuffer} 输出流。
	 *
	 * @param inputStream 待编码的对象输入流（若需编码为单值而非元素流，请使用 {@link Mono} 实例）
	 * @param bufferFactory 创建输出流 {@code DataBuffer} 的缓冲区工厂
	 * @param elementType 输入流的元素预期类型（必须预先通过 {@link #canEncode} 校验并返回 {@code true}）
	 * @param mimeType 输出流的 MIME 类型（可选）
	 * @param hints 编码操作的附加配置信息
	 * @return 编码后的输出流
	 *
	 * Encode a stream of Objects of type {@code T} into a {@link DataBuffer}
	 * output stream.
	 * @param inputStream the input stream of Objects to encode. If the input should be
	 * encoded as a single value rather than as a stream of elements, an instance of
	 * {@link Mono} should be used.
	 * @param bufferFactory for creating output stream {@code DataBuffer}'s
	 * @param elementType the expected type of elements in the input stream;
	 * this type must have been previously passed to the {@link #canEncode}
	 * method and it must have returned {@code true}.
	 * @param mimeType the MIME type for the output stream (optional)
	 * @param hints additional information about how to do encode
	 * @return the output stream
	 */
	Flux<DataBuffer> encode(Publisher<? extends T> inputStream, DataBufferFactory bufferFactory,
			ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

	/**
	 * 返回给定项目的字节长度（若已知）。
	 * @param t 待检查的项目
	 * @return 字节长度（未知时返回 {@code null}）
	 * @since 5.0.5
	 * @deprecated 此方法因 {@code EncoderHttpMessageWriter} 需设置内容长度头而添加，
	 * 但在 5.0.7 版本架构改进后已废弃且不再使用。
	 *
	 * Return the length for the given item, if known.
	 * @param t the item to check
	 * @return the length in bytes, or {@code null} if not known.
	 * @since 5.0.5
	 * @deprecated this method was added so {@code EncoderHttpMessageWriter}
	 * can set the content-length header. However after further improvements as
	 * of 5.0.7, it is no longer needed, and not used.
	 */
	@Nullable
	@Deprecated
	default Long getContentLength(T t, @Nullable MimeType mimeType) {
		return null;
	}

	/**
	 * 返回此编码器支持的MIME类型列表。
	 * Return the list of mime types this encoder supports.
	 */
	List<MimeType> getEncodableMimeTypes();

}
