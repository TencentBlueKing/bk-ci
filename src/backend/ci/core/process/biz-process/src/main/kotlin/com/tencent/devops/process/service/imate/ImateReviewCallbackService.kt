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

package com.tencent.devops.process.service.imate

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.process.pojo.imate.ImateReviewCallbackAction
import com.tencent.devops.process.pojo.imate.ImateReviewCallbackRequest
import com.tencent.devops.process.pojo.imate.ImateReviewCallbackResult
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * IMate 审核回调的业务实现。
 *
 * 设计原则：
 * - 入参里的 projectId / pipelineId / buildId / stageId 由 IMate 端在收到原始消息时持久化保存并原样回传；
 * - 复用既有 [PipelineBuildFacadeService.buildManualStartStage]（与 user 端的 `manualStartStage` 完全等价）；
 * - userId 来自 IMate 端的真实点击人；权限校验在 [PipelineBuildFacadeService.buildManualStartStage] 内部完成
 *   （检查 reviewer 是否包含 userId）；
 * - 业务态错误（已审核 / 状态不对 / 用户无权限）以结果体形式返回，避免 IMate 无意义重试；
 *   仅未知错误才抛 5xx 让 IMate 端重试。
 */
@Service
class ImateReviewCallbackService @Autowired constructor(
    private val pipelineBuildFacadeService: PipelineBuildFacadeService
) {

    fun handle(userId: String, request: ImateReviewCallbackRequest): ImateReviewCallbackResult {
        if (userId.isBlank()) throw ParamBlankException("userId is blank")
        if (request.projectId.isBlank() || request.pipelineId.isBlank() ||
            request.buildId.isBlank() || request.stageId.isBlank()
        ) {
            return ImateReviewCallbackResult(
                success = false,
                code = ERR_PARAM,
                message = "projectId/pipelineId/buildId/stageId all required"
            )
        }

        val isCancel = when (request.action) {
            ImateReviewCallbackAction.APPROVE -> false
            ImateReviewCallbackAction.REJECT -> true
        }

        val reviewRequest = StageReviewRequest(
            reviewParams = emptyList(),
            id = request.groupId,
            suggest = request.suggest ?: ""
        )

        return try {
            pipelineBuildFacadeService.buildManualStartStage(
                userId = userId,
                projectId = request.projectId,
                pipelineId = request.pipelineId,
                buildId = request.buildId,
                stageId = request.stageId,
                isCancel = isCancel,
                reviewRequest = reviewRequest
            )
            logger.info(
                "[IMATE_CALLBACK] handled|user=$userId|build=${request.buildId}|stage=${request.stageId}|" +
                    "action=${request.action}"
            )
            ImateReviewCallbackResult(success = true)
        } catch (e: ErrorCodeException) {
            logger.warn(
                "[IMATE_CALLBACK] business error|user=$userId|build=${request.buildId}|" +
                    "stage=${request.stageId}|err=${e.errorCode}|msg=${e.defaultMessage}"
            )
            val code = if (e.statusCode == Response.Status.FORBIDDEN.statusCode) ERR_FORBIDDEN else ERR_BUSINESS
            ImateReviewCallbackResult(success = false, code = code, message = e.defaultMessage ?: e.errorCode)
        } catch (e: Exception) {
            logger.error("[IMATE_CALLBACK] unexpected error|user=$userId|build=${request.buildId}", e)
            throw ErrorCodeException(
                statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                errorCode = CommonMessageCode.SYSTEM_ERROR,
                defaultMessage = e.message
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ImateReviewCallbackService::class.java)
        const val ERR_PARAM = "IMATE_PARAM_INVALID"
        const val ERR_FORBIDDEN = "IMATE_FORBIDDEN"
        const val ERR_BUSINESS = "IMATE_BUSINESS_ERROR"
    }
}
