package com.tencent.devops.artifactory.service

import javax.ws.rs.core.Response

interface ReportService {
    fun get(
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        path: String
    ): Response
}