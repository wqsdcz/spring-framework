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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ResolvedModule;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.NativeDetector;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.VfsResource;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * 一个 {@link ResourcePatternResolver} 实现，能够将指定的资源位置路径解析为一个或多个匹配的 Resources。
 *
 * <p>源路径可能是一个简单的路径，它与目标 {@link org.springframework.core.io.Resource} 有一对一的映射，
 * 或者可能包含特殊的 "{@code classpath*:}" 前缀和/或内部 Ant 风格的路径模式（使用 Spring 的 {@link AntPathMatcher} 工具进行匹配）。
 * 后两者实际上都是通配符。
 *
 * <h3>无通配符</h3>
 *
 * <p>在简单情况下，如果指定的位置路径不以 {@code "classpath*:}" 前缀开头且不包含 {@link PathMatcher} 模式，
 * 则此解析器将仅通过在基础 {@code ResourceLoader} 上调用 {@code getResource()} 返回单个资源。
 * 示例包括真实 URL，如 "{@code file:C:/context.xml}"、伪 URL 如 "{@code classpath:/context.xml}"，
 * 以及简单的无前缀路径，如 "{@code /WEB-INF/context.xml}"。后者将以特定于基础 {@code ResourceLoader} 的方式解析
 * （例如，{@code ServletContextResource} 用于 {@code WebApplicationContext}）。
 *
 * <h3>Ant 风格模式</h3>
 *
 * <p>当路径位置包含 Ant 风格模式时，例如：
 * <pre class="code">
 * /WEB-INF/*-context.xml
 * com/example/**&#47;applicationContext.xml
 * file:C:/some/path/*-context.xml
 * classpath:com/example/**&#47;applicationContext.xml</pre>
 * 解析器遵循更复杂但定义明确的过程来尝试解析通配符。它为路径生成一个 {@code Resource}，
 * 直到最后一个非通配符段，并从中获取 {@code URL}。如果此 URL 不是 "{@code jar:}" URL
 * 或容器特定的变体（例如，WebLogic 中的 "{@code zip:}"，WebSphere 中的 "{@code wsjar}" 等），
 * 则获取与 URL 关联的文件系统的根目录，并用于通过遍历文件系统来解析通配符。
 * 在 jar URL 的情况下，解析器要么从中获取 {@code java.net.JarURLConnection}，
 * 要么手动解析 jar URL，然后遍历 jar 文件的内容，以解析通配符。
 *
 * <h3>对可移植性的影响</h3>
 *
 * <p>如果指定的路径已经是文件 URL（无论是显式还是隐式，因为基础 {@code ResourceLoader} 是文件系统的一个），
 * 那么通配符保证在完全可移植的方式下工作。
 *
 * <p>如果指定的路径是类路径位置，则解析器必须通过 {@code Classloader.getResource()} 调用
 * 获取最后一个非通配符路径段 URL。由于这只是路径的一个节点（而不是结尾处的文件），
 * 因此在这种情况下返回的 URL 类型实际上是未定义的（在 ClassLoader Javadocs 中）。
 * 实际上，它通常是一个表示目录的 {@code java.io.File}，其中类路径资源解析为文件系统位置，
 * 或某种类型的 jar URL，其中类路径资源解析为 jar 位置。尽管如此，此操作存在可移植性问题。
 *
 * <p>如果为最后一个非通配符段获得了 jar URL，则解析器必须能够从中获取 {@code java.net.JarURLConnection}，
 * 或手动解析 jar URL，以便能够遍历 jar 的内容并解析通配符。
 * 这将在大多数环境中工作，但在其他环境中会失败，强烈建议您在依赖之前彻底测试您的特定环境中来自 jar 的资源通配符解析。
 *
 * <h3>{@code classpath*:} 前缀</h3>
 *
 * <p>有特殊支持用于通过 "{@code classpath*:}" 前缀检索具有相同名称的多个类路径资源。
 * 例如，"{@code classpath*:META-INF/beans.xml}" 将找到类路径中所有的 "META-INF/beans.xml" 文件，
 * 无论是在 "classes" 目录还是在 JAR 文件中。这对于在每个 jar 文件中相同位置自动检测同名配置文件特别有用。
 * 在内部，这是通过 {@code ClassLoader.getResources()} 调用完成的，完全可移植。
 *
 * <p>"{@code classpath*:}" 前缀也可以与位置路径其余部分的 {@code PathMatcher} 模式结合使用 &mdash; 例如，
 * "{@code classpath*:META-INF/*-beans.xml"}。在这种情况下，解析策略相当简单：
 * 在最后一个非通配符路径段上使用 {@code ClassLoader.getResources()} 调用来获取类加载器层次结构中的所有匹配资源，
 * 然后对每个资源使用上述相同的 {@code PathMatcher} 解析策略来解析通配符子模式。
 *
 * <h3>其他说明</h3>
 *
 * <p>从 Spring Framework 6.0 开始，如果使用 "{@code classpath*:}" 前缀的位置模式调用 {@link #getResources(String)}，
 * 它将首先搜索 {@linkplain ModuleLayer#boot() 引导层} 中的所有模块，排除 {@linkplain ModuleFinder#ofSystem() 系统模块}。
 * 然后它将使用 {@link ClassLoader} API 搜索类路径，如前所述，并返回组合结果。
 * 因此，当应用程序作为模块部署时，某些类路径搜索的限制可能不适用。
 *
 * <p><b>警告：</b>请注意，当 "{@code classpath*:}" 与 Ant 风格模式结合使用时，
 * 除非实际目标文件位于文件系统中，否则只有在模式开始之前至少有一个根目录时才能可靠地工作。
 * 这意味着像 "{@code classpath*:*.xml}" 这样的模式<i>不会</i>从 jar 文件的根目录中检索文件，
 * 而只会从展开目录的根目录中检索。这源于 JDK 的 {@code ClassLoader.getResources()} 方法的限制，
 * 该方法仅返回传递的空字符串的文件系统位置（指示潜在的搜索根目录）。
 * 此 {@code ResourcePatternResolver} 实现试图通过对 {@link URLClassLoader} 进行内省和
 * "{@code java.class.path}" 清单评估来缓解 jar 根目录查找限制；但是，没有可移植性保证。
 *
 * <p><b>警告：</b>当使用 "{@code classpath:}" 资源的 Ant 风格模式时，如果要搜索的基本包在多个类路径位置中可用，
 * 则不能保证找到匹配的资源。这是因为这样的资源，例如
 * <pre class="code">
 *   com/example/package1/service-context.xml</pre>
 * 可能只存在于一个类路径位置中，但当使用如下位置模式时
 * <pre class="code">
 *   classpath:com/example/**&#47;service-context.xml</pre>
 * 来尝试解析它时，解析器将基于 {@code getResource("com/example")} 返回的（第一个）URL 工作。
 * 如果 {@code com/example} 基本包节点存在于多个类路径位置中，实际所需资源可能不会出现在第一个 URL 的 {@code com/example} 基本包下。
 * 因此，在这种情况下最好使用带有相同 Ant 风格模式的 "{@code classpath*:}"，它将搜索包含基本包的<i>所有</i>类路径位置。
 *
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @author Marius Bogoevici
 * @author Costin Leau
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Dave Syer
 * @since 1.0.2
 * @see #CLASSPATH_ALL_URL_PREFIX
 * @see org.springframework.util.AntPathMatcher
 * @see org.springframework.core.io.ResourceLoader#getResource(String)
 * @see ClassLoader#getResources(String)
 */
public class PathMatchingResourcePatternResolver implements ResourcePatternResolver {

	private static final Resource[] EMPTY_RESOURCE_ARRAY = {};

	private static final Log logger = LogFactory.getLog(PathMatchingResourcePatternResolver.class);

	/**
	 * {@linkplain ModuleFinder#ofSystem() 系统模块}名称的 {@link Set}。
	 * @since 6.0
	 * @see #isNotSystemModule
	 */
	private static final Set<String> systemModuleNames = NativeDetector.inNativeImage() ? Collections.emptySet() :
			ModuleFinder.ofSystem().findAll().stream()
					.map(moduleReference -> moduleReference.descriptor().name())
					.collect(Collectors.toSet());

	/**
	 * 测试提供的 {@link ResolvedModule} 是否不是
	 * {@linkplain ModuleFinder#ofSystem() 系统模块}的 {@link Predicate}。
	 * @since 6.0
	 * @see #systemModuleNames
	 */
	private static final Predicate<ResolvedModule> isNotSystemModule =
			resolvedModule -> !systemModuleNames.contains(resolvedModule.name());

	@Nullable
	private static Method equinoxResolveMethod;

	static {
		try {
			// Detect Equinox OSGi (for example, on WebSphere 6.1)
			Class<?> fileLocatorClass = ClassUtils.forName("org.eclipse.core.runtime.FileLocator",
					PathMatchingResourcePatternResolver.class.getClassLoader());
			equinoxResolveMethod = fileLocatorClass.getMethod("resolve", URL.class);
			logger.trace("Found Equinox FileLocator for OSGi bundle URL resolution");
		}
		catch (Throwable ex) {
			equinoxResolveMethod = null;
		}
	}


	private final ResourceLoader resourceLoader;

	private PathMatcher pathMatcher = new AntPathMatcher();

	@Nullable
	private Boolean useCaches;

	private final Map<String, Resource[]> rootDirCache = new ConcurrentHashMap<>();

	private final Map<String, NavigableSet<String>> jarEntriesCache = new ConcurrentHashMap<>();

	@Nullable
	private volatile Set<ClassPathManifestEntry> manifestEntriesCache;


	/**
	 * 使用 {@link DefaultResourceLoader} 创建一个 {@code PathMatchingResourcePatternResolver}。
	 * <p>类加载器访问将通过线程上下文类加载器发生。
	 * @see DefaultResourceLoader
	 */
	public PathMatchingResourcePatternResolver() {
		this.resourceLoader = new DefaultResourceLoader();
	}

	/**
	 * 使用提供的 {@link ResourceLoader} 创建一个 {@code PathMatchingResourcePatternResolver}。
	 * <p>类加载器访问将通过线程上下文类加载器发生。
	 * @param resourceLoader 用于加载根目录和实际资源的 {@code ResourceLoader}
	 */
	public PathMatchingResourcePatternResolver(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}

	/**
	 * 使用 {@link DefaultResourceLoader} 和提供的 {@link ClassLoader} 创建一个 {@code PathMatchingResourcePatternResolver}。
	 * @param classLoader 用于加载类路径资源的 ClassLoader，
	 * 或 {@code null} 用于在实际资源访问时使用线程上下文类加载器
	 * @see org.springframework.core.io.DefaultResourceLoader
	 */
	public PathMatchingResourcePatternResolver(@Nullable ClassLoader classLoader) {
		this.resourceLoader = new DefaultResourceLoader(classLoader);
	}


	/**
	 * 返回此模式解析器使用的 {@link ResourceLoader}。
	 */
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		return getResourceLoader().getClassLoader();
	}

	/**
	 * 设置此资源模式解析器使用的 {@link PathMatcher} 实现。
	 * <p>默认值是 {@link AntPathMatcher}。
	 * @see AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	/**
	 * 返回此资源模式解析器使用的 {@link PathMatcher}。
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 指定此解析器是否应使用 jar 缓存。默认值为 {@code true}。
	 * <p>将此标志切换为 {@code false} 以避免任何 jar 缓存，在
	 * {@link JarURLConnection} 级别以及在此解析器实例中。
	 * <p>请注意，{@link JarURLConnection#setDefaultUseCaches} 可以独立关闭。
	 * 此解析器级别设置旨在仅在必要时强制执行
	 * {@code JarURLConnection#setUseCaches(true/false)}，但否则保留 JVM 级别的默认值
	 * （如果未调用此设置器）。
	 * <p>从 6.2.10 开始，此设置传播到 {@link UrlResource#setUseCaches}。
	 * @since 6.1.19
	 * @see JarURLConnection#setUseCaches
	 * @see UrlResource#setUseCaches
	 * @see #clearCache()
	 */
	public void setUseCaches(boolean useCaches) {
		this.useCaches = useCaches;
	}


	@Override
	public Resource getResource(String location) {
		Resource resource = getResourceLoader().getResource(location);
		if (this.useCaches != null && resource instanceof UrlResource urlResource) {
			urlResource.setUseCaches(this.useCaches);
		}
		return resource;
	}

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		Assert.notNull(locationPattern, "Location pattern must not be null");
		if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
			// a class path resource (multiple resources for same name possible)
			String locationPatternWithoutPrefix = locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length());
			// Search the module path first.
			Set<Resource> resources = findAllModulePathResources(locationPatternWithoutPrefix);
			// Search the class path next.
			if (getPathMatcher().isPattern(locationPatternWithoutPrefix)) {
				// a class path resource pattern
				Collections.addAll(resources, findPathMatchingResources(locationPattern));
			}
			else {
				// all class path resources with the given name
				Collections.addAll(resources, findAllClassPathResources(locationPatternWithoutPrefix));
			}
			return resources.toArray(EMPTY_RESOURCE_ARRAY);
		}
		else {
			// Generally only look for a pattern after a prefix here,
			// and on Tomcat only after the "*/" separator for its "war:" protocol.
			int prefixEnd = (locationPattern.startsWith("war:") ? locationPattern.indexOf("*/") + 1 :
					locationPattern.indexOf(':') + 1);
			if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {
				// a file pattern
				return findPathMatchingResources(locationPattern);
			}
			else {
				// a single resource with the given name
				return new Resource[] {getResource(locationPattern)};
			}
		}
	}

	/**
	 * 清除本地资源缓存，删除所有缓存的类路径/jar 结构。
	 * @since 6.2
	 */
	public void clearCache() {
		this.rootDirCache.clear();
		this.jarEntriesCache.clear();
		this.manifestEntriesCache = null;
	}


	/**
	 * 通过 ClassLoader 查找具有给定位置的所有类位置资源。
	 * <p>委托给 {@link #doFindAllClassPathResources(String)}。
	 * @param location 类路径中的绝对路径
	 * @return 结果作为 Resource 数组
	 * @throws IOException 在 I/O 错误的情况下
	 * @see java.lang.ClassLoader#getResources
	 * @see #convertClassLoaderURL
	 */
	protected Resource[] findAllClassPathResources(String location) throws IOException {
		String path = stripLeadingSlash(location);
		Set<Resource> result = doFindAllClassPathResources(path);
		if (logger.isTraceEnabled()) {
			logger.trace("Resolved class path location [" + path + "] to resources " + result);
		}
		return result.toArray(EMPTY_RESOURCE_ARRAY);
	}

	/**
	 * 通过配置的 {@link #getClassLoader() ClassLoader} 查找具有给定路径的所有类路径资源。
	 * <p>由 {@link #findAllClassPathResources(String)} 调用。
	 * @param path 类路径中的绝对路径（永远没有前导斜杠）
	 * @return 匹配的 Resource 实例的可变 Set
	 * @since 4.1.1
	 */
	protected Set<Resource> doFindAllClassPathResources(String path) throws IOException {
		Set<Resource> result = new LinkedHashSet<>(16);
		ClassLoader cl = getClassLoader();
		Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path));
		while (resourceUrls.hasMoreElements()) {
			URL url = resourceUrls.nextElement();
			result.add(convertClassLoaderURL(url));
		}
		if (!StringUtils.hasLength(path)) {
			// The above result is likely to be incomplete, i.e. only containing file system references.
			// We need to have pointers to each of the jar files on the class path as well...
			addAllClassLoaderJarRoots(cl, result);
		}
		return result;
	}

	/**
	 * 将从配置的 {@link #getClassLoader() ClassLoader} 返回的给定 URL 转换为 {@link Resource}，
	 * 应用于无模式的路径查找（参见 {@link #findAllClassPathResources}）。
	 * <p>从 6.0.5 开始，默认实现在 "file" 协议的情况下创建 {@link FileSystemResource}，
	 * 否则创建 {@link UrlResource}，匹配相同资源布局中基于模式的类路径遍历的结果，
	 * 以及匹配模块路径搜索的结果。
	 * @param url 从配置的 ClassLoader 返回的 URL
	 * @return 相应的 Resource 对象
	 * @see java.lang.ClassLoader#getResources
	 * @see #doFindAllClassPathResources
	 * @see #doFindPathMatchingFileResources
	 */
	@SuppressWarnings("deprecation")  // on JDK 20 (deprecated URL constructor)
	protected Resource convertClassLoaderURL(URL url) {
		if (ResourceUtils.URL_PROTOCOL_FILE.equals(url.getProtocol())) {
			try {
				// URI decoding for special characters such as spaces.
				return new FileSystemResource(ResourceUtils.toURI(url).getSchemeSpecificPart());
			}
			catch (URISyntaxException ex) {
				// Fallback for URLs that are not valid URIs (should hardly ever happen).
				return new FileSystemResource(url.getFile());
			}
		}
		else {
			UrlResource resource = null;
			String urlString = url.toString();
			String cleanedPath = StringUtils.cleanPath(urlString);
			if (!cleanedPath.equals(urlString)) {
				// Prefer cleaned URL, aligned with UrlResource#createRelative(String)
				try {
					// Retain original URL instance, potentially including custom URLStreamHandler.
					resource = new UrlResource(new URL(url, cleanedPath));
				}
				catch (MalformedURLException ex) {
					// Fallback to regular URL construction below...
				}
			}
			// Retain original URL instance, potentially including custom URLStreamHandler.
			if (resource == null) {
				resource = new UrlResource(url);
			}
			if (this.useCaches != null) {
				resource.setUseCaches(this.useCaches);
			}
			return resource;
		}
	}

	/**
	 * 搜索所有 {@link URLClassLoader} URLs 以查找 jar 文件引用，并以指向 jar 文件内容根的指针形式
	 * 将每个添加到给定的资源集。
	 * @param classLoader 要搜索的 ClassLoader（包括其祖先）
	 * @param result 要向其中添加 jar 根的资源集
	 * @since 4.1.1
	 */
	protected void addAllClassLoaderJarRoots(@Nullable ClassLoader classLoader, Set<Resource> result) {
		if (classLoader instanceof URLClassLoader urlClassLoader) {
			try {
				for (URL url : urlClassLoader.getURLs()) {
					try {
						UrlResource jarResource = (ResourceUtils.URL_PROTOCOL_JAR.equals(url.getProtocol()) ?
								new UrlResource(url) :
								new UrlResource(ResourceUtils.JAR_URL_PREFIX + url + ResourceUtils.JAR_URL_SEPARATOR));
						if (this.useCaches != null) {
							jarResource.setUseCaches(this.useCaches);
						}
						if (jarResource.exists()) {
							result.add(jarResource);
						}
					}
					catch (MalformedURLException ex) {
						if (logger.isDebugEnabled()) {
							logger.debug("Cannot search for matching files underneath [" + url +
									"] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage());
						}
					}
				}
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot introspect jar files since ClassLoader [" + classLoader +
							"] does not support 'getURLs()': " + ex);
				}
			}
		}

		if (classLoader == ClassLoader.getSystemClassLoader()) {
			// JAR "Class-Path" manifest header evaluation...
			addClassPathManifestEntries(result);
		}

		if (classLoader != null) {
			try {
				// Hierarchy traversal...
				addAllClassLoaderJarRoots(classLoader.getParent(), result);
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot introspect jar files in parent ClassLoader since [" + classLoader +
							"] does not support 'getParent()': " + ex);
				}
			}
		}
	}

	/**
	 * 从 {@code Class-Path} 清单条目确定 jar 文件引用（这些条目由系统
	 * 类加载器添加到 {@code java.class.path} JVM 系统属性中）并将每个以指向
	 * jar 文件内容根的指针形式添加到给定的资源集中。
	 * @param result 要向其中添加 jar 根的资源集
	 * @since 4.3
	 */
	protected void addClassPathManifestEntries(Set<Resource> result) {
		Set<ClassPathManifestEntry> entries = this.manifestEntriesCache;
		if (entries == null) {
			entries = getClassPathManifestEntries();
			if (this.useCaches == null || this.useCaches) {
				this.manifestEntriesCache = entries;
			}
		}
		for (ClassPathManifestEntry entry : entries) {
			if (!result.contains(entry.resource()) &&
					(entry.alternative() != null && !result.contains(entry.alternative()))) {
				result.add(entry.resource());
			}
		}
	}

	private Set<ClassPathManifestEntry> getClassPathManifestEntries() {
		Set<ClassPathManifestEntry> manifestEntries = new LinkedHashSet<>();
		Set<File> seen = new HashSet<>();
		try {
			String paths = System.getProperty("java.class.path");
			for (String path : StringUtils.delimitedListToStringArray(paths, File.pathSeparator)) {
				try {
					File jar = new File(path).getAbsoluteFile();
					if (jar.isFile() && seen.add(jar)) {
						manifestEntries.add(ClassPathManifestEntry.of(jar, this.useCaches));
						manifestEntries.addAll(getClassPathManifestEntriesFromJar(jar));
					}
				}
				catch (MalformedURLException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Cannot search for matching files underneath [" + path +
								"] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage());
					}
				}
			}
			return Collections.unmodifiableSet(manifestEntries);
		}
		catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to evaluate 'java.class.path' manifest entries: " + ex);
			}
			return Collections.emptySet();
		}
	}

	private Set<ClassPathManifestEntry> getClassPathManifestEntriesFromJar(File jar) throws IOException {
		URL base = jar.toURI().toURL();
		File parent = jar.getAbsoluteFile().getParentFile();

		try (JarFile jarFile = new JarFile(jar)) {
			Manifest manifest = jarFile.getManifest();
			Attributes attributes = (manifest != null ? manifest.getMainAttributes() : null);
			String classPath = (attributes != null ? attributes.getValue(Name.CLASS_PATH) : null);

			Set<ClassPathManifestEntry> manifestEntries = new LinkedHashSet<>();
			if (StringUtils.hasLength(classPath)) {
				StringTokenizer tokenizer = new StringTokenizer(classPath);
				while (tokenizer.hasMoreTokens()) {
					String path = tokenizer.nextToken();
					if (path.indexOf(':') >= 0 && !"file".equalsIgnoreCase(new URL(base, path).getProtocol())) {
						// See jdk.internal.loader.URLClassPath.JarLoader.tryResolveFile(URL, String)
						continue;
					}

					// Handle absolute paths correctly: do not apply parent to absolute paths.
					File pathFile = null;
					boolean absolute = false;
					if (path.startsWith(ResourceUtils.FILE_URL_PREFIX)) {
						try {
							pathFile = new File(ResourceUtils.toURI(path));
							absolute = true;
						}
						catch (URISyntaxException | IllegalArgumentException ex) {
							// Fall back to plain String constructor below.
						}
					}
					if (pathFile == null) {
						pathFile = new File(path);
						absolute = pathFile.isAbsolute();
					}
					File candidate = (absolute ? pathFile : new File(parent, path));

					// For relative paths, enforce security check: must be under parent.
					// For absolute paths, just verify file exists (matching JVM behavior).
					if (candidate.isFile() && (absolute ||
							candidate.getCanonicalPath().contains(parent.getCanonicalPath()))) {
						manifestEntries.add(ClassPathManifestEntry.of(candidate, this.useCaches));
					}
				}
			}
			return Collections.unmodifiableSet(manifestEntries);
		}
		catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to load manifest entries from jar file '" + jar + "': " + ex);
			}
			return Collections.emptySet();
		}
	}

	/**
	 * 通过 Ant 风格的 {@link #getPathMatcher() PathMatcher} 查找匹配给定位置模式的所有资源。
	 * <p>支持 OSGi 捆绑包、JBoss VFS、jar 文件、zip 文件和文件系统中的资源。
	 * @param locationPattern 要匹配的位置模式
	 * @return 结果作为 Resource 数组
	 * @throws IOException 在 I/O 错误的情况下
	 * @see #determineRootDir(String)
	 * @see #resolveRootDirResource(Resource)
	 * @see #isJarResource(Resource)
	 * @see #doFindPathMatchingJarResources(Resource, URL, String)
	 * @see #doFindPathMatchingFileResources(Resource, String)
	 * @see org.springframework.util.PathMatcher
	 */
	protected Resource[] findPathMatchingResources(String locationPattern) throws IOException {
		String rootDirPath = determineRootDir(locationPattern);
		String subPattern = locationPattern.substring(rootDirPath.length());

		// Look for pre-cached root dir resources, either a direct match or
		// a match for a parent directory in the same classpath locations.
		Resource[] rootDirResources = this.rootDirCache.get(rootDirPath);
		String actualRootPath = null;
		if (rootDirResources == null) {
			// No direct match -> search for a common parent directory match
			// (cached based on repeated searches in the same base location,
			// in particular for different root directories in the same jar).
			String commonPrefix = null;
			String existingPath = null;
			boolean commonUnique = true;
			for (String path : this.rootDirCache.keySet()) {
				String currentPrefix = null;
				for (int i = 0; i < path.length(); i++) {
					if (i == rootDirPath.length() || path.charAt(i) != rootDirPath.charAt(i)) {
						currentPrefix = path.substring(0, path.lastIndexOf('/', i - 1) + 1);
						break;
					}
				}
				if (currentPrefix != null) {
					if (checkPathWithinPackage(path.substring(currentPrefix.length()))) {
						// A prefix match found, potentially to be turned into a common parent cache entry.
						if (commonPrefix == null || !commonUnique || currentPrefix.length() > commonPrefix.length()) {
							commonPrefix = currentPrefix;
							existingPath = path;
						}
						else if (currentPrefix.equals(commonPrefix)) {
							commonUnique = false;
						}
					}
				}
				else if (actualRootPath == null || path.length() > actualRootPath.length()) {
					// A direct match found for a parent directory -> use it.
					rootDirResources = this.rootDirCache.get(path);
					actualRootPath = path;
				}
			}
			if (rootDirResources == null && StringUtils.hasLength(commonPrefix)) {
				// Try common parent directory as long as it points to the same classpath locations.
				rootDirResources = getResources(commonPrefix);
				Resource[] existingResources = this.rootDirCache.get(existingPath);
				if (existingResources != null && rootDirResources.length == existingResources.length) {
					// Replace existing subdirectory cache entry with common parent directory,
					// avoiding repeated determination of root directories in the same jar.
					this.rootDirCache.remove(existingPath);
					this.rootDirCache.put(commonPrefix, rootDirResources);
					actualRootPath = commonPrefix;
				}
				else if (commonPrefix.equals(rootDirPath)) {
					// The identified common directory is equal to the currently requested path ->
					// worth caching specifically, even if it cannot replace the existing sub-entry.
					this.rootDirCache.put(rootDirPath, rootDirResources);
				}
				else {
					// Mismatch: parent directory points to more classpath locations.
					rootDirResources = null;
				}
			}
			if (rootDirResources == null) {
				// Lookup for specific directory, creating a cache entry for it.
				rootDirResources = getResources(rootDirPath);
				if (this.useCaches == null || this.useCaches) {
					this.rootDirCache.put(rootDirPath, rootDirResources);
				}
			}
		}

		Set<Resource> result = new LinkedHashSet<>(64);
		for (Resource rootDirResource : rootDirResources) {
			if (actualRootPath != null && actualRootPath.length() < rootDirPath.length()) {
				// Create sub-resource for requested sub-location from cached common root directory.
				rootDirResource = rootDirResource.createRelative(rootDirPath.substring(actualRootPath.length()));
			}
			rootDirResource = resolveRootDirResource(rootDirResource);
			URL rootDirUrl = rootDirResource.getURL();
			if (equinoxResolveMethod != null && rootDirUrl.getProtocol().startsWith("bundle")) {
				URL resolvedUrl = (URL) ReflectionUtils.invokeMethod(equinoxResolveMethod, null, rootDirUrl);
				if (resolvedUrl != null) {
					rootDirUrl = resolvedUrl;
				}
				UrlResource urlResource = new UrlResource(rootDirUrl);
				if (this.useCaches != null) {
					urlResource.setUseCaches(this.useCaches);
				}
				rootDirResource = urlResource;
			}
			if (rootDirUrl.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
				result.addAll(VfsResourceMatchingDelegate.findMatchingResources(rootDirUrl, subPattern, getPathMatcher()));
			}
			else if (ResourceUtils.isJarURL(rootDirUrl) || isJarResource(rootDirResource)) {
				result.addAll(doFindPathMatchingJarResources(rootDirResource, rootDirUrl, subPattern));
			}
			else {
				result.addAll(doFindPathMatchingFileResources(rootDirResource, subPattern));
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Resolved location pattern [" + locationPattern + "] to resources " + result);
		}
		return result.toArray(EMPTY_RESOURCE_ARRAY);
	}

	/**
	 * 确定给定位置的根目录。
	 * <p>用于确定文件匹配的起点，解析要传递给 {@link #getResources(String)} 的根目录位置，
	 * 位置的其余部分将用作子模式。
	 * <p>例如，对于位置 "/WEB-INF/*.xml"，将返回 "/WEB-INF/"。
	 * @param location 要检查的位置
	 * @return 表示根目录的位置部分
	 * @see #findPathMatchingResources(String)
	 */
	protected String determineRootDir(String location) {
		int prefixEnd = location.indexOf(':') + 1;
		int rootDirEnd = location.length();
		while (rootDirEnd > prefixEnd && getPathMatcher().isPattern(location.substring(prefixEnd, rootDirEnd))) {
			rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2) + 1;
		}
		if (rootDirEnd == 0) {
			rootDirEnd = prefixEnd;
		}
		return location.substring(0, rootDirEnd);
	}

	/**
	 * 解析提供的根目录资源以进行路径匹配。
	 * <p>默认情况下，{@link #findPathMatchingResources(String)} 将 Equinox
	 * OSGi "bundleresource:" 和 "bundleentry:" URLs 解析为标准 jar 文件 URLs，
	 * 这些 URLs 将使用 Spring 的标准 jar 文件遍历算法进行遍历。
	 * <p>对于任何自定义解析，请重写此模板方法并相应替换提供的资源句柄。
	 * <p>此方法的默认实现返回未修改的提供的资源。
	 * @param original 要解析的资源
	 * @return 解析后的资源（可能与提供的资源相同）
	 * @throws IOException 在解析失败的情况下
	 * @see #findPathMatchingResources(String)
	 */
	protected Resource resolveRootDirResource(Resource original) throws IOException {
		return original;
	}

	/**
	 * 确定给定的资源句柄是否表示 jar 资源，{@link #doFindPathMatchingJarResources} 方法可以处理。
	 * <p>{@link #findPathMatchingResources(String)} 委托给
	 * {@link ResourceUtils#isJarURL(URL)} 来确定给定的 URL 是否指向 jar 文件中的资源，
	 * 并且仅在回退时调用此方法。
	 * <p>因此，此模板方法允许检测更多种类的类似 jar 的资源 &mdash; 例如，
	 * 通过在资源句柄类型上进行 {@code instanceof} 检查。
	 * <p>此方法的默认实现返回 {@code false}。
	 * @param resource 要检查的资源句柄（通常是启动路径匹配的根目录）
	 * @return 如果给定的资源句柄表示 jar 资源，则为 {@code true}
	 * @throws IOException 在 I/O 错误的情况下
	 * @see #findPathMatchingResources(String)
	 * @see #doFindPathMatchingJarResources(Resource, URL, String)
	 * @see org.springframework.util.ResourceUtils#isJarURL
	 */
	protected boolean isJarResource(Resource resource) throws IOException {
		return false;
	}

	/**
	 * 通过 Ant 风格的 {@link #getPathMatcher() PathMatcher} 在 jar 文件中查找匹配给定位置模式的所有资源。
	 * @param rootDirResource 作为 Resource 的根目录
	 * @param rootDirUrl 预解析的根目录 URL
	 * @param subPattern 要匹配的子模式（在根目录下面）
	 * @return 匹配的 Resource 实例的可变 Set
	 * @throws IOException 在 I/O 错误的情况下
	 * @since 4.3
	 * @see java.net.JarURLConnection
	 * @see org.springframework.util.PathMatcher
	 */
	protected Set<Resource> doFindPathMatchingJarResources(Resource rootDirResource, URL rootDirUrl, String subPattern)
			throws IOException {

		String jarFileUrl = null;
		String rootEntryPath = "";

		String urlFile = rootDirUrl.getFile();
		int separatorIndex = urlFile.indexOf(ResourceUtils.WAR_URL_SEPARATOR);
		if (separatorIndex == -1) {
			separatorIndex = urlFile.indexOf(ResourceUtils.JAR_URL_SEPARATOR);
		}
		if (separatorIndex >= 0) {
			jarFileUrl = urlFile.substring(0, separatorIndex);
			rootEntryPath = urlFile.substring(separatorIndex + 2);  // both separators are 2 chars
			NavigableSet<String> entriesCache = this.jarEntriesCache.get(jarFileUrl);
			if (entriesCache != null) {
				Set<Resource> result = new LinkedHashSet<>(64);
				// Clean root entry path to match jar entries format without "!" separators
				rootEntryPath = rootEntryPath.replace(ResourceUtils.JAR_URL_SEPARATOR, "/");
				// Search sorted entries from first entry with rootEntryPath prefix
				boolean rootEntryPathFound = false;
				for (String entryPath : entriesCache.tailSet(rootEntryPath, false)) {
					if (!entryPath.startsWith(rootEntryPath)) {
						// We are beyond the potential matches in the current TreeSet.
						break;
					}
					rootEntryPathFound = true;
					String relativePath = entryPath.substring(rootEntryPath.length());
					if (getPathMatcher().match(subPattern, relativePath)) {
						result.add(rootDirResource.createRelative(relativePath));
					}
				}
				if (rootEntryPathFound) {
					return result;
				}
			}
		}

		URLConnection con = rootDirUrl.openConnection();
		JarFile jarFile;
		boolean closeJarFile;

		if (con instanceof JarURLConnection jarCon) {
			// Should usually be the case for traditional JAR files.
			if (this.useCaches != null) {
				jarCon.setUseCaches(this.useCaches);
			}
			try {
				jarFile = jarCon.getJarFile();
				jarFileUrl = jarCon.getJarFileURL().toExternalForm();
				JarEntry jarEntry = jarCon.getJarEntry();
				rootEntryPath = (jarEntry != null ? jarEntry.getName() : "");
				closeJarFile = !jarCon.getUseCaches();
			}
			catch (ZipException | FileNotFoundException | NoSuchFileException ex) {
				// Happens in case of a non-jar file or in case of a cached root directory
				// without the specific subdirectory present, respectively.
				return Collections.emptySet();
			}
		}
		else {
			// No JarURLConnection -> need to resort to URL file parsing.
			// We'll assume URLs of the format "jar:path!/entry", with the protocol
			// being arbitrary as long as following the entry format.
			// We'll also handle paths with and without leading "file:" prefix.
			try {
				if (jarFileUrl != null) {
					jarFile = getJarFile(jarFileUrl);
				}
				else {
					jarFile = new JarFile(urlFile);
					jarFileUrl = urlFile;
					rootEntryPath = "";
				}
				closeJarFile = true;
			}
			catch (ZipException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Skipping invalid jar class path entry [" + urlFile + "]");
				}
				return Collections.emptySet();
			}
		}

		try {
			if (logger.isTraceEnabled()) {
				logger.trace("Looking for matching resources in jar file [" + jarFileUrl + "]");
			}
			if (StringUtils.hasLength(rootEntryPath) && !rootEntryPath.endsWith("/")) {
				// Root entry path must end with slash to allow for proper matching.
				// The Sun JRE does not return a slash here, but BEA JRockit does.
				rootEntryPath = rootEntryPath + "/";
			}
			Set<Resource> result = new LinkedHashSet<>(64);
			NavigableSet<String> entriesCache = new TreeSet<>();
			for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
				entriesCache.add(entries.nextElement().getName());
			}
			for (String entryPath : entriesCache) {
				if (entryPath.startsWith(rootEntryPath)) {
					String relativePath = entryPath.substring(rootEntryPath.length());
					if (getPathMatcher().match(subPattern, relativePath)) {
						result.add(rootDirResource.createRelative(relativePath));
					}
				}
			}
			if (this.useCaches == null || this.useCaches) {
				// Cache jar entries in TreeSet for efficient searching on re-encounter.
				this.jarEntriesCache.put(jarFileUrl, entriesCache);
			}
			return result;
		}
		finally {
			if (closeJarFile) {
				jarFile.close();
			}
		}
	}

	/**
	 * 将给定的 jar 文件 URL 解析为 JarFile 对象。
	 */
	protected JarFile getJarFile(String jarFileUrl) throws IOException {
		if (jarFileUrl.startsWith(ResourceUtils.FILE_URL_PREFIX)) {
			try {
				return new JarFile(ResourceUtils.toURI(jarFileUrl).getSchemeSpecificPart());
			}
			catch (URISyntaxException ex) {
				// Fallback for URLs that are not valid URIs (should hardly ever happen).
				return new JarFile(jarFileUrl.substring(ResourceUtils.FILE_URL_PREFIX.length()));
			}
		}
		else {
			return new JarFile(jarFileUrl);
		}
	}

	/**
	 * 通过 Ant 风格的 {@link #getPathMatcher() PathMatcher} 在提供的根目录的文件系统中
	 * 查找匹配给定位置子模式的所有资源。
	 * @param rootDirResource 作为 Resource 的根目录
	 * @param subPattern 要匹配的子模式（在根目录下面）
	 * @return 匹配的 Resource 实例的可变 Set
	 * @throws IOException 在 I/O 错误的情况下
	 * @see org.springframework.util.PathMatcher
	 */
	protected Set<Resource> doFindPathMatchingFileResources(Resource rootDirResource, String subPattern)
			throws IOException {

		Set<Resource> result = new LinkedHashSet<>(64);
		URI rootDirUri;
		try {
			rootDirUri = rootDirResource.getURI();
		}
		catch (Exception ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to resolve directory [%s] as URI: %s".formatted(rootDirResource, ex));
			}
			return result;
		}

		Path rootPath = null;
		if (rootDirUri.isAbsolute() && !rootDirUri.isOpaque()) {
			// Prefer Path resolution from URI if possible
			try {
				try {
					rootPath = Path.of(rootDirUri);
				}
				catch (FileSystemNotFoundException ex) {
					// If the file system was not found, assume it's a custom file system that needs to be installed.
					FileSystems.newFileSystem(rootDirUri, Map.of(), ClassUtils.getDefaultClassLoader());
					rootPath = Path.of(rootDirUri);
				}
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to resolve %s in file system: %s".formatted(rootDirUri, ex));
				}
				// Fallback via Resource.getFile() below
			}
		}

		if (rootPath == null) {
			// Resource.getFile() resolution as a fallback -
			// for custom URI formats and custom Resource implementations
			try {
				rootPath = Path.of(rootDirResource.getFile().getAbsolutePath());
			}
			catch (FileNotFoundException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot search for matching files underneath " + rootDirResource +
							" in the file system: " + ex.getMessage());
				}
				return result;
			}
			catch (Exception ex) {
				if (logger.isInfoEnabled()) {
					logger.info("Failed to resolve " + rootDirResource + " in the file system: " + ex);
				}
				return result;
			}
		}

		if (!Files.exists(rootPath)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Skipping search for files matching pattern [%s]: directory [%s] does not exist"
						.formatted(subPattern, rootPath.toAbsolutePath()));
			}
			return result;
		}

		String rootDir = StringUtils.cleanPath(rootPath.toString());
		if (!rootDir.endsWith("/")) {
			rootDir += "/";
		}

		Path rootPathForPattern = rootPath;
		String resourcePattern = rootDir + StringUtils.cleanPath(subPattern);
		Predicate<Path> isMatchingFile = path -> (!path.equals(rootPathForPattern) &&
				getPathMatcher().match(resourcePattern, StringUtils.cleanPath(path.toString())));

		if (logger.isTraceEnabled()) {
			logger.trace("Searching directory [%s] for files matching pattern [%s]"
					.formatted(rootPath.toAbsolutePath(), subPattern));
		}

		try (Stream<Path> files = Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS)) {
			files.filter(isMatchingFile).sorted().map(FileSystemResource::new).forEach(result::add);
		}
		catch (Exception ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to search in directory [%s] for files matching pattern [%s]: %s"
						.formatted(rootPath.toAbsolutePath(), subPattern, ex));
			}
		}
		return result;
	}

	/**
	 * 将给定位置模式解析为 {@code Resource} 对象，用于在模块路径中找到的所有匹配资源。
	 * <p>位置模式可能是显式的资源路径，如 {@code "com/example/config.xml"}
	 * 或使用配置的 {@link #getPathMatcher() PathMatcher} 匹配的模式，如
	 * <code>"com/example/**&#47;config-*.xml"</code>。
	 * <p>默认实现在 {@linkplain ModuleLayer#boot() 引导层} 中扫描所有模块，
	 * 排除 {@linkplain ModuleFinder#ofSystem() 系统模块}。
	 * @param locationPattern 要解析的位置模式
	 * @return 包含相应的 {@code Resource} 对象的可修改 {@code Set}
	 * @throws IOException 在 I/O 错误的情况下
	 * @since 6.0
	 * @see ModuleLayer#boot()
	 * @see ModuleFinder#ofSystem()
	 * @see ModuleReader
	 * @see PathMatcher#match(String, String)
	 */
	protected Set<Resource> findAllModulePathResources(String locationPattern) throws IOException {
		Set<Resource> result = new LinkedHashSet<>(64);

		// Skip scanning the module path when running in a native image.
		if (NativeDetector.inNativeImage()) {
			return result;
		}

		String resourcePattern = stripLeadingSlash(locationPattern);
		Predicate<String> resourcePatternMatches = (getPathMatcher().isPattern(resourcePattern) ?
				path -> getPathMatcher().match(resourcePattern, path) :
				resourcePattern::equals);

		try {
			ModuleLayer.boot().configuration().modules().stream()
					.filter(isNotSystemModule)
					.forEach(resolvedModule -> {
						// NOTE: a ModuleReader and a Stream returned from ModuleReader.list() must be closed.
						try (ModuleReader moduleReader = resolvedModule.reference().open();
								Stream<String> names = moduleReader.list()) {
							names.filter(resourcePatternMatches)
									.map(name -> findResource(moduleReader, name))
									.filter(Objects::nonNull)
									.forEach(result::add);
						}
						catch (IOException ex) {
							if (logger.isDebugEnabled()) {
								logger.debug("Failed to read contents of module [%s]".formatted(resolvedModule), ex);
							}
							throw new UncheckedIOException(ex);
						}
					});
		}
		catch (UncheckedIOException ex) {
			// Unwrap IOException to conform to this method's contract.
			throw ex.getCause();
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Resolved module-path location pattern [%s] to resources %s".formatted(resourcePattern, result));
		}
		return result;
	}

	@Nullable
	private Resource findResource(ModuleReader moduleReader, String name) {
		try {
			return moduleReader.find(name)
					.map(this::convertModuleSystemURI)
					.orElse(null);
		}
		catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to find resource [%s] in module path".formatted(name), ex);
			}
			return null;
		}
	}

	/**
	 * 如果是 "file:" URI，使用 {@link FileSystemResource} 避免通过类路径扫描发现的相同路径的重复项。
	 */
	private Resource convertModuleSystemURI(URI uri) {
		return (ResourceUtils.URL_PROTOCOL_FILE.equals(uri.getScheme()) ?
				new FileSystemResource(uri.getPath()) : UrlResource.from(uri));
	}

	private static String stripLeadingSlash(String path) {
		return (path.startsWith("/") ? path.substring(1) : path);
	}

	private static boolean checkPathWithinPackage(String path) {
		return (path.contains("/") && !path.contains(ResourceUtils.JAR_URL_SEPARATOR));
	}


	/**
	 * 内部委托类，避免在运行时对 JBoss VFS API 的硬依赖。
	 */
	private static class VfsResourceMatchingDelegate {

		public static Set<Resource> findMatchingResources(
				URL rootDirUrl, String locationPattern, PathMatcher pathMatcher) throws IOException {

			Object root = VfsPatternUtils.findRoot(rootDirUrl);
			PatternVirtualFileVisitor visitor =
					new PatternVirtualFileVisitor(VfsPatternUtils.getPath(root), locationPattern, pathMatcher);
			VfsPatternUtils.visit(root, visitor);
			return visitor.getResources();
		}
	}


	/**
	 * 用于路径匹配的 VFS 访问器。
	 */
	@SuppressWarnings("unused")
	private static class PatternVirtualFileVisitor implements InvocationHandler {

		private final String subPattern;

		private final PathMatcher pathMatcher;

		private final String rootPath;

		private final Set<Resource> resources = new LinkedHashSet<>(64);

		public PatternVirtualFileVisitor(String rootPath, String subPattern, PathMatcher pathMatcher) {
			this.subPattern = subPattern;
			this.pathMatcher = pathMatcher;
			this.rootPath = (rootPath.isEmpty() || rootPath.endsWith("/") ? rootPath : rootPath + "/");
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String methodName = method.getName();
			if (Object.class == method.getDeclaringClass()) {
				switch (methodName) {
					case "equals" -> {
						// Only consider equal when proxies are identical.
						return (proxy == args[0]);
					}
					case "hashCode" -> {
						return System.identityHashCode(proxy);
					}
				}
			}
			return switch (methodName) {
				case "getAttributes" -> getAttributes();
				case "visit" -> {
					visit(args[0]);
					yield null;
				}
				case "toString" -> toString();
				default -> throw new IllegalStateException("Unexpected method invocation: " + method);
			};
		}

		public void visit(Object vfsResource) {
			if (this.pathMatcher.match(this.subPattern,
					VfsPatternUtils.getPath(vfsResource).substring(this.rootPath.length()))) {
				this.resources.add(new VfsResource(vfsResource));
			}
		}

		@Nullable
		public Object getAttributes() {
			return VfsPatternUtils.getVisitorAttributes();
		}

		public Set<Resource> getResources() {
			return this.resources;
		}

		public int size() {
			return this.resources.size();
		}

		@Override
		public String toString() {
			return "sub-pattern: " + this.subPattern + ", resources: " + this.resources;
		}
	}


	/**
	 * 单个 {@code Class-Path} 清单条目。
	 */
	private record ClassPathManifestEntry(Resource resource, @Nullable Resource alternative) {

		private static final String JARFILE_URL_PREFIX = ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX;

		static ClassPathManifestEntry of(File file, @Nullable Boolean useCaches) throws MalformedURLException {
			String path = fixPath(file.getAbsolutePath());
			Resource resource = asJarFileResource(path, useCaches);
			Resource alternative = createAlternative(path, useCaches);
			return new ClassPathManifestEntry(resource, alternative);
		}

		private static String fixPath(String path) {
			int prefixIndex = path.indexOf(':');
			if (prefixIndex == 1) {
				// Possibly a drive prefix on Windows (for example, "c:"), so we prepend a slash
				// and convert the drive letter to uppercase for consistent duplicate detection.
				path = "/" + StringUtils.capitalize(path);
			}
			// Since '#' can appear in directories/filenames, java.net.URL should not treat it as a fragment
			return StringUtils.replace(path, "#", "%23");
		}

		/**
		 * 返回资源的替代形式，即带或不带前导斜杠。
		 * @param path 文件路径（带或不带前导斜杠）
		 * @return 替代形式或 {@code null}
		 */
		@Nullable
		private static Resource createAlternative(String path, @Nullable Boolean useCaches) {
			try {
				String alternativePath = path.startsWith("/") ? path.substring(1) : "/" + path;
				return asJarFileResource(alternativePath, useCaches);
			}
			catch (MalformedURLException ex) {
				return null;
			}
		}

		private static Resource asJarFileResource(String path, @Nullable Boolean useCaches) throws MalformedURLException {
			UrlResource resource = new UrlResource(JARFILE_URL_PREFIX + path + ResourceUtils.JAR_URL_SEPARATOR);
			if (useCaches != null) {
				resource.setUseCaches(useCaches);
			}
			return resource;
		}
	}

}
