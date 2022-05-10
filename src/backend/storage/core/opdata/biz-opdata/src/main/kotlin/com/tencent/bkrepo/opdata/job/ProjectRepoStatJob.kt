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
import com.tencent.bkrepo.opdata.config.InfluxDbConfig
import com.tencent.bkrepo.opdata.model.NodeModel
import com.tencent.bkrepo.opdata.model.ProjectModel
import com.tencent.bkrepo.opdata.model.RepoModel
import com.tencent.bkrepo.opdata.model.TProjectMetrics
import com.tencent.bkrepo.opdata.pojo.RepoMetrics
import com.tencent.bkrepo.opdata.repository.ProjectMetricsRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.influxdb.dto.BatchPoints
import org.influxdb.dto.Point
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * stat bkrepo running status
 */
@Component
class ProjectRepoStatJob(
    private val nodeModel: NodeModel,
    private val projectModel: ProjectModel,
    private val repoModel: RepoModel,
    private val influxDbConfig: InfluxDbConfig,
    private val projectMetricsRepository: ProjectMetricsRepository
) {

    @Scheduled(cron = "00 00 */1 * * ?")
    @SchedulerLock(name = "ProjectRepoStatJob", lockAtMostFor = "PT10H")
    fun statProjectRepoSize() {
        logger.info("start to stat project metrics")
        val influxDb = influxDbConfig.influxDbUtils().getInstance() ?: run {
            logger.error("init influxdb fail")
            return
        }
        val timeMillis = System.currentTimeMillis()
        val batchPoints = BatchPoints
            .database(influxDbConfig.database)
            .build()
        val projects = projectModel.getProjectList()
        val projectMetrics = mutableListOf<TProjectMetrics>()
        projects.forEach { it ->
            val projectId = it.name
            val repos = repoModel.getRepoListByProjectId(it.name)
            val table = TABLE_PREFIX + (projectId.hashCode() and 255).toString()
            var repoCapSize = 0L
            var repoNodeNum = 0L
            val repoMetrics = mutableListOf<RepoMetrics>()
            repos.forEach {
                val repoName = it.name
                val node = nodeModel.getNodeSize(projectId, repoName)

                // 有效仓库的统计数据
                if (node.size != 0L && node.num != 0L) {
                    logger.info("project : [$projectId],repo: [$repoName],size:[$node]")
                    val point = Point.measurement(INFLUX_COLLECION)
                        .time(timeMillis, TimeUnit.MILLISECONDS)
                        .addField("size", node.size / TOGIGABYTE)
                        .addField("num", node.num)
                        .tag("projectId", projectId)
                        .tag("repoName", repoName)
                        .tag("table", table)
                        .build()
                    repoCapSize += node.size
                    repoNodeNum += node.num
                    batchPoints.point(point)
                    repoMetrics.add(
                        RepoMetrics(repoName, it.credentialsKey, node.size / TOGIGABYTE, node.num)
                    )
                }
            }
            // 有效项目的统计数据
            if (repoNodeNum != 0L && repoCapSize != 0L) {
                val metrics = TProjectMetrics(projectId, repoNodeNum, repoCapSize / TOGIGABYTE, repoMetrics)
                projectMetrics.add(metrics)
            }
        }

        // 数据写入 influxdb
        logger.info("start to insert influxdb metrics ")
        influxDb.write(batchPoints)
        influxDb.close()

        // 数据写入mongodb统计表
        projectMetricsRepository.deleteAll()
        logger.info("start to insert  mongodb metrics ")
        projectMetricsRepository.insert(projectMetrics)
        logger.info("stat project metrics done")
    }

    companion object {
        private val logger = LoggerHolder.jobLogger
        private const val INFLUX_COLLECION = "repoInfo"
        private const val TABLE_PREFIX = "node_"
        private const val TOGIGABYTE = 1024 * 1024 * 1024
    }
}
