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

package com.tencent.devops.process.trigger.scm.listener

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_CREATE_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_CREATE_SUCCESS
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_DELETE_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_DELETE_SUCCESS
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_DELETE_VERSION_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_DELETE_VERSION_SUCCESS
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_UPDATE_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_UPDATE_SUCCESS
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetail
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetailCombination
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedErrorCode
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMatch
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMsg
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetailMessageCode
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReason
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReasonDetail
import com.tencent.devops.process.pojo.trigger.PipelineTriggerStatus
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.yaml.pojo.YamlPipelineActionType
import org.springframework.stereotype.Service

/**
 * 流水线触发事件记录监听器
 */
@Service
class WebhookTriggerEventListener(
    private val pipelineTriggerEventService: PipelineTriggerEventService
) : WebhookTriggerListenerSupport(), PipelineYamlChangeListener {

    override fun onBuildSuccess(context: WebhookTriggerContext) {
        val triggerDetail = with(context) {
            if (buildId == null) return
            PipelineTriggerDetail(
                detailId = pipelineTriggerEventService.getDetailId(),
                projectId = projectId,
                eventId = eventId,
                status = PipelineTriggerStatus.SUCCEED.name,
                pipelineId = pipelineId,
                pipelineName = pipelineInfo!!.pipelineName,
                reason = PipelineTriggerReason.TRIGGER_SUCCESS.name,
                buildId = buildId!!.id,
                buildNum = buildId!!.num?.toString() ?: ""
            )
        }
        pipelineTriggerEventService.saveTriggerDetail(triggerDetail)
    }

    override fun onError(context: WebhookTriggerContext, exception: Exception) {
        val exceptionReasonDetail = when (exception) {
            is ErrorCodeException -> PipelineTriggerFailedErrorCode(
                errorCode = exception.errorCode,
                params = exception.params?.toList()
            )

            else -> PipelineTriggerFailedMsg(exception.message ?: "unknown error")
        }
        val triggerDetail = with(context) {
            PipelineTriggerDetail(
                detailId = pipelineTriggerEventService.getDetailId(),
                projectId = projectId,
                eventId = eventId,
                status = PipelineTriggerStatus.FAILED.name,
                pipelineId = pipelineId,
                pipelineName = pipelineInfo?.pipelineName ?: "",
                reason = PipelineTriggerReason.TRIGGER_FAILED.name,
                reasonDetail = exceptionReasonDetail
            )
        }
        pipelineTriggerEventService.saveTriggerDetail(triggerDetail)
    }

    override fun onMatchFailed(context: WebhookTriggerContext) {
        val triggerDetail = with(context) {
            PipelineTriggerDetail(
                detailId = pipelineTriggerEventService.getDetailId(),
                projectId = projectId,
                eventId = eventId,
                status = PipelineTriggerStatus.FAILED.name,
                pipelineId = pipelineId,
                pipelineName = pipelineInfo!!.pipelineName,
                reason = PipelineTriggerReason.TRIGGER_NOT_MATCH.name,
                reasonDetail = PipelineTriggerFailedMatch(elements = context.failedMatchElements!!)
            )
        }
        pipelineTriggerEventService.saveTriggerDetail(triggerDetail)
    }

    override fun onChangeSuccess(context: PipelineYamlChangeContext) {
        val triggerDetail = with(context) {
            val reasonDetail = getChangeSuccessMsg() ?: return
            PipelineTriggerDetail(
                detailId = pipelineTriggerEventService.getDetailId(),
                projectId = projectId,
                eventId = eventId,
                status = PipelineTriggerStatus.SUCCEED.name,
                pipelineId = filePath,
                pipelineName = filePath,
                reason = PipelineTriggerReason.TRIGGER_SUCCESS.name,
                reasonDetail = reasonDetail
            )
        }
        pipelineTriggerEventService.saveTriggerDetail(triggerDetail)
    }

    override fun onChangeError(context: PipelineYamlChangeContext, exception: java.lang.Exception) {
        val exceptionReasonDetail = when (exception) {
            is ErrorCodeException -> PipelineTriggerFailedErrorCode(
                errorCode = exception.errorCode,
                params = exception.params?.toList()
            )

            else -> PipelineTriggerFailedMsg(exception.message ?: "unknown error")
        }

        val triggerDetail = with(context) {
            val msgReasonDetail = getChangeFailedMsg() ?: return
            val reasonDetail = PipelineTriggerDetailCombination(
                details = listOf(
                    msgReasonDetail,
                    exceptionReasonDetail
                )
            )
            PipelineTriggerDetail(
                detailId = pipelineTriggerEventService.getDetailId(),
                projectId = projectId,
                eventId = eventId,
                status = PipelineTriggerStatus.FAILED.name,
                pipelineId = filePath,
                pipelineName = filePath,
                reason = PipelineTriggerReason.TRIGGER_FAILED.name,
                reasonDetail = reasonDetail
            )
        }
        pipelineTriggerEventService.saveTriggerDetail(triggerDetail)
    }

    /**
     * 获取yaml流水线变更成功说明
     */
    private fun PipelineYamlChangeContext.getChangeSuccessMsg(): PipelineTriggerReasonDetail? {
        return when (actionType) {
            YamlPipelineActionType.CREATE -> {
                val linkUrl = getPipelineUrl(projectId = projectId, pipelineId = pipelineId)
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_CREATE_SUCCESS,
                    params = listOf(linkUrl, pipelineName ?: "", versionName ?: "")
                )
            }

            YamlPipelineActionType.UPDATE -> {
                val linkUrl = getPipelineUrl(projectId = projectId, pipelineId = pipelineId)
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_UPDATE_SUCCESS,
                    params = listOf(linkUrl, pipelineName ?: "", versionName ?: "")
                )
            }

            YamlPipelineActionType.DELETE_VERSION -> {
                val linkUrl = getPipelineUrl(projectId = projectId, pipelineId = pipelineId)
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_DELETE_VERSION_SUCCESS,
                    params = listOf(linkUrl, pipelineName ?: "", versionName ?: "")
                )
            }

            YamlPipelineActionType.DELETE -> {
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_DELETE_SUCCESS,
                    params = listOf(pipelineName ?: "", pipelineId ?: "")
                )
            }
            else -> null
        }
    }

    /**
     * 获取yaml流水线变更失败原因
     */
    private fun PipelineYamlChangeContext.getChangeFailedMsg(): PipelineTriggerReasonDetail? {
        return when (actionType) {
            YamlPipelineActionType.CREATE -> {
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_CREATE_FAILED
                )
            }

            YamlPipelineActionType.UPDATE -> {
                val linkUrl = getPipelineUrl(projectId = projectId, pipelineId = pipelineId)
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_UPDATE_FAILED,
                    listOf(linkUrl, pipelineName ?: pipelineId ?: "")
                )
            }

            YamlPipelineActionType.DELETE_VERSION -> {
                val linkUrl = getPipelineUrl(projectId = projectId, pipelineId = pipelineId)
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_DELETE_VERSION_FAILED,
                    listOf(linkUrl, pipelineName ?: "", versionName ?: "")
                )
            }

            YamlPipelineActionType.DELETE -> {
                val linkUrl = getPipelineUrl(projectId = projectId, pipelineId = pipelineId)
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_DELETE_FAILED,
                    params = listOf(linkUrl, pipelineName ?: pipelineId ?: "")
                )
            }
            else -> null
        }
    }

    private fun getPipelineUrl(projectId: String?, pipelineId: String?): String {
        return if (projectId.isNullOrBlank() || pipelineId.isNullOrBlank()) {
            return ""
        } else {
            "/console/pipeline/$projectId/$pipelineId"
        }
    }
}
