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
 *
 */

package com.tencent.devops.process.webhook.listener

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.webhook.atom.IWebhookAtomTask
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.common.webhook.pojo.WebhookRequestReplay
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TRIGGER_TYPE_REPLAY_NOT_SUPPORT
import com.tencent.devops.process.dao.PipelineTriggerEventDao
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineTriggerRequestService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineTriggerEventDao: PipelineTriggerEventDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTriggerRequestService::class.java)
        private val triggerTypeToTaskAtom = mapOf(
            PipelineTriggerType.CODE_GIT.name to CodeGitWebHookTriggerElement.taskAtom,
            PipelineTriggerType.CODE_TGIT.name to CodeTGitWebHookTriggerElement.taskAtom,
            PipelineTriggerType.CODE_P4.name to CodeP4WebHookTriggerElement.taskAtom,
            PipelineTriggerType.CODE_GITLAB to CodeGitlabWebHookTriggerElement.taskAtom,
            PipelineTriggerType.CODE_SVN to CodeSVNWebHookTriggerElement.taskAtom,
            PipelineTriggerType.GITHUB to CodeGithubWebHookTriggerElement.taskAtom
        )
    }

    fun handleRequest(taskAtom: String, request: WebhookRequest) {
        SpringContextUtil.getBean(IWebhookAtomTask::class.java, taskAtom).request(request = request)
    }

    fun handleReplayRequest(
        userId: String,
        projectId: String,
        eventId: Long,
        pipelineId: String? = null
    ) {
        val triggerEvent = pipelineTriggerEventDao.getTriggerEvent(
            dslContext = dslContext,
            projectId = projectId,
            eventId = eventId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_TRIGGER_EVENT_NOT_FOUND,
            params = arrayOf(eventId.toString())
        )
        val taskAtom = triggerTypeToTaskAtom[triggerEvent.triggerType] ?: throw ErrorCodeException(
            errorCode = ERROR_TRIGGER_TYPE_REPLAY_NOT_SUPPORT,
            params = arrayOf(triggerEvent.triggerType)
        )
        val request = WebhookRequestReplay(
            userId = userId,
            projectId = projectId,
            hookRequestId = triggerEvent.hookRequestId!!,
            pipelineId = pipelineId
        )
        SpringContextUtil.getBean(IWebhookAtomTask::class.java, taskAtom).replay(request = request)
    }
}
