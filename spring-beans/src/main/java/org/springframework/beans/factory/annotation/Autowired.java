/*
 * Copyright 2002-2019 the original author or authors.
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
 * 将【构造函数】、【字段】、【setter方法】或【配置方法】标记为通过【Spring的依赖注入设施】进行自动装配。
 * 这是JSR-330 {@link javax.inject.Inject}注解的替代方案，增加了必需与可选的语义。
 *
 * <p>
 *     任何给定bean类中只能有一个构造函数可以将此注解的'required'属性设置为{@code true}，表示作为Spring bean使用时<em>要自动装配的</em>构造函数。
 *     此外，如果'required'属性设置为{@code true}，则只能有一个构造函数可以用{@code @Autowired}注解。
 *     如果多个<em>非必需</em>构造函数声明了该注解，它们将被视为自动装配的候选者。
 *     将选择能够通过Spring容器中的匹配bean满足最多依赖项的构造函数。
 *     如果没有候选者能够被满足，则将使用主/默认构造函数（如果存在）。
 *     如果一个类最初只声明一个构造函数，则即使没有注解，也始终会使用它。带注解的构造函数不必是public的。
 *
 * <p>
 *     【字段注入】发生在bean构造之后、调用所有配置方法之前。
 *     这样的配置字段不必是public的。
 *
 * <p>
 *     配置方法可以具有任意名称和任意数量的参数；每个参数都将使用Spring容器中的匹配bean进行自动装配。
 *     Bean属性setter方法实际上只是这种通用配置方法的特例。此类配置方法不必是public的。
 *
 * <p>
 *     对于多参数构造函数或方法，'required'属性适用于所有参数。
 *     可以将各个参数声明为Java 8风格的{@link java.util.Optional}，
 *     或者从Spring Framework 5.0开始，也可以声明为{@code @Nullable}或Kotlin中的非空参数类型，从而覆盖基本的必需语义。
 *
 * <p>
 *     对于{@link java.util.Collection}或{@link java.util.Map}依赖类型，容器会自动装配所有与声明的值类型匹配的bean。
 *     为此，map键必须声明为String类型，这将解析为相应的bean名称。
 *     此类容器提供的集合将是有序的，会考虑目标组件的{@link org.springframework.core.Ordered}/{@link org.springframework.core.annotation.Order}值，否则遵循它们在容器中的注册顺序。
 *     或者，单个匹配的目标bean本身也可以是泛型类型的 {@code Collection}或{@code Map}，按原样注入。
 *
 * <p>
 *     请注意，实际注入是通过{@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor}执行的，
 *     这反过来意味着您<em>不能</em>使用{@code @Autowired}将引用注入到
 *     {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor}或
 *     {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor}类型中。
 *     请查阅{@link AutowiredAnnotationBeanPostProcessor}类的javadoc（默认情况下，该类检查此注解的存在）。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
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
	 * 声明被注解的依赖是否是必需的。
	 * <p>默认为 {@code true}。
	 */
	boolean required() default true;

}
