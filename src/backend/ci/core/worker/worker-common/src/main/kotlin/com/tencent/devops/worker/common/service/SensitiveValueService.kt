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

package com.tencent.devops.worker.common.service

import org.slf4j.LoggerFactory

object SensitiveValueService {

    private val logger = LoggerFactory.getLogger(SensitiveValueService::class.java)
    private const val SENSITIVE_MIXER = "******"

    /**
     * 每个Job内维护的敏感信息集合
     */
    val sensitiveStringSet = mutableSetOf<String>()

    fun addSensitiveValues(sensitiveValues: List<String>?) {
        sensitiveValues?.let {
            logger.info("Append sensitive string set")
            sensitiveStringSet.addAll(it)
            logger.info("Sensitive string set size: ${sensitiveStringSet.size}")
        }
    }

    fun matchSensitiveValue(value: String): Boolean {
        sensitiveStringSet.forEach { sensitive ->
            if (value.contains(sensitive)) return true
        }
        return false
    }

    fun fixSensitiveContent(message: String): String {
        var realMessage = message
        sensitiveStringSet.forEach { sensitiveStr ->
            if (realMessage.contains(sensitiveStr)) {
                realMessage = realMessage.replace(sensitiveStr, SENSITIVE_MIXER)
            }
        }
        return realMessage
    }
}
