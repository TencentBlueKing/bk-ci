/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.service.ServicePipelineYamlResource
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.service.loader.CodeRepositoryServiceRegistrar
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
    private val client: Client
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
        val repository = repositoryDao.get(dslContext = dslContext, repositoryId = repositoryId, projectId = projectId)
        if (repository.enablePac == true) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.ERROR_REPO_REPEATEDLY_ENABLED_PAC
            )
        }
        val codeRepositoryService = CodeRepositoryServiceRegistrar.getServiceByScmType(repository.type)
        codeRepositoryService.pacCheckEnabled(
            projectId = projectId,
            userId = userId,
            record = repository,
            retry = false
        )
        client.get(ServicePipelineYamlResource::class).enable(
            userId = userId,
            projectId = projectId,
            repoHashId = repositoryHashId,
            scmType = ScmType.valueOf(repository.type)
        )
        repositoryDao.enablePac(
            dslContext = dslContext,
            userId = userId,
            projectId = projectId,
            repositoryId = repositoryId
        )
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
            repositoryId = repositoryId,
            syncStatus = syncStatus
        )
    }

    fun getPacRepository(externalId: String, scmType: ScmType): Repository? {
        val codeRepositoryService = CodeRepositoryServiceRegistrar.getServiceByScmType(scmType.name)
        val record = codeRepositoryService.getPacRepository(externalId = externalId) ?: return null
        return codeRepositoryService.compose(record)
    }
}
