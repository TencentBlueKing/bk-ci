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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service.code

import com.tencent.devops.process.engine.service.code.GitWebHookMatcher
import com.tencent.devops.process.pojo.code.git.GitEvent
import com.tencent.devops.process.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.process.pojo.code.git.GitPushEvent
import com.tencent.devops.process.pojo.code.git.GitTagPushEvent
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.slf4j.LoggerFactory

class TencentGitWebHookMatcher(
    private val gitEvent: GitEvent,
    private val gitIncludeHost: String?
) : GitWebHookMatcher(gitEvent) {

    companion object {
        private val logger = LoggerFactory.getLogger(TencentGitWebHookMatcher::class.java)
    }
    override fun matchUrl(url: String): Boolean {
        if (isCodeGitHook(url)) {
            logger.info("git match url by projectName, url:$url")
            return matchProjectName(url)
        }
        return super.matchUrl(url)
    }

    /**
     * 判断是否是工蜂触发过来的事件,工蜂触发过来的事件通过对比项目名
     */
    private fun isCodeGitHook(url: String): Boolean {
        if (gitIncludeHost.isNullOrBlank()) {
            return false
        }
        val includeHosts = gitIncludeHost!!.split(",")
        val hookUrl = when (gitEvent) {
            is GitPushEvent ->
                gitEvent.repository.git_http_url
            is GitTagPushEvent ->
                gitEvent.repository.git_http_url
            is GitMergeRequestEvent ->
                gitEvent.object_attributes.target.http_url
            else ->
                return false
        }
        val hookUrlHost = GitUtils.getDomainAndRepoName(hookUrl).first
        return includeHosts.contains(hookUrlHost)
    }

    private fun matchProjectName(url: String): Boolean {
        return when (gitEvent) {
            is GitPushEvent -> {
                val repoProjectName = GitUtils.getProjectName(url)
                val eventHttpProjectName =
                    GitUtils.getProjectName(gitEvent.repository.git_http_url)
                val eventSshProjectName =
                    GitUtils.getProjectName(gitEvent.repository.git_ssh_url)
                repoProjectName == eventSshProjectName || repoProjectName == eventHttpProjectName
            }
            is GitTagPushEvent -> {
                val repoProjectName = GitUtils.getProjectName(url)
                val eventHttpProjectName =
                    GitUtils.getProjectName(gitEvent.repository.git_http_url)
                val eventSshProjectName =
                    GitUtils.getProjectName(gitEvent.repository.git_ssh_url)
                repoProjectName == eventSshProjectName || repoProjectName == eventHttpProjectName
            }
            is GitMergeRequestEvent -> {
                val repoProjectName = GitUtils.getProjectName(url)
                val eventHttpProjectName =
                    GitUtils.getProjectName(gitEvent.object_attributes.target.http_url)
                val eventSshProjectName =
                    GitUtils.getProjectName(gitEvent.object_attributes.target.ssh_url)
                repoProjectName == eventSshProjectName || repoProjectName == eventHttpProjectName
            }
            else -> {
                false
            }
        }
    }
}