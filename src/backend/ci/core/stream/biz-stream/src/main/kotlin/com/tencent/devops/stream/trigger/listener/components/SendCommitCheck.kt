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

package com.tencent.devops.stream.trigger.listener.components

import com.tencent.devops.common.api.enums.BuildReviewType
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.constant.StreamMessageCode.STARTUP_CONFIG_MISSING
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.data.context.BuildFinishData
import com.tencent.devops.stream.trigger.actions.data.context.BuildFinishStageData
import com.tencent.devops.stream.trigger.actions.data.context.getBuildStatus
import com.tencent.devops.stream.trigger.actions.data.context.getGitCommitCheckState
import com.tencent.devops.stream.trigger.actions.data.context.isSuccess
import com.tencent.devops.stream.trigger.actions.data.isStreamMr
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.util.StreamPipelineUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Suppress("NestedBlockDepth")
@Component
class SendCommitCheck @Autowired constructor(
    private val client: Client,
    private val streamTriggerCache: StreamTriggerCache,
    private val streamGitConfig: StreamGitConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SendCommitCheck::class.java)
        private const val BUILD_RUNNING_DESC = "Running."
        private const val BUILD_STAGE_SUCCESS_DESC =
            "Warning: your pipeline「%s」 is stage succeed. Rejected by %s, reason is %s."
        private const val BUILD_SUCCESS_DESC = "Successful in %s."
        private const val BUILD_CANCEL_DESC = "Your pipeline「%s」 was cancelled."
        private const val BUILD_FAILED_DESC = "Failing after %s."
        private const val BUILD_GATE_REVIEW_DESC =
            "Pending: gate access requirement is not met, gatekeeper's approval is needed."
        private const val BUILD_MANUAL_REVIEW_DESC =
            "Stage success: pipeline approval is needed."
    }

    fun sendCommitCheck(
        action: BaseAction
    ) {
        try {
            if (action.data.setting.enableCommitCheck && action.needSendCommitCheck()) {
                sendCommitCheckV2(action)
            }
        } catch (e: Throwable) {
            logger.warn("SendCommitCheck|error=${action.format()}")
        }
    }

    private fun sendCommitCheckV2(
        action: BaseAction
    ) {
        val streamGitProjectInfo = streamTriggerCache.getAndSaveRequestGitProjectInfo(
            gitProjectKey = action.data.eventCommon.gitProjectId,
            action = action,
            getProjectInfo = action.api::getGitProjectInfo
        )!!

        val finishData = action.data.context.finishData!!
        action.sendCommitCheck(
            buildId = finishData.buildId,
            gitProjectName = streamGitProjectInfo.name,
            state = finishData.getGitCommitCheckState(),
            block = action.metaData.isStreamMr() && action.data.setting.enableMrBlock &&
                    !finishData.isSuccess(),
            context = "${action.data.context.pipeline!!.filePath}@${action.metaData.streamObjectKind.name}",
            targetUrl = getTargetUrl(action),
            description = getDescByBuildStatus(
                pipelineName = action.data.context.pipeline!!.displayName,
                finishData = finishData
            )
        )
    }

    // 根据状态切换描述
    private fun getDescByBuildStatus(pipelineName: String, finishData: BuildFinishData): String {
        return when (finishData.getBuildStatus()) {
            BuildStatus.REVIEWING -> {
                getStageReviewDesc(finishData)
            }
            BuildStatus.REVIEW_PROCESSED -> {
                BUILD_RUNNING_DESC
            }
            else -> {
                getFinishDesc(finishData, pipelineName)
            }
        }
    }

    private fun getStageReviewDesc(
        finishData: BuildFinishData
    ): String {
        if (finishData !is BuildFinishStageData) {
            return BUILD_RUNNING_DESC
        }
        return when (finishData.reviewType) {
            BuildReviewType.STAGE_REVIEW -> {
                BUILD_MANUAL_REVIEW_DESC
            }
            BuildReviewType.QUALITY_CHECK_IN, BuildReviewType.QUALITY_CHECK_OUT -> {
                BUILD_GATE_REVIEW_DESC
            }
            // 这里先这么写，未来如果这么枚举扩展代码编译时可以第一时间感知，防止漏过事件
            BuildReviewType.TASK_REVIEW -> {
                logger.warn("SendCommitCheck|getStageReviewDesc|event not match|${finishData.reviewType}")
                BUILD_RUNNING_DESC
            }
            BuildReviewType.QUALITY_TASK_REVIEW_PASS, BuildReviewType.QUALITY_TASK_REVIEW_ABORT,
            BuildReviewType.TRIGGER_REVIEW -> {
                ""
            }
        }
    }

    private fun getFinishDesc(
        finishData: BuildFinishData,
        pipelineName: String
    ) = when {
        (finishData.isSuccess()) -> {
            if (finishData.getBuildStatus() == BuildStatus.STAGE_SUCCESS) {
                val (name, reason) = getReviewInfo(finishData)
                BUILD_STAGE_SUCCESS_DESC.format(pipelineName, name, reason)
            } else {
                BUILD_SUCCESS_DESC.format(getFinishTime(finishData.startTime))
            }
        }
        finishData.getBuildStatus().isCancel() -> {
            BUILD_CANCEL_DESC.format(pipelineName)
        }
        else -> {
            BUILD_FAILED_DESC.format(getFinishTime(finishData.startTime))
        }
    }

    private fun getFinishTime(startTimeTimeStamp: Long?): String {
        val zoneId = ZoneId.systemDefault()
        val startTime = LocalDateTime.ofInstant(startTimeTimeStamp?.let { Instant.ofEpochMilli(it) }, zoneId)
        return startTime.between(LocalDateTime.now()).format()
    }

    private fun Duration.format(): String {
        if (this === Duration.ZERO) {
            return "0s"
        }
        val day = (seconds / 86400).toInt()
        val hours = ((seconds / 3600) % 24).toInt()
        val minutes = (seconds % 3600 / 60).toInt()
        val secs = (seconds % 60).toInt()
        fun join(int: Int, name: String) = if (int > 0) " $int$name" else ""
        return join(day, "d") + join(hours, "h") + join(minutes, "m") + join(secs, "s")
    }

    private fun getTargetUrl(
        action: BaseAction
    ): String {
        var checkIn: String?
        var checkOut: String?
        val finishData = action.data.context.finishData!!
        if (finishData !is BuildFinishStageData) {
            checkIn = null
            checkOut = null
        } else {
            val pair = finishData.isCheckInOrOut()
            checkIn = pair.first
            checkOut = pair.second
        }
        return StreamPipelineUtils.genStreamV2BuildUrl(
            homePage = streamGitConfig.streamUrl ?: throw ParamBlankException(
                I18nUtil.getCodeLanMessage(
                    messageCode = STARTUP_CONFIG_MISSING,
                    params = arrayOf(" streamUrl")
                )
            ),
            gitProjectId = action.data.getGitProjectId(),
            pipelineId = action.data.context.pipeline!!.pipelineId,
            buildId = finishData.buildId,
            openCheckInId = checkIn,
            openCheckOutId = checkOut
        )
    }

    private fun getReviewInfo(finishData: BuildFinishData): Pair<String, String> {
        val model = try {
            client.get(ServiceBuildResource::class).getBuildDetail(
                userId = finishData.userId,
                projectId = finishData.projectId,
                pipelineId = finishData.pipelineId,
                buildId = finishData.buildId,
                channelCode = ChannelCode.GIT
            ).data!!.model
        } catch (e: Exception) {
            logger.warn("SendCommitCheck|getReviewInfo|get build finish model info error|${e.message}")
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

private fun LocalDateTime.between(endLocalDateTime: LocalDateTime?): Duration {
    return Duration.between(this, endLocalDateTime)
}

private fun BuildFinishStageData.isCheckInOrOut(): Pair<String?, String?> {
    return when (this.reviewType) {
        // 人工审核目前只在checkIn
        BuildReviewType.STAGE_REVIEW, BuildReviewType.QUALITY_CHECK_IN -> {
            Pair(this.stageId, null)
        }
        BuildReviewType.QUALITY_CHECK_OUT -> {
            Pair(null, this.stageId)
        }
        // 这里先这么写，未来如果这么枚举扩展代码编译时可以第一时间感知，防止漏过事件
        BuildReviewType.TASK_REVIEW -> {
            Pair(null, null)
        }
        BuildReviewType.QUALITY_TASK_REVIEW_PASS, BuildReviewType.QUALITY_TASK_REVIEW_ABORT,
        BuildReviewType.TRIGGER_REVIEW -> {
            Pair(null, null)
        }
    }
}
