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

package com.tencent.devops.notify.pojo

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 发送 IMate 会话消息请求体（创作流后台 -> IMate 后台）。
 *
 * 设计要点：
 * - **模板源在 stream 后台**：bk-ci 用 classpath 模板 + 业务参数本地渲染好 [html] 后发送，
 *   IMate 仅作为渲染容器和交互按钮事件转发器；
 * - 业务上下文（projectId/pipelineId/buildId/stageId/groupId/executeCount）必须 IMate 端持久化保存；
 *   当用户点击审核卡片上的「同意/驳回」按钮时，IMate 把这些字段原样回传到本端
 *   [com.tencent.devops.process.api.open.OpenStreamImateReviewResource.callback] 接口；
 * - 本对象**不带模板编码、不带占位符变量**——这是和「模板存 IMate 后台」方案的关键区别。
 */
@Schema(title = "IMate 会话消息发送请求")
data class ImateSendMessageRequest(
    @get:Schema(title = "IMate 会话ID（必填）", required = true)
    val sessionId: String,
    @get:Schema(title = "业务来源（如 CREATIVE_STREAM）", required = true)
    val bizType: String = "CREATIVE_STREAM",
    @get:Schema(
        title = "业务场景编码（如 STAGE_REVIEW / PIPELINE_FINISH_SUCCESS / PIPELINE_FINISH_FAIL）",
        required = true
    )
    val sceneCode: String,
    @get:Schema(title = "卡片标题", required = false)
    val title: String? = null,
    @get:Schema(title = "已渲染好的最终 HTML（含交互按钮 data-action='APPROVE/REJECT'）", required = true)
    val html: String,
    @get:Schema(
        title = "业务上下文：IMate 端必须持久化保存，按钮点击回调时原样回传到 stream 后台",
        required = true
    )
    val bizContext: ImateBizContext,
    @get:Schema(title = "交互按钮列表（同意/驳回 等，含按钮代码与文案）", required = false)
    val actions: List<ImateAction>? = null
)

/**
 * 业务上下文
 */
@Schema(title = "IMate 卡片业务上下文")
data class ImateBizContext(
    @get:Schema(title = "项目 ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线 ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "构建 ID", required = true)
    val buildId: String,
    @get:Schema(title = "Stage ID（仅 STAGE_REVIEW 场景必填）", required = false)
    val stageId: String? = null,
    @get:Schema(title = "审核组 ID（仅 STAGE_REVIEW 场景；不传则按当前活跃组处理）", required = false)
    val groupId: String? = null,
    @get:Schema(title = "执行次数（重试场景区分）", required = false)
    val executeCount: Int? = null
)

@Schema(title = "IMate 交互按钮")
data class ImateAction(
    @get:Schema(title = "按钮唯一标识，如 APPROVE / REJECT", required = true)
    val code: String,
    @get:Schema(title = "按钮文案", required = true)
    val label: String,
    @get:Schema(title = "按钮样式：primary / danger / default", required = false)
    val style: String? = "default"
)
