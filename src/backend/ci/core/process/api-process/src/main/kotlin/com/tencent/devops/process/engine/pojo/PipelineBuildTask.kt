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

package com.tencent.devops.process.engine.pojo

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.api.pojo.ErrorType
import java.time.LocalDateTime

data class PipelineBuildTask(
    val projectId: String,
    val pipelineId: String,
    val templateId: String? = null,
    val buildId: String,
    val stageId: String,
    val containerId: String,
    val containerHashId: String?,
    val containerType: String,
    val taskSeq: Int,
    val taskId: String,
    val taskName: String,
    val taskType: String,
    val taskAtom: String,
    var status: BuildStatus,
    var taskParams: MutableMap<String, Any>,
    val additionalOptions: ElementAdditionalOptions?,
    var executeCount: Int? = 1,
    var starter: String,
    val approver: String?,
    var subProjectId: String?,
    var subBuildId: String?,
    var startTime: LocalDateTime? = null,
    var endTime: LocalDateTime? = null,
    var errorType: ErrorType? = null,
    var errorCode: Int? = null,
    var errorMsg: String? = null,
    val atomCode: String? = null,
    val stepId: String? = null,
    var totalTime: Long? = null
) {
    fun getTaskParam(paramName: String): String {
        return if (taskParams[paramName] != null) {
            taskParams[paramName].toString().trim()
        } else {
            ""
        }
    }
}
