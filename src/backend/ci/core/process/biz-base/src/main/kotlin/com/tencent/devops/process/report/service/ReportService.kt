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

package com.tencent.devops.process.report.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.model.process.tables.records.TReportRecord
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.pojo.Report
import com.tencent.devops.common.archive.pojo.ReportListDTO
import com.tencent.devops.common.archive.pojo.TaskReport
import com.tencent.devops.process.pojo.report.ReportEmail
import com.tencent.devops.process.pojo.report.enums.ReportTypeEnum
import com.tencent.devops.process.report.dao.ReportDao
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.nio.file.Paths
import javax.ws.rs.core.Response

@Suppress("ALL")
@Service
class ReportService @Autowired constructor(
    private val dslContext: DSLContext,
    private val reportDao: ReportDao,
    private val client: Client,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineRuntimeService: PipelineRuntimeService
) {
    private val logger = LoggerFactory.getLogger(ReportService::class.java)

    fun create(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        indexFile: String,
        name: String,
        reportType: ReportTypeEnum,
        reportEmail: ReportEmail? = null
    ) {
        val taskInfo = reportDao.getAtomInfo(
                dslContext = dslContext,
                buildId = buildId,
                taskId = taskId
        )

        val indexFilePath = if (reportType == ReportTypeEnum.INTERNAL) {
            Paths.get(indexFile).normalize().toString()
        } else {
            indexFile // 防止第三方报告url路径http://截断成http:/
        }
        logger.info(
            "[$buildId]|pipelineId=$pipelineId|projectId=$projectId|taskId=$taskId" +
                "|indexFile=$indexFile|name=$name|reportType=$reportType|indexFilePath=$indexFilePath"
        )

//        if (!reportDao.exists(
//                dslContext = dslContext,
//                projectId = projectId,
//                pipelineId = pipelineId,
//                buildId = buildId,
//                name = name
//            )
//        ) {
            reportDao.create(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                elementId = taskId,
                indexFile = indexFilePath,
                name = name,
                type = reportType.name,
                atomCode = taskInfo?.value1() ?: "",
                taskName = taskInfo?.value2() ?: "",
                id = client.get(ServiceAllocIdResource::class).generateSegmentId("REPORT").data
            )
//        } else {
//            reportDao.update(
//                dslContext = dslContext,
//                projectId = projectId,
//                pipelineId = pipelineId,
//                buildId = buildId,
//                elementId = taskId,
//                indexFile = indexFilePath,
//                name = name,
//                type = reportType
//            )
//        }

        if (reportEmail != null) {
            sendEmail(reportEmail.receivers, reportEmail.title, reportEmail.html)
        }
    }

    fun list(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String? = null
    ): List<Report> {
        val reportRecordList = reportDao.list(dslContext, projectId, pipelineId, buildId)

        val result = mutableListOf<Report>()
        reportRecordList.forEach {
            if (taskId.isNullOrBlank()) {
                result.add(buildReport(projectId, pipelineId, buildId, it))
            } else {
                if (taskId == it.elementId) {
                    result.add(buildReport(projectId, pipelineId, buildId, it))
                }
            }
        }
        return result
    }

    fun listContainTask(reportListDTO: ReportListDTO): List<TaskReport> {
        val projectId = reportListDTO.projectId
        val reportRecordList = reportDao.list(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = reportListDTO.pipelineId,
            buildId = reportListDTO.buildId
        )
        return reportRecordList.map {
            val taskRecord = pipelineTaskService.getBuildTask(projectId, reportListDTO.buildId, it.elementId)
            val atomCode = taskRecord?.atomCode ?: ""
            val atomName = taskRecord?.taskName ?: ""
            if (it.type == ReportTypeEnum.INTERNAL.name) {
                val indexFile = Paths.get(it.indexFile).normalize().toString()
                val urlPrefix = getRootUrl(
                    projectId = reportListDTO.projectId,
                    pipelineId = reportListDTO.pipelineId,
                    buildId = reportListDTO.buildId,
                    taskId = it.elementId
                )
                TaskReport(
                    name = it.name,
                    indexFileUrl = "$urlPrefix$indexFile",
                    type = it.type,
                    taskId = it.elementId,
                    atomCode = atomCode,
                    atomName = atomName,
                    createTime = it.createTime
                )
            } else {
                TaskReport(
                    name = it.name,
                    indexFileUrl = it.indexFile,
                    type = it.type,
                    taskId = it.elementId,
                    atomCode = atomCode,
                    atomName = atomName,
                    createTime = it.createTime
                )
            }
        }
    }

    fun getRootUrl(projectId: String, buildId: String, taskId: String): String {
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )
        return getRootUrl(
            projectId = buildInfo.projectId,
            pipelineId = buildInfo.pipelineId,
            buildId = buildId,
            taskId = taskId
        )
    }

    private fun getRootUrl(projectId: String, pipelineId: String, buildId: String, taskId: String): String =
        "${HomeHostUtil.innerApiHost()}/artifactory/api-html/user/reports/$projectId/$pipelineId/$buildId/$taskId/"

    private fun sendEmail(receivers: Set<String>, title: String, html: String) {
        val emailNotifyMessage = EmailNotifyMessage()
        emailNotifyMessage.addAllReceivers(receivers)
        emailNotifyMessage.format = EnumEmailFormat.HTML
        emailNotifyMessage.title = title
        emailNotifyMessage.body = html
        client.get(ServiceNotifyResource::class).sendEmailNotify(emailNotifyMessage)
    }

    private fun buildReport(projectId: String, pipelineId: String, buildId: String, info: TReportRecord): Report {
        return if (info.type == ReportTypeEnum.INTERNAL.name) {
            val indexFile = Paths.get(info.indexFile).normalize().toString()
            val urlPrefix = getRootUrl(projectId, pipelineId, buildId, info.elementId)
            Report(info.name, "$urlPrefix$indexFile", info.type)
        } else {
            Report(info.name, info.indexFile, info.type)
        }
    }

    fun listNoApiHost(userId: String, projectId: String, pipelineId: String, buildId: String): List<Report> {
        val reportRecordList = reportDao.list(dslContext, projectId, pipelineId, buildId)

        val reportList = mutableListOf<Report>()
        reportRecordList.forEach {
            if (it.type == ReportTypeEnum.INTERNAL.name) {
                val indexFile = Paths.get(it.indexFile).normalize().toString()
                val urlPrefix = getRootUrlNoApiHost(projectId, pipelineId, buildId, it.elementId)
                reportList.add(Report(it.name, "$urlPrefix$indexFile", it.type))
            } else {
                reportList.add(Report(it.name, it.indexFile, it.type))
            }
        }
        return reportList
    }

    private fun getRootUrlNoApiHost(projectId: String, pipelineId: String, buildId: String, taskId: String): String {
        return "/$projectId/report/$pipelineId/$buildId/$taskId/"
    }
}
