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

package com.tencent.devops.process.engine.utils

import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.BK_MUTEX_WAITING
import com.tencent.devops.process.constant.ProcessMessageCode.BK_PENDING
import com.tencent.devops.process.constant.ProcessMessageCode.BK_QUEUING

object ContainerUtils {

    fun getContainerStartupKey(
        pipelineId: String,
        buildId: String,
        containerId: String
    ) = "container:startup:$pipelineId:$buildId:$containerId"

    fun getContainerRunEvenCancelTaskKey(
        pipelineId: String,
        buildId: String,
        containerId: String
    ) = "container:taskEvenCancel:$pipelineId:$buildId:$containerId"

    fun isNormalContainerEnable(normalContainer: NormalContainer): Boolean {
        return if (normalContainer.jobControlOption != null) {
            normalContainer.jobControlOption!!.enable
        } else {
            normalContainer.enableSkip == false
        }
    }

    fun isVMBuildContainerEnable(container: VMBuildContainer): Boolean {
        return container.jobControlOption == null || container.jobControlOption!!.enable
    }

    private fun getMutexPrefix() = I18nUtil.getCodeLanMessage(BK_MUTEX_WAITING)

    fun getMutexFixedContainerName(containerName: String) =
        if (containerName.startsWith(getMutexPrefix())) {
            containerName.substring(getMutexPrefix().length)
        } else containerName

    fun getMutexWaitName(containerName: String) =
        if (containerName.startsWith(getMutexPrefix())) {
            containerName
        } else {
            "${getMutexPrefix()}$containerName"
        }

    private fun getQueuePrefix() = I18nUtil.getCodeLanMessage(BK_QUEUING)
    private fun getReviewPrefix() = I18nUtil.getCodeLanMessage(BK_PENDING)

    fun getClearedQueueContainerName(containerName: String): String {
        return if (containerName.startsWith(getQueuePrefix())) {
            containerName.substring(getQueuePrefix().length)
        } else if (containerName.startsWith(getReviewPrefix())) {
            containerName.substring(getReviewPrefix().length)
        } else containerName
    }

    fun getQueuingWaitName(containerName: String, startBuildStatus: BuildStatus): String {
        if (containerName.startsWith(getQueuePrefix()) || containerName.startsWith(getReviewPrefix())) {
            return containerName
        }
        return if (startBuildStatus == BuildStatus.TRIGGER_REVIEWING) {
            "${getReviewPrefix()}$containerName"
        } else {
            "${getQueuePrefix()}$containerName"
        }
    }
}
