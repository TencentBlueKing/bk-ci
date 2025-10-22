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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.process.engine.pojo.event.PipelineBuildReviewReminderEvent
import com.tencent.devops.common.notify.utils.NotifyUtils
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.engine.pojo.event.PipelineBuildNotifyEvent
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 审核提醒控制器
 * @version 1.0
 */
@Service
class ReviewReminderService @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineUrlBean: PipelineUrlBean
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ReviewReminderService::class.java)
    }

    /**
     * 入口
     */
    @BkTimed
    fun handle(event: PipelineBuildReviewReminderEvent) {
        with(event) {
            execute()
        }
    }

    /**
     * 处理[PipelineBuildReviewReminderEvent]事件
     */
    private fun PipelineBuildReviewReminderEvent.execute() {
        LOG.info(
            "ReviewReminder|$buildId|$source|ATOM_$actionType|t($taskId)" +
                "|ec=$executeCount|rc=$reminderCount"
        )

        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)

        val buildTask = pipelineTaskService.getBuildTask(projectId, buildId, taskId)
        // 检查event的执行次数是否和当前执行次数是否一致
        if (executeCount != buildTask?.executeCount) {
            LOG.info(
                "ReviewReminder|$buildId|$source|ATOM_$actionType|t($taskId)" +
                    "|ec=$executeCount|tec=${buildTask?.executeCount}|BAD_EC_WARN"
            )
            return
        }
        // 检查构建状态,防止重复跑
        if (buildInfo?.status?.isRunning() != true || !buildTask.status.isRunning()) {
            LOG.info(
                "ReviewReminder|$buildId|$source|ATOM_$actionType|t($taskId)" +
                    "|build=${buildInfo?.status}|task=${buildTask.status}｜TASK_DONE_WARNING"
            )
            return
        }

        val param = JsonUtil.mapTo((buildTask.taskParams), ManualReviewUserTaskElement::class.java)

        pipelineEventDispatcher.dispatch(
            PipelineBuildNotifyEvent(
                notifyTemplateEnum = PipelineNotifyTemplateEnum
                    .PIPELINE_MANUAL_REVIEW_ATOM_REMINDER_NOTIFY_TEMPLATE.name,
                source = "ManualReviewTaskAtom", projectId = projectId, pipelineId = pipelineId,
                userId = buildTask.starter, buildId = buildId,
                receivers = reviewUsers.toList(),
                notifyType = NotifyUtils.checkNotifyType(param.notifyType),
                titleParams = mutableMapOf(),
                bodyParams = mutableMapOf(
                    "title" to notifyTitle,
                    "body" to notifyBody,
                    "reviewUrl" to pipelineUrlBean.genBuildDetailUrl(
                        projectCode = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        position = null,
                        stageId = null,
                        needShortUrl = true
                    ),
                    "reviewAppUrl" to pipelineUrlBean.genAppBuildDetailUrl(
                        projectCode = projectId, pipelineId = pipelineId, buildId = buildId
                    ),
                    // 企业微信组
                    NotifyUtils.WEWORK_GROUP_KEY to (weworkGroup.joinToString(separator = ","))
                ),
                position = null,
                stageId = null,
                markdownContent = param.markdownContent
            ),
            this.copy(reminderCount = reminderCount + 1)
        )
    }
}
