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

package org.springframework.http;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;

/**
 * <p>一种“响应式”的 HTTP 输出消息，它将输出以 {@link Publisher} 的形式进行接收。</p>
 * <p>通常由【客户端的 HTTP 请求】或【服务器端的 HTTP 响应】来实现。</p>
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface ReactiveHttpOutputMessage extends HttpMessage {

	/**
	 * Return a {@link DataBufferFactory} that can be used to create the body.
	 * @return a buffer factory
	 * @see #writeWith(Publisher)
	 */
	DataBufferFactory bufferFactory();

	/**
	 * Register an action to apply just before the HttpOutputMessage is committed.
	 * <p><strong>Note:</strong> the supplied action must be properly deferred,
	 * e.g. via {@link Mono#defer} or {@link Mono#fromRunnable}, to ensure it's
	 * executed in the right order, relative to other actions.
	 * @param action the action to apply
	 */
	void beforeCommit(Supplier<? extends Mono<Void>> action);

	/**
	 * 是否提交HttpOutputMessage。
	 */
	boolean isCommitted();

	/**
	 * 使用给定的 {@link Publisher} 将消息的主体内容写入底层的 HTTP 层。
	 * @param body 消息主体内容的发布者
	 * @return 一个表示完成或错误的 {@link Mono} 对象
	 */
	Mono<Void> writeWith(Publisher<? extends DataBuffer> body);

	/**
	 * Use the given {@link Publisher} of {@code Publishers} to write the body
	 * of the HttpOutputMessage to the underlying HTTP layer, flushing after
	 * each {@code Publisher<DataBuffer>}.
	 * @param body the body content publisher
	 * @return a {@link Mono} that indicates completion or error
	 */
	Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body);

	/**
	 * Indicate that message handling is complete, allowing for any cleanup or
	 * end-of-processing tasks to be performed such as applying header changes
	 * made via {@link #getHeaders()} to the underlying HTTP message (if not
	 * applied already).
	 * <p>This method should be automatically invoked at the end of message
	 * processing so typically applications should not have to invoke it.
	 * If invoked multiple times it should have no side effects.
	 * @return a {@link Mono} that indicates completion or error
	 */
	Mono<Void> setComplete();

}
