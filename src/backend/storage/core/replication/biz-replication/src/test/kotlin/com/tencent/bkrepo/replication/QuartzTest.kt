/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.replication

import com.tencent.bkrepo.common.api.constant.StringPool.uniqueId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobBuilder
import org.quartz.JobExecutionContext
import org.quartz.JobKey
import org.quartz.TriggerBuilder
import org.quartz.TriggerKey
import org.quartz.impl.StdSchedulerFactory
import org.quartz.impl.matchers.GroupMatcher
import java.util.Date

internal class QuartzTest {

    @Test
    @DisplayName("SimpleTrigger执行完后，job和trigger会被删除")
    fun testSimpleTrigger() {
        val scheduler = StdSchedulerFactory.getDefaultScheduler()
        scheduler.start()

        val jobKey = JobKey(uniqueId())
        val triggerKey = TriggerKey(uniqueId())

        val jobDetail = JobBuilder.newJob(SayHelloJob::class.java)
            .withIdentity(jobKey)
            .build()

        val startAt = Date(System.currentTimeMillis() + 2 * 1000)
        val trigger = TriggerBuilder.newTrigger()
            .withIdentity(triggerKey)
            .startAt(startAt)
            .build()
        scheduler.scheduleJob(jobDetail, trigger)

        Thread.sleep(10 * 1000)
        println(scheduler.getJobKeys(GroupMatcher.anyGroup()).size)
        println(scheduler.getTriggersOfJob(jobKey).size)

        Thread.sleep(20 * 1000)
        scheduler.shutdown(true)
    }

    @Test
    @DisplayName("CronTrigger执行完后，job和trigger会保留")
    fun testCronTrigger() {
        val scheduler = StdSchedulerFactory.getDefaultScheduler()
        scheduler.start()

        val jobKey = JobKey(uniqueId())
        val triggerKey = TriggerKey(uniqueId())

        val jobDetail = JobBuilder.newJob(SayHelloJob::class.java)
            .withIdentity(jobKey)
            .build()

        val cronExpression = "0/5 * * * * ?"
        val trigger = TriggerBuilder.newTrigger()
            .withIdentity(triggerKey)
            .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
            .build()
        scheduler.scheduleJob(jobDetail, trigger)

        Thread.sleep(6 * 1000)
        println(scheduler.getJobKeys(GroupMatcher.anyGroup()).size)
        println(scheduler.getTriggersOfJob(jobKey).size)

        scheduler.unscheduleJob(triggerKey)
        Thread.sleep(5 * 1000)
        println(scheduler.getJobKeys(GroupMatcher.anyGroup()).size)
        println(scheduler.getTriggersOfJob(jobKey).size)

        Thread.sleep(10 * 1000)
        scheduler.shutdown(true)
    }

    @Test
    @DisplayName("删除不存在的job，执行成功不会异常")
    fun testDeleteNonExistJob() {
        val scheduler = StdSchedulerFactory.getDefaultScheduler()
        scheduler.start()

        val jobKey = JobKey(uniqueId())
        scheduler.deleteJob(jobKey)

        Thread.sleep(5 * 1000)
        scheduler.shutdown(true)
    }

    class SayHelloJob : Job {
        override fun execute(context: JobExecutionContext) {
            println("Hello: " + Thread.currentThread().name)
        }
    }
}
