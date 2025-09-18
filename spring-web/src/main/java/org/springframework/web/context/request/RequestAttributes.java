/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * 用于访问与请求相关联的属性对象的抽象接口。
 * 支持访问请求作用域属性及会话作用域属性，并可选择支持"全局会话"的概念。
 *
 * <p>可适配于各类请求/会话机制，特别适用于Servlet请求场景。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ServletRequestAttributes
 */
public interface RequestAttributes {

	/**
	 * 标识请求作用域的常量。
	 */
	int SCOPE_REQUEST = 0;

	/**
	 * 标识会话作用域的常量。
	 * <p>
	 *     在支持会话隔离的环境（例如Portlet环境）中，优先指向本地隔离会话；否则直接指向通用会话。
	 */
	int SCOPE_SESSION = 1;

	/**
	 * 请求对象的标准引用名称："request"。
	 * @see #resolveReference
	 */
	String REFERENCE_REQUEST = "request";

	/**
	 * 会话对象的标准引用名称："session"。
	 * @see #resolveReference
	 */
	String REFERENCE_SESSION = "session";


	/**
	 * 返回指定名称的作用域属性值（如果存在）。
	 *
	 * @param name 属性名称
	 * @param scope 作用域标识符
	 * @return 当前属性值，未找到时返回 {@code null}
	 */
	@Nullable
	Object getAttribute(String name, int scope);

	/**
	 * 设置指定名称的作用域属性值，并替换现有值（如果存在）。
	 *
	 * @param name 属性名称
	 * @param scope 作用域标识符
	 * @param value 要设置的属性值
	 */
	void setAttribute(String name, Object value, int scope);

	/**
	 * 移除指定名称的作用域属性（如果存在）。
	 * <p>
	 *     注意：实现方应同时移除该属性注册的销毁回调（如果存在）。
	 *     但在此场景下<i>不需要</i>执行已注册的销毁回调，因为对象将由调用方负责销毁（如适用）。
	 *
	 * @param name 属性名称
	 * @param scope 作用域标识符
	 */
	void removeAttribute(String name, int scope);

	/**
	 * 获取指定作用域内所有属性的名称。
	 *
	 * @param scope 作用域标识符
	 * @return 属性名称字符串数组
	 */
	String[] getAttributeNames(int scope);

	/**
	 * 注册在给定作用域内指定属性销毁时执行的回调函数。
	 * <p>
	 *     实现方应确保回调在适当时机执行：即在请求完成或会话终止时分别触发。
	 *     若底层运行环境不支持此类回调，则<i>必须忽略</i>该回调并记录相应警告。
	 * <p>
	 *     请注意：'销毁'通常指整个作用域的销毁，而非应用显式移除单个属性时的操作。
	 *     若通过此门面的{@link #removeAttribute(String, int)}方法移除属性，
	 *     则应同时禁用所有已注册的销毁回调——此处假定被移除的对象将被重用或手动销毁。
	 * <p>
	 *     <b>注意：</b>若为会话作用域注册回调，则回调对象通常需支持序列化，
	 *     否则回调（甚至整个会话）可能无法在Web应用重启后保留。
	 *
	 * @param name 要注册回调的属性名称
	 * @param callback 待执行的销毁回调函数
	 * @param scope 作用域标识符
	 */
	void registerDestructionCallback(String name, Runnable callback, int scope);

	/**
	 * 解析指定键对应的上下文引用（如果存在）。
	 * <p>
	 *     至少支持：
	 *     键为"request"时返回HttpServletRequest/PortletRequest引用，
	 *     键为"session"时返回HttpSession/PortletSession引用。
	 *
	 * @param key 上下文键
	 * @return 对应的对象，未找到时返回 {@code null}
	 */
	@Nullable
	Object resolveReference(String key);

	/**
	 * 返回当前底层会话的标识符。
	 *
	 * @return 会话标识字符串（永不返回 {@code null}）
	 */
	String getSessionId();

	/**
	 * 暴露底层会话的最佳互斥锁：即用于同步底层会话的同步对象。
	 *
	 * @return 要使用的会话互斥锁（永不返回 {@code null}）
	 */
	Object getSessionMutex();

}
