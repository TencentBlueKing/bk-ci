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

package com.tencent.devops.process.notify

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.PipelineEventListener
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.constant.ProcessMessageCode.BK_BUILD_IN_REVIEW_STATUS
import com.tencent.devops.process.engine.pojo.event.PipelineBuildNotifyEvent
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import com.tencent.devops.process.service.ProjectCacheService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PipelineBuildNotifyListener @Autowired constructor(
    private val client: Client,
    private val pipelineUrlBean: PipelineUrlBean,
    private val projectCacheService: ProjectCacheService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : PipelineEventListener<PipelineBuildNotifyEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineBuildNotifyEvent) {
        try {
            val pipelineNotDeleted = client.get(ServicePipelineResource::class)
                .getPipelineInfo(projectId = event.projectId, pipelineId = event.pipelineId, channelCode = null).data
            if (pipelineNotDeleted == null) {
                logger.warn("NOTIFY|CHECK_PIPE|Pipeline[${event.projectId}/${event.pipelineId}] may be deleted!")
                return
            }
        } catch (ignore: Exception) {
            logger.warn("NOTIFY|CHECK_PIPE|SKIP_ERROR_CHECK", ignore)
        }
        val notifyTemplateEnumType = PipelineNotifyTemplateEnum.parse(event.notifyTemplateEnum)
        when {
            notifyTemplateEnumType.isReviewNotifyTemplate() ->
                handleReviewNotify(event, notifyTemplateEnumType)

            notifyTemplateEnumType == PipelineNotifyTemplateEnum.PIPELINE_MANUAL_REVIEW_ATOM_REMINDER_NOTIFY_TEMPLATE ->
                sendNotifyRequest(event.buildNotifyRequest())

            else -> {
                // need to add
            }
        }
    }

    /**
     * 处理审核类通知：包含完成审核检查、生成审核 URL、发送审核通知等逻辑
     */
    private fun handleReviewNotify(
        event: PipelineBuildNotifyEvent,
        notifyTemplateEnumType: PipelineNotifyTemplateEnum
    ) {
        if (event.notifyCompleteCheck) {
            event.completeReviewNotify()
            return
        }
        // 获取流水线渠道信息，用于生成对应渠道的 URL
        val channelCode = getChannelCode(event.projectId, event.pipelineId)
        // 仅人工审核插件和 stage 审核需要跳转到审核页面，其余跳转构建详情页
        val reviewUrl = if (notifyTemplateEnumType.isReviewPageTemplate()) {
            pipelineUrlBean.genBuildReviewUrl(
                projectCode = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                stageSeq = event.stageSeq,
                taskId = event.taskId,
                needShortUrl = true,
                channelCode = channelCode
            )
        } else {
            pipelineUrlBean.genBuildDetailUrl(
                projectCode = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                position = event.position,
                stageId = event.stageId,
                needShortUrl = true,
                channelCode = channelCode
            )
        }
        event.sendReviewNotify(
            reviewUrl = reviewUrl,
            reviewAppUrl = pipelineUrlBean.genAppBuildDetailUrl(
                projectCode = event.projectId, pipelineId = event.pipelineId,
                buildId = event.buildId, channelCode = channelCode
            )
        )
    }

    /**
     * 发送审核通知，在发送前补充项目名称和审核 URL 等参数
     */
    private fun PipelineBuildNotifyEvent.sendReviewNotify(reviewUrl: String, reviewAppUrl: String) {
        try {
            val projectName = projectCacheService.getProjectName(projectId) ?: projectId
            if (titleParams["content"].isNullOrBlank()) {
                val buildNum = bodyParams["buildNum"]
                val pipelineName = bodyParams["pipelineName"]
                titleParams["content"] = MessageUtil.getMessageByLocale(
                    BK_BUILD_IN_REVIEW_STATUS,
                    I18nUtil.getDefaultLocaleLanguage(),
                    arrayOf(projectName, "$pipelineName", "$buildNum")
                )
            }
            titleParams["projectName"] = projectName
            bodyParams["reviewUrl"] = reviewUrl
            bodyParams["reviewAppUrl"] = reviewAppUrl
            bodyParams["projectName"] = projectName
            sendNotifyRequest(buildNotifyRequest())
        } catch (ignored: Exception) {
            logger.warn("[$buildId]|[$source]|PIPELINE_SEND_NOTIFY_FAIL| receivers: $receivers error: $ignored")
        }
    }

    /**
     * 从事件对象构建通知请求
     */
    private fun PipelineBuildNotifyEvent.buildNotifyRequest() = SendNotifyMessageTemplateRequest(
        templateCode = PipelineNotifyTemplateEnum.valueOf(notifyTemplateEnum).templateCode,
        receivers = receivers.toMutableSet(),
        cc = receivers.toMutableSet(),
        titleParams = titleParams,
        bodyParams = bodyParams,
        notifyType = notifyType,
        markdownContent = markdownContent,
        mentionReceivers = mentionReceivers,
        callbackData = callbackData
    )

    /**
     * 发送通知请求
     */
    private fun sendNotifyRequest(request: SendNotifyMessageTemplateRequest) {
        client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
    }

    /**
     * 获取流水线的渠道信息
     */
    private fun getChannelCode(projectId: String, pipelineId: String): ChannelCode? {
        return try {
            pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)?.channelCode
        } catch (ignored: Throwable) {
            logger.warn("NOTIFY|GET_CHANNEL|Failed to get channelCode for $projectId/$pipelineId", ignored)
            null
        }
    }

    /**
     * 取消审批单.
     */
    fun PipelineBuildNotifyEvent.completeReviewNotify() {
        try {
            val request = SendNotifyMessageTemplateRequest(
                templateCode = PipelineNotifyTemplateEnum.valueOf(notifyTemplateEnum).templateCode,
                receivers = receivers.toMutableSet(),
                callbackData = callbackData
            )
            client.get(ServiceNotifyMessageTemplateResource::class).completeNotifyMessageByTemplate(request)
        } catch (ignored: Exception) {
            logger.warn("[$buildId]|[$source]|PIPELINE_SEND_FINISH_NOTIFY_FAIL| receivers: $receivers error: $ignored")
        }
    }
}
