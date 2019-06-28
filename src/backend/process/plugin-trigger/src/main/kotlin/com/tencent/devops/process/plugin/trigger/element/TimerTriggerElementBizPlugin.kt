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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.plugin.trigger.element

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.process.plugin.ElementBizPlugin
import com.tencent.devops.process.plugin.annotation.ElementBiz
import com.tencent.devops.process.plugin.trigger.service.PipelineTimerService
import org.quartz.CronExpression
import org.slf4j.LoggerFactory

@ElementBiz
class TimerTriggerElementBizPlugin constructor(
    private val pipelineTimerService: PipelineTimerService
) : ElementBizPlugin<TimerTriggerElement> {

    override fun elementClass(): Class<TimerTriggerElement> {
        return TimerTriggerElement::class.java
    }

    override fun check(element: TimerTriggerElement, appearedCnt: Int) {
        if (appearedCnt > 1) {
            throw IllegalArgumentException("只允许一个代码扫描原子")
        }
    }

    override fun afterCreate(
        element: TimerTriggerElement,
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        userId: String,
        channelCode: ChannelCode
    ) {
        if (element.isElementEnable()) {
            val crontabExpressions = mutableSetOf<String>()
            val eConvertExpressions = element.convertExpressions()
            if (eConvertExpressions.isEmpty()) {
                throw OperationException("定时触发器的定时参数为空,不合法")
            }
            eConvertExpressions.forEach { cron ->
                if (!CronExpression.isValidExpression(cron)) {
                    throw OperationException("定时触发器的定时参数[$cron]不合法")
                }
            }
            crontabExpressions.addAll(eConvertExpressions)
            if (crontabExpressions.isNotEmpty()) {
                val result = pipelineTimerService.saveTimer(
                    projectId, pipelineId, userId, crontabExpressions, channelCode
                )
                logger.info("[$pipelineId]| update pipeline timer|crontab=$crontabExpressions")
                if (result.isNotOk()) {
                    throw OperationException(result.message ?: "保存定时器失败，请检查定时表达式参数")
                }
            } else {
                pipelineTimerService.deleteTimer(pipelineId, userId)
                logger.info("[$pipelineId]| delete pipeline timer|crontab=empty")
            }
        }
    }

    override fun beforeDelete(element: TimerTriggerElement, userId: String, pipelineId: String?) {
        if (pipelineId != null) {
            pipelineTimerService.deleteTimer(pipelineId, userId)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TimerTriggerElementBizPlugin::class.java)
    }
}