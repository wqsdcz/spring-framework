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

package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;

/**
 * 配置接口，大多数（如果不是全部）{@link ConversionService}类型都应该实现。
 * 整合并暴露了 {@link ConversionService} 的只读操作和 {@link ConverterRegistry} 的变更操作，
 * 从而允许通过便捷的临时方式添加和移除
 * {@link org.springframework.core.convert.converter.Converter 转换器}。
 * 后者在应用程序上下文引导代码中使用
 * {@link org.springframework.core.env.ConfigurableEnvironment 可配置环境}实例时特别有用。
 *
 * @author Chris Beams
 * @since 3.1
 * @see org.springframework.core.env.ConfigurablePropertyResolver#getConversionService()
 * @see org.springframework.core.env.ConfigurableEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment()
 */
public interface ConfigurableConversionService extends ConversionService, ConverterRegistry {

}
