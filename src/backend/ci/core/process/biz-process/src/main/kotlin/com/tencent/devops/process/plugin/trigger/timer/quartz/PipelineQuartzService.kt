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

package com.tencent.devops.process.plugin.trigger.timer.quartz

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.BkApiUtil
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.plugin.trigger.lock.PipelineTimerTriggerLock
import com.tencent.devops.process.plugin.trigger.pojo.event.PipelineTimerBuildEvent
import com.tencent.devops.process.plugin.trigger.service.PipelineTimerService
import com.tencent.devops.process.plugin.trigger.timer.SchedulerManager
import com.tencent.devops.process.plugin.trigger.timer.quartz.PipelineQuartzJob.Companion.JOB_DATA_MAP_KEY_MD5
import com.tencent.devops.process.plugin.trigger.timer.quartz.PipelineQuartzJob.Companion.JOB_DATA_MAP_KEY_PIPELINE_ID
import com.tencent.devops.process.plugin.trigger.timer.quartz.PipelineQuartzJob.Companion.JOB_DATA_MAP_KEY_PROJECT_ID
import com.tencent.devops.process.plugin.trigger.timer.quartz.PipelineQuartzJob.Companion.JOB_DATA_MAP_KEY_TASK_ID
import com.tencent.devops.project.api.service.ServiceProjectTagResource
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
import jakarta.annotation.PreDestroy
import org.apache.commons.lang3.StringUtils

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

    @Value("\${timer.execute:#{null}}")
    private val timeExecute: String? = null

    companion object {
        private val jobBeanClass = PipelineQuartzJob::class.java
        private val init = AtomicBoolean(false)
    }

    @Suppress("ALL")
    @Scheduled(initialDelay = 20000, fixedDelay = 3000000)
    fun reloadTimer() {
        // 通过配置决定对应的环境是否执行定时任务
        if (!timeExecute.isNullOrEmpty()) {
            logger.info("env can not execute timer plugin")
            return
        }

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
                    addJob(
                        projectId = timer.projectId,
                        pipelineId = timer.pipelineId,
                        crontab = crontab,
                        taskId = timer.taskId
                    )
                }
            }
            start += limit
        }

        logger.warn("TIMER_RELOAD| reload ok!")
    }

    fun addJob(projectId: String, pipelineId: String, crontab: String, taskId: String) {
        try {
            val md5 = DigestUtils.md5Hex(crontab)
            val comboKey = if (taskId.isBlank()) {
                "${pipelineId}_${md5}_$projectId"
            } else {
                "${pipelineId}_${md5}_${projectId}_$taskId"
            }
            // 移除旧的定时任务key
            if (schedulerManager.checkExists(comboKey)) {
                schedulerManager.deleteJob(comboKey)
            }
            schedulerManager.addJob(
                key = comboKey,
                cronExpression = crontab,
                jobBeanClass = jobBeanClass,
                projectId = projectId,
                pipelineId = pipelineId,
                taskId = taskId,
                md5 = md5
            )
        } catch (ignore: Exception) {
            logger.error("TIMER_RELOAD| add job error|pipelineId=$pipelineId|crontab=$crontab", ignore)
        }
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
    companion object {
        const val JOB_DATA_MAP_KEY_PROJECT_ID = "projectId"
        const val JOB_DATA_MAP_KEY_PIPELINE_ID = "pipelineId"
        const val JOB_DATA_MAP_KEY_TASK_ID = "taskId"
        const val JOB_DATA_MAP_KEY_MD5 = "md5"
    }

    override fun execute(context: JobExecutionContext?) {
        SpringContextUtil.getBean(PipelineJobBean::class.java).execute(context)
    }
}

class PipelineJobBean(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val schedulerManager: SchedulerManager,
    private val pipelineTimerService: PipelineTimerService,
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val pipelineRepositoryService: PipelineRepositoryService
) {

    private val logger = LoggerFactory.getLogger(javaClass)!!

    @Suppress("ALL")
    fun execute(context: JobExecutionContext?) {
        val jobKey = context?.jobDetail?.key ?: return
        val comboKey = jobKey.name
        val jobDataMap = context.jobDetail.jobDataMap ?: return
        if (jobDataMap.isEmpty()) {
            logger.warn("PIPELINE_TIMER_INVALID_DATA|jobDataMap is empty|comboKey=$comboKey")
            return
        }
        // 格式：pipelineId_{md5}_{projectId}_{taskId}
        val pipelineId = jobDataMap.getString(JOB_DATA_MAP_KEY_PIPELINE_ID)
        val crontabMd5 = jobDataMap.getString(JOB_DATA_MAP_KEY_MD5)
        val projectId = jobDataMap.getString(JOB_DATA_MAP_KEY_PROJECT_ID)
        val taskId = jobDataMap.getString(JOB_DATA_MAP_KEY_TASK_ID) ?: ""
        if (StringUtils.isAnyEmpty(pipelineId, projectId, crontabMd5)) {
            // 缺少关键字段
            logger.warn("PIPELINE_TIMER_INVALID_DATA|miss require param|$projectId|$pipelineId|$taskId|$crontabMd5")
            return
        }
        val watcher = Watcher(id = "timer|[$comboKey]")
        try {
            if (redisOperation.isMember(BkApiUtil.getApiAccessLimitPipelinesKey(), pipelineId)) {
                logger.warn("Pipeline[$pipelineId] has restricted build permissions,please try again later!")
                return
            }
            if (redisOperation.isMember(BkApiUtil.getApiAccessLimitProjectsKey(), projectId)) {
                logger.warn("Project[$projectId] has restricted build permissions,please try again later!")
                return
            }
            val pipelineTimer = pipelineTimerService.get(projectId, pipelineId, taskId)
            if (null == pipelineTimer) {
                logger.info("[$comboKey]|PIPELINE_TIMER_EXPIRED|Timer is expire, delete it from queue!")
                schedulerManager.deleteJob(comboKey)
                return
            }

            watcher.start("projectRouterTagCheck")
            val projectRouterTagCheck = client.get(ServiceProjectTagResource::class)
                .checkProjectRouter(pipelineTimer.projectId).data ?: return
            if (!projectRouterTagCheck) {
                logger.warn("timePipeline ${pipelineTimer.projectId} router tag is not this cluster")
                return
            }

            var find = false
            pipelineTimer.crontabExpressions.forEach {
                if (DigestUtils.md5Hex(it) == crontabMd5) {
                    find = true
                }
            }
            if (!find) {
                logger.info("[$pipelineId]|PIPELINE_TIMER_EXPIRED|can not find crontab, delete it from queue!")
                schedulerManager.deleteJob(comboKey)
                return
            }

            val scheduledFireTime = DateFormatUtils.format(context.scheduledFireTime, "yyyyMMddHHmmss")
            // 相同触发的要锁定，防止误差导致重复执行
            watcher.start("redisLock")
            val pipelineLockKey = "$pipelineId:$taskId"
            val redisLock = PipelineTimerTriggerLock(redisOperation, pipelineLockKey, scheduledFireTime)
            if (redisLock.tryLock()) {
                try {
                    logger.info("[$projectId]|$pipelineLockKey|PIPELINE_TIMER|scheduledFireTime=$scheduledFireTime")
                    watcher.start("dispatch")
                    pipelineEventDispatcher.dispatch(
                        PipelineTimerBuildEvent(
                            source = "timer_trigger",
                            projectId = pipelineTimer.projectId,
                            pipelineId = pipelineId,
                            userId = pipelineRepositoryService.getPipelineOauthUser(
                                projectId = projectId,
                                pipelineId = pipelineId
                            ) ?: pipelineTimer.startUser,
                            channelCode = pipelineTimer.channelCode,
                            taskId = taskId,
                            startParam = pipelineTimer.startParam,
                            expectedStartTime = context.scheduledFireTime.time
                        )
                    )
                } catch (ignored: Exception) {
                    logger.error(
                        "[$pipelineLockKey]|PIPELINE_TIMER|scheduledFireTime=$scheduledFireTime|Dispatch event fail",
                        ignored
                    )
                }
            } else {
                logger.info(
                    "[$pipelineLockKey]|PIPELINE_TIMER_CONCURRENT|scheduledFireTime=$scheduledFireTime" +
                            "|lock fail, skip!"
                )
            }
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher = watcher, warnThreshold = 50)
        }
    }
}
