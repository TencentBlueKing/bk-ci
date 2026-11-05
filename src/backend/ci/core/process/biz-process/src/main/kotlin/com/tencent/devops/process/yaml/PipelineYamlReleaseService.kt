package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.HTTP_401
import com.tencent.devops.common.api.constant.HTTP_403
import com.tencent.devops.common.api.constant.HTTP_404
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.pipeline.PipelineResourceOnlyVersion
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileReleaseReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileReleaseReqSource
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileReleaseResult
import com.tencent.devops.process.pojo.pipeline.enums.PipelineYamlStatus
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceOnlyVersion
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import com.tencent.devops.process.service.view.PipelineViewGroupService
import com.tencent.devops.process.trigger.yaml.PipelineYamlService
import com.tencent.devops.process.trigger.yaml.common.Constansts
import com.tencent.devops.process.trigger.yaml.common.YamlFileUtils
import com.tencent.devops.process.trigger.yaml.pojo.PipelineYamlTriggerLock
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.api.scm.ServiceScmFileApiResource
import com.tencent.devops.repository.api.scm.ServiceScmPullRequestApiResource
import com.tencent.devops.repository.api.scm.ServiceScmRepositoryApiResource
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.repository.pojo.credential.UserOauthTokenAuthCred
import com.tencent.devops.repository.pojo.hub.ScmFilePushReq
import com.tencent.devops.repository.pojo.hub.ScmPullRequestCreateReq
import com.tencent.devops.scm.api.pojo.Commit
import com.tencent.devops.scm.api.pojo.Content
import com.tencent.devops.scm.api.pojo.PullRequest
import com.tencent.devops.scm.api.pojo.repository.git.GitScmServerRepository
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PipelineYamlReleaseService(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val pipelineYamlService: PipelineYamlService,
    private val pipelineYamlViewService: PipelineYamlViewService,
    private val pipelineViewGroupService: PipelineViewGroupService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlReleaseService::class.java)
        private const val MAX_PULL_REQUEST_TITLE_LENGTH = 255
    }

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
                throw when (ignored.httpStatus) {
                    // 目标仓库被删除
                    HTTP_404 -> ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_GIT_PROJECT_NOT_FOUND_OR_NOT_PERMISSION,
                        params = arrayOf(repository.projectName)
                    )

                    HTTP_401, HTTP_403 -> ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_USER_NO_PUSH_PERMISSION,
                        params = arrayOf(userId, repository.projectName)
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
            return doReleaseYamlFile(yamlFileReleaseReq = yamlFileReleaseReq)
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
            !YamlFileUtils.checkYamlPipelineFile(filePath.substringAfter(".ci/"))
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
                targetBranch = targetBranch,
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
                targetBranch = targetBranch,
                source = PipelineYamlFileReleaseReqSource.TEMPLATE
            )
        }
    }

    /**
     * 发布流水线
     *
     */
    @Suppress("CyclomaticComplexMethod")
    private fun doReleaseYamlFile(yamlFileReleaseReq: PipelineYamlFileReleaseReq): PipelineYamlFileReleaseResult {
        with(yamlFileReleaseReq) {
            logger.info(
                "[PAC_PIPELINE]|release pipeline yaml file|" +
                    "$userId|$projectId|$pipelineId|$repoHashId|$version|$versionName"
            )
            val repository = client.get(ServiceRepositoryResource::class).get(
                projectId = projectId,
                repositoryId = repoHashId,
                repositoryType = RepositoryType.ID
            ).data ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.GIT_NOT_FOUND,
                params = arrayOf(repoHashId)
            )
            val lock = PipelineYamlTriggerLock(
                redisOperation = redisOperation,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                expiredTimeInSeconds = 180
            )
            return try {
                val authRepository = AuthRepository(repository)
                val serverRepository = client.get(ServiceScmRepositoryApiResource::class).getServerRepository(
                    projectId = projectId,
                    authRepository = authRepository
                ).data
                if (serverRepository !is GitScmServerRepository) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_NOT_SUPPORT_REPOSITORY_TYPE_ENABLE_PAC
                    )
                }
                if (!YamlFileUtils.isTemplateFile(filePath)) {
                    pipelineYamlViewService.createYamlViewIfAbsent(
                        userId = userId,
                        projectId = projectId,
                        repoHashId = repoHashId,
                        aliasName = repository.aliasName,
                        directoryList = setOf(YamlFileUtils.getCiDirectory(filePath))
                    )
                }
                lock.lock()
                val defaultBranch = serverRepository.defaultBranch!!
                val ref = when {
                    targetAction == CodeTargetAction.COMMIT_TO_MASTER -> defaultBranch
                    targetAction == CodeTargetAction.COMMIT_TO_BRANCH && targetBranch == defaultBranch -> defaultBranch
                    else -> versionName!!
                }
                // 发布应该使用流水线更新人的身份,不能使用pac开启人的身份
                val pushAuthRepository = authRepository.copy(
                    auth = UserOauthTokenAuthCred(
                        userId = userId
                    )
                )
                // 推送文件
                val filePushResult = client.get(ServiceScmFileApiResource::class).pushFile(
                    projectId = projectId,
                    filePushReq = ScmFilePushReq(
                        path = filePath,
                        ref = ref,
                        defaultBranch = defaultBranch,
                        content = content,
                        message = commitMessage,
                        authRepository = pushAuthRepository
                    )
                ).data!!
                // 创建mr
                val needCreatePullRequest = targetAction == CodeTargetAction.CHECKOUT_BRANCH_AND_REQUEST_MERGE ||
                    targetAction == CodeTargetAction.COMMIT_TO_SOURCE_BRANCH_AND_REQUEST_MERGE
                val pullRequest = if (needCreatePullRequest) {
                    createPullRequest(
                        ref = ref,
                        targetBranch = defaultBranch,
                        commitMessage = commitMessage,
                        newFile = filePushResult.newFile,
                        authRepository = pushAuthRepository
                    )
                } else {
                    null
                }

                createOrUpdateYamlPipeline(
                    ref = ref,
                    defaultBranch = defaultBranch,
                    content = filePushResult.content,
                    commit = filePushResult.commit
                )
                PipelineYamlFileReleaseResult(
                    projectId = projectId,
                    repoHashId = repoHashId,
                    filePath = filePath,
                    branch = ref,
                    pullRequestUrl = pullRequest?.link,
                    pullRequestId = pullRequest?.id,
                )
            } catch (ignored: RemoteServiceException) {
                throw when (ignored.httpStatus) {
                    // 目标仓库被删除
                    HTTP_404 -> ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_GIT_PROJECT_NOT_FOUND_OR_NOT_PERMISSION,
                        params = arrayOf(repository.projectName)
                    )

                    HTTP_401, HTTP_403 -> ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_USER_NO_PUSH_PERMISSION,
                        params = arrayOf(userId, repository.projectName)
                    )

                    else -> ignored
                }
            } catch (ignored: Exception) {
                logger.error(
                    "[PAC_PIPELINE]|Failed to release yaml pipeline|" +
                            "$projectId|$pipelineId|$repoHashId|$version|$versionName",
                    ignored
                )
                throw ignored
            } finally {
                lock.unlock()
            }
        }
    }

    private fun PipelineYamlFileReleaseReq.createPullRequest(
        ref: String,
        targetBranch: String,
        commitMessage: String,
        newFile: Boolean,
        authRepository: AuthRepository
    ): PullRequest? {

        // 优先使用用户输入的描述作为 MR 标题,为空白时回退到平台自动生成标题
        val title = buildPullRequestTitle(commitMessage = commitMessage, newFile = newFile)
        return client.get(ServiceScmPullRequestApiResource::class).createPullRequestIfAbsent(
            projectId = projectId,
            pullRequestCreateReq = ScmPullRequestCreateReq(
                title = title,
                body = commitMessage,
                sourceBranch = ref,
                targetBranch = targetBranch,
                authRepository = authRepository
            )
        ).data!!
    }

    private fun PipelineYamlFileReleaseReq.createOrUpdateYamlPipeline(
        ref: String,
        defaultBranch: String,
        content: Content,
        commit: Commit
    ) {
        val directory = YamlFileUtils.getCiDirectory(filePath)
        // 保存流水线
        val pipelineYamlInfo = pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath
        )
        val resourceType = YamlFileUtils.getYamlResourceType(
            filePath = filePath,
            fileContent = content.content
        )
        if (pipelineYamlInfo == null) {
            pipelineYamlService.save(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                directory = directory,
                defaultBranch = defaultBranch,
                blobId = content.blobId!!,
                ref = ref,
                commitId = commit.sha,
                commitTime = commit.commitTime ?: LocalDateTime.now(),
                pipelineId = pipelineId,
                status = if (ref == defaultBranch) {
                    PipelineYamlStatus.OK.name
                } else {
                    PipelineYamlStatus.UN_MERGED.name
                },
                version = version,
                userId = userId,
                resourceType = resourceType
            )
            pipelineViewGroupService.updateGroupAfterPipelineUpdate(
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                creator = userId,
                userId = userId
            )
        } else {
            pipelineYamlService.update(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                blobId = content.blobId,
                commitId = commit.sha,
                commitTime = commit.commitTime ?: LocalDateTime.now(),
                ref = ref,
                defaultBranch = defaultBranch,
                pipelineId = pipelineId,
                version = version,
                userId = userId,
                resourceType = resourceType
            )
        }
    }

    /**
     * 构造 MR 标题
     *
     * 优先使用用户输入的描述(commitMessage),为空白时回退到平台自动生成的标题。
     * MR 标题需为单行,因此仅取描述第一行,并对超长标题进行截断。
     */
    private fun PipelineYamlFileReleaseReq.buildPullRequestTitle(
        commitMessage: String,
        newFile: Boolean
    ): String {
        val userTitle = commitMessage.substringBefore('\n').trim()
        if (userTitle.isBlank()) {
            return getPullRequestTitle(newFile = newFile)
        }
        return if (userTitle.length > MAX_PULL_REQUEST_TITLE_LENGTH) {
            userTitle.take(MAX_PULL_REQUEST_TITLE_LENGTH - 3) + "..."
        } else {
            userTitle
        }
    }

    private fun PipelineYamlFileReleaseReq.getPullRequestTitle(
        newFile: Boolean
    ): String {
        val dateStr = DateTimeUtil.toDateTime(LocalDateTime.now())
        val title = if (newFile) {
            when (source) {
                PipelineYamlFileReleaseReqSource.PIPELINE -> {
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProcessMessageCode.BK_MERGE_PIPELINE_YAML_UPDATE_TITLE,
                        params = arrayOf(dateStr, pipelineName),
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                }

                PipelineYamlFileReleaseReqSource.TEMPLATE -> {
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProcessMessageCode.BK_MERGE_TEMPLATE_YAML_CREATE_TITLE,
                        params = arrayOf(dateStr, pipelineName),
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                }

                PipelineYamlFileReleaseReqSource.TEMPLATE_INSTANCE -> {
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProcessMessageCode.BK_MERGE_TEMPLATE_INSTANCE_YAML_TITLE,
                        params = arrayOf(dateStr, templateName ?: ""),
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                }
            }
        } else {
            when (source) {
                PipelineYamlFileReleaseReqSource.PIPELINE -> {
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProcessMessageCode.BK_MERGE_PIPELINE_YAML_UPDATE_TITLE,
                        params = arrayOf(dateStr, pipelineName),
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                }

                PipelineYamlFileReleaseReqSource.TEMPLATE -> {
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProcessMessageCode.BK_MERGE_TEMPLATE_YAML_UPDATE_TITLE,
                        params = arrayOf(dateStr, pipelineName),
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                }

                PipelineYamlFileReleaseReqSource.TEMPLATE_INSTANCE -> {
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProcessMessageCode.BK_MERGE_TEMPLATE_INSTANCE_YAML_TITLE,
                        params = arrayOf(dateStr, templateName ?: ""),
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                }
            }
        }
        return title
    }
}
