package com.tencent.devops.artifactory.resources.user

import com.tencent.devops.artifactory.api.user.UserReportResource
import com.tencent.devops.artifactory.service.ReportService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class UserReportResourceImpl @Autowired constructor(
    private val reportService: ReportService
) : UserReportResource {

    override fun get(userId: String, projectId: String, pipelineId: String, buildId: String, elementId: String, path: String): Response {
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
        if (elementId.isBlank()) {
            throw ParamBlankException("Invalid elementId")
        }
        if (path.isBlank()) {
            throw ParamBlankException("Invalid path")
        }
        return reportService.get(projectId, pipelineId, buildId, elementId, path)
    }
}