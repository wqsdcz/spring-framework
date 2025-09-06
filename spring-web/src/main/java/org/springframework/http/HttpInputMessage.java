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

package org.springframework.http;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>表示一个 HTTP 输入消息，该消息包含 {@linkplain #getHeaders() 头部} 和 可读的 {@linkplain #getBody() 主体}。</p>
 * <p>通常由【服务器端的 HTTP 请求处理程序】或【客户端的 HTTP 响应处理程序】来实现。</p>
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface HttpInputMessage extends HttpMessage {

	/**
	 * 将消息的主体作为输入流返回。
	 * @return 输入流主体（绝不会为 {@code null}）
	 * @throws IOException 如果出现 I/O 错误则抛出此异常
	 */
	InputStream getBody() throws IOException;

}
