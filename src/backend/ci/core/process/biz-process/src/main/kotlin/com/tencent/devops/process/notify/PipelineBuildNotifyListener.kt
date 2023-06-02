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

package com.tencent.devops.process.notify

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.constant.ProcessMessageCode.BK_BUILD_IN_REVIEW_STATUS
import com.tencent.devops.process.engine.pojo.event.PipelineBuildNotifyEvent
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import com.tencent.devops.process.service.ProjectCacheService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PipelineBuildNotifyListener @Autowired constructor(
    private val client: Client,
    private val pipelineUrlBean: PipelineUrlBean,
    private val projectCacheService: ProjectCacheService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseListener<PipelineBuildNotifyEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineBuildNotifyEvent) {
        when (val notifyTemplateEnumType = PipelineNotifyTemplateEnum.parse(event.notifyTemplateEnum)) {
            PipelineNotifyTemplateEnum.PIPELINE_MANUAL_REVIEW_STAGE_NOTIFY_TEMPLATE,
            PipelineNotifyTemplateEnum.PIPELINE_MANUAL_REVIEW_ATOM_NOTIFY_TEMPLATE,
            PipelineNotifyTemplateEnum.PIPELINE_TRIGGER_REVIEW_NOTIFY_TEMPLATE,
            PipelineNotifyTemplateEnum.PIPELINE_MANUAL_REVIEW_STAGE_NOTIFY_TO_TRIGGER_TEMPLATE,
            PipelineNotifyTemplateEnum.PIPELINE_MANUAL_REVIEW_STAGE_REJECT_TO_TRIGGER_TEMPLATE
            -> {
                if (event.notifyCompleteCheck) {
                    event.completeReviewNotify()
                } else {
                    event.sendReviewNotify(
                        templateCode = notifyTemplateEnumType.templateCode,
                        reviewUrl = pipelineUrlBean.genBuildDetailUrl(
                            projectCode = event.projectId,
                            pipelineId = event.pipelineId,
                            buildId = event.buildId,
                            position = event.position,
                            stageId = event.stageId,
                            needShortUrl = true
                        ),
                        reviewAppUrl = pipelineUrlBean.genAppBuildDetailUrl(
                            projectCode = event.projectId, pipelineId = event.pipelineId, buildId = event.buildId
                        )
                    )
                }
            }
            else -> {
                // need to add
            }
        }
    }

    fun PipelineBuildNotifyEvent.sendReviewNotify(reviewUrl: String, reviewAppUrl: String, templateCode: String) {
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
            val request = SendNotifyMessageTemplateRequest(
                templateCode = PipelineNotifyTemplateEnum.valueOf(notifyTemplateEnum).templateCode,
                receivers = receivers.toMutableSet(),
                cc = receivers.toMutableSet(),
                titleParams = titleParams,
                bodyParams = bodyParams,
                notifyType = notifyType,
                markdownContent = markdownContent,
                callbackData = callbackData
            )
            client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
        } catch (ignored: Exception) {
            logger.warn("[$buildId]|[$source]|PIPELINE_SEND_NOTIFY_FAIL| receivers: $receivers error: $ignored")
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
