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

package com.tencent.devops.process.pojo.imate

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "IMate 审核回调动作")
enum class ImateReviewCallbackAction {
    @Schema(title = "同意（继续执行流水线）")
    APPROVE,

    @Schema(title = "驳回（终止流水线）")
    REJECT
}

/**
 * IMate 审核按钮回调请求体。
 *
 * 设计要点：
 * - 全部审核坐标使用**显式字段**，IMate 端在收到原始消息时（[com.tencent.devops.notify.pojo.ImateSendMessageRequest.bizContext]）
 *   持久化保存，并在按钮点击时原样回传到本接口；
 * - 不依赖任何签名 token —— Open 接口的平台间鉴权由 [com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN]
 *   承担（双方平台预共享 token，由网关 [com.tencent.devops.common.web.constant.BkApiHandleType.API_OPEN_TOKEN_CHECK] 校验）。
 */
@Schema(title = "IMate 审核回调请求")
data class ImateReviewCallbackRequest(
    @get:Schema(title = "项目 ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线 ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "构建 ID", required = true)
    val buildId: String,
    @get:Schema(title = "Stage ID", required = true)
    val stageId: String,
    @get:Schema(title = "审核组 ID（不传时由后端按当前活跃审核组解析）", required = false)
    val groupId: String? = null,
    @get:Schema(title = "执行次数（重试场景区分；不传则不校验）", required = false)
    val executeCount: Int? = null,
    @get:Schema(title = "动作（APPROVE / REJECT）", required = true)
    val action: ImateReviewCallbackAction,
    @get:Schema(title = "审核建议", required = false)
    val suggest: String? = null
)

@Schema(title = "IMate 审核回调响应")
data class ImateReviewCallbackResult(
    @get:Schema(title = "是否成功", required = true)
    val success: Boolean,
    @get:Schema(title = "错误码（成功时为空）", required = false)
    val code: String? = null,
    @get:Schema(title = "错误信息（成功时为空）", required = false)
    val message: String? = null
)
