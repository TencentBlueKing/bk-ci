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

package com.tencent.devops.process.service.pipeline.task

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscription
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.store.pojo.trigger.enums.TriggerTargetEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * TAPD 触发器版本处理器
 */
@Service
class TapdTriggerElementVersionProcessor @Autowired constructor(
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao
) : PipelineTaskVersionProcessor {

    companion object {
        private val logger = LoggerFactory.getLogger(TapdTriggerElementVersionProcessor::class.java)
    }

    override fun support(element: Element): Boolean = element is TapdWebHookTriggerElement

    override fun postProcessAfterSave(
        transactionContext: DSLContext,
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting,
        element: Element,
        variables: Map<String, String>
    ) {
        val triggerElement = element as TapdWebHookTriggerElement
        val projectId = pipelineResourceVersion.projectId
        val pipelineId = pipelineResourceVersion.pipelineId
        val taskId = triggerElement.id ?: run {
            logger.warn("skip register tapd subscription|element id is null|$projectId|$pipelineId")
            return
        }
        // 插件被禁用，移除订阅关系
        if (!triggerElement.elementEnabled()) {
            logger.info("tapd trigger disabled, remove subscription|$projectId|$pipelineId|$taskId")
            pipelineEventSubscriptionDao.delete(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                taskId = taskId
            )
            return
        }
        val input = triggerElement.data.input
        // 解析变量后的实际值（支持流水线变量替换）
        val tapdProjectId = EnvUtils.parseEnv(input.tapdProjectId, variables)
        val eventType = input.eventType?.value ?: run {
            logger.warn("skip register tapd subscription|eventType is null|$projectId|$pipelineId|$taskId")
            return
        }
        if (tapdProjectId.isBlank()) {
            logger.warn("skip register tapd subscription|tapdProjectId is blank|$projectId|$pipelineId|$taskId")
            return
        }
        val subscription = PipelineEventSubscription(
            projectId = projectId,
            pipelineId = pipelineId,
            taskId = taskId,
            eventCode = eventType,
            eventSource = tapdProjectId,
            eventType = eventType,
            channelCode = context.pipelineBasicInfo.channelCode,
            triggerTarget = TriggerTargetEnum.PIPELINE
        )
        pipelineEventSubscriptionDao.save(
            dslContext = transactionContext,
            userId = context.userId,
            subscription = subscription
        )
        logger.info(
            "register tapd subscription success|$projectId|$pipelineId|$taskId|" +
                    "tapdProjectId=$tapdProjectId|eventType=$eventType"
        )
    }
}
