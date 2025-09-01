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

import com.tencent.devops.repository.pojo.hub.ScmFilePushReq
import com.tencent.devops.repository.pojo.hub.ScmFilePushResult
import com.tencent.devops.repository.pojo.hub.ScmPullRequestCreateReq
import com.tencent.devops.repository.service.RepositoryScmConfigService
import com.tencent.devops.repository.service.RepositoryService
import com.tencent.devops.repository.service.ScmApiManager
import com.tencent.devops.scm.api.enums.PullRequestState
import com.tencent.devops.scm.api.pojo.ContentInput
import com.tencent.devops.scm.api.pojo.PullRequest
import com.tencent.devops.scm.api.pojo.PullRequestInput
import com.tencent.devops.scm.api.pojo.PullRequestListOptions
import com.tencent.devops.scm.api.pojo.ReferenceInput
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 对代码源api接口进行组合,形成新的接口
 */
@Service
class ScmApiComposer @Autowired constructor(
    private val repositoryService: RepositoryService,
    private val providerRepositoryFactory: ScmProviderRepositoryFactory,
    private val repositoryScmConfigService: RepositoryScmConfigService,
    private val scmApiManager: ScmApiManager
) : AbstractScmApiService(
    repositoryService = repositoryService,
    providerRepositoryFactory = providerRepositoryFactory,
    repositoryScmConfigService = repositoryScmConfigService
) {

    /**
     * 推送文件
     *
     * 推送的分支不存在时创建分支,判断文件是否存在,创建或更新文件
     */
    fun pushFile(projectId: String, filePushReq: ScmFilePushReq): ScmFilePushResult {
        return with(filePushReq) {
            invokeApi(
                projectId = projectId,
                authRepository = authRepository
            ) { providerProperties, providerRepository ->
                // 判断分支是否存在,不存在则创建
                scmApiManager.findBranch(
                    providerProperties = providerProperties,
                    providerRepository = providerRepository,
                    name = ref
                ) ?: run {
                    val referenceInput = ReferenceInput(
                        name = ref,
                        sha = defaultBranch
                    )
                    scmApiManager.createBranch(
                        providerProperties = providerProperties,
                        providerRepository = providerRepository,
                        input = referenceInput
                    )
                }
                // 判断文件是否存在
                val newFile = scmApiManager.getFileContent(
                    providerProperties = providerProperties,
                    providerRepository = providerRepository,
                    path = path,
                    ref = ref
                ) == null
                // 创建或更新文件
                val contentInput = ContentInput(
                    ref = ref,
                    content = content,
                    message = message
                )
                if (newFile) {
                    scmApiManager.createFile(
                        providerProperties = providerProperties,
                        providerRepository = providerRepository,
                        path = path,
                        input = contentInput
                    )
                } else {
                    scmApiManager.updateFile(
                        providerProperties = providerProperties,
                        providerRepository = providerRepository,
                        path = path,
                        input = contentInput
                    )
                }
                // 获取文件信息
                val newFileContent = scmApiManager.getFileContent(
                    providerProperties = providerProperties,
                    providerRepository = providerRepository,
                    path = path,
                    ref = ref
                )
                // 获取提交信息
                val commit = scmApiManager.findCommit(
                    providerProperties = providerProperties,
                    providerRepository = providerRepository,
                    sha = ref
                )
                ScmFilePushResult(
                    content = newFileContent!!,
                    commit = commit,
                    newFile = newFile
                )
            }
        }
    }

    fun createPullRequestIfAbsent(
        projectId: String,
        pullRequestCreateReq: ScmPullRequestCreateReq
    ): PullRequest {
        return with(pullRequestCreateReq) {
            invokeApi(
                projectId = projectId,
                authRepository = authRepository
            ) { providerProperties, providerRepository ->
                // 判断源分支和目标分支的合并请求是否已经存在open的
                val opts = PullRequestListOptions(
                    state = PullRequestState.OPENED,
                    sourceBranch = sourceBranch,
                    targetBranch = targetBranch,
                    page = 1,
                    pageSize = 1
                )
                val pullRequests = scmApiManager.listPullRequest(
                    providerProperties = providerProperties,
                    providerRepository = providerRepository,
                    opts = opts
                )
                if (pullRequests.isNotEmpty()) {
                    pullRequests.first()
                } else {
                    val input = PullRequestInput(
                        title = title,
                        body = body,
                        sourceBranch = sourceBranch,
                        targetBranch = targetBranch
                    )
                    scmApiManager.createPullRequest(
                        providerProperties = providerProperties,
                        providerRepository = providerRepository,
                        input = input
                    )
                }
            }
        }
    }
}
