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

package org.springframework.core.serializer.support;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.util.Assert;

/**
 * A {@link Converter} that delegates to a
 * {@link org.springframework.core.serializer.Serializer}
 * to convert an object to a byte array.
 *
 * <p>委托给{@link org.springframework.core.serializer.Serializer}的{@link Converter}，
 * 用于将对象转换为字节数组。
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @since 3.0.5
 */
public class SerializingConverter implements Converter<Object, byte[]> {

	private final Serializer<Object> serializer;


	/**
	 * Create a default {@code SerializingConverter} that uses standard Java serialization.
	 *
	 * <p>创建使用标准Java序列化的默认{@code SerializingConverter}。
	 */
	public SerializingConverter() {
		this.serializer = new DefaultSerializer();
	}

	/**
	 * Create a {@code SerializingConverter} that delegates to the provided {@link Serializer}.
	 *
	 * <p>创建委托给提供的{@link Serializer}的{@code SerializingConverter}。
	 */
	public SerializingConverter(Serializer<Object> serializer) {
		Assert.notNull(serializer, "Serializer must not be null");
		this.serializer = serializer;
	}


	/**
	 * Serializes the source object and returns the byte array result.
	 *
	 * <p>序列化源对象并返回字节数组结果。
	 */
	@Override
	public byte[] convert(Object source) {
		try {
			return this.serializer.serializeToByteArray(source);
		}
		catch (Throwable ex) {
			throw new SerializationFailedException("Failed to serialize object using " +
					this.serializer.getClass().getSimpleName(), ex);
		}
	}

}
