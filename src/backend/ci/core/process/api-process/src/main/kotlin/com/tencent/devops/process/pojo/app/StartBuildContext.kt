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

package com.tencent.devops.process.pojo.app

import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildNoType
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.utils.DependOnUtils
import com.tencent.devops.process.utils.PIPELINE_RETRY_ALL_FAILED_CONTAINER
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PIPELINE_RETRY_START_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_SKIP_FAILED_TASK
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME

/**
 * 启动流水线上下文类，属于非线程安全类
 */
data class StartBuildContext(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val resourceVersion: Int,
    val actionType: ActionType,
    val executeCount: Int = 1,
    val stageRetry: Boolean,
    val retryStartTaskId: String?,
    var firstTaskId: String,
    var containerSeq: Int = 0,
    var taskCount: Int = 0,
    val userId: String,
    val triggerUser: String,
    val startType: StartType,
    val parentBuildId: String?,
    val parentTaskId: String?,
    val channelCode: ChannelCode,
    val retryFailedContainer: Boolean,
    var needUpdateStage: Boolean,
    val skipFailedTask: Boolean, // 跳过失败的插件 配合 stageRetry 可判断是否跳过所有失败插件
    var buildNoType: BuildNoType? = null,
    var currentBuildNo: Int? = null
) {

    /**
     * 检查Stage是否属于失败重试[stageRetry]时，当前[stage]是否需要跳过
     */
    fun needSkipWhenStageFailRetry(stage: Stage): Boolean {
        return if (needRerun(stage)) { // finally stage 不会跳过, 当前stage是要失败重试的不会跳过
            false
        } else if (!stageRetry) { // 不是stage失败重试的动作也不会跳过
            false
        } else { // 如果失败重试的不是当前stage，并且当前stage已经是完成状态，则跳过
            BuildStatus.parse(stage.status).isFinish()
        }
    }

    fun needSkipContainerWhenFailRetry(stage: Stage, container: Container): Boolean {
        val containerStatus = BuildStatus.parse(container.status)
        return if (needRerun(stage)) { // finally stage 不会跳过, 当前stage是要失败重试的不会跳过，不会跳过
            false
        } else if (!containerStatus.isFailure() && !containerStatus.isCancel()) { // 跳过失败和被取消的其他job
            false
        } else { // 插件失败重试的，会跳过
            !retryStartTaskId.isNullOrBlank()
        }
    }

    fun needSkipTaskWhenRetry(stage: Stage, container: Container, taskId: String?): Boolean {
        return when {
            stage.finally -> {
                false // finally stage 不会跳过
            }
            stage.id!! == retryStartTaskId -> { // 失败重试的Stage，不会跳过
                false
            }
            retryStartTaskId.isNullOrBlank() -> { // rebuild or start 不会跳过
                false
            }
            isRetryDependOnContainer(container) -> { // 开启dependOn Job并状态是跳过的不会跳过
                false
            }
            else -> { // 当前插件不是要失败重试或要跳过的插件，会跳过
                retryStartTaskId != taskId
            }
        }
    }

    fun inSkipStage(stage: Stage, atom: Element): Boolean {
        return if (skipFailedTask && retryStartTaskId == atom.id) {
            true
        } else { // 如果是全部跳过Stage下所有失败插件的，则这个插件必须是处于失败的状态
            skipFailedTask && (stage.id == retryStartTaskId && BuildStatus.parse(atom.status).isFailure())
        }
    }

    /**
     * 是否是要重试的失败容器
     */
    fun isRetryFailedContainer(stage: Stage, container: Container): Boolean {
        return when {
            stage.finally -> {
                if (stage.id == retryStartTaskId) { // finallyStage的重试
                    retryFailedContainer && BuildStatus.parse(container.status).isSuccess() // 只重试失败的Job
                } else {
                    false
                }
            }
            isRetryDependOnContainer(container) -> false
            else -> retryFailedContainer && BuildStatus.parse(container.status).isSuccess()
        }
    }

    // 失败重试,跳过的dependOn容器也应该被执行
    fun isRetryDependOnContainer(container: Container): Boolean {
        return DependOnUtils.enableDependOn(container) && BuildStatus.parse(container.status) == BuildStatus.SKIP
    }

    fun needRerun(stage: Stage): Boolean {
        return stage.finally || retryStartTaskId == null || stage.id!! == retryStartTaskId
    }

    fun needRerunTask(stage: Stage, container: Container): Boolean {
        return needRerun(stage) || isRetryDependOnContainer(container)
    }

    companion object {

        fun init(
            projectId: String,
            pipelineId: String,
            buildId: String,
            resourceVersion: Int,
            params: Map<String, Any>
        ): StartBuildContext {

            val retryStartTaskId = params[PIPELINE_RETRY_START_TASK_ID]?.toString()

            val (actionType, executeCount, isStageRetry) = if (params[PIPELINE_RETRY_COUNT] != null) {
                val count = try {
                    params[PIPELINE_RETRY_COUNT].toString().trim().toInt().coerceAtLeast(0) // 不允许负数
                } catch (ignored: NumberFormatException) {
                    0
                }
                Triple(ActionType.RETRY, count + 1, retryStartTaskId?.startsWith("stage-") == true)
            } else {
                Triple(ActionType.START, 1, false)
            }

            return StartBuildContext(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                resourceVersion = resourceVersion,
                actionType = actionType,
                executeCount = executeCount,
                firstTaskId = params[PIPELINE_START_TASK_ID]?.toString() ?: "",
                stageRetry = isStageRetry,
                retryStartTaskId = retryStartTaskId,
                userId = params[PIPELINE_START_USER_ID].toString(),
                triggerUser = params[PIPELINE_START_USER_NAME].toString(),
                startType = StartType.valueOf(params[PIPELINE_START_TYPE] as String),
                parentBuildId = params[PIPELINE_START_PARENT_BUILD_ID]?.toString(),
                parentTaskId = params[PIPELINE_START_PARENT_BUILD_TASK_ID]?.toString(),
                channelCode = if (params[PIPELINE_START_CHANNEL] != null) {
                    ChannelCode.valueOf(params[PIPELINE_START_CHANNEL].toString())
                } else {
                    ChannelCode.BS
                },
                retryFailedContainer = params[PIPELINE_RETRY_ALL_FAILED_CONTAINER]?.toString()?.toBoolean() ?: false,
                skipFailedTask = params[PIPELINE_SKIP_FAILED_TASK]?.toString()?.toBoolean() ?: false,
                needUpdateStage = false
            )
        }
    }
}
