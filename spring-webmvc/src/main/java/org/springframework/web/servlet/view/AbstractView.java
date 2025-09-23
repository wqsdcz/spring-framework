/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.view;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ContextExposingHttpServletRequest;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContext;

/**
 * {@link org.springframework.web.servlet.View}实现的抽象基类。
 * 子类应为JavaBeans，以便能够作为Spring管理的bean实例进行便捷配置。
 *
 * <p>
 *     提供对静态属性的支持，这些属性将被提供给视图，并提供了多种指定静态属性的方式。
 *     静态属性将在每次渲染操作时与给定的动态属性（控制器返回的模型）合并。
 *
 * <p>
 *     继承{@link WebApplicationObjectSupport}，这对某些视图会有帮助。
 *     子类只需实现实际的渲染逻辑。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setAttributes
 * @see #setAttributesMap
 * @see #renderMergedOutputModel
 */
public abstract class AbstractView extends WebApplicationObjectSupport implements View, BeanNameAware {

	/** 默认内容类型。可作为bean属性进行覆盖 */
	public static final String DEFAULT_CONTENT_TYPE = "text/html;charset=ISO-8859-1";

	/** 临时输出字节数组（如有）的初始大小 */
	private static final int OUTPUT_BYTE_ARRAY_INITIAL_SIZE = 4096;


	@Nullable
	private String contentType = DEFAULT_CONTENT_TYPE;

	@Nullable
	private String requestContextAttribute;

	private final Map<String, Object> staticAttributes = new LinkedHashMap<>();

	private boolean exposePathVariables = true;

	private boolean exposeContextBeansAsAttributes = false;

	@Nullable
	private Set<String> exposedContextBeanNames;

	@Nullable
	private String beanName;



	/**
	 * 设置此视图的内容类型。
	 * 默认为"text/html;charset=ISO-8859-1"。
	 * <p>如果视图本身假定会设置内容类型（例如JSP情况），子类可能会忽略此设置。
	 */
	public void setContentType(@Nullable String contentType) {
		this.contentType = contentType;
	}

	/**
	 * 返回此视图的内容类型。
	 */
	@Override
	@Nullable
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * 设置此视图的RequestContext属性名称。
	 * 默认不设置。
	 */
	public void setRequestContextAttribute(@Nullable String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	/**
	 * 返回RequestContext属性名称（如果有的话）。
	 */
	@Nullable
	public String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	/**
	 * 以CSV字符串格式设置静态属性。
	 * 格式为：attname0={value1},attname1={value1}
	 * <p>
	 *     "静态"属性是在视图实例配置中指定的固定属性，而"动态"属性则是作为模型的一部分传入的值。
	 */
	public void setAttributesCSV(@Nullable String propString) throws IllegalArgumentException {
		if (propString != null) {
			StringTokenizer st = new StringTokenizer(propString, ",");
			while (st.hasMoreTokens()) {
				String tok = st.nextToken();
				int eqIdx = tok.indexOf('=');
				if (eqIdx == -1) {
					throw new IllegalArgumentException(
							"Expected '=' in attributes CSV string '" + propString + "'");
				}
				if (eqIdx >= tok.length() - 2) {
					throw new IllegalArgumentException(
							"At least 2 characters ([]) required in attributes CSV string '" + propString + "'");
				}
				String name = tok.substring(0, eqIdx);
				String value = tok.substring(eqIdx + 1);

				// Delete first and last characters of value: { and }
				value = value.substring(1);
				value = value.substring(0, value.length() - 1);

				addStaticAttribute(name, value);
			}
		}
	}

	/**
	 * 通过{@code java.util.Properties}对象为此视图设置静态属性。
	 *
	 * <p>
	 *     "静态"属性是在视图实例配置中指定的固定属性，而"动态"属性则是作为模型的一部分传入的值。
	 * <p>
	 *     这是设置静态属性最便捷的方式。
	 *     请注意，如果模型中包含同名值，静态属性会被动态属性覆盖。
	 *
	 * <p>
	 *     可通过字符串"value"（通过PropertiesEditor解析）或XML bean定义中的"props"元素来填充。
	 *
	 * @see org.springframework.beans.propertyeditors.PropertiesEditor
	 */
	public void setAttributes(Properties attributes) {
		CollectionUtils.mergePropertiesIntoMap(attributes, this.staticAttributes);
	}

	/**
	 * 通过Map为此视图设置静态属性。这允许设置任何类型的属性值，例如bean引用。
	 *
	 * <p>
	 *     "静态"属性是在视图实例配置中指定的固定属性，而"动态"属性则是作为模型的一部分传入的值。
	 *
	 * <p>可通过XML bean定义中的"map"或"props"元素来填充。
	 *
	 * @param attributes 一个以名称字符串为键、属性对象为值的Map
	 */
	public void setAttributesMap(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			attributes.forEach(this::addStaticAttribute);
		}
	}

	/**
	 * 允许通过Map访问此视图的静态属性，
	 * 并提供添加或覆盖特定条目的选项。
	 * <p>
	 *     适用于直接指定条目，例如通过"attributesMap[myKey]"方式。
	 *     这对于在子视图定义中添加或覆盖条目特别有用。
	 */
	public Map<String, Object> getAttributesMap() {
		return this.staticAttributes;
	}

	/**
	 * 向此视图添加静态数据，这些数据将在每个视图中暴露。
	 * <p>"静态"属性是在视图实例配置中指定的固定属性，而"动态"属性则是作为模型的一部分传入的值。
	 * <p>必须在调用{@code render}方法之前调用此方法。
	 *
	 * @param name 要暴露的属性名称
	 * @param value 要暴露的属性值
	 * @see #render
	 */
	public void addStaticAttribute(String name, Object value) {
		this.staticAttributes.put(name, value);
	}

	/**
	 * 返回此视图的静态属性。便于测试使用。
	 * <p>返回一个不可修改的Map，因为这不是用于操作Map而是仅用于检查内容。
	 *
	 * @return 此视图中的静态属性
	 */
	public Map<String, Object> getStaticAttributes() {
		return Collections.unmodifiableMap(this.staticAttributes);
	}

	/**
	 * 指定是否将【路径变量】添加到模型中。
	 * <p>
	 *     【路径变量】通常通过【{@code @PathVariable}注解】与【URI模板变量】绑定。
	 *     它们实际上是应用了类型转换的URI模板变量，用于派生类型化的对象值。
	 *     视图中经常需要这些值来构建指向相同或其他URL的链接。
	 *
	 * <p>
	 *     添加到模型中的路径变量会覆盖静态属性（参见{@link #setAttributes(Properties)}），但不会覆盖模型中已存在的属性。
	 *
	 * <p>
	 *     默认情况下此标志设置为{@code true}。
	 *     具体视图类型可以覆盖此设置。
	 *
	 * @param exposePathVariables {@code true}表示暴露路径变量，{@code false}则不暴露
	 */
	public void setExposePathVariables(boolean exposePathVariables) {
		this.exposePathVariables = exposePathVariables;
	}

	/**
	 * 返回是否将路径变量添加到模型中。
	 */
	public boolean isExposePathVariables() {
		return this.exposePathVariables;
	}

	/**
	 * 设置是否使应用程序上下文中的所有Spring bean都可通过请求属性访问，
	 * 通过惰性检查在属性被访问时一次性完成。
	 * <p>
	 *     这将使所有此类bean在JSP 2.0页面中可以通过普通的{@code ${...}}表达式访问，
	 *     也可以在JSTL的{@code c:out}值表达式中访问。
	 * <p>默认为"false"。开启此标志可透明地在请求属性命名空间中暴露所有Spring bean。
	 * <p>
	 *     <b>注意：</b>上下文bean将覆盖手动添加的同名自定义请求或会话属性。
	 *     但是，同名的模型属性（明确暴露给此视图的）将始终覆盖上下文bean。
	 * @see #getRequestToExpose
	 */
	public void setExposeContextBeansAsAttributes(boolean exposeContextBeansAsAttributes) {
		this.exposeContextBeansAsAttributes = exposeContextBeansAsAttributes;
	}

	/**
	 * 指定上下文中需要暴露的bean名称。
	 * 如果此值非空，则只有指定的bean有资格作为属性暴露。
	 * <p>
	 *     如果需要暴露应用上下文中的所有Spring bean，
	 *     请开启{@link #setExposeContextBeansAsAttributes "exposeContextBeansAsAttributes"}标志，
	 *     但不要为此属性列出具体的bean名称。
	 */
	public void setExposedContextBeanNames(String... exposedContextBeanNames) {
		this.exposedContextBeanNames = new HashSet<>(Arrays.asList(exposedContextBeanNames));
	}

	/**
	 * 设置视图名称。有助于提高可追溯性。
	 * <p>框架代码在构建视图时必须调用此方法。
	 */
	@Override
	public void setBeanName(@Nullable String beanName) {
		this.beanName = beanName;
	}

	/**
	 * 返回视图名称。
	 * 如果视图配置正确，返回值应永远不为{@code null}。
	 */
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}


	/**
	 * 使用指定模型准备视图，必要时将其与静态属性和RequestContext属性合并。
	 * 将实际渲染工作委托给renderMergedOutputModel方法。
	 * @see #renderMergedOutputModel
	 */
	@Override
	public void render(@Nullable Map<String, ?> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		if (logger.isTraceEnabled()) {
			logger.trace("Rendering view with name '" + this.beanName + "' with model " + model +
				" and static attributes " + this.staticAttributes);
		}

		Map<String, Object> mergedModel = createMergedOutputModel(model, request, response);
		prepareResponse(request, response);
		renderMergedOutputModel(mergedModel, getRequestToExpose(request), response);
	}

	/**
	 * 创建一个包含动态值和静态属性的合并输出Map（绝不会为{@code null}）。
	 * 动态值优先于静态属性。
	 */
	protected Map<String, Object> createMergedOutputModel(@Nullable Map<String, ?> model,
			HttpServletRequest request, HttpServletResponse response) {

		@SuppressWarnings("unchecked")
		Map<String, Object> pathVars = (this.exposePathVariables ?
				(Map<String, Object>) request.getAttribute(View.PATH_VARIABLES) : null);

		// Consolidate static and dynamic model attributes.
		int size = this.staticAttributes.size();
		size += (model != null ? model.size() : 0);
		size += (pathVars != null ? pathVars.size() : 0);

		Map<String, Object> mergedModel = new LinkedHashMap<>(size);
		mergedModel.putAll(this.staticAttributes);
		if (pathVars != null) {
			mergedModel.putAll(pathVars);
		}
		if (model != null) {
			mergedModel.putAll(model);
		}

		// Expose RequestContext?
		if (this.requestContextAttribute != null) {
			mergedModel.put(this.requestContextAttribute, createRequestContext(request, response, mergedModel));
		}

		return mergedModel;
	}

	/**
	 * 在指定属性名下创建要暴露的RequestContext。
	 * <p>
	 *     默认实现为给定的请求和模型创建标准的RequestContext实例。
	 *     子类可重写此方法以创建自定义实例。
	 *
	 * @param request 当前HTTP请求
	 * @param model 合并的输出Map（绝不会为{@code null}），其中动态值优先于静态属性
	 * @return RequestContext实例
	 * @see #setRequestContextAttribute
	 * @see org.springframework.web.servlet.support.RequestContext
	 */
	protected RequestContext createRequestContext(
			HttpServletRequest request, HttpServletResponse response, Map<String, Object> model) {

		return new RequestContext(request, response, getServletContext(), model);
	}

	/**
	 * 为渲染准备给定的HTTP响应。
	 * <p>
	 *     默认实现针对通过HTTPS发送下载内容时的IE浏览器bug应用一个解决方案。
	 *
	 * @param request 当前HTTP请求
	 * @param response 当前HTTP响应
	 */
	protected void prepareResponse(HttpServletRequest request, HttpServletResponse response) {
		if (generatesDownloadContent()) {
			response.setHeader("Pragma", "private");
			response.setHeader("Cache-Control", "private, must-revalidate");
		}
	}

	/**
	 * 返回此视图是否生成下载内容（通常是PDF或Excel文件等二进制内容）。
	 * <p>
	 *     默认实现返回{@code false}。
	 *     如果子类确知自己正在生成需要客户端临时缓存的下载内容（通常通过响应输出流实现），建议重写此方法返回{@code true}。
	 *
	 * @see #prepareResponse
	 * @see javax.servlet.http.HttpServletResponse#getOutputStream()
	 */
	protected boolean generatesDownloadContent() {
		return false;
	}

	/**
	 * 获取要暴露给{@link #renderMergedOutputModel}（即视图）的请求句柄。
	 * <p>
	 *     默认实现包装原始请求以支持将Spring bean作为请求属性暴露（如果需要）。
	 *
	 * @param originalRequest 引擎提供的原始servlet请求
	 * @return 包装后的请求，如果无需包装则返回原始请求
	 * @see #setExposeContextBeansAsAttributes
	 * @see #setExposedContextBeanNames
	 * @see org.springframework.web.context.support.ContextExposingHttpServletRequest
	 */
	protected HttpServletRequest getRequestToExpose(HttpServletRequest originalRequest) {
		if (this.exposeContextBeansAsAttributes || this.exposedContextBeanNames != null) {
			WebApplicationContext wac = getWebApplicationContext();
			Assert.state(wac != null, "No WebApplicationContext");
			return new ContextExposingHttpServletRequest(originalRequest, wac, this.exposedContextBeanNames);
		}
		return originalRequest;
	}

	/**
	 * 子类必须实现此方法以实际执行视图渲染。
	 * <p>
	 *     第一步是准备请求：在JSP情况下，这意味着将模型对象设置为请求属性。
	 *     第二步是视图的实际渲染，例如通过RequestDispatcher包含JSP。
	 *
	 * @param model 合并的输出Map（绝不会为{@code null}），其中动态值优先于静态属性
	 * @param request 当前HTTP请求
	 * @param response 当前HTTP响应
	 * @throws Exception 如果渲染失败
	 */
	protected abstract void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception;


	/**
	 * 将给定Map中的模型对象暴露为请求属性。
	 * 名称将从模型Map中获取。
	 * 此方法适用于通过{@link javax.servlet.RequestDispatcher}访问的所有资源。
	 *
	 * @param model 要暴露的模型对象Map
	 * @param request 当前HTTP请求
	 */
	protected void exposeModelAsRequestAttributes(Map<String, Object> model,
			HttpServletRequest request) throws Exception {

		model.forEach((modelName, modelValue) -> {
			if (modelValue != null) {
				request.setAttribute(modelName, modelValue);
				if (logger.isDebugEnabled()) {
					logger.debug("Added model object '" + modelName + "' of type [" + modelValue.getClass().getName() +
							"] to request in view with name '" + getBeanName() + "'");
				}
			}
			else {
				request.removeAttribute(modelName);
				if (logger.isDebugEnabled()) {
					logger.debug("Removed model object '" + modelName +
							"' from request in view with name '" + getBeanName() + "'");
				}
			}
		});
	}

	/**
	 * 为此视图创建临时输出流。
	 * <p>
	 *     通常用于IE兼容方案，在实际将内容写入HTTP响应之前，通过临时流设置内容长度标头。
	 */
	protected ByteArrayOutputStream createTemporaryOutputStream() {
		return new ByteArrayOutputStream(OUTPUT_BYTE_ARRAY_INITIAL_SIZE);
	}

	/**
	 * 将给定的临时输出流写入HTTP响应。
	 *
	 * @param response 当前HTTP响应
	 * @param baos 要写入的临时输出流
	 * @throws IOException 如果写入/刷新失败
	 */
	protected void writeToResponse(HttpServletResponse response, ByteArrayOutputStream baos) throws IOException {
		// Write content type and also length (determined via byte array).
		response.setContentType(getContentType());
		response.setContentLength(baos.size());

		// Flush byte array to servlet output stream.
		ServletOutputStream out = response.getOutputStream();
		baos.writeTo(out);
		out.flush();
	}

	/**
	 * 将响应的内容类型设置为已配置的 {@link #setContentType(String) 内容类型}，
	 * 除非请求属性 {@link View#SELECTED_CONTENT_TYPE} 存在且已设置为具体媒体类型。
	 */
	protected void setResponseContentType(HttpServletRequest request, HttpServletResponse response) {
		MediaType mediaType = (MediaType) request.getAttribute(View.SELECTED_CONTENT_TYPE);
		if (mediaType != null && mediaType.isConcrete()) {
			response.setContentType(mediaType.toString());
		}
		else {
			response.setContentType(getContentType());
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName());
		if (getBeanName() != null) {
			sb.append(": name '").append(getBeanName()).append("'");
		}
		else {
			sb.append(": unnamed");
		}
		return sb.toString();
	}

}
