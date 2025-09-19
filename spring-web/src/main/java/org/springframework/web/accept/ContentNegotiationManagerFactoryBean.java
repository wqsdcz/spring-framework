/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.ServletContextAware;

/**
 * 用于创建{@code ContentNegotiationManager}并通过一个或多个{@link ContentNegotiationStrategy}实例进行配置的工厂。
 *
 * <p>从5.0版本开始，您可以通过{@link #setStrategies(List)}方法设置要使用的具体策略。</p>
 *
 * <p>作为替代方案，您也可以依赖下面描述的一组默认配置（可通过本构建器的方法开启、关闭或自定义）：</p>
 *
 * <table>
 * <tr>
 * <th>属性设置方法</th>
 * <th>底层策略</th>
 * <th>默认设置</th>
 * </tr>
 * <tr>
 * <td>{@link #setFavorPathExtension}</td>
 * <td>{@link PathExtensionContentNegotiationStrategy 路径扩展策略}</td>
 * <td>开启</td>
 * </tr>
 * <tr>
 * <td>{@link #setFavorParameter favorParameter}</td>
 * <td>{@link ParameterContentNegotiationStrategy 参数策略}</td>
 * <td>关闭</td>
 * </tr>
 * <tr>
 * <td>{@link #setIgnoreAcceptHeader ignoreAcceptHeader}</td>
 * <td>{@link HeaderContentNegotiationStrategy 头部策略}</td>
 * <td>开启</td>
 * </tr>
 * <tr>
 * <td>{@link #setDefaultContentType defaultContentType}</td>
 * <td>{@link FixedContentNegotiationStrategy 固定内容策略}</td>
 * <td>未设置</td>
 * </tr>
 * <tr>
 * <td>{@link #setDefaultContentTypeStrategy defaultContentTypeStrategy}</td>
 * <td>{@link ContentNegotiationStrategy}</td>
 * <td>未设置</td>
 * </tr>
 * </table>
 *
 * <strong>注意：</strong>如果必须使用基于URL的内容类型解析，
 * 使用查询参数比使用路径扩展更简单且更可取，因为后者可能会导致URI变量、路径参数和URI解码问题。
 * 考虑将{@link #setFavorPathExtension}设置为{@literal false}，或通过{@link #setStrategies(List)}显式设置要使用的策略。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 3.2
 */
public class ContentNegotiationManagerFactoryBean
		implements FactoryBean<ContentNegotiationManager>, ServletContextAware, InitializingBean {

	@Nullable
	private List<ContentNegotiationStrategy> strategies;


	private boolean favorPathExtension = true;

	private boolean favorParameter = false;

	private boolean ignoreAcceptHeader = false;

	private Map<String, MediaType> mediaTypes = new HashMap<>();

	private boolean ignoreUnknownPathExtensions = true;

	@Nullable
	private Boolean useRegisteredExtensionsOnly;

	private String parameterName = "format";

	@Nullable
	private ContentNegotiationStrategy defaultNegotiationStrategy;

	@Nullable
	private ContentNegotiationManager contentNegotiationManager;

	@Nullable
	private ServletContext servletContext;


	/**
	 * 设置要使用的具体策略列表。
	 * <p>
	 *     <strong>注意：</strong>
	 *     此方法与类中所有其他用于定制默认固定策略集的setter方法互斥。
	 *     更多详细信息请参阅类级别文档。
	 *
	 * @param strategies 要使用的策略
	 * @since 5.0
	 */
	public void setStrategies(@Nullable List<ContentNegotiationStrategy> strategies) {
		this.strategies = (strategies != null ? new ArrayList<>(strategies) : null);
	}

	/**
	 * 是否应该使用URL路径中的路径扩展名来确定所请求的媒体类型。
	 * <p>
	 *     默认情况下，该值设置为{@code true}。
	 *     在这种情况下，对{@code /hotels.pdf}的请求将被解释为对{@code "application/pdf"}的请求，而无论'Accept'头信息如何。
	 */
	public void setFavorPathExtension(boolean favorPathExtension) {
		this.favorPathExtension = favorPathExtension;
	}

	/**
	 * 添加键(从路径扩展名或查询参数中提取的)到MediaType的映射。
	 * 这是参数策略正常工作所必需的。
	 * 此处显式注册的任何扩展名也会被加入白名单，用于反射文件下载攻击检测（有关RFD攻击防护的更多详细信息，请参阅Spring Framework参考文档）。
	 * <p>
	 *     路径扩展策略还会尝试使用{@link ServletContext#getMimeType}和{@link org.springframework.http.MediaTypeFactory}来解析路径扩展名。
	 *
	 * @param mediaTypes 媒体类型映射
	 * @see #addMediaType(String, MediaType)
	 * @see #addMediaTypes(Map)
	 */
	public void setMediaTypes(Properties mediaTypes) {
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			mediaTypes.forEach((key, value) -> {
				String extension = ((String) key).toLowerCase(Locale.ENGLISH);
				MediaType mediaType = MediaType.valueOf((String) value);
				this.mediaTypes.put(extension, mediaType);
			});
		}
	}

	/**
	 * 用于在Java代码中替代{@link #setMediaTypes}的方法。
	 * @see #setMediaTypes
	 * @see #addMediaTypes
	 */
	public void addMediaType(String fileExtension, MediaType mediaType) {
		this.mediaTypes.put(fileExtension, mediaType);
	}

	/**
	 * 用于在Java代码中替代{@link #setMediaTypes}的方法。
	 * @see #setMediaTypes
	 * @see #addMediaType
	 */
	public void addMediaTypes(@Nullable Map<String, MediaType> mediaTypes) {
		if (mediaTypes != null) {
			this.mediaTypes.putAll(mediaTypes);
		}
	}

	/**
	 * 是否忽略无法解析到任何媒体类型的路径扩展名请求。
	 * 将此设置为 {@code false} 时，如果没有匹配的媒体类型，将会抛出{@code HttpMediaTypeNotAcceptableException} 异常。
	 * <p>默认情况下，此值设置为 {@code true}。
	 */
	public void setIgnoreUnknownPathExtensions(boolean ignore) {
		this.ignoreUnknownPathExtensions = ignore;
	}

	/**
	 * @deprecated 从5.0版本开始废弃，推荐使用 {@link #setUseRegisteredExtensionsOnly(boolean)}，该新方法具有相反的行为逻辑。
	 */
	@Deprecated
	public void setUseJaf(boolean useJaf) {
		setUseRegisteredExtensionsOnly(!useJaf);
	}

	/**
	 * 当设置了 {@link #setFavorPathExtension 偏好路径扩展} 或 {@link #setFavorParameter(boolean) 偏好参数} 时，
	 * 此属性决定是仅使用注册的 {@code MediaType} 映射，还是允许动态解析（例如: 通过 {@link MediaTypeFactory}）。
	 * <p>默认情况下未设置此属性，此时动态解析处于开启状态。
	 */
	public void setUseRegisteredExtensionsOnly(boolean useRegisteredExtensionsOnly) {
		this.useRegisteredExtensionsOnly = useRegisteredExtensionsOnly;
	}

	private boolean useRegisteredExtensionsOnly() {
		return (this.useRegisteredExtensionsOnly != null && this.useRegisteredExtensionsOnly);
	}

	/**
	 * 是否应使用请求参数（默认为"format"）来确定所请求的媒体类型。
	 * 要使此选项生效，必须注册 {@link #setMediaTypes 媒体类型映射}。
	 * <p>默认情况下，此选项设置为 {@code false}。
	 * @see #setParameterName
	 */
	public void setFavorParameter(boolean favorParameter) {
		this.favorParameter = favorParameter;
	}

	/**
	 * 设置当启用 {@link #setFavorParameter} 时使用的查询参数名称。
	 * <p>默认参数名为 {@code "format"}。
	 */
	public void setParameterName(String parameterName) {
		Assert.notNull(parameterName, "parameterName is required");
		this.parameterName = parameterName;
	}

	/**
	 * 是否禁用检查 'Accept' 请求头。
	 * <p>默认情况下该值设置为 {@code false}。
	 */
	public void setIgnoreAcceptHeader(boolean ignoreAcceptHeader) {
		this.ignoreAcceptHeader = ignoreAcceptHeader;
	}

	/**
	 * 设置当未请求内容类型时使用的默认内容类型。
	 * <p>默认情况下此值为未设置状态。
	 * @see #setDefaultContentTypeStrategy
	 */
	public void setDefaultContentType(MediaType contentType) {
		this.defaultNegotiationStrategy = new FixedContentNegotiationStrategy(contentType);
	}

	/**
	 * 设置当未请求内容类型时使用的默认内容类型列表。
	 * <p>默认情况下此值为未设置状态。
	 * @see #setDefaultContentTypeStrategy
	 * @since 5.0
	 */
	public void setDefaultContentTypes(List<MediaType> contentTypes) {
		this.defaultNegotiationStrategy = new FixedContentNegotiationStrategy(contentTypes);
	}

	/**
	 * 设置自定义的 {@link ContentNegotiationStrategy}，用于在未请求内容类型时确定要使用的内容类型。
	 * <p>默认情况下此值为未设置状态。
	 * @see #setDefaultContentType
	 * @since 4.1.2
	 */
	public void setDefaultContentTypeStrategy(ContentNegotiationStrategy strategy) {
		this.defaultNegotiationStrategy = strategy;
	}

	/**
	 * 由 Spring 调用以注入 ServletContext。
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	@Override
	public void afterPropertiesSet() {
		build();
	}

	/**
	 * 实际构建 {@link ContentNegotiationManager} 的方法。
	 * @since 5.0
	 */
	public ContentNegotiationManager build() {
		List<ContentNegotiationStrategy> strategies = new ArrayList<>();

		if (this.strategies != null) {
			strategies.addAll(this.strategies);
		}
		else {
			if (this.favorPathExtension) {
				PathExtensionContentNegotiationStrategy strategy;
				if (this.servletContext != null && !useRegisteredExtensionsOnly()) {
					strategy = new ServletPathExtensionContentNegotiationStrategy(this.servletContext, this.mediaTypes);
				}
				else {
					strategy = new PathExtensionContentNegotiationStrategy(this.mediaTypes);
				}
				strategy.setIgnoreUnknownExtensions(this.ignoreUnknownPathExtensions);
				if (this.useRegisteredExtensionsOnly != null) {
					strategy.setUseRegisteredExtensionsOnly(this.useRegisteredExtensionsOnly);
				}
				strategies.add(strategy);
			}

			if (this.favorParameter) {
				ParameterContentNegotiationStrategy strategy = new ParameterContentNegotiationStrategy(this.mediaTypes);
				strategy.setParameterName(this.parameterName);
				if (this.useRegisteredExtensionsOnly != null) {
					strategy.setUseRegisteredExtensionsOnly(this.useRegisteredExtensionsOnly);
				}
				else {
					strategy.setUseRegisteredExtensionsOnly(true);  // backwards compatibility
				}
				strategies.add(strategy);
			}

			if (!this.ignoreAcceptHeader) {
				strategies.add(new HeaderContentNegotiationStrategy());
			}

			if (this.defaultNegotiationStrategy != null) {
				strategies.add(this.defaultNegotiationStrategy);
			}
		}

		this.contentNegotiationManager = new ContentNegotiationManager(strategies);
		return this.contentNegotiationManager;
	}


	@Override
	@Nullable
	public ContentNegotiationManager getObject() {
		return this.contentNegotiationManager;
	}

	@Override
	public Class<?> getObjectType() {
		return ContentNegotiationManager.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
