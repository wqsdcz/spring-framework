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

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 一种返回固定内容类型的 {@code ContentNegotiationStrategy} 实现。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class FixedContentNegotiationStrategy implements ContentNegotiationStrategy {

	private static final Log logger = LogFactory.getLog(FixedContentNegotiationStrategy.class);

	private final List<MediaType> contentTypes;


	/**
	 * 只有一个默认的{@code MediaType}的构造函数。
	 */
	public FixedContentNegotiationStrategy(MediaType contentType) {
		this(Collections.singletonList(contentType));
	}

	/**
	 * 构造方法，接收一个有序的默认 {@code MediaType} 列表，用于支持多种内容类型的应用程序。
	 * <p>如果存在不支持其他默认媒体类型的目标端点，建议在列表末尾追加 {@link MediaType#ALL}。
	 * @since 5.0
	 */
	public FixedContentNegotiationStrategy(List<MediaType> contentTypes) {
		Assert.notNull(contentTypes, "'contentTypes' must not be null");
		this.contentTypes = Collections.unmodifiableList(contentTypes);
	}


	/**
	 * 返回已配置的媒体类型列表。
	 */
	public List<MediaType> getContentTypes() {
		return this.contentTypes;
	}


	@Override
	public List<MediaType> resolveMediaTypes(NativeWebRequest request) {
		if (logger.isDebugEnabled()) {
			logger.debug("Requested media types: " + this.contentTypes);
		}
		return this.contentTypes;
	}

}
