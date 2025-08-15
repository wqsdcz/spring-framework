/**
 * Generic abstraction for (file-based) resources, used throughout the framework.
 */
@NonNullApi
@NonNullFields
package org.springframework.core.io;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
/**
 * IO流的源
 * InputStreamSource
 *
 * 资源
 * Resource
 * WritableResource
 * ContextResource
 * AbstractResource
 * AbstractFileResolvingResource
 * ByteArrayResource
 * InputStreamResource
 * ClassPathResource
 * FileSystemResource
 * PathResource
 * UrlResource
 * FileUrlResource
 * VfsResource
 * DescriptiveResource
 *
 * 协议
 * ProtocolResolver
 *
 * 资源的加载
 * ResourceLoader
 * DefaultResourceLoader       url字符串 -> UrlResource、 "classpath:"开头的字符串 -> ClassPathResource、 其他字符串 -> ClassPathContextResource
 * ClassRelativeResourceLoader 其他字符串 -> ClassRelativeContextResource
 * FileSystemResourceLoader    其他字符串 -> FileSystemContextResource
 *
 * PropertyEditor实现
 * ResourceEditor
 *
 * 工具类
 * VfsUtils
 *
 */