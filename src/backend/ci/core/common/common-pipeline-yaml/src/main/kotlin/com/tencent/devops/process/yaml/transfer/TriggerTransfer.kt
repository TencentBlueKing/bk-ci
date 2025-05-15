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

package com.tencent.devops.process.yaml.transfer

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.TriggerRepositoryType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerData
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerInput
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerData
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerInput
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.PathFilterType
import com.tencent.devops.common.webhook.enums.code.tgit.TGitMrEventAction
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushActionType
import com.tencent.devops.process.yaml.transfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectWrapper
import com.tencent.devops.process.yaml.transfer.inner.TransferCreator
import com.tencent.devops.process.yaml.transfer.pojo.WebHookTriggerElementChanger
import com.tencent.devops.process.yaml.transfer.pojo.YamlTransferInput
import com.tencent.devops.process.yaml.v3.models.on.CustomFilter
import com.tencent.devops.process.yaml.v3.models.on.EnableType
import com.tencent.devops.process.yaml.v3.models.on.IssueRule
import com.tencent.devops.process.yaml.v3.models.on.MrRule
import com.tencent.devops.process.yaml.v3.models.on.NoteRule
import com.tencent.devops.process.yaml.v3.models.on.PushRule
import com.tencent.devops.process.yaml.v3.models.on.ReviewRule
import com.tencent.devops.process.yaml.v3.models.on.TagRule
import com.tencent.devops.process.yaml.v3.models.on.TriggerOn
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TriggerTransfer @Autowired(required = false) constructor(
    val client: Client,
    @Autowired(required = false)
    val creator: TransferCreator,
    val transferCache: TransferCacheService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TriggerTransfer::class.java)
    }

    @Suppress("ComplexMethod")
    fun yaml2TriggerGit(triggerOn: TriggerOn, elementQueue: MutableList<Element>) {
        val repositoryType = if (triggerOn.repoName.isNullOrBlank()) {
            TriggerRepositoryType.SELF
        } else {
            TriggerRepositoryType.NAME
        }
        triggerOn.push?.let { push ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    name = push.name ?: "Git事件触发",
                    branchName = push.branches.nonEmptyOrNull()?.join(),
                    excludeBranchName = push.branchesIgnore.nonEmptyOrNull()?.join(),
                    includePaths = push.paths.nonEmptyOrNull()?.join(),
                    excludePaths = push.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = push.users,
                    excludeUsers = push.usersIgnore,
                    pathFilterType = push.pathFilterType?.let { PathFilterType.valueOf(it) }
                        ?: PathFilterType.NamePrefixFilter,
                    eventType = CodeEventType.PUSH,
                    includePushAction = push.action ?: listOf(
                        TGitPushActionType.PUSH_FILE.value,
                        TGitPushActionType.NEW_BRANCH.value
                    ),
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName,
                    enableThirdFilter = !push.custom?.url.isNullOrBlank(),
                    thirdUrl = push.custom?.url,
                    thirdSecretToken = push.custom?.credentials
                ).checkTriggerElementEnable(push.enable).apply {
                    version = "2.*"
                }
            )
        }

        triggerOn.tag?.let { tag ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    name = tag.name ?: "Git事件触发",
                    tagName = tag.tags.nonEmptyOrNull()?.join(),
                    excludeTagName = tag.tagsIgnore.nonEmptyOrNull()?.join(),
                    fromBranches = tag.fromBranches.nonEmptyOrNull()?.join(),
                    includeUsers = tag.users,
                    excludeUsers = tag.usersIgnore,
                    eventType = CodeEventType.TAG_PUSH,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(tag.enable).apply {
                    version = "2.*"
                }
            )
        }

        triggerOn.mr?.let { mr ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    name = mr.name ?: "Git事件触发",
                    branchName = mr.targetBranches.nonEmptyOrNull()?.join(),
                    excludeBranchName = mr.targetBranchesIgnore.nonEmptyOrNull()?.join(),
                    includeSourceBranchName = mr.sourceBranches.nonEmptyOrNull()?.join(),
                    excludeSourceBranchName = mr.sourceBranchesIgnore.nonEmptyOrNull()?.join(),
                    includePaths = mr.paths.nonEmptyOrNull()?.join(),
                    excludePaths = mr.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = mr.users,
                    excludeUsers = mr.usersIgnore,
                    block = mr.blockMr,
                    webhookQueue = mr.webhookQueue,
                    enableCheck = mr.reportCommitCheck,
                    pathFilterType = mr.pathFilterType?.let { PathFilterType.valueOf(it) }
                        ?: PathFilterType.NamePrefixFilter,
                    includeMrAction = mr.action ?: listOf(
                        TGitMrEventAction.OPEN.value,
                        TGitMrEventAction.REOPEN.value,
                        TGitMrEventAction.PUSH_UPDATE.value
                    ),
                    eventType = CodeEventType.MERGE_REQUEST,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName,
                    enableThirdFilter = !mr.custom?.url.isNullOrBlank(),
                    thirdUrl = mr.custom?.url,
                    thirdSecretToken = mr.custom?.credentials,
                    skipWip = mr.skipWip
                ).checkTriggerElementEnable(mr.enable).apply {
                    version = "2.*"
                }
            )
        }

        triggerOn.review?.let { review ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    name = review.name ?: "Git事件触发",
                    includeCrState = review.states,
                    includeCrTypes = review.types,
                    eventType = CodeEventType.REVIEW,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(review.enable).apply {
                    version = "2.*"
                }
            )
        }

        triggerOn.issue?.let { issue ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    name = issue.name ?: "Git事件触发",
                    includeIssueAction = issue.action,
                    eventType = CodeEventType.ISSUES,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(issue.enable).apply {
                    version = "2.*"
                }
            )
        }

        triggerOn.note?.let { note ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    name = note.name ?: "Git事件触发",
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
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(note.enable).apply {
                    version = "2.*"
                }
            )
        }
    }

    @Suppress("ComplexMethod")
    fun git2YamlTriggerOn(
        elements: List<WebHookTriggerElementChanger>,
        projectId: String,
        aspectWrapper: PipelineTransferAspectWrapper,
        defaultName: String
    ): List<TriggerOn> {
        val res = mutableMapOf<String, TriggerOn>()
        val indexName = mutableMapOf<String, Int>()
        elements.forEach { git ->
            val name = when (git.repositoryType) {
                TriggerRepositoryType.ID -> git.repositoryHashId ?: ""
                TriggerRepositoryType.NAME -> git.repositoryName ?: ""
                TriggerRepositoryType.SELF -> "self"
                else -> ""
            }
            val repoName = when (name) {
                git.repositoryHashId -> transferCache.getGitRepository(projectId, RepositoryType.ID, name)
                    ?.aliasName ?: "unknown"

                git.repositoryName -> name
                /* self 或其他状况为 null*/
                else -> null
            }
            /*由于存在多个相同代码库并且配置了相同触发条件的触发器，所以需要设计存储多个触发器*/
            val index = indexName.getOrPut("$name-${git.eventType}") { 0 }
            val nowExist = res.getOrPut("$name-$index") {
                TriggerOn(repoName = repoName)
            }
            indexName["$name-${git.eventType}"] = index + 1
            when (git.eventType) {
                CodeEventType.PUSH -> nowExist.push = PushRule(
                    name = git.name.nullIfDefault(defaultName),
                    enable = git.enable.nullIfDefault(true),
                    branches = git.branchName?.disjoin(),
                    branchesIgnore = git.excludeBranchName?.disjoin(),
                    paths = git.includePaths?.disjoin(),
                    pathsIgnore = git.excludePaths?.disjoin(),
                    users = git.includeUsers,
                    usersIgnore = git.excludeUsers,
                    pathFilterType = git.pathFilterType?.name.nullIfDefault(PathFilterType.NamePrefixFilter.name),
                    action = git.includePushAction,
                    custom = if (git.enableThirdFilter == true) CustomFilter(
                        url = git.thirdUrl,
                        credentials = git.thirdSecretToken
                    ) else null
                )

                CodeEventType.TAG_PUSH -> nowExist.tag = TagRule(
                    name = git.name.nullIfDefault(defaultName),
                    enable = git.enable.nullIfDefault(true),
                    tags = git.tagName?.disjoin(),
                    tagsIgnore = git.excludeTagName?.disjoin(),
                    fromBranches = git.fromBranches?.disjoin(),
                    users = git.includeUsers,
                    usersIgnore = git.excludeUsers
                )

                CodeEventType.MERGE_REQUEST -> nowExist.mr = MrRule(
                    name = git.name.nullIfDefault(defaultName),
                    enable = git.enable.nullIfDefault(true),
                    targetBranches = git.branchName?.disjoin(),
                    targetBranchesIgnore = git.excludeBranchName?.disjoin(),
                    sourceBranches = git.includeSourceBranchName?.disjoin(),
                    sourceBranchesIgnore = git.excludeSourceBranchName?.disjoin(),
                    paths = git.includePaths?.disjoin(),
                    pathsIgnore = git.excludePaths?.disjoin(),
                    users = git.includeUsers,
                    usersIgnore = git.excludeUsers,
                    blockMr = git.block,
                    webhookQueue = git.webhookQueue.nullIfDefault(false),
                    reportCommitCheck = git.enableCheck.nullIfDefault(true),
                    pathFilterType = git.pathFilterType?.name.nullIfDefault(PathFilterType.NamePrefixFilter.name),
                    action = git.includeMrAction,
                    custom = if (git.enableThirdFilter == true) CustomFilter(
                        url = git.thirdUrl,
                        credentials = git.thirdSecretToken
                    ) else null,
                    skipWip = git.skipWip
                )
                CodeEventType.MERGE_REQUEST_ACCEPT ->
                    throw PipelineTransferException(
                        errorCode = CommonMessageCode.MR_ACCEPT_EVENT_NOT_SUPPORT_TRANSFER,
                        params = arrayOf(git.name)
                    )

                CodeEventType.REVIEW -> nowExist.review = ReviewRule(
                    name = git.name.nullIfDefault(defaultName),
                    enable = git.enable.nullIfDefault(true),
                    states = git.includeCrState,
                    types = git.includeCrTypes
                )

                CodeEventType.ISSUES -> nowExist.issue = IssueRule(
                    name = git.name.nullIfDefault(defaultName),
                    enable = git.enable.nullIfDefault(true),
                    action = git.includeIssueAction
                )

                CodeEventType.NOTE -> nowExist.note = NoteRule(
                    name = git.name.nullIfDefault(defaultName),
                    enable = git.enable.nullIfDefault(true),
                    types = git.includeNoteTypes?.map {
                        when (it) {
                            "Commit" -> "commit"
                            "Review" -> "merge_request"
                            "Issue" -> "issue"
                            else -> it
                        }
                    },
                    comment = git.includeNoteComment?.disjoin()
                )

                CodeEventType.POST_COMMIT -> nowExist.push = PushRule(
                    name = git.name.nullIfDefault(defaultName),
                    enable = git.enable.nullIfDefault(true),
                    branches = null,
                    paths = git.includePaths?.disjoin(),
                    pathsIgnore = git.excludePaths?.disjoin(),
                    users = git.includeUsers,
                    usersIgnore = git.excludeUsers,
                    pathFilterType = git.pathFilterType?.name.nullIfDefault(PathFilterType.NamePrefixFilter.name)
                )

                in CodeEventType.CODE_P4_EVENTS -> {
                    buildP4TriggerOn(
                        codeEventType = git.eventType,
                        triggerOn = nowExist,
                        rule = PushRule(
                            name = git.name.nullIfDefault(defaultName),
                            enable = git.enable.nullIfDefault(true),
                            branches = null,
                            branchesIgnore = null,
                            paths = git.includePaths?.disjoin(),
                            pathsIgnore = git.excludePaths?.disjoin()
                        )
                    )
                }

                CodeEventType.PULL_REQUEST -> nowExist.mr = MrRule(
                    name = git.name.nullIfDefault(defaultName),
                    enable = git.enable.nullIfDefault(true),
                    targetBranches = git.branchName?.disjoin(),
                    targetBranchesIgnore = git.excludeBranchName?.disjoin(),
                    sourceBranches = git.includeSourceBranchName?.disjoin(),
                    sourceBranchesIgnore = git.excludeSourceBranchName?.disjoin(),
                    paths = git.includePaths?.disjoin(),
                    pathsIgnore = git.excludePaths?.disjoin(),
                    users = git.includeUsers,
                    usersIgnore = git.excludeUsers,
                    pathFilterType = git.pathFilterType?.name.nullIfDefault(PathFilterType.NamePrefixFilter.name),
                    action = git.includeMrAction
                )

                else -> {}
            }
            aspectWrapper.setYamlTriggerOn(nowExist, PipelineTransferAspectWrapper.AspectType.AFTER)
        }
        return res.values.toList()
    }

    @Suppress("ComplexMethod")
    fun yaml2TriggerTGit(triggerOn: TriggerOn, elementQueue: MutableList<Element>) {
        val repositoryType = if (triggerOn.repoName.isNullOrBlank()) {
            TriggerRepositoryType.SELF
        } else {
            TriggerRepositoryType.NAME
        }
        triggerOn.push?.let { push ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    name = push.name ?: "TGit事件触发",
                    data = CodeTGitWebHookTriggerData(
                        input = CodeTGitWebHookTriggerInput(
                            branchName = push.branches.nonEmptyOrNull()?.join(),
                            excludeBranchName = push.branchesIgnore.nonEmptyOrNull()?.join(),
                            includePaths = push.paths.nonEmptyOrNull()?.join(),
                            excludePaths = push.pathsIgnore.nonEmptyOrNull()?.join(),
                            includeUsers = push.users,
                            excludeUsers = push.usersIgnore,
                            pathFilterType = push.pathFilterType?.let { PathFilterType.valueOf(it) }
                                ?: PathFilterType.NamePrefixFilter,
                            eventType = CodeEventType.PUSH,
                            includeMrAction = push.action ?: listOf(
                                TGitPushActionType.PUSH_FILE.value,
                                TGitPushActionType.NEW_BRANCH.value
                            ),
                            repositoryType = repositoryType,
                            repositoryName = triggerOn.repoName
                        )
                    )
                ).checkTriggerElementEnable(push.enable).apply {
                    version = "2.*"
                }
            )
        }

        triggerOn.tag?.let { tag ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    name = tag.name ?: "TGit事件触发",
                    data = CodeTGitWebHookTriggerData(
                        input = CodeTGitWebHookTriggerInput(
                            tagName = tag.tags.nonEmptyOrNull()?.join(),
                            excludeTagName = tag.tagsIgnore.nonEmptyOrNull()?.join(),
                            fromBranches = tag.fromBranches.nonEmptyOrNull()?.join(),
                            includeUsers = tag.users,
                            excludeUsers = tag.usersIgnore,
                            eventType = CodeEventType.TAG_PUSH,
                            repositoryType = repositoryType,
                            repositoryName = triggerOn.repoName
                        )
                    )
                ).checkTriggerElementEnable(tag.enable).apply {
                    version = "2.*"
                }
            )
        }

        triggerOn.mr?.let { mr ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    name = mr.name ?: "TGit事件触发",
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
                            block = mr.blockMr,
                            webhookQueue = mr.webhookQueue,
                            enableCheck = mr.reportCommitCheck,
                            pathFilterType = mr.pathFilterType?.let { PathFilterType.valueOf(it) }
                                ?: PathFilterType.NamePrefixFilter,
                            includeMrAction = mr.action ?: listOf(
                                TGitMrEventAction.OPEN.value,
                                TGitMrEventAction.REOPEN.value,
                                TGitMrEventAction.PUSH_UPDATE.value
                            ),
                            eventType = CodeEventType.MERGE_REQUEST,
                            repositoryType = repositoryType,
                            repositoryName = triggerOn.repoName,
                            skipWip = mr.skipWip
                        )
                    )
                ).checkTriggerElementEnable(mr.enable).apply {
                    version = "2.*"
                }
            )
        }

        triggerOn.review?.let { review ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    name = review.name ?: "TGit事件触发",
                    data = CodeTGitWebHookTriggerData(
                        input = CodeTGitWebHookTriggerInput(
                            includeCrState = review.states,
                            includeCrTypes = review.types,
                            eventType = CodeEventType.REVIEW,
                            repositoryType = repositoryType,
                            repositoryName = triggerOn.repoName
                        )
                    )
                ).checkTriggerElementEnable(review.enable).apply {
                    version = "2.*"
                }
            )
        }

        triggerOn.issue?.let { issue ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    name = issue.name ?: "TGit事件触发",
                    data = CodeTGitWebHookTriggerData(
                        input = CodeTGitWebHookTriggerInput(
                            includeIssueAction = issue.action,
                            eventType = CodeEventType.ISSUES,
                            repositoryType = repositoryType,
                            repositoryName = triggerOn.repoName
                        )
                    )
                ).checkTriggerElementEnable(issue.enable).apply {
                    version = "2.*"
                }
            )
        }

        triggerOn.note?.let { note ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    name = note.name ?: "TGit事件触发",
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
                            repositoryType = repositoryType,
                            repositoryName = triggerOn.repoName
                        )
                    )
                ).checkTriggerElementEnable(note.enable).apply {
                    version = "2.*"
                }
            )
        }
    }

    @Suppress("ComplexMethod")
    fun yaml2TriggerGithub(triggerOn: TriggerOn, elementQueue: MutableList<Element>) {
        val repositoryType = if (triggerOn.repoName.isNullOrBlank()) {
            TriggerRepositoryType.SELF
        } else {
            TriggerRepositoryType.NAME
        }
        triggerOn.push?.let { push ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    name = push.name ?: "GitHub事件触发",
                    branchName = push.branches.nonEmptyOrNull()?.join(),
                    excludeBranchName = push.branchesIgnore.nonEmptyOrNull()?.join(),
                    includePaths = push.paths.nonEmptyOrNull()?.join(),
                    excludePaths = push.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = push.users,
                    excludeUsers = push.usersIgnore.nonEmptyOrNull()?.join(),
                    pathFilterType = push.pathFilterType?.let { PathFilterType.valueOf(it) }
                        ?: PathFilterType.NamePrefixFilter,
                    eventType = CodeEventType.PUSH,
                    // todo action
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(push.enable)
            )
        }

        triggerOn.tag?.let { tag ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    name = tag.name ?: "GitHub事件触发",
                    tagName = tag.tags.nonEmptyOrNull()?.join(),
                    excludeTagName = tag.tagsIgnore.nonEmptyOrNull()?.join(),
                    fromBranches = tag.fromBranches.nonEmptyOrNull()?.join(),
                    includeUsers = tag.users,
                    excludeUsers = tag.usersIgnore.nonEmptyOrNull()?.join(),
                    eventType = CodeEventType.TAG_PUSH,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(tag.enable)
            )
        }

        triggerOn.mr?.let { mr ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    name = mr.name ?: "GitHub事件触发",
                    branchName = mr.targetBranches.nonEmptyOrNull()?.join(),
                    excludeBranchName = mr.targetBranchesIgnore.nonEmptyOrNull()?.join(),
                    includeSourceBranchName = mr.sourceBranches.nonEmptyOrNull()?.join(),
                    excludeSourceBranchName = mr.sourceBranchesIgnore.nonEmptyOrNull()?.join(),
                    includePaths = mr.paths.nonEmptyOrNull()?.join(),
                    excludePaths = mr.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = mr.users,
                    excludeUsers = mr.usersIgnore.nonEmptyOrNull()?.join(),
                    webhookQueue = mr.webhookQueue,
                    enableCheck = mr.reportCommitCheck,
                    pathFilterType = mr.pathFilterType?.let { PathFilterType.valueOf(it) }
                        ?: PathFilterType.NamePrefixFilter,
                    includeMrAction = mr.action,
                    eventType = CodeEventType.PULL_REQUEST,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(mr.enable)
            )
        }

        triggerOn.review?.let { review ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    name = review.name ?: "GitHub事件触发",
                    includeCrState = review.states,
                    includeCrTypes = review.types,
                    eventType = CodeEventType.REVIEW,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(review.enable)
            )
        }

        triggerOn.issue?.let { issue ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    name = issue.name ?: "GitHub事件触发",
                    includeIssueAction = issue.action,
                    eventType = CodeEventType.ISSUES,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(issue.enable)
            )
        }

        triggerOn.note?.let { note ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    name = note.name ?: "GitHub事件触发",
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
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(note.enable)
            )
        }
    }

    @Suppress("ComplexMethod")
    fun yaml2TriggerSvn(triggerOn: TriggerOn, elementQueue: MutableList<Element>) {
        val repositoryType = if (triggerOn.repoName.isNullOrBlank()) {
            TriggerRepositoryType.SELF
        } else {
            TriggerRepositoryType.NAME
        }
        triggerOn.push?.let { push ->
            elementQueue.add(
                CodeSVNWebHookTriggerElement(
                    name = push.name ?: "SVN事件触发",
                    relativePath = push.paths.nonEmptyOrNull()?.join(),
                    excludePaths = push.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = push.users,
                    excludeUsers = push.usersIgnore.nonEmptyOrNull(),
                    pathFilterType = push.pathFilterType?.let { PathFilterType.valueOf(it) }
                        ?: PathFilterType.NamePrefixFilter,
                    // todo action
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(push.enable)
            )
        }
    }

    @Suppress("ComplexMethod")
    fun yaml2TriggerP4(triggerOn: TriggerOn, elementQueue: MutableList<Element>) {
        with(triggerOn) {
            val repositoryType = if (repoName.isNullOrBlank()) {
                TriggerRepositoryType.SELF
            } else {
                TriggerRepositoryType.NAME
            }
            elementQueue.addAll(
                listOfNotNull(
                    buildP4TriggerElement(push, CodeEventType.CHANGE_COMMIT, repositoryType, repoName),
                    buildP4TriggerElement(changeCommit, CodeEventType.CHANGE_COMMIT, repositoryType, repoName),
                    buildP4TriggerElement(changeContent, CodeEventType.CHANGE_CONTENT, repositoryType, repoName),
                    buildP4TriggerElement(changeSubmit, CodeEventType.CHANGE_SUBMIT, repositoryType, repoName),
                    buildP4TriggerElement(shelveCommit, CodeEventType.SHELVE_COMMIT, repositoryType, repoName),
                    buildP4TriggerElement(shelveSubmit, CodeEventType.SHELVE_SUBMIT, repositoryType, repoName)
                )
            )
        }
    }

    @Suppress("ComplexMethod")
    fun yaml2TriggerBase(yamlInput: YamlTransferInput, triggerOn: TriggerOn, elementQueue: MutableList<Element>) {
        triggerOn.manual?.let { manual ->
            elementQueue.add(
                ManualTriggerElement(
                    name = manual.name ?: "手动触发",
                    id = "T-1-1-1",
                    canElementSkip = manual.canElementSkip,
                    useLatestParameters = manual.useLatestParameters
                ).apply {
                    this.additionalOptions = ElementAdditionalOptions(enable = manual.enable ?: true)
                }
            )
        }

        triggerOn.schedules?.let { schedule ->
            schedule.forEach { timer ->
                val repositoryType = when {
                    !timer.repoId.isNullOrBlank() ->
                        TriggerRepositoryType.ID

                    !timer.repoName.isNullOrBlank() ->
                        TriggerRepositoryType.NAME

                    timer.repoType == TriggerRepositoryType.NONE.name ->
                        null
                    // code -> ui,默认监听PAC代码库
                    else -> TriggerRepositoryType.SELF
                }
                elementQueue.add(
                    TimerTriggerElement(
                        name = timer.name ?: "定时触发",
                        repositoryType = repositoryType,
                        repoHashId = timer.repoId,
                        repoName = timer.repoName,
                        branches = timer.branches,
                        newExpression = timer.newExpression,
                        advanceExpression = timer.advanceExpression,
                        noScm = timer.always != true,
                        startParams = timer.startParams?.let {
                            val params = it.map { entry ->
                                mapOf(
                                    "key" to entry.key,
                                    "value" to entry.value
                                )
                            }
                            JsonUtil.toJson(params, false)
                        }
                    ).checkTriggerElementEnable(timer.enable)
                )
            }
        }

        triggerOn.remote?.let { remote ->
            elementQueue.add(
                RemoteTriggerElement(
                    name = remote.name ?: "远程触发",
                    remoteToken = yamlInput.pipelineInfo?.pipelineId?.let {
                        transferCache.getPipelineRemoteToken(
                            userId = yamlInput.userId,
                            projectId = yamlInput.projectCode,
                            pipelineId = it
                        )
                    } ?: ""
                ).checkTriggerElementEnable(remote.enable == EnableType.TRUE.value)
            )
        }
    }

    @Suppress("ComplexMethod")
    fun yaml2TriggerGitlab(triggerOn: TriggerOn, elementQueue: MutableList<Element>) {
        val repositoryType = if (triggerOn.repoName.isNullOrBlank()) {
            TriggerRepositoryType.SELF
        } else {
            TriggerRepositoryType.NAME
        }
        triggerOn.push?.let { push ->
            elementQueue.add(
                CodeGitlabWebHookTriggerElement(
                    name = push.name ?: "Gitlab事件触发",
                    branchName = push.branches.nonEmptyOrNull()?.join(),
                    excludeBranchName = push.branchesIgnore.nonEmptyOrNull()?.join(),
                    includePaths = push.paths.nonEmptyOrNull()?.join(),
                    excludePaths = push.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = push.users,
                    excludeUsers = push.usersIgnore,
                    pathFilterType = push.pathFilterType?.let { PathFilterType.valueOf(it) }
                        ?: PathFilterType.NamePrefixFilter,
                    eventType = CodeEventType.PUSH,
                    includeMrAction = push.action ?: listOf(
                        TGitPushActionType.PUSH_FILE.value,
                        TGitPushActionType.NEW_BRANCH.value
                    ),
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(push.enable).apply {
                    version = "2.*"
                }
            )
        }

        triggerOn.tag?.let { tag ->
            elementQueue.add(
                CodeGitlabWebHookTriggerElement(
                    name = tag.name ?: "Gitlab变更触发",
                    tagName = tag.tags.nonEmptyOrNull()?.join(),
                    excludeTagName = tag.tagsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = tag.users,
                    excludeUsers = tag.usersIgnore,
                    eventType = CodeEventType.TAG_PUSH,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(tag.enable)
            )
        }

        triggerOn.mr?.let { mr ->
            elementQueue.add(
                CodeGitlabWebHookTriggerElement(
                    name = mr.name ?: "Gitlab事件触发",
                    branchName = mr.targetBranches.nonEmptyOrNull()?.join(),
                    excludeBranchName = mr.targetBranchesIgnore.nonEmptyOrNull()?.join(),
                    includeSourceBranchName = mr.sourceBranches.nonEmptyOrNull()?.join(),
                    excludeSourceBranchName = mr.sourceBranchesIgnore.nonEmptyOrNull()?.join(),
                    includePaths = mr.paths.nonEmptyOrNull()?.join(),
                    excludePaths = mr.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = mr.users,
                    excludeUsers = mr.usersIgnore,
                    block = mr.blockMr,
                    pathFilterType = mr.pathFilterType?.let { PathFilterType.valueOf(it) }
                        ?: PathFilterType.NamePrefixFilter,
                    includeMrAction = mr.action ?: listOf(
                        TGitMrEventAction.OPEN.value,
                        TGitMrEventAction.REOPEN.value,
                        TGitMrEventAction.PUSH_UPDATE.value
                    ),
                    eventType = CodeEventType.MERGE_REQUEST,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(mr.enable).apply {
                    version = "2.*"
                }
            )
        }
    }

    private fun Element.checkTriggerElementEnable(enabled: Boolean?): Element {
        if (additionalOptions == null) {
            additionalOptions = ElementAdditionalOptions(runCondition = RunCondition.PRE_TASK_SUCCESS)
        }
        additionalOptions!!.enable = enabled ?: true
        return this
    }

    private fun List<String>.join() = this.joinToString(separator = ",")

    private fun String.disjoin() = this.split(",")

    private fun List<String>?.nonEmptyOrNull() = this?.ifEmpty { null }

    private fun buildP4TriggerElement(
        rule: PushRule?,
        eventType: CodeEventType,
        repositoryType: TriggerRepositoryType,
        repoName: String?
    ): Element? {
        return rule?.let {
            CodeP4WebHookTriggerElement(
                name = rule.name ?: "P4事件触发",
                data = CodeP4WebHookTriggerData(
                    input = CodeP4WebHookTriggerInput(
                        includePaths = rule.paths.nonEmptyOrNull()?.join(),
                        excludePaths = rule.pathsIgnore.nonEmptyOrNull()?.join(),
                        eventType = eventType,
                        repositoryType = repositoryType,
                        repositoryName = repoName
                    )
                )
            ).checkTriggerElementEnable(rule.enable).apply {
                // P4触发器(v2)仅支持CHANGE_COMMIT事件
                version = if (eventType == CodeEventType.CHANGE_COMMIT) "2.*" else "1.*"
            }
        }
    }

    fun buildP4TriggerOn(codeEventType: CodeEventType?, triggerOn: TriggerOn, rule: PushRule) {
        when (codeEventType) {
            CodeEventType.CHANGE_COMMIT -> triggerOn.changeCommit = rule
            CodeEventType.CHANGE_SUBMIT -> triggerOn.changeSubmit = rule
            CodeEventType.CHANGE_CONTENT -> triggerOn.changeContent = rule
            CodeEventType.SHELVE_COMMIT -> triggerOn.shelveCommit = rule
            CodeEventType.SHELVE_SUBMIT -> triggerOn.shelveSubmit = rule
            else -> {}
        }
    }
}
