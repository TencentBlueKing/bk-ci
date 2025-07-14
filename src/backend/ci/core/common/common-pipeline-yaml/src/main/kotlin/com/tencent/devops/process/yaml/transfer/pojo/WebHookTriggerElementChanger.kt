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

package com.tencent.devops.process.yaml.transfer.pojo

import com.tencent.devops.common.api.enums.TriggerRepositoryType
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeScmGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeScmSvnWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.PathFilterType
import com.tencent.devops.common.webhook.util.WebhookUtils
import io.swagger.v3.oas.annotations.media.Schema

data class WebHookTriggerElementChanger(
    @get:Schema(title = "任务名称", required = true)
    val name: String = "Git变更触发",
    @get:Schema(title = "仓库ID", required = true)
    val repositoryHashId: String? = null,
    @get:Schema(title = "分支名称", required = false)
    val branchName: String? = null,
    @get:Schema(title = "用于排除的分支名", required = false)
    val excludeBranchName: String? = null,
    @get:Schema(title = "路径过滤类型", required = true)
    val pathFilterType: PathFilterType? = PathFilterType.NamePrefixFilter,
    @get:Schema(title = "用于包含的路径", required = false)
    val includePaths: String? = null,
    @get:Schema(title = "用于排除的路径", required = false)
    val excludePaths: String? = null,
    @get:Schema(title = "用户白名单", required = false)
    val includeUsers: List<String>? = null,
    @get:Schema(title = "用于排除的user id", required = false)
    val excludeUsers: List<String>? = null,
    @get:Schema(title = "事件类型", required = false)
    val eventType: CodeEventType?,
    @get:Schema(title = "是否为block", required = false)
    val block: Boolean? = null,
    @get:Schema(title = "新版的git原子的类型")
    val repositoryType: TriggerRepositoryType? = null,
    @get:Schema(title = "新版的git代码库名")
    val repositoryName: String? = null,
    @get:Schema(title = "tag名称", required = false)
    val tagName: String? = null,
    @get:Schema(title = "用于排除的tag名称", required = false)
    val excludeTagName: String? = null,
    @get:Schema(title = "tag从哪条分支创建", required = false)
    val fromBranches: String? = null,
    @get:Schema(title = "用于排除的源分支名称", required = false)
    val excludeSourceBranchName: String? = null,
    @get:Schema(title = "用于包含的源分支名称", required = false)
    val includeSourceBranchName: String? = null,
    @get:Schema(title = "webhook队列", required = false)
    val webhookQueue: Boolean? = false,
    @get:Schema(title = "code review 状态", required = false)
    val includeCrState: List<String>? = null,
    @get:Schema(title = "code review 类型", required = false)
    val includeCrTypes: List<String>? = null,
    @get:Schema(title = "code note comment", required = false)
    val includeNoteComment: String? = null,
    @get:Schema(title = "code note 类型", required = false)
    val includeNoteTypes: List<String>? = null,
    @get:Schema(title = "是否启用回写")
    val enableCheck: Boolean? = true,
    @get:Schema(title = "issue事件action")
    val includeIssueAction: List<String>? = null,
    @get:Schema(title = "mr事件action")
    val includeMrAction: List<String>? = null,
    @get:Schema(title = "push事件action")
    val includePushAction: List<String>? = null,
    @get:Schema(title = "是否启用第三方过滤")
    val enableThirdFilter: Boolean? = false,
    @get:Schema(title = "第三方应用地址")
    val thirdUrl: String? = null,
    @get:Schema(title = "第三方应用鉴权token")
    val thirdSecretToken: String? = null,
    @get:Schema(title = "是否启用插件")
    val enable: Boolean,
    @get:Schema(title = "跳过WIP")
    val skipWip: Boolean? = false,
    @get:Schema(title = "代码库标识")
    val scmCode: String? = null
) {
    constructor(data: CodeGitWebHookTriggerElement) : this(
        name = data.name,
        repositoryHashId = data.repositoryHashId,
        branchName = data.branchName,
        excludeBranchName = data.excludeBranchName,
        pathFilterType = data.pathFilterType,
        includePaths = data.includePaths,
        excludePaths = data.excludePaths,
        includeUsers = data.includeUsers,
        excludeUsers = data.excludeUsers,
        eventType = data.eventType,
        block = data.block,
        repositoryType = data.repositoryType,
        repositoryName = data.repositoryName,
        tagName = data.tagName,
        excludeTagName = data.excludeTagName,
        fromBranches = data.fromBranches,
        excludeSourceBranchName = data.excludeSourceBranchName,
        includeSourceBranchName = data.includeSourceBranchName,
        webhookQueue = data.webhookQueue,
        includeCrState = data.includeCrState,
        includeCrTypes = data.includeCrTypes,
        includeNoteComment = data.includeNoteComment,
        includeNoteTypes = data.includeNoteTypes,
        enableCheck = data.enableCheck,
        includeIssueAction = data.includeIssueAction,
        includeMrAction = data.includeMrAction,
        includePushAction = data.includePushAction,
        enableThirdFilter = data.enableThirdFilter,
        thirdUrl = data.thirdUrl,
        thirdSecretToken = data.thirdSecretToken,
        enable = data.elementEnabled(),
        skipWip = data.skipWip
    )

    constructor(data: CodeTGitWebHookTriggerElement) : this(
        name = data.name,
        repositoryHashId = data.data.input.repositoryHashId,
        branchName = data.data.input.branchName,
        excludeBranchName = data.data.input.excludeBranchName,
        pathFilterType = data.data.input.pathFilterType,
        includePaths = data.data.input.includePaths,
        excludePaths = data.data.input.excludePaths,
        includeUsers = data.data.input.includeUsers,
        excludeUsers = data.data.input.excludeUsers,
        eventType = data.data.input.eventType,
        block = data.data.input.block,
        repositoryType = data.data.input.repositoryType,
        repositoryName = data.data.input.repositoryName,
        tagName = data.data.input.tagName,
        excludeTagName = data.data.input.excludeTagName,
        fromBranches = data.data.input.fromBranches,
        excludeSourceBranchName = data.data.input.excludeSourceBranchName,
        includeSourceBranchName = data.data.input.includeSourceBranchName,
        webhookQueue = data.data.input.webhookQueue,
        includeCrState = data.data.input.includeCrState,
        includeCrTypes = data.data.input.includeCrTypes,
        includeNoteComment = data.data.input.includeNoteComment,
        includeNoteTypes = data.data.input.includeNoteTypes,
        enableCheck = data.data.input.enableCheck,
        includeIssueAction = data.data.input.includeIssueAction,
        includeMrAction = data.data.input.includeMrAction,
        includePushAction = data.data.input.includePushAction,
        enableThirdFilter = data.data.input.enableThirdFilter,
        enable = data.elementEnabled(),
        skipWip = data.data.input.skipWip
    )

    constructor(data: CodeGithubWebHookTriggerElement) : this(
        name = data.name,
        repositoryHashId = data.repositoryHashId,
        branchName = data.branchName,
        excludeBranchName = data.excludeBranchName,
        pathFilterType = data.pathFilterType,
        includePaths = data.includePaths,
        excludePaths = data.excludePaths,
        includeUsers = data.includeUsers,
        excludeUsers = data.excludeUsers?.split(","),
        eventType = data.eventType,
        repositoryType = data.repositoryType,
        repositoryName = data.repositoryName,
        tagName = data.tagName,
        excludeTagName = data.excludeTagName,
        fromBranches = data.fromBranches,
        excludeSourceBranchName = data.excludeSourceBranchName,
        includeSourceBranchName = data.includeSourceBranchName,
        webhookQueue = data.webhookQueue,
        includeCrState = data.includeCrState,
        includeCrTypes = data.includeCrTypes,
        includeNoteComment = data.includeNoteComment,
        includeNoteTypes = data.includeNoteTypes,
        enableCheck = data.enableCheck,
        includeIssueAction = data.includeIssueAction,
        includeMrAction = data.includeMrAction,
        includePushAction = data.includePushAction,
        enableThirdFilter = data.enableThirdFilter,
        enable = data.elementEnabled()
    )

    constructor(data: CodeSVNWebHookTriggerElement) : this(
        name = data.name,
        repositoryHashId = data.repositoryHashId,
        pathFilterType = data.pathFilterType,
        includePaths = data.relativePath,
        excludePaths = data.excludePaths,
        includeUsers = data.includeUsers,
        excludeUsers = data.excludeUsers,
        eventType = CodeEventType.POST_COMMIT,
        repositoryType = data.repositoryType,
        repositoryName = data.repositoryName,
        enable = data.elementEnabled()
    )

    constructor(data: CodeP4WebHookTriggerElement) : this(
        name = data.name,
        repositoryHashId = data.data.input.repositoryHashId,
        includePaths = data.data.input.includePaths,
        excludePaths = data.data.input.excludePaths,
        eventType = data.data.input.eventType,
        repositoryType = data.data.input.repositoryType,
        repositoryName = data.data.input.repositoryName,
        enable = data.elementEnabled()
    )

    constructor(data: CodeGitlabWebHookTriggerElement) : this(
        name = data.name,
        repositoryHashId = data.repositoryHashId,
        branchName = data.branchName,
        excludeBranchName = data.excludeBranchName,
        pathFilterType = data.pathFilterType,
        includePaths = data.includePaths,
        excludePaths = data.excludePaths,
        includeUsers = data.includeUsers,
        excludeUsers = data.excludeUsers,
        eventType = data.eventType,
        block = data.block,
        repositoryType = data.repositoryType,
        repositoryName = data.repositoryName,
        tagName = data.tagName,
        excludeTagName = data.excludeTagName,
        excludeSourceBranchName = data.excludeSourceBranchName,
        includeSourceBranchName = data.includeSourceBranchName,
        includeMrAction = data.includeMrAction,
        includePushAction = data.includePushAction,
        enable = data.elementEnabled()
    )

    constructor(data: CodeScmGitWebHookTriggerElement) : this(
        name = data.name,
        repositoryHashId = data.data.input.repositoryHashId,
        branchName = data.data.input.branchName,
        excludeBranchName = data.data.input.excludeBranchName,
        pathFilterType = data.data.input.pathFilterType,
        includePaths = data.data.input.includePaths,
        excludePaths = data.data.input.excludePaths,
        includeUsers = WebhookUtils.convert(data.data.input.includeUsers),
        excludeUsers = WebhookUtils.convert(data.data.input.excludeUsers),
        eventType = data.data.input.eventType,
        block = data.data.input.block,
        repositoryType = data.data.input.repositoryType,
        repositoryName = data.data.input.repositoryName,
        tagName = data.data.input.tagName,
        excludeTagName = data.data.input.excludeTagName,
        excludeSourceBranchName = data.data.input.excludeSourceBranchName,
        includeSourceBranchName = data.data.input.includeSourceBranchName,
        enableCheck = data.data.input.enableCheck,
        includeMrAction = if (data.data.input.eventType == CodeEventType.MERGE_REQUEST) {
            // action 统一格式
            data.data.input.actions
        } else listOf(),
        includePushAction = if (data.data.input.eventType == CodeEventType.PUSH) {
            data.data.input.actions
        } else listOf(),
        enable = data.elementEnabled(),
        scmCode = data.data.input.scmCode
    )

    constructor(data: CodeScmSvnWebHookTriggerElement) : this(
        name = data.name,
        repositoryHashId = data.data.input.repositoryHashId,
        pathFilterType = data.data.input.pathFilterType,
        includePaths = data.data.input.relativePath,
        excludePaths = data.data.input.excludePaths,
        includeUsers = WebhookUtils.convert(data.data.input.includeUsers),
        excludeUsers = WebhookUtils.convert(data.data.input.excludeUsers),
        eventType = data.data.input.eventType,
        repositoryType = data.data.input.repositoryType,
        repositoryName = data.data.input.repositoryName,
        enable = data.elementEnabled()
    )
}
