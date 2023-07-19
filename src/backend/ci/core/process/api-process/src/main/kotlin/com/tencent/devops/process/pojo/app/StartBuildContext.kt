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
import com.tencent.devops.common.api.util.Watcher
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
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.utils.BUILD_NO
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
import java.time.LocalDateTime

/**
 * 启动流水线上下文类，属于非线程安全类
 */
@Suppress("ComplexMethod", "LongParameterList")
data class StartBuildContext(
    val now: LocalDateTime = LocalDateTime.now(),
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
    val pipelineParamMap: MutableMap<String, BuildParameters>,
    val buildParameters: MutableList<BuildParameters>,
    val concurrencyGroup: String?,
    val pipelineSetting: PipelineSetting?,
    var buildNumAlias: String? = null, // 注意：该字段是在pipelineRuntimeService.startBuild 才赋值
    var buildNum: Int = 1, // 注意：该字段是在pipelineRuntimeService.startBuild 才赋值
    // 注意：该字段是在pipelineRuntimeService.startBuild 才赋值
    var buildNoType: BuildNoType? = null,
    // 注意：该字段在 PipelineContainerService.setUpTriggerContainer 中可能会被修改
    var currentBuildNo: Int? = null
) {
    val watcher: Watcher = Watcher("startBuild-$buildId")

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
        private const val DELTA = 16

        fun init(
            projectId: String,
            pipelineId: String,
            buildId: String,
            resourceVersion: Int,
            pipelineSetting: PipelineSetting? = null,
            realStartParamKeys: List<String>,
            pipelineParamMap: MutableMap<String, BuildParameters>,
            webHookStartParam: MutableMap<String, BuildParameters> = mutableMapOf(),
            triggerReviewers: List<String>? = null,
            currentBuildNo: Int? = null
        ): StartBuildContext {

            val params: Map<String, String> = pipelineParamMap.values.associate { it.key to it.value.toString() }

            val retryStartTaskId = params[PIPELINE_RETRY_START_TASK_ID]

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
                userId = params[PIPELINE_START_USER_ID]!!,
                triggerUser = params[PIPELINE_START_USER_NAME]!!,
                startType = StartType.valueOf(params[PIPELINE_START_TYPE]!!),
                parentBuildId = params[PIPELINE_START_PARENT_BUILD_ID],
                parentTaskId = params[PIPELINE_START_PARENT_BUILD_TASK_ID],
                channelCode = if (params[PIPELINE_START_CHANNEL] != null) {
                    ChannelCode.valueOf(params[PIPELINE_START_CHANNEL]!!)
                } else {
                    ChannelCode.BS
                },
                retryFailedContainer = params[PIPELINE_RETRY_ALL_FAILED_CONTAINER]?.toBoolean() ?: false,
                skipFailedTask = params[PIPELINE_SKIP_FAILED_TASK]?.toBoolean() ?: false,
                currentBuildNo = currentBuildNo,
                webhookInfo = getWebhookInfo(params),
                buildMsg = params[PIPELINE_BUILD_MSG]?.coerceAtMaxLength(MAX_LENGTH),
                buildParameters = genOriginStartParamsList(realStartParamKeys, pipelineParamMap),
                // 优化并发组逻辑，只在GROUP_LOCK时才保存进history表
                concurrencyGroup = pipelineSetting?.takeIf { it.runLockType == PipelineRunLockType.GROUP_LOCK }
                    ?.concurrencyGroup?.let {
                        val webhookParam = webHookStartParam.values.associate { p -> p.key to p.value.toString() }
                        val tConcurrencyGroup = EnvUtils.parseEnv(
                            it, PipelineVarUtil.fillContextVarMap(webhookParam.plus(params))
                        )
                        logger.info("[$pipelineId]|[$buildId]|ConcurrencyGroup=$tConcurrencyGroup")
                        tConcurrencyGroup
                    },
                triggerReviewers = triggerReviewers,
                startBuildStatus =
                if (triggerReviewers.isNullOrEmpty()) BuildStatus.QUEUE else BuildStatus.TRIGGER_REVIEWING,
                needUpdateStage = false,
                pipelineSetting = pipelineSetting,
                pipelineParamMap = pipelineParamMap
            )
        }

        private fun getWebhookInfo(params: Map<String, String>): WebhookInfo? {
            if (params[PIPELINE_START_TYPE] != StartType.WEB_HOOK.name) {
                return null
            }
            return WebhookInfo(
                codeType = params[BK_REPO_WEBHOOK_REPO_TYPE],
                nameWithNamespace = params[BK_REPO_WEBHOOK_REPO_NAME],
                webhookMessage = params[PIPELINE_WEBHOOK_COMMIT_MESSAGE],
                webhookRepoUrl = params[BK_REPO_WEBHOOK_REPO_URL],
                webhookType = params[PIPELINE_WEBHOOK_TYPE],
                webhookBranch = params[PIPELINE_WEBHOOK_BRANCH],
                webhookAliasName = params[BK_REPO_WEBHOOK_REPO_ALIAS_NAME],
                // GIT事件分为MR和MR accept,但是PIPELINE_WEBHOOK_EVENT_TYPE值只有MR
                webhookEventType = if (params[PIPELINE_WEBHOOK_TYPE] == CodeType.GIT.name) {
                    params[BK_REPO_GIT_WEBHOOK_EVENT_TYPE]
                } else {
                    params[PIPELINE_WEBHOOK_EVENT_TYPE]
                },
                refId = params[PIPELINE_WEBHOOK_REVISION],
                webhookCommitId = params[PIPELINE_WEBHOOK_REVISION],
                webhookMergeCommitSha = params[BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA],
                webhookSourceBranch = params[BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH],
                mrId = params[BK_REPO_GIT_WEBHOOK_MR_ID],
                mrIid = params[BK_REPO_GIT_WEBHOOK_MR_NUMBER],
                mrUrl = params[BK_REPO_GIT_WEBHOOK_MR_URL],
                repoAuthUser = params[BK_REPO_WEBHOOK_REPO_AUTH_USER],
                tagName = params[BK_REPO_GIT_WEBHOOK_TAG_NAME],
                issueIid = params[BK_REPO_GIT_WEBHOOK_ISSUE_IID],
                noteId = params[BK_REPO_GIT_WEBHOOK_NOTE_ID],
                reviewId = params[BK_REPO_GIT_WEBHOOK_REVIEW_ID]
            )
        }

        /**
         * 简易只为实现推送PipelineBuildStartEvent事件所需要的参数，不是全部
         */
        fun init4SendBuildStartEvent(
            userId: String,
            projectId: String,
            pipelineId: String,
            buildId: String,
            resourceVersion: Int,
            actionType: ActionType,
            executeCount: Int,
            firstTaskId: String,
            startType: StartType,
            startBuildStatus: BuildStatus
        ): StartBuildContext = StartBuildContext(
            now = LocalDateTime.now(),
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            resourceVersion = resourceVersion,
            actionType = actionType,
            executeCount = executeCount,
            userId = userId,
            firstTaskId = firstTaskId,
            startBuildStatus = startBuildStatus,
            startType = startType,
            parentBuildId = "",
            stageRetry = false,
            retryStartTaskId = null,
            triggerUser = "",
            parentTaskId = "",
            channelCode = ChannelCode.BS,
            retryFailedContainer = false,
            needUpdateStage = false,
            skipFailedTask = false,
            variables = emptyMap(),
            webhookInfo = null,
            buildMsg = null,
            triggerReviewers = null,
            pipelineParamMap = mutableMapOf(),
            buildParameters = mutableListOf(),
            concurrencyGroup = null,
            pipelineSetting = null

        )

        private const val CONTEXT_PREFIX = "variables."

        /**
         * 根据[realStartParamKeys]启动参数Key列表读取[pipelineParamMap]参数值来生成流水线启动变量列表，不包含其他
         */
        private fun genOriginStartParamsList(
            realStartParamKeys: List<String>,
            pipelineParamMap: MutableMap<String, BuildParameters>
        ): ArrayList<BuildParameters> {

            val originStartParams = ArrayList<BuildParameters>(realStartParamKeys.size + DELTA)

            // 将用户定义的变量增加上下文前缀的版本，与原变量相互独立
            val originStartContexts = HashMap<String, BuildParameters>(realStartParamKeys.size, /* loadFactor */ 1F)
            realStartParamKeys.forEach { key ->
                pipelineParamMap[key]?.let { param ->
                    originStartParams.add(param)
                    if (key.startsWith(CONTEXT_PREFIX)) {
                        originStartContexts[key] = param
                    } else {
                        val ctxKey = CONTEXT_PREFIX + key
                        originStartContexts[ctxKey] = param.copy(key = ctxKey)
                    }
                }
            }
            pipelineParamMap.putAll(originStartContexts)

            pipelineParamMap[BUILD_NO]?.let { buildNoParam -> originStartParams.add(buildNoParam) }
            pipelineParamMap[PIPELINE_BUILD_MSG]?.let { buildMsgParam -> originStartParams.add(buildMsgParam) }
            pipelineParamMap[PIPELINE_RETRY_COUNT]?.let { retryCountParam -> originStartParams.add(retryCountParam) }

            return originStartParams
        }
    }
}
