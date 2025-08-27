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
 */

package com.tencent.devops.repository.service

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.util.ThreadPoolUtil
import com.tencent.devops.model.repository.tables.records.TRepositoryRecord
import com.tencent.devops.repository.dao.GitTokenDao
import com.tencent.devops.repository.dao.RepoPipelineRefDao
import com.tencent.devops.repository.dao.RepositoryCodeGitDao
import com.tencent.devops.repository.dao.RepositoryCodeGitLabDao
import com.tencent.devops.repository.dao.RepositoryCodeSvnDao
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.dao.RepositoryGithubDao
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.sdk.github.request.GetRepositoryRequest
import com.tencent.devops.repository.sdk.github.service.GithubRepositoryService
import com.tencent.devops.repository.service.github.GithubTokenService
import com.tencent.devops.repository.service.scm.IGitOauthService
import com.tencent.devops.repository.service.scm.IScmOauthService
import com.tencent.devops.repository.service.scm.IScmService
import com.tencent.devops.scm.code.git.CodeGitWebhookEvent
import com.tencent.devops.scm.pojo.GitProjectInfo
import com.tencent.devops.ticket.api.ServiceCredentialResource
import org.jooq.DSLContext
import org.jooq.Record
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Service
@SuppressWarnings("ALL")
class OPRepositoryService @Autowired constructor(
    private val repositoryDao: RepositoryDao,
    private val dslContext: DSLContext,
    private val codeGitLabDao: RepositoryCodeGitLabDao,
    private val codeGitDao: RepositoryCodeGitDao,
    private val codeGithubDao: RepositoryGithubDao,
    private val scmService: IScmService,
    private val scmOauthService: IScmOauthService,
    private val gitOauthService: IGitOauthService,
    private val githubTokenService: GithubTokenService,
    private val githubRepositoryService: GithubRepositoryService,
    private val credentialService: RepoCredentialService,
    private val repoPipelineRefDao: RepoPipelineRefDao,
    private val codeSvnDao: RepositoryCodeSvnDao,
    private val client: Client,
    private val gitTokenDao: GitTokenDao
) {
    fun addHashId() {
        val startTime = System.currentTimeMillis()
        logger.info("OPRepositoryService:begin addHashId-----------")
        val threadPoolExecutor = ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(1),
            Executors.defaultThreadFactory(),
            ThreadPoolExecutor.AbortPolicy()
        )
        threadPoolExecutor.submit {
            logger.info("OPRepositoryService:begin addHashId threadPoolExecutor-----------")
            var offset = 0
            val limit = 1000
            try {
                do {
                    val repoRecords = repositoryDao.getAllRepo(dslContext, limit, offset)
                    val repoSize = repoRecords?.size
                    logger.info("repoSize:$repoSize")
                    repoRecords?.map {
                        val id = it.value1()
                        val hashId = HashUtil.encodeOtherLongId(it.value1())
                        repositoryDao.updateHashId(dslContext, id, hashId)
                    }
                    offset += limit
                } while (repoSize == 1000)
            } catch (e: Exception) {
                logger.warn("OpRepositoryService：addHashId failed | $e ")
            } finally {
                threadPoolExecutor.shutdown()
            }
        }
        logger.info("OPRepositoryService:finish addHashId-----------")
        logger.info("addhashid time cost: ${System.currentTimeMillis() - startTime}")
    }

    @SuppressWarnings("NestedBlockDepth", "MagicNumber")
    fun updateGitDomain(
        oldGitDomain: String,
        newGitDomain: String,
        grayProject: String?,
        grayWeight: Int?,
        grayWhiteProject: String?
    ): Boolean {
        logger.info(
            "start to update gitDomain|oldGitDomain:$oldGitDomain,newGitDomain:$newGitDomain," +
                "grayProject:$grayProject,grayWeight:$grayWeight,grayWhiteProject:$grayWhiteProject"
        )
        var offset = 0
        val limit = 1000
        val grayWhiteProjects = grayWhiteProject?.split(",") ?: emptyList()
        val grayProjects = grayProject?.split(",") ?: emptyList()
        try {
            do {
                logger.info("update gitDomain project range,offset:$offset,limit:$limit")
                val projectIds = repositoryDao.getProjectIdByGitDomain(
                    dslContext = dslContext,
                    gitDomain = oldGitDomain,
                    limit = limit,
                    offset = offset
                )
                val projectSize = projectIds.size
                logger.info("update gitDomain projectSize:$projectSize")
                projectIds.forEach { projectId ->
                    if (isGrayProject(
                            projectId = projectId,
                            grayProjects = grayProjects,
                            grayWhiteProjects = grayWhiteProjects,
                            grayWeight = grayWeight
                        )
                    ) {
                        logger.info("update gitDomain projectId:$projectId")
                        repositoryDao.updateGitDomainByProjectId(
                            dslContext = dslContext,
                            oldGitDomain = oldGitDomain,
                            newGitDomain = newGitDomain,
                            projectId = projectId
                        )
                    }
                }
                offset += limit
            } while (projectSize == 1000)
        } catch (ignore: Exception) {
            logger.warn("Failed to update gitDomain", ignore)
        }
        return true
    }

    @SuppressWarnings("MagicNumber")
    private fun isGrayProject(
        projectId: String,
        grayProjects: List<String>,
        grayWhiteProjects: List<String>,
        grayWeight: Int?
    ): Boolean {
        val hash = (projectId.hashCode() and Int.MAX_VALUE) % 100
        return when {
            grayWhiteProjects.contains(projectId) -> false
            grayProjects.contains(projectId) -> true
            hash <= (grayWeight ?: -1) -> true
            else -> false
        }
    }

    fun updateAction(actionName: String, updateActions: List<() -> Unit>) {
        val startTime = System.currentTimeMillis()
        logger.info("OPRepositoryService:begin $actionName-----------")
        val threadPoolExecutor = ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(1),
            Executors.defaultThreadFactory(),
            ThreadPoolExecutor.AbortPolicy()
        )
        threadPoolExecutor.submit {
            logger.info("OPRepositoryService:begin $actionName threadPoolExecutor-----------")
            try {
                updateActions.forEach {
                    it.invoke()
                }
            } catch (ignored: Exception) {
                logger.warn("OpRepositoryService：$actionName failed | $ignored ")
            } finally {
                threadPoolExecutor.shutdown()
            }
        }
        logger.info("OPRepositoryService:finish $actionName-----------")
        logger.info("$actionName time cost: ${System.currentTimeMillis() - startTime}")
    }

    fun updateGitLabProjectId() {
        var offset = 0
        val limit = 100
        logger.info("OPRepositoryService:begin updateGitLabProjectId")
        do {
            val repoRecords = codeGitLabDao.getAllRepo(dslContext, limit, offset)
            val repoSize = repoRecords?.size
            logger.info("repoSize:$repoSize")
            val repositoryIds = repoRecords?.map { it.repositoryId } ?: ArrayList()
            val repoMap = mutableMapOf<Long, TRepositoryRecord>()
            repositoryDao.getRepoByIds(
                repositoryIds = repositoryIds,
                dslContext = dslContext
            )?.forEach { it ->
                run {
                    repoMap[it.repositoryId] = it
                }
            }
            repoRecords?.forEach {
                val repositoryId = it.repositoryId
                // 基础信息
                val repositoryInfo = repoMap[repositoryId]
                if (repositoryInfo == null) {
                    logger.warn("Invalid gitlab repository info,repositoryId=[$repositoryId]")
                    codeGitLabDao.updateGitProjectId(
                        dslContext = dslContext,
                        id = repositoryId,
                        gitProjectId = 0L
                    )
                    return@forEach
                }
                // 仅处理未删除代码库信息
                if (repositoryInfo.isDeleted) {
                    logger.warn("Invalid gitlab repository info,repository deleted,repositoryId=[$repositoryId]")
                    codeGitLabDao.updateGitProjectId(
                        dslContext = dslContext,
                        id = repositoryId,
                        gitProjectId = 0L
                    )
                    return@forEach
                }
                // 获取token
                val token = getToken(false, it, repositoryInfo)
                val repositoryProjectInfo = getProjectInfo(
                    projectName = it.projectName,
                    token = token,
                    url = repositoryInfo.url,
                    type = ScmType.CODE_GITLAB,
                    isOauth = false
                )
                val gitlabProjectId = repositoryProjectInfo?.id ?: 0L
                codeGitLabDao.updateGitProjectId(
                    dslContext = dslContext,
                    id = repositoryId,
                    gitProjectId = gitlabProjectId
                )
            }
            offset += limit
            // 避免限流，增加一秒休眠时间
            Thread.sleep(1 * 1000)
        } while (repoSize == 100)
        logger.info("OPRepositoryService:end updateGitLabProjectId")
    }

    fun updateCodeGitProjectId() {
        var offset = 0
        val limit = 100
        logger.info("OPRepositoryService:begin updateCodeGitProjectId")
        do {
            val repoRecords = codeGitDao.getAllRepo(dslContext, limit, offset)
            val repoSize = repoRecords?.size
            logger.info("repoSize:$repoSize")
            val repositoryIds = repoRecords?.map { it.repositoryId } ?: ArrayList()
            val repoMap = mutableMapOf<Long, TRepositoryRecord>()
            repositoryDao.getRepoByIds(
                repositoryIds = repositoryIds,
                dslContext = dslContext
            )?.forEach { it ->
                run {
                    repoMap[it.repositoryId] = it
                }
            }
            repoRecords?.forEach {
                val repositoryId = it.repositoryId
                // 基础信息
                val repositoryInfo = repoMap[repositoryId]
                if (repositoryInfo == null) {
                    logger.warn("Invalid codeGit repository info,repositoryId=[$repositoryId]")
                    codeGitDao.updateGitProjectId(
                        dslContext = dslContext,
                        id = repositoryId,
                        gitProjectId = 0L
                    )
                    return@forEach
                }
                // 仅处理未删除代码库信息
                if (repositoryInfo.isDeleted) {
                    logger.warn("Invalid codeGit repository info,repository deleted,repositoryId=[$repositoryId]")
                    codeGitDao.updateGitProjectId(
                        dslContext = dslContext,
                        id = repositoryId,
                        gitProjectId = 0L
                    )
                    return@forEach
                }
                // 是否为OAUTH
                val isOauth = RepoAuthType.OAUTH.name == it.authType
                // 获取token
                val token = getToken(isOauth, it, repositoryInfo)
                val type = if (repositoryInfo.type == ScmType.CODE_GIT.name) ScmType.CODE_GIT else ScmType.CODE_TGIT
                logger.info(
                    "get codeGit project info,projectName=[${it.projectName}]" +
                        "|repoType=[$type]" +
                        "|repoId=[$repositoryId]"
                )
                // 获取代码库信息
                val repositoryProjectInfo = getProjectInfo(
                    projectName = it.projectName,
                    token = token,
                    url = repositoryInfo.url,
                    type = type,
                    isOauth = isOauth
                )
                val gitProjectId = repositoryProjectInfo?.id ?: 0L
                codeGitDao.updateGitProjectId(
                    dslContext = dslContext,
                    id = repositoryId,
                    gitProjectId = gitProjectId
                )
            }
            offset += limit
            // 避免限流，增加一秒休眠时间
            Thread.sleep(1 * 1000)
        } while (repoSize == 100)
        logger.info("OPRepositoryService:end updateCodeGitProjectId")
    }

    @SuppressWarnings("ComplexMethod")
    fun updateCodeGithubProjectId() {
        var offset = 0
        val limit = 100
        logger.info("OPRepositoryService:begin updateCodeGithubProjectId")
        do {
            val repoRecords = codeGithubDao.getAllRepo(
                dslContext = dslContext,
                limit = limit,
                offset = offset
            )
            val repoSize = repoRecords?.size
            logger.info("repoSize:$repoSize")
            val repositoryIds = repoRecords?.map { it.repositoryId } ?: ArrayList()
            val repoMap = mutableMapOf<Long, TRepositoryRecord>()
            repositoryDao.getRepoByIds(
                repositoryIds = repositoryIds,
                dslContext = dslContext
            )?.forEach { it ->
                run {
                    repoMap[it.repositoryId] = it
                }
            }
            repoRecords?.forEach {
                val repositoryId = it.repositoryId
                // 基础信息
                val repositoryInfo = repoMap[repositoryId]
                if (repositoryInfo == null) {
                    logger.warn("Invalid codeGithub repository info,repositoryId=[$repositoryId]")
                    codeGithubDao.updateGitProjectId(
                        dslContext = dslContext,
                        id = repositoryId,
                        gitProjectId = 0L
                    )
                    return@forEach
                }
                // 仅处理未删除代码库信息
                if (repositoryInfo.isDeleted) {
                    logger.warn("Invalid codeGithub repository info,repository deleted,repositoryId=[$repositoryId]")
                    codeGithubDao.updateGitProjectId(
                        dslContext = dslContext,
                        id = repositoryId,
                        gitProjectId = 0L
                    )
                    return@forEach
                }
                // 获取token
                val token = githubTokenService.getAccessToken(it.userName)?.accessToken
                if (token.isNullOrBlank()) {
                    logger.warn("Invalid codeGithub repository token,accessToken is blank|userId[${it.userName}]")
                    codeGithubDao.updateGitProjectId(
                        dslContext = dslContext,
                        id = repositoryId,
                        gitProjectId = 0L
                    )
                    return@forEach
                }
                // 获取代码库信息
                val repositoryProjectInfo = try {
                    githubRepositoryService.getRepository(
                        request = GetRepositoryRequest(
                            repoName = it.projectName
                        ),
                        token = token
                    )
                } catch (ignored: Exception) {
                    logger.warn(
                        "get github project info failed,repositoryId=[${repositoryInfo.repositoryId}] | $ignored"
                    )
                    null
                }
                val gitProjectId = repositoryProjectInfo?.id ?: 0L
                codeGithubDao.updateGitProjectId(
                    dslContext = dslContext,
                    id = repositoryId,
                    gitProjectId = gitProjectId
                )
            }
            offset += limit
            // 避免限流，增加一秒休眠时间
            Thread.sleep(1 * 1000)
        } while (repoSize == 100)
        logger.info("OPRepositoryService:end updateCodeGithubProjectId")
    }

    fun updateGitHookUrl(projectId: String, repositoryId: Long, newHookUrl: String, oldHookUrl: String) {
        if (newHookUrl.isBlank() || oldHookUrl.isBlank()) {
            logger.info("newHookUrl and oldHookUrl can not empty")
            return
        }
        val repository = repositoryDao.get(dslContext = dslContext, repositoryId = repositoryId)
        val gitRepo = codeGitDao.get(dslContext = dslContext, repositoryId = repositoryId)
        val isOauth = RepoAuthType.OAUTH.name == gitRepo.authType
        val token = getToken(isOauth = isOauth, it = gitRepo, repositoryInfo = repository)
        val existHooks = if (isOauth) {
            scmOauthService.getWebHooks(
                projectName = gitRepo.projectName,
                url = repository.url,
                token = token,
                type = ScmType.valueOf(repository.type)
            )
        } else {
            scmService.getWebHooks(
                projectName = gitRepo.projectName,
                url = repository.url,
                token = token,
                type = ScmType.valueOf(repository.type)
            )
        }
        if (existHooks.isNotEmpty()) {
            existHooks.forEach {
                if (it.url.contains(oldHookUrl)) {
                    val event = when {
                        it.pushEvents -> CodeGitWebhookEvent.PUSH_EVENTS.value
                        it.tagPushEvents -> CodeGitWebhookEvent.TAG_PUSH_EVENTS.value
                        it.mergeRequestsEvents -> CodeGitWebhookEvent.MERGE_REQUESTS_EVENTS.value
                        it.issuesEvents -> CodeGitWebhookEvent.ISSUES_EVENTS.value
                        it.noteEvents -> CodeGitWebhookEvent.NOTE_EVENTS.value
                        it.reviewEvents -> CodeGitWebhookEvent.REVIEW_EVENTS.value
                        else -> null
                    }
                    if (isOauth) {
                        scmOauthService.updateWebHook(
                            hookId = it.id,
                            projectName = gitRepo.projectName,
                            url = repository.url,
                            token = token,
                            type = ScmType.valueOf(repository.type),
                            privateKey = null,
                            passPhrase = null,
                            region = null,
                            userName = gitRepo.userName,
                            event = event,
                            hookUrl = newHookUrl
                        )
                    } else {
                        scmService.updateWebHook(
                            hookId = it.id,
                            projectName = gitRepo.projectName,
                            url = repository.url,
                            token = token,
                            type = ScmType.valueOf(repository.type),
                            privateKey = null,
                            passPhrase = null,
                            region = null,
                            userName = gitRepo.userName,
                            event = event,
                            hookUrl = newHookUrl
                        )
                    }
                }
            }
        }
    }

    private fun getToken(isOauth: Boolean, it: Record, repositoryInfo: TRepositoryRecord): String? {
        return try {
            if (isOauth) {
                gitOauthService.getAccessToken(it.get("USER_NAME").toString())?.accessToken
            } else {
                credentialService.getCredentialInfo(
                    projectId = repositoryInfo.projectId,
                    CodeGitRepository(
                        aliasName = repositoryInfo.aliasName,
                        url = repositoryInfo.url,
                        credentialId = it.get("CREDENTIAL_ID").toString(),
                        projectName = it.get("PROJECT_NAME").toString(),
                        userName = repositoryInfo.userId,
                        projectId = repositoryInfo.projectId,
                        repoHashId = repositoryInfo.repositoryHashId,
                        authType = null,
                        gitProjectId = 0L
                    )
                ).token
            }
        } catch (e: Exception) {
            logger.warn(
                "get git credential info failed,set token to Empty String," +
                    "repositoryId=[${repositoryInfo.repositoryId}] | $e "
            )
            ""
        }
    }

    private fun getProjectInfo(
        projectName: String,
        token: String?,
        url: String,
        type: ScmType,
        isOauth: Boolean
    ): GitProjectInfo? {
        return try {
            if (isOauth) {
                scmOauthService.getProjectInfo(
                    projectName = projectName,
                    url = url,
                    type = type,
                    token = token
                )
            } else {
                scmService.getProjectInfo(
                    projectName = projectName,
                    url = url,
                    type = type,
                    token = token
                )
            }
        } catch (e: Exception) {
            logger.warn("get codeGit project info failed,projectName=[$projectName] | $e ")
            null
        }
    }

    fun removeRepositoryPipelineRef(projectId: String, repoHashId: String) {
        logger.info("start remove repository pipeline ref,projectId=[$projectId]|repoHashId=[$repoHashId]")
        val repositoryId = HashUtil.decodeOtherIdToLong(repoHashId)
        val repository = repositoryDao.get(dslContext = dslContext, repositoryId = repositoryId)
        val counts = repoPipelineRefDao.removeRepositoryPipelineRefsById(
            repoId = repository.repositoryId,
            dslContext = dslContext
        )
        logger.info("end remove repository pipeline ref,change counts=[$counts]")
    }

    fun updateRepoCredentialType(projectId: String?, repoHashId: String?) {
        var offset = 0
        val limit = 100
        logger.info("OPRepositoryService:begin updateRepoCredentialType")
        // projectId to Map<credentialId, credentialType>
        val credentialCache = mutableMapOf<String, MutableMap<String, String>>()
        do {
            // 获取仓库列表（仅刷新GIT/TGIT/SVN类型）
            val repoList = repositoryDao.list(
                dslContext = dslContext,
                projectId = projectId,
                repoHashId = repoHashId,
                repositoryTypes = listOf(
                    ScmType.CODE_GIT.name,
                    ScmType.CODE_TGIT.name,
                    ScmType.CODE_SVN.name
                ),
                limit = limit,
                offset = offset
            )
            // 移除过期缓存
            val projectIds = repoList.groupBy { it.projectId }.keys
            credentialCache.keys.subtract(projectIds).toSet().forEach {
                logger.info("remove expired cache projectId=$it")
                credentialCache.remove(it)
            }
            val repoProjectMap = repoList.associate { it.repositoryId to it.projectId }
            // 仓库分类
            val repoIdsMap = repoList.groupBy {
                if (it.type == ScmType.CODE_SVN.name) "SVN" else "GIT"
            }.mapValues { it.value.map { repoInfo -> repoInfo.repositoryId } }
            val svnRepoList = codeSvnDao.list(
                dslContext = dslContext,
                repositoryIds = repoIdsMap["SVN"]?.toSet() ?: setOf()
            )
            val gitRepoList = codeGitDao.list(
                dslContext = dslContext,
                repositoryIds = repoIdsMap["GIT"]?.toSet() ?: setOf()
            )?.filter { it.authType != RepoAuthType.OAUTH.name } ?: listOf()
            // projectId to set(credentialId)
            val credentialMap = svnRepoList.groupBy { repoProjectMap[it.repositoryId] }
                    .mapValues { it.value.map { svnRepo -> svnRepo.credentialId }.toMutableSet() }
                    .toMutableMap()
            gitRepoList.forEach {
                val repoProjectId = repoProjectMap[it.repositoryId]!!
                if (credentialMap[repoProjectId] == null) {
                    credentialMap[repoProjectId] = mutableSetOf()
                }
                credentialMap[repoProjectId]!!.add(it.credentialId)
            }
            // 缓存凭据类型
            credentialMap.forEach {
                val key = it.key!!
                if (credentialCache[key] == null) {
                    credentialCache[key] = mutableMapOf()
                }
                val credentialIds = it.value
                        .filter { credentialId -> !credentialCache[key]!!.contains(credentialId) }
                        .toSet()
                credentialCache[key]!!.putAll(getCredentialType(key, credentialIds))
            }
            logger.info("svnRepoList=${svnRepoList.size}|gitRepoList=${gitRepoList.size}")
            // 更新SVN仓库的凭据类型
            svnRepoList.forEach {
                val repoProjectId = repoProjectMap[it.repositoryId]!!
                val credentialType = credentialCache[repoProjectId]?.get(it.credentialId)
                if (credentialType.isNullOrBlank()) {
                    logger.warn("skip|credentialType is null|projectId=$repoProjectId|credentialId=${it.credentialId}")
                    return@forEach
                }
                val changeCount = codeSvnDao.updateCredentialType(
                    dslContext = dslContext,
                    repositoryId = it.repositoryId,
                    credentialType = credentialCache[repoProjectId]?.get(it.credentialId) ?: ""
                )
                logger.info("update svn credential type|${it.repositoryId}|changeCount=$changeCount")
            }
            // 更新GIT仓库的凭据类型
            gitRepoList.forEach {
                val repoProjectId = repoProjectMap[it.repositoryId]!!
                val credentialType = credentialCache[repoProjectId]?.get(it.credentialId)
                if (credentialType.isNullOrBlank()) {
                    logger.warn("skip|credentialType is null|projectId=$repoProjectId|credentialId=${it.credentialId}")
                    return@forEach
                }
                val changeCount = codeGitDao.updateCredentialType(
                    dslContext = dslContext,
                    repositoryId = it.repositoryId,
                    credentialType = credentialCache[repoProjectId]?.get(it.credentialId) ?: ""
                )
                logger.info("update git credential type|${it.repositoryId}|changeCount=$changeCount")
            }

            offset += limit
            // 避免限流，增加一秒休眠时间
            Thread.sleep(1 * 1000)
        } while (repoList.size == 100)
        logger.info("OPRepositoryService:end updateRepoCredentialType")
    }

    fun updateRepoScmCode(projectId: String?, repoHashId: String?) {
        var offset = 0
        val limit = 100
        logger.info("OPRepositoryService:begin updateRepoScmCode")
        do {
            // 获取仓库列表
            val repoList = repositoryDao.list(
                dslContext = dslContext,
                projectId = projectId,
                repoHashId = repoHashId,
                repositoryTypes = null,
                nullScmCode = true,
                limit = limit,
                offset = offset
            )
            repoList.chunked(25) {
                repositoryDao.updateScmCode(
                    dslContext = dslContext,
                    repositoryId = it.map { it.repositoryId }.toSet()
                )
            }
            // 避免限流，增加一秒休眠时间
            Thread.sleep(1 * 1000)
        } while (repoList.size == 100)
        logger.info("OPRepositoryService:end updateRepoCredentialType")
    }

    private fun getCredentialType(projectId: String, credentialIds: Set<String>): Map<String, String> {
        val credentialInfos = try {
            client.get(ServiceCredentialResource::class)
                    .getCredentialByIds(
                        projectId = projectId,
                        credentialId = credentialIds
                    ).data ?: setOf()
        } catch (ignored: Exception) {
            logger.warn("failed to get credential info, projectId=$projectId, credentialIds=$credentialIds", ignored)
            setOf()
        }
        return credentialInfos.associate { it.credentialId to it.credentialType.name }
    }

    fun addGithubOperator() {
        ThreadPoolUtil.submitAction(
            action = {
                val limit = 100
                do {
                    val list = githubTokenService.listEmptyOperator(
                        dsl = dslContext,
                        limit = limit
                    )
                    githubTokenService.updateOperator(
                        dsl = dslContext,
                        userIds = list.map { it.userId }.toSet()
                    )
                } while (list.size == limit)
            },
            actionTitle = "add github operator"
        )
    }

    fun addGitOperator() {
        ThreadPoolUtil.submitAction(
            action = {
                val limit = 100
                do {
                    val list = gitTokenDao.listEmptyOperator(
                        dslContext = dslContext,
                        limit = limit
                    )
                    gitTokenDao.updateOperator(
                        dslContext = dslContext,
                        userIds = list.map { it.userId }.toSet()
                    )
                } while (list.size == limit)
            },
            actionTitle = "add git operator"
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OPRepositoryService::class.java)
    }
}
