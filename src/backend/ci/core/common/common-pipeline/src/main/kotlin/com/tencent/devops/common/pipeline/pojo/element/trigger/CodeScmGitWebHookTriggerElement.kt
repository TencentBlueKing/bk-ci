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
import com.tencent.devops.common.pipeline.utils.TriggerElementPropUtils.vuexInput
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "ScmGit事件触发", description = CodeScmGitWebHookTriggerElement.classType)
data class CodeScmGitWebHookTriggerElement(
    @get:Schema(title = "任务名称", required = true)
    override val name: String = "Git通用事件触发",
    @get:Schema(title = "id", required = false)
    override var id: String? = null,
    @get:Schema(title = "状态", required = false)
    override var status: String? = null,
    @get:Schema(title = "数据", required = true)
    val data: CodeScmGitWebHookTriggerData
) : WebHookTriggerElement(name, id, status) {
    companion object {
        const val classType = "codeScmGitWebHookTrigger"
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
        with(data.input) {
            val props = when (eventType) {
                CodeEventType.PUSH -> {
                    listOf(
                        vuexInput(name = "action", value = joinToString(actions)),
                        vuexInput(name = "branchName", value = branchName),
                        vuexInput(name = "excludeBranchName", value = excludeBranchName),
                        vuexInput(name = "includePaths", value = includePaths),
                        vuexInput(name = "excludePaths", value = excludePaths),
                        vuexInput(name = "includeUsers", value = includeUsers),
                        vuexInput(name = "excludeUsers", value = excludeUsers)
                    )
                }

                CodeEventType.MERGE_REQUEST -> {
                    listOf(
                        vuexInput(name = "action", value = joinToString(actions)),
                        vuexInput(name = "branchName", value = branchName),
                        vuexInput(name = "excludeBranchName", value = excludeBranchName),
                        vuexInput(name = "includeSourceBranchName", value = includeSourceBranchName),
                        vuexInput(name = "includeSourceBranchName", value = includeSourceBranchName),
                        vuexInput(name = "includePaths", value = includePaths),
                        vuexInput(name = "excludePaths", value = excludePaths),
                        vuexInput(name = "includeUsers", value = includeUsers),
                        vuexInput(name = "excludeUsers", value = excludeUsers)
                    )
                }

                CodeEventType.TAG_PUSH -> {
                    listOf(
                        vuexInput(name = "tagName", value = tagName),
                        vuexInput(name = "excludeTagName", value = excludeTagName),
                        vuexInput(name = "includeUsers", value = includeUsers),
                        vuexInput(name = "excludeUsers", value = excludeUsers)
                    )
                }

                else ->
                    emptyList()
            }
            return props.filterNotNull()
        }
    }
}

data class CodeScmGitWebHookTriggerData(
    @get:Schema(title = "ScmGit事件触发数据", required = false)
    val input: CodeScmGitWebHookTriggerInput
)

data class CodeScmGitWebHookTriggerInput(
    @get:Schema(title = "代码库标识", required = true)
    val scmCode: String,
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
    val includeUsers: String? = null,
    @get:Schema(title = "用于排除的user id", required = false)
    val excludeUsers: String? = null,
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
    @get:Schema(title = "用于排除的源分支名称", required = false)
    val excludeSourceBranchName: String? = null,
    @get:Schema(title = "用于包含的源分支名称", required = false)
    val includeSourceBranchName: String? = null,
    @get:Schema(title = "是否启用回写")
    val enableCheck: Boolean? = true,
    @get:Schema(title = "事件动作")
    val actions: List<String>? = null
)
