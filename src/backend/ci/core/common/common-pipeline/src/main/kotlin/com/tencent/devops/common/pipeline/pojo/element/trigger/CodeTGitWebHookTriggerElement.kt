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

package com.tencent.devops.common.pipeline.pojo.element.trigger

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.ElementProp
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.PathFilterType
import com.tencent.devops.common.pipeline.utils.TriggerElementPropUtils.selector
import com.tencent.devops.common.pipeline.utils.TriggerElementPropUtils.staffInput
import com.tencent.devops.common.pipeline.utils.TriggerElementPropUtils.vuexInput
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "TGit事件触发", description = CodeTGitWebHookTriggerElement.classType)
data class CodeTGitWebHookTriggerElement(
    @Schema(name = "任务名称", required = true)
    override val name: String = "TGit变更触发",
    @Schema(name = "id", required = false)
    override var id: String? = null,
    @Schema(name = "状态", required = false)
    override var status: String? = null,
    @Schema(name = "数据", required = true)
    val data: CodeTGitWebHookTriggerData
) : WebHookTriggerElement(name, id, status) {
    companion object {
        const val classType = "codeTGitWebHookTrigger"
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
        with(data.input) {
            val props = when (eventType) {
                CodeEventType.PUSH -> {
                    listOf(
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
                        vuexInput(name = "branchName", value = branchName),
                        vuexInput(name = "excludeBranchName", value = excludeBranchName),
                        vuexInput(
                            name = "includeSourceBranchName",
                            value = includeSourceBranchName
                        ),
                        vuexInput(
                            name = "includeSourceBranchName",
                            value = includeSourceBranchName
                        ),
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
                        vuexInput(
                            name = "includeSourceBranchName",
                            value = includeSourceBranchName
                        ),
                        vuexInput(
                            name = "includeSourceBranchName",
                            value = includeSourceBranchName
                        ),
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
                        vuexInput(name = "fromBranches", value = fromBranches)
                    )
                }

                CodeEventType.REVIEW -> {
                    listOf(
                        selector(name = "includeCrState", value = includeCrState)
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
                    listOf()
            }
            return props.filterNotNull()
        }
    }
}

data class CodeTGitWebHookTriggerData(
    @Schema(name = "TGit事件触发数据", required = false)
    val input: CodeTGitWebHookTriggerInput
)

@Schema(name = "TGit事件触发数据")
data class CodeTGitWebHookTriggerInput(
    @Schema(name = "仓库ID", required = true)
    val repositoryHashId: String?,
    @Schema(name = "分支名称", required = false)
    val branchName: String?,
    @Schema(name = "用于排除的分支名", required = false)
    val excludeBranchName: String?,
    @Schema(name = "路径过滤类型", required = true)
    val pathFilterType: PathFilterType? = PathFilterType.NamePrefixFilter,
    @Schema(name = "用于包含的路径", required = false)
    val includePaths: String?,
    @Schema(name = "用于排除的路径", required = false)
    val excludePaths: String?,
    @Schema(name = "用户白名单", required = false)
    val includeUsers: List<String>? = null,
    @Schema(name = "用于排除的user id", required = false)
    val excludeUsers: List<String>?,
    @Schema(name = "事件类型", required = false)
    val eventType: CodeEventType?,
    @Schema(name = "是否为block", required = false)
    val block: Boolean?,
    @Schema(name = "新版的git原子的类型")
    val repositoryType: RepositoryType? = null,
    @Schema(name = "新版的git代码库名")
    val repositoryName: String? = null,
    @Schema(name = "tag名称", required = false)
    val tagName: String? = null,
    @Schema(name = "用于排除的tag名称", required = false)
    val excludeTagName: String? = null,
    @Schema(name = "用于排除的源分支名称", required = false)
    val excludeSourceBranchName: String? = null,
    @Schema(name = "用于包含的源分支名称", required = false)
    val includeSourceBranchName: String? = null,
    @Schema(name = "tag从哪条分支创建", required = false)
    val fromBranches: String? = null,
    @Schema(name = "code review 状态", required = false)
    val includeCrState: List<String>? = null,
    @Schema(name = "code note comment", required = false)
    val includeNoteComment: String? = null,
    @Schema(name = "code note 类型", required = false)
    val includeNoteTypes: List<String>? = null,
    @Schema(name = "issue事件action")
    val includeIssueAction: List<String>? = null,
    @Schema(name = "是否启用回写")
    val enableCheck: Boolean? = true,
    @Schema(name = "mr事件action")
    val includeMrAction: List<String>? = null,
    @Schema(name = "push事件action")
    val includePushAction: List<String>? = null,
    @Schema(name = "webhook队列", required = false)
    val webhookQueue: Boolean? = false
)
