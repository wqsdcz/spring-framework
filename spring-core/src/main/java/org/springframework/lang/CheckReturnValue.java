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

package org.springframework.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Specifies that the method return value must be used.
 * 指定方法返回值必须被使用。
 *
 * <p>Inspired by {@code org.jetbrains.annotations.CheckReturnValue}, this variant
 * has been introduced in the {@code org.springframework.lang} package to avoid
 * requiring an extra dependency, while still following similar semantics.
 * 受 {@code org.jetbrains.annotations.CheckReturnValue} 启发，此变体
 * 已在 {@code org.springframework.lang} 包中引入，以避免
 * 需要额外依赖，同时仍遵循类似的语义。
 *
 * <p>This annotation should not be used if the return value of the method
 * provides only <i>additional</i> information. For example, the main purpose
 * of {@link java.util.Collection#add(Object)} is to modify the collection
 * and the return value is only interesting when adding an element to a set,
 * to see if the set already contained that element before.
 * 如果方法的返回值仅提供<i>附加</i>信息，则不应使用此注解。例如，
 * {@link java.util.Collection#add(Object)} 的主要目的是修改集合，
 * 而返回值只有在向集合添加元素时才有意义，
 * 以查看集合之前是否已包含该元素。
 *
 * @author Sebastien Deleuze
 * @since 6.2
 */
@Documented
@Target(ElementType.METHOD)
public @interface CheckReturnValue {
}
