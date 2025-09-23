/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet;

import java.util.Locale;

import org.springframework.lang.Nullable;

/**
 * 用于通过名称解析视图的对象需要实现的接口。
 *
 * <p>视图状态在应用程序运行期间不会改变，因此实现类可以自由缓存视图。
 *
 * <p>鼓励实现类支持国际化，即本地化的视图解析。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.servlet.view.InternalResourceViewResolver
 * @see org.springframework.web.servlet.view.ResourceBundleViewResolver
 * @see org.springframework.web.servlet.view.XmlViewResolver
 */
public interface ViewResolver {

	/**
	 * 根据给定名称解析对应的视图。
	 * <p>
	 *     注意：为了支持视图解析器链，如果当前解析器中未定义指定名称的视图，则应返回 {@code null}。
	 *     但这不是强制要求：某些视图解析器会始终尝试使用给定名称构建视图对象，无法返回 {@code null}（而是在视图创建失败时抛出异常）。
	 *
	 * @param viewName 要解析的视图名称
	 * @param locale 用于解析视图的区域设置。支持国际化的视图解析器应遵守此参数。
	 * @return 视图对象，如果未找到则返回 {@code null}（可选，以支持视图解析器链）
	 * @throws Exception 如果无法解析视图（通常在创建实际视图对象时出现问题的情况下抛出）
	 */
	@Nullable
	View resolveViewName(String viewName, Locale locale) throws Exception;

}
