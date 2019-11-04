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

package com.tencent.devops.process.plugin.trigger.timer.quartz

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.plugin.trigger.pojo.event.PipelineTimerBuildEvent
import com.tencent.devops.process.plugin.trigger.service.PipelineTimerService
import com.tencent.devops.process.plugin.trigger.timer.SchedulerManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang.time.DateFormatUtils
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PreDestroy

/**
 * 调度服务框架
 * @version 1.0
 */
@Service
class PipelineQuartzService @Autowired constructor(
    private val pipelineTimerService: PipelineTimerService,
    private val schedulerManager: SchedulerManager
) {
    private val logger = LoggerFactory.getLogger(javaClass)!!

    companion object {
        private val jobBeanClass = PipelineQuartzJob::class.java
        private val init = AtomicBoolean(false)
    }

    @Scheduled(initialDelay = 20000, fixedDelay = 3000000)
    fun reloadTimer() {
        logger.info("TIMER_RELOAD| start add timer pipeline to quartz queue!")
        var start = 0
        val limit = 200
        loop@ while (true) {
            val resp = pipelineTimerService.list(start, limit)
            val list = resp.data
            if (list == null || list.isEmpty()) {
                logger.info("list timer pipeline finish|start=$start|limit=$limit|resp=$resp")
                break@loop
            }
            list.forEach { timer ->
                logger.info("TIMER_RELOAD| load crontab($timer)")
                timer.crontabExpressions.forEach { crontab ->
                    val md5 = DigestUtils.md5Hex(crontab)
                    val comboKey = "${timer.pipelineId}_$md5"
                    schedulerManager.addJob(
                        comboKey, crontab,
                        jobBeanClass
                    )
                }
            }
            start += limit
        }

        logger.warn("TIMER_RELOAD| reload ok!")
    }

    @PreDestroy
    fun stop() {
        if (init.get()) {
            schedulerManager.shutdown()
            init.set(false)
            logger.warn("STOP| timer quartz have been stop!")
        } else {
            logger.warn("STOP| do not init!")
        }
    }
}

class PipelineQuartzJob : Job {
    override fun execute(context: JobExecutionContext?) {
        SpringContextUtil.getBean(PipelineJobBean::class.java).execute(context)
    }
}

class PipelineJobBean(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val schedulerManager: SchedulerManager,
    private val pipelineTimerService: PipelineTimerService,
    private val redisOperation: RedisOperation
) {

    private val logger = LoggerFactory.getLogger(javaClass)!!

    fun execute(context: JobExecutionContext?) {
        val jobKey = context?.jobDetail?.key ?: return
        val comboKey = jobKey.name
        val comboKeys = comboKey.split("_")
        val pipelineId = comboKeys[0]
        val crontabMd5 = comboKeys[1]
        val pipelineTimer = pipelineTimerService.get(pipelineId)
        if (null == pipelineTimer) {
            logger.info("[$comboKey]|PIPELINE_TIMER_EXPIRED|Timer is expire, delete it from queue!")
            schedulerManager.deleteJob(comboKey)
            return
        }

        var find = false
        pipelineTimer.crontabExpressions.forEach {
            if (DigestUtils.md5Hex(it) == crontabMd5) {
                find = true
            }
        }
        if (!find) {
            logger.info("[$comboKey]|PIPELINE_TIMER_EXPIRED|can not find crontab, delete it from queue!")
            schedulerManager.deleteJob(comboKey)
            return
        }
        val scheduledFireTime = DateFormatUtils.format(context.scheduledFireTime, "yyyyMMddHHmmss")
        // 相同触发的要锁定，防止误差导致重复执行
        val redisLock = RedisLock(redisOperation, "process:pipeline:timer:trigger:$pipelineId:$scheduledFireTime", 58)
        if (redisLock.tryLock()) {
            try {
                logger.info("[$comboKey]|PIPELINE_TIMER|scheduledFireTime=$scheduledFireTime")
                pipelineEventDispatcher.dispatch(
                    PipelineTimerBuildEvent(
                        "timer_trigger", pipelineTimer.projectId, pipelineId, pipelineTimer.startUser,
                        pipelineTimer.channelCode
                    )
                )
            } catch (e: Exception) {
                logger.error(
                    "[$comboKey]|PIPELINE_TIMER|scheduledFireTime=$scheduledFireTime| Dispatch event fail, e=$e",
                    e
                )
            }
        } else {
            logger.info("[$comboKey]|PIPELINE_TIMER_CONCURRENT|scheduledFireTime=$scheduledFireTime| Timer have been trigger by other, skip!")
        }
    }
}
