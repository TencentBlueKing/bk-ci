/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.engine.common

import com.tencent.devops.common.api.util.timestampmilli
import java.time.LocalDateTime
import kotlin.random.Random

/**
 *
 * @version 1.0
 */
@Suppress("ALL")
object VMUtils {

    fun genStageId(seq: Int) = "stage-$seq"

    fun genStageIdForUser(seq: Int) = "stage_$seq"

    fun genStopVMTaskId(seq: Int) = "${getStopVmLabel()}$seq"

    fun genEndPointTaskId(seq: Int) = "${getEndLabel()}$seq"

    fun genVMTaskSeq(containerSeq: Int, taskSeq: Int): Int = containerSeq * 1000 + taskSeq

    fun genMatrixContainerSeq(matrixGroupId: Int, innerIndex: Int): Int = matrixGroupId * 1000 + innerIndex

    fun genMatrixJobId(groupJobId: String, innerSeq: Int) = "$groupJobId.$innerSeq"

    fun genStartVMTaskId(containerSeq: String) = "${getStartVmLabel()}$containerSeq"

    fun getStopVmLabel() = "stopVM-"

    fun getCleanVmLabel() = "Clean_Job#"

    fun getStartVmLabel() = "startVM-"

    fun getPrepareVmLabel() = "Prepare_Job#"

    fun getWaitLabel() = "Wait_Finish_Job#"

    fun getEndLabel() = "end-"

    fun getContainerJobId(randomSeed: Int, jobIdSet: MutableSet<String>): String {
        val random = Random(randomSeed)
        val sequence = StringBuilder()
        for (i in 0 until 3) {
            val randomChar = ('A'..'z').random(random)
            sequence.append(randomChar)
        }
        val jobId = "job_$sequence"
        return if (jobIdSet.contains(jobId)) {
            "${jobId}_${LocalDateTime.now().timestampmilli()}"
        } else {
            jobId
        }
    }

    fun getVmLabel(taskId: String) = when {
        taskId.startsWith(getStartVmLabel()) -> getStartVmLabel()
        taskId.startsWith(getStopVmLabel()) -> getStopVmLabel()
        taskId.startsWith(getEndLabel()) -> getEndLabel()
        else -> null
    }

    fun isVMTask(taskId: String) = taskId.startsWith(getStartVmLabel()) ||
        taskId.startsWith(getStopVmLabel()) ||
        taskId.startsWith(getEndLabel())

    fun isMatrixContainerId(containerId: String) = try {
        containerId.toInt() > 1000
    } catch (ignore: Throwable) {
        false
    }
}
