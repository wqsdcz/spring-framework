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

package org.springframework.core.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>为能够提供 {@link InputStream} 的对象提供的简单接口。
 * 
 * <p>这是 Spring 更广泛的 {@link Resource} 接口的基础接口。
 * 
 * <p>对于一次性流，可以对任何给定的 {@code InputStream} 使用 {@link InputStreamResource}。
 * Spring 的 {@link ByteArrayResource} 或任何基于文件的 {@code Resource} 实现可以作为具体实例使用，
 * 允许多次读取底层内容流。这使得该接口可用作邮件附件等的抽象内容源。
 *
 * @author Juergen Hoeller
 * @since 20.01.2004
 * @see java.io.InputStream
 * @see Resource
 * @see InputStreamResource
 * @see ByteArrayResource
 */
@FunctionalInterface
public interface InputStreamSource {

	/**
	 * <p>返回底层资源的 {@link InputStream} 内容。
	 * 
	 * <p>通常期望每次调用都创建一个<i>新的</i>流。
	 * 
	 * <p>当你考虑 JavaMail 这样的 API 时，这个要求特别重要，
	 * 它在创建邮件附件时需要能够多次读取流。对于这样的用例，
	 * <i>要求</i>每次调用 {@code getInputStream()} 都返回一个新流。
	 * 
	 * @return  底层资源的输入流（不能为 {@code null}）
	 * @throws java.io.FileNotFoundException  如果底层资源不存在
	 * @throws IOException  如果内容流无法打开
	 * @see Resource#isReadable()
	 * @see Resource#isOpen()
	 */
	InputStream getInputStream() throws IOException;

}
