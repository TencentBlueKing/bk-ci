/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.opdata.client.actuator

import com.tencent.bkrepo.auth.constant.AUTHORIZATION
import com.tencent.bkrepo.common.api.constant.HttpStatus.FORBIDDEN
import com.tencent.bkrepo.common.api.constant.HttpStatus.NOT_FOUND
import com.tencent.bkrepo.common.api.constant.HttpStatus.UNAUTHORIZED
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.artifact.metrics.ARTIFACT_DOWNLOADING_COUNT
import com.tencent.bkrepo.common.artifact.metrics.ARTIFACT_UPLOADING_COUNT
import com.tencent.bkrepo.common.security.util.BasicAuthUtils
import com.tencent.bkrepo.opdata.client.ArtifactMetricsClient
import com.tencent.bkrepo.opdata.config.OkHttpConfiguration
import com.tencent.bkrepo.opdata.config.OpProperties
import com.tencent.bkrepo.opdata.pojo.registry.InstanceInfo
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class ActuatorArtifactMetricsClient @Autowired constructor(
    @Qualifier(OkHttpConfiguration.OP_OKHTTP_CLIENT_NAME) private val httpClient: OkHttpClient,
    private val opProperties: OpProperties
) : ArtifactMetricsClient {
    override fun uploadingCount(instanceInfo: InstanceInfo): Long {
        try {
            return count(instanceInfo, ARTIFACT_UPLOADING_COUNT)
        } catch (e: Exception) {
            logger.error("get uploading count failed", e)
        }
        return UNKNOWN
    }

    override fun downloadingCount(instanceInfo: InstanceInfo): Long {
        try {
            return count(instanceInfo, ARTIFACT_DOWNLOADING_COUNT)
        } catch (e: Exception) {
            logger.error("get downloading count failed", e)
        }
        return UNKNOWN
    }

    private fun count(instanceInfo: InstanceInfo, metricsName: String): Long {
        if (!checkUsernameAndPassword()) {
            logger.warn("get $metricsName failed, username or password is empty")
            return UNKNOWN
        }

        val req = buildRequest(instanceInfo, metricsName)
        httpClient.newCall(req).execute().use { res ->
            if (res.isSuccessful) {
                val metrics = res.body()!!.string().readJsonString<Metrics>()
                require(metrics.measurements.size == 1)
                return metrics.measurements[0].value.toLong()
            }

            val resCode = res.code()
            val logMsg = "request metrics actuator $metricsName failed, code: $resCode, message: ${res.message()}"
            if (resCode == NOT_FOUND.value || resCode == UNAUTHORIZED.value || resCode == FORBIDDEN.value) {
                logger.warn(logMsg)
            } else {
                logger.error(logMsg)
            }

            return UNKNOWN
        }
    }

    private fun buildRequest(instanceInfo: InstanceInfo, metricsName: String): Request {
        val url = HttpUrl.Builder()
            .scheme(ACTUATOR_SCHEME)
            .host(instanceInfo.host)
            .port(instanceInfo.port)
            .addPathSegments(ACTUATOR_ENDPOINT_METRICS)
            .addPathSegment(metricsName)
            .build()

        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader(AUTHORIZATION, BasicAuthUtils.encode(opProperties.adminUsername, opProperties.adminPassword))

        return reqBuilder.build()
    }

    private fun checkUsernameAndPassword(): Boolean {
        return opProperties.adminUsername.isNotEmpty() && opProperties.adminPassword.isNotEmpty()
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ActuatorArtifactMetricsClient::class.java)
        private const val UNKNOWN = -1L
        private const val ACTUATOR_SCHEME = "http"
        private const val ACTUATOR_ENDPOINT_METRICS = "actuator/metrics"
    }
}
