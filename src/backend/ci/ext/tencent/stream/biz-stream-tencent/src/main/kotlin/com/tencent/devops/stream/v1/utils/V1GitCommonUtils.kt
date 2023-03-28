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

package com.tencent.devops.stream.v1.utils

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.stream.constant.StreamMessageCode.INCORRECT_ID_BLUE_SHIELD_PROJECT
import com.tencent.devops.stream.v1.pojo.V1GitProjectCache
import com.tencent.devops.stream.v1.pojo.V1GitRequestEvent
import com.tencent.devops.stream.v1.pojo.isFork
import org.slf4j.LoggerFactory

@Suppress("NestedBlockDepth", "DuplicateCaseInWhenExpression")
object V1GitCommonUtils {

    private val logger = LoggerFactory.getLogger(V1GitCommonUtils::class.java)

    //    private const val dockerHubUrl = "https://index.docker.io/v1/"

    private const val projectPrefix = "git_"

    private const val httpPrefix = "http://"

    private const val httpsPrefix = "https://"

    private const val gitEnd = ".git"

    fun getPathWithNameSpace(url: String?): String? {
        if (url.isNullOrBlank()) {
            return null
        }
        val nameWithWeb = url.removePrefix(httpPrefix).removePrefix(httpsPrefix).removeSuffix(gitEnd)
        // xxx.com/PathWithNameSpace
        val index = nameWithWeb.indexOf("/")
        return nameWithWeb.substring(index + 1)
    }

    // 获取 name/projectName格式的项目名称
    fun getRepoName(httpUrl: String, name: String): String {
        return try {
            getRepoOwner(httpUrl) + "/" + name
        } catch (e: Throwable) {
            name
        }
    }

    fun getRepoOwner(httpUrl: String): String {
        return when {
            httpUrl.startsWith(httpPrefix) -> {
                val urls = httpUrl.removePrefix(httpPrefix)
                    .split("/").toMutableList()
                urls.removeAt(0)
                urls.removeAt(urls.lastIndex)
                urls.joinToString("/")
            }
            httpUrl.startsWith(httpsPrefix) -> {
                val urls = httpUrl.removePrefix(httpsPrefix)
                    .split("/").toMutableList()
                urls.removeAt(0)
                urls.removeAt(urls.lastIndex)
                urls.joinToString("/")
            }
            else -> ""
        }
    }

    fun getRepoName(httpUrl: String): String {
        return when {
            httpUrl.startsWith(httpPrefix) -> {
                httpUrl.removePrefix(httpPrefix)
                    .split("/")[2]
            }
            httpUrl.startsWith(httpsPrefix) -> {
                httpUrl.removePrefix(httpsPrefix)
                    .split("/")[2]
            }
            else -> ""
        }
    }

    // 判断是否为fork库的mr请求并返回带fork库信息的event
    fun checkAndGetForkBranch(
        gitRequestEvent: V1GitRequestEvent,
        gitProjectCache: Lazy<V1GitProjectCache?>?
    ): V1GitRequestEvent {
        var realEvent = gitRequestEvent
        // 如果是来自fork库的分支，单独标识,触发源项目ID和当先不同说明不是同一个库，为fork库
        if (gitRequestEvent.isFork()) {
            realEvent = gitRequestEvent.copy(
                // name_with_namespace: git_namespace/project_name , 要的是  git_namespace:branch
                branch = gitProjectCache?.value?.pathWithNamespace?.let {
                    "${it.split("/")[0]}:${gitRequestEvent.branch}"
                } ?: gitRequestEvent.branch
            )
        }
        return realEvent
    }

    // 判断是否为远程库的请求并返回带远程库信息的event
    fun checkAndGetRepoBranch(gitRequestEvent: V1GitRequestEvent, pathWithNamespace: String?): V1GitRequestEvent {
        return gitRequestEvent.copy(
            // name_with_namespace: git_namespace/project_name , 要的是  git_namespace/project_name:branch
            branch = if (pathWithNamespace != null) {
                "$pathWithNamespace:${gitRequestEvent.branch}"
            } else {
                gitRequestEvent.branch
            }
        )
    }

    // 判断是否为远程库的请求并返回带远程库信息的event
    fun checkAndGetRepoBranch(
        gitRequestEvent: V1GitRequestEvent,
        gitProjectCache: V1GitProjectCache
    ): V1GitRequestEvent {
        return gitRequestEvent.copy(
            // name_with_namespace: git_namespace/project_name , 要的是  git_namespace/project_name:branch
            branch = if (gitProjectCache.pathWithNamespace != null) {
                "${gitProjectCache.pathWithNamespace}:${gitRequestEvent.branch}"
            } else {
                gitRequestEvent.branch
            }
        )
    }

    // 判断是否为fork库的mr请求并返回带fork库信息的branchName
    fun checkAndGetForkBranchName(
        gitProjectId: Long,
        sourceGitProjectId: Long?,
        branch: String,
        gitProjectCache: Lazy<V1GitProjectCache?>?
    ): String {
        // 如果是来自fork库的分支，单独标识,触发源项目ID和当先不同说明不是同一个库，为fork库
        if (sourceGitProjectId != null && gitProjectId != sourceGitProjectId) {
            // name_with_namespace: git_namespace/project_name , 要的是  git_namespace:branch
            return gitProjectCache?.value?.pathWithNamespace?.let {
                "${it.split("/")[0]}:$branch"
            } ?: branch
        }
        return branch
    }

    fun getGitProjectId(projectId: String): Long {
        if (projectId.startsWith(projectPrefix)) {
            try {
                return projectId.removePrefix(projectPrefix).toLong()
            } catch (e: Exception) {
                throw OperationException(
                    MessageUtil.getMessageByLocale(
                        messageCode = INCORRECT_ID_BLUE_SHIELD_PROJECT,
                        language = I18nUtil.getLanguage()
                    )
                )
            }
        } else {
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    messageCode = INCORRECT_ID_BLUE_SHIELD_PROJECT,
                    language = I18nUtil.getLanguage()
                )
            )
        }
    }

    // 获取蓝盾项目名称
    fun getCiProjectId(gitProjectId: Long) = "${projectPrefix}$gitProjectId"
}
