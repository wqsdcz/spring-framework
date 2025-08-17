/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.Ordered;

/**
 * <p>{@code @Order}定义了被注释组件的排序顺序。<p/>
 *
 * <p>{@link #value} 属性为可选参数，表示遵循 {@link Ordered} 接口定义的顺序值。
 * 数值越小优先级越高，默认值为 {@code Ordered.LOWEST_PRECEDENCE}（表示最低优先级，低于任何显式指定的顺序值）。<p/>
 *
 * <p><b>注意：</b>
 * 自 Spring 4.0 起，基于注解的排序已支持 Spring 中的多种组件，甚至在集合注入场景中也会考虑目标组件的顺序值（可从目标类或其 {@code @Bean} 方法获取）。
 * 尽管这些顺序值会影响注入点的优先级，但请注意它们不影响单例启动顺序——该顺序是由依赖关系和 {@code @DependsOn} 声明确定的正交关注点（影响运行时确定的依赖关系图）。<p/>
 *
 * <p>自 Spring 4.1 起，标准注解 {@link javax.annotation.Priority} 可在排序场景中直接替代本注解。
 * 需注意当需要选取单个元素时，{@code @Priority} 可能具有额外语义（参见 {@link AnnotationAwareOrderComparator#getPriority}）。<p/>
 *
 * <p>此外，也可通过 {@link Ordered} 接口按实例确定顺序值，从而允许使用配置决定的实例值替代硬编码到特定类的值。<p/>
 *
 * <p>关于无序对象的排序语义细节，请参阅 {@link org.springframework.core.OrderComparator OrderComparator} 的 Javadoc。<p/>
 *
 * {@code @Order} defines the sort order for an annotated component.
 *
 * <p>The {@link #value} is optional and represents an order value as defined in the
 * {@link Ordered} interface. Lower values have higher priority. The default value is
 * {@code Ordered.LOWEST_PRECEDENCE}, indicating lowest priority (losing to any other
 * specified order value).
 *
 * <p><b>NOTE:</b> Since Spring 4.0, annotation-based ordering is supported for many
 * kinds of components in Spring, even for collection injection where the order values
 * of the target components are taken into account (either from their target class or
 * from their {@code @Bean} method). While such order values may influence priorities
 * at injection points, please be aware that they do not influence singleton startup
 * order which is an orthogonal concern determined by dependency relationships and
 * {@code @DependsOn} declarations (influencing a runtime-determined dependency graph).
 *
 * <p>Since Spring 4.1, the standard {@link javax.annotation.Priority} annotation
 * can be used as a drop-in replacement for this annotation in ordering scenarios.
 * Note that {@code @Priority} may have additional semantics when a single element
 * has to be picked (see {@link AnnotationAwareOrderComparator#getPriority}).
 *
 * <p>Alternatively, order values may also be determined on a per-instance basis
 * through the {@link Ordered} interface, allowing for configuration-determined
 * instance values instead of hard-coded values attached to a particular class.
 *
 * <p>Consult the javadoc for {@link org.springframework.core.OrderComparator
 * OrderComparator} for details on the sort semantics for non-ordered objects.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.core.Ordered
 * @see AnnotationAwareOrderComparator
 * @see OrderUtils
 * @see javax.annotation.Priority
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface Order {

	/**
	 * The order value.
	 * <p>Default is {@link Ordered#LOWEST_PRECEDENCE}.
	 * @see Ordered#getOrder()
	 */
	int value() default Ordered.LOWEST_PRECEDENCE;

}
