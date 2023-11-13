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

package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.dispatch.devcloud.client.DispatchDevCloudClient
import com.tencent.devops.dispatch.devcloud.common.ErrorCodeEnum
import com.tencent.devops.dispatch.devcloud.pojo.Action
import com.tencent.devops.dispatch.devcloud.pojo.DEVOPS_AGENTSLIM_CONTAINER_NAME
import com.tencent.devops.dispatch.devcloud.pojo.DEVOPS_AGENTSLIM_FILEGATEWAY
import com.tencent.devops.dispatch.devcloud.pojo.DEVOPS_AGENTSLIM_GATEWAY
import com.tencent.devops.dispatch.devcloud.pojo.DEVOPS_AGENTSLIM_ISDEBUG
import com.tencent.devops.dispatch.devcloud.pojo.DEVOPS_AGENTSLIM_JAVA_PATH
import com.tencent.devops.dispatch.devcloud.pojo.DEVOPS_AGENTSLIM_LANUAGE
import com.tencent.devops.dispatch.devcloud.pojo.DEVOPS_AGENTSLIM_LOGPATH
import com.tencent.devops.dispatch.devcloud.pojo.DEVOPS_AGENTSLIM_MAX_WORKER_COUNT
import com.tencent.devops.dispatch.devcloud.pojo.DEVOPS_AGENTSLIM_PROJECT_ID
import com.tencent.devops.dispatch.devcloud.pojo.DEVOPS_AGENTSLIM_WORKER_DETECTSHELL
import com.tencent.devops.dispatch.devcloud.pojo.DEVOPS_AGENTSLIM_WORKER_PATH
import com.tencent.devops.dispatch.devcloud.pojo.DEVOPS_AGENTSLIM_WORKER_USER
import com.tencent.devops.dispatch.devcloud.pojo.ENV_DEFAULT_LOCALE_LANGUAGE
import com.tencent.devops.dispatch.devcloud.pojo.ENV_DEVCLOUD_CPU
import com.tencent.devops.dispatch.devcloud.pojo.ENV_DEVCLOUD_DISK
import com.tencent.devops.dispatch.devcloud.pojo.ENV_DEVCLOUD_MEM
import com.tencent.devops.dispatch.devcloud.pojo.ENV_JOB_BUILD_TYPE
import com.tencent.devops.dispatch.devcloud.pojo.ENV_KEY_AGENT_ID
import com.tencent.devops.dispatch.devcloud.pojo.ENV_KEY_AGENT_SECRET_KEY
import com.tencent.devops.dispatch.devcloud.pojo.ENV_KEY_GATEWAY
import com.tencent.devops.dispatch.devcloud.pojo.ENV_KEY_PROJECT_ID
import com.tencent.devops.dispatch.devcloud.pojo.SLAVE_ENVIRONMENT
import com.tencent.devops.dispatch.devcloud.service.context.DcStartupHandlerContext
import com.tencent.devops.process.engine.common.VMUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
abstract class StartupContainerHandler @Autowired constructor(
    private val commonConfig: CommonConfig,
    private val buildLogPrinter: BuildLogPrinter,
    private val dispatchDevCloudClient: DispatchDevCloudClient
) : Handler<DcStartupHandlerContext>() {

    @Value("\${atom.fuse.container.label}")
    val fuseContainerLabel: String? = null

    @Value("\${atom.fuse.atom-code}")
    val fuseAtomCode: String? = null

    @Value("\${devCloud.entrypoint}")
    val entrypoint: String = "devcloud_init.sh"

    @Value("\${devCloud.clusterType:normal}")
    var clusterType: String? = "normal"

    @Value("\${devCloud.fitPersistenceCluster.persistenceEntrypoint:}")
    val persistenceEntrypoint: String = "devcloud_persistence_init.sh"

    @Value("\${devCloud.fitPersistenceCluster.clusterType:fit}")
    var persistenceClusterType: String? = "fit"

    private val overlayFsLabel = "checkout"

    fun generateContainerEnvs(handlerContext: DcStartupHandlerContext): Map<String, Any> {
        // 拼接环境变量
        with(handlerContext) {
            val envs = mutableMapOf<String, Any>()
            if (customBuildEnv != null) {
                envs.putAll(customBuildEnv)
            }
            envs.putAll(
                mapOf(
                    ENV_KEY_PROJECT_ID to projectId,
                    ENV_KEY_AGENT_ID to agentId,
                    ENV_KEY_AGENT_SECRET_KEY to secretKey,
                    ENV_KEY_GATEWAY to gateway,
                    "TERM" to "xterm-256color",
                    SLAVE_ENVIRONMENT to "DevCloud",
                    ENV_JOB_BUILD_TYPE to (dispatchType?.buildType()?.name ?: BuildType.PUBLIC_DEVCLOUD.name),
                    ENV_DEFAULT_LOCALE_LANGUAGE to commonConfig.devopsDefaultLocaleLanguage,
                    ENV_DEVCLOUD_CPU to cpu.toString(),
                    ENV_DEVCLOUD_MEM to memory,
                    ENV_DEVCLOUD_DISK to disk
                )
            )

            if (persistence) {
                envs.putAll(
                    mapOf(
                        DEVOPS_AGENTSLIM_ISDEBUG to "true",
                        DEVOPS_AGENTSLIM_LOGPATH to "/data/logs/agent",
                        DEVOPS_AGENTSLIM_WORKER_USER to "root",
                        DEVOPS_AGENTSLIM_LANUAGE to commonConfig.devopsDefaultLocaleLanguage,
                        DEVOPS_AGENTSLIM_MAX_WORKER_COUNT to "1",
                        DEVOPS_AGENTSLIM_GATEWAY to (commonConfig.devopsDevnetProxyGateway ?: ""),
                        DEVOPS_AGENTSLIM_FILEGATEWAY to (commonConfig.fileDevnetGateway ?: ""),
                        DEVOPS_AGENTSLIM_PROJECT_ID to projectId,
                        DEVOPS_AGENTSLIM_CONTAINER_NAME to persistenceAgentId,
                        DEVOPS_AGENTSLIM_WORKER_PATH to "/data/docker.jar",
                        DEVOPS_AGENTSLIM_JAVA_PATH to "/usr/local/jre/bin/java",
                        DEVOPS_AGENTSLIM_WORKER_DETECTSHELL to "true"
                    )
                )
            }

            return envs
        }
    }

    fun generateContainerLabels(handlerContext: DcStartupHandlerContext): MutableMap<String, String> {
        with(handlerContext) {
            val containerLabels = mutableMapOf(
                "projectId" to projectId,
                "pipelineId" to pipelineId,
                "buildId" to buildId,
                "vmSeqId" to vmSeqId
            )

            // 针对fuse插件优化
            fuseAtomCode?.split(",")?.forEach {
                if (it in atoms.keys) {
                    val (key, value) = fuseContainerLabel!!.split(":")
                    containerLabels[key] = value
                    return@forEach
                }
            }

            // overlayfs代码拉取优化
            if (overlayFsLabel in atoms.keys) {
                containerLabels[overlayFsLabel] = "true"
            }

            return containerLabels
        }
    }

    fun generateContainerCommand(persistence: Boolean): List<String> {
        return if (persistence) {
            listOf("/bin/sh", persistenceEntrypoint)
        } else {
            listOf("/bin/sh", entrypoint)
        }
    }

    fun generateClusterType(persistence: Boolean): String {
        return if (persistence) {
            persistenceClusterType ?: ""
        } else {
            clusterType ?: ""
        }
    }

    fun clearExceptionContainer(
        containerName: String,
        handlerContext: DcStartupHandlerContext
    ) {
        with(handlerContext) {
            try {
                // 下发删除，不管成功失败
                logger.info("$buildLogKey userId: $userId clear exceptionContainer: $containerName")
                dispatchDevCloudClient.operateContainer(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    userId = userId,
                    name = containerName,
                    action = Action.DELETE,
                    persistence = persistence
                )
            } catch (e: Exception) {
                logger.error("$buildLogKey Failed to clear exceptionContainer.", e)
            }
        }
    }

    fun printLog(
        message: String,
        handlerContext: DcStartupHandlerContext
    ) {
        with(handlerContext) {
            buildLogPrinter.addLine(
                buildId = buildId,
                message = message,
                tag = VMUtils.genStartVMTaskId(vmSeqId),
                jobId = containerHashId,
                executeCount = executeCount ?: 1
            )
        }
    }

    fun checkContainerName(containerName: String?) {
        if (containerName.isNullOrBlank()) {
            throw BuildFailureException(
                ErrorCodeEnum.START_VM_ERROR.errorType,
                ErrorCodeEnum.START_VM_ERROR.errorCode,
                ErrorCodeEnum.START_VM_ERROR.getErrorMessage(),
                "ContainerName is null"
            )
        }
    }

    fun getContainerStatus(
        containerName: String?,
        handlerContext: DcStartupHandlerContext
    ): String? {
        with(handlerContext) {
            if (containerName.isNullOrBlank()) {
                return null
            }

            val statusResponse = dispatchDevCloudClient.getContainerStatus(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                userId = userId,
                name = containerName,
                persistence = persistence
            )
            if (statusResponse.optInt("actionCode") != 200) {
                return null
            }

            return statusResponse.optString("data")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StartupContainerHandler::class.java)
    }
}
