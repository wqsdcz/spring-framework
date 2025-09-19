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

package org.springframework.web.servlet.view.document;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.web.servlet.view.AbstractView;

/**
 * PDF视图的抽象基类。
 * 应用特定的视图类将扩展此类。
 * 视图将直接保存在子类中，而非模板中。
 *
 * <p>
 *     该视图实现使用Bruno Lowagie的<a href="https://www.lowagie.com/iText">iText</a> API。
 *     已知兼容原始iText 2.1.7版本及其分支版本<a href="https://github.com/LibrePDF/OpenPDF">OpenPDF</a>。
 * <b>
 *     我们强烈推荐使用OpenPDF，因为它积极维护并修复了处理不可信PDF内容时的重要安全漏洞。
 *
 * <p>
 *     注意：Internet Explorer浏览器要求使用".pdf"扩展名，因为它并不总是遵循声明的content-type。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @see AbstractPdfStamperView
 */
public abstract class AbstractPdfView extends AbstractView {

	/**
	 * 此构造函数设置正确的内容类型"application/pdf"。
	 * 请注意IE浏览器不会完全遵循此设置，但我们对此能做的也很有限。
	 * 生成的文档应具有".pdf"扩展名。
	 */
	public AbstractPdfView() {
		setContentType("application/pdf");
	}


	@Override
	protected boolean generatesDownloadContent() {
		return true;
	}

	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// IE解决方案：先写入字节数组。
		ByteArrayOutputStream baos = createTemporaryOutputStream();

		// Apply preferences and build metadata.
		Document document = newDocument();
		PdfWriter writer = newWriter(document, baos);
		prepareWriter(model, writer, request);
		buildPdfMetadata(model, document, request);

		// 构建PDF文档。
		document.open();
		buildPdfDocument(model, document, writer, request, response);
		document.close();

		// 刷新到HTTP响应。
		writeToResponse(response, baos);
	}

	/**
	 * 创建一个用于承载PDF内容的新文档。
	 * <p>
	 *     默认返回A4尺寸文档，但子类可指定任意Document，并可通过View中定义的bean属性进行参数化配置。
	 * @return 新创建的iText Document实例
	 * @see com.lowagie.text.Document#Document(com.lowagie.text.Rectangle)
	 */
	protected Document newDocument() {
		return new Document(PageSize.A4);
	}

	/**
	 * 为指定的iText文档创建新的PdfWriter。
	 * @param document 需要创建写入器的iText文档
	 * @param os 要写入的输出流
	 * @return 要使用的PdfWriter实例
	 * @throws DocumentException 写入器创建过程中抛出异常时
	 */
	protected PdfWriter newWriter(Document document, OutputStream os) throws DocumentException {
		return PdfWriter.getInstance(document, os);
	}

	/**
	 * 准备给定的PdfWriter。在构建PDF文档之前调用，即在调用{@code Document.open()}之前。
	 *
	 * <p>
	 *     可用于注册页面事件监听器等场景。
	 *     默认实现设置由此类的{@code getViewerPreferences()}方法返回的查看器偏好。
	 *
	 * @param model 模型，用于从中填充元信息
	 * @param writer 要准备的PdfWriter
	 * @param request 用于获取区域设置等（不应查看属性）
	 * @throws DocumentException 如果写入器准备过程中抛出异常
	 * @see com.lowagie.text.Document#open()
	 * @see com.lowagie.text.pdf.PdfWriter#setPageEvent
	 * @see com.lowagie.text.pdf.PdfWriter#setViewerPreferences
	 * @see #getViewerPreferences()
	 */
	protected void prepareWriter(Map<String, Object> model, PdfWriter writer, HttpServletRequest request)
			throws DocumentException {

		writer.setViewerPreferences(getViewerPreferences());
	}

	/**
	 * 返回PDF文件的查看器偏好设置。
	 *
	 * <p>
	 *     默认返回{@code AllowPrinting}（允许打印）和 {@code PageLayoutSinglePage}（单页布局），但可由子类重写。
	 *     子类可以设置固定偏好或从View定义的bean属性中获取。
	 *
	 * @return 包含PdfWriter定义位信息的整型值
	 * @see com.lowagie.text.pdf.PdfWriter#AllowPrinting
	 * @see com.lowagie.text.pdf.PdfWriter#PageLayoutSinglePage
	 */
	protected int getViewerPreferences() {
		return PdfWriter.ALLOW_PRINTING | PdfWriter.PageLayoutSinglePage;
	}

	/**
	 * 填充iText文档的元数据字段（作者、标题等）。
	 *
	 * <p>
	 *    默认为空实现。
	 *    子类可重写此方法来添加标题、主题、作者、创建者、关键词等元数据字段。
	 *    此方法在为Document分配PdfWriter之后、调用{@code document.open()}之前被调用。
	 *
	 * @param model 模型数据，用于从中提取元信息
	 * @param document 正在被填充的iText文档
	 * @param request 用于获取区域设置等信息（不应查看属性）
	 * @see com.lowagie.text.Document#addTitle
	 * @see com.lowagie.text.Document#addSubject
	 * @see com.lowagie.text.Document#addKeywords
	 * @see com.lowagie.text.Document#addAuthor
	 * @see com.lowagie.text.Document#addCreator
	 * @see com.lowagie.text.Document#addProducer
	 * @see com.lowagie.text.Document#addCreationDate
	 * @see com.lowagie.text.Document#addHeader
	 */
	protected void buildPdfMetadata(Map<String, Object> model, Document document, HttpServletRequest request) {
	}

	/**
	 * 子类必须实现此方法来构建iText PDF文档，根据提供的模型数据。
	 * 在{@code Document.open()}和{@code Document.close()}调用之间执行。
	 * <p>
	 *     注意：传入的HTTP响应仅应用于设置cookie或其他HTTP头部信息。
	 *     构建的PDF文档本身将在该方法返回后自动写入响应。
	 *
	 * @param model 模型Map
	 * @param document 要添加元素的iText Document对象
	 * @param writer 要使用的PdfWriter
	 * @param request 用于获取区域设置等（不应查看属性）
	 * @param response 用于设置cookie（不应直接写入响应）
	 * @throws Exception 文档构建过程中发生的任何异常
	 * @see com.lowagie.text.Document#open()
	 * @see com.lowagie.text.Document#close()
	 */
	protected abstract void buildPdfDocument(Map<String, Object> model, Document document, PdfWriter writer,
			HttpServletRequest request, HttpServletResponse response) throws Exception;

}
