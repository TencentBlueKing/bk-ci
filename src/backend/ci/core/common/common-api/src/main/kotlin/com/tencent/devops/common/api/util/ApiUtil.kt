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

import org.hashids.Hashids
import java.net.URI
import java.net.URLEncoder
import java.util.Random

/**
 *
 * Powered By Tencent
 */
object ApiUtil {
    private const val HASH_SALT = "soda3:com.tencent.soda.open.api.access.key.salt"
    private val hashids = Hashids(HASH_SALT, 8, "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789")
    private const val secretSeed = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    fun accessKey(projectId: Long): String {
        return hashids.encode(projectId)
    }

    fun randomSecretKey(): String {
        val random = Random()
        val buf = StringBuffer()
        for (i in 0 until 31) {
            val num = random.nextInt(secretSeed.length)
            buf.append(secretSeed[num])
        }
        return buf.toString()
    }

    /**
    * 向 URL 安全添加或更新 Query 参数
    * @param originalUrl 原始 URL
    * @param paramName 参数名
    * @param paramValue 参数值（自动编码）
    * @return 处理后的完整 URL
    */
    fun appendUrlQueryParam(
        originalUrl: String,
        paramName: String,
        paramValue: String
    ): String {
        // 分解原始 URI
        val uri = URI.create(originalUrl)
        val encodedName = URLEncoder.encode(paramName, Charsets.UTF_8.name())
        val encodedValue = URLEncoder.encode(paramValue, Charsets.UTF_8.name())

        // 解析现有参数到 Map
        val params = uri.query?.split("&")
            ?.associateTo(mutableMapOf()) {
                it.split("=", limit = 2).let { parts ->
                    parts.first() to parts.getOrNull(1)
                }
            } ?: mutableMapOf()

        // 更新参数
        params[encodedName] = encodedValue

        // 构建新 Query
        val newQuery = params.entries.joinToString("&") { (k, v) ->
            if (v != null) "$k=$v" else k
        }

        // 重组 URI
        return URI(
            uri.scheme,
            uri.authority,
            uri.rawPath,
            newQuery.ifEmpty { null },
            uri.fragment
        ).toString()
    }
}
