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

package com.tencent.bkrepo.opdata.job

import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.opdata.model.TBkRepoMetrics
import com.tencent.bkrepo.opdata.repository.BkRepoMetricsRepository
import com.tencent.bkrepo.opdata.repository.ProjectMetricsRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.Calendar

/**
 * stat bkrepo running status at
 * 00 45 23 * * ?
 */
@Component
class BkRepoStatJob(
    private val bkRepoMetricsRepository: BkRepoMetricsRepository,
    private val projectMetricsRepository: ProjectMetricsRepository
) {

    @Scheduled(cron = "00 45 23 * * ?")
    @SchedulerLock(name = "BkRepoStatJob", lockAtMostFor = "PT1H")
    fun statBkRepoInfo() {
        logger.info("start to stat bkrepo metrics")
        val projectsMetrics = projectMetricsRepository.findAll()
        var tms = Calendar.getInstance()
        val date = tms.get(Calendar.YEAR).toString() +
            "-" + tms.get(Calendar.MONTH).toString() + "-" + tms.get(Calendar.DAY_OF_MONTH).toString()
        var capSize = 0L
        var nodeNum = 0L
        var projectNum = 0L
        projectsMetrics.forEach {
            projectNum += 1
            capSize += it.capSize
            nodeNum += it.nodeNum
        }
        val data = TBkRepoMetrics(date, projectNum, nodeNum, capSize)
        bkRepoMetricsRepository.insert(data)
    }

    companion object {
        private val logger = LoggerHolder.jobLogger
    }
}
