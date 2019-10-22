package com.tencent.devops.artifactory.resources.gw

import com.tencent.devops.artifactory.api.gw.GwArtifactoryResource
import com.tencent.devops.artifactory.pojo.DownloadUrl
import com.tencent.devops.artifactory.service.ArtifactoryDownloadService
import com.tencent.devops.artifactory.service.ArtifactoryService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.BkAuthResourceType
import com.tencent.devops.common.auth.api.BkAuthServiceCode
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class GwArtifactoryResourceImpl @Autowired constructor(
    private val artifactoryService: ArtifactoryService,
    private val artifactoryDownloadService: ArtifactoryDownloadService
) : GwArtifactoryResource {

    override fun hasDownloadPermission(userId: String, projectId: String, serviceCode: String, resourceType: String, path: String): Result<Boolean> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (path.isBlank()) {
            throw ParamBlankException("Invalid path")
        }
        val bkAuthServiceCode = BkAuthServiceCode.get(serviceCode)
        val bkAuthResourceType = BkAuthResourceType.get(resourceType)
        return Result(artifactoryService.hasDownloadPermission(userId, projectId, bkAuthServiceCode, bkAuthResourceType, path))
    }

    override fun getDownloadUrl(token: String): Result<DownloadUrl> {
        if (token.isBlank()) {
            throw ParamBlankException("Invalid token")
        }
        return Result(artifactoryDownloadService.getDownloadUrl(token))
    }
}