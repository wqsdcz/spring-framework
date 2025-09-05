/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.aop;

/**
 * <p>用于标记AOP代理接口（特别是引入接口）的特殊接口，该接口明确指示需要返回原始目标对象（通常方法调用返回时会被代理对象替换）。</p>
 *
 * <p>
 *     请注意这是一个遵循{@link java.io.Serializable}风格的标记接口，其语义作用于声明的接口而非具体对象的完整类。
 *     换言之，该标记仅适用于特定接口（通常是不作为AOP代理主接口的引入接口），因此不会影响具体AOP代理可能实现的其他接口。
 * </p>
 *
 * @author Juergen Hoeller
 * @since 2.0.5
 * @see org.springframework.aop.scope.ScopedObject
 */
public interface RawTargetAccess {

}
