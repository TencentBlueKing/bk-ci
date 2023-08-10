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

package com.tencent.devops.process.yaml.modelTransfer

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerData
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerInput
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.yaml.modelTransfer.inner.TransferModelCreator
import com.tencent.devops.process.yaml.modelTransfer.pojo.WebHookTriggerElementChanger
import com.tencent.devops.process.yaml.v2.models.on.IssueRule
import com.tencent.devops.process.yaml.v2.models.on.MrRule
import com.tencent.devops.process.yaml.v2.models.on.NoteRule
import com.tencent.devops.process.yaml.v2.models.on.PushRule
import com.tencent.devops.process.yaml.v2.models.on.ReviewRule
import com.tencent.devops.process.yaml.v2.models.on.TagRule
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TriggerTransfer @Autowired(required = false) constructor(
    val client: Client,
    @Autowired(required = false)
    val creator: TransferModelCreator,
    val transferCache: TransferCacheService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TriggerTransfer::class.java)
    }

    @Suppress("ComplexMethod")
    fun yaml2TriggerGit(triggerOn: TriggerOn, elementQueue: MutableList<Element>) {
        if (triggerOn.manual != "disabled") {
            elementQueue.add(
                ManualTriggerElement(
                    I18nUtil.getCodeLanMessage(CommonMessageCode.BK_MANUAL_TRIGGER),
                    "T-1-1-1"
                )
            )
        }

        triggerOn.push?.let { push ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    branchName = push.branches.nonEmptyOrNull()?.join(),
                    excludeBranchName = push.branchesIgnore.nonEmptyOrNull()?.join(),
                    includePaths = push.paths.nonEmptyOrNull()?.join(),
                    excludePaths = push.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = push.users,
                    excludeUsers = push.usersIgnore,
                    eventType = CodeEventType.PUSH,
                    // todo action
                    repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                    repositoryHashId = triggerOn.repoHashId,
                    repositoryName = triggerOn.name
                )
            )
        }

        triggerOn.tag?.let { tag ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    tagName = tag.tags.nonEmptyOrNull()?.join(),
                    excludeTagName = tag.tagsIgnore.nonEmptyOrNull()?.join(),
                    fromBranches = tag.fromBranches.nonEmptyOrNull()?.join(),
                    includeUsers = tag.users,
                    excludeUsers = tag.usersIgnore,
                    eventType = CodeEventType.TAG_PUSH,
                    repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                    repositoryHashId = triggerOn.repoHashId,
                    repositoryName = triggerOn.name
                )
            )
        }

        triggerOn.mr?.let { mr ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    branchName = mr.targetBranches.nonEmptyOrNull()?.join(),
                    excludeBranchName = mr.targetBranchesIgnore.nonEmptyOrNull()?.join(),
                    includeSourceBranchName = mr.sourceBranches.nonEmptyOrNull()?.join(),
                    excludeSourceBranchName = mr.sourceBranchesIgnore.nonEmptyOrNull()?.join(),
                    includePaths = mr.paths.nonEmptyOrNull()?.join(),
                    excludePaths = mr.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = mr.users,
                    excludeUsers = mr.usersIgnore,
                    block = mr.block,
                    webhookQueue = mr.webhookQueue,
                    enableCheck = mr.enableCheck,
                    // todo action
                    eventType = CodeEventType.MERGE_REQUEST,
                    repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                    repositoryHashId = triggerOn.repoHashId,
                    repositoryName = triggerOn.name
                )
            )
        }

        triggerOn.review?.let { review ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    includeCrState = review.states,
                    includeCrTypes = review.types,
                    eventType = CodeEventType.REVIEW,
                    repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                    repositoryHashId = triggerOn.repoHashId,
                    repositoryName = triggerOn.name
                )
            )
        }

        triggerOn.issue?.let { issue ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    includeIssueAction = issue.action,
                    eventType = CodeEventType.ISSUES,
                    repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                    repositoryHashId = triggerOn.repoHashId,
                    repositoryName = triggerOn.name
                )
            )
        }

        triggerOn.note?.let { note ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    includeNoteTypes = note.types?.map {
                        when (it) {
                            "commit" -> "Commit"
                            "merge_request" -> "Review"
                            "issue" -> "Issue"
                            else -> it
                        }
                    },
                    includeNoteComment = note.comment.nonEmptyOrNull()?.join(),
                    eventType = CodeEventType.NOTE,
                    repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                    repositoryHashId = triggerOn.repoHashId,
                    repositoryName = triggerOn.name
                )
            )
        }
    }

    @Suppress("ComplexMethod")
    fun git2YamlTriggerOn(elements: List<WebHookTriggerElementChanger>): List<TriggerOn> {
        val fix = elements.groupBy {
            when (it.repositoryType) {
                RepositoryType.ID -> it.repositoryHashId ?: ""
                RepositoryType.NAME -> it.repositoryName ?: ""
                else -> ""
            }
        }
        val res = mutableMapOf<String, TriggerOn>()
        fix.forEach { (name, group) ->
            group.forEach { git ->
                val nowExist = res.getOrPut(name) {
                    when (name) {
                        git.repositoryHashId -> TriggerOn(repoHashId = name)
                        git.repositoryName -> TriggerOn(name = name)
                        else -> TriggerOn()
                    }
                }
                when (git.eventType) {
                    CodeEventType.PUSH -> nowExist.push = PushRule(
                        branches = git.branchName?.disjoin() ?: emptyList(),
                        branchesIgnore = git.excludeBranchName?.disjoin(),
                        paths = git.includePaths?.disjoin(),
                        pathsIgnore = git.excludePaths?.disjoin(),
                        users = git.includeUsers,
                        usersIgnore = git.excludeUsers,
                        // todo action
                        action = null
                    )
                    CodeEventType.TAG_PUSH -> nowExist.tag = TagRule(
                        tags = git.tagName?.disjoin(),
                        tagsIgnore = git.excludeTagName?.disjoin(),
                        fromBranches = git.fromBranches?.disjoin(),
                        users = git.includeUsers,
                        usersIgnore = git.excludeUsers
                    )
                    CodeEventType.MERGE_REQUEST -> nowExist.mr = MrRule(
                        targetBranches = git.branchName?.disjoin(),
                        targetBranchesIgnore = git.excludeBranchName?.disjoin(),
                        sourceBranches = git.includeSourceBranchName?.disjoin(),
                        sourceBranchesIgnore = git.excludeSourceBranchName?.disjoin(),
                        paths = git.includePaths?.disjoin(),
                        pathsIgnore = git.excludePaths?.disjoin(),
                        users = git.includeUsers,
                        usersIgnore = git.excludeUsers,
                        block = git.block,
                        webhookQueue = git.webhookQueue,
                        enableCheck = git.enableCheck,
                        // todo action
                        action = null
                    )
                    CodeEventType.REVIEW -> nowExist.review = ReviewRule(
                        states = git.includeCrState,
                        types = git.includeCrTypes
                    )
                    CodeEventType.ISSUES -> nowExist.issue = IssueRule(
                        action = git.includeIssueAction
                    )
                    CodeEventType.NOTE -> nowExist.note = NoteRule(
                        types = git.includeNoteTypes?.map {
                            when (it) {
                                "Commit" -> "commit"
                                "Review" -> "merge_request"
                                "Issue" -> "issue"
                                else -> it
                            }
                        }
                    )
                }
            }
//            res[name] = nowExist
        }
        return res.values.toList()
    }

    @Suppress("ComplexMethod")
    fun yaml2TriggerTGit(triggerOn: TriggerOn, elementQueue: MutableList<Element>) {
        triggerOn.push?.let { push ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    data = CodeTGitWebHookTriggerData(
                        input = CodeTGitWebHookTriggerInput(
                            branchName = push.branches.nonEmptyOrNull()?.join(),
                            excludeBranchName = push.branchesIgnore.nonEmptyOrNull()?.join(),
                            includePaths = push.paths.nonEmptyOrNull()?.join(),
                            excludePaths = push.pathsIgnore.nonEmptyOrNull()?.join(),
                            includeUsers = push.users,
                            excludeUsers = push.usersIgnore,
                            eventType = CodeEventType.PUSH,
                            // todo action
                            repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                            repositoryHashId = triggerOn.repoHashId,
                            repositoryName = triggerOn.name
                        )
                    )
                )
            )
        }

        triggerOn.tag?.let { tag ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    data = CodeTGitWebHookTriggerData(
                        input = CodeTGitWebHookTriggerInput(
                            tagName = tag.tags.nonEmptyOrNull()?.join(),
                            excludeTagName = tag.tagsIgnore.nonEmptyOrNull()?.join(),
                            fromBranches = tag.fromBranches.nonEmptyOrNull()?.join(),
                            includeUsers = tag.users,
                            excludeUsers = tag.usersIgnore,
                            eventType = CodeEventType.TAG_PUSH,
                            repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                            repositoryHashId = triggerOn.repoHashId,
                            repositoryName = triggerOn.name
                        )
                    )
                )
            )
        }

        triggerOn.mr?.let { mr ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    data = CodeTGitWebHookTriggerData(
                        input = CodeTGitWebHookTriggerInput(
                            branchName = mr.targetBranches.nonEmptyOrNull()?.join(),
                            excludeBranchName = mr.targetBranchesIgnore.nonEmptyOrNull()?.join(),
                            includeSourceBranchName = mr.sourceBranches.nonEmptyOrNull()?.join(),
                            excludeSourceBranchName = mr.sourceBranchesIgnore.nonEmptyOrNull()?.join(),
                            includePaths = mr.paths.nonEmptyOrNull()?.join(),
                            excludePaths = mr.pathsIgnore.nonEmptyOrNull()?.join(),
                            includeUsers = mr.users,
                            excludeUsers = mr.usersIgnore,
                            block = mr.block,
                            webhookQueue = mr.webhookQueue,
                            enableCheck = mr.enableCheck,
                            // todo action
                            eventType = CodeEventType.MERGE_REQUEST,
                            repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                            repositoryHashId = triggerOn.repoHashId,
                            repositoryName = triggerOn.name
                        )
                    )
                )
            )
        }

        triggerOn.review?.let { review ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    data = CodeTGitWebHookTriggerData(
                        input = CodeTGitWebHookTriggerInput(
                            includeCrState = review.states,
                            includeCrTypes = review.types,
                            eventType = CodeEventType.REVIEW,
                            repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                            repositoryHashId = triggerOn.repoHashId,
                            repositoryName = triggerOn.name
                        )
                    )
                )
            )
        }

        triggerOn.issue?.let { issue ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    data = CodeTGitWebHookTriggerData(
                        input = CodeTGitWebHookTriggerInput(
                            includeIssueAction = issue.action,
                            eventType = CodeEventType.ISSUES,
                            repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                            repositoryHashId = triggerOn.repoHashId,
                            repositoryName = triggerOn.name
                        )
                    )
                )
            )
        }

        triggerOn.note?.let { note ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    data = CodeTGitWebHookTriggerData(
                        input = CodeTGitWebHookTriggerInput(
                            includeNoteTypes = note.types?.map {
                                when (it) {
                                    "commit" -> "Commit"
                                    "merge_request" -> "Review"
                                    "issue" -> "Issue"
                                    else -> it
                                }
                            },
                            includeNoteComment = note.comment.nonEmptyOrNull()?.join(),
                            eventType = CodeEventType.NOTE,
                            repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                            repositoryHashId = triggerOn.repoHashId,
                            repositoryName = triggerOn.name
                        )
                    )
                )
            )
        }
    }

    @Suppress("ComplexMethod")
    fun yaml2TriggerGithub(triggerOn: TriggerOn, elementQueue: MutableList<Element>) {
        triggerOn.push?.let { push ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    branchName = push.branches.nonEmptyOrNull()?.join(),
                    excludeBranchName = push.branchesIgnore.nonEmptyOrNull()?.join(),
                    includePaths = push.paths.nonEmptyOrNull()?.join(),
                    excludePaths = push.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = push.users,
                    excludeUsers = push.usersIgnore.nonEmptyOrNull()?.join(),
                    eventType = CodeEventType.PUSH,
                    // todo action
                    repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                    repositoryHashId = triggerOn.repoHashId,
                    repositoryName = triggerOn.name
                )
            )
        }

        triggerOn.tag?.let { tag ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    tagName = tag.tags.nonEmptyOrNull()?.join(),
                    excludeTagName = tag.tagsIgnore.nonEmptyOrNull()?.join(),
                    fromBranches = tag.fromBranches.nonEmptyOrNull()?.join(),
                    includeUsers = tag.users,
                    excludeUsers = tag.usersIgnore.nonEmptyOrNull()?.join(),
                    eventType = CodeEventType.TAG_PUSH,
                    repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                    repositoryHashId = triggerOn.repoHashId,
                    repositoryName = triggerOn.name
                )
            )
        }

        triggerOn.mr?.let { mr ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    branchName = mr.targetBranches.nonEmptyOrNull()?.join(),
                    excludeBranchName = mr.targetBranchesIgnore.nonEmptyOrNull()?.join(),
                    includeSourceBranchName = mr.sourceBranches.nonEmptyOrNull()?.join(),
                    excludeSourceBranchName = mr.sourceBranchesIgnore.nonEmptyOrNull()?.join(),
                    includePaths = mr.paths.nonEmptyOrNull()?.join(),
                    excludePaths = mr.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = mr.users,
                    excludeUsers = mr.usersIgnore.nonEmptyOrNull()?.join(),
                    webhookQueue = mr.webhookQueue,
                    enableCheck = mr.enableCheck,
                    // todo action
                    eventType = CodeEventType.PULL_REQUEST,
                    repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                    repositoryHashId = triggerOn.repoHashId,
                    repositoryName = triggerOn.name
                )
            )
        }

        triggerOn.review?.let { review ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    includeCrState = review.states,
                    includeCrTypes = review.types,
                    eventType = CodeEventType.REVIEW,
                    repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                    repositoryHashId = triggerOn.repoHashId,
                    repositoryName = triggerOn.name
                )
            )
        }

        triggerOn.issue?.let { issue ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    includeIssueAction = issue.action,
                    eventType = CodeEventType.ISSUES,
                    repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                    repositoryHashId = triggerOn.repoHashId,
                    repositoryName = triggerOn.name
                )
            )
        }

        triggerOn.note?.let { note ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    includeNoteTypes = note.types?.map {
                        when (it) {
                            "commit" -> "Commit"
                            "merge_request" -> "Review"
                            "issue" -> "Issue"
                            else -> it
                        }
                    },
                    includeNoteComment = note.comment.nonEmptyOrNull()?.join(),
                    eventType = CodeEventType.NOTE,
                    repositoryType = if (triggerOn.name.isNullOrBlank()) RepositoryType.ID else RepositoryType.NAME,
                    repositoryHashId = triggerOn.repoHashId,
                    repositoryName = triggerOn.name
                )
            )
        }
    }

    private fun List<String>.join() = this.joinToString(separator = ",")

    private fun String.disjoin() = this.split(",")

    private fun List<String>?.nonEmptyOrNull() = this?.ifEmpty { null }
}
