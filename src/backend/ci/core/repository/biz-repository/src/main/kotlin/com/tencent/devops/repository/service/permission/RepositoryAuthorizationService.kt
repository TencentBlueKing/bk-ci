package com.tencent.devops.repository.service.permission

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthAuthorizationApi
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverResult
import com.tencent.devops.common.auth.enums.ResourceAuthorizationHandoverStatus
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.service.RepositoryService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RepositoryAuthorizationService constructor(
    private val authAuthorizationApi: AuthAuthorizationApi,
    private val repositoryService: RepositoryService
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

    fun resetRepositoryAuthorization(
        projectId: String,
        preCheck: Boolean,
        resourceAuthorizationHandoverDTOs: List<ResourceAuthorizationHandoverDTO>
    ): Map<ResourceAuthorizationHandoverStatus, List<ResourceAuthorizationHandoverDTO>> {
        logger.info("reset repository authorization|$preCheck|$projectId|$resourceAuthorizationHandoverDTOs")
        return authAuthorizationApi.resetResourceAuthorization(
            projectId = projectId,
            preCheck = preCheck,
            resourceAuthorizationHandoverDTOs = resourceAuthorizationHandoverDTOs,
            handoverResourceAuthorization = ::handoverRepositoryAuthorization
        )
    }

    private fun handoverRepositoryAuthorization(
        preCheck: Boolean,
        resourceAuthorizationHandoverDTO: ResourceAuthorizationHandoverDTO
    ): ResourceAuthorizationHandoverResult {
        with(resourceAuthorizationHandoverDTO) {
            val handoverTo = handoverTo!!
            try {
                val repositoryRecord = repositoryService.getRepository(
                    projectId = projectCode,
                    repositoryConfig = RepositoryConfig(
                        repositoryName = null,
                        repositoryHashId = resourceCode,
                        repositoryType = RepositoryType.ID
                    )
                )
                val repository = repositoryService.compose(repositoryRecord)
                validateResourcePermission(
                    userId = resourceAuthorizationHandoverDTO.handoverTo!!,
                    projectCode = resourceAuthorizationHandoverDTO.projectCode,
                    resourceName = resourceAuthorizationHandoverDTO.resourceName,
                    resourceCode = resourceAuthorizationHandoverDTO.resourceCode,
                    repository = repository
                )
                if (!preCheck) {
                    // 重置权限
                    repositoryService.reOauth(
                        repository = repository,
                        repositoryRecord = repositoryRecord,
                        userId = handoverTo,
                        projectId = projectCode
                    )
                }
            } catch (ignore: Exception) {
                return ResourceAuthorizationHandoverResult(
                    status = ResourceAuthorizationHandoverStatus.FAILED,
                    message = when (ignore) {
                        is PermissionForbiddenException -> ignore.defaultMessage
                        else -> ignore.message
                    }
                )
            }
            return ResourceAuthorizationHandoverResult(
                status = ResourceAuthorizationHandoverStatus.SUCCESS
            )
        }
    }

    /**
     * 校验资源权限
     * @param userId 用户名
     * @param projectCode 项目英文名称
     * @param resourceName 资源名称
     * @param resourceCode 代码库hashID
     * @param repository 代码库关联信息
     */
    private fun validateResourcePermission(
        userId: String,
        projectCode: String,
        resourceName: String,
        resourceCode: String,
        repository: Repository
    ) {
        // 校验编辑权限
        val repositoryId = HashUtil.decodeOtherIdToLong(resourceCode)
        repositoryService.validatePermission(
            user = userId,
            projectId = projectCode,
            repositoryId = repositoryId,
            authPermission = AuthPermission.EDIT,
            message = MessageUtil.getMessageByLocale(
                messageCode = RepositoryMessageCode.USER_EDIT_PEM_ERROR,
                params = arrayOf(userId, projectCode, resourceName),
                language = I18nUtil.getLanguage(userId)
            )
        )
        // 校验下载权限
        repositoryService.checkRepoDownloadPem(
            userId = userId,
            projectId = projectCode,
            repository = repository
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryAuthorizationService::class.java)
    }
}
