/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.common.api.context.ChannelContext
import com.tencent.devops.common.api.pojo.FieldLocaleInfo
import com.tencent.devops.common.api.pojo.I18nFieldInfo
import java.lang.reflect.Field
import java.net.URLDecoder
import java.text.MessageFormat
import java.util.Locale
import java.util.Properties
import java.util.ResourceBundle
import org.slf4j.LoggerFactory

object MessageUtil {

    private val logger = LoggerFactory.getLogger(MessageUtil::class.java)
    private const val DEFAULT_BASE_NAME = "i18n/message"

    /** 流水线/创作流 关键字在国际化资源中的 key，用于根据渠道替换描述中的称谓（与 ChannelCode.CREATIVE_STREAM 渠道配合） */
    private const val PIPELINE_KEYWORD_PIPELINE = "pipelineKeyword.pipeline"
    private const val PIPELINE_KEYWORD_CREATIVE_STREAM = "pipelineKeyword.creativeStream"
    private const val CHANNEL_CREATIVE_STREAM = "CREATIVE_STREAM"

    /**
     * 根据语言环境获取对应的描述信息
     * 当请求渠道为 CREATIVE_STREAM时，会将描述中的「流水线」关键字替换为「创作流」
     * （替换文案来自 pipelineKeyword.pipeline / pipelineKeyword.creativeStream 的国际化）。
     *
     * 渠道来源：优先使用参数 [channel]；若为 null 则使用 [ChannelContext.getChannel()]。
     * - HTTP 请求：由 Filter 设置 Context，一般无需传 [channel]。
     * - 异步线程/MQ/转发等：Context 可能为空，调用方应传入 [channel]（如 event.channelCode.name）
     *   或在入口处使用 [ChannelContext.withChannel]包裹。
     *
     * @param messageCode 消息标识
     * @param language 语言信息
     * @param params 替换描述信息占位符的参数数组
     * @param baseName 基础资源名称
     * @param defaultMessage 默认信息
     * @param channel 可选，渠道标识（与 ChannelCode.name 一致）。为 null 时从 ChannelContext 读取，用于异步/MQ 等无请求上下文的场景
     * @return 描述信息
     */
    fun getMessageByLocale(
        messageCode: String,
        language: String,
        params: Array<String>? = null,
        baseName: String = DEFAULT_BASE_NAME,
        defaultMessage: String? = null,
        checkUrlDecoder: Boolean = false,
        channel: String? = null
    ): String {
        var message: String? = null
        try {
            val parts = language.split("_")
            val localeObj = if (parts.size > 1) {
                Locale(parts[0], parts[1])
            } else {
                Locale(language)
            }
            val resourceBundle = ResourceBundle.getBundle(baseName, localeObj)
            message = resourceBundle.getString(messageCode)
            // 请求渠道为创作流时，将描述中的「流水线」替换为「创作流」
            message = replaceKeywordByChannel(resourceBundle, message, messageCode, channel)
            if (!message.isNullOrBlank() && !params.isNullOrEmpty()) {
                val mf = MessageFormat(message)
                message = mf.format(params)
            }
        } catch (ignored: Throwable) {
            logger.warn("Fail to get i18nMessage of messageCode[$messageCode]")
        }
        val res = message ?: defaultMessage ?: ""
        return if (checkUrlDecoder) URLDecoder.decode(res, Charsets.UTF_8.name()) else res
    }

    /**
     * 根据请求渠道替换普通文本消息中的关键字（公开方法）。
     *
     * 与 [getMessageByLocale] 中内置的关键字替换不同，本方法用于非国际化资源场景——
     * 例如 notify 模块从 DB 模板获取的通知标题和正文。
     * 渠道取值：优先使用 [channel]；若为 null 则从 [ChannelContext.getChannel()] 获取。
     *
     * @param message  待处理的文本，可能为 null 或空白
     * @param language 语言标识（如 zh_CN），用于从国际化资源中获取关键字本地化文本
     * @param channel  可选，渠道标识（与 ChannelCode.name 一致）；为 null 时从 ChannelContext 读取
     * @return 替换后的文本；若渠道不匹配、消息为空或发生异常，则原样返回
     */
    fun replaceKeywordByChannel(
        message: String?,
        language: String,
        channel: String? = null
    ): String? {
        val effectiveChannel = channel ?: ChannelContext.getChannel()
        if (effectiveChannel != CHANNEL_CREATIVE_STREAM || message.isNullOrBlank()) {
            return message
        }
        return try {
            val parts = language.split("_")
            val localeObj = if (parts.size > 1) Locale(parts[0], parts[1]) else Locale(language)
            val resourceBundle = ResourceBundle.getBundle(DEFAULT_BASE_NAME, localeObj)
            val pipelineKeyword = resourceBundle.getString(PIPELINE_KEYWORD_PIPELINE)
            val creativeStreamKeyword = resourceBundle.getString(PIPELINE_KEYWORD_CREATIVE_STREAM)
            if (pipelineKeyword.isNotBlank() && creativeStreamKeyword.isNotBlank() &&
                message.contains(pipelineKeyword)
            ) {
                message.replace(pipelineKeyword, creativeStreamKeyword)
            } else {
                message
            }
        } catch (ignored: Throwable) {
            logger.warn("Replace pipeline keyword by channel skip: message=$message", ignored)
            message
        }
    }

    /**
     * 根据请求渠道替换消息中的关键字（内部方法，接收 ResourceBundle）。
     *
     * 背景：蓝盾平台存在多个接入渠道（详见 ChannelCode），其中 CREATIVE_STREAM（创作流）渠道
     * 需要将国际化消息中出现的「流水线」字样统一替换为「创作流」，以适配该渠道的产品术语。
     *
     * 渠道取值：优先使用 [channelOverride]；为 null 时使用 [ChannelContext.getChannel()]。
     * 异步/MQ 等场景下 Context 常为空，调用 [getMessageByLocale] 时建议传入 [channel] 或入口处使用 [ChannelContext.withChannel]。
     *
     * @param resourceBundle 当前语言环境对应的国际化资源包，包含关键字的本地化文本
     * @param message        待处理的国际化消息文本，可能为 null 或空白
     * @param messageCode    消息编码，仅用于异常时的日志记录，方便定位问题
     * @param channelOverride 可选，渠道标识（与 ChannelCode.name 一致）；为 null 时从 ChannelContext 读取
     * @return 替换后的消息文本；若渠道不匹配、消息为空、或替换过程中发生异常，则原样返回
     */
    private fun replaceKeywordByChannel(
        resourceBundle: ResourceBundle,
        message: String?,
        messageCode: String,
        channelOverride: String? = null
    ): String? {
        val effectiveChannel = channelOverride ?: ChannelContext.getChannel()
        if (effectiveChannel != CHANNEL_CREATIVE_STREAM || message.isNullOrBlank()) {
            return message
        }
        return try {
            // 从国际化资源包中分别获取「流水线」和「创作流」对应当前语言的本地化文本
            val pipelineKeyword = resourceBundle.getString(PIPELINE_KEYWORD_PIPELINE)
            val creativeStreamKeyword = resourceBundle.getString(PIPELINE_KEYWORD_CREATIVE_STREAM)
            // 仅当两个关键字均非空白且消息中确实包含「流水线」关键字时，才执行替换
            if (pipelineKeyword.isNotBlank() && creativeStreamKeyword.isNotBlank() &&
                message.contains(pipelineKeyword)
            ) {
                message.replace(pipelineKeyword, creativeStreamKeyword)
            } else {
                // 关键字为空或消息中不包含目标关键字，无需替换，原样返回
                message
            }
        } catch (ignored: Throwable) {
            // 资源包中可能不存在对应 key（MissingResourceException）等异常情况，返回原消息
            logger.warn("Replace pipeline keyword by channel skip: messageCode=$messageCode", ignored)
            message
        }
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
