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

package com.tencent.devops.stream.listener.components

import com.tencent.devops.common.api.enums.BuildReviewType
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.between
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.stream.config.StreamBuildFinishConfig
import com.tencent.devops.stream.listener.StreamBuildListenerContextV2
import com.tencent.devops.stream.listener.StreamBuildStageListenerContextV2
import com.tencent.devops.stream.listener.getBuildStatus
import com.tencent.devops.stream.listener.getGitCommitCheckState
import com.tencent.devops.stream.listener.isSuccess
import com.tencent.devops.stream.pojo.isMr
import com.tencent.devops.stream.trigger.GitCheckService
import com.tencent.devops.stream.trigger.StreamTriggerCache
import com.tencent.devops.stream.utils.CommitCheckUtils
import com.tencent.devops.stream.utils.GitCIPipelineUtils
import com.tencent.devops.stream.v2.service.StreamGitTokenService
import com.tencent.devops.stream.v2.service.StreamScmService
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Suppress("NestedBlockDepth")
@Component
class SendCommitCheck @Autowired constructor(
    private val client: Client,
    private val config: StreamBuildFinishConfig,
    private val gitCheckService: GitCheckService,
    private val streamGitTokenService: StreamGitTokenService,
    private val streamTriggerCache: StreamTriggerCache,
    private val streamScmService: StreamScmService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SendCommitCheck::class.java)
        private const val BUILD_RUNNING_DESC = "Running."
        private const val BUILD_STAGE_SUCCESS_DESC =
            "Warning: your pipeline「%s」 is stage succeed. Rejected by %s, reason is %s."
        private const val BUILD_SUCCESS_DESC = "Successful in %sm."
        private const val BUILD_CANCEL_DESC = "Your pipeline「%s」 was cancelled."
        private const val BUILD_FAILED_DESC = "Failing after %sm."
        private const val BUILD_GATE_REVIEW_DESC =
            "Pending: gate access requirement is not met, gatekeeper's approval is needed."
        private const val BUILD_MANUAL_REVIEW_DESC =
            "Stage success: pipeline approval is needed."
    }

    fun sendCommitCheck(
        context: StreamBuildListenerContextV2
    ) {
        try {
            if (CommitCheckUtils.needSendCheck(context.requestEvent, context.streamSetting)) {
                sendCommitCheckV2(context)
            }
        } catch (e: Throwable) {
            logger.error("sendCommitCheck error: ${context.requestEvent}")
        }
    }

    private fun sendCommitCheckV2(
        context: StreamBuildListenerContextV2
    ) {
        with(context) {
            gitCheckService.pushCommitCheck(
                streamGitProjectInfo = streamTriggerCache.getAndSaveRequestGitProjectInfo(
                    gitRequestEventId = requestEvent.id!!,
                    gitProjectId = requestEvent.gitProjectId.toString(),
                    token = streamGitTokenService.getToken(requestEvent.gitProjectId),
                    useAccessToken = true,
                    getProjectInfo = streamScmService::getProjectInfoRetry
                ),
                commitId = requestEvent.commitId,
                description = getDescByBuildStatus(this),
                // 由stage event红线评论发送
                mergeRequestId = null,
                projectId = pipeline.gitProjectId,
                buildId = buildEvent.buildId,
                userId = buildEvent.userId,
                status = getGitCommitCheckState(),
                context = "${pipeline.filePath}@${requestEvent.objectKind.toUpperCase()}",
                pipelineId = buildEvent.pipelineId,
                block = requestEvent.isMr() && !context.isSuccess() && streamSetting.enableMrBlock,
                targetUrl = getTargetUrl(context)
            )
        }
    }

    // 根据状态切换描述
    private fun getDescByBuildStatus(context: StreamBuildListenerContextV2): String {
        val pipelineName = context.pipeline.displayName
        return when (context.getBuildStatus()) {
            BuildStatus.REVIEWING -> {
                getStageReviewDesc(context)
            }
            BuildStatus.REVIEW_PROCESSED -> {
                BUILD_RUNNING_DESC
            }
            else -> {
                getFinishDesc(context, pipelineName)
            }
        }
    }

    private fun getStageReviewDesc(
        context: StreamBuildListenerContextV2
    ): String {
        if (context !is StreamBuildStageListenerContextV2) {
            return BUILD_RUNNING_DESC
        }
        return when (context.reviewType) {
            BuildReviewType.STAGE_REVIEW -> {
                BUILD_MANUAL_REVIEW_DESC
            }
            BuildReviewType.QUALITY_CHECK_IN, BuildReviewType.QUALITY_CHECK_OUT -> {
                BUILD_GATE_REVIEW_DESC
            }
            // 这里先这么写，未来如果这么枚举扩展代码编译时可以第一时间感知，防止漏过事件
            BuildReviewType.TASK_REVIEW -> {
                logger.warn("buildReviewListener event not match: ${context.reviewType}")
                BUILD_RUNNING_DESC
            }
        }
    }

    private fun getFinishDesc(
        context: StreamBuildListenerContextV2,
        pipelineName: String
    ) = when {
        (context.isSuccess()) -> {
            if (context.getBuildStatus() == BuildStatus.STAGE_SUCCESS) {
                val (name, reason) = getReviewInfo(context)
                BUILD_STAGE_SUCCESS_DESC.format(pipelineName, name, reason)
            } else {
                BUILD_SUCCESS_DESC.format(getFinishTime(context.buildEvent.startTime).toString())
            }
        }
        context.getBuildStatus().isCancel() -> {
            BUILD_CANCEL_DESC.format(pipelineName)
        }
        else -> {
            BUILD_FAILED_DESC.format(getFinishTime(context.buildEvent.startTime).toString())
        }
    }

    private fun getFinishTime(startTimeTimeStamp: Long?): Long {
        val zoneId = ZoneId.systemDefault()
        val startTime = LocalDateTime.ofInstant(startTimeTimeStamp?.let { Instant.ofEpochMilli(it) }, zoneId)
        return startTime.between(LocalDateTime.now()).toMinutes()
    }

    private fun getTargetUrl(
        context: StreamBuildListenerContextV2
    ): String {
        var checkIn: String?
        var checkOut: String?
        with(context) {
            if (context !is StreamBuildStageListenerContextV2) {
                checkIn = null
                checkOut = null
            } else {
                val pair = context.isCheckInOrOut()
                checkIn = pair.first
                checkOut = pair.second
            }
            return GitCIPipelineUtils.genGitCIV2BuildUrl(
                homePage = config.v2GitUrl ?: throw ParamBlankException("启动配置缺少 rtx.v2GitUrl"),
                gitProjectId = streamSetting.gitProjectId,
                pipelineId = pipeline.pipelineId,
                buildId = buildEvent.buildId,
                openCheckInId = checkIn,
                openCheckOutId = checkOut
            )
        }
    }

    private fun getReviewInfo(context: StreamBuildListenerContextV2): Pair<String, String> {
        val model = try {
            client.get(ServiceBuildResource::class).getBuildDetail(
                userId = context.buildEvent.userId,
                projectId = context.buildEvent.projectId,
                pipelineId = context.buildEvent.pipelineId,
                buildId = context.buildEvent.buildId,
                channelCode = ChannelCode.GIT
            ).data!!.model
        } catch (e: Exception) {
            logger.warn("get build finish model info error: ${e.message}")
            return Pair(" ", " ")
        }
        model.stages.forEach { stage ->
            if (stage.checkIn?.status == BuildStatus.REVIEW_ABORT.name) {
                stage.checkIn?.reviewGroups?.forEach { review ->
                    if (review.status == ManualReviewAction.ABORT.name) {
                        return Pair(review.operator ?: " ", review.suggest ?: " ")
                    }
                }
            }
        }
        return Pair(" ", " ")
    }
}

private fun StreamBuildStageListenerContextV2.isCheckInOrOut(): Pair<String?, String?> {
    return when (this.reviewType) {
        // 人工审核目前只在checkIn
        BuildReviewType.STAGE_REVIEW, BuildReviewType.QUALITY_CHECK_IN -> {
            Pair(this.buildEvent.stageId, null)
        }
        BuildReviewType.QUALITY_CHECK_OUT -> {
            Pair(null, this.buildEvent.stageId)
        }
        // 这里先这么写，未来如果这么枚举扩展代码编译时可以第一时间感知，防止漏过事件
        BuildReviewType.TASK_REVIEW -> {
            Pair(null, null)
        }
    }
}
