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

package com.tencent.devops.process.plugin.svn

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.PipelineWebhookRevisionDao
import com.tencent.devops.process.engine.dao.PipelineWebhookDao
import com.tencent.devops.process.plugin.svn.service.TriggerSvnService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import java.util.Calendar
import java.util.Date

@Suppress("LongParameterList")
@Configuration
@ConditionalOnProperty(prefix = "scm.svn", name = ["enabled"], havingValue = "true")
class PollSvnConfig : SchedulingConfigurer {

    @Bean
    fun triggerSvnService(
        client: Client,
        pipelineWebhookRevisionDao: PipelineWebhookRevisionDao,
        pipelineWebhookDao: PipelineWebhookDao,
        dslContext: DSLContext,
        redisOperation: RedisOperation,
        streamBridge: StreamBridge
    ): TriggerSvnService {
        return TriggerSvnService(
            client = client,
            pipelineWebhookRevisionDao = pipelineWebhookRevisionDao,
            pipelineWebhookDao = pipelineWebhookDao,
            dslContext = dslContext,
            redisOperation = redisOperation,
            streamBridge = streamBridge
        )
    }

    @Autowired
    private lateinit var triggerSvnService: TriggerSvnService
    /**
     *  轮询间隔时间 单位: 秒
     */
    @Value("\${scm.svn.interval:180}")
    private var interval: Long = MIN_POLL_INTERVAL

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        val taskScheduler = ThreadPoolTaskScheduler()
        taskScheduler.poolSize = POOL_SIZE
        taskScheduler.initialize()
        taskRegistrar.setTaskScheduler(taskScheduler)
        taskRegistrar.addTriggerTask(
            {
                if (interval > 0)
                    triggerSvnService.start(checkoutInterval())
            },
            { triggerContext ->
                val nextExecutionTime: Calendar = Calendar.getInstance()
                val lastActualExecutionTime: Date = triggerContext.lastActualExecutionTime()
                    ?: Date()
                nextExecutionTime.time = lastActualExecutionTime
                if (interval > 0) {
                    nextExecutionTime.add(
                        Calendar.SECOND,
                        interval.toInt()
                    )
                } else {
                    logger.info("CI_REPOSITORY_POLLING_SVN_INTERVAL < 0, check after 5 minutes")
                    // 如果间隔时间设为负数, 关闭轮询, 就每5分钟检查是不是打开了
                    nextExecutionTime.add(Calendar.MINUTE, INTERVAL_MINUTE_5)
                }
                nextExecutionTime.time.toInstant()
            }
        )
    }

    private fun checkoutInterval(): Long {
        if (interval < MIN_POLL_INTERVAL) return MIN_POLL_INTERVAL
        return interval
    }

    companion object {
        const val MIN_POLL_INTERVAL = 60L
        const val POOL_SIZE = 10
        const val INTERVAL_MINUTE_5 = 5
    }

    private val logger = LoggerFactory.getLogger(PollSvnConfig::class.qualifiedName)
}
