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
 */

package com.tencent.devops.repository.service.hub

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.repository.service.RepositoryOauthService
import com.tencent.devops.repository.service.RepositoryScmConfigService
import com.tencent.devops.repository.service.RepositoryService
import com.tencent.devops.repository.service.ScmApiManager
import com.tencent.devops.repository.service.oauth2.Oauth2TokenStoreManager
import com.tencent.devops.scm.api.enums.ScmEventType
import com.tencent.devops.scm.api.pojo.BranchListOptions
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
import com.tencent.devops.scm.config.GitConfig
import com.tencent.devops.scm.spring.properties.ScmProviderProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

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
    private val repositoryOauthService: RepositoryOauthService
) : AbstractScmApiService(
    repositoryService = repositoryService,
    providerRepositoryFactory = providerRepositoryFactory,
    repositoryScmConfigService = repositoryScmConfigService
) {
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
        search: String?
    ): AuthorizeResult {
        val oauthTokenInfo = oauth2TokenStoreManager.get(userId = userId, scmCode = scmCode) ?: run {
            val redirectUrl = gitConfig.redirectUrl + "/$projectId/?scmCode=$scmCode&popupScm"
            val oauthUrl = repositoryOauthService.oauthUrl(
                userId = userId,
                scmCode = scmCode,
                redirectUrl = redirectUrl
            )
            return AuthorizeResult(status = 403, url = oauthUrl.url)
        }
        val opts = RepoListOptions.builder().repoName(search).page(1).pageSize(20).build()
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
                opts = BranchListOptions.builder()
                        .search(search)
                        .page(page)
                        .pageSize(pageSize)
                        .build()
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
                opts = TagListOptions.builder()
                        .search(search)
                        .page(page)
                        .pageSize(pageSize)
                        .build()
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
        hookUrl: String,
        events: List<String>,
        secret: String? = null,
        authRepository: AuthRepository
    ): List<Hook> {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            val existsEvents = existsHookEvents(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                hookUrl = hookUrl
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
                    existsEvents = existsEvents
                )
                hooks.add(hook)
            }
            hooks
        }
    }

    private fun createHook(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        hookUrl: String,
        event: String,
        secret: String? = null,
        existsEvents: Map<String, Hook>
    ): Hook {
        return if (existsEvents.contains(event)) {
            existsEvents[event]!!
        } else {
            val builder = HookInput.builder().url(hookUrl).secret(secret)
            ScmEventType.fromValue(event)?.let {
                builder.events(HookEvents(it))
            } ?: builder.nativeEvents(listOf(event))

            scmApiManager.createHook(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                input = builder.build()
            )
        }
    }

    @Suppress("NestedBlockDepth")
    private fun existsHookEvents(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        hookUrl: String
    ): Map<String, Hook> {
        val existsEvents = mutableMapOf<String, Hook>()
        var page = 1
        val pageSize = PageUtil.MAX_PAGE_SIZE
        do {
            val opts = ListOptions.builder()
                .page(page)
                .pageSize(PageUtil.MAX_PAGE_SIZE)
                .build()
            val hooks = scmApiManager.listHooks(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                opts = opts
            )
            hooks.forEach { existsHook ->
                if (existsHook.url == hookUrl) {
                    existsHook.events.enabledEvents.forEach { existsEvents[it] = existsHook }
                    if (!existsHook.nativeEvents.isNullOrEmpty()) {
                        existsHook.nativeEvents.forEach { existsEvents[it] = existsHook }
                    }
                }
            }
            page++
        } while (hooks.size == pageSize)
        return existsEvents
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScmRepositoryApiService::class.java)
    }
}
