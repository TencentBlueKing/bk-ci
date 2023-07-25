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

import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import com.tencent.devops.process.pojo.code.WebhookInfo

data class BuildInfo(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val version: Int,
    val buildNum: Int,
    val trigger: String,
    var status: BuildStatus,
    val queueTime: Long,
    val executeTime: Long,
    val startUser: String, // 真正用来执行构建的人的身份（一般像Git触发，有可能]与触发人不一样，因为Git平台账号不一定是人）
    val triggerUser: String, // 真正的触发人（不一定是人，也可能是机器账号，比如git平台账号）
    val startTime: Long?,
    var endTime: Long?,
    val taskCount: Int,
    val firstTaskId: String,
    val parentBuildId: String?,
    val parentTaskId: String?,
    val channelCode: ChannelCode,
    val buildParameters: List<BuildParameters>?,
    var errorInfoList: List<ErrorInfo>?,
    val stageStatus: List<BuildStageStatus>?,
    @Deprecated("后续只用executeCount做判断")
    val retryFlag: Boolean? = null,
    val executeCount: Int? = 1,
    val concurrencyGroup: String? = null,
    val webhookInfo: WebhookInfo? = null,
    val buildMsg: String? = null,
    val material: List<PipelineBuildMaterial>? = null,
    val remark: String? = null,
    val errorType: Int? = null,
    val errorCode: Int? = null,
    val errorMsg: String? = null
) {

    fun isFinish() = when {
        status.name == BuildStatus.STAGE_SUCCESS.name &&
            endTime != null &&
            endTime!! > 0 &&
            startTime != null &&
            endTime!! > startTime
        -> true
        else -> status.isFinish()
    }

    fun isSuccess() = when {
        status.name == BuildStatus.STAGE_SUCCESS.name &&
            endTime != null &&
            endTime!! > 0 &&
            startTime != null &&
            endTime!! > startTime
        -> true
        else -> status.isSuccess()
    }

    fun isFailure() = status.isFailure()

    fun isCancel() = status.isCancel()

    fun isStageSuccess() = status == BuildStatus.STAGE_SUCCESS

    fun isTriggerReviewing() = status == BuildStatus.TRIGGER_REVIEWING

    fun isReadyToRun() = status.isReadyToRun()
}
