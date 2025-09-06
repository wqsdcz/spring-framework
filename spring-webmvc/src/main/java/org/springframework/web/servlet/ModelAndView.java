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

package org.springframework.web.servlet;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;

/**
 * <p>
 *     Web MVC框架中Model和View的持有者。
 *     需要注意的是，Model与View在本质上是完全独立的。
 *     该类的存在仅仅是为了让控制器能够通过单个返回值同时返回模型和视图。
 * </p>
 * <p>
 *     它表示处理器返回的模型和视图，将由DispatcherServlet进行解析。
 *     视图可以有两种形式：
 *     一种是需要由ViewResolver对象解析的字符串视图名称；
 *     另一种是可直接指定的View对象。
 *     模型则是一个Map结构，允许通过名称作为键来存储多个对象。
 * </p>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rossen Stoyanchev
 * @see DispatcherServlet
 * @see ViewResolver
 * @see HandlerAdapter#handle
 * @see org.springframework.web.servlet.mvc.Controller#handleRequest
 */
public class ModelAndView {

	/** View实例 或 视图名称的String实例 */
	@Nullable
	private Object view;

	/** Model的Map */
	@Nullable
	private ModelMap model;

	/** 响应的可选HTTP状态 */
	@Nullable
	private HttpStatus status;

	/** 指示是否通过调用{@link #clear()}清除此实例 */
	private boolean cleared = false;


	/**
	 * 用于Bean风格使用的默认构造函数：通过填充Bean属性而非传入构造函数参数的方式进行对象初始化。
	 * @see #setView(View)
	 * @see #setViewName(String)
	 */
	public ModelAndView() {
	}

	/**
	 * 当无需暴露模型数据时，使用的便捷构造函数。也可与 {@code addObject} 方法结合使用。
	 * @param viewName 待渲染的视图名称，将由 DispatcherServlet 的 ViewResolver 进行解析
	 * @see #addObject
	 */
	public ModelAndView(String viewName) {
		this.view = viewName;
	}

	/**
	 * 当无需暴露模型数据时，使用的便捷构造函数。也可与 {@code addObject} 方法配合使用。
	 * @param view 待渲染的视图对象
	 * @see #addObject
	 */
	public ModelAndView(View view) {
		this.view = view;
	}

	/**
	 * 根据视图名称和模型数据创建新的 ModelAndView 实例。
	 * @param viewName 待渲染的视图名称，将由 DispatcherServlet 的 ViewResolver 组件进行解析
	 * @param model 模型映射表，包含模型名称（String 类型）到模型对象（Object 类型）的映射关系。
	 * Model条目不可为 {@code null}，但若无模型数据时，模型 Map 本身可为 {@code null}
	 */
	public ModelAndView(String viewName, @Nullable Map<String, ?> model) {
		this.view = viewName;
		if (model != null) {
			getModelMap().addAllAttributes(model);
		}
	}

	/**
	 * 根据View对象和模型数据创建新的 ModelAndView 实例。
	 * <em>特别注意：提供的模型数据会被复制到本类的内部存储结构中，在将模型 Map 传递给本类后，不应再尝试修改该 Map</em>
	 * @param view 待渲染的视图对象
	 * @param model 模型映射表，包含模型名称（String 类型）到模型对象（Object 类型）的映射关系。
	 * Model条目不可为 {@code null}，但若无模型数据时，模型 Map 本身可为 {@code null}
	 */
	public ModelAndView(View view, @Nullable Map<String, ?> model) {
		this.view = view;
		if (model != null) {
			getModelMap().addAllAttributes(model);
		}
	}

	/**
	 * 根据视图名称和HTTP状态码创建新的ModelAndView实例。
	 * @param viewName 待渲染的视图名称，将由DispatcherServlet的ViewResolver组件进行解析
	 * @param status 用于响应的HTTP状态码（将在视图渲染前设置）
	 * @since 4.3.8 自4.3.8版本起提供
	 */
	public ModelAndView(String viewName, HttpStatus status) {
		this.view = viewName;
		this.status = status;
	}

	/**
	 * 根据视图名称、模型数据及HTTP状态码创建新的ModelAndView实例。
	 * @param viewName 待渲染的视图名称，将由DispatcherServlet的ViewResolver组件进行解析
	 * @param model 模型映射表，包含模型名称（String类型）到模型对象（Object类型）的映射关系。
	 * Model条目不可为{@code null}，但若无模型数据时，模型Map本身可为{@code null}
	 * @param status 用于响应的HTTP状态码（将在视图渲染前设置）
	 * @since 4.3
	 */
	public ModelAndView(@Nullable String viewName, @Nullable Map<String, ?> model, @Nullable HttpStatus status) {
		this.view = viewName;
		if (model != null) {
			getModelMap().addAllAttributes(model);
		}
		this.status = status;
	}

	/**
	 * 用于接收单个模型对象的便捷构造函数。
	 * @param viewName 待渲染的视图名称，将由DispatcherServlet的ViewResolver组件进行解析
	 * @param modelName 模型中单个条目的名称
	 * @param modelObject 单个模型对象
	 */
	public ModelAndView(String viewName, String modelName, Object modelObject) {
		this.view = viewName;
		addObject(modelName, modelObject);
	}

	/**
	 * 用于接收单个模型对象的便捷构造函数。
	 * @param view 待渲染的视图对象
	 * @param modelName 模型中单个条目的名称
	 * @param modelObject 单个模型对象
	 */
	public ModelAndView(View view, String modelName, Object modelObject) {
		this.view = view;
		addObject(modelName, modelObject);
	}


	/**
	 * 为此 ModelAndView 设置视图名称，该名称将由 DispatcherServlet 通过 ViewResolver 进行解析。
	 * 此操作将覆盖任何已存在的视图名称或 View 对象。
	 */
	public void setViewName(@Nullable String viewName) {
		this.view = viewName;
	}

	/**
	 * 返回将由 DispatcherServlet 通过 ViewResolver 解析的视图名称，若当前使用的是 View 对象则返回 {@code null}。
	 */
	@Nullable
	public String getViewName() {
		return (this.view instanceof String ? (String) this.view : null);
	}

	/**
	 * 为此 ModelAndView 设置 View 对象。
	 * 此操作将覆盖任何已存在的视图名称或 View 对象。
	 */
	public void setView(@Nullable View view) {
		this.view = view;
	}

	/**
	 * 返回设置的 View 对象，若当前使用的是需通过 DispatcherServlet 经由 ViewResolver 解析的视图名称，则返回 {@code null}。
	 */
	@Nullable
	public View getView() {
		return (this.view instanceof View ? (View) this.view : null);
	}

	/**
	 * 指示此 {@code ModelAndView} 是否包含视图（无论是以视图名称的形式还是直接的 {@link View} 实例形式）。
	 */
	public boolean hasView() {
		return (this.view != null);
	}

	/**
	 * 返回是否使用视图引用的判断结果：若
	 * 视图是通过名称指定，并需由 DispatcherServlet 经由 ViewResolver 进行解析，则返回 {@code true}。
	 */
	public boolean isReference() {
		return (this.view instanceof String);
	}

	/**
	 * 返回模型的映射。可能返回 {@code null}。
	 * 此方法由 DispatcherServlet 调用以评估模型数据。
	 */
	@Nullable
	protected Map<String, Object> getModelInternal() {
		return this.model;
	}

	/**
	 * 返回底层的 {@code ModelMap} 实例（永不返回 {@code null}）。
	 */
	public ModelMap getModelMap() {
		if (this.model == null) {
			this.model = new ModelMap();
		}
		return this.model;
	}

	/**
	 * 返回模型映射，永不返回 {@code null}。
	 * 此方法专供应用程序代码调用以修改模型数据。
	 */
	public Map<String, Object> getModel() {
		return getModelMap();
	}

	/**
	 * 设置用于响应的HTTP状态码。
	 * <p>响应状态码将在视图渲染之前设置。
	 * @since 4.3 自4.3版本起提供
	 */
	public void setStatus(@Nullable HttpStatus status) {
		this.status = status;
	}

	/**
	 * 返回响应中已配置的HTTP状态码（若存在）。
	 * @since 4.3 自4.3版本起提供
	 */
	@Nullable
	public HttpStatus getStatus() {
		return this.status;
	}


	/**
	 * 向模型中添加属性。
	 * @param attributeName 要添加到模型中的属性名称（永不为 {@code null}）
	 * @param attributeValue 要添加到模型中的属性值（可为 {@code null}）
	 * @see ModelMap#addAttribute(String, Object)
	 * @see #getModelMap()
	 */
	public ModelAndView addObject(String attributeName, @Nullable Object attributeValue) {
		getModelMap().addAttribute(attributeName, attributeValue);
		return this;
	}

	/**
	 * 使用参数名生成机制向模型中添加属性。
	 * @param attributeValue 要添加到模型中的属性值（永不为 {@code null}）
	 * @see ModelMap#addAttribute(Object)
	 * @see #getModelMap()
	 */
	public ModelAndView addObject(Object attributeValue) {
		getModelMap().addAttribute(attributeValue);
		return this;
	}

	/**
	 * 将指定Map中包含的所有属性批量添加到模型中。
	 * @param modelMap 属性名到属性值的映射关系Map（可为空Map）
	 * @see ModelMap#addAllAttributes(Map)
	 * @see #getModelMap()
	 */
	public ModelAndView addAllObjects(@Nullable Map<String, ?> modelMap) {
		getModelMap().addAllAttributes(modelMap);
		return this;
	}


	/**
	 * 清空此 ModelAndView 对象的状态。调用后该对象将变为空状态。
	 * <p>可用于在 HandlerInterceptor 的 {@code postHandle} 方法中抑制指定 ModelAndView 对象的渲染。
	 * @see #isEmpty()
	 * @see HandlerInterceptor#postHandle
	 */
	public void clear() {
		this.view = null;
		this.model = null;
		this.cleared = true;
	}

	/**
	 * 返回此 ModelAndView 对象是否为空的状态，
	 * 即判断该对象是否既未持有任何视图，也不包含模型数据。
	 */
	public boolean isEmpty() {
		return (this.view == null && CollectionUtils.isEmpty(this.model));
	}

	/**
	 * 返回此 ModelAndView 对象是否因调用 {@link #clear} 方法而处于空状态，
	 * 即判断该对象是否既未持有任何视图，也不包含模型数据。
	 * <p>若在调用 {@link #clear} 方法<strong>之后</strong>又向实例中添加了任何附加状态，则将返回 {@code false}。
	 * @see #clear()
	 */
	public boolean wasCleared() {
		return (this.cleared && isEmpty());
	}


	/**
	 * 返回关于此模型和视图的诊断信息。
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ModelAndView: ");
		if (isReference()) {
			sb.append("reference to view with name '").append(this.view).append("'");
		}
		else {
			sb.append("materialized View is [").append(this.view).append(']');
		}
		sb.append("; model is ").append(this.model);
		return sb.toString();
	}

}
