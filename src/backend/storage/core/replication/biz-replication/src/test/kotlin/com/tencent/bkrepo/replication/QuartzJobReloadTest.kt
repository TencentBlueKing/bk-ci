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

import com.tencent.bkrepo.replication.replica.schedule.ReplicaTaskScheduler
import com.tencent.bkrepo.replication.replica.schedule.ReplicaTaskScheduler.Companion.REPLICA_JOB_GROUP
import org.junit.jupiter.api.Test
import org.quartz.InterruptableJob
import org.quartz.JobBuilder
import org.quartz.JobDetail
import org.quartz.JobExecutionContext
import org.quartz.Scheduler
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.LoggerFactory
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

internal class QuartzJobReloadTest {

    @Test
    fun test() {
        thread {
            // 添加任务
            sleep(5)
            taskMap["1"] = "new"
            sleep(5)
            taskMap["2"] = "new"
            taskMap.remove("1")
            sleep(5)
            taskMap["3"] = "new"
            sleep(5)
            taskMap["4"] = "new"
        }

        repeat(1) {
            thread {
                val scheduler = createScheduler("scheduler$it")
                val scheduleService = ReplicaTaskScheduler(scheduler)
                // 加载任务
                while (true) {
                    logger.info("Start to reload task")
                    val taskIdList = listUndoFullTask()
                    val jobKeyList = scheduleService.listJobKeys().map { it.name }
                    val expiredTaskId = jobKeyList subtract taskIdList

                    var newTaskCount = 0
                    var expiredTaskCount = 0
                    val totalCount = taskIdList.size

                    // 移除过期job
                    expiredTaskId.forEach {
                        scheduleService.deleteJob(it)
                        expiredTaskCount += 1
                    }

                    // 创建新job
                    taskIdList.forEach { id ->
                        if (!scheduleService.exist(id)) {
                            val jobDetail = createJobDetail(id)
                            val trigger = createTrigger(id)
                            scheduleService.scheduleJob(jobDetail, trigger)
                            newTaskCount += 1
                        }
                    }
                    logger.info(
                        "Success to reload replication task, " +
                            "total: $totalCount, new: $newTaskCount, expired: $expiredTaskCount"
                    )
                    sleep(3)
                }
            }
        }

        sleep(60)
    }

    private fun createJobDetail(id: String): JobDetail {
        return JobBuilder.newJob(HelloJob::class.java)
            .withIdentity(id, REPLICA_JOB_GROUP)
            .usingJobData("id", id)
            .requestRecovery()
            .build()
    }

    private fun createTrigger(id: String): Trigger {
        return TriggerBuilder.newTrigger()
            .withIdentity(id, REPLICA_JOB_GROUP)
            .startNow()
            .build()
    }

    private fun listUndoFullTask(): List<String> {
        return taskMap.filter {
            it.value != "finished"
        }.keys.toList()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(QuartzJobReloadTest::class.java)
        private val taskMap = ConcurrentHashMap<String, String>()
        private fun sleep(seconds: Int) {
            Thread.sleep(seconds * 1000L)
        }

        private fun createScheduler(name: String): Scheduler {
            val stdSchedulerFactory = StdSchedulerFactory()
            val props = Properties()
            props["org.quartz.scheduler.instanceName"] = name
            props["org.quartz.threadPool.threadCount"] = "10"
            stdSchedulerFactory.initialize(props)
            return stdSchedulerFactory.scheduler.apply { start() }
        }
    }

    class HelloJob : InterruptableJob {

        private lateinit var currentThread: Thread

        override fun interrupt() {
            println("interrupt")
            currentThread.interrupt()
        }

        override fun execute(context: JobExecutionContext) {
            currentThread = Thread.currentThread()
            val id = context.jobDetail.jobDataMap.getString("id")
            logger.info("job[$id] start")
            taskMap[id] = "finished"
            logger.info("job[$id] end")
        }
    }
}
