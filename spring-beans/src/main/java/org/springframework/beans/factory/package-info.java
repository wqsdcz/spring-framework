/**
 * The core package implementing Spring's lightweight Inversion of Control (IoC) container.
 *
 * <p>Provides an alternative to the Singleton and Prototype design
 * patterns, including a consistent approach to configuration management.
 * Builds on the org.springframework.beans package.
 *
 * <p>This package and related packages are discussed in Chapter 11 of
 * <a href="https://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson (Wrox, 2002).
 */
@NonNullApi
@NonNullFields
package org.springframework.beans.factory;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
/**
 *  Aware
 *  BeanNameAware    	 —— 对应 NamedBean
 *  BeanFactoryAware  	 —— 对应 BeanFactory
 *  BeanClassLoaderAware —— 对应 ClassLoader
 *
 *  NamedBean        —— 已命名的Bean
 *  FactoryBean      —— 工厂的Bean
 *  SmartFactoryBean —— 智能工厂的Bean
 *
 *  InitializingBean            —— 可初始化的Bean
 *  SmartInitializingSingleton  —— 可智能初始化的Bean
 *  DisposableBean              —— 可销毁的Bean
 *
 *  BeanFactory             —— Bean工厂
 *  ListableBeanFactory     —— 列表化的Bean工厂
 *  HierarchicalBeanFactory —— 层级式的Bean工厂
 *
 *  ObjectProvider  —— 对象提供者（特定于InjectionPoint 的 ObjectFactory的变体）
 *  ObjectFactory   —— 对象工厂
 *
 *  BeanFactoryUtils
 *
 *  InjectionPoint  —— 注入点
 *
 */
