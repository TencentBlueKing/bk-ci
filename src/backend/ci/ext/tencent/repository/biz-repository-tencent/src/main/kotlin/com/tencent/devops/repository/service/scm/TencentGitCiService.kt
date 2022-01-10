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

package com.tencent.devops.repository.service.scm

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.api.ServiceGitCiResource
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Primary
@Service
class TencentGitCiService @Autowired constructor(
    private val gitService: IGitService,
    private val client: Client
) {
    private val projectTokenCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(30, TimeUnit.DAYS)
        .build(object : CacheLoader<String, String>() {
            override fun load(projectName: String): String {
                return getToken(projectName)?.accessToken
                    ?: throw RuntimeException("get auth token fail for repo: $projectName")
            }
        })

    fun getToken(gitProjectId: String): GitToken? {
        return client.getScm(ServiceGitCiResource::class).getToken(gitProjectId).data
    }

    fun clearToken(token: String): Boolean {
        return client.getScm(ServiceGitCiResource::class).clearToken(token).data ?: false
    }

    fun getFileContent(repoUrl: String, filePath: String, ref: String?, subModule: String?): String {
        val projectName = if (subModule.isNullOrBlank()) GitUtils.getProjectName(repoUrl) else subModule!!
        val token = projectTokenCache.get(projectName)
        return gitService.getGitFileContent(
            repoName = projectName,
            filePath = filePath.removePrefix("/"),
            authType = RepoAuthType.OAUTH,
            token = token,
            ref = ref ?: "master")
    }
}
