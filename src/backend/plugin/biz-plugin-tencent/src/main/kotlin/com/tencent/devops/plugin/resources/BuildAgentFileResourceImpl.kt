package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.BuildAgentFileResource
import com.tencent.devops.plugin.service.SecurityService
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

@RestResource
class BuildAgentFileResourceImpl @Autowired constructor(
    private val securityService: SecurityService
) : BuildAgentFileResource {

    override fun securityUpload(projectId: String, elementId: String, agentId: String, secretKey: String, buildId: String, fileStream: InputStream, envId: String, fileName: String): Result<String> {
        return Result(securityService.upload(fileStream, envId, fileName, projectId, buildId, elementId))
    }
}