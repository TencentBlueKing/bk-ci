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

package com.tencent.bkrepo.opdata.job

import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.opdata.config.InfluxDbConfig
import com.tencent.bkrepo.opdata.model.NodeModel
import com.tencent.bkrepo.opdata.model.ProjectModel
import com.tencent.bkrepo.opdata.model.RepoModel
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.influxdb.dto.BatchPoints
import org.influxdb.dto.Point
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * stat bkrepo running status
 */
@Component
class ProjectRepoStatJob {

    @Autowired
    private lateinit var nodeModel: NodeModel

    @Autowired
    private lateinit var projectModel: ProjectModel

    @Autowired
    private lateinit var repoModel: RepoModel

    @Autowired
    private lateinit var influxDbConfig: InfluxDbConfig

    @Scheduled(cron = "00 00 */1 * * ?")
    @SchedulerLock(name = "ProjectRepoStatJob", lockAtMostFor = "PT1H")
    fun statProjectRepoSize() {
        logger.info("start to stat project metrics")
        val inluxdDb = influxDbConfig.influxDbUtils().getInstance() ?: run {
            logger.error("init influxdb fail")
            return
        }
        val timeMillis = System.currentTimeMillis()
        val batchPoints = BatchPoints
            .database(influxDbConfig.database)
            .build()
        val projects = projectModel.getProjectList()
        projects.forEach {
            val projectId = it.name
            val repos = repoModel.getRepoListByProjectId(it.name)
            val table = TABLE_PREFIX + (projectId.hashCode() and 255).toString()
            repos.forEach {
                val repoName = it
                val result = nodeModel.getNodeSize(projectId, repoName)
                if (result.size != 0L && result.num != 0L) {
                    logger.info("project : [$projectId],repo: [$repoName],size:[$result]")
                    val point = Point.measurement(INFLUX_COLLECION)
                        .time(timeMillis, TimeUnit.MILLISECONDS)
                        .addField("size", result.size / (1024 * 1024 * 1024))
                        .addField("num", result.num)
                        .tag("projectId", projectId)
                        .tag("repoName", repoName)
                        .tag("table", table)
                        .build()
                    batchPoints.point(point)
                }
            }
        }
        inluxdDb.write(batchPoints)
        inluxdDb.close()
    }

    companion object {
        private val logger = LoggerHolder.jobLogger
        private const val INFLUX_COLLECION = "repoInfo"
        private const val TABLE_PREFIX = "node_"
    }
}
