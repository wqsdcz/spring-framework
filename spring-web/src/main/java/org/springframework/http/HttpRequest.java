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

import java.net.URI;

import org.springframework.lang.Nullable;

/**
 * 表示一个 HTTP 请求消息，它由 {@linkplain #getMethod() 方法} 和 {@linkplain #getURI() 地址} 组成。
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public interface HttpRequest extends HttpMessage {

	/**
	 * 返回请求的 HTTP 方法。
	 * @return 以 HttpMethod 枚举值的形式返回 HTTP 方法，若无法解析则返回 {@code null}（例如在非标准 HTTP 方法的情况下）
	 * @see #getMethodValue()
	 * @see HttpMethod#resolve(String)
	 */
	@Nullable
	default HttpMethod getMethod() {
		return HttpMethod.resolve(getMethodValue());
	}

	/**
	 * 将请求的 HTTP 方法以字符串形式返回。
	 * @return HTTP 方法作为纯字符串形式
	 * @since 5.0
	 * @see #getMethod()
	 */
	String getMethodValue();

	/**
	 * 返回请求的 URI（如果存在查询字符串，则包括其中的内容，但仅在该字符串符合 URI 表示形式的规范时才包含）。
	 * @return URI（绝不会为 {@code null}）
	 */
	URI getURI();

}
