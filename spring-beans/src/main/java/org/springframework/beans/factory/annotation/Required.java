/*
 * Copyright 2002-2012 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 由 {@link RequiredAnnotationBeanPostProcessor} 负责识别
 *
 * 将某个方法（通常是JavaBean的setter方法）标记为"必需"：这意味着必须对该setter方法配置依赖注入值。
 *
 * <p>请务必参考{@link RequiredAnnotationBeanPostProcessor}类的javadoc文档（默认情况下，该校验器会检查是否存在此注解）。
 *
 * Marks a method (typically a JavaBean setter method) as being 'required': that is,
 * the setter method must be configured to be dependency-injected with a value.
 *
 * <p>Please do consult the javadoc for the {@link RequiredAnnotationBeanPostProcessor}
 * class (which, by default, checks for the presence of this annotation).
 *
 * @author Rob Harrop
 * @since 2.0
 * @see RequiredAnnotationBeanPostProcessor
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Required {

}
