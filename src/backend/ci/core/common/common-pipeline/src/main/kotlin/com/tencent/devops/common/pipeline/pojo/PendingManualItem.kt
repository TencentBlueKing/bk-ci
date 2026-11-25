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

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 待人工处理项，用于运行中构建顶部的「待人工处理 N 项」提示条。
 *
 * 处理人有两种表达方式，二者互斥：
 * - [handlers] 有明确名单（人工审核的审核人、质量红线的把关人、阶段准入的审核人）
 * - [handlerDescCode] 无固定名单，按权限判定（执行前暂停由「有执行权限者」处理），
 *   存国际化 messageCode 而非文案，由读取侧按读者语言渲染到 [handlerDesc]
 */
@Schema(title = "待人工处理项")
data class PendingManualItem(
    @get:Schema(title = "待处理类型", required = true)
    val itemType: PendingItemType,
    @get:Schema(title = "待处理类型描述(国际化)", required = false)
    var itemTypeDesc: String? = null,
    @get:Schema(title = "处理人名单(有明确名单时填充)", required = false)
    val handlers: List<String>? = null,
    @get:Schema(title = "处理人描述标识(无固定名单时填充)", required = false)
    val handlerDescCode: String? = null,
    @get:Schema(title = "处理人描述(国际化，读取时按handlerDescCode渲染)", required = false)
    var handlerDesc: String? = null,
    @get:Schema(title = "位置编码(如3-1-1表示Stage3的Job1的Task1, 3表示Stage3)", required = true)
    val position: String,
    @get:Schema(title = "组件路径(如: 质量检查/单元测试/执行单测)", required = true)
    val componentPath: String,
    @get:Schema(title = "阶段ID(锚点定位)", required = true)
    val stageId: String,
    @get:Schema(title = "容器ID(锚点定位，阶段级待处理项为空)", required = false)
    val containerId: String? = null,
    @get:Schema(title = "插件ID(锚点定位，任务级待处理项填充)", required = false)
    val taskId: String? = null,
    @get:Schema(title = "容器HashId(日志跳转)", required = false)
    val containerHashId: String? = null,
    @get:Schema(title = "步骤ID(日志跳转)", required = false)
    val stepId: String? = null,
    @get:Schema(title = "是否构建矩阵", required = false)
    val matrixFlag: Boolean? = null,
    @get:Schema(title = "审核说明", required = false)
    val reviewDesc: String? = null
)

/**
 * 待人工处理项类型。
 *
 * 任务级与阶段级刻意分开：二者的处理入口、权限判定和前端跳转锚点都不同
 * （任务级跳到插件，阶段级跳到 Stage 的准入/准出弹窗）。
 */
enum class PendingItemType(val displayName: String) {
    // 插件配置了执行前暂停，等待有执行权限者继续或终止
    TASK_PAUSE("taskPause"),
    // 人工审核插件等待审核
    TASK_REVIEW("taskReview"),
    // 插件级质量红线审核，等待把关人处理
    TASK_QUALITY_GATE("taskQualityGate"),
    // 阶段准入/准出人工审核
    STAGE_REVIEW("stageReview"),
    // 阶段准入/准出质量红线等待把关
    STAGE_QUALITY_GATE("stageQualityGate")
}
