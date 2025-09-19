/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet.mvc.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * 一种逻辑析取（' || '）请求条件，用于将请求与一组URL路径模式进行匹配。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class PatternsRequestCondition extends AbstractRequestCondition<PatternsRequestCondition> {

	private final Set<String> patterns;

	private final UrlPathHelper pathHelper;

	private final PathMatcher pathMatcher;

	private final boolean useSuffixPatternMatch;

	private final boolean useTrailingSlashMatch;

	private final List<String> fileExtensions = new ArrayList<>();


	/**
	 * 使用给定的URL模式创建新实例。
	 * 每个非空且不以"/"开头的模式都会自动在前面添加"/"。
	 * @param patterns 0个或多个URL模式；如果为0，则该条件将匹配所有请求。
	 */
	public PatternsRequestCondition(String... patterns) {
		this(Arrays.asList(patterns), null, null, true, true, null);
	}

	/**
	 * 带有后缀模式(.*)和尾部斜杠匹配标志的附加构造函数。
	 * @param patterns 要使用的URL模式；如果为0，则该条件将匹配所有请求。
	 * @param urlPathHelper 用于确定请求的查找路径
	 * @param pathMatcher 用于模式路径匹配
	 * @param useSuffixPatternMatch 是否启用后缀匹配(".*")
	 * @param useTrailingSlashMatch 是否匹配带或不带尾部斜杠的路径
	 */
	public PatternsRequestCondition(String[] patterns, @Nullable UrlPathHelper urlPathHelper,
			@Nullable PathMatcher pathMatcher, boolean useSuffixPatternMatch, boolean useTrailingSlashMatch) {

		this(Arrays.asList(patterns), urlPathHelper, pathMatcher, useSuffixPatternMatch, useTrailingSlashMatch, null);
	}

	/**
	 * 使用给定的URL模式创建新实例。
	 * 每个非空且不以"/"开头的模式都会自动在前面添加"/"。
	 * @param patterns 要使用的URL模式；如果为0，则该条件将匹配所有请求。
	 * @param urlPathHelper 用于确定请求查找路径的{@link UrlPathHelper}
	 * @param pathMatcher 用于模式路径匹配的{@link PathMatcher}
	 * @param useSuffixPatternMatch 是否启用后缀匹配(".*")
	 * @param useTrailingSlashMatch 是否匹配带或不带尾部斜杠的路径
	 * @param fileExtensions 用于路径匹配考虑的文件扩展名列表
	 */
	public PatternsRequestCondition(String[] patterns, @Nullable UrlPathHelper urlPathHelper,
			@Nullable PathMatcher pathMatcher, boolean useSuffixPatternMatch,
			boolean useTrailingSlashMatch, @Nullable List<String> fileExtensions) {

		this(Arrays.asList(patterns), urlPathHelper, pathMatcher, useSuffixPatternMatch,
				useTrailingSlashMatch, fileExtensions);
	}

	/**
	 * 接受模式集合的私有构造函数。
	 */
	private PatternsRequestCondition(Collection<String> patterns, @Nullable UrlPathHelper urlPathHelper,
			@Nullable PathMatcher pathMatcher, boolean useSuffixPatternMatch,
			boolean useTrailingSlashMatch, @Nullable List<String> fileExtensions) {

		this.patterns = Collections.unmodifiableSet(prependLeadingSlash(patterns));
		this.pathHelper = (urlPathHelper != null ? urlPathHelper : UrlPathHelper.defaultInstance);
		this.pathMatcher = (pathMatcher != null ? pathMatcher : new AntPathMatcher());
		this.useSuffixPatternMatch = useSuffixPatternMatch;
		this.useTrailingSlashMatch = useTrailingSlashMatch;

		if (fileExtensions != null) {
			for (String fileExtension : fileExtensions) {
				if (fileExtension.charAt(0) != '.') {
					fileExtension = "." + fileExtension;
				}
				this.fileExtensions.add(fileExtension);
			}
		}
	}


	private static Set<String> prependLeadingSlash(Collection<String> patterns) {
		Set<String> result = new LinkedHashSet<>(patterns.size());
		for (String pattern : patterns) {
			if (StringUtils.hasLength(pattern) && !pattern.startsWith("/")) {
				pattern = "/" + pattern;
			}
			result.add(pattern);
		}
		return result;
	}

	public Set<String> getPatterns() {
		return this.patterns;
	}

	@Override
	protected Collection<String> getContent() {
		return this.patterns;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 返回一个新实例，该实例包含来自当前实例（"this"）和"other"实例的URL模式，
	 * 组合规则如下：
	 * <ul>
	 *     <li>如果两个实例中都存在模式，则使用{@link PathMatcher#combine(String, String)}将"this"中的模式与"other"中的模式进行合并
	 *     <li>如果只有一个实例包含模式，则直接使用这些模式
	 *     <li>如果两个实例都不包含模式，则使用空字符串（即""）
	 * </ul>
	 */
	@Override
	public PatternsRequestCondition combine(PatternsRequestCondition other) {
		Set<String> result = new LinkedHashSet<>();
		if (!this.patterns.isEmpty() && !other.patterns.isEmpty()) {
			for (String pattern1 : this.patterns) {
				for (String pattern2 : other.patterns) {
					result.add(this.pathMatcher.combine(pattern1, pattern2));
				}
			}
		}
		else if (!this.patterns.isEmpty()) {
			result.addAll(this.patterns);
		}
		else if (!other.patterns.isEmpty()) {
			result.addAll(other.patterns);
		}
		else {
			result.add("");
		}
		return new PatternsRequestCondition(result, this.pathHelper, this.pathMatcher,
				this.useSuffixPatternMatch, this.useTrailingSlashMatch, this.fileExtensions);
	}

	/**
	 * 检查是否存在与给定请求匹配的模式，并返回保证包含匹配模式的新实例，
	 * 这些模式通过{@link PathMatcher#getPatternComparator(String)}进行排序。
	 * <p>通过按以下顺序进行检查来获取匹配模式：
	 * <ul>
	 *      <li>直接匹配
	 *      <li>如果模式不包含"."，则在模式后附加".*"进行模式匹配
	 *      <li>模式匹配
	 *      <li>如果模式不以"/"结尾，则在模式后附加"/"进行模式匹配
	 * </ul>
	 *
	 * @param request 当前请求
	 * @return 如果条件不包含模式，则返回相同实例；
	 *         或者返回包含已排序匹配模式的新条件实例；
	 *         或者如果没有模式匹配，则返回{@code null}
	 */
	@Override
	@Nullable
	public PatternsRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (this.patterns.isEmpty()) {
			return this;
		}
		String lookupPath = this.pathHelper.getLookupPathForRequest(request);
		List<String> matches = getMatchingPatterns(lookupPath);
		return (!matches.isEmpty() ?
				new PatternsRequestCondition(matches, this.pathHelper, this.pathMatcher,
						this.useSuffixPatternMatch, this.useTrailingSlashMatch, this.fileExtensions) : null);
	}

	/**
	 * 查找与给定查找路径匹配的模式。
	 * 调用此方法应产生与调用 {@link #getMatchingCondition(javax.servlet.http.HttpServletRequest)} 等效的结果。
	 * 此方法提供了在没有请求对象可用时（例如内省、工具处理等场景）的替代方案。
	 *
	 * @param lookupPath 要与现有模式进行匹配的查找路径
	 * @return 匹配模式的集合，按最接近的匹配排序（最佳匹配排在最前）
	 */
	public List<String> getMatchingPatterns(String lookupPath) {
		List<String> matches = new ArrayList<>();
		for (String pattern : this.patterns) {
			String match = getMatchingPattern(pattern, lookupPath);
			if (match != null) {
				matches.add(match);
			}
		}
		if (matches.size() > 1) {
			matches.sort(this.pathMatcher.getPatternComparator(lookupPath));
		}
		return matches;
	}

	@Nullable
	private String getMatchingPattern(String pattern, String lookupPath) {
		if (pattern.equals(lookupPath)) {
			return pattern;
		}
		if (this.useSuffixPatternMatch) {
			if (!this.fileExtensions.isEmpty() && lookupPath.indexOf('.') != -1) {
				for (String extension : this.fileExtensions) {
					if (this.pathMatcher.match(pattern + extension, lookupPath)) {
						return pattern + extension;
					}
				}
			}
			else {
				boolean hasSuffix = pattern.indexOf('.') != -1;
				if (!hasSuffix && this.pathMatcher.match(pattern + ".*", lookupPath)) {
					return pattern + ".*";
				}
			}
		}
		if (this.pathMatcher.match(pattern, lookupPath)) {
			return pattern;
		}
		if (this.useTrailingSlashMatch) {
			if (!pattern.endsWith("/") && this.pathMatcher.match(pattern + "/", lookupPath)) {
				return pattern + "/";
			}
		}
		return null;
	}

	/**
	 * 根据两个条件所包含的URL模式进行比较。
	 * 通过{@link PathMatcher#getPatternComparator(String)}方法，对模式进行逐行从上到下的比较。
	 * 如果所有比较的模式匹配度相同，但其中一个实例包含更多模式，则认为它是更精确的匹配。
	 * <p>
	 *     假定两个实例都是通过{@link #getMatchingCondition(HttpServletRequest)}获取的，
	 *     以确保它们仅包含与请求匹配的模式，并且按照最佳匹配优先的顺序排序。
	 */
	@Override
	public int compareTo(PatternsRequestCondition other, HttpServletRequest request) {
		String lookupPath = this.pathHelper.getLookupPathForRequest(request);
		Comparator<String> patternComparator = this.pathMatcher.getPatternComparator(lookupPath);
		Iterator<String> iterator = this.patterns.iterator();
		Iterator<String> iteratorOther = other.patterns.iterator();
		while (iterator.hasNext() && iteratorOther.hasNext()) {
			int result = patternComparator.compare(iterator.next(), iteratorOther.next());
			if (result != 0) {
				return result;
			}
		}
		if (iterator.hasNext()) {
			return -1;
		}
		else if (iteratorOther.hasNext()) {
			return 1;
		}
		else {
			return 0;
		}
	}

}
