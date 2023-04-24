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

import com.tencent.devops.common.api.constant.coerceAtMaxLength
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildNoType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_IID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_NUMBER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_URL
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_NOTE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_TAG_NAME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_ALIAS_NAME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_AUTH_USER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_NAME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_TYPE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_URL
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_COMMIT_MESSAGE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REVISION
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TYPE
import com.tencent.devops.process.pojo.code.WebhookInfo
import com.tencent.devops.process.utils.DependOnUtils
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
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
import com.tencent.devops.process.utils.PipelineVarUtil
import org.slf4j.LoggerFactory

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
    val variables: Map<String, String>,
    val startBuildStatus: BuildStatus,
    val webhookInfo: WebhookInfo?,
    val buildMsg: String?,
    val triggerReviewers: List<String>?,
    val buildParameters: MutableList<BuildParameters>,
    val concurrencyGroup: String?,
    val buildNumAlias: String? = null,
    var buildNum: Int = 1, // 注意：该字段是在pipelineRuntimeService.startBuild 才赋值
    // 注意：该字段是在pipelineRuntimeService.startBuild 才赋值
    var buildNoType: BuildNoType? = null,
    // 注意：该字段在 PipelineContainerService.setUpTriggerContainer 中可能会被修改
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
    private fun isRetryDependOnContainer(container: Container): Boolean {
        return DependOnUtils.enableDependOn(container) && BuildStatus.parse(container.status) == BuildStatus.SKIP
    }

    private fun needRerun(stage: Stage): Boolean {
        return stage.finally || retryStartTaskId == null || stage.id!! == retryStartTaskId
    }

    fun needRerunTask(stage: Stage, container: Container): Boolean {
        return needRerun(stage) || isRetryDependOnContainer(container)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StartBuildContext::class.java)
        private const val MAX_LENGTH = 255

        fun init(
            projectId: String,
            pipelineId: String,
            buildId: String,
            resourceVersion: Int,
            params: Map<String, String>,
            buildParameters: MutableList<BuildParameters>,
            buildNumAlias: String? = null,
            startBuildStatus: BuildStatus? = null,
            concurrencyGroup: String? = null,
            triggerReviewers: List<String>? = null,
            currentBuildNo: Int? = null
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
                variables = params,
                resourceVersion = resourceVersion,
                actionType = actionType,
                executeCount = executeCount,
                firstTaskId = params[PIPELINE_START_TASK_ID] ?: "",
                stageRetry = isStageRetry,
                retryStartTaskId = retryStartTaskId,
                userId = params[PIPELINE_START_USER_ID].toString(),
                triggerUser = params[PIPELINE_START_USER_NAME].toString(),
                startType = StartType.valueOf(params[PIPELINE_START_TYPE] as String),
                parentBuildId = params[PIPELINE_START_PARENT_BUILD_ID],
                parentTaskId = params[PIPELINE_START_PARENT_BUILD_TASK_ID],
                channelCode = if (params[PIPELINE_START_CHANNEL] != null) {
                    ChannelCode.valueOf(params[PIPELINE_START_CHANNEL].toString())
                } else {
                    ChannelCode.BS
                },
                retryFailedContainer = params[PIPELINE_RETRY_ALL_FAILED_CONTAINER]?.toBoolean() ?: false,
                skipFailedTask = params[PIPELINE_SKIP_FAILED_TASK]?.toBoolean() ?: false,
                currentBuildNo = currentBuildNo,
                webhookInfo = getWebhookInfo(params),
                buildMsg = params[PIPELINE_BUILD_MSG]?.coerceAtMaxLength(MAX_LENGTH),
                buildParameters = buildParameters,
                concurrencyGroup = concurrencyGroup?.let { self ->
                    val tConcurrencyGroup = EnvUtils.parseEnv(self, PipelineVarUtil.fillContextVarMap(params))
                    logger.info("[$pipelineId]|[$buildId]|ConcurrencyGroup=$tConcurrencyGroup")
                    tConcurrencyGroup
                },
                triggerReviewers = triggerReviewers,
                startBuildStatus = startBuildStatus ?: if (triggerReviewers.isNullOrEmpty())
                    BuildStatus.QUEUE else BuildStatus.TRIGGER_REVIEWING,
                needUpdateStage = false,
                buildNumAlias = buildNumAlias
            )
        }

        private fun getWebhookInfo(params: Map<String, Any>): WebhookInfo? {
            if (params[PIPELINE_START_TYPE] != StartType.WEB_HOOK.name) {
                return null
            }
            return WebhookInfo(
                codeType = params[BK_REPO_WEBHOOK_REPO_TYPE]?.toString(),
                nameWithNamespace = params[BK_REPO_WEBHOOK_REPO_NAME]?.toString(),
                webhookMessage = params[PIPELINE_WEBHOOK_COMMIT_MESSAGE]?.toString(),
                webhookRepoUrl = params[BK_REPO_WEBHOOK_REPO_URL]?.toString(),
                webhookType = params[PIPELINE_WEBHOOK_TYPE]?.toString(),
                webhookBranch = params[PIPELINE_WEBHOOK_BRANCH]?.toString(),
                webhookAliasName = params[BK_REPO_WEBHOOK_REPO_ALIAS_NAME]?.toString(),
                // GIT事件分为MR和MR accept,但是PIPELINE_WEBHOOK_EVENT_TYPE值只有MR
                webhookEventType = if (params[PIPELINE_WEBHOOK_TYPE] == CodeType.GIT.name) {
                    params[BK_REPO_GIT_WEBHOOK_EVENT_TYPE]?.toString()
                } else {
                    params[PIPELINE_WEBHOOK_EVENT_TYPE]?.toString()
                },
                refId = params[PIPELINE_WEBHOOK_REVISION]?.toString(),
                webhookCommitId = params[PIPELINE_WEBHOOK_REVISION] as String?,
                webhookMergeCommitSha = params[BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA]?.toString(),
                webhookSourceBranch = params[BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH]?.toString(),
                mrId = params[BK_REPO_GIT_WEBHOOK_MR_ID]?.toString(),
                mrIid = params[BK_REPO_GIT_WEBHOOK_MR_NUMBER]?.toString(),
                mrUrl = params[BK_REPO_GIT_WEBHOOK_MR_URL]?.toString(),
                repoAuthUser = params[BK_REPO_WEBHOOK_REPO_AUTH_USER]?.toString(),
                tagName = params[BK_REPO_GIT_WEBHOOK_TAG_NAME]?.toString(),
                issueIid = params[BK_REPO_GIT_WEBHOOK_ISSUE_IID]?.toString(),
                noteId = params[BK_REPO_GIT_WEBHOOK_NOTE_ID]?.toString(),
                reviewId = params[BK_REPO_GIT_WEBHOOK_REVIEW_ID]?.toString()
            )
        }
    }
}
