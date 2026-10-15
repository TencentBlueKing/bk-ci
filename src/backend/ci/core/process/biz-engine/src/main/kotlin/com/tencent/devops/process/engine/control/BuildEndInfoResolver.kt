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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildEndCategory
import com.tencent.devops.common.pipeline.enums.BuildEndType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.pojo.BuildEndInfo
import com.tencent.devops.common.pipeline.pojo.EndPosition
import com.tencent.devops.common.pipeline.pojo.StagePauseCheck
import com.tencent.devops.common.pipeline.pojo.SubPipelineInfo
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.SubPipelineCallElement
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.common.pipeline.utils.BuildEndPositionCollector
import com.tencent.devops.common.quality.pojo.QualityRuleInterceptRecord
import com.tencent.devops.common.quality.pojo.enums.QualityOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_SUGGEST
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_USERID
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.SubPipelineTaskService
import com.tencent.devops.quality.api.v2.ServiceQualityInterceptResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 构建终态解析上下文，聚合解析所需的全部输入，避免方法参数过多。
 *
 * [buildTasks] 与 [buildStages] 由调用方在构建结束流程中一次性加载后传入，
 * 避免解析器重复查询构建任务/阶段表。
 */
data class BuildEndContext(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val buildStatus: BuildStatus,
    val model: Model,
    val errorInfoList: List<ErrorInfo>?,
    val buildTasks: Collection<PipelineBuildTask>,
    val buildStages: Collection<PipelineBuildStage>
)

/**
 * 构建终态详情解析器：在构建结束时把失败/超时/成功的原因与位置归纳为 [BuildEndInfo]。
 *
 * 设计要点：
 * 1. 失败位置以 errorInfoList 为准（构建结束时已有的权威聚合结果），不改动其生成逻辑；
 *    人工审核驳回因不写 errorType 而不在该列表中，单独从构建任务补齐。
 * 2. 位置的编码与路径统一走 [EndPositionUtils] 的 Model 索引，与取消场景保持一致口径。
 * 3. 质量红线的指标详情需跨模块调用 quality 服务，仅在确实存在红线失败时调用一次。
 */
@Component
class BuildEndInfoResolver @Autowired constructor(
    private val client: Client
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(BuildEndInfoResolver::class.java)

        private val QUALITY_ATOM_CODES = setOf(
            QualityGateInElement.classType,
            QualityGateOutElement.classType
        )

        /**
         * 子流水线插件同时存在内置插件与市场插件两种形态，二者失败都应归类为子流水线失败。
         * 不能按错误码判定：市场插件走的是 USER_INPUT_INVAILD 这类通用错误码，其他插件也会使用。
         */
        private val SUB_PIPELINE_ATOM_CODES = setOf(
            SubPipelineCallElement.classType,
            SubPipelineTaskService.SUB_PIPELINE_EXEC_ATOM_CODE
        )

        /** 单条终态原因的最大长度，避免异常长的插件错误信息撑爆 MODEL_VAR */
        private const val REASON_MAX_LENGTH = 512

        /** 位置列表上限，超大流水线全量失败时防止 MODEL_VAR 过大 */
        private const val POSITION_MAX_SIZE = 50
    }

    /**
     * 解析构建终态详情。返回 null 表示无需落库（取消场景由取消链路自行记录，
     * 普通成功场景在读取时合成，避免每次构建成功都额外写一次库）。
     */
    fun resolve(context: BuildEndContext): BuildEndInfo? {
        val status = context.buildStatus
        return when {
            // CANCELED 可能来自用户取消/父流水线级联，也可能来自Job超时、Agent心跳超时的
            // TERMINATE 链路（见 StartActionTaskContainerCmd 对 QUEUE_CACHE 任务的处理）。
            // 这些入口都已按取消类（与最终状态同类）落库了更精确的成因，
            // 此处按错误信息重新归类只会把取消误判为执行失败
            status.isCancel() -> null
            status.isSuccess() || status == BuildStatus.STAGE_SUCCESS -> resolveSuccess(context)
            else -> resolveAbnormal(context)
        }
    }

    /**
     * 成功态：仅在存在阶段准入/准出被驳回时才记录，普通成功不落库。
     */
    private fun resolveSuccess(context: BuildEndContext): BuildEndInfo? {
        // 绝大多数成功构建没有阶段驳回，先做一次廉价判断，避免为其遍历整个 Model 建索引
        if (context.buildStages.none { it.hasReviewAbort() }) return null
        val index = EndPositionUtils.buildPositionIndex(context.model)
        val positions = collectStageAbortPositions(context, index)
        if (positions.isEmpty()) return null
        // 阶段准入被驳回后构建即结束，只会有一个驳回位置，驳回意见可直接作为构建级原因展示；
        // 极端情况下（准入准出同时驳回）退化为国际化兜底文案，避免只展示其中一条造成歧义
        val suggest = positions.singleOrNull()?.reviewSuggest?.takeIf { it.isNotBlank() }
        return BuildEndInfo.of(
            endType = BuildEndType.SUCCESS_STAGE_ABORT,
            reason = suggest?.take(REASON_MAX_LENGTH),
            reasonCode = if (suggest == null) ProcessMessageCode.BK_BUILD_END_STAGE_REVIEW_ABORT else null
        ).withPositions(positions)
    }

    /**
     * 失败/超时态：归纳每个受影响位置的子类型，再汇总为构建级子类型。
     *
     * 一个位置都收集不到时仍要给出终态详情：前端整张详情卡片以 buildEndInfo 是否存在为开关，
     * 返回 null 会让「构建已失败但详情打不开」，如 Job 互斥组抢锁失败这种没有任何插件错误的构建。
     */
    private fun resolveAbnormal(context: BuildEndContext): BuildEndInfo {
        val index = EndPositionUtils.buildPositionIndex(context.model)
        val collected = collectAbnormalPositions(context, index)
        if (collected.isEmpty()) return BuildEndInfo.of(endType = fallbackEndType(context.buildStatus))

        val positions = fillFailPositionReasons(context, index, collected)
        val endType = aggregateEndType(positions, context.buildStatus)
        return buildAbnormalEndInfo(endType, positions).withPositions(positions)
    }

    /**
     * 无位置可归因时的构建级子类型：构建自身以超时收尾的只有排队超时，其余一律归执行失败。
     */
    private fun fallbackEndType(status: BuildStatus): BuildEndType =
        if (status.isTimeout()) BuildEndType.TIMEOUT_QUEUE else BuildEndType.FAIL_EXEC

    /**
     * 汇总构建级终态子类型：位置子类型先按构建最终状态归类（见 [alignToBuildStatus]），
     * 归类后唯一时直接采用，否则视为多类失败。
     *
     * FastKill 只作为位置级类型出现，不能当构建级标签（页面没有「因失败即停被终止」这种失败类型）。
     * 但它与其它失败并存时必须归「多类失败」：页面在单一失败类型下按构建级口径渲染位置，
     * FastKill 位置会退化成只展示错误码 2199010，设计稿里的「因失败即停被终止」整行都不会出现。
     */
    private fun aggregateEndType(positions: List<EndPosition>, status: BuildStatus): BuildEndType {
        val distinct = positions.mapNotNull { it.endType }.distinct()
        val hasFastKill = BuildEndType.FAIL_FAST_KILL in distinct
        val causes = distinct.filter { it != BuildEndType.FAIL_FAST_KILL }
            .map { alignToBuildStatus(it, status) }
            .distinct()
        return when {
            causes.isEmpty() -> BuildEndType.FAIL_EXEC
            hasFastKill -> BuildEndType.FAIL_MULTIPLE
            causes.size == 1 -> causes.first()
            causes.all { it.category == BuildEndCategory.TIMEOUT } -> causes.first()
            else -> BuildEndType.FAIL_MULTIPLE
        }
    }

    /**
     * 构建级子类型的大类必须与构建最终状态同类，否则页面会出现「状态：失败 / 类型：步骤超时」这类矛盾组合。
     *
     * Job与步骤超时都是以终止事件收尾的，构建最终状态只会是失败/终止（引擎的构建级状态集里没有
     * EXEC_TIMEOUT），因此在构建级降级为执行失败；超时这个成因不会丢——对应位置的 statusAtEnd
     * 本就是超时状态，位置级仍保留 TIMEOUT_JOB / TIMEOUT_STEP 及其原因文案。
     */
    private fun alignToBuildStatus(endType: BuildEndType, status: BuildStatus): BuildEndType =
        if (endType.category == BuildEndCategory.TIMEOUT && !status.isTimeout()) {
            BuildEndType.FAIL_EXEC
        } else {
            endType
        }

    /**
     * 组装构建级终态信息。
     *
     * 失败类不设构建级原因：同一次构建可能有多个失败位置，各自的原因（驳回意见、红线指标、
     * 子流水线名、超时时限）只能逐位置表达，构建级取其中一条会与位置列表相互矛盾
     * （多个人工审核驳回时只展示一条驳回意见即是此问题）。
     * 只有构建自身以超时状态收尾（排队超时）时，原因对整次构建唯一，才保留在构建级。
     */
    private fun buildAbnormalEndInfo(endType: BuildEndType, positions: List<EndPosition>): BuildEndInfo {
        return if (endType.category == BuildEndCategory.TIMEOUT) {
            BuildEndInfo.of(endType = endType, reason = firstErrorMsg(positions))
        } else {
            BuildEndInfo.of(endType = endType)
        }
    }

    private fun firstErrorMsg(positions: List<EndPosition>): String? =
        positions.firstNotNullOfOrNull { it.errorMsg?.takeIf { msg -> msg.isNotBlank() } }?.take(REASON_MAX_LENGTH)

    /**
     * 逐位置补齐失败原因：驳回意见、质量红线指标、子流水线名称、FastKill 连带说明、超时时限。
     * 质量红线指标需跨模块查询，仅在确实存在红线失败位置时查询一次。
     */
    private fun fillFailPositionReasons(
        context: BuildEndContext,
        index: ModelPositionIndex,
        positions: List<EndPosition>
    ): List<EndPosition> {
        val qualityRecords = if (positions.any { it.endType == BuildEndType.FAIL_QUALITY }) {
            listFailedQualityRecords(context)
        } else {
            emptyList()
        }
        val fastKillCauseJobs = if (positions.any { it.endType == BuildEndType.FAIL_FAST_KILL }) {
            resolveFastKillCauseJobs(positions, index)
        } else {
            emptyMap()
        }
        return positions.map { position ->
            when (position.endType) {
                // 无驳回意见时不给空原因，让前端只渲染「人工审核驳回」标签
                BuildEndType.FAIL_REVIEW -> position.copy(
                    reason = position.reviewSuggest?.takeIf { it.isNotBlank() }?.take(REASON_MAX_LENGTH)
                )
                BuildEndType.FAIL_QUALITY -> position.withQualityReason(qualityRecords)
                BuildEndType.FAIL_SUB_PIPELINE -> position.withSubPipelineReason()
                BuildEndType.FAIL_FAST_KILL -> position.withFastKillReason(fastKillCauseJobs[position.stageId])
                BuildEndType.TIMEOUT_STEP -> position.withStepTimeoutReason(context)
                BuildEndType.TIMEOUT_JOB -> position.withJobTimeoutReason(index)
                BuildEndType.FAIL_EXEC -> position.withExecFailReason(context, index)
                else -> position
            }
        }
    }

    private fun EndPosition.withQualityReason(records: List<QualityRuleInterceptRecord>): EndPosition {
        // 控制点插件ID对应位置的插件ID，取不到时（如Stage级红线）退化为全部未通过指标
        val matched = records.filter { it.controlPointElementId == taskId }.ifEmpty { records }
        return when (matched.size) {
            0 -> copy(reasonCode = ProcessMessageCode.BK_BUILD_END_FAIL_QUALITY)
            1 -> matched.first().let { record ->
                val threshold = record.value?.takeIf { it.isNotBlank() }
                    // 阈值缺失时「超过阈值」会成为半句话，退化为不带指标详情的兜底文案
                    ?: return copy(reasonCode = ProcessMessageCode.BK_BUILD_END_FAIL_QUALITY)
                copy(
                    reasonCode = record.operation.thresholdBreachCode(),
                    reasonParams = listOf(record.indicatorName, threshold)
                )
            }
            else -> copy(
                reasonCode = ProcessMessageCode.BK_BUILD_END_FAIL_QUALITY_INDICATORS,
                reasonParams = listOf(matched.first().indicatorName, matched.size.toString())
            )
        }
    }

    /**
     * 红线未达标的说法取决于阈值方向：阈值是上限（如代码坏味道数 <= 50）时实际值高于阈值，
     * 阈值是下限（如覆盖率 >= 80）时实际值低于阈值，两种情况的文案不能混用。
     */
    private fun QualityOperation.thresholdBreachCode(): String = when (this) {
        QualityOperation.LT, QualityOperation.LE -> ProcessMessageCode.BK_BUILD_END_FAIL_QUALITY_INDICATOR_EXCEED
        else -> ProcessMessageCode.BK_BUILD_END_FAIL_QUALITY_INDICATOR_BELOW
    }

    /**
     * 子流水线失败位置展示子流水线名称，供用户识别是哪条子流水线出的问题；
     * 构建号不进文案，由前端从 [SubPipelineInfo] 取用于跳转入口，避免与设计稿的展示形态不一致。
     * 存量构建未记录名称时退化为国际化兜底文案。
     */
    private fun EndPosition.withSubPipelineReason(): EndPosition {
        val subPipelineName = subPipelineInfo?.pipelineName?.takeIf { it.isNotBlank() }
            ?: return copy(reasonCode = ProcessMessageCode.BK_BUILD_END_FAIL_SUB_PIPELINE)
        return copy(reason = subPipelineName.take(REASON_MAX_LENGTH))
    }

    /**
     * 步骤超时：时限取自超时插件自身的 additionalOptions.timeout（分钟）。
     * 取不到或配置为0（表示不限制）时退化为插件错误信息，避免展示出空的时限。
     */
    private fun EndPosition.withStepTimeoutReason(context: BuildEndContext): EndPosition {
        val minutes = taskId
            ?.let { id -> context.buildTasks.firstOrNull { it.taskId == id } }
            ?.additionalOptions?.timeout
            ?.takeIf { it > 0 }
            ?: return copy(reason = errorMsg?.takeIf { it.isNotBlank() }?.take(REASON_MAX_LENGTH))
        return copy(
            reasonCode = ProcessMessageCode.BK_BUILD_END_TIMEOUT_STEP,
            reasonParams = listOf(minutes.toString())
        )
    }

    /**
     * 执行失败位置若还没有原因：优先识别执行前暂停被终止，其次复用插件错误信息，
     * 再看所属 Job 是否超时，最后给兜底文案，避免卡片上只剩插件名。
     */
    private fun EndPosition.withExecFailReason(
        context: BuildEndContext,
        index: ModelPositionIndex
    ): EndPosition {
        if (!reasonCode.isNullOrBlank() || !reason.isNullOrBlank()) return this
        val element = findModelElement(index)
        if (element?.additionalOptions?.pauseBeforeExec == true) {
            return copy(reasonCode = ProcessMessageCode.BK_BUILD_END_FAIL_PAUSE_TERMINATED)
        }
        val container = index.locateContainer(containerId)?.container
        if (container != null && BuildStatus.parse(container.status).isTimeout()) {
            return withJobTimeoutReason(index)
        }
        val message = errorMsg?.takeIf { it.isNotBlank() }
            ?: element?.errorMsg?.takeIf { it.isNotBlank() }
            ?: context.buildTasks.firstOrNull { it.taskId == taskId }?.errorMsg?.takeIf { it.isNotBlank() }
        if (!message.isNullOrBlank()) {
            return copy(reason = BuildEndPositionCollector.toDisplayReason(message))
        }
        return copy(reasonCode = ProcessMessageCode.BK_BUILD_END_FAIL_PLUGIN)
    }

    private fun EndPosition.findModelElement(index: ModelPositionIndex): Element? {
        val id = taskId?.takeIf { it.isNotBlank() } ?: return null
        return index.locateContainer(containerId)?.container?.elements?.firstOrNull { it.id == id }
    }

    /**
     * Job超时：时限取自所属容器的 JobControlOption（分钟）。
     * 与构建级取消原因复用同一词条——两处描述的都是「所属 Job 超时导致步骤被终止」这同一件事。
     */
    private fun EndPosition.withJobTimeoutReason(index: ModelPositionIndex): EndPosition {
        val minutes = index.locateContainer(containerId)?.container
            ?.let { container ->
                when (container) {
                    is VMBuildContainer -> container.jobControlOption?.timeout
                    is NormalContainer -> container.jobControlOption?.timeout
                    else -> null
                }
            }
            ?.takeIf { it > 0 }
            ?: return copy(reason = errorMsg?.takeIf { it.isNotBlank() }?.take(REASON_MAX_LENGTH))
        return copy(
            reasonCode = ProcessMessageCode.BK_BUILD_CANCEL_SYSTEM_JOB_EXEC_TIMEOUT,
            reasonParams = listOf(minutes.toString())
        )
    }

    /**
     * FastKill 连带终止的位置一律以「因失败即停被终止」示人，不展示插件自身的错误信息——
     * 被强制终止时插件给不出真实原因，那里只有「Force Terminate!」这类引擎占位文案。
     *
     * 文案对齐设计稿：已启用 Fastkill，因「Job 名称」失败被终止。
     * 定位不到引发终止的 Job（如 Stage 级质量红线，错误信息里没有容器）时退化为不带 Job 名的文案，
     * 仍要给出原因，否则页面按「类型：原因」整行渲染时会连类型标签一起丢掉。
     */
    private fun EndPosition.withFastKillReason(causeJobName: String?): EndPosition {
        if (causeJobName.isNullOrBlank()) {
            return copy(reason = null, reasonCode = ProcessMessageCode.BK_BUILD_END_FAIL_FAST_KILL_STAGE)
        }
        return copy(
            reason = null,
            reasonCode = ProcessMessageCode.BK_BUILD_END_FAIL_FAST_KILL,
            reasonParams = listOf(causeJobName)
        )
    }

    /**
     * 按阶段找出引发 FastKill 的 Job 名称：FastKill 只终止同一阶段内的其他 Job。
     *
     * 先看已收集的非 FastKill 位置（含人工审核驳回、互斥组这类不写错误信息的成因），
     * 再扫一遍同阶段里失败但未被标成 FastKill 的容器，避免成因 Job 没进位置列表时原因里缺 Job 名。
     */
    private fun resolveFastKillCauseJobs(
        positions: List<EndPosition>,
        index: ModelPositionIndex
    ): Map<String, String> {
        val causeJobs = mutableMapOf<String, String>()
        positions.forEach { position ->
            if (position.endType == BuildEndType.FAIL_FAST_KILL) return@forEach
            if (causeJobs.containsKey(position.stageId)) return@forEach
            index.locateContainer(position.containerId)?.container?.name
                ?.takeIf { it.isNotBlank() }
                ?.let { causeJobs[position.stageId] = it }
        }
        val fastKillStageIds = positions
            .filter { it.endType == BuildEndType.FAIL_FAST_KILL }
            .mapTo(mutableSetOf()) { it.stageId }
        val fastKillContainerIds = positions
            .filter { it.endType == BuildEndType.FAIL_FAST_KILL && it.containerId.isNotBlank() }
            .mapTo(mutableSetOf()) { it.containerId }
        index.containers().forEach { location ->
            val stageId = location.stagePosition.stageId
            if (stageId !in fastKillStageIds || causeJobs.containsKey(stageId)) return@forEach
            if (location.containerId in fastKillContainerIds) return@forEach
            if (location.container.matrixGroupFlag == true) return@forEach
            if (!BuildStatus.parse(location.container.status).isFailure()) return@forEach
            location.container.name.takeIf { it.isNotBlank() }?.let { causeJobs[stageId] = it }
        }
        return causeJobs
    }

    /**
     * 收集失败/超时位置：以 errorInfoList 为主，再补齐不在其中的人工审核驳回位置。
     */
    private fun collectAbnormalPositions(
        context: BuildEndContext,
        index: ModelPositionIndex
    ): List<EndPosition> {
        val positions = mutableListOf<EndPosition>()
        val coveredTaskIds = mutableSetOf<String>()
        val taskMap = context.buildTasks.associateBy { it.taskId }

        context.errorInfoList?.forEach { errorInfo ->
            buildPositionFromError(errorInfo, index, taskMap)?.let { position ->
                positions.add(position)
                position.taskId?.takeIf { it.isNotBlank() }?.let { coveredTaskIds.add(it) }
            }
        }
        // 人工审核驳回的插件不会写 errorType，因此不在 errorInfoList 中，需单独补齐
        collectReviewAbortPositions(context, index, coveredTaskIds).forEach { position ->
            positions.add(position)
            position.taskId?.takeIf { it.isNotBlank() }?.let { coveredTaskIds.add(it) }
        }
        // 暂停插件被取消后常被置为 FAILED 且不写 errorType，按模型终态补齐，避免只留下取消时的 PAUSE。
        // FastKill 阶段仍走后面的 Job 级补齐，避免把连带终止的插件误标成独立执行失败。
        val fastKillStageIds = context.buildStages
            .filter { it.controlOption?.fastKill == true && it.status.isFailure() }
            .mapTo(mutableSetOf()) { it.stageId }
        BuildEndPositionCollector.collectFailPositions(context.model).forEach { position ->
            val taskId = position.taskId
            if (taskId.isNullOrBlank() || taskId in coveredTaskIds) return@forEach
            if (position.stageId in fastKillStageIds) return@forEach
            positions.add(position)
            coveredTaskIds.add(taskId)
        }
        // 再补齐整个 Job 都没有任务错误信息的位置
        val coveredContainerIds = positions.filter { it.containerId.isNotBlank() }.mapTo(mutableSetOf()) {
            it.containerId
        }
        val stagesWithCause = positions
            .filter { it.endType != BuildEndType.FAIL_FAST_KILL && it.stageId.isNotBlank() }
            .mapTo(mutableSetOf()) { it.stageId }
        positions.addAll(collectJobLevelPositions(context, index, coveredContainerIds, stagesWithCause))

        return positions.take(POSITION_MAX_SIZE)
    }

    /**
     * 补齐没有任何任务错误信息的 Job 级位置。
     *
     * 位置以 errorInfoList 为主，但有两类 Job 级终止从不写任务错误信息：
     * - 互斥组抢锁失败（未开启排队 / 排队超时 / 队列已满）：容器被直接置为失败，其下任务从未启动
     * - FastKill 波及到尚未写出错误码的 Job：运行中的被强制置为失败，停在领取队列中的则是取消态
     *   （见 [com.tencent.devops.process.engine.control.command.container.impl.StartActionTaskContainerCmd]）
     * 不补齐则页面上这些 Job 完全不出现在失败位置里，互斥组场景连终态详情都取不到。
     *
     * 取消态或失败态容器只在 FastKill 真正生效过的阶段才算 FastKill 位置：引擎对运行中 Job 强制结束时
     * 会把容器置为 FAILED（见 BuildStatusSwitcher.forceFinish(fastKill=true)），不是取消态。
     * 若把这些 Job 当成执行失败，原因会落成「Job 异常结束」，与设计稿的 FastKill 文案不符。
     *
     * 同阶段里已经有非 FastKill 成因时，其余未覆盖的失败 Job 才归 FastKill；
     * 互斥组抢锁失败也是 FAILED 且无任务错误，不能被 FastKill 抢走。
     */
    private fun collectJobLevelPositions(
        context: BuildEndContext,
        index: ModelPositionIndex,
        coveredContainerIds: Set<String>,
        stagesWithCause: MutableSet<String>
    ): List<EndPosition> {
        val fastKillStageIds = context.buildStages
            .filter { it.controlOption?.fastKill == true && it.status.isFailure() }
            .mapTo(mutableSetOf()) { it.stageId }
        val startedContainerIds = context.buildTasks
            .filter { it.startTime != null }
            .mapTo(mutableSetOf()) { it.containerId }
        return index.containers().mapNotNull { location ->
            val container = location.container
            if (location.containerId in coveredContainerIds) return@mapNotNull null
            // 矩阵组容器本身不执行插件，位置一律落到其下的子容器上，否则同一次失败会重复出现
            if (container.matrixGroupFlag == true) return@mapNotNull null
            val status = BuildStatus.parse(container.status)
            val started = location.containerId in startedContainerIds
            val abortReason = if ((status.isFailure() || status.isCancel()) && !status.isTimeout()) {
                container.resolveJobAbortReason(started)
            } else {
                null
            }
            val endType = resolveJobLevelEndType(
                status = status,
                stageId = location.stagePosition.stageId,
                fastKillStageIds = fastKillStageIds,
                stagesWithCause = stagesWithCause,
                abortReason = abortReason
            ) ?: return@mapNotNull null
            EndPosition(
                position = location.position,
                componentPath = location.componentPath,
                statusAtEnd = status.name,
                endType = endType,
                reasonCode = abortReason?.first.takeIf { endType == BuildEndType.FAIL_EXEC },
                reasonParams = abortReason?.second.takeIf { endType == BuildEndType.FAIL_EXEC },
                stageId = location.stagePosition.stageId,
                containerId = location.containerId,
                matrixFlag = location.matrixFlag.takeIf { it },
                containerHashId = container.containerHashId
            )
        }
    }

    /**
     * Job 级位置的子类型：超时优先；互斥组保持执行失败；FastKill 阶段里其余失败/取消归连带终止。
     */
    private fun resolveJobLevelEndType(
        status: BuildStatus,
        stageId: String,
        fastKillStageIds: Set<String>,
        stagesWithCause: MutableSet<String>,
        abortReason: Pair<String, List<String>?>?
    ): BuildEndType? {
        val inFastKillStage = stageId in fastKillStageIds
        val mutexAbort = abortReason != null &&
            abortReason.first != ProcessMessageCode.BK_BUILD_END_FAIL_JOB_ABORTED
        return when {
            status.isTimeout() -> BuildEndType.TIMEOUT_JOB
            status.isFailure() && !mutexAbort && inFastKillStage && stageId in stagesWithCause ->
                BuildEndType.FAIL_FAST_KILL
            status.isFailure() -> {
                if (!mutexAbort) stagesWithCause.add(stageId)
                BuildEndType.FAIL_EXEC
            }
            status.isCancel() && mutexAbort -> BuildEndType.FAIL_EXEC
            status.isCancel() && inFastKillStage -> BuildEndType.FAIL_FAST_KILL
            else -> null
        }
    }

    /**
     * 一个插件都没启动过就失败的 Job，唯一可枚举的成因是互斥组抢锁失败：未开启排队时被其他构建占用即刻终止，
     * 开启排队时则是排队超时或队列已满，具体是哪一种要看排队配置——三种情况引擎都只留下一条容器失败记录，
     * 无从区分超时与队列已满。
     *
     * [started] 为真说明插件已经跑过，失败另有成因（如构建结束时容器仍在运行而被强制置为失败），
     * 此时不能按互斥组下结论，只给不带成因的兜底文案。
     */
    private fun Container.resolveJobAbortReason(started: Boolean): Pair<String, List<String>?> {
        val fallback = ProcessMessageCode.BK_BUILD_END_FAIL_JOB_ABORTED to null
        if (started) return fallback
        val mutexGroup = when (this) {
            is VMBuildContainer -> mutexGroup
            is NormalContainer -> mutexGroup
            else -> null
        }?.takeIf { it.enable }
        val groupName = mutexGroup?.fetchRuntimeMutexGroup()?.takeIf { it.isNotBlank() } ?: return fallback
        return if (mutexGroup.queueEnable) {
            ProcessMessageCode.BK_BUILD_END_FAIL_MUTEX_QUEUE to listOf(groupName)
        } else {
            ProcessMessageCode.BK_BUILD_END_FAIL_MUTEX_QUEUE_DISABLED to listOf(groupName)
        }
    }

    /**
     * 将一条错误信息映射为位置详情。taskId 为空表示 Job 级或 Stage 级问题。
     * 日志跳转参数优先取构建任务上的值，Model 中的编排数据仅作兜底。
     */
    private fun buildPositionFromError(
        errorInfo: ErrorInfo,
        index: ModelPositionIndex,
        taskMap: Map<String, PipelineBuildTask>
    ): EndPosition? {
        val endType = classifyError(errorInfo)
        val containerLocation = index.locateContainer(errorInfo.containerId)
        // 容器定位不到时退化为 Stage 级位置（如 Stage 级质量红线失败，containerId 为空）
            ?: return buildStageLevelPosition(errorInfo, index, endType)

        val taskId = errorInfo.taskId.takeIf { it.isNotBlank() }
        val buildTask = taskId?.let { taskMap[it] }
        // taskId 存在但在 Model 中找不到对应插件时 element 为空，此时退化为 Job 级位置，保证仍可定位
        val element = taskId?.let { id -> containerLocation.container.elements.firstOrNull { it.id == id } }
        val taskSeq = element?.let { containerLocation.taskSeqOf(taskId) }

        return EndPosition(
            position = taskSeq?.let { "${containerLocation.position}-$it" } ?: containerLocation.position,
            componentPath = element?.let { "${containerLocation.componentPath}/${it.name}" }
                ?: containerLocation.componentPath,
            statusAtEnd = resolveStatusAtEnd(element, buildTask, errorInfo),
            endType = endType,
            stageId = containerLocation.stagePosition.stageId,
            containerId = containerLocation.containerId,
            taskId = taskId.takeIf { element != null },
            matrixFlag = (errorInfo.matrixFlag == true || containerLocation.matrixFlag).takeIf { it },
            errorType = errorInfo.errorType,
            errorCode = errorInfo.errorCode,
            errorMsg = errorInfo.errorMsg.takeIf { it.isNotBlank() },
            containerHashId = buildTask?.containerHashId ?: containerLocation.container.containerHashId,
            stepId = buildTask?.stepId ?: element?.stepId,
            subPipelineInfo = element?.let { resolveSubPipelineInfo(it) }
        )
    }

    /**
     * Stage 级位置：用于 Stage 准入/准出质量红线失败等没有具体 Job 的场景。
     */
    private fun buildStageLevelPosition(
        errorInfo: ErrorInfo,
        index: ModelPositionIndex,
        endType: BuildEndType
    ): EndPosition? {
        val stagePosition = index.locateStage(errorInfo.stageId) ?: return null
        val statusAtEnd = if (endType == BuildEndType.FAIL_QUALITY) {
            BuildStatus.QUALITY_CHECK_FAIL
        } else {
            BuildStatus.FAILED
        }
        return EndPosition(
            position = stagePosition.position,
            componentPath = stagePosition.stageName,
            statusAtEnd = statusAtEnd.name,
            endType = endType,
            stageId = stagePosition.stageId,
            containerId = "",
            errorType = errorInfo.errorType,
            errorCode = errorInfo.errorCode,
            errorMsg = errorInfo.errorMsg.takeIf { it.isNotBlank() }
        )
    }

    /**
     * 补齐人工审核驳回位置：这类插件以 REVIEW_ABORT 结束且不写 errorType，
     * 不会进入 errorInfoList，因此从构建任务中识别，并取出驳回人与驳回意见。
     */
    private fun collectReviewAbortPositions(
        context: BuildEndContext,
        index: ModelPositionIndex,
        coveredTaskIds: Set<String>
    ): List<EndPosition> {
        return context.buildTasks.asSequence()
            .filter { it.status == BuildStatus.REVIEW_ABORT && it.taskId !in coveredTaskIds }
            .mapNotNull { task -> buildReviewAbortPosition(task, index) }
            .toList()
    }

    private fun buildReviewAbortPosition(task: PipelineBuildTask, index: ModelPositionIndex): EndPosition? {
        val containerLocation = index.locateContainer(task.containerId) ?: return null
        val taskSeq = containerLocation.taskSeqOf(task.taskId)
        val element = containerLocation.container.elements.firstOrNull { it.id == task.taskId }
        val suggest = task.taskParams[BS_MANUAL_ACTION_SUGGEST] as? String
            ?: (element as? ManualReviewUserTaskElement)?.suggest
        return EndPosition(
            position = taskSeq?.let { "${containerLocation.position}-$it" } ?: containerLocation.position,
            componentPath = element?.let { "${containerLocation.componentPath}/${it.name}" }
                ?: "${containerLocation.componentPath}/${task.taskName}",
            statusAtEnd = BuildStatus.REVIEW_ABORT.name,
            endType = BuildEndType.FAIL_REVIEW,
            stageId = containerLocation.stagePosition.stageId,
            containerId = containerLocation.containerId,
            taskId = task.taskId,
            matrixFlag = containerLocation.matrixFlag.takeIf { it },
            operator = task.taskParams[BS_MANUAL_ACTION_USERID] as? String,
            reviewSuggest = suggest?.takeIf { it.isNotBlank() },
            containerHashId = containerLocation.container.containerHashId,
            stepId = element?.stepId
        )
    }

    /**
     * 收集阶段准入/准出被驳回的位置。
     */
    private fun collectStageAbortPositions(
        context: BuildEndContext,
        index: ModelPositionIndex
    ): List<EndPosition> {
        val positions = mutableListOf<EndPosition>()
        context.buildStages.forEach { stage ->
            val stagePosition = index.locateStage(stage.stageId) ?: return@forEach
            stage.abortedChecks().forEach { check ->
                val abortIndex = check.reviewGroups?.indexOfLast { it.status == ManualReviewAction.ABORT.name }
                val abortGroup = abortIndex?.takeIf { it >= 0 }?.let { check.reviewGroups?.get(it) }
                positions.add(
                    EndPosition(
                        position = stagePosition.position,
                        componentPath = stagePosition.stageName,
                        statusAtEnd = BuildStatus.REVIEW_ABORT.name,
                        endType = BuildEndType.SUCCESS_STAGE_ABORT,
                        stageId = stagePosition.stageId,
                        containerId = "",
                        operator = abortGroup?.operator,
                        reviewSuggest = abortGroup?.suggest?.takeIf { it.isNotBlank() },
                        reviewGroupSeq = abortIndex?.takeIf { it >= 0 }?.plus(1),
                        reviewGroupName = abortGroup?.name
                    )
                )
            }
        }
        return positions
    }

    /** 阶段准入/准出中被驳回的审核配置，准入与准出可能同时存在 */
    private fun PipelineBuildStage.abortedChecks(): List<StagePauseCheck> =
        listOfNotNull(checkIn, checkOut).filter { it.status == BuildStatus.REVIEW_ABORT.name }

    private fun PipelineBuildStage.hasReviewAbort(): Boolean = abortedChecks().isNotEmpty()

    /**
     * 判定单条错误信息属于哪种终态子类型。判定顺序由具体到通用，先命中先返回。
     */
    private fun classifyError(errorInfo: ErrorInfo): BuildEndType {
        return when {
            errorInfo.errorCode == ErrorCode.USER_TASK_OUTTIME_LIMIT -> BuildEndType.TIMEOUT_STEP
            errorInfo.errorCode == ErrorCode.USER_JOB_OUTTIME_LIMIT -> BuildEndType.TIMEOUT_JOB
            errorInfo.errorCode == ErrorCode.USER_STAGE_FASTKILL_TERMINATE -> BuildEndType.FAIL_FAST_KILL
            errorInfo.errorCode == ErrorCode.USER_QUALITY_CHECK_FAIL ||
                errorInfo.atomCode in QUALITY_ATOM_CODES -> BuildEndType.FAIL_QUALITY
            errorInfo.atomCode == ManualReviewUserTaskElement.classType -> BuildEndType.FAIL_REVIEW
            errorInfo.atomCode in SUB_PIPELINE_ATOM_CODES -> BuildEndType.FAIL_SUB_PIPELINE
            else -> BuildEndType.FAIL_EXEC
        }
    }

    /**
     * 终态时的组件状态：优先取构建任务的真实终态，其次取 Model 中的插件状态，
     * 两者都不可用时按错误码推断，避免出现空值。
     */
    private fun resolveStatusAtEnd(element: Element?, buildTask: PipelineBuildTask?, errorInfo: ErrorInfo): String {
        buildTask?.status?.takeIf { it.isFinish() }?.let { return it.name }
        element?.status?.let { BuildStatus.parse(it) }?.takeIf { it.isFinish() }?.let { return it.name }
        return if (errorInfo.errorCode == ErrorCode.USER_TASK_OUTTIME_LIMIT) {
            BuildStatus.EXEC_TIMEOUT.name
        } else {
            BuildStatus.FAILED.name
        }
    }

    private fun resolveSubPipelineInfo(element: Element): SubPipelineInfo? {
        val subInfo = element.subPipelineBuildInfo ?: return null
        return SubPipelineInfo(
            projectId = subInfo.projectId,
            pipelineId = subInfo.pipelineId,
            pipelineName = subInfo.pipelineName,
            buildId = subInfo.buildId,
            buildNum = subInfo.buildNum
        )
    }

    /**
     * 查询未通过的质量红线指标。指标详情存放在 quality 模块，需跨服务查询，
     * 因此仅在确认存在红线失败位置时调用一次，结果随 BuildEndInfo 一并落库，读取详情时不再跨模块。
     */
    private fun listFailedQualityRecords(context: BuildEndContext): List<QualityRuleInterceptRecord> {
        return try {
            client.get(ServiceQualityInterceptResource::class).listHistory(
                projectId = context.projectId,
                pipelineId = context.pipelineId,
                buildId = context.buildId
            ).data.orEmpty()
                .flatMap { it.resultMsg }
                .filter { !it.pass }
        } catch (ignored: Exception) {
            // 质量服务不可用不应影响构建结束流程，退化为无指标详情
            LOG.warn("ENGINE|${context.buildId}|BUILD_END_INFO|fetch quality intercept failed", ignored)
            emptyList()
        }
    }
}
