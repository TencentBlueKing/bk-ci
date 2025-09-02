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

package com.tencent.devops.repository.service.hub

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.GithubCheckRuns
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.repository.sdk.github.pojo.CheckRunOutput
import com.tencent.devops.repository.service.RepositoryOauthService
import com.tencent.devops.repository.service.RepositoryScmConfigService
import com.tencent.devops.repository.service.RepositoryService
import com.tencent.devops.repository.service.ScmApiManager
import com.tencent.devops.repository.service.github.IGithubService
import com.tencent.devops.repository.service.oauth2.Oauth2TokenStoreManager
import com.tencent.devops.scm.api.enums.CheckRunConclusion
import com.tencent.devops.scm.api.enums.CheckRunStatus
import com.tencent.devops.scm.api.enums.ScmEventType
import com.tencent.devops.scm.api.enums.ScmProviderCodes
import com.tencent.devops.scm.api.pojo.BranchListOptions
import com.tencent.devops.scm.api.pojo.CheckRun
import com.tencent.devops.scm.api.pojo.CheckRunInput
import com.tencent.devops.scm.api.pojo.CommentInput
import com.tencent.devops.scm.api.pojo.Hook
import com.tencent.devops.scm.api.pojo.HookEvents
import com.tencent.devops.scm.api.pojo.HookInput
import com.tencent.devops.scm.api.pojo.ListOptions
import com.tencent.devops.scm.api.pojo.Perm
import com.tencent.devops.scm.api.pojo.Reference
import com.tencent.devops.scm.api.pojo.RepoListOptions
import com.tencent.devops.scm.api.pojo.TagListOptions
import com.tencent.devops.scm.api.pojo.auth.AccessTokenScmAuth
import com.tencent.devops.scm.api.pojo.auth.IScmAuth
import com.tencent.devops.scm.api.pojo.repository.ScmProviderRepository
import com.tencent.devops.scm.api.pojo.repository.ScmServerRepository
import com.tencent.devops.scm.api.pojo.repository.svn.SvnScmProviderRepository
import com.tencent.devops.scm.config.GitConfig
import com.tencent.devops.scm.config.P4Config
import com.tencent.devops.scm.config.ScmConfig
import com.tencent.devops.scm.spring.properties.ScmProviderProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 代码源-仓库api
 */
@Service
class ScmRepositoryApiService @Autowired constructor(
    private val repositoryService: RepositoryService,
    private val providerRepositoryFactory: ScmProviderRepositoryFactory,
    private val repositoryScmConfigService: RepositoryScmConfigService,
    private val scmApiManager: ScmApiManager,
    private val oauth2TokenStoreManager: Oauth2TokenStoreManager,
    private val serverRepositoryFactory: ScmServerRepositoryFactory,
    private val gitConfig: GitConfig,
    private val repositoryOauthService: RepositoryOauthService,
    private val p4Config: P4Config,
    private val scmConfig: ScmConfig,
    private val githubService: IGithubService
) : AbstractScmApiService(
    repositoryService = repositoryService,
    providerRepositoryFactory = providerRepositoryFactory,
    repositoryScmConfigService = repositoryScmConfigService
) {

    @Value("\${scm.webhook.url:#{null}}")
    private val webhookUrl: String = ""

    fun findRepository(
        projectId: String,
        authRepository: AuthRepository
    ): ScmServerRepository {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            scmApiManager.findRepository(
                providerProperties = providerProperties,
                providerRepository = providerRepository
            )
        }
    }

    fun listRepoBaseInfo(
        userId: String,
        projectId: String,
        scmCode: String,
        search: String?,
        oauthUserId: String?
    ): AuthorizeResult {
        // 若指定指定授权账号，则以目标账号权限拉取仓库列表
        val oauthTokenInfo = oauth2TokenStoreManager.get(
            userId = if (oauthUserId.isNullOrBlank()) {
                userId
            } else {
                oauthUserId
            },
            scmCode = scmCode
        ) ?: run {
            val redirectUrl = gitConfig.redirectUrl + "/$projectId/?scmCode=$scmCode&popupScm"
            val oauthUrl = repositoryOauthService.oauthUrl(
                userId = userId,
                scmCode = scmCode,
                redirectUrl = redirectUrl,
                oauthUserId = userId
            )
            return AuthorizeResult(status = 403, url = oauthUrl.url)
        }
        val opts = RepoListOptions(
            repoName = search,
            page = 1,
            pageSize = 20
        )
        val projects = listRepository(
            scmCode = scmCode,
            auth = AccessTokenScmAuth(oauthTokenInfo.accessToken),
            opts = opts
        ).map {
            serverRepositoryFactory.create(it)
        }.toMutableList()
        return AuthorizeResult(status = 200, project = projects)
    }

    fun listRepository(
        scmCode: String,
        auth: IScmAuth,
        opts: RepoListOptions
    ): List<ScmServerRepository> {
        return invokeApi(scmCode = scmCode) { providerProperties ->
            scmApiManager.listRepository(
                providerProperties = providerProperties,
                auth = auth,
                opts = opts
            )
        }
    }

    fun findPerm(
        projectId: String,
        username: String,
        authRepository: AuthRepository
    ): Perm {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            scmApiManager.findPerm(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                username = username
            )
        }
    }

    fun findBranches(
        projectId: String,
        authRepository: AuthRepository,
        search: String?,
        page: Int,
        pageSize: Int
    ): List<Reference> {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            scmApiManager.listBranches(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                opts = BranchListOptions(
                    search = search,
                    page = page,
                    pageSize = pageSize
                )
            )
        }
    }

    fun getBranch(
        projectId: String,
        authRepository: AuthRepository,
        branch: String
    ): Reference? {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            scmApiManager.findBranch(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                name = branch
            )
        }
    }

    fun findTags(
        projectId: String,
        authRepository: AuthRepository,
        search: String?,
        page: Int,
        pageSize: Int
    ): List<Reference> {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            scmApiManager.findTags(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                opts = TagListOptions(
                    search = search,
                    page = page,
                    pageSize = pageSize
                )
            )
        }
    }

    /**
     * 批量创建hook,蓝盾每个事件一条hook记录,方便用户查询webhook历史
     *
     * @param event webhook事件,如果能够在ScmEventType中找到,则转换成HookEvent,否则转换成nativeEvent
     */
    fun createHook(
        projectId: String,
        events: List<String>,
        secret: String? = null,
        authRepository: AuthRepository,
        scmType: ScmType,
        scmCode: String
    ): List<Hook> {
        val hookUrl = getHookUrl(scmType, scmCode)
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            // SVN 注册webhook时需要指定子路径
            val subPath = if (providerRepository is SvnScmProviderRepository) {
                providerRepository.url.substringAfter(
                    providerRepository.projectIdOrPath.toString(),
                    "/"
                ).ifEmpty { "/" }
            } else null
            val existsEvents = existsHookEvents(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                hookUrl = hookUrl,
                subPath = subPath
            )
            logger.info("create hook|projectId:$projectId|hookUrl:$hookUrl|events:$events|existsEvents:$existsEvents")
            val hooks = mutableListOf<Hook>()
            events.forEach { event ->
                val hook = createHook(
                    providerProperties = providerProperties,
                    providerRepository = providerRepository,
                    hookUrl = hookUrl,
                    event = event,
                    secret = secret,
                    existsEvents = existsEvents,
                    subPath = subPath
                )
                hooks.add(hook)
            }
            hooks
        }
    }

    fun createCheckRun(
        projectId: String,
        repositoryType: RepositoryType,
        repoId: String,
        checkRunInput: CheckRunInput
    ): CheckRun {
        val repo = getRepo(projectId, repositoryType, repoId)
        return when (val providerCode = repositoryScmConfigService.get(repo.scmCode).providerCode) {
            ScmProviderCodes.TGIT.name, ScmProviderCodes.GITEE.name -> {
                invokeApi(
                    projectId = projectId,
                    authRepository = AuthRepository(repo)
                ) { providerProperties, providerRepository ->
                    scmApiManager.createCheckRun(
                        providerProperties = providerProperties,
                        providerRepository = providerRepository,
                        input = checkRunInput
                    )
                }
            }

            // github 暂时没对接sdk，先走老接口创建
            ScmProviderCodes.GITHUB.name -> {
                githubService.addCheckRuns(
                    token = oauth2TokenStoreManager.get(
                        userId = repo.userName,
                        scmCode = repo.scmCode
                    )?.accessToken ?: "",
                    projectName = repo.projectName,
                    checkRuns = checkRunInput.convertGithubCheckRun()
                ).let {
                    CheckRun(
                        id = it.id,
                        name = checkRunInput.name,
                        status = checkRunInput.status,
                        summary = checkRunInput.output?.summary,
                        detailsUrl = checkRunInput.detailsUrl,
                        conclusion = checkRunInput.conclusion,
                        detail = checkRunInput.output?.text
                    )
                }
            }

            else -> {
                throw UnsupportedOperationException("repo($providerCode) unsupported create checkRun")
            }
        }
    }

    fun updateCheckRun(
        projectId: String,
        repositoryType: RepositoryType,
        repoId: String,
        checkRunInput: CheckRunInput
    ): CheckRun {
        val repo = getRepo(projectId, repositoryType, repoId)
        return when (val providerCode = repositoryScmConfigService.get(repo.scmCode).providerCode) {
            ScmProviderCodes.TGIT.name, ScmProviderCodes.GITEE.name -> {
                invokeApi(
                    projectId = projectId,
                    authRepository = AuthRepository(repo)
                ) { providerProperties, providerRepository ->
                    scmApiManager.updateCheckRun(
                        providerProperties = providerProperties,
                        providerRepository = providerRepository,
                        input = checkRunInput
                    )
                }
            }

            // github 暂时没对接sdk，先走老接口创建
            ScmProviderCodes.GITHUB.name -> {
                val checkRunId = checkRunInput.id!!
                githubService.updateCheckRuns(
                    token = oauth2TokenStoreManager.get(
                        userId = repo.userName,
                        scmCode = repo.scmCode
                    )?.accessToken ?: "",
                    projectName = repo.projectName,
                    checkRunId = checkRunId,
                    checkRuns = checkRunInput.convertGithubCheckRun()
                ).let {
                    CheckRun(
                        id = checkRunId,
                        name = checkRunInput.name,
                        status = checkRunInput.status,
                        summary = checkRunInput.output?.summary,
                        detailsUrl = checkRunInput.detailsUrl,
                        conclusion = checkRunInput.conclusion,
                        detail = checkRunInput.output?.text
                    )
                }
            }

            else -> {
                throw UnsupportedOperationException("repo($providerCode) unsupported create checkRun")
            }
        }
    }

    fun addComment(
        projectId: String,
        repositoryType: RepositoryType,
        repoId: String,
        number: Int,
        input: CommentInput
    ) {
        val repo = getRepo(projectId, repositoryType, repoId)
        invokeApi(
            projectId = projectId,
            authRepository = AuthRepository(repo)
        ) { providerProperties, providerRepository ->
            scmApiManager.createPullRequestComment(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                number = number,
                input = input
            )
        }
    }

    private fun createHook(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        hookUrl: String,
        event: String,
        secret: String? = null,
        existsEvents: Map<String, Hook>,
        subPath: String? = null
    ): Hook {
        return if (existsEvents.contains(event)) {
            existsEvents[event]!!
        } else {
            val hookEvents = ScmEventType.values().find { it.value == event }?.let {
                HookEvents(it)
            }
            val hookInput = HookInput(
                url = hookUrl,
                secret = secret,
                events = hookEvents,
                nativeEvents = if (hookEvents == null) {
                    listOf(event)
                } else {
                    listOf()
                },
                name = "",
                path = if (subPath.isNullOrBlank()) {
                    null
                } else {
                    subPath
                }
            )
            scmApiManager.createHook(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                input = hookInput
            )
        }
    }

    @Suppress("NestedBlockDepth")
    private fun existsHookEvents(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        hookUrl: String,
        subPath: String?
    ): Map<String, Hook> {
        val existsEvents = mutableMapOf<String, Hook>()
        var page = 1
        val pageSize = PageUtil.MAX_PAGE_SIZE
        do {
            val opts = ListOptions(
                page = page,
                pageSize = PageUtil.MAX_PAGE_SIZE
            )
            val hooks = scmApiManager.listHooks(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                opts = opts
            )
            hooks.forEach { existsHook ->
                val matchSubPath = subPath?.let { existsHook.path == it } ?: true
                if (existsHook.url == hookUrl && matchSubPath) {
                    existsHook.events?.getEnabledEvents()?.forEach { existsEvents[it] = existsHook }
                    if (!existsHook.nativeEvents.isNullOrEmpty()) {
                        existsHook.nativeEvents?.forEach { existsEvents[it] = existsHook }
                    }
                }
            }
            page++
        } while (hooks.size == pageSize)
        return existsEvents
    }

    private fun getHookUrl(type: ScmType, scmCode: String): String {
        return when (type) {
            ScmType.CODE_GIT -> {
                gitConfig.gitHookUrl
            }

            ScmType.CODE_GITLAB -> {
                gitConfig.gitlabHookUrl
            }

            ScmType.CODE_TGIT -> {
                gitConfig.tGitHookUrl
            }

            ScmType.CODE_P4 -> {
                p4Config.p4HookUrl
            }

            ScmType.SCM_GIT, ScmType.SCM_SVN -> {
                scmConfig.outerHookUrl.replace("{scmCode}", scmCode)
            }

            else -> {
                webhookUrl
            }
        }
    }

    private fun getRepo(
        projectId: String,
        repositoryType: RepositoryType,
        repoId: String
    ) = repositoryService.getRepository(
        projectId,
        repositoryHashId = if (repositoryType == RepositoryType.ID) repoId else null,
        repoAliasName = if (repositoryType == RepositoryType.NAME) repoId else null
    )

    private fun CheckRunInput.convertGithubCheckRun() =
        GithubCheckRuns(
            name = name,
            headSha = ref!!,
            detailsUrl = detailsUrl ?: "",
            status = when (status) {
                CheckRunStatus.IN_PROGRESS -> "in_progress"
                else -> "completed"
            },
            startedAt = startedAt?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ISO_INSTANT),
            completedAt = completedAt?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ISO_INSTANT),
            conclusion = when (conclusion) {
                CheckRunConclusion.SUCCESS -> "success"
                else -> "failure"
            },
            output = CheckRunOutput(
                summary = output?.summary,
                title = output?.title,
                text = output?.text
            ),
            externalId = externalId ?: ""
        )

    companion object {
        private val logger = LoggerFactory.getLogger(ScmRepositoryApiService::class.java)
    }
}
