package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.HTTP_401
import com.tencent.devops.common.api.constant.HTTP_403
import com.tencent.devops.common.api.constant.HTTP_404
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.pipeline.PipelineResourceOnlyVersion
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileReleaseReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileReleaseReqSource
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileReleaseResult
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceOnlyVersion
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.common.Constansts
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.api.scm.ServiceScmRepositoryApiResource
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.repository.pojo.credential.UserOauthTokenAuthCred
import com.tencent.devops.scm.api.pojo.repository.git.GitScmServerRepository
import org.springframework.stereotype.Service

@Service
class PipelineYamlReleaseService(
    private val client: Client,
    private val pipelineYamlService: PipelineYamlService,
    private val pipelineYamlFileManager: PipelineYamlFileManager
) {

    fun validateReleaseYamlFile(
        context: PipelineVersionCreateContext,
        resourceOnlyVersion: PipelineResourceOnlyVersion,
        source: PipelineYamlFileReleaseReqSource = PipelineYamlFileReleaseReqSource.PIPELINE
    ) {
        if (!context.enablePac) {
            return
        }
        validateReleaseYamlFile(
            yamlFileReleaseReq = buildYamlFileReleaseReq(
                context = context,
                resourceOnlyVersion = resourceOnlyVersion,
                source = source
            )
        )
    }

    fun validateReleaseYamlFile(
        context: PipelineTemplateVersionCreateContext,
        resourceOnlyVersion: PTemplateResourceOnlyVersion
    ) {
        if (!context.enablePac) {
            return
        }
        validateReleaseYamlFile(
            yamlFileReleaseReq = buildYamlFileReleaseReq(
                context = context,
                resourceOnlyVersion = resourceOnlyVersion
            )
        )
    }

    fun validateReleaseYamlFile(yamlFileReleaseReq: PipelineYamlFileReleaseReq) {
        with(yamlFileReleaseReq) {
            checkPushParam(
                projectId = projectId,
                pipelineId = pipelineId,
                content = content,
                repoHashId = repoHashId,
                filePath = filePath,
                targetAction = targetAction,
                versionName = versionName,
                targetBranch = targetBranch
            )
            val repository = client.get(ServiceRepositoryResource::class).get(
                projectId = projectId,
                repositoryId = repoHashId,
                repositoryType = RepositoryType.ID
            ).data ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.GIT_NOT_FOUND,
                params = arrayOf(repoHashId)
            )
            val authRepository = AuthRepository(repository)
            // 查看仓库信息,需要使用流水线更新人的身份,主要用于验证是否有代码库的查看权限和是否有oauth授权
            val serverRepository = try {
                client.get(ServiceScmRepositoryApiResource::class).getServerRepository(
                    projectId = projectId,
                    authRepository = authRepository.copy(
                        auth = UserOauthTokenAuthCred(userId = userId)
                    )
                ).data
            } catch (ignored: RemoteServiceException) {
                throw when (ignored.errorCode) {
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
            if (serverRepository !is GitScmServerRepository) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_NOT_SUPPORT_REPOSITORY_TYPE_ENABLE_PAC
                )
            }
            val perm = client.get(ServiceScmRepositoryApiResource::class).findPerm(
                projectId = projectId,
                username = userId,
                authRepository = authRepository
            ).data!!
            if (!perm.push) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_NOT_REPOSITORY_PUSH_PERMISSION,
                    params = arrayOf(userId, serverRepository.fullName)
                )
            }
        }
    }

    fun releaseYamlFile(
        context: PipelineVersionCreateContext,
        resourceOnlyVersion: PipelineResourceOnlyVersion,
        source: PipelineYamlFileReleaseReqSource
    ): PipelineYamlFileReleaseResult? {
        if (!context.enablePac) {
            return null
        }
        return releaseYamlFile(
            yamlFileReleaseReq = buildYamlFileReleaseReq(
                context = context,
                resourceOnlyVersion = resourceOnlyVersion,
                source = source
            )
        )
    }

    fun releaseYamlFile(
        context: PipelineTemplateVersionCreateContext,
        resourceOnlyVersion: PTemplateResourceOnlyVersion
    ): PipelineYamlFileReleaseResult? {
        if (!context.enablePac) {
            return null
        }
        return releaseYamlFile(
            yamlFileReleaseReq = buildYamlFileReleaseReq(
                context = context,
                resourceOnlyVersion = resourceOnlyVersion
            )
        )
    }

    fun releaseYamlFile(yamlFileReleaseReq: PipelineYamlFileReleaseReq): PipelineYamlFileReleaseResult {
        with(yamlFileReleaseReq) {
            checkPushParam(
                projectId = projectId,
                pipelineId = pipelineId,
                content = content,
                repoHashId = repoHashId,
                filePath = filePath,
                targetAction = targetAction,
                versionName = versionName,
                targetBranch = targetBranch
            )
            return pipelineYamlFileManager.releaseYamlFile(yamlFileReleaseReq = yamlFileReleaseReq)
        }
    }

    private fun checkPushParam(
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

    private fun buildYamlFileReleaseReq(
        context: PipelineVersionCreateContext,
        resourceOnlyVersion: PipelineResourceOnlyVersion,
        source: PipelineYamlFileReleaseReqSource
    ): PipelineYamlFileReleaseReq {
        with(context) {
            return PipelineYamlFileReleaseReq(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = pipelineBasicInfo.pipelineName,
                version = resourceOnlyVersion.version,
                versionName = resourceOnlyVersion.versionName,
                repoHashId = yamlFileInfo!!.repoHashId,
                filePath = yamlFileInfo.filePath,
                content = pipelineResourceWithoutVersion.yaml!!,
                commitMessage = pipelineResourceWithoutVersion.description
                    ?: "update pipeline ${pipelineBasicInfo.pipelineName}",
                targetAction = targetAction!!,
                targetBranch = branchName,
                source = source,
                templateName = templateInstanceBasicInfo?.templateName
            )
        }
    }

    private fun buildYamlFileReleaseReq(
        context: PipelineTemplateVersionCreateContext,
        resourceOnlyVersion: PTemplateResourceOnlyVersion
    ): PipelineYamlFileReleaseReq {
        with(context) {
            return PipelineYamlFileReleaseReq(
                userId = userId,
                projectId = projectId,
                pipelineId = templateId,
                pipelineName = pipelineTemplateInfo.name,
                version = resourceOnlyVersion.version.toInt(),
                versionName = resourceOnlyVersion.versionName,
                repoHashId = yamlFileInfo!!.repoHashId,
                filePath = yamlFileInfo.filePath,
                content = pTemplateResourceWithoutVersion.yaml!!,
                commitMessage = pTemplateResourceWithoutVersion.description
                    ?: "update template ${pipelineTemplateInfo.name}",
                targetAction = targetAction!!,
                targetBranch = branchName,
                source = PipelineYamlFileReleaseReqSource.TEMPLATE
            )
        }
    }
}
