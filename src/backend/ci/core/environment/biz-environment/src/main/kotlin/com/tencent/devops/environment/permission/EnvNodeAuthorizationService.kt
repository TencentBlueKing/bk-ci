package com.tencent.devops.environment.permission

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthAuthorizationApi
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverResult
import com.tencent.devops.common.auth.enums.ResourceAuthorizationHandoverStatus
import com.tencent.devops.environment.service.NodeService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EnvNodeAuthorizationService constructor(
    val authAuthorizationApi: AuthAuthorizationApi,
    val nodeService: NodeService
) {
    fun batchModifyHandoverFrom(
        projectId: String,
        resourceAuthorizationHandoverList: List<ResourceAuthorizationHandoverDTO>
    ) {
        authAuthorizationApi.batchModifyHandoverFrom(
            projectId = projectId,
            resourceAuthorizationHandoverList = resourceAuthorizationHandoverList
        )
    }

    fun addResourceAuthorization(
        projectId: String,
        resourceAuthorizationList: List<ResourceAuthorizationDTO>
    ) {
        authAuthorizationApi.addResourceAuthorization(
            projectId = projectId,
            resourceAuthorizationList = resourceAuthorizationList
        )
    }

    fun resetEnvNodeAuthorization(
        projectId: String,
        preCheck: Boolean,
        resourceAuthorizationHandoverDTOs: List<ResourceAuthorizationHandoverDTO>
    ): Map<ResourceAuthorizationHandoverStatus, List<ResourceAuthorizationHandoverDTO>> {
        logger.info("reset env node authorization|$preCheck|$projectId|$resourceAuthorizationHandoverDTOs")
        return authAuthorizationApi.resetResourceAuthorization(
            projectId = projectId,
            preCheck = preCheck,
            resourceAuthorizationHandoverDTOs = resourceAuthorizationHandoverDTOs,
            handoverResourceAuthorization = ::handoverEnvNodeAuthorization
        )
    }

    private fun handoverEnvNodeAuthorization(
        preCheck: Boolean,
        resourceAuthorizationHandoverDTO: ResourceAuthorizationHandoverDTO
    ): ResourceAuthorizationHandoverResult {
        with(resourceAuthorizationHandoverDTO) {
            try {
                if (preCheck) {
                    nodeService.checkCmdbOperator(
                        userId = handoverTo!!,
                        projectId = projectCode,
                        nodeHashId = resourceCode
                    )
                } else {
                    nodeService.changeCreatedUser(
                        userId = handoverTo!!,
                        projectId = projectCode,
                        nodeHashId = resourceCode
                    )
                }
            } catch (ignore: Exception) {
                return ResourceAuthorizationHandoverResult(
                    status = ResourceAuthorizationHandoverStatus.FAILED,
                    message = when (ignore) {
                        is ErrorCodeException -> ignore.defaultMessage
                        else -> ignore.message
                    }
                )
            }
        }
        return ResourceAuthorizationHandoverResult(
            status = ResourceAuthorizationHandoverStatus.SUCCESS
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EnvNodeAuthorizationService::class.java)
    }
}
