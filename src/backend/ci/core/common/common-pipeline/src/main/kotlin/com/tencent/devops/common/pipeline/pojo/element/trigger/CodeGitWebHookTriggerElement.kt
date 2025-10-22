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

package com.tencent.devops.common.pipeline.pojo.element.trigger

import com.tencent.devops.common.api.enums.TriggerRepositoryType
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.ElementProp
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.PathFilterType
import com.tencent.devops.common.pipeline.utils.TriggerElementPropUtils.selector
import com.tencent.devops.common.pipeline.utils.TriggerElementPropUtils.staffInput
import com.tencent.devops.common.pipeline.utils.TriggerElementPropUtils.vuexInput
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Git事件触发", description = CodeGitWebHookTriggerElement.classType)
data class CodeGitWebHookTriggerElement(
    @get:Schema(title = "任务名称", required = true)
    override val name: String = "Git事件触发",
    @get:Schema(title = "id", required = false)
    override var id: String? = null,
    @get:Schema(title = "状态", required = false)
    override var status: String? = null,
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
    @get:Schema(title = "跳过WIP")
    val skipWip: Boolean? = false
) : WebHookTriggerElement(name, id, status) {
    companion object {
        const val classType = "codeGitWebHookTrigger"
        const val MERGE_ACTION_OPEN = "open"
        const val MERGE_ACTION_CLOSE = "close"
        const val MERGE_ACTION_REOPEN = "reopen"
        const val MERGE_ACTION_PUSH_UPDATE = "push-update"
        const val MERGE_ACTION_MERGE = "merge"
        const val PUSH_ACTION_CREATE_BRANCH = "new-branch"
        const val PUSH_ACTION_PUSH_FILE = "push-file"
    }

    override fun getClassType() = classType

    override fun findFirstTaskIdByStartType(startType: StartType): String {
        return if (startType.name == StartType.WEB_HOOK.name) {
            this.id!!
        } else {
            super.findFirstTaskIdByStartType(startType)
        }
    }

    // 增加条件这里也要补充上,不然代码库触发器列表展示会不对
    override fun triggerCondition(): List<ElementProp> {
        val props = when (eventType) {
            CodeEventType.PUSH -> {
                listOf(
                    vuexInput(name = "action", value = joinToString(includePushAction)),
                    vuexInput(name = "branchName", value = branchName),
                    vuexInput(name = "excludeBranchName", value = excludeBranchName),
                    vuexInput(name = "includePaths", value = includePaths),
                    vuexInput(name = "excludePaths", value = excludePaths),
                    staffInput(name = "includeUsers", value = includeUsers),
                    staffInput(name = "excludeUsers", value = excludeUsers)
                )
            }

            CodeEventType.MERGE_REQUEST -> {
                listOf(
                    vuexInput(name = "action", value = joinToString(includeMrAction)),
                    selector(name = "skip-wip", value = listOf((skipWip ?: false).toString())),
                    vuexInput(name = "branchName", value = branchName),
                    vuexInput(name = "excludeBranchName", value = excludeBranchName),
                    vuexInput(name = "includeSourceBranchName", value = includeSourceBranchName),
                    vuexInput(name = "includeSourceBranchName", value = includeSourceBranchName),
                    vuexInput(name = "includePaths", value = includePaths),
                    vuexInput(name = "excludePaths", value = excludePaths),
                    staffInput(name = "includeUsers", value = includeUsers),
                    staffInput(name = "excludeUsers", value = excludeUsers)
                )
            }

            CodeEventType.MERGE_REQUEST_ACCEPT -> {
                listOf(
                    vuexInput(name = "action", value = "merge"),
                    vuexInput(name = "branchName", value = branchName),
                    vuexInput(name = "excludeBranchName", value = excludeBranchName),
                    vuexInput(name = "includeSourceBranchName", value = includeSourceBranchName),
                    vuexInput(name = "includeSourceBranchName", value = includeSourceBranchName),
                    vuexInput(name = "includePaths", value = includePaths),
                    vuexInput(name = "excludePaths", value = excludePaths),
                    staffInput(name = "includeUsers", value = includeUsers),
                    staffInput(name = "excludeUsers", value = excludeUsers)
                )
            }

            CodeEventType.TAG_PUSH -> {
                listOf(
                    vuexInput(name = "tagName", value = tagName),
                    vuexInput(name = "excludeTagName", value = excludeTagName),
                    vuexInput(name = "fromBranches", value = fromBranches),
                    staffInput(name = "includeUsers", value = includeUsers),
                    staffInput(name = "excludeUsers", value = excludeUsers)
                )
            }

            CodeEventType.REVIEW -> {
                listOf(
                    selector(name = "includeCrState", value = includeCrState),
                    selector(name = "includeCrTypes", value = includeCrTypes)
                )
            }

            CodeEventType.ISSUES -> {
                listOf(
                    selector(name = "includeIssueAction", value = includeIssueAction)
                )
            }

            CodeEventType.NOTE -> {
                listOf(
                    selector(name = "includeNoteTypes", value = includeNoteTypes),
                    vuexInput(name = "includeNoteComment", value = includeNoteComment)
                )
            }

            else ->
                emptyList()
        }
        return props.filterNotNull()
    }
}
