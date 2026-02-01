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

package org.springframework.core.env;

import java.util.function.Predicate;

/**
 * Profile 断言（predicate），可以被 {@link Environment} 的
 * {@linkplain Environment#acceptsProfiles(Profiles) acceptsProfiles} 方法接受。
 *
 * <p>可以直接实现此接口，但通常更推荐使用 {@link #of(String...) of(...)} 工厂方法来创建。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.1
 * @see Environment#acceptsProfiles(Profiles)
 * @see Environment#matchesProfiles(String...)
 */
@FunctionalInterface
public interface Profiles {

	/**
	 * 测试此 {@code Profiles} 实例是否与给定的断言<em>匹配</em>。
	 * @param isProfileActive 用于测试给定 profile 当前是否处于激活状态的断言
	 */
	boolean matches(Predicate<String> isProfileActive);


	/**
	 * 创建一个新的 {@link Profiles} 实例，用于检查是否与给定的 <em>profile 表达式</em> 匹配。
	 * <p>如果给定的任一 profile 表达式匹配，则返回的实例将
	 * {@linkplain Profiles#matches(Predicate) 匹配成功}。
	 * <p>profile 表达式可以包含简单的 profile 名称（例如 {@code "production"}），
	 * 也可以是复合表达式。复合表达式允许表达更复杂的 profile 逻辑，例如
	 * {@code "production & cloud"}。
	 * <p>profile 表达式支持以下运算符：
	 * <ul>
	 * <li>{@code !} - 对 profile 名称或复合表达式进行逻辑<em>非</em>运算</li>
	 * <li>{@code &} - 对 profile 名称或复合表达式进行逻辑<em>与</em>运算</li>
	 * <li>{@code |} - 对 profile 名称或复合表达式进行逻辑<em>或</em>运算</li>
	 * </ul>
	 * <p>请注意，如果不使用括号，{@code &} 和 {@code |} 运算符不能混合使用。
	 * 例如，{@code "a & b | c"} 不是有效的表达式：必须写成 {@code "(a & b) | c"} 或
	 * {@code "a & (b | c)"}。
	 * <p>如果两个通过此方法返回的 {@code Profiles} 实例是使用相同的 <em>profile 表达式</em>
	 * 创建的，则它们在 {@code equals()} 和 {@code hashCode()} 语义上被认为是等价的。
	 * @param profileExpressions 要包含的 <em>profile 表达式</em>
	 * @return 一个新的 {@link Profiles} 实例
	 */
	static Profiles of(String... profileExpressions) {
		return ProfilesParser.parse(profileExpressions);
	}

}
