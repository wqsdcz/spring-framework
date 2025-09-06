/**
 * Contains a basic abstraction over client/server-side HTTP. This package contains
 * the {@code HttpInputMessage} and {@code HttpOutputMessage} interfaces.
 */
@NonNullApi
@NonNullFields
package org.springframework.http;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;

/**
 * HttpMessage 报头
 * HttpRequest 报头、请求方法、请求URI
 * HttpInputMessage 报头、写入消息体
 * HttpOutputMessage 报头、读取消息体
 * ReactiveHttpInputMessage 报头、写入消息体（反应式）
 * ReactiveHttpOutputMessage 报头、读取消息体（反应式）
 * StreamingHttpOutputMessage 报头、写入消息体（流式）
 * ZeroCopyHttpOutputMessage 报头、写入消息体（零拷贝）
 *
 *
 */
