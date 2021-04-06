/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.replication.service

import com.tencent.bkrepo.replication.constant.DEFAULT_GROUP_ID
import com.tencent.bkrepo.replication.constant.TASK_ID
import com.tencent.bkrepo.replication.job.ReplicationQuartzJob
import com.tencent.bkrepo.replication.model.TReplicationTask
import org.quartz.CronScheduleBuilder
import org.quartz.JobBuilder
import org.quartz.JobDetail
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.util.Date

@Service
class TaskReloadService(
    private val taskService: TaskService,
    private val scheduleService: ScheduleService
) {

    /**
     * 定时从数据库中重新加载任务列表
     * 已存在的任务: 根据id判断是否存在，存在则跳过
     * 新增的任务: 加入到scheduler
     * 修改的任务: 每次修改任务会删除旧任务，因此id会变更，可以当做新任务假如
     * 删除的任务: 当job执行时，判断数据库中是否存在对应的task，如果不存在表示该任务过期了，跳过执行即可
     */
    @Scheduled(initialDelay = 10 * 1000, fixedDelay = 10 * 1000)
    fun reloadTask() {
        val cronTaskList = taskService.listUndoFullTask()
        val taskIdList = cronTaskList.map { it.id!! }
        val jobKeyList = scheduleService.listJobKeys().map { it.name }
        val expiredTaskId = jobKeyList subtract taskIdList

        var newTaskCount = 0
        var expiredTaskCount = 0
        val totalCount = cronTaskList.size

        // 移除过期job
        expiredTaskId.forEach {
            scheduleService.deleteJob(it)
            expiredTaskCount += 1
        }

        // 创建新job
        cronTaskList.forEach {
            if (!scheduleService.checkExists(it.id!!)) {
                val jobDetail = createJobDetail(it)
                val trigger = createTrigger(it)
                scheduleService.scheduleJob(jobDetail, trigger)
                newTaskCount += 1
            }
        }
        if (logger.isDebugEnabled) {
            logger.debug(
                "Success to reload replication task, " +
                    "total: $totalCount, new: $newTaskCount, expired: $expiredTaskCount"
            )
        }
    }

    private fun createJobDetail(task: TReplicationTask): JobDetail {
        return JobBuilder.newJob(ReplicationQuartzJob::class.java)
            .withIdentity(task.id, DEFAULT_GROUP_ID)
            .usingJobData(TASK_ID, task.id)
            .requestRecovery()
            .build()
    }

    private fun createTrigger(task: TReplicationTask): Trigger {
        with(task.setting.executionPlan) {
            return when {
                executeImmediately -> {
                    TriggerBuilder.newTrigger()
                        .withIdentity(task.id, DEFAULT_GROUP_ID)
                        .startNow()
                        .build()
                }
                executeTime != null -> {
                    TriggerBuilder.newTrigger()
                        .withIdentity(task.id, DEFAULT_GROUP_ID)
                        .startAt(Date.from(executeTime!!.atZone(ZoneId.systemDefault()).toInstant()))
                        .build()
                }
                else -> {
                    TriggerBuilder.newTrigger()
                        .withIdentity(task.id, DEFAULT_GROUP_ID)
                        .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                        .build()
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskReloadService::class.java)
    }
}
