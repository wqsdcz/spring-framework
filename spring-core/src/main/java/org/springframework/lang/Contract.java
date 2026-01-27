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

package org.springframework.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * <p>指定方法行为的某些方面，取决于参数。
 * 可以被工具用于高级数据流分析。请注意，此注解
 * 只是描述代码的工作方式，并不通过代码生成添加任何功能。
 *
 * <p>受 {@code org.jetbrains.annotations.Contract} 启发，此变体已在
 * {@code org.springframework.lang} 包中引入，以避免
 * 需要额外依赖项，同时仍遵循相同的语义。
 *
 * <p>方法契约具有以下语法:
 * <pre>{@code
 *  contract ::= (clause ';')* clause
 *  clause ::= args '->' effect
 *  args ::= ((arg ',')* arg )?
 *  arg ::= value-constraint
 *  value-constraint ::= '_' | 'null' | '!null' | 'false' | 'true'
 *  effect ::= value-constraint | 'fail' | 'this' | 'new' | 'param<N>'}</pre>
 *
 * <p>约束表示以下内容:
 * <ul>
 * <li>{@code _} - 任何值
 * <li>{@code null} - null值
 * <li>{@code !null} - 静态证明为非空的值
 * <li>{@code true} - true布尔值
 * <li>{@code false} - false布尔值
 * </ul>
 *
 * <p>附加的返回值表示以下内容:
 * <ul>
 * <li>{@code fail} - 如果参数满足参数约束，方法抛出异常
 * <li>{@code new} - 方法返回一个非空的新对象，该对象与方法执行前堆中存在的任何其他对象都不同。
 * 如果方法没有可见的副作用，那么我们可以确保新对象不会存储到任何字段/数组中，如果方法的返回值未被使用，则该新对象将丢失。
 * <li>{@code this} - 方法返回其限定符值(不适用于静态方法)
 * <li>{@code param1, param2, ...} - 方法返回其第一(第二，...)参数值
 * </ul>
 *
 * <p>示例:
 * <ul>
 * <li>{@code @Contract("_, null -> null")} - 如果第二个参数为null，方法返回null。
 * <li>{@code @Contract("_, null -> null; _, !null -> !null")} - 如果第二个参数为null，方法返回null，否则返回非空值。
 * <li>{@code @Contract("true -> fail")} - 一个典型的 {@code assertFalse} 方法，如果传入 {@code true} 则抛出异常。
 * <li>{@code @Contract("_ -> this")} - 方法总是返回其限定符(例如 {@link StringBuilder#append(String)})。
 * <li>{@code @Contract("null -> fail; _ -> param1")} - 如果第一个参数为null，方法抛出异常，
 * 否则返回第一个参数(例如 {@code Objects.requireNonNull})。
 * <li>{@code @Contract("!null, _ -> param1; null, !null -> param2; null, null -> fail")} - 方法返回第一个非空参数，
 * 如果两个参数都为null则抛出异常(例如 {@code Objects.requireNonNullElse})。
 * </ul>
 *
 * @author Sebastien Deleuze
 * @since 6.2
 * @see <a href="https://github.com/JetBrains/java-annotations/blob/master/src/jvmMain/java/org/jetbrains/annotations/Contract.java">org.jetbrains.annotations.Contract</a>
 * @see <a href="https://github.com/uber/NullAway/wiki/Configuration#custom-contract-annotations">
 * NullAway 自定义契约注解</a>
 */
@Documented
@Target(ElementType.METHOD)
public @interface Contract {

	/**
	 * Contains the contract clauses describing causal relations between call arguments and the returned value.
	 * 包含描述调用参数和返回值之间因果关系的契约子句。
	 */
	String value() default "";

}
