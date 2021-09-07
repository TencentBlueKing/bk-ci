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

package com.tencent.devops.stream.trigger.parsers

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.TriggerOn
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.stream.common.exception.CommitCheck
import com.tencent.devops.stream.common.exception.TriggerBaseException
import com.tencent.devops.stream.common.exception.TriggerException
import com.tencent.devops.stream.common.exception.Yamls
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.GitCICommitCheckState
import com.tencent.devops.stream.pojo.enums.StreamMrEventAction
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.pojo.git.GitCommit
import com.tencent.devops.stream.pojo.git.GitEvent
import com.tencent.devops.stream.pojo.git.GitMergeRequestEvent
import com.tencent.devops.stream.pojo.git.GitPushEvent
import com.tencent.devops.stream.pojo.git.GitTagPushEvent
import com.tencent.devops.stream.trigger.template.pojo.NoReplaceTemplate
import com.tencent.devops.stream.v2.service.ScmService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import java.util.regex.Pattern

@Suppress("ComplexMethod", "NestedBlockDepth", "ComplexCondition")
@Component
class TriggerMatcher @Autowired constructor(
    private val scmService: ScmService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TriggerMatcher::class.java)
        private val matcher = AntPathMatcher()
    }

    @Throws(TriggerBaseException::class)
    fun isMatch(
        event: GitEvent,
        gitRequestEvent: GitRequestEvent,
        pipeline: GitProjectPipeline,
        originYaml: String
    ): Pair<Boolean, Boolean> {
        val newYaml = try {
            // 触发器需要将 on: 转为 TriggerOn:
            val realYaml = ScriptYmlUtils.formatYaml(originYaml)
            YamlUtil.getObjectMapper().readValue(realYaml, object : TypeReference<NoReplaceTemplate>() {})
        } catch (e: Throwable) {
            when (e) {
                is JsonProcessingException, is TypeCastException -> {
                    TriggerException.triggerError(
                        request = gitRequestEvent,
                        event = event,
                        pipeline = pipeline,
                        reason = TriggerReason.CI_YAML_INVALID,
                        reasonParams = listOf(e.message ?: ""),
                        yamls = Yamls(originYaml, null, null),
                        commitCheck = CommitCheck(
                            block = event is GitMergeRequestEvent,
                            state = GitCICommitCheckState.FAILURE
                        )
                    )
                }
                else -> {
                    throw e
                }
            }
        }
        return isMatch(
            triggerOn = ScriptYmlUtils.formatTriggerOn(newYaml.triggerOn),
            event = event,
            gitRequestEvent = gitRequestEvent
        )
    }

    @Throws(ErrorCodeException::class)
    private fun isMatch(
        triggerOn: TriggerOn,
        event: GitEvent,
        gitRequestEvent: GitRequestEvent
    ): Pair<Boolean, Boolean> {
        val eventBranch = getBranch(event)
        val eventTag = getTag(event)
        val eventType = getEventType(event)

        if (triggerOn.mr != null || triggerOn.push != null || triggerOn.tag != null) {
            when (eventType) {
                CodeEventType.PUSH -> {
                    if (isPushMatch(triggerOn, eventBranch, event)) return Pair(first = true, second = false)
                }
                CodeEventType.TAG_PUSH -> {
                    if (isTagPushMatch(triggerOn, eventTag, event)) return Pair(first = true, second = false)
                }
                CodeEventType.MERGE_REQUEST -> {
                    if (
                        isMrMatch(
                            triggerOn = triggerOn,
                            eventBranch = eventBranch,
                            event = event,
                            gitRequestEvent = gitRequestEvent
                        )
                    ) return Pair(first = true, second = false)
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

    private fun isPushMatch(triggerOn: TriggerOn, eventBranch: String, event: GitEvent): Boolean {
        // 如果没有配置push，默认匹配
        if (triggerOn.push == null) {
            return false
        }

        val pushRule = triggerOn.push!!
        // 1、check branchIgnore，满足屏蔽条件直接返回不匹配
        if (isIgnoreBranchMatch(pushRule.branchesIgnore, event)) return false

        // 2、check pathIgnore，满足屏蔽条件直接返回不匹配
        if (isIgnorePathMatch(pushRule.pathsIgnore, event)) return false

        // 3、check userIgnore,满足屏蔽条件直接返回不匹配
        if (isIgnoreUserMatch(pushRule.usersIgnore, event)) return false

        // include
        if (isBranchMatch(pushRule.branches, event) && isIncludePathMatch(pushRule.paths, event) &&
            isUserMatch(pushRule.users, event)) {
            logger.info("Git trigger branch($eventBranch) is included and path(${pushRule.paths}) is included")
            return true
        }
        return false
    }

    private fun isTagPushMatch(triggerOn: TriggerOn, eventTag: String, event: GitEvent): Boolean {
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

        if (isIgnoreUserMatch(tagRule.usersIgnore, event)) return false

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

        if (tagIncluded && isUserMatch(tagRule.users, event)) {
            logger.info("Git trigger tags($eventTag) is included and path(${triggerOn.tag!!.tags}) is included")
            return true
        }
        return false
    }

    private fun isMrMatch(
        triggerOn: TriggerOn,
        eventBranch: String,
        event: GitEvent,
        gitRequestEvent: GitRequestEvent
    ): Boolean {
        if (triggerOn.mr == null) {
            return false
        }

        // 先排除exclude和ignore
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

        if (isIgnorePathMatch(triggerOn.mr!!.pathsIgnore, event)) return false
        if (isIgnoreUserMatch(triggerOn.mr!!.usersIgnore, event)) return false

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

        if (targetBranchMatch &&
            isMrIncludePathMatch(triggerOn.mr!!.paths, event, gitRequestEvent) &&
            isUserMatch(triggerOn.mr!!.users, event) &&
            isMrActionMatch(triggerOn.mr!!.action, event)) {
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

    private fun isIgnoreBranchMatch(branchList: List<String>?, event: GitEvent): Boolean {
        val eventBranch = getBranch(event)
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

    private fun isIgnorePathMatch(pathIgnoreList: List<String>?, event: GitEvent): Boolean {
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

    private fun isBranchMatch(branchList: List<String>?, event: GitEvent): Boolean {
        var branchIncluded = false
        val eventBranch = getBranch(event)
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

    private fun isIncludePathMatch(pathList: List<String>?, event: GitEvent): Boolean {
        var pathIncluded = false
        if (pathList != null && pathList.isNotEmpty()) {
            logger.info("Include path set($pathList)")
            run outside@{
                var commits = listOf<GitCommit>()
                if (event is GitPushEvent) {
                    commits = event.commits
                }

                commits.forEach { commit ->
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

    private fun isMrIncludePathMatch(
        pathList: List<String>?,
        event: GitEvent,
        gitRequestEvent: GitRequestEvent
    ): Boolean {
        var mrPathIncluded = false
        if (pathList != null && pathList.isNotEmpty()) {
            logger.info("Mr Include path set($pathList)")
            val mrId = (event as GitMergeRequestEvent).object_attributes.id
            // 使用本次触发的项目ID，不然fork库过来的请求未开启CI无法触发
            val gitProjectId = gitRequestEvent.gitProjectId
            val gitMrChangeInfo = scmService.getMergeRequestChangeInfo(
                userId = event.user.name,
                token = null,
                gitProjectId = gitProjectId,
                mrId = mrId
            )

            if (gitMrChangeInfo != null) {
                val mrChangeFiles = gitMrChangeInfo.files.map {
                    if (it.deletedFile) {
                        it.oldPath
                    } else {
                        it.newPath
                    }
                }

                run outside@{
                    mrChangeFiles.forEach { changeFilePath ->
                        pathList.forEach { includePath ->
                            if (isPathMatch(changeFilePath, includePath)) {
                                logger.info("The include path($includePath) " +
                                    "include the git mr update one($changeFilePath)")
                                mrPathIncluded = true
                                return@outside
                            }
                        }
                    }
                }
            } else {
                logger.info("isMrIncludePathMatch gitMrChangeInfo is null.")
            }
        } else {
            logger.info("isMrIncludePathMatch trigger path include is empty.")
            mrPathIncluded = true
        }

        return mrPathIncluded
    }

    private fun isUserMatch(userList: List<String>?, event: GitEvent): Boolean {
        return if (userList != null && userList.isNotEmpty()) {
            userList.contains(getUser(event))
        } else {
            true
        }
    }

    private fun isIgnoreUserMatch(ignoreUserList: List<String>?, event: GitEvent): Boolean {
        if (ignoreUserList != null && ignoreUserList.isEmpty() && ignoreUserList.contains(getUser(event))) {
            return true
        }

        return false
    }

    private fun isBranchMatch(branchName: String, ref: String): Boolean {
        val eventBranch = ref.removePrefix("refs/heads/")
        return matcher.match(branchName.replace("*", "**"), eventBranch)
    }

    private fun isTagMatch(tagName: String, ref: String): Boolean {
        val eventTag = ref.removePrefix("refs/tags/")
        return matcher.match(tagName.replace("*", "**"), eventTag)
    }

    /**
     * Check if the path match
     * example:
     * fullPath: a/1.txt
     * prefixPath: a/
     */
    private fun isPathMatch(fullPath: String, prefixPath: String): Boolean {
        logger.info("fullPath: $fullPath, prefixPath: $prefixPath")
        val fullPathList = fullPath.removePrefix("/").split("/")
        val prefixPathList = prefixPath.removePrefix("/").split("/")
        if (fullPathList.size < prefixPathList.size) {
            return false
        }

        for (i in prefixPathList.indices) {
            val pattern = Pattern.compile(prefixPathList[i].replace("*", "\\S*"))
            val matcher = pattern.matcher(fullPathList[i])
            if (prefixPathList[i] != "*" && !matcher.matches()) {
                return false
            }
        }

        return true
    }

    private fun isMrActionMatch(actionList: List<String>?, event: GitEvent): Boolean {
        val realActionList = if (actionList.isNullOrEmpty()) {
            // 缺省时使用默认值
            listOf(
                StreamMrEventAction.OPEN.value,
                StreamMrEventAction.REOPEN.value,
                StreamMrEventAction.PUSH_UPDATE.value
            )
        } else {
            actionList
        }

        val mrAction = StreamMrEventAction.getActionValue(event as GitMergeRequestEvent) ?: false
        realActionList.forEach {
            if (it == mrAction) {
                return true
            }
        }

        return false
    }

    private fun getUser(event: GitEvent): String {
        return when (event) {
            is GitPushEvent -> event.user_name
            is GitTagPushEvent -> event.user_name
            is GitMergeRequestEvent -> event.user.username
            else -> ""
        }
    }

    private fun getBranch(event: GitEvent): String {
        return when (event) {
            is GitPushEvent -> getBranch(event.ref)
            is GitTagPushEvent -> getBranch(event.ref)
            is GitMergeRequestEvent -> event.object_attributes.target_branch
            else -> ""
        }
    }

    private fun getTag(event: GitEvent): String {
        return when (event) {
            is GitPushEvent -> getTag(event.ref)
            is GitTagPushEvent -> getTag(event.ref)
            is GitMergeRequestEvent -> event.object_attributes.target_branch
            else -> ""
        }
    }

    private fun getEventType(event: GitEvent): CodeEventType {
        return when (event) {
            is GitPushEvent -> CodeEventType.PUSH
            is GitTagPushEvent -> CodeEventType.TAG_PUSH
            is GitMergeRequestEvent -> CodeEventType.MERGE_REQUEST
            else -> CodeEventType.PUSH
        }
    }

    private fun getBranch(ref: String): String {
        return ref.removePrefix("refs/heads/")
    }

    private fun getTag(ref: String): String {
        return ref.removePrefix("refs/tags/")
    }
}
