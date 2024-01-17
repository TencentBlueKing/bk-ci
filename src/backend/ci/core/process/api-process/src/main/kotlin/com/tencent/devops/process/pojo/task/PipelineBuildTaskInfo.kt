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

package com.tencent.devops.process.pojo.task

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.api.pojo.ErrorType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "")
data class PipelineBuildTaskInfo(
    @Schema(title = "项目id", required = false)
    val projectId: String,
    @Schema(title = "流水线id", required = false)
    val pipelineId: String,
    @Schema(title = "模板id", required = false)
    val templateId: String? = null,
    @Schema(title = "构建id", required = false)
    val buildId: String,
    @Schema(title = "阶段id", required = false)
    val stageId: String,
    @Schema(title = "容器id", required = false)
    val containerId: String,
    @Schema(title = "容器hash id", required = false)
    val containerHashId: String?,
    @Schema(title = "容器类型", required = false)
    val containerType: String,
    @Schema(title = "任务序列", required = false)
    val taskSeq: Int,
    @Schema(title = "任务id", required = false)
    val taskId: String,
    @Schema(title = "任务名称", required = false)
    val taskName: String,
    @Schema(title = "任务类型", required = false)
    val taskType: String,
    @Schema(title = "任务atom代码", required = false)
    val taskAtom: String,
    @Schema(title = "状态", required = false)
    var status: BuildStatus,
    @Schema(title = "任务参数集合", required = false)
    val taskParams: MutableMap<String, Any>,
    @Schema(title = "其他选项", required = false)
    val additionalOptions: ElementAdditionalOptions?,
    @Schema(title = "执行次数", required = false)
    val executeCount: Int? = 1,
    @Schema(title = "启动者", required = false)
    var starter: String,
    @Schema(title = "审批人", required = false)
    val approver: String?,
    @Schema(title = "子构建id", required = false)
    var subBuildId: String?,
    @Schema(title = "启动时间", required = false)
    val startTime: Long? = null,
    @Schema(title = "结束时间", required = false)
    val endTime: Long? = null,
    @Schema(title = "错误类型", required = false)
    var errorType: ErrorType? = null,
    @Schema(title = "错误代码", required = false)
    var errorCode: Int? = null,
    @Schema(title = "错误信息", required = false)
    var errorMsg: String? = null
) {
    fun getTaskParam(paramName: String): String {
        return if (taskParams[paramName] != null) {
            taskParams[paramName].toString().trim()
        } else {
            ""
        }
    }
}
