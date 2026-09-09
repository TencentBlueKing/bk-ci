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
        title = "终态大类(结束成因归类，恒等于endType所属大类；构建最终状态见ModelRecord.status，二者可能不同类)",
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
