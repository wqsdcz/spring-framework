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

package org.springframework.util;

import java.util.Comparator;
import java.util.Map;

/**
 * 【路径匹配器】
 * <p>基于{@code String}的路径匹配策略接口。<p/>
 * <p>被 {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}、
 * {@link org.springframework.web.servlet.handler.AbstractUrlHandlerMapping}、
 * {@link org.springframework.web.servlet.mvc.WebContentInterceptor} 所使用。<p/>
 * <p>{@link AntPathMatcher}是其的默认实现，支持Ant风格的模式语法。<p/>
 *
 * Strategy interface for {@code String}-based path matching.
 *
 * <p>Used by {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver},
 * {@link org.springframework.web.servlet.handler.AbstractUrlHandlerMapping},
 * and {@link org.springframework.web.servlet.mvc.WebContentInterceptor}.
 *
 * <p>The default implementation is {@link AntPathMatcher}, supporting the
 * Ant-style pattern syntax.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see AntPathMatcher
 */
public interface PathMatcher {

	/**
	 * 给定的 {@code path}是否是模式，该模式会被该接口的实现使用匹配？
	 * <p>如果返回值为 {@code false}，那么就不必使用 {@link #match} 方法，因为对静态路径字符串进行直接的相等性比较将得出相同的结果。
	 *
	 * Does the given {@code path} represent a pattern that can be matched
	 * by an implementation of this interface?
	 * <p>If the return value is {@code false}, then the {@link #match}
	 * method does not have to be used because direct equality comparisons
	 * on the static path Strings will lead to the same result.
	 * @param path the path String to check
	 * @return {@code true} if the given {@code path} represents a pattern
	 */
	boolean isPattern(String path);

	/**
	 * <p>根据PathMatcher的匹配策略，将给定的{@code path}与给定的{@code pattern}进行匹配。<p/>
	 *
	 * Match the given {@code path} against the given {@code pattern},
	 * according to this PathMatcher's matching strategy.
	 * @param pattern the pattern to match against
	 * @param path the path String to test
	 * @return {@code true} if the supplied {@code path} matched,
	 * {@code false} if it didn't
	 */
	boolean match(String pattern, String path);

	/**
	 * <p>根据PathMatcher的匹配策略，将给定的{@code path}与给定的{@code pattern}的相应部分进行匹配。
	 * <p>确定模式是否至少匹配给定的基本路径，假设完整路径也可以匹配。
	 *
	 * @param pattern the pattern to match against
	 * @param path the path String to test
	 * @return {@code true} if the supplied {@code path} matched,
	 * {@code false} if it didn't
	 */
	boolean matchStart(String pattern, String path);

	/**
	 * 【使用模式提取路径】
	 * <p>指定一个模式和一个完整路径，确定模式映射的部分。
	 * <p>此方法旨在通过实际模式动态地找出路径中与之匹配的部分，也就是说，它会从给定的完整路径中去除静态定义的前导路径，只返回实际模式匹配的部分的路径。
	 * <p>例如：模式为"myroot/*.html"，完整路径为"myroot/myfile.html"，此方法应返回"myfile.html"。详细的确定规则由此PathMatcher的匹配策略指定。
	 * <p>一个简单的实现可能会在实际模式的情况下直接返回给定的完整路径，而在模式不包含任何动态部分（即参数“pattern”是一个静态路径，不符合实际的“#isPattern 模式”的条件）的情况下返回空字符串。
	 * 一个复杂的实现将区分给定路径模式的静态部分和动态部分。
	 * <p>【简单实现】可能会在真实匹配模式时按原样返回给定的完整路径，若模式不包含任何动态部分（即{@code pattern}参数为静态路径，不符合真实{@link #isPattern 模式}的条件）
	 * 则返回空字符串。而【高级实现】则会区分给定路径模式的静态部分和动态部分。
	 *
	 * @param pattern 路径模式
	 * @param path 内省的完整路径
	 * @return 给定{@code path}中，模式映射的部分 (不会为{@code null})
	 */
	String extractPathWithinPattern(String pattern, String path);

	/**
	 * 【提取URI模板变量】
	 * <p>指定一个模式和一个完整路径，提取URI模板的变量。URI模板变量通过花括号（'{'和'}'）来表示。
	 * <p>例如：模式为"/hotels/{hotel}"，路径为"/hotels/1"时，此方法将返回一个包含"hotel"->"1"的映射<p/>
	 *
	 * @param pattern 路径模式，可能包含 URI 模板
	 * @param path 用于提取模板变量的完整路径
	 * @return 一个映射，其中变量名称作为键，变量值作为值
	 */
	Map<String, String> extractUriTemplateVariables(String pattern, String path);

	/**
	 * <p>指定一个完整路径，返回一个对模式进行排序的{@link Comparator}，根据与该路径的匹程度对模式进行排序。
	 * <p>所使用的完整算法取决于具体的实现方式，但通常情况下，返回的{@code Comparator}会将列表进行{@linkplain java.util.List#sort(java.util.Comparator) 排序}，
	 * 使得更具体的模式会排在更泛化的模式之前。<p/>
	 *
	 * @param path 用于比较的完整路径
	 * @return 一种能够按照明确程度对模式进行排序的比较器
	 */
	Comparator<String> getPatternComparator(String path);

	/**
	 * <p>将两个模式合并为返回的新模式。
	 * <p>用于组合这两种模式的完整算法取决于底层实现。<p/>
	 *
	 * @param pattern1 第一种模式
	 * @param pattern2 第二种模式
	 * @return 两种模式的组合
	 * @throws IllegalArgumentException 当两种模式不能组合时
	 */
	String combine(String pattern1, String pattern2);

}
