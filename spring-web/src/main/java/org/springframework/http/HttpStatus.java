/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * HTTP状态码的枚举类型。
 *
 * <p>可通过 {@link #series()} 方法获取HTTP状态码系列。
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 3.0
 * @see HttpStatus.Series
 * @see <a href="https://www.iana.org/assignments/http-status-codes">HTTP状态码注册表</a>
 * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">HTTP状态码列表 - Wikipedia</a>
 */
public enum HttpStatus {

	// 1xx 信息性状态

	/**
	 * {@code 100 Continue} (继续)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.2.1">HTTP/1.1: Semantics and Content, section 6.2.1</a>
	 */
	CONTINUE(100, "Continue"),
	/**
	 * {@code 101 Switching Protocols} (切换协议)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.2.2">HTTP/1.1: Semantics and Content, section 6.2.2</a>
	 */
	SWITCHING_PROTOCOLS(101, "Switching Protocols"),
	/**
	 * {@code 102 Processing} (处理中)。
	 * @see <a href="https://tools.ietf.org/html/rfc2518#section-10.1">WebDAV</a>
	 */
	PROCESSING(102, "Processing"),
	/**
	 * {@code 103 Checkpoint} (检查点)。
	 * @see <a href="https://code.google.com/p/gears/wiki/ResumableHttpRequestsProposal">支持HTTP/1.0中可恢复的POST/PUT HTTP请求的提案</a>
	 */
	CHECKPOINT(103, "Checkpoint"),

    // 2xx 成功状态

	/**
	 * {@code 200 OK} (成功)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.1">HTTP/1.1: Semantics and Content, section 6.3.1</a>
	 */
	OK(200, "OK"),
	/**
	 * {@code 201 Created} (已创建)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.2">HTTP/1.1: Semantics and Content, section 6.3.2</a>
	 */
	CREATED(201, "Created"),
	/**
	 * {@code 202 Accepted} (已接受)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.3">HTTP/1.1: Semantics and Content, section 6.3.3</a>
	 */
	ACCEPTED(202, "Accepted"),
	/**
	 * {@code 203 Non-Authoritative Information} (非权威信息)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.4">HTTP/1.1: Semantics and Content, section 6.3.4</a>
	 */
	NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),
	/**
	 * {@code 204 No Content} (无内容)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.5">HTTP/1.1: Semantics and Content, section 6.3.5</a>
	 */
	NO_CONTENT(204, "No Content"),
	/**
	 * {@code 205 Reset Content} (重置内容)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.6">HTTP/1.1: Semantics and Content, section 6.3.6</a>
	 */
	RESET_CONTENT(205, "Reset Content"),
	/**
	 * {@code 206 Partial Content} (部分内容)。
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-4.1">HTTP/1.1: Range Requests, section 4.1</a>
	 */
	PARTIAL_CONTENT(206, "Partial Content"),
	/**
	 * {@code 207 Multi-Status} (多状态)。
	 * @see <a href="https://tools.ietf.org/html/rfc4918#section-13">WebDAV</a>
	 */
	MULTI_STATUS(207, "Multi-Status"),
	/**
	 * {@code 208 Already Reported} (已报告)。
	 * @see <a href="https://tools.ietf.org/html/rfc5842#section-7.1">WebDAV Binding Extensions</a>
	 */
	ALREADY_REPORTED(208, "Already Reported"),
	/**
	 * {@code 226 IM Used} (IM已使用)。
	 * @see <a href="https://tools.ietf.org/html/rfc3229#section-10.4.1">Delta encoding in HTTP</a>
	 */
	IM_USED(226, "IM Used"),

	// 3xx 重定向状态

	/**
	 * {@code 300 Multiple Choices} (多种选择)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.1">HTTP/1.1: Semantics and Content, section 6.4.1</a>
	 */
	MULTIPLE_CHOICES(300, "Multiple Choices"),
	/**
	 * {@code 301 Moved Permanently} (永久移动)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.2">HTTP/1.1: Semantics and Content, section 6.4.2</a>
	 */
	MOVED_PERMANENTLY(301, "Moved Permanently"),
	/**
	 * {@code 302 Found} (临时移动)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.3">HTTP/1.1: Semantics and Content, section 6.4.3</a>
	 */
	FOUND(302, "Found"),
	/**
	 * {@code 302 Moved Temporarily} (临时移动)。
	 * @see <a href="https://tools.ietf.org/html/rfc1945#section-9.3">HTTP/1.0, section 9.3</a>
	 * @deprecated 由 {@link #FOUND} 替代，{@code HttpStatus.valueOf(302)} 将返回后者
	 */
	@Deprecated
	MOVED_TEMPORARILY(302, "Moved Temporarily"),
	/**
	 * {@code 303 See Other} (查看其他位置)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.4">HTTP/1.1: Semantics and Content, section 6.4.4</a>
	 */
	SEE_OTHER(303, "See Other"),
	/**
	 * {@code 304 Not Modified} (未修改)。
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-4.1">HTTP/1.1: Conditional Requests, section 4.1</a>
	 */
	NOT_MODIFIED(304, "Not Modified"),
	/**
	 * {@code 305 Use Proxy} (使用代理)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.5">HTTP/1.1: Semantics and Content, section 6.4.5</a>
	 * @deprecated 由于代理带内配置的安全问题已弃用
	 */
	@Deprecated
	USE_PROXY(305, "Use Proxy"),
	/**
	 * {@code 307 Temporary Redirect} (临时重定向)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.7">HTTP/1.1: Semantics and Content, section 6.4.7</a>
	 */
	TEMPORARY_REDIRECT(307, "Temporary Redirect"),
	/**
	 * {@code 308 Permanent Redirect} (永久重定向)。
	 * @see <a href="https://tools.ietf.org/html/rfc7238">RFC 7238</a>
	 */
	PERMANENT_REDIRECT(308, "Permanent Redirect"),

	// --- 4xx 客户端错误 ---

	/**
	 * {@code 400 Bad Request}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.1">HTTP/1.1: Semantics and Content, section 6.5.1</a>
	 */
	BAD_REQUEST(400, "Bad Request"),
	/**
	 * {@code 401 Unauthorized}.
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-3.1">HTTP/1.1: Authentication, section 3.1</a>
	 */
	UNAUTHORIZED(401, "Unauthorized"),
	/**
	 * {@code 402 Payment Required}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.2">HTTP/1.1: Semantics and Content, section 6.5.2</a>
	 */
	PAYMENT_REQUIRED(402, "Payment Required"),
	/**
	 * {@code 403 Forbidden}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.3">HTTP/1.1: Semantics and Content, section 6.5.3</a>
	 */
	FORBIDDEN(403, "Forbidden"),
	/**
	 * {@code 404 Not Found}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.4">HTTP/1.1: Semantics and Content, section 6.5.4</a>
	 */
	NOT_FOUND(404, "Not Found"),
	/**
	 * {@code 405 Method Not Allowed}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.5">HTTP/1.1: Semantics and Content, section 6.5.5</a>
	 */
	METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
	/**
	 * {@code 406 Not Acceptable}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.6">HTTP/1.1: Semantics and Content, section 6.5.6</a>
	 */
	NOT_ACCEPTABLE(406, "Not Acceptable"),
	/**
	 * {@code 407 Proxy Authentication Required}.
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-3.2">HTTP/1.1: Authentication, section 3.2</a>
	 */
	PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
	/**
	 * {@code 408 Request Timeout}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.7">HTTP/1.1: Semantics and Content, section 6.5.7</a>
	 */
	REQUEST_TIMEOUT(408, "Request Timeout"),
	/**
	 * {@code 409 Conflict}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.8">HTTP/1.1: Semantics and Content, section 6.5.8</a>
	 */
	CONFLICT(409, "Conflict"),
	/**
	 * {@code 410 Gone}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.9">
	 *     HTTP/1.1: Semantics and Content, section 6.5.9</a>
	 */
	GONE(410, "Gone"),
	/**
	 * {@code 411 Length Required}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.10">
	 *     HTTP/1.1: Semantics and Content, section 6.5.10</a>
	 */
	LENGTH_REQUIRED(411, "Length Required"),
	/**
	 * {@code 412 Precondition failed}.
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-4.2">
	 *     HTTP/1.1: Conditional Requests, section 4.2</a>
	 */
	PRECONDITION_FAILED(412, "Precondition Failed"),
	/**
	 * {@code 413 Payload Too Large}.
	 * @since 4.1
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.11">
	 *     HTTP/1.1: Semantics and Content, section 6.5.11</a>
	 */
	PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
	/**
	 * {@code 413 Request Entity Too Large}.
	 * @see <a href="https://tools.ietf.org/html/rfc2616#section-10.4.14">HTTP/1.1, section 10.4.14</a>
	 * @deprecated in favor of {@link #PAYLOAD_TOO_LARGE} which will be
	 * returned from {@code HttpStatus.valueOf(413)}
	 */
	@Deprecated
	REQUEST_ENTITY_TOO_LARGE(413, "Request Entity Too Large"),
	/**
	 * {@code 414 URI Too Long}.
	 * @since 4.1
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.12">
	 *     HTTP/1.1: Semantics and Content, section 6.5.12</a>
	 */
	URI_TOO_LONG(414, "URI Too Long"),
	/**
	 * {@code 414 Request-URI Too Long}.
	 * @see <a href="https://tools.ietf.org/html/rfc2616#section-10.4.15">HTTP/1.1, section 10.4.15</a>
	 * @deprecated in favor of {@link #URI_TOO_LONG} which will be returned from {@code HttpStatus.valueOf(414)}
	 */
	@Deprecated
	REQUEST_URI_TOO_LONG(414, "Request-URI Too Long"),
	/**
	 * {@code 415 Unsupported Media Type}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.13">
	 *     HTTP/1.1: Semantics and Content, section 6.5.13</a>
	 */
	UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
	/**
	 * {@code 416 Requested Range Not Satisfiable}.
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-4.4">HTTP/1.1: Range Requests, section 4.4</a>
	 */
	REQUESTED_RANGE_NOT_SATISFIABLE(416, "Requested range not satisfiable"),
	/**
	 * {@code 417 Expectation Failed}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.14">
	 *     HTTP/1.1: Semantics and Content, section 6.5.14</a>
	 */
	EXPECTATION_FAILED(417, "Expectation Failed"),
	/**
	 * {@code 418 I'm a teapot}.
	 * @see <a href="https://tools.ietf.org/html/rfc2324#section-2.3.2">HTCPCP/1.0</a>
	 */
	I_AM_A_TEAPOT(418, "I'm a teapot"),
	/**
	 * @deprecated See
	 * <a href="https://tools.ietf.org/rfcdiff?difftype=--hwdiff&url2=draft-ietf-webdav-protocol-06.txt">
	 *     WebDAV Draft Changes</a>
	 */
	@Deprecated
	INSUFFICIENT_SPACE_ON_RESOURCE(419, "Insufficient Space On Resource"),
	/**
	 * @deprecated See
	 * <a href="https://tools.ietf.org/rfcdiff?difftype=--hwdiff&url2=draft-ietf-webdav-protocol-06.txt">
	 *     WebDAV Draft Changes</a>
	 */
	@Deprecated
	METHOD_FAILURE(420, "Method Failure"),
	/**
	 * @deprecated
	 * See <a href="https://tools.ietf.org/rfcdiff?difftype=--hwdiff&url2=draft-ietf-webdav-protocol-06.txt">
	 *     WebDAV Draft Changes</a>
	 */
	@Deprecated
	DESTINATION_LOCKED(421, "Destination Locked"),
	/**
	 * {@code 422 Unprocessable Entity}.
	 * @see <a href="https://tools.ietf.org/html/rfc4918#section-11.2">WebDAV</a>
	 */
	UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"),
	/**
	 * {@code 423 Locked}.
	 * @see <a href="https://tools.ietf.org/html/rfc4918#section-11.3">WebDAV</a>
	 */
	LOCKED(423, "Locked"),
	/**
	 * {@code 424 Failed Dependency}.
	 * @see <a href="https://tools.ietf.org/html/rfc4918#section-11.4">WebDAV</a>
	 */
	FAILED_DEPENDENCY(424, "Failed Dependency"),
	/**
	 * {@code 426 Upgrade Required}.
	 * @see <a href="https://tools.ietf.org/html/rfc2817#section-6">Upgrading to TLS Within HTTP/1.1</a>
	 */
	UPGRADE_REQUIRED(426, "Upgrade Required"),
	/**
	 * {@code 428 Precondition Required}.
	 * @see <a href="https://tools.ietf.org/html/rfc6585#section-3">Additional HTTP Status Codes</a>
	 */
	PRECONDITION_REQUIRED(428, "Precondition Required"),
	/**
	 * {@code 429 Too Many Requests}.
	 * @see <a href="https://tools.ietf.org/html/rfc6585#section-4">Additional HTTP Status Codes</a>
	 */
	TOO_MANY_REQUESTS(429, "Too Many Requests"),
	/**
	 * {@code 431 Request Header Fields Too Large}.
	 * @see <a href="https://tools.ietf.org/html/rfc6585#section-5">Additional HTTP Status Codes</a>
	 */
	REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),
	/**
	 * {@code 451 Unavailable For Legal Reasons}.
	 * @see <a href="https://tools.ietf.org/html/draft-ietf-httpbis-legally-restricted-status-04">
	 * An HTTP Status Code to Report Legal Obstacles</a>
	 * @since 4.3
	 */
	UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable For Legal Reasons"),

	// --- 5xx 服务器错误 ---

	/**
	 * {@code 500 Internal Server Error} (内部服务器错误)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.1">HTTP/1.1: Semantics and Content, section 6.6.1</a>
	 */
	INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
	/**
	 * {@code 501 Not Implemented} (未实现)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.2">HTTP/1.1: Semantics and Content, section 6.6.2</a>
	 */
	NOT_IMPLEMENTED(501, "Not Implemented"),
	/**
	 * {@code 502 Bad Gateway} (错误网关)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.3">HTTP/1.1: Semantics and Content, section 6.6.3</a>
	 */
	BAD_GATEWAY(502, "Bad Gateway"),
	/**
	 * {@code 503 Service Unavailable} (服务不可用)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.4">HTTP/1.1: Semantics and Content, section 6.6.4</a>
	 */
	SERVICE_UNAVAILABLE(503, "Service Unavailable"),
	/**
	 * {@code 504 Gateway Timeout} (网关超时)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.5">HTTP/1.1: Semantics and Content, section 6.6.5</a>
	 */
	GATEWAY_TIMEOUT(504, "Gateway Timeout"),
	/**
	 * {@code 505 HTTP Version Not Supported} (HTTP版本不受支持)。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.6">HTTP/1.1: Semantics and Content, section 6.6.6</a>
	 */
	HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version not supported"),
	/**
	 * {@code 506 Variant Also Negotiates} (变体仍需协商)
	 * @see <a href="https://tools.ietf.org/html/rfc2295#section-8.1">透明内容协商</a>
	 */
	VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates"),
	/**
	 * {@code 507 Insufficient Storage} (存储空间不足)
	 * @see <a href="https://tools.ietf.org/html/rfc4918#section-11.5">WebDAV</a>
	 */
	INSUFFICIENT_STORAGE(507, "Insufficient Storage"),
	/**
	 * {@code 508 Loop Detected} (检测到循环)
	 * @see <a href="https://tools.ietf.org/html/rfc5842#section-7.2">WebDAV绑定扩展</a>
	 */
	LOOP_DETECTED(508, "Loop Detected"),
	/**
	 * {@code 509 Bandwidth Limit Exceeded} (超出带宽限制)
	 */
	BANDWIDTH_LIMIT_EXCEEDED(509, "Bandwidth Limit Exceeded"),
	/**
	 * {@code 510 Not Extended} (未扩展)
	 * @see <a href="https://tools.ietf.org/html/rfc2774#section-7">HTTP扩展框架</a>
	 */
	NOT_EXTENDED(510, "Not Extended"),
	/**
	 * {@code 511 Network Authentication Required} (要求网络认证)。
	 * @see <a href="https://tools.ietf.org/html/rfc6585#section-6">附加HTTP状态码</a>
	 */
	NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required");


	private final int value;

	private final String reasonPhrase;


	HttpStatus(int value, String reasonPhrase) {
		this.value = value;
		this.reasonPhrase = reasonPhrase;
	}


	/**
	 * 返回此状态码的整数值。
	 */
	public int value() {
		return this.value;
	}

	/**
	 * 返回此状态码的原因短语。
	 */
	public String getReasonPhrase() {
		return this.reasonPhrase;
	}

	/**
	 * 返回此状态码的HTTP状态系列。
	 * @see HttpStatus.Series
	 */
	public Series series() {
		return Series.valueOf(this);
	}

	/**
	 * 判断此状态码是否属于{@link org.springframework.http.HttpStatus.Series#INFORMATIONAL} HTTP系列。
	 * 这是检查{@link #series()}值的快捷方式。
	 * @since 4.0
	 * @see #series()
	 */
	public boolean is1xxInformational() {
		return (series() == Series.INFORMATIONAL);
	}

	/**
	 * 判断此状态码是否属于{@link org.springframework.http.HttpStatus.Series#SUCCESSFUL} HTTP系列。
	 * 这是检查{@link #series()}值的快捷方式。
	 * @since 4.0
	 * @see #series()
	 */
	public boolean is2xxSuccessful() {
		return (series() == Series.SUCCESSFUL);
	}

	/**
	 * 判断此状态码是否属于{@link org.springframework.http.HttpStatus.Series#REDIRECTION} HTTP系列。
	 * 这是检查{@link #series()}值的快捷方式。
	 * @since 4.0
	 * @see #series()
	 */
	public boolean is3xxRedirection() {
		return (series() == Series.REDIRECTION);
	}

	/**
	 * 判断此状态码是否属于{@link org.springframework.http.HttpStatus.Series#CLIENT_ERROR} HTTP系列。
	 * 这是检查{@link #series()}值的快捷方式。
	 * @since 4.0
	 * @see #series()
	 */
	public boolean is4xxClientError() {
		return (series() == Series.CLIENT_ERROR);
	}

	/**
	 * 判断此状态码是否属于{@link org.springframework.http.HttpStatus.Series#SERVER_ERROR} HTTP系列。
	 * 这是检查{@link #series()}值的快捷方式。
	 * @since 4.0
	 * @see #series()
	 */
	public boolean is5xxServerError() {
		return (series() == Series.SERVER_ERROR);
	}

	/**
	 * 判断此状态码是否属于
	 * {@link org.springframework.http.HttpStatus.Series#CLIENT_ERROR} 或
	 * {@link org.springframework.http.HttpStatus.Series#SERVER_ERROR} HTTP系列。
	 * 这是检查{@link #series()}值的快捷方式。
	 * @since 5.0
	 * @see #is4xxClientError()
	 * @see #is5xxServerError()
	 */
	public boolean isError() {
		return (is4xxClientError() || is5xxServerError());
	}

	/**
	 * 返回此状态码的字符串表示形式。
	 */
	@Override
	public String toString() {
		return Integer.toString(this.value);
	}


	/**
	 * 返回具有指定数值的此类型枚举常量。
	 * @param statusCode 要返回的枚举数值
	 * @return 具有指定数值的枚举常量
	 * @throws IllegalArgumentException 如果此枚举没有对应指定数值的常量
	 */
	public static HttpStatus valueOf(int statusCode) {
		HttpStatus status = resolve(statusCode);
		if (status == null) {
			throw new IllegalArgumentException("No matching constant for [" + statusCode + "]");
		}
		return status;
	}

	/**
	 * 将给定的状态码解析为 {@code HttpStatus}（如果可能）。
	 * @param statusCode HTTP状态码（可能为非标准码）
	 * @return 对应的 {@code HttpStatus}，如果未找到则返回 {@code null}
	 * @since 5.0
	 */
	@Nullable
	public static HttpStatus resolve(int statusCode) {
		for (HttpStatus status : values()) {
			if (status.value == statusCode) {
				return status;
			}
		}
		return null;
	}


	/**
	 * HTTP状态系列的枚举类型。
	 * <p>可通过{@link HttpStatus#series()}获取。
	 */
	public enum Series {

		INFORMATIONAL(1),
		SUCCESSFUL(2),
		REDIRECTION(3),
		CLIENT_ERROR(4),
		SERVER_ERROR(5);

		private final int value;

		Series(int value) {
			this.value = value;
		}

		/**
		 * 返回此状态系列的整数值。范围为1到5。
		 */
		public int value() {
			return this.value;
		}

		/**
		 * 根据对应的状态系列返回此类型的枚举常量。
		 * @param status 标准HTTP状态枚举值
		 * @return 对应系列的枚举常量
		 * @throws IllegalArgumentException 如果此枚举没有对应的常量
		 */
		public static Series valueOf(HttpStatus status) {
			return valueOf(status.value);
		}

		/**
		 * 根据对应的状态系列返回此类型的枚举常量。
		 * @param statusCode HTTP状态码（可能为非标准码）
		 * @return 对应系列的枚举常量
		 * @throws IllegalArgumentException 如果此枚举没有对应的常量
		 */
		public static Series valueOf(int statusCode) {
			int seriesCode = statusCode / 100;
			for (Series series : values()) {
				if (series.value == seriesCode) {
					return series;
				}
			}
			throw new IllegalArgumentException("No matching constant for [" + statusCode + "]");
		}
	}

}
