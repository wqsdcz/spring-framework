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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * 用于Web交互的MVC视图。实现类负责渲染内容并暴露模型。
 * 单个视图可暴露多个模型属性。
 *
 * <p>此类别及与之相关的MVC方法在Rod Johnson所著的
 * <a href="https://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">
 * 《Expert One-On-One J2EE Design and Development》</a>
 * （Wrox出版社，2002年）第12章中有详细讨论。
 *
 * <p>
 *     视图实现可能差异很大。
 *     明显的实现可以是基于JSP的，其他实现可能基于XSLT或使用HTML生成库。
 *     此接口的设计旨在避免限制可能的实现范围。
 *
 * <p>
 *     视图应为Bean对象，很可能由ViewResolver以Bean形式实例化。
 *     由于此接口是无状态的，视图实现应该是线程安全的。
 *
 * @author Rod Johnson
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @see org.springframework.web.servlet.view.AbstractView
 * @see org.springframework.web.servlet.view.InternalResourceView
 */
public interface View {

	/**
	 * 包含响应状态码的{@link HttpServletRequest}属性名称。
	 * <p>注意：并非所有视图实现都需要支持此属性。
	 * @since 3.0
	 */
	String RESPONSE_STATUS_ATTRIBUTE = View.class.getName() + ".responseStatus";

 	/**
	 * 包含路径变量Map的{@link HttpServletRequest}属性名称。
	 * 该Map包含基于字符串的URI模板变量名作为键，以及它们对应的基于对象的值——这些值从URL段中提取并经过类型转换。
	 * <p>注意：并非所有视图实现都需要支持此属性。
	 * @since 3.1
	 */
	String PATH_VARIABLES = View.class.getName() + ".pathVariables";

	/**
	 * 在内容协商过程中选择的{@link org.springframework.http.MediaType}，可能比视图配置的媒体类型更具体。
	 * 例如："application/vnd.example-v1+xml" 与 "application/*+xml"。
	 * @since 3.2
	 */
	String SELECTED_CONTENT_TYPE = View.class.getName() + ".selectedContentType";


	/**
	 * 返回视图的预定义内容类型（如果已确定）。
	 * <p>
	 *     可用于提前检查视图的内容类型，即在尝试实际渲染之前。
	 *
	 * @return 内容类型字符串（可选包含字符集），如果未预先确定则返回 {@code null}
	 */
	@Nullable
	default String getContentType() {
		return null;
	}

	/**
	 * 根据指定模型渲染视图。
	 * <p>
	 *     第一步将是准备请求：在JSP情况下，这意味着将模型对象设置为请求属性。
	 *     第二步将是视图的实际渲染，例如：通过RequestDispatcher包含JSP。
	 *
	 * @param model 包含名称字符串作为键和对应模型对象作为值的Map（对于空模型的情况，Map也可以为{@code null}）
	 * @param request 当前HTTP请求
	 * @param response 正在构建的HTTP响应
	 * @throws Exception 如果渲染失败
	 */
	void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception;

}
