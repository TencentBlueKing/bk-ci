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

package com.tencent.devops.stream.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("蓝盾工蜂项目配置")
data class GitRepositoryConf(
    @ApiModelProperty("工蜂项目ID")
    override val gitProjectId: Long,
    @ApiModelProperty("工蜂项目名")
    override val name: String,
    @ApiModelProperty("工蜂项目url")
    override val url: String,
    @ApiModelProperty("homepage")
    override val homepage: String,
    @ApiModelProperty("gitHttpUrl")
    override val gitHttpUrl: String,
    @ApiModelProperty("gitSshUrl")
    override val gitSshUrl: String,
    @ApiModelProperty("是否启用CI")
    val enableCi: Boolean,
    @ApiModelProperty("Build pushed branches")
    val buildPushedBranches: Boolean = true,
    @ApiModelProperty("Limit concurrent jobs")
    val limitConcurrentJobs: Int?,
    @ApiModelProperty("Build pushed pull request")
    val buildPushedPullRequest: Boolean = true,
    @ApiModelProperty("Auto cancel branch builds")
    val autoCancelBranchBuilds: Boolean = false,
    @ApiModelProperty("Auto cancel pull request builds")
    val autoCancelPullRequestBuilds: Boolean = false,
    @ApiModelProperty("Environment variable")
    val env: List<EnvironmentVariables>?,
    @ApiModelProperty("创建时间")
    val createTime: Long?,
    @ApiModelProperty("修改时间")
    val updateTime: Long?,
    @ApiModelProperty("蓝盾项目Code")
    val projectCode: String?,
    @ApiModelProperty("企业微信客服通知")
    val rtxCustomProperty: RtxCustomProperty?,
    @ApiModelProperty("企业微信群通知")
    val rtxGroupProperty: RtxGroupProperty?,
    @ApiModelProperty("邮件通知")
    val emailProperty: EmailProperty?,
    @ApiModelProperty("是否只在失败时通知")
    val onlyFailedNotify: Boolean? = true,
    @ApiModelProperty("是否开启Mr锁定")
    val enableMrBlock: Boolean? = true
) : Repository(gitProjectId, name, url, homepage, gitHttpUrl, gitSshUrl)
