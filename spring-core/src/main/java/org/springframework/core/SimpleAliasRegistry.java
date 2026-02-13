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

package org.springframework.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * 简单的{@link AliasRegistry} 接口实现类。
 * <p> 用作{@link org.springframework.beans.factory.support.BeanDefinitionRegistry}实现类的基类。
 *
 *
 * @author Juergen Hoeller
 * @author Qimiao Chen
 * @author Sam Brannen
 * @since 2.5.2
 */
public class SimpleAliasRegistry implements AliasRegistry {

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 从别名到规范名称的映射。 */
	private final Map<String, String> aliasMap = new ConcurrentHashMap<>(16);

	/** 别名列表，按注册顺序排列。 */
	private final List<String> aliasNames = new ArrayList<>(16);


	/**
	 * 1、如果alias == name，则注销掉旧别名映射关系（意味着这是一个注销操作）。
	 * 2、如果alias != name，且别名已经注册，且registeredName == name，则无需注册直接返回（意味着这是一个重复操作）。
	 * 3、如果alias != name，且别名已经注册，且registeredName != name，且不允许覆盖旧别名映射关系，则抛出IllegalStateException异常。
	 * 4、如果alias != name，且别名已经注册，且registeredName != name，且允许覆盖旧别名映射关系，且不存在循环引用，则注册新的别名映射关系。
	 * 5、如果alias != name，且别名已经注册，且registeredName != name，且允许覆盖旧别名映射关系，且存在循环引用，则抛出IllegalStateException异常。
	 * 5、如果alias != name，且别名未经注册，且存在循环引用，则抛出IllegalStateException异常。
	 * 6、如果alias != name，且别名未经注册，且不存在循环引用，则注册新的别名映射关系。
	 * @param name the canonical name
	 * @param alias the alias to be registered
	 */
	@Override
	public void registerAlias(String name, String alias) {
		Assert.hasText(name, "'name' must not be empty");
		Assert.hasText(alias, "'alias' must not be empty");
		synchronized (this.aliasMap) {
			if (alias.equals(name)) {
				this.aliasMap.remove(alias);
				this.aliasNames.remove(alias);
				if (logger.isDebugEnabled()) {
					logger.debug("Alias definition '" + alias + "' ignored since it points to same name");
				}
			}
			else {
				String registeredName = this.aliasMap.get(alias);
				if (registeredName != null) {
					if (registeredName.equals(name)) {
						// An existing alias - no need to re-register
						return;
					}
					if (!allowAliasOverriding()) {
						throw new IllegalStateException("Cannot define alias '" + alias + "' for name '" +
								name + "': It is already registered for name '" + registeredName + "'.");
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Overriding alias '" + alias + "' definition for registered name '" +
								registeredName + "' with new target name '" + name + "'");
					}
				}
				checkForAliasCircle(name, alias);
				this.aliasMap.put(alias, name);
				this.aliasNames.add(alias);
				if (logger.isTraceEnabled()) {
					logger.trace("Alias definition '" + alias + "' registered for name '" + name + "'");
				}
			}
		}
	}

	/**
	 * 确定是否允许覆盖旧的已注册的别名映射关系。
	 * <p>默认为{@code true}。
	 */
	protected boolean allowAliasOverriding() {
		return true;
	}

	/**
	 * Determine whether the given name has the given alias registered.
	 * @param name the name to check
	 * @param alias the alias to look for
	 * @since 4.2.1
	 */
	public boolean hasAlias(String name, String alias) {
		String registeredName = this.aliasMap.get(alias);
		return ObjectUtils.nullSafeEquals(registeredName, name) ||
				(registeredName != null && hasAlias(name, registeredName));
	}

	@Override
	public void removeAlias(String alias) {
		synchronized (this.aliasMap) {
			String name = this.aliasMap.remove(alias);
			this.aliasNames.remove(alias);
			if (name == null) {
				throw new IllegalStateException("No alias '" + alias + "' registered");
			}
		}
	}

	@Override
	public boolean isAlias(String name) {
		return this.aliasMap.containsKey(name);
	}

	@Override
	public String[] getAliases(String name) {
		List<String> result = new ArrayList<>();
		synchronized (this.aliasMap) {
			retrieveAliases(name, result);
		}
		return StringUtils.toStringArray(result);
	}

	/**
	 * 递归地检索给定name 的所有别名。
	 * 返回 result 列表包含：
	 * 1、给定name的别名。
	 * 2、给定name的别名的别名。
	 * 3、给定name的别名的别名的别名。
	 * 4、依此类推
	 * 这些都被称为别名;
	 * Transitively retrieve all aliases for the given name.
	 * @param name the target name to find aliases for
	 * @param result the resulting aliases list
	 */
	private void retrieveAliases(String name, List<String> result) {
		this.aliasMap.forEach((alias, registeredName) -> {
			if (registeredName.equals(name)) {
				result.add(alias);
				retrieveAliases(alias, result);
			}
		});
	}

	/**
	 * Resolve all alias target names and aliases registered in this
	 * registry, applying the given {@link StringValueResolver} to them.
	 * <p>The value resolver may for example resolve placeholders
	 * in target bean names and even in alias names.
	 * @param valueResolver the StringValueResolver to apply
	 */
	public void resolveAliases(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		synchronized (this.aliasMap) {
			List<String> aliasNamesCopy = new ArrayList<>(this.aliasNames);
			aliasNamesCopy.forEach(alias -> {
				String registeredName = this.aliasMap.get(alias);
				if (registeredName != null) {
					String resolvedAlias = valueResolver.resolveStringValue(alias);
					String resolvedName = valueResolver.resolveStringValue(registeredName);
					if (resolvedAlias == null || resolvedName == null || resolvedAlias.equals(resolvedName)) {
						this.aliasMap.remove(alias);
						this.aliasNames.remove(alias);
					}
					else if (!resolvedAlias.equals(alias)) {
						String existingName = this.aliasMap.get(resolvedAlias);
						if (existingName != null) {
							if (existingName.equals(resolvedName)) {
								// Pointing to existing alias - just remove placeholder
								this.aliasMap.remove(alias);
								this.aliasNames.remove(alias);
								return;
							}
							throw new IllegalStateException(
									"Cannot register resolved alias '" + resolvedAlias + "' (original: '" + alias +
									"') for name '" + resolvedName + "': It is already registered for name '" +
									existingName + "'.");
						}
						checkForAliasCircle(resolvedName, resolvedAlias);
						this.aliasMap.remove(alias);
						this.aliasNames.remove(alias);
						this.aliasMap.put(resolvedAlias, resolvedName);
						this.aliasNames.add(resolvedAlias);
					}
					else if (!registeredName.equals(resolvedName)) {
						this.aliasMap.put(alias, resolvedName);
						this.aliasNames.add(alias);
					}
				}
			});
		}
	}

	/**
	 * Check whether the given name points back to the given alias as an alias
	 * in the other direction already, catching a circular reference upfront
	 * and throwing a corresponding IllegalStateException.
	 * @param name the candidate name
	 * @param alias the candidate alias
	 * @see #registerAlias
	 * @see #hasAlias
	 */
	protected void checkForAliasCircle(String name, String alias) {
		if (hasAlias(alias, name)) {
			throw new IllegalStateException("Cannot register alias '" + alias +
					"' for name '" + name + "': Circular reference - '" +
					name + "' is a direct or indirect alias for '" + alias + "' already");
		}
	}

	/**
	 * Determine the raw name, resolving aliases to canonical names.
	 * @param name the user-specified name
	 * @return the transformed name
	 */
	public String canonicalName(String name) {
		String canonicalName = name;
		// Handle aliasing...
		String resolvedName;
		do {
			resolvedName = this.aliasMap.get(canonicalName);
			if (resolvedName != null) {
				canonicalName = resolvedName;
			}
		}
		while (resolvedName != null);
		return canonicalName;
	}

}
