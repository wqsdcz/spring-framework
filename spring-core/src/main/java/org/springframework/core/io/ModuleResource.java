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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link java.lang.Module} 解析的 {@link Resource} 实现，
 * 通过 {@link Module#getResourceAsStream} 执行 {@link #getInputStream()} 访问。
 *
 * <p>或者，考虑通过 {@link ClassPathResource} 访问模块路径布局中的资源以获取导出的资源，
 * 或者通过 {@link ClassPathResource#ClassPathResource(String, Class)} 特别针对 {@code Class} 
 * 进行解析以在特定类所在的模块内进行本地解析。在常见场景中，模块资源将简单地透明可见为
 * 类路径资源，因此根本不需要任何特殊处理。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 6.1
 * @see Module#getResourceAsStream
 * @see ClassPathResource
 */
public class ModuleResource extends AbstractResource {

	private final Module module;

	private final String path;


	/**
	 * 为给定的 {@link Module} 和给定的资源路径创建一个新的 {@code ModuleResource}。
	 * @param module 在其中搜索的运行时模块
	 * @param path 模块内的资源路径
	 */
	public ModuleResource(Module module, String path) {
		Assert.notNull(module, "Module must not be null");
		Assert.notNull(path, "Path must not be null");
		this.module = module;
		this.path = path;
	}


	/**
	 * 返回此资源的 {@link Module}。
	 */
	public final Module getModule() {
		return this.module;
	}

	/**
	 * 返回此资源的路径。
	 */
	public final String getPath() {
		return this.path;
	}


	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is = this.module.getResourceAsStream(this.path);
		if (is == null) {
			throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
		}
		return is;
	}

	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return new ModuleResource(this.module, pathToUse);
	}

	@Override
	@Nullable
	public String getFilename() {
		return StringUtils.getFilename(this.path);
	}

	@Override
	public String getDescription() {
		return "module resource [" + this.path + "]" +
				(this.module.isNamed() ? " from module [" + this.module.getName() + "]" : "");
	}


	@Override
	public boolean equals(@Nullable Object obj) {
		return (this == obj || (obj instanceof ModuleResource that &&
				this.module.equals(that.module) && this.path.equals(that.path)));
	}

	@Override
	public int hashCode() {
		return this.module.hashCode() * 31 + this.path.hashCode();
	}

}
