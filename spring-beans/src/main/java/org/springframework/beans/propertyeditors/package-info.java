/**
 * Properties editors used to convert from String values to object
 * types such as java.util.Properties.
 *
 * <p>Some of these editors are registered automatically by BeanWrapperImpl.
 * "CustomXxxEditor" classes are intended for manual registration in
 * specific binding processes, as they are localized or the like.
 *
 * ZoneIdEditor   —— String 转 ZoneId
 * TimeZoneEditor —— String 转 TimeZone
 * UUIDEditor   —— String 转 UUID
 * URLEditor    —— String 转 URL
 * URIEditor    —— String 转 URI
 *
 *
 * StringTrimmerEditor        —— String 转 String（修剪掉指定字符串中的字符）
 * StringArrayPropertyEditor  —— String 转 String[] (已出指定字符穿中存在的字符、修剪)
 *
 *
 *
 */
@NonNullApi
@NonNullFields
package org.springframework.beans.propertyeditors;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
