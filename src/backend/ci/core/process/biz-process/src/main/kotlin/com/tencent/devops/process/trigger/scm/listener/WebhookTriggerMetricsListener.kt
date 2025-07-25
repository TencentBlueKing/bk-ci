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

import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.event.pojo.measure.ProjectUserDailyEvent
import com.tencent.devops.common.event.pojo.measure.ProjectUserOperateMetricsData
import com.tencent.devops.common.event.pojo.measure.ProjectUserOperateMetricsEvent
import com.tencent.devops.common.event.pojo.measure.UserOperateCounterData
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_START_WEBHOOK_USER_ID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * 流水线触发度量监听器
 */
@Service
class WebhookTriggerMetricsListener(
    private val measureEventDispatcher: SampleEventDispatcher
) : WebhookTriggerListenerSupport() {
    override fun onBuildSuccess(context: WebhookTriggerContext) {
        with(context) {
            // 上报项目用户度量
            startParams?.get(PIPELINE_START_WEBHOOK_USER_ID)?.let {
                uploadProjectUserMetrics(
                    userId = it.toString(),
                    projectId = projectId,
                    theDate = LocalDate.now()
                )
            }
        }
    }

    private fun uploadProjectUserMetrics(
        userId: String,
        projectId: String,
        theDate: LocalDate
    ) {
        try {
            val projectUserOperateMetricsKey = ProjectUserOperateMetricsData(
                projectId = projectId,
                userId = userId,
                operate = WEBHOOK_COMMIT_TRIGGER,
                theDate = theDate
            ).getProjectUserOperateMetricsKey()
            measureEventDispatcher.dispatch(
                ProjectUserDailyEvent(
                    projectId = projectId,
                    userId = userId,
                    theDate = theDate
                ),
                ProjectUserOperateMetricsEvent(
                    userOperateCounterData = UserOperateCounterData().apply {
                        this.increment(projectUserOperateMetricsKey)
                    }
                )
            )
        } catch (ignored: Exception) {
            logger.error("save auth user metrics", ignored)
        }
    }

    companion object {
        const val WEBHOOK_COMMIT_TRIGGER = "webhook_commit_trigger"
        private val logger = LoggerFactory.getLogger(WebhookTriggerMetricsListener::class.java)
    }
}
