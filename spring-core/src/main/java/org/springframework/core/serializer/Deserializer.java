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

package org.springframework.core.serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A strategy interface for converting from data in an InputStream to an Object.
 *
 * <p>用于将InputStream中的数据转换为对象的策略接口。
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0.5
 * @param <T> the object type
 * @param <T> 对象类型
 * @see Serializer
 */
@FunctionalInterface
public interface Deserializer<T> {

	/**
	 * Read (assemble) an object of type T from the given InputStream.
	 * <p>Note: Implementations should not close the given InputStream
	 * (or any decorators of that InputStream) but rather leave this up
	 * to the caller.
	 *
	 * <p>从给定的InputStream读取（组装）类型T的对象。
	 * <p>注意：实现不应关闭给定的InputStream
	 * （或该InputStream的任何装饰器），而应将其留给调用者处理。
	 * @param inputStream the input stream
	 * <p>输入流
	 * @return the deserialized object
	 * <p>反序列化的对象
	 * @throws IOException in case of errors reading from the stream
	 * <p>从流中读取时发生错误
	 */
	T deserialize(InputStream inputStream) throws IOException;

	/**
	 * Read (assemble) an object of type T from the given byte array.
	 *
	 * <p>从给定的字节数组读取（组装）类型T的对象。
	 * @param serialized the byte array
	 * <p> 字节数组
	 * @return the deserialized object
	 * <p>反序列化的对象
	 * @throws IOException in case of deserialization failure
	 * <p>反序列化失败时
	 * @since 5.2.7
	 */
	default T deserializeFromByteArray(byte[] serialized) throws IOException {
		return deserialize(new ByteArrayInputStream(serialized));
	}

}
