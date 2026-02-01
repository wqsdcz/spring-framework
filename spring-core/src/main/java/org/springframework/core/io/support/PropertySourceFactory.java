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

import org.springframework.core.env.PropertySource;
import org.springframework.lang.Nullable;

/**
 * 创建基于资源的 {@link PropertySource} 包装器的策略接口。
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see DefaultPropertySourceFactory
 * @see ResourcePropertySource
 */
public interface PropertySourceFactory {

	/**
	 * 创建一个包装给定资源的 {@link PropertySource}。
	 * <p>实现通常会创建 {@link ResourcePropertySource}
	 * 实例，而 {@link PropertySourceProcessor} 会自动通过
	 * {@link ResourcePropertySource#withResourceName()} 适配
	 * 属性源名称（如有必要），例如，当将同一名称的多个源
	 * 组合成 {@link org.springframework.core.env.CompositePropertySource} 时。
	 * 具有自定义 {@link PropertySource} 类型的自定义实现
	 * 需要确保公开足够不同的名称，可能的话从
	 * {@link ResourcePropertySource} 派生。
	 * @param name 属性源的名称
	 * （可以是 {@code null}，在这种情况下工厂实现
	 * 将不得不基于给定资源生成名称）
	 * @param resource 要包装的资源（可能是编码的）
	 * @return 新的 {@link PropertySource}（永不为 {@code null}）
	 * @throws IOException 如果资源解析失败
	 */
	PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) throws IOException;

}
