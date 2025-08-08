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
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.repository.service.RepositoryScmConfigService
import com.tencent.devops.repository.service.RepositoryService
import com.tencent.devops.repository.service.ScmApiManager
import com.tencent.devops.scm.api.pojo.Content
import com.tencent.devops.scm.api.pojo.Tree
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 代码源文件api接口
 */
@Service
class ScmFileApiService @Autowired constructor(
    private val repositoryService: RepositoryService,
    private val providerRepositoryFactory: ScmProviderRepositoryFactory,
    private val repositoryScmConfigService: RepositoryScmConfigService,
    private val scmApiManager: ScmApiManager
) : AbstractScmApiService(
    repositoryService = repositoryService,
    providerRepositoryFactory = providerRepositoryFactory,
    repositoryScmConfigService = repositoryScmConfigService
) {
    fun listFileTree(
        projectId: String,
        repositoryType: RepositoryType?,
        repoHashIdOrName: String,
        path: String,
        ref: String,
        recursive: Boolean
    ): List<Tree> {
        return invokeApi(
            projectId = projectId,
            repositoryType = repositoryType,
            repoHashIdOrName = repoHashIdOrName
        ) { providerProperties, providerRepository ->
            scmApiManager.listFileTree(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                path = path,
                ref = ref,
                recursive = recursive
            )
        }
    }

    fun listFileTree(
        projectId: String,
        path: String,
        ref: String,
        recursive: Boolean,
        authRepository: AuthRepository
    ): List<Tree> {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            scmApiManager.listFileTree(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                path = path,
                ref = ref,
                recursive = recursive
            )
        }
    }

    fun getFileContent(
        projectId: String,
        repositoryType: RepositoryType?,
        repoHashIdOrName: String,
        path: String,
        ref: String
    ): Content? {
        return invokeApi(
            projectId = projectId,
            repositoryType = repositoryType,
            repoHashIdOrName = repoHashIdOrName
        ) { providerProperties, providerRepository ->
            scmApiManager.getFileContent(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                path = path,
                ref = ref
            )
        }
    }

    fun getFileContent(
        projectId: String,
        path: String,
        ref: String,
        authRepository: AuthRepository
    ): Content? {
        return invokeApi(
            projectId = projectId,
            authRepository = authRepository
        ) { providerProperties, providerRepository ->
            scmApiManager.getFileContent(
                providerProperties = providerProperties,
                providerRepository = providerRepository,
                path = path,
                ref = ref
            )
        }
    }
}
