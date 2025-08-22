/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.core.type;

/**
 * 该接口定义对指定类的注释的抽象访问，在这种形式下，不需要加载该类。
 * Interface that defines abstract access to the annotations of a specific
 * class, in a form that does not require that class to be loaded yet.
 *
 * @author Juergen Hoeller
 * @author Mark Pollack
 * @author Chris Beams
 * @author Phillip Webb
 * @since 3.0
 * @see StandardMethodMetadata
 * @see AnnotationMetadata#getAnnotatedMethods
 * @see AnnotatedTypeMetadata
 */
public interface MethodMetadata extends AnnotatedTypeMetadata {

	/**
	 * 返回方法的名称
	 * Return the name of the method.
	 */
	String getMethodName();

	/**
	 * 返回声明该方法的类的全限定名
	 * Return the fully-qualified name of the class that declares this method.
	 */
	String getDeclaringClassName();

	/**
	 * 返回该方法声明的返回类型的全限定名
	 * Return the fully-qualified name of this method's declared return type.
	 * @since 4.2
	 */
	String getReturnTypeName();

	/**
	 * 判断底层的方式是都是抽象的：例如，在类中标记为抽象的方法，或者在接口中声明为常规的非默认方法
	 *
	 * Return whether the underlying method is effectively abstract:
	 * i.e. marked as abstract on a class or declared as a regular,
	 * non-default method in an interface.
	 * @since 4.2
	 */
	boolean isAbstract();

	/**
	 * 判断底层方法是否声明为 'static'
	 * Return whether the underlying method is declared as 'static'.
	 */
	boolean isStatic();

	/**
	 * 判断底层方法是否标记为 'final'
	 * Return whether the underlying method is marked as 'final'.
	 */
	boolean isFinal();

	/**
	 * 判断底层方法是否是可覆盖的：例如，未标记为 'static', 'final' or 'private'。
	 * Return whether the underlying method is overridable,
	 * i.e. not marked as static, final or private.
	 */
	boolean isOverridable();

}
