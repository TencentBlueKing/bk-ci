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
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_URL
import com.tencent.devops.common.pipeline.utils.PipelineParamUtils
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
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_TRIGGER_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_COMMIT_MESSAGE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REVISION
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TYPE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_NODE_ID
import com.tencent.devops.process.pojo.code.WebhookInfo
import com.tencent.devops.process.utils.BK_CI_MATERIAL_ID
import com.tencent.devops.process.utils.BK_CI_MATERIAL_NAME
import com.tencent.devops.process.utils.BK_CI_MATERIAL_URL
import com.tencent.devops.process.utils.BUILD_NO
import com.tencent.devops.process.utils.DependOnUtils
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.process.utils.PIPELINE_RETRY_ALL_FAILED_CONTAINER
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PIPELINE_RETRY_MATRIX_CONTAINER_ID
import com.tencent.devops.process.utils.PIPELINE_RETRY_MATRIX_GROUP_ID
import com.tencent.devops.process.utils.PIPELINE_RETRY_RUNNING_BUILD
import com.tencent.devops.process.utils.PIPELINE_RETRY_START_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_RETRY_TASK_IN_CONTAINER_ID
import com.tencent.devops.process.utils.PIPELINE_RETRY_TASK_IN_STAGE_ID
import com.tencent.devops.process.utils.PIPELINE_SKIP_FAILED_TASK
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PROJECT_ID
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
    val versionName: String?,
    val yamlVersion: String?,
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
    var currentBuildNo: Int? = null,
    val debug: Boolean,
    val debugModelStr: String?,
    // 重试正在运行时的构建
    val retryOnRunningBuild: Boolean = false,
    // 重试插件所属的stageId
    val retryTaskInStageId: String? = null,
    // 重试插件对应的containerId
    val retryTaskInContainerId: String? = null,
    // 触发事件标识
    val triggerEventType: String? = null,
    // 草稿版本号
    val draftVersion: Int? = null,
    // 矩阵局部重试：目标父矩阵容器ID，非空表示本次为矩阵组内的局部重试
    val retryMatrixGroupId: String? = null,
    // 矩阵局部重试：目标子容器ID，为空且 retryFailedContainer=true 时表示重试该矩阵下所有失败子Job
    val retryMatrixContainerId: String? = null
) {
    val watcher: Watcher = Watcher("startBuild-$buildId")

    /**
     * 局部重试遍历 Stage 时，是否已经到达重试目标 Stage（含目标本身）。
     * 用于跳过前序已完成 Stage，避免 UNEXEC/CANCELED 等把 checkIn 清空后再次审核。
     */
    var reachedRetryTargetStage: Boolean = false

    /**
     * 是否为矩阵组内的局部重试（子Job/子插件重试、跳过或矩阵级批量重试）
     */
    fun isMatrixPartialRetry(): Boolean = !retryMatrixGroupId.isNullOrBlank()

    /**
     * 当前[container]是否为本次矩阵局部重试所针对的父矩阵容器
     */
    fun isRetryMatrixGroup(container: Container): Boolean {
        return isMatrixPartialRetry() && retryMatrixGroupId == container.id
    }

    /**
     * 当前[stage]是否为本次局部重试的目标 Stage（Stage 级重试 / 任务所在 Stage / 矩阵所在 Stage）
     */
    fun isRetryTargetStage(stage: Stage): Boolean {
        val stageId = stage.id ?: return false
        if (stageId == retryStartTaskId) return true
        if (!retryTaskInStageId.isNullOrBlank()) return stageId == retryTaskInStageId
        if (retryStartTaskId.isNullOrBlank()) return false
        return stage.containers.any { containerContainsRetryStart(it) }
    }

    private fun containerContainsRetryStart(container: Container): Boolean {
        if (container.id == retryStartTaskId) return true
        if (container.elements.any { it.id == retryStartTaskId || it.stepId == retryStartTaskId }) return true
        return container.fetchGroupContainers()?.any { containerContainsRetryStart(it) } == true
    }

    /**
     * 前序 Stage 是否已经完成准入审核。status 异常时仍据此跳过，避免再次弹出审核。
     */
    fun hasCompletedStageReview(stage: Stage): Boolean {
        val checkIn = stage.checkIn ?: return false
        return checkIn.manualTrigger == true &&
            !checkIn.reviewGroups.isNullOrEmpty() &&
            checkIn.groupToReview() == null
    }

    /**
     * 是否允许 resetBuildOption / 回写 checkIn。全新构建，或已到达重试点之后才允许。
     * 到达之前一律禁止，避免前序 Stage 因 UNEXEC/CANCELED 被标脏后清空审核组再次弹审核。
     */
    fun allowResetStageReview(): Boolean = retryStartTaskId.isNullOrBlank() || reachedRetryTargetStage

    /**
     * 检查当前[stage]在失败重试时是否需要跳过。
     * Stage 失败重试：非目标且已完成的 Stage 跳过。
     * 任务/Job/矩阵局部重试：重试点之前已完成（或已审过）的 Stage 整段跳过，禁止刷新以免清空 checkIn。
     */
    fun needSkipWhenStageFailRetry(stage: Stage): Boolean {
        return if (needRerunStage(stage)) { // finally stage 不会跳过, 当前stage是要失败重试的不会跳过
            false
        } else if (stageRetry) {
            // Stage 失败重试：非目标且已完成（或已审过）的 Stage 跳过
            BuildStatus.parse(stage.status).isFinish() || hasCompletedStageReview(stage)
        } else if (!retryStartTaskId.isNullOrBlank() && !reachedRetryTargetStage && !isRetryTargetStage(stage)) {
            // 任务/Job/矩阵局部重试：重试点之前已完成（或已审过）的 Stage 整段跳过，禁止刷新以免清空 checkIn
            BuildStatus.parse(stage.status).isFinish() || hasCompletedStageReview(stage)
        } else {
            false
        }
    }

    fun needSkipContainerWhenFailRetry(stage: Stage, container: Container): Boolean {
        val containerStatus = BuildStatus.parse(container.status)
        return if (needRerunStage(stage)) { // finally stage 不会跳过, 当前stage是要失败重试的不会跳过，不会跳过
            false
        } else if (!containerStatus.isFailure() && !containerStatus.isCancel()) { // 跳过失败和被取消的其他job
            false
        } else if (containerStatus.isCancel() && dependOnRetryContainer(stage, container)) {
            // #13407 上一次被取消的Job，若依赖本次重试插件所在的Job，则其依赖项本次会重新执行，
            // 它必须跟着唤起重跑。否则该Job会带着上一次残留的CANCELED参与本次Stage状态聚合，
            // 使构建无论重试多少次都停在取消态
            false
        } else { // 插件失败重试的，会跳过
            !retryStartTaskId.isNullOrBlank()
        }
    }

    /**
     * 判断上一次被取消的[container]是否直接或间接依赖本次重试插件[retryStartTaskId]所在的Job。
     * dependOn关系只在同一Stage内声明，重试插件不在本[stage]时视为无依赖关系，保持原有跳过逻辑。
     */
    private fun dependOnRetryContainer(stage: Stage, container: Container): Boolean {
        if (retryStartTaskId.isNullOrBlank()) {
            return false
        }
        val retryContainerId = stage.containers.firstOrNull { candidate ->
            candidate.elements.any { it.id == retryStartTaskId }
        }?.id ?: return false
        return DependOnUtils.dependOnContainer(
            stage = stage,
            container = container,
            targetContainerId = retryContainerId
        )
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

            // 单插件失败重试/跳过时，被操作插件之后、同Job内的后续插件也需要重新排队，
            // 以便引擎重新评估其运行条件（如 PRE_TASK_FAILED_ONLY 等）：
            // 重试场景——被重试插件重跑后，下游按最新结果重新判定；
            // 跳过场景——被跳过插件置为SKIP（不再算失败）后，下游同样需要据此重新判定，
            // 否则仅因上游失败才执行过的下游插件（如失败通知）会保留旧的失败态，导致跳过后Job仍为失败。
            isAfterRetryTaskInSameContainer(container, taskId) -> {
                false
            }

            else -> { // 当前插件不是要失败重试或要跳过的插件，会跳过
                retryStartTaskId != taskId
            }
        }
    }

    /**
     * 判断[taskId]对应的插件是否与被重试/跳过的插件[retryStartTaskId]处于同一个[container]，
     * 且执行顺序排在其之后。用于单插件失败重试/跳过时一并重排后续插件，从而重新评估运行条件。
     * post-action 任务交由原有 post 重试逻辑处理，这里不纳入，避免误重跑前序插件的 post。
     */
    private fun isAfterRetryTaskInSameContainer(container: Container, taskId: String?): Boolean {
        if (taskId.isNullOrBlank() || retryStartTaskId.isNullOrBlank()) {
            return false
        }
        val elements = container.elements
        val retryIndex = elements.indexOfFirst { it.id == retryStartTaskId }
        if (retryIndex < 0) { // 要重试的插件不在当前容器（其它并行Job），保持原有跳过逻辑
            return false
        }
        val currentElement = elements.firstOrNull { it.id == taskId } ?: return false
        if (currentElement.additionalOptions?.elementPostInfo != null) { // post任务交由原有逻辑处理
            return false
        }
        return elements.indexOf(currentElement) > retryIndex
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

    fun needRerunStage(stage: Stage): Boolean {
        // 重试所在stage的后续stage都需要
        return stage.finally || retryStartTaskId == null || stage.id!! == retryStartTaskId
    }

    fun needRerunTask(stage: Stage, container: Container): Boolean {
        return needRerunStage(stage) || isRetryDependOnContainer(container)
    }

    /**
     * #13500 任务/Job/矩阵局部重试：目标 Stage 内已经成功或跳过的兄弟 Job 整段跳过。
     * 这些 Job 若再走进 prepareBuildContainerTasks，UNEXEC/CANCELED 残留会把整 Job 重置并再次下发
     * （例如先跳过 executeCount=N 的 Job，再重试更早一轮失败 Job 时，把 N 已跑完的 Job 重新准备环境）。
     *
     * 不改 [needSkipContainerWhenFailRetry]：失败/取消 Job 仍走 #2318，取消+dependOn 留给其它修复。
     * 本次重试点所在 Job、Stage 级重试、finally、dependOn 被跳过须重评的 Job、矩阵局部重试父容器，都不跳过。
     */
    fun needSkipCompletedContainerWhenTaskRetry(stage: Stage, container: Container): Boolean {
        if (retryStartTaskId.isNullOrBlank()) return false
        if (needRerunStage(stage)) return false
        if (isRetryDependOnContainer(container)) return false
        if (isRetryMatrixGroup(container)) return false
        if (containerContainsRetryStart(container)) return false
        return BuildStatus.parse(container.status).isSuccess()
    }

    /**
     * 应该跳过刷新stage状态当运行时重试时
     */
    fun shouldSkipRefreshWhenRetryRunning(stage: Stage): Boolean {
        return retryOnRunningBuild && retryTaskInStageId != stage.id
    }

    /**
     * 应该跳过刷新container状态当运行时重试时
     */
    fun shouldSkipRefreshWhenRetryRunning(stage: Stage, container: Container): Boolean {
        // 运行中矩阵局部重试豁免：目标失败子容器挂在矩阵父容器的 groupContainers 下，
        // 模型遍历到的是矩阵父容器（container.id 为父ID，≠ retryTaskInContainerId 子容器ID），
        // 若按下方普通规则(id不同即跳过)父容器会被整体跳过，导致 prepareBuildContainerTasks 的矩阵分支不执行、
        // 目标子Job无法在同一执行次数下就地重置并重新下发。故本次为该父矩阵容器下的局部重试时，父容器必须参与刷新。
        if (retryOnRunningBuild && isRetryMatrixGroup(container)) return false
        // 运行中重试：直接或间接依赖被重试Job的container需要一起刷新
        val targetContainerId = retryTaskInContainerId
        return if (retryOnRunningBuild && container.id != targetContainerId && !targetContainerId.isNullOrBlank()) {
            !DependOnUtils.dependOnContainer(
                stage = stage,
                container = container,
                targetContainerId = targetContainerId
            )
        } else {
            false
        }
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
            versionName: String?,
            yamlVersion: String?,
            modelStr: String,
            debug: Boolean,
            pipelineSetting: PipelineSetting? = null,
            realStartParamKeys: List<String>,
            pipelineParamMap: MutableMap<String, BuildParameters>,
            webHookStartParam: MutableMap<String, BuildParameters> = mutableMapOf(),
            triggerReviewers: List<String>? = null,
            currentBuildNo: Int? = null,
            draftVersion: Int? = null
        ): StartBuildContext {
            val buildParam = genOriginStartParamsList(realStartParamKeys, pipelineParamMap)
            val params: Map<String, String> = pipelineParamMap.values.associate { it.key to it.value.toString() }
            // 解析出定义的流水线变量
            val retryStartTaskId = params[PIPELINE_RETRY_START_TASK_ID]

            val retryOnRunningBuild = params[PIPELINE_RETRY_RUNNING_BUILD]?.toBoolean() ?: false

            val (actionType, executeCount, isStageRetry) = if (params[PIPELINE_RETRY_COUNT] != null) {
                val count = try {
                    params[PIPELINE_RETRY_COUNT].toString().trim().toInt().coerceAtLeast(0) // 不允许负数
                } catch (ignored: NumberFormatException) {
                    0
                }
                val retryCount = if (retryOnRunningBuild) {
                    count
                } else {
                    count + 1
                }
                Triple(ActionType.RETRY, retryCount, retryStartTaskId?.startsWith("stage-") == true)
            } else {
                Triple(ActionType.START, 1, false)
            }
            val channelCode = if (params[PIPELINE_START_CHANNEL] != null) {
                ChannelCode.valueOf(params[PIPELINE_START_CHANNEL]!!)
            } else {
                ChannelCode.getRequestChannelCode()
            }
            val startType = StartType.valueOf(params[PIPELINE_START_TYPE]!!)
            return StartBuildContext(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                variables = params,
                resourceVersion = resourceVersion,
                versionName = versionName,
                actionType = actionType,
                executeCount = executeCount,
                firstTaskId = params[PIPELINE_START_TASK_ID] ?: "",
                stageRetry = isStageRetry,
                retryStartTaskId = retryStartTaskId,
                userId = params[PIPELINE_START_USER_ID]!!,
                triggerUser = params[PIPELINE_START_USER_NAME]!!,
                startType = startType,
                parentBuildId = params[PIPELINE_START_PARENT_BUILD_ID],
                parentTaskId = params[PIPELINE_START_PARENT_BUILD_TASK_ID],
                channelCode = channelCode,
                retryFailedContainer = params[PIPELINE_RETRY_ALL_FAILED_CONTAINER]?.toBoolean() ?: false,
                skipFailedTask = params[PIPELINE_SKIP_FAILED_TASK]?.toBoolean() ?: false,
                currentBuildNo = currentBuildNo,
                webhookInfo = getWebhookInfo(params, channelCode),
                buildMsg = params[PIPELINE_BUILD_MSG]?.coerceAtMaxLength(MAX_LENGTH),
                buildParameters = buildParam,
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
                pipelineParamMap = pipelineParamMap,
                debug = debug,
                debugModelStr = modelStr,
                yamlVersion = yamlVersion,
                retryOnRunningBuild = retryOnRunningBuild,
                retryTaskInStageId = params[PIPELINE_RETRY_TASK_IN_STAGE_ID],
                retryTaskInContainerId = params[PIPELINE_RETRY_TASK_IN_CONTAINER_ID],
                triggerEventType = params[PIPELINE_TRIGGER_EVENT_TYPE]?.let {
                    it.ifBlank { startType.name }
                } ?: startType.name,
                draftVersion = draftVersion,
                retryMatrixGroupId = params[PIPELINE_RETRY_MATRIX_GROUP_ID]?.takeIf { it.isNotBlank() },
                retryMatrixContainerId = params[PIPELINE_RETRY_MATRIX_CONTAINER_ID]?.takeIf { it.isNotBlank() }
            )
        }

        private fun getWebhookInfo(params: Map<String, String>, channelCode: ChannelCode): WebhookInfo? {
            // 支持webhookInfo的启动类型
            val startTypes = listOf(
                StartType.WEB_HOOK.name,
                StartType.PIPELINE.name,
                StartType.SERVICE.name,
                StartType.REMOTE.name,
                StartType.TRIGGER_EVENT.name
            )
            val startType = params[PIPELINE_START_TYPE]
            if (!startTypes.contains(startType)) {
                return null
            }
            val (webhookEventType, refId) = when {
                channelCode == ChannelCode.CREATIVE_STREAM && startType == StartType.TRIGGER_EVENT.name -> {
                    params[PIPELINE_TRIGGER_EVENT_TYPE] to params[CI_NODE_ID]
                }

                params[PIPELINE_WEBHOOK_TYPE] == CodeType.GIT.name -> {
                    params[BK_REPO_GIT_WEBHOOK_EVENT_TYPE] to params[PIPELINE_WEBHOOK_REVISION]
                }

                else -> {
                    params[PIPELINE_WEBHOOK_EVENT_TYPE] to params[PIPELINE_WEBHOOK_REVISION]
                }
            }
            return WebhookInfo(
                codeType = if (supportCustomMaterials(startType)) {
                    startType
                } else {
                    params[BK_REPO_WEBHOOK_REPO_TYPE]
                },
                nameWithNamespace = params[BK_REPO_WEBHOOK_REPO_NAME],
                webhookMessage = params[PIPELINE_WEBHOOK_COMMIT_MESSAGE],
                webhookRepoUrl = params[BK_REPO_WEBHOOK_REPO_URL],
                webhookType = params[PIPELINE_WEBHOOK_TYPE],
                webhookBranch = params[PIPELINE_WEBHOOK_BRANCH].takeIf { startType == StartType.WEB_HOOK.name },
                webhookAliasName = params[BK_REPO_WEBHOOK_REPO_ALIAS_NAME],
                // GIT事件分为MR和MR accept,但是PIPELINE_WEBHOOK_EVENT_TYPE值只有MR
                webhookEventType = webhookEventType,
                refId = refId,
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
                reviewId = params[BK_REPO_GIT_WEBHOOK_REVIEW_ID],
                parentProjectId = params[PIPELINE_START_PARENT_PROJECT_ID],
                parentPipelineId = params[PIPELINE_START_PARENT_PIPELINE_ID],
                parentPipelineName = params[PIPELINE_START_PARENT_PIPELINE_NAME],
                parentBuildId = params[PIPELINE_START_PARENT_BUILD_ID],
                parentBuildNum = params[PIPELINE_START_PARENT_BUILD_NUM],
                linkUrl = if (supportCustomMaterials(startType)) {
                    // 自定义触发材料
                    params[BK_CI_MATERIAL_URL]
                } else {
                    params[PIPELINE_GIT_EVENT_URL]
                },
                materialId = params[BK_CI_MATERIAL_ID],
                materialName = params[BK_CI_MATERIAL_NAME],
                channelCode = channelCode.name
            )
        }

        /**
         * 是否支持自定义触发材料
         */
        private fun supportCustomMaterials(startType: String?) = startType == StartType.REMOTE.name ||
            startType == StartType.SERVICE.name

        /**
         * 简易只为实现推送PipelineBuildStartEvent事件所需要的参数，不是全部
         */
        fun init4SendBuildStartEvent(
            userId: String,
            projectId: String,
            pipelineId: String,
            buildId: String,
            resourceVersion: Int,
            versionName: String?,
            actionType: ActionType,
            executeCount: Int,
            firstTaskId: String,
            startType: StartType,
            startBuildStatus: BuildStatus,
            debug: Boolean,
            channelCode: ChannelCode
        ): StartBuildContext = StartBuildContext(
            now = LocalDateTime.now(),
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            resourceVersion = resourceVersion,
            versionName = versionName,
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
            channelCode = channelCode,
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
            pipelineSetting = null,
            debug = debug,
            debugModelStr = null,
            yamlVersion = null
        )

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
                    originStartParams.addAll(PipelineParamUtils.getStartParamList(param, originStartContexts))
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
