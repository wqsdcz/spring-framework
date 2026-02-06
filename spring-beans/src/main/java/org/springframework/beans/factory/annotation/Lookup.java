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
 * 一个指示"查找"方法的注解，这些方法将被容器重写以将它们重定向回
 * {@link org.springframework.beans.factory.BeanFactory} 进行 {@code getBean} 调用。
 * 这本质上是基于注解的 XML {@code lookup-method} 属性版本，产生相同的运行时安排。
 *
 * <p>目标 bean 的解析可以基于返回类型 ({@code getBean(Class)}) 或建议的 bean 名称
 * ({@code getBean(String)})，在这两种情况下都会将方法的参数传递给 {@code getBean} 调用，
 * 以将它们应用为目标工厂方法参数或构造函数参数。
 *
 * <p>这样的查找方法可以有默认的（存根）实现，这些实现将简单地被容器替换，
 * 或者它们可以声明为抽象的 - 让容器在运行时填充它们。在这两种情况下，
 * 容器都将通过 CGLIB 生成包含该方法的类的运行时子类，这就是为什么这样的
 * 查找方法只能在容器通过常规构造函数实例化的 bean 上工作的原因：
 * 即查找方法无法在从工厂方法返回的 bean 上被替换，因为我们无法动态地为它们提供子类。
 *
 * <p><b>典型 Spring 配置场景的建议：</b>
 * 当在某些场景中可能需要具体类时，请考虑为您的查找方法提供存根实现。
 * 并且请记住，查找方法在从配置类中的 {@code @Bean} 方法返回的 bean 上不会起作用；
 * 您必须改用 {@code @Inject Provider<TargetBean>} 或类似的方式。
 *
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.beans.factory.BeanFactory#getBean(Class, Object...)
 * @see org.springframework.beans.factory.BeanFactory#getBean(String, Object...)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lookup {

	/**
	 * 此注解属性可以建议要查找的目标 bean 名称。
	 * 如果未指定，则将根据注解方法的返回类型声明来解析目标 bean。
	 */
	String value() default "";

}
