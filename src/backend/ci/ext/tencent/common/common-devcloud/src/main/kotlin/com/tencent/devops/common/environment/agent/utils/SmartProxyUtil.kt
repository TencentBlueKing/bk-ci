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

package com.tencent.devops.common.environment.agent.utils

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.RandomStringUtils

/**
 * 智能网关工具类
 */
object SmartProxyUtil {

    fun makeHeaders(
        appId: String,
        token: String,
        staffName: String,
        proxyToken: String,
        projectId: String? = "",
        pipelineId: String? = ""
    ): Map<String, String> {
        val headerBuilder = mutableMapOf<String, String>()
        headerBuilder["APPID"] = appId
        val random = RandomStringUtils.randomAlphabetic(8)
        headerBuilder["RANDOM"] = random
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        headerBuilder["TIMESTP"] = timestamp
        headerBuilder["STAFFNAME"] = staffName
        val encKey = DigestUtils.md5Hex("$token$timestamp$random")
        headerBuilder["ENCKEY"] = encKey
        headerBuilder["TIMESTAMP"] = timestamp
        val staffId = "mock"
        headerBuilder["STAFFID"] = staffId
        headerBuilder["X-EXT-DATA"] = ""
        val seq = "mock"
        headerBuilder["X-RIO-SEQ"] = seq
        val signature = ShaUtils.sha256("$timestamp$proxyToken$seq,$staffId,$staffName,$timestamp")
        headerBuilder["SIGNATURE"] = signature.toUpperCase()
        headerBuilder["X-Project"] = projectId ?: ""
        headerBuilder["X-Pipeline-Id"] = pipelineId ?: ""
        headerBuilder[AUTH_HEADER_DEVOPS_PROJECT_ID] = projectId ?: ""
        headerBuilder[AUTH_HEADER_DEVOPS_PIPELINE_ID] = pipelineId ?: ""

        return headerBuilder
    }

    fun makeIdcProxyHeaders(
        appId: String,
        token: String,
        userId: String
    ): Map<String, String> {
        val headerBuilder = mutableMapOf<String, String>()
        headerBuilder["APPID"] = appId
        val random = RandomStringUtils.randomAlphabetic(8)
        headerBuilder["RANDOM"] = random
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        headerBuilder["TIMESTP"] = timestamp
        val encKey = DigestUtils.md5Hex("$token$timestamp$random")
        headerBuilder["ENCKEY"] = encKey
        headerBuilder["TIMESTAMP"] = timestamp
        headerBuilder["X-STAFFNAME"] = userId

        return headerBuilder
    }
}
