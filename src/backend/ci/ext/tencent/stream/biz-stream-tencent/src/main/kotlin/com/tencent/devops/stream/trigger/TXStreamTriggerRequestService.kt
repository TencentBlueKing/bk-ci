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

package com.tencent.devops.stream.trigger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.dao.StreamPipelineTriggerDao
import com.tencent.devops.stream.trigger.actions.EventActionFactory
import com.tencent.devops.stream.trigger.exception.handler.StreamTriggerExceptionHandler
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.trigger.parsers.TXPreTrigger
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerMatcher
import com.tencent.devops.stream.trigger.service.RepoTriggerEventService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Suppress("ComplexCondition")
@Primary
@Service
class TXStreamTriggerRequestService @Autowired constructor(
    dslContext: DSLContext,
    actionFactory: EventActionFactory,
    streamTriggerCache: StreamTriggerCache,
    exHandler: StreamTriggerExceptionHandler,
    triggerMatcher: TriggerMatcher,
    repoTriggerEventService: RepoTriggerEventService,
    streamTriggerRequestRepoService: StreamTriggerRequestRepoService,
    streamSettingDao: StreamBasicSettingDao,
    gitRequestEventDao: GitRequestEventDao,
    gitPipelineResourceDao: GitPipelineResourceDao,
    streamPipelineTriggerDao: StreamPipelineTriggerDao,
    rabbitTemplate: RabbitTemplate,
    streamGitConfig: StreamGitConfig,
    private val objectMapper: ObjectMapper,
    private val txPreTrigger: TXPreTrigger
) : StreamTriggerRequestService(
    objectMapper = objectMapper,
    dslContext = dslContext,
    rabbitTemplate = rabbitTemplate,
    actionFactory = actionFactory,
    streamTriggerCache = streamTriggerCache,
    exHandler = exHandler,
    triggerMatcher = triggerMatcher,
    repoTriggerEventService = repoTriggerEventService,
    streamTriggerRequestRepoService = streamTriggerRequestRepoService,
    streamSettingDao = streamSettingDao,
    gitRequestEventDao = gitRequestEventDao,
    gitPipelineResourceDao = gitPipelineResourceDao,
    streamGitConfig = streamGitConfig,
    streamPipelineTriggerDao = streamPipelineTriggerDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TXStreamTriggerRequestService::class.java)
    }

    override fun externalCodeGitBuild(eventType: String?, webHookType: String, event: String): Boolean? {
        logger.info("TXStreamTriggerRequestService|externalCodeGitBuild|event|$event|type|$eventType")
        val eventObject = try {
            objectMapper.readValue<GitEvent>(event)
        } catch (ignore: Exception) {
            logger.warn(
                "TXStreamTriggerRequestService|externalCodeGitBuild" +
                    "|Fail to parse the git web hook commit event, errMsg: ${ignore.message}"
            )
            return false
        }

        // 做一些在接收到请求后做的预处理
        if (eventObject is GitPushEvent) {
            txPreTrigger.enableAtomCi(eventObject)
        }

        return start(eventObject, event, ScmType.CODE_GIT)
    }
}
