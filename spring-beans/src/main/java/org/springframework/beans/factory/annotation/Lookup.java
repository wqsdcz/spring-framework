/*
 * Copyright 2002-2020 the original author or authors.
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
 * 标识'查找'方法的注解，该方法将由容器重写以将其重定向回
 * {@link org.springframework.beans.factory.BeanFactory} 进行 {@code getBean} 调用。
 * 这本质上是基于注解版本的XML {@code lookup-method} 属性，产生相同的运行时安排。
 *
 * <p>目标bean的解析可以基于返回类型 ({@code getBean(Class)})
 * 或建议的bean名称 ({@code getBean(String)})，
 * 在这两种情况下都会将方法的参数传递给 {@code getBean} 调用，
 * 以将其作为目标工厂方法参数或构造函数参数应用。
 *
 * <p>此类查找方法可以具有默认（存根）实现，这些实现将被容器简单替换，
 * 或者可以声明为抽象方法 - 由容器在运行时填充。在这两种情况下，
 * 容器将通过CGLIB生成方法包含类的运行时子类，这就是为什么此类查找方法
 * 只能对容器通过常规构造函数实例化的bean起作用的原因：即查找方法无法替换
 * 从工厂方法返回的bean，因为我们无法动态为其提供子类。
 *
 * <p><b>典型Spring配置场景的建议：</b>
 * 当在某些场景中可能需要具体类时，请考虑提供查找方法的存根实现。
 * 并请记住，查找方法不适用于配置类中从 {@code @Bean} 方法返回的bean；
 * 您必须改用 {@code @Inject Provider<TargetBean>} 等方法。
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
	 * 此注解属性可以建议要查找的目标bean名称。
	 * 如果未指定，将根据注解方法的返回类型声明解析目标bean。
	 */
	String value() default "";

}
