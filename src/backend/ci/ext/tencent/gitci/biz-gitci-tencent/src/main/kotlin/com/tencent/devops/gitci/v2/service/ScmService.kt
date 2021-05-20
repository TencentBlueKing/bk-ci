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

package com.tencent.devops.gitci.v2.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.repository.pojo.git.GitMember
import com.tencent.devops.scm.api.ServiceGitCiResource
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.scm.pojo.Commit
import com.tencent.devops.scm.pojo.GitCICreateFile
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.pojo.GitCodeBranchesOrder
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ScmService @Autowired constructor(
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ScmService::class.java)
    }

    fun getYamlFromGit(
        token: String,
        gitProjectId: Long,
        fileName: String,
        ref: String,
        useAccessToken: Boolean
    ): String {
        return try {
            val result = client.getScm(ServiceGitResource::class).getGitCIFileContent(
                gitProjectId = gitProjectId,
                filePath = fileName,
                token = token,
                ref = getTriggerBranch(ref),
                useAccessToken = useAccessToken
            )
            if (result.data == null) {
                throw RuntimeException("yaml $fileName is null")
            }
            result.data!!
        } catch (e: Throwable) {
            logger.error("Get yaml from git failed", e)
            throw RuntimeException("Get yaml $fileName from git failed ${e.message}")
        }
    }

    fun getProjectInfo(
        token: String,
        gitProjectId: String,
        useAccessToken: Boolean
    ): GitCIProjectInfo? {
        return client.getScm(ServiceGitResource::class).getProjectInfo(
            accessToken = token,
            gitProjectId = gitProjectId.toLong(),
            useAccessToken = useAccessToken
        ).data
    }

    fun getCommits(
        token: String,
        gitProjectId: Long,
        filePath: String?,
        branch: String?,
        since: String?,
        until: String?,
        page: Int?,
        perPage: Int?
    ): List<Commit>? {
        return client.getScm(ServiceGitResource::class).getCommits(
            gitProjectId = gitProjectId,
            filePath = filePath,
            branch = branch,
            token = token,
            since = since,
            until = until,
            page = page ?: 1,
            perPage = perPage ?: 20
        ).data
    }

    fun createNewFile(
        token: String,
        gitProjectId: String,
        gitCICreateFile: GitCICreateFile
    ): Boolean {
        return client.getScm(ServiceGitResource::class).gitCICreateFile(
            gitProjectId = gitProjectId,
            token = token,
            gitCICreateFile = gitCICreateFile
        ).data!!
    }

    fun getProjectMembers(
        token: String,
        gitProjectId: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): List<GitMember>? {
        return client.getScm(ServiceGitCiResource::class).getMembers(
            token = token,
            gitProjectId = gitProjectId,
            page = page ?: 1,
            pageSize = pageSize ?: 20,
            search = search
        ).data
    }

    fun getProjectBranches(
        token: String,
        gitProjectId: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: GitCodeBranchesOrder?,
        sort: GitCodeBranchesSort?
    ): List<String>? {
        return client.getScm(ServiceGitCiResource::class)
            .getBranches(
                token = token,
                gitProjectId = gitProjectId,
                page = page ?: 1,
                pageSize = pageSize ?: 20,
                search = search,
                orderBy = orderBy,
                sort = sort
            ).data
    }

    // 获取项目ID，兼容没有source字段的旧数据，和fork库中源项目id不同的情况
    fun getProjectId(isFork: Boolean = false, gitRequestEvent: GitRequestEvent): Long {
        with(gitRequestEvent) {
            return if (isFork) {
                sourceGitProjectId!!
            } else {
                gitProjectId
            }
        }
    }

    private fun getTriggerBranch(branch: String): String {
        return when {
            branch.startsWith("refs/heads/") -> branch.removePrefix("refs/heads/")
            branch.startsWith("refs/tags/") -> branch.removePrefix("refs/tags/")
            else -> branch
        }
    }
}
