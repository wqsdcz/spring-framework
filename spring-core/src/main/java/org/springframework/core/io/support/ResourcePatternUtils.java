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

import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * 用于确定给定 URL 是否是可以通过 {@link ResourcePatternResolver} 加载的资源位置的工具类。
 *
 * <p>调用者通常会假设如果 {@link #isUrl(String)} 方法返回 {@code false}，则位置是相对路径。
 *
 * @author Juergen Hoeller
 * @since 1.2.3
 */
public abstract class ResourcePatternUtils {

	/**
	 * 返回给定的资源位置是否是 URL：要么是特殊的 "classpath" 或 "classpath*" 伪 URL，
	 * 要么是标准 URL。
	 * @param resourceLocation 要检查的位置字符串
	 * @return 位置是否符合 URL 的条件
	 * @see ResourcePatternResolver#CLASSPATH_ALL_URL_PREFIX
	 * @see org.springframework.util.ResourceUtils#CLASSPATH_URL_PREFIX
	 * @see org.springframework.util.ResourceUtils#isUrl(String)
	 * @see java.net.URL
	 */
	public static boolean isUrl(@Nullable String resourceLocation) {
		return (resourceLocation != null &&
				(resourceLocation.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX) ||
						ResourceUtils.isUrl(resourceLocation)));
	}

	/**
	 * 为给定的 {@link ResourceLoader} 返回默认的 {@link ResourcePatternResolver}。
	 * <p>这可能是 {@code ResourceLoader} 本身，如果它实现了 {@code ResourcePatternResolver} 扩展，
	 * 或者是基于给定 {@code ResourceLoader} 构建的默认 {@link PathMatchingResourcePatternResolver}。
	 * @param resourceLoader 要为其构建模式解析器的 ResourceLoader
	 * （可能是 {@code null} 以表示默认 ResourceLoader）
	 * @return ResourcePatternResolver
	 * @see PathMatchingResourcePatternResolver
	 */
	public static ResourcePatternResolver getResourcePatternResolver(@Nullable ResourceLoader resourceLoader) {
		if (resourceLoader instanceof ResourcePatternResolver resolver) {
			return resolver;
		}
		else if (resourceLoader != null) {
			return new PathMatchingResourcePatternResolver(resourceLoader);
		}
		else {
			return new PathMatchingResourcePatternResolver();
		}
	}

}
