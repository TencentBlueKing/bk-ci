package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.HTTP_401
import com.tencent.devops.common.api.constant.HTTP_403
import com.tencent.devops.common.api.constant.HTTP_404
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.repository.api.scm.ServiceScmRepositoryApiResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.scm.api.pojo.repository.git.GitScmServerRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PipelineYamlCommonService(
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlCommonService::class.java)
    }

    fun getDefaultBranch(
        projectId: String,
        repoHashId: String?
    ): String? {
        if (repoHashId.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf("repoHashId")
            )
        }
        val serverRepository = client.get(ServiceScmRepositoryApiResource::class).getServerRepositoryById(
            projectId = projectId,
            repositoryType = RepositoryType.ID,
            repoHashIdOrName = repoHashId
        ).data
        if (serverRepository !is GitScmServerRepository) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_NOT_SUPPORT_REPOSITORY_TYPE_ENABLE_PAC
            )
        }
        return serverRepository.defaultBranch
    }

    fun getServiceRepository(
        projectId: String,
        repository: Repository
    ) = try {
        client.get(ServiceScmRepositoryApiResource::class).getServerRepository(
            projectId = projectId,
            authRepository = AuthRepository(repository)
        ).data
    } catch (ignored: RemoteServiceException) {
        throw when (ignored.httpStatus) {
            // 目标仓库被删除
            HTTP_404 -> ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_GIT_PROJECT_NOT_FOUND_OR_NOT_PERMISSION,
                params = arrayOf(repository.projectName)
            )

            HTTP_401, HTTP_403 -> ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_USER_NO_PUSH_PERMISSION,
                params = arrayOf(repository.userName, repository.projectName)
            )

            else -> ignored
        }
    } catch (ignored: Exception) {
        throw ignored
    }

    fun getServiceBranch(
        projectId: String,
        repository: Repository,
        page: Int,
        pageSize: Int,
        search: String?
    ) = try {
        client.get(ServiceScmRepositoryApiResource::class).listBranches(
            projectId = projectId,
            authRepository = AuthRepository(repository),
            page = page,
            pageSize = pageSize,
            search = search
        ).data
    } catch (ignored: Exception) {
        logger.warn("failed to get service branch", ignored)
        null
    }
}
