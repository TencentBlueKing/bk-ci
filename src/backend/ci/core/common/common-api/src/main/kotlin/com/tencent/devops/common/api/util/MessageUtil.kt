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
import java.util.Locale
import java.util.ResourceBundle

object MessageUtil {

    /**
     * 根据语言环境获取对应的描述信息
     * @param messageCode 消息标识
     * @param locale 语言环境
     * @param baseName 基础资源名称
     * @return 描述信息
     */
    fun getMessageByLocale(
        messageCode: String,
        locale: String,
        baseName: String = "i18n/message"
    ): String {
        val localeObj = Locale(locale)
        // 根据locale和baseName生成resourceBundle对象
        val resourceBundle = ResourceBundle.getBundle(baseName, localeObj)
        // 通过resourceBundle获取对应语言的描述信息
        return String(resourceBundle.getString(messageCode).toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
    }

    /**
     * 遍历map集合获取字段列表
     * @param dataMap map集合
     * @param fieldLocaleInfoList 字段集合
     * @param keyPrefix 字段key前缀
     * @return 字段列表
     */
    fun traverseMap(
        dataMap: Map<*, *>,
        fieldLocaleInfoList: MutableList<FieldLocaleInfo>,
        keyPrefix: String? = null
    ): MutableList<FieldLocaleInfo> {
        dataMap.forEach { (key, value) ->
            val dataKey = if (!keyPrefix.isNullOrBlank()) {
                // 如果字段key前缀不为空，需为key加上前缀
                "$keyPrefix.$key"
            } else {
                key.toString()
            }
            when (value) {
                is Map<*, *> -> {
                    // value类型为map，需要递归遍历value获取字段列表
                    traverseMap(value, fieldLocaleInfoList, dataKey)
                }

                is List<*> -> {
                    // value类型为list，需要递归遍历value获取字段列表
                    traverseList(value, fieldLocaleInfoList, dataKey)
                }

                else -> {
                    // 如果value不是集合类型则直接加入字段列表中
                    fieldLocaleInfoList.add(FieldLocaleInfo(dataKey, value.toString()))
                }
            }
        }
        return fieldLocaleInfoList
    }

    /**
     * 遍历list集合获取字段列表
     * @param dataList list集合
     * @param fieldLocaleInfoList 字段集合
     * @param keyPrefix 字段key前缀
     * @return 字段列表
     */
    fun traverseList(
        dataList: List<*>,
        fieldLocaleInfoList: MutableList<FieldLocaleInfo>,
        keyPrefix: String? = null
    ): MutableList<FieldLocaleInfo> {
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
                    traverseMap(value, fieldLocaleInfoList, dataKey)
                }

                is List<*> -> {
                    // value类型为list，需要递归遍历value获取字段列表
                    traverseList(value, fieldLocaleInfoList, dataKey)
                }

                else -> {
                    if (!dataKey.isNullOrBlank()) {
                        // 如果value不是集合类型则直接加入字段列表中
                        fieldLocaleInfoList.add(FieldLocaleInfo(dataKey, value.toString()))
                    }
                }
            }
        }
        return fieldLocaleInfoList
    }
}
