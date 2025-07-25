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

import com.tencent.devops.process.plugin.trigger.timer.SchedulerManager
import org.quartz.Scheduler
import org.quartz.impl.StdSchedulerFactory
import org.springframework.boot.autoconfigure.quartz.QuartzProperties
import java.util.Properties

class QuartzSchedulerManager(
    private val quartzProperties: QuartzProperties,
    private val quartzTraceJobListener: QuartzTraceJobListener
) : SchedulerManager() {

    private var scheduler: Scheduler = initSchedulerFactory().scheduler

    private val triggerGroup = "bkTriggerGroup"

    private val jobGroup = "bkJobGroup"

    init {
        scheduler.listenerManager.addJobListener(quartzTraceJobListener)
        scheduler.start()
    }

    override fun getJobGroup(): String {
        return jobGroup
    }

    override fun getTriggerGroup(): String {
        return triggerGroup
    }

    override fun getScheduler(): Scheduler {
        return scheduler
    }

    private fun initSchedulerFactory(): StdSchedulerFactory {
        val properties = Properties()
        properties.putAll(quartzProperties.properties)
        if (properties[PROP_THREAD_COUNT] == null) {
            properties.setProperty(PROP_THREAD_COUNT, DEFAULT_THREAD_COUNT.toString())
        }
        val stdSchedulerFactory = StdSchedulerFactory()
        stdSchedulerFactory.initialize(properties)
        return stdSchedulerFactory
    }

    companion object {
        private const val PROP_THREAD_COUNT = "org.quartz.threadPool.threadCount"
        private const val DEFAULT_THREAD_COUNT = 10
    }
}
