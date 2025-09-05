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

package org.springframework.aop;

import org.springframework.lang.Nullable;

/**
 * <p>暴露代理背后的目标类的最小化接口。</p>
 * <p>该接口由AOP代理对象、代理工厂（通过{@link org.springframework.aop.framework.Advised}接口） 以及{@link TargetSource 目标源}共同实现。</p>
 *
 * Minimal interface for exposing the target class behind a proxy.
 *
 * <p>Implemented by AOP proxy objects and proxy factories
 * (via {@link org.springframework.aop.framework.Advised})
 * as well as by {@link TargetSource TargetSources}.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see org.springframework.aop.support.AopUtils#getTargetClass(Object)
 */
public interface TargetClassAware {

	/**
	 * 返回实现对象（通常是代理配置或实际代理实例）背后的目标类。
	 * @return 目标类，若未知目标类则返回 {@code null}
	 */
	@Nullable
	Class<?> getTargetClass();

}
