/**
 * 实现Spring轻量级控制反转(IoC)容器的核心包。
 * The core package implementing Spring's lightweight Inversion of Control (IoC) container.
 *
 * <p>提供了单例和原型设计模式的替代方案，
 * 包括一致的配置管理方法。
 * Provides an alternative to the Singleton and Prototype design
 * patterns, including a consistent approach to configuration management.
 * 建立在org.springframework.beans包之上。
 * Builds on the org.springframework.beans package.
 *
 * <p>此包及相关包在Rod Johnson所著
 * <a href="https://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * 一书的第11章中讨论。
 * This package and related packages are discussed in Chapter 11 of
 * <a href="https://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson (Wrox, 2002).
 */
@NonNullApi
@NonNullFields
package org.springframework.beans.factory;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
