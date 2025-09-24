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
 * 字段或方法/构造函数参数级别的注解，
 * 用于为受影响的参数指示默认值表达式。
 *
 * <p>通常用于表达式驱动的依赖注入。也支持处理器方法参数的动态解析，
 * 例如在Spring MVC中。
 *
 * <p>常见用例是使用{@code #{systemProperties.myProp}}样式表达式分配默认字段值。
 *
 * <p>请注意，{@code @Value}注解的实际处理由
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor}
 * 执行，这意味着您<em>不能</em>在
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor}或
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor}
 * 类型中使用{@code @Value}。请查阅{@link AutowiredAnnotationBeanPostProcessor}
 * 类的javadoc（默认情况下，该类会检查此注解的存在）。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see AutowiredAnnotationBeanPostProcessor
 * @see Autowired
 * @see org.springframework.beans.factory.config.BeanExpressionResolver
 * @see org.springframework.beans.factory.support.AutowireCandidateResolver#getSuggestedValue
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {

	/**
	 * 实际的值表达式：例如{@code #{systemProperties.myProp}}。
	 */
	String value();

}
