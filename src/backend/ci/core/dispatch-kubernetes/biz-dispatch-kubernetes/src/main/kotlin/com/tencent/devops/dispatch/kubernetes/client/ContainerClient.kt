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
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.dispatch.kubernetes.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.kubernetes.client.DeploymentClient
import com.tencent.devops.dispatch.kubernetes.kubernetes.client.PodsClient
import com.tencent.devops.dispatch.kubernetes.pojo.Action
import com.tencent.devops.dispatch.kubernetes.pojo.BuildContainer
import com.tencent.devops.dispatch.kubernetes.pojo.ContainerType
import com.tencent.devops.dispatch.kubernetes.pojo.Params
import com.tencent.devops.dispatch.kubernetes.pojo.resp.OperateContainerResult
import com.tencent.devops.dispatch.kubernetes.utils.CommonUtils
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesClientUtil
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesClientUtil.getFirstDeploy
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesClientUtil.getFirstPod
import io.kubernetes.client.openapi.ApiException
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
        private const val MAX_WAIT = 1000
    }

    /**
     * 获取container状态
     */
    fun getContainerStatus(
        containerName: String
    ): Result<V1ContainerStatus> {
        try {
            val result = podsClient.listWithHttpInfo(containerName)
            if (result.isNotOk()) {
                throw CommonUtils.buildFailureException(
                    ErrorCodeEnum.VM_STATUS_INTERFACE_ERROR,
                    KubernetesClientUtil.getClientFailInfo(
                        "获取容器状态接口异常（Fail to get container status, http response: $result"
                    )
                )
            }
            return Result(
                status = result.status,
                message = result.message,
                data = result.data?.getFirstPod()?.status?.containerStatuses?.ifEmpty { null }?.get(0)
            )
        } catch (e: Throwable) {
            logger.error("getContainerStatus error.", e)
            return Result(
                status = -1,
                message = e.message,
                data = null
            )
        }
    }

    /**
     * 创建container
     */
    fun createContainer(
        dispatchMessage: DispatchMessage,
        buildContainer: BuildContainer
    ): String {
        val containerName = KubernetesClientUtil.getKubernetesWorkloadOnlyLabelValue(dispatchMessage.buildId)

        logger.info("ContainerClient createContainer containerName: $containerName dispatchMessage: $dispatchMessage")

        when (buildContainer.type) {
            ContainerType.DEV -> {
                val resp = deploymentClient.create(buildContainer, containerName)
                if (resp.isNotOk()) {
                    throw CommonUtils.buildFailureException(
                        ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL,
                        KubernetesClientUtil.getClientFailInfo(
                            "创建容器接口异常 （Fail to create container status, http response $resp"
                        )
                    )
                }
            }
            ContainerType.STATEFUL -> {
                TODO()
            }
            ContainerType.STATELESS -> {
                TODO()
            }
        }
        return containerName
    }

    /**
     * 等待container启动
     */
    fun waitContainerStart(
        containerName: String
    ): OperateContainerResult {
        var statusResult = getContainerStatus(containerName)
        var finish = statusResult.isOk() && statusResult.data?.state?.running != null
        var max = MAX_WAIT
        while (!finish && max != 0) {
            statusResult = getContainerStatus(containerName)
            finish = statusResult.isOk() && statusResult.data?.state?.running != null
            Thread.sleep(1000)
            max--
        }
        return OperateContainerResult(containerName, finish)
    }

    /**
     * 根据action操作container
     */
    fun operateContainer(
        containerName: String,
        action: Action,
        param: Params?
    ): OperateContainerResult {
        return when (action) {
            Action.DELETE -> {
                val result = deploymentClient.delete(containerName)
                if (result.isNotOk()) {
                    OperateContainerResult("", false, result.data?.message)
                }
                OperateContainerResult("", true)
            }
            Action.START -> {
                try {
                    deploymentClient.start(containerName, param)
                } catch (e: Exception) {
                    val message = if (e is ApiException) {
                        e.responseBody
                    } else {
                        e.message
                    }
                    logger.error("start container $containerName error: $message")
                    throw CommonUtils.buildFailureException(
                        ErrorCodeEnum.CREATE_VM_INTERFACE_FAIL,
                        KubernetesClientUtil.getClientFailInfo("启动容器接口错误, $message")
                    )
                }
                OperateContainerResult("", true)
            }
            Action.STOP -> {
                val result = deploymentClient.stop(containerName)
                if (result.isNotOk()) {
                    logger.error("stop container error: ${result.message}")
                }
                OperateContainerResult("", result.isOk())
            }
        }
    }

    /**
     * 等待container停止
     */
    fun waitContainerStop(
        containerName: String
    ): OperateContainerResult {
        var replicas = try {
            val resp = deploymentClient.list(containerName)
            if (resp.isNotOk()) {
                return OperateContainerResult(containerName, false, resp.message)
            }
            resp.data?.getFirstDeploy()?.spec?.replicas
        } catch (e: Throwable) {
            return OperateContainerResult(containerName, false, e.message)
        }
        var max = MAX_WAIT
        while (replicas != 0 && max != 0) {
            replicas = deploymentClient.list(containerName).data?.getFirstDeploy()?.spec?.replicas
            Thread.sleep(1000)
            max--
        }
        return OperateContainerResult(containerName, replicas == 0)
    }
}
