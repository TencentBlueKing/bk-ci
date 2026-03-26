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

package com.tencent.devops.store.util

import com.tencent.devops.common.api.util.JsonUtil
import org.slf4j.LoggerFactory

/**
 * 服务范围工具类
 * 用于处理 SERVICE_SCOPE 字段的大小写兼容性问题
 *
 * 统一使用大写格式存储，如 ["PIPELINE"、"CREATIVE_STREAM"]
 * 支持读取小写格式的数据，但统一转换为大写格式存储
 */
object ServiceScopeUtil {

    private val logger = LoggerFactory.getLogger(ServiceScopeUtil::class.java)

    /**
     * 从JSON字符串解析服务范围列表（自动标准化）
     *
     * @param serviceScopeJson 服务范围JSON字符串
     * @return 标准化后的服务范围列表（统一大写）
     */
    @Suppress("UNCHECKED_CAST")
    fun parseServiceScopes(serviceScopeJson: String?): List<String> {
        if (serviceScopeJson.isNullOrBlank()) return emptyList()

        try {
            val serviceScopes =
                JsonUtil.toOrNull(serviceScopeJson, List::class.java) as? List<String> ?: return emptyList()

            return normalizeList(serviceScopes)
        } catch (ignored: Throwable) {
            logger.warn("Failed to parse serviceScope: $serviceScopeJson", ignored)
            return emptyList()
        }
    }

    /**
     * 标准化服务范围值（统一转换为大写）
     *
     * @param value 原始值，支持 "PIPELINE"、"pipeline"、"Pipeline" 等
     * @return 标准化后的大写值，如果无法识别则返回原值的大写形式
     */
    fun normalize(value: String?): String? {
        if (value.isNullOrBlank()) return value

        // 已知的服务范围值，统一转换为大写
        return value.uppercase()
    }

    /**
     * 标准化服务范围列表（统一转换为大写）
     *
     * @param values 原始值列表
     * @return 标准化后的大写值列表
     */
    fun normalizeList(values: List<String>?): List<String> {
        if (values.isNullOrEmpty()) return emptyList()
        return values.mapNotNull { normalize(it) }
    }

    /**
     * 将服务范围列表转换为JSON字符串（统一使用大写）
     *
     * @param serviceScopes 服务范围列表
     * @return JSON字符串，如 ["PIPELINE","CREATIVE_STREAM"]
     */
    fun toJson(serviceScopes: List<String>?): String {
        if (serviceScopes.isNullOrEmpty()) return "[]"
        val normalized = normalizeList(serviceScopes)
        return JsonUtil.toJson(normalized, formatted = false)
    }

    /**
     * 检查服务范围列表是否为空或无效
     *
     * @param serviceScopeJson 服务范围JSON字符串
     * @return 是否为空或无效
     */
    fun isEmpty(serviceScopeJson: String?): Boolean {
        return parseServiceScopes(serviceScopeJson).isEmpty()
    }
}
