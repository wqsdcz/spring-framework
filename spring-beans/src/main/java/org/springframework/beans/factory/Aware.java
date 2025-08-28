/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.beans.factory;

/**
 * <p>
 *     这是一个标记性接口，用于表示一个Bean可以感知到特定的框架对象（由Spring容器通过具体的{@link Aware}子接口的回调方法传入）。
 *     具体的回调方法由各个{@link Aware}子接口自己定义。这个回调方法通常只有一个入参，且返回类型为void。
 * </p>
 * <p>
 *     需要注意的是，仅仅实现{@link Aware}子接口，并不能获得上述的功能。需要明确提供{@link Aware}子接口处理方式，
 *     例如：提供{@link org.springframework.beans.factory.config.BeanPostProcessor}实现类来负责处理。
 *     有关处理特定{@code *Aware}接口回调的示例，请参考{@link org.springframework.context.support.ApplicationContextAwareProcessor}。
 * </p>
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public interface Aware {

}
