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

package com.tencent.devops.dispatch.bcs.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.bcs.client.BcsBuilderClient
import com.tencent.devops.dispatch.bcs.client.BcsTaskClient
import com.tencent.devops.dispatch.bcs.pojo.BcsBuilder
import com.tencent.devops.dispatch.bcs.pojo.BcsBuilderStatusEnum
import com.tencent.devops.dispatch.bcs.pojo.BcsDeleteBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.BcsStartBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.BcsStopBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.BcsTaskStatusEnum
import com.tencent.devops.dispatch.bcs.pojo.canReStart
import com.tencent.devops.dispatch.bcs.pojo.getCodeMessage
import com.tencent.devops.dispatch.bcs.pojo.hasException
import com.tencent.devops.dispatch.bcs.pojo.isFailed
import com.tencent.devops.dispatch.bcs.pojo.isRunning
import com.tencent.devops.dispatch.bcs.pojo.isStarting
import com.tencent.devops.dispatch.bcs.pojo.isSuccess
import com.tencent.devops.dispatch.bcs.pojo.readyToStart
import com.tencent.devops.dispatch.bcs.utils.BcsCommonUtils
import com.tencent.devops.dispatch.kubernetes.common.BUILDER_NAME
import com.tencent.devops.dispatch.kubernetes.common.ENV_DEFAULT_LOCALE_LANGUAGE
import com.tencent.devops.dispatch.kubernetes.common.ENV_JOB_BUILD_TYPE
import com.tencent.devops.dispatch.kubernetes.common.ENV_KEY_AGENT_ID
import com.tencent.devops.dispatch.kubernetes.common.ENV_KEY_AGENT_SECRET_KEY
import com.tencent.devops.dispatch.kubernetes.common.ENV_KEY_GATEWAY
import com.tencent.devops.dispatch.kubernetes.common.ENV_KEY_PROJECT_ID
import com.tencent.devops.dispatch.kubernetes.common.SLAVE_ENVIRONMENT
import com.tencent.devops.dispatch.kubernetes.components.LogsPrinter
import com.tencent.devops.dispatch.kubernetes.interfaces.ContainerService
import com.tencent.devops.dispatch.kubernetes.pojo.BK_BUILD_MACHINE_CREATION_FAILED
import com.tencent.devops.dispatch.kubernetes.pojo.BK_DISTRIBUTE_BUILD_MACHINE_REQUEST_SUCCESS
import com.tencent.devops.dispatch.kubernetes.pojo.BK_MACHINE_BUILD_COMPLETED_WAITING_FOR_STARTUP
import com.tencent.devops.dispatch.kubernetes.pojo.BK_READY_CREATE_BCS_BUILD_MACHINE
import com.tencent.devops.dispatch.kubernetes.pojo.BK_START_BCS_BUILD_CONTAINER_FAIL
import com.tencent.devops.dispatch.kubernetes.pojo.BK_THIRD_SERVICE_BCS_BUILD_ERROR
import com.tencent.devops.dispatch.kubernetes.pojo.BK_TROUBLE_SHOOTING
import com.tencent.devops.dispatch.kubernetes.pojo.DispatchBuildLog
import com.tencent.devops.dispatch.kubernetes.pojo.DockerRegistry
import com.tencent.devops.dispatch.kubernetes.pojo.Pool
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildImageReq
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildStatusResp
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchTaskResp
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildBuilderStatus
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildOperateBuilderParams
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildOperateBuilderType
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.pojo.debug.DispatchBuilderDebugStatus
import com.tencent.devops.dispatch.kubernetes.utils.CommonUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("bcsContainerService")
class BcsContainerService @Autowired constructor(
    private val bcsBuilderClient: BcsBuilderClient,
    private val logsPrinter: LogsPrinter,
    private val bcsTaskClient: BcsTaskClient,
    private val commonConfig: CommonConfig
) : ContainerService {

    companion object {
        private val logger = LoggerFactory.getLogger(BcsContainerService::class.java)
    }

    override val shutdownLockBaseKey = "dispatch_bcs_shutdown_lock_"

    override fun getLog() = DispatchBuildLog(
        readyStartLog =
        I18nUtil.getCodeLanMessage(BK_READY_CREATE_BCS_BUILD_MACHINE, I18nUtil.getDefaultLocaleLanguage()),
        startContainerError =
        I18nUtil.getCodeLanMessage(BK_START_BCS_BUILD_CONTAINER_FAIL, I18nUtil.getDefaultLocaleLanguage()),
        troubleShooting = I18nUtil.getCodeLanMessage(
            BK_THIRD_SERVICE_BCS_BUILD_ERROR,
            I18nUtil.getDefaultLocaleLanguage()
        )
    )

    @Value("\${bcs.resources.builder.cpu}")
    override var cpu: Double = 32.0

    @Value("\${bcs.resources.builder.memory}")
    override var memory: String = "65535"

    @Value("\${bcs.resources.builder.disk}")
    override var disk: String = "500"

    @Value("\${bcs.entrypoint}")
    override val entrypoint: String = "bcs_init.sh"

    @Value("\${bcs.sleepEntrypoint}")
    override val sleepEntrypoint: String = "sleep.sh"

    override val helpUrl: String? = ""

    override fun getBuilderStatus(
        buildId: String,
        vmSeqId: String,
        userId: String,
        builderName: String,
        retryTime: Int
    ): Result<DispatchBuildBuilderStatus> {
        val result = bcsBuilderClient.getBuilderDetail(
            buildId = buildId,
            vmSeqId = vmSeqId,
            userId = userId,
            name = builderName,
            retryTime = retryTime
        )
        if (result.isNotOk()) {
            return Result(result.code, result.message)
        }

        val status = when {
            result.data!!.readyToStart() -> DispatchBuildBuilderStatus.READY_START
            result.data.hasException() -> DispatchBuildBuilderStatus.HAS_EXCEPTION
            result.data.canReStart() -> DispatchBuildBuilderStatus.CAN_RESTART
            result.data.isRunning() -> DispatchBuildBuilderStatus.RUNNING
            result.data.isStarting() -> DispatchBuildBuilderStatus.STARTING
            else -> DispatchBuildBuilderStatus.UNKNOWN
        }

        return Result(status)
    }

    override fun operateBuilder(
        buildId: String,
        vmSeqId: String,
        userId: String,
        builderName: String,
        param: DispatchBuildOperateBuilderParams
    ): String {
        return bcsBuilderClient.operateBuilder(
            buildId = buildId,
            vmSeqId = vmSeqId,
            userId = userId,
            name = builderName,
            param = when (param.type) {
                DispatchBuildOperateBuilderType.START_SLEEP -> BcsStartBuilderParams(
                    env = param.env,
                    command = listOf("/bin/sh", sleepEntrypoint)
                )
                DispatchBuildOperateBuilderType.DELETE -> BcsDeleteBuilderParams()
                DispatchBuildOperateBuilderType.STOP -> BcsStopBuilderParams()
            }
        )
    }

    override fun createAndStartBuilder(
        dispatchMessages: DispatchMessage,
        containerPool: Pool,
        poolNo: Int,
        cpu: Double,
        mem: String,
        disk: String
    ): Pair<String, String> {
        with(dispatchMessages) {
            val (host, name, tag) = CommonUtils.parseImage(containerPool.container!!)
            val userName = containerPool.credential?.user
            val password = containerPool.credential?.password

            val builderName = BcsCommonUtils.getOnlyName(userId)
            val bcsTaskId = bcsBuilderClient.createBuilder(
                buildId = buildId,
                vmSeqId = vmSeqId,
                userId = userId,
                bcsBuilder = BcsBuilder(
                    name = builderName,
                    image = "$host/$name:$tag",
                    registry = DockerRegistry(host, userName, password),
                    cpu = cpu,
                    mem = mem,
                    disk = disk,
                    env = mapOf(
                        ENV_KEY_PROJECT_ID to projectId,
                        ENV_KEY_AGENT_ID to id,
                        ENV_KEY_AGENT_SECRET_KEY to secretKey,
                        ENV_KEY_GATEWAY to gateway,
                        "TERM" to "xterm-256color",
                        SLAVE_ENVIRONMENT to "Bcs",
                        ENV_JOB_BUILD_TYPE to (dispatchType?.buildType()?.name ?: BuildType.PUBLIC_BCS.name),
                        BUILDER_NAME to builderName,
                        ENV_DEFAULT_LOCALE_LANGUAGE to commonConfig.devopsDefaultLocaleLanguage
                    ),
                    command = listOf("/bin/sh", entrypoint)
                )
            )
            logger.info(
                "buildId: $buildId,vmSeqId: $vmSeqId,executeCount: $executeCount,poolNo: $poolNo createBuilder, " +
                    "taskId:($bcsTaskId)"
            )
            logsPrinter.printLogs(
                this,
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_DISTRIBUTE_BUILD_MACHINE_REQUEST_SUCCESS,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ) + " builderName: $builderName "
            )

            val (taskStatus, failedMsg) = bcsTaskClient.waitTaskFinish(userId, bcsTaskId)

            if (taskStatus == BcsTaskStatusEnum.SUCCEEDED) {
                // 启动成功
                logger.info(
                    "buildId: $buildId,vmSeqId: $vmSeqId,executeCount: $executeCount,poolNo: $poolNo create bcs " +
                        "vm success, wait vm start..."
                )
                logsPrinter.printLogs(
                    this,
                    I18nUtil.getCodeLanMessage(
                        BK_MACHINE_BUILD_COMPLETED_WAITING_FOR_STARTUP,
                        I18nUtil.getDefaultLocaleLanguage()
                    )
                )
            } else {
                // 清除构建异常容器，并重新置构建池为空闲
                clearExceptionBuilder(builderName)
                throw BuildFailureException(
                    ErrorCodeEnum.BCS_CREATE_VM_ERROR.errorType,
                    ErrorCodeEnum.BCS_CREATE_VM_ERROR.errorCode,
                    I18nUtil.getCodeLanMessage(ErrorCodeEnum.BCS_CREATE_VM_ERROR.errorCode.toString()),
                    I18nUtil.getCodeLanMessage(BK_TROUBLE_SHOOTING) +
                            I18nUtil.getCodeLanMessage(BK_BUILD_MACHINE_CREATION_FAILED) +
                    ":${failedMsg ?: taskStatus.message}"
                )
            }
            return Pair(startBuilder(dispatchMessages, builderName, poolNo, cpu, mem, disk), builderName)
        }
    }

    override fun startBuilder(
        dispatchMessages: DispatchMessage,
        builderName: String,
        poolNo: Int,
        cpu: Double,
        mem: String,
        disk: String
    ): String {
        with(dispatchMessages) {
            return bcsBuilderClient.operateBuilder(
                buildId = buildId,
                vmSeqId = vmSeqId,
                userId = userId,
                name = builderName,
                param = BcsStartBuilderParams(
                    env = mapOf(
                        ENV_KEY_PROJECT_ID to projectId,
                        ENV_KEY_AGENT_ID to id,
                        ENV_KEY_AGENT_SECRET_KEY to secretKey,
                        ENV_KEY_GATEWAY to gateway,
                        "TERM" to "xterm-256color",
                        SLAVE_ENVIRONMENT to "Bcs",
                        ENV_JOB_BUILD_TYPE to (dispatchType?.buildType()?.name ?: BuildType.PUBLIC_BCS.name),
                        BUILDER_NAME to builderName,
                        ENV_DEFAULT_LOCALE_LANGUAGE to commonConfig.devopsDefaultLocaleLanguage
                    ),
                    command = listOf("/bin/sh", entrypoint)
                )
            )
        }
    }

    private fun DispatchMessage.clearExceptionBuilder(builderName: String) {
        try {
            // 下发删除，不管成功失败
            logger.info("[$buildId]|[$vmSeqId] Delete builder, userId: $userId, builderName: $builderName")
            bcsBuilderClient.operateBuilder(
                buildId = buildId,
                vmSeqId = vmSeqId,
                userId = userId,
                name = builderName,
                param = BcsDeleteBuilderParams()
            )
        } catch (e: Exception) {
            logger.error("[$buildId]|[$vmSeqId] delete builder failed", e)
        }
    }

    override fun waitTaskFinish(userId: String, taskId: String): DispatchBuildTaskStatus {
        val startResult = bcsTaskClient.waitTaskFinish(userId, taskId)
        if (startResult.first == BcsTaskStatusEnum.SUCCEEDED) {
            return DispatchBuildTaskStatus(DispatchBuildTaskStatusEnum.SUCCEEDED, null)
        } else {
            return DispatchBuildTaskStatus(DispatchBuildTaskStatusEnum.FAILED, startResult.second)
        }
    }

    override fun getTaskStatus(userId: String, taskId: String): DispatchBuildStatusResp {
        val taskResponse = bcsTaskClient.getTasksStatus(userId, taskId)
        val status = BcsTaskStatusEnum.realNameOf(taskResponse.data?.status)
        if (taskResponse.isNotOk() || taskResponse.data == null) {
            // 创建失败
            val msg = "${taskResponse.message ?: taskResponse.getCodeMessage()}"
            logger.error("Execute task: $taskId failed, actionCode is ${taskResponse.code}, msg: $msg")
            return DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, msg)
        }
        // 请求成功但是任务失败
        if (status != null && status.isFailed()) {
            return DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, taskResponse.data.message)
        }
        return when {
            status!!.isRunning() -> DispatchBuildStatusResp(DispatchBuildStatusEnum.running.name)
            status.isSuccess() -> {
                DispatchBuildStatusResp(DispatchBuildStatusEnum.succeeded.name)
            }
            else -> DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, status.message)
        }
    }

    override fun waitDebugBuilderRunning(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        userId: String,
        builderName: String
    ): DispatchBuilderDebugStatus {
        val status = bcsBuilderClient.waitContainerRunning(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            userId = userId,
            containerName = builderName
        )
        return when (status) {
            BcsBuilderStatusEnum.READY_TO_RUN, BcsBuilderStatusEnum.STOP_FAILED ->
                DispatchBuilderDebugStatus.CAN_RESTART
            BcsBuilderStatusEnum.RUNNING -> DispatchBuilderDebugStatus.RUNNING
            BcsBuilderStatusEnum.STARTING -> DispatchBuilderDebugStatus.STARTING
            else -> DispatchBuilderDebugStatus.UNKNOWN
        }
    }

    override fun getDebugWebsocketUrl(
        projectId: String,
        pipelineId: String,
        staffName: String,
        builderName: String
    ): String {
        return bcsBuilderClient.getWebsocketUrl(projectId, pipelineId, staffName, builderName).data!!
    }

    override fun buildAndPushImage(
        userId: String,
        projectId: String,
        buildId: String,
        dispatchBuildImageReq: DispatchBuildImageReq
    ): DispatchTaskResp {
        logger.info(
            "projectId: $projectId, buildId: $buildId build and push image. " +
                JsonUtil.toJson(dispatchBuildImageReq)
        )

        return DispatchTaskResp(
            bcsBuilderClient.buildAndPushImage(
                userId, dispatchBuildImageReq
            )
        )
    }
}
