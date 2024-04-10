package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.Report
import com.tencent.devops.process.service.TXReportService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class TXUserReportResourceImpl @Autowired constructor(
    private val reportService: TXReportService
) : TXUserReportResource {

    override fun getGitCI(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<List<Report>> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        val result = reportService.listNoApiHost(userId, projectId, pipelineId, buildId)
        return Result(result)
    }
}