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

package org.springframework.beans;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * 默认的 {@link BeanWrapper} 实现，应足以满足所有典型用例。
 * 缓存内省结果以提高效率。
 *
 * <p>注意：自动注册来自 {@code org.springframework.beans.propertyeditors} 包的默认属性编辑器，
 * 这些编辑器在 JDK 标准 PropertyEditors 之外应用。应用程序可以调用
 * {@link #registerCustomEditor(Class, java.beans.PropertyEditor)} 方法
 * 为特定实例注册编辑器（即它们不在整个应用程序中共享）。
 * 有关详细信息，请参阅基类 {@link PropertyEditorRegistrySupport}。
 *
 * <p><b>注意：自 Spring 2.5 起，这几乎在所有用途上都是内部类。</b>
 * 它之所以是公共的，只是为了允许从其他框架包访问。
 * 对于标准应用程序访问，请改用 {@link PropertyAccessorFactory#forBeanPropertyAccess} 工厂方法。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Stephane Nicoll
 * @since 15 April 2001
 * @see #registerCustomEditor
 * @see #setPropertyValues
 * @see #setPropertyValue
 * @see #getPropertyValue
 * @see #getPropertyType
 * @see BeanWrapper
 * @see PropertyEditorRegistrySupport
 */
public class BeanWrapperImpl extends AbstractNestablePropertyAccessor implements BeanWrapper {

	/**
	 * 此对象的缓存内省结果，以避免每次都遇到 JavaBeans 内省的成本。
	 */
	@Nullable
	private CachedIntrospectionResults cachedIntrospectionResults;


	/**
	 * 创建新的空 BeanWrapperImpl。之后需要设置被包装实例。
	 * 注册默认编辑器。
	 * @see #setWrappedInstance
	 */
	public BeanWrapperImpl() {
		this(true);
	}

	/**
	 * 创建新的空 BeanWrapperImpl。之后需要设置被包装实例。
	 * @param registerDefaultEditors 是否注册默认编辑器
	 * （如果 BeanWrapper 不需要任何类型转换，可以抑制）
	 * @see #setWrappedInstance
	 */
	public BeanWrapperImpl(boolean registerDefaultEditors) {
		super(registerDefaultEditors);
	}

	/**
	 * 为给定对象创建新的 BeanWrapperImpl。
	 * @param object 此 BeanWrapper 包装的对象
	 */
	public BeanWrapperImpl(Object object) {
		super(object);
	}

	/**
	 * 创建新的 BeanWrapperImpl，包装指定类的新实例。
	 * @param clazz 要实例化和包装的类
	 */
	public BeanWrapperImpl(Class<?> clazz) {
		super(clazz);
	}

	/**
	 * Create a new BeanWrapperImpl for the given object,
	 * registering a nested path that the object is in.
	 * @param object the object wrapped by this BeanWrapper
	 * @param nestedPath the nested path of the object
	 * @param rootObject the root object at the top of the path
	 */
	public BeanWrapperImpl(Object object, String nestedPath, Object rootObject) {
		super(object, nestedPath, rootObject);
	}

	/**
	 * Create a new BeanWrapperImpl for the given object,
	 * registering a nested path that the object is in.
	 * @param object the object wrapped by this BeanWrapper
	 * @param nestedPath the nested path of the object
	 * @param parent the containing BeanWrapper (must not be {@code null})
	 */
	private BeanWrapperImpl(Object object, String nestedPath, BeanWrapperImpl parent) {
		super(object, nestedPath, parent);
	}


	/**
	 * Set a bean instance to hold, without any unwrapping of {@link java.util.Optional}.
	 * @param object the actual target object
	 * @since 4.3
	 * @see #setWrappedInstance(Object)
	 */
	public void setBeanInstance(Object object) {
		this.wrappedObject = object;
		this.rootObject = object;
		this.typeConverterDelegate = new TypeConverterDelegate(this, this.wrappedObject);
		setIntrospectionClass(object.getClass());
	}

	@Override
	public void setWrappedInstance(Object object, @Nullable String nestedPath, @Nullable Object rootObject) {
		super.setWrappedInstance(object, nestedPath, rootObject);
		setIntrospectionClass(getWrappedClass());
	}

	/**
	 * Set the class to introspect.
	 * Needs to be called when the target object changes.
	 * @param clazz the class to introspect
	 */
	protected void setIntrospectionClass(Class<?> clazz) {
		if (this.cachedIntrospectionResults != null && this.cachedIntrospectionResults.getBeanClass() != clazz) {
			this.cachedIntrospectionResults = null;
		}
	}

	/**
	 * Obtain a lazily initialized CachedIntrospectionResults instance
	 * for the wrapped object.
	 */
	private CachedIntrospectionResults getCachedIntrospectionResults() {
		if (this.cachedIntrospectionResults == null) {
			this.cachedIntrospectionResults = CachedIntrospectionResults.forClass(getWrappedClass());
		}
		return this.cachedIntrospectionResults;
	}


	/**
	 * Convert the given value for the specified property to the latter's type.
	 * <p>This method is only intended for optimizations in a BeanFactory.
	 * Use the {@code convertIfNecessary} methods for programmatic conversion.
	 * @param value the value to convert
	 * @param propertyName the target property
	 * (note that nested or indexed properties are not supported here)
	 * @return the new value, possibly the result of type conversion
	 * @throws TypeMismatchException if type conversion failed
	 */
	@Nullable
	public Object convertForProperty(@Nullable Object value, String propertyName) throws TypeMismatchException {
		CachedIntrospectionResults cachedIntrospectionResults = getCachedIntrospectionResults();
		PropertyDescriptor pd = cachedIntrospectionResults.getPropertyDescriptor(propertyName);
		if (pd == null) {
			throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName,
					"No property '" + propertyName + "' found");
		}
		TypeDescriptor td = ((GenericTypeAwarePropertyDescriptor) pd).getTypeDescriptor();
		return convertForProperty(propertyName, null, value, td);
	}

	@Override
	@Nullable
	protected PropertyHandler getLocalPropertyHandler(String propertyName) {
		PropertyDescriptor pd = getCachedIntrospectionResults().getPropertyDescriptor(propertyName);
		return (pd != null ? new BeanPropertyHandler((GenericTypeAwarePropertyDescriptor) pd) : null);
	}

	@Override
	protected BeanWrapperImpl newNestedPropertyAccessor(Object object, String nestedPath) {
		return new BeanWrapperImpl(object, nestedPath, this);
	}

	@Override
	protected NotWritablePropertyException createNotWritablePropertyException(String propertyName) {
		PropertyMatches matches = PropertyMatches.forProperty(propertyName, getRootClass());
		throw new NotWritablePropertyException(getRootClass(), getNestedPath() + propertyName,
				matches.buildErrorMessage(), matches.getPossibleMatches());
	}

	@Override
	public PropertyDescriptor[] getPropertyDescriptors() {
		return getCachedIntrospectionResults().getPropertyDescriptors();
	}

	@Override
	public PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException {
		BeanWrapperImpl nestedBw = (BeanWrapperImpl) getPropertyAccessorForPropertyPath(propertyName);
		String finalPath = getFinalPath(nestedBw, propertyName);
		PropertyDescriptor pd = nestedBw.getCachedIntrospectionResults().getPropertyDescriptor(finalPath);
		if (pd == null) {
			throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName,
					"No property '" + propertyName + "' found");
		}
		return pd;
	}


	private class BeanPropertyHandler extends PropertyHandler {

		private final GenericTypeAwarePropertyDescriptor pd;

		public BeanPropertyHandler(GenericTypeAwarePropertyDescriptor pd) {
			super(pd.getPropertyType(), pd.getReadMethod() != null, pd.getWriteMethod() != null);
			this.pd = pd;
		}

		@Override
		public TypeDescriptor toTypeDescriptor() {
			return this.pd.getTypeDescriptor();
		}

		@Override
		public ResolvableType getResolvableType() {
			return this.pd.getReadMethodType();
		}

		@Override
		public TypeDescriptor getMapValueType(int nestingLevel) {
			return new TypeDescriptor(
					this.pd.getReadMethodType().getNested(nestingLevel).asMap().getGeneric(1),
					null, this.pd.getTypeDescriptor().getAnnotations());
		}

		@Override
		public TypeDescriptor getCollectionType(int nestingLevel) {
			return new TypeDescriptor(
					this.pd.getReadMethodType().getNested(nestingLevel).asCollection().getGeneric(),
					null, this.pd.getTypeDescriptor().getAnnotations());
		}

		@Override
		@Nullable
		public TypeDescriptor nested(int level) {
			return this.pd.getTypeDescriptor().nested(level);
		}

		@Override
		@Nullable
		public Object getValue() throws Exception {
			Method readMethod = this.pd.getReadMethod();
			Assert.state(readMethod != null, "No read method available");
			ReflectionUtils.makeAccessible(readMethod);
			return readMethod.invoke(getWrappedInstance(), (Object[]) null);
		}

		@Override
		public void setValue(@Nullable Object value) throws Exception {
			Method writeMethod = this.pd.getWriteMethodForActualAccess();
			ReflectionUtils.makeAccessible(writeMethod);
			writeMethod.invoke(getWrappedInstance(), value);
		}

		@Override
		public boolean setValueFallbackIfPossible(@Nullable Object value) {
			try {
				Method writeMethod = this.pd.getWriteMethodFallback(value != null ? value.getClass() : null);
				if (writeMethod == null) {
					writeMethod = this.pd.getUniqueWriteMethodFallback();
					if (writeMethod != null) {
						// Conversion necessary as we would otherwise have received the method
						// from the type-matching getWriteMethodFallback call above already
						value = convertForProperty(this.pd.getName(), null, value,
								new TypeDescriptor(new MethodParameter(writeMethod, 0)));
					}
				}
				if (writeMethod != null) {
					ReflectionUtils.makeAccessible(writeMethod);
					writeMethod.invoke(getWrappedInstance(), value);
					return true;
				}
			}
			catch (Exception ex) {
				LogFactory.getLog(BeanPropertyHandler.class).debug("Write method fallback failed", ex);
			}
			return false;
		}
	}

}
