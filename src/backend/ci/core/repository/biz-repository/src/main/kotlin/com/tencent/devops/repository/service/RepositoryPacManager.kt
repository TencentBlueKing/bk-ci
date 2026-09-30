package com.tencent.devops.repository.service

import com.tencent.devops.common.api.constant.HttpStatus
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.service.ServicePipelineYamlResource
import com.tencent.devops.process.pojo.pipeline.PipelineYamlPacDisableReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlPacEnableReq
import com.tencent.devops.repository.constant.RepositoryConstants
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.constant.RepositoryMessageCode.ERROR_AUTH_TYPE_ENABLED_PAC
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.pojo.RepoCondition
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.repository.pojo.enums.RepoYamlSyncStatusEnum
import com.tencent.devops.repository.service.hub.ScmFileApiService
import com.tencent.devops.repository.service.hub.ScmRefApiService
import com.tencent.devops.repository.service.hub.ScmRepositoryApiService
import com.tencent.devops.repository.service.loader.CodeRepositoryServiceRegistrar
import com.tencent.devops.repository.utils.RepositoryUtils
import com.tencent.devops.scm.api.enums.ContentKind
import com.tencent.devops.scm.api.enums.ScmEventType
import com.tencent.devops.scm.api.exception.ScmApiException
import com.tencent.devops.scm.api.pojo.Tree
import com.tencent.devops.scm.api.pojo.repository.git.GitScmServerRepository
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RepositoryPacManager @Autowired constructor(
    private val dslContext: DSLContext,
    private val repositoryDao: RepositoryDao,
    private val client: Client,
    private val repositoryApiService: ScmRepositoryApiService,
    private val fileApiService: ScmFileApiService,
    private val refApiService: ScmRefApiService
) {

    fun validateEnablePac(
        userId: String,
        projectId: String,
        repository: Repository
    ) {
        logger.info("validate enable pac|$userId|$projectId|${repository.repoHashId}")
        val isOauth = RepositoryUtils.getOauthUser(repository).first
        if (!isOauth) {
            throw ErrorCodeException(errorCode = ERROR_AUTH_TYPE_ENABLED_PAC)
        }
        val authRepository = AuthRepository(repository)
        val serverRepository = repositoryApiService.findRepository(
            projectId = projectId,
            authRepository = authRepository
        )
        if (serverRepository !is GitScmServerRepository) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.ERROR_NOT_SUPPORT_REPOSITORY_TYPE_ENABLE_PAC
            )
        }
        doValidateEnablePac(
            projectId = projectId,
            repository = repository,
            authRepository = authRepository,
            serverRepository = serverRepository
        )
    }

    /**
     * 开启 pac（幂等）。默认先执行 [validateEnablePac]；创建场景可在入库前单独校验，入库后传 [skipValidate]=true。
     *
     * @param skipValidate 为 true 时跳过校验（调用方已提前校验）
     */
    fun enablePac(
        userId: String,
        projectId: String,
        repository: Repository,
        skipValidate: Boolean = false
    ) {
        val isOauth = RepositoryUtils.getOauthUser(repository).first
        if (!isOauth) {
            throw ErrorCodeException(errorCode = ERROR_AUTH_TYPE_ENABLED_PAC)
        }
        val repositoryId = HashUtil.decodeOtherIdToLong(repository.repoHashId!!)
        val authRepository = AuthRepository(repository)
        val serverRepository = repositoryApiService.findRepository(
            projectId = projectId,
            authRepository = authRepository
        )
        if (serverRepository !is GitScmServerRepository) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.ERROR_NOT_SUPPORT_REPOSITORY_TYPE_ENABLE_PAC
            )
        }
        if (!skipValidate) {
            doValidateEnablePac(
                projectId = projectId,
                repository = repository,
                authRepository = authRepository,
                serverRepository = serverRepository
            )
        }
        repositoryApiService.createHook(
            projectId = projectId,
            events = listOf(
                ScmEventType.PUSH.value,
                ScmEventType.PULL_REQUEST.value
            ),
            authRepository = authRepository,
            scmType = repository.getScmType(),
            scmCode = repository.scmCode
        )
        val defaultBranch = serverRepository.defaultBranch!!
        val fileTrees = fileApiService.listFileTree(
            projectId = projectId,
            path = RepositoryConstants.CI_DIR_PATH,
            ref = defaultBranch,
            recursive = true,
            authRepository = authRepository
        )
        if (fileTrees.isEmpty()) {
            repositoryDao.enablePac(
                dslContext = dslContext,
                userId = userId,
                projectId = projectId,
                repositoryId = repositoryId,
                syncStatus = RepoYamlSyncStatusEnum.SUCCEED.name
            )
        } else {
            val commit = refApiService.findCommit(
                projectId = projectId,
                authRepository = authRepository,
                sha = defaultBranch
            )
            repositoryDao.enablePac(
                dslContext = dslContext,
                userId = userId,
                projectId = projectId,
                repositoryId = repositoryId,
                syncStatus = RepoYamlSyncStatusEnum.SYNC.name
            )
            client.get(ServicePipelineYamlResource::class).enablePac(
                userId = userId,
                projectId = projectId,
                yamlPacEnableReq = PipelineYamlPacEnableReq(
                    repository = repository,
                    fileTrees = fileTrees,
                    defaultBranch = defaultBranch,
                    commit = commit
                )
            )
        }
    }

    fun listCiFileTree(projectId: String, repository: Repository): List<Tree> {
        val authRepository = AuthRepository(repository)
        val serverRepository = repositoryApiService.findRepository(
            projectId = projectId,
            authRepository = authRepository
        )
        if (serverRepository !is GitScmServerRepository) {
            return emptyList()
        }
        val defaultBranch = serverRepository.defaultBranch ?: return emptyList()
        return fileApiService.listFileTree(
            projectId = projectId,
            path = RepositoryConstants.CI_DIR_PATH,
            ref = defaultBranch,
            recursive = false,
            authRepository = authRepository
        )
    }

    fun checkCiDirExists(projectId: String, repository: Repository): Boolean {
        return listCiFileTree(projectId = projectId, repository = repository).isNotEmpty()
    }

    fun getCiSubDir(projectId: String, repository: Repository): List<String> {
        return listCiFileTree(projectId = projectId, repository = repository)
            .filter { it.kind == ContentKind.DIRECTORY }
            .map { it.path.substringAfterLast('/') }
    }

    fun disablePac(userId: String, projectId: String, repository: Repository) {
        val repositoryId = HashUtil.decodeOtherIdToLong(repository.repoHashId!!)
        if (repository.enablePac == false) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.ERROR_REPO_NOT_ENABLED_PAC
            )
        }

        val defaultBranch = getDefaultBranchForDisable(
            userId = userId,
            projectId = projectId,
            repository = repository
        )
        repositoryDao.disablePac(
            dslContext = dslContext,
            userId = userId,
            projectId = projectId,
            repositoryId = repositoryId
        )
        client.get(ServicePipelineYamlResource::class).disablePac(
            userId = userId,
            projectId = projectId,
            yamlPacDisableReq = PipelineYamlPacDisableReq(
                repository = repository,
                defaultBranch = defaultBranch
            )
        )
    }

    /**
     * 解析关闭PAC所需的默认分支并校验.ci已清理。
     * 服务端仓库已删除(404)时返回null仍允许关闭; .ci不存在(404)同样放行。
     */
    private fun getDefaultBranchForDisable(
        userId: String,
        projectId: String,
        repository: Repository
    ): String? {
        val authRepository = AuthRepository(repository)
        return try {
            val serverRepository = repositoryApiService.findRepository(
                projectId = projectId,
                authRepository = authRepository
            )
            if (serverRepository !is GitScmServerRepository) {
                throw ErrorCodeException(
                    errorCode = RepositoryMessageCode.ERROR_NOT_SUPPORT_REPOSITORY_TYPE_ENABLE_PAC
                )
            }
            val defaultBranch = serverRepository.defaultBranch ?: throw ErrorCodeException(
                errorCode = RepositoryMessageCode.ERROR_DEFAULT_BRANCH_IS_EMPTY
            )
            val ciFileTree = fileApiService.listFileTree(
                projectId = projectId,
                path = RepositoryConstants.CI_DIR_PATH,
                ref = defaultBranch,
                recursive = false,
                authRepository = authRepository
            )
            if (ciFileTree.isNotEmpty()) {
                throw ErrorCodeException(
                    errorCode = RepositoryMessageCode.ERROR_REPO_CI_DIR_EXISTS
                )
            }
            defaultBranch
        } catch (ignored: Exception) {
            if (isNotFoundException(ignored)) {
                logger.info(
                    "server repository not found, continue disable pac|" +
                        "$userId|$projectId|${repository.repoHashId}",
                    ignored
                )
                null
            } else {
                throw ignored
            }
        }
    }

    private fun isNotFoundException(exception: Exception): Boolean {
        val httpStatus = when (exception) {
            is ErrorCodeException -> exception.statusCode
            is ScmApiException -> exception.statusCode
            is RemoteServiceException -> exception.httpStatus
            else -> return false
        }
        return httpStatus == HttpStatus.NOT_FOUND.value
    }

    private fun doValidateEnablePac(
        projectId: String,
        repository: Repository,
        authRepository: AuthRepository,
        serverRepository: GitScmServerRepository
    ) {
        if (serverRepository.defaultBranch == null) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.ERROR_DEFAULT_BRANCH_IS_EMPTY
            )
        }
        val codeRepositoryService = CodeRepositoryServiceRegistrar.getService(repository)
        val condition = RepoCondition(
            projectId = projectId,
            scmCode = repository.scmCode,
            projectName = serverRepository.fullName,
            enablePac = true
        )
        val existsPacRepo = codeRepositoryService.listByCondition(
            repoCondition = condition,
            offset = 0,
            limit = 10
        )?.filter { it.repoHashId != repository.repoHashId }
        if (!existsPacRepo.isNullOrEmpty()) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.ERROR_REPO_URL_HAS_ENABLED_PAC,
                params = arrayOf(existsPacRepo.first().projectId!!)
            )
        }
        val perm = repositoryApiService.findPerm(
            projectId = projectId,
            username = repository.userName,
            authRepository = authRepository
        )
        if (!perm.admin) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.ERROR_MEMBER_LEVEL_LOWER_MASTER,
                params = arrayOf(repository.userName)
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryPacManager::class.java)
    }
}
