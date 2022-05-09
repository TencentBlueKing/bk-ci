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

package com.tencent.devops.common.webhook.util

import com.google.common.base.Splitter
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.webhook.pojo.code.git.GitCommit
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_COUNT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_PREFIX
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_AUTHOR_PREFIX
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_MSG_PREFIX
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_PREFIX
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_TIMESTAMP_PREFIX
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_COUNT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_PREFIX
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_COUNT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_PREFIX
import java.util.regex.Pattern

object WebhookUtils {

    private val separatorPattern = Pattern.compile("[,;]")
    private const val MAX_VARIABLE_COUNT = 32

    fun convert(commaSeparatedString: String?): List<String> {
        if (commaSeparatedString == null) {
            return emptyList()
        }
        return Splitter.on(separatorPattern)
            .omitEmptyStrings()
            .trimResults()
            .split(commaSeparatedString)
            .toList()
    }

    fun getBranch(ref: String): String {
        return ref.removePrefix("refs/heads/")
    }

    fun getTag(ref: String): String {
        return ref.removePrefix("refs/tags/")
    }

    @SuppressWarnings("ALL")
    fun getRelativePath(url: String): String {
        val urlArray = url.split("//")
        if (urlArray.size < 2) {
            return ""
        }

        val path = urlArray[1]
        val repoSplit = path.split("/")
        if (repoSplit.size < 4) {
            return ""
        }
        val domain = repoSplit[0]
        val first = repoSplit[1]
        val second = repoSplit[2]

        return path.removePrefix("$domain/$first/$second").removePrefix("/")
    }

    fun getFullPath(projectRelativePath: String, relativeSubPath: String): String {
        return ("${projectRelativePath.removeSuffix("/")}/" +
            relativeSubPath.removePrefix("/")).removePrefix("/")
    }

    fun genCommitsParam(commits: List<GitCommit>): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        var addCount = 0
        var modifyCount = 0
        var deleteCount = 0
        commits.forEachIndexed { index, gitCommit ->
            val curIndex = index + 1
            startParams[BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_PREFIX + curIndex] = gitCommit.id
            startParams[BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_MSG_PREFIX + curIndex] = gitCommit.message
            startParams[BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_TIMESTAMP_PREFIX + curIndex] =
                DateTimeUtil.zoneDateToTimestamp(gitCommit.timestamp)
            startParams[BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_AUTHOR_PREFIX + curIndex] = gitCommit.author
            addCount += gitCommit.added?.size ?: 0
            modifyCount += gitCommit.modified?.size ?: 0
            deleteCount += gitCommit.removed?.size ?: 0

            var count = 0
            run {
                gitCommit.added?.forEachIndexed { innerIndex, file ->
                    startParams[BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_PREFIX + curIndex + "_" + (innerIndex + 1)] = file
                    count++
                    if (count > MAX_VARIABLE_COUNT) return@run
                }
            }

            run {
                gitCommit.modified?.forEachIndexed { innerIndex, file ->
                    startParams[BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_PREFIX + curIndex + "_" + (innerIndex + 1)] = file
                    count++
                    if (count > MAX_VARIABLE_COUNT) return@run
                }
            }

            run {
                gitCommit.removed?.forEachIndexed { innerIndex, file ->
                    startParams[BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_PREFIX + curIndex + "_" + (innerIndex + 1)] = file
                    count++
                    if (count > MAX_VARIABLE_COUNT) return@run
                }
            }
        }
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_COUNT] = addCount
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_COUNT] = modifyCount
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_COUNT] = deleteCount
        return startParams
    }
}
