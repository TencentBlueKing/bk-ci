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

package com.tencent.devops.dispatch.kubernetes.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.type.kubernetes.KubernetesDispatchType
import com.tencent.devops.dispatch.kubernetes.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.config.DispatchBuildConfig
import com.tencent.devops.dispatch.kubernetes.dao.BuildContainerPoolNoDao
import com.tencent.devops.dispatch.kubernetes.pojo.Credential
import com.tencent.devops.dispatch.kubernetes.pojo.Pool
import com.tencent.devops.dispatch.kubernetes.service.BuildHisService
import com.tencent.devops.dispatch.kubernetes.service.ContainerService
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesClientUtil
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class KubernetesListener @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val buildContainerPoolNoDao: BuildContainerPoolNoDao,
    private val dispatchConfig: DispatchBuildConfig,
    private val buildLogPrinter: BuildLogPrinter,
    private val containerService: ContainerService,
    private val buildHisService: BuildHisService
) : BuildListener {

    private val threadLocalCpu = ThreadLocal<Int>()
    private val threadLocalMemory = ThreadLocal<String>()
    private val threadLocalDisk = ThreadLocal<String>()

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesListener::class.java)
    }

    override fun getStartupQueue() = ".kubernetes"

    override fun getShutdownQueue() = ".kubernetes"

    override fun getVmType() = JobQuotaVmType.KUBERNETES

    override fun onStartup(dispatchMessage: DispatchMessage) {
        logger.info("On start up - ($dispatchMessage)")

        val dispatch = dispatchMessage.dispatchType as KubernetesDispatchType
        printLogs(
            dispatchMessage = dispatchMessage,
            message = "Start kubernetes deployment build ${dispatch.kubernetesBuildVersion} for the build"
        )

        val buildContainerPoolNo = buildContainerPoolNoDao.getDevCloudBuildLastPoolNo(
            dslContext = dslContext,
            buildId = dispatchMessage.buildId,
            vmSeqId = dispatchMessage.vmSeqId,
            executeCount = dispatchMessage.executeCount ?: 1
        )
        logger.info("buildContainerPoolNo: $buildContainerPoolNo")
        if (buildContainerPoolNo.isNotEmpty() && buildContainerPoolNo[0].second != null) {
            retry()
        } else {
            createOrStartContainer(dispatchMessage)
        }
    }

    override fun onShutdown(event: PipelineAgentShutdownEvent) {
        TODO("Not yet implemented")
    }

    private fun createOrStartContainer(dispatchMessage: DispatchMessage) {
        threadLocalCpu.set(dispatchConfig.deploymentCpu)
        threadLocalMemory.set(dispatchConfig.deploymentMemory)
        threadLocalDisk.set(dispatchConfig.deploymentDisk)

        try {
            val containerPool = (objectMapper.readValue<Pool>(dispatchMessage.dispatchMessage)).getContainerPool()
            printLogs(dispatchMessage, "启动镜像：${containerPool.container}")

            val (lastIdleContainer, poolNo, containerChanged) = containerService.getIdleContainer(
                dispatchMessage, threadLocalCpu, threadLocalMemory, threadLocalDisk
            )

            // 记录构建历史
            buildHisService.recordBuildHisAndGatewayCheck(
                poolNo = poolNo,
                lastIdleContainer = lastIdleContainer,
                dispatchMessage = dispatchMessage,
                cpu = threadLocalCpu,
                memory = threadLocalMemory,
                disk = threadLocalDisk
            )

            // 用户第一次构建，或者用户更换了镜像，或者容器配置有变更，则重新创建容器。否则，使用已有容器，start起来即可
            if (null == lastIdleContainer || containerChanged) {
                logger.info(
                    "buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} " +
                            "create new container, poolNo: $poolNo"
                )
                containerService.createNewContainer(
                    dispatchMessage = dispatchMessage,
                    containerPool = containerPool,
                    poolNo = poolNo,
                    cpu = threadLocalCpu,
                    memory = threadLocalMemory,
                    disk = threadLocalDisk
                ).let {
                    if (!it.result) {
                        onFailure(
                            ErrorCodeEnum.CREATE_VM_ERROR.errorType,
                            ErrorCodeEnum.CREATE_VM_ERROR.errorCode,
                            ErrorCodeEnum.CREATE_VM_ERROR.formatErrorMessage,
                            KubernetesClientUtil.getClientFailInfo("构建机创建失败: ${it.errorMessage}")
                        )
                    }
                }
            } else {
                logger.info(
                    "buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} " +
                            "start idle container, containerName: $lastIdleContainer"
                )
                startContainer(lastIdleContainer, dispatchMessage, poolNo)
            }
        } catch (e: BuildFailureException) {
            logger.error(
                "buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} " +
                        "create deployment failed. msg:${e.message}."
            )
            onFailure(
                errorType = e.errorType,
                errorCode = e.errorCode,
                formatErrorMessage = e.formatErrorMessage,
                message = e.message ?: "启动kubernetes deployment构建容器"
            )
        } catch (e: Exception) {
            logger.error("buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} create devCloud failed, msg:${e.message}")
            if (e.message.equals("timeout")) {
                onFailure(
                    ErrorCodeEnum.KUBERNETES_INTERFACE_TIMEOUT.errorType,
                    ErrorCodeEnum.KUBERNETES_INTERFACE_TIMEOUT.errorCode,
                    ErrorCodeEnum.KUBERNETES_INTERFACE_TIMEOUT.formatErrorMessage,
                    "Dispatch kubernetes - 接口请求超时"
                )
            }
            onFailure(
                ErrorCodeEnum.SYSTEM_ERROR.errorType,
                ErrorCodeEnum.SYSTEM_ERROR.errorCode,
                ErrorCodeEnum.SYSTEM_ERROR.formatErrorMessage,
                "创建构建机失败，错误信息:${e.message}."
            )
        }
    }

    private fun Pool.getContainerPool(): Pool {
        if (third != null && !third) {
            val containerPoolFixed = if (container!!.startsWith(dispatchConfig.registryHost!!)) {
                Pool(
                    container,
                    Credential(dispatchConfig.registryUser!!, dispatchConfig.registryPwd!!),
                    performanceConfigId,
                    third
                )
            } else {
                Pool(
                    dispatchConfig.registryHost + "/" + container,
                    Credential(dispatchConfig.registryUser!!, dispatchConfig.registryPwd!!),
                    performanceConfigId,
                    third
                )
            }

            return containerPoolFixed
        }

        return this
    }

    private fun printLogs(dispatchMessage: DispatchMessage, message: String) {
        try {
            buildLogPrinter.addLine(
                buildId = dispatchMessage.buildId,
                jobId = dispatchMessage.containerHashId,
                tag = VMUtils.genStartVMTaskId(dispatchMessage.vmSeqId),
                message = message,
                executeCount = dispatchMessage.executeCount ?: 1
            )
        } catch (e: Throwable) {
            // 日志有问题就不打日志了，不能影响正常流程
            logger.error("", e)
        }
    }
}
