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

package com.tencent.devops.dispatch.kubernetes.client

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.dispatch.kubernetes.client.kubernetes.DeploymentClient
import com.tencent.devops.dispatch.kubernetes.client.kubernetes.PodsClient
import com.tencent.devops.dispatch.kubernetes.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.pojo.BuildContainer
import com.tencent.devops.dispatch.kubernetes.pojo.ContainerType
import com.tencent.devops.dispatch.kubernetes.pojo.resp.CreatContainerResult
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesClientUtil
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesClientUtil.isSuccessful
import io.kubernetes.client.openapi.models.V1ContainerStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ContainerClient @Autowired constructor(
    private val podsClient: PodsClient,
    private val deploymentClient: DeploymentClient
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ContainerClient::class.java)
    }

    fun getContainerStatus(
        buildId: String,
        vmSeqId: String,
        userId: String,
        name: String,
        retryTime: Int = 3
    ): Result<V1ContainerStatus> {
        logger.info("[$buildId]|[$vmSeqId] request $name getContainerStatus")
        val result = podsClient.list(name)
        logger.info("[$buildId]|[$vmSeqId] containerName: $name response: $result")
        if (!result.isSuccessful()) {
            // 先不添加重试逻辑，看后续使用
            //           if (retryTime > 0) {
            //               val retryTimeLocal = retryTime - 1
            //               return getContainerStatus(buildId, vmSeqId, userId, name, retryTimeLocal)
            //           }
            throw BuildFailureException(
                ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.errorType,
                ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.errorCode,
                ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR.formatErrorMessage,
                KubernetesClientUtil.getClientFailInfo(
                    "获取容器状态接口异常（Fail to get container status, http response code: ${result.statusCode}"
                )
            )
        }
        return Result(
            status = result.statusCode,
            message = null,
            data = result.data.items[0].status?.containerStatuses?.get(0)
        )
    }

    fun createContainer(
        dispatchMessage: DispatchMessage,
        buildContainer: BuildContainer
    ): CreatContainerResult {
        val containerName = "${dispatchMessage.userId}1574210195791"
        val result = when (buildContainer.type) {
            ContainerType.DEV -> {
                deploymentClient.create(buildContainer, containerName)
            }
            ContainerType.STATEFUL -> {
                TODO()
            }
            ContainerType.STATELESS -> {
                TODO()
            }
        }

        return CreatContainerResult()
    }
}
