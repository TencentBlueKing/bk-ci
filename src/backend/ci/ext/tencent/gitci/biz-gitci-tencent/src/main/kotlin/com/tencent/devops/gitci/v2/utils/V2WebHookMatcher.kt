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
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.gitci.pojo.git.GitEvent
import com.tencent.devops.gitci.pojo.git.GitMergeRequestEvent
import com.tencent.devops.gitci.pojo.git.GitPushEvent
import com.tencent.devops.gitci.pojo.git.GitTagPushEvent
import com.tencent.devops.common.ci.yaml.v2.MergeRequest
import com.tencent.devops.common.ci.yaml.v2.TriggerOn
import com.tencent.devops.process.utils.GIT_MR_NUMBER
import org.slf4j.LoggerFactory
import org.springframework.util.AntPathMatcher

class V2WebHookMatcher(private val event: GitEvent) {

    companion object {
        private val logger = LoggerFactory.getLogger(V2WebHookMatcher::class.java)
        private val matcher = AntPathMatcher()
    }

    fun isMatch(
        triggerOn: TriggerOn
    ): Boolean {
        val eventBranch = getBranch()
        val eventTag = getTag()
        val eventType = getEventType()

        when (eventType) {
            CodeEventType.PUSH -> {
                if (isPushMatch(triggerOn = triggerOn, eventBranch = eventBranch)) return true
            }
            CodeEventType.TAG_PUSH -> {
                if (isTagPushMatch(triggerOn = triggerOn, eventTag = eventTag)) return true
            }
            CodeEventType.MERGE_REQUEST -> {
                if (isMrMatch(triggerOn = triggerOn, eventBranch = eventBranch)) return true
            }
            else -> {
                return false
            }
        }

        logger.info("The include branch($eventBranch) or tag($eventTag) doesn't match the git event($event)")
        return false
    }

    private fun isPushMatch(triggerOn: TriggerOn, eventBranch: String): Boolean {
        // exclude
        if (triggerOn.push!!.branchesIgnore != null && triggerOn.push!!.branchesIgnore!!.isNotEmpty()) {
            triggerOn.push!!.branchesIgnore!!.forEach {
                if (isBranchMatch(it, eventBranch)) {
                    logger.info("The exclude branch($it) exclude the git branch ($eventBranch)")
                    return false
                }
            }
        }
        if (!isExcludePathMatch(triggerOn)) return false
        // include
        var branchIncluded = false
        if (triggerOn.push!!.branches != null && triggerOn.push!!.branches!!.isNotEmpty()) {
            logger.info("Include branch set(${triggerOn.push!!.branches})")
            run outside@{
                triggerOn.push!!.branches!!.forEach {
                    if (isBranchMatch(it, eventBranch)) {
                        logger.info("The include branch($it) include the git update one($eventBranch)")
                        branchIncluded = true
                        return@outside
                    }
                }
            }
        }
        val pathIncluded = isIncludePathMatch(triggerOn)
        if (branchIncluded && pathIncluded) {
            logger.info("Git trigger branch($eventBranch) is included and path(${triggerOn.push!!.paths}) is included")
            return true
        }
        return false
    }

    private fun isTagPushMatch(triggerOn: TriggerOn, eventTag: String): Boolean {
        // exclude
        if (triggerOn.tag?.tagsIgnore != null && triggerOn.tag!!.tagsIgnore!!.isNotEmpty()) {
            triggerOn.tag!!.tagsIgnore!!.forEach {
                if (isTagMatch(it, eventTag)) {
                    logger.info("The exclude tag($it) exclude the git tag ($eventTag)")
                    return false
                }
            }
        }
        if (!isExcludePathMatch(triggerOn)) return false
        // include
        var tagIncluded = false
        if (triggerOn.tag!!.tags != null && triggerOn.tag!!.tags!!.isNotEmpty()) {
            logger.info("Include tags set(${triggerOn.tag!!.tags})")
            run outside@{
                triggerOn.tag!!.tags!!.forEach {
                    if (isTagMatch(it, eventTag)) {
                        logger.info("The include tags($it) include the git update one($eventTag)")
                        tagIncluded = true
                        return@outside
                    }
                }
            }
        }
        val pathIncluded = isIncludePathMatch(triggerOn)
        if (tagIncluded && pathIncluded) {
            logger.info("Git trigger tags($eventTag) is included and path(${triggerOn.tag!!.tags}) is included")
            return true
        }
        return false
    }

    private fun isExcludePathMatch(triggerOn: TriggerOn): Boolean {
        if (triggerOn.push!!.pathsIgnore != null && triggerOn.push!!.pathsIgnore!!.isNotEmpty()) {
            logger.info("Exclude path set (${triggerOn.push!!.pathsIgnore})")
            (event as GitPushEvent).commits.forEach { commit ->
                commit.added?.forEach { path ->
                    triggerOn.push!!.pathsIgnore!!.forEach { excludePath ->
                        if (isPathMatch(path, excludePath)) {
                            logger.info("The exclude path($excludePath) exclude the git update one($path)")
                            return false
                        }
                    }
                }
                commit.modified?.forEach { path ->
                    triggerOn.push!!.pathsIgnore!!.forEach { excludePath ->
                        if (isPathMatch(path, excludePath)) {
                            logger.info("The exclude path($excludePath) exclude the git update one($path)")
                            return false
                        }
                    }
                }
                commit.removed?.forEach { path ->
                    triggerOn.push!!.pathsIgnore!!.forEach { excludePath ->
                        if (isPathMatch(path, excludePath)) {
                            logger.info("The exclude path($excludePath) exclude the git update one($path)")
                            return false
                        }
                    }
                }
            }
        }
        return true
    }

    private fun isIncludePathMatch(triggerOn: TriggerOn): Boolean {
        var pathIncluded = false
        if (triggerOn.push!!.paths != null && triggerOn.push!!.paths!!.isNotEmpty()) {
            logger.info("Include path set(${triggerOn.push!!.paths})")
            run outside@{
                (event as GitPushEvent).commits.forEach { commit ->
                    commit.added?.forEach { path ->
                        triggerOn.push!!.paths!!.forEach { includePath ->
                            if (isPathMatch(path, includePath)) {
                                logger.info("The include path($includePath) include the git update one($path)")
                                pathIncluded = true
                                return@outside
                            }
                        }
                    }
                    commit.modified?.forEach { path ->
                        triggerOn.push!!.paths!!.forEach { includePath ->
                            if (isPathMatch(path, includePath)) {
                                logger.info("The include path($includePath) include the git update one($path)")
                                pathIncluded = true
                                return@outside
                            }
                        }
                    }
                    commit.removed?.forEach { path ->
                        triggerOn.push!!.paths!!.forEach { includePath ->
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

    private fun isMrMatch(triggerOn: TriggerOn, eventBranch: String): Boolean {
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
        // mr 没有path？

        // include branch of mr
        if (triggerOn.mr!!.targetBranches != null && triggerOn.mr!!.targetBranches!!.isNotEmpty()) {
            logger.info("Include branch set(${triggerOn.mr!!.targetBranches})")
            triggerOn.mr!!.targetBranches!!.forEach {
                if (isBranchMatch(it, eventBranch)) {
                    logger.info("The include branch($it) include the git update one($eventBranch)")
                    return true
                }
            }
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

    fun getUsername(): String {
        return when (event) {
            is GitPushEvent -> event.user_name
            is GitTagPushEvent -> event.user_name
            is GitMergeRequestEvent -> event.user.username
            else -> ""
        }
    }

    fun getRevision(): String {
        return when (event) {
            is GitPushEvent -> event.checkout_sha ?: ""
            is GitTagPushEvent -> event.checkout_sha ?: ""
            is GitMergeRequestEvent -> event.object_attributes.last_commit.id
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

    fun getHookSourceBranch(): String? {
        return if (event is GitMergeRequestEvent) event.object_attributes.source_branch else null
    }

    fun getHookTargetBranch(): String? {
        return if (event is GitMergeRequestEvent) event.object_attributes.target_branch else null
    }

    fun getHookSourceUrl(): String? {
        return if (event is GitMergeRequestEvent) event.object_attributes.source.http_url else null
    }

    fun getHookTargetUrl(): String? {
        return if (event is GitMergeRequestEvent) event.object_attributes.target.http_url else null
    }

    fun getCodeType() = CodeType.GIT

    fun getEnv(): Map<String, Any> {
        if (event is GitMergeRequestEvent) {
            return mapOf(GIT_MR_NUMBER to event.object_attributes.iid)
        }
        return emptyMap<String, Any>()
    }

    fun getRepoName(): String {
        val sshUrl = when (event) {
            is GitPushEvent -> event.repository.git_ssh_url
            is GitTagPushEvent -> event.repository.git_ssh_url
            is GitMergeRequestEvent -> event.object_attributes.target.ssh_url
            else -> ""
        }
        return sshUrl.removePrefix("git@git.code.oa.com:").removeSuffix(".git")
    }

    fun getBranchName(): String {
        return when (event) {
            is GitPushEvent -> org.eclipse.jgit.lib.Repository.shortenRefName(event.ref)
            is GitTagPushEvent -> org.eclipse.jgit.lib.Repository.shortenRefName(event.ref)
            is GitMergeRequestEvent -> event.object_attributes.target_branch
            else -> ""
        }
    }

    fun getMergeRequestId(): Long? {
        return when (event) {
            is GitMergeRequestEvent -> event.object_attributes.id
            else -> null
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
