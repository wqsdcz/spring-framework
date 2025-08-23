/**
 * {@link org.springframework.core.codec.Encoder} and
 * {@link org.springframework.core.codec.Decoder} abstractions to convert
 * between a reactive stream of bytes and Java objects.
 */
@NonNullApi
@NonNullFields
package org.springframework.core.codec;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;

/**
 * org.springframework.core.codec 是 Spring Framework 5+（特别是 Spring WebFlux）中的核心编解码模块，
 * 为响应式编程提供了一套统一的编解码抽象。它在处理 HTTP 请求/响应、消息传递等场景中负责数据转换，
 * 支持从字节流到对象的高效转换。
 *
 */
