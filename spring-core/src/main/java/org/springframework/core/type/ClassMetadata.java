/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * 该接口定义了指定类型的抽象元数据，在这种形式下，不需要加载该类。
 *
 * Interface that defines abstract metadata of a specific class,
 * in a form that does not require that class to be loaded yet.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see StandardClassMetadata
 * @see org.springframework.core.type.classreading.MetadataReader#getClassMetadata()
 * @see AnnotationMetadata
 */
public interface ClassMetadata {

	/**
	 * 返回底层类的名称
	 * Return the name of the underlying class.
	 */
	String getClassName();

	/**
	 * 判断底层类是否是接口
	 * Return whether the underlying class represents an interface.
	 */
	boolean isInterface();

	/**
	 * 判断底层类是否是注解
	 * Return whether the underlying class represents an annotation.
	 * @since 4.1
	 */
	boolean isAnnotation();

	/**
	 * 判断底层类是否使抽象的
	 * Return whether the underlying class is marked as abstract.
	 */
	boolean isAbstract();

	/**
	 * 判断底层类是否使具体的类
	 * Return whether the underlying class represents a concrete class,
	 * i.e. neither an interface nor an abstract class.
	 */
	boolean isConcrete();

	/**
	 * 判断底层类是否使标记有'final'
	 * Return whether the underlying class is marked as 'final'.
	 */
	boolean isFinal();

	/**
	 * 判断底层类是否是独立的，即它是否为顶级类或嵌套类（静态内部类），并且是否能够独立于外部类进行构造。
	 * Determine whether the underlying class is independent, i.e. whether
	 * it is a top-level class or a nested class (static inner class) that
	 * can be constructed independently from an enclosing class.
	 */
	boolean isIndependent();

	/**
	 * 判断底层类是否是在外部类中声明的（即该基础类是内部/嵌套类，或者是某个方法内的局部类）。
	 * <p>如果此方法返回值为“false”，则表示该基础类为顶级类。
	 *
	 * Return whether the underlying class is declared within an enclosing
	 * class (i.e. the underlying class is an inner/nested class or a
	 * local class within a method).
	 * <p>If this method returns {@code false}, then the underlying
	 * class is a top-level class.
	 */
	boolean hasEnclosingClass();

	/**
	 * 返回底层类的外部类的名称，如果底层类是顶级类，则返回值为 {@code null} 。
	 * Return the name of the enclosing class of the underlying class,
	 * or {@code null} if the underlying class is a top-level class.
	 */
	@Nullable
	String getEnclosingClassName();

	/**
	 * 判断底层类是否有父类
	 * Return whether the underlying class has a super class.
	 */
	boolean hasSuperClass();

	/**
	 * 返回底层类的父类的名称，如果没有定义父类，则返回 {@code null}。
	 * Return the name of the super class of the underlying class,
	 * or {@code null} if there is no super class defined.
	 */
	@Nullable
	String getSuperClassName();

	/**
	 * 返回底层类实现的所有接口的名称，如果没有，则返回空数组。
	 * Return the names of all interfaces that the underlying class
	 * implements, or an empty array if there are none.
	 */
	String[] getInterfaceNames();

	/**
	 * 返回由此 ClassMetadata 对象所代表的类所声明的所有成员类的名称。
	 * 这包括该类声明的公共、受保护、默认（包）访问级别以及私有的类和接口，但不包括继承的类和接口。
	 * 如果不存在任何成员类或接口，则返回一个空数组。
	 * @since 3.1
	 * Return the names of all classes declared as members of the class represented by
	 * this ClassMetadata object. This includes public, protected, default (package)
	 * access, and private classes and interfaces declared by the class, but excludes
	 * inherited classes and interfaces. An empty array is returned if no member classes
	 * or interfaces exist.
	 * @since 3.1
	 */
	String[] getMemberClassNames();

}
