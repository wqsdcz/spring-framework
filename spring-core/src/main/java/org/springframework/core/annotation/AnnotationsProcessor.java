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

import org.springframework.lang.Nullable;

/**
 * 用于处理注解的回调接口。
 *
 * @author Phillip Webb
 * @since 5.2
 * @param <C> 上下文类型
 * @param <R> 结果类型
 * @see AnnotationsScanner
 * @see TypeMappedAnnotations
 */
@FunctionalInterface
interface AnnotationsProcessor<C, R> {

	/**
	 * 当聚合即将被处理时调用。
	 * 在处理一个注解聚合（例如类上的所有注解）之前被调用。
	 * 此方法可以返回一个非空结果以跳过任何进一步的处理。
	 *
	 * @param context 处理器相关的上下文信息
	 * @param aggregateIndex 即将被处理的聚合索引
	 * @return 如果不需要进一步处理，则返回一个非空结果
	 */
	@Nullable
	default R doWithAggregate(C context, int aggregateIndex) {
		return null;
	}

	/**
	 * 当可以处理注解数组时调用。
	 * 当某个聚合内的所有注解（数组形式）准备就绪时调用。
	 * 此方法可以返回一个 {@code 非空} 结果以跳过任何进一步的处理。
	 *
	 * @param context 与处理器相关的上下文信息
	 * @param aggregateIndex 提供的注解的聚合索引
	 * @param source 注解的原始来源（如果已知）
	 * @param annotations 要处理的注解（此数组可能包含 {@code null} 元素）
	 * @return 如果不需要进一步处理，则返回一个 {@code 非空} 结果
	 */
	@Nullable
	R doWithAnnotations(C context, int aggregateIndex, @Nullable Object source, Annotation[] annotations);

	/**
	 * 所有聚合处理完毕后调用。
	 * 默认情况下，此方法返回最后处理的结果。
	 *
	 * @param result 最后一次提前退出的结果，如果没有则为 {@code null}
	 * @return 要返回给调用者的最终结果
	 */
	@Nullable
	default R finish(@Nullable R result) {
		return result;
	}

}
