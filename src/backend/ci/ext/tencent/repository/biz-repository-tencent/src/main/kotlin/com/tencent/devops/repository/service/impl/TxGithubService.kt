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

package com.tencent.devops.repository.service.impl

import com.tencent.devops.common.api.constant.HTTP_200
import com.tencent.devops.common.api.constant.HTTP_403
import com.tencent.devops.common.client.Client
import com.tencent.devops.external.api.ExternalGithubResource
import com.tencent.devops.external.api.ServiceGithubResource
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.GithubCheckRuns
import com.tencent.devops.repository.pojo.GithubCheckRunsResponse
import com.tencent.devops.repository.pojo.github.GithubBranch
import com.tencent.devops.repository.pojo.github.GithubTag
import com.tencent.devops.repository.service.github.GithubTokenService
import com.tencent.devops.repository.service.github.IGithubService
import com.tencent.devops.scm.pojo.Project
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class TxGithubService @Autowired constructor(
    private val client: Client,
    private val githubTokenService: GithubTokenService
) : IGithubService {

    override fun webhookCommit(event: String, guid: String, signature: String, body: String) {
        client.get(ExternalGithubResource::class).webhookCommit(
            event = event,
            guid = guid,
            signature = signature,
            body = body
        )
    }

    override fun addCheckRuns(token: String, projectName: String, checkRuns: GithubCheckRuns): GithubCheckRunsResponse {
        val result = client.get(ServiceGithubResource::class).addCheckRuns(
            accessToken = token,
            projectName = projectName,
            checkRuns = checkRuns
        )
        return result.data!!
    }

    override fun updateCheckRuns(token: String, projectName: String, checkRunId: Long, checkRuns: GithubCheckRuns) {
        client.get(ServiceGithubResource::class).updateCheckRuns(
            accessToken = token,
            projectName = projectName,
            checkRunId = checkRunId,
            checkRuns = checkRuns
        )
    }

    override fun getProject(projectId: String, userId: String, repoHashId: String?): AuthorizeResult {

        val accessToken = githubTokenService.getAccessToken(userId)
        if (accessToken == null) {
            val url = client.get(ServiceGithubResource::class).getOauth(
                projectId = projectId,
                userId = userId,
                repoHashId = repoHashId
            ).data?.redirectUrl ?: ""
            return AuthorizeResult(status = HTTP_403, url = url)
        }

        return try {
            val projects = client.get(ServiceGithubResource::class).getProject(
                accessToken = accessToken.accessToken,
                userId = userId
            ).data!!.map {
                Project(
                    id = it.id,
                    name = it.name,
                    nameWithNameSpace = it.fullName,
                    sshUrl = it.sshUrl,
                    httpUrl = it.httpUrl,
                    lastActivity = it.updatedAt
                )
            }
            AuthorizeResult(status = HTTP_200, url = "", project = projects.toMutableList())
        } catch (ignored: Throwable) {
            val url = client.get(ServiceGithubResource::class).getOauth(
                projectId = projectId,
                userId = userId,
                repoHashId = repoHashId
            ).data?.redirectUrl ?: ""
            AuthorizeResult(status = HTTP_403, url = url)
        }
    }

    override fun getBranch(token: String, projectName: String, branch: String?): GithubBranch? {
        return client.get(ServiceGithubResource::class).getGithubBranch(
            accessToken = token,
            projectName = projectName,
            branch = branch
        ).data
    }

    override fun getTag(token: String, projectName: String, tag: String): GithubTag? {
        return client.get(ServiceGithubResource::class)
            .getGithubTag(accessToken = token, projectName = projectName, tag = tag).data
    }

    override fun getFileContent(projectName: String, ref: String, filePath: String): String {
        return client.get(ServiceGithubResource::class).getFileContent(
            projectName = projectName,
            ref = ref,
            filePath = filePath
        ).data ?: ""
    }

    override fun listBranches(token: String, projectName: String): List<String> {
        return client.get(ServiceGithubResource::class).listBranches(
            accessToken = token,
            projectName = projectName
        ).data!!
    }

    override fun listTags(token: String, projectName: String): List<String> {
        return client.get(ServiceGithubResource::class).listTags(
            accessToken = token,
            projectName = projectName
        ).data!!
    }
}
