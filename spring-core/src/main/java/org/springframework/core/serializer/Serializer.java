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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A strategy interface for streaming an object to an OutputStream.
 *
 * <p>用于将对象流式传输到OutputStream的策略接口。
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0.5
 * @param <T> the object type
 * <p> 对象类型
 * @see Deserializer
 */
@FunctionalInterface
public interface Serializer<T> {

	/**
	 * Write an object of type T to the given OutputStream.
	 * <p>Note: Implementations should not close the given OutputStream
	 * (or any decorators of that OutputStream) but rather leave this up
	 * to the caller.
	 *
	 * <p>将类型T的对象写入给定的OutputStream。
	 * <p>注意：实现不应关闭给定的OutputStream
	 * （或该OutputStream的任何装饰器），而应将其留给调用者处理。
	 * @param object the object to serialize
	 * <p>要序列化的对象
	 * @param outputStream the output stream
	 * <p>输出流
	 * @throws IOException in case of errors writing to the stream
	 * <p>写入流时发生错误
	 */
	void serialize(T object, OutputStream outputStream) throws IOException;

	/**
	 * Turn an object of type T into a serialized byte array.
	 *
	 * <p>将类型T的对象转换为序列化的字节数组。
	 * @param object the object to serialize
	 * <p>要序列化的对象
	 * @return the resulting byte array
	 * <p>生成的字节数组
	 * @throws IOException in case of serialization failure
	 * <p>序列化失败时
	 * @since 5.2.7
	 */
	default byte[] serializeToByteArray(T object) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		serialize(object, out);
		return out.toByteArray();
	}

}
