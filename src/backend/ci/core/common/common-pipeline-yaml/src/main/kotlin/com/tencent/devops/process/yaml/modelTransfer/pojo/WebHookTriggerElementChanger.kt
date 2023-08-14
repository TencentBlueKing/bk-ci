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

package com.tencent.devops.process.yaml.modelTransfer.pojo

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.PathFilterType
import io.swagger.annotations.ApiModelProperty

data class WebHookTriggerElementChanger(
    @ApiModelProperty("任务名称", required = true)
    val name: String = "Git变更触发",
    @ApiModelProperty("仓库ID", required = true)
    val repositoryHashId: String? = null,
    @ApiModelProperty("分支名称", required = false)
    val branchName: String? = null,
    @ApiModelProperty("用于排除的分支名", required = false)
    val excludeBranchName: String? = null,
    @ApiModelProperty("路径过滤类型", required = true)
    val pathFilterType: PathFilterType? = PathFilterType.NamePrefixFilter,
    @ApiModelProperty("用于包含的路径", required = false)
    val includePaths: String? = null,
    @ApiModelProperty("用于排除的路径", required = false)
    val excludePaths: String? = null,
    @ApiModelProperty("用户白名单", required = false)
    val includeUsers: List<String>? = null,
    @ApiModelProperty("用于排除的user id", required = false)
    val excludeUsers: List<String>? = null,
    @ApiModelProperty("事件类型", required = false)
    val eventType: CodeEventType?,
    @ApiModelProperty("是否为block", required = false)
    val block: Boolean? = null,
    @ApiModelProperty("新版的git原子的类型")
    val repositoryType: RepositoryType? = null,
    @ApiModelProperty("新版的git代码库名")
    val repositoryName: String? = null,
    @ApiModelProperty("tag名称", required = false)
    val tagName: String? = null,
    @ApiModelProperty("用于排除的tag名称", required = false)
    val excludeTagName: String? = null,
    @ApiModelProperty("tag从哪条分支创建", required = false)
    val fromBranches: String? = null,
    @ApiModelProperty("用于排除的源分支名称", required = false)
    val excludeSourceBranchName: String? = null,
    @ApiModelProperty("用于包含的源分支名称", required = false)
    val includeSourceBranchName: String? = null,
    @ApiModelProperty("webhook队列", required = false)
    val webhookQueue: Boolean? = false,
    @ApiModelProperty("code review 状态", required = false)
    val includeCrState: List<String>? = null,
    @ApiModelProperty("code review 类型", required = false)
    val includeCrTypes: List<String>? = null,
    @ApiModelProperty("code note comment", required = false)
    val includeNoteComment: String? = null,
    @ApiModelProperty("code note 类型", required = false)
    val includeNoteTypes: List<String>? = null,
    @ApiModelProperty("是否启用回写")
    val enableCheck: Boolean? = true,
    @ApiModelProperty("issue事件action")
    val includeIssueAction: List<String>? = null,
    @ApiModelProperty("mr事件action")
    val includeMrAction: List<String>? = null,
    @ApiModelProperty("push事件action")
    val includePushAction: List<String>? = null,
    @ApiModelProperty("是否启用第三方过滤")
    val enableThirdFilter: Boolean? = false,
    @ApiModelProperty("第三方应用地址")
    val thirdUrl: String? = null,
    @ApiModelProperty("第三方应用鉴权token")
    val thirdSecretToken: String? = null
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
        thirdSecretToken = data.thirdSecretToken
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
        enableThirdFilter = data.data.input.enableThirdFilter
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
        enableThirdFilter = data.enableThirdFilter
    )
}
