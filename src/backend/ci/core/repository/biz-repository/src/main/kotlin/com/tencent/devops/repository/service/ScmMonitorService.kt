package com.tencent.devops.repository.service

import org.springframework.stereotype.Service

@Service
class ScmMonitorService {
    fun reportCommitCheck(
        requestTime: Long,
        responseTime: Long,
        statusCode: Int,
        statusMessage: String?,
        projectName: String,
        commitId: String,
        block: Boolean,
        targetUrl: String
    ) = Unit
}