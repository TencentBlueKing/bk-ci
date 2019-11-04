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

package com.tencent.devops.process.plugin.trigger.timer.listener

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.process.plugin.trigger.pojo.event.PipelineTimerChangeEvent
import com.tencent.devops.process.plugin.trigger.timer.SchedulerManager
import com.tencent.devops.process.plugin.trigger.timer.quartz.PipelineQuartzJob
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 *  MQ实现的流水线定时调度器变更处理，保证所有节点都能收到变更消息，保持同步
 *
 * @version 1.0
 */
@Component
class PipelineTimerChangerListener @Autowired constructor(
    pipelineEventDispatcher: PipelineEventDispatcher,
    private val schedulerManager: SchedulerManager
) : BaseListener<PipelineTimerChangeEvent>(pipelineEventDispatcher) {

    private val jobBeanClass = PipelineQuartzJob::class.java

    override fun run(event: PipelineTimerChangeEvent) {
        val pipelineId = event.pipelineId
        val crontabExpressions =
            try {
                JsonUtil.to(event.crontabExpressionJson, object : TypeReference<List<String>>() {})
            } catch (ignored: Throwable) { // 兼容旧数据
                logger.warn("[$pipelineId]|Old Timer Crontab=${event.crontabExpressionJson}| ${ignored.message}")
                listOf(event.crontabExpressionJson)
            }
        try {
            crontabExpressions.forEach { crontab ->
                val md5 = DigestUtils.md5Hex(crontab)
                val comboKey = "${pipelineId}_$md5"
                if (schedulerManager.checkExists(comboKey)) {
                    schedulerManager.deleteJob(comboKey)
                }
                if (ActionType.REFRESH == (event.actionType)) {
                    val success = schedulerManager.addJob(comboKey, crontab, jobBeanClass)
                    logger.info("[$pipelineId]|TimerChange|crontab=$crontab|success=$success")
                }
            }
        } catch (ignored: Throwable) {
            logger.error("[$pipelineId]|TimerChange fail event=$event| error=${ignored.message}", ignored)
        }
    }
}
