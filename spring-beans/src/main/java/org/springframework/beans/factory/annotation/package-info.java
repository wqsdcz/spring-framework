/**
 * Support package for annotation-driven bean configuration.
 */
@NonNullApi
@NonNullFields
package org.springframework.beans.factory.annotation;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;

/**
 * 注解：
 * @Autowired     —— 字段、参数、构造器、方法
 * @Configurable  —— 允许对‌非Spring容器管理的对象‌进行Spring依赖注入。
 * @Lookup      —— 注入原型Bean
 * @Qualifier   —— 指定注入Bean的beanName
 * @Required    —— 方法
 * @Value       —— 字段、参数、方法
 *
 * Bean的定义：
 * InjectionMetadata
 * AnnotatedBeanDefinition
 * AnnotatedGenericBeanDefinition
 *
 * 附加处理器：
 * CustomAutowireConfigurer
 * AutowiredAnnotationBeanPostProcessor
 * RequiredAnnotationBeanPostProcessor
 * InitDestroyAnnotationBeanPostProcessor
 *
 * AnnotationBeanWiringInfoResolver
 * QualifierAnnotationAutowireCandidateResolver
 *
 *
 */