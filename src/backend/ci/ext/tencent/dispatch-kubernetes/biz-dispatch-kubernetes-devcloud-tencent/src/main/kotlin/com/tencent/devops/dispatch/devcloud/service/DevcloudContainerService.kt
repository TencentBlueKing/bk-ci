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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.dispatch.devcloud.utils.DevcloudWorkspaceRedisUtils
import com.tencent.devops.dispatch.kubernetes.components.LogsPrinter
import com.tencent.devops.dispatch.kubernetes.interfaces.ContainerService
import com.tencent.devops.dispatch.kubernetes.pojo.DispatchBuildLog
import com.tencent.devops.dispatch.kubernetes.pojo.Pool
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildImageReq
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildStatusResp
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchTaskResp
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildBuilderStatus
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildOperateBuilderParams
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.debug.DispatchBuilderDebugStatus
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatusEnum
import org.apache.commons.lang3.RandomStringUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("devcloudContainerService")
class DevcloudContainerService @Autowired constructor(
    private val logsPrinter: LogsPrinter,
    private val dslContext: DSLContext,
    private val devcloudWorkspaceRedisUtils: DevcloudWorkspaceRedisUtils
) : ContainerService {

    companion object {
        private val logger = LoggerFactory.getLogger(DevcloudContainerService::class.java)
    }

    override val shutdownLockBaseKey = "workspace_devcloud_shutdown_lock_"

    override val log = DispatchBuildLog(
        readyStartLog = "准备创建devcloud开发环境...",
        startContainerError = "启动devcloud开发环境失败，请联系蓝盾助手反馈处理.\n容器构建异常请参考：",
        troubleShooting = "Devcloud构建异常，请联系蓝盾助手排查，异常信息 - "
    )

    @Value("\${devCloud.resources.builder.cpu}")
    override var cpu: Double = 32.0

    @Value("\${devCloud.resources.builder.memory}")
    override var memory: String = "65535"

    @Value("\${devCloud.resources.builder.disk}")
    override var disk: String = "500"

    @Value("\${devCloud.entrypoint}")
    override val entrypoint: String = "kubernetes_init.sh"

    @Value("\${devCloud.sleepEntrypoint}")
    override val sleepEntrypoint: String = "sleep.sh"

    override val helpUrl: String? = ""

    override fun getBuilderStatus(buildId: String, vmSeqId: String, userId: String, builderName: String, retryTime: Int): Result<DispatchBuildBuilderStatus> {
        TODO("Not yet implemented")
    }

    override fun operateBuilder(buildId: String, vmSeqId: String, userId: String, builderName: String, param: DispatchBuildOperateBuilderParams): String {
        TODO("Not yet implemented")
    }

    override fun createAndStartBuilder(dispatchMessages: DispatchMessage, containerPool: Pool, poolNo: Int, cpu: Double, mem: String, disk: String): Pair<String, String> {
        TODO("Not yet implemented")
    }

    override fun startBuilder(dispatchMessages: DispatchMessage, builderName: String, poolNo: Int, cpu: Double, mem: String, disk: String): String {
        TODO("Not yet implemented")
    }

    override fun waitTaskFinish(userId: String, taskId: String): DispatchBuildTaskStatus {
        // 将task放入缓存，等待回调
        devcloudWorkspaceRedisUtils.refreshTaskStatus(
            userId = userId,
            taskUid = taskId,
            taskStatus = TaskStatus(taskId)
        )

        // 轮训十分钟
        val startTime = System.currentTimeMillis()
        loop@ while (true) {
            if (System.currentTimeMillis() - startTime > 10 * 60 * 1000) {
                logger.error("Wait task: $taskId finish timeout(10min)")
                return DispatchBuildTaskStatus(DispatchBuildTaskStatusEnum.FAILED, "DevCloud任务超时（10min）")
            }
            Thread.sleep(1 * 1000)
            val taskStatus = devcloudWorkspaceRedisUtils.getTaskStatus(taskId)
            if (taskStatus?.status != null) {
                logger.info("Loop task status: ${JsonUtil.toJson(taskStatus)}")
                return if (taskStatus.status == TaskStatusEnum.successed) {
                    DispatchBuildTaskStatus(DispatchBuildTaskStatusEnum.SUCCEEDED, null)
                } else {
                    DispatchBuildTaskStatus(DispatchBuildTaskStatusEnum.FAILED, taskStatus.logs.toString())
                }
            }
        }
    }

    override fun getTaskStatus(userId: String, taskId: String): DispatchBuildStatusResp {
        TODO("Not yet implemented")
    }

    override fun waitDebugBuilderRunning(projectId: String, pipelineId: String, buildId: String, vmSeqId: String, userId: String, builderName: String): DispatchBuilderDebugStatus {
        TODO("Not yet implemented")
    }

    override fun getDebugWebsocketUrl(projectId: String, pipelineId: String, staffName: String, builderName: String): String {
        TODO("Not yet implemented")
    }

    override fun buildAndPushImage(userId: String, projectId: String, buildId: String, dispatchBuildImageReq: DispatchBuildImageReq): DispatchTaskResp {
        TODO("Not yet implemented")
    }


    private fun getOnlyName(userId: String): String {
        val subUserId = if (userId.length > 14) {
            userId.substring(0 until 14)
        } else {
            userId
        }
        return "${subUserId.replace("_", "-")}${System.currentTimeMillis()}-" +
            RandomStringUtils.randomAlphabetic(8).toLowerCase()
    }
}
