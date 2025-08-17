/*
 * Copyright 2002-2007 the original author or authors.
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
 * <p>用于将位置模式（例如Ant风格路径模式）解析为Resource对象的策略接口。<p/>
 *
 * <p>此接口是 {@link org.springframework.core.io.ResourceLoader} 的扩展。
 * 传入的ResourceLoader（例如:通过 {@link org.springframework.context.ResourceLoaderAware} 在上下文中传递的 {@link org.springframework.context.ApplicationContext}）
 * 可被检测是否也实现了此扩展接口。 <p/>
 *
 * <p>{@link PathMatchingResourcePatternResolver} 是可在ApplicationContext之外使用的独立实现，
 * 也被 {@link ResourceArrayPropertyEditor} 用于注入Resource数组类型的bean属性。 <p/>
 *
 * <p>可适配任意类型的位置模式（例如 "/WEB-INF/*-context.xml"）： 输入模式需与策略实现相匹配，本接口仅规定转换方法而非特定模式格式。 <p/>
 *
 * <p>此接口还提出新的资源前缀 "classpath*:"， 用于匹配类路径中所有同名资源。
 * 请注意： 此场景要求资源路径必须是不包含占位符的纯路径（如 "/beans.xml"）—— 因为JAR文件或类目录中可能存在多个同名文件。<p/>
 *
 * Strategy interface for resolving a location pattern (for example,
 * an Ant-style path pattern) into Resource objects.
 *
 * <p>This is an extension to the {@link org.springframework.core.io.ResourceLoader}
 * interface. A passed-in ResourceLoader (for example, an
 * {@link org.springframework.context.ApplicationContext} passed in via
 * {@link org.springframework.context.ResourceLoaderAware} when running in a context)
 * can be checked whether it implements this extended interface too.
 *
 * <p>{@link PathMatchingResourcePatternResolver} is a standalone implementation
 * that is usable outside an ApplicationContext, also used by
 * {@link ResourceArrayPropertyEditor} for populating Resource array bean properties.
 *
 * <p>Can be used with any sort of location pattern (e.g. "/WEB-INF/*-context.xml"):
 * Input patterns have to match the strategy implementation. This interface just
 * specifies the conversion method rather than a specific pattern format.
 *
 * <p>This interface also suggests a new resource prefix "classpath*:" for all
 * matching resources from the class path. Note that the resource location is
 * expected to be a path without placeholders in this case (e.g. "/beans.xml");
 * JAR files or classes directories can contain multiple files of the same name.
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.ResourceLoader
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 */
public interface ResourcePatternResolver extends ResourceLoader {

	/**
	 * <p>类路径资源匹配的伪URL前缀："classpath*:"
	 * 该前缀与ResourceLoader的普通类路径URL前缀的区别在于：
	 * 它能检索给定名称（例如"/beans.xml"）对应的全部匹配资源，
	 * 例如在所有已部署JAR文件的根目录下进行检索。<p/>
	 *
	 * Pseudo URL prefix for all matching resources from the class path: "classpath*:"
	 * This differs from ResourceLoader's classpath URL prefix in that it
	 * retrieves all matching resources for a given name (e.g. "/beans.xml"),
	 * for example in the root of all deployed JAR files.
	 * @see org.springframework.core.io.ResourceLoader#CLASSPATH_URL_PREFIX
	 */
	String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

	/**
	 * Resolve the given location pattern into Resource objects.
	 * <p>Overlapping resource entries that point to the same physical
	 * resource should be avoided, as far as possible. The result should
	 * have set semantics.
	 * @param locationPattern the location pattern to resolve
	 * @return the corresponding Resource objects
	 * @throws IOException in case of I/O errors
	 */
	Resource[] getResources(String locationPattern) throws IOException;

}
