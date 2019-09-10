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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.repository.service

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.model.repository.tables.records.TRepositoryRecord
import com.tencent.devops.process.api.ServiceBuildResource
import com.tencent.devops.repository.dao.RepositoryCodeGitDao
import com.tencent.devops.repository.dao.RepositoryCodeGitLabDao
import com.tencent.devops.repository.dao.RepositoryCodeSvnDao
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.dao.RepositoryGithubDao
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.CodeTGitRepository
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.RepositoryInfoWithPermission
import com.tencent.devops.repository.pojo.enums.CodeSvnRegion
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

@Service
class RepositoryService @Autowired constructor(
    private val repositoryDao: RepositoryDao,
    private val repositoryCodeSvnDao: RepositoryCodeSvnDao,
    private val repositoryCodeGitDao: RepositoryCodeGitDao,
    private val repositoryCodeGitLabDao: RepositoryCodeGitLabDao,
    private val repositoryGithubDao: RepositoryGithubDao,
    private val dslContext: DSLContext,
    private val client: Client,
    private val repositoryPermissionService: RepositoryPermissionService
) {

    fun hasAliasName(projectId: String, repositoryHashId: String?, aliasName: String): Boolean {
        val repositoryId = if (repositoryHashId != null) HashUtil.decodeOtherIdToLong(repositoryHashId) else 0L
        if (repositoryId != 0L) {
            val record = repositoryDao.get(dslContext, repositoryId, projectId)
            if (record.aliasName == aliasName) return false
        }
        return repositoryDao.countByProjectAndAliasName(dslContext, projectId, repositoryId, aliasName) != 0L
    }

    fun userCreate(userId: String, projectId: String, repository: Repository): String {
        repositoryPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            bkAuthPermission = BkAuthPermission.CREATE,
            message = "用户($userId)在工程($projectId)下没有代码库创建权限"
        )
        val repositoryId = createRepository(repository, projectId, userId)
        return HashUtil.encodeOtherLongId(repositoryId)
    }

    private fun createRepository(repository: Repository, projectId: String, userId: String): Long {
        if (!repository.isLegal()) {
            logger.warn("The repository($repository) is illegal")
            throw OperationException("代码仓库路径不正确，仓库路径应该以(${repository.getStartPrefix()})开头")
        }

        if (hasAliasName(projectId, null, repository.aliasName)) {
            throw OperationException("代码库别名（${repository.aliasName}）已存在")
        }

        val repositoryId = dslContext.transactionResult { configuration ->
            val transactionContext = DSL.using(configuration)
            val repositoryId = when (repository) {
                is CodeSvnRepository -> {
                    val repositoryId = repositoryDao.create(
                        transactionContext,
                        projectId,
                        userId,
                        repository.aliasName,
                        repository.getFormatURL(),
                        ScmType.CODE_SVN
                    )
                    repositoryCodeSvnDao.create(
                        transactionContext,
                        repositoryId,
                        repository.region,
                        repository.projectName,
                        repository.userName,
                        repository.credentialId,
                        repository.svnType
                    )
                    repositoryId
                }
                is CodeGitRepository -> {
                    val repositoryId = repositoryDao.create(
                        transactionContext,
                        projectId,
                        userId,
                        repository.aliasName,
                        repository.getFormatURL(),
                        ScmType.CODE_GIT
                    )
                    repositoryCodeGitDao.create(
                        transactionContext,
                        repositoryId,
                        repository.projectName,
                        repository.userName,
                        repository.credentialId,
                        repository.authType
                    )
                    repositoryId
                }
                is CodeTGitRepository -> {
                    val repositoryId = repositoryDao.create(
                        transactionContext,
                        projectId,
                        userId,
                        repository.aliasName,
                        repository.getFormatURL(),
                        ScmType.CODE_TGIT
                    )
                    repositoryCodeGitDao.create(
                        transactionContext,
                        repositoryId,
                        repository.projectName,
                        repository.userName,
                        repository.credentialId,
                        repository.authType
                    )
                    repositoryId
                }
                is CodeGitlabRepository -> {
                    val repositoryId = repositoryDao.create(
                        transactionContext,
                        projectId,
                        userId,
                        repository.aliasName,
                        repository.getFormatURL(),
                        ScmType.CODE_GITLAB
                    )
                    repositoryCodeGitLabDao.create(
                        transactionContext,
                        repositoryId,
                        repository.projectName,
                        repository.userName,
                        repository.credentialId
                    )
                    repositoryId
                }
                is GithubRepository -> {
                    val repositoryId = repositoryDao.create(
                        transactionContext,
                        projectId,
                        userId,
                        repository.aliasName,
                        repository.getFormatURL(),
                        ScmType.GITHUB
                    )
                    repositoryGithubDao.create(dslContext, repositoryId, repository.projectName, userId)
                    repositoryId
                }
                else -> throw InvalidParamException("Unknown repository type")
            }
            repositoryId
        }

        repositoryPermissionService.createResource(userId, projectId, repositoryId, repository.aliasName)
        return repositoryId
    }

    fun userGet(userId: String, projectId: String, repositoryConfig: RepositoryConfig): Repository {
        val repository = getRepository(projectId, repositoryConfig)

        val repositoryId = repository.repositoryId
        repositoryPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            repositoryId = repositoryId,
            bkAuthPermission = BkAuthPermission.VIEW,
            message = "用户($userId)在工程($projectId)下没有代码库(${repository.aliasName})查看权限"
        )
        return compose(repository)
    }

    fun serviceGet(projectId: String, repositoryConfig: RepositoryConfig): Repository {
        return compose(getRepository(projectId, repositoryConfig))
    }

    private fun getRepository(projectId: String, repositoryConfig: RepositoryConfig): TRepositoryRecord {
        logger.info("[$projectId]Start to get the repository - ($repositoryConfig)")
        return when (repositoryConfig.repositoryType) {
            RepositoryType.ID -> {
                val repositoryId = HashUtil.decodeOtherIdToLong(repositoryConfig.getRepositoryId())
                repositoryDao.get(dslContext, repositoryId, projectId)
            }
            RepositoryType.NAME -> {
                repositoryDao.getByName(dslContext, projectId, repositoryConfig.getRepositoryId())
            }
        }
    }

    private fun compose(repository: TRepositoryRecord): Repository {
        val repositoryId = repository.repositoryId
        val hashId = HashUtil.encodeOtherLongId(repository.repositoryId)
        return when (repository.type) {
            ScmType.CODE_SVN.name -> {
                val record = repositoryCodeSvnDao.get(dslContext, repositoryId)
                CodeSvnRepository(
                    repository.aliasName,
                    repository.url,
                    record.credentialId,
                    if (record.region.isNullOrBlank()) {
                        CodeSvnRegion.TC
                    } else {
                        CodeSvnRegion.valueOf(record.region)
                    },
                    record.projectName,
                    record.userName,
                    repository.projectId,
                    hashId,
                    record.svnType
                )
            }
            ScmType.CODE_GIT.name -> {
                val record = repositoryCodeGitDao.get(dslContext, repositoryId)
                CodeGitRepository(
                    repository.aliasName,
                    repository.url,
                    record.credentialId,
                    record.projectName,
                    record.userName,
                    RepoAuthType.parse(record.authType),
                    repository.projectId,
                    HashUtil.encodeOtherLongId(repository.repositoryId)
                )
            }
            ScmType.CODE_TGIT.name -> {
                val record = repositoryCodeGitDao.get(dslContext, repositoryId)
                CodeTGitRepository(
                    repository.aliasName,
                    repository.url,
                    record.credentialId,
                    record.projectName,
                    record.userName,
                    RepoAuthType.parse(record.authType),
                    repository.projectId,
                    hashId

                )
            }
            ScmType.CODE_GITLAB.name -> {
                val record = repositoryCodeGitLabDao.get(dslContext, repositoryId)
                CodeGitlabRepository(
                    repository.aliasName,
                    repository.url,
                    record.credentialId,
                    record.projectName,
                    record.userName,
                    repository.projectId,
                    hashId
                )
            }
            ScmType.GITHUB.name -> {
                val record = repositoryGithubDao.get(dslContext, repositoryId)
                GithubRepository(
                    repository.aliasName,
                    repository.url,
                    repository.userId,
                    record.projectName,
                    repository.projectId,
                    hashId
                )
            }
            else -> throw InvalidParamException("Unknown repository type")
        }
    }

    fun buildGet(buildId: String, repositoryConfig: RepositoryConfig): Repository {
        val buildBasicInfoResult = client.get(ServiceBuildResource::class).serviceBasic(buildId)
        if (buildBasicInfoResult.isNotOk()) {
            throw RemoteServiceException("Failed to build the basic information based on the buildId")
        }
        val buildBasicInfo = buildBasicInfoResult.data
            ?: throw RemoteServiceException("Failed to build the basic information based on the buildId")
        return serviceGet(buildBasicInfo.projectId, repositoryConfig)
    }

    fun userEdit(userId: String, projectId: String, repositoryHashId: String, repository: Repository) {
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        repositoryPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            repositoryId = repositoryId,
            bkAuthPermission = BkAuthPermission.EDIT,
            message = "用户($userId)在工程($projectId)下没有代码库($repositoryHashId)编辑权限"
        )
        val record = repositoryDao.get(dslContext, repositoryId, projectId)
        if (record.projectId != projectId) {
            throw NotFoundException("Repository is not part of the project")
        }

        if (!repository.isLegal()) {
            logger.warn("The repository($repository) is illegal")
            throw OperationException("代码仓库路径不正确，仓库路径应该以(${repository.getStartPrefix()})开头")
        }

        if (hasAliasName(projectId, repositoryHashId, repository.aliasName)) {
            throw OperationException("代码库别名（${repository.aliasName}）已存在")
        }

        // 判断仓库类型是否一致
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            when (record.type) {
                ScmType.CODE_GIT.name -> {
                    if (repository !is CodeGitRepository) {
                        throw OperationException("无效的GIT仓库")
                    }
                    repositoryDao.edit(
                        transactionContext,
                        repositoryId,
                        repository.aliasName,
                        repository.getFormatURL()
                    )
                    repositoryCodeGitDao.edit(
                        transactionContext,
                        repositoryId,
                        repository.projectName,
                        repository.userName,
                        repository.credentialId,
                        repository.authType
                    )
                }
                ScmType.CODE_TGIT.name -> {
                    if (repository !is CodeTGitRepository) {
                        throw OperationException("无效的TGIT仓库")
                    }
                    repositoryDao.edit(
                        transactionContext,
                        repositoryId,
                        repository.aliasName,
                        repository.getFormatURL()
                    )
                    repositoryCodeGitDao.edit(
                        transactionContext,
                        repositoryId,
                        repository.projectName,
                        repository.userName,
                        repository.credentialId,
                        repository.authType
                    )
                }
                ScmType.CODE_SVN.name -> {
                    if (repository !is CodeSvnRepository) {
                        throw OperationException("无效的SVN仓库")
                    }
                    repositoryDao.edit(
                        transactionContext,
                        repositoryId,
                        repository.aliasName,
                        repository.getFormatURL()
                    )
                    repositoryCodeSvnDao.edit(
                        transactionContext,
                        repositoryId,
                        repository.region,
                        repository.projectName,
                        repository.userName,
                        repository.credentialId,
                        repository.svnType
                    )
                }
                ScmType.CODE_GITLAB.name -> {
                    if (repository !is CodeGitlabRepository) {
                        throw OperationException("无效的GITLAB仓库")
                    }
                    repositoryDao.edit(
                        transactionContext,
                        repositoryId,
                        repository.aliasName,
                        repository.getFormatURL()
                    )
                    repositoryCodeGitLabDao.edit(
                        transactionContext,
                        repositoryId,
                        repository.projectName,
                        repository.userName,
                        repository.credentialId
                    )
                }
                ScmType.GITHUB.name -> {
                    if (repository !is GithubRepository) {
                        throw OperationException("无效的GITHUB仓库")
                    }
                    repositoryDao.edit(
                        transactionContext,
                        repositoryId,
                        repository.aliasName,
                        repository.getFormatURL()
                    )
                    repositoryGithubDao.edit(dslContext, repositoryId, repository.projectName, repository.userName)
                }
            }
        }
        repositoryPermissionService.editResource(projectId, repositoryId, repository.aliasName)
    }

    fun serviceList(
        projectId: String,
        scmType: ScmType?
    ): List<RepositoryInfoWithPermission> {
        val dbRecords = repositoryDao.listByProject(dslContext, projectId, scmType)
        val gitRepoIds = dbRecords.filter { it.type == "CODE_GIT" }.map { it.repositoryId }.toSet()
        val gitAuthMap = repositoryCodeGitDao.list(dslContext, gitRepoIds)?.map { it.repositoryId to it }?.toMap()

        return dbRecords.map { repository ->
            val authType = gitAuthMap?.get(repository.repositoryId)?.authType ?: "SSH"
            RepositoryInfoWithPermission(
                HashUtil.encodeOtherLongId(repository.repositoryId),
                repository.aliasName,
                repository.url,
                ScmType.valueOf(repository.type),
                repository.updatedTime.timestamp(),
                true,
                true,
                authType
            )
        }.toList()
    }

    fun serviceCount(
        projectId: Set<String>,
        repositoryHashId: String,
        repositoryType: ScmType?,
        aliasName: String
    ): Long {
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        val repoIds = if (repositoryId == 0L) setOf() else setOf(repositoryId)
        return repositoryDao.count(dslContext, projectId, repositoryType, repoIds, aliasName)
    }

    fun userList(
        userId: String,
        projectId: String,
        repositoryType: ScmType?,
        offset: Int,
        limit: Int
    ): Pair<SQLPage<RepositoryInfoWithPermission>, Boolean> {
        val hasCreatePermission = repositoryPermissionService.hasPermission(userId, projectId, BkAuthPermission.CREATE)
        val permissionToListMap =
            repositoryPermissionService.filterRepositories(
                userId = userId,
                projectId = projectId,
                bkAuthPermissions = setOf(BkAuthPermission.LIST, BkAuthPermission.EDIT, BkAuthPermission.DELETE)
            )
        val hasListPermissionRepoList = permissionToListMap[BkAuthPermission.LIST]!!
        val hasEditPermissionRepoList = permissionToListMap[BkAuthPermission.EDIT]!!
        val hasDeletePermissionRepoList = permissionToListMap[BkAuthPermission.DELETE]!!

        val count =
            repositoryDao.countByProject(dslContext, projectId, repositoryType, hasListPermissionRepoList.toSet())
        val repositoryRecordList = repositoryDao.listByProject(
            dslContext,
            projectId,
            repositoryType,
            hasListPermissionRepoList.toSet(),
            offset,
            limit
        )
        val gitRepoIds =
            repositoryRecordList.filter { it.type == ScmType.CODE_GIT.name }.map { it.repositoryId }.toSet()
        val gitAuthMap = repositoryCodeGitDao.list(dslContext, gitRepoIds)?.map { it.repositoryId to it }?.toMap()

        val svnRepoIds =
            repositoryRecordList.filter { it.type == ScmType.CODE_SVN.name }.map { it.repositoryId }.toSet()
        val svnRepoRecords = repositoryCodeSvnDao.list(dslContext, svnRepoIds).map { it.repositoryId to it }.toMap()

        val repositoryList = repositoryRecordList.map {
            val hasEditPermission = hasEditPermissionRepoList.contains(it.repositoryId)
            val hasDeletePermission = hasDeletePermissionRepoList.contains(it.repositoryId)
            val authType = gitAuthMap?.get(it.repositoryId)?.authType ?: "SSH"
            val svnType = svnRepoRecords[it.repositoryId]?.svnType
            RepositoryInfoWithPermission(
                HashUtil.encodeOtherLongId(it.repositoryId),
                it.aliasName,
                it.url,
                ScmType.valueOf(it.type),
                it.updatedTime.timestamp(),
                hasEditPermission,
                hasDeletePermission,
                authType,
                svnType
            )
        }
        return Pair(SQLPage(count, repositoryList), hasCreatePermission)
    }

    fun hasPermissionList(
        userId: String,
        projectId: String,
        repositoryType: ScmType?,
        bkAuthPermission: BkAuthPermission,
        offset: Int,
        limit: Int
    ): SQLPage<RepositoryInfo> {
        val hasPermissionList = repositoryPermissionService.filterRepository(userId, projectId, bkAuthPermission)

        val count = repositoryDao.countByProject(dslContext, projectId, repositoryType, hasPermissionList.toSet())
        val repositoryRecordList =
            repositoryDao.listByProject(dslContext, projectId, repositoryType, hasPermissionList.toSet(), offset, limit)
        val repositoryList = repositoryRecordList.map {
            RepositoryInfo(
                HashUtil.encodeOtherLongId(it.repositoryId),
                it.aliasName,
                it.url,
                ScmType.valueOf(it.type),
                it.updatedTime.timestamp()
            )
        }
        return SQLPage(count, repositoryList)
    }

    fun userDelete(userId: String, projectId: String, repositoryHashId: String) {
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        repositoryPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            repositoryId = repositoryId,
            bkAuthPermission = BkAuthPermission.DELETE,
            message = "用户($userId)在工程($projectId)下没有代码库($repositoryHashId)删除权限"
        )

        val record = repositoryDao.get(dslContext, repositoryId, projectId)
        if (record.projectId != projectId) {
            throw NotFoundException("Repository is not part of the project")
        }

        repositoryPermissionService.deleteResource(projectId, repositoryId)
        repositoryDao.delete(dslContext, repositoryId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryService::class.java)
    }
}
