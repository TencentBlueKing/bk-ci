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

package com.tencent.devops.scm.utils.code.git

import com.tencent.devops.common.api.constant.CommonMessageCode.CALL_REPO_ERROR
import com.tencent.devops.common.api.constant.CommonMessageCode.GIT_INVALID_PRIVATE_KEY
import com.tencent.devops.common.api.constant.CommonMessageCode.GIT_LOGIN_FAIL
import com.tencent.devops.common.api.constant.CommonMessageCode.GIT_SERCRT_WRONG
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.scm.exception.ScmException
import org.slf4j.LoggerFactory
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

@Suppress("MagicNumber", "TooManyFunctions")
object GitUtils {
    // 工蜂pre-push虚拟分支
    private const val PRE_PUSH_BRANCH_NAME_PREFIX = "refs/for/"

    private val logger = LoggerFactory.getLogger(GitUtils::class.java)

    private val GIT_URL_REGEX_LIST = listOf(
        Regex("git@([-.a-z0-9A-Z]+):([0-9]+/)?(.*)\\.git"),
        Regex("http[s]?://([-.a-z0-9A-Z]+)(:[0-9]+)?/(.*)\\.git"),
        Regex("http[s]?://([-.a-z0-9A-Z]+)(:[0-9]+)?/(.*)")
    )

    fun urlDecode(s: String): String = URLDecoder.decode(s, "UTF-8")

    fun urlEncode(s: String): String = URLEncoder.encode(s, "UTF-8")

    fun getProjectName(gitUrl: String) = getDomainAndRepoName(gitUrl).second

    fun getDomainAndRepoName(gitUrl: String): Pair<String/*domain*/, String/*repoName*/> {
        // 兼容http存在端口的情況 http://gitlab.xx:8888/xx.git
        // [.git] 后缀小数点需转义, 否则会匹配失败
        val groups = GIT_URL_REGEX_LIST.firstNotNullOfOrNull { regex -> regex.find(gitUrl)?.groups }
            ?: throw ScmException("Git error, invalid field [http_url]:$gitUrl", ScmType.CODE_GIT.name)

        if (groups.size < 3) {
            throw ScmException("Git error, invalid field [http_url]:$gitUrl", ScmType.CODE_GIT.name)
        }

        if (gitUrl.startsWith("http")) {
            val url = URL(gitUrl)
            return url.authority to groups[3]!!.value
        }

        return "${groups[1]!!.value}${hasPort(groups[2])}" to groups[3]!!.value
    }

    private fun hasPort(port: MatchGroup?): String {
        return if (port == null) ""
        else ":${port.value.removeSuffix("/")}"
    }

    /**
     * 根据apiUrl与真正的仓库url，判断出真正的apiUrl
     */
    fun getGitApiUrl(apiUrl: String, repoUrl: String): String {
        val urlDomainAndRepoName = getDomainAndRepoName(repoUrl)
        val parseApiUri = partApiUrl(apiUrl)
        // #2894 先使用配置协议，配置协议为空则使用仓库协议
        val protocol = parseApiUri?.first ?: partApiUrl(repoUrl)?.first ?: "http://"
        return when {
            parseApiUri == null -> {
                "$protocol${urlDomainAndRepoName.first}/$apiUrl"
            }
            urlDomainAndRepoName.second != parseApiUri.second -> { // 如果域名不一样，则以仓库域名为准
                "$protocol${urlDomainAndRepoName.first}/${parseApiUri.third}"
            }
            else -> {
                apiUrl
            }
        }
    }

    private fun partApiUrl(apiUrl: String): Triple<String, String, String>? {
        val groups = Regex("(http[s]?://)([-.a-z0-9A-Z]+)(:[0-9]+)?/(.*)").find(apiUrl)?.groups
            ?: return null
        return Triple(groups[1]!!.value, groups[2]!!.value, groups[4]!!.value) // http[s]//, xxx.com, api/v4
    }

    fun isPrePushBranch(branchName: String?): Boolean {
        if (branchName == null) {
            return false
        }
        return branchName.startsWith(PRE_PUSH_BRANCH_NAME_PREFIX)
    }

    fun isLegalHttpUrl(url: String): Boolean {
        return Regex("http[s]?://([-.a-z0-9A-Z]+)(:[0-9]+)?/(.*).git").matches(url)
    }

    fun isLegalSshUrl(url: String): Boolean {
        return Regex("git@([-.a-z0-9A-Z]+):(.*).git").matches(url)
    }

    fun getRepoGroupAndName(projectName: String): Pair<String, String> {
        val repoName = projectName.split("/")
        val repoProjectName = if (repoName.size >= 2) {
            val index = projectName.lastIndexOf("/")
            projectName.substring(index + 1)
        } else {
            projectName
        }
        val repoGroupName = if (repoName.size >= 2) {
            projectName.removeSuffix("/$repoProjectName")
        } else {
            projectName
        }
        return Pair(repoGroupName, repoProjectName)
    }

    fun getShortSha(commitId: String?): String {
        return if (commitId.isNullOrBlank() || commitId.length < 8) {
            ""
        } else {
            commitId.substring(0, 8)
        }
    }

    /**
     * 校验代码库url
     */
    fun diffRepoUrl(
        sourceRepoUrl: String,
        targetRepoUrl: String
    ): Boolean {
        val sourceRepoInfo = GitUtils.getDomainAndRepoName(sourceRepoUrl)
        val targetRepoInfo = GitUtils.getDomainAndRepoName(targetRepoUrl)
        return sourceRepoInfo.first != targetRepoInfo.first ||
                sourceRepoInfo.second != targetRepoInfo.second
    }

    /**
     * 匹配异常状态码
     */
    fun matchExceptionCode(message: String) = when {
        Regex("Git repository not found").containsMatchIn(message) -> GIT_SERCRT_WRONG
        Regex("invalid privatekey").containsMatchIn(message) -> GIT_INVALID_PRIVATE_KEY
        Regex("connection failed").containsMatchIn(message) ||
                Regex("connection is closed by foreign host").containsMatchIn(message) -> CALL_REPO_ERROR
        Regex("not authorized").containsMatchIn(message) -> GIT_LOGIN_FAIL
        else -> null
    }

    fun getHttpUrl(sshUrl: String) = when {
        sshUrl.startsWith("http://") || sshUrl.startsWith("https://") -> sshUrl
        sshUrl.startsWith("git@") -> {
            val (domain, repoName) = getDomainAndRepoName(sshUrl)
            "https://$domain/$repoName"
        }
        else -> throw IllegalArgumentException("Unknown code repository URL")
    }

    /**
     * 判断是否需要过滤当前提交信息
     * @param message 提交信息原文
     * @param prefixes 需过滤的前缀
     * @param keywords 需过滤的关键词
     * @return true：需要过滤该提交；false：保留该提交
     */
    fun isFilterCommitMessage(message: String?, prefixes: String?, keywords: String?): Boolean {
        val trimmedMessage = message?.takeIf { it.isNotBlank() }?.trimStart() ?: return false
        val prefixList = prefixes
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        val keywordList = keywords
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        // 判断是否匹配前缀
        val matchesPrefix = prefixList.any { trimmedMessage.startsWith(it, ignoreCase = true) }

        // 判断是否匹配关键词
        val containsKeyword = keywordList.any { trimmedMessage.contains(it, ignoreCase = true) }

        return matchesPrefix || containsKeyword
    }

    /**
     * 获取仓库名称, 如果获取失败，返回原始字符串
     */
    fun tryGetRepoName(url: String?) = if (isValidGitUrl(url)) {
        try {
            getDomainAndRepoName(url!!).second
        } catch (ignored: Exception) {
            logger.warn("failed to get domain and repo name: $url, use source string", ignored)
            url
        }
    } else {
        url
    }

    /**
     * 判断字符串是否为有效的git仓库地址
     * @param url 待验证的字符串
     */
    fun isValidGitUrl(url: String?) = if (url.isNullOrBlank()) {
        false
    } else {
        GIT_URL_REGEX_LIST.any { regex -> regex.matches(url) }
    }

    /**
     * 获取仓库commit 详情链接
     */
    fun getRepoCommitUrl(repoUrl: String, commitId: String, scmType: ScmType): String {
        if (repoUrl.isEmpty() || commitId.isEmpty()) {
            return ""
        }
        val (domain, repoName) = getDomainAndRepoName(repoUrl)
        return when (scmType) {
            ScmType.CODE_GIT, ScmType.CODE_TGIT -> {
                "https://$domain/$repoName/commit/$commitId"
            }

            ScmType.GITHUB ->
                "https://github.com/$repoName/commit/$commitId"

            ScmType.CODE_GITLAB -> {
                "https://$domain/$repoName/-/commit/$commitId"
            }

            else -> ""
        }
    }
}
