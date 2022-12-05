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

package com.tencent.bkrepo.opdata.handler.impl

import com.tencent.bkrepo.opdata.constant.OPDATA_CUSTOM
import com.tencent.bkrepo.opdata.constant.OPDATA_CUSTOM_NUM
import com.tencent.bkrepo.opdata.constant.OPDATA_CUSTOM_SIZE
import com.tencent.bkrepo.opdata.constant.OPDATA_GRAFANA_NUMBER
import com.tencent.bkrepo.opdata.constant.OPDATA_GRAFANA_STRING
import com.tencent.bkrepo.opdata.constant.OPDATA_PIPELINE
import com.tencent.bkrepo.opdata.constant.OPDATA_PIPELINE_NUM
import com.tencent.bkrepo.opdata.constant.OPDATA_PIPELINE_SIZE
import com.tencent.bkrepo.opdata.handler.QueryHandler
import com.tencent.bkrepo.opdata.model.TProjectMetrics
import com.tencent.bkrepo.opdata.pojo.Columns
import com.tencent.bkrepo.opdata.pojo.QueryResult
import com.tencent.bkrepo.opdata.pojo.Target
import com.tencent.bkrepo.opdata.pojo.enums.Metrics
import com.tencent.bkrepo.opdata.repository.ProjectMetricsRepository
import org.springframework.stereotype.Component

/**
 * 项目统计信息列表
 */
@Component
class ProjectListHandler(
    private val projectMetricsRepository: ProjectMetricsRepository
) : QueryHandler {

    override val metric: Metrics get() = Metrics.PROJECTLIST

    override fun handle(target: Target, result: MutableList<Any>) {
        val rows = mutableListOf<List<Any>>()
        val columns = mutableListOf<Columns>()
        val info = projectMetricsRepository.findAll()
        columns.add(Columns(TProjectMetrics::projectId.name, OPDATA_GRAFANA_STRING))
        columns.add(Columns(TProjectMetrics::nodeNum.name, OPDATA_GRAFANA_NUMBER))
        columns.add(Columns(TProjectMetrics::capSize.name, OPDATA_GRAFANA_NUMBER))
        columns.add(Columns(OPDATA_CUSTOM_NUM, OPDATA_GRAFANA_NUMBER))
        columns.add(Columns(OPDATA_CUSTOM_SIZE, OPDATA_GRAFANA_NUMBER))
        columns.add(Columns(OPDATA_PIPELINE_NUM, OPDATA_GRAFANA_NUMBER))
        columns.add(Columns(OPDATA_PIPELINE_SIZE, OPDATA_GRAFANA_NUMBER))
        info.forEach { repo ->
            var customNum = 0L
            var customSize = 0L
            var pipelineNum = 0L
            var pipelineSize = 0L
            repo.repoMetrics.forEach {
                if (it.repoName == OPDATA_CUSTOM) {
                    customNum = it.num
                    customSize = it.size
                }
                if (it.repoName == OPDATA_PIPELINE) {
                    pipelineNum = it.num
                    pipelineSize = it.size
                }
            }
            val row = listOf(
                repo.projectId, repo.nodeNum, repo.capSize,
                customNum, customSize, pipelineNum, pipelineSize
            )
            rows.add(row)
        }
        val data = QueryResult(columns, rows, target.type)
        result.add(data)
    }
}
