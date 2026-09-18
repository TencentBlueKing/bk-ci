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

import com.tencent.devops.common.pipeline.enums.BuildEndType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 构建终态位置详情，统一描述取消/失败/超时时被影响的具体组件。
 *
 * 位置级 endType 用于「多类失败」场景：同一次构建中不同位置的终止原因可能不同
 * （如一个位置是插件执行失败、另一个是质量红线未达标），前端需按位置分别渲染标签。
 *
 * 位置级 [reason]/[reasonCode] 与构建级 [BuildEndInfo.reason] 取值规则一致（reasonCode 优先），
 * 用于同一构建存在多个同类位置、构建级无法用一句话概括的场景：
 * 如多个人工审核驳回时，每个驳回位置各自的驳回意见只能挂在位置上，
 * 否则构建级只能展示其中一条，与位置列表相互冲突。
 */
@Schema(title = "终态位置详情")
data class EndPosition(
    @get:Schema(title = "位置编码(如3-1-1表示Stage3的Job1的Task1, 3-2表示Stage3的Job2)", required = true)
    val position: String,
    @get:Schema(title = "组件路径(如: 质量检查/单元测试/执行单测)", required = true)
    val componentPath: String,
    @get:Schema(title = "终态时的组件状态枚举值(BuildStatus.name)", required = true)
    val statusAtEnd: String,
    @get:Schema(title = "终态时的组件状态描述(国际化)", required = false)
    var statusAtEndDesc: String? = null,
    @get:Schema(title = "该位置的终态子类型(多类失败时用于区分每个位置的原因)", required = false)
    val endType: BuildEndType? = null,
    @get:Schema(title = "该位置终态子类型描述(国际化)", required = false)
    var endTypeDesc: String? = null,
    @get:Schema(title = "该位置的终态原因(兜底文案，如审核驳回意见、子流水线失败信息)", required = false)
    var reason: String? = null,
    @get:Schema(title = "该位置终态原因的国际化标识", required = false)
    val reasonCode: String? = null,
    @get:Schema(title = "该位置终态原因的国际化占位符参数", required = false)
    val reasonParams: List<String>? = null,
    @get:Schema(title = "阶段ID(锚点定位)", required = true)
    val stageId: String,
    @get:Schema(title = "容器ID(锚点定位)", required = true)
    val containerId: String,
    @get:Schema(title = "插件ID(锚点定位,task级别时填充)", required = false)
    val taskId: String? = null,
    @get:Schema(title = "是否构建矩阵", required = false)
    val matrixFlag: Boolean? = null,
    @get:Schema(title = "错误类型(ErrorType.num，前端据此展示错误图标)", required = false)
    val errorType: Int? = null,
    @get:Schema(title = "错误码", required = false)
    val errorCode: Int? = null,
    @get:Schema(title = "错误信息", required = false)
    val errorMsg: String? = null,
    @get:Schema(title = "操作人(人工审核驳回等场景的处理人)", required = false)
    val operator: String? = null,
    @get:Schema(title = "审核意见(人工审核驳回原因)", required = false)
    val reviewSuggest: String? = null,
    @get:Schema(title = "审核组序号(阶段准入审核位置填充，从1开始)", required = false)
    val reviewGroupSeq: Int? = null,
    @get:Schema(title = "审核组名称(阶段准入审核位置填充)", required = false)
    val reviewGroupName: String? = null,
    @get:Schema(title = "待审核人列表(阶段准入审核中位置填充)", required = false)
    val reviewers: List<String>? = null,
    @get:Schema(title = "容器HashId(日志跳转)", required = false)
    val containerHashId: String? = null,
    @get:Schema(title = "步骤ID(日志跳转)", required = false)
    val stepId: String? = null,
    @get:Schema(title = "子流水线信息(子流水线失败时填充，供跳转子构建详情)", required = false)
    val subPipelineInfo: SubPipelineInfo? = null,
    @get:Schema(title = "依赖Job导航信息(DEPENDENT_WAITING状态时填充)", required = false)
    val dependOnJobs: List<DependOnJobInfo>? = null
)

/**
 * 子流水线导航信息，用于「子流水线失败」位置跳转到子构建详情页。
 * 子流水线可能与父流水线不属于同一项目，因此必须携带 projectId。
 */
@Schema(title = "子流水线信息")
data class SubPipelineInfo(
    @get:Schema(title = "子流水线所属项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "子流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "子流水线名称", required = false)
    val pipelineName: String? = null,
    @get:Schema(title = "子构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "子构建号", required = false)
    val buildNum: Int? = null
)

@Schema(title = "依赖Job导航信息")
data class DependOnJobInfo(
    @get:Schema(title = "依赖的Job名称", required = true)
    val jobName: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "执行次数", required = true)
    val executeCount: Int,
    @get:Schema(title = "依赖Job所在阶段ID(锚点定位)", required = true)
    val stageId: String,
    @get:Schema(title = "依赖Job的容器ID(锚点定位)", required = true)
    val containerId: String,
    @get:Schema(title = "依赖Job是否为构建矩阵", required = false)
    val matrixFlag: Boolean? = null
)
