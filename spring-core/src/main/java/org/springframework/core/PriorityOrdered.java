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


/**
 * Extension of the {@link Ordered} interface, expressing a <em>priority</em>
 * ordering: {@code PriorityOrdered} objects are always applied before
 * <em>plain</em> {@link Ordered} objects regardless of their order values.
 *
 * <p>当对一组 {@code Ordered} 对象进行排序时，{@code PriorityOrdered}
 * 对象总是应用于<em>普通</em> {@link Ordered} 对象之前，无论它们的顺序值如何。
 *
 * <p>When sorting a set of {@code Ordered} objects, {@code PriorityOrdered}
 * objects and <em>plain</em> {@code Ordered} objects are effectively treated as
 * two separate subsets, with the set of {@code PriorityOrdered} objects preceding
 * the set of <em>plain</em> {@code Ordered} objects and with relative
 * ordering applied within those subsets.
 *
 * <p>当对一组 {@code Ordered} 对象进行排序时，{@code PriorityOrdered}
 * 对象和<em>普通</em> {@code Ordered} 对象实际上被视为两个独立的子集，
 * 其中 {@code PriorityOrdered} 对象集合位于<em>普通</em> {@code Ordered}
 * 对象集合之前，并且在这些子集中应用相对排序。
 *
 * <p>This is primarily a special-purpose interface, used within the framework
 * itself for objects where it is particularly important to recognize
 * <em>prioritized</em> objects first, potentially without even obtaining the
 * remaining objects. A typical example: prioritized post-processors in a Spring
 * {@link org.springframework.context.ApplicationContext}.
 *
 * <p>这主要是特殊用途的接口，在框架内部用于那些特别重要、需要首先识别
 * <em>优先</em>对象的场景，甚至可能无需获取其余对象。一个典型例子：
 * Spring {@link org.springframework.context.ApplicationContext} 中的优先处理器。
 *
 * <p>Note: {@code PriorityOrdered} post-processor beans are initialized in
 * a special phase, ahead of other post-processor beans. This subtly
 * affects their autowiring behavior: they will only be autowired against
 * beans which do not require eager initialization for type matching.
 *
 * <p>注意：{@code PriorityOrdered} 后置处理器 bean 在一个特殊的阶段初始化，
 * 在其他后置处理器 bean 之前。这会微妙地影响它们的自动装配行为：
 * 它们只会与那些不需要急切初始化来进行类型匹配的 bean 进行自动装配。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 * @see org.springframework.beans.factory.config.PropertyOverrideConfigurer
 */
public interface PriorityOrdered extends Ordered {
}
