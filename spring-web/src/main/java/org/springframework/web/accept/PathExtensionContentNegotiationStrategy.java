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

package org.springframework.web.accept;

import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * 一种 {@code ContentNegotiationStrategy} 实现，用于将请求路径中的文件扩展名解析为用于查找媒体类型的键。
 *
 * <p>如果在提供给构造函数的显式注册中找不到文件扩展名，则使用{@link MediaTypeFactory} 作为回退机制。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class PathExtensionContentNegotiationStrategy extends AbstractMappingContentNegotiationStrategy {

	private UrlPathHelper urlPathHelper = new UrlPathHelper();


	/**
	 * 创建一个没有任何初始映射的实例。如果通过 Java Activation 框架解析了任何扩展名，
	 * 可以在后续添加映射。
	 */
	public PathExtensionContentNegotiationStrategy() {
		this(null);
	}

	/**
	 * 使用给定的文件扩展名与媒体类型的映射表创建实例。
	 */
	public PathExtensionContentNegotiationStrategy(@Nullable Map<String, MediaType> mediaTypes) {
		super(mediaTypes);
		setUseRegisteredExtensionsOnly(false);
		setIgnoreUnknownExtensions(true);
		this.urlPathHelper.setUrlDecode(false);
	}


	/**
	 * 配置一个在 {@link #getMediaTypeKey} 中使用的 {@code UrlPathHelper}，以便从目标请求 URL 路径推导出查找路径。
	 * @since 4.2.8
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * @deprecated 自 5.0 版本起，推荐使用 {@link #setUseRegisteredExtensionsOnly(boolean)}。
	 */
	@Deprecated
	public void setUseJaf(boolean useJaf) {
		setUseRegisteredExtensionsOnly(!useJaf);
	}

	@Override
	@Nullable
	protected String getMediaTypeKey(NativeWebRequest webRequest) {
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		if (request == null) {
			logger.warn("An HttpServletRequest is required to determine the media type key");
			return null;
		}
		String path = this.urlPathHelper.getLookupPathForRequest(request);
		String extension = UriUtils.extractFileExtension(path);
		return (StringUtils.hasText(extension) ? extension.toLowerCase(Locale.ENGLISH) : null);
	}

	/**
	 * 一个公共方法，展示路径扩展策略将文件扩展名解析为 {@link MediaType} 的能力，本例中针对给定的 {@link Resource}。
	 * 该方法首先查找任何显式注册的文件扩展名，然后在可用的情况下回退到 {@link MediaTypeFactory}。
	 *
	 * @param resource 要查找的资源
	 * @return 扩展名对应的 MediaType，如果未找到则返回 {@code null}
	 * @since 4.3
	 */
	@Nullable
	public MediaType getMediaTypeForResource(Resource resource) {
		Assert.notNull(resource, "Resource must not be null");
		MediaType mediaType = null;
		String filename = resource.getFilename();
		String extension = StringUtils.getFilenameExtension(filename);
		if (extension != null) {
			mediaType = lookupMediaType(extension);
		}
		if (mediaType == null) {
			mediaType = MediaTypeFactory.getMediaType(filename).orElse(null);
		}
		return mediaType;
	}

}
