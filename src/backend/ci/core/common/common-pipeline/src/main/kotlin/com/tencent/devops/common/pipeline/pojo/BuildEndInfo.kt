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

package com.tencent.devops.common.pipeline.pojo

import com.tencent.devops.common.pipeline.enums.BuildEndCategory
import com.tencent.devops.common.pipeline.enums.BuildEndType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.utils.BuildEndPositionCollector
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 构建终态详情——统一描述取消/失败/超时/成功等所有结束场景。
 *
 * 所有终态共用同一套 endType / reason / positions 字段，
 * 新增终态子类型只需扩展 [BuildEndType] 并补充对应工厂方法，读取侧与前端无需改动。
 *
 * **[reason] 与 [reasonCode] 的取值规则**：读取时若 [reasonCode] 有值，会用其国际化文案覆盖 [reason]，
 * [reason] 仅作为词条缺失时的兜底默认值。因此写入方二者只应择一：
 * 文案可枚举（取消、超时等）时填 [reasonCode]；
 * 文案是运行时产生的具体内容（插件错误信息、审核驳回意见）时只填 [reason]。
 */
@Schema(title = "构建终态详情")
data class BuildEndInfo(
    @get:Schema(title = "终态子类型", required = true)
    val endType: BuildEndType,
    @get:Schema(
        title = "终态大类(结束成因归类，恒等于endType所属大类，且与ModelRecord.status同类)",
        required = false
    )
    var endCategory: BuildEndCategory? = null,
    @get:Schema(title = "操作人(仅用户主动操作时有值)", required = false)
    val operator: String? = null,
    @get:Schema(title = "终态原因(兜底文案)", required = false)
    var reason: String? = null,
    @get:Schema(title = "终态子类型描述(国际化)", required = false)
    var endTypeDesc: String? = null,
    @get:Schema(title = "终态原因国际化标识", required = false)
    var reasonCode: String? = null,
    @get:Schema(title = "终态原因国际化占位符参数", required = false)
    var reasonParams: List<String>? = null,
    @get:Schema(title = "终态时间戳(毫秒)", required = false)
    val endTime: Long? = null,
    @get:Schema(title = "构建运行总时长(毫秒，读取时按构建开始/结束时间计算)", required = false)
    var totalCostTime: Long? = null,
    @get:Schema(
        title = "已等待时长(毫秒，仅SUCCESS_STAGE_REVIEWING等待审核中场景有值，读取时按审核开始时间计算)",
        required = false
    )
    var waitCostTime: Long? = null,
    @get:Schema(title = "被影响的组件位置列表", required = false)
    var positions: List<EndPosition>? = null,
    @get:Schema(title = "被影响位置总数", required = false)
    var positionCount: Int = 0,
    @get:Schema(title = "父流水线信息(父流水线级联终止时)", required = false)
    val parentPipelineInfo: ParentPipelineInfo? = null
) {
    /**
     * 统一设置位置列表，保证 positions 与 positionCount 不会出现不一致。
     * 空列表时 positions 置空，避免前端渲染出空的位置区块。
     */
    fun withPositions(endPositions: List<EndPosition>): BuildEndInfo {
        positions = endPositions.takeIf { it.isNotEmpty() }
        positionCount = endPositions.size
        return this
    }

    /**
     * 补充/覆盖终态原因国际化标识，用于位置信息收集完成后才能确定占位符参数的场景，
     * 如用户取消需要在文案中带出被终止的在途位置数。
     */
    fun withReason(reasonCode: String, reasonParams: List<String>? = null): BuildEndInfo {
        this.reasonCode = reasonCode
        this.reasonParams = reasonParams
        return this
    }

    /**
     * 构建级大类是否与最终状态同类。运行中尚未形成终态时视为相容，不阻断提前落库的取消信息。
     */
    fun matchesBuildStatus(status: BuildStatus): Boolean {
        val expected = BuildEndCategory.of(status) ?: return true
        return endType.category == expected
    }

    /**
     * 构建结束用失败卡片覆盖时，保留心跳/Job 超时等系统取消已经写好的成因。
     * 不改 endType，避免失败构建再被做成取消卡片。
     */
    fun preserveSystemCause(existing: BuildEndInfo?): BuildEndInfo {
        if (existing == null) return this
        val keepCause = existing.endType == BuildEndType.CANCEL_SYSTEM ||
            existing.endType == BuildEndType.CANCEL_PARENT_PIPELINE
        if (!keepCause) return this
        if (!reasonCode.isNullOrBlank() || !reason.isNullOrBlank()) return this
        return copy(
            reason = existing.reason,
            reasonCode = existing.reasonCode,
            reasonParams = existing.reasonParams,
            parentPipelineInfo = parentPipelineInfo ?: existing.parentPipelineInfo
        )
    }

    /**
     * 读取侧兜底：
     * 1. 大类与最终状态同类时，刷新位置状态、补齐原因；取消类还会把模型里
     *    其它已取消/暂停插件并进来（Job 超时 / 用户取消落库往往只拍到当时一个位置）。
     *    失败类只补审核驳回和执行前暂停被终止，不把 FastKill 连带失败再塞进来。
     * 2. 大类错配时（取消链路先写了 CANCEL_USER，暂停插件随后被收成失败），
     *    改写成与状态同类的详情，避免「状态：失败 / 卡片：用户取消」。
     *
     * 失败类优先采用模型里插件的当前终态（FAILED / REVIEW_ABORT 等）。
     * 成功/取消无法从错误的落库安全还原，返回 null 交给读取侧重新合成。
     */
    fun alignedTo(
        status: BuildStatus,
        modelFailPositions: List<EndPosition> = emptyList(),
        modelCancelPositions: List<EndPosition> = emptyList(),
        latestStatusAtEnd: (EndPosition) -> String? = { null }
    ): BuildEndInfo? {
        if (matchesBuildStatus(status)) {
            return enrichMatched(status, modelFailPositions, modelCancelPositions, latestStatusAtEnd)
        }
        return when (BuildEndCategory.of(status)) {
            BuildEndCategory.FAIL -> {
                // 用户取消文案不能出现在失败卡片上；系统取消/父流水线级联的成因（心跳失联、Job超时）仍可保留
                val keepCause = endType == BuildEndType.CANCEL_SYSTEM ||
                    endType == BuildEndType.CANCEL_PARENT_PIPELINE
                val failPositions = modelFailPositions.ifEmpty {
                    positions.orEmpty().map { it.refreshStatusAtEnd(latestStatusAtEnd) }
                }
                BuildEndInfo(
                    endType = BuildEndPositionCollector.aggregateFailEndType(failPositions),
                    reason = reason.takeIf { keepCause },
                    reasonCode = reasonCode.takeIf { keepCause },
                    reasonParams = reasonParams.takeIf { keepCause },
                    endTime = endTime,
                    parentPipelineInfo = parentPipelineInfo.takeIf { keepCause }
                ).withPositions(failPositions)
            }
            BuildEndCategory.TIMEOUT -> BuildEndInfo(
                endType = BuildEndType.TIMEOUT_QUEUE,
                endTime = endTime
            )
            else -> null
        }
    }

    /**
     * 大类已经对上时只做位置级修正：刷新 statusAtEnd、补齐模型里能推断的原因。
     * 失败卡片只追加暂停终止/审核驳回；取消卡片按模型补齐所有仍停在取消/暂停/终止的用户插件，
     * 避免 Job 超时 IfAbsent 只记下第一个插件、用户取消只拍到当时在途的那一批。
     */
    private fun enrichMatched(
        status: BuildStatus,
        modelFailPositions: List<EndPosition>,
        modelCancelPositions: List<EndPosition>,
        latestStatusAtEnd: (EndPosition) -> String?
    ): BuildEndInfo {
        val current = positions.orEmpty()
        val reasonSource = modelFailPositions + modelCancelPositions
        if (BuildEndCategory.of(status) == BuildEndCategory.CANCEL && modelCancelPositions.isNotEmpty()) {
            // 编排图以模型里的插件状态为准。Job 超时只拍到当时一个容器，
            // 其它仍停在暂停的插件必须进来，并且不能再沿用快照里的「取消」。
            return mergeCancelSnapshot(modelCancelPositions)
        }
        val refreshed = current.map { pos ->
            pos.refreshStatusAtEnd(latestStatusAtEnd).fillMissingReason(reasonSource)
        }
        val existingIds = refreshed.mapNotNull { it.identity() }.toSet()
        val extras = when (BuildEndCategory.of(status)) {
            BuildEndCategory.FAIL -> modelFailPositions.filter { pos ->
                val id = pos.identity() ?: return@filter false
                id !in existingIds && pos.shouldAppendWhenMatched()
            }
            else -> emptyList()
        }
        val merged = (refreshed + extras).take(BuildEndPositionCollector.POSITION_MAX_SIZE)
        if (merged == current) return this
        val nextType = if (extras.isEmpty() || endType.category != BuildEndCategory.FAIL) {
            endType
        } else {
            BuildEndPositionCollector.aggregateFailEndType(merged)
        }
        return copy(endType = nextType).withPositions(merged)
    }

    private fun EndPosition.refreshStatusAtEnd(latestStatusAtEnd: (EndPosition) -> String?): EndPosition {
        val latest = latestStatusAtEnd(this)
        return if (latest.isNullOrBlank() || latest == statusAtEnd) this
        else copy(statusAtEnd = latest)
    }

    private fun EndPosition.fillMissingReason(modelFailPositions: List<EndPosition>): EndPosition {
        if (!reasonCode.isNullOrBlank() || !reason.isNullOrBlank()) return this
        val fromModel = modelFailPositions.firstOrNull { it.identity() != null && it.identity() == identity() }
            ?: return this
        if (fromModel.reasonCode.isNullOrBlank() && fromModel.reason.isNullOrBlank()) return this
        return copy(
            endType = endType ?: fromModel.endType,
            reason = fromModel.reason,
            reasonCode = fromModel.reasonCode,
            reasonParams = fromModel.reasonParams
        )
    }

    private fun EndPosition.identity(): String? {
        return taskId?.takeIf { it.isNotBlank() }
            ?: containerId.takeIf { it.isNotBlank() && taskId.isNullOrBlank() }?.let { "job:$it" }
    }

    /**
     * 取消卡片的位置以模型当前插件为准，覆盖超时瞬间拍下的单容器快照。
     * 模型里没有的 Job 级位置（排队、依赖等待）仍保留。
     */
    fun mergeCancelSnapshot(modelPositions: List<EndPosition>): BuildEndInfo {
        if (endType.category != BuildEndCategory.CANCEL || modelPositions.isEmpty()) return this
        val modelTaskKeys = modelPositions.mapNotNull { it.identity() }.toSet()
        val modelContainers = modelPositions.map { it.containerId }.filter { it.isNotBlank() }.toSet()
        val keptJobs = positions.orEmpty().filter { stored ->
            stored.taskId.isNullOrBlank() &&
                stored.containerId.isNotBlank() &&
                stored.containerId !in modelContainers &&
                stored.identity() !in modelTaskKeys
        }
        val merged = (modelPositions + keptJobs)
            .dropJobWhenTaskPresent()
            .take(BuildEndPositionCollector.POSITION_MAX_SIZE)
        if (merged == positions.orEmpty()) return this
        return withPositions(merged)
    }

    /**
     * 取消落库常先记 Job 级位置（排队/准备环境），读取再补插件后，
     * 同一容器会出现「Job + 插件」两行，卡片个数会大于编排图。
     */
    private fun List<EndPosition>.dropJobWhenTaskPresent(): List<EndPosition> {
        val containersWithTask = mapNotNull { pos ->
            pos.taskId?.takeIf { it.isNotBlank() }?.let {
                pos.containerId.takeIf { id -> id.isNotBlank() }
            }
        }.toSet()
        if (containersWithTask.isEmpty()) return this
        return filterNot { pos ->
            pos.taskId.isNullOrBlank() && pos.containerId in containersWithTask
        }
    }

    /**
     * 已有失败卡片只补「模型能确定、落库时漏掉」的位置：
     * 执行前暂停被终止、人工审核驳回。不把 FastKill 连带失败插件再塞进来。
     */
    private fun EndPosition.shouldAppendWhenMatched(): Boolean {
        return endType == BuildEndType.FAIL_REVIEW ||
            reasonCode == BuildEndPositionCollector.REASON_PAUSE_TERMINATED
    }

    companion object {
        const val MODEL_VAR_KEY = "buildEndInfo"

        /**
         * 用户主动取消
         */
        fun ofCancelUser(
            operator: String,
            reasonCode: String,
            reasonParams: List<String>? = null,
            reasonDefault: String? = null
        ): BuildEndInfo {
            return BuildEndInfo(
                endType = BuildEndType.CANCEL_USER,
                operator = operator,
                reason = reasonDefault,
                reasonCode = reasonCode,
                reasonParams = reasonParams,
                endTime = System.currentTimeMillis()
            )
        }

        /**
         * 系统自动取消（心跳超时、执行超时、排队满、并发互斥等）
         */
        fun ofCancelSystem(
            reasonCode: String,
            reasonParams: List<String>? = null,
            reasonDefault: String? = null
        ): BuildEndInfo {
            return BuildEndInfo(
                endType = BuildEndType.CANCEL_SYSTEM,
                reason = reasonDefault,
                reasonCode = reasonCode,
                reasonParams = reasonParams,
                endTime = System.currentTimeMillis()
            )
        }

        /**
         * 父流水线级联取消
         */
        fun ofCancelParentPipeline(
            reasonCode: String,
            parentPipelineInfo: ParentPipelineInfo,
            reasonParams: List<String>? = null,
            reasonDefault: String? = null
        ): BuildEndInfo {
            return BuildEndInfo(
                endType = BuildEndType.CANCEL_PARENT_PIPELINE,
                reason = reasonDefault,
                reasonCode = reasonCode,
                reasonParams = reasonParams,
                endTime = System.currentTimeMillis(),
                parentPipelineInfo = parentPipelineInfo
            )
        }

        /**
         * 通用构造：失败/超时/成功等所有非取消场景。
         *
         * 刻意不按大类拆成 ofFail/ofTimeout/ofSuccess——调用方在写入时点表达的是**结束成因**
         * （由 [endType] 承载），大类由 [BuildEndType.category] 直接决定，无需调用方重复指定，
         * 拆成三个方法只会让实现完全相同的重载散落各处。
         *
         * 调用方须保证 [endType] 的大类与构建最终状态同类，否则页面会出现
         * 「状态：已取消 / 类型：Job 超时」这类矛盾组合。放不进大类的具体成因用 [reason] 表达：
         * 例如 Job 执行超时派发的是 TERMINATE 事件，构建以取消/终止/失败收尾而非超时状态，
         * 应写取消或失败类子类型，「超过执行时限 15m」放在 [reason] 里。
         */
        fun of(
            endType: BuildEndType,
            reason: String? = null,
            reasonCode: String? = null,
            reasonParams: List<String>? = null
        ): BuildEndInfo {
            return BuildEndInfo(
                endType = endType,
                reason = reason,
                reasonCode = reasonCode,
                reasonParams = reasonParams,
                endTime = System.currentTimeMillis()
            )
        }
    }
}

@Schema(title = "父流水线信息")
data class ParentPipelineInfo(
    @get:Schema(title = "父流水线所属项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "父流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "父流水线名称", required = false)
    val pipelineName: String? = null,
    @get:Schema(title = "父构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "父构建号", required = false)
    val buildNum: Int? = null,
    @get:Schema(title = "父流水线操作人(仅用户主动操作时有值)", required = false)
    val operator: String? = null
)
