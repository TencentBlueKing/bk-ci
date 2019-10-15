package com.tencent.devops.image.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.image.api.ServiceDevCloudImageResource
import com.tencent.devops.image.pojo.DockerTag
import com.tencent.devops.image.service.ImageArtifactoryService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceDevCloudImageResourceImpl @Autowired constructor(
    private val artifactoryService: ImageArtifactoryService
) : ServiceDevCloudImageResource {

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceDevCloudImageResourceImpl::class.java)
    }

    override fun listDevCloudImages(userId: String, projectId: String, public: Boolean): Result<List<DockerTag>> {
        checkUserAndProject(userId, projectId)

        try {
            return Result(artifactoryService.listDevCloudImages(projectId, public))
        } catch (e: Exception) {
            logger.error("list dev cloud image failed", e)
            throw RuntimeException("list dev cloud image failed")
        }
    }

    private fun checkUserAndProject(userId: String, projectId: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("projectId required")
        }
        if (userId.isBlank()) {
            throw ParamBlankException("userId required")
        }
    }
}