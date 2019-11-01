package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.ServiceCustomDirResource
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.service.CustomDirGsService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceCustomDirResourceImpl @Autowired constructor(
    private val customDirGsService: CustomDirGsService
) : ServiceCustomDirResource {
    override fun getGsDownloadUrl(projectId: String, fileName: String, userId: String): Result<Url> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (fileName.isBlank()) {
            throw ParamBlankException("Invalid fileName")
        }
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        return Result(Url(customDirGsService.getDownloadUrl(projectId, fileName, userId)))
    }
}