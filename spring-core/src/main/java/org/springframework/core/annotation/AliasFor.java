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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @AliasFor} 是一个用于声明注解属性别名的注解。
 * <p>
 * <h3>使用场景</h3>
 * <ul>
 *		<li>
 *		    <strong>注解内 的 显式别名</strong>：
 *		    在同一个注解内部，两个属性上都可以使用{@code @AliasFor}互相指向，表示这两个属性是彼此的别名，
 *		    即设置其中一个，另一个也会被设置成相同的值。
 * 		</li>
 * 		<li>
 * 		    <strong>元注解中属性 的 显式别名</strong>：
 * 		    当 {@code @AliasFor} 的 {@link #annotation} 属性指定为另一个注解（元注解）时，
 * 		    那么当前注解的属性就是元注解中指定属性的别名。
 * 		    这实际上是一种显式的元注解属性覆盖。通过这种方式，可以精确控制覆盖元注解的哪个属性。
 * 		    例如，可以使用 {@code @AliasFor} 为元注解的{@code value}属性声明一个别名。
 * 		</li>
 * 		<li>
 * 		    <strong>注解内 的 隐式别名</strong>：
 * 		    如果一个注解中的多个属性都声明为覆盖同一个元注解属性（可以是直接覆盖，也可以是传递性的覆盖），那么这些属性就形成了一组隐式别名。
 * 		    它们的行为类似于注解内的显式别名，即这些属性中任意一个被设置，其他属性也会被设置成相同的值。
 * 		</li>
 * </ul>
 *
 * <p>
 * <h3>使用要求</h3>
 * <p>与Java中的任何注解一样，仅存在 {@code @AliasFor} 本身并不会强制执行别名语义。
 * 要强制执行别名语义，必须通过 {@link MergedAnnotations} <em>加载</em> 注解。
 *
 * <p>
 * <h3>实现要求</h3>
 * <ul>
 * 		<li>
 * 		    <strong>注解内的显式别名</strong>：
 * 			<ol>
 * 				<li>
 * 				    组成别名对的两个属性都应使用 {@code @AliasFor} 进行注解，
 * 				    并且 {@link #attribute} 或 {@link #value} 必须引用别名对中的 <em>另一个属性</em>。
 * 				    自 Spring Framework 5.2.1 起，技术上可以仅为别名对中的某一个属性添加注解；
 * 				    然而，建议为别名对中的两个属性都添加注解，以获得更好的文档说明以及与早期版本 Spring Framework 的兼容性。
 * 				</li>
 * 				<li>别名属性必须声明相同的返回类型。</li>
 * 				<li>别名属性必须声明默认值。</li>
 * 				<li>别名属性必须声明相同的默认值。</li>
 *	 			<li>不应声明 {@link #annotation}。</li>
 * 			</ol>
 * 		</li>
 * 		<li>
 * 		    <strong>元注解中属性的显式别名</strong>：
 * 			<ol>
 * 				<li>
 * 				    作为元注解中属性别名的属性必须使用 {@code @AliasFor} 进行注解，并且 {@link #attribute} 必须引用元注解中的属性。
 * 				</li>
 * 				<li>别名属性必须声明相同的返回类型。</li>
 * 				<li>{@link #annotation} 必须引用元注解。</li>
 * 				<li>引用的元注解必须在声明 {@code @AliasFor} 的注解类上 <em>元存在</em>。</li>
 * 			</ol>
 * 		</li>
 * 		<li>
 * 		    <strong>注解内的隐式别名</strong>：
 * 			<ol>
 * 				<li>
 * 				    属于一组隐式别名的每个属性都必须使用 {@code @AliasFor} 进行注解，
 * 				    并且 {@link #attribute} 必须引用同一元注解中的相同属性（直接或通过注解层次结构中的其他显式元注解属性覆盖传递地）。
 * 				</li>
 * 				<li>别名属性必须声明相同的返回类型。</li>
 * 				<li>别名属性必须声明默认值。</li>
 * 				<li>别名属性必须声明相同的默认值。</li>
 * 				<li>{@link #annotation} 必须引用适当的元注解。</li>
 * 				<li>引用的元注解必须在声明 {@code @AliasFor} 的注解类上 <em>元存在</em>。</li>
 * 			</ol>
 * 		</li>
 * </ul>
 *
 * <h3>示例：注解内的显式别名</h3>
 * <p>在 {@code @ContextConfiguration} 中，{@code value} 和 {@code locations} 彼此是显式别名。
 *
 * <pre class="code">
 * public &#064;interface ContextConfiguration {
 *
 *    &#064;AliasFor("locations")
 *    String[] value() default {};
 *
 *    &#064;AliasFor("value")
 *    String[] locations() default {};
 *
 *    // ...
 * }
 * </pre>
 *
 * <h3>示例：元注解中属性的显式别名</h3>
 * <p>在 {@code @XmlTestConfig} 中，{@code xmlFiles} 是 {@code @ContextConfiguration} 中
 * {@code locations} 的显式别名。换句话说，{@code xmlFiles} 覆盖了 {@code @ContextConfiguration}
 * 中的 {@code locations} 属性。
 *
 * <pre class="code">
 * &#064;ContextConfiguration
 * public &#064;interface XmlTestConfig {
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] xmlFiles();
 * }
 * </pre>
 *
 * <h3>示例：注解内的隐式别名</h3>
 * <p>在 {@code @MyTestConfig} 中，{@code value}、{@code groovyScripts} 和 {@code xmlFiles}
 * 都是 {@code @ContextConfiguration} 中 {@code locations} 属性的显式元注解属性覆盖。
 * 因此，这三个属性也是彼此的隐式别名。
 *
 * <pre class="code">
 * &#064;ContextConfiguration
 * public &#064;interface MyTestConfig {
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] value() default {};
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] groovyScripts() default {};
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] xmlFiles() default {};
 * }
 * </pre>
 *
 * <h3>示例：注解内的传递隐式别名</h3>
 * <p>在 {@code @GroovyOrXmlTestConfig} 中，{@code groovy} 是 {@code @MyTestConfig} 中
 * {@code groovyScripts} 属性的显式覆盖；而 {@code xml} 是 {@code @ContextConfiguration} 中
 * {@code locations} 属性的显式覆盖。此外，{@code groovy} 和 {@code xml} 是彼此的传递隐式别名，
 * 因为它们都有效地覆盖了 {@code @ContextConfiguration} 中的 {@code locations} 属性。
 *
 * <pre class="code">
 * &#064;MyTestConfig
 * public &#064;interface GroovyOrXmlTestConfig {
 *
 *    &#064;AliasFor(annotation = MyTestConfig.class, attribute = "groovyScripts")
 *    String[] groovy() default {};
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] xml() default {};
 * }
 * </pre>
 *
 * <h3>支持属性别名的 Spring 注解</h3>
 * <p>自 Spring Framework 4.2 起，核心 Spring 中的几个注解已更新为使用 {@code @AliasFor}
 * 来配置其内部属性别名。有关详细信息，请参阅各个注解的 Javadoc 以及参考手册。
 *
 * @author Sam Brannen
 * @since 4.2
 * @see MergedAnnotations
 * @see AnnotationUtils#isSynthesizedAnnotation(Annotation)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface AliasFor {

	/**
	 * {@link #attribute} 的别名。
	 * 当未声明 {@link #annotation} 时，此属性可以替代 {@link #attribute} 使用——例如：@AliasFor("value") 代替 @AliasFor(attribute = "value") 。
	 */
	@AliasFor("attribute")
	String value() default "";

	/**
	 * 此属性所对应的别名属性的名称。
	 * @see #value
	 */
	@AliasFor("value")
	String attribute() default "";

	/**
	 * 声明别名属性 {@link #attribute} 的注解类型。
	 * <p>默认为 {@link Annotation}，表示别名属性是在与此属性相同的注解中声明的。
	 */
	Class<? extends Annotation> annotation() default Annotation.class;

}
