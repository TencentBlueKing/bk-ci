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

import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus

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

    private const val mutexPrefix = "互斥中(Mutex waiting)"

    fun clearMutexContainerName(container: Container) {
        if (container.name.startsWith(mutexPrefix)) {
            container.name = container.name.substring(mutexPrefix.length)
        }
    }

    fun getMutexFixedContainerName(containerName: String) =
        if (containerName.startsWith(mutexPrefix)) {
            containerName.substring(mutexPrefix.length)
        } else containerName

    fun setMutexWaitName(container: Container) {
        if (container.name.startsWith(mutexPrefix)) {
            return
        }

        container.name = "$mutexPrefix${container.name}"
    }

    fun getMutexWaitName(containerName: String) =
        if (containerName.startsWith(mutexPrefix)) {
            containerName
        } else {
            "$mutexPrefix$containerName"
        }

    private const val queuePrefix = "排队中(Queuing)"
    private const val reviewPrefix = "审核中(Pending)"

    fun clearQueueContainerName(container: Container) {
        if (container.name.startsWith(queuePrefix)) {
            container.name = container.name.substring(queuePrefix.length)
        } else if (container.name.startsWith(reviewPrefix)) {
            container.name = container.name.substring(reviewPrefix.length)
        }
    }

    fun getQueueFixedContainerName(containerName: String) =
        if (containerName.startsWith(queuePrefix)) {
            containerName.substring(queuePrefix.length)
        } else if (containerName.startsWith(reviewPrefix)) {
            containerName.substring(reviewPrefix.length)
        } else containerName

    fun setQueuingWaitName(container: Container, startBuildStatus: BuildStatus) {
        if (container.name.startsWith(queuePrefix) || container.name.startsWith(reviewPrefix)) {
            return
        }
        if (startBuildStatus == BuildStatus.TRIGGER_REVIEWING) {
            container.name = "$reviewPrefix${container.name}"
        } else {
            container.name = "$queuePrefix${container.name}"
        }
    }
}
