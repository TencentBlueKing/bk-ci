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

import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.ElementProp
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.utils.TriggerElementPropUtils.selector
import com.tencent.devops.common.pipeline.utils.TriggerElementPropUtils.staffInput
import com.tencent.devops.common.pipeline.utils.TriggerElementPropUtils.vuexInput
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("TGit_Ce事件触发", description = CodeTGitCeWebHookTriggerElement.classType)
data class CodeTGitCeWebHookTriggerElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "TGit变更触发",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("数据", required = true)
    val data: CodeTGitWebHookTriggerData
) : WebHookTriggerElement(name, id, status) {
    companion object {
        const val classType = "codeTGitCeWebHookTrigger"
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
