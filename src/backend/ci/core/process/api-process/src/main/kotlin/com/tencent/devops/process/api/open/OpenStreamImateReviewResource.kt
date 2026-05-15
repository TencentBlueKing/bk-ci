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

package com.tencent.devops.process.api.open

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.imate.ImateReviewCallbackRequest
import com.tencent.devops.process.pojo.imate.ImateReviewCallbackResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

/**
 * 创作流-IMate 审核回调（Open 类型接口）。
 *
 * 用法：IMate 后台在用户点击「同意 / 驳回」按钮后调用本接口；
 * 鉴权：网关层通过 [com.tencent.devops.common.web.constant.BkApiHandleType.API_OPEN_TOKEN_CHECK]
 *       校验 [AUTH_HEADER_DEVOPS_BK_TOKEN]（双方平台预共享 token）。
 *
 * 等价于 user 端的 [com.tencent.devops.process.api.user.UserBuildResource.manualStartStage]，
 * 区别仅在于：
 *   - 调用方是外部系统（IMate），所以走 /open；
 *   - 审核坐标（projectId/pipelineId/buildId/stageId 等）由请求体里的显式字段提供 ——
 *     这些字段是 IMate 在收到原始消息（[com.tencent.devops.notify.pojo.ImateSendMessageRequest.bizContext]）时持久化保存，
 *     在按钮点击时原样回传过来的。
 */
@Tag(name = "OPEN_STREAM_REVIEW", description = "open-创作流 IMate 审核回调")
@Path("/open/stream-review")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpenStreamImateReviewResource {

    @Operation(
        summary = "IMate 审核回调（同意/驳回）",
        description = "由 IMate 后台在用户点击交互按钮后调用，驱动 stage 审核（等价于 user 端的 manualStartStage）。" +
            "审核坐标 projectId/pipelineId/buildId/stageId 等由请求体显式字段提供。"
    )
    @POST
    @Path("/callback")
    fun callback(
        @Parameter(description = "平台间预共享 BK 鉴权 token（IMate 后台 → 创作流后台）", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        token: String,
        @Parameter(
            description = "操作人（IMate 端真实点击人；由 IMate 后台保证可信）",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "回调请求体", required = true)
        request: ImateReviewCallbackRequest
    ): Result<ImateReviewCallbackResult>
}
