/*
 * Copyright 2002-2016 the original author or authors.
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
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

/**
 * <p>将 {@link DataBuffer} 输入流 解码为 {@code <T>} 类型元素输出流的策略。<p/>
 *
 * Strategy for decoding a {@link DataBuffer} input stream into an output stream
 * of elements of type {@code <T>}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> the type of elements in the output stream
 */
public interface Decoder<T> {

	/**
	 * 判断该解码器是否支持给定的目标元素类型及源流 MIME 类型。
	 * @param elementType 输出流的目标元素类型
	 * @param mimeType 待解码流的 MIME 类型（未指定时可传 {@code null}）
	 * @return {@code true} 支持，{@code false} 不支持
	 *
	 * Whether the decoder supports the given target element type and the MIME
	 * type of the source stream.
	 * @param elementType the target element type for the output stream
	 * @param mimeType the mime type associated with the stream to decode
	 * (can be {@code null} if not specified)
	 * @return {@code true} if supported, {@code false} otherwise
	 */
	boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType);

	/**
	 * 将 {@link DataBuffer} 输入流解码为 {@code T} 类型 {@code Flux} 流。
	 *
	 * @param inputStream 待解码的 {@code DataBuffer} 输入流
	 * @param elementType 输出流的元素预期类型（必须预先通过 {@link #canDecode} 校验并返回 {@code true}）
	 * @param mimeType 输入流的 MIME 类型（可选）
	 * @param hints 解码操作的附加配置信息
	 * @return 包含已解码元素的输出流
	 *
	 * Decode a {@link DataBuffer} input stream into a Flux of {@code T}.
	 * @param inputStream the {@code DataBuffer} input stream to decode
	 * @param elementType the expected type of elements in the output stream;
	 * this type must have been previously passed to the {@link #canDecode}
	 * method and it must have returned {@code true}.
	 * @param mimeType the MIME type associated with the input stream (optional)
	 * @param hints additional information about how to do encode
	 * @return the output stream with decoded elements
	 */
	Flux<T> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

	/**
	 * 将 {@link DataBuffer} 输入流解码为 {@code T} 类型 {@code Mono} 流。
	 *
	 * @param inputStream 待解码的 {@code DataBuffer} 输入流
	 * @param elementType 输出流的元素预期类型（必须预先通过 {@link #canDecode} 校验并返回 {@code true}）
	 * @param mimeType 输入流的 MIME 类型（可选）
	 * @param hints 解码操作的附加配置信息
	 * @return 包含已解码元素的输出流
	 *
	 * Decode a {@link DataBuffer} input stream into a Mono of {@code T}.
	 * @param inputStream the {@code DataBuffer} input stream to decode
	 * @param elementType the expected type of elements in the output stream;
	 * this type must have been previously passed to the {@link #canDecode}
	 * method and it must have returned {@code true}.
	 * @param mimeType the MIME type associated with the input stream (optional)
	 * @param hints additional information about how to do encode
	 * @return the output stream with the decoded element
	 */
	Mono<T> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

	/**
	 * 返回此解码器支持的MIME类型列表。
	 * Return the list of MIME types this decoder supports.
	 */
	List<MimeType> getDecodableMimeTypes();

}
