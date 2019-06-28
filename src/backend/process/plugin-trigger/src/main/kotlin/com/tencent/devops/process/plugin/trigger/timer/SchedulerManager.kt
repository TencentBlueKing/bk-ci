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

package com.tencent.devops.process.plugin.trigger.timer

import com.tencent.devops.process.plugin.trigger.exception.InvalidTimerException
import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.CronTrigger
import org.quartz.Job
import org.quartz.JobBuilder.newJob
import org.quartz.JobDetail
import org.quartz.JobKey
import org.quartz.ObjectAlreadyExistsException
import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.TriggerBuilder.newTrigger
import org.slf4j.LoggerFactory
import java.util.Date

/**
 * 调度管理封装
 * @version 1.0
 */
abstract class SchedulerManager {

    protected val logger = LoggerFactory.getLogger(javaClass)!!

    /**
     * @param key 标识定时任务唯一主键
     * @param cronExpression 定时表达式
     * @param jobBeanClass 定时业务实现类
     * @return 增加定时任务是否成功
     * @throws InvalidTimerException 定时格式不正确时异常
     */
    @Synchronized
    @Throws(InvalidTimerException::class)
    fun addJob(key: String, cronExpression: String, jobBeanClass: Class<out Job>): Boolean {
        val trigger = newTrigger().withIdentity(key, this.getTriggerGroup()).withSchedule(
            cronSchedule(cronExpression)
        ).build()
        val jobKey = JobKey.jobKey(key, this.getJobGroup())
        val jobDetail = newJob(jobBeanClass).withIdentity(jobKey).build()
        return try {
            val nextFireTime = trigger.getFireTimeAfter(Date()) ?: throw InvalidTimerException()
            logger.info("[$key]|nextFireTime=$nextFireTime")
            getScheduler().scheduleJob(jobDetail, trigger)
            true
        } catch (e: ObjectAlreadyExistsException) {
            resetJob(jobKey, jobDetail, trigger)
        } catch (ignored: Exception) {
            logger.error("SchedulerManager.addJob fail! e:$ignored", ignored)
            try {
                getScheduler().deleteJob(jobKey)
            } catch (ignored: Exception) {
            }
            false
        }
    }

    private fun resetJob(
        jobKey: JobKey?,
        jobDetail: JobDetail?,
        trigger: CronTrigger?
    ): Boolean {
        return try {
            getScheduler().deleteJob(jobKey)
            getScheduler().scheduleJob(jobDetail, trigger)
            true
        } catch (e: Exception) {
            logger.error("SchedulerManager.addJob fail! e:$e", e)
            try {
                getScheduler().deleteJob(jobKey)
            } catch (ignored: Exception) {
            }
            false
        }
    }

    /**
     * 删除一个任务
     *
     * @param crontabId 任务id
     * @return 是否删除成功
     */
    @Synchronized
    fun deleteJob(crontabId: String): Boolean {
        return try {
            getScheduler().deleteJob(JobKey.jobKey(crontabId, this.getTriggerGroup()))
        } catch (e: Exception) {
            logger.error("SchedulerManager.deleteJob fail! e:$e", e)
            false
        }
    }

    /**
     * 检测定时任务是否存在
     *
     * @param crontabId 任务id
     * @return
     */
    @Synchronized
    fun checkExists(crontabId: String): Boolean {
        return try {
            getScheduler().checkExists(JobKey.jobKey(crontabId, this.getJobGroup()))
        } catch (e: SchedulerException) {
            logger.error("SchedulerManager.checkExists fail! e:$e", e)
            false
        }
    }

    /**
     * 关闭
     *
     * @param waitForJobsToComplete
     */
    @Synchronized
    fun shutdown(waitForJobsToComplete: Boolean = true) {
        try {
            getScheduler().shutdown(waitForJobsToComplete)
        } catch (e: Exception) {
            logger.error("SchedulerManager.shutdown fail! e:$e", e)
        }
    }

    // 抽象方法，获取各实现类的trigger_group字符key
    abstract fun getTriggerGroup(): String

    // 抽象方法，获取各实现类的job_group字符key
    abstract fun getJobGroup(): String

    // 抽象方法，获取各实现类的Scheduler
    abstract fun getScheduler(): Scheduler
}
