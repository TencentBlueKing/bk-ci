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
import com.tencent.devops.gitci.v2.service.trigger.V2RequestTrigger
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.pojo.GitFileInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class ScmService @Autowired constructor(
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ScmService::class.java)
        private const val ciFileName = ".ci.yml"
        private const val templateDirectoryName = ".ci/templates"
        private const val ciFileExtension = ".yml"
    }

    fun getCIYamlList(
        gitToken: GitToken,
        gitRequestEvent: GitRequestEvent,
        isFork: Boolean = false
    ): MutableList<String> {
        val ciFileList = getFileTreeFromGit(gitToken, gitRequestEvent, templateDirectoryName, isFork)
            .filter { it.name.endsWith(ciFileExtension) }
        return ciFileList.map { templateDirectoryName + File.separator + it.name }.toMutableList()
    }

    fun getFileTreeFromGit(
        gitToken: GitToken,
        gitRequestEvent: GitRequestEvent,
        filePath: String,
        isFork: Boolean = false
    ): List<GitFileInfo> {
        return try {
            val result = client.getScm(ServiceGitResource::class).getGitCIFileTree(
                gitProjectId = getProjectId(isFork, gitRequestEvent),
                path = filePath,
                token = gitToken.accessToken,
                ref = getTriggerBranch(gitRequestEvent)
            )
            result.data!!
        } catch (e: Throwable) {
            logger.error("Get yaml from git failed", e)
            emptyList()
        }
    }

    fun getYamlFromGit(
        gitToken: GitToken,
        gitRequestEvent: GitRequestEvent,
        fileName: String,
        isMrEvent: Boolean = false
    ): String? {
        return try {
            val result = client.getScm(ServiceGitResource::class).getGitCIFileContent(
                gitProjectId = getProjectId(isMrEvent, gitRequestEvent),
                filePath = fileName,
                token = gitToken.accessToken,
                ref = getTriggerBranch(gitRequestEvent)
            )
            result.data
        } catch (e: Throwable) {
            logger.error("Get yaml from git failed", e)
            null
        }
    }

    fun getAllTemplates(
        token: String,
        gitProjectId: Long,
        filePath: String,
        removePrefix: String,
        addLastFix: String,
        ref: String,
        useAccessToken: Boolean
    ): Map<String, String?> {
        val templateFileList = getCIYamlList(token, gitProjectId, filePath, ref, useAccessToken)
        val templates = mutableMapOf<String, String?>()
        templateFileList.forEach { fileName ->
            templates[fileName.removePrefix(removePrefix) + addLastFix] = getYamlFromGit(
                token, gitProjectId, fileName, ref, useAccessToken
            )
        }
        return templates
    }

    fun getCIYamlList(
        token: String,
        gitProjectId: Long,
        filePath: String,
        ref: String,
        useAccessToken: Boolean
    ): MutableList<String> {
        val ciFileList = getFileTreeFromGit(token, gitProjectId, filePath, ref, useAccessToken)
            .filter { it.name.endsWith(ciFileExtension) }
        return ciFileList.map { filePath + File.separator + it.name }.toMutableList()
    }

    fun getFileTreeFromGit(
        token: String,
        gitProjectId: Long,
        filePath: String,
        ref: String,
        useAccessToken: Boolean
    ): List<GitFileInfo> {
        return try {
            val result = client.getScm(ServiceGitResource::class).getGitCIFileTree(
                gitProjectId = gitProjectId,
                path = filePath,
                token = token,
                ref = getTriggerBranch(ref),
                useAccessToken = useAccessToken
            )
            result.data!!
        } catch (e: Throwable) {
            logger.error("Get yaml from git failed", e)
            emptyList()
        }
    }

    fun getYamlFromGit(
        token: String,
        gitProjectId: Long,
        fileName: String,
        ref: String,
        useAccessToken: Boolean
    ): String? {
        return try {
            val result = client.getScm(ServiceGitResource::class).getGitCIFileContent(
                gitProjectId = gitProjectId,
                filePath = fileName,
                token = token,
                ref = getTriggerBranch(ref),
                useAccessToken = useAccessToken
            )
            result.data
        } catch (e: Throwable) {
            logger.error("Get yaml from git failed", e)
            null
        }
    }

    fun getProjectInfo(
        token: String,
        gitProjectId: String,
        useAccessToken: Boolean
    ): GitCIProjectInfo? {
        return client.getScm(ServiceGitResource::class).getProjectInfo(token, gitProjectId, useAccessToken).data
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

    private fun getTriggerBranch(gitRequestEvent: GitRequestEvent): String {
        return when {
            gitRequestEvent.branch.startsWith("refs/heads/") -> gitRequestEvent.branch.removePrefix("refs/heads/")
            gitRequestEvent.branch.startsWith("refs/tags/") -> gitRequestEvent.branch.removePrefix("refs/tags/")
            else -> gitRequestEvent.branch
        }
    }
}
