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

package com.tencent.devops.misc.service.process

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.misc.pojo.BasicAuthProperties
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@EnableConfigurationProperties(BasicAuthProperties::class)
@Primary
@Service
class TxProcessRelatedPlatformDataClearServiceImpl(
    properties: BasicAuthProperties
) : ProcessRelatedPlatformDataClearService {

    private val basicAuths = properties.basicAuths

    override fun cleanBuildData(projectId: String, pipelineId: String, buildIds: List<String>) {
        basicAuths.forEach {
            cleanBuildDataRequest(
                projectId = projectId,
                pipelineId = pipelineId,
                buildIds = buildIds,
                url = it.url,
                userName = it.username,
                password = it.password
            )
        }
    }

    private fun cleanBuildDataRequest(
        projectId: String,
        pipelineId: String,
        buildIds: List<String>,
        url: String,
        userName: String,
        password: String
    ) {
        val context = mapOf<String, Any>(
            "projectId" to projectId,
            "pipelineId" to pipelineId,
            "buildIds" to buildIds
        )

        val body = RequestBody.create(
            MediaType.parse("application/json"),
            JsonUtil.toJson(context)
        )

        val request = Request.Builder()
            .url(url)
            .put(body)
            .addHeader("Authorization", Credentials.basic(userName, password))
            .addHeader(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId)
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("cleanBuildDataRequest fail body is $body")
            }
            logger.info("cleanBuildDataRequest response is $responseContent")
        }
    }
    companion object {
        private val logger = LoggerFactory.getLogger(TxProcessRelatedPlatformDataClearServiceImpl::class.java)
    }
}
