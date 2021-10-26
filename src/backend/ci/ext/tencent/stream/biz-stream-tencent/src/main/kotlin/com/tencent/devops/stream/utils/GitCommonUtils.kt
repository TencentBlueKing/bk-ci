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

package com.tencent.devops.stream.utils

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.slf4j.LoggerFactory
import java.util.Base64

@Suppress("NestedBlockDepth", "DuplicateCaseInWhenExpression")
object GitCommonUtils {

    private val logger = LoggerFactory.getLogger(GitCommonUtils::class.java)

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
    fun checkAndGetForkBranch(gitRequestEvent: GitRequestEvent, client: Client): GitRequestEvent {
        var realEvent = gitRequestEvent
        // 如果是来自fork库的分支，单独标识,触发源项目ID和当先不同说明不是同一个库，为fork库
        if (gitRequestEvent.sourceGitProjectId != null &&
            gitRequestEvent.gitProjectId != gitRequestEvent.sourceGitProjectId
        ) {
            try {
                val gitToken = client.getScm(ServiceGitResource::class)
                    .getToken(gitRequestEvent.sourceGitProjectId!!).data!!
                logger.info(
                    "get token for gitProjectId[${gitRequestEvent.sourceGitProjectId!!}] form scm, " +
                            "token: $gitToken"
                )
                val sourceRepositoryConf = client.getScm(ServiceGitResource::class)
                    .getProjectInfo(gitToken.accessToken, gitRequestEvent.sourceGitProjectId!!).data
                realEvent = gitRequestEvent.copy(
                    // name_with_namespace: git_namespace/project_name , 要的是  git_namespace:branch
                    branch = if (sourceRepositoryConf != null) {
                        val path = sourceRepositoryConf.pathWithNamespace ?: sourceRepositoryConf.nameWithNamespace
                        "${path.split("/")[0]}:${gitRequestEvent.branch}"
                    } else {
                        gitRequestEvent.branch
                    }
                )
            } catch (e: Exception) {
                logger.error("Cannot get source GitProjectInfo: ", e)
            }
        }
        return realEvent
    }

    // 判断是否为fork库的mr请求并返回带fork库信息的branchName
    fun checkAndGetForkBranchName(
        gitProjectId: Long,
        sourceGitProjectId: Long?,
        branch: String,
        client: Client
    ): String {
        // 如果是来自fork库的分支，单独标识,触发源项目ID和当先不同说明不是同一个库，为fork库
        if (sourceGitProjectId != null && gitProjectId != sourceGitProjectId) {
            try {
                val gitToken = client.getScm(ServiceGitResource::class).getToken(sourceGitProjectId).data!!
                logger.info("get token for gitProjectId[$sourceGitProjectId] form scm, token: $gitToken")
                val sourceRepositoryConf = client.getScm(ServiceGitResource::class).getProjectInfo(
                    gitToken
                        .accessToken, sourceGitProjectId
                ).data
                // name_with_namespace: git_namespace/project_name , 要的是  git_namespace:branch
                return if (sourceRepositoryConf != null) {
                    val path = sourceRepositoryConf.pathWithNamespace ?: sourceRepositoryConf.nameWithNamespace
                    "${path.split("/")[0]}:$branch"
                } else {
                    branch
                }
            } catch (e: Exception) {
                logger.error("Cannot get source GitProjectInfo: ", e)
            }
        }
        return branch
    }

    fun getGitProjectId(projectId: String): Long {
        if (projectId.startsWith(projectPrefix)) {
            try {
                return projectId.removePrefix(projectPrefix).toLong()
            } catch (e: Exception) {
                throw RuntimeException("蓝盾项目ID不正确")
            }
        } else {
            throw RuntimeException("蓝盾项目ID不正确")
        }
    }

    // 获取蓝盾项目名称
    fun getCiProjectId(gitProjectId: Long) = "${projectPrefix}$gitProjectId"

    @Throws(ParamBlankException::class)
    fun getCredential(
        client: Client,
        projectId: String,
        credentialId: String,
        type: CredentialType
    ): MutableMap<String, String> {
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val decoder = Base64.getDecoder()
        val credentialResult = client.get(ServiceCredentialResource::class).get(
            projectId, credentialId,
            encoder.encodeToString(pair.publicKey)
        )
        if (credentialResult.isNotOk() || credentialResult.data == null) {
            logger.error(
                "Fail to get the credential($credentialId) of project($projectId) " +
                        "because of ${credentialResult.message}"
            )
            throw RuntimeException("Fail to get the credential($credentialId) of project($projectId)")
        }

        val credential = credentialResult.data!!
        if (type != credential.credentialType) {
            logger.error("CredentialId is invalid, expect:${type.name}, but real:${credential.credentialType.name}")
            throw ParamBlankException("Fail to get the credential($credentialId) of project($projectId)")
        }

        val ticketMap = mutableMapOf<String, String>()
        val v1 = String(
            DHUtil.decrypt(
                decoder.decode(credential.v1),
                decoder.decode(credential.publicKey),
                pair.privateKey
            )
        )
        ticketMap["v1"] = v1

        if (credential.v2 != null && credential.v2!!.isNotEmpty()) {
            val v2 = String(
                DHUtil.decrypt(
                    decoder.decode(credential.v2),
                    decoder.decode(credential.publicKey),
                    pair.privateKey
                )
            )
            ticketMap["v2"] = v2
        }

        if (credential.v3 != null && credential.v3!!.isNotEmpty()) {
            val v3 = String(
                DHUtil.decrypt(
                    decoder.decode(credential.v3),
                    decoder.decode(credential.publicKey),
                    pair.privateKey
                )
            )
            ticketMap["v3"] = v3
        }

        if (credential.v4 != null && credential.v4!!.isNotEmpty()) {
            val v4 = String(
                DHUtil.decrypt(
                    decoder.decode(credential.v4),
                    decoder.decode(credential.publicKey),
                    pair.privateKey
                )
            )
            ticketMap["v4"] = v4
        }

        return ticketMap
    }
}
