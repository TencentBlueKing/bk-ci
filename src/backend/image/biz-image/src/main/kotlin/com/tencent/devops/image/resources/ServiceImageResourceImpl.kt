package com.tencent.devops.image.resources

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.image.api.ServiceImageResource
import com.tencent.devops.image.pojo.DockerTag
import com.tencent.devops.image.service.ImageArtifactoryService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceImageResourceImpl @Autowired constructor(
    private val artifactoryService: ImageArtifactoryService
) : ServiceImageResource {
    companion object {
        private val logger = LoggerFactory.getLogger(UserImageResourceImpl::class.java)
    }

    override fun listDockerBuildImages(userId: String, projectId: String): Result<List<DockerTag>> {
        checkUserAndProject(userId, projectId)
        try {
            return Result(artifactoryService.listDockerBuildImages(projectId))
        } catch (e: Exception) {
            logger.error("list docker build image failed", e)
            throw RuntimeException("list docker build image failed")
        }
    }

    override fun setBuildImage(userId: String, projectId: String, imageRepo: String, imageTag: String): Result<Boolean> {
        if (imageRepo.isBlank()) {
            throw OperationException("imageRepo required")
        }
        if (imageTag.isBlank()) {
            throw OperationException("imageTag required")
        }

        return try {
            Result(artifactoryService.copyToBuildImage(projectId, imageRepo, imageTag))
        } catch (e: OperationException) {
            Result(1, e.message!!)
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