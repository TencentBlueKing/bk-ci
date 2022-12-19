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

package com.tencent.devops.stream.trigger.timer.quartz

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.project.api.service.ServiceProjectTagResource
import com.tencent.devops.stream.trigger.timer.SchedulerManager
import com.tencent.devops.stream.trigger.timer.pojo.event.StreamTimerBuildEvent
import com.tencent.devops.stream.trigger.timer.service.StreamTimerService
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PreDestroy

/**
 * 调度服务框架
 * @version 1.0
 */
@Service
class StreamQuartzService @Autowired constructor(
    private val streamTimerService: StreamTimerService,
    private val schedulerManager: SchedulerManager
) {
    private val logger = LoggerFactory.getLogger(javaClass)!!

    @Value("\${timer.execute:#{null}}")
    private val timeExecute: String? = null

    companion object {
        private val jobBeanClass = StreamQuartzJob::class.java
        private val init = AtomicBoolean(false)
    }

    @Suppress("ALL")
    @Scheduled(initialDelay = 20000, fixedDelay = 3000000)
    fun reloadTimer() {
        // 通过配置决定对应的环境是否执行定时任务
        if (!timeExecute.isNullOrEmpty()) {
            logger.info("StreamQuartzService|reloadTimer|env can not execute timer plugin")
            return
        }

        logger.info("StreamQuartzService|reloadTimer|start add timer pipeline to quartz queue!")
        var start = 0
        val limit = 200
        loop@ while (true) {
            val resp = streamTimerService.list(start, limit)
            val list = resp.data
            if (list == null || list.isEmpty()) {
                logger.info(
                    "StreamQuartzService|reloadTimer" +
                        "|list timer pipeline finish|start=$start|limit=$limit|resp=$resp"
                )
                break@loop
            }
            list.forEach { timer ->
                logger.info("StreamQuartzService|reloadTimer|TIMER_RELOAD|crontab|$timer")
                timer.crontabExpressions.forEach { crontab ->
                    addJob(projectId = timer.projectId, pipelineId = timer.pipelineId, crontab = crontab)
                }
            }
            start += limit
        }

        logger.warn("StreamQuartzService|reloadTimer|TIMER_RELOAD|ok")
    }

    fun addJob(projectId: String, pipelineId: String, crontab: String) {
        try {
            val md5 = DigestUtils.md5Hex(crontab)
            val comboKey = "${pipelineId}_${md5}_$projectId"
            schedulerManager.addJob(
                comboKey, crontab,
                jobBeanClass
            )
        } catch (ignore: Exception) {
            logger.warn("StreamQuartzService|addJob|error|pipelineId=$pipelineId|crontab=$crontab", ignore)
        }
    }

    @PreDestroy
    fun stop() {
        if (init.get()) {
            schedulerManager.shutdown()
            init.set(false)
            logger.warn("StreamQuartzService|STOP| timer quartz have been stop!")
        } else {
            logger.warn("StreamQuartzService|STOP| do not init!")
        }
    }
}

class StreamQuartzJob : Job {
    override fun execute(context: JobExecutionContext?) {
        SpringContextUtil.getBean(StreamJobBean::class.java).execute(context)
    }
}

class StreamJobBean(
    private val streamEventDispatcher: PipelineEventDispatcher,
    private val schedulerManager: SchedulerManager,
    private val streamTimerService: StreamTimerService,
    private val redisOperation: RedisOperation,
    private val client: Client
) {

    private val logger = LoggerFactory.getLogger(javaClass)!!

    @Suppress("ALL")
    fun execute(context: JobExecutionContext?) {
        val jobKey = context?.jobDetail?.key ?: return
        val comboKey = jobKey.name
        val comboKeys = comboKey.split("_")
        val pipelineId = comboKeys[0]
        val crontabMd5 = comboKeys[1]
        val streamTimer = streamTimerService.get(pipelineId)
        if (null == streamTimer) {
            logger.info(
                "StreamQuartzService|execute|[$comboKey]" +
                    "|PIPELINE_TIMER_EXPIRED|Timer is expire, delete it from queue!"
            )
            schedulerManager.deleteJob(comboKey)
            return
        }

        val projectRouterTagCheck = client.get(ServiceProjectTagResource::class)
            .checkProjectRouter(streamTimer.projectId).data ?: return
        if (!projectRouterTagCheck) {
            logger.warn(
                "StreamQuartzService|execute" +
                    "|router tag is not this cluster|timePipeline ${streamTimer.projectId}"
            )
            return
        }

        var find = false
        streamTimer.crontabExpressions.forEach {
            if (DigestUtils.md5Hex(it) == crontabMd5) {
                find = true
            }
        }
        if (!find) {
            logger.info(
                "StreamQuartzService|execute|[$comboKey]" +
                    "|PIPELINE_TIMER_EXPIRED|can not find crontab, delete it from queue!"
            )
            schedulerManager.deleteJob(comboKey)
            return
        }
        val scheduledFireTime = DateFormatUtils.format(context.scheduledFireTime, "yyyyMMddHHmmss")
        // 相同触发的要锁定，防止误差导致重复执行
        val redisLock = RedisLock(redisOperation, "process:pipeline:timer:trigger:$pipelineId:$scheduledFireTime", 58)
        if (redisLock.tryLock()) {
            try {
                logger.info("StreamQuartzService|execute|[$comboKey]|STREAM_TIMER|scheduledFireTime=$scheduledFireTime")
                streamEventDispatcher.dispatch(
                    StreamTimerBuildEvent(
                        source = "timer_trigger",
                        projectId = streamTimer.projectId,
                        pipelineId = pipelineId,
                        userId = streamTimer.userId,
                        gitProjectId = streamTimer.gitProjectId,
                        branchs = streamTimer.branchs,
                        always = streamTimer.always,
                        eventId = streamTimer.eventId,
                        channelCode = streamTimer.channelCode,
                        originYaml = streamTimer.originYaml
                    )
                )
            } catch (ignored: Exception) {
                logger.error(
                    "BKSystemErrorMonitor|StreamQuartzService|execute|$comboKey|$scheduledFireTime|error", ignored
                )
            }
        } else {
            logger.info(
                "StreamQuartzService|execute|[$comboKey]" +
                    "|STREAM_TIMER_CONCURRENT|scheduledFireTime=$scheduledFireTime| lock fail, skip!"
            )
        }
    }
}
