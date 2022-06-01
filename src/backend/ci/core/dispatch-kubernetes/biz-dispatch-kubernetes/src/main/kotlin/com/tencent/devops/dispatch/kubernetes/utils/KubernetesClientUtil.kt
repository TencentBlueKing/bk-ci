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

package com.tencent.devops.dispatch.kubernetes.utils

import com.tencent.devops.common.api.pojo.Result
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.ApiResponse
import io.kubernetes.client.openapi.models.V1Container
import io.kubernetes.client.openapi.models.V1Deployment
import io.kubernetes.client.openapi.models.V1DeploymentList
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.openapi.models.V1PodList
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus

object KubernetesClientUtil {

    private val logger = LoggerFactory.getLogger(KubernetesClientUtil::class.java)

    fun Map<String, String>.toLabelSelector(): String {
        return this.map {
            "${it.key}=${it.value}"
        }.joinToString()
    }

    private fun <T> ApiResponse<T>.isSuccessful(): Boolean = statusCode in 200..299

    /**
     * 获取错误信息
     */
    fun getClientFailInfo(message: String) = "Dispatch-kubernetes 异常信息 - $message"

    /**
     * 获取container名称
     */
    fun getKubernetesWorkloadOnlyLabelValue(buildId: String) =
        "$buildId-${RandomStringUtils.random(6, false, true)}"

    fun V1DeploymentList?.getFirstDeploy(): V1Deployment? {
        return this?.items?.ifEmpty { null }?.get(0)
    }

    fun V1PodList?.getFirstPod(): V1Pod? {
        return this?.items?.ifEmpty { null }?.get(0)
    }

    fun V1Pod?.getFirstContainer(): V1Container? {
        return this?.spec?.containers?.ifEmpty { null }?.get(0)
    }

    /**
     * ApiResponse装换为Result
     */
    fun <T> apiHandle(
        api: () -> ApiResponse<T>
    ): Result<T> {
        return try {
            HttpStatus.ACCEPTED
            api().let {
                Result(
                    status = if (it.isSuccessful()) {
                        0
                    } else {
                        it.statusCode
                    },
                    data = it.data
                )
            }
        } catch (e: ApiException) {
            logger.error("api exception", e)
            Result(
                status = e.code,
                message = e.responseBody
            )
        }
    }
}
