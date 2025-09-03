/*
 * Copyright 2002-2016 the original author or authors.
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

package org.aopalliance.intercept;

/**
 * <p>该接口代表程序中的调用过程。</p>
 * <p>调用作为连接点（joinpoint），可被拦截器（interceptor）所截获。</p>
 *
 * @author Rod Johnson
 */
public interface Invocation extends Joinpoint {

	/**
	 * 以数组形式返回调用参数。
	 * 可通过修改此数组中的元素值来改变实际参数。
	 * @return 调用参数列表
	 */
	Object[] getArguments();

}
