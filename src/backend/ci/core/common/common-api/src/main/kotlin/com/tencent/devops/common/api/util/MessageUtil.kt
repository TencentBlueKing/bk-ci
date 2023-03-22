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

import com.tencent.devops.common.api.pojo.FieldLocaleInfo
import java.text.MessageFormat
import java.util.*

object MessageUtil {

    private const val DEFAULT_BASE_NAME = "i18n/message"

    /**
     * 根据语言环境获取对应的描述信息
     * @param messageCode 消息标识
     * @param language 语言信息
     * @param params 替换描述信息占位符的参数数组
     * @param baseName 基础资源名称
     * @return 描述信息
     */
    fun getMessageByLocale(
        messageCode: String,
        language: String,
        params: Array<String>? = null,
        baseName: String = DEFAULT_BASE_NAME
    ): String {
        val localeObj = Locale(language)
        // 根据locale和baseName生成resourceBundle对象
        val resourceBundle = ResourceBundle.getBundle(baseName, localeObj)
        // 通过resourceBundle获取对应语言的描述信息
        var message = String(resourceBundle.getString(messageCode).toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
        if (null != params) {
            val mf = MessageFormat(message)
            // 根据参数动态替换状态码描述里的占位符
            message = mf.format(params)
        }
        return message
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
                    properties?.let {
                        val propertyValue = properties[dataKey]?.toString()
                        // 如果properties参数不为空则进行国际化内容替换
                        propertyValue?.let { dataMap[key] = propertyValue }
                    }
                    // 如果value不是集合类型则直接加入字段列表中
                    fieldLocaleInfos.add(FieldLocaleInfo(dataKey, dataMap[key].toString()))
                }
            }
        }
        return fieldLocaleInfos
    }

    /**
     * 遍历list集合获取字段列表
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
                        properties?.let {
                            val propertyValue = properties[dataKey]?.toString()
                            // 如果properties参数不为空则进行国际化内容替换
                            propertyValue?.let { dataList[index] = propertyValue }
                        }
                        // 如果value不是集合类型则直接加入字段列表中
                        fieldLocaleInfos.add(FieldLocaleInfo(dataKey, dataList[index].toString()))
                    }
                }
            }
        }
        return fieldLocaleInfos
    }
}
