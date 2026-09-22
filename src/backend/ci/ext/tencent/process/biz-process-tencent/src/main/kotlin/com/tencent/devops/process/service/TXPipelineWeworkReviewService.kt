/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.process.service

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.notify.pojo.wework.WeworkReviewCardConst
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.pojo.pipeline.ExtServiceWeworkReviewCallBackReq
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 企业微信审核模板卡片同意/拒绝回调（对齐 MyOA 一键审核）。
 */
@Service
class TXPipelineWeworkReviewService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService,
    private val redisOperation: RedisOperation
) {
    private val logger = LoggerFactory.getLogger(TXPipelineWeworkReviewService::class.java)

    @Value("\${esb.appSecret:}")
    private val appSecret: String = ""

    /**
     * 网关透传的 /external/review/wework 入口。兼容简易字段与企微 EventKey/FromUserName。
     */
    fun handleExternalCallBack(req: ExtServiceWeworkReviewCallBackReq): Result<String> {
        val (userId, action, taskId) = resolveExternalCallBack(req)
        return Result(handleReviewAction(userId, action, taskId))
    }

    /**
     * 企微机器人「通过」走跳转 GET，浏览器内展示结果。
     */
    fun handleExternalJump(req: ExtServiceWeworkReviewCallBackReq): String {
        val result = handleExternalCallBack(req)
        val message = result.data ?: result.message ?: "处理完成"
        val success = message.startsWith("已由")
        return htmlPage(success, message)
    }

    fun handleReviewAction(userId: String, action: String, taskId: String): String {
        logger.info("wework review card callback|userId=$userId|action=$action|taskId=$taskId")
        if (taskId.isBlank()) {
            return "无效的审核请求"
        }
        if (action != WeworkReviewCardConst.ACTION_AGREE && action != WeworkReviewCardConst.ACTION_REJECT) {
            return "不支持的操作: $action"
        }
        val cacheJson = redisOperation.get(WeworkReviewCardConst.REDIS_KEY_PREFIX + taskId)
        if (cacheJson.isNullOrBlank()) {
            return "审核卡片已过期或不存在，请打开详情页处理"
        }
        val callBackData: Map<String, String> = try {
            JsonUtil.to(cacheJson, jacksonTypeRef())
        } catch (ignored: Exception) {
            logger.warn("parse wework review card cache failed|taskId=$taskId", ignored)
            return "审核数据解析失败，请打开详情页处理"
        }
        val actualUserId = userId.ifBlank {
            WeworkReviewCardConst.weworkUserId(callBackData["receiver"].orEmpty())
        }
        if (actualUserId.isBlank()) {
            return "无效的审核请求"
        }
        logger.info("wework review card callback resolved|userId=$actualUserId|action=$action|taskId=$taskId")

        return try {
            when (callBackData["reviewType"]) {
                WeworkReviewCardConst.REVIEW_TYPE_STAGE -> handleStageReview(actualUserId, action, callBackData)
                else -> handleAtomReview(actualUserId, action, callBackData)
            }
        } catch (e: ErrorCodeException) {
            logger.info("wework review rejected by business rule: ${e.errorCode}|${e.message}")
            e.defaultMessage ?: "审核失败：${e.errorCode}"
        } catch (ignored: Throwable) {
            logger.warn("wework review card callback failed", ignored)
            "审核处理失败：${ignored.message ?: "unknown"}"
        }
    }

    private fun handleAtomReview(userId: String, action: String, callBackData: Map<String, String>): String {
        val projectId = callBackData["projectId"].orEmpty()
        val pipelineId = callBackData["pipelineId"].orEmpty()
        val buildId = callBackData["buildId"].orEmpty()
        val elementId = callBackData["elementId"].orEmpty()
        val callbackSignature = callBackData["signature"].orEmpty()
        checkParameter(projectId, pipelineId, buildId, elementId, callbackSignature)

        val signature = ShaUtils.sha256(projectId + buildId + elementId + appSecret)
        if (!ShaUtils.isEqual(signature, callbackSignature)) {
            logger.warn("wework atom review signature mismatch|$callbackSignature|$signature")
            return "签名校验失败"
        }

        val reviewUsers = callBackData["reviewUsers"].orEmpty().split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (reviewUsers.isNotEmpty() && userId !in reviewUsers) {
            return "您不是当前审核人，无法操作"
        }

        val status = pipelineTaskService.getTaskStatus(projectId = projectId, buildId = buildId, taskId = elementId)
        if (status?.isRunning() == false) {
            return "该审核已处理，无需重复操作"
        }

        if (action == WeworkReviewCardConst.ACTION_AGREE &&
            callBackData["hasRequiredParams"].equals("true", ignoreCase = true)
        ) {
            val reviewUrl = callBackData["reviewUrl"].orEmpty()
            return if (reviewUrl.isNotBlank()) {
                "该审核含必填自定义参数，请打开详情填写后再同意：$reviewUrl"
            } else {
                "该审核含必填自定义参数，请打开详情填写后再同意"
            }
        }

        val params = pipelineBuildFacadeService.goToReview(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            elementId = elementId
        ).apply {
            this.status = when (action) {
                WeworkReviewCardConst.ACTION_AGREE -> ManualReviewAction.PROCESS
                else -> ManualReviewAction.ABORT
            }
            if (action == WeworkReviewCardConst.ACTION_REJECT) {
                suggest = WeworkReviewCardConst.DEFAULT_REJECT_SUGGEST
            }
        }
        // goToReview 可能返回空对象（找不到插件），兜底构造
        if (params.projectId.isBlank()) {
            params.projectId = projectId
            params.pipelineId = pipelineId
            params.buildId = buildId
            params.status = if (action == WeworkReviewCardConst.ACTION_AGREE) {
                ManualReviewAction.PROCESS
            } else {
                ManualReviewAction.ABORT
            }
            if (action == WeworkReviewCardConst.ACTION_REJECT) {
                params.suggest = WeworkReviewCardConst.DEFAULT_REJECT_SUGGEST
            }
        }

        val pipelineInfo = pipelineInfoDao.getPipelineInfo(dslContext, projectId, pipelineId)
        val channelCode = pipelineInfo?.let { ChannelCode.valueOf(it.channel) } ?: ChannelCode.getRequestChannelCode()
        pipelineBuildFacadeService.buildManualReview(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            elementId = elementId,
            params = params,
            channelCode = channelCode,
            checkPermission = ChannelCode.isNeedAuth(channelCode),
            stepId = null
        )
        return if (action == WeworkReviewCardConst.ACTION_AGREE) {
            "已由 $userId 同意审核"
        } else {
            "已由 $userId 拒绝审核"
        }
    }

    private fun handleStageReview(userId: String, action: String, callBackData: Map<String, String>): String {
        val projectId = callBackData["projectId"].orEmpty()
        val pipelineId = callBackData["pipelineId"].orEmpty()
        val buildId = callBackData["buildId"].orEmpty()
        val stageId = callBackData["stageId"].orEmpty()
        val groupId = callBackData["groupId"].orEmpty().ifBlank { null }
        val callbackSignature = callBackData["signature"].orEmpty()
        checkParameter(projectId, pipelineId, buildId, stageId, callbackSignature)

        val signature = ShaUtils.sha256(projectId + buildId + stageId + (groupId ?: "") + appSecret)
        if (!ShaUtils.isEqual(signature, callbackSignature)) {
            logger.warn("wework stage review signature mismatch|$callbackSignature|$signature")
            return "签名校验失败"
        }

        val reviewUsers = callBackData["reviewUsers"].orEmpty().split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (reviewUsers.isNotEmpty() && userId !in reviewUsers) {
            return "您不是当前审核人，无法操作"
        }

        if (action == WeworkReviewCardConst.ACTION_AGREE &&
            callBackData["hasRequiredParams"].equals("true", ignoreCase = true)
        ) {
            val reviewUrl = callBackData["reviewUrl"].orEmpty()
            return if (reviewUrl.isNotBlank()) {
                "该 Stage 审核含必填自定义参数，请打开详情填写后再同意：$reviewUrl"
            } else {
                "该 Stage 审核含必填自定义参数，请打开详情填写后再同意"
            }
        }

        val reviewRequest = StageReviewRequest(
            reviewParams = emptyList(),
            id = groupId,
            suggest = if (action == WeworkReviewCardConst.ACTION_REJECT) {
                WeworkReviewCardConst.DEFAULT_REJECT_SUGGEST
            } else {
                ""
            }
        )
        pipelineBuildFacadeService.buildManualStartStage(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            isCancel = action == WeworkReviewCardConst.ACTION_REJECT,
            reviewRequest = reviewRequest
        )
        return if (action == WeworkReviewCardConst.ACTION_AGREE) {
            "已由 $userId 同意 Stage 审核"
        } else {
            "已由 $userId 拒绝 Stage 审核"
        }
    }

    private fun checkParameter(vararg args: String) {
        args.find { it.isBlank() }?.let { throw IllegalArgumentException("parameter is null or blank") }
    }

    companion object {
        /**
         * 解析网关透传体：优先独立字段，缺省时从 EventKey 拆 action/taskId。
         */
        fun resolveExternalCallBack(req: ExtServiceWeworkReviewCallBackReq): Triple<String, String, String> {
            val userId = WeworkReviewCardConst.weworkUserId(req.userId.orEmpty())
            val parsed = req.eventKey?.takeIf { it.isNotBlank() }?.let {
                WeworkReviewCardConst.parseButtonKey(it)
            }
            val rawAction = req.action?.takeIf { it.isNotBlank() }
            val action = when {
                rawAction != null -> {
                    if (rawAction == WeworkReviewCardConst.ACTION_APPROVE) {
                        WeworkReviewCardConst.ACTION_AGREE
                    } else {
                        rawAction
                    }
                }
                parsed != null -> parsed.first
                else -> ""
            }
            val taskId = req.taskId?.takeIf { it.isNotBlank() } ?: parsed?.second.orEmpty()
            return Triple(userId, action, taskId)
        }

        fun htmlPage(success: Boolean, message: String): String {
            val safe = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            val color = if (success) "#0b6e4f" else "#8a1c1c"
            return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<title>流水线审核</title></head>" +
                "<body style=\"font-family:sans-serif;padding:24px;color:$color\"><p>$safe</p></body></html>"
        }
    }
}
