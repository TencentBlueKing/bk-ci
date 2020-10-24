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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.replication.service

import com.tencent.bkrepo.replication.constant.DEFAULT_GROUP_ID
import org.quartz.JobDetail
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.Trigger
import org.quartz.UnableToInterruptJobException
import org.quartz.impl.matchers.GroupMatcher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ScheduleService(
    private val scheduler: Scheduler
) {

    fun scheduleJob(jobDetail: JobDetail, trigger: Trigger) {
        try {
            scheduler.scheduleJob(jobDetail, trigger)
            logger.info("Success to schedule job[${jobDetail.key}]")
        } catch (exception: SchedulerException) {
            logger.error("Failed to schedule job[${jobDetail.key}]", exception)
        }
    }

    fun listJobKeys(): Set<JobKey> {
        return scheduler.getJobKeys(GroupMatcher.jobGroupEquals(DEFAULT_GROUP_ID))
    }

    fun interruptJob(id: String) {
        val jobKey = JobKey.jobKey(id, DEFAULT_GROUP_ID)
        try {
            scheduler.interrupt(jobKey)
            logger.info("Success to interrupt job[$jobKey]")
        } catch (exception: UnableToInterruptJobException) {
            logger.error("Failed to interrupt job[$id]", exception)
        }
    }

    fun deleteJob(id: String) {
        val jobKey = JobKey.jobKey(id, DEFAULT_GROUP_ID)
        try {
            interruptJob(id)
            scheduler.deleteJob(jobKey)
            logger.info("Success to delete job[$jobKey]")
        } catch (exception: SchedulerException) {
            logger.error("Failed to delete job[$id]", exception)
        }
    }

    fun checkExists(id: String): Boolean {
        return try {
            scheduler.checkExists(JobKey.jobKey(id, DEFAULT_GROUP_ID))
        } catch (exception: SchedulerException) {
            logger.error("Failed to check exist job[$id].", exception)
            false
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScheduleService::class.java)
    }
}
