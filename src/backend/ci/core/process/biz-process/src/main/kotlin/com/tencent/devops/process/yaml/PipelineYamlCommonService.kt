package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.common.Constansts
import com.tencent.devops.repository.api.scm.ServiceScmRepositoryApiResource
import com.tencent.devops.scm.api.pojo.repository.git.GitScmServerRepository
import org.springframework.stereotype.Service

@Service
class PipelineYamlCommonService(
    private val pipelineYamlService: PipelineYamlService,
    private val client: Client
) {

    fun checkPushParam(
        projectId: String,
        pipelineId: String,
        content: String,
        repoHashId: String,
        filePath: String,
        targetAction: CodeTargetAction,
        versionName: String?,
        targetBranch: String?
    ) {
        if (content.isBlank()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_YAML_CONTENT_IS_EMPTY,
                params = arrayOf(repoHashId)
            )
        }
        if (filePath.startsWith(Constansts.ciFileDirectoryName) &&
            !GitActionCommon.checkYamlPipelineFile(filePath.substringAfter(".ci/"))
        ) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_YAML_FILE_NAME_FORMAT
            )
        }
        if (
            (targetAction != CodeTargetAction.COMMIT_TO_MASTER &&
                targetAction != CodeTargetAction.COMMIT_TO_BRANCH) &&
            versionName.isNullOrBlank()
        ) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf("versionName")
            )
        }
        if (targetAction == CodeTargetAction.COMMIT_TO_BRANCH && targetBranch.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf("targetBranch")
            )
        }
        pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId, repoHashId = repoHashId, filePath = filePath
        )?.let {
            if (it.pipelineId != pipelineId) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_YAML_BOUND_PIPELINE,
                    params = arrayOf(filePath, it.pipelineId)
                )
            }
        }
        pipelineYamlService.getPipelineYamlInfo(projectId = projectId, pipelineId = pipelineId)?.let {
            if (it.repoHashId != repoHashId) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_BOUND_REPO,
                    params = arrayOf(it.repoHashId)
                )
            }
            if (it.filePath != filePath) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_BOUND_YAML,
                    params = arrayOf(it.filePath)
                )
            }
        }
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
}
