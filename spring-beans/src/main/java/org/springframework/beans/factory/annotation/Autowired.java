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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记构造函数、字段、setter方法或配置方法，表示应由Spring的依赖注入工具进行自动装配。
 * 这是JSR-330 {@link jakarta.inject.Inject} 注解的替代方案，增加了必需与可选的语义。
 *
 * <h3>自动装配构造函数</h3>
 * <p>任何给定bean类只有一个构造函数可以声明此注解并将 {@link #required} 属性设置为 {@code true}，
 * 表示当作为Spring bean使用时要自动装配的<i>那个</i>构造函数。此外，如果 {@code required}
 * 属性设置为 {@code true}，则只有一个构造函数可以使用 {@code @Autowired} 进行注解。
 * 如果多个<i>非必需</i>构造函数声明了该注解，它们将被视为自动装配的候选者。
 * 将选择能够通过匹配Spring容器中的bean来满足最多依赖项的构造函数。
 * 如果没有候选者能够满足，则将使用主构造函数/默认构造函数（如果存在）。
 * 同样，如果一个类声明了多个构造函数但没有一个使用 {@code @Autowired} 进行注解，
 * 则将使用主构造函数/默认构造函数（如果存在）。如果一个类只声明了一个构造函数，
 * 它将始终被使用，即使没有注解。带注解的构造函数不必是公共的。
 *
 * <h3>自动装配字段</h3>
 * <p>字段在bean构造之后、调用任何配置方法之前就被注入。这样的配置字段不必是公共的。
 *
 * <h3>自动装配方法</h3>
 * <p>配置方法可以有任意名称和任意数量的参数；每个参数都将与Spring容器中的匹配bean进行自动装配。
 * Bean属性setter方法实际上只是这种通用配置方法的一个特例。这样的配置方法不必是公共的。
 *
 * <h3>自动装配参数</h3>
 * <p>虽然从技术上讲 {@code @Autowired} 可以声明在单独的方法或构造函数参数上，
 * 但框架的大多数部分都忽略此类声明。核心Spring框架中唯一积极支持自动装配参数的部分
 * 是 {@code spring-test} 模块中的JUnit Jupiter支持（详情请参见
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#testcontext-junit-jupiter-di">TestContext框架</a>
 * 参考文档）。
 *
 * <h3>多参数和"必需"语义</h3>
 * <p>对于多参数构造函数或方法，{@link #required} 属性适用于所有参数。
 * 单个参数可以声明为Java 8风格的 {@link java.util.Optional} 以及 {@code @Nullable}
 * 或Kotlin中的非空参数类型，覆盖基本的"必需"语义。
 *
 * <h3>自动装配数组、集合和映射</h3>
 * <p>对于数组、{@link java.util.Collection} 或 {@link java.util.Map} 依赖类型，
 * 容器会自动装配所有匹配声明值类型的bean。为此目的，映射键必须声明为 {@code String} 类型，
 * 这将解析为相应的bean名称。容器提供的此类集合将是有序的，考虑到目标组件的
 * {@link org.springframework.core.Ordered Ordered} 和
 * {@link org.springframework.core.annotation.Order @Order} 值，
 * 否则遵循它们在容器中的注册顺序。或者，单个匹配的目标bean本身也可以是一般类型的
 * {@code Collection} 或 {@code Map}，并以此方式注入。
 *
 * <h3>不支持在 {@code BeanPostProcessor} 或 {@code BeanFactoryPostProcessor} 中使用</h3>
 * <p>请注意，实际注入是通过
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor}
 * 执行的，这意味着您<em>不能</em>使用 {@code @Autowired} 将引用注入到
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor} 或
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor}
 * 类型中。请查阅 {@link AutowiredAnnotationBeanPostProcessor} 类的javadoc
 * （默认情况下检查此注解的存在）。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Sam Brannen
 * @since 2.5
 * @see AutowiredAnnotationBeanPostProcessor
 * @see Qualifier
 * @see Value
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {

	/**
	 * 声明注解的依赖项是否必需。
	 * <p>默认为 {@code true}。
	 */
	boolean required() default true;

}
