/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core.io;

/**
 * <p>从封闭"上下文"加载的资源的扩展接口，例如，从 {@link jakarta.servlet.ServletContext}，
 * 但也包括普通的classpath路径或相对文件系统路径（没有显式前缀指定，因此相对于本地
 * {@link ResourceLoader} 的上下文应用）。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.web.context.support.ServletContextResource
 */
public interface ContextResource extends Resource {

	/**
	 * <p>返回封闭'上下文'中的路径。
	 * <p>这通常是相对于特定上下文的根目录的路径，例如 ServletContext 根或 PortletContext 根。
	 */
	String getPathWithinContext();

}
