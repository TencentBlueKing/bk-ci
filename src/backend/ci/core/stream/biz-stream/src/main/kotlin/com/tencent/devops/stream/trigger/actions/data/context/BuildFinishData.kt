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

package com.tencent.devops.stream.trigger.actions.data.context

import com.tencent.devops.common.api.enums.BuildReviewType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState

/**
 * 构建结束的相关监听逻辑需要的数据
 * @param streamBuildId stream保存流水线构建记录的唯一id
 * @param eventId stream 保存event唯一id
 * @param version stream yaml版本
 * @param normalizedYaml 当前结束构建yaml原文
 * @param projectId 当前结束构建的蓝盾项目id
 * @param pipelineId 当前结束构建的流水线id
 * @param userId 当前构建发起人
 * @param status 当前构建状态
 * @param startTime 当前构建开始时间
 * @param stageId 当前构建stage id
 */
open class BuildFinishData(
    val streamBuildId: Long,
    val eventId: Long,
    val version: String?,
    val normalizedYaml: String,
    val projectId: String,
    val pipelineId: String,
    val userId: String,
    val buildId: String,
    val status: String,
    val startTime: Long?,
    val stageId: String?
)

fun BuildFinishData.getBuildStatus(): BuildStatus {
    return try {
        BuildStatus.valueOf(status)
    } catch (e: Exception) {
        BuildStatus.UNKNOWN
    }
}

fun BuildFinishData.isSuccess() =
    (getBuildStatus() == BuildStatus.SUCCEED || getBuildStatus() == BuildStatus.STAGE_SUCCESS)

fun BuildFinishData.getGitCommitCheckState(): StreamCommitCheckState {
    // stage审核的状态专门判断为成功
    return when (getBuildStatus()) {
        BuildStatus.REVIEWING -> {
            StreamCommitCheckState.PENDING
        }
        //  审核成功的阶段性状态
        BuildStatus.REVIEW_PROCESSED -> {
            StreamCommitCheckState.PENDING
        }
        else -> {
            if (isSuccess()) {
                StreamCommitCheckState.SUCCESS
            } else {
                StreamCommitCheckState.FAILURE
            }
        }
    }
}

/**
 * stage阶段构建结束需要的数据
 * @param reviewType 当前stage审核状态，主要针对审核事件
 */
class BuildFinishStageData(
    streamBuildId: Long,
    eventId: Long,
    version: String?,
    normalizedYaml: String,
    projectId: String,
    pipelineId: String,
    userId: String,
    buildId: String,
    status: String,
    startTime: Long?,
    stageId: String?,
    val reviewType: BuildReviewType
) : BuildFinishData(
    streamBuildId,
    eventId,
    version,
    normalizedYaml,
    projectId,
    pipelineId,
    userId,
    buildId,
    status,
    startTime,
    stageId
)
