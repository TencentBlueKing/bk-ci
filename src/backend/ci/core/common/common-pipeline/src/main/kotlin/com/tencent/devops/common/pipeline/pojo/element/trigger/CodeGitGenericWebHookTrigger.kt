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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.pipeline.pojo.element.trigger

import com.tencent.devops.common.api.enums.RepositoryTypeNew
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Git通用事件触发", description = CodeGitGenericWebHookTriggerElement.classType)
data class CodeGitGenericWebHookTriggerElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "TGit变更触发",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("数据", required = true)
    val data: CodeGitGenericWebHookTriggerData
) : WebHookTriggerElement(name, id, status) {
    companion object {
        const val classType = "codeGitGenericWebHookTrigger"
    }

    override fun getClassType() = classType
}

data class CodeGitGenericWebHookTriggerData(
    val input: CodeGitGenericWebHookTriggerInput
)

data class CodeGitGenericWebHookTriggerInput(
    @ApiModelProperty("代码库类型", required = true)
    val scmType: String,
    @ApiModelProperty("代码库类型")
    val repositoryType: RepositoryTypeNew? = null,
    @ApiModelProperty("仓库ID", required = true)
    val repositoryHashId: String?,
    @ApiModelProperty("代码库名")
    val repositoryName: String? = null,
    @ApiModelProperty("代码库链接", required = true)
    val repositoryUrl: String,
    @ApiModelProperty("凭证ID", required = false)
    val credentialId: String? = null,
    @ApiModelProperty("凭证ID", required = false)
    val token: String? = null,
    @ApiModelProperty("触发事件", required = true)
    val eventType: String = CodeEventType.PUSH.name,
    @ApiModelProperty("hook地址,目前只对codecc开放", required = false)
    var hookUrl: String? = null,
    @ApiModelProperty("branch", required = false)
    val branchName: String?,
    @ApiModelProperty("excludeBranch", required = false)
    val excludeBranchName: String?,
    @ApiModelProperty("includePaths", required = false)
    val includePaths: String?,
    @ApiModelProperty("相对路径", required = true)
    val relativePath: String?,
    @ApiModelProperty("excludePaths", required = false)
    val excludePaths: String?,
    @ApiModelProperty("excludeUsers", required = false)
    val excludeUsers: List<String>?,
    @ApiModelProperty("用户白名单", required = false)
    val includeUsers: List<String>?,
    @ApiModelProperty("block", required = false)
    val block: Boolean?,
    @ApiModelProperty("tagName", required = false)
    val tagName: String? = null,
    @ApiModelProperty("excludeTagName", required = false)
    val excludeTagName: String? = null,
    @ApiModelProperty("excludeSourceBranchName", required = false)
    val excludeSourceBranchName: String? = null,
    @ApiModelProperty("includeSourceBranchName", required = false)
    val includeSourceBranchName: String? = null
)