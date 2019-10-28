package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.BuildFileResource
import com.tencent.devops.plugin.service.EnterpriseService
import com.tencent.devops.plugin.service.SecurityService
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

@RestResource
class BuildFileResourceImpl @Autowired constructor(
    private val securityService: SecurityService,
    private val enterpriseService: EnterpriseService
) : BuildFileResource {

    override fun securityUpload(fileStream: InputStream, envId: String, fileName: String, projectId: String, buildId: String, elementId: String): Result<String> {
        return Result(securityService.upload(fileStream, envId, fileName, projectId, buildId, elementId))
    }

    override fun enterpriseSignUpload(fileStream: InputStream, md5: String, size: String, fileName: String, props: String, elementId: String, projectId: String, pipelineId: String, buildId: String): Result<String> {
        return Result(enterpriseService.upload(fileStream, md5, size.toLong(), projectId, pipelineId, buildId,
                elementId, fileName, props))
    }
}