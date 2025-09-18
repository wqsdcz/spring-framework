/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.context.request;

import java.security.Principal;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * 用于Web请求的通用接口。
 * 主要设计用于通用Web请求拦截器，为其提供对常规请求元数据的访问能力，而非实际处理请求。
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 2.0
 * @see WebRequestInterceptor
 */
public interface WebRequest extends RequestAttributes {

	/**
	 * 返回指定名称的请求头值，如不存在则返回 {@code null}。
	 * <p>对于多值请求头，仅返回第一个头值。
	 *
	 * @since 3.0
	 * @see javax.servlet.http.HttpServletRequest#getHeader(String)
	 */
	@Nullable
	String getHeader(String headerName);

	/**
	 * 返回指定头名称的所有请求头值数组，如不存在则返回 {@code null}。
	 * <p>单值请求头将暴露为仅包含单个元素的数组。
	 *
	 * @since 3.0
	 * @see javax.servlet.http.HttpServletRequest#getHeaders(String)
	 */
	@Nullable
	String[] getHeaderValues(String headerName);

	/**
	 * 返回请求头名称的迭代器。
	 *
	 * @since 3.0
	 * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
	 */
	Iterator<String> getHeaderNames();

	/**
	 * 返回指定名称的请求参数值，如不存在则返回 {@code null}。
	 * <p>对于多值参数，仅返回第一个参数值。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getParameter(String)
	 */
	@Nullable
	String getParameter(String paramName);

	/**
	 * 返回指定参数名称的所有参数值数组，如不存在则返回 {@code null}。
	 * <p>单值参数将暴露为仅包含单个元素的数组。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getParameterValues(String)
	 */
	@Nullable
	String[] getParameterValues(String paramName);

	/**
	 * 返回请求参数名称的迭代器。
	 *
	 * @since 3.0
	 * @see javax.servlet.http.HttpServletRequest#getParameterNames()
	 */
	Iterator<String> getParameterNames();

	/**
	 * 返回请求参数的不可变映射，其中参数名作为映射键，参数值作为映射值（值为字符串数组类型）。
	 * <p>单值参数将暴露为仅包含单个元素的数组。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getParameterMap()
	 */
	Map<String, String[]> getParameterMap();

	/**
	 * 返回此请求的主区域设置（Locale）。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getLocale()
	 */
	Locale getLocale();

 	/**
	 * 返回此请求的上下文路径（通常是当前Web应用被映射的根路径）。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getContextPath()
	 */
	String getContextPath();

	/**
	 * 返回此请求的远程用户（如果存在）。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
	 */
	@Nullable
	String getRemoteUser();

	/**
	 * 返回此请求的用户主体（如果存在）。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
	 */
	@Nullable
	Principal getUserPrincipal();

	/**
	 * 判断当前用户是否属于此请求的指定角色。
	 *
	 * @see javax.servlet.http.HttpServletRequest#isUserInRole(String)
	 */
	boolean isUserInRole(String role);

	/**
	 * 返回此请求是否通过安全传输机制（如SSL）发送。
	 *
	 * @see javax.servlet.http.HttpServletRequest#isSecure()
	 */
	boolean isSecure();

	/**
	 * 根据提供的最后修改时间戳（由应用程序确定）检查请求的资源是否已被修改。
	 * <p>该方法还会在适用时自动设置"Last-Modified"响应头和HTTP状态。
	 * <p>典型用法：
	 * <pre class="code">
	 * public String myHandleMethod(WebRequest request, Model model) {
	 *   long lastModified = // 应用特定的计算
	 *   if (request.checkNotModified(lastModified)) {
	 *     // 快速退出 - 无需进一步处理
	 *     return null;
	 *   }
	 *   // 进一步的请求处理，实际构建内容
	 *   model.addAttribute(...);
	 *   return "myViewName";
	 * }</pre>
	 * <p>此方法适用于条件GET/HEAD请求，也适用于条件POST/PUT/DELETE请求。
	 * <p>
	 *     <strong>注意：</strong>
	 *     您可以使用此{@code #checkNotModified(long)}方法，或使用{@link #checkNotModified(String)}方法。
	 *     如需同时强制使用强实体标签和Last-Modified值（按照HTTP规范建议），则应使用{@link #checkNotModified(String, long)}方法。
	 * <p>
	 *     如果"If-Modified-Since"请求头已设置但无法解析为日期值，
	 *     本方法将忽略该头信息并继续在响应中设置最后修改时间戳。
	 *
	 * @param lastModifiedTimestamp 应用程序确定的底层资源的最后修改时间戳（毫秒）
	 * @return 请求是否可判定为未修改，允许中止请求处理并依靠响应告知客户端内容未修改
	 */
	boolean checkNotModified(long lastModifiedTimestamp);

	/**
	 * 根据应用程序提供的{@code ETag}（实体标签）检查请求的资源是否已被修改。
	 * <p>该方法还会在适用时自动设置"ETag"响应头和HTTP状态。
	 * <p>典型用法：
	 * <pre class="code">
	 * public String myHandleMethod(WebRequest request, Model model) {
	 *   String eTag = // 应用特定的计算
	 *   if (request.checkNotModified(eTag)) {
	 *     // 快速退出 - 无需进一步处理
	 *     return null;
	 *   }
	 *   // 进一步的请求处理，实际构建内容
	 *   model.addAttribute(...);
	 *   return "myViewName";
	 * }</pre>
	 * <p>
	 *     <strong>注意：</strong>
	 *     您可以使用此{@code #checkNotModified(String)}方法，或使用{@link #checkNotModified(long)}方法。
	 *     如需同时强制使用强实体标签和Last-Modified值（按照HTTP规范建议），则应使用{@link #checkNotModified(String, long)}方法。
	 *
	 * @param etag 应用程序确定的底层资源的实体标签。如有需要，此参数将自动用引号（"）包裹。
	 * @return 如果请求不需要进一步处理则返回true
	 */
	boolean checkNotModified(String etag);

	/**
	 * 根据应用程序提供的{@code ETag}（实体标签）和最后修改时间戳，检查请求的资源是否已被修改。
	 * <p>该方法还会在适用时自动设置"ETag"和"Last-Modified"响应头，以及相应的HTTP状态。
	 * <p>典型用法：
	 * <pre class="code">
	 * public String myHandleMethod(WebRequest request, Model model) {
	 *   String eTag = // 应用特定的计算
	 *   long lastModified = // 应用特定的计算
	 *   if (request.checkNotModified(eTag, lastModified)) {
	 *     // 快速退出 - 无需进一步处理
	 *     return null;
	 *   }
	 *   // 进一步的请求处理，实际构建内容
	 *   model.addAttribute(...);
	 *   return "myViewName";
	 * }</pre>
	 * <p>此方法适用于条件GET/HEAD请求，也适用于条件POST/PUT/DELETE请求。
	 * <p>
	 *     <strong>注意：</strong>
	 *     HTTP规范建议同时设置ETag和Last-Modified值，但您也可以使用{@code #checkNotModified(String)}或{@link #checkNotModified(long)}方法。
	 *
	 * @param etag 应用程序确定的底层资源的实体标签。如有需要，此参数将自动用引号（"）包裹。
	 * @param lastModifiedTimestamp 应用程序确定的底层资源的最后修改时间戳（毫秒）
	 * @return 如果请求不需要进一步处理则返回true
	 * @since 4.2
	 */
	boolean checkNotModified(@Nullable String etag, long lastModifiedTimestamp);

	/**
	 * 获取此请求的简短描述，通常包含请求URI和会话ID。
	 *
	 * @param includeClientInfo 是否包含客户端特定信息，例如会话ID和用户名
	 * @return 请求的描述字符串
	 */
	String getDescription(boolean includeClientInfo);

}
