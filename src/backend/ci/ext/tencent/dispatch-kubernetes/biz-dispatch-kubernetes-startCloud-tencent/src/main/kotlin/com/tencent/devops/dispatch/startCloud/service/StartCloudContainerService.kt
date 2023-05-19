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

package com.tencent.devops.dispatch.startCloud.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.kubernetes.components.LogsPrinter
import com.tencent.devops.dispatch.kubernetes.interfaces.ContainerService
import com.tencent.devops.dispatch.kubernetes.pojo.BK_CONTAINER_BUILD_ERROR
import com.tencent.devops.dispatch.kubernetes.pojo.BK_READY_CREATE_DEVCLOUD_BUILD_MACHINE
import com.tencent.devops.dispatch.kubernetes.pojo.BK_START_BUILD_CONTAINER_FAIL
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
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("startcloudContainerService")
class StartCloudContainerService @Autowired constructor(
    private val logsPrinter: LogsPrinter,
    private val dslContext: DSLContext
) : ContainerService {

    companion object {
        private val logger = LoggerFactory.getLogger(StartCloudContainerService::class.java)
    }

    override val shutdownLockBaseKey = "workspace_startCloud_shutdown_lock_"

    override fun getLog() = DispatchBuildLog(
        readyStartLog = I18nUtil.getCodeLanMessage(BK_READY_CREATE_DEVCLOUD_BUILD_MACHINE),
        startContainerError = I18nUtil.getCodeLanMessage(
            messageCode = BK_START_BUILD_CONTAINER_FAIL,
            params = arrayOf("startCloud")
        ),
        troubleShooting = I18nUtil.getCodeLanMessage(
            messageCode = BK_CONTAINER_BUILD_ERROR,
            params = arrayOf("startCloud")
        )
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

    override fun getBuilderStatus(
        buildId: String,
        vmSeqId: String,
        userId: String,
        builderName: String,
        retryTime: Int
    ): Result<DispatchBuildBuilderStatus> {
        TODO("Not yet implemented")
    }

    override fun operateBuilder(
        buildId: String,
        vmSeqId: String,
        userId: String,
        builderName: String,
        param: DispatchBuildOperateBuilderParams
    ): String {
        TODO("Not yet implemented")
    }

    override fun createAndStartBuilder(
        dispatchMessages: DispatchMessage,
        containerPool: Pool,
        poolNo: Int,
        cpu: Double,
        mem: String,
        disk: String
    ): Pair<String, String> {
        TODO("Not yet implemented")
    }

    override fun startBuilder(
        dispatchMessages: DispatchMessage,
        builderName: String,
        poolNo: Int,
        cpu: Double,
        mem: String,
        disk: String
    ): String {
        TODO("Not yet implemented")
    }

    override fun waitTaskFinish(userId: String, taskId: String): DispatchBuildTaskStatus {
        return DispatchBuildTaskStatus(DispatchBuildTaskStatusEnum.SUCCEEDED, null)
    }

    override fun getTaskStatus(userId: String, taskId: String): DispatchBuildStatusResp {
        TODO("Not yet implemented")
    }

    override fun waitDebugBuilderRunning(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        userId: String,
        builderName: String
    ): DispatchBuilderDebugStatus {
        TODO("Not yet implemented")
    }

    override fun getDebugWebsocketUrl(
        projectId: String,
        pipelineId: String,
        staffName: String,
        builderName: String
    ): String {
        TODO("Not yet implemented")
    }

    override fun buildAndPushImage(
        userId: String,
        projectId: String,
        buildId: String,
        dispatchBuildImageReq: DispatchBuildImageReq
    ): DispatchTaskResp {
        TODO("Not yet implemented")
    }
}
