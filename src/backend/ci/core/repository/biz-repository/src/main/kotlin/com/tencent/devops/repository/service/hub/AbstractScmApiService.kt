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
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.repository.service.RepositoryScmConfigService
import com.tencent.devops.repository.service.RepositoryService
import com.tencent.devops.scm.api.enums.ScmProviderType
import com.tencent.devops.scm.api.pojo.repository.ScmProviderRepository
import com.tencent.devops.scm.spring.properties.ScmProviderProperties
import com.tencent.devops.scm.utils.code.git.GitUtils
import java.net.URI

abstract class AbstractScmApiService(
    private val repositoryService: RepositoryService,
    private val providerRepositoryFactory: ScmProviderRepositoryFactory,
    private val repositoryScmConfigService: RepositoryScmConfigService
) {
    protected fun <T> invokeApi(
        projectId: String,
        repositoryType: RepositoryType?,
        repoHashIdOrName: String,
        action: (providerProperties: ScmProviderProperties, providerRepository: ScmProviderRepository) -> T
    ): T {
        val repository = repositoryService.serviceGet(
            projectId = projectId,
            repositoryConfig = RepositoryConfigUtils.buildConfig(repoHashIdOrName, repositoryType)
        )
        return invokeApi(projectId = projectId, authRepository = AuthRepository(repository), action)
    }

    protected fun <T> invokeApi(
        projectId: String,
        authRepository: AuthRepository,
        action: (providerProperties: ScmProviderProperties, providerRepository: ScmProviderRepository) -> T
    ): T {
        return invokeApi(
            authRepository = authRepository,
            action = action
        )
    }

    protected fun <T> invokeApi(
        authRepository: AuthRepository,
        action: (providerProperties: ScmProviderProperties, providerRepository: ScmProviderRepository) -> T
    ): T {
        val properties = repositoryScmConfigService.getProps(scmCode = authRepository.scmCode)
        val providerRepository = providerRepositoryFactory.create(
            properties = properties,
            authRepository = authRepository
        )
        // 推断真是apiUrl
        properties.httpClientProperties?.apiUrl = extractApiUrl(authRepository.url, properties)
        return action.invoke(properties, providerRepository)
    }

    protected fun <T> invokeApi(
        scmCode: String,
        action: (providerProperties: ScmProviderProperties) -> T
    ): T {
        val properties = repositoryScmConfigService.getProps(scmCode = scmCode)
        return action.invoke(properties)
    }

    /**
     * 根据仓库url和配置apiUrl，推断实际apiUrl
     */
    private fun extractApiUrl(repoUrl: String, providerProperties: ScmProviderProperties): String {
        val apiUrl = providerProperties.httpClientProperties?.apiUrl ?: ""
        return when (providerProperties.providerType) {
            ScmProviderType.GIT.name -> {
                val host = GitUtils.getDomainAndRepoName(repoUrl).first
                val configHost = URI.create(apiUrl).host
                if (host == configHost) {
                    apiUrl
                } else {
                    apiUrl.replace(configHost, host)
                }
            }

            else -> apiUrl
        }
    }
}
