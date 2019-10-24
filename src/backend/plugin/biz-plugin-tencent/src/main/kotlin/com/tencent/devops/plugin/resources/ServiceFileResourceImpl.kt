package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.ServiceFileResource
import com.tencent.devops.plugin.pojo.security.UploadParams
import com.tencent.devops.plugin.service.SecurityService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceFileResourceImpl @Autowired constructor(
    private val securityService: SecurityService
) : ServiceFileResource {
    override fun securityUpload(uploadParams: UploadParams): Result<String> {
        return Result(securityService.noEnvUpload(uploadParams))
    }

    override fun getSecurityResult(envId: String, projectId: String, buildId: String, elementId: String, taskId: String): Result<String> {
        return Result(securityService.getFinalResult(projectId, envId, buildId, elementId, taskId))
    }
}