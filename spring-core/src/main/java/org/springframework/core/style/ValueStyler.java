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

package org.springframework.core.style;

import org.springframework.lang.Nullable;

/**
 * Strategy that encapsulates value String styling algorithms
 * according to Spring conventions.
 *
 * <p>根据Spring约定封装值字符串样式化算法的策略。
 *
 * @author Keith Donald
 * @since 1.2.2
 */
public interface ValueStyler {

	/**
	 * Style the given value, returning a String representation.
	 *
	 * <p>样式化给定的值，返回字符串表示形式。
	 * @param value 要样式化的对象值
	 * @return 样式化的字符串
	 */
	String style(@Nullable Object value);

}
