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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.docker.sdk.utils

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.dispatch.docker.sdk.pojo.DispatchMessage
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import org.springframework.beans.factory.annotation.Autowired

class DispatcherUtils @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val printerLogPrinter: BuildLogPrinter
) {

    fun onContainerFailure(dispatchMessage: DispatchMessage) {
        pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                source = "container_startup_sdk",
                projectId = dispatchMessage.projectId,
                pipelineId = dispatchMessage.pipelineId,
                buildId = dispatchMessage.buildId,
                userId = dispatchMessage.userId,
                stageId = dispatchMessage.stageId,
                containerId = dispatchMessage.containerId,
                containerType = dispatchMessage.containerType,
                actionType = ActionType.TERMINATE
            )
        )
    }

    fun log(dispatchMessage: DispatchMessage, message: String) {
        log(dispatchMessage.buildId, dispatchMessage.containerHashId, dispatchMessage.vmSeqId, message, dispatchMessage.executeCount)
    }

    fun log(buildId: String, containerHashId: String?, vmSeqId: String, message: String, executeCount: Int?) {
        printerLogPrinter.addLine(
            buildId,
            message,
            VMUtils.genStartVMTaskId(vmSeqId),
            containerHashId,
            executeCount ?: 1
        )
    }

    fun logRed(dispatchMessage: DispatchMessage, message: String) {
        logRed(dispatchMessage.buildId, dispatchMessage.containerHashId, dispatchMessage.vmSeqId, message, dispatchMessage.executeCount)
    }

    fun logRed(buildId: String, containerHashId: String?, vmSeqId: String, message: String, executeCount: Int?) {
        printerLogPrinter.addRedLine(
            buildId,
            message,
            VMUtils.genStartVMTaskId(vmSeqId),
            containerHashId,
            executeCount ?: 1
        )
    }
}