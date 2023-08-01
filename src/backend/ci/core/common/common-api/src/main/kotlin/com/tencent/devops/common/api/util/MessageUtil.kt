/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.util

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.pojo.FieldLocaleInfo
import com.tencent.devops.common.api.pojo.I18nFieldInfo
import java.lang.reflect.Field
import java.text.MessageFormat
import java.util.Locale
import java.util.Properties
import java.util.ResourceBundle
import org.slf4j.LoggerFactory
import java.net.URLDecoder

object MessageUtil {

    private val logger = LoggerFactory.getLogger(MessageUtil::class.java)
    private const val DEFAULT_BASE_NAME = "i18n/message"

    /**
     * 根据语言环境获取对应的描述信息
     * @param messageCode 消息标识
     * @param language 语言信息
     * @param params 替换描述信息占位符的参数数组
     * @param baseName 基础资源名称
     * @param defaultMessage 默认信息
     * @return 描述信息
     */
    fun getMessageByLocale(
        messageCode: String,
        language: String,
        params: Array<String>? = null,
        baseName: String = DEFAULT_BASE_NAME,
        defaultMessage: String? = null,
        checkUrlDecoder: Boolean = false
    ): String {
        var message: String? = null
        try {
            val parts = language.split("_")
            val localeObj = if (parts.size > 1) {
                Locale(parts[0], parts[1])
            } else {
                Locale(language)
            }
            // 根据locale和baseName生成resourceBundle对象
            val resourceBundle = ResourceBundle.getBundle(baseName, localeObj)
            // 通过resourceBundle获取对应语言的描述信息
            message = String(resourceBundle.getString(messageCode).toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
        } catch (ignored: Throwable) {
            logger.warn("Fail to get i18nMessage of messageCode[$messageCode]")
        }
        if (null != params && null != message) {
            val mf = MessageFormat(message)
            // 根据参数动态替换状态码描述里的占位符
            message = mf.format(params)
        }
        val res = message ?: defaultMessage ?: ""
        return if (checkUrlDecoder) URLDecoder.decode(res, Charsets.UTF_8.name()) else res
    }

    /**
     * 获取国际化资源文件特性对象
     * @param fileStr 国际化资源文件内容
     * @return 国际化资源文件特性对象
     */
    fun getMessageProperties(fileStr: String): Properties {
        val properties = Properties()
        properties.load(fileStr.reader())
        return properties
    }

    /**
     * 遍历map集合获取字段列表
     * @param dataMap map集合
     * @param keyPrefix 字段key前缀
     * @param properties 国际化资源文件特性对象
     * @return 字段列表
     */
    @Suppress("UNCHECKED_CAST")
    fun traverseMap(
        dataMap: MutableMap<String, Any>,
        keyPrefix: String? = null,
        properties: Properties? = null
    ): MutableList<FieldLocaleInfo> {
        val fieldLocaleInfos = mutableListOf<FieldLocaleInfo>()
        dataMap.forEach { (key, value) ->
            val dataKey = if (!keyPrefix.isNullOrBlank()) {
                // 如果字段key前缀不为空，需为key加上前缀
                "$keyPrefix.$key"
            } else {
                key
            }
            when (value) {
                is Map<*, *> -> {
                    // value类型为map，需要递归遍历value获取字段列表
                    fieldLocaleInfos.addAll(
                        traverseMap(
                            dataMap = value as MutableMap<String, Any>,
                            keyPrefix = dataKey,
                            properties = properties
                        )
                    )
                }

                is List<*> -> {
                    // value类型为list，需要递归遍历value获取字段列表
                    fieldLocaleInfos.addAll(
                        traverseList(
                            dataList = value as MutableList<Any>,
                            keyPrefix = dataKey,
                            properties = properties
                        )
                    )
                }

                else -> {
                    val keyValue = dataMap[key]
                    val propertyValue = properties?.get(dataKey)?.toString()
                    // 如果properties参数不为空则进行国际化内容替换(只有值为字符串的字段才需要做国际化)
                    if (keyValue is String && !propertyValue.isNullOrBlank()) {
                        dataMap[key] = propertyValue
                        // 如果value不是集合类型则直接加入字段列表中
                        fieldLocaleInfos.add(FieldLocaleInfo(dataKey, keyValue.toString()))
                    }
                }
            }
        }
        return fieldLocaleInfos
    }

    /**
     * 遍历list集合获取国际化字段列表
     * @param dataList list集合
     * @param keyPrefix 字段key前缀
     * @param properties 国际化资源文件特性对象
     * @return 字段列表
     */
    @Suppress("UNCHECKED_CAST")
    fun traverseList(
        dataList: MutableList<Any>,
        keyPrefix: String? = null,
        properties: Properties? = null
    ): MutableList<FieldLocaleInfo> {
        val fieldLocaleInfos = mutableListOf<FieldLocaleInfo>()
        dataList.forEachIndexed { index, value ->
            val dataKey = if (!keyPrefix.isNullOrBlank()) {
                // 如果字段key前缀不为空，需为key加上前缀
                "$keyPrefix[$index]"
            } else {
                keyPrefix
            }
            when (value) {
                is Map<*, *> -> {
                    // value类型为map，需要递归遍历value获取字段列表
                    fieldLocaleInfos.addAll(
                        traverseMap(
                            dataMap = value as MutableMap<String, Any>,
                            keyPrefix = dataKey,
                            properties = properties
                        )
                    )
                }

                is List<*> -> {
                    // value类型为list，需要递归遍历value获取字段列表
                    fieldLocaleInfos.addAll(
                        traverseList(
                            dataList = value as MutableList<Any>,
                            keyPrefix = dataKey,
                            properties = properties
                        )
                    )
                }

                else -> {
                    if (!dataKey.isNullOrBlank()) {
                        val keyValue = dataList[index]
                        val propertyValue = properties?.get(dataKey)?.toString()
                        // 如果properties参数不为空则进行国际化内容替换(只有值为字符串的字段才需要做国际化)
                        if (keyValue is String && !propertyValue.isNullOrBlank()) {
                            dataList[index] = propertyValue
                            // 如果value不是集合类型则直接加入字段列表中
                            fieldLocaleInfos.add(FieldLocaleInfo(dataKey, keyValue.toString()))
                        }
                    }
                }
            }
        }
        return fieldLocaleInfos
    }

    /**
     * 从实体对象中获取需要进行国际化翻译的字段集合
     * @param entity 实体对象
     * @param fieldPath 字段路径
     * @return 需要进行国际化翻译的字段集合
     */
    fun getBkI18nFieldMap(
        entity: Any,
        fieldPath: String = ""
    ): MutableMap<String, I18nFieldInfo> {
        val bkI18nFieldMap = mutableMapOf<String, I18nFieldInfo>()
        when (entity) {
            is List<*> -> {
                // entity如果是list集合，需遍历集合中的对象收集国际化字段信息
                entity.forEachIndexed { index, itemEntity ->
                    handleItemEntityI18nInfo(
                        fieldPath = fieldPath,
                        index = index,
                        itemEntity = itemEntity,
                        bkI18nFieldMap = bkI18nFieldMap
                    )
                }
            }

            is Set<*> -> {
                // entity如果是set集合，为了保证字段有序，需先将其转换为有序的list集合然后遍历集合中的对象收集国际化字段信息
                entity.toList().sortedBy {
                    it?.let { ShaUtils.sha1InputStream(JsonUtil.toJson(it, false).byteInputStream()) }
                }.forEachIndexed { index, itemEntity ->
                    handleItemEntityI18nInfo(
                        fieldPath = fieldPath,
                        index = index,
                        itemEntity = itemEntity,
                        bkI18nFieldMap = bkI18nFieldMap
                    )
                }
            }

            else -> {
                doCommonEntityBus(entity, fieldPath, bkI18nFieldMap)
            }
        }
        return bkI18nFieldMap
    }

    private fun doCommonEntityBus(
        entity: Any,
        fieldPath: String,
        bkI18nFieldMap: MutableMap<String, I18nFieldInfo>
    ) {
        val entityClass = entity::class
        // 统计出返回对象需要进行国际化翻译的字段
        entityClass.java.declaredFields.forEach { dataField ->
            val dataFieldName = dataField.name
            // 生成完整字段路径
            val fullFieldPath = if (fieldPath.isNotBlank()) {
                "$fieldPath.$dataFieldName"
            } else {
                dataFieldName
            }
            // 判断字段上是否有BkFieldI18n注解，有该注解的字段才需要做国际化替换
            val bkFieldI18nAnnotation =
                dataField.annotations.firstOrNull { it is BkFieldI18n } as? BkFieldI18n ?: return@forEach
            // 获取字段的值，如果字段的值为空则无需进行国际化替换
            val dataFieldValue = getFieldValue(dataField, entity)
            if (dataFieldValue?.toString().isNullOrBlank()) {
                return@forEach
            }
            if (ReflectUtil.isNativeType(dataFieldValue!!) || dataFieldValue is String ||
                dataFieldValue is Enum<*>
            ) {
                // 如果字段的值是基本类型则把该字段放入需要国际化翻译的集合中
                val i18nFieldInfo = I18nFieldInfo(
                    field = dataField,
                    entity = entity,
                    source = bkFieldI18nAnnotation.source,
                    translateType = bkFieldI18nAnnotation.translateType,
                    keyPrefixName = bkFieldI18nAnnotation.keyPrefixName,
                    reusePrefixFlag = bkFieldI18nAnnotation.reusePrefixFlag,
                    convertName = bkFieldI18nAnnotation.convertName
                )
                bkI18nFieldMap[fullFieldPath] = i18nFieldInfo
            } else {
                // 如果字段的值不是基本类型则进行递归收集国际化字段处理
                bkI18nFieldMap.putAll(getBkI18nFieldMap(entity = dataFieldValue, fieldPath = fullFieldPath))
            }
        }
    }

    /**
     * 获取字段的值
     * @param field 字段
     * @param entity 实体对象
     * @return 字段值
     */
    fun getFieldValue(field: Field, entity: Any): Any? {
        // 判断字段是否可以访问
        if (!field.isAccessible) {
            // 设置字段为可访问
            field.isAccessible = true
        }
        return field.get(entity)
    }

    /**
     * 处理实体对象的国际化信息
     * @param fieldPath 字段路径
     * @param index 字段数组下标
     * @param itemEntity 实体对象
     * @param bkI18nFieldMap 需要国际化翻译的字段的map集合
     */
    private fun handleItemEntityI18nInfo(
        fieldPath: String,
        index: Int,
        itemEntity: Any?,
        bkI18nFieldMap: MutableMap<String, I18nFieldInfo>
    ) {
        // 把实体对象的国际化字段信息放入集合中
        itemEntity?.let {
            // 生成字段路径
            val newFieldPath = if (fieldPath.isNotBlank()) {
                "$fieldPath[$index]"
            } else {
                "[$index]"
            }
            bkI18nFieldMap.putAll(getBkI18nFieldMap(entity = itemEntity, fieldPath = newFieldPath))
        }
    }

    /**
     * 获取前缀名称中的动态参数
     * @param prefixName 前缀名称
     * @return 动态参数
     */
    fun getPrefixNameVar(prefixName: String): String? {
        val regex = Regex("\\{(.*)}")
        val matchResult = regex.find(prefixName)
        return matchResult?.groupValues?.get(1)
    }
}
