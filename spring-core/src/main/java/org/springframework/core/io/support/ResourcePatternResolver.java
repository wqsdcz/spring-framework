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

package org.springframework.core.io.support;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * 将位置模式（例如，Ant风格的路径模式）解析为 {@link Resource} 对象的策略接口。
 *
 * <p>这是对 {@link org.springframework.core.io.ResourceLoader} 接口的扩展。
 * 传入的 {@code ResourceLoader}（例如，在上下文中运行时通过
 * {@link org.springframework.context.ResourceLoaderAware} 传入的
 * {@link org.springframework.context.ApplicationContext}）可以检查是否也实现了此扩展接口。
 *
 * <p>{@link PathMatchingResourcePatternResolver} 是一个独立的实现，
 * 可在 {@code ApplicationContext} 外部使用，也被
 * {@link ResourceArrayPropertyEditor} 用于填充 {@code Resource} 数组 bean 属性。
 *
 * <p>可以与任何类型的位置模式一起使用 &mdash; 例如，
 * {@code "/WEB-INF/*-context.xml"}。然而，输入模式必须匹配
 * 策略实现。此接口只指定转换方法，
 * 而不是特定的模式格式。
 *
 * <p>此接口还定义了 {@value #CLASSPATH_ALL_URL_PREFIX} 资源
 * 前缀，用于从模块路径和类路径中获取所有匹配的资源。请注意
 * 资源位置也可能包含占位符 &mdash; 例如
 * {@code "/beans-*.xml"}。JAR 文件或模块路径
 * 或类路径中的不同目录可以包含多个同名文件。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.0.2
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.ResourceLoader
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 */
public interface ResourcePatternResolver extends ResourceLoader {

	/**
	 * 来自类路径的所有匹配资源的伪 URL 前缀：{@code "classpath*:"}。
	 * <p>这与 ResourceLoader 的 {@code "classpath:"} URL 前缀不同，
	 * 因为它检索给定路径的所有匹配资源 &mdash; 例如，
	 * 要在所有部署的 JAR 文件根目录中查找所有 "beans.xml" 文件，
	 * 您可以使用位置模式 {@code "classpath*:/beans.xml"}。
	 * <p>从 Spring Framework 6.0 开始，{@code "classpath*:"}
	 * 前缀的语义已扩展到包括模块路径以及类路径。
	 * @see org.springframework.core.io.ResourceLoader#CLASSPATH_URL_PREFIX
	 */
	String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

	/**
	 * 将给定的位置模式解析为 {@code Resource} 对象。
	 * <p>应尽可能避免指向同一物理资源的重叠资源条目。
	 * 结果应该具有集合语义。
	 * @param locationPattern 要解析的位置模式
	 * @return 对应的 {@code Resource} 对象
	 * @throws IOException 在出现 I/O 错误的情况下
	 */
	Resource[] getResources(String locationPattern) throws IOException;

}
