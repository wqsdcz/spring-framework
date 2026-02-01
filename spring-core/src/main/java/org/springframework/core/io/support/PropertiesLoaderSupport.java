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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.PropertiesPersister;

/**
 * 需要从一个或多个资源加载属性的 JavaBean 风格组件的基类。
 * 同样支持本地属性，具有可配置的覆盖功能。
 *
 * @author Juergen Hoeller
 * @since 1.2.2
 */
public abstract class PropertiesLoaderSupport {

	/** 子类可用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	protected Properties[] localProperties;

	protected boolean localOverride = false;

	@Nullable
	private Resource[] locations;

	private boolean ignoreResourceNotFound = false;

	@Nullable
	private String fileEncoding;

	private PropertiesPersister propertiesPersister = DefaultPropertiesPersister.INSTANCE;


	/**
	 * 设置本地属性，例如，通过 XML bean 定义中的 "props" 标签。
	 * 这些可以被视为默认值，会被从文件加载的属性覆盖。
	 */
	public void setProperties(Properties properties) {
		this.localProperties = new Properties[] {properties};
	}

	/**
	 * 设置本地属性，例如，通过 XML bean 定义中的 "props" 标签，
	 * 允许将多个属性集合并为一个。
	 */
	public void setPropertiesArray(Properties... propertiesArray) {
		this.localProperties = propertiesArray;
	}

	/**
	 * 设置要加载的属性文件的位置。
	 * <p>可以指向经典属性文件或遵循 Java 属性 XML 格式的 XML 文件。
	 */
	public void setLocation(Resource location) {
		this.locations = new Resource[] {location};
	}

	/**
	 * 设置要加载的属性文件的位置。
	 * <p>可以指向经典属性文件或遵循 Java 属性 XML 格式的 XML 文件。
	 * <p>注意：后面文件中定义的属性将覆盖前面文件中定义的属性（在键重叠的情况下）。
	 * 因此，请确保最具体的文件是给定位置列表中的最后几个文件。
	 */
	public void setLocations(Resource... locations) {
		this.locations = locations;
	}

	/**
	 * 设置本地属性是否覆盖来自文件的属性。
	 * <p>默认为 "false"：来自文件的属性覆盖本地默认值。
	 * 可以切换为 "true" 以让本地属性覆盖来自文件的默认值。
	 */
	public void setLocalOverride(boolean localOverride) {
		this.localOverride = localOverride;
	}

	/**
	 * 设置是否应忽略找不到属性资源的失败。
	 * <p>"true" 适用于属性文件完全是可选的情况。
	 * 默认为 "false"。
	 */
	public void setIgnoreResourceNotFound(boolean ignoreResourceNotFound) {
		this.ignoreResourceNotFound = ignoreResourceNotFound;
	}

	/**
	 * 设置用于解析属性文件的编码。
	 * <p>默认为空，使用 {@code java.util.Properties} 的默认编码。
	 * <p>仅适用于经典属性文件，不适用于 XML 文件。
	 * @see org.springframework.util.PropertiesPersister#load
	 */
	public void setFileEncoding(String encoding) {
		this.fileEncoding = encoding;
	}

	/**
	 * 设置用于解析属性文件的 PropertiesPersister。
	 * 默认为 {@code DefaultPropertiesPersister}。
	 * @see DefaultPropertiesPersister#INSTANCE
	 */
	public void setPropertiesPersister(@Nullable PropertiesPersister propertiesPersister) {
		this.propertiesPersister =
				(propertiesPersister != null ? propertiesPersister : DefaultPropertiesPersister.INSTANCE);
	}


	/**
	 * 返回一个合并的 {@link Properties} 实例，包含加载的属性和在此组件上设置的属性。
	 */
	protected Properties mergeProperties() throws IOException {
		Properties result = new Properties();

		if (this.localOverride) {
			// 预先从文件加载属性，以让本地属性覆盖。
			loadProperties(result);
		}

		if (this.localProperties != null) {
			for (Properties localProp : this.localProperties) {
				CollectionUtils.mergePropertiesIntoMap(localProp, result);
			}
		}

		if (!this.localOverride) {
			// 之后从文件加载属性，以让这些属性覆盖。
			loadProperties(result);
		}

		return result;
	}

	/**
	 * 将属性加载到给定实例中。
	 * @param props 要加载到的 Properties 实例
	 * @throws IOException 在 I/O 错误的情况下
	 * @see #setLocations
	 */
	protected void loadProperties(Properties props) throws IOException {
		if (this.locations != null) {
			for (Resource location : this.locations) {
				if (logger.isTraceEnabled()) {
					logger.trace("Loading properties file from " + location);
				}
				try {
					PropertiesLoaderUtils.fillProperties(
							props, new EncodedResource(location, this.fileEncoding), this.propertiesPersister);
				}
				catch (FileNotFoundException | UnknownHostException | SocketException ex) {
					if (this.ignoreResourceNotFound) {
						if (logger.isDebugEnabled()) {
							logger.debug("Properties resource not found: " + ex.getMessage());
						}
					}
					else {
						throw ex;
					}
				}
			}
		}
	}

}
