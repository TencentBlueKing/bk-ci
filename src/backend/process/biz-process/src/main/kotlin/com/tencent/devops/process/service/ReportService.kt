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

package com.tencent.devops.process.service

import com.tencent.devops.artifactory.api.ServiceArtifactoryResource
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.dao.ReportDao
import com.tencent.devops.process.pojo.Report
import com.tencent.devops.process.pojo.report.enums.ReportTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.nio.file.Paths

@Service
class ReportService @Autowired constructor(
    private val dslContext: DSLContext,
    private val reportDao: ReportDao,
    private val client: Client
) {
    private val logger = LoggerFactory.getLogger(ReportService::class.java)

    fun create(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        indexFile: String,
        name: String,
        reportType: ReportTypeEnum
    ) {
        val indexFilePath = if (reportType == ReportTypeEnum.INTERNAL) {
            Paths.get(indexFile).normalize().toString()
        } else {
            indexFile // 防止第三方报告url路径http://截断成http:/
        }
        logger.info(
            "[$buildId]|pipelineId=$pipelineId|projectId=$projectId|taskId=$taskId" +
                "|indexFile=$indexFile|name=$name|reportType=$reportType|indexFilePath=$indexFilePath"
        )
        reportDao.create(dslContext, projectId, pipelineId, buildId, taskId, indexFilePath, name, reportType.name)
    }

    fun list(userId: String, projectId: String, pipelineId: String, buildId: String): List<Report> {
        val reportRecordList = reportDao.list(dslContext, projectId, pipelineId, buildId)

        return reportRecordList.map {
            if (it.type == ReportTypeEnum.INTERNAL.name) {
                val indexFile = Paths.get(it.indexFile).normalize().toString()
                val result = client.get(ServiceArtifactoryResource::class).getReportRootUrl(
                    projectCode = projectId, pipelineId = pipelineId, buildId = buildId, taskId = it.elementId
                )
                val urlPrefix = "${result.data}/$indexFile"
                Report(it.name, urlPrefix, it.type)
            } else {
                Report(it.name, it.indexFile, it.type)
            }
        }
    }
}
