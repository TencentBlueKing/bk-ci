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

package com.tencent.devops.gitci.v2.utils

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.ci.v2.TriggerOn
import com.tencent.devops.gitci.pojo.git.GitEvent
import com.tencent.devops.gitci.pojo.git.GitMergeRequestEvent
import com.tencent.devops.gitci.pojo.git.GitPushEvent
import com.tencent.devops.gitci.pojo.git.GitTagPushEvent
import org.slf4j.LoggerFactory
import org.springframework.util.AntPathMatcher

@Suppress("ALL")
class V2WebHookMatcher(private val event: GitEvent) {

    companion object {
        private val logger = LoggerFactory.getLogger(V2WebHookMatcher::class.java)
        private val matcher = AntPathMatcher()
    }

    fun isMatch(
        triggerOn: TriggerOn
    ): Pair<Boolean, Boolean> {
        val eventBranch = getBranch()
        val eventTag = getTag()
        val eventType = getEventType()

        if (triggerOn.mr != null || triggerOn.push != null || triggerOn.tag != null) {
            when (eventType) {
                CodeEventType.PUSH -> {
                    if (isPushMatch(triggerOn, eventBranch)) return Pair(first = true, second = false)
                }
                CodeEventType.TAG_PUSH -> {
                    if (isTagPushMatch(triggerOn, eventTag)) return Pair(first = true, second = false)
                }
                CodeEventType.MERGE_REQUEST -> {
                    if (isMrMatch(triggerOn, eventBranch)) return Pair(first = true, second = false)
                }
                else -> {
                    return Pair(first = false, second = false)
                }
            }
        } else if (triggerOn.schedules != null) {
            if (isSchedulesMatch(triggerOn, eventBranch)) return Pair(first = false, second = true)
        }

        logger.info("The triggerOn doesn't match the git event($event)")
        return Pair(first = false, second = false)
    }

    private fun isPushMatch(triggerOn: TriggerOn, eventBranch: String): Boolean {
        // 如果没有配置push，默认匹配
        if (triggerOn.push == null) {
            return false
        }

        val pushRule = triggerOn.push!!
        // 1、check branchIgnore，满足屏蔽条件直接返回不匹配
        if (isIgnoreBranchMatch(pushRule.branchesIgnore)) return false

        // 2、check pathIgnore，满足屏蔽条件直接返回不匹配
        if (isIgnorePathMatch(pushRule.pathsIgnore)) return false

        // 3、check userIgnore,满足屏蔽条件直接返回不匹配
        if (isIgnoreUserMatch(pushRule.usersIgnore)) return false

        // include
        if (isBranchMatch(pushRule.branches) && isIncludePathMatch(pushRule.paths) && isUserMatch(pushRule.users)) {
            logger.info("Git trigger branch($eventBranch) is included and path(${pushRule.paths}) is included")
            return true
        }
        return false
    }

    private fun isTagPushMatch(triggerOn: TriggerOn, eventTag: String): Boolean {
        if (triggerOn.tag == null) {
            return false
        }

        val tagRule = triggerOn.tag!!
        // ignore
        if (tagRule.tagsIgnore != null && tagRule.tagsIgnore!!.isNotEmpty()) {
            tagRule.tagsIgnore!!.forEach {
                if (isTagMatch(it, eventTag)) {
                    logger.info("The exclude tag($it) exclude the git tag ($eventTag)")
                    return false
                }
            }
        }

        if (isIgnoreUserMatch(tagRule.usersIgnore)) return false

        // include
        var tagIncluded = false
        if (tagRule.tags != null && tagRule.tags!!.isNotEmpty()) {
            logger.info("Include tags set(${tagRule.tags})")
            run outside@{
                tagRule.tags!!.forEach {
                    if (isTagMatch(it, eventTag)) {
                        logger.info("The include tags($it) include the git update one($eventTag)")
                        tagIncluded = true
                        return@outside
                    }
                }
            }
        }

        if (tagIncluded && isUserMatch(tagRule.users)) {
            logger.info("Git trigger tags($eventTag) is included and path(${triggerOn.tag!!.tags}) is included")
            return true
        }
        return false
    }

    private fun isMrMatch(triggerOn: TriggerOn, eventBranch: String): Boolean {
        if (triggerOn.mr == null) {
            return false
        }

        // exclude branch of mr
        if (triggerOn.mr!!.sourceBranchesIgnore != null && triggerOn.mr!!.sourceBranchesIgnore!!.isNotEmpty()) {
            logger.info("Exclude branch set(${triggerOn.mr!!.sourceBranchesIgnore})")
            triggerOn.mr!!.sourceBranchesIgnore!!.forEach {
                if (isBranchMatch(it, eventBranch)) {
                    logger.info("The exclude branch($it) exclude the git update one($eventBranch)")
                    return false
                }
            }
        }

        if (isIgnorePathMatch(triggerOn.mr!!.pathsIgnore)) return false
        if (isIgnoreUserMatch(triggerOn.mr!!.usersIgnore)) return false

        // include branch of mr
        var targetBranchMatch = false
        if (triggerOn.mr!!.targetBranches != null && triggerOn.mr!!.targetBranches!!.isNotEmpty()) {
            logger.info("Include branch set(${triggerOn.mr!!.targetBranches})")
            triggerOn.mr!!.targetBranches!!.forEach {
                if (isBranchMatch(it, eventBranch)) {
                    logger.info("The include branch($it) include the git update one($eventBranch)")
                    targetBranchMatch = true
                }
            }
        }

        if (targetBranchMatch && isIncludePathMatch(triggerOn.mr!!.paths) &&
                isUserMatch(triggerOn.mr!!.users)) {
            return true
        }

        return false
    }

    private fun isSchedulesMatch(triggerOn: TriggerOn, eventBranch: String): Boolean {
        // TODO 需要进一步迭代定时任务的检查与分支匹配检测
        if (triggerOn.schedules == null || triggerOn.schedules?.cron.isNullOrBlank()) {
            logger.info("The schedules cron is invalid($eventBranch)")
            return false
        }
        return true
    }

    private fun isIgnoreBranchMatch(branchList: List<String>?): Boolean {
        val eventBranch = getBranch()
        if (branchList != null && branchList.isNotEmpty()) {
            branchList.forEach {
                if (isBranchMatch(it, eventBranch)) {
                    logger.info("The branchesIgnore($it) exclude the git branch ($eventBranch)")
                    return true
                }
            }
        }

        return false
    }

    private fun isIgnorePathMatch(pathIgnoreList: List<String>?): Boolean {
        if (pathIgnoreList != null && pathIgnoreList.isNotEmpty()) {
            logger.info("Exclude path set ($pathIgnoreList)")
            (event as GitPushEvent).commits.forEach { commit ->
                commit.added?.forEach { path ->
                    pathIgnoreList.forEach { excludePath ->
                        if (isPathMatch(path, excludePath)) {
                            logger.info("The exclude path($excludePath) exclude the git update one($path)")
                            return true
                        }
                    }
                }
                commit.modified?.forEach { path ->
                    pathIgnoreList.forEach { excludePath ->
                        if (isPathMatch(path, excludePath)) {
                            logger.info("The exclude path($excludePath) exclude the git update one($path)")
                            return true
                        }
                    }
                }
                commit.removed?.forEach { path ->
                    pathIgnoreList.forEach { excludePath ->
                        if (isPathMatch(path, excludePath)) {
                            logger.info("The exclude path($excludePath) exclude the git update one($path)")
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    private fun isBranchMatch(branchList: List<String>?): Boolean {
        var branchIncluded = false
        val eventBranch = getBranch()
        if (branchList != null && branchList.isNotEmpty()) {
            logger.info("Include branch set($branchList)")
            if (branchList.size == 1 && branchList[0] == "*") {
                branchIncluded = true
            } else {
                run outside@{
                    branchList.forEach {
                        if (isBranchMatch(it, eventBranch)) {
                            logger.info("The include branch($it) include the git update one($eventBranch)")
                            branchIncluded = true
                            return@outside
                        }
                    }
                }
            }
        }

        return branchIncluded
    }

    private fun isIncludePathMatch(pathList: List<String>?): Boolean {
        var pathIncluded = false
        if (pathList != null && pathList.isNotEmpty()) {
            logger.info("Include path set($pathList)")
            run outside@{
                (event as GitPushEvent).commits.forEach { commit ->
                    commit.added?.forEach { path ->
                        pathList.forEach { includePath ->
                            if (isPathMatch(path, includePath)) {
                                logger.info("The include path($includePath) include the git update one($path)")
                                pathIncluded = true
                                return@outside
                            }
                        }
                    }
                    commit.modified?.forEach { path ->
                        pathList.forEach { includePath ->
                            if (isPathMatch(path, includePath)) {
                                logger.info("The include path($includePath) include the git update one($path)")
                                pathIncluded = true
                                return@outside
                            }
                        }
                    }
                    commit.removed?.forEach { path ->
                        pathList.forEach { includePath ->
                            if (isPathMatch(path, includePath)) {
                                logger.info("The include path($includePath) include the git update one($path)")
                                pathIncluded = true
                                return@outside
                            }
                        }
                    }
                }
            }
        } else {
            logger.info("trigger path include is empty.")
            pathIncluded = true
        }

        return pathIncluded
    }

    private fun isUserMatch(userList: List<String>?): Boolean {
        return if (userList != null && userList.isNotEmpty()) {
            userList.contains(getUser())
        } else {
            true
        }
    }

    private fun isIgnoreUserMatch(ignoreUserList: List<String>?): Boolean {
        if (ignoreUserList != null && ignoreUserList.isEmpty() && ignoreUserList.contains(getUser())) {
            return true
        }

        return false
    }

    private fun isBranchMatch(branchName: String, ref: String): Boolean {
        val eventBranch = ref.removePrefix("refs/heads/")
        return matcher.match(branchName, eventBranch)
    }

    private fun isTagMatch(tagName: String, ref: String): Boolean {
        val eventTag = ref.removePrefix("refs/tags/")
        return matcher.match(tagName, eventTag)
    }

    private fun matchUrl(url: String): Boolean {
        return when (event) {
            is GitPushEvent -> {
                val repoHttpUrl = url.removePrefix("http://").removePrefix("https://")
                val eventHttpUrl =
                        event.repository.git_http_url.removePrefix("http://").removePrefix("https://")
                url == event.repository.git_ssh_url || repoHttpUrl == eventHttpUrl
            }
            is GitTagPushEvent -> {
                val repoHttpUrl = url.removePrefix("http://").removePrefix("https://")
                val eventHttpUrl =
                        event.repository.git_http_url.removePrefix("http://").removePrefix("https://")
                url == event.repository.git_ssh_url || repoHttpUrl == eventHttpUrl
            }
            is GitMergeRequestEvent -> {
                val repoHttpUrl = url.removePrefix("http://").removePrefix("https://")
                val eventHttpUrl =
                        event.object_attributes.target.http_url.removePrefix("http://").removePrefix("https://")
                url == event.object_attributes.target.ssh_url || repoHttpUrl == eventHttpUrl
            }
            else -> {
                false
            }
        }
    }

    private fun getUser(): String {
        return when (event) {
            is GitPushEvent -> event.user_name
            is GitTagPushEvent -> event.user_name
            is GitMergeRequestEvent -> event.user.username
            else -> ""
        }
    }

    private fun getBranch(): String {
        return when (event) {
            is GitPushEvent -> getBranch(event.ref)
            is GitTagPushEvent -> getBranch(event.ref)
            is GitMergeRequestEvent -> event.object_attributes.target_branch
            else -> ""
        }
    }

    private fun getTag(): String {
        return when (event) {
            is GitPushEvent -> getTag(event.ref)
            is GitTagPushEvent -> getTag(event.ref)
            is GitMergeRequestEvent -> event.object_attributes.target_branch
            else -> ""
        }
    }

    private fun getEventType(): CodeEventType {
        return when (event) {
            is GitPushEvent -> CodeEventType.PUSH
            is GitTagPushEvent -> CodeEventType.TAG_PUSH
            is GitMergeRequestEvent -> CodeEventType.MERGE_REQUEST
            else -> CodeEventType.PUSH
        }
    }

    /**
     * Check if the path match
     * example:
     * fullPath: a/1.txt
     * prefixPath: a/
     */
    private fun isPathMatch(fullPath: String, prefixPath: String): Boolean {
        return fullPath.removePrefix("/").startsWith(prefixPath.removePrefix("/"))
    }

    private fun getBranch(ref: String): String {
        return ref.removePrefix("refs/heads/")
    }

    private fun getTag(ref: String): String {
        return ref.removePrefix("refs/tags/")
    }
}
