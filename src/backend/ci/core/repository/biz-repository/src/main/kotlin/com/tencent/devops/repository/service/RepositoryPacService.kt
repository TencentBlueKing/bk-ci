/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.tencent.devops.repository.service

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.service.ServicePipelineYamlResource
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileSyncReq
import com.tencent.devops.repository.constant.RepositoryConstants
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.pojo.RepoCondition
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.repository.pojo.enums.RepoYamlSyncStatusEnum
import com.tencent.devops.repository.service.hub.ScmFileApiService
import com.tencent.devops.repository.service.hub.ScmRefApiService
import com.tencent.devops.repository.service.hub.ScmRepositoryApiService
import com.tencent.devops.repository.service.loader.CodeRepositoryServiceRegistrar
import com.tencent.devops.scm.api.enums.ScmEventType
import com.tencent.devops.scm.api.pojo.repository.git.GitScmServerRepository
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RepositoryPacService @Autowired constructor(
    private val dslContext: DSLContext,
    private val repositoryDao: RepositoryDao,
    private val repositoryService: RepositoryService,
    private val client: Client,
    private val repositoryApiService: ScmRepositoryApiService,
    private val fileApiService: ScmFileApiService,
    private val refApiService: ScmRefApiService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryPacService::class.java)
    }

    fun getPacProjectId(
        userId: String,
        repoUrl: String,
        repositoryType: ScmType
    ): String? {
        return CodeRepositoryServiceRegistrar.getServiceByScmType(repositoryType.name).getPacProjectId(
            userId = userId, repoUrl = repoUrl
        )
    }

    fun enablePac(userId: String, projectId: String, repositoryHashId: String) {
        logger.info("enable pac|$userId|$projectId|$repositoryHashId")
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        // 权限校验
        repositoryService.validatePermission(
            user = userId,
            projectId = projectId,
            repositoryId = repositoryId,
            authPermission = AuthPermission.EDIT,
            message = MessageUtil.getMessageByLocale(
                messageCode = RepositoryMessageCode.USER_EDIT_PEM_ERROR,
                params = arrayOf(userId, projectId, repositoryHashId),
                language = I18nUtil.getLanguage(userId)
            )
        )
        val repository = repositoryService.serviceGet(
            projectId = projectId,
            repositoryConfig = RepositoryConfigUtils.buildConfig(repositoryHashId, RepositoryType.ID)
        )
        validateEnablePac(userId = userId, projectId = projectId, repository = repository)
        enablePac(userId = userId, projectId = projectId, repository = repository)
    }

    fun getYamlSyncStatus(projectId: String, repositoryHashId: String): String? {
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        val repository = repositoryDao.get(dslContext = dslContext, repositoryId = repositoryId, projectId = projectId)
        return repository.yamlSyncStatus
    }

    fun retry(userId: String, projectId: String, repositoryHashId: String) {
        logger.info("retry pac|$userId|$projectId|$repositoryHashId")
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        // 权限校验
        repositoryService.validatePermission(
            user = userId,
            projectId = projectId,
            repositoryId = repositoryId,
            authPermission = AuthPermission.EDIT,
            message = MessageUtil.getMessageByLocale(
                messageCode = RepositoryMessageCode.USER_EDIT_PEM_ERROR,
                params = arrayOf(userId, projectId, repositoryHashId),
                language = I18nUtil.getLanguage(userId)
            )
        )
        val repository = repositoryDao.get(dslContext = dslContext, repositoryId = repositoryId, projectId = projectId)

        val codeRepositoryService = CodeRepositoryServiceRegistrar.getServiceByScmType(repository.type)
        codeRepositoryService.pacCheckEnabled(
            projectId = projectId,
            userId = userId,
            record = repository,
            retry = true
        )
        client.get(ServicePipelineYamlResource::class).enable(
            userId = userId,
            projectId = projectId,
            repoHashId = repositoryHashId,
            scmType = ScmType.valueOf(repository.type)
        )
    }

    fun disablePac(
        userId: String,
        projectId: String,
        repositoryHashId: String
    ) {
        logger.info("disable repository pac|$userId|$projectId|$repositoryHashId")
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        // 权限校验
        repositoryService.validatePermission(
            user = userId,
            projectId = projectId,
            repositoryId = repositoryId,
            authPermission = AuthPermission.EDIT,
            message = MessageUtil.getMessageByLocale(
                messageCode = RepositoryMessageCode.USER_EDIT_PEM_ERROR,
                params = arrayOf(userId, projectId, repositoryHashId),
                language = I18nUtil.getLanguage(userId)
            )
        )
        val repository = repositoryDao.get(dslContext = dslContext, repositoryId = repositoryId, projectId = projectId)
        if (repository.enablePac == false) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.ERROR_REPO_NOT_ENABLED_PAC
            )
        }
        val codeRepositoryService = CodeRepositoryServiceRegistrar.getServiceByScmType(repository.type)
        val ciDirExists = codeRepositoryService.getGitFileTree(
            projectId = projectId,
            userId = userId,
            record = repository
        ).isNotEmpty()
        if (ciDirExists) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.ERROR_REPO_CI_DIR_EXISTS
            )
        }
        client.get(ServicePipelineYamlResource::class).disable(
            userId = userId,
            projectId = projectId,
            repoHashId = repositoryHashId,
            scmType = ScmType.valueOf(repository.type)
        )
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            repositoryDao.disablePac(
                dslContext = context,
                userId = userId,
                projectId = projectId,
                repositoryId = repositoryId
            )
        }
    }

    fun checkCiDirExists(
        userId: String,
        projectId: String,
        repositoryHashId: String
    ): Boolean {
        logger.info("check ci dir exists|$userId|$projectId|$repositoryHashId")
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        val repository = repositoryDao.get(dslContext = dslContext, repositoryId = repositoryId, projectId = projectId)
        val codeRepositoryService = CodeRepositoryServiceRegistrar.getServiceByScmType(repository.type)
        return codeRepositoryService.getGitFileTree(
            projectId = projectId,
            userId = userId,
            record = repository
        ).isNotEmpty()
    }

    fun getCiSubDir(
        userId: String,
        projectId: String,
        repositoryHashId: String
    ): List<String> {
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        // 权限校验
        repositoryService.validatePermission(
            user = userId,
            projectId = projectId,
            repositoryId = repositoryId,
            authPermission = AuthPermission.VIEW,
            message = MessageUtil.getMessageByLocale(
                messageCode = RepositoryMessageCode.USER_VIEW_PEM_ERROR,
                params = arrayOf(userId, projectId, repositoryHashId),
                language = I18nUtil.getLanguage(userId)
            )
        )
        val repository = repositoryDao.get(dslContext = dslContext, repositoryId = repositoryId, projectId = projectId)
        val codeRepositoryService = CodeRepositoryServiceRegistrar.getServiceByScmType(repository.type)
        return codeRepositoryService.getGitFileTree(
            projectId = projectId,
            userId = userId,
            record = repository
        ).filter { it.type == "tree" }.map { it.name }
    }

    fun updateYamlSyncStatus(
        projectId: String,
        repoHashId: String,
        syncStatus: String
    ) {
        logger.info("update yaml sync status|$projectId|$repoHashId|$syncStatus")
        val repositoryId = HashUtil.decodeOtherIdToLong(repoHashId)
        repositoryDao.updateYamlSyncStatus(
            dslContext = dslContext,
            projectId = projectId,
            repositoryId = repositoryId,
            syncStatus = syncStatus
        )
    }

    fun getPacRepository(externalId: String, scmType: ScmType): Repository? {
        val codeRepositoryService = CodeRepositoryServiceRegistrar.getServiceByScmType(scmType.name)
        val record = codeRepositoryService.getPacRepository(externalId = externalId) ?: return null
        return codeRepositoryService.compose(record)
    }

    /**
     * pac开启验证
     */
    fun validateEnablePac(userId: String, projectId: String, repository: Repository) {
        if (repository.enablePac == true) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.ERROR_REPO_REPEATEDLY_ENABLED_PAC
            )
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
        // 验证是否已经有关联的代码库开启PAC
        val codeRepositoryService = CodeRepositoryServiceRegistrar.getService(repository)
        val condition = RepoCondition(
            projectId = projectId,
            scmCode = repository.scmCode,
            projectName = serverRepository.fullName,
            enablePac = true
        )
        val existsPacRepo = codeRepositoryService.listByCondition(repoCondition = condition, offset = 1, limit = 1)
        if (!existsPacRepo.isNullOrEmpty()) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.ERROR_REPO_URL_HAS_ENABLED_PAC,
                params = arrayOf(existsPacRepo.first().projectId!!)
            )
        }
        // 验证代码库是否有代码库的管理员权限
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

    /**
     * 开启pac
     *
     * 调用前必须先调用validateEnablePac验证合法性
     */
    fun enablePac(userId: String, projectId: String, repository: Repository) {
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
        // 创建webhook,开启PAC时默认注册push和合并请求事件
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
        // 获取yaml文件列表
        val defaultBranch = serverRepository.defaultBranch!!
        val fileTrees = fileApiService.listFileTree(
            projectId = projectId,
            path = RepositoryConstants.CI_DIR_PATH,
            ref = defaultBranch,
            recursive = true,
            authRepository = authRepository
        )
        // 文件列表为空,不需要同步yaml文件
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
            client.get(ServicePipelineYamlResource::class).syncYamlFile(
                userId = userId,
                projectId = projectId,
                yamlFileSyncReq = PipelineYamlFileSyncReq(
                    repository = repository,
                    fileTrees = fileTrees,
                    defaultBranch = defaultBranch,
                    commit = commit
                )
            )
        }
        repositoryService.addGrayRepoWhite(
            scmCode = repository.scmCode,
            pac = true,
            projectNames = listOf(serverRepository.fullName)
        )
    }
}
