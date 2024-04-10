package com.tencent.devops.process.service

import com.tencent.devops.process.report.dao.ReportDao
import com.tencent.devops.process.pojo.Report
import com.tencent.devops.process.pojo.report.enums.ReportTypeEnum
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.nio.file.Paths

@Service
class TXReportService @Autowired constructor(
    private val dslContext: DSLContext,
    private val reportDao: ReportDao
) {

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
