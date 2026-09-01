package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.repository.api.scm.ServiceScmRepositoryApiResource
import com.tencent.devops.scm.api.pojo.repository.git.GitScmServerRepository
import org.springframework.stereotype.Service

@Service
class PipelineYamlCommonService(
    private val client: Client
) {
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
}
