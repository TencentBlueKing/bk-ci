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
 * 契约约定（与 IMate 端）：
 * - **UI 与交互都在 [body] 内**：bk-ci 用 classpath 模板 + 业务参数本地渲染好 HTML 后发送，
 *   按钮也写在 HTML 里（形如 `<button data-action="APPROVE">同意</button>`），如需更复杂的交互可在模板里写内联 JS；
 *   IMate 客户端只需在卡片 root 节点上委托监听 `click` 事件，命中 `[data-action]` 元素时
 *   读取其 `dataset.action`（必要时还有 `dataset.*` 携带的额外字段）作为回调入参；
 * - **业务上下文走 [bizContext]**：IMate 端必须按卡片维度持久化保存，按钮点击回调时
 *   原样回传到本端 [com.tencent.devops.process.api.open.OpenStreamImateReviewResource.callback]；
 * - 本对象不带占位符变量 / 按钮结构化描述 —— [templateCode] 与 [com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest.templateCode]
 *   语义一致，IMate 端只用作日志归类 / 灰度 / 风控键，不参与渲染（渲染已在 stream 后台完成）。
 */
@Schema(title = "IMate 会话消息发送请求")
data class ImateSendMessageRequest(
    @get:Schema(title = "IMate 会话ID（必填）", required = true)
    val sessionId: String,
    @get:Schema(title = "业务来源（如 CREATIVE_STREAM）", required = true)
    val bizType: String = "CREATIVE_STREAM",
    @get:Schema(
        title = "通知模板代码（如 CREATIVE_STREAM_STAGE_REVIEW / CREATIVE_STREAM_PIPELINE_FINISH_SUCCESS 等），" +
            "供 IMate 端做日志归类 / 灰度 / 风控使用，不参与渲染",
        required = true
    )
    val templateCode: String,
    @get:Schema(title = "卡片标题（用于会话列表预览 / 通知栏；正文样式以 body 为准）", required = false)
    val title: String? = null,
    @get:Schema(
        title = "已渲染好的最终消息内容（HTML，含交互按钮 `<button data-action='APPROVE|REJECT'>` 及必要的内联 JS）",
        required = true
    )
    val body: String,
    @get:Schema(
        title = "业务上下文：IMate 端必须按卡片维度持久化保存，按钮点击回调时原样回传到 stream 后台",
        required = true
    )
    val bizContext: ImateBizContext
)

/**
 * 业务上下文 —— 用于把审核卡片绑定到具体的某次 stage 审核。
 * IMate 端必须保存并在按钮点击回调中原样回传
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
    @get:Schema(title = "执行次数", required = false)
    val executeCount: Int? = null
)
